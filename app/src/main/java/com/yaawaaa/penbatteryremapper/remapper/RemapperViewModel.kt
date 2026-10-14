package com.yaawaaa.penbatteryremapper.remapper

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yaawaaa.penbatteryremapper.service.PenRemapperAccessibilityService
import com.yaawaaa.penbatteryremapper.ui.ActionTargetSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemapperViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RemapperRepository(application)

    val settings: StateFlow<RemapperSettings> = repository.remapperSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RemapperSettings()
    )

    val appProfiles: StateFlow<List<AppProfile>> = repository.appProfiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isAccessibilityServiceEnabled = MutableStateFlow(false)
    val isAccessibilityServiceEnabled: StateFlow<Boolean> = _isAccessibilityServiceEnabled.asStateFlow()

    private val _isRootAvailable = MutableStateFlow(false)
    val isRootAvailable: StateFlow<Boolean> = _isRootAvailable.asStateFlow()

    private val _recentKeyEvent = MutableStateFlow<KeyEventInfo?>(null)
    val recentKeyEvent: StateFlow<KeyEventInfo?> = _recentKeyEvent.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    init {
        checkServiceAndRootState()
        loadInstalledApps()
        observeKeyEvents()
        observeSettingsForRootProtection()
    }

    private fun observeSettingsForRootProtection() {
        viewModelScope.launch(Dispatchers.IO) {
            settings.collect { currentSettings ->
                if (currentSettings.engineMode == EngineMode.ROOT && RootHelper.isRootAvailable()) {
                    val packageName = getApplication<Application>().packageName
                    RootHelper.enableAccessibilityServiceViaRoot(packageName)
                    RootHelper.protectProcessFromBackgroundKill(packageName)
                }
            }
        }
    }

    fun checkServiceAndRootState() {
        viewModelScope.launch(Dispatchers.IO) {
            val enabled = PenRemapperAccessibilityService.isServiceEnabled(getApplication())
            _isAccessibilityServiceEnabled.value = enabled

            val rootAvailable = RootHelper.isRootAvailable()
            _isRootAvailable.value = rootAvailable
        }
    }

    private fun observeKeyEvents() {
        viewModelScope.launch {
            PenRemapperAccessibilityService.keyEventsFlow.collect { eventInfo ->
                _recentKeyEvent.value = eventInfo
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context: Context = getApplication()
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            val apps = resolveInfos.map { resolveInfo ->
                val pkgName = resolveInfo.activityInfo.packageName
                val isSystem = try {
                    val appInfo = pm.getApplicationInfo(pkgName, 0)
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                } catch (e: Exception) {
                    false
                }
                AppInfo(
                    packageName = pkgName,
                    appName = resolveInfo.loadLabel(pm).toString(),
                    icon = null,
                    isSystemApp = isSystem
                )
            }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }

            _installedApps.value = apps
        }
    }

    fun addOrUpdateAppProfile(profile: AppProfile) {
        viewModelScope.launch {
            repository.addOrUpdateAppProfile(profile)
        }
    }

    fun deleteAppProfile(packageName: String) {
        viewModelScope.launch {
            repository.deleteAppProfile(packageName)
        }
    }

    fun restoreBackup(newSettings: RemapperSettings, newProfiles: List<AppProfile>) {
        viewModelScope.launch {
            repository.saveAllSettings(newSettings)
            repository.saveAppProfiles(newProfiles)
        }
    }

    fun toggleRemapper(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRemapperEnabled(enabled)
        }
    }

    fun setEngineMode(mode: EngineMode) {
        viewModelScope.launch {
            repository.setEngineMode(mode)
        }
    }

    fun setPrimaryClickAction(action: RemapAction) {
        viewModelScope.launch {
            repository.setPrimaryClickAction(action)
        }
    }

    fun setPrimaryClickAction2(action: RemapAction) {
        viewModelScope.launch {
            repository.setPrimaryClickAction2(action)
        }
    }

    fun setPrimaryClickAppPackage2(pkg: String) {
        viewModelScope.launch {
            repository.setPrimaryClickAppPackage2(pkg)
        }
    }

    fun setPrimaryDoubleClickAction(action: RemapAction) {
        viewModelScope.launch {
            repository.setPrimaryDoubleClickAction(action)
        }
    }

    fun setPrimaryDoubleClickAction2(action: RemapAction) {
        viewModelScope.launch {
            repository.setPrimaryDoubleClickAction2(action)
        }
    }

    fun setPrimaryDoubleClickAppPackage2(pkg: String) {
        viewModelScope.launch {
            repository.setPrimaryDoubleClickAppPackage2(pkg)
        }
    }

    fun setPrimaryTripleClickAction(action: RemapAction) {
        viewModelScope.launch {
            repository.setPrimaryTripleClickAction(action)
        }
    }

    fun setPrimaryTripleClickAction2(action: RemapAction) {
        viewModelScope.launch {
            repository.setPrimaryTripleClickAction2(action)
        }
    }

    fun setPrimaryTripleClickAppPackage2(pkg: String) {
        viewModelScope.launch {
            repository.setPrimaryTripleClickAppPackage2(pkg)
        }
    }

    fun setPrimaryLongPressAction(action: RemapAction) {
        viewModelScope.launch {
            repository.setPrimaryLongPressAction(action)
        }
    }

    fun setPrimaryLongPressAction2(action: RemapAction) {
        viewModelScope.launch {
            repository.setPrimaryLongPressAction2(action)
        }
    }

    fun setPrimaryLongPressAppPackage2(pkg: String) {
        viewModelScope.launch {
            repository.setPrimaryLongPressAppPackage2(pkg)
        }
    }

    fun setSecondaryClickAction(action: RemapAction) {
        viewModelScope.launch {
            repository.setSecondaryClickAction(action)
        }
    }

    fun setSecondaryClickAction2(action: RemapAction) {
        viewModelScope.launch {
            repository.setSecondaryClickAction2(action)
        }
    }

    fun setSecondaryClickAppPackage2(pkg: String) {
        viewModelScope.launch {
            repository.setSecondaryClickAppPackage2(pkg)
        }
    }

    fun setSecondaryDoubleClickAction(action: RemapAction) {
        viewModelScope.launch {
            repository.setSecondaryDoubleClickAction(action)
        }
    }

    fun setSecondaryDoubleClickAction2(action: RemapAction) {
        viewModelScope.launch {
            repository.setSecondaryDoubleClickAction2(action)
        }
    }

    fun setSecondaryDoubleClickAppPackage2(pkg: String) {
        viewModelScope.launch {
            repository.setSecondaryDoubleClickAppPackage2(pkg)
        }
    }

    fun setSecondaryTripleClickAction(action: RemapAction) {
        viewModelScope.launch {
            repository.setSecondaryTripleClickAction(action)
        }
    }

    fun setSecondaryTripleClickAction2(action: RemapAction) {
        viewModelScope.launch {
            repository.setSecondaryTripleClickAction2(action)
        }
    }

    fun setSecondaryTripleClickAppPackage2(pkg: String) {
        viewModelScope.launch {
            repository.setSecondaryTripleClickAppPackage2(pkg)
        }
    }

    fun setSecondaryLongPressAction(action: RemapAction) {
        viewModelScope.launch {
            repository.setSecondaryLongPressAction(action)
        }
    }

    fun setSecondaryLongPressAction2(action: RemapAction) {
        viewModelScope.launch {
            repository.setSecondaryLongPressAction2(action)
        }
    }
    fun setSecondaryLongPressAppPackage2(pkg: String) {
        viewModelScope.launch {
            repository.setSecondaryLongPressAppPackage2(pkg)
        }
    }

    fun setLongPressDurationMs(durationMs: Int) {
        viewModelScope.launch {
            repository.setLongPressDurationMs(durationMs)
        }
    }

    fun setDoubleClickTimeoutMs(timeoutMs: Int) {
        viewModelScope.launch {
            repository.setDoubleClickTimeoutMs(timeoutMs)
        }
    }

    fun setTripleClickTimeoutMs(timeoutMs: Int) {
        viewModelScope.launch {
            repository.setTripleClickTimeoutMs(timeoutMs)
        }
    }

    fun setUseCustomGestureTimeouts(useCustom: Boolean) {
        viewModelScope.launch {
            repository.setUseCustomGestureTimeouts(useCustom)
        }
    }

    fun setUseCustomGesturePositions(useCustom: Boolean) {
        viewModelScope.launch {
            repository.setUseCustomGesturePositions(useCustom)
        }
    }

    fun setSlotCustomPos(slot: ActionTargetSlot, enabled: Boolean) {
        viewModelScope.launch {
            repository.setSlotCustomPos(slot, enabled)
        }
    }

    fun setPositionPresetsJson(json: String) {
        viewModelScope.launch {
            repository.setPositionPresetsJson(json)
        }
    }

    fun setPrimaryDoubleClickTimeoutMs(timeoutMs: Int) {
        viewModelScope.launch {
            repository.setPrimaryDoubleClickTimeoutMs(timeoutMs)
        }
    }

    fun setPrimaryTripleClickTimeoutMs(timeoutMs: Int) {
        viewModelScope.launch {
            repository.setPrimaryTripleClickTimeoutMs(timeoutMs)
        }
    }

    fun setPrimaryLongPressDurationMs(durationMs: Int) {
        viewModelScope.launch {
            repository.setPrimaryLongPressDurationMs(durationMs)
        }
    }

    fun setSecondaryDoubleClickTimeoutMs(timeoutMs: Int) {
        viewModelScope.launch {
            repository.setSecondaryDoubleClickTimeoutMs(timeoutMs)
        }
    }

    fun setSecondaryTripleClickTimeoutMs(timeoutMs: Int) {
        viewModelScope.launch {
            repository.setSecondaryTripleClickTimeoutMs(timeoutMs)
        }
    }

    fun setSecondaryLongPressDurationMs(durationMs: Int) {
        viewModelScope.launch {
            repository.setSecondaryLongPressDurationMs(durationMs)
        }
    }

    fun setDeviceNameFilter(filter: String) {
        viewModelScope.launch {
            repository.setDeviceNameFilter(filter)
        }
    }

    fun setPrimaryClickShortcut(shortcut: String) {
        viewModelScope.launch {
            repository.setPrimaryClickShortcut(shortcut)
        }
    }

    fun setPrimaryDoubleClickShortcut(shortcut: String) {
        viewModelScope.launch {
            repository.setPrimaryDoubleClickShortcut(shortcut)
        }
    }

    fun setPrimaryTripleClickShortcut(shortcut: String) {
        viewModelScope.launch {
            repository.setPrimaryTripleClickShortcut(shortcut)
        }
    }

    fun setPrimaryLongPressShortcut(shortcut: String) {
        viewModelScope.launch {
            repository.setPrimaryLongPressShortcut(shortcut)
        }
    }

    fun setSecondaryClickShortcut(shortcut: String) {
        viewModelScope.launch {
            repository.setSecondaryClickShortcut(shortcut)
        }
    }

    fun setSecondaryDoubleClickShortcut(shortcut: String) {
        viewModelScope.launch {
            repository.setSecondaryDoubleClickShortcut(shortcut)
        }
    }

    fun setSecondaryTripleClickShortcut(shortcut: String) {
        viewModelScope.launch {
            repository.setSecondaryTripleClickShortcut(shortcut)
        }
    }

    fun setSecondaryLongPressShortcut(shortcut: String) {
        viewModelScope.launch {
            repository.setSecondaryLongPressShortcut(shortcut)
        }
    }

    fun applySettings() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = settings.value
            if (currentSettings.engineMode == EngineMode.ROOT && RootHelper.isRootAvailable()) {
                val packageName = getApplication<Application>().packageName
                RootHelper.enableAccessibilityServiceViaRoot(packageName)
                RootHelper.protectProcessFromBackgroundKill(packageName)
            }
            checkServiceAndRootState()
        }
    }

    fun enableViaRoot() {
        viewModelScope.launch(Dispatchers.IO) {
            val packageName = getApplication<Application>().packageName
            RootHelper.enableAccessibilityServiceViaRoot(packageName)
            RootHelper.protectProcessFromBackgroundKill(packageName)
            checkServiceAndRootState()
        }
    }

    fun simulateTestKeyPress(keyCode: Int, keyName: String, deviceName: String = "Xiaomi Smart Pen Gen 2") {
        val event = KeyEventInfo(keyCode = keyCode, keyName = keyName, deviceName = deviceName)
        PenRemapperAccessibilityService.emitTestKeyEvent(event)
    }

    fun clearRecentKeyEvent() {
        _recentKeyEvent.value = null
    }

    fun setAirCommandItemsJson(json: String) {
        viewModelScope.launch {
            repository.setAirCommandApps(json)
        }
    }

    fun setAirCommandScale(scale: Float) {
        viewModelScope.launch {
            repository.setAirCommandScale(scale)
        }
    }
}
