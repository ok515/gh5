package com.pecmi.studio.presentation.dialogs

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

import com.pecmi.studio.ui.language.LocalAppStrings

@Composable
fun ColorWheelPickerDialog(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current

    // Parse initial color into HSV + Alpha
    val initialHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor, hsv)
        hsv
    }
    val initialAlpha = remember(initialColor) {
        ((initialColor ushr 24) and 0xFF) / 255f
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember { mutableFloatStateOf(initialAlpha) }

    var showHexDialog by remember { mutableStateOf(false) }

    val currentColor = Color.hsv(hue, saturation, value, alpha)
    val currentColorArgb = currentColor.toArgb()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Color Swatch Preview (Circle with Checkerboard background)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.LightGray, CircleShape)
                ) {
                    CheckerboardBackground(modifier = Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(currentColor)
                    )
                }

                // Hex and ARGB Display Bar
                val hexCode = String.format("#%08X", currentColorArgb)
                val alphaInt = (alpha * 255).toInt()
                val redInt = (currentColor.red * 255).toInt()
                val greenInt = (currentColor.green * 255).toInt()
                val blueInt = (currentColor.blue * 255).toInt()
                val argbText = "$hexCode - ($alphaInt,$redInt,$greenInt,$blueInt)"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = argbText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray,
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                // Default color reset or preset eyedropper action
                                hue = 0f
                                saturation = 1f
                                value = 1f
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Colorize,
                                contentDescription = "Eyedropper",
                                tint = Color(0xFF616161),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showHexDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Hex",
                                tint = Color(0xFF616161),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Main Circular Color Wheel
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val sizePx = with(LocalDensity.current) { min(maxWidth.toPx(), 260.dp.toPx()) }
                    val wheelRadius = sizePx / 2f

                    Box(
                        modifier = Modifier
                            .size(with(LocalDensity.current) { sizePx.toDp() })
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    fun updateFromTouch(touchPos: Offset) {
                                        val dx = touchPos.x - wheelRadius
                                        val dy = touchPos.y - wheelRadius
                                        val dist = hypot(dx, dy)
                                        val clampedDist = min(dist, wheelRadius)

                                        var angleDeg = (atan2(dy, dx) * 180f / PI).toFloat()
                                        if (angleDeg < 0) angleDeg += 360f

                                        hue = angleDeg
                                        saturation = (clampedDist / wheelRadius).coerceIn(0f, 1f)
                                    }

                                    detectTapGestures { offset ->
                                        updateFromTouch(offset)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        val touchPos = change.position
                                        val dx = touchPos.x - wheelRadius
                                        val dy = touchPos.y - wheelRadius
                                        val dist = hypot(dx, dy)
                                        val clampedDist = min(dist, wheelRadius)

                                        var angleDeg = (atan2(dy, dx) * 180f / PI).toFloat()
                                        if (angleDeg < 0) angleDeg += 360f

                                        hue = angleDeg
                                        saturation = (clampedDist / wheelRadius).coerceIn(0f, 1f)
                                    }
                                }
                        ) {
                            val center = Offset(wheelRadius, wheelRadius)

                            // Sweep gradient for Hues
                            val rainbowColors = listOf(
                                Color.Red, Color.Yellow, Color.Green,
                                Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                            )
                            drawCircle(
                                brush = Brush.sweepGradient(rainbowColors, center),
                                radius = wheelRadius,
                                center = center
                            )

                            // Radial gradient for Saturation (White at center -> Transparent at outer edge)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                                    center = center,
                                    radius = wheelRadius
                                ),
                                radius = wheelRadius,
                                center = center
                            )

                            // Darken overlay according to Value (Brightness)
                            if (value < 1f) {
                                drawCircle(
                                    color = Color.Black.copy(alpha = 1f - value),
                                    radius = wheelRadius,
                                    center = center
                                )
                            }

                            // Wheel border
                            drawCircle(
                                color = Color.Gray.copy(alpha = 0.5f),
                                radius = wheelRadius,
                                center = center,
                                style = Stroke(width = 2f)
                            )

                            // Touch Indicator Circle
                            val handleAngleRad = hue * PI / 180f
                            val handleDist = saturation * wheelRadius
                            val handleX = center.x + handleDist * cos(handleAngleRad).toFloat()
                            val handleY = center.y + handleDist * sin(handleAngleRad).toFloat()

                            drawCircle(
                                color = Color.DarkGray,
                                radius = 11.dp.toPx(),
                                center = Offset(handleX, handleY),
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 9.dp.toPx(),
                                center = Offset(handleX, handleY),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                // Sliders Below Wheel
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Slider 1: Value / Brightness (Black -> Bright)
                    val valueBrush = remember(hue, saturation) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.hsv(hue, saturation, 0f),
                                Color.hsv(hue, saturation, 1f)
                            )
                        )
                    }
                    CustomGradientSlider(
                        value = value,
                        onValueChange = { value = it },
                        trackBrush = valueBrush
                    )

                    // Slider 2: Saturation / Shade (White/Gray -> Full Color)
                    val satBrush = remember(hue, value) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.hsv(hue, 0f, value),
                                Color.hsv(hue, 1f, value)
                            )
                        )
                    }
                    CustomGradientSlider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        trackBrush = satBrush
                    )

                    // Slider 3: Opacity / Alpha (Checkerboard + Transparent -> Opaque)
                    val alphaBrush = remember(hue, saturation, value) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.hsv(hue, saturation, value, 0f),
                                Color.hsv(hue, saturation, value, 1f)
                            )
                        )
                    }
                    CustomGradientSlider(
                        value = alpha,
                        onValueChange = { alpha = it },
                        trackBrush = alphaBrush,
                        isCheckerboard = true
                    )
                }

                // Action Buttons Row (OK / Cancel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel, fontSize = 16.sp, color = Color(0xFF00897B), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        onClick = {
                            onColorSelected(currentColorArgb)
                            onDismiss()
                        }
                    ) {
                        Text(strings.ok, fontSize = 16.sp, color = Color(0xFF00897B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Direct Hex Edit Dialog
    if (showHexDialog) {
        var hexInput by remember { mutableStateOf(String.format("%08X", currentColorArgb)) }

        AlertDialog(
            onDismissRequest = { showHexDialog = false },
            title = { Text(strings.enterHexColor) },
            text = {
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { hexInput = it },
                    label = { Text("Hex e.g. FFFF0000") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val parsed = android.graphics.Color.parseColor(
                                if (hexInput.startsWith("#")) hexInput else "#$hexInput"
                            )
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(parsed, hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2]
                            alpha = ((parsed ushr 24) and 0xFF) / 255f
                        } catch (e: Exception) {
                            // Invalid hex
                        }
                        showHexDialog = false
                    }
                ) {
                    Text(strings.ok)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHexDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun CustomGradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    trackBrush: Brush,
    isCheckerboard: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
    ) {
        if (isCheckerboard) {
            CheckerboardBackground(modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(trackBrush)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CheckerboardBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val squareSizePx = 6.dp.toPx()
        val cols = (size.width / squareSizePx).toInt() + 1
        val rows = (size.height / squareSizePx).toInt() + 1

        val lightPaint = Color(0xFFE0E0E0)
        val darkPaint = Color(0xFFBDBDBD)

        for (i in 0 until cols) {
            for (j in 0 until rows) {
                val color = if ((i + j) % 2 == 0) lightPaint else darkPaint
                drawRect(
                    color = color,
                    topLeft = Offset(i * squareSizePx, j * squareSizePx),
                    size = androidx.compose.ui.geometry.Size(squareSizePx, squareSizePx)
                )
            }
        }
    }
}
