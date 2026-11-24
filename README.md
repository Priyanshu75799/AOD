# AOD - Always-On Display & Battery Monitor

An ultra-power-efficient, feature-packed **Always-On Display (AOD)**, Charging Telemetry Monitor, and Battery Dashboard application for Android built with Jetpack Compose. Designed specifically for charging telemetry, screen saver customization, battery usage analytics, and **AMOLED/LCD Display Protection**.

---

## 🌟 Version Comparison (v1.2 vs v1.3)

| Feature / Capabilities | 📌 Version 1.2 | 🚀 Version 1.3 (Latest) |
| :--- | :---: | :---: |
| **Always-On Display Overlay** | ✅ Included | ✅ Enhanced with dynamic update throttling |
| **Screen Saver & Timers** | ✅ Included | ✅ Shifted to dedicated 'AOD controls' hub |
| **Reduce Brightness Delay** | ✅ Included | ✅ Shifted to dedicated 'AOD controls' hub |
| **Main Dashboard Layout** | Screen Saver & Brightness blocks | Information Window + Battery Usage Graph |
| **Navigation & Settings Hub** | Single Screen Dashboard | Dashboard + Settings + AOD Controls Hub |
| **System Back Gesture (`BackHandler`)** | Exited App | In-App Back Navigation (`AodControls` → `Settings` → `Dashboard`) |
| **Charging Telemetry Monitor** | Single Live Feed | **Independent Service** with **Min/Max Current, Voltage & Power** |
| **Battery Usage Graph** | Basic Area Curve | **Stock Android Pixel Style** (Blue curve + Dotted charging projection) |
| **Top 3 Battery Consuming Apps** | ❌ Not available | ✅ Real-time analysis with instant `ON_RESUME` permission refresh |
| **OEM Extra Dim Feature Detection** | Basic dimming | ✅ Auto-detects Xiaomi, Oppo, Vivo, Realme, Samsung & Pixel Extra Dim |
| **Power Button Fail-Safe Dismissal** | Basic unlock | ✅ Instant dismissal on physical power button for ALL lockscreen types |
| **Clock Time Format** | Fixed 12-Hour AM/PM | ✅ Selectable **24-Hour** (`13:45`, Default) vs **12-Hour AM/PM** |
| **Media Player Widget on AOD** | ❌ Not available | ✅ Show currently playing track title & artist (Default: OFF) |
| **OLED Burn-in Protection Toggle** | Always Active | ✅ Toggleable pixel shifting (Default: ON, disable for LCD to save CPU) |
| **Battery Gauge Arc Color Picker** | Fixed Blue | ✅ **10 Curated Colors** (White [Default], Blue, Green, Gold, Purple, Red, Pink, Cyan, Orange, Teal) |
| **AOD Telemetry Update Frequency** | Fixed 1000ms | ✅ Selectable rate (`500ms`, `1sec` Default, `2sec`, `3sec`, `5sec`, `7sec`, `10sec`, `15sec`) |
| **Instant Battery Usage Refresh** | On App Start | ✅ Header Refresh Button + Instant `ACTION_BATTERY_CHANGED` receiver |
| **App Launch / Splash Background** | Default Light White | ✅ Solid Pure Black (`#000000`) theme background |

---

## 🚀 Key Features in Version 1.3

### 📱 1. Navigation & Settings Restructuring
- **Top Bar Gear Icon**: Main Dashboard top-right corner features a Settings Gear icon leading directly to the Settings Section.
- **AOD Controls Hub**: Settings section features a dedicated **'AOD controls'** block, housing Screen Saver timers, Reduce Brightness options, and AOD Refresh Rate settings.
- **System Back Gesture Navigation**: Integrated `BackHandler` ensures pressing the device's back button navigates backwards smoothly (`AodControls` → `Settings` → `Dashboard`) without exiting the app.

### ⚡ 2. Independent Charging Telemetry Monitor Service
- **Dedicated Telemetry Service**: Operates as an independent foreground service (`ChargingTelemetryService`) without clashing with `AodForegroundService`.
- **Min/Max Charging Telemetry Window**: Real-time recording of:
  - **Min & Max Charging Current (mA)**
  - **Min & Max Charging Voltage (mV)**
  - **Min & Max Charging Power (Wattage - W)**
  - Live charging metrics with instant Reset Stats capability.

