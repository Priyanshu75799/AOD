package com.priyanshudeveloper.aod.phoenixengine.model

data class BatteryInfo(
    val percentage: Int = 0,
    val isCharging: Boolean = false,
    val temperatureCelsius: Float = 0f,
    val voltageMv: Int = 0,
    val currentMa: Int = 0,
    val wattage: Float = 0f,
    val chargingStatusText: String = "CHARGING",
    val estimateString: String = "--",
    val chargingSpeedCategory: String = "Normal"
)
