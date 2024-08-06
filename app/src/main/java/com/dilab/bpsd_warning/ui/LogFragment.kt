package com.dilab.bpsd_warning.ui

import android.content.BroadcastReceiver
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.devicelock.DeviceId
import android.net.Uri
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dilab.bpsd_warning.MainActivity
import java.io.File
import com.dilab.bpsd_warning.R
import com.dilab.bpsd_warning.log.LogAdapter
import com.dilab.bpsd_warning.log.LogItem
import com.dilab.bpsd_warning.log.LogManager

class LogFragment : Fragment() {

    private lateinit var logRecyclerView: RecyclerView
    private lateinit var clearButton: View
    private lateinit var syncButton: View
    private lateinit var goWebButton: Button
    private lateinit var logManager: LogManager
    private lateinit var selectedDevice: String

    companion object {
        const val ACTION_UPDATE_LOG = "com.dilab.bpsd_warning.UPDATE_LOG"
    }

    private val localBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(requireContext())
    }

    override fun onAttach(context: Context) {
        Log.d("LogFragment", "onAttach context: $context activity: $activity")
        super.onAttach(context)
        logManager = LogManager(context)
        val sharedPreferences = context.getSharedPreferences("device", Context.MODE_PRIVATE)
        selectedDevice = sharedPreferences.getString("device", "All") ?: "All"
        localBroadcastManager.registerReceiver(logUpdateReceiver, IntentFilter(ACTION_UPDATE_LOG))
    }

    private val logUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val title = intent?.getStringExtra("title")
            val message = intent?.getStringExtra("message")
            val time = intent?.getStringExtra("time")
            logManager.addLog(LogItem(title ?: "No title", message ?: "No message", time ?: "No time"))
            // scroll to top
            logRecyclerView.scrollToPosition(0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_log, container, false)

        logRecyclerView = view.findViewById(R.id.logRecyclerView)
        logRecyclerView.layoutManager = LinearLayoutManager(context)
        logRecyclerView.adapter = logManager.adapter

        // add dummy log button, just for testing
//        val dummyButton = view.findViewById<View>(R.id.dummyLogButton)
//        dummyButton.setOnClickListener {
//            logManager.addLog(LogItem("Dummy Log", "This is a dummy log"))
//        }

        clearButton = view.findViewById(R.id.clearLogButton)
        clearButton.setOnClickListener {
            logManager.clearLogs()
        }

        syncButton = view.findViewById(R.id.syncLogButton)
        syncButton.setOnClickListener {
            logManager.syncLogs()
        }

        // button for more detailed log on website
        val url = "http://140.113.193.87:20061/" + selectedDevice
        goWebButton = view.findViewById(R.id.goWebButton)
        goWebButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        return view
    }
}
