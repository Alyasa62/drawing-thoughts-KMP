package org.example.project.presentation.whiteboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Professional Color Picker Dialog
 *
 * Features:
 * - Vast variety of colors organized in categories
 * - Quick access to common colors
 * - Material Design color palette
 * - Custom color section
 * - Clean, fast, and effective UI
 */
@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(360.dp)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Text(
                    text = "Choose Color",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Common") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Material") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("All") }
                    )
                }

                // Color Grid
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> CommonColorsGrid(currentColor, onColorSelected)
                        1 -> MaterialColorsGrid(currentColor, onColorSelected)
                        2 -> AllColorsGrid(currentColor, onColorSelected)
                    }
                }

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommonColorsGrid(
    currentColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val commonColors = remember {
        listOf(
            // Grayscale
            Color.Black, Color(0xFF424242), Color(0xFF757575),
            Color(0xFFBDBDBD), Color(0xFFE0E0E0), Color.White,

            // Primary Colors
            Color.Red, Color(0xFFFF5722), Color(0xFFFF9800),
            Color(0xFFFFC107), Color(0xFFFFEB3B), Color(0xFFCDDC39),

            // Secondary Colors
            Color(0xFF8BC34A), Color(0xFF4CAF50), Color(0xFF009688),
            Color(0xFF00BCD4), Color(0xFF03A9F4), Color(0xFF2196F3),

            // Tertiary Colors
            Color(0xFF3F51B5), Color(0xFF673AB7), Color(0xFF9C27B0),
            Color(0xFFE91E63), Color(0xFFF44336), Color(0xFF795548)
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(commonColors) { color ->
            ColorCircle(
                color = color,
                isSelected = color == currentColor,
                onClick = { onColorSelected(color) }
            )
        }
    }
}

@Composable
private fun MaterialColorsGrid(
    currentColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val materialColors = remember {
        listOf(
            // Red shades
            Color(0xFFFFEBEE), Color(0xFFFFCDD2), Color(0xFFEF9A9A), Color(0xFFE57373),
            Color(0xFFEF5350), Color(0xFFF44336), Color(0xFFE53935), Color(0xFFD32F2F),

            // Pink shades
            Color(0xFFFCE4EC), Color(0xFFF8BBD0), Color(0xFFF48FB1), Color(0xFFF06292),
            Color(0xFFEC407A), Color(0xFFE91E63), Color(0xFFD81B60), Color(0xFFC2185B),

            // Purple shades
            Color(0xFFF3E5F5), Color(0xFFE1BEE7), Color(0xFFCE93D8), Color(0xFFBA68C8),
            Color(0xFFAB47BC), Color(0xFF9C27B0), Color(0xFF8E24AA), Color(0xFF7B1FA2),

            // Deep Purple shades
            Color(0xFFEDE7F6), Color(0xFFD1C4E9), Color(0xFFB39DDB), Color(0xFF9575CD),
            Color(0xFF7E57C2), Color(0xFF673AB7), Color(0xFF5E35B1), Color(0xFF512DA8),

            // Indigo shades
            Color(0xFFE8EAF6), Color(0xFFC5CAE9), Color(0xFF9FA8DA), Color(0xFF7986CB),
            Color(0xFF5C6BC0), Color(0xFF3F51B5), Color(0xFF3949AB), Color(0xFF303F9F),

            // Blue shades
            Color(0xFFE3F2FD), Color(0xFFBBDEFB), Color(0xFF90CAF9), Color(0xFF64B5F6),
            Color(0xFF42A5F5), Color(0xFF2196F3), Color(0xFF1E88E5), Color(0xFF1976D2),

            // Cyan shades
            Color(0xFFE0F7FA), Color(0xFFB2EBF2), Color(0xFF80DEEA), Color(0xFF4DD0E1),
            Color(0xFF26C6DA), Color(0xFF00BCD4), Color(0xFF00ACC1), Color(0xFF0097A7),

            // Teal shades
            Color(0xFFE0F2F1), Color(0xFFB2DFDB), Color(0xFF80CBC4), Color(0xFF4DB6AC),
            Color(0xFF26A69A), Color(0xFF009688), Color(0xFF00897B), Color(0xFF00796B),

            // Green shades
            Color(0xFFE8F5E9), Color(0xFFC8E6C9), Color(0xFFA5D6A7), Color(0xFF81C784),
            Color(0xFF66BB6A), Color(0xFF4CAF50), Color(0xFF43A047), Color(0xFF388E3C),

            // Light Green shades
            Color(0xFFF1F8E9), Color(0xFFDCEDC8), Color(0xFFC5E1A5), Color(0xFFAED581),
            Color(0xFF9CCC65), Color(0xFF8BC34A), Color(0xFF7CB342), Color(0xFF689F38),

            // Lime shades
            Color(0xFFF9FBE7), Color(0xFFF0F4C3), Color(0xFFE6EE9C), Color(0xFFDCE775),
            Color(0xFFD4E157), Color(0xFFCDDC39), Color(0xFFC0CA33), Color(0xFFAFB42B),

            // Yellow shades
            Color(0xFFFFFDE7), Color(0xFFFFF9C4), Color(0xFFFFF59D), Color(0xFFFFF176),
            Color(0xFFFFEE58), Color(0xFFFFEB3B), Color(0xFFFDD835), Color(0xFFFBC02D),

            // Amber shades
            Color(0xFFFFF8E1), Color(0xFFFFECB3), Color(0xFFFFE082), Color(0xFFFFD54F),
            Color(0xFFFFCA28), Color(0xFFFFC107), Color(0xFFFFB300), Color(0xFFFFA000),

            // Orange shades
            Color(0xFFFFF3E0), Color(0xFFFFE0B2), Color(0xFFFFCC80), Color(0xFFFFB74D),
            Color(0xFFFFA726), Color(0xFFFF9800), Color(0xFFFB8C00), Color(0xFFF57C00),

            // Deep Orange shades
            Color(0xFFFBE9E7), Color(0xFFFFCCBC), Color(0xFFFFAB91), Color(0xFFFF8A65),
            Color(0xFFFF7043), Color(0xFFFF5722), Color(0xFFF4511E), Color(0xFFE64A19),

            // Brown shades
            Color(0xFFEFEBE9), Color(0xFFD7CCC8), Color(0xFFBCAAA4), Color(0xFFA1887F),
            Color(0xFF8D6E63), Color(0xFF795548), Color(0xFF6D4C41), Color(0xFF5D4037),

            // Grey shades
            Color(0xFFFAFAFA), Color(0xFFF5F5F5), Color(0xFFEEEEEE), Color(0xFFE0E0E0),
            Color(0xFFBDBDBD), Color(0xFF9E9E9E), Color(0xFF757575), Color(0xFF616161),

            // Blue Grey shades
            Color(0xFFECEFF1), Color(0xFFCFD8DC), Color(0xFFB0BEC5), Color(0xFF90A4AE),
            Color(0xFF78909C), Color(0xFF607D8B), Color(0xFF546E7A), Color(0xFF455A64)
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(materialColors) { color ->
            ColorCircle(
                color = color,
                isSelected = color == currentColor,
                onClick = { onColorSelected(color) },
                size = 32.dp
            )
        }
    }
}

@Composable
private fun AllColorsGrid(
    currentColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val allColors = remember {
        buildList {
            // Add common colors
            addAll(listOf(
                Color.Black, Color.White,
                Color.Red, Color.Green, Color.Blue,
                Color.Yellow, Color.Cyan, Color.Magenta
            ))

            // Generate gradient colors
            for (hue in 0 until 360 step 30) {
                for (saturation in listOf(0.3f, 0.6f, 1.0f)) {
                    for (value in listOf(0.5f, 0.75f, 1.0f)) {
                        add(Color.hsv(hue.toFloat(), saturation, value))
                    }
                }
            }

            // Add grayscale
            for (i in 0..10) {
                val gray = (i * 255 / 10).toFloat() / 255f
                add(Color(gray, gray, gray))
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(9),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(allColors) { color ->
            ColorCircle(
                color = color,
                isSelected = color == currentColor,
                onClick = { onColorSelected(color) },
                size = 28.dp
            )
        }
    }
}

@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    size: Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF18A0FB) else Color.Gray.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
