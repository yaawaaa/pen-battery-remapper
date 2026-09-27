package com.yaawaaa.penbattchecker2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_logs")
data class BatteryLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: Int,
    val isConnected: Boolean
)
