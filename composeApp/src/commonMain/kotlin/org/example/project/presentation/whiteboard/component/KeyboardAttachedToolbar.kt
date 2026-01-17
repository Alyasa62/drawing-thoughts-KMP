package org.example.project.presentation.whiteboard.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.example.project.presentation.whiteboard.WhiteBoardState

/**
 * Keyboard-attached toolbar that appears above the IME keyboard during text editing.
 * Uses shared TextSettingsRow components for UI consistency.
 */
@Composable
fun KeyboardAttachedToolbar(
    state: WhiteBoardState,
    visible: Boolean,
    modifier: Modifier = Modifier,
    onColorClick: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontFamilyChange: (FontFamily) -> Unit,
    onDoneClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime) // Anchors above keyboard
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: Color + Font Size + Done Button (using shared components)
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
                        onClick = onColorClick
                    )

                    FontSizeRow(
                        selectedSize = state.textFontSize,
                        onSizeChange = onFontSizeChange,
                        modifier = Modifier.weight(1f)
                    )

                    // Done button (Checkmark)
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

                // Row 2: Font Family (using shared components)
                FontFamilyRow(
                    selectedFamily = state.textFontFamily,
                    onFamilyChange = onFontFamilyChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
