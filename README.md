AOD - Always-On Display & Battery Monitor

AOD is a lightweight, feature-rich Always-On Display (AOD), Charging Telemetry Monitor, and Battery Dashboard application for Android, built with Kotlin and Jetpack Compose.

It provides a customizable always-on screen, real-time charging telemetry, battery information, and display protection features for OLED and LCD devices.

---

What's New in Version 1.4

Version 1.4 is a major stability and functionality update focused on resolving issues identified in v1.3, improving device compatibility, and refining the overall AOD experience.

Major Improvements & Bug Fixes

Feature / Issue| Version 1.3| Version 1.4
AOD Media Controls| Displayed track title and artist only| Added Play/Pause and Previous/Next controls
App Stability| Frequent application crashes reported| Major crash-related issues fixed
AOD Window Compatibility| AOD window crashes on certain devices| Device-specific stability improvements
OLED Burn-in Protection| Pixel shifting did not work reliably| Improved pixel-shifting behavior
AOD Window Lifecycle| Stability and cleanup limitations| Improved receiver cleanup and lifecycle handling
Power Button Dismissal| Less consistent dismissal behavior| Improved power-button dismissal handling
Codebase Optimization| Standard implementation| Code cleanup, unused resource/import removal, and release build optimization

Version 1.4 focuses on reliability, addressing previously reported crashes, media-control limitations, and OLED protection issues.

---

Key Features

1. Always-On Display & Customization

- Custom AOD Interface: Displays essential information while the device is idle.
- Clock Time Format: Switch between 24-hour format ("13:45", default) and 12-hour AM/PM format ("1:45 PM").
- Battery Gauge Arc: Choose from 10 curated color schemes:
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
- AOD Refresh Frequency: Configure update intervals of "500ms", "2sec", "3sec", "5sec" (default), "7sec", "10sec", and "15sec".

2. Media Player Widget

Control currently playing media directly from the AOD window.

- Displays the current track title and artist.
- Play/Pause controls.
- Previous and Next track controls.
- Disabled by default and can be enabled through Settings.

3. OLED Burn-in Protection

- Pixel Shifting: Minute-by-minute pixel position adjustment to help reduce the risk of static image retention on OLED displays.
- Configurable Protection: Enabled by default.
- LCD Compatibility: LCD users can disable pixel shifting to avoid unnecessary CPU activity.

4. Independent Charging Telemetry Monitor

A dedicated "ChargingTelemetryService" operates independently of "AodForegroundService".

Real-time charging statistics include:

- Minimum and Maximum Charging Current (mA).
- Minimum and Maximum Charging Voltage (mV).
- Minimum and Maximum Charging Power (W).
- Live charging metrics.
- Instant Reset Stats functionality.

5. Battery Usage Dashboard

A battery monitoring interface inspired by the visual design of Stock Android and Google Pixel battery statistics.

- Current battery percentage.
- Battery level graph with percentage-based Y-axis.
- Time-based X-axis labels.
- Light-blue graph styling ("#8AB4F8").
- Translucent graph fill.
- Projected charging curve when the device is plugged in.
- Direct access to Android System Battery Settings for detailed per-app battery usage.

6. Extra Dim Detection & Power Saving

Detects supported system-level display dimming and eye-protection features across Android and various OEM implementations.

Detection targets include:

- Stock Android / Pixel
- Xiaomi / MIUI
- Oppo / ColorOS
- Vivo / FuntouchOS
- Realme
- Samsung

Additional behavior:

- Detects supported Extra Dim, Night Mode, and Eye Protection settings.
- Displays contextual recommendations for additional dimming.
- Applies an ultra-low-brightness AOD scrim when supported Extra Dim conditions are detected.

Feature detection and availability may vary depending on the device manufacturer and Android version.

7. AOD Navigation & Settings

- Settings Shortcut: Gear icon on the main dashboard provides direct access to Settings.
- AOD Controls Hub: Dedicated section for screen saver timers, brightness reduction, and AOD refresh settings.
- Back Navigation: Integrated Android "BackHandler" for navigating between AOD Controls, Settings, and Dashboard.

8. Power Button & Lockscreen Handling

Improved AOD dismissal and screen-state handling.

- Handles supported screen-state broadcasts, including "ACTION_SCREEN_OFF", "ACTION_SCREEN_ON", "ACTION_USER_PRESENT", and "ACTION_CLOSE_SYSTEM_DIALOGS".
- Improved power-button dismissal handling.
- Designed to work with different lockscreen configurations, including PIN, Pattern, Fingerprint, Swipe, and devices without a lock screen.

---

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

10. APK Installation, Google Play Protect & Permission Setup

AOD requires Notification Access to provide its media player controls and related functionality. Since Notification Access is a sensitive Android permission, installing AOD through a manually downloaded APK may trigger security warnings or permission restrictions.

Follow the instructions below to install and configure the application.

Step 1: Download and Install the APK

1. Download the AOD APK from the official GitHub Releases page.
2. Open the downloaded APK and start the installation.
3. Google Play Protect may display a security warning or block the installation because the APK is installed outside Google Play and requests sensitive permissions.
4. If Play Protect blocks the installation, carefully review the warning. If you have verified that the APK came from the official AOD release source and understand the risk, use the available installation options, if provided, to proceed.

Important: Depending on your Android version and Play Protect policies, you may not be given an option to bypass the warning. Do not assume that every device will allow installation.

Step 2: Allow Restricted Settings

After installation, Android may prevent AOD from receiving certain sensitive permissions until restricted settings are explicitly allowed.

1. Open your device's Settings.
2. Navigate to Apps → AOD → App Info.
3. Tap the three-dot menu in the top-right corner.
4. Select Allow restricted settings.
5. Authenticate using your device's PIN, password, pattern, or biometric verification.
6. Return to AOD's App Info and configure the required permissions.

The exact menu names and availability may vary depending on your Android version and device manufacturer.

Step 3: Enable Notification Access

1. Open Settings → Special app access → Notification access.
2. Find AOD in the application list.
3. Enable Notification Access for AOD.
4. Confirm the permission request when prompted.

This permission is required for AOD to interact with supported media notifications and provide media controls such as Play/Pause and Previous/Next.

Google Play Protect: Important Information

- Google Play Protect may warn about or block an APK installation. Its response depends on the APK, device, Android version, and security policies.
- If you choose to proceed after verifying the APK's source, you may need to use the installation options provided by your device.
- Do not permanently disable Google Play Protect. Keeping it enabled is recommended.
- Allowing restricted settings does not require permanently disabling Google Play Protect. Both settings serve different purposes.

Privacy & Permission Disclosure

Notification Access is a sensitive permission. When enabled, Android allows AOD to access notification information made available to its notification listener service. This can include notification text and media-related information.

Only grant this permission if you understand its purpose and trust the application.

AOD users should review all permission requests before granting them. Never approve a permission request without understanding why the application needs it.

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

License

AOD is proprietary software. Its source code is publicly available for viewing, but reuse, modification, redistribution, and commercial use are not permitted without prior written permission.

See the "LICENSE" (LICENSE) file for details.

Previously released MIT-licensed versions remain subject to their original license terms.