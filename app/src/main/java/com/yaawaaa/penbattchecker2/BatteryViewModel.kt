package com.yaawaaa.penbattchecker2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class BatteryState {
    object Loading : BatteryState()
    data class Success(val level: Int) : BatteryState()
    data class Error(val message: String) : BatteryState()
}

class BatteryViewModel : ViewModel() {
    private val batteryFilePath = "/sys/class/power_supply/idt/reverse_pen_soc"

    private val _batteryState = MutableStateFlow<BatteryState>(BatteryState.Loading)
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                readBattery()
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _batteryState.value = BatteryState.Loading
            readBattery()
        }
    }

    private fun readBattery() {
        try {
            val file = File(batteryFilePath)
            if (file.exists() && file.canRead()) {
                val content = file.readText().trim()
                val level = content.toIntOrNull()
                if (level != null) {
                    _batteryState.value = BatteryState.Success(level)
                } else {
                    _batteryState.value = BatteryState.Error("Invalid battery format: $content")
                }
            } else if (!file.exists()) {
                _batteryState.value = BatteryState.Error("File not found: $batteryFilePath")
            } else {
                _batteryState.value = BatteryState.Error("Permission denied: $batteryFilePath")
            }
        } catch (e: Exception) {
            _batteryState.value = BatteryState.Error(e.message ?: "Unknown error")
        }
    }
}
