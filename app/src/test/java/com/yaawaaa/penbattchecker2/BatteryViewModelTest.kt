package com.yaawaaa.penbattchecker2

import com.yaawaaa.penbattchecker2.data.BatteryLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryViewModelTest {

    @Test
    fun testChargingStatusDataClasses() {
        val supportedStatus = ChargingStatus.Supported(statusText = "Charging", isCharging = true)
        assertTrue(supportedStatus.isCharging)
        assertEquals("Charging", supportedStatus.statusText)

        val unsupportedStatus: ChargingStatus = ChargingStatus.Unsupported
        assertNotNull(unsupportedStatus)
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
}
