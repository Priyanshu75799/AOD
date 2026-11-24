package com.priyanshudeveloper.aod

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.compose.ui.geometry.Size
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.priyanshudeveloper.aod.phoenixengine.core.AodManager
import com.priyanshudeveloper.aod.phoenixengine.core.AodPrefs
import com.priyanshudeveloper.aod.phoenixengine.core.BatteryUtils
import com.priyanshudeveloper.aod.phoenixengine.core.BatteryUsageHelper
import com.priyanshudeveloper.aod.phoenixengine.core.AppUsageItem
import com.priyanshudeveloper.aod.phoenixengine.core.ExtraDimUtils
import com.priyanshudeveloper.aod.phoenixengine.core.TelemetryManager
import com.priyanshudeveloper.aod.phoenixengine.service.AodForegroundService
import com.priyanshudeveloper.aod.phoenixengine.service.ChargingTelemetryService
import com.priyanshudeveloper.aod.ui.theme.AODTheme
import com.priyanshudeveloper.aod.ui.theme.AmoledFontFamily

enum class AppScreen {
    Dashboard,
    Settings,
    AodControls
}

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
                    AppNavigationHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppNavigationHost(modifier: Modifier = Modifier) {
    var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }

    // Intercept System Navigation Back Button to navigate within app screens
    BackHandler(enabled = (currentScreen != AppScreen.Dashboard)) {
        when (currentScreen) {
            AppScreen.AodControls -> currentScreen = AppScreen.Settings
            AppScreen.Settings -> currentScreen = AppScreen.Dashboard
            else -> {}
        }
    }

    when (currentScreen) {
        AppScreen.Dashboard -> MainDashboardScreen(
            modifier = modifier,
            onNavigateToSettings = { currentScreen = AppScreen.Settings }
        )
        AppScreen.Settings -> SettingsScreen(
            modifier = modifier,
            onBack = { currentScreen = AppScreen.Dashboard },
            onNavigateToAodControls = { currentScreen = AppScreen.AodControls }
        )
        AppScreen.AodControls -> AodControlsScreen(
            modifier = modifier,
            onBack = { currentScreen = AppScreen.Settings }
        )
    }
}

