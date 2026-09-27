package com.yaawaaa.penbatteryremapper

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
        updateTileState()
        postNotification()
    }

    fun updateTileState() {
        val tile = qsTile ?: return
        val soc = readBatteryLevel()

        if (soc != null && soc != 50 && soc != 255) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Pen $soc%"
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
