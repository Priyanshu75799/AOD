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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.priyanshudeveloper.aod.phoenixengine.core.AodManager
import com.priyanshudeveloper.aod.phoenixengine.core.AodPrefs
import com.priyanshudeveloper.aod.phoenixengine.core.BatteryUtils
import com.priyanshudeveloper.aod.phoenixengine.service.AodForegroundService
import com.priyanshudeveloper.aod.ui.theme.AODTheme
import com.priyanshudeveloper.aod.ui.theme.AmoledFontFamily

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AODTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Black
                ) { innerPadding ->
                    MainDashboard(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainDashboard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isServiceRunning by AodManager.isServiceRunning.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startAodService(context)
            Toast.makeText(context, "Service started successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission is required for background service", Toast.LENGTH_SHORT).show()
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            checkAndStartService(context, { permission ->
                notificationPermissionLauncher.launch(permission)
            }, {
                Toast.makeText(context, "Service started successfully", Toast.LENGTH_SHORT).show()
            })
        } else {
            Toast.makeText(context, "Overlay permission is required for AOD", Toast.LENGTH_SHORT).show()
        }
    }

    // Screen Saver Block States
    var screenSaverEnabled by remember { mutableStateOf(AodPrefs.isScreenSaverEnabled(context)) }
    var selectedScreenOnTimer by remember { mutableStateOf(AodPrefs.getScreenOnTimer(context)) }
    var selectedScreenOffTimer by remember { mutableStateOf(AodPrefs.getScreenOffTimer(context)) }

    // Track user interaction state for Save button popup
    var hasScreenOnSelectionChanged by remember { mutableStateOf(false) }
    var hasScreenOffSelectionChanged by remember { mutableStateOf(false) }

    // Reduce Brightness After State
    var selectedBrightnessDelay by remember { mutableStateOf(AodPrefs.getReduceBrightnessAfter(context)) }

    val screenOnOptions = listOf(
        "Off", "10sec", "20sec", "30sec", "60sec",
        "1min", "5min", "10min", "30min", "1hr", "2hr", "3hr", "Always on"
    )

    val screenOffOptions = listOf(
        "Always on", "10sec", "20sec", "30sec", "60sec",
        "1min", "5min", "10min", "30min", "1hr", "2hr", "3hr"
    )

    val brightnessOptions = listOf(
        "None", "2sec", "5sec", "10sec", "15sec", "20sec", "30sec", "45sec", "60sec"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // Top Header Row (Logo + Title + Subtitle on Left, Upright Service Switch on Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "AOD Logo",
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "AOD",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "Powered by Phoenix Engine",
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF8E9BAE),
                        fontFamily = AmoledFontFamily
                    )
                }
            }

            // Upright Service Start Switch
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

                        checkAndStartService(context, { permission ->
                            notificationPermissionLauncher.launch(permission)
                        }, {
                            Toast.makeText(context, "Service started successfully", Toast.LENGTH_SHORT).show()
                        })
                    } else {
                        stopAodService(context)
                        Toast.makeText(context, "Service stopped", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF3B82F6),
                    uncheckedThumbColor = Color(0xFF718096),
                    uncheckedTrackColor = Color(0xFF1E2330)
                )
            )
        }

        // BLOCK 1: Screen Saver Block (Hardware Accelerated GPU Layer Animation)
        val cardBorderColor by animateColorAsState(
            targetValue = if (screenSaverEnabled) Color(0xFF3B82F6) else Color(0xFF262C3D),
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "cardBorderColor"
        )
        val cardContainerColor by animateColorAsState(
            targetValue = if (screenSaverEnabled) Color(0xFF181D2D) else Color(0xFF141721),
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "cardContainerColor"
        )
        val contentAlpha by animateFloatAsState(
            targetValue = if (screenSaverEnabled) 1.0f else 0.45f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "contentAlpha"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
            border = BorderStroke(1.dp, cardBorderColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Block Title & Activation Switch & Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Screen Saver",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val badgeBgColor by animateColorAsState(
                            targetValue = if (screenSaverEnabled) Color(0xFF1E3A8A) else Color(0xFF1E2330),
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            label = "badgeBgColor"
                        )
                        val badgeTextColor by animateColorAsState(
                            targetValue = if (screenSaverEnabled) Color(0xFF60A5FA) else Color(0xFF64748B),
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            label = "badgeTextColor"
                        )
                        Box(
                            modifier = Modifier
                                .background(badgeBgColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (screenSaverEnabled) "Active" else "Off",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeTextColor,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    Switch(
                        checked = screenSaverEnabled,
                        onCheckedChange = { enabled ->
                            screenSaverEnabled = enabled
                            AodPrefs.setScreenSaverEnabled(context, enabled)
                            if (!enabled) {
                                // Restore defaults when Screen Saver is turned OFF
                                selectedScreenOnTimer = "Always on"
                                selectedScreenOffTimer = "Always on"
                                AodPrefs.saveScreenSaverTimers(context, "Always on", "Always on")
                                Toast.makeText(
                                    context,
                                    "Screen Saver off. Restored to default Always-On Display",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Screen Saver active",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B82F6),
                            uncheckedThumbColor = Color(0xFF718096),
                            uncheckedTrackColor = Color(0xFF1E2330)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Options & Save Button Container with GPU Hardware Accelerated Layer Alpha
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = contentAlpha
                        }
                ) {
                    // Section A: Screen-on-timer
                    Text(
                        text = "Screen-on-timer",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3B82F6),
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRadioButtonGrid(
                        options = screenOnOptions,
                        selectedOption = selectedScreenOnTimer,
                        enabled = screenSaverEnabled,
                        onOptionSelected = { choice ->
                            selectedScreenOnTimer = choice
                            hasScreenOnSelectionChanged = true

                            if (choice == "Always on") {
                                selectedScreenOffTimer = "Always on"
                                hasScreenOffSelectionChanged = true
                            } else {
                                if (selectedScreenOffTimer == "Always on" || selectedScreenOffTimer == "Always off display") {
                                    selectedScreenOffTimer = "10sec"
                                    hasScreenOffSelectionChanged = true
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Section B: Screen-off-timer
                    Text(
                        text = "Screen-off-timer",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3B82F6),
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val disabledInScreenOff = if (selectedScreenOnTimer != "Off" && selectedScreenOnTimer.isNotEmpty()) "Always off display" else null

                    FlowRadioButtonGrid(
                        options = screenOffOptions,
                        selectedOption = selectedScreenOffTimer,
                        disabledOption = disabledInScreenOff,
                        enabled = screenSaverEnabled,
                        onOptionSelected = { choice ->
                            selectedScreenOffTimer = choice
                            hasScreenOffSelectionChanged = true

                            if (choice == "Always on") {
                                selectedScreenOnTimer = "Always on"
                                hasScreenOnSelectionChanged = true
                            } else {
                                if (selectedScreenOnTimer == "Always on" || selectedScreenOnTimer == "Off") {
                                    selectedScreenOnTimer = "10sec"
                                    hasScreenOnSelectionChanged = true
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Downright Save Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (selectedScreenOnTimer.isNotEmpty() && selectedScreenOffTimer.isNotEmpty()) {
                                    AodPrefs.saveScreenSaverTimers(
                                        context,
                                        selectedScreenOnTimer,
                                        selectedScreenOffTimer
                                    )
                                    hasScreenOnSelectionChanged = false
                                    hasScreenOffSelectionChanged = false
                                    Toast.makeText(
                                        context,
                                        "Screen Saver settings saved successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Please select options in both Screen-on-timer and Screen-off-timer sections",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            enabled = screenSaverEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6),
                                disabledContainerColor = Color(0xFF1E2330)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Save",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }
        }

        // BLOCK 2: Reduce Brightness after: Block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141721)),
            border = BorderStroke(1.dp, Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Reduce Brightness after:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif
                )

                Spacer(modifier = Modifier.height(14.dp))

                FlowRadioButtonGrid(
                    options = brightnessOptions,
                    selectedOption = selectedBrightnessDelay,
                    enabled = true,
                    onOptionSelected = { choice ->
                        selectedBrightnessDelay = choice
                        AodPrefs.saveReduceBrightnessAfter(context, choice)
                        Toast.makeText(
                            context,
                            "Reduce Brightness set to $choice",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}

@Composable
fun FlowRadioButtonGrid(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    disabledOption: String? = null,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val chunked = options.chunked(3)
        for (rowOptions in chunked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                for (option in rowOptions) {
                    val isDisabled = (disabledOption == option) || !enabled
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable(enabled = !isDisabled) { onOptionSelected(option) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = if (!isDisabled) { { onOptionSelected(option) } } else null,
                            enabled = !isDisabled,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF3B82F6),
                                unselectedColor = Color(0xFF718096),
                                disabledSelectedColor = Color(0xFF333A48),
                                disabledUnselectedColor = Color(0xFF222733)
                            )
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        Text(
                            text = option,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDisabled) Color(0xFF475569) else Color(0xFFE2E8F0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
                if (rowOptions.size < 3) {
                    Spacer(modifier = Modifier.weight((3 - rowOptions.size).toFloat()))
                }
            }
        }
    }
}

private fun checkAndStartService(
    context: Context,
    requestNotificationPermission: (String) -> Unit,
    onSuccess: () -> Unit
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
    onSuccess()
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
