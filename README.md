# Pen Battery Checker

> A lightweight utility for Xiaomi Pen Gen 2 on Xiaomi Pad / Android devices.

## 📥 Download

Get the latest APK directly from the [Releases Page](https://github.com/yaawaaa/pen-battery-checker/releases).

## Overview

**Pen Battery Checker** is an open-source Android utility application designed specifically for monitoring the battery level and connection status of the Xiaomi Stylus Pen (Gen 2) on supported Xiaomi Pad and Android tablet devices.

## Features

- **Single-Screen Modern UI**: Jetpack Compose single-screen layout with real-time status display, battery percentage indicators, and manual refresh controls.
- **Quick Settings Tile**: Android Quick Settings Tile integration allowing instant battery status checks directly from the system shade with push notifications.
- **Persistent Notification Center Alert**: Dynamic notification banner featuring quick **Refresh** and **Dismiss** actions for seamless interaction.
- **Glance Home Screen Widget**: App Widget built with Jetpack Glance for at-a-glance battery level updates on your home screen.
- **Hardware-Specific Edge Case Handling**: Dedicated parsing logic to accurately filter raw sysfs values, specifically handling `50%` disconnected defaults and `255%` transient connection states.

## How It Works

### Hardware Sysfs Node Path
The app queries the Linux kernel power supply subsystem node exposed by the Xiaomi tablet firmware:
```
/sys/class/power_supply/idt/reverse_pen_soc
```

### State Detection Logic
1. **Disconnected State (`50%`)**: When the pen is unattached or inactive, the kernel driver defaults to reporting `50` (or `50%`). The app interprets this value as **Disconnected** rather than a valid 50% state.
2. **Transient Connecting State (`255`)**: During initial magnetic attachment or pairing transitions, the kernel briefly reports `255`. The app filters out `255` as a transient state and retains the last valid state until state stabilization.
3. **Active Charge/Level (`0% – 100%`)**: Valid readings within range are reported directly to the UI, Widget, and Notifications.

## Requirements & Permissions

### Requirements
- **Android OS**: Android 8.0 (API Level 26) or higher.
- **Hardware**: Xiaomi Pad device with Xiaomi Smart Pen / Pen Gen 2 support.

### Permissions
- `POST_NOTIFICATIONS`: Required on Android 13 (API Level 33) and above to display battery status notifications, QS tile alerts, and background status updates.
- `FOREGROUND_SERVICE`: Required to run foreground services for background monitoring and persistent status notifications when enabled.

## Build & Run

### Prerequisites
- JDK 17 or higher
- Android SDK 35

### Building from Source

To build the debug APK using Gradle:

```bash
# Clone the repository
git clone https://github.com/your-username/PenBatteryChecker.git
cd PenBatteryChecker

# Build debug APK
./gradlew assembleDebug
```

The compiled debug APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

## License

Distributed under the Apache 2.0 License. See [`LICENSE`](LICENSE) for details.
