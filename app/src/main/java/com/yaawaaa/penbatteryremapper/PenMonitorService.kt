package com.yaawaaa.penbatteryremapper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.yaawaaa.penbatteryremapper.data.AppDatabase
import com.yaawaaa.penbatteryremapper.data.BatteryRepository
import com.yaawaaa.penbatteryremapper.notification.LowPowerNotificationManager
import com.yaawaaa.penbatteryremapper.remapper.RootHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

class PenMonitorService : Service() {

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val STATE_NOTIFICATION_ID = 2
        private const val ACTION_REFRESH = "ACTION_REFRESH"
        private const val ACTION_DISMISS = "ACTION_DISMISS"
    }

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private var isConnected: Boolean? = null
    private var lastLoggedLevel: Int? = null
    private var lastLoggedConnected: Boolean? = null

    private lateinit var repository: BatteryRepository
    private lateinit var lowPowerNotificationManager: LowPowerNotificationManager

    private val sysfsFile = File("/sys/class/power_supply/idt/reverse_pen_soc")
    private val readBuffer = ByteArray(16)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        repository = BatteryRepository(db.batteryLogDao(), this)
        lowPowerNotificationManager = LowPowerNotificationManager(this)

        createChannels()
        startForegroundService()

        scope.launch {
            RootHelper.protectCurrentProcessFromLMK()
            RootHelper.protectProcessFromBackgroundKill(packageName)
        }

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
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "pen_monitor_channel")
            .setContentTitle("Pen Monitor Active")
            .setContentText("Monitoring pen connection state in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun startMonitoring() {
        scope.launch {
            val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
            while (true) {
                checkState(forceUpdate = false)

                val isInteractive = powerManager?.isInteractive ?: true
                val pollingIntervalMs = when {
                    !isInteractive -> 20000L // 20s when screen off
                    isConnected != true -> 8000L // 8s when disconnected
                    else -> 2000L // 2s when screen on and connected
                }
                delay(pollingIntervalMs)
            }
        }
    }

    private fun updateQuickSettingsTile() {
        try {
            TileService.requestListeningState(
                this,
                ComponentName(this, PenBatteryTileService::class.java)
            )
        } catch (e: Exception) {
            Log.e("PenMonitorService", "Failed to update Quick Settings tile", e)
        }
    }

    private fun readSysfsSoc(): Int? {
        if (!sysfsFile.exists() || !sysfsFile.canRead()) return null
        return try {
            FileInputStream(sysfsFile).use { fis ->
                val bytesRead = fis.read(readBuffer)
                if (bytesRead <= 0) return null
                var value = 0
                var hasDigits = false
                for (i in 0 until bytesRead) {
                    val b = readBuffer[i]
                    if (b in '0'.code.toByte()..'9'.code.toByte()) {
                        value = value * 10 + (b - '0'.code.toByte())
                        hasDigits = true
                    } else if (hasDigits) {
                        break
                    }
                }
                if (hasDigits) value else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun checkState(forceUpdate: Boolean) {
        var batteryLevel: Int? = null
        val rawValue = readSysfsSoc()
        val currentlyConnected = if (rawValue != null) {
            if (rawValue == 255) {
                return // Ignore 255% spike
            }
            batteryLevel = rawValue
            rawValue != 50
        } else {
            false
        }

        // Log battery level and check low power if reading valid
        if (batteryLevel != null) {
            val level = batteryLevel
            if (lastLoggedLevel != level || lastLoggedConnected != currentlyConnected) {
                lastLoggedLevel = level
                lastLoggedConnected = currentlyConnected
                scope.launch {
                    repository.insertLog(level = level, isConnected = currentlyConnected)
                }
                // Request real-time tile update when battery level or connection state changes
                updateQuickSettingsTile()
            }

            if (currentlyConnected) {
                lowPowerNotificationManager.checkAndNotifyLowPower(level)
            }
        }

        if (isConnected == null) {
            isConnected = currentlyConnected
            updateQuickSettingsTile()
            if (!forceUpdate) return
        }

        if (forceUpdate || currentlyConnected != isConnected) {
            isConnected = currentlyConnected
            updateQuickSettingsTile()
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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(monitorChannel)

        val alertChannel = NotificationChannel(
            "pen_alert_channel",
            "Pen Connection Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(alertChannel)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            scope.launch {
                System.gc()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
