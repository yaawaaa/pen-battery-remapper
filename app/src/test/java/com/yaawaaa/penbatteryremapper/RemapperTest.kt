package com.yaawaaa.penbatteryremapper

import com.yaawaaa.penbatteryremapper.remapper.ActionExecutor
import com.yaawaaa.penbatteryremapper.remapper.AppInfo
import com.yaawaaa.penbatteryremapper.remapper.AppProfile
import com.yaawaaa.penbatteryremapper.remapper.EngineMode
import com.yaawaaa.penbatteryremapper.remapper.KeyEventInfo
import com.yaawaaa.penbatteryremapper.remapper.RemapAction
import com.yaawaaa.penbatteryremapper.remapper.RemapperBackupManager
import com.yaawaaa.penbatteryremapper.remapper.RemapperSettings
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
        assertEquals(400, settings.tripleClickTimeoutMs)
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
            val matchedProfile = appProfiles.find { it.packageName == activePkg }
            return if (matchedProfile != null) {
                if (isPrimary) matchedProfile.primaryClickAction to matchedProfile.primaryClickShortcut
                else matchedProfile.secondaryClickAction to matchedProfile.secondaryClickShortcut
            } else {
                if (isPrimary) defaultSettings.primaryClickAction to defaultSettings.primaryClickShortcut
                else defaultSettings.secondaryClickAction to defaultSettings.secondaryClickShortcut
            }
        }

        // Test in GoodNotes
        val primaryInGoodNotes = resolveForApp("com.goodnotes.app", isPrimary = true)
        assertEquals(RemapAction.TOGGLE_PEN_ERASER, primaryInGoodNotes.first)
        assertEquals("CTRL+E", primaryInGoodNotes.second)

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
}
