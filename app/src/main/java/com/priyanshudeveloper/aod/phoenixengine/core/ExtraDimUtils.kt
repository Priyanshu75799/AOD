package com.priyanshudeveloper.aod.phoenixengine.core

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object ExtraDimUtils {

    fun isExtraDimSupported(context: Context): Boolean {
        val cr = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Check if standard Android 12+ Extra Dim setting key exists
                Settings.Secure.getInt(cr, "reduce_bright_colors_activated")
                return true
            } catch (_: Settings.SettingNotFoundException) {
                // Key not found, check OEM settings below
            }
        }

        // Check OEM specific keys (Oppo, Vivo, Realme, Xiaomi, Samsung)
        val oemKeys = listOf(
            "extra_dim_mode",
            "coloros_extra_dim",
            "screen_extra_dim",
            "reduce_bright_colors_activated",
            "eye_protect_mode",
            "vivo_night_mode"
        )

        for (key in oemKeys) {
            try {
                Settings.Secure.getInt(cr, key)
                return true
            } catch (_: Exception) {}
            try {
                Settings.System.getInt(cr, key)
                return true
            } catch (_: Exception) {}
        }
        return false
    }

    fun isExtraDimEnabled(context: Context): Boolean {
        val cr = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val value = Settings.Secure.getInt(cr, "reduce_bright_colors_activated")
                if (value == 1) return true
            } catch (_: Exception) {}
        }

        val oemKeys = listOf(
            "extra_dim_mode",
            "coloros_extra_dim",
            "screen_extra_dim",
            "reduce_bright_colors_activated"
        )

        for (key in oemKeys) {
            try {
                if (Settings.Secure.getInt(cr, key, 0) == 1) return true
            } catch (_: Exception) {}
            try {
                if (Settings.System.getInt(cr, key, 0) == 1) return true
            } catch (_: Exception) {}
        }

        return false
    }

    fun getExtraDimFeatureName(context: Context): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> "Extra Dim / Reading Mode"
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> "Extra Dim / Eye Comfort"
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> "Extra Dim / Eye Protection"
            manufacturer.contains("samsung") -> "Extra Dim"
            else -> "Extra Dim"
        }
    }

    fun openExtraDimSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                // Fallback to general settings
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
}
