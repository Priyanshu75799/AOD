package com.priyanshudeveloper.aod.phoenixengine.core

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object ExtraDimUtils {

    fun isExtraDimSupported(context: Context): Boolean {
        try {
            val cr = context.contentResolver ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val valSec = Settings.Secure.getInt(cr, "reduce_bright_colors_activated", -1)
                    if (valSec != -1) return true
                } catch (_: Throwable) {}
            }

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
                    val valSec = Settings.Secure.getInt(cr, key, -1)
                    if (valSec != -1) return true
                } catch (_: Throwable) {}
                try {
                    val valSys = Settings.System.getInt(cr, key, -1)
                    if (valSys != -1) return true
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
        return false
    }

    fun isExtraDimEnabled(context: Context): Boolean {
        try {
            val cr = context.contentResolver ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val value = Settings.Secure.getInt(cr, "reduce_bright_colors_activated", 0)
                    if (value == 1) return true
                } catch (_: Throwable) {}
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
                } catch (_: Throwable) {}
                try {
                    if (Settings.System.getInt(cr, key, 0) == 1) return true
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}

        return false
    }

    fun getExtraDimFeatureName(context: Context): String {
        return try {
            val manufacturer = Build.MANUFACTURER.lowercase()
            when {
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> "Extra Dim / Reading Mode"
                manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> "Extra Dim / Eye Comfort"
                manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> "Extra Dim / Eye Protection"
                manufacturer.contains("samsung") -> "Extra Dim"
                else -> "Extra Dim"
            }
        } catch (_: Throwable) {
            "Extra Dim"
        }
    }

    fun openExtraDimSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Throwable) {}
            }
        }
    }
}
