package com.yaawaaa.penbattchecker2.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yaawaaa.penbattchecker2.data.AppDatabase
import com.yaawaaa.penbattchecker2.data.BatteryRepository
import com.yaawaaa.penbattchecker2.notification.LowPowerNotificationManager
import java.io.File
import java.util.concurrent.TimeUnit

class BatteryPollingWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val file = File("/sys/class/power_supply/idt/reverse_pen_soc")
            if (file.exists() && file.canRead()) {
                val content = file.readText().trim()
                val level = content.toIntOrNull()
                if (level != null && level != 255) {
                    val isConnected = (level != 50)
                    val db = AppDatabase.getDatabase(applicationContext)
                    val repository = BatteryRepository(db.batteryLogDao())

                    // Log battery reading
                    repository.insertLog(level = level, isConnected = isConnected)

                    // Check low-power warnings
                    if (isConnected) {
                        val notificationManager = LowPowerNotificationManager(applicationContext)
                        notificationManager.checkAndNotifyLowPower(level)
                    }
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "BatteryPollingWorker"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<BatteryPollingWorker>(
                15, TimeUnit.MINUTES,
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest,
            )
        }
    }
}
