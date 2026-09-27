package com.yaawaaa.penbatteryremapper.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AirCommandController {
    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    var isContinuousHold = false
        private set

    fun toggle() {
        isContinuousHold = false
        _isVisible.value = !_isVisible.value
    }

    fun startHold() {
        isContinuousHold = true
        _isVisible.value = true
    }

    fun stopHold() {
        if (isContinuousHold) {
            _isVisible.value = false
            isContinuousHold = false
        }
    }

    fun hide() {
        isContinuousHold = false
        _isVisible.value = false
    }

    fun show() {
        isContinuousHold = false
        _isVisible.value = true
    }
}
