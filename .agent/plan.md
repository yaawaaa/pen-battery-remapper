# Project Plan

Update the Xiaomi Pen Gen 2 Battery Monitor app with the following features:
1. Add a manual refresh button in the main app UI to update the battery percentage immediately.
2. Update the Quick Settings Tile so that tapping it posts a notification in the Android notification center showing the current battery percentage.
3. Add a feature to detect when the pen connects or disconnects (e.g., based on the sysfs file availability or value) and trigger a high-priority "heads-up" notification to alert the user of the state change.
4. REMOVE the Settings page/screen from the app entirely, as it is unnecessary. The app should just be a single-screen main interface.

## Project Brief

# Project Brief: Xiaomi Pen Gen 2 Battery Monitor

## Features
1. **Single-Screen Battery Dashboard**: A streamlined, settings-free main interface that clearly displays the current battery percentage of the Xiaomi Pen Gen 2, equipped with a manual refresh button for immediate updates.
2. **Quick Settings Tile Integration**: A custom Quick Settings Tile that allows users to tap and instantly receive a system notification displaying the real-time battery percentage without opening the app.
3. **Connection State Alerts**: Background monitoring (e.g., observing `sysfs` file availability or state changes) that automatically triggers a high-priority "heads-up" notification to alert the user whenever the pen connects or disconnects.

## High-Level Tech Stack
* **Language**: Kotlin
* **UI Framework**: Jetpack Compose
* **Navigation & Adaptive Strategy**: Jetpack Navigation 3 (state-driven) and the Compose Material Adaptive library for adaptive layout management across the single-screen interface.
* **Concurrency & Background Work**: Kotlin Coroutines & Flows for efficient, non-blocking `sysfs` file reading and continuous background state monitoring.
* **System Integrations**: 
  * `TileService` API to build the Quick Settings Tile.
  * Android Notifications API (including Notification Channels) to post standard notifications and high-priority heads-up alerts.

## Implementation Steps
**Total Duration:** 25m 50s

### Task_1_CoreDataAndMainUI: Implement battery reading logic from sysfs using Coroutines/StateFlow and build the Main Status Screen using Jetpack Compose, Material Adaptive, and Navigation 3.
- **Status:** COMPLETED
- **Updates:** Implemented battery reading logic using Coroutines/StateFlow. Built Main Status Screen using Jetpack Compose, Material Adaptive (NavigationSuiteScaffold), and Navigation 3. Added error handling for file readability.
- **Acceptance Criteria:**
  - Reads battery from /sys/class/power_supply/idt/reverse_pen_soc
  - Main screen displays battery percentage
  - Uses Compose Material Adaptive and Navigation 3
- **Duration:** 13m 7s

### Task_2_QuickSettingsTile: Implement a Quick Settings Tile using TileService API to display the pen's battery percentage in the drop-down panel.
- **Status:** COMPLETED
- **Updates:** Created PenBatteryTileService extending TileService. Implemented battery reading logic in the tile to update its state, label, and subtitle dynamically in onStartListening and onClick. Registered the service with the proper permissions and intent filters in AndroidManifest.xml. Project builds successfully.
- **Acceptance Criteria:**
  - Quick Settings Tile service created
  - Tile updates and displays current battery percentage
- **Duration:** 1m 15s

### Task_3_HomeScreenWidget: Create a home screen widget using Jetpack Glance to display the pen battery status on the launcher.
- **Status:** COMPLETED
- **Updates:** Added Jetpack Glance dependencies. Created BatteryWidget extending GlanceAppWidget and BatteryWidgetReceiver extending GlanceAppWidgetReceiver. Implemented widget UI to display battery status fetched from the sys file. Added XML widget provider info configuration and registered the receiver in the AndroidManifest.xml. Project builds successfully.
- **Acceptance Criteria:**
  - Glance widget implemented
  - Widget displays battery status on launcher
- **Duration:** 5m 15s

### Task_5_UI_And_Tile_Updates: Remove the Settings screen from the app to make it a single-screen dashboard. Add a manual refresh button to the Main UI. Update the Quick Settings Tile so tapping it posts a system notification with the battery percentage.
- **Status:** COMPLETED
- **Updates:** Removed Settings screen and navigation suite, leaving a single-screen dashboard. Added a manual refresh button to the Main UI. Updated PenBatteryTileService to post a system notification with the battery percentage on click. Added and handled POST_NOTIFICATIONS permission. Project builds successfully.
- **Acceptance Criteria:**
  - Settings screen is removed
  - Refresh button added to main UI
  - Tapping Quick Settings Tile posts a system notification
  - Project builds successfully
- **Duration:** 2m 31s

### Task_6_Connection_Alerts_And_Verify: Implement background monitoring of the sysfs file to detect connection state changes. Trigger high-priority heads-up notifications on connect/disconnect. Run and Verify the application.
- **Status:** COMPLETED
- **Updates:** Implemented PenMonitorService as a Foreground Service to monitor the sysfs file periodically. The service posts high-priority heads-up notifications when connection state changes (connected/disconnected). Added required foreground service permissions and a UI toggle in the main screen to start/stop the service. App builds and tests pass successfully.
- **Acceptance Criteria:**
  - Background monitoring detects connection state changes
  - High-priority heads-up notification triggered on connect/disconnect
  - make sure all existing tests pass
  - build pass
  - app does not crash
- **Duration:** 3m 42s

