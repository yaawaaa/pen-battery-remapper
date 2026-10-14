package com.yaawaaa.penbatteryremapper.remapper

import android.graphics.drawable.Drawable

enum class RemapAction(val displayName: String, val description: String) {
    PLAY_PAUSE("Play / Pause", "Toggle media playback"),
    NEXT_TRACK("Next Track", "Skip to next media track"),
    PREV_TRACK("Previous Track", "Go to previous media track"),
    VOLUME_UP("Volume Up", "Increase media volume"),
    VOLUME_DOWN("Volume Down", "Decrease media volume"),
    SCREENSHOT("Take Screenshot", "Capture device screen"),
    FLASHLIGHT("Toggle Flashlight", "Turn camera torch on or off"),
    LAUNCH_APP("Launch App", "Open a specific installed application"),
    CUSTOM_KEYBOARD_SHORTCUT("Custom Keyboard Shortcut", "Execute custom key combination (Ctrl+C/V/Z, Alt+Tab, etc.)"),
    TOGGLE_PEN_ERASER("Pen / Eraser Toggle", "Toggle Pen/Eraser mode in note-taking apps"),
    PASSTHROUGH("Passthrough", "Default button behavior (do nothing)"),
    HOME("Home", "Navigate to home screen"),
    BACK("Back", "Navigate back"),
    RECENTS("Recent Apps", "Show recent apps overview"),
    LOCK_SCREEN("Lock Screen", "Lock device screen"),
    MUTE("Mute Audio", "Toggle audio mute state"),
    TOGGLE_SCREEN("Toggle Screen", "Turn screen off / toggle screen lock"),
    SCREEN_ON("Turn Screen On", "Force display on / wake up screen"),
    SCREEN_OFF("Turn Screen Off", "Force display off / lock screen"),
    SPLIT_SCREEN("Split Screen", "Toggle split-screen mode"),
    OPEN_NOTIFICATIONS("Open Notifications", "Expand notification shade"),
    OPEN_QUICK_SETTINGS("Open Quick Settings", "Expand Quick Settings shade"),
    TOGGLE_AUTO_ROTATE("Toggle Auto-Rotate", "Toggle system auto-rotate setting"),
    CLEAR_NOTIFICATIONS("Clear Notifications", "Dismiss notification shade"),
    KILL_FOREGROUND_APP("Kill Foreground App", "Force-stop active foreground app via root"),
    SWIPE_UP("Swipe Up", "Perform an upward air gesture swipe"),
    SWIPE_DOWN("Swipe Down", "Perform a downward air gesture swipe"),
    SWIPE_LEFT("Swipe Left", "Perform a leftward air gesture swipe"),
    SWIPE_RIGHT("Swipe Right", "Perform a rightward air gesture swipe"),
    SCREEN_DOUBLE_TAP_LEFT("Screen Double Tap (Left)", "Double tap left side of screen"),
    SCREEN_DOUBLE_TAP_RIGHT("Screen Double Tap (Right)", "Double tap right side of screen"),
    SCREEN_LONG_PRESS_LEFT("Screen Long Press (Left)", "Long press left side of screen"),
    SCREEN_LONG_PRESS_RIGHT("Screen Long Press (Right)", "Long press right side of screen"),
    SCREEN_TAP("Screen Tap", "Single tap screen at target location"),
    SCREEN_TAP_LEFT("Screen Tap (Left)", "Single tap left side of screen"),
    SCREEN_TAP_RIGHT("Screen Tap (Right)", "Single tap right side of screen"),
    AIR_COMMAND("Air Command", "Toggle or trigger Air Command overlay toolbar"),
    LOCKSCREEN_AWAKE("Lockscreen Awake", "Trigger Lockscreen Awake background action via explicit Intent"),
    TRIGGER_EXTERNAL_APP("Trigger External App", "Trigger external app background action via explicit Intent");

    companion object {
        fun fromNameOrDefault(name: String?, default: RemapAction = PASSTHROUGH): RemapAction {
            if (name == null) return default
            return try {
                valueOf(name)
            } catch (e: IllegalArgumentException) {
                default
            }
        }
    }
}

enum class EngineMode(val displayName: String) {
    ACCESSIBILITY("Accessibility Service"),
    ROOT("Root Helper");

    companion object {
        fun fromNameOrDefault(name: String?, default: EngineMode = ACCESSIBILITY): EngineMode {
            if (name == null) return default
            return try {
                valueOf(name)
            } catch (e: IllegalArgumentException) {
                default
            }
        }
    }
}

data class KeyEventInfo(
    val keyCode: Int,
    val keyName: String,
    val deviceName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false
)