@Composable
fun MainDashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val isServiceRunning by AodManager.isServiceRunning.collectAsState()
    val telemetryData by TelemetryManager.telemetryData.collectAsState()

    var liveBattery by remember { mutableStateOf(BatteryUtils.getBatteryInfo(context)) }
    var hasUsagePermission by remember { mutableStateOf(false) }
    var topApps by remember { mutableStateOf<List<AppUsageItem>>(emptyList()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Instant Battery Change Receiver
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                liveBattery = BatteryUtils.getBatteryInfo(c, intent)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    // Instantly re-evaluate permission state & top apps on ON_RESUME
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = BatteryUsageHelper.hasUsageStatsPermission(context)
                hasUsagePermission = granted
                if (granted) {
                    topApps = BatteryUsageHelper.getTop3AppUsage(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // Top Header Row (Logo + Title + Subtitle, Upright AOD Switch, Gear Settings Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "AOD Logo",
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "AOD",
                        fontSize = 22.sp,
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

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upright AOD Service Start Switch
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

                Spacer(modifier = Modifier.width(8.dp))

                // Settings Gear Button
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF141721), CircleShape)
                        .border(1.dp, Color(0xFF262C3D), CircleShape)
                ) {
                    SettingsCanvasIcon(modifier = Modifier.size(20.dp), color = Color(0xFFE2E8F0))
                }
            }
        }

        // BLOCK 1: Charging Telemetry Monitor Information Window
        val telemetryBorderColor by animateColorAsState(
            targetValue = if (telemetryData.isServiceRunning) Color(0xFF10B981) else Color(0xFF262C3D),
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "telemetryBorderColor"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141721)),
            border = BorderStroke(1.dp, telemetryBorderColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header with Upright Telemetry Service Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Charging Telemetry Monitor",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.SansSerif
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (telemetryData.isServiceRunning) Color(0xFF064E3B) else Color(0xFF1E2330),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (telemetryData.isServiceRunning) "Recording" else "Idle",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (telemetryData.isServiceRunning) Color(0xFF34D399) else Color(0xFF64748B),
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                        Text(
                            text = "Tracks min/max charging current, voltage & power",
                            fontSize = 11.sp,
                            color = Color(0xFF8E9BAE),
                            fontFamily = AmoledFontFamily
                        )
                    }

                    Switch(
                        checked = telemetryData.isServiceRunning,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (!BatteryUtils.isPluggedIn(context)) {
                                    Toast.makeText(context, "Connect charger to start Charging Telemetry Monitor", Toast.LENGTH_LONG).show()
                                    return@Switch
                                }
                                startChargingTelemetryService(context)
                                Toast.makeText(context, "Charging Telemetry Monitor started", Toast.LENGTH_SHORT).show()
                            } else {
                                stopChargingTelemetryService(context)
                                Toast.makeText(context, "Charging Telemetry Monitor stopped", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color(0xFF718096),
                            uncheckedTrackColor = Color(0xFF1E2330)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Telemetry Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Current (mA)",
                        liveValue = if (telemetryData.currentMa > 0) "${telemetryData.currentMa} mA" else "--",
                        minValue = if (telemetryData.minCurrentMa != Int.MAX_VALUE) "${telemetryData.minCurrentMa} mA" else "--",
                        maxValue = if (telemetryData.maxCurrentMa > 0) "${telemetryData.maxCurrentMa} mA" else "--",
                        accentColor = Color(0xFF3B82F6)
                    )
                    TelemetryMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Voltage (mV)",
                        liveValue = if (telemetryData.voltageMv > 0) "${telemetryData.voltageMv} mV" else "--",
                        minValue = if (telemetryData.minVoltageMv != Int.MAX_VALUE) "${telemetryData.minVoltageMv} mV" else "--",
                        maxValue = if (telemetryData.maxVoltageMv > 0) "${telemetryData.maxVoltageMv} mV" else "--",
                        accentColor = Color(0xFF8B5CF6)
                    )
                    TelemetryMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Power (W)",
                        liveValue = if (telemetryData.wattage > 0f) "${"%.1f".format(telemetryData.wattage)} W" else "--",
                        minValue = if (telemetryData.minWattage != Float.MAX_VALUE) "${"%.1f".format(telemetryData.minWattage)} W" else "--",
                        maxValue = if (telemetryData.maxWattage > 0f) "${"%.1f".format(telemetryData.maxWattage)} W" else "--",
                        accentColor = Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            TelemetryManager.resetStats()
                            Toast.makeText(context, "Telemetry stats reset", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, Color(0xFF333A48)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Reset Stats",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }

        // BLOCK 2: Battery Usage Graph & Top 3 Applications Block (Pixel Stock Design)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141721)),
            border = BorderStroke(1.dp, Color(0xFF262C3D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Stock Android Battery Usage Header Row with Instant Refresh Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Battery usage",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${liveBattery.percentage}%",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = AmoledFontFamily
                        )
                    }

                    // Header Instant Refresh Button
                    IconButton(
                        onClick = {
                            liveBattery = BatteryUtils.getBatteryInfo(context)
                            if (hasUsagePermission) {
                                topApps = BatteryUsageHelper.getTop3AppUsage(context)
                            }
                            Toast.makeText(context, "Battery usage refreshed", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF1E2330), CircleShape)
                            .border(1.dp, Color(0xFF333A48), CircleShape)
                    ) {
                        RefreshCanvasIcon(modifier = Modifier.size(24.dp), color = Color(0xFF60A5FA))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detailed Custom Canvas Battery Level Graph matching Pixel screenshot
                BatteryLevelGraphCard(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    batteryPercentage = liveBattery.percentage,
                    isCharging = liveBattery.isCharging,
                    estimateString = liveBattery.estimateString
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Top 3 Battery Consuming Apps Section
                Text(
                    text = "Top Battery Consuming Applications",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF3B82F6),
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (hasUsagePermission) {
                    if (topApps.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            topApps.forEachIndexed { index, app ->
                                AppUsageRow(
                                    rank = index + 1,
                                    appName = app.appName,
                                    timeStr = app.usageTimeStr,
                                    pctStr = app.percentageEstimateStr
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Analyzing battery usage patterns for active apps...",
                            fontSize = 12.sp,
                            color = Color(0xFF8E9BAE),
                            fontFamily = AmoledFontFamily
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E2330), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Usage Access Required",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Grant usage access permission to view top 3 battery consuming apps.",
                                fontSize = 11.sp,
                                color = Color(0xFF8E9BAE)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { BatteryUsageHelper.openUsageStatsSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(text = "Grant", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
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
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2330)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Open System Battery Settings",
                            fontSize = 12.sp,
                            color = Color(0xFF60A5FA),
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNavigateToAodControls: () -> Unit
) {
    val context = LocalContext.current

    var selectedClockFormat by remember { mutableStateOf(AodPrefs.getClockFormat(context)) }
    var isMediaPlayerEnabled by remember { mutableStateOf(AodPrefs.isMediaPlayerEnabled(context)) }
    var isOledProtectionEnabled by remember { mutableStateOf(AodPrefs.isOledProtectionEnabled(context)) }
    var selectedArcColorHex by remember { mutableStateOf(AodPrefs.getArcColorHex(context)) }

    // Extra Dim Detection
    val isExtraDimSupported = remember { ExtraDimUtils.isExtraDimSupported(context) }
    var isExtraDimEnabled by remember { mutableStateOf(ExtraDimUtils.isExtraDimEnabled(context)) }

    LaunchedEffect(Unit) {
        isExtraDimEnabled = ExtraDimUtils.isExtraDimEnabled(context)
    }

    val scrollState = rememberScrollState()

    val arcColors = listOf(
        Pair("White", "#FFFFFF"),
        Pair("Electric Blue", "#3B82F6"),
        Pair("Emerald Green", "#10B981"),
        Pair("Sunfire Gold", "#F59E0B"),
        Pair("Cyber Purple", "#8B5CF6"),
        Pair("Flame Red", "#EF4444"),
        Pair("Neon Pink", "#EC4899"),
        Pair("Cyan Ice", "#06B6D4"),
        Pair("Amber Orange", "#F97316"),
        Pair("Mint Teal", "#14B8A6")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // Top Bar: Back Button + Title with Dead-Centered Back Canvas Arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF141721), CircleShape)
                    .border(1.dp, Color(0xFF262C3D), CircleShape)
            ) {
                BackCanvasIcon(modifier = Modifier.size(18.dp), color = Color.White)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif
            )
        }

        // Extra Dim Suggestion Banner inside Settings section if supported but disabled
        if (isExtraDimSupported && !isExtraDimEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2330)),
                border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery Saver Tip: ${ExtraDimUtils.getExtraDimFeatureName(context)} Detected",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA),
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Turn on Extra Dim in phone settings to dim AOD display further & maximize battery efficiency.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            fontFamily = AmoledFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { ExtraDimUtils.openExtraDimSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Enable",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // BLOCK 1: 'AOD controls' Section Button / Card with Dead-Centered Forward Arrow
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { onNavigateToAodControls() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181D2D)),
            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AOD controls",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configure Screen Saver, Screen-on/off timers, Reduce Brightness delay, and Telemetry update frequency.",
                        fontSize = 12.sp,
                        color = Color(0xFF8E9BAE),
                        fontFamily = AmoledFontFamily
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1E3A8A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ForwardCanvasIcon(modifier = Modifier.size(16.dp), color = Color(0xFF60A5FA))
                }
            }
        }

        // BLOCK 2: Clock Time Format Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141721)),
            border = BorderStroke(1.dp, Color(0xFF262C3D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Clock Time Format",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select time format to display on AOD screen clock. Default: 24-Hour.",
                    fontSize = 11.sp,
                    color = Color(0xFF8E9BAE),
                    fontFamily = AmoledFontFamily
                )
                Spacer(modifier = Modifier.height(12.dp))

                val formatOptions = listOf("24-Hour", "12-Hour AM/PM")
                formatOptions.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clickable {
                                selectedClockFormat = format
                                AodPrefs.saveClockFormat(context, format)
                                Toast.makeText(context, "Clock format set to $format", Toast.LENGTH_SHORT).show()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedClockFormat == format),
                            onClick = {
                                selectedClockFormat = format
                                AodPrefs.saveClockFormat(context, format)
                                Toast.makeText(context, "Clock format set to $format", Toast.LENGTH_SHORT).show()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF3B82F6))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = format,
                            fontSize = 13.sp,
                            color = Color(0xFFE2E8F0),
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }

        // BLOCK 3: Media Player Widget Option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141721)),
            border = BorderStroke(1.dp, Color(0xFF262C3D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Media Player Widget on AOD",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Show currently playing music title and artist details on AOD screen. Default: Off.",
                            fontSize = 11.sp,
                            color = Color(0xFF8E9BAE),
                            fontFamily = AmoledFontFamily
                        )
                    }

                    Switch(
                        checked = isMediaPlayerEnabled,
                        onCheckedChange = { enabled ->
                            isMediaPlayerEnabled = enabled
                            AodPrefs.setMediaPlayerEnabled(context, enabled)
                            Toast.makeText(context, if (enabled) "Media player on AOD enabled" else "Media player on AOD disabled", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B82F6)
                        )
                    )
                }
            }
        }

        // BLOCK 4: OLED Burn-In Protection Option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141721)),
            border = BorderStroke(1.dp, Color(0xFF262C3D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OLED Burn-In Protection",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Shift pixel positions every minute to prevent OLED burn-in. LCD screen users can turn this OFF to save CPU cycles. Default: On.",
                            fontSize = 11.sp,
                            color = Color(0xFF8E9BAE),
                            fontFamily = AmoledFontFamily
                        )
                    }

                    Switch(
                        checked = isOledProtectionEnabled,
                        onCheckedChange = { enabled ->
                            isOledProtectionEnabled = enabled
                            AodPrefs.setOledProtectionEnabled(context, enabled)
                            Toast.makeText(context, if (enabled) "OLED Protection Enabled" else "OLED Protection Disabled", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B82F6)
                        )
                    )
                }
            }
        }

        // BLOCK 5: Battery Percentage Arc Color Picker
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141721)),
            border = BorderStroke(1.dp, Color(0xFF262C3D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Battery Gauge Arc Color",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose your favorite color for the circular battery arc gauge on the AOD screen. Default: White.",
                    fontSize = 11.sp,
                    color = Color(0xFF8E9BAE),
                    fontFamily = AmoledFontFamily
                )
                Spacer(modifier = Modifier.height(14.dp))

                val chunkedColors = arcColors.chunked(2)
                for (row in chunkedColors) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for ((name, hex) in row) {
                            val colorValue = try {
                                Color(android.graphics.Color.parseColor(hex))
                            } catch (_: Exception) { Color.White }

                            val isSelected = (selectedArcColorHex.equals(hex, ignoreCase = true))

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) Color(0xFF1E2330) else Color(0xFF0F121C),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E2330),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedArcColorHex = hex
                                        AodPrefs.saveArcColorHex(context, hex)
                                        Toast.makeText(context, "Arc color set to $name", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(colorValue, CircleShape)
                                        .border(1.dp, Color.Gray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AodControlsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Screen Saver Block States
    var screenSaverEnabled by remember { mutableStateOf(AodPrefs.isScreenSaverEnabled(context)) }
    var selectedScreenOnTimer by remember { mutableStateOf(AodPrefs.getScreenOnTimer(context)) }
    var selectedScreenOffTimer by remember { mutableStateOf(AodPrefs.getScreenOffTimer(context)) }

    // Reduce Brightness After State
    var selectedBrightnessDelay by remember { mutableStateOf(AodPrefs.getReduceBrightnessAfter(context)) }

    // Telemetry Update Interval State (Default: 1sec)
    var selectedUpdateInterval by remember { mutableStateOf(AodPrefs.getAodUpdateInterval(context)) }

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

    val updateIntervalOptions = listOf(
        "500ms", "1sec", "2sec", "3sec", "5sec", "7sec", "10sec", "15sec"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // Top Bar Header with Dead-Centered Back Canvas Arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF141721), CircleShape)
                    .border(1.dp, Color(0xFF262C3D), CircleShape)
            ) {
                BackCanvasIcon(modifier = Modifier.size(18.dp), color = Color.White)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "AOD Controls",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif
            )
        }

        // BLOCK 1: Screen Saver Block
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
                            if (choice == "Always on") {
                                selectedScreenOffTimer = "Always on"
                            } else {
                                if (selectedScreenOffTimer == "Always on" || selectedScreenOffTimer == "Always off display") {
                                    selectedScreenOffTimer = "10sec"
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
                            if (choice == "Always on") {
                                selectedScreenOnTimer = "Always on"
                            } else {
                                if (selectedScreenOnTimer == "Always on" || selectedScreenOnTimer == "Off") {
                                    selectedScreenOnTimer = "10sec"
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

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
                                    Toast.makeText(
                                        context,
                                        "Screen Saver settings saved successfully",
                                        Toast.LENGTH_SHORT
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
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // BLOCK 2: Reduce Brightness after: Block
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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

        // BLOCK 3: AOD Telemetry Update Frequency
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141721)),
            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "AOD Telemetry Update Frequency",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Set how fast battery current, voltage and wattage refresh on AOD & Charging Telemetry Monitor. Default: 1sec.",
                    fontSize = 11.sp,
                    color = Color(0xFF8E9BAE),
                    fontFamily = AmoledFontFamily
                )

                Spacer(modifier = Modifier.height(14.dp))

                FlowRadioButtonGrid(
                    options = updateIntervalOptions,
                    selectedOption = selectedUpdateInterval,
                    enabled = true,
                    onOptionSelected = { choice ->
                        selectedUpdateInterval = choice
                        AodPrefs.saveAodUpdateInterval(context, choice)
                        Toast.makeText(
                            context,
                            "Telemetry update rate set to $choice",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}

@Composable
fun TelemetryMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    liveValue: String,
    minValue: String,
    maxValue: String,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .background(Color(0xFF0F121C), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1E2330), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = liveValue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = AmoledFontFamily
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Min: $minValue",
                fontSize = 9.sp,
                color = Color(0xFF8E9BAE),
                fontFamily = AmoledFontFamily
            )
            Text(
                text = "Max: $maxValue",
                fontSize = 9.sp,
                color = Color(0xFF8E9BAE),
                fontFamily = AmoledFontFamily
            )
        }
    }
}

@Composable
fun AppUsageRow(
    rank: Int,
    appName: String,
    timeStr: String,
    pctStr: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F121C), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E2330), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF1E3A8A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF60A5FA)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = appName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timeStr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3B82F6),
                fontFamily = AmoledFontFamily
            )
            Text(
                text = pctStr,
                fontSize = 10.sp,
                color = Color(0xFF718096),
                fontFamily = AmoledFontFamily
            )
        }
    }
}

@Composable
fun BatteryLevelGraphCard(
    modifier: Modifier = Modifier,
    batteryPercentage: Int = 18,
    isCharging: Boolean = false,
    estimateString: String = "8 hrs left until full"
) {
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val totalWidth = size.width
            val totalHeight = size.height

            val rightPadding = 38.dp.toPx()
            val bottomPadding = 18.dp.toPx()
            val graphWidth = totalWidth - rightPadding
            val graphHeight = totalHeight - bottomPadding

            // 1. Horizontal Grid Lines and Y-Axis Percentage Labels on the Right (100%, 50%, 0%)
            val yLevels = listOf(Pair(100, 0f), Pair(50, graphHeight * 0.5f), Pair(0, graphHeight))
            for ((pct, y) in yLevels) {
                drawLine(
                    color = Color(0xFF262C3D),
                    start = Offset(0f, y),
                    end = Offset(graphWidth, y),
                    strokeWidth = 1.dp.toPx()
                )

                drawContext.canvas.nativeCanvas.drawText(
                    "$pct%",
                    graphWidth + 8.dp.toPx(),
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#94A3B8")
                        textSize = 10.sp.toPx()
                        typeface = android.graphics.Typeface.SANS_SERIF
                        isAntiAlias = true
                    }
                )
            }

            // 2. X-Axis Labels (24 hrs ago on left, estimate or now on right)
            drawContext.canvas.nativeCanvas.drawText(
                "24 hrs ago",
                0f,
                totalHeight - 2.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = 10.sp.toPx()
                    typeface = android.graphics.Typeface.SANS_SERIF
                    textAlign = android.graphics.Paint.Align.LEFT
                    isAntiAlias = true
                }
            )

            val rightLabel = if (isCharging) {
                if (estimateString.contains("Full", ignoreCase = true)) "Full" else "$estimateString left until full"
            } else {
                "Discharging"
            }

            drawContext.canvas.nativeCanvas.drawText(
                rightLabel,
                graphWidth,
                totalHeight - 2.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = 10.sp.toPx()
                    typeface = android.graphics.Typeface.SANS_SERIF
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )

            // 3. Discharging Segment (Solid Light Blue Line matching Pixel screenshot)
            val point1 = Offset(0f, graphHeight * 0.12f)
            val point2 = Offset(graphWidth * 0.15f, graphHeight * 0.40f)
            val point3 = Offset(graphWidth * 0.32f, graphHeight * 0.52f)
            val point4 = Offset(graphWidth * 0.42f, graphHeight * 0.48f)

            // Break / Gap in line matching Pixel screenshot
            val point5 = Offset(graphWidth * 0.48f, graphHeight * 0.46f)
            val point6 = Offset(graphWidth * 0.70f, graphHeight * 0.60f)
            val point7 = Offset(graphWidth * 0.82f, graphHeight * 0.82f)

            // Current battery level point (e.g. 18%)
            val currentPointY = graphHeight * (1f - (batteryPercentage / 100f).coerceIn(0f, 1f))
            val currentPoint = Offset(graphWidth * 0.88f, currentPointY)

            // Segment A Path
            val segmentA = Path().apply {
                moveTo(point1.x, point1.y)
                cubicTo((point1.x + point2.x) / 2, point1.y, (point1.x + point2.x) / 2, point2.y, point2.x, point2.y)
                cubicTo((point2.x + point3.x) / 2, point2.y, (point2.x + point3.x) / 2, point3.y, point3.x, point3.y)
                lineTo(point4.x, point4.y)
            }

            val fillPathA = Path().apply {
                addPath(segmentA)
                lineTo(point4.x, graphHeight)
                lineTo(point1.x, graphHeight)
                close()
            }

            drawPath(
                path = fillPathA,
                color = Color(0x228AB4F8)
            )

            drawPath(
                path = segmentA,
                color = Color(0xFF8AB4F8),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Segment B Path
            val segmentB = Path().apply {
                moveTo(point5.x, point5.y)
                cubicTo((point5.x + point6.x) / 2, point5.y, (point5.x + point6.x) / 2, point6.y, point6.x, point6.y)
                cubicTo((point6.x + point7.x) / 2, point6.y, (point6.x + point7.x) / 2, point7.y, point7.x, point7.y)
                lineTo(currentPoint.x, currentPoint.y)
            }

            val fillPathB = Path().apply {
                addPath(segmentB)
                lineTo(currentPoint.x, graphHeight)
                lineTo(point5.x, graphHeight)
                close()
            }

            drawPath(
                path = fillPathB,
                color = Color(0x228AB4F8)
            )

            drawPath(
                path = segmentB,
                color = Color(0xFF8AB4F8),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 4. Projected Dotted Line when Charging (matching Pixel screenshot)
            if (isCharging) {
                val targetPoint = Offset(graphWidth, 0f) // 100% full
                val projectionPath = Path().apply {
                    moveTo(currentPoint.x, currentPoint.y)
                    lineTo(targetPoint.x, targetPoint.y)
                }

                drawPath(
                    path = projectionPath,
                    color = Color(0xFFE2E8F0),
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                        cap = StrokeCap.Round
                    )
                )
            }

            // Current level dot node
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = currentPoint)
            drawCircle(color = Color(0xFF8AB4F8), radius = 2.5.dp.toPx(), center = currentPoint)
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

// Dead-Centered Back Canvas Arrow (<)
@Composable
fun BackCanvasIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.62f, h * 0.22f)
            lineTo(w * 0.32f, h * 0.50f)
            lineTo(w * 0.62f, h * 0.78f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// Dead-Centered Forward Canvas Arrow (>)
@Composable
fun ForwardCanvasIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.38f, h * 0.22f)
            lineTo(w * 0.68f, h * 0.50f)
            lineTo(w * 0.38f, h * 0.78f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// Refresh Canvas Icon - Corrected Alignment
@Composable
fun RefreshCanvasIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {

        val strokeWidth = 2.5.dp.toPx()
        val radius = size.minDimension / 2f - strokeWidth
        val centerPoint = center

        val startAngle = 45f
        val sweepAngle = 280f
        val endAngle = startAngle + sweepAngle

        // Circular Refresh Arc
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(
                centerPoint.x - radius,
                centerPoint.y - radius
            ),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )

        // Calculate Arrowhead Position at Arc Endpoint
        val angleRad = Math.toRadians(endAngle.toDouble())

        val tipX = centerPoint.x +
                radius * kotlin.math.cos(angleRad).toFloat()

        val tipY = centerPoint.y +
                radius * kotlin.math.sin(angleRad).toFloat()

        // Arrow Direction
        val tangentX = -kotlin.math.sin(angleRad).toFloat()
        val tangentY = kotlin.math.cos(angleRad).toFloat()

        val normalX = -tangentY
        val normalY = tangentX

        val headLength = 3.dp.toPx()
        val headWidth = 3.dp.toPx()

        val baseX = tipX - tangentX * headLength
        val baseY = tipY - tangentY * headLength

        // Draw Properly Connected Arrowhead
        val headPath = Path().apply {

            moveTo(
                baseX + normalX * headWidth,
                baseY + normalY * headWidth
            )

            lineTo(tipX, tipY)

            lineTo(
                baseX - normalX * headWidth,
                baseY - normalY * headWidth
            )
        }

        drawPath(
            path = headPath,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )
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

private fun startChargingTelemetryService(context: Context) {
    val intent = Intent(context, ChargingTelemetryService::class.java)
    ContextCompat.startForegroundService(context, intent)
}

private fun stopChargingTelemetryService(context: Context) {
    val intent = Intent(context, ChargingTelemetryService::class.java)
    context.stopService(intent)
}
