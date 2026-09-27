# Pen Battery & Remapper

> A powerful utility & key remapping suite for Xiaomi Stylus Pens on Xiaomi Pad and Android tablet devices.

## 📥 Download

Get the latest APK directly from the [Releases Page](https://github.com/yaawaaa/pen-battery-remapper/releases/tag/v2.0.0).

---

## 🚀 Overview

**Pen Battery & Remapper** is an open-source Android application that combines real-time stylus battery monitoring with deep hardware button remapping, custom gestures, floating Air Command sidebar toolbars, and per-app profile customizations.

---

## ✨ Features

### 🔋 Battery Checker Tab
* **Live Connection & Charging Gauge**: Real-time status display with custom gauge arcs, live battery percentages, and active magnetic charging indicators.
* **Quick Settings Battery Tile**: Android Quick Settings Tile integration with presence verification and a manual "Add Tile" trigger directly from the app.
* **Battery Discharge History Chart**: Interactive smooth line chart detailing historical battery discharge levels and sample stats.
* **Glance Home Screen Widget**: At-a-glance battery updates directly on your home screen.
* **Persistent Notification Center Alerts**: Dynamic notification banner with quick refresh and dismiss controls.
* **Hardware Edge-Case Filtering**: Filters raw sysfs values (such as `50%` disconnected defaults and `255%` transient states) for accurate readings.

### 🔘 Key Remapper Tab
* **Primary & Secondary Button Remapping**:
  * Complete support for **Single Click**, **Double Click**, **Triple Click**, and **Long Press** on both physical buttons (Top Page Up / 92 and Bottom Page Down / 93).
  * **Per-gesture reset buttons** on every gesture for effortless configuration resets.
  * **Dual-Action Gestures**: Trigger two sequential actions (e.g., launch app + simulate keyboard shortcut) on a single gesture.
* **Air Command Floating Sidebar Toolbar**:
  * Floating sidebar toolbar with an **iOS-inspired frosted glass translucent design**.
  * **Live Toolbar Resizing Slider** (0.65x to 1.30x scaling) with a reset scale button and live test overlay trigger.
  * Customizable shortcuts for apps, system actions, shortcuts, and utilities.
* **Per-App Profiles**:
  * Dedicated gesture profiles for specific applications (e.g., GoodNotes, Squid, Concepts, Clip Studio Paint).
  * Complete **triple-click gesture support** across all per-app profile definitions.
* **Note-Taking Pen/Eraser Toggle Presets**:
  * Presets for standard note apps (e.g., `E` key, `CTRL+E`, `Stylus Button 202`).
* **Backup & Restore**:
  * Full JSON export and import for all settings and per-app profiles.
* **Interactive Key Tester**:
  * Live key code detector and test key press simulator.

---

## 🛠️ How It Works

### Hardware Sysfs Node Path
The battery status is queried from the kernel power supply subsystem node exposed by Xiaomi tablet firmware:
```
/sys/class/power_supply/idt/reverse_pen_soc
```

### State Detection Logic
1. **Disconnected State (`50%`)**: Unattached or inactive pen states default to `50%`. The app accurately parses this as **Disconnected**.
2. **Transient Connecting State (`255`)**: Brief pairing transitions report `255`, which the app filters out to prevent erroneous readings.
3. **Active Charge/Level (`0% – 100%`)**: Valid readings are instantly broadcasted across the UI, Quick Settings Tile, Home Screen Widget, and Notifications.

---

## 📱 Requirements & Permissions

### Requirements
* **Android OS**: Android 8.0 (API Level 26) or higher.
* **Hardware**: Xiaomi Pad device with Xiaomi Smart Pen / Pen Gen 2 support.

### Permissions
* `POST_NOTIFICATIONS`: Required on Android 13+ to display battery status notifications and QS tile alerts.
* `SYSTEM_ALERT_WINDOW`: Required for the floating Air Command sidebar overlay.
* `BIND_ACCESSIBILITY_SERVICE`: Required for global hardware button remapping without root.
* `FOREGROUND_SERVICE`: Required for background battery monitoring and persistent overlay services.

---

## 🔨 Build & Installation

### Prerequisites
* JDK 17 or higher
* Android SDK 37

### Building from Source

To compile the release APK:

```bash
# Clone the repository
git clone https://github.com/yaawaaa/pen-battery-remapper.git
cd pen-battery-remapper

# Build debug APK
./gradlew assembleDebug
```

The compiled APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📄 License

Distributed under the Apache 2.0 License. See [`LICENSE`](LICENSE) for details.
