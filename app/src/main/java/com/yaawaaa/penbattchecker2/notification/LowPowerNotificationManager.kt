package com.yaawaaa.penbattchecker2.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.yaawaaa.penbattchecker2.MainActivity
import com.yaawaaa.penbattchecker2.R

class LowPowerNotificationManager(private val context: Context) {

    companion object {
        const val LOW_POWER_CHANNEL_ID = "pen_low_power_channel"
        const val LOW_POWER_NOTIFICATION_ID = 1001

        @Volatile
        private var lastNotifiedLevel: Int? = null
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Pen Low Power Alerts"
        val descriptionText = "High-priority warnings when pen battery drops to 20% or 10%"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(LOW_POWER_CHANNEL_ID, name, importance).apply {
            description = descriptionText
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun checkAndNotifyLowPower(batteryLevel: Int) {
        val lastLevel = lastNotifiedLevel
        if (batteryLevel <= 10 && (lastLevel == null || lastLevel > 10)) {
            sendLowPowerNotification(
                title = "Pen Battery Critical (10%)",
                text = "Xiaomi Pen Gen 2 battery is critically low ($batteryLevel%). Please charge it now.",
                isCritical = true,
            )
            lastNotifiedLevel = 10
        } else if (batteryLevel in 11..20 && (lastLevel == null || lastLevel > 20)) {
            sendLowPowerNotification(
                title = "Pen Battery Low (20%)",
                text = "Xiaomi Pen Gen 2 battery is low ($batteryLevel%). Consider charging soon.",
                isCritical = false,
            )
            lastNotifiedLevel = 20
        } else if (batteryLevel > 20) {
            lastNotifiedLevel = null
        }
    }

    fun sendLowPowerNotification(title: String, text: String, isCritical: Boolean = false) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(context, LOW_POWER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(LOW_POWER_NOTIFICATION_ID, builder.build())
    }
}
