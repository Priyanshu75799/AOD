# AOD - Always-On Display & Battery Monitor (v1.1)

An ultra-power-efficient **Always-On Display (AOD)** Android application built with Jetpack Compose. Designed specifically for charging monitoring with built-in **AMOLED Burn-In Protection** and hardware telemetry.

---

## Features

- **Live Charging Metrics**: Real-time display of battery percentage, charging current (mA), voltage (mV), temperature (°C), wattage (W), and charging speed category (Slow, Normal, Fast, and Super Fast).
- **Service Duration Timer**: Dynamic curved arc header showing exact elapsed time since the AOD service started.
- **AMOLED Burn-In Protection**: Automatic minute-by-minute pixel-shifting system that moves screen components smoothly to protect OLED/AMOLED panels from image retention.
- **AMOLED-Friendly Typography**: Uses uniform Monospace typography to minimize active subpixel exposure and reduce battery drain.
- **Flashlight Quick Action**:
  - **Press & Hold (3 sec)**: Turns Flashlight ON.
  - **Single Tap**: Turns Flashlight OFF.
  - Moves dynamically with AMOLED burn-in protection.
- **Battery Settings Quick Action**: Double-tap on the settings icon opens native System Battery Settings directly.
- **Auto-Trigger on Charge**: Starts monitoring automatically when plugged into AC, USB, or Wireless chargers.

---

## Tech Stack & Architecture

- **Language**: Kotlin 2.1+
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: Clean Architecture with Foreground Service & Mutating StateFlows
- **Graphics**: Custom Canvas Arc Gauge & `nativeCanvas` Curved Text on Path
- **Target SDK**: Android 16 (API 36) | **Min SDK**: Android 10 (API 29)

---

## Getting Started

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Priyanshu75799/AOD.git
   ```
2. **Open in Android Studio**: Open the cloned folder in Android Studio (Ladybug / Jellyfish or newer).
3. **Build & Run**:
   - Connect an Android device (Android 10+).
   - Press **Run 'app'** (`Shift + F10`).
   - Connect your charger and turn off the screen to trigger the AOD experience.

---
