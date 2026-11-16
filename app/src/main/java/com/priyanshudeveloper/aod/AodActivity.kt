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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.priyanshudeveloper.aod.phoenixengine.core.AodManager
import com.priyanshudeveloper.aod.phoenixengine.core.AodPrefs
import com.priyanshudeveloper.aod.phoenixengine.core.BatteryUtils
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

    private val powerButtonReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AodManager.setAodActivity(this)

        setupAodWindow()
        enableEdgeToEdge()

        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        registerReceiver(powerButtonReceiver, filter)

        setContent {
            AODTheme {
                AodScreen(onDismiss = { finish() })
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Physical Power Button or User Navigation -> Dismiss AOD Activity like Triple Tap
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            toggleTorch(this, false)
            unregisterReceiver(powerButtonReceiver)
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
        layoutParams.screenBrightness = brightness.coerceIn(0.001f, 1.0f)
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
    val batteryInfo by AodManager.batteryInfo.collectAsState()

    var currentTimeText by remember { mutableStateOf("") }
    var currentDateText by remember { mutableStateOf("") }

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    var isTorchOn by remember { mutableStateOf(false) }
    var lastSettingsTapTime by remember { mutableLongStateOf(0L) }

    var serviceElapsedSeconds by remember { mutableLongStateOf(0L) }

    // Screen Saver Sleep Phase State (true = Screen OFF phase, false = Screen ON phase)
    var isScreenSaverOffPhase by remember { mutableStateOf(false) }

    // Screen Saver & Reduce Brightness Dynamic Runtime Loop
    LaunchedEffect(Unit) {
        val isSaverEnabled = AodPrefs.isScreenSaverEnabled(context)
        val screenOnMs = AodPrefs.timerToMillis(AodPrefs.getScreenOnTimer(context))
        val screenOffMs = AodPrefs.timerToMillis(AodPrefs.getScreenOffTimer(context))

        val reduceChoice = AodPrefs.getReduceBrightnessAfter(context)
        val reduceDelayMs = AodPrefs.timerToMillis(reduceChoice)

        val activity = context as? AodActivity

        if (isSaverEnabled && screenOnMs > 0L && screenOffMs > 0L) {
            var isFirstCycle = true
            // SCREEN SAVER MODE: Continuous Screen-ON & Screen-OFF cycle
            while (isActive) {
                // PHASE 1: Screen-ON Phase (Information is visible)
                isScreenSaverOffPhase = false

                if (isFirstCycle) {
                    activity?.setWindowBrightness(0.5f)
                    val reduceJob = launch {
                        if (reduceDelayMs > 0L && reduceDelayMs < screenOnMs) {
                            delay(reduceDelayMs)
                            activity?.setWindowBrightness(0.01f)
                        }
                    }
                    delay(screenOnMs)
                    reduceJob.cancel()
                    isFirstCycle = false
                } else {
                    activity?.setWindowBrightness(0.01f)
                    delay(screenOnMs)
                }

                // PHASE 2: Screen-OFF Phase (Everything turns black; AMOLED/OLED pixels turn off physically, LCD backlight remains awake)
                isScreenSaverOffPhase = true
                activity?.setWindowBrightness(0.001f)
                delay(screenOffMs)
            }
        } else if (isSaverEnabled && screenOnMs == 0L) {
            // Screen-ON timer is "Off": stay in black screen-off phase
            isScreenSaverOffPhase = true
            activity?.setWindowBrightness(0.001f)
        } else {
            // STANDARD ALWAYS-ON DISPLAY MODE (Continuous screen-on)
            isScreenSaverOffPhase = false
            activity?.setWindowBrightness(0.5f)
            if (reduceDelayMs > 0L) {
                delay(reduceDelayMs)
                activity?.setWindowBrightness(0.01f)
            }
        }
    }

    // Service duration timer update every second using persistent AodManager start time
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
        targetValue = shiftX,
        animationSpec = tween(durationMillis = 1000),
        label = "shiftXAnimation"
    )
    val animatedShiftY by animateDpAsState(
        targetValue = shiftY,
        animationSpec = tween(durationMillis = 1000),
        label = "shiftYAnimation"
    )

    // AMOLED Burn-In Protection: shift position slightly every minute (60 seconds)
    LaunchedEffect(Unit) {
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
    }

    // Always update clock and battery charging data every second on AOD
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeText = timeFormat.format(now)
            currentDateText = dateFormat.format(now)

            // Force refresh battery charging data every second
            AodManager.batteryInfo.value = BatteryUtils.getBatteryInfo(context)

            delay(1000)
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
                            // Turn off torch when dismissing AOD
                            if (isTorchOn) {
                                isTorchOn = false
                                toggleTorch(context, false)
                            }
                            onDismiss()
                        }
                    }
                )
            }
            .padding(horizontal = 20.dp, vertical = 32.dp)
    ) {
        if (!isScreenSaverOffPhase) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = animatedShiftX, y = animatedShiftY),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(16.dp))

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

                // Circular Arc Gauge with Curved Arc Header OUTSIDE ABOVE (Timer & Current)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(260.dp)
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

                        // 2. Active Arc
                        val activeSweep = (animatedPercentage / 100f) * totalSweep
                        drawArc(
                            color = Color(0xFF3B82F6),
                            startAngle = startAngle,
                            sweepAngle = activeSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 3. Curved Arc Text OUTSIDE ABOVE the Circular Gauge (Service Timer & Real-time Current)
                        val curvedArcRadius = 122.dp.toPx()
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
                            -30f,
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
                                            // Ignore if settings cannot be opened
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

        // Flashlight Head
        val headPath = Path().apply {
            moveTo(w * 0.3f, h * 0.25f)
            lineTo(w * 0.7f, h * 0.25f)
            lineTo(w * 0.62f, h * 0.42f)
            lineTo(w * 0.38f, h * 0.42f)
            close()
        }
        drawPath(path = headPath, color = iconColor, style = Stroke(width = 1.8.dp.toPx()))

        // Flashlight Body
        val bodyPath = Path().apply {
            moveTo(w * 0.38f, h * 0.42f)
            lineTo(w * 0.62f, h * 0.42f)
            lineTo(w * 0.58f, h * 0.88f)
            lineTo(w * 0.42f, h * 0.88f)
            close()
        }
        drawPath(path = bodyPath, color = iconColor, style = Stroke(width = 1.8.dp.toPx()))

        if (isOn) {
            // Light rays when ON
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
