# AOD - Always-On Display & Battery Monitor

**AOD** is an ultra-power-efficient, feature-rich Always-On Display (AOD), Charging Telemetry Monitor, and Battery Dashboard application for Android, built using Kotlin and Jetpack Compose.

Designed to provide customizable always-on screen information, real-time charging telemetry, battery analytics, and OLED/LCD display protection.

---

## What's New in Version 1.4

Version 1.4 is a major stability and functionality update focused on fixing the issues identified in v1.3, improving device compatibility, and refining the overall AOD experience.

### Major Improvements & Bug Fixes

| Feature / Issue | Version 1.3 | Version 1.4 |
| :--- | :--- | :--- |
| **AOD Media Controls** | Displayed track title and artist only | Added seek slider, elapsed/total playback time, and Previous/Next controls |
| **App Stability** | Frequent application crashes reported | Major crash-related issues fixed |
| **AOD Window Compatibility** | AOD window crashes observed on certain devices | Device-specific AOD window stability issues addressed |
| **OLED Burn-in Protection** | Pixel shifting did not work reliably | OLED protection behavior improved |
| **AOD Window Lifecycle** | Stability and cleanup limitations | Improved receiver cleanup and lifecycle handling |
| **Power Button Dismissal** | Less consistent dismissal behavior | Improved power-button dismissal handling |
| **Codebase Optimization** | Standard implementation | Code cleanup, unused resource/import removal, and optimized release build |

**Version 1.4 focuses on reliability:** Previously reported crashes, media-control limitations, and OLED protection issues have been addressed and tested by the developer.

---

## Key Features

### 1. Always-On Display & Customization

- **Custom AOD Interface:** Displays essential information on the screen while the device is idle.
- **Clock Time Format:** Switch between 24-hour format (`13:45`, Default) and 12-hour AM/PM format (`1:45 PM`).
- **Battery Gauge Arc:** Choose from 10 curated color schemes:
  - White (Default)
  - Electric Blue
  - Emerald Green
  - Sunfire Gold
  - Cyber Purple
  - Flame Red
  - Neon Pink
  - Cyan Ice
  - Amber Orange
  - Mint Teal
- **AOD Refresh Frequency:** Choose from configurable update intervals of `500ms`, `2sec`, `3sec`, `5sec` (Default), `7sec`, `10sec`, and `15sec`.

### 2. Media Player Widget

Control and monitor currently playing media directly from the AOD window.

- Displays current track title and artist.
- Seek slider for playback position.
- Elapsed and total playback time.
- Previous and Next track controls.
- Media widget is disabled by default and can be enabled through settings.

### 3. OLED Burn-in Protection

- **Pixel Shifting:** Minute-by-minute pixel position adjustment to help reduce the risk of static image retention on OLED displays.
- **Configurable Protection:** Enabled by default.
- **LCD Compatibility:** LCD users can disable pixel shifting to avoid unnecessary CPU activity.

### 4. Independent Charging Telemetry Monitor

A dedicated `ChargingTelemetryService` operates independently of `AodForegroundService`.

Real-time charging statistics include:

- Minimum and Maximum Charging Current (mA).
- Minimum and Maximum Charging Voltage (mV).
- Minimum and Maximum Charging Power (W).
- Live charging metrics.
- Instant Reset Stats functionality.

### 5. Battery Usage Dashboard

A battery monitoring interface inspired by the visual design of Stock Android / Google Pixel battery statistics.

- Current battery percentage.
- Battery usage history graph.
- Percentage-based Y-axis with grid lines.
- Time-based X-axis labels.
- Solid light-blue battery history curve (`#8AB4F8`).
- Translucent graph fill.
- Dotted projected charging curve when the device is plugged in.
- System battery settings integration for detailed per-app battery usage.

### 6. Extra Dim Detection & Power Saving

Detects supported system-level display dimming and eye-protection features across Android and various OEM implementations.

Supported detection targets include:

- Stock Android / Pixel
- Xiaomi / MIUI
- Oppo / ColorOS
- Vivo / FuntouchOS
- Realme
- Samsung

