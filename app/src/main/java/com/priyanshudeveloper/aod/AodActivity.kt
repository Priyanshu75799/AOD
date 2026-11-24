package com.priyanshudeveloper.aod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.priyanshudeveloper.aod.phoenixengine.core.AodManager
import com.priyanshudeveloper.aod.phoenixengine.core.AodPrefs
import com.priyanshudeveloper.aod.phoenixengine.core.BatteryUtils
import com.priyanshudeveloper.aod.phoenixengine.core.ExtraDimUtils
import com.priyanshudeveloper.aod.phoenixengine.core.MediaHelper
import com.priyanshudeveloper.aod.phoenixengine.core.MediaInfo
import com.priyanshudeveloper.aod.ui.theme.AODTheme
import com.priyanshudeveloper.aod.ui.theme.AmoledFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

class AodActivity : ComponentActivity() {

    private val screenEventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_USER_PRESENT,
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_SCREEN_ON,
                @Suppress("DEPRECATION")
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AodManager.setAodActivity(this)

        setupAodWindow()
        enableEdgeToEdge()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            @Suppress("DEPRECATION")
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenEventsReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(screenEventsReceiver, filter)
        }

        setContent {
            AODTheme {
                AodScreen(onDismiss = { finish() })
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_POWER) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            toggleTorch(this, false)
            unregisterReceiver(screenEventsReceiver)
        } catch (_: Exception) {
            // Ignore
        }
        AodManager.setAodActivity(null)
    }

    private fun setupAodWindow() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun setWindowBrightness(brightness: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness.coerceIn(0.0005f, 1.0f)
        window.attributes = layoutParams
    }
}

fun toggleTorch(context: Context, turnOn: Boolean): Boolean {
    return try {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        if (cameraId != null) {
            cameraManager.setTorchMode(cameraId, turnOn)
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}

@Composable
fun AodScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var batteryInfo by remember { mutableStateOf(BatteryUtils.getBatteryInfo(context)) }

    var currentTimeText by remember { mutableStateOf("") }
    var currentDateText by remember { mutableStateOf("") }

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    var isTorchOn by remember { mutableStateOf(false) }
    var lastSettingsTapTime by remember { mutableLongStateOf(0L) }

    var serviceElapsedSeconds by remember { mutableLongStateOf(0L) }

    // Screen Saver Sleep Phase State (true = Screen OFF phase, false = Screen ON phase)
    var isScreenSaverOffPhase by remember { mutableStateOf(false) }

    // Read Preferences for Clock Format, Arc Color, OLED Protection, Media Player, and Refresh Rate
    val clockFormatPref = remember { AodPrefs.getClockFormat(context) }
    val isMediaPlayerOptionEnabled = remember { AodPrefs.isMediaPlayerEnabled(context) }
    val isOledProtectionOptionEnabled = remember { AodPrefs.isOledProtectionEnabled(context) }
    val arcColorHexPref = remember { AodPrefs.getArcColorHex(context) }
    val updateIntervalMs = remember { AodPrefs.getAodUpdateIntervalMillis(context) }

    val isExtraDimActive = remember { ExtraDimUtils.isExtraDimEnabled(context) }

    var mediaInfo by remember { mutableStateOf(MediaInfo()) }

    // Parse Battery Arc Color
    val activeArcColor = remember(arcColorHexPref) {
        try {
            Color(android.graphics.Color.parseColor(arcColorHexPref))
        } catch (_: Exception) {
            Color.White
        }
    }

    // Screen Saver & Reduce Brightness Dynamic Runtime Loop
    LaunchedEffect(Unit) {
        val isSaverEnabled = AodPrefs.isScreenSaverEnabled(context)
        val screenOnMs = AodPrefs.timerToMillis(AodPrefs.getScreenOnTimer(context))
        val screenOffMs = AodPrefs.timerToMillis(AodPrefs.getScreenOffTimer(context))

        val reduceChoice = AodPrefs.getReduceBrightnessAfter(context)
        val reduceDelayMs = AodPrefs.timerToMillis(reduceChoice)

        val activity = context as? AodActivity
        val minDimBrightness = if (isExtraDimActive) 0.0005f else 0.01f

        if (isSaverEnabled && screenOnMs > 0L && screenOffMs > 0L) {
            var isFirstCycle = true
            while (isActive) {
                isScreenSaverOffPhase = false

                if (isFirstCycle) {
                    activity?.setWindowBrightness(0.5f)
                    val reduceJob = launch {
                        if (reduceDelayMs > 0L && reduceDelayMs < screenOnMs) {
                            delay(reduceDelayMs)
                            activity?.setWindowBrightness(minDimBrightness)
                        }
                    }
                    delay(screenOnMs)
                    reduceJob.cancel()
                    isFirstCycle = false
                } else {
                    activity?.setWindowBrightness(minDimBrightness)
                    delay(screenOnMs)
                }

                isScreenSaverOffPhase = true
                activity?.setWindowBrightness(0.0005f)
                delay(screenOffMs)
            }
        } else if (isSaverEnabled && screenOnMs == 0L) {
            isScreenSaverOffPhase = true
            activity?.setWindowBrightness(0.0005f)
        } else {
            isScreenSaverOffPhase = false
            activity?.setWindowBrightness(0.5f)
            if (reduceDelayMs > 0L) {
                delay(reduceDelayMs)
                activity?.setWindowBrightness(minDimBrightness)
            }
        }
    }

    // Service duration timer update
    LaunchedEffect(Unit) {
        if (AodManager.serviceStartTimeMs == 0L) {
            AodManager.serviceStartTimeMs = System.currentTimeMillis()
            AodManager.initialServiceBatteryPercentage = batteryInfo.percentage
        }
        val startTime = AodManager.serviceStartTimeMs
        while (isActive) {
            val now = System.currentTimeMillis()
            serviceElapsedSeconds = (now - startTime) / 1000L
            delay(1000L)
        }
    }

    // AMOLED Display Protection System - Pixel Shifting Offsets
    var shiftX by remember { mutableStateOf(0.dp) }
    var shiftY by remember { mutableStateOf(0.dp) }

    val animatedShiftX by animateDpAsState(
        targetValue = if (isOledProtectionOptionEnabled) shiftX else 0.dp,
        animationSpec = tween(durationMillis = 1000),
        label = "shiftXAnimation"
    )
    val animatedShiftY by animateDpAsState(
        targetValue = if (isOledProtectionOptionEnabled) shiftY else 0.dp,
        animationSpec = tween(durationMillis = 1000),
        label = "shiftYAnimation"
    )

    // Pixel shifting loop (runs only if OLED Protection is enabled)
    LaunchedEffect(isOledProtectionOptionEnabled) {
        if (isOledProtectionOptionEnabled) {
            while (isActive) {
                delay(60000L)
                var newX: Int
                var newY: Int
                do {
                    newX = (-12..12).random()
                    newY = (-12..12).random()
                } while (newX == shiftX.value.toInt() && newY == shiftY.value.toInt())

                shiftX = newX.dp
                shiftY = newY.dp
            }
        } else {
            shiftX = 0.dp
            shiftY = 0.dp
        }
    }

    // Dynamic AOD Telemetry & Clock Update Loop strictly adhering to user's selected update frequency
    LaunchedEffect(updateIntervalMs) {
        val timePattern = if (clockFormatPref == "12-Hour AM/PM") "h:mm a" else "HH:mm"
        val timeFormat = SimpleDateFormat(timePattern, Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

        while (isActive) {
            val now = Date()
            currentTimeText = timeFormat.format(now)
            currentDateText = dateFormat.format(now)

            // Refresh Battery Charging Data strictly at user-configured rate
            batteryInfo = BatteryUtils.getBatteryInfo(context)

            // Refresh Media Info if feature enabled
            if (isMediaPlayerOptionEnabled) {
                mediaInfo = MediaHelper.getCurrentMediaInfo(context)
            }

            delay(updateIntervalMs)
        }
    }

    val animatedPercentage by animateFloatAsState(
        targetValue = batteryInfo.percentage.toFloat(),
        animationSpec = tween(durationMillis = 800),
        label = "batteryPercentage"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTapTime < 500L) {
                            tapCount++
                        } else {
                            tapCount = 1
                        }
                        lastTapTime = currentTime

                        if (tapCount >= 3) {
                            if (isTorchOn) {
                                isTorchOn = false
                                toggleTorch(context, false)
                            }
                            onDismiss()
                        }
                    }
                )
            }
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        if (!isScreenSaverOffPhase) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = animatedShiftX, y = animatedShiftY),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Time & Date Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentTimeText,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = AmoledFontFamily,
                        color = Color(0xFFE2E8F0),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentDateText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = AmoledFontFamily,
                        color = Color(0xFF8E9BAE)
                    )
                }

                // Media Player Card (If enabled and playing)
                if (isMediaPlayerOptionEnabled && mediaInfo.isPlaying) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(Color(0xFF0F121C), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF1E2330), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🎵 ${mediaInfo.title} • ${mediaInfo.artist}",
                            fontSize = 11.sp,
                            color = Color(0xFF60A5FA),
                            fontFamily = AmoledFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Circular Arc Gauge with Custom Color & Curved Arc Header
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(250.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 10.dp.toPx()
                        val startAngle = 135f
                        val totalSweep = 270f

                        // 1. Track Arc
                        drawArc(
                            color = Color(0xFF1E2330),
                            startAngle = startAngle,
                            sweepAngle = totalSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 2. Active Arc with User-Selected Color
                        val activeSweep = (animatedPercentage / 100f) * totalSweep
                        drawArc(
                            color = activeArcColor,
                            startAngle = startAngle,
                            sweepAngle = activeSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 3. Curved Arc Text OUTSIDE ABOVE the Circular Gauge
                        val curvedArcRadius = 118.dp.toPx()
                        val androidPath = android.graphics.Path().apply {
                            addArc(
                                center.x - curvedArcRadius,
                                center.y - curvedArcRadius,
                                center.x + curvedArcRadius,
                                center.y + curvedArcRadius,
                                205f,
                                130f
                            )
                        }

                        val serviceHours = serviceElapsedSeconds / 3600
                        val serviceMins = (serviceElapsedSeconds % 3600) / 60
                        val serviceSecs = serviceElapsedSeconds % 60
                        val timerText = if (serviceHours > 0) {
                            "%02d:%02d:%02d".format(serviceHours, serviceMins, serviceSecs)
                        } else {
                            "%02d:%02d".format(serviceMins, serviceSecs)
                        }

                        val initialPercent = if (AodManager.initialServiceBatteryPercentage >= 0) {
                            AodManager.initialServiceBatteryPercentage
                        } else {
                            batteryInfo.percentage
                        }
                        val batteryGained = (batteryInfo.percentage - initialPercent).coerceAtLeast(0)

                        val curvedText = "Time $timerText  •  +$batteryGained%  •  ${batteryInfo.currentMa} mA"

                        drawContext.canvas.nativeCanvas.drawTextOnPath(
                            curvedText,
                            androidPath,
                            0f,
                            -28f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#8E9BAE")
                                textSize = 10.sp.toPx()
                                typeface = android.graphics.Typeface.MONOSPACE
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${batteryInfo.percentage}%",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AmoledFontFamily,
                            color = Color(0xFFE2E8F0)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = batteryInfo.chargingStatusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = AmoledFontFamily,
                            color = Color(0xFF8E9BAE),
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                // 3 Card Containers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AodInfoCard(
                        modifier = Modifier.weight(1f),
                        value = "${"%.1f".format(batteryInfo.temperatureCelsius)}°C",
                        label = "Battery temperature"
                    )
                    AodInfoCard(
                        modifier = Modifier.weight(1f),
                        value = "${batteryInfo.voltageMv} mV",
                        label = "Battery voltage"
                    )
                    AodInfoCard(
                        modifier = Modifier.weight(1f),
                        value = batteryInfo.estimateString,
                        label = "Estimate"
                    )
                }

                // Bottom Wattage & Status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${"%.1f".format(batteryInfo.wattage)} W",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = AmoledFontFamily,
                        color = Color(0xFFE2E8F0)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = batteryInfo.chargingSpeedCategory,
                        fontSize = 12.sp,
                        fontFamily = AmoledFontFamily,
                        color = Color(0xFF8E9BAE)
                    )
                }

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Torch Button (Tap-and-Hold to turn ON, Single tap to turn OFF)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isTorchOn) Color(0xFF3B82F6) else Color(0xFF141721), CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        if (isTorchOn) {
                                            isTorchOn = false
                                            toggleTorch(context, false)
                                        }
                                    },
                                    onLongPress = {
                                        if (!isTorchOn) {
                                            isTorchOn = true
                                            toggleTorch(context, true)
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        TorchCanvasIcon(
                            modifier = Modifier.size(20.dp),
                            isOn = isTorchOn
                        )
                    }

                    Text(
                        text = "Triple tap to dismiss",
                        fontSize = 10.sp,
                        fontFamily = AmoledFontFamily,
                        color = Color(0xFF475569)
                    )

                    // Settings Button (Requires Double-Tap)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF141721), CircleShape)
                            .clickable {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastSettingsTapTime < 500L) {
                                    try {
                                        val batteryIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(batteryIntent)
                                    } catch (_: Exception) {
                                        try {
                                            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(settingsIntent)
                                        } catch (_: Exception) {
                                            // Ignore
                                        }
                                    }
                                    lastSettingsTapTime = 0L
                                } else {
                                    lastSettingsTapTime = currentTime
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        SettingsCanvasIcon(modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Extra Dimming Scrim Overlay if Extra Dim is active on device
        if (isExtraDimActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
fun AodInfoCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Box(
        modifier = modifier
            .background(Color(0xFF0F121C), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E2330), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AmoledFontFamily,
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontFamily = AmoledFontFamily,
                color = Color(0xFF718096),
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun TorchCanvasIcon(modifier: Modifier = Modifier, isOn: Boolean = false) {
    val iconColor = if (isOn) Color.White else Color(0xFF64748B)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val headPath = Path().apply {
            moveTo(w * 0.3f, h * 0.25f)
            lineTo(w * 0.7f, h * 0.25f)
            lineTo(w * 0.62f, h * 0.42f)
            lineTo(w * 0.38f, h * 0.42f)
            close()
        }
        drawPath(path = headPath, color = iconColor, style = Stroke(width = 1.8.dp.toPx()))

        val bodyPath = Path().apply {
            moveTo(w * 0.38f, h * 0.42f)
            lineTo(w * 0.62f, h * 0.42f)
            lineTo(w * 0.58f, h * 0.88f)
            lineTo(w * 0.42f, h * 0.88f)
            close()
        }
        drawPath(path = bodyPath, color = iconColor, style = Stroke(width = 1.8.dp.toPx()))

        if (isOn) {
            drawLine(iconColor, Offset(w * 0.3f, h * 0.18f), Offset(w * 0.2f, h * 0.06f), strokeWidth = 1.8.dp.toPx())
            drawLine(iconColor, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.04f), strokeWidth = 1.8.dp.toPx())
            drawLine(iconColor, Offset(w * 0.7f, h * 0.18f), Offset(w * 0.8f, h * 0.06f), strokeWidth = 1.8.dp.toPx())
        }
    }
}

@Composable
fun SettingsCanvasIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF64748B)) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 3.2f
        val centerPoint = center
        drawCircle(
            color = color,
            radius = radius,
            center = centerPoint,
            style = Stroke(width = 1.8.dp.toPx())
        )
        for (i in 0 until 6) {
            val angle = Math.toRadians(i * 60.0)
            val startX = centerPoint.x + (radius * cos(angle)).toFloat()
            val startY = centerPoint.y + (radius * sin(angle)).toFloat()
            val endX = centerPoint.x + ((radius + 3.dp.toPx()) * cos(angle)).toFloat()
            val endY = centerPoint.y + ((radius + 3.dp.toPx()) * sin(angle)).toFloat()
            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
