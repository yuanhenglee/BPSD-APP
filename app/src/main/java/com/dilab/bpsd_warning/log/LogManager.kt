package com.dilab.bpsd_warning.log

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.LinkedList
import java.io.IOException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class LogManager(private val context: Context) {
    private var logList = LinkedList<LogItem>()
    private var logFile: File

    private val url = "http://140.113.193.87:20061/get_warning"
    private val topic = context.getSharedPreferences("device", Context.MODE_PRIVATE).getString("device", "All")

    var adapter: LogAdapter

    init {
        val logDir: File = File(context.filesDir, "log")
        if (!logDir.exists()) {
            Log.d("LogManager", "onAttach: create log dir")
            logDir.mkdir()
        }
        logFile = File(logDir, "log.json")
        if (!logFile.exists()) {
            Log.d("LogManager", "create log file")
            logFile.createNewFile()
        }
        adapter = LogAdapter(logList)
        syncLogs()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addLog(logItem: LogItem) {
        logList.addFirst(logItem)
        list2File()
        adapter.notifyItemInserted(0)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearLogs() {
        logList.clear()
        list2File()
        adapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun syncLogs() {
        // TODO: sync logs from server, temporarily just read from file
        val client = OkHttpClient()
        val requestBody =
            "{\"topic\": \"$topic\"}".toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("LogManager", "syncLogs: onFailure")
                Log.d("LogManager", "syncLogs: onFailure: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.d("LogManager", "syncLogs: onResponse: not successful ${response.body?.string()}")
                    return
                }
                val body = response.body?.string()
                val logs = JSONArray(body)
                logList.clear()
                logList.addAll(
                    (0 until logs.length()).map { i ->
                        LogItem(logs.getJSONObject(i))
                    }
                )
                list2File()
                // Ensure UI update on main thread
                Handler(Looper.getMainLooper()).post {
                    adapter.notifyDataSetChanged()
                }

//                adapter.notifyDataSetChanged()
            }
        })
    }

    private fun list2File() {
        val logs = JSONArray(
            logList.map { it.toJson() }
        )
        val logJson = JSONObject()
        logJson.put("logs", logs)
        logFile.writeText(logJson.toString())
    }

    private fun file2List() {
        logList.clear()
        logFile.readText().let {
            val logJson = JSONObject(it)
            val logs = logJson.getJSONArray("logs")
            logList.addAll(
                (0 until logs.length()).map { i ->
                    LogItem(logs.getJSONObject(i))
                }
            )
        }
    }
}