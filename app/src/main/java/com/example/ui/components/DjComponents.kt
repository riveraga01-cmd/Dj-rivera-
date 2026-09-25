package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Theme Colors
val DjBgDark = Color(0xFF0A0A0C)
val DjPanelDark = Color(0xFF1A1D24)
val DjCardDark = Color(0xFF222632)
val DjCyan = Color(0xFF00E5FF)
val DjOrange = Color(0xFFFF6D00)
val DjGreen = Color(0xFF00E676)
val DjRed = Color(0xFFFF1744)
val DjAmber = Color(0xFFFFD600)
val DjBorder = Color(0xFF2D3344)
val DjTextMuted = Color(0xFF8E95A5)

/**
 * 1. ROTARY KNOB: Hardware-style rotary dial with interactive drag, neon arc and readout
 */
@Composable
fun RotaryKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    label: String = "",
    displayValue: String = "",
    accentColor: Color = DjCyan,
    size: Dp = 56.dp,
    bipolar: Boolean = false,
    testTag: String = "rotary_knob"
) {
    val totalRange = (range.endInclusive - range.start).coerceAtLeast(0.001f)
    val normalizedValue = ((value - range.start) / totalRange).coerceIn(0f, 1f)

    Column(
        modifier = modifier.testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                color = DjTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        Box(
            modifier = Modifier
                .size(size)
                .pointerInput(range) {
                    detectVerticalDragGestures { _, dragAmount ->
                        val delta = -dragAmount / 180f
                        val newValue = (value + delta * totalRange).coerceIn(range.start, range.endInclusive)
                        onValueChange(newValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 4.dp.toPx()
                val radius = (this.size.minDimension - strokeW * 2) / 2
                val center = Offset(this.size.width / 2, this.size.height / 2)

                // Arc angles: 135 deg to 405 deg (270 degrees sweep)
                val startAngle = 135f
                val sweepAngle = 270f

                // Background track arc
                drawArc(
                    color = Color(0xFF282D3C),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // Active glowing arc
                if (bipolar) {
                    val centerAngle = startAngle + sweepAngle / 2
                    val currentAngleSweep = (normalizedValue - 0.5f) * sweepAngle
                    drawArc(
                        color = accentColor,
                        startAngle = centerAngle,
                        sweepAngle = currentAngleSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeW + 1f, cap = StrokeCap.Round)
                    )
                } else {
                    drawArc(
                        color = accentColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * normalizedValue,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeW + 1f, cap = StrokeCap.Round)
                    )
                }

                // Inner knob body with metallic brushed bevel
                val knobRadius = radius * 0.76f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF32384A), Color(0xFF181B23)),
                        center = center,
                        radius = knobRadius
                    ),
                    radius = knobRadius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF4A526B),
                    radius = knobRadius,
                    center = center,
                    style = Stroke(width = 1.5f)
                )

                // Pointer notch
                val pointerAngleDeg = startAngle + sweepAngle * normalizedValue
                val pointerAngleRad = pointerAngleDeg * (PI / 180f)
                val innerPoint = Offset(
                    center.x + (knobRadius * 0.40f) * cos(pointerAngleRad).toFloat(),
                    center.y + (knobRadius * 0.40f) * sin(pointerAngleRad).toFloat()
                )
                val outerPoint = Offset(
                    center.x + (knobRadius * 0.90f) * cos(pointerAngleRad).toFloat(),
                    center.y + (knobRadius * 0.90f) * sin(pointerAngleRad).toFloat()
                )
                drawLine(
                    color = accentColor,
                    start = innerPoint,
                    end = outerPoint,
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }

        if (displayValue.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = displayValue,
                color = accentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 2. VERTICAL FADER: Hardware-style mixer channel fader with dB ticks and glowing cap
 */
@Composable
fun VerticalFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    accentColor: Color = DjCyan,
    height: Dp = 130.dp,
    testTag: String = "vertical_fader"
) {
    val clampedValue = value.coerceIn(0f, 1f)

    Column(
        modifier = modifier.testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                color = DjTextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Box(
            modifier = Modifier
                .width(42.dp)
                .height(height)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        val localY = change.position.y
                        val normalized = 1f - (localY / size.height.toFloat()).coerceIn(0f, 1f)
                        onValueChange(normalized)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Background slot and dB ticks
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val centerX = w / 2

                // Center slot groove
                drawRoundRect(
                    color = Color(0xFF101217),
                    topLeft = Offset(centerX - 3.dp.toPx(), 6.dp.toPx()),
                    size = Size(6.dp.toPx(), h - 12.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                // Calibration ticks: +10, 0, -6, -12, -24, -inf
                val ticks = listOf(0.08f, 0.25f, 0.45f, 0.65f, 0.85f, 0.95f)
                ticks.forEach { t ->
                    val y = h * (1f - t)
                    // Left tick
                    drawLine(
                        color = Color(0xFF384054),
                        start = Offset(centerX - 16.dp.toPx(), y),
                        end = Offset(centerX - 6.dp.toPx(), y),
                        strokeWidth = 1.2f
                    )
                    // Right tick
                    drawLine(
                        color = Color(0xFF384054),
                        start = Offset(centerX + 6.dp.toPx(), y),
                        end = Offset(centerX + 16.dp.toPx(), y),
                        strokeWidth = 1.2f
                    )
                }
            }

            // Fader Handle / Cap
            val capHeight = 26.dp
            val capWidth = 36.dp
            val usableHeight = height - capHeight

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(capWidth),
                contentAlignment = Alignment.TopCenter
            ) {
                val offsetY = usableHeight * (1f - clampedValue)

                Box(
                    modifier = Modifier
                        .offset(y = offsetY)
                        .size(width = capWidth, height = capHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF3E4558), Color(0xFF1E222D), Color(0xFF12141C))
                            )
                        )
                        .border(1.dp, Color(0xFF555F7A), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Center neon line on the fader cap
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(2.5.dp)
                            .background(accentColor, shape = CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${(clampedValue * 100).toInt()}%",
            color = accentColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 3. VU METER LED: Multi-segment LED bar: Green (safe) -> Amber (warning) -> Red (peak)
 */
@Composable
fun VuMeterLed(
    level: Float,
    modifier: Modifier = Modifier,
    segments: Int = 12,
    width: Dp = 10.dp,
    height: Dp = 100.dp,
    testTag: String = "vu_meter_led"
) {
    val clampedLevel = level.coerceIn(0f, 1f)
    val activeCount = (clampedLevel * segments).toInt()

    Column(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF0F1117))
            .border(1.dp, Color(0xFF262C3C), RoundedCornerShape(3.dp))
            .padding(1.5.dp)
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.Bottom)
    ) {
        for (i in (segments - 1) downTo 0) {
            val isActive = i < activeCount
            val segmentColor = when {
                i >= segments - 2 -> DjRed // Top 2 peak (Red)
                i >= segments - 5 -> DjAmber // Mid-high (Amber/Yellow)
                else -> DjGreen // Lower (Green)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (isActive) segmentColor else segmentColor.copy(alpha = 0.15f)
                    )
            )
        }
    }
}

