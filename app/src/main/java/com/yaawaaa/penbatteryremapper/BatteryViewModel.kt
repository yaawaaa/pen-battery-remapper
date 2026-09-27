package com.yaawaaa.penbatteryremapper

import android.app.ActivityManager
import android.app.Application
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yaawaaa.penbatteryremapper.data.AppDatabase
import com.yaawaaa.penbatteryremapper.data.BatteryLog
import com.yaawaaa.penbatteryremapper.data.BatteryRepository
import com.yaawaaa.penbatteryremapper.remapper.RemapperRepository
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
    private val remapperRepo = RemapperRepository(application)

    val isTileAdded: StateFlow<Boolean> = remapperRepo.isTileAdded.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _batteryState = MutableStateFlow<BatteryState>(BatteryState.Loading)
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    private val _chargingStatus = MutableStateFlow<ChargingStatus>(ChargingStatus.Disconnected)
    val chargingStatus: StateFlow<ChargingStatus> = _chargingStatus.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private var previousLevel: Int? = null
    private val _isActivelyCharging = MutableStateFlow(false)
    val isActivelyCharging: StateFlow<Boolean> = _isActivelyCharging.asStateFlow()

    val isLoggingEnabled: StateFlow<Boolean>
    val batteryLogs: StateFlow<List<BatteryLog>>

    init {
        val dao = AppDatabase.getDatabase(application).batteryLogDao()
        repository = BatteryRepository(dao, application)
        isLoggingEnabled = repository.isLoggingEnabled

        batteryLogs = repository.getRecentLogs(100).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        checkServiceState()
        startPolling()
    }

    fun toggleLogging(enabled: Boolean) {
        repository.setLoggingEnabled(enabled)
    }

    fun setTileAdded(added: Boolean) {
        viewModelScope.launch {
            remapperRepo.setTileAdded(added)
        }
    }

    fun requestAddTile(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                if (statusBarManager != null) {
                    val componentName = ComponentName(context, PenBatteryTileService::class.java)
                    val icon = Icon.createWithResource(context, R.drawable.ic_launcher_foreground)
                    statusBarManager.requestAddTileService(
                        componentName,
                        "Pen Battery",
                        icon,
                        context.mainExecutor
                    ) { resultCode ->
                        if (resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ||
                            resultCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED
                        ) {
                            setTileAdded(true)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(
                context,
                "To add Quick Settings Tile, swipe down Control Center, tap Edit, and drag 'Pen Battery' to your panel.",
                Toast.LENGTH_LONG
            ).show()
        }
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

                    // Active charging trend detection: connected AND currentLevel > previousLevel
                    val prev = previousLevel
                    _isActivelyCharging.value = isConnected && (prev != null) && (level > prev)
                    previousLevel = level

                    // Log to Room DB (if logging enabled)
                    repository.insertLog(level = level, isConnected = isConnected)

                    _chargingStatus.value = if (isConnected) {
                        ChargingStatus.DockedAndCharging(level)
                    } else {
                        ChargingStatus.Disconnected
                    }
                } else if (level == 255) {
                    // Ignore transient 255 spike
                } else {
                    _batteryState.value = BatteryState.Error("Invalid battery format: $content")
                    _chargingStatus.value = ChargingStatus.Disconnected
                    _isActivelyCharging.value = false
                }
            } else if (!file.exists()) {
                _batteryState.value = BatteryState.Error("File not found: $batteryFilePath")
                _chargingStatus.value = ChargingStatus.Disconnected
                _isActivelyCharging.value = false
            } else {
                _batteryState.value = BatteryState.Error("Permission denied: $batteryFilePath")
                _chargingStatus.value = ChargingStatus.Disconnected
                _isActivelyCharging.value = false
            }
        } catch (e: Exception) {
            _batteryState.value = BatteryState.Error(e.message ?: "Unknown error")
            _chargingStatus.value = ChargingStatus.Disconnected
            _isActivelyCharging.value = false
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
