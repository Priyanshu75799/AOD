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
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.priyanshudeveloper.aod.AodActivity
import com.priyanshudeveloper.aod.MainActivity
import com.priyanshudeveloper.aod.R
import com.priyanshudeveloper.aod.phoenixengine.core.AodManager
import com.priyanshudeveloper.aod.phoenixengine.core.BatteryUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AodForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "aod_service_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private var isReceiverRegistered = false
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var updateJob: Job? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    AodManager.batteryInfo.value = BatteryUtils.getBatteryInfo(context, intent)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    Toast.makeText(context, "Charger disconnected. AOD service stopped.", Toast.LENGTH_SHORT).show()
                    killAppAndService()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    if (BatteryUtils.isPluggedIn(context)) {
                        launchAodActivity()
                    }
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    val powerManager = context.getSystemService(POWER_SERVICE) as? PowerManager
                    if (powerManager?.isInteractive == false) {
                        launchAodActivity()
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(batteryReceiver, filter)
        isReceiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!BatteryUtils.isPluggedIn(this)) {
            Toast.makeText(this, "Please Connect Your Charger for AOD services", Toast.LENGTH_LONG).show()
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

        AodManager.isServiceRunning.value = true
        startBatteryUpdateTimer()

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
                // Ignore if already unregistered
            }
            isReceiverRegistered = false
        }
        AodManager.isServiceRunning.value = false
        AodManager.finishAodActivity()
    }

    private fun killAppAndService() {
        AodManager.isServiceRunning.value = false
        AodManager.finishAodActivity()
        stopSelf()
        serviceScope.launch {
            delay(300)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun startBatteryUpdateTimer() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                AodManager.batteryInfo.value = BatteryUtils.getBatteryInfo(this@AodForegroundService)
                delay(1000L)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Always-On Display Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows Always-On Display battery info while charging"
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Always-On Display Active")
            .setContentText("AOD service is monitoring charging status")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun launchAodActivity() {
        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "AOD:WakeLock"
        )
        wakeLock?.acquire(3000)

        val aodIntent = Intent(this, AodActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(aodIntent)
    }
}
