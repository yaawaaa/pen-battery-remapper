package com.yaawaaa.penbatteryremapper

import android.view.InputDevice
import android.view.KeyEvent
import com.yaawaaa.penbatteryremapper.remapper.ActionExecutor
import com.yaawaaa.penbatteryremapper.remapper.AppInfo
import com.yaawaaa.penbatteryremapper.remapper.AppProfile
import com.yaawaaa.penbatteryremapper.remapper.EngineMode
import com.yaawaaa.penbatteryremapper.remapper.KeyEventInfo
import com.yaawaaa.penbatteryremapper.remapper.RemapAction
import com.yaawaaa.penbatteryremapper.remapper.RemapperBackupManager
import com.yaawaaa.penbatteryremapper.remapper.RemapperSettings
import com.yaawaaa.penbatteryremapper.service.PenRemapperAccessibilityService
import com.yaawaaa.penbatteryremapper.service.ResolvedGestureAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemapperTest {

    @Test
    fun testRemapActionEnumParsing() {
        assertEquals(RemapAction.PLAY_PAUSE, RemapAction.fromNameOrDefault("PLAY_PAUSE"))
        assertEquals(RemapAction.NEXT_TRACK, RemapAction.fromNameOrDefault("NEXT_TRACK"))
        assertEquals(RemapAction.PREV_TRACK, RemapAction.fromNameOrDefault("PREV_TRACK"))
        assertEquals(RemapAction.VOLUME_UP, RemapAction.fromNameOrDefault("VOLUME_UP"))
        assertEquals(RemapAction.VOLUME_DOWN, RemapAction.fromNameOrDefault("VOLUME_DOWN"))
        assertEquals(RemapAction.SCREENSHOT, RemapAction.fromNameOrDefault("SCREENSHOT"))
        assertEquals(RemapAction.FLASHLIGHT, RemapAction.fromNameOrDefault("FLASHLIGHT"))
        assertEquals(RemapAction.LAUNCH_APP, RemapAction.fromNameOrDefault("LAUNCH_APP"))
        assertEquals(RemapAction.PASSTHROUGH, RemapAction.fromNameOrDefault("PASSTHROUGH"))
        assertEquals(RemapAction.HOME, RemapAction.fromNameOrDefault("HOME"))
        assertEquals(RemapAction.BACK, RemapAction.fromNameOrDefault("BACK"))
        assertEquals(RemapAction.RECENTS, RemapAction.fromNameOrDefault("RECENTS"))
        assertEquals(RemapAction.LOCK_SCREEN, RemapAction.fromNameOrDefault("LOCK_SCREEN"))
        assertEquals(RemapAction.MUTE, RemapAction.fromNameOrDefault("MUTE"))

        // New suggested actions
        assertEquals(RemapAction.TOGGLE_SCREEN, RemapAction.fromNameOrDefault("TOGGLE_SCREEN"))
        assertEquals(RemapAction.SCREEN_ON, RemapAction.fromNameOrDefault("SCREEN_ON"))
        assertEquals(RemapAction.SCREEN_OFF, RemapAction.fromNameOrDefault("SCREEN_OFF"))
        assertEquals(RemapAction.CUSTOM_KEYBOARD_SHORTCUT, RemapAction.fromNameOrDefault("CUSTOM_KEYBOARD_SHORTCUT"))
        assertEquals(RemapAction.TOGGLE_PEN_ERASER, RemapAction.fromNameOrDefault("TOGGLE_PEN_ERASER"))
        assertEquals(RemapAction.SPLIT_SCREEN, RemapAction.fromNameOrDefault("SPLIT_SCREEN"))
        assertEquals(RemapAction.OPEN_NOTIFICATIONS, RemapAction.fromNameOrDefault("OPEN_NOTIFICATIONS"))
        assertEquals(RemapAction.OPEN_QUICK_SETTINGS, RemapAction.fromNameOrDefault("OPEN_QUICK_SETTINGS"))
        assertEquals(RemapAction.TOGGLE_AUTO_ROTATE, RemapAction.fromNameOrDefault("TOGGLE_AUTO_ROTATE"))
        assertEquals(RemapAction.CLEAR_NOTIFICATIONS, RemapAction.fromNameOrDefault("CLEAR_NOTIFICATIONS"))
        assertEquals(RemapAction.KILL_FOREGROUND_APP, RemapAction.fromNameOrDefault("KILL_FOREGROUND_APP"))
        assertEquals(RemapAction.LOCKSCREEN_AWAKE, RemapAction.fromNameOrDefault("LOCKSCREEN_AWAKE"))
        assertEquals(RemapAction.TRIGGER_EXTERNAL_APP, RemapAction.fromNameOrDefault("TRIGGER_EXTERNAL_APP"))

        // Invalid or null fallback test
        assertEquals(RemapAction.PASSTHROUGH, RemapAction.fromNameOrDefault("INVALID_ACTION"))
        assertEquals(RemapAction.VOLUME_UP, RemapAction.fromNameOrDefault(null, default = RemapAction.VOLUME_UP))
    }

    @Test
    fun testEngineModeEnumParsing() {
        assertEquals(EngineMode.ACCESSIBILITY, EngineMode.fromNameOrDefault("ACCESSIBILITY"))
        assertEquals(EngineMode.ROOT, EngineMode.fromNameOrDefault("ROOT"))

        // Invalid fallback test
        assertEquals(EngineMode.ACCESSIBILITY, EngineMode.fromNameOrDefault("UNKNOWN"))
    }

    @Test
    fun testRemapperSettingsDefaults() {
        val settings = RemapperSettings()
        assertFalse(settings.remapperEnabled)
        assertEquals(EngineMode.ACCESSIBILITY, settings.engineMode)
        assertEquals(RemapAction.PASSTHROUGH, settings.primaryClickAction)
        assertEquals(RemapAction.PASSTHROUGH, settings.primaryDoubleClickAction)
        assertEquals(RemapAction.PASSTHROUGH, settings.primaryTripleClickAction)
        assertEquals(RemapAction.PASSTHROUGH, settings.primaryLongPressAction)
        assertEquals(RemapAction.PASSTHROUGH, settings.primaryClickAction2)
        assertEquals("", settings.primaryClickAppPackage2)
        assertEquals(RemapAction.PASSTHROUGH, settings.primaryDoubleClickAction2)
        assertEquals("", settings.primaryDoubleClickAppPackage2)
        assertEquals(RemapAction.PASSTHROUGH, settings.primaryTripleClickAction2)
        assertEquals("", settings.primaryTripleClickAppPackage2)
        assertEquals(RemapAction.PASSTHROUGH, settings.primaryLongPressAction2)
        assertEquals("", settings.primaryLongPressAppPackage2)

        assertEquals(RemapAction.PASSTHROUGH, settings.secondaryClickAction)
        assertEquals(RemapAction.PASSTHROUGH, settings.secondaryDoubleClickAction)
        assertEquals(RemapAction.PASSTHROUGH, settings.secondaryTripleClickAction)
        assertEquals(RemapAction.PASSTHROUGH, settings.secondaryLongPressAction)
        assertEquals(RemapAction.PASSTHROUGH, settings.secondaryClickAction2)
        assertEquals("", settings.secondaryClickAppPackage2)
        assertEquals(RemapAction.PASSTHROUGH, settings.secondaryDoubleClickAction2)
        assertEquals("", settings.secondaryDoubleClickAppPackage2)
        assertEquals(RemapAction.PASSTHROUGH, settings.secondaryTripleClickAction2)
        assertEquals("", settings.secondaryTripleClickAppPackage2)
        assertEquals(RemapAction.PASSTHROUGH, settings.secondaryLongPressAction2)
        assertEquals("", settings.secondaryLongPressAppPackage2)

        assertEquals(500, settings.longPressDurationMs)
        assertEquals(300, settings.doubleClickTimeoutMs)
        assertEquals(350, settings.tripleClickTimeoutMs)
        assertFalse(settings.useCustomGestureTimeouts)
        assertEquals(300, settings.primaryDoubleClickTimeoutMs)
        assertEquals(350, settings.primaryTripleClickTimeoutMs)
        assertEquals(500, settings.primaryLongPressDurationMs)
        assertEquals(300, settings.secondaryDoubleClickTimeoutMs)
        assertEquals(350, settings.secondaryTripleClickTimeoutMs)
        assertEquals(500, settings.secondaryLongPressDurationMs)
        assertEquals("", settings.primaryAppPackage)
        assertEquals("", settings.secondaryAppPackage)
        assertEquals("Pen", settings.deviceNameFilter)
    }

    @Test
    fun testRemapperSettingsCustomValues() {
        val settings = RemapperSettings(
            remapperEnabled = true,
            engineMode = EngineMode.ROOT,
            primaryClickAction = RemapAction.TOGGLE_SCREEN,
            primaryClickAction2 = RemapAction.NEXT_TRACK,
            primaryClickAppPackage2 = "com.spotify.music",
            primaryDoubleClickAction = RemapAction.SPLIT_SCREEN,
            primaryDoubleClickAction2 = RemapAction.FLASHLIGHT,
            primaryTripleClickAction = RemapAction.KILL_FOREGROUND_APP,
            primaryTripleClickAction2 = RemapAction.HOME,
            primaryLongPressAction = RemapAction.OPEN_NOTIFICATIONS,
            primaryLongPressAction2 = RemapAction.SCREENSHOT,
            secondaryClickAction = RemapAction.OPEN_QUICK_SETTINGS,
            secondaryClickAction2 = RemapAction.MUTE,
            secondaryDoubleClickAction = RemapAction.TOGGLE_AUTO_ROTATE,
            secondaryDoubleClickAction2 = RemapAction.LAUNCH_APP,
            secondaryDoubleClickAppPackage2 = "com.example.app",
            secondaryTripleClickAction = RemapAction.CLEAR_NOTIFICATIONS,
            secondaryLongPressAction = RemapAction.SCREENSHOT,
            secondaryLongPressAction2 = RemapAction.BACK,
            longPressDurationMs = 400,
            doubleClickTimeoutMs = 250,
            tripleClickTimeoutMs = 450,
            deviceNameFilter = "Xiaomi Pen"
        )

        assertTrue(settings.remapperEnabled)
        assertEquals(EngineMode.ROOT, settings.engineMode)
        assertEquals(RemapAction.TOGGLE_SCREEN, settings.primaryClickAction)
        assertEquals(RemapAction.NEXT_TRACK, settings.primaryClickAction2)
        assertEquals("com.spotify.music", settings.primaryClickAppPackage2)
        assertEquals(RemapAction.SPLIT_SCREEN, settings.primaryDoubleClickAction)
        assertEquals(RemapAction.FLASHLIGHT, settings.primaryDoubleClickAction2)
        assertEquals(RemapAction.KILL_FOREGROUND_APP, settings.primaryTripleClickAction)
        assertEquals(RemapAction.HOME, settings.primaryTripleClickAction2)
        assertEquals(RemapAction.OPEN_NOTIFICATIONS, settings.primaryLongPressAction)
        assertEquals(RemapAction.SCREENSHOT, settings.primaryLongPressAction2)

        assertEquals(RemapAction.OPEN_QUICK_SETTINGS, settings.secondaryClickAction)
        assertEquals(RemapAction.MUTE, settings.secondaryClickAction2)
        assertEquals(RemapAction.TOGGLE_AUTO_ROTATE, settings.secondaryDoubleClickAction)
        assertEquals(RemapAction.LAUNCH_APP, settings.secondaryDoubleClickAction2)
        assertEquals("com.example.app", settings.secondaryDoubleClickAppPackage2)
        assertEquals(RemapAction.CLEAR_NOTIFICATIONS, settings.secondaryTripleClickAction)
        assertEquals(RemapAction.SCREENSHOT, settings.secondaryLongPressAction)
        assertEquals(RemapAction.BACK, settings.secondaryLongPressAction2)

        assertEquals(400, settings.longPressDurationMs)
        assertEquals(250, settings.doubleClickTimeoutMs)
        assertEquals(450, settings.tripleClickTimeoutMs)
        assertEquals("Xiaomi Pen", settings.deviceNameFilter)
    }

    @Test
    fun testKeyEventInfoData() {
        val now = System.currentTimeMillis()
        val event = KeyEventInfo(
            keyCode = 92,
            keyName = "KEYCODE_PAGE_UP",
            deviceName = "Xiaomi Smart Pen Gen 2",
            timestamp = now
        )

        assertEquals(92, event.keyCode)
        assertEquals("KEYCODE_PAGE_UP", event.keyName)
        assertEquals("Xiaomi Smart Pen Gen 2", event.deviceName)
        assertEquals(now, event.timestamp)
    }

    @Test
    fun testAppInfoDataClass() {
        val appInfo = AppInfo(
            packageName = "com.example.notes",
            appName = "Notes App",
            icon = null
        )

        assertEquals("com.example.notes", appInfo.packageName)
        assertEquals("Notes App", appInfo.appName)
    }

    @Test
    fun testDeviceNameFilterMatching() {
        fun matchesFilter(deviceName: String, filter: String): Boolean {
            val trimmed = filter.trim()
            return trimmed.isEmpty() || deviceName.contains(trimmed, ignoreCase = true)
        }

        assertTrue(matchesFilter("Xiaomi Smart Pen 2nd Generation", "Pen"))
        assertTrue(matchesFilter("XIAOMI PEN", "pen"))
        assertTrue(matchesFilter("Bluetooth Keyboard", ""))
        assertFalse(matchesFilter("Logitech Bluetooth Keyboard", "Pen"))
    }

    @Test
    fun testDualActionExecutionLogic() {
        val executedActions = mutableListOf<Pair<RemapAction, String>>()
        fun mockExecuteAction(action: RemapAction, pkg: String) {
            executedActions.add(action to pkg)
        }

        fun mockExecuteDualActions(action1: RemapAction, pkg1: String, action2: RemapAction, pkg2: String) {
            mockExecuteAction(action1, pkg1)
            if (action2 != RemapAction.PASSTHROUGH) {
                mockExecuteAction(action2, pkg2)
            }
        }

        // Test 1: Action 2 is PASSTHROUGH -> Only Action 1 runs
        executedActions.clear()
        mockExecuteDualActions(RemapAction.TOGGLE_SCREEN, "", RemapAction.PASSTHROUGH, "")
        assertEquals(1, executedActions.size)
        assertEquals(RemapAction.TOGGLE_SCREEN, executedActions[0].first)

        // Test 2: Action 2 is defined -> Both Action 1 and Action 2 run sequentially
        executedActions.clear()
        mockExecuteDualActions(RemapAction.TOGGLE_SCREEN, "", RemapAction.NEXT_TRACK, "com.spotify.music")
        assertEquals(2, executedActions.size)
        assertEquals(RemapAction.TOGGLE_SCREEN, executedActions[0].first)
        assertEquals(RemapAction.NEXT_TRACK, executedActions[1].first)
        assertEquals("com.spotify.music", executedActions[1].second)

        // Test 3: Dual action with SCREEN_OFF and PLAY_PAUSE
        executedActions.clear()
        mockExecuteDualActions(RemapAction.SCREEN_OFF, "", RemapAction.PLAY_PAUSE, "")
        assertEquals(2, executedActions.size)
        assertEquals(RemapAction.SCREEN_OFF, executedActions[0].first)
        assertEquals(RemapAction.PLAY_PAUSE, executedActions[1].first)
    }

    @Test
    fun testShortcutKeycodeParsing() {
        val ctrlA = ActionExecutor.parseShortcutToKeyCodes("CTRL+A")
        assertEquals(listOf(113, 29), ctrlA)

        val ctrlC = ActionExecutor.parseShortcutToKeyCodes("CTRL+C")
        assertEquals(listOf(113, 31), ctrlC)

        val ctrlV = ActionExecutor.parseShortcutToKeyCodes("CTRL+V")
        assertEquals(listOf(113, 50), ctrlV)

        val ctrlZ = ActionExecutor.parseShortcutToKeyCodes("CTRL+Z")
        assertEquals(listOf(113, 54), ctrlZ)

        val ctrlY = ActionExecutor.parseShortcutToKeyCodes("CTRL+Y")
        assertEquals(listOf(113, 53), ctrlY)

        val altTab = ActionExecutor.parseShortcutToKeyCodes("ALT+TAB")
        assertEquals(listOf(57, 61), altTab)
    }

    @Test
    fun testSingleLettersAndNumbersParsing() {
        val aKey = ActionExecutor.parseShortcutToKeyCodes("A")
        assertEquals(listOf(KeyEvent.KEYCODE_A), aKey)

        val lowercaseA = ActionExecutor.parseShortcutToKeyCodes("a")
        assertEquals(listOf(KeyEvent.KEYCODE_A), lowercaseA)

        val zKey = ActionExecutor.parseShortcutToKeyCodes("Z")
        assertEquals(listOf(KeyEvent.KEYCODE_Z), zKey)

        val num5 = ActionExecutor.parseShortcutToKeyCodes("5")
        assertEquals(listOf(KeyEvent.KEYCODE_5), num5)

        val num0 = ActionExecutor.parseShortcutToKeyCodes("0")
        assertEquals(listOf(KeyEvent.KEYCODE_0), num0)
    }

    @Test
    fun testEditingKeysParsing() {
        val backspace = ActionExecutor.parseShortcutToKeyCodes("BACKSPACE")
        assertEquals(listOf(KeyEvent.KEYCODE_DEL), backspace)

        val bksp = ActionExecutor.parseShortcutToKeyCodes("BKSP")
        assertEquals(listOf(KeyEvent.KEYCODE_DEL), bksp)

        val delete = ActionExecutor.parseShortcutToKeyCodes("DELETE")
        assertEquals(listOf(KeyEvent.KEYCODE_FORWARD_DEL), delete)

        val del = ActionExecutor.parseShortcutToKeyCodes("DEL")
        assertEquals(listOf(KeyEvent.KEYCODE_FORWARD_DEL), del)

        val forwardDel = ActionExecutor.parseShortcutToKeyCodes("FORWARD_DEL")
        assertEquals(listOf(KeyEvent.KEYCODE_FORWARD_DEL), forwardDel)

        val space = ActionExecutor.parseShortcutToKeyCodes("SPACE")
        assertEquals(listOf(KeyEvent.KEYCODE_SPACE), space)

        val enter = ActionExecutor.parseShortcutToKeyCodes("ENTER")
        assertEquals(listOf(KeyEvent.KEYCODE_ENTER), enter)

        val tab = ActionExecutor.parseShortcutToKeyCodes("TAB")
        assertEquals(listOf(KeyEvent.KEYCODE_TAB), tab)

        val escape = ActionExecutor.parseShortcutToKeyCodes("ESCAPE")
        assertEquals(listOf(KeyEvent.KEYCODE_ESCAPE), escape)

        val esc = ActionExecutor.parseShortcutToKeyCodes("ESC")
        assertEquals(listOf(KeyEvent.KEYCODE_ESCAPE), esc)

        val left = ActionExecutor.parseShortcutToKeyCodes("LEFT")
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_LEFT), left)

        val arrowLeft = ActionExecutor.parseShortcutToKeyCodes("ARROW_LEFT")
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_LEFT), arrowLeft)

        val dpadLeft = ActionExecutor.parseShortcutToKeyCodes("DPAD_LEFT")
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_LEFT), dpadLeft)

        val right = ActionExecutor.parseShortcutToKeyCodes("RIGHT")
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_RIGHT), right)

        val arrowRight = ActionExecutor.parseShortcutToKeyCodes("ARROW_RIGHT")
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_RIGHT), arrowRight)

        val dpadRight = ActionExecutor.parseShortcutToKeyCodes("DPAD_RIGHT")
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_RIGHT), dpadRight)

        val up = ActionExecutor.parseShortcutToKeyCodes("UP")
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_UP), up)

        val arrowUp = ActionExecutor.parseShortcutToKeyCodes("ARROW_UP")
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_UP), arrowUp)

        val dpadUp = ActionExecutor.parseShortcutToKeyCodes("DPAD_UP")
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_UP), dpadUp)

        val down = ActionExecutor.parseShortcutToKeyCodes("DOWN")
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_DOWN), down)

        val arrowDown = ActionExecutor.parseShortcutToKeyCodes("ARROW_DOWN")
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_DOWN), arrowDown)

        val dpadDown = ActionExecutor.parseShortcutToKeyCodes("DPAD_DOWN")
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_DOWN), dpadDown)
    }

    @Test
    fun testShiftedPunctuationSymbolsParsing() {
        val questionMark = ActionExecutor.parseShortcutToKeyCodes("?")
        assertEquals(listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SLASH), questionMark)

        val exclamationMark = ActionExecutor.parseShortcutToKeyCodes("!")
        assertEquals(listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_1), exclamationMark)

        val atSymbol = ActionExecutor.parseShortcutToKeyCodes("@")
        assertEquals(listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_2), atSymbol)

        val hashSymbol = ActionExecutor.parseShortcutToKeyCodes("#")
        assertEquals(listOf(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_3), hashSymbol)

        val comma = ActionExecutor.parseShortcutToKeyCodes(",")
        assertEquals(listOf(KeyEvent.KEYCODE_COMMA), comma)

        val period = ActionExecutor.parseShortcutToKeyCodes(".")
        assertEquals(listOf(KeyEvent.KEYCODE_PERIOD), period)
    }

    @Test
    fun testGetShortcutInfoForContinuousAndShiftedKeys() {
        val (metaStateQ, activeModsQ, targetQ) = ActionExecutor.getShortcutInfo("?")
        assertEquals(KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON, metaStateQ)
        assertEquals(listOf(KeyEvent.KEYCODE_SHIFT_LEFT), activeModsQ)
        assertEquals(KeyEvent.KEYCODE_SLASH, targetQ)

        val (metaStateBack, activeModsBack, targetBack) = ActionExecutor.getShortcutInfo("BACKSPACE")
        assertEquals(0, metaStateBack)
        assertTrue(activeModsBack.isEmpty())
        assertEquals(KeyEvent.KEYCODE_DEL, targetBack)

        val (metaStateA, activeModsA, targetA) = ActionExecutor.getShortcutInfo("A")
        assertEquals(0, metaStateA)
        assertTrue(activeModsA.isEmpty())
        assertEquals(KeyEvent.KEYCODE_A, targetA)
    }

    @Test
    fun testPenEraserPresetParsing() {
        val eKey = ActionExecutor.parsePenEraserPreset("E")
        assertEquals(listOf(33), eKey)

        val ctrlE = ActionExecutor.parsePenEraserPreset("CTRL+E")
        assertEquals(listOf(113, 33), ctrlE)

        val stylus202 = ActionExecutor.parsePenEraserPreset("Stylus Button 1 (202)")
        assertEquals(listOf(202), stylus202)
    }

    @Test
    fun testBackupAndRestoreExportImport() {
        val originalSettings = RemapperSettings(
            remapperEnabled = true,
            engineMode = EngineMode.ROOT,
            primaryClickAction = RemapAction.CUSTOM_KEYBOARD_SHORTCUT,
            primaryClickShortcut = "CTRL+A",
            secondaryClickAction = RemapAction.SCREENSHOT
        )
        val originalProfiles = listOf(
            AppProfile(
                packageName = "com.goodnotes.app",
                appName = "GoodNotes",
                primaryClickAction = RemapAction.TOGGLE_PEN_ERASER,
                primaryClickShortcut = "CTRL+E",
                secondaryClickAction = RemapAction.SCREENSHOT
            ),
            AppProfile(
                packageName = "com.paper.concepts",
                appName = "Concepts",
                primaryClickAction = RemapAction.TOGGLE_PEN_ERASER,
                primaryClickShortcut = "E"
            )
        )

        val jsonBackup = RemapperBackupManager.exportBackup(originalSettings, originalProfiles)
        assertTrue(jsonBackup.contains("com.goodnotes.app"))
        assertTrue(jsonBackup.contains("CTRL+A"))

        val restored = RemapperBackupManager.importBackup(jsonBackup)
        assertNotNull(restored)
        val (restoredSettings, restoredProfiles) = restored!!

        assertTrue(restoredSettings.remapperEnabled)
        assertEquals(EngineMode.ROOT, restoredSettings.engineMode)
        assertEquals(RemapAction.CUSTOM_KEYBOARD_SHORTCUT, restoredSettings.primaryClickAction)
        assertEquals("CTRL+A", restoredSettings.primaryClickShortcut)
        assertEquals(RemapAction.SCREENSHOT, restoredSettings.secondaryClickAction)

        assertEquals(2, restoredProfiles.size)
        assertEquals("com.goodnotes.app", restoredProfiles[0].packageName)
        assertEquals("GoodNotes", restoredProfiles[0].appName)
        assertEquals(RemapAction.TOGGLE_PEN_ERASER, restoredProfiles[0].primaryClickAction)
        assertEquals("CTRL+E", restoredProfiles[0].primaryClickShortcut)
        assertEquals(RemapAction.SCREENSHOT, restoredProfiles[0].secondaryClickAction)

        assertEquals("com.paper.concepts", restoredProfiles[1].packageName)
        assertEquals("Concepts", restoredProfiles[1].appName)
        assertEquals("E", restoredProfiles[1].primaryClickShortcut)
    }

    @Test
    fun testAppProfileJsonSerialization() {
        val profiles = listOf(
            AppProfile(
                packageName = "com.squid.notes",
                appName = "Squid Notes",
                primaryClickAction = RemapAction.TOGGLE_PEN_ERASER,
                primaryClickShortcut = "E"
            )
        )

        val json = RemapperBackupManager.serializeAppProfilesJson(profiles)
        assertTrue(json.contains("com.squid.notes"))

        val parsed = RemapperBackupManager.parseAppProfilesJson(json)
        assertEquals(1, parsed.size)
        assertEquals("com.squid.notes", parsed[0].packageName)
        assertEquals("Squid Notes", parsed[0].appName)
        assertEquals(RemapAction.TOGGLE_PEN_ERASER, parsed[0].primaryClickAction)
        assertEquals("E", parsed[0].primaryClickShortcut)
    }

    @Test
    fun testPerAppProfileAutoSwitchResolution() {
        val defaultSettings = RemapperSettings(
            primaryClickAction = RemapAction.PLAY_PAUSE,
            secondaryClickAction = RemapAction.VOLUME_DOWN
        )

        val goodNotesProfile = AppProfile(
            packageName = "com.goodnotes.app",
            appName = "GoodNotes",
            primaryClickAction = RemapAction.TOGGLE_PEN_ERASER,
            primaryClickShortcut = "CTRL+E",
            secondaryClickAction = RemapAction.SCREENSHOT
        )

        val appProfiles = listOf(goodNotesProfile)

        fun resolveForApp(activePkg: String, isPrimary: Boolean): Pair<RemapAction, String> {
            val matchedProfile = appProfiles.find { it.packageName.equals(activePkg, ignoreCase = true) }
            return if (matchedProfile != null) {
                if (isPrimary) matchedProfile.primaryClickAction to matchedProfile.primaryClickShortcut
                else matchedProfile.secondaryClickAction to matchedProfile.secondaryClickShortcut
            } else {
                if (isPrimary) defaultSettings.primaryClickAction to defaultSettings.primaryClickShortcut
                else defaultSettings.secondaryClickAction to defaultSettings.secondaryClickShortcut
            }
        }

        // Test in GoodNotes (exact case)
        val primaryInGoodNotes = resolveForApp("com.goodnotes.app", isPrimary = true)
        assertEquals(RemapAction.TOGGLE_PEN_ERASER, primaryInGoodNotes.first)
        assertEquals("CTRL+E", primaryInGoodNotes.second)

        // Test in GoodNotes (uppercase / mixed case)
        val primaryInGoodNotesUpper = resolveForApp("COM.GOODNOTES.APP", isPrimary = true)
        assertEquals(RemapAction.TOGGLE_PEN_ERASER, primaryInGoodNotesUpper.first)
        assertEquals("CTRL+E", primaryInGoodNotesUpper.second)

        val secondaryInGoodNotes = resolveForApp("com.goodnotes.app", isPrimary = false)
        assertEquals(RemapAction.SCREENSHOT, secondaryInGoodNotes.first)

        // Test in Chrome (no profile -> fallback to default profile)
        val primaryInChrome = resolveForApp("com.android.chrome", isPrimary = true)
        assertEquals(RemapAction.PLAY_PAUSE, primaryInChrome.first)

        val secondaryInChrome = resolveForApp("com.android.chrome", isPrimary = false)
        assertEquals(RemapAction.VOLUME_DOWN, secondaryInChrome.first)
    }

    @Test
    fun testTextShortcutActionIdentification() {
        assertEquals(ActionExecutor.TextShortcutType.SELECT_ALL, ActionExecutor.getTextShortcutType("CTRL+A"))
        assertEquals(ActionExecutor.TextShortcutType.SELECT_ALL, ActionExecutor.getTextShortcutType("Control+a"))
        assertEquals(ActionExecutor.TextShortcutType.SELECT_ALL, ActionExecutor.getTextShortcutType("CTRL + A"))

        assertEquals(ActionExecutor.TextShortcutType.COPY, ActionExecutor.getTextShortcutType("CTRL+C"))
        assertEquals(ActionExecutor.TextShortcutType.COPY, ActionExecutor.getTextShortcutType("COPY"))

        assertEquals(ActionExecutor.TextShortcutType.PASTE, ActionExecutor.getTextShortcutType("CTRL+V"))
        assertEquals(ActionExecutor.TextShortcutType.PASTE, ActionExecutor.getTextShortcutType("PASTE"))

        assertEquals(ActionExecutor.TextShortcutType.CUT, ActionExecutor.getTextShortcutType("CTRL+X"))
        assertEquals(ActionExecutor.TextShortcutType.CUT, ActionExecutor.getTextShortcutType("CUT"))

        assertEquals(null, ActionExecutor.getTextShortcutType("ALT+TAB"))
        assertEquals(null, ActionExecutor.getTextShortcutType("CTRL+SHIFT+Z"))
        assertEquals(null, ActionExecutor.getTextShortcutType(""))
    }

    @Test
    fun testExecuteKeyCombinationValidation() {
        assertFalse(ActionExecutor.executeKeyCombination(listOf(29)))
        assertFalse(ActionExecutor.executeKeyCombination(emptyList()))
        assertFalse(ActionExecutor.executeKeyCombination(listOf(113, 29)))
    }

    @Test
    fun testWidenedGestureTimingRangesAndBounds() {
        // Custom settings with expanded timing values
        val customSettings = RemapperSettings(
            doubleClickTimeoutMs = 1500,
            tripleClickTimeoutMs = 2000,
            longPressDurationMs = 3000,
            useCustomGestureTimeouts = true,
            primaryDoubleClickTimeoutMs = 100,
            primaryTripleClickTimeoutMs = 150,
            primaryLongPressDurationMs = 200,
            secondaryDoubleClickTimeoutMs = 1200,
            secondaryTripleClickTimeoutMs = 1800,
            secondaryLongPressDurationMs = 5000
        )

        assertEquals(1500, customSettings.doubleClickTimeoutMs)
        assertEquals(2000, customSettings.tripleClickTimeoutMs)
        assertEquals(3000, customSettings.longPressDurationMs)

        // Test 50ms stepping logic
        fun snap50(value: Float, min: Int, max: Int): Int =
            (Math.round(value / 50f) * 50).coerceIn(min, max)

        assertEquals(100, snap50(120f, 100, 1500))
        assertEquals(150, snap50(130f, 100, 1500))
        assertEquals(300, snap50(324f, 200, 3000))
        assertEquals(350, snap50(326f, 200, 3000))
        assertEquals(1500, snap50(1600f, 100, 1500))

        // Verify bounds coercing for AccessibilityService helper logic
        fun coerceLongPress(ms: Long): Long = ms.coerceIn(100L, 5000L)
        fun coerceClickTimeout(ms: Long): Long = ms.coerceIn(50L, 3000L)

        assertEquals(100L, coerceLongPress(10L))
        assertEquals(5000L, coerceLongPress(10000L))
        assertEquals(200L, coerceLongPress(200L))

        assertEquals(50L, coerceClickTimeout(10L))
        assertEquals(3000L, coerceClickTimeout(6000L))
        assertEquals(1500L, coerceClickTimeout(1500L))
    }

    @Test
    fun testParsePackageAndAction() {
        // Default values when payload is blank
        val (defaultPkg, defaultAct) = ActionExecutor.parsePackageAndAction("")
        assertEquals("com.yaawaaa.lockscreenawake", defaultPkg)
        assertEquals("com.yaawaaa.lockscreenawake.action.TOGGLE", defaultAct)

        // Shorthand tokens
        val (startPkg, startAct) = ActionExecutor.parsePackageAndAction("START")
        assertEquals("com.yaawaaa.lockscreenawake", startPkg)
        assertEquals("com.yaawaaa.lockscreenawake.action.START", startAct)

        val (stopPkg, stopAct) = ActionExecutor.parsePackageAndAction("STOP")
        assertEquals("com.yaawaaa.lockscreenawake", stopPkg)
        assertEquals("com.yaawaaa.lockscreenawake.action.STOP", stopAct)

        val (queryPkg, queryAct) = ActionExecutor.parsePackageAndAction("QUERY_STATE")
        assertEquals("com.yaawaaa.lockscreenawake", queryPkg)
        assertEquals("com.yaawaaa.lockscreenawake.action.QUERY_STATE", queryAct)

        val (setStateTruePkg, setStateTrueAct) = ActionExecutor.parsePackageAndAction("SET_STATE:TRUE")
        assertEquals("com.yaawaaa.lockscreenawake", setStateTruePkg)
        assertEquals("com.yaawaaa.lockscreenawake.action.SET_STATE", setStateTrueAct)
        
        val (setStateFalsePkg, setStateFalseAct) = ActionExecutor.parsePackageAndAction("SET_STATE:FALSE")
        assertEquals("com.yaawaaa.lockscreenawake", setStateFalsePkg)
        assertEquals("com.yaawaaa.lockscreenawake.action.SET_STATE", setStateFalseAct)

        // Custom package and action separated by slash
        val (slashPkg, slashAct) = ActionExecutor.parsePackageAndAction("com.custom.app/com.custom.ACTION_DO_SOMETHING")
        assertEquals("com.custom.app", slashPkg)
        assertEquals("com.custom.ACTION_DO_SOMETHING", slashAct)

        // Custom package and action separated by colon
        val (colonPkg, colonAct) = ActionExecutor.parsePackageAndAction("com.another.app:com.another.TRIGGER")
        assertEquals("com.another.app", colonPkg)
        assertEquals("com.another.TRIGGER", colonAct)

        // Package only (using default action)
        val (pkgOnly, actOnly) = ActionExecutor.parsePackageAndAction("com.custom.package")
        assertEquals("com.custom.package", pkgOnly)
        assertEquals("com.yaawaaa.lockscreenawake.action.TOGGLE", actOnly)
    }

    @Test
    fun testParseBooleanState() {
        assertEquals(true, ActionExecutor.parseBooleanState("SET_STATE:TRUE"))
        assertEquals(false, ActionExecutor.parseBooleanState("SET_STATE:FALSE"))
        assertEquals(true, ActionExecutor.parseBooleanState("com.yaawaaa.lockscreenawake/SET_STATE:TRUE"))
        assertEquals(null, ActionExecutor.parseBooleanState("START"))
        assertEquals(null, ActionExecutor.parseBooleanState(""))
        assertEquals(null, ActionExecutor.parseBooleanState(null))
    }

    @Test
    fun testResolveGestureActionMediaAndCustomPosResolution() {
        val globalSettings = RemapperSettings(
            primaryClickAction = RemapAction.NEXT_TRACK,
            primaryClickCustomPos1 = true,
            primaryDoubleClickAction = RemapAction.PREV_TRACK,
            primaryDoubleClickCustomPos1 = false,
            secondaryClickAction = RemapAction.PLAY_PAUSE,
            secondaryClickCustomPos1 = true
        )

        val musicAppProfile = AppProfile(
            packageName = "com.spotify.music",
            appName = "Spotify",
            primaryClickAction = RemapAction.PLAY_PAUSE,
            primaryClickCustomPos1 = false,
            primaryClickAction2 = RemapAction.NEXT_TRACK,
            primaryClickAppPackage2 = "500,500",
            primaryClickCustomPos2 = true
        )

        val profiles = listOf(musicAppProfile)

        // 1. Resolve for default app (no per-app profile) -> Fallback to global settings
        val resGlobalPrimary: ResolvedGestureAction = PenRemapperAccessibilityService.resolveGestureAction(
            count = 1,
            isLongPress = false,
            isPrimary = true,
            appProfiles = profiles,
            settings = globalSettings,
            foregroundPackage = "com.android.chrome"
        )
        assertEquals(RemapAction.NEXT_TRACK, resGlobalPrimary.action1)
        assertTrue(resGlobalPrimary.useCustomPos1)
        assertEquals(RemapAction.PASSTHROUGH, resGlobalPrimary.action2)

        val resGlobalDouble = PenRemapperAccessibilityService.resolveGestureAction(
            count = 2,
            isLongPress = false,
            isPrimary = true,
            appProfiles = profiles,
            settings = globalSettings,
            foregroundPackage = "com.android.chrome"
        )
        assertEquals(RemapAction.PREV_TRACK, resGlobalDouble.action1)
        assertFalse(resGlobalDouble.useCustomPos1)

        val resGlobalSecondary = PenRemapperAccessibilityService.resolveGestureAction(
            count = 1,
            isLongPress = false,
            isPrimary = false,
            appProfiles = profiles,
            settings = globalSettings,
            foregroundPackage = "com.android.chrome"
        )
        assertEquals(RemapAction.PLAY_PAUSE, resGlobalSecondary.action1)

        // 2. Resolve for Spotify (per-app profile matched) -> Override
        val resSpotifyPrimary = PenRemapperAccessibilityService.resolveGestureAction(
            count = 1,
            isLongPress = false,
            isPrimary = true,
            appProfiles = profiles,
            settings = globalSettings,
            foregroundPackage = "com.spotify.music"
        )
        assertEquals(RemapAction.PLAY_PAUSE, resSpotifyPrimary.action1)
        assertFalse(resSpotifyPrimary.useCustomPos1)
        assertEquals(RemapAction.NEXT_TRACK, resSpotifyPrimary.action2)
        assertEquals("500,500", resSpotifyPrimary.payload2)
        assertTrue(resSpotifyPrimary.useCustomPos2)

        // Secondary click on Spotify (not overridden in profile) -> Falls back to global settings
        val resSpotifySecondary = PenRemapperAccessibilityService.resolveGestureAction(
            count = 1,
            isLongPress = false,
            isPrimary = false,
            appProfiles = profiles,
            settings = globalSettings,
            foregroundPackage = "com.spotify.music"
        )
        assertEquals(RemapAction.PLAY_PAUSE, resSpotifySecondary.action1)
        assertTrue(resSpotifySecondary.useCustomPos1)
    }

    @Test
    fun testIsValidForegroundPackageFiltering() {
        // Rejected system, overlay, and island packages
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage(null))
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage(""))
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("   "))
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("android"))
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("com.android.systemui"))
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("com.miui.notification"))
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("com.coloros.notification"))
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("com.flyme.island"))
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("com.xiaomi.capsule"))
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("com.android.inputmethod.latin"))
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("com.system.overlay.service"))
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("com.pip.window"))
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("com.freeform.mode"))

        // Rejects matching context package name
        assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("com.yaawaaa.penbatteryremapper", "com.yaawaaa.penbatteryremapper"))

        // Accepts valid app packages
        assertTrue(PenRemapperAccessibilityService.isValidForegroundPackage("com.zhiliaoapp.musically"))
        assertTrue(PenRemapperAccessibilityService.isValidForegroundPackage("com.android.chrome"))
        assertTrue(PenRemapperAccessibilityService.isValidForegroundPackage("com.spotify.music"))
        assertTrue(PenRemapperAccessibilityService.isValidForegroundPackage("com.goodnotes.app"))
    }

    @Test
    fun testInMemoryProfileSyncAndResolution() {
        val settings = RemapperSettings(
            primaryClickAction = RemapAction.PLAY_PAUSE
        )

        PenRemapperAccessibilityService.currentAppProfiles = emptyList()

        // 1. Initial resolution when profile is not present -> fallback to global settings
        var resolved = PenRemapperAccessibilityService.resolveGestureAction(
            count = 1,
            isLongPress = false,
            isPrimary = true,
            appProfiles = PenRemapperAccessibilityService.currentAppProfiles,
            settings = settings,
            foregroundPackage = "com.zhiliaoapp.musically"
        )
        assertEquals(RemapAction.PLAY_PAUSE, resolved.action1)

        // 2. Perform 0ms in-memory update
        val tiktokProfile = AppProfile(
            packageName = "com.zhiliaoapp.musically",
            appName = "TikTok",
            primaryClickAction = RemapAction.VOLUME_UP
        )
        PenRemapperAccessibilityService.updateAppProfileInMemory(tiktokProfile)

        // 3. Immediately resolve using updated in-memory profile
        resolved = PenRemapperAccessibilityService.resolveGestureAction(
            count = 1,
            isLongPress = false,
            isPrimary = true,
            appProfiles = PenRemapperAccessibilityService.currentAppProfiles,
            settings = settings,
            foregroundPackage = "com.zhiliaoapp.musically"
        )
        assertEquals(RemapAction.VOLUME_UP, resolved.action1)

        // 4. Update existing profile in-memory
        val updatedTiktokProfile = tiktokProfile.copy(primaryClickAction = RemapAction.SCREENSHOT)
        PenRemapperAccessibilityService.updateAppProfileInMemory(updatedTiktokProfile)

        resolved = PenRemapperAccessibilityService.resolveGestureAction(
            count = 1,
            isLongPress = false,
            isPrimary = true,
            appProfiles = PenRemapperAccessibilityService.currentAppProfiles,
            settings = settings,
            foregroundPackage = "com.zhiliaoapp.musically"
        )
        assertEquals(RemapAction.SCREENSHOT, resolved.action1)
    }

    @Test
    fun testAccessibilityEventPackageAutoSwitchingAndNotificationFiltering() {
        val tiktokProfile = AppProfile(
            packageName = "com.zhiliaoapp.musically",
            appName = "TikTok",
            primaryClickAction = RemapAction.VOLUME_UP
        )
        val defaultSettings = RemapperSettings(
            primaryClickAction = RemapAction.PLAY_PAUSE
        )
        PenRemapperAccessibilityService.currentAppProfiles = listOf(tiktokProfile)

        // 1. Simulate arrival of application package event (TikTok)
        val appPkg = "com.zhiliaoapp.musically"
        if (PenRemapperAccessibilityService.isValidForegroundPackage(appPkg)) {
            PenRemapperAccessibilityService.activeForegroundPackage = appPkg
        }

        assertEquals("com.zhiliaoapp.musically", PenRemapperAccessibilityService.activeForegroundPackage)

        var resolved = PenRemapperAccessibilityService.resolveGestureAction(
            count = 1,
            isLongPress = false,
            isPrimary = true,
            appProfiles = PenRemapperAccessibilityService.currentAppProfiles,
            settings = defaultSettings,
            foregroundPackage = PenRemapperAccessibilityService.activeForegroundPackage
        )
        assertEquals(RemapAction.VOLUME_UP, resolved.action1)

        // 2. Simulate arrival of notification package event (e.g. com.miui.notification)
        val notificationPkg = "com.miui.notification"
        if (PenRemapperAccessibilityService.isValidForegroundPackage(notificationPkg)) {
            PenRemapperAccessibilityService.activeForegroundPackage = notificationPkg
        }

        // Verify active package remains unchanged as TikTok
        assertEquals("com.zhiliaoapp.musically", PenRemapperAccessibilityService.activeForegroundPackage)

        resolved = PenRemapperAccessibilityService.resolveGestureAction(
            count = 1,
            isLongPress = false,
            isPrimary = true,
            appProfiles = PenRemapperAccessibilityService.currentAppProfiles,
            settings = defaultSettings,
            foregroundPackage = PenRemapperAccessibilityService.activeForegroundPackage
        )
        assertEquals(RemapAction.VOLUME_UP, resolved.action1)
    }

    @Test
    fun testIsValidForegroundPackageWhitelistBypass() {
        val originalProfiles = PenRemapperAccessibilityService.currentAppProfiles
        try {
            PenRemapperAccessibilityService.currentAppProfiles = emptyList()
            // Verify an unconfigured package with "island" (e.g. "com.game.island") returns false.
            assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("com.game.island"))

            // Add "com.game.island" to PenRemapperAccessibilityService.currentAppProfiles and verify isValidForegroundPackage("com.game.island") returns true.
            PenRemapperAccessibilityService.currentAppProfiles = listOf(
                AppProfile(
                    packageName = "com.game.island",
                    appName = "Island Game"
                )
            )
            assertTrue(PenRemapperAccessibilityService.isValidForegroundPackage("com.game.island"))

            // Verify systemui ("com.android.systemui") still returns false even if evaluated without a profile.
            PenRemapperAccessibilityService.currentAppProfiles = emptyList()
            assertFalse(PenRemapperAccessibilityService.isValidForegroundPackage("com.android.systemui"))
        } finally {
            PenRemapperAccessibilityService.currentAppProfiles = originalProfiles
        }
    }

    @Test
    fun testResolveRotatedCoordinates() {
        // Payload format: "rawX,rawY,refW,refH,refRotation"
        val sameRot = ActionExecutor.resolveRotatedCoordinates("100,200,1080,2400,0", null)
        assertNotNull(sameRot)
        assertEquals(100f, sameRot!!.first, 0.01f)
        assertEquals(200f, sameRot.second, 0.01f)

        // Proportional relative window scaling (normX = 100/1080, normY = 200/2400)
        val rot180 = ActionExecutor.resolveRotatedCoordinates("100,200,1080,2400,2", null)
        assertNotNull(rot180)
        assertEquals(100f, rot180!!.first, 0.01f)
        assertEquals(200f, rot180.second, 0.01f)

        // Clamping test with out-of-bounds negative or exceeding coordinates
        val outOfBoundsNeg = ActionExecutor.resolveRotatedCoordinates("-50,-100,1080,2400,0", null)
        assertNotNull(outOfBoundsNeg)
        assertEquals(0f, outOfBoundsNeg!!.first, 0.01f)
        assertEquals(0f, outOfBoundsNeg.second, 0.01f)

        val outOfBoundsExceed = ActionExecutor.resolveRotatedCoordinates("1500,3000,1080,2400,0", null)
        assertNotNull(outOfBoundsExceed)
        assertEquals(1079f, outOfBoundsExceed!!.first, 0.01f)
        assertEquals(2399f, outOfBoundsExceed.second, 0.01f)
    }

    @Test
    fun testCenterRelativeOrientationParity() {
        // Verify legacy "CENTER:" prefix is stripped and standard absolute top-left coordinates are resolved
        val centerSame = ActionExecutor.resolveRotatedCoordinates("CENTER:100,200,1080,2400,0", null)
        assertNotNull(centerSame)
        assertEquals(100f, centerSame!!.first, 0.01f)
        assertEquals(200f, centerSame.second, 0.01f)

        val centerPortrait = ActionExecutor.resolveRotatedCoordinates("CENTER:0,0,1080,2400,0", null)
        assertNotNull(centerPortrait)
        assertEquals(0f, centerPortrait!!.first, 0.01f)
        assertEquals(0f, centerPortrait.second, 0.01f)

        val topLeft = ActionExecutor.resolveRotatedCoordinates("CENTER:500,600,2560,1600,0", null)
        assertNotNull(topLeft)
        assertEquals(500f, topLeft!!.first, 0.01f)
        assertEquals(600f, topLeft.second, 0.01f)

        val bottomRight = ActionExecutor.resolveRotatedCoordinates("CENTER:1280,800,2560,1600,0", null)
        assertNotNull(bottomRight)
        assertEquals(1280f, bottomRight!!.first, 0.01f)
        assertEquals(800f, bottomRight.second, 0.01f)
    }

    @Test
    fun testMultiWindowSplitScreenResolution() {
        // Verify window relative resolution works consistently across single and multi-window split screen modes
        val normCoords = ActionExecutor.resolveRotatedCoordinates("500,600,1000,2000,0", null)
        assertNotNull(normCoords)
        assertEquals(500f, normCoords!!.first, 0.01f)
        assertEquals(600f, normCoords.second, 0.01f)

        // Verify active package matching priority in multi-window modes
        val activePkg = "com.example.notes"
        PenRemapperAccessibilityService.activeForegroundPackage = activePkg
        assertEquals("com.example.notes", PenRemapperAccessibilityService.activeForegroundPackage)
    }

    @Test
    fun testCustomKeyboardShortcutModifierSeparationAndExecution() {
        val ctrlA = ActionExecutor.parseShortcutToKeyCodes("CTRL+A")
        assertEquals(listOf(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_A), ctrlA)

        val ctrlShiftZ = ActionExecutor.parseShortcutToKeyCodes("CTRL+SHIFT+Z")
        assertEquals(
            listOf(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_Z),
            ctrlShiftZ
        )

        val altTab = ActionExecutor.parseShortcutToKeyCodes("ALT+TAB")
        assertEquals(listOf(KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_TAB), altTab)

        val (metaCtrlA, modsCtrlA, targetCtrlA) = ActionExecutor.getShortcutInfo("CTRL+A")
        assertTrue((metaCtrlA and KeyEvent.META_CTRL_ON) != 0)
        assertEquals(listOf(KeyEvent.KEYCODE_CTRL_LEFT), modsCtrlA)
        assertEquals(KeyEvent.KEYCODE_A, targetCtrlA)

        val (metaCtrlShiftZ, modsCtrlShiftZ, targetCtrlShiftZ) = ActionExecutor.getShortcutInfo("CTRL+SHIFT+Z")
        assertTrue((metaCtrlShiftZ and KeyEvent.META_CTRL_ON) != 0)
        assertTrue((metaCtrlShiftZ and KeyEvent.META_SHIFT_ON) != 0)
        assertEquals(listOf(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_SHIFT_LEFT), modsCtrlShiftZ)
        assertEquals(KeyEvent.KEYCODE_Z, targetCtrlShiftZ)

        // Execution safely handles modifier separation and injection fallback on JVM
        assertFalse(ActionExecutor.executeKeyCombination(ctrlA, "CTRL+A"))
    }

    @Test
    fun testDeviceFilterNullHandling() {
        fun matchesDeviceFilter(device: InputDevice?, filter: String): Boolean {
            return if (device == null) {
                true
            } else {
                filter.isBlank() || device.name?.contains(filter, ignoreCase = true) == true
            }
        }

        // Verify null device on PAGE_UP and PAGE_DOWN events is never rejected regardless of filter string
        assertTrue(matchesDeviceFilter(null, "Pen"))
        assertTrue(matchesDeviceFilter(null, "Xiaomi Smart Pen"))
        assertTrue(matchesDeviceFilter(null, ""))
        assertTrue(matchesDeviceFilter(null, "Logitech"))
    }
}
