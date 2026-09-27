package com.yaawaaa.penbattchecker2

import com.yaawaaa.penbattchecker2.data.BatteryLog
import com.yaawaaa.penbattchecker2.data.BatteryLogDao
import com.yaawaaa.penbattchecker2.data.BatteryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryViewModelTest {

    private class FakeBatteryLogDao : BatteryLogDao {
        val logs = mutableListOf<BatteryLog>()

        override suspend fun insertLog(log: BatteryLog) {
            logs.add(log)
        }

        override fun getAllLogs(): Flow<List<BatteryLog>> = flowOf(logs)

        override fun getRecentLogs(limit: Int): Flow<List<BatteryLog>> = flowOf(logs.take(limit))

        override suspend fun getLatestLog(): BatteryLog? = logs.lastOrNull()

        override suspend fun clearLogs() {
            logs.clear()
        }
    }

    @Test
    fun testChargingStatusDataClasses() {
        val dockedStatus = ChargingStatus.DockedAndCharging(level = 85)
        assertEquals(85, dockedStatus.level)

        val disconnectedStatus: ChargingStatus = ChargingStatus.Disconnected
        assertNotNull(disconnectedStatus)
    }

    @Test
    fun testBatteryStateSuccess() {
        val successState = BatteryState.Success(level = 85, isConnected = true)
        assertEquals(85, successState.level)
        assertTrue(successState.isConnected)
    }

    @Test
    fun testBatteryLogData() {
        val now = System.currentTimeMillis()
        val log = BatteryLog(id = 1, timestamp = now, level = 75, isConnected = true)
        assertEquals(1L, log.id)
        assertEquals(now, log.timestamp)
        assertEquals(75, log.level)
        assertTrue(log.isConnected)
    }

    @Test
    fun testActiveChargingTrendCondition() {
        fun checkCharging(connected: Boolean, prev: Int?, current: Int): Boolean {
            return connected && (prev != null) && (current > prev)
        }

        assertTrue(checkCharging(connected = true, prev = 70, current = 75))
        assertFalse(checkCharging(connected = true, prev = 70, current = 65))
        assertFalse(checkCharging(connected = false, prev = 70, current = 75))
        assertFalse(checkCharging(connected = true, prev = null, current = 75))
    }

    @Test
    fun testRepositoryLoggingToggleAndClear() = runBlocking {
        val fakeDao = FakeBatteryLogDao()
        val repository = BatteryRepository(fakeDao, context = null)

        // Default logging enabled
        assertTrue(repository.isLoggingEnabled.value)

        repository.insertLog(level = 80, isConnected = true)
        assertEquals(1, fakeDao.logs.size)

        // Disable logging
        repository.setLoggingEnabled(false)
        assertFalse(repository.isLoggingEnabled.value)

        // Attempt insert while logging disabled -> should not insert new logs
        repository.insertLog(level = 85, isConnected = true)
        assertEquals(1, fakeDao.logs.size) // Existing log remains saved

        // Clear logs
        repository.clearLogs()
        assertEquals(0, fakeDao.logs.size)
    }
}
