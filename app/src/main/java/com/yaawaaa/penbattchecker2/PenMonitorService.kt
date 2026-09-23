package com.yaawaaa.penbattchecker2

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class PenMonitorService : Service() {

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val STATE_NOTIFICATION_ID = 2
        private const val ACTION_REFRESH = "ACTION_REFRESH"
        private const val ACTION_DISMISS = "ACTION_DISMISS"
    }

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    // Assume disconnected initially, or we could determine initial state in onCreate.
    // To avoid false positive on first start, we'll initialize it to null
    private var isConnected: Boolean? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForegroundService()
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REFRESH -> {
                scope.launch {
                    checkState(forceUpdate = true)
                }
            }
            ACTION_DISMISS -> {
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(STATE_NOTIFICATION_ID)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "pen_monitor_channel")
            .setContentTitle("Pen Monitor Active")
            .setContentText("Monitoring pen connection state in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun startMonitoring() {
        scope.launch {
            while (true) {
                checkState(forceUpdate = false)
                delay(2000)
            }
        }
    }

    private fun checkState(forceUpdate: Boolean) {
        val file = File("/sys/class/power_supply/idt/reverse_pen_soc")
        var batteryLevel: Int? = null
        val currentlyConnected = try {
            if (file.exists() && file.canRead()) {
                val content = file.readText().trim()
                val value = content.toIntOrNull()
                if (value != null) {
                    if (value == 255) {
                        return // Ignore 255% spike
                    }
                    batteryLevel = value
                    value != 50
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }

        if (isConnected == null) {
            isConnected = currentlyConnected
            if (!forceUpdate) return
        }

        if (forceUpdate || currentlyConnected != isConnected) {
            isConnected = currentlyConnected
            if (currentlyConnected == true) {
                postAlertNotification(
                    title = "Xiaomi Pen Gen 2",
                    text = "Connected: ${batteryLevel ?: 0}% Battery",
                    isOngoing = true
                )
            } else {
                postAlertNotification(
                    title = "Xiaomi Pen Gen 2",
                    text = "Disconnected",
                    isOngoing = false
                )
            }
        }
    }

    private fun postAlertNotification(title: String, text: String, isOngoing: Boolean) {
        val refreshIntent = Intent(this, PenMonitorService::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getService(
            this, 0, refreshIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val dismissIntent = Intent(this, PenMonitorService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, 1, dismissIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "pen_alert_channel")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(Notification.DEFAULT_ALL)
            .setOngoing(isOngoing)
            .addAction(0, "Refresh", refreshPendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(STATE_NOTIFICATION_ID, notification)
    }

    private fun createChannels() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        val monitorChannel = NotificationChannel(
            "pen_monitor_channel",
            "Pen Monitor Service",
            NotificationManager.IMPORTANCE_MIN
        )
        manager.createNotificationChannel(monitorChannel)

        val alertChannel = NotificationChannel(
            "pen_alert_channel",
            "Pen Connection Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(alertChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}