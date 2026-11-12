package com.priyanshudeveloper.aod

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.priyanshudeveloper.aod.phoenixengine.core.AodManager
import com.priyanshudeveloper.aod.phoenixengine.core.BatteryUtils
import com.priyanshudeveloper.aod.phoenixengine.service.AodForegroundService
import com.priyanshudeveloper.aod.ui.theme.AODTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AODTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isServiceRunning by AodManager.isServiceRunning.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startAodService(context)
        } else {
            Toast.makeText(context, "Notification permission is required for background service", Toast.LENGTH_SHORT).show()
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            checkAndStartService(context) { permission ->
                notificationPermissionLauncher.launch(permission)
            }
        } else {
            Toast.makeText(context, "Overlay permission is required for AOD", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Battery info on AOD",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Start services",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (!BatteryUtils.isPluggedIn(context)) {
                                Toast.makeText(
                                    context,
                                    "Please Connect Your Charger for AOD services",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@Switch
                            }

                            if (!Settings.canDrawOverlays(context)) {
                                Toast.makeText(
                                    context,
                                    "Please grant Display over other apps permission for AOD",
                                    Toast.LENGTH_LONG
                                ).show()
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                overlayPermissionLauncher.launch(intent)
                                return@Switch
                            }

                            checkAndStartService(context) { permission ->
                                notificationPermissionLauncher.launch(permission)
                            }
                        } else {
                            stopAodService(context)
                        }
                    }
                )
            }
        }
    }
}

private fun checkAndStartService(
    context: Context,
    requestNotificationPermission: (String) -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val hasNotificationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasNotificationPermission) {
            requestNotificationPermission(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
    }

    startAodService(context)
}

private fun startAodService(context: Context) {
    if (!BatteryUtils.isPluggedIn(context)) {
        Toast.makeText(context, "Please Connect Your Charger for AOD services", Toast.LENGTH_LONG).show()
        return
    }
    val intent = Intent(context, AodForegroundService::class.java)
    ContextCompat.startForegroundService(context, intent)
}

private fun stopAodService(context: Context) {
    val intent = Intent(context, AodForegroundService::class.java)
    context.stopService(intent)
}
