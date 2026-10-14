package com.yaawaaa.penbatteryremapper.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import java.util.concurrent.ConcurrentHashMap
import com.yaawaaa.penbatteryremapper.remapper.ActionExecutor
import com.yaawaaa.penbatteryremapper.remapper.AppIconCache
import com.yaawaaa.penbatteryremapper.remapper.AppProfile
import com.yaawaaa.penbatteryremapper.remapper.EngineMode
import com.yaawaaa.penbatteryremapper.remapper.KeyEventInfo
import com.yaawaaa.penbatteryremapper.remapper.RemapAction
import com.yaawaaa.penbatteryremapper.remapper.RemapperBackupManager
import com.yaawaaa.penbatteryremapper.remapper.RemapperRepository
import com.yaawaaa.penbatteryremapper.remapper.RemapperSettings
import com.yaawaaa.penbatteryremapper.remapper.RootHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class ResolvedGestureAction(
    val action1: RemapAction = RemapAction.PASSTHROUGH,
    val payload1: String = "",
    val useCustomPos1: Boolean = true,
    val action2: RemapAction = RemapAction.PASSTHROUGH,
    val payload2: String = "",
    val useCustomPos2: Boolean = true
)

class PenRemapperAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repository: RemapperRepository
    private var currentSettings = RemapperSettings()
    private var lmkRefreshJob: Job? = null

    private var isPrimaryPressed = false
    private var isSecondaryPressed = false

    private var primaryClickCount = 0
    private var primaryClickRunnable: Runnable? = null
    private var primaryLongPressRunnable: Runnable? = null
    private var isPrimaryLongPressTriggered = false

    private var secondaryClickCount = 0
    private var secondaryClickRunnable: Runnable? = null
    private var secondaryLongPressRunnable: Runnable? = null
    private var isSecondaryLongPressTriggered = false

    private val handler = Handler(Looper.getMainLooper())

    private data class ActionTuple(
        val action1: RemapAction = RemapAction.PASSTHROUGH,
        val payload1: String = "",
        val useCustomPos1: Boolean = true,
        val action2: RemapAction = RemapAction.PASSTHROUGH,
        val payload2: String = "",
        val useCustomPos2: Boolean = true
    )

    companion object {
        var activeForegroundPackage: String = ""
            internal set

        @Volatile
        var currentAppProfiles: List<AppProfile> = emptyList()
            internal set

        val windowPackageMap = ConcurrentHashMap<Int, String>()

        fun loadFastCacheFromSharedPreferences(context: Context): Pair<RemapperSettings, List<AppProfile>> {
            try {
                val prefs = context.getSharedPreferences("remapper_fast_cache", MODE_PRIVATE)
                val enabled = prefs.getBoolean("remapper_enabled", false)
                val json = prefs.getString("app_profiles_json", "") ?: ""
                val profiles = RemapperBackupManager.parseAppProfilesJson(json)

                val engineModeStr = prefs.getString("engine_mode", EngineMode.ACCESSIBILITY.name)
                val primaryClickActionStr = prefs.getString("primary_click_action", RemapAction.PASSTHROUGH.name)

                val settings = RemapperSettings(
                    remapperEnabled = enabled,
                    engineMode = EngineMode.fromNameOrDefault(engineModeStr, EngineMode.ACCESSIBILITY),
                    primaryClickAction = RemapAction.fromNameOrDefault(primaryClickActionStr, RemapAction.PASSTHROUGH),
                    primaryClickShortcut = prefs.getString("primary_click_shortcut", "") ?: "",
                    primaryDoubleClickAction = RemapAction.fromNameOrDefault(prefs.getString("primary_double_click_action", null), RemapAction.PASSTHROUGH),
                    primaryDoubleClickShortcut = prefs.getString("primary_double_click_shortcut", "") ?: "",
                    primaryTripleClickAction = RemapAction.fromNameOrDefault(prefs.getString("primary_triple_click_action", null), RemapAction.PASSTHROUGH),
                    primaryTripleClickShortcut = prefs.getString("primary_triple_click_shortcut", "") ?: "",
                    primaryLongPressAction = RemapAction.fromNameOrDefault(prefs.getString("primary_long_press_action", null), RemapAction.PASSTHROUGH),
                    primaryLongPressShortcut = prefs.getString("primary_long_press_shortcut", "") ?: "",
                    primaryClickAction2 = RemapAction.fromNameOrDefault(prefs.getString("primary_click_action_2", null), RemapAction.PASSTHROUGH),
                    primaryClickAppPackage2 = prefs.getString("primary_click_app_package_2", "") ?: "",
                    primaryDoubleClickAction2 = RemapAction.fromNameOrDefault(prefs.getString("primary_double_click_action_2", null), RemapAction.PASSTHROUGH),
                    primaryDoubleClickAppPackage2 = prefs.getString("primary_double_click_app_package_2", "") ?: "",
                    primaryTripleClickAction2 = RemapAction.fromNameOrDefault(prefs.getString("primary_triple_click_action_2", null), RemapAction.PASSTHROUGH),
                    primaryTripleClickAppPackage2 = prefs.getString("primary_triple_click_app_package_2", "") ?: "",
                    primaryLongPressAction2 = RemapAction.fromNameOrDefault(prefs.getString("primary_long_press_action_2", null), RemapAction.PASSTHROUGH),
                    primaryLongPressAppPackage2 = prefs.getString("primary_long_press_app_package_2", "") ?: "",
                    secondaryClickAction = RemapAction.fromNameOrDefault(prefs.getString("secondary_click_action", null), RemapAction.PASSTHROUGH),
                    secondaryClickShortcut = prefs.getString("secondary_click_shortcut", "") ?: "",
                    secondaryDoubleClickAction = RemapAction.fromNameOrDefault(prefs.getString("secondary_double_click_action", null), RemapAction.PASSTHROUGH),
                    secondaryDoubleClickShortcut = prefs.getString("secondary_double_click_shortcut", "") ?: "",
                    secondaryTripleClickAction = RemapAction.fromNameOrDefault(prefs.getString("secondary_triple_click_action", null), RemapAction.PASSTHROUGH),
                    secondaryTripleClickShortcut = prefs.getString("secondary_triple_click_shortcut", "") ?: "",
                    secondaryLongPressAction = RemapAction.fromNameOrDefault(prefs.getString("secondary_long_press_action", null), RemapAction.PASSTHROUGH),
                    secondaryLongPressShortcut = prefs.getString("secondary_long_press_shortcut", "") ?: "",
                    secondaryClickAction2 = RemapAction.fromNameOrDefault(prefs.getString("secondary_click_action_2", null), RemapAction.PASSTHROUGH),
                    secondaryClickAppPackage2 = prefs.getString("secondary_click_app_package_2", "") ?: "",
                    secondaryDoubleClickAction2 = RemapAction.fromNameOrDefault(prefs.getString("secondary_double_click_action_2", null), RemapAction.PASSTHROUGH),
                    secondaryDoubleClickAppPackage2 = prefs.getString("secondary_double_click_app_package_2", "") ?: "",
                    secondaryTripleClickAction2 = RemapAction.fromNameOrDefault(prefs.getString("secondary_triple_click_action_2", null), RemapAction.PASSTHROUGH),
                    secondaryTripleClickAppPackage2 = prefs.getString("secondary_triple_click_app_package_2", "") ?: "",
                    secondaryLongPressAction2 = RemapAction.fromNameOrDefault(prefs.getString("secondary_long_press_action_2", null), RemapAction.PASSTHROUGH),
                    secondaryLongPressAppPackage2 = prefs.getString("secondary_long_press_app_package_2", "") ?: "",
                    longPressDurationMs = prefs.getInt("long_press_duration_ms", 500),
                    doubleClickTimeoutMs = prefs.getInt("double_click_timeout_ms", 300),
                    tripleClickTimeoutMs = prefs.getInt("triple_click_timeout_ms", 350),
                    useCustomGestureTimeouts = prefs.getBoolean("use_custom_gesture_timeouts", false),
                    useCustomGesturePositions = prefs.getBoolean("use_custom_gesture_positions", false),
                    primaryClickCustomPos1 = prefs.getBoolean("primary_click_custom_pos_1", false),
                    primaryClickCustomPos2 = prefs.getBoolean("primary_click_custom_pos_2", false),
                    primaryDoubleClickCustomPos1 = prefs.getBoolean("primary_double_click_custom_pos_1", false),
                    primaryDoubleClickCustomPos2 = prefs.getBoolean("primary_double_click_custom_pos_2", false),
                    primaryTripleClickCustomPos1 = prefs.getBoolean("primary_triple_click_custom_pos_1", false),
                    primaryTripleClickCustomPos2 = prefs.getBoolean("primary_triple_click_custom_pos_2", false),
                    primaryLongPressCustomPos1 = prefs.getBoolean("primary_long_press_custom_pos_1", false),
                    primaryLongPressCustomPos2 = prefs.getBoolean("primary_long_press_custom_pos_2", false),
                    secondaryClickCustomPos1 = prefs.getBoolean("secondary_click_custom_pos_1", false),
                    secondaryClickCustomPos2 = prefs.getBoolean("secondary_click_custom_pos_2", false),
                    secondaryDoubleClickCustomPos1 = prefs.getBoolean("secondary_double_click_custom_pos_1", false),
                    secondaryDoubleClickCustomPos2 = prefs.getBoolean("secondary_double_click_custom_pos_2", false),
                    secondaryTripleClickCustomPos1 = prefs.getBoolean("secondary_triple_click_custom_pos_1", false),
                    secondaryTripleClickCustomPos2 = prefs.getBoolean("secondary_triple_click_custom_pos_2", false),
                    secondaryLongPressCustomPos1 = prefs.getBoolean("secondary_long_press_custom_pos_1", false),
                    secondaryLongPressCustomPos2 = prefs.getBoolean("secondary_long_press_custom_pos_2", false),
                    primaryDoubleClickTimeoutMs = prefs.getInt("primary_double_click_timeout", 300),
                    primaryTripleClickTimeoutMs = prefs.getInt("primary_triple_click_timeout", 350),
                    primaryLongPressDurationMs = prefs.getInt("primary_long_press_duration", 500),
                    secondaryDoubleClickTimeoutMs = prefs.getInt("secondary_double_click_timeout", 300),
                    secondaryTripleClickTimeoutMs = prefs.getInt("secondary_triple_click_timeout", 350),
                    secondaryLongPressDurationMs = prefs.getInt("secondary_long_press_duration", 500),
                    primaryAppPackage = prefs.getString("primary_app_package", "") ?: "",
                    secondaryAppPackage = prefs.getString("secondary_app_package", "") ?: "",
                    deviceNameFilter = prefs.getString("device_name_filter", "Pen") ?: "Pen",
                    airCommandEnabled = prefs.getBoolean("air_command_enabled", false),
                    showEdgeHandle = prefs.getBoolean("show_edge_handle", true),
                    airCommandScale = prefs.getFloat("air_command_scale", 1.0f),
                    airCommandOpacity = prefs.getFloat("air_command_opacity", 1.0f),
                    airCommandApps = prefs.getString("air_command_apps", "") ?: "",
                    positionPresetsJson = prefs.getString("position_presets_json", "") ?: ""
                )
                return Pair(settings, profiles)
            } catch (e: Exception) {
                e.printStackTrace()
                return Pair(RemapperSettings(), emptyList())
            }
        }

        fun updateAppProfileInMemory(profile: AppProfile) {
            val list = currentAppProfiles.toMutableList()
            val index = list.indexOfFirst { it.packageName.equals(profile.packageName, ignoreCase = true) }
            if (index >= 0) {
                list[index] = profile
            } else {
                list.add(profile)
            }
            currentAppProfiles = list
        }

        fun isValidForegroundPackage(pkg: String?, contextPackageName: String? = null): Boolean {
            if (pkg.isNullOrBlank()) return false
            if (!contextPackageName.isNullOrBlank() && pkg.equals(contextPackageName, ignoreCase = true)) return false
            if (pkg.equals("android", ignoreCase = true)) return false

            if (currentAppProfiles.any { it.packageName.equals(pkg, ignoreCase = true) }) return true

            val lower = pkg.lowercase()
            val blacklist = listOf(
                "systemui",
                "notification",
                "inputmethod",
                "toast",
                "overlay",
                "island",
                "capsule",
                "pip",
                "freeform"
            )
            for (keyword in blacklist) {
                if (lower.contains(keyword)) return false
            }
            return true
        }

        private val _keyEventsFlow = MutableSharedFlow<KeyEventInfo>(
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val keyEventsFlow: SharedFlow<KeyEventInfo> = _keyEventsFlow.asSharedFlow()

        var isInstanceRunning: Boolean = false
            private set

        fun isServiceEnabled(context: Context): Boolean {
            val expectedService = "${context.packageName}/${PenRemapperAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedService, ignoreCase = true) ||
                    componentName.equals("${context.packageName}/.service.PenRemapperAccessibilityService", ignoreCase = true)
                ) {
                    return true
                }
            }
            return false
        }

        fun emitTestKeyEvent(event: KeyEventInfo) {
            _keyEventsFlow.tryEmit(event)
        }

        fun resolveGestureAction(
            count: Int,
            isLongPress: Boolean,
            isPrimary: Boolean,
            appProfiles: List<AppProfile>,
            settings: RemapperSettings,
            foregroundPackage: String
        ): ResolvedGestureAction {
            val profile = appProfiles.find { it.packageName.equals(foregroundPackage, ignoreCase = true) }
            val s = settings

            val pTuple = if (profile != null) {
                if (isPrimary) {
                    if (isLongPress) {
                        ActionTuple(
                            profile.primaryLongPressAction,
                            profile.primaryLongPressShortcut.ifBlank { profile.primaryAppPackage },
                            profile.primaryLongPressCustomPos1,
                            profile.primaryLongPressAction2,
                            profile.primaryLongPressAppPackage2,
                            profile.primaryLongPressCustomPos2
                        )
                    } else if (count <= 1) {
                        ActionTuple(
                            profile.primaryClickAction,
                            profile.primaryClickShortcut.ifBlank { profile.primaryAppPackage },
                            profile.primaryClickCustomPos1,
                            profile.primaryClickAction2,
                            profile.primaryClickAppPackage2,
                            profile.primaryClickCustomPos2
                        )
                    } else if (count == 2) {
                        ActionTuple(
                            profile.primaryDoubleClickAction,
                            profile.primaryDoubleClickShortcut.ifBlank { profile.primaryAppPackage },
                            profile.primaryDoubleClickCustomPos1,
                            profile.primaryDoubleClickAction2,
                            profile.primaryDoubleClickAppPackage2,
                            profile.primaryDoubleClickCustomPos2
                        )
                    } else {
                        ActionTuple(
                            profile.primaryTripleClickAction,
                            profile.primaryTripleClickShortcut.ifBlank { profile.primaryAppPackage },
                            profile.primaryTripleClickCustomPos1,
                            profile.primaryTripleClickAction2,
                            profile.primaryTripleClickAppPackage2,
                            profile.primaryTripleClickCustomPos2
                        )
                    }
                } else {
                    if (isLongPress) {
                        ActionTuple(
                            profile.secondaryLongPressAction,
                            profile.secondaryLongPressShortcut.ifBlank { profile.secondaryAppPackage },
                            profile.secondaryLongPressCustomPos1,
                            profile.secondaryLongPressAction2,
                            profile.secondaryLongPressAppPackage2,
                            profile.secondaryLongPressCustomPos2
                        )
                    } else if (count <= 1) {
                        ActionTuple(
                            profile.secondaryClickAction,
                            profile.secondaryClickShortcut.ifBlank { profile.secondaryAppPackage },
                            profile.secondaryClickCustomPos1,
                            profile.secondaryClickAction2,
                            profile.secondaryClickAppPackage2,
                            profile.secondaryClickCustomPos2
                        )
                    } else if (count == 2) {
                        ActionTuple(
                            profile.secondaryDoubleClickAction,
                            profile.secondaryDoubleClickShortcut.ifBlank { profile.secondaryAppPackage },
                            profile.secondaryDoubleClickCustomPos1,
                            profile.secondaryDoubleClickAction2,
                            profile.secondaryDoubleClickAppPackage2,
                            profile.secondaryDoubleClickCustomPos2
                        )
                    } else {
                        ActionTuple(
                            profile.secondaryTripleClickAction,
                            profile.secondaryTripleClickShortcut.ifBlank { profile.secondaryAppPackage },
                            profile.secondaryTripleClickCustomPos1,
                            profile.secondaryTripleClickAction2,
                            profile.secondaryTripleClickAppPackage2,
                            profile.secondaryTripleClickCustomPos2
                        )
                    }
                }
            } else ActionTuple(RemapAction.PASSTHROUGH, "", true, RemapAction.PASSTHROUGH, "", true)

            val sTuple = if (isPrimary) {
                if (isLongPress) {
                    ActionTuple(
                        s.primaryLongPressAction,
                        s.primaryLongPressShortcut.ifBlank { s.primaryAppPackage },
                        s.primaryLongPressCustomPos1,
                        s.primaryLongPressAction2,
                        s.primaryLongPressAppPackage2,
                        s.primaryLongPressCustomPos2
                    )
                } else if (count <= 1) {
                    ActionTuple(
                        s.primaryClickAction,
                        s.primaryClickShortcut.ifBlank { s.primaryAppPackage },
                        s.primaryClickCustomPos1,
                        s.primaryClickAction2,
                        s.primaryClickAppPackage2,
                        s.primaryClickCustomPos2
                    )
                } else if (count == 2) {
                    ActionTuple(
                        s.primaryDoubleClickAction,
                        s.primaryDoubleClickShortcut.ifBlank { s.primaryAppPackage },
                        s.primaryDoubleClickCustomPos1,
                        s.primaryDoubleClickAction2,
                        s.primaryDoubleClickAppPackage2,
                        s.primaryDoubleClickCustomPos2
                    )
                } else {
                    ActionTuple(
                        s.primaryTripleClickAction,
                        s.primaryTripleClickShortcut.ifBlank { s.primaryAppPackage },
                        s.primaryTripleClickCustomPos1,
                        s.primaryTripleClickAction2,
                        s.primaryTripleClickAppPackage2,
                        s.primaryTripleClickCustomPos2
                    )
                }
            } else {
                if (isLongPress) {
                    ActionTuple(
                        s.secondaryLongPressAction,
                        s.secondaryLongPressShortcut.ifBlank { s.secondaryAppPackage },
                        s.secondaryLongPressCustomPos1,
                        s.secondaryLongPressAction2,
                        s.secondaryLongPressAppPackage2,
                        s.secondaryLongPressCustomPos2
                    )
                } else if (count <= 1) {
                    ActionTuple(
                        s.secondaryClickAction,
                        s.secondaryClickShortcut.ifBlank { s.secondaryAppPackage },
                        s.secondaryClickCustomPos1,
                        s.secondaryClickAction2,
                        s.secondaryClickAppPackage2,
                        s.secondaryClickCustomPos2
                    )
                } else if (count == 2) {
                    ActionTuple(
                        s.secondaryDoubleClickAction,
                        s.secondaryDoubleClickShortcut.ifBlank { s.secondaryAppPackage },
                        s.secondaryDoubleClickCustomPos1,
                        s.secondaryDoubleClickAction2,
                        s.secondaryDoubleClickAppPackage2,
                        s.secondaryDoubleClickCustomPos2
                    )
                } else {
                    ActionTuple(
                        s.secondaryTripleClickAction,
                        s.secondaryTripleClickShortcut.ifBlank { s.secondaryAppPackage },
                        s.secondaryTripleClickCustomPos1,
                        s.secondaryTripleClickAction2,
                        s.secondaryTripleClickAppPackage2,
                        s.secondaryTripleClickCustomPos2
                    )
                }
            }

            val finalAction1 = if (pTuple.action1 != RemapAction.PASSTHROUGH) pTuple.action1 else sTuple.action1
            val finalPayload1 = if (pTuple.action1 != RemapAction.PASSTHROUGH) pTuple.payload1 else sTuple.payload1
            val finalCustomPos1 = if (pTuple.action1 != RemapAction.PASSTHROUGH) pTuple.useCustomPos1 else sTuple.useCustomPos1

            val finalAction2 = if (pTuple.action2 != RemapAction.PASSTHROUGH) pTuple.action2 else sTuple.action2
            val finalPayload2 = if (pTuple.action2 != RemapAction.PASSTHROUGH) pTuple.payload2 else sTuple.payload2
            val finalCustomPos2 = if (pTuple.action2 != RemapAction.PASSTHROUGH) pTuple.useCustomPos2 else sTuple.useCustomPos2

            return ResolvedGestureAction(finalAction1, finalPayload1, finalCustomPos1, finalAction2, finalPayload2, finalCustomPos2)
        }
    }

    private fun loadFastCacheFromSharedPreferences() {
        val (settings, profiles) = loadFastCacheFromSharedPreferences(applicationContext)
        if (settings.remapperEnabled) {
            currentSettings = settings
        }
        if (profiles.isNotEmpty()) {
            currentAppProfiles = profiles
        }
    }

    private fun startLmkRefreshLoop() {
        if (lmkRefreshJob?.isActive == true) return
        lmkRefreshJob = serviceScope.launch(Dispatchers.IO) {
            while (true) {
                RootHelper.protectCurrentProcessFromLMK()
                RootHelper.protectProcessFromBackgroundKill(packageName)
                delay(60_000L)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isInstanceRunning = true
        loadFastCacheFromSharedPreferences()

        repository = RemapperRepository(applicationContext)

        serviceScope.launch { repository.remapperSettings.collect { currentSettings = it } }
        serviceScope.launch { repository.appProfiles.collect { currentAppProfiles = it } }

        startLmkRefreshLoop()
        AirCommandOverlayService.ensureServiceRunning(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        loadFastCacheFromSharedPreferences()
        startLmkRefreshLoop()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isInstanceRunning = false
        stopAllTimers()
        lmkRefreshJob?.cancel()
        lmkRefreshJob = null
        windowPackageMap.clear()
        AppIconCache.clear()
        ActionExecutor.closeRootShell()
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        windowPackageMap.clear()
        AppIconCache.clear()
        ActionExecutor.closeRootShell()
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            serviceScope.launch(Dispatchers.IO) {
                System.gc()
                RootHelper.protectCurrentProcessFromLMK()
            }
        }
    }

    private fun stopAllTimers() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventPkg = event?.packageName?.toString()
        val winId = event?.windowId
        if (!eventPkg.isNullOrBlank() && winId != null && winId >= 0) {
            windowPackageMap[winId] = eventPkg
        }

        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                if (isValidForegroundPackage(eventPkg)) {
                    activeForegroundPackage = eventPkg!!
                } else {
                    updateActiveForegroundPackage()
                }
            }
        }
    }

    private fun isValidForegroundPackage(pkg: String?): Boolean {
        return isValidForegroundPackage(pkg, packageName)
    }

    private fun updateActiveForegroundPackage() {
        try {
            var rootNode: AccessibilityNodeInfo? = null
            try {
                rootNode = rootInActiveWindow
                if (rootNode != null) {
                    val pkg = rootNode.packageName?.toString()
                    if (isValidForegroundPackage(pkg)) {
                        activeForegroundPackage = pkg!!
                        return
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                @Suppress("DEPRECATION")
                rootNode?.recycle()
            }

            var focusedNode: AccessibilityNodeInfo? = null
            try {
                focusedNode = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focusedNode != null) {
                    val pkg = focusedNode.packageName?.toString()
                    if (isValidForegroundPackage(pkg)) {
                        activeForegroundPackage = pkg!!
                        return
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                @Suppress("DEPRECATION")
                focusedNode?.recycle()
            }

            val windowList = try { windows } catch (_: Exception) { null }
            if (!windowList.isNullOrEmpty()) {
                fun getPkgForWindow(win: AccessibilityWindowInfo): String? {
                    var pkg = windowPackageMap[win.id]
                    if (pkg.isNullOrBlank()) {
                        var node: AccessibilityNodeInfo? = null
                        try {
                            node = win.root
                            pkg = node?.packageName?.toString()
                            if (!pkg.isNullOrBlank()) {
                                windowPackageMap[win.id] = pkg
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            @Suppress("DEPRECATION")
                            node?.recycle()
                        }
                    }
                    return pkg
                }

                val activeWin = windowList.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && (it.isFocused || it.isActive) }
                    ?: windowList.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                val pkg = activeWin?.let { getPkgForWindow(it) }
                if (isValidForegroundPackage(pkg)) {
                    activeForegroundPackage = pkg!!
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {}

    fun resolveGestureAction(count: Int, isLongPress: Boolean, isPrimary: Boolean): ResolvedGestureAction {
        return resolveGestureAction(
            count = count,
            isLongPress = isLongPress,
            isPrimary = isPrimary,
            appProfiles = currentAppProfiles,
            settings = currentSettings,
            foregroundPackage = activeForegroundPackage
        )
    }

    private fun getLongPressDurationMs(isPrimary: Boolean): Long {
        val s = currentSettings
        val duration = if (s.useCustomGestureTimeouts) {
            if (isPrimary) s.primaryLongPressDurationMs else s.secondaryLongPressDurationMs
        } else {
            s.longPressDurationMs
        }
        return duration.toLong().coerceIn(100L, 5000L)
    }

    private fun getClickTimeoutMs(isPrimary: Boolean, isTripleClick: Boolean): Long {
        val s = currentSettings
        val timeout = if (s.useCustomGestureTimeouts) {
            if (isPrimary) {
                if (isTripleClick) s.primaryTripleClickTimeoutMs else s.primaryDoubleClickTimeoutMs
            } else {
                if (isTripleClick) s.secondaryTripleClickTimeoutMs else s.secondaryDoubleClickTimeoutMs
            }
        } else {
            if (isTripleClick) s.tripleClickTimeoutMs else s.doubleClickTimeoutMs
        }
        return timeout.toLong().coerceIn(50L, 3000L)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_PAGE_UP && keyCode != KeyEvent.KEYCODE_PAGE_DOWN) {
            return super.onKeyEvent(event)
        }

        if (!currentSettings.remapperEnabled) {
            val (cachedSettings, cachedProfiles) = loadFastCacheFromSharedPreferences(applicationContext)
            if (cachedSettings.remapperEnabled) {
                currentSettings = cachedSettings
                if (cachedProfiles.isNotEmpty() || currentAppProfiles.isEmpty()) {
                    currentAppProfiles = cachedProfiles
                }
            }
        }

        if (!currentSettings.remapperEnabled) {
            return super.onKeyEvent(event)
        }

        val device = event.device
        val isDeviceMatch = if (device == null) {
            true
        } else {
            currentSettings.deviceNameFilter.isBlank() || device.name?.contains(currentSettings.deviceNameFilter, ignoreCase = true) == true
        }
        if (!isDeviceMatch) {
            return super.onKeyEvent(event)
        }

        val deviceName = device?.name ?: ""
        val keyName = if (keyCode == KeyEvent.KEYCODE_PAGE_UP) "PAGE_UP (Primary)" else "PAGE_DOWN (Secondary)"
        _keyEventsFlow.tryEmit(KeyEventInfo(keyCode = keyCode, keyName = keyName, deviceName = deviceName))

        try {
            val isPrimary = (keyCode == KeyEvent.KEYCODE_PAGE_UP)

            if (event.action == KeyEvent.ACTION_DOWN) {
                if (activeForegroundPackage.isBlank()) {
                    updateActiveForegroundPackage()
                } else {
                    serviceScope.launch {
                        updateActiveForegroundPackage()
                    }
                }
                if (event.repeatCount == 0) {
                    if (isPrimary) {
                        isPrimaryPressed = true
                        primaryClickRunnable?.let { handler.removeCallbacks(it) }
                        primaryLongPressRunnable?.let { handler.removeCallbacks(it) }

                        primaryLongPressRunnable = Runnable {
                            isPrimaryLongPressTriggered = true
                            primaryClickRunnable?.let { handler.removeCallbacks(it) }
                            primaryClickCount = 0
                            val resolved = resolveGestureAction(0, isLongPress = true, isPrimary = true)
                            ActionExecutor.startAction(resolved.action1, applicationContext, resolved.payload1, this@PenRemapperAccessibilityService, resolved.useCustomPos1)
                            if (resolved.action2 != RemapAction.PASSTHROUGH) {
                                ActionExecutor.startAction(resolved.action2, applicationContext, resolved.payload2, this@PenRemapperAccessibilityService, resolved.useCustomPos2)
                            }
                        }
                        handler.postDelayed(primaryLongPressRunnable!!, getLongPressDurationMs(isPrimary = true))
                    } else {
                        isSecondaryPressed = true
                        secondaryClickRunnable?.let { handler.removeCallbacks(it) }
                        secondaryLongPressRunnable?.let { handler.removeCallbacks(it) }

                        secondaryLongPressRunnable = Runnable {
                            isSecondaryLongPressTriggered = true
                            secondaryClickRunnable?.let { handler.removeCallbacks(it) }
                            secondaryClickCount = 0
                            val resolved = resolveGestureAction(0, isLongPress = true, isPrimary = false)
                            ActionExecutor.startAction(resolved.action1, applicationContext, resolved.payload1, this@PenRemapperAccessibilityService, resolved.useCustomPos1)
                            if (resolved.action2 != RemapAction.PASSTHROUGH) {
                                ActionExecutor.startAction(resolved.action2, applicationContext, resolved.payload2, this@PenRemapperAccessibilityService, resolved.useCustomPos2)
                            }
                        }
                        handler.postDelayed(secondaryLongPressRunnable!!, getLongPressDurationMs(isPrimary = false))
                    }
                }
                return true
            } else if (event.action == KeyEvent.ACTION_UP) {
                if (isPrimary) {
                    isPrimaryPressed = false
                    primaryLongPressRunnable?.let { handler.removeCallbacks(it) }

                    if (isPrimaryLongPressTriggered) {
                        val resolved = resolveGestureAction(0, isLongPress = true, isPrimary = true)
                        ActionExecutor.stopAction(resolved.action1, applicationContext, resolved.payload1, this@PenRemapperAccessibilityService, resolved.useCustomPos1)
                        if (resolved.action2 != RemapAction.PASSTHROUGH) {
                            ActionExecutor.stopAction(resolved.action2, applicationContext, resolved.payload2, this@PenRemapperAccessibilityService, resolved.useCustomPos2)
                        }
                        isPrimaryLongPressTriggered = false
                        primaryClickCount = 0
                        primaryClickRunnable?.let { handler.removeCallbacks(it) }
                    } else {
                        primaryClickCount++
                        primaryClickRunnable?.let { handler.removeCallbacks(it) }
                        val delay = getClickTimeoutMs(isPrimary = true, isTripleClick = primaryClickCount >= 2)
                        primaryClickRunnable = Runnable {
                            val count = primaryClickCount
                            primaryClickCount = 0
                            val resolved = resolveGestureAction(count, isLongPress = false, isPrimary = true)
                            ActionExecutor.executeAction(resolved.action1, applicationContext, resolved.payload1, this@PenRemapperAccessibilityService, resolved.useCustomPos1)
                            if (resolved.action2 != RemapAction.PASSTHROUGH) {
                                ActionExecutor.executeAction(resolved.action2, applicationContext, resolved.payload2, this@PenRemapperAccessibilityService, resolved.useCustomPos2)
                            }
                        }
                        handler.postDelayed(primaryClickRunnable!!, delay)
                    }
                } else {
                    isSecondaryPressed = false
                    secondaryLongPressRunnable?.let { handler.removeCallbacks(it) }

                    if (isSecondaryLongPressTriggered) {
                        val resolved = resolveGestureAction(0, isLongPress = true, isPrimary = false)
                        ActionExecutor.stopAction(resolved.action1, applicationContext, resolved.payload1, this@PenRemapperAccessibilityService, resolved.useCustomPos1)
                        if (resolved.action2 != RemapAction.PASSTHROUGH) {
                            ActionExecutor.stopAction(resolved.action2, applicationContext, resolved.payload2, this@PenRemapperAccessibilityService, resolved.useCustomPos2)
                        }
                        isSecondaryLongPressTriggered = false
                        secondaryClickCount = 0
                        secondaryClickRunnable?.let { handler.removeCallbacks(it) }
                    } else {
                        secondaryClickCount++
                        secondaryClickRunnable?.let { handler.removeCallbacks(it) }
                        val delay = getClickTimeoutMs(isPrimary = false, isTripleClick = secondaryClickCount >= 2)
                        secondaryClickRunnable = Runnable {
                            val count = secondaryClickCount
                            secondaryClickCount = 0
                            val resolved = resolveGestureAction(count, isLongPress = false, isPrimary = false)
                            ActionExecutor.executeAction(resolved.action1, applicationContext, resolved.payload1, this@PenRemapperAccessibilityService, resolved.useCustomPos1)
                            if (resolved.action2 != RemapAction.PASSTHROUGH) {
                                ActionExecutor.executeAction(resolved.action2, applicationContext, resolved.payload2, this@PenRemapperAccessibilityService, resolved.useCustomPos2)
                            }
                        }
                        handler.postDelayed(secondaryClickRunnable!!, delay)
                    }
                }
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }
}
