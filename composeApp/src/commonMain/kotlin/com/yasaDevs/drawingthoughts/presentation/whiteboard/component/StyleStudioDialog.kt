package com.yasaDevs.drawingthoughts.presentation.whiteboard.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yasaDevs.drawingthoughts.utils.ColorPalettes

enum class ColorTarget {
    BACKGROUND, STROKE, FILL
}

/**
 * Main Style Studio Dialog
 * Reference: MainDialog.png
 */
@Composable
fun StyleStudioDialog(
    currentBackgroundColor: Color,
    currentStrokeColor: Color,
    currentFillColor: Color,
    currentStrokeWidth: Float,
    currentAlpha: Float,
    onBackgroundColorChange: (Color) -> Unit,
    onStrokeColorChange: (Color) -> Unit,
    onFillColorChange: (Color) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }
    var customPickerTarget by remember { mutableStateOf<ColorTarget?>(null) }
    var customPickerInitialColor by remember { mutableStateOf(Color.Black) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2B2B2B)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Title
                Text(
                    text = "Untitled",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Background Colors
                ColorPresetRow(
                    label = "Background",
                    colors = ColorPalettes.backgroundColors,
                    selectedColor = currentBackgroundColor,
                    onColorSelected = onBackgroundColorChange,
                    onCustomPickerClick = {
                        customPickerTarget = ColorTarget.BACKGROUND
                        customPickerInitialColor = currentBackgroundColor
                        showCustomPicker = true
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Stroke Colors
                ColorPresetRow(
                    label = "Stroke",
                    colors = ColorPalettes.strokeColors,
                    selectedColor = currentStrokeColor,
                    onColorSelected = onStrokeColorChange,
                    onCustomPickerClick = {
                        customPickerTarget = ColorTarget.STROKE
                        customPickerInitialColor = currentStrokeColor
                        showCustomPicker = true
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Fill Colors
                ColorPresetRow(
                    label = "Fill",
                    colors = ColorPalettes.fillColors,
                    selectedColor = currentFillColor,
                    onColorSelected = onFillColorChange,
                    onCustomPickerClick = {
                        customPickerTarget = ColorTarget.FILL
                        customPickerInitialColor = currentFillColor
                        showCustomPicker = true
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Stroke Width Slider
                StrokeWidthSlider(
                    strokeWidth = currentStrokeWidth,
                    color = currentStrokeColor,
                    onStrokeWidthChange = onStrokeWidthChange
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Alpha Slider
                AlphaSlider(
                    alpha = currentAlpha,
                    color = currentStrokeColor,
                    onAlphaChange = onAlphaChange
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5B9FFF)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Done",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Custom Color Picker Dialog
    if (showCustomPicker && customPickerTarget != null) {
        CustomColorPickerDialog(
            initialColor = customPickerInitialColor,
            onDismiss = { showCustomPicker = false },
            onColorSelected = { selectedColor ->
                when (customPickerTarget) {
                    ColorTarget.BACKGROUND -> onBackgroundColorChange(selectedColor)
                    ColorTarget.STROKE -> onStrokeColorChange(selectedColor)
                    ColorTarget.FILL -> onFillColorChange(selectedColor)
                    null -> {}
                }
                showCustomPicker = false
            }
        )
    }
}

@Composable
private fun ColorPresetRow(
    label: String,
    colors: List<Color>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onCustomPickerClick: () -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(colors) { color ->
                ColorSwatch(
                    color = color,
                    isSelected = color == selectedColor,
                    onClick = { onColorSelected(color) }
                )
            }

            // Rainbow icon for custom picker
            item {
                RainbowPickerIcon(onClick = onCustomPickerClick)
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            // Determine if color is light or dark based on RGB values
            val isLightColor = (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f) > 0.5f
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (isLightColor) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun RainbowPickerIcon(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rainbowColors = listOf(
                Color(0xFFFF0000),
                Color(0xFFFFFF00),
                Color(0xFF00FF00),
                Color(0xFF00FFFF),
                Color(0xFF0000FF),
                Color(0xFFFF00FF),
                Color(0xFFFF0000)
            )
            val gradient = Brush.sweepGradient(colors = rainbowColors)
            drawCircle(gradient)
        }
    }
}

@Composable
private fun StrokeWidthSlider(
    strokeWidth: Float,
    color: Color,
    onStrokeWidthChange: (Float) -> Unit
) {
    Column {
        Text(
            text = "Stroke Width",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dynamic preview circles
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((strokeWidth * 2).dp.coerceAtMost(56.dp))
                        .clip(CircleShape)
                        .background(color)
                )
            }

            Slider(
                value = strokeWidth,
                onValueChange = onStrokeWidthChange,
                valueRange = 1f..50f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF5B9FFF),
                    activeTrackColor = Color(0xFF5B9FFF),
                    inactiveTrackColor = Color(0xFF4A4A4A)
                )
            )
        }
    }
}

@Composable
private fun AlphaSlider(
    alpha: Float,
    color: Color,
    onAlphaChange: (Float) -> Unit
) {
    Column {
        Text(
            text = "Alpha",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gradient = Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0f),
                            color.copy(alpha = 1f)
                        )
                    )
                    drawRoundRect(gradient, cornerRadius = androidx.compose.ui.geometry.CornerRadius(48f))

                    // Draw thumb position indicator
                    val thumbX = alpha * size.width
                    drawCircle(
                        color = Color.White,
                        radius = 24f,
                        center = Offset(thumbX, size.height / 2),
                        style = Stroke(width = 3f)
                    )
                }

                Slider(
                    value = alpha,
                    onValueChange = onAlphaChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxSize(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Transparent,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }
        }
    }
}