Additional behavior:

- Detects supported Extra Dim, Night Mode, and Eye Protection settings.
- Displays contextual recommendations for additional dimming.
- Applies an ultra-low-brightness AOD scrim when supported Extra Dim conditions are active.

### 7. AOD Navigation & Settings

- **Settings Shortcut:** Gear icon on the main dashboard provides direct access to Settings.
- **AOD Controls Hub:** Dedicated section for screen saver timers, brightness reduction, and AOD refresh settings.
- **Back Navigation:** Integrated Android `BackHandler` for navigating between AOD Controls, Settings, and Dashboard.

### 8. Power Button & Lockscreen Handling

Improved AOD dismissal and screen-state handling.

- Handles supported screen-state broadcasts such as `ACTION_SCREEN_OFF`, `ACTION_SCREEN_ON`, `ACTION_USER_PRESENT`, and `ACTION_CLOSE_SYSTEM_DIALOGS`.
- Improved handling for power-button dismissal.
- Designed to work with different lockscreen configurations, including PIN, Pattern, Fingerprint, Swipe, and devices without a lock screen.

## 9. Screenshots

- **AOD Window**
  <img width="720" height="1600" alt="Screenshot_20261128-134355" src="https://github.com/user-attachments/assets/708b5f16-a75c-40cb-8123-2f00183593ce" />

- **Dashboard**
  <img width="720" height="1600" alt="Screenshot_20261128-192008" src="https://github.com/user-attachments/assets/0c2acc4a-de74-4a4b-a5d2-2930edf0a59a" />

- **Settings**
  <img width="720" height="1916" alt="Screenshot_20261128-133836" src="https://github.com/user-attachments/assets/8c5347b6-4278-49fb-9bcf-b9fb1ff5c8d1" />

- **AOD controls**
  <img width="720" height="1600" alt="Screenshot_20261128-133857" src="https://github.com/user-attachments/assets/22d5240d-26e9-40a7-b474-077af0fc7d0e" />


---

## Tech Stack & Architecture

| Component | Technology |
| :--- | :--- |
| Programming Language | Kotlin 2.2+ |
| UI Framework | Jetpack Compose |
| Design System | Material 3 |
| Architecture | MVVM / Clean Architecture |
| Asynchronous Processing | Kotlin Coroutines & StateFlow |
| Background Services | AodForegroundService & ChargingTelemetryService |
| Graphics | Compose Canvas, Custom Arc Gauge, Battery Charts |
| Text Rendering | Android Native Canvas |
| Minimum SDK | Android 10 (API 29) |
| Target SDK | Android 16 (API 36) |
| Build Optimization | R8 / ProGuard |

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/Priyanshu75799/AOD.git
```

### 2. Open in Android Studio

Open the cloned project directory using Android Studio (Jellyfish, Ladybug, or newer).

### 3. Build and Run

1. Connect an Android device running Android 10 or later.
2. Select the `app` configuration.
3. Press **Run** (`Shift + F10`).
4. Configure AOD settings and charging telemetry as required.

---

## Upgrade & Troubleshooting

AOD v1.4 supports direct in-place updates from v1.3. Uninstalling the previous version is not required for a normal update.

If you experience unexpected issues after updating:

1. Restart AOD and check whether the issue persists.
2. If the problem continues, uninstall the existing AOD installation.
3. Download and install the latest version from the official GitHub Releases page.

**Important:** Uninstalling AOD may erase locally stored app data and settings. Back up important information before uninstalling.

---

## Releases

Download the latest stable APK from the official GitHub Releases page:

https://github.com/Priyanshu75799/AOD/releases

---

## Developer

**Priyanshu Rajput**

GitHub: https://github.com/Priyanshu75799

---

## License

AOD is licensed under the **MIT License**.

You are free to use, copy, modify, merge, publish, distribute, sublicense, and sell copies of the software, subject to the conditions of the MIT License.

See the [LICENSE](LICENSE) file for the complete license text.
