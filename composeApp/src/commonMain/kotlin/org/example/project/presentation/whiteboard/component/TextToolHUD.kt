package org.example.project.presentation.whiteboard.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import org.example.project.presentation.whiteboard.WhiteBoardState

@Composable
fun TextToolHUD(
    state: WhiteBoardState,
    visible: Boolean,
    modifier: Modifier = Modifier,
    onColorClick: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontFamilyChange: (FontFamily) -> Unit,
    onFontWeightChange: (FontWeight) -> Unit,
    onFontStyleChange: (FontStyle) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn() + slideInVertically { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // First row: Color and Font Size
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color Dot
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(state.currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                            .clickable { onColorClick() }
                    )

                    // Font Size Selector
                    Text(
                        text = "Size:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(12f, 16f, 20f, 24f, 32f, 40f, 48f, 64f).forEach { size ->
                            val isSelected = state.textFontSize == size
                            Text(
                                text = size.toInt().toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else
                                            androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    .clickable { onFontSizeChange(size) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Second row: Font Styles (scrollable)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Style:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
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
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = family,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else
                                            androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    .clickable { onFontFamilyChange(family) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
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
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = weight,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else
                                            androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    .clickable { onFontWeightChange(weight) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
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
                                style = MaterialTheme.typography.labelMedium,
                                fontStyle = style,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else
                                            androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    .clickable { onFontStyleChange(style) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
