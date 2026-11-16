package com.priyanshudeveloper.aod.phoenixengine.core

import android.content.Context

object AodPrefs {
    private const val PREFS_NAME = "aod_dashboard_preferences"

    private const val KEY_SCREEN_SAVER_ENABLED = "screen_saver_enabled"
    private const val KEY_SCREEN_ON_TIMER = "screen_on_timer"
    private const val KEY_SCREEN_OFF_TIMER = "screen_off_timer"
    private const val KEY_REDUCE_BRIGHTNESS_AFTER = "reduce_brightness_after"

    fun isScreenSaverEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SCREEN_SAVER_ENABLED, false)
    }

    fun setScreenSaverEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SCREEN_SAVER_ENABLED, enabled).apply()
    }

    fun getScreenOnTimer(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SCREEN_ON_TIMER, "Always on") ?: "Always on"
    }

    fun getScreenOffTimer(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SCREEN_OFF_TIMER, "Always on") ?: "Always on"
    }

    fun saveScreenSaverTimers(context: Context, screenOn: String, screenOff: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SCREEN_ON_TIMER, screenOn)
            .putString(KEY_SCREEN_OFF_TIMER, screenOff)
            .apply()
    }

    fun getReduceBrightnessAfter(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_REDUCE_BRIGHTNESS_AFTER, "5sec") ?: "5sec"
    }

    fun saveReduceBrightnessAfter(context: Context, value: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_REDUCE_BRIGHTNESS_AFTER, value).apply()
    }

    fun timerToMillis(value: String): Long {
        return when (value) {
            "None", "Off" -> 0L
            "2sec" -> 2_000L
            "5sec" -> 5_000L
            "10sec" -> 10_000L
            "15sec" -> 15_000L
            "20sec" -> 20_000L
            "30sec" -> 30_000L
            "45sec" -> 45_000L
            "60sec", "1min" -> 60_000L
            "5min" -> 300_000L
            "10min" -> 600_000L
            "30min" -> 1_800_000L
            "1hr" -> 3_600_000L
            "2hr" -> 7_200_000L
            "3hr" -> 10_800_000L
            "Always on display", "Always off display", "Always on", "On" -> -1L
            else -> 5_000L
        }
    }
}
