package com.priyanshudeveloper.aod.phoenixengine.core

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.concurrent.TimeUnit

data class AppUsageItem(
    val appName: String,
    val packageName: String,
    val usageTimeStr: String,
    val percentageEstimateStr: String
)

object BatteryUsageHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageStatsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun getTop3AppUsage(context: Context): List<AppUsageItem> {
        if (!hasUsageStatsPermission(context)) {
            return emptyList()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (24 * 60 * 60 * 1000L) // Last 24 hours

        val usageStatsList: List<UsageStats>? = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (usageStatsList.isNullOrEmpty()) {
            return emptyList()
        }

        val packageManager = context.packageManager
        val activeApps = usageStatsList
            .filter { it.totalTimeInForeground > 1000L }
            .sortedByDescending { it.totalTimeInForeground }

        val totalForegroundTimeMs = activeApps.sumOf { it.totalTimeInForeground }.coerceAtLeast(1L)

        val result = mutableListOf<AppUsageItem>()
        for (stats in activeApps) {
            if (result.size >= 3) break
            val pkgName = stats.packageName

            val label = try {
                val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                pkgName.substringAfterLast('.')
            }

            // Skip system UI or launcher if needed, or keep top 3
            if (label.isBlank() || pkgName == context.packageName) continue

            val millis = stats.totalTimeInForeground
            val hours = TimeUnit.MILLISECONDS.toHours(millis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            val timeStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

            val pct = ((millis.toDouble() / totalForegroundTimeMs.toDouble()) * 100).toInt().coerceIn(1, 99)
            val pctStr = "~$pct% battery share"

            result.add(AppUsageItem(appName = label, packageName = pkgName, usageTimeStr = timeStr, percentageEstimateStr = pctStr))
        }

        return result
    }
}
