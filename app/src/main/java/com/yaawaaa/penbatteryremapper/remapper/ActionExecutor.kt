package com.yaawaaa.penbatteryremapper.remapper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
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
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.yaawaaa.penbatteryremapper.service.AirCommandOverlayService
import com.yaawaaa.penbatteryremapper.service.PenRemapperAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object ActionExecutor {

    private val Rect.rectW: Int get() = right - left
    private val Rect.rectH: Int get() = bottom - top
    private val Rect.rectCenterX: Float get() = left + (right - left) / 2f
    private val Rect.rectCenterY: Float get() = top + (bottom - top) / 2f

    private var isTorchOn = false

    private var activeTouchDownTime = 0L
    private var activeTouchX = 0f
    private var activeTouchY = 0f
    private var activeHoldStroke: GestureDescription.StrokeDescription? = null

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var volumeRunnable: Runnable? = null
    private var repeatKeyRunnable: Runnable? = null
    private val executorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var rootShellProcess: Process? = null
    private var rootShellWriter: OutputStreamWriter? = null
    private var rootDrainerThread: Thread? = null

    @Synchronized
    private fun getRootWriter(): OutputStreamWriter? {
        if (rootShellProcess == null || rootShellWriter == null) {
            try {
                val proc = ProcessBuilder("su").redirectErrorStream(true).start()
                rootShellProcess = proc
                rootShellWriter = OutputStreamWriter(proc.outputStream, Charsets.UTF_8)
                rootDrainerThread = Thread {
                    try {
                        val buf = ByteArray(1024)
                        val inStream = proc.inputStream
                        while (inStream.read(buf) != -1) {}
                    } catch (_: Exception) {}
                }.apply { isDaemon = true; start() }
            } catch (e: Exception) {
                closeRootShell()
            }
        }
        return rootShellWriter
    }

    @Synchronized
    fun closeRootShell() {
        try { rootDrainerThread?.interrupt() } catch (_: Exception) {}
        try { rootShellWriter?.flush() } catch (_: Exception) {}
        try { rootShellWriter?.close() } catch (_: Exception) {}
        try { rootShellProcess?.outputStream?.close() } catch (_: Exception) {}
        try { rootShellProcess?.inputStream?.close() } catch (_: Exception) {}
        try { rootShellProcess?.errorStream?.close() } catch (_: Exception) {}
        try { rootShellProcess?.destroy() } catch (_: Exception) {}
        rootDrainerThread = null
        rootShellWriter = null
        rootShellProcess = null
    }

    @Synchronized
    fun executeShellCommand(command: String) {
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit {
            try {
                var writer = getRootWriter()
                if (writer != null) {
                    try {
                        writer.write("$command\n")
                        writer.flush()
                        return@submit
                    } catch (e: Exception) {
                        closeRootShell()
                        writer = getRootWriter()
                        if (writer != null) {
                            writer.write("$command\n")
                            writer.flush()
                            return@submit
                        }
                    }
                }
                val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
                val drainer = Thread {
                    try {
                        val buf = ByteArray(1024)
                        val inStream = process.inputStream
                        while (inStream.read(buf) != -1) {}
                    } catch (_: Exception) {}
                }.apply { isDaemon = true; start() }
                val finished = process.waitFor(2, TimeUnit.SECONDS)
                if (!finished) {
                    process.destroy()
                }
                drainer.interrupt()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            future.get(2, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            closeRootShell()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            executor.shutdownNow()
        }
    }

    fun sendRootKeyInput(keyCodesStr: String?) {
        if (keyCodesStr.isNullOrBlank()) return
        val parts = keyCodesStr.trim().split(" ", "+", ",").mapNotNull { it.toIntOrNull() }
        if (parts.size > 1) {
            executeKeyCombination(parts)
        } else {
            runShellAsync("input keyevent $keyCodesStr")
        }
    }

    private fun runShellAsync(command: String) {
        executorScope.launch {
            executeShellCommand(command)
        }
    }

    private fun stopRepeatKeyRunnable() {
        repeatKeyRunnable?.let {
            handler.removeCallbacks(it)
            repeatKeyRunnable = null
        }
    }

    fun startAction(
        action: RemapAction,
        context: Context,
        appPackage: String = "",
        accessibilityService: AccessibilityService? = null,
        useCustomPositions: Boolean = true
    ) {
        val effectiveContext = accessibilityService ?: context
        when (action) {
            RemapAction.VOLUME_UP -> startVolumeRepeat(effectiveContext, AudioManager.ADJUST_RAISE)
            RemapAction.VOLUME_DOWN -> startVolumeRepeat(effectiveContext, AudioManager.ADJUST_LOWER)
            RemapAction.SCREEN_LONG_PRESS_LEFT -> startScreenHold(effectiveContext, true, accessibilityService, appPackage, useCustomPositions)
            RemapAction.SCREEN_LONG_PRESS_RIGHT -> startScreenHold(effectiveContext, false, accessibilityService, appPackage, useCustomPositions)
            RemapAction.CUSTOM_KEYBOARD_SHORTCUT -> startShortcut(appPackage, effectiveContext, accessibilityService)
            RemapAction.AIR_COMMAND -> AirCommandOverlayService.showHold(effectiveContext)
            else -> executeAction(action, context, appPackage, accessibilityService, useCustomPositions)
        }
    }

    fun stopAction(
        action: RemapAction,
        context: Context,
        appPackage: String = "",
        accessibilityService: AccessibilityService? = null,
        useCustomPositions: Boolean = true
    ) {
        val effectiveContext = accessibilityService ?: context
        when (action) {
            RemapAction.VOLUME_UP, RemapAction.VOLUME_DOWN -> stopVolumeRepeat()
            RemapAction.SCREEN_LONG_PRESS_LEFT, RemapAction.SCREEN_LONG_PRESS_RIGHT -> stopScreenHold(accessibilityService)
            RemapAction.CUSTOM_KEYBOARD_SHORTCUT -> stopShortcut(appPackage, effectiveContext, accessibilityService)
            RemapAction.AIR_COMMAND -> AirCommandOverlayService.hideHold(effectiveContext)
            else -> { /* Do nothing for one-shot actions */ }
        }
    }

    private fun startVolumeRepeat(context: Context, direction: Int) {
        stopVolumeRepeat()
        adjustVolume(context, direction)
        val runnable = object : Runnable {
            override fun run() {
                adjustVolume(context, direction)
                handler.postDelayed(this, 60L)
            }
        }
        volumeRunnable = runnable
        handler.postDelayed(runnable, 300L)
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
        accessibilityService: AccessibilityService? = null,
        useCustomPositions: Boolean = true
    ) {
        val effectiveContext = accessibilityService ?: context
        try {
            when (action) {
                RemapAction.PLAY_PAUSE -> dispatchMediaKey(effectiveContext, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                RemapAction.NEXT_TRACK -> dispatchMediaKey(effectiveContext, KeyEvent.KEYCODE_MEDIA_NEXT)
                RemapAction.PREV_TRACK -> dispatchMediaKey(effectiveContext, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                RemapAction.VOLUME_UP -> adjustVolume(effectiveContext, AudioManager.ADJUST_RAISE)
                RemapAction.VOLUME_DOWN -> adjustVolume(effectiveContext, AudioManager.ADJUST_LOWER)
                RemapAction.SCREENSHOT -> takeScreenshot(accessibilityService)
                RemapAction.FLASHLIGHT -> toggleFlashlight(effectiveContext)
                RemapAction.LAUNCH_APP -> launchApp(effectiveContext, appPackage)
                RemapAction.CUSTOM_KEYBOARD_SHORTCUT -> executeShortcut(appPackage, effectiveContext, accessibilityService)
                RemapAction.TOGGLE_PEN_ERASER -> executePenEraserToggle(appPackage, effectiveContext, accessibilityService)
                RemapAction.PASSTHROUGH -> { /* do nothing */ }
                RemapAction.HOME -> performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_HOME, KeyEvent.KEYCODE_HOME)
                RemapAction.BACK -> performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_BACK, KeyEvent.KEYCODE_BACK)
                RemapAction.RECENTS -> performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_RECENTS, KeyEvent.KEYCODE_APP_SWITCH)
                RemapAction.LOCK_SCREEN -> performLockScreen(accessibilityService)
                RemapAction.MUTE -> toggleMute(effectiveContext)
                RemapAction.TOGGLE_SCREEN -> toggleScreen(effectiveContext, accessibilityService)
                RemapAction.SCREEN_ON -> wakeUpScreen(effectiveContext)
                RemapAction.SCREEN_OFF -> turnScreenOff(effectiveContext, accessibilityService)
                RemapAction.SPLIT_SCREEN -> toggleSplitScreen(accessibilityService)
                RemapAction.OPEN_NOTIFICATIONS -> openNotifications(accessibilityService)
                RemapAction.OPEN_QUICK_SETTINGS -> openQuickSettings(accessibilityService)
                RemapAction.TOGGLE_AUTO_ROTATE -> toggleAutoRotate(effectiveContext)
                RemapAction.CLEAR_NOTIFICATIONS -> clearNotifications(accessibilityService)
                RemapAction.KILL_FOREGROUND_APP -> killForegroundApp()
                RemapAction.SWIPE_UP, RemapAction.SWIPE_DOWN, RemapAction.SWIPE_LEFT, RemapAction.SWIPE_RIGHT -> executeSwipe(action, accessibilityService, effectiveContext, appPackage)
                RemapAction.SCREEN_TAP -> executeScreenTap(null, accessibilityService, effectiveContext, appPackage, useCustomPositions)
                RemapAction.SCREEN_TAP_LEFT -> executeScreenTap(true, accessibilityService, effectiveContext, appPackage, useCustomPositions)
                RemapAction.SCREEN_TAP_RIGHT -> executeScreenTap(false, accessibilityService, effectiveContext, appPackage, useCustomPositions)
                RemapAction.SCREEN_DOUBLE_TAP_LEFT -> executeScreenDoubleTap(true, accessibilityService, effectiveContext, appPackage, useCustomPositions)
                RemapAction.SCREEN_DOUBLE_TAP_RIGHT -> executeScreenDoubleTap(false, accessibilityService, effectiveContext, appPackage, useCustomPositions)
                RemapAction.SCREEN_LONG_PRESS_LEFT -> executeScreenLongPress(true, accessibilityService, effectiveContext, appPackage, useCustomPositions)
                RemapAction.SCREEN_LONG_PRESS_RIGHT -> executeScreenLongPress(false, accessibilityService, effectiveContext, appPackage, useCustomPositions)
                RemapAction.AIR_COMMAND -> AirCommandOverlayService.toggle(effectiveContext)
                RemapAction.LOCKSCREEN_AWAKE -> triggerLockscreenAwake(effectiveContext, appPackage)
                RemapAction.TRIGGER_EXTERNAL_APP -> triggerExternalApp(effectiveContext, appPackage)
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
        runShellAsync("input keyevent 224")
    }

    fun turnScreenOff(context: Context, accessibilityService: AccessibilityService? = null) {
        var handled = false
        if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            handled = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        }
        if (!handled) {
            runShellAsync("input keyevent 26")
        }
    }

    private fun performGlobalAction(
        accessibilityService: AccessibilityService?,
        actionId: Int,
        fallbackKeyCode: Int? = null
    ): Boolean {
        return try {
            var handled = false
            if (accessibilityService != null) {
                handled = accessibilityService.performGlobalAction(actionId)
            }
            if (!handled && fallbackKeyCode != null) {
                sendRootKeyInput(fallbackKeyCode.toString())
                true
            } else {
                handled
            }
        } catch (e: Exception) {
            if (fallbackKeyCode != null) {
                sendRootKeyInput(fallbackKeyCode.toString())
                true
            } else {
                e.printStackTrace()
                false
            }
        }
    }

    private fun performLockScreen(accessibilityService: AccessibilityService?) {
        try {
            var handled = false
            if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                handled = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            }
            if (!handled) {
                runShellAsync("input keyevent 26")
            }
        } catch (e: Exception) {
            runShellAsync("input keyevent 26")
        }
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
        } catch (e: Exception) {
            runShellAsync("input keyevent 26")
        }
    }

    fun executeKeyStroke(shortcutSpec: String?, context: Context, accessibilityService: AccessibilityService? = null) {
        if (shortcutSpec.isNullOrBlank()) return
        val allKeyCodes = parseShortcutToKeyCodes(shortcutSpec)
        if (allKeyCodes.isNotEmpty()) {
            executeKeyCombination(allKeyCodes, shortcutSpec, context, accessibilityService)
        }
    }

    fun startShortcut(shortcutSpec: String?, context: Context, accessibilityService: AccessibilityService?) {
        if (shortcutSpec.isNullOrBlank()) return
        stopRepeatKeyRunnable()

        executeKeyStroke(shortcutSpec, context, accessibilityService)

        val runnable = object : Runnable {
            override fun run() {
                executeKeyStroke(shortcutSpec, context, accessibilityService)
                handler.postDelayed(this, 50L)
            }
        }
        repeatKeyRunnable = runnable
        handler.postDelayed(runnable, 250L)
    }

    fun stopShortcut(shortcutSpec: String?, context: Context, accessibilityService: AccessibilityService?) {
        if (shortcutSpec.isNullOrBlank()) return
        stopRepeatKeyRunnable()
    }

    fun getShortcutInfo(shortcutSpec: String?): Triple<Int, List<Int>, Int> {
        val safeSpec = shortcutSpec ?: ""
        val upperSpec = safeSpec.uppercase()
        val ctrl = upperSpec.contains("CTRL") || upperSpec.contains("CONTROL")
        val alt = upperSpec.contains("ALT") || upperSpec.contains("OPTION")
        val shift = upperSpec.contains("SHIFT")

        val tokens = tokenizeShortcut(safeSpec)
        val shiftedSymbols = setOf('!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '{', '}', '|', ':', '"', '<', '>', '?', '~')
        val hasShiftedSymbol = tokens.any { token -> token.any { c -> c in shiftedSymbols } || (token == "+" && tokens.size == 1) }

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
        if (shift || hasShiftedSymbol) {
            metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
            if (!activeModifiers.contains(KeyEvent.KEYCODE_SHIFT_LEFT)) {
                activeModifiers.add(KeyEvent.KEYCODE_SHIFT_LEFT)
            }
        }

        val allKeyCodes = parseShortcutToKeyCodes(safeSpec)
        val modifierKeyCodes = setOf(
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT
        )
        val targetKeyCode = allKeyCodes.firstOrNull { it !in modifierKeyCodes }
            ?: allKeyCodes.lastOrNull()
            ?: KeyEvent.KEYCODE_UNKNOWN

        return Triple(metaState, activeModifiers.distinct(), targetKeyCode)
    }

    fun executeShortcut(shortcutSpec: String?, context: Context, accessibilityService: AccessibilityService? = null) {
        executeKeyStroke(shortcutSpec, context, accessibilityService)
    }

    fun executePenEraserToggle(presetSpec: String?, context: Context, accessibilityService: AccessibilityService? = null) {
        val safeSpec = presetSpec ?: ""
        val trimmed = safeSpec.trim()
        if (trimmed.contains("CTRL", ignoreCase = true) || trimmed.contains("ALT", ignoreCase = true) || trimmed.contains("SHIFT", ignoreCase = true)) {
            executeShortcut(trimmed, context, accessibilityService)
        } else {
            val keyCodes = parsePenEraserPreset(safeSpec)
            executeKeyCombo(keyCodes)
        }
    }

    enum class TextShortcutType { SELECT_ALL, COPY, PASTE, CUT }

    fun getTextShortcutType(shortcutSpec: String?): TextShortcutType? {
        if (shortcutSpec.isNullOrBlank()) return null
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

    private fun tryAccessibilityNodeAction(shortcutSpec: String?, accessibilityService: AccessibilityService?): Boolean {
        if (accessibilityService == null || shortcutSpec.isNullOrBlank()) return false
        val type = getTextShortcutType(shortcutSpec) ?: return false

        return try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return false
            val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: rootNode.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                ?: findFocusedOrEditableNode(rootNode)
                ?: return false

            when (type) {
                TextShortcutType.SELECT_ALL -> {
                    val textLength = focusedNode.text?.length ?: 1000000
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

    private fun findFocusedOrEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused || node.isAccessibilityFocused) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFocusedOrEditableNode(child)
            if (result != null) return result
        }
        return null
    }

    fun parsePenEraserPreset(presetSpec: String?): List<Int> {
        val safeSpec = presetSpec ?: ""
        val trimmed = safeSpec.trim()
        return when {
            trimmed.equals("CTRL+E", ignoreCase = true) -> listOf(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_E)
            trimmed.equals("E", ignoreCase = true) -> listOf(KeyEvent.KEYCODE_E)
            trimmed.contains("202") || trimmed.contains("Stylus", ignoreCase = true) -> listOf(202)
            else -> parseShortcutToKeyCodes(trimmed)
        }
    }

    fun tokenizeShortcut(shortcut: String?): List<String> {
        val safeSpec = shortcut ?: ""
        val trimmed = safeSpec.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length == 1) return listOf(trimmed)

        val upper = trimmed.uppercase()
        val knownSingleKeys = setOf(
            "SPACE", "BACKSPACE", "BKSP", "DELETE", "FORWARD_DEL", "DEL",
            "ENTER", "RETURN", "TAB", "ESCAPE", "ESC",
            "UP", "DOWN", "LEFT", "RIGHT",
            "ARROW_UP", "ARROW_DOWN", "ARROW_LEFT", "ARROW_RIGHT",
            "UP_ARROW", "DOWN_ARROW", "LEFT_ARROW", "RIGHT_ARROW",
            "DPAD_UP", "DPAD_DOWN", "DPAD_LEFT", "DPAD_RIGHT",
            "PAGE_UP", "PAGE_DOWN", "PAGEUP", "PAGEDOWN",
            "COPY", "PASTE", "CUT"
        )
        if (upper in knownSingleKeys) return listOf(trimmed)
        if (upper.startsWith("KEYCODE_")) return listOf(trimmed)

        val rawTokens = trimmed.split("+", " ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (rawTokens.isEmpty() && trimmed.contains("+")) {
            return listOf("+")
        }

        if (trimmed.endsWith("+") && rawTokens.isNotEmpty() && rawTokens.last().uppercase() in setOf("CTRL", "CONTROL", "ALT", "OPTION", "SHIFT", "META", "CMD", "COMMAND")) {
            return rawTokens + "+"
        }

        return if (rawTokens.isNotEmpty()) rawTokens else listOf(trimmed)
    }

    fun parseTokenToKeyCodes(token: String?): List<Int> {
        val safeToken = token ?: ""
        val upper = safeToken.uppercase()
        return when (upper) {
            "CTRL", "CONTROL" -> listOf(KeyEvent.KEYCODE_CTRL_LEFT)
            "ALT", "OPTION" -> listOf(KeyEvent.KEYCODE_ALT_LEFT)
            "SHIFT" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT)
            "META", "CMD", "COMMAND" -> listOf(KeyEvent.KEYCODE_META_LEFT)
            "COPY" -> listOf(KeyEvent.KEYCODE_COPY)
            "PASTE" -> listOf(KeyEvent.KEYCODE_PASTE)
            "CUT" -> listOf(KeyEvent.KEYCODE_CUT)
            "SPACE" -> listOf(KeyEvent.KEYCODE_SPACE)
            "BACKSPACE", "BKSP" -> listOf(KeyEvent.KEYCODE_DEL)
            "DELETE", "DEL", "FORWARD_DEL" -> listOf(KeyEvent.KEYCODE_FORWARD_DEL)
            "ENTER", "RETURN" -> listOf(KeyEvent.KEYCODE_ENTER)
            "TAB" -> listOf(KeyEvent.KEYCODE_TAB)
            "ESCAPE", "ESC" -> listOf(KeyEvent.KEYCODE_ESCAPE)
            "UP", "ARROW_UP", "UP_ARROW", "DPAD_UP" -> listOf(KeyEvent.KEYCODE_DPAD_UP)
            "DOWN", "ARROW_DOWN", "DOWN_ARROW", "DPAD_DOWN" -> listOf(KeyEvent.KEYCODE_DPAD_DOWN)
            "LEFT", "ARROW_LEFT", "LEFT_ARROW", "DPAD_LEFT" -> listOf(KeyEvent.KEYCODE_DPAD_LEFT)
            "RIGHT", "ARROW_RIGHT", "RIGHT_ARROW", "DPAD_RIGHT" -> listOf(KeyEvent.KEYCODE_DPAD_RIGHT)
            "PAGE_UP", "PAGEUP" -> listOf(KeyEvent.KEYCODE_PAGE_UP)
            "PAGE_DOWN", "PAGEDOWN" -> listOf(KeyEvent.KEYCODE_PAGE_DOWN)
            "0" -> listOf(KeyEvent.KEYCODE_0)
            "1" -> listOf(KeyEvent.KEYCODE_1)
            "2" -> listOf(KeyEvent.KEYCODE_2)
            "3" -> listOf(KeyEvent.KEYCODE_3)
            "4" -> listOf(KeyEvent.KEYCODE_4)
            "5" -> listOf(KeyEvent.KEYCODE_5)
            "6" -> listOf(KeyEvent.KEYCODE_6)
            "7" -> listOf(KeyEvent.KEYCODE_7)
            "8" -> listOf(KeyEvent.KEYCODE_8)
            "9" -> listOf(KeyEvent.KEYCODE_9)
            "," -> listOf(KeyEvent.KEYCODE_COMMA)
            "." -> listOf(KeyEvent.KEYCODE_PERIOD)
            "/" -> listOf(KeyEvent.KEYCODE_SLASH)
            ";" -> listOf(KeyEvent.KEYCODE_SEMICOLON)
            "'" -> listOf(KeyEvent.KEYCODE_APOSTROPHE)
            "[" -> listOf(KeyEvent.KEYCODE_LEFT_BRACKET)
            "]" -> listOf(KeyEvent.KEYCODE_RIGHT_BRACKET)
            "\\" -> listOf(KeyEvent.KEYCODE_BACKSLASH)
            "-" -> listOf(KeyEvent.KEYCODE_MINUS)
            "=" -> listOf(KeyEvent.KEYCODE_EQUALS)
            "`" -> listOf(KeyEvent.KEYCODE_GRAVE)
            "!" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_1)
            "@" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_2)
            "#" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_3)
            "$" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_4)
            "%" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_5)
            "^" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_6)
            "&" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_7)
            "*" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_8)
            "(" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_9)
            ")" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_0)
            "_" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_MINUS)
            "+" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_EQUALS)
            "{" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_LEFT_BRACKET)
            "}" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_RIGHT_BRACKET)
            "|" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_BACKSLASH)
            ":" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SEMICOLON)
            "\"" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_APOSTROPHE)
            "<" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_COMMA)
            ">" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_PERIOD)
            "?" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SLASH)
            "~" -> listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_GRAVE)
            else -> {
                if (upper.length == 1 && upper[0] in 'A'..'Z') {
                    listOf(KeyEvent.KEYCODE_A + (upper[0] - 'A'))
                } else {
                    val codeFromInt = upper.toIntOrNull()
                    if (codeFromInt != null) {
                        listOf(codeFromInt)
                    } else {
                        try {
                            val keyName = if (upper.startsWith("KEYCODE_")) upper else "KEYCODE_$upper"
                            val code = KeyEvent.keyCodeFromString(keyName)
                            if (code != KeyEvent.KEYCODE_UNKNOWN) listOf(code) else emptyList()
                        } catch (e: Throwable) {
                            emptyList()
                        }
                    }
                }
            }
        }
    }

    fun parseShortcutToKeyCodes(shortcut: String?): List<Int> {
        if (shortcut.isNullOrBlank()) return listOf(KeyEvent.KEYCODE_C)
        val tokens = tokenizeShortcut(shortcut)
        val result = tokens.flatMap { token -> parseTokenToKeyCodes(token) }
        return if (result.isNotEmpty()) result else listOf(KeyEvent.KEYCODE_C)
    }

    fun executeKeyCombo(keyCodes: List<Int>) {
        if (keyCodes.isNotEmpty()) {
            executeKeyCombination(keyCodes, null, null, null)
        }
    }

    fun executeKeyCombination(keyCodes: List<Int>): Boolean {
        return executeKeyCombination(keyCodes, null, null, null)
    }

    fun executeKeyCombination(
        keyCodes: List<Int>,
        shortcutSpec: String? = null,
        context: Context? = null,
        accessibilityService: AccessibilityService? = null
    ): Boolean {
        if (keyCodes.isEmpty()) return false

        if (!shortcutSpec.isNullOrBlank() && tryAccessibilityNodeAction(shortcutSpec, accessibilityService)) {
            return true
        }

        val modifierKeyCodes = setOf(
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT
        )

        val activeModifiers = keyCodes.filter { it in modifierKeyCodes }.distinct()
        var targetKeyCodes = keyCodes.filter { it !in modifierKeyCodes }

        if (targetKeyCodes.isEmpty() && activeModifiers.isNotEmpty()) {
            targetKeyCodes = listOf(activeModifiers.last())
        }

        var metaState = 0
        for (mod in activeModifiers) {
            metaState = metaState or getMetaForKeyCode(mod)
        }

        val targetContext = context ?: accessibilityService
        val injectionSuccess = tryInjectInputEvents(activeModifiers, targetKeyCodes, metaState)
        if (injectionSuccess) {
            return true
        }

        try {
            val packageName = targetContext?.packageName ?: "com.yaawaaa.penbatteryremapper"
            executeShellCommand("pm grant $packageName android.permission.INJECT_EVENTS")
            val retrySuccess = tryInjectInputEvents(activeModifiers, targetKeyCodes, metaState)
            if (retrySuccess) {
                return true
            }
        } catch (_: Exception) {}

        return executeRootShellKeyCombo(activeModifiers, targetKeyCodes, keyCodes)
    }

    private fun tryInjectInputEvents(
        modifiers: List<Int>,
        targets: List<Int>,
        metaState: Int
    ): Boolean {
        return try {
            val im = Class.forName("android.hardware.input.InputManager")
                .getMethod("getInstance")
                .invoke(null) ?: return false
            val injectMethod = Class.forName("android.hardware.input.InputManager")
                .getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)

            val downTime = SystemClock.uptimeMillis()
            var currentMeta = 0

            for (modCode in modifiers) {
                currentMeta = currentMeta or getMetaForKeyCode(modCode)
                val downEvent = KeyEvent(
                    downTime,
                    SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_DOWN,
                    modCode,
                    0,
                    currentMeta,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    KeyEvent.FLAG_FROM_SYSTEM,
                    InputDevice.SOURCE_KEYBOARD
                )
                val res = injectMethod.invoke(im, downEvent, 0) as? Boolean ?: false
                if (!res) return false
            }

            val effectiveMeta = if (metaState != 0) metaState else currentMeta
            for (targetCode in targets) {
                val eventTime = SystemClock.uptimeMillis()
                val targetDown = KeyEvent(
                    downTime,
                    eventTime,
                    KeyEvent.ACTION_DOWN,
                    targetCode,
                    0,
                    effectiveMeta,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    KeyEvent.FLAG_FROM_SYSTEM,
                    InputDevice.SOURCE_KEYBOARD
                )
                val resDown = injectMethod.invoke(im, targetDown, 0) as? Boolean ?: false
                if (!resDown) return false

                val targetUp = KeyEvent(
                    downTime,
                    SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_UP,
                    targetCode,
                    0,
                    effectiveMeta,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    KeyEvent.FLAG_FROM_SYSTEM,
                    InputDevice.SOURCE_KEYBOARD
                )
                val resUp = injectMethod.invoke(im, targetUp, 0) as? Boolean ?: false
                if (!resUp) return false
            }

            for (modCode in modifiers.reversed()) {
                currentMeta = currentMeta and getMetaForKeyCode(modCode).inv()
                val upEvent = KeyEvent(
                    downTime,
                    SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_UP,
                    modCode,
                    0,
                    currentMeta,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    KeyEvent.FLAG_FROM_SYSTEM,
                    InputDevice.SOURCE_KEYBOARD
                )
                val res = injectMethod.invoke(im, upEvent, 0) as? Boolean ?: false
                if (!res) return false
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getMetaForKeyCode(keyCode: Int): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT ->
                KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT ->
                KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT ->
                KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT ->
                KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
            else -> 0
        }
    }

    private fun executeRootShellKeyCombo(
        modifiers: List<Int>,
        targets: List<Int>,
        allKeyCodes: List<Int>
    ): Boolean {
        val allCodesStr = allKeyCodes.joinToString(" ")
        if (modifiers.isNotEmpty() && targets.isNotEmpty()) {
            val comboStr = (modifiers + targets).joinToString(" ")
            try {
                val writer = getRootWriter()
                if (writer != null) {
                    writer.write("input keycombination $comboStr\n")
                    writer.flush()
                    return true
                } else {
                    val proc = ProcessBuilder("su", "-c", "input keycombination $comboStr").redirectErrorStream(true).start()
                    val drainer = Thread {
                        try {
                            val buf = ByteArray(1024)
                            val inStream = proc.inputStream
                            while (inStream.read(buf) != -1) { /* drain */ }
                        } catch (_: Exception) {}
                    }.apply { isDaemon = true; start() }
                    val finished = proc.waitFor(1500, TimeUnit.MILLISECONDS)
                    if (!finished) proc.destroy()
                    drainer.interrupt()
                    if (finished && proc.exitValue() == 0) return true
                }
            } catch (_: Exception) {}

            try {
                val proc = ProcessBuilder("su", "-c", "cmd input keycombination $comboStr").redirectErrorStream(true).start()
                val drainer = Thread {
                    try {
                        val buf = ByteArray(1024)
                        val inStream = proc.inputStream
                        while (inStream.read(buf) != -1) { /* drain */ }
                    } catch (_: Exception) {}
                }.apply { isDaemon = true; start() }
                val finished = proc.waitFor(1500, TimeUnit.MILLISECONDS)
                if (!finished) proc.destroy()
                drainer.interrupt()
                if (finished && proc.exitValue() == 0) return true
            } catch (_: Exception) {}
        }

        return try {
            val writer = getRootWriter()
            if (writer != null) {
                writer.write("input keyevent $allCodesStr\n")
                writer.flush()
                true
            } else {
                val process = ProcessBuilder("su", "-c", "input keyevent $allCodesStr").redirectErrorStream(true).start()
                val drainer = Thread {
                    try {
                        val buf = ByteArray(1024)
                        val inStream = process.inputStream
                        while (inStream.read(buf) != -1) { /* drain */ }
                    } catch (_: Exception) {}
                }.apply { isDaemon = true; start() }
                val finished = process.waitFor(1500, TimeUnit.MILLISECONDS)
                if (!finished) process.destroy()
                drainer.interrupt()
                finished && process.exitValue() == 0
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun toggleSplitScreen(accessibilityService: AccessibilityService?) {
        try {
            var handled = false
            if (accessibilityService != null) {
                handled = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            }
            if (!handled) {
                sendRootKeyInput("${KeyEvent.KEYCODE_WINDOW}")
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun openNotifications(accessibilityService: AccessibilityService?) {
        try {
            var handled = false
            if (accessibilityService != null) {
                handled = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            }
            if (!handled) {
                runShellAsync("cmd statusbar expand-notifications")
            }
        } catch (e: Exception) {
            runShellAsync("cmd statusbar expand-notifications")
        }
    }

    private fun openQuickSettings(accessibilityService: AccessibilityService?) {
        try {
            var handled = false
            if (accessibilityService != null) {
                handled = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            }
            if (!handled) {
                runShellAsync("cmd statusbar expand-settings")
            }
        } catch (e: Exception) {
            runShellAsync("cmd statusbar expand-settings")
        }
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
        } catch (e: Exception) {
            runShellAsync("settings put system accelerometer_rotation 1")
        }
    }

    private fun clearNotifications(accessibilityService: AccessibilityService?) {
        try {
            var handled = false
            if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                handled = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            }
            if (!handled) {
                runShellAsync("cmd statusbar collapse")
            }
        } catch (e: Exception) {
            runShellAsync("cmd statusbar collapse")
        }
    }

    private fun killForegroundApp() {
        executorScope.launch {
            try {
                val activePkg = PenRemapperAccessibilityService.activeForegroundPackage
                if (activePkg.isNotBlank()) {
                    executeShellCommand("am force-stop $activePkg")
                } else {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "dumpsys window | grep -E mCurrentFocus"))
                    val reader = process.inputStream.bufferedReader()
                    val line = reader.readLine()
                    val pkg = line?.substringAfter("{")?.substringBefore("/")?.split(" ")?.lastOrNull()
                    if (!pkg.isNullOrBlank()) {
                        executeShellCommand("am force-stop $pkg")
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun toggleMute(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioManager.adjustVolume(AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true)
                }
            } else {
                sendRootKeyInput("${KeyEvent.KEYCODE_VOLUME_MUTE}")
            }
        } catch (e: Exception) {
            sendRootKeyInput("${KeyEvent.KEYCODE_VOLUME_MUTE}")
        }
    }

    private fun dispatchMediaKey(context: Context, keyCode: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val eventTime = SystemClock.uptimeMillis()
                audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
                audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
            } else {
                sendRootKeyInput(keyCode.toString())
            }
        } catch (e: Exception) {
            sendRootKeyInput(keyCode.toString())
        }
    }

    private fun adjustVolume(context: Context, direction: Int) {
        val keyStr = if (direction == AudioManager.ADJUST_RAISE) "24" else "25"
        var adjusted = false
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                audioManager.adjustSuggestedStreamVolume(direction, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)
                adjusted = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (!adjusted) {
            sendRootKeyInput(keyStr)
        }
    }

    private fun takeScreenshot(accessibilityService: AccessibilityService?) {
        try {
            var handled = false
            if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                handled = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            }
            if (!handled) {
                sendRootKeyInput("120")
            }
        } catch (e: Exception) {
            sendRootKeyInput("120")
        }
    }

    private fun toggleFlashlight(context: Context) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                try {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } catch (e: Exception) { false }
            } ?: cameraManager.cameraIdList.firstOrNull() ?: return

            isTorchOn = !isTorchOn
            cameraManager.setTorchMode(cameraId, isTorchOn)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun launchApp(context: Context, packageName: String?) {
        if (packageName.isNullOrBlank()) return
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null) {
                context.startActivity(intent)
            } else {
                runShellAsync("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
            }
        } catch (e: Exception) {
            runShellAsync("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
        }
    }

    private fun getTargetWindowBounds(
        accessibilityService: AccessibilityService?,
        context: Context?,
        targetPackage: String? = null
    ): Rect {
        val targetBounds = Rect()
        var boundsSet = false

        if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val windowList = accessibilityService.windows
                val activePkg = if (!targetPackage.isNullOrBlank() &&
                    !targetPackage.contains(",") &&
                    !targetPackage.startsWith("CENTER:") &&
                    !targetPackage.contains(" ")
                ) {
                    targetPackage
                } else {
                    PenRemapperAccessibilityService.activeForegroundPackage
                }
                var matchedWindow: AccessibilityWindowInfo? = null

                if (!windowList.isNullOrEmpty()) {
                    val appWindows = windowList.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }

                    fun getWinPkg(win: AccessibilityWindowInfo): String? {
                        var pkg = PenRemapperAccessibilityService.windowPackageMap[win.id]
                        if (pkg.isNullOrBlank()) {
                            var rootNode: AccessibilityNodeInfo? = null
                            try {
                                rootNode = win.root
                                pkg = rootNode?.packageName?.toString()
                                if (!pkg.isNullOrBlank()) {
                                    PenRemapperAccessibilityService.windowPackageMap[win.id] = pkg
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                @Suppress("DEPRECATION")
                                rootNode?.recycle()
                            }
                        }
                        return pkg
                    }

                    if (activePkg.isNotBlank()) {
                        // Priority 1: Window matching activePkg that is focused or active
                        matchedWindow = appWindows.firstOrNull { win ->
                            (win.isFocused || win.isActive) && getWinPkg(win)?.equals(activePkg, ignoreCase = true) == true
                        }

                        // Priority 2: Any window matching activePkg
                        if (matchedWindow == null) {
                            matchedWindow = appWindows.firstOrNull { win ->
                                getWinPkg(win)?.equals(activePkg, ignoreCase = true) == true
                            }
                        }
                    }

                    // Priority 3: Actively focused application window
                    if (matchedWindow == null) {
                        matchedWindow = appWindows.firstOrNull { it.isFocused }
                    }

                    // Priority 4: Active application window
                    if (matchedWindow == null) {
                        matchedWindow = appWindows.firstOrNull { it.isActive }
                    }

                    // Priority 5: Any application window
                    if (matchedWindow == null) {
                        matchedWindow = appWindows.firstOrNull()
                    }
                }

                if (matchedWindow != null) {
                    matchedWindow.getBoundsInScreen(targetBounds)
                    if (targetBounds.rectW > 0 && targetBounds.rectH > 0) {
                        boundsSet = true
                    }
                }

                if (!boundsSet) {
                    val rootNode = accessibilityService.rootInActiveWindow
                    if (rootNode != null) {
                        rootNode.getBoundsInScreen(targetBounds)
                        @Suppress("DEPRECATION")
                        rootNode.recycle()
                        if (targetBounds.rectW > 0 && targetBounds.rectH > 0) {
                            boundsSet = true
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!boundsSet) {
            val ctx = accessibilityService ?: context
            if (ctx != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    val bounds = wm?.currentWindowMetrics?.bounds
                    if (bounds != null && (bounds.right - bounds.left) > 0 && (bounds.bottom - bounds.top) > 0) {
                        targetBounds.left = bounds.left
                        targetBounds.top = bounds.top
                        targetBounds.right = bounds.right
                        targetBounds.bottom = bounds.bottom
                        boundsSet = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (!boundsSet) {
            val ctx = accessibilityService ?: context
            val dm = ctx?.resources?.displayMetrics
            val w = dm?.widthPixels ?: 1080
            val h = dm?.heightPixels ?: 2400
            targetBounds.left = 0
            targetBounds.top = 0
            targetBounds.right = w
            targetBounds.bottom = h
        }

        return targetBounds
    }

    private fun executeSwipe(
        action: RemapAction,
        accessibilityService: AccessibilityService?,
        context: Context? = null,
        appPackage: String = ""
    ) {
        try {
            val targetBounds = getTargetWindowBounds(accessibilityService, context, appPackage)
            val centerX = targetBounds.rectCenterX
            val centerY = targetBounds.rectCenterY
            val width = targetBounds.rectW.toFloat()
            val height = targetBounds.rectH.toFloat()

            val fromX: Float
            val fromY: Float
            val toX: Float
            val toY: Float

            when (action) {
                RemapAction.SWIPE_UP -> {
                    fromX = centerX; fromY = targetBounds.top + height * 0.70f
                    toX = centerX; toY = targetBounds.top + height * 0.30f
                }
                RemapAction.SWIPE_DOWN -> {
                    fromX = centerX; fromY = targetBounds.top + height * 0.30f
                    toX = centerX; toY = targetBounds.top + height * 0.70f
                }
                RemapAction.SWIPE_LEFT -> {
                    fromX = targetBounds.left + width * 0.70f; fromY = centerY
                    toX = targetBounds.left + width * 0.30f; toY = centerY
                }
                RemapAction.SWIPE_RIGHT -> {
                    fromX = targetBounds.left + width * 0.30f; fromY = centerY
                    toX = targetBounds.left + width * 0.70f; toY = centerY
                }
                else -> return
            }

            var dispatched = false
            if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val path = Path().apply {
                    moveTo(fromX, fromY)
                    lineTo(toX, toY)
                }
                val builder = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, 250L))
                dispatched = accessibilityService.dispatchGesture(builder.build(), null, null)
            }
            if (!dispatched) {
                runShellAsync("input swipe ${fromX.toInt()} ${fromY.toInt()} ${toX.toInt()} ${toY.toInt()} 250")
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun resolveRotatedCoordinates(
        payload: String?,
        context: Context?,
        accessibilityService: AccessibilityService? = null
    ): Pair<Float, Float>? {
        if (payload.isNullOrBlank()) return null

        val cleanPayload = payload.removePrefix("CENTER:")
        val parts = cleanPayload.split(",", "x", "X", " ", ";", ":")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toFloatOrNull() }
        if (parts.size < 2) return null

        val rawX = parts[0]
        val rawY = parts[1]

        val defaultW = if (parts.size >= 3) parts[2].toInt() else 1080
        val defaultH = if (parts.size >= 4) parts[3].toInt() else 2400
        val targetBounds = Rect()
        if (accessibilityService != null || context != null) {
            val b = getTargetWindowBounds(accessibilityService, context, payload)
            targetBounds.left = b.left
            targetBounds.top = b.top
            targetBounds.right = b.right
            targetBounds.bottom = b.bottom
        } else {
            targetBounds.left = 0
            targetBounds.top = 0
            targetBounds.right = defaultW
            targetBounds.bottom = defaultH
        }
        val targetW = targetBounds.rectW.toFloat()
        val targetH = targetBounds.rectH.toFloat()
        if (targetW <= 0f || targetH <= 0f) return null

        val refW = if (parts.size >= 5) parts[2] else if (parts.size == 4) parts[2] else targetW
        val refH = if (parts.size >= 5) parts[3] else if (parts.size == 4) parts[3] else targetH
        if (refW <= 0f || refH <= 0f) return null

        // Android UI layouts are responsive; buttons and controls maintain their relative
        // normalized positions within the app window rather than rotating like a fixed sheet of paper.
        val normX = (rawX / refW).coerceIn(0f, 1f)
        val normY = (rawY / refH).coerceIn(0f, 1f)

        val finalX = (targetBounds.left + normX * targetW).coerceIn(
            targetBounds.left.toFloat(),
            (targetBounds.left + targetW - 1f).coerceAtLeast(targetBounds.left.toFloat())
        )
        val finalY = (targetBounds.top + normY * targetH).coerceIn(
            targetBounds.top.toFloat(),
            (targetBounds.top + targetH - 1f).coerceAtLeast(targetBounds.top.toFloat())
        )

        return Pair(finalX, finalY)
    }

    fun executeScreenTap(
        isLeft: Boolean?,
        accessibilityService: AccessibilityService?,
        context: Context? = null,
        appPackage: String = "",
        useCustomPositions: Boolean = true
    ) {
        try {
            val customCoords = if (useCustomPositions) resolveRotatedCoordinates(appPackage, accessibilityService ?: context, accessibilityService) else null
            val x: Float
            val y: Float
            if (customCoords != null) {
                x = customCoords.first
                y = customCoords.second
            } else {
                val targetBounds = getTargetWindowBounds(accessibilityService, context, appPackage)
                val width = targetBounds.rectW.toFloat()
                x = when (isLeft) {
                    true -> targetBounds.left + width * 0.15f
                    false -> targetBounds.left + width * 0.85f
                    null -> targetBounds.rectCenterX
                }
                y = targetBounds.rectCenterY
            }

            var dispatched = false
            if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val path = Path().apply {
                    moveTo(x, y)
                    lineTo(x, y)
                }
                val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, 50L)).build()
                dispatched = accessibilityService.dispatchGesture(gesture, null, null)
            }
            if (!dispatched) {
                runShellAsync("input tap ${x.toInt()} ${y.toInt()}")
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun executeScreenDoubleTap(
        isLeft: Boolean?,
        accessibilityService: AccessibilityService?,
        context: Context? = null,
        appPackage: String = "",
        useCustomPositions: Boolean = true
    ) {
        try {
            val customCoords = if (useCustomPositions) resolveRotatedCoordinates(appPackage, accessibilityService ?: context, accessibilityService) else null
            val x: Float
            val y: Float
            if (customCoords != null) {
                x = customCoords.first
                y = customCoords.second
            } else {
                val targetBounds = getTargetWindowBounds(accessibilityService, context, appPackage)
                val width = targetBounds.rectW.toFloat()
                x = when (isLeft) {
                    true -> targetBounds.left + width * 0.15f
                    false -> targetBounds.left + width * 0.85f
                    null -> targetBounds.rectCenterX
                }
                y = targetBounds.rectCenterY
            }

            var dispatched = false
            if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val path = Path().apply {
                    moveTo(x, y)
                    lineTo(x, y)
                }
                val stroke1 = GestureDescription.StrokeDescription(path, 0L, 40L)
                val stroke2 = GestureDescription.StrokeDescription(path, 100L, 40L)
                val gesture = GestureDescription.Builder()
                    .addStroke(stroke1)
                    .addStroke(stroke2)
                    .build()
                dispatched = accessibilityService.dispatchGesture(gesture, null, null)
            }
            if (!dispatched) {
                runShellAsync("input tap ${x.toInt()} ${y.toInt()} && sleep 0.05 && input tap ${x.toInt()} ${y.toInt()}")
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun executeScreenLongPress(
        isLeft: Boolean?,
        accessibilityService: AccessibilityService?,
        context: Context? = null,
        appPackage: String = "",
        useCustomPositions: Boolean = true
    ) {
        try {
            val customCoords = if (useCustomPositions) resolveRotatedCoordinates(appPackage, accessibilityService ?: context, accessibilityService) else null
            val x: Float
            val y: Float
            if (customCoords != null) {
                x = customCoords.first
                y = customCoords.second
            } else {
                val targetBounds = getTargetWindowBounds(accessibilityService, context, appPackage)
                val width = targetBounds.rectW.toFloat()
                val height = targetBounds.rectH.toFloat()
                x = when (isLeft) {
                    true -> targetBounds.left + width * 0.25f
                    false -> targetBounds.left + width * 0.65f
                    null -> targetBounds.rectCenterX
                }
                y = targetBounds.top + height * 0.50f
            }

            var dispatched = false
            if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val path = Path().apply {
                    moveTo(x, y)
                    lineTo(x, y)
                }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0L, 600L))
                    .build()
                dispatched = accessibilityService.dispatchGesture(gesture, null, null)
            }
            if (!dispatched) {
                runShellAsync("input swipe ${x.toInt()} ${y.toInt()} ${x.toInt()} ${y.toInt()} 600")
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun startScreenHold(
        context: Context,
        isLeft: Boolean?,
        accessibilityService: AccessibilityService? = null,
        appPackage: String = "",
        useCustomPositions: Boolean = true
    ) {
        if (activeTouchDownTime != 0L) return

        val customCoords = if (useCustomPositions) resolveRotatedCoordinates(appPackage, accessibilityService ?: context, accessibilityService) else null
        if (customCoords != null) {
            activeTouchX = customCoords.first
            activeTouchY = customCoords.second
        } else {
            val targetBounds = getTargetWindowBounds(accessibilityService, context, appPackage)
            val width = targetBounds.rectW.toFloat()
            val height = targetBounds.rectH.toFloat()
            activeTouchX = when (isLeft) {
                true -> targetBounds.left + width * 0.25f
                false -> targetBounds.left + width * 0.65f
                null -> targetBounds.rectCenterX
            }
            activeTouchY = targetBounds.top + height * 0.50f
        }
        activeTouchDownTime = SystemClock.uptimeMillis()

        var dispatched = false
        if (accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val path = Path().apply {
                    moveTo(activeTouchX, activeTouchY)
                    lineTo(activeTouchX, activeTouchY)
                }
                val stroke = GestureDescription.StrokeDescription(path, 0L, 10000L, true)
                activeHoldStroke = stroke
                val gesture = GestureDescription.Builder().addStroke(stroke).build()
                dispatched = accessibilityService.dispatchGesture(gesture, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!dispatched) {
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
    }

    fun stopScreenHold(accessibilityService: AccessibilityService? = null) {
        if (activeTouchDownTime != 0L) {
            val stroke = activeHoldStroke
            if (accessibilityService != null && stroke != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val releasePath = Path().apply {
                        moveTo(activeTouchX, activeTouchY)
                        lineTo(activeTouchX, activeTouchY)
                    }
                    val releaseStroke = stroke.continueStroke(releasePath, 0L, 50L, false)
                    val releaseGesture = GestureDescription.Builder().addStroke(releaseStroke).build()
                    accessibilityService.dispatchGesture(releaseGesture, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                activeHoldStroke = null
            } else {
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
            }
            activeTouchDownTime = 0L
        }
    }

    fun parseBooleanState(payload: String?): Boolean? {
        if (payload == null) return null
        val upper = payload.trim().uppercase()
        return when {
            upper.contains(":TRUE") || upper.contains("=TRUE") || upper.endsWith(":TRUE") -> true
            upper.contains(":FALSE") || upper.contains("=FALSE") || upper.endsWith(":FALSE") -> false
            else -> null
        }
    }

    fun resolveActionToken(
        actionToken: String,
        defaultAction: String = "com.yaawaaa.lockscreenawake.action.TOGGLE"
    ): String {
        val trimmedToken = actionToken.trim()
        val upperToken = trimmedToken.uppercase()
        return when {
            upperToken.isBlank() -> defaultAction
            upperToken == "TOGGLE" || upperToken == "COM.YAAWAAA.LOCKSCREENAWAKE.ACTION.TOGGLE" -> "com.yaawaaa.lockscreenawake.action.TOGGLE"
            upperToken == "START" || upperToken == "COM.YAAWAAA.LOCKSCREENAWAKE.ACTION.START" -> "com.yaawaaa.lockscreenawake.action.START"
            upperToken == "STOP" || upperToken == "COM.YAAWAAA.LOCKSCREENAWAKE.ACTION.STOP" -> "com.yaawaaa.lockscreenawake.action.STOP"
            upperToken == "QUERY_STATE" || upperToken == "COM.YAAWAAA.LOCKSCREENAWAKE.ACTION.QUERY_STATE" -> "com.yaawaaa.lockscreenawake.action.QUERY_STATE"
            upperToken.startsWith("SET_STATE") || upperToken.startsWith("COM.YAAWAAA.LOCKSCREENAWAKE.ACTION.SET_STATE") -> "com.yaawaaa.lockscreenawake.action.SET_STATE"
            else -> trimmedToken
        }
    }

    fun triggerLockscreenAwake(context: Context, appPackage: String? = "") {
        val safePkg = appPackage ?: ""
        dispatchExternalAppIntent(
            context = context,
            payload = safePkg,
            defaultPackage = "com.yaawaaa.lockscreenawake",
            defaultAction = "com.yaawaaa.lockscreenawake.action.TOGGLE"
        )
    }

    fun triggerExternalApp(context: Context, appPackage: String? = "") {
        dispatchExternalAppIntent(
            context = context,
            payload = appPackage ?: "",
            defaultPackage = "com.yaawaaa.lockscreenawake",
            defaultAction = "com.yaawaaa.lockscreenawake.action.TOGGLE"
        )
    }

    fun dispatchExternalAppIntent(
        context: Context,
        payload: String? = "",
        defaultPackage: String = "com.yaawaaa.lockscreenawake",
        defaultAction: String = "com.yaawaaa.lockscreenawake.action.TOGGLE"
    ) {
        val safePayload = payload ?: ""
        val (targetPackage, targetAction) = parsePackageAndAction(safePayload, defaultPackage, defaultAction)
        val booleanState = parseBooleanState(safePayload)

        val broadcastIntent = Intent(targetAction).apply {
            setPackage(targetPackage)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES) // 0x00000020
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)     // 0x10000000
            if (booleanState != null) {
                putExtra("state", booleanState)
            }
            if (targetPackage == "com.yaawaaa.lockscreenawake") {
                component = ComponentName("com.yaawaaa.lockscreenawake", "com.yaawaaa.lockscreenawake.ExternalControlReceiver")
            }
        }

        try {
            context.sendBroadcast(broadcastIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Root Fallback if broadcast dispatch failed
            val extraArg = if (booleanState != null) " --ez state $booleanState" else ""
            if (targetPackage == "com.yaawaaa.lockscreenawake") {
                runShellAsync("am broadcast -a $targetAction -n com.yaawaaa.lockscreenawake/.ExternalControlReceiver --include-stopped-packages -f 0x10000020$extraArg")
            } else {
                runShellAsync("am broadcast -a $targetAction -p $targetPackage --include-stopped-packages -f 0x10000020$extraArg")
            }
        }
    }

    fun parsePackageAndAction(
        payload: String?,
        defaultPackage: String = "com.yaawaaa.lockscreenawake",
        defaultAction: String = "com.yaawaaa.lockscreenawake.action.TOGGLE"
    ): Pair<String, String> {
        val safePayload = payload ?: ""
        val trimmed = safePayload.trim()
        if (trimmed.isBlank()) {
            return Pair(defaultPackage, resolveActionToken(defaultAction, defaultAction))
        }

        val upper = trimmed.uppercase()
        if (upper == "TOGGLE" || upper == "START" || upper == "STOP" || upper == "QUERY_STATE" || upper.startsWith("SET_STATE")) {
            return Pair(defaultPackage, resolveActionToken(trimmed, defaultAction))
        }

        return when {
            trimmed.contains("/") -> {
                val parts = trimmed.split("/", limit = 2)
                val pkg = parts[0].trim().ifBlank { defaultPackage }
                val rawAct = parts[1].trim().ifBlank { defaultAction }
                val act = resolveActionToken(rawAct, defaultAction)
                Pair(pkg, act)
            }
            trimmed.contains(":") -> {
                val parts = trimmed.split(":", limit = 2)
                val firstPart = parts[0].trim()
                val secondPart = parts[1].trim()
                if (firstPart.contains(".")) {
                    val pkg = firstPart.ifBlank { defaultPackage }
                    val rawAct = secondPart.ifBlank { defaultAction }
                    val act = resolveActionToken(rawAct, defaultAction)
                    Pair(pkg, act)
                } else {
                    Pair(defaultPackage, resolveActionToken(trimmed, defaultAction))
                }
            }
            trimmed.contains(".") && !trimmed.contains("ACTION", ignoreCase = true) && !trimmed.contains("TOGGLE", ignoreCase = true) -> {
                Pair(trimmed, resolveActionToken(defaultAction, defaultAction))
            }
            else -> {
                Pair(defaultPackage, resolveActionToken(trimmed, defaultAction))
            }
        }
    }
}
