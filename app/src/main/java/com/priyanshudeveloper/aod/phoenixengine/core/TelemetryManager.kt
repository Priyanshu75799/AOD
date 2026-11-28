package com.priyanshudeveloper.aod.phoenixengine.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TelemetryData(
    val isServiceRunning: Boolean = false,
    val currentMa: Int = 0,
    val minCurrentMa: Int = Int.MAX_VALUE,
    val maxCurrentMa: Int = 0,
    val voltageMv: Int = 0,
    val minVoltageMv: Int = Int.MAX_VALUE,
    val maxVoltageMv: Int = 0,
    val wattage: Float = 0f,
    val minWattage: Float = Float.MAX_VALUE,
    val maxWattage: Float = 0f,
    val sessionStartTimeMs: Long = 0L
)

object TelemetryManager {
    private val _telemetryData = MutableStateFlow(TelemetryData())
    val telemetryData = _telemetryData.asStateFlow()

    fun setServiceRunning(running: Boolean) {
        if (running) {
            _telemetryData.value = TelemetryData(
                isServiceRunning = true,
                sessionStartTimeMs = System.currentTimeMillis()
            )
        } else {
            _telemetryData.value = _telemetryData.value.copy(isServiceRunning = false)
        }
    }

    fun updateMetrics(currentMa: Int, voltageMv: Int, wattage: Float) {
        val current = _telemetryData.value
        if (!current.isServiceRunning) return

        val newMinCurrent = if (currentMa > 0) minOf(current.minCurrentMa, currentMa) else current.minCurrentMa
        val newMaxCurrent = maxOf(current.maxCurrentMa, currentMa)

        val newMinVoltage = if (voltageMv > 0) minOf(current.minVoltageMv, voltageMv) else current.minVoltageMv
        val newMaxVoltage = maxOf(current.maxVoltageMv, voltageMv)

        val newMinWattage = if (wattage > 0.05f) minOf(current.minWattage, wattage) else current.minWattage
        val newMaxWattage = maxOf(current.maxWattage, wattage)

        _telemetryData.value = current.copy(
            currentMa = currentMa,
            minCurrentMa = if (newMinCurrent == Int.MAX_VALUE) currentMa else newMinCurrent,
            maxCurrentMa = newMaxCurrent,
            voltageMv = voltageMv,
            minVoltageMv = if (newMinVoltage == Int.MAX_VALUE) voltageMv else newMinVoltage,
            maxVoltageMv = newMaxVoltage,
            wattage = wattage,
            minWattage = if (newMinWattage == Float.MAX_VALUE) wattage else newMinWattage,
            maxWattage = newMaxWattage
        )
    }

    fun resetStats() {
        _telemetryData.value = TelemetryData(
            isServiceRunning = _telemetryData.value.isServiceRunning,
            sessionStartTimeMs = if (_telemetryData.value.isServiceRunning) System.currentTimeMillis() else 0L
        )
    }
}
