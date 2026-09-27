package com.yaawaaa.penbatteryremapper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BatteryLog)

    @Query("SELECT * FROM battery_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<BatteryLog>>

    @Query("SELECT * FROM battery_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<BatteryLog>>

    @Query("SELECT * FROM battery_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLog(): BatteryLog?

    @Query("DELETE FROM battery_logs")
    suspend fun clearLogs()
}
