package org.example.project.presentation.whiteboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.presentation.whiteboard.WhiteBoardState

/**
 * UNIFIED Text Settings Bar
 * This is the SINGLE source of truth for text tool settings UI.
 * Used in BOTH contexts:
 * 1. When TEXT tool is selected (floating HUD)
 * 2. When editing text (keyboard-attached toolbar)
 *
 * Contains FULL settings: Color, Font Size, Font Family, Font Weight, Font Style, Done Button
 */
@Composable
fun UnifiedTextSettingsBar(
    state: WhiteBoardState,
    modifier: Modifier = Modifier,
    onColorClick: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontFamilyChange: (FontFamily) -> Unit,
    onFontWeightChange: (FontWeight) -> Unit,
    onFontStyleChange: (FontStyle) -> Unit,
    onDoneClick: (() -> Unit)? = null, // Only shown when editing (non-null)
    showDoneButton: Boolean = false
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Color + Font Size + Done Button
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Get the actual text color from currentShape if editing
                val displayColor = (state.currentShape as? org.example.project.domain.model.DrawnShape.Text)?.color
                    ?: state.currentColor

                ColorDot(
                    color = displayColor,
                    onClick = onColorClick,
                    size = 32
                )

                FontSizeRow(
                    selectedSize = state.textFontSize,
                    onSizeChange = onFontSizeChange,
                    modifier = Modifier.weight(1f)
                )

                // Done button (only shown during editing)
                if (showDoneButton && onDoneClick != null) {
                    FilledIconButton(
                        onClick = onDoneClick,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Row 2: Font Family + Font Weight + Font Style (FULL options)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Style:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Font Family Options
                    listOf(
                        "Default" to FontFamily.Default,
                        "Serif" to FontFamily.Serif,
                        "Sans" to FontFamily.SansSerif,
                        "Mono" to FontFamily.Monospace,
                        "Cursive" to FontFamily.Cursive
                    ).forEach { (name, family) ->
                        val isSelected = state.textFontFamily == family
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = family,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                                .clickable { onFontFamilyChange(family) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // Font Weight Options
                    listOf(
                        "Light" to FontWeight.Light,
                        "Normal" to FontWeight.Normal,
                        "Bold" to FontWeight.Bold
                    ).forEach { (name, weight) ->
                        val isSelected = state.textFontWeight == weight
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = weight,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                                .clickable { onFontWeightChange(weight) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // Font Style Options
                    listOf(
                        "Normal" to FontStyle.Normal,
                        "Italic" to FontStyle.Italic
                    ).forEach { (name, style) ->
                        val isSelected = state.textFontStyle == style
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelLarge,
                            fontStyle = style,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                                .clickable { onFontStyleChange(style) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
