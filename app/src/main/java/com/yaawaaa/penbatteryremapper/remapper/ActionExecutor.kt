package com.yaawaaa.penbatteryremapper.remapper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.yaawaaa.penbatteryremapper.service.AirCommandOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class ActionType {
    ONE_SHOT,
    CONTINUOUS
}

object ActionExecutor {

    private var isTorchOn = false

    private var activeTouchDownTime = 0L
    private var activeTouchX = 0f
    private var activeTouchY = 0f

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var volumeRunnable: Runnable? = null
    private val executorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun runShellAsync(command: String) {
        executorScope.launch {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getActionType(action: RemapAction): ActionType {
        return when (action) {
            RemapAction.VOLUME_UP, RemapAction.VOLUME_DOWN,
            RemapAction.SCREEN_LONG_PRESS_LEFT, RemapAction.SCREEN_LONG_PRESS_RIGHT,
            RemapAction.CUSTOM_KEYBOARD_SHORTCUT,
            RemapAction.AIR_COMMAND -> ActionType.CONTINUOUS
            else -> ActionType.ONE_SHOT
        }
    }

    fun startAction(
        action: RemapAction,
        context: Context,
        appPackage: String = "",
        accessibilityService: AccessibilityService? = null
    ) {
        when (action) {
            RemapAction.VOLUME_UP -> startVolumeRepeat(context, AudioManager.ADJUST_RAISE)
            RemapAction.VOLUME_DOWN -> startVolumeRepeat(context, AudioManager.ADJUST_LOWER)
            RemapAction.SCREEN_LONG_PRESS_LEFT -> startScreenHold(context, true, accessibilityService)
            RemapAction.SCREEN_LONG_PRESS_RIGHT -> startScreenHold(context, false, accessibilityService)
            RemapAction.CUSTOM_KEYBOARD_SHORTCUT -> startShortcut(appPackage, context, accessibilityService)
            RemapAction.AIR_COMMAND -> AirCommandOverlayService.showHold(context)
            else -> executeAction(action, context, appPackage, accessibilityService)
        }
    }

    fun stopAction(
        action: RemapAction,
        context: Context,
        appPackage: String = "",
        accessibilityService: AccessibilityService? = null
    ) {
        when (action) {
            RemapAction.VOLUME_UP, RemapAction.VOLUME_DOWN -> stopVolumeRepeat()
            RemapAction.SCREEN_LONG_PRESS_LEFT, RemapAction.SCREEN_LONG_PRESS_RIGHT -> stopScreenHold(accessibilityService)
            RemapAction.CUSTOM_KEYBOARD_SHORTCUT -> stopShortcut(appPackage, context, accessibilityService)
            RemapAction.AIR_COMMAND -> AirCommandOverlayService.hideHold(context)
            else -> { /* Do nothing for one-shot actions */ }
        }
    }

    fun startDualActions(
        action1: RemapAction,
        app1: String = "",
        action2: RemapAction = RemapAction.PASSTHROUGH,
        app2: String = "",
        context: Context,
        accessibilityService: AccessibilityService? = null
    ) {
        startAction(action1, context, app1, accessibilityService)
        if (action2 != RemapAction.PASSTHROUGH) {
            startAction(action2, context, app2, accessibilityService)
        }
    }

    fun stopDualActions(
        action1: RemapAction,
        app1: String = "",
        action2: RemapAction = RemapAction.PASSTHROUGH,
        app2: String = "",
        context: Context,
        accessibilityService: AccessibilityService? = null
    ) {
        stopAction(action1, context, app1, accessibilityService)
        if (action2 != RemapAction.PASSTHROUGH) {
            stopAction(action2, context, app2, accessibilityService)
        }
    }

    private fun startVolumeRepeat(context: Context, direction: Int) {
        stopVolumeRepeat()
        val runnable = object : Runnable {
            override fun run() {
                adjustVolume(context, direction)
                handler.postDelayed(this, 60L)
            }
        }
        volumeRunnable = runnable
        handler.post(runnable)
    }

    private fun stopVolumeRepeat() {
        volumeRunnable?.let {
            handler.removeCallbacks(it)
            volumeRunnable = null
        }
    }

    fun executeAction(
        action: RemapAction,
        context: Context,
        appPackage: String = "",
        accessibilityService: AccessibilityService? = null
    ) {
        try {
            when (action) {
                RemapAction.PLAY_PAUSE -> dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                RemapAction.NEXT_TRACK -> dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
                RemapAction.PREV_TRACK -> dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                RemapAction.VOLUME_UP -> adjustVolume(context, AudioManager.ADJUST_RAISE)
                RemapAction.VOLUME_DOWN -> adjustVolume(context, AudioManager.ADJUST_LOWER)
                RemapAction.SCREENSHOT -> takeScreenshot(accessibilityService)
                RemapAction.FLASHLIGHT -> toggleFlashlight(context)
                RemapAction.LAUNCH_APP -> launchApp(context, appPackage)
                RemapAction.CUSTOM_KEYBOARD_SHORTCUT -> executeShortcut(appPackage, context, accessibilityService)
                RemapAction.TOGGLE_PEN_ERASER -> executePenEraserToggle(appPackage, context, accessibilityService)
                RemapAction.PASSTHROUGH -> { /* do nothing */ }
                RemapAction.HOME -> performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_HOME)
                RemapAction.BACK -> performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_BACK)
                RemapAction.RECENTS -> performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_RECENTS)
                RemapAction.LOCK_SCREEN -> performLockScreen(accessibilityService)
                RemapAction.MUTE -> toggleMute(context)
                RemapAction.TOGGLE_SCREEN -> toggleScreen(context, accessibilityService)
                RemapAction.SCREEN_ON -> wakeUpScreen(context)
                RemapAction.SCREEN_OFF -> turnScreenOff(context, accessibilityService)
                RemapAction.SPLIT_SCREEN -> toggleSplitScreen(accessibilityService)
                RemapAction.OPEN_NOTIFICATIONS -> openNotifications(accessibilityService)
                RemapAction.OPEN_QUICK_SETTINGS -> openQuickSettings(accessibilityService)
                RemapAction.TOGGLE_AUTO_ROTATE -> toggleAutoRotate(context)
                RemapAction.CLEAR_NOTIFICATIONS -> clearNotifications(accessibilityService)
                RemapAction.KILL_FOREGROUND_APP -> killForegroundApp()
                RemapAction.SWIPE_UP, RemapAction.SWIPE_DOWN, RemapAction.SWIPE_LEFT, RemapAction.SWIPE_RIGHT -> executeSwipe(action, accessibilityService)
                RemapAction.SCREEN_DOUBLE_TAP_LEFT -> executeScreenDoubleTap(true, accessibilityService)
                RemapAction.SCREEN_DOUBLE_TAP_RIGHT -> executeScreenDoubleTap(false, accessibilityService)
                RemapAction.SCREEN_LONG_PRESS_LEFT -> startScreenHold(context, true, accessibilityService)
                RemapAction.SCREEN_LONG_PRESS_RIGHT -> startScreenHold(context, false, accessibilityService)
                RemapAction.AIR_COMMAND -> AirCommandOverlayService.toggle(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun wakeUpScreen(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "PenRemapper:WakeLock"
            )
            wakeLock?.acquire(3000L)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun turnScreenOff(context: Context, accessibilityService: AccessibilityService? = null) {
        if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val success = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            if (success) return
        }
        runShellAsync("input keyevent 26")
    }

    private fun performGlobalAction(accessibilityService: AccessibilityService?, actionId: Int) {
        try {
            accessibilityService?.performGlobalAction(actionId)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun performLockScreen(accessibilityService: AccessibilityService?) {
        try {
            if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun toggleScreen(context: Context, accessibilityService: AccessibilityService?) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isInteractive = powerManager?.isInteractive ?: true
            if (!isInteractive) {
                wakeUpScreen(context)
            } else {
                turnScreenOff(context, accessibilityService)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun startShortcut(shortcutSpec: String, context: Context, accessibilityService: AccessibilityService?) {
        if (shortcutSpec.isBlank()) return
        val (metaState, activeModifiers, targetKeyCode) = getShortcutInfo(shortcutSpec)
        injectKeyEvents(context, activeModifiers, targetKeyCode, metaState, KeyEvent.ACTION_DOWN)
    }

    fun stopShortcut(shortcutSpec: String, context: Context, accessibilityService: AccessibilityService?) {
        if (shortcutSpec.isBlank()) return
        val (metaState, activeModifiers, targetKeyCode) = getShortcutInfo(shortcutSpec)
        injectKeyEvents(context, activeModifiers, targetKeyCode, metaState, KeyEvent.ACTION_UP)
    }

    private fun getShortcutInfo(shortcutSpec: String): Triple<Int, List<Int>, Int> {
        val upperSpec = shortcutSpec.uppercase()
        val ctrl = upperSpec.contains("CTRL") || upperSpec.contains("CONTROL")
        val alt = upperSpec.contains("ALT") || upperSpec.contains("OPTION")
        val shift = upperSpec.contains("SHIFT")

        var metaState = 0
        val activeModifiers = mutableListOf<Int>()
        if (ctrl) {
            metaState = metaState or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
            activeModifiers.add(KeyEvent.KEYCODE_CTRL_LEFT)
        }
        if (alt) {
            metaState = metaState or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
            activeModifiers.add(KeyEvent.KEYCODE_ALT_LEFT)
        }
        if (shift) {
            metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
            activeModifiers.add(KeyEvent.KEYCODE_SHIFT_LEFT)
        }

        val allKeyCodes = parseShortcutToKeyCodes(shortcutSpec)
        val modifierKeyCodes = setOf(
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT
        )
        val targetKeyCode = allKeyCodes.firstOrNull { it !in modifierKeyCodes }
            ?: allKeyCodes.lastOrNull()
            ?: KeyEvent.KEYCODE_UNKNOWN
            
        return Triple(metaState, activeModifiers, targetKeyCode)
    }

    private fun injectKeyEvents(context: Context, modifiers: List<Int>, targetKey: Int, metaState: Int, action: Int) {
        try {
            val inputManagerClass = Class.forName("android.hardware.input.InputManager")
            val inputManager = context.getSystemService(Context.INPUT_SERVICE)
                ?: inputManagerClass.getMethod("getInstance").invoke(null)

            if (inputManager != null) {
                val injectMethod = inputManager.javaClass.getMethod(
                    "injectInputEvent",
                    InputEvent::class.java,
                    Int::class.javaPrimitiveType
                )

                val now = SystemClock.uptimeMillis()

                if (action == KeyEvent.ACTION_DOWN) {
                    modifiers.forEach { modKey ->
                        val modDown = KeyEvent(now, now, KeyEvent.ACTION_DOWN, modKey, 0, metaState)
                        injectMethod.invoke(inputManager, modDown, 0)
                    }
                    if (targetKey != KeyEvent.KEYCODE_UNKNOWN) {
                        val downEvent = KeyEvent(now, now, KeyEvent.ACTION_DOWN, targetKey, 0, metaState)
                        injectMethod.invoke(inputManager, downEvent, 0)
                    }
                } else {
                    if (targetKey != KeyEvent.KEYCODE_UNKNOWN) {
                        val upEvent = KeyEvent(now, now, KeyEvent.ACTION_UP, targetKey, 0, metaState)
                        injectMethod.invoke(inputManager, upEvent, 0)
                    }
                    modifiers.reversed().forEach { modKey ->
                        val modUp = KeyEvent(now, now, KeyEvent.ACTION_UP, modKey, 0, metaState)
                        injectMethod.invoke(inputManager, modUp, 0)
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun executeShortcut(shortcutSpec: String, context: Context, accessibilityService: AccessibilityService? = null) {
        if (shortcutSpec.isBlank()) return
        if (tryAccessibilityNodeAction(shortcutSpec, accessibilityService)) return

        val allKeyCodes = parseShortcutToKeyCodes(shortcutSpec)
        val (metaState, activeModifiers, targetKeyCode) = getShortcutInfo(shortcutSpec)
        
        if (targetKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            injectKeyEvents(context, activeModifiers, targetKeyCode, metaState, KeyEvent.ACTION_DOWN)
            injectKeyEvents(context, activeModifiers, targetKeyCode, metaState, KeyEvent.ACTION_UP)
        } else {
            executeKeyCombo(allKeyCodes)
        }
    }

    fun executePenEraserToggle(presetSpec: String, context: Context, accessibilityService: AccessibilityService? = null) {
        val trimmed = presetSpec.trim()
        if (trimmed.contains("CTRL", ignoreCase = true) || trimmed.contains("ALT", ignoreCase = true) || trimmed.contains("SHIFT", ignoreCase = true)) {
            executeShortcut(trimmed, context, accessibilityService)
        } else {
            val keyCodes = parsePenEraserPreset(presetSpec)
            executeKeyCombo(keyCodes)
        }
    }

    enum class TextShortcutType { SELECT_ALL, COPY, PASTE, CUT }

    fun getTextShortcutType(shortcutSpec: String): TextShortcutType? {
        if (shortcutSpec.isBlank()) return null
        val upper = shortcutSpec.uppercase().replace(" ", "").replace("CONTROL", "CTRL").replace("OPTION", "ALT").replace("COMMAND", "CMD")
        val keyCodes = parseShortcutToKeyCodes(shortcutSpec)
        val targetKeyCode = keyCodes.firstOrNull { it !in setOf(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT, KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT, KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT) } ?: KeyEvent.KEYCODE_UNKNOWN

        return when {
            upper.contains("CTRL+A") || upper == "SELECT_ALL" || (targetKeyCode == KeyEvent.KEYCODE_A && upper.contains("CTRL")) -> TextShortcutType.SELECT_ALL
            upper.contains("CTRL+C") || upper == "COPY" || (targetKeyCode == KeyEvent.KEYCODE_C && upper.contains("CTRL")) -> TextShortcutType.COPY
            upper.contains("CTRL+V") || upper == "PASTE" || (targetKeyCode == KeyEvent.KEYCODE_V && upper.contains("CTRL")) -> TextShortcutType.PASTE
            upper.contains("CTRL+X") || upper == "CUT" || (targetKeyCode == KeyEvent.KEYCODE_X && upper.contains("CTRL")) -> TextShortcutType.CUT
            else -> null
        }
    }

    private fun tryAccessibilityNodeAction(shortcutSpec: String, accessibilityService: AccessibilityService?): Boolean {
        if (accessibilityService == null) return false
        val type = getTextShortcutType(shortcutSpec) ?: return false

        return try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return false
            val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false

            when (type) {
                TextShortcutType.SELECT_ALL -> {
                    val textLength = focusedNode.text?.length ?: Int.MAX_VALUE
                    val args = Bundle().apply {
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, textLength)
                    }
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args) || focusedNode.performAction(AccessibilityNodeInfo.ACTION_SELECT)
                }
                TextShortcutType.COPY -> focusedNode.performAction(AccessibilityNodeInfo.ACTION_COPY)
                TextShortcutType.PASTE -> focusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                TextShortcutType.CUT -> focusedNode.performAction(AccessibilityNodeInfo.ACTION_CUT)
            }
        } catch (e: Exception) { false }
    }

    fun parsePenEraserPreset(presetSpec: String): List<Int> {
        val trimmed = presetSpec.trim()
        return when {
            trimmed.equals("CTRL+E", ignoreCase = true) -> listOf(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_E)
            trimmed.equals("E", ignoreCase = true) -> listOf(KeyEvent.KEYCODE_E)
            trimmed.contains("202") || trimmed.contains("Stylus", ignoreCase = true) -> listOf(202)
            else -> parseShortcutToKeyCodes(trimmed)
        }
    }

    fun parseShortcutToKeyCodes(shortcut: String): List<Int> {
        if (shortcut.isBlank()) return listOf(KeyEvent.KEYCODE_C)
        val tokens = shortcut.trim().split("+", "-", " ").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        return tokens.mapNotNull { token ->
            when (token) {
                "CTRL", "CONTROL" -> KeyEvent.KEYCODE_CTRL_LEFT
                "ALT", "OPTION" -> KeyEvent.KEYCODE_ALT_LEFT
                "SHIFT" -> KeyEvent.KEYCODE_SHIFT_LEFT
                "META", "CMD", "COMMAND" -> KeyEvent.KEYCODE_META_LEFT
                "COPY" -> KeyEvent.KEYCODE_COPY
                "PASTE" -> KeyEvent.KEYCODE_PASTE
                "CUT" -> KeyEvent.KEYCODE_CUT
                "ENTER" -> KeyEvent.KEYCODE_ENTER
                "TAB" -> KeyEvent.KEYCODE_TAB
                "A" -> KeyEvent.KEYCODE_A
                "B" -> KeyEvent.KEYCODE_B
                "C" -> KeyEvent.KEYCODE_C
                "D" -> KeyEvent.KEYCODE_D
                "E" -> KeyEvent.KEYCODE_E
                "F" -> KeyEvent.KEYCODE_F
                "G" -> KeyEvent.KEYCODE_G
                "H" -> KeyEvent.KEYCODE_H
                "I" -> KeyEvent.KEYCODE_I
                "J" -> KeyEvent.KEYCODE_J
                "K" -> KeyEvent.KEYCODE_K
                "L" -> KeyEvent.KEYCODE_L
                "M" -> KeyEvent.KEYCODE_M
                "N" -> KeyEvent.KEYCODE_N
                "O" -> KeyEvent.KEYCODE_O
                "P" -> KeyEvent.KEYCODE_P
                "Q" -> KeyEvent.KEYCODE_Q
                "R" -> KeyEvent.KEYCODE_R
                "S" -> KeyEvent.KEYCODE_S
                "T" -> KeyEvent.KEYCODE_T
                "U" -> KeyEvent.KEYCODE_U
                "V" -> KeyEvent.KEYCODE_V
                "W" -> KeyEvent.KEYCODE_W
                "X" -> KeyEvent.KEYCODE_X
                "Y" -> KeyEvent.KEYCODE_Y
                "Z" -> KeyEvent.KEYCODE_Z
                "0" -> KeyEvent.KEYCODE_0
                "1" -> KeyEvent.KEYCODE_1
                "2" -> KeyEvent.KEYCODE_2
                "3" -> KeyEvent.KEYCODE_3
                "4" -> KeyEvent.KEYCODE_4
                "5" -> KeyEvent.KEYCODE_5
                "6" -> KeyEvent.KEYCODE_6
                "7" -> KeyEvent.KEYCODE_7
                "8" -> KeyEvent.KEYCODE_8
                "9" -> KeyEvent.KEYCODE_9
                else -> {
                    val codeFromInt = token.toIntOrNull()
                    if (codeFromInt != null) {
                        codeFromInt
                    } else {
                        try {
                            val keyName = if (token.startsWith("KEYCODE_")) token else "KEYCODE_$token"
                            val code = KeyEvent.keyCodeFromString(keyName)
                            if (code != KeyEvent.KEYCODE_UNKNOWN) code else null
                        } catch (e: Throwable) {
                            null
                        }
                    }
                }
            }
        }
    }

    fun executeKeyCombo(keyCodes: List<Int>) {
        executeKeyCombination(keyCodes)
    }

    fun executeKeyCombination(keyCodes: List<Int>): Boolean {
        if (keyCodes.isEmpty()) return false
        val keyCodesStr = keyCodes.joinToString(" ")
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "input keyevent $keyCodesStr"))
            if (Looper.myLooper() == Looper.getMainLooper()) {
                executorScope.launch {
                    try { process.waitFor() } catch (e: Exception) { e.printStackTrace() }
                }
                true
            } else {
                val exitCode = process.waitFor()
                exitCode == 0
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun toggleSplitScreen(accessibilityService: AccessibilityService?) {
        try {
            if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun openNotifications(accessibilityService: AccessibilityService?) {
        try {
            accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun openQuickSettings(accessibilityService: AccessibilityService?) {
        try {
            accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun toggleAutoRotate(context: Context) {
        try {
            val current = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            val newValue = if (current == 1) 0 else 1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(context)) {
                Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, newValue)
            } else {
                runShellAsync("settings put system accelerometer_rotation $newValue")
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun clearNotifications(accessibilityService: AccessibilityService?) {
        try {
            if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun killForegroundApp() {
        executorScope.launch {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "dumpsys window | grep -E mCurrentFocus"))
                val reader = process.inputStream.bufferedReader()
                val line = reader.readLine()
                val pkg = line?.substringAfter("{")?.substringBefore("/")?.split(" ")?.lastOrNull()
                if (!pkg.isNullOrBlank()) {
                    Runtime.getRuntime().exec(arrayOf("su", "-c", "am force-stop $pkg"))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun toggleMute(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustVolume(AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun dispatchMediaKey(context: Context, keyCode: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val eventTime = SystemClock.uptimeMillis()
            audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
            audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun adjustVolume(context: Context, direction: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            audioManager.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun takeScreenshot(accessibilityService: AccessibilityService?) {
        try {
            if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun toggleFlashlight(context: Context) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            isTorchOn = !isTorchOn
            cameraManager.setTorchMode(cameraId, isTorchOn)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun launchApp(context: Context, packageName: String) {
        if (packageName.isBlank()) return
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } ?: return
            context.startActivity(intent)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun executeSwipe(action: RemapAction, accessibilityService: AccessibilityService?) {
        try {
            if (accessibilityService == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
            val metrics = accessibilityService.resources.displayMetrics
            val path = Path()
            when (action) {
                RemapAction.SWIPE_UP -> path.apply { moveTo(metrics.widthPixels / 2f, metrics.heightPixels * 0.75f); lineTo(metrics.widthPixels / 2f, metrics.heightPixels * 0.25f) }
                RemapAction.SWIPE_DOWN -> path.apply { moveTo(metrics.widthPixels / 2f, metrics.heightPixels * 0.25f); lineTo(metrics.widthPixels / 2f, metrics.heightPixels * 0.75f) }
                RemapAction.SWIPE_LEFT -> path.apply { moveTo(metrics.widthPixels * 0.8f, metrics.heightPixels / 2f); lineTo(metrics.widthPixels * 0.2f, metrics.heightPixels / 2f) }
                RemapAction.SWIPE_RIGHT -> path.apply { moveTo(metrics.widthPixels * 0.2f, metrics.heightPixels / 2f); lineTo(metrics.widthPixels * 0.8f, metrics.heightPixels / 2f) }
                else -> return
            }
            val builder = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, 150L))
            accessibilityService.dispatchGesture(builder.build(), null, null)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun executeScreenDoubleTap(isLeft: Boolean, accessibilityService: AccessibilityService?) {
        try {
            if (accessibilityService == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
            val metrics = accessibilityService.resources.displayMetrics
            val x = if (isLeft) metrics.widthPixels * 0.15f else metrics.widthPixels * 0.85f
            val path = Path().apply { moveTo(x, metrics.heightPixels / 2f) }
            val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, 50L)).build()
            accessibilityService.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gd: GestureDescription?) {
                    super.onCompleted(gd)
                    handler.postDelayed({ accessibilityService.dispatchGesture(gesture, null, null) }, 50L)
                }
            }, null)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun startScreenHold(context: Context, isLeft: Boolean, accessibilityService: AccessibilityService? = null) {
        if (activeTouchDownTime != 0L) return
        val metrics = context.resources.displayMetrics
        activeTouchX = if (isLeft) metrics.widthPixels * 0.15f else metrics.widthPixels * 0.85f
        activeTouchY = metrics.heightPixels * 0.5f
        activeTouchDownTime = SystemClock.uptimeMillis()

        try {
            val downEvent = MotionEvent.obtain(
                activeTouchDownTime,
                activeTouchDownTime,
                MotionEvent.ACTION_DOWN,
                activeTouchX,
                activeTouchY,
                0
            ).apply {
                source = InputDevice.SOURCE_TOUCHSCREEN
            }

            var injected = false
            try {
                val im = Class.forName("android.hardware.input.InputManager").getMethod("getInstance").invoke(null)
                val injectMethod = Class.forName("android.hardware.input.InputManager").getMethod(
                    "injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType
                )
                injectMethod.invoke(im, downEvent, 0)
                injected = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
            downEvent.recycle()

            if (!injected) {
                runShellAsync("input motionevent DOWN $activeTouchX $activeTouchY")
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun stopScreenHold(accessibilityService: AccessibilityService? = null) {
        if (activeTouchDownTime != 0L) {
            try {
                val now = SystemClock.uptimeMillis()
                val upEvent = MotionEvent.obtain(
                    activeTouchDownTime,
                    now,
                    MotionEvent.ACTION_UP,
                    activeTouchX,
                    activeTouchY,
                    0
                ).apply {
                    source = InputDevice.SOURCE_TOUCHSCREEN
                }

                var injected = false
                try {
                    val im = Class.forName("android.hardware.input.InputManager").getMethod("getInstance").invoke(null)
                    val injectMethod = Class.forName("android.hardware.input.InputManager").getMethod(
                        "injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType
                    )
                    injectMethod.invoke(im, upEvent, 0)
                    injected = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                upEvent.recycle()

                if (!injected) {
                    runShellAsync("input motionevent UP $activeTouchX $activeTouchY")
                }
            } catch (e: Exception) { e.printStackTrace() }
            activeTouchDownTime = 0L
        }
    }
}
