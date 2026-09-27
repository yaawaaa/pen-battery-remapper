package com.yaawaaa.penbattchecker2.data

import kotlinx.coroutines.flow.Flow

class BatteryRepository(private val batteryLogDao: BatteryLogDao) {

    val allLogs: Flow<List<BatteryLog>> = batteryLogDao.getAllLogs()

    fun getRecentLogs(limit: Int = 100): Flow<List<BatteryLog>> {
        return batteryLogDao.getRecentLogs(limit)
    }

    suspend fun insertLog(level: Int, isConnected: Boolean) {
        val log = BatteryLog(level = level, isConnected = isConnected)
        batteryLogDao.insertLog(log)
    }

    suspend fun insertLog(log: BatteryLog) {
        batteryLogDao.insertLog(log)
    }

    suspend fun getLatestLog(): BatteryLog? {
        return batteryLogDao.getLatestLog()
    }

    suspend fun clearLogs() {
        batteryLogDao.clearLogs()
    }
}
