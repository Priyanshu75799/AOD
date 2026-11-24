package com.priyanshudeveloper.aod.phoenixengine.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import com.priyanshudeveloper.aod.MainActivity
import com.priyanshudeveloper.aod.R
import com.priyanshudeveloper.aod.phoenixengine.core.AodPrefs
import com.priyanshudeveloper.aod.phoenixengine.core.BatteryUtils
import com.priyanshudeveloper.aod.phoenixengine.core.TelemetryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChargingTelemetryService : Service() {

    companion object {
        private const val CHANNEL_ID = "charging_telemetry_channel"
        private const val NOTIFICATION_ID = 2002
    }

    private var isReceiverRegistered = false
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var updateJob: Job? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val info = BatteryUtils.getBatteryInfo(context, intent)
                    TelemetryManager.updateMetrics(info.currentMa, info.voltageMv, info.wattage)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    Toast.makeText(context, "Charger disconnected. Telemetry service stopped.", Toast.LENGTH_SHORT).show()
                    stopSelf()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryReceiver, filter)
        isReceiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!BatteryUtils.isPluggedIn(this)) {
            Toast.makeText(this, "Connect charger to start Charging Telemetry Monitor", Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        TelemetryManager.setServiceRunning(true)
        startPollingTimer()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        serviceScope.cancel()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {
                // Ignore
            }
            isReceiverRegistered = false
        }
        TelemetryManager.setServiceRunning(false)
    }

    private fun startPollingTimer() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                val intervalMs = AodPrefs.getAodUpdateIntervalMillis(this@ChargingTelemetryService)
                val info = BatteryUtils.getBatteryInfo(this@ChargingTelemetryService)
                TelemetryManager.updateMetrics(info.currentMa, info.voltageMv, info.wattage)
                delay(intervalMs)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Charging Telemetry Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors Min/Max Charging Current, Voltage & Power metrics"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Charging Telemetry Monitor Active")
            .setContentText("Recording Min/Max charging current, voltage and power")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
