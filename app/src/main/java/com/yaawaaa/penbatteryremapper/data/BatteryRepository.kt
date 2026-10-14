package com.yaawaaa.penbatteryremapper.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryRepository(
    private val batteryLogDao: BatteryLogDao,
    context: Context? = null
) {
    private val prefs = context?.getSharedPreferences("battery_prefs", Context.MODE_PRIVATE)

    private val _isLoggingEnabled = MutableStateFlow(
        prefs?.getBoolean("is_logging_enabled", true) ?: true
    )
    val isLoggingEnabled: StateFlow<Boolean> = _isLoggingEnabled.asStateFlow()

    fun setLoggingEnabled(enabled: Boolean) {
        _isLoggingEnabled.value = enabled
        prefs?.edit()?.putBoolean("is_logging_enabled", enabled)?.apply()
    }

    val allLogs: Flow<List<BatteryLog>> = batteryLogDao.getAllLogs()

    fun getRecentLogs(limit: Int = 100): Flow<List<BatteryLog>> {
        return batteryLogDao.getRecentLogs(limit)
    }

    suspend fun insertLog(level: Int, isConnected: Boolean) {
        if (!_isLoggingEnabled.value) return
        val log = BatteryLog(level = level, isConnected = isConnected)
        batteryLogDao.insertLog(log)
        batteryLogDao.pruneOldLogs(300)
    }

    suspend fun insertLog(log: BatteryLog) {
        if (!_isLoggingEnabled.value) return
        batteryLogDao.insertLog(log)
        batteryLogDao.pruneOldLogs(300)
    }

    suspend fun getLatestLog(): BatteryLog? {
        return batteryLogDao.getLatestLog()
    }

    suspend fun clearLogs() {
        batteryLogDao.clearLogs()
    }
}
