package com.yaawaaa.penbatteryremapper.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.yaawaaa.penbatteryremapper.remapper.ActionExecutor
import com.yaawaaa.penbatteryremapper.remapper.AppProfile
import com.yaawaaa.penbatteryremapper.remapper.KeyEventInfo
import com.yaawaaa.penbatteryremapper.remapper.RemapAction
import com.yaawaaa.penbatteryremapper.remapper.RemapperRepository
import com.yaawaaa.penbatteryremapper.remapper.RemapperSettings
import com.yaawaaa.penbatteryremapper.remapper.RootHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class ResolvedGestureAction(
    val action1: RemapAction,
    val payload1: String,
    val action2: RemapAction,
    val payload2: String
)

class PenRemapperAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var repository: RemapperRepository
    private var currentSettings = RemapperSettings()
    private var currentAppProfiles: List<AppProfile> = emptyList()
    private var activeForegroundPackage: String = ""

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

    companion object {
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
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isInstanceRunning = true
        repository = RemapperRepository(applicationContext)

        serviceScope.launch { repository.remapperSettings.collect { currentSettings = it } }
        serviceScope.launch { repository.appProfiles.collect { currentAppProfiles = it } }

        serviceScope.launch(Dispatchers.IO) {
            RootHelper.protectCurrentProcessFromLMK()
            RootHelper.protectProcessFromBackgroundKill(packageName)
        }
        AirCommandOverlayService.ensureServiceRunning(applicationContext)
    }

    override fun onDestroy() {
        isInstanceRunning = false
        stopAllTimers()
        super.onDestroy()
    }

    private fun stopAllTimers() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val type = event?.eventType
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrBlank()) {
                val myPkg = applicationContext.packageName
                if (pkg != myPkg && pkg != "android" && !pkg.contains("systemui", ignoreCase = true)) {
                    activeForegroundPackage = pkg
                }
            }
        }
    }

    override fun onInterrupt() {}

    private data class ActionTuple(
        val action1: RemapAction,
        val payload1: String,
        val action2: RemapAction,
        val payload2: String
    )

    private fun resolveGestureAction(count: Int, isLongPress: Boolean, isPrimary: Boolean): ResolvedGestureAction {
        val profile = currentAppProfiles.find { it.packageName == activeForegroundPackage }
        val s = currentSettings

        val pTuple = if (profile != null) {
            if (isPrimary) {
                if (isLongPress) ActionTuple(profile.primaryLongPressAction, profile.primaryLongPressShortcut.ifBlank { profile.primaryAppPackage }, profile.primaryLongPressAction2, profile.primaryLongPressAppPackage2)
                else if (count == 1) ActionTuple(profile.primaryClickAction, profile.primaryClickShortcut.ifBlank { profile.primaryAppPackage }, profile.primaryClickAction2, profile.primaryClickAppPackage2)
                else if (count == 2) ActionTuple(profile.primaryDoubleClickAction, profile.primaryDoubleClickShortcut.ifBlank { profile.primaryAppPackage }, profile.primaryDoubleClickAction2, profile.primaryDoubleClickAppPackage2)
                else ActionTuple(profile.primaryTripleClickAction, profile.primaryTripleClickShortcut.ifBlank { profile.primaryAppPackage }, profile.primaryTripleClickAction2, profile.primaryTripleClickAppPackage2)
            } else {
                if (isLongPress) ActionTuple(profile.secondaryLongPressAction, profile.secondaryLongPressShortcut.ifBlank { profile.secondaryAppPackage }, profile.secondaryLongPressAction2, profile.secondaryLongPressAppPackage2)
                else if (count == 1) ActionTuple(profile.secondaryClickAction, profile.secondaryClickShortcut.ifBlank { profile.secondaryAppPackage }, profile.secondaryClickAction2, profile.secondaryClickAppPackage2)
                else if (count == 2) ActionTuple(profile.secondaryDoubleClickAction, profile.secondaryDoubleClickShortcut.ifBlank { profile.secondaryAppPackage }, profile.secondaryDoubleClickAction2, profile.secondaryDoubleClickAppPackage2)
                else ActionTuple(profile.secondaryTripleClickAction, profile.secondaryTripleClickShortcut.ifBlank { profile.secondaryAppPackage }, profile.secondaryTripleClickAction2, profile.secondaryTripleClickAppPackage2)
            }
        } else ActionTuple(RemapAction.PASSTHROUGH, "", RemapAction.PASSTHROUGH, "")

        val sTuple = if (isPrimary) {
            if (isLongPress) ActionTuple(s.primaryLongPressAction, s.primaryLongPressShortcut.ifBlank { s.primaryAppPackage }, s.primaryLongPressAction2, s.primaryLongPressAppPackage2)
            else if (count == 1) ActionTuple(s.primaryClickAction, s.primaryClickShortcut.ifBlank { s.primaryAppPackage }, s.primaryClickAction2, s.primaryClickAppPackage2)
            else if (count == 2) ActionTuple(s.primaryDoubleClickAction, s.primaryDoubleClickShortcut.ifBlank { s.primaryAppPackage }, s.primaryDoubleClickAction2, s.primaryDoubleClickAppPackage2)
            else ActionTuple(s.primaryTripleClickAction, s.primaryTripleClickShortcut.ifBlank { s.primaryAppPackage }, s.primaryTripleClickAction2, s.primaryTripleClickAppPackage2)
        } else {
            if (isLongPress) ActionTuple(s.secondaryLongPressAction, s.secondaryLongPressShortcut.ifBlank { s.secondaryAppPackage }, s.secondaryLongPressAction2, s.secondaryLongPressAppPackage2)
            else if (count == 1) ActionTuple(s.secondaryClickAction, s.secondaryClickShortcut.ifBlank { s.secondaryAppPackage }, s.secondaryClickAction2, s.secondaryClickAppPackage2)
            else if (count == 2) ActionTuple(s.secondaryDoubleClickAction, s.secondaryDoubleClickShortcut.ifBlank { s.secondaryAppPackage }, s.secondaryDoubleClickAction2, s.secondaryDoubleClickAppPackage2)
            else ActionTuple(s.secondaryTripleClickAction, s.secondaryTripleClickShortcut.ifBlank { s.secondaryAppPackage }, s.secondaryTripleClickAction2, s.secondaryTripleClickAppPackage2)
        }

        val finalAction1 = if (pTuple.action1 != RemapAction.PASSTHROUGH) pTuple.action1 else sTuple.action1
        val finalPayload1 = if (pTuple.action1 != RemapAction.PASSTHROUGH) pTuple.payload1 else sTuple.payload1
        val finalAction2 = if (pTuple.action2 != RemapAction.PASSTHROUGH) pTuple.action2 else sTuple.action2
        val finalPayload2 = if (pTuple.action2 != RemapAction.PASSTHROUGH) pTuple.payload2 else sTuple.payload2

        return ResolvedGestureAction(finalAction1, finalPayload1, finalAction2, finalPayload2)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!currentSettings.remapperEnabled) return super.onKeyEvent(event)
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_PAGE_UP && keyCode != KeyEvent.KEYCODE_PAGE_DOWN) return super.onKeyEvent(event)

        val deviceName = event.device?.name ?: ""
        if (currentSettings.deviceNameFilter.isNotBlank() && !deviceName.contains(currentSettings.deviceNameFilter, ignoreCase = true)) {
            return super.onKeyEvent(event)
        }

        val keyName = if (keyCode == KeyEvent.KEYCODE_PAGE_UP) "PAGE_UP (Primary)" else "PAGE_DOWN (Secondary)"
        _keyEventsFlow.tryEmit(KeyEventInfo(keyCode = keyCode, keyName = keyName, deviceName = deviceName))

        try {
            val isPrimary = (keyCode == KeyEvent.KEYCODE_PAGE_UP)

            if (event.action == KeyEvent.ACTION_DOWN) {
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
                            ActionExecutor.startDualActions(resolved.action1, resolved.payload1, resolved.action2, resolved.payload2, applicationContext, this@PenRemapperAccessibilityService)
                        }
                        handler.postDelayed(primaryLongPressRunnable!!, currentSettings.longPressDurationMs.toLong().coerceIn(200L, 1500L))
                    } else {
                        isSecondaryPressed = true
                        secondaryClickRunnable?.let { handler.removeCallbacks(it) }
                        secondaryLongPressRunnable?.let { handler.removeCallbacks(it) }

                        secondaryLongPressRunnable = Runnable {
                            isSecondaryLongPressTriggered = true
                            secondaryClickRunnable?.let { handler.removeCallbacks(it) }
                            secondaryClickCount = 0
                            val resolved = resolveGestureAction(0, isLongPress = true, isPrimary = false)
                            ActionExecutor.startDualActions(resolved.action1, resolved.payload1, resolved.action2, resolved.payload2, applicationContext, this@PenRemapperAccessibilityService)
                        }
                        handler.postDelayed(secondaryLongPressRunnable!!, currentSettings.longPressDurationMs.toLong().coerceIn(200L, 1500L))
                    }
                }
                return true
            } else if (event.action == KeyEvent.ACTION_UP) {
                if (isPrimary) {
                    isPrimaryPressed = false
                    primaryLongPressRunnable?.let { handler.removeCallbacks(it) }

                    if (isPrimaryLongPressTriggered) {
                        val resolved = resolveGestureAction(0, isLongPress = true, isPrimary = true)
                        ActionExecutor.stopDualActions(resolved.action1, resolved.payload1, resolved.action2, resolved.payload2, applicationContext, this@PenRemapperAccessibilityService)
                        isPrimaryLongPressTriggered = false
                        primaryClickCount = 0
                        primaryClickRunnable?.let { handler.removeCallbacks(it) }
                    } else {
                        primaryClickCount++
                        primaryClickRunnable?.let { handler.removeCallbacks(it) }
                        val delay = if (primaryClickCount >= 2) currentSettings.tripleClickTimeoutMs.toLong() else currentSettings.doubleClickTimeoutMs.toLong()
                        primaryClickRunnable = Runnable {
                            val count = primaryClickCount
                            primaryClickCount = 0
                            val resolved = resolveGestureAction(count, isLongPress = false, isPrimary = true)
                            if (resolved.action1 == RemapAction.AIR_COMMAND) {
                                ActionExecutor.executeAction(resolved.action1, applicationContext, resolved.payload1, this@PenRemapperAccessibilityService)
                            } else {
                                ActionExecutor.startAction(resolved.action1, applicationContext, resolved.payload1, this@PenRemapperAccessibilityService)
                                ActionExecutor.stopAction(resolved.action1, applicationContext, resolved.payload1, this@PenRemapperAccessibilityService)
                            }
                            if (resolved.action2 != RemapAction.PASSTHROUGH) {
                                if (resolved.action2 == RemapAction.AIR_COMMAND) {
                                    ActionExecutor.executeAction(resolved.action2, applicationContext, resolved.payload2, this@PenRemapperAccessibilityService)
                                } else {
                                    ActionExecutor.startAction(resolved.action2, applicationContext, resolved.payload2, this@PenRemapperAccessibilityService)
                                    ActionExecutor.stopAction(resolved.action2, applicationContext, resolved.payload2, this@PenRemapperAccessibilityService)
                                }
                            }
                        }
                        handler.postDelayed(primaryClickRunnable!!, delay.coerceIn(150L, 800L))
                    }
                } else {
                    isSecondaryPressed = false
                    secondaryLongPressRunnable?.let { handler.removeCallbacks(it) }

                    if (isSecondaryLongPressTriggered) {
                        val resolved = resolveGestureAction(0, isLongPress = true, isPrimary = false)
                        ActionExecutor.stopDualActions(resolved.action1, resolved.payload1, resolved.action2, resolved.payload2, applicationContext, this@PenRemapperAccessibilityService)
                        isSecondaryLongPressTriggered = false
                        secondaryClickCount = 0
                        secondaryClickRunnable?.let { handler.removeCallbacks(it) }
                    } else {
                        secondaryClickCount++
                        secondaryClickRunnable?.let { handler.removeCallbacks(it) }
                        val delay = if (secondaryClickCount >= 2) currentSettings.tripleClickTimeoutMs.toLong() else currentSettings.doubleClickTimeoutMs.toLong()
                        secondaryClickRunnable = Runnable {
                            val count = secondaryClickCount
                            secondaryClickCount = 0
                            val resolved = resolveGestureAction(count, isLongPress = false, isPrimary = false)
                            if (resolved.action1 == RemapAction.AIR_COMMAND) {
                                ActionExecutor.executeAction(resolved.action1, applicationContext, resolved.payload1, this@PenRemapperAccessibilityService)
                            } else {
                                ActionExecutor.startAction(resolved.action1, applicationContext, resolved.payload1, this@PenRemapperAccessibilityService)
                                ActionExecutor.stopAction(resolved.action1, applicationContext, resolved.payload1, this@PenRemapperAccessibilityService)
                            }
                            if (resolved.action2 != RemapAction.PASSTHROUGH) {
                                if (resolved.action2 == RemapAction.AIR_COMMAND) {
                                    ActionExecutor.executeAction(resolved.action2, applicationContext, resolved.payload2, this@PenRemapperAccessibilityService)
                                } else {
                                    ActionExecutor.startAction(resolved.action2, applicationContext, resolved.payload2, this@PenRemapperAccessibilityService)
                                    ActionExecutor.stopAction(resolved.action2, applicationContext, resolved.payload2, this@PenRemapperAccessibilityService)
                                }
                            }
                        }
                        handler.postDelayed(secondaryClickRunnable!!, delay.coerceIn(150L, 800L))
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
