package org.example.project.presentation.whiteboard.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.example.project.utils.HSVColor

/**
 * Custom Color Picker Dialog with HSV selection
 * Reference: CustomPicker.png
 */
@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var hsvColor by remember { mutableStateOf(HSVColor.fromColor(initialColor)) }
    val currentColor by remember { derivedStateOf { hsvColor.toColor() } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2B2B2B)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title with preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose Color",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )

                    // Color preview circle
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Saturation/Value Box
                SaturationValueBox(
                    hue = hsvColor.hue,
                    saturation = hsvColor.saturation,
                    value = hsvColor.value,
                    onSaturationValueChange = { s, v ->
                        hsvColor = hsvColor.copy(saturation = s, value = v)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Hue Slider
                HueSlider(
                    hue = hsvColor.hue,
                    onHueChange = { newHue ->
                        hsvColor = hsvColor.copy(hue = newHue)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB8C5D6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", color = Color(0xFF2B2B2B))
                    }

                    Button(
                        onClick = { onColorSelected(currentColor) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5B9FFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Select", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Saturation/Value selection box
 */
@Composable
private fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbPosition by remember(saturation, value) {
        mutableStateOf(Offset(saturation, 1f - value))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val position = change.position
                        val x = (position.x / size.width).coerceIn(0f, 1f)
                        val y = (position.y / size.height).coerceIn(0f, 1f)
                        thumbPosition = Offset(x, y)
                        onSaturationValueChange(x, 1f - y)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val x = (offset.x / size.width).coerceIn(0f, 1f)
                        val y = (offset.y / size.height).coerceIn(0f, 1f)
                        thumbPosition = Offset(x, y)
                        onSaturationValueChange(x, 1f - y)
                    }
                }
        ) {
            // Get the base color for current hue
            val baseColor = HSVColor(hue, 1f, 1f, 1f).toColor()

            // Draw white to color gradient (saturation)
            val saturationGradient = Brush.horizontalGradient(
                colors = listOf(Color.White, baseColor)
            )
            drawRect(saturationGradient)

            // Draw black overlay (value)
            val valueGradient = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black)
            )
            drawRect(valueGradient)

            // Draw thumb indicator
            val thumbX = thumbPosition.x * size.width
            val thumbY = thumbPosition.y * size.height
            drawCircle(
                color = Color.White,
                radius = 16f,
                center = Offset(thumbX, thumbY),
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = 12f,
                center = Offset(thumbX, thumbY),
                style = Stroke(width = 2f)
            )
        }
    }
}

/**
 * Hue slider with rainbow gradient
 */
@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember(hue) { mutableStateOf(hue / 360f) }

    Box(
        modifier = modifier.clip(RoundedCornerShape(24.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val position = (change.position.x / size.width).coerceIn(0f, 1f)
                        sliderPosition = position
                        onHueChange(position * 360f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val position = (offset.x / size.width).coerceIn(0f, 1f)
                        sliderPosition = position
                        onHueChange(position * 360f)
                    }
                }
        ) {
            // Rainbow gradient
            val rainbowColors = listOf(
                Color(0xFFFF0000), // Red
                Color(0xFFFFFF00), // Yellow
                Color(0xFF00FF00), // Green
                Color(0xFF00FFFF), // Cyan
                Color(0xFF0000FF), // Blue
                Color(0xFFFF00FF), // Magenta
                Color(0xFFFF0000)  // Red
            )
            val gradient = Brush.horizontalGradient(colors = rainbowColors)
            drawRect(gradient)

            // Draw thumb indicator
            val thumbX = sliderPosition * size.width
            val thumbY = size.height / 2f
            drawCircle(
                color = Color.White,
                radius = size.height / 2f + 4f,
                center = Offset(thumbX, thumbY),
                style = Stroke(width = 4f)
            )
        }
    }
}
