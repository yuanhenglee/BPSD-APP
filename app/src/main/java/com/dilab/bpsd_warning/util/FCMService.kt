package com.dilab.bpsd_warning.util

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.dilab.bpsd_warning.R
import com.dilab.bpsd_warning.log.LogItem
import com.dilab.bpsd_warning.log.LogManager
import com.dilab.bpsd_warning.ui.LogFragment
import com.dilab.bpsd_warning.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    private lateinit var logManager: LogManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "onNewToken: $token")
    }

    override fun onCreate() {
        super.onCreate()
        logManager = LogManager(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCMService", "onMessageReceived: ${remoteMessage.notification} From: ${remoteMessage.from}")
//        remoteMessage.notification?.let {
//            // show notification
//            showNotification(it.title, it.body)
//            // add log
//            logManager.addLog(LogItem(it.title!!, it.body!!))
//        }
        remoteMessage.data.let {
            // show notification
            showNotification(it["title"], it["message"])
            // add log
//            logManager.addLog(LogItem(it["title"]!!, it["message"]!!))
            val intent = Intent(LogFragment.ACTION_UPDATE_LOG)
            intent.putExtra("title", it["title"])
            intent.putExtra("message", it["message"])
            intent.putExtra("time", it["time"])
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    private fun showNotification(title: String?, message: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openLogFragment", true)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, "BPSD")
            .setSmallIcon(R.drawable.ic_logo_white)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@FCMService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("FCMService", "showNotification: no permission")
                return@with
            }
            notify(0, builder.build())
        }
        Log.d("FCMService", "showNotification: $title, $message")
    }

}
