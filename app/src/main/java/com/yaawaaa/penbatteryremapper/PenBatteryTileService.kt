package com.yaawaaa.penbatteryremapper

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yaawaaa.penbatteryremapper.data.AppDatabase
import com.yaawaaa.penbatteryremapper.data.BatteryRepository
import com.yaawaaa.penbatteryremapper.remapper.RemapperRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PenBatteryTileService : TileService() {
    private val TAG = "PenBatteryTileService"
    private val BATTERY_FILE_PATH = "/sys/class/power_supply/idt/reverse_pen_soc"
    private val CHANNEL_ID = "PenBatteryChannel"

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileState()
        persistTileState(true)
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        persistTileState(false)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
        persistTileState(true)
    }

    private fun persistTileState(isAdded: Boolean) {
        val repo = RemapperRepository(applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            repo.setTileAdded(isAdded)
        }
    }

    override fun onClick() {
        super.onClick()

        // Start or ensure PenMonitorService is running in background to monitor pen connectivity
        try {
            val serviceIntent = Intent(applicationContext, PenMonitorService::class.java).apply {
                action = "ACTION_REFRESH"
            }
            ContextCompat.startForegroundService(applicationContext, serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting PenMonitorService from QS tile", e)
        }

        // Trigger immediate battery poll on Dispatchers.IO and update tile
        CoroutineScope(Dispatchers.IO).launch {
            val soc = readBatteryLevel()

            // Trigger poll/logging via BatteryRepository if valid reading
            if (soc != null && soc != 50 && soc != 255) {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val repository = BatteryRepository(db.batteryLogDao(), applicationContext)
                    repository.insertLog(level = soc, isConnected = true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting battery log on tile click", e)
                }
            }

            val tile = qsTile ?: return@launch
            tile.state = Tile.STATE_ACTIVE
            if (soc != null && soc != 50 && soc != 255) {
                tile.label = "Pen: $soc%"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "$soc%"
                }
            } else {
                tile.label = "Pen Off"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Disconnected"
                }
            }
            tile.updateTile()
        }

        postNotification()
    }

    fun updateTileState() {
        CoroutineScope(Dispatchers.IO).launch {
            val tile = qsTile ?: return@launch
            val soc = readBatteryLevel()

            if (soc != null && soc != 50 && soc != 255) {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Pen: $soc%"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "$soc%"
                }
            } else {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Pen Off"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Disconnected"
                }
            }
            tile.updateTile()
        }
    }

    private fun postNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val batteryLevel = readBatteryLevel()
        val contentText = if (batteryLevel != null && batteryLevel != 50 && batteryLevel != 255) {
            "Current pen battery is $batteryLevel%"
        } else {
            "Pen disconnected (Off)"
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pen Battery",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows current battery level for the pen"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pen Battery")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun readBatteryLevel(): Int? {
        return try {
            val file = File(BATTERY_FILE_PATH)
            if (file.exists() && file.canRead()) {
                val content = file.readText().trim()
                content.toIntOrNull()
            } else {
                Log.w(TAG, "Battery file does not exist or is not readable")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading battery level", e)
            null
        }
    }
}