/**
 * 4. PAD BUTTON: Backlit MPC-style performance pad with glowing borders & active press
 */
@Composable
fun PadButton(
    title: String,
    subtitle: String = "",
    accentColor: Color = DjCyan,
    isActive: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    testTag: String = "pad_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = tween(durationMillis = 80),
        label = "pad_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    if (isActive || isPressed) {
                        listOf(accentColor.copy(alpha = 0.35f), Color(0xFF1E2333))
                    } else {
                        listOf(Color(0xFF262B3A), Color(0xFF141722))
                    }
                )
            )
            .border(
                width = if (isActive || isPressed) 2.dp else 1.dp,
                color = if (isActive || isPressed) accentColor else Color(0xFF384056),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isActive) accentColor else accentColor.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = if (isActive) Color.White else accentColor,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = DjTextMuted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 5. STATUS SWITCH: Professional illuminated toggle/rocker switch
 */
@Composable
fun StatusSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    activeColor: Color = DjGreen,
    inactiveColor: Color = DjRed,
    testTag: String = "status_switch"
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161922))
            .border(1.dp, Color(0xFF2A3142), RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Physical casing with glowing LED indicator dot
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (checked) activeColor.copy(alpha = 0.25f) else Color(0xFF1E222E))
                .border(1.dp, if (checked) activeColor else Color(0xFF404960), RoundedCornerShape(8.dp)),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (checked) activeColor else inactiveColor)
            )
        }

        if (label.isNotBlank()) {
            Text(
                text = label,
                color = if (checked) Color.White else DjTextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
