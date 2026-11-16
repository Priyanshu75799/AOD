# AOD - Always-On Display & Battery Monitor (v1.2)

An ultra-power-efficient, feature-packed **Always-On Display (AOD)** and Battery Dashboard application for Android built with Jetpack Compose. Designed specifically for charging telemetry, screen saver customization, and **AMOLED/LCD Display Protection**.

---

##  Comprehensive Feature List

###  1. Smart Screen Saver & Dynamic Timers
- **Dual Phase Cycling**: Continuous automated cycle between **Screen-ON** (information visible) and **Screen-OFF** (black sleep phase).
- **Customizable Screen-ON Timer**: Options range from `Off`, `10sec`, `20sec`, `30sec`, `60sec`, `1min` up to `3hr`, and `Always on`.
- **Customizable Screen-OFF Timer**: Options range from `Always on`, `10sec`, `20sec`, `30sec`, `60sec`, `1min` up to `3hr`.
- **Smart Auto-Defaulting**:
  - Fresh installation or toggling Screen Saver OFF restores both timers to `'Always on'`.
  - Changing either section first automatically sets the opposing timer to `'10sec'` for seamless UX.

###  2. Advanced Brightness & Display Management
- **Single Initial Brightness Boost**: Brightness rises to 50% (`0.5f`) **ONLY ONCE** on the initial launch cycle, preventing unwanted eye glare or power spikes on subsequent cycles.
- **Reduce Brightness After Delay**: Automatically dims screen brightness to minimum 1% (`0.01f`) after configurable delay: `None`, `2sec`, `5sec`, `10sec`, `15sec`, `20sec`, `30sec`, `45sec`, `60sec` (Default: `5sec`).
- **AMOLED & OLED Optimization**: Renders solid pure black (`#000000`) during sleep phase, physically turning off self-emissive pixels for zero power consumption.
- **LCD Backlight Keeping Awake**: Maintains `FLAG_KEEP_SCREEN_ON` with minimum brightness (`0.001f`) during sleep phase to keep LCD backlights awake without light bleed.

###  3. Real-Time Battery Telemetry & Charging Metrics
- **Live Metrics Dashboard**: Real-time monitoring of:
  - **Battery Percentage (%)** with animated circular arc gauge.
  - **Charging Current (mA)** & **Voltage (mV)**.
  - **Battery Temperature (°C)** & **Wattage (W)**.
  - **Charging Speed Category**: Automatically classifies speed (`Slow`, `Normal`, `Fast`, `Super Fast`).
  - **Charging Status**: Detailed status (`CHARGING (AC)`, `CHARGING (USB)`, `CHARGING (WIRELESS)`, `BATTERY FULL`, `DISCHARGING`).
  - **Estimate to Full / Remaining Time**.
- **Curved Path Canvas Header**: Outer arc text displaying total service duration (`HH:MM:SS`), battery percentage gained (`+X%`), and real-time mA current draw.

###  4. AMOLED Burn-In Protection
- **Minute-by-Minute Pixel Shifting**: Moves screen components by random offsets every 60 seconds with smooth 1000ms transition animations.
- **Subpixel-Optimized Monospace Typography**: Custom AmoledFontFamily to minimize subpixel wear and maximize readability.

###  5. Quick Actions & Gesture Controls
- **Smart Flashlight / Torch Action**:
  - **Long Press (Press & Hold)**: Turns flashlight ON.
  - **Single Tap**: Turns flashlight OFF.
  - Automatic torch cleanup upon exiting AOD or activity destruction.
- **Double-Tap Settings**: Quick double-tap on the settings icon directly opens native Android System Battery Settings.
- **Triple-Tap Dismiss**: Triple tap anywhere on screen immediately dismisses the AOD overlay.
- **Hardware Unlock & Button Dismiss**: Physical power button, fingerprint/face unlock (`ACTION_USER_PRESENT`), or navigation gestures finish AOD cleanly.

### ️ 6. Foreground Service & System Integration
- **Auto Trigger on Plug-In**: Automatically starts service and launches AOD when connected to AC, USB, or Wireless charger (`ACTION_POWER_CONNECTED` / `ACTION_SCREEN_OFF`).
- **Auto Disconnect Cleanup**: Automatically stops service and cleans up memory when charger is unplugged (`ACTION_POWER_DISCONNECTED`).
- **Android 14/15 Special Use Foreground Service**: Built-in support for `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` with full property declarations.
- **System Overlay Support**: Uses `SYSTEM_ALERT_WINDOW` permissions to display smoothly over lockscreens and system windows.

---

##  Tech Stack & Architecture

- **Language**: Kotlin 2.2+
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM / Clean Architecture with StateFlow & Coroutines
- **Graphics**: Custom Compose Canvas Arc Gauge & `nativeCanvas` Curved Text on Path
- **Target SDK**: Android 16 (API 36) | **Min SDK**: Android 10 (API 29)
- **Build Optimization**: R8 / ProGuard rules enabled for Release builds

---

## Getting Started

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Priyanshu75799/AOD.git
   ```
2. **Open in Android Studio**: Open the project folder in Android Studio (Jellyfish / Ladybug or newer).
3. **Build & Run**:
   - Connect an Android device (Android 10+).
   - Select **app** and press **Run** (`Shift + F10`).
   - Connect your charger and lock the device to experience AOD v1.2.

---
