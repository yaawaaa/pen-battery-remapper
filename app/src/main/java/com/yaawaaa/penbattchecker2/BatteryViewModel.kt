package com.yaawaaa.penbattchecker2

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yaawaaa.penbattchecker2.data.AppDatabase
import com.yaawaaa.penbattchecker2.data.BatteryLog
import com.yaawaaa.penbattchecker2.data.BatteryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class BatteryState {
    object Loading : BatteryState()
    data class Success(
        val level: Int,
        val isConnected: Boolean,
        val lastUpdated: Long = System.currentTimeMillis()
    ) : BatteryState()
    data class Error(val message: String) : BatteryState()
}

sealed class ChargingStatus {
    data class DockedAndCharging(val level: Int) : ChargingStatus()
    object Disconnected : ChargingStatus()
}

class BatteryViewModel(application: Application) : AndroidViewModel(application) {
    private val batteryFilePath = "/sys/class/power_supply/idt/reverse_pen_soc"
    private val repository: BatteryRepository

    private val _batteryState = MutableStateFlow<BatteryState>(BatteryState.Loading)
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    private val _chargingStatus = MutableStateFlow<ChargingStatus>(ChargingStatus.Disconnected)
    val chargingStatus: StateFlow<ChargingStatus> = _chargingStatus.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    val batteryLogs: StateFlow<List<BatteryLog>>

    init {
        val dao = AppDatabase.getDatabase(application).batteryLogDao()
        repository = BatteryRepository(dao)

        batteryLogs = repository.getRecentLogs(100).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        checkServiceState()
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                readBatteryAndCharging()
                checkServiceState()
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _batteryState.value = BatteryState.Loading
            readBatteryAndCharging()
            checkServiceState()
        }
    }

    fun toggleService(context: Context) {
        val intent = Intent(context, PenMonitorService::class.java)
        if (_isServiceRunning.value) {
            context.stopService(intent)
            _isServiceRunning.value = false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _isServiceRunning.value = true
        }
    }

    fun checkServiceState() {
        val context = getApplication<Application>()
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val running = manager?.getRunningServices(Int.MAX_VALUE)?.any {
            it.service.className == PenMonitorService::class.java.name
        } ?: false
        _isServiceRunning.value = running
    }

    private suspend fun readBatteryAndCharging() {
        // Read battery percentage from sysfs and determine connection / charging status
        try {
            val file = File(batteryFilePath)
            if (file.exists() && file.canRead()) {
                val content = file.readText().trim()
                val level = content.toIntOrNull()
                if (level != null && level != 255) {
                    val isConnected = (level != 50)
                    _batteryState.value = BatteryState.Success(
                        level = level,
                        isConnected = isConnected,
                        lastUpdated = System.currentTimeMillis()
                    )
                    // Log to Room DB
                    repository.insertLog(level = level, isConnected = isConnected)

                    _chargingStatus.value = if (level != 50) {
                        ChargingStatus.DockedAndCharging(level)
                    } else {
                        ChargingStatus.Disconnected
                    }
                } else if (level == 255) {
                    // Ignore transient 255 spike
                } else {
                    _batteryState.value = BatteryState.Error("Invalid battery format: $content")
                    _chargingStatus.value = ChargingStatus.Disconnected
                }
            } else if (!file.exists()) {
                _batteryState.value = BatteryState.Error("File not found: $batteryFilePath")
                _chargingStatus.value = ChargingStatus.Disconnected
            } else {
                _batteryState.value = BatteryState.Error("Permission denied: $batteryFilePath")
                _chargingStatus.value = ChargingStatus.Disconnected
            }
        } catch (e: Exception) {
            _batteryState.value = BatteryState.Error(e.message ?: "Unknown error")
            _chargingStatus.value = ChargingStatus.Disconnected
        }
    }

    fun addSampleLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val hourMs = 3600_000L
            val sampleData = listOf(
                100 to (now - 7 * hourMs),
                92 to (now - 6 * hourMs),
                84 to (now - 5 * hourMs),
                70 to (now - 4 * hourMs),
                58 to (now - 3 * hourMs),
                42 to (now - 2 * hourMs),
                28 to (now - 1 * hourMs),
                15 to now
            )
            for ((level, timestamp) in sampleData) {
                repository.insertLog(BatteryLog(timestamp = timestamp, level = level, isConnected = true))
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
        }
    }
}