### 📊 3. Stock Android Pixel Style Battery Usage Graph
- **Pixel Stock Design**: Styled to match Stock Android / Pixel Battery usage screens:
  - Current battery level percentage header (`18%`).
  - Right Y-Axis grid lines & percentage labels (`100%`, `50%`, `0%`).
  - Bottom X-Axis labels (`24 hrs ago` and remaining time estimate e.g. `8 hrs left until full`).
  - **Solid Light Blue Curve (`#8AB4F8`)**: Past battery level trends with translucent vertical fill.
  - **Dotted Projected Charging Line (`. . . . . .`)**: Slopes upwards towards `100%` when phone is plugged in.
- **Top 3 Battery Consuming Apps**: Auto-refreshing app usage statistics with instant `ON_RESUME` permission grant detection.

### 🌙 4. Extra Dim Feature Detection & Power Saving
- **OEM Extra Dim Detection**: Automatically detects Extra Dim, Night Mode, and Eye Protection features across Android 12+, Xiaomi/MIUI, Oppo/ColorOS, Vivo/FuntouchOS, Realme, Samsung, and Pixel devices.
- **Dynamic Advice Banner**: Recommends enabling Extra Dim in Settings for extra battery savings.
- **Ultra-Dimming AOD Scrim**: Automatically applies ultra-low screen brightness (`0.0005f`) and dark scrim overlays when Extra Dim is active.

### 🔒 5. Universal Power Button Fail-Safe Dismissal
- **Physical Power Button Interception**: Pressing the physical power button (`KEYCODE_POWER`) immediately dismisses `AodActivity`.
- **Universal Lockscreen Support**: Handles `ACTION_SCREEN_OFF`, `ACTION_SCREEN_ON`, `ACTION_USER_PRESENT`, and `ACTION_CLOSE_SYSTEM_DIALOGS`, ensuring seamless dismissal for Fingerprint, PIN, Pattern, Swipe, and No Lock Screen devices without crashes or bugs.

### ⚙️ 6. Extended Customization & Options
- **Clock Time Format**: Toggle between **24-Hour** (`13:45`, Default) and **12-Hour AM/PM** (`1:45 PM`) formats.
- **Media Player Widget**: Show currently playing music title and artist details on the AOD screen (Default: OFF).
- **OLED Burn-in Protection Toggle**: Enable minute-by-minute pixel shifting (Default: ON). LCD display users can turn this OFF to save CPU cycles.
- **Battery Gauge Arc Color Selector**: Choose from 10 curated color schemes (White [Default], Electric Blue, Emerald Green, Sunfire Gold, Cyber Purple, Flame Red, Neon Pink, Cyan Ice, Amber Orange, Mint Teal).
- **AOD Telemetry Update Frequency**: Selectable refresh intervals (`500ms`, `1sec` Default, `2sec`, `3sec`, `5sec`, `7sec`, `10sec`, `15sec`) controlling both AOD screen and Charging Telemetry Monitor.

---

## 🛠 Tech Stack & Architecture

- **Language**: Kotlin 2.2+
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM / Clean Architecture with StateFlow & Coroutines
- **Services**: Dual Independent Foreground Services (`AodForegroundService` & `ChargingTelemetryService`)
- **Graphics**: Custom Compose Canvas Arc Gauge, Pixel Curve Charts, and `nativeCanvas` Curved Text on Path
- **Target SDK**: Android 16 (API 36) | **Min SDK**: Android 10 (API 29)
- **Build Optimization**: R8 / ProGuard rules enabled for Release builds

---

## 🚀 Getting Started

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Priyanshu75799/AOD.git
   ```
2. **Open in Android Studio**: Open the project folder in Android Studio (Jellyfish / Ladybug or newer).
3. **Build & Run**:
   - Connect an Android device (Android 10+).
   - Select **app** and press **Run** (`Shift + F10`).
   - Connect your charger and lock the device to experience AOD v1.3.

---
