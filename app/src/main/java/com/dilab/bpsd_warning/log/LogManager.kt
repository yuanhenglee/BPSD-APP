package com.dilab.bpsd_warning.log

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // Make sure this is imported
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Collections // Import for Collections.synchronizedList
import java.util.LinkedList
import java.io.IOException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class LogManager(private val context: Context) {
    private val logList: MutableList<LogItem> = Collections.synchronizedList(LinkedList<LogItem>())
    private lateinit var logFile: File // Initialize in init

    private val url = "http://140.113.193.87:20061/get_warning"
    private val topic = context.getSharedPreferences("device", Context.MODE_PRIVATE).getString("device", "All")

    var adapter: LogAdapter

    private val logScope = CoroutineScope(Dispatchers.IO)

    init {
        val logDir = File(context.filesDir, "log")
        logFile = File(logDir, "log.json") // logFile needs to be initialized before syncLogs or other uses

        logScope.launch { // Still on Dispatchers.IO
            if (!logDir.exists()) {
                Log.d("LogManager", "Creating log directory")
                try {
                    logDir.mkdir()
                } catch (e: SecurityException) {
                    Log.e("LogManager", "Error creating log directory", e)
                }
            }
            if (!logFile.exists()) {
                Log.d("LogManager", "Creating log file")
                try {
                    logFile.createNewFile()
                } catch (e: IOException) {
                    Log.e("LogManager", "Error creating log file", e)
                }
            }

            // After file operations, switch to Main thread for adapter init and syncLogs call
            withContext(Dispatchers.Main) {
                adapter = LogAdapter(logList)
                syncLogs()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addLog(logItem: LogItem) {
        logList.add(0, logItem) // Changed from addFirst to add(0, ...)
        logScope.launch {
            list2File()
        }
        // UI update must be on the main thread
        Handler(Looper.getMainLooper()).post {
            adapter.notifyItemInserted(0)
            // Consider scrolling to top: recyclerView.scrollToPosition(0) if applicable
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearLogs() {
        logList.clear() // Clear list immediately for UI responsiveness
        logScope.launch {
            list2File() // Perform file operation in background
        }
        // UI update must be on the main thread
        Handler(Looper.getMainLooper()).post {
            adapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun syncLogs() {
        val client = OkHttpClient()
        val requestBodyJson = JSONObject()
        try {
            requestBodyJson.put("topic", topic)
        } catch (e: org.json.JSONException) {
            Log.e("LogManager", "Error creating JSON for syncLogs", e)
            // Handle error, perhaps by not attempting the sync or logging appropriately
            return
        }
        val requestBody = requestBodyJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("LogManager", "syncLogs: onFailure", e)
                // Potentially load logs from file as a fallback
                // logScope.launch { file2List() } // Example: load from file on network failure
                // Post to main thread if UI needs to be updated about the failure
                // Handler(Looper.getMainLooper()).post { adapter.notifyDataSetChanged() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e("LogManager", "syncLogs: onResponse: not successful - Code: ${response.code}, Body: $errorBody")
                    // Potentially load logs from file as a fallback
                    // logScope.launch { file2List() } // Example: load from file on error response
                    // Post to main thread if UI needs to be updated
                    // Handler(Looper.getMainLooper()).post { adapter.notifyDataSetChanged() }
                    return
                }
                val body = response.body?.string()
                if (body == null) {
                    Log.e("LogManager", "syncLogs: onResponse: body is null")
                    return
                }

                try {
                    val logs = JSONArray(body)
                    val tempList = LinkedList<LogItem>() // Process into a temp list first
                    for (i in 0 until logs.length()) {
                        tempList.add(LogItem(logs.getJSONObject(i)))
                    }

                    logList.clear()
                    logList.addAll(tempList)

                    logScope.launch { // Launch a coroutine to call the suspend function
                        list2File()
                    }
                    // Ensure UI update on main thread
                    Handler(Looper.getMainLooper()).post {
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: org.json.JSONException) {
                    Log.e("LogManager", "syncLogs: Error parsing JSON response", e)
                    // Clear the list and notify the adapter on the main thread
                    logList.clear() // logList is now thread-safe
                    Handler(Looper.getMainLooper()).post {
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    private suspend fun list2File() {
        val logsJsonArray = JSONArray()
        logList.forEach { item -> // More Kotlin idiomatic way to iterate
            logsJsonArray.put(it.toJson())
        }
        val logJsonRoot = JSONObject()
        try {
            logJsonRoot.put("logs", logsJsonArray)
            logFile.writeText(logJsonRoot.toString())
        } catch (e: IOException) {
            Log.e("LogManager", "Error writing to log file", e)
        } catch (e: org.json.JSONException) {
            Log.e("LogManager", "Error creating JSON for list2File", e)
        }
    }

    @Suppress("unused") // Keep if it might be used later
    private suspend fun file2List() { // Should be called from a coroutine if used
        if (!logFile.exists()) {
            Log.w("LogManager", "file2List: Log file does not exist.")
            return
        }
        try {
            val fileContent = logFile.readText()
            if (fileContent.isEmpty()) {
                Log.w("LogManager", "file2List: Log file is empty.")
                logList.clear() // Ensure list is empty if file is empty
                return
            }
            val logJsonRoot = JSONObject(fileContent)
            if (!logJsonRoot.has("logs")) {
                Log.w("LogManager", "file2List: JSON has no 'logs' array.")
                logList.clear()
                return
            }
            val logsJsonArray = logJsonRoot.getJSONArray("logs")
            val tempList = LinkedList<LogItem>()
            for (i in 0 until logsJsonArray.length()) {
                tempList.add(LogItem(logsJsonArray.getJSONObject(i)))
            }
            logList.clear() // Clear existing logs before adding new ones
            logList.addAll(tempList)

        } catch (e: IOException) {
            Log.e("LogManager", "Error reading log file", e)
        } catch (e: org.json.JSONException) {
            Log.e("LogManager", "Error parsing log file JSON", e)
        }
    }
}
