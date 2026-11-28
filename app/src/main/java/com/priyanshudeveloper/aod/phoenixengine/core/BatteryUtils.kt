package com.priyanshudeveloper.aod.phoenixengine.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.priyanshudeveloper.aod.phoenixengine.model.BatteryInfo
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object BatteryUtils {

    fun isPluggedIn(context: Context): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return false
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS ||
                plugged > 0 ||
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun getBatteryInfo(context: Context, batteryIntent: Intent? = null): BatteryInfo {
        val intent = batteryIntent ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val percentage = if (scale > 0 && level >= 0) (level * 100) / scale else 0

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperatureCelsius = tempTenths / 10.0f

        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val rawCurrent = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0

        val absCurrent = abs(rawCurrent)
        val currentMa = if (absCurrent > 10000) absCurrent / 1000 else absCurrent

        val voltageVolts = voltageMv / 1000.0f
        val currentAmps = currentMa / 1000.0f
        val wattage = voltageVolts * currentAmps

        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1

        val chargingStatusText = when {
            status == BatteryManager.BATTERY_STATUS_FULL -> "BATTERY FULL"
            status == BatteryManager.BATTERY_STATUS_CHARGING -> {
                when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_AC -> "CHARGING (AC)"
                    BatteryManager.BATTERY_PLUGGED_USB -> "CHARGING (USB)"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "CHARGING (WIRELESS)"
                    else -> "CHARGING"
                }
            }
            status == BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
            status == BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "NOT CHARGING"
            else -> "CHARGING"
        }

        val speedCategory = when {
            wattage >= 18.0f -> "Super Fast"
            wattage >= 6.0f -> "Fast"
            wattage >= 4.0f -> "Normal"
            wattage >= 0.1f -> "Slow"
            else -> "Normal"
        }

        val estimateStr: String
        if (isCharging) {
            val chargeTimeRemainingMs = batteryManager?.computeChargeTimeRemaining() ?: -1L

            if (chargeTimeRemainingMs > 0) {
                val hours = TimeUnit.MILLISECONDS.toHours(chargeTimeRemainingMs)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(chargeTimeRemainingMs) % 60
                estimateStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
            } else {
                val remainingPercent = 100 - percentage
                if (remainingPercent <= 0) {
                    estimateStr = "Full"
                } else {
                    val effectiveCurrentMa = if (currentMa > 100) currentMa else 1200
                    val remainingMah = 4500.0f * (remainingPercent / 100.0f)
                    val totalMinutes = ((remainingMah / effectiveCurrentMa) * 60).toLong()
                    val hours = totalMinutes / 60
                    val minutes = totalMinutes % 60
                    estimateStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                }
            }
        } else {
            estimateStr = "Discharging"
        }

        return BatteryInfo(
            percentage = percentage,
            isCharging = isCharging,
            temperatureCelsius = temperatureCelsius,
            voltageMv = voltageMv,
            currentMa = currentMa,
            wattage = wattage,
            chargingStatusText = chargingStatusText,
            estimateString = estimateStr,
            chargingSpeedCategory = speedCategory
        )
    }
}
