package com.yasaDevs.drawingthoughts.presentation.whiteboard.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.yasaDevs.drawingthoughts.presentation.whiteboard.WhiteBoardState

/**
 * Keyboard-attached toolbar that appears above the IME keyboard during text editing.
 * Uses the EXACT SAME UnifiedTextSettingsBar as the floating HUD.
 * Only difference: IME padding to anchor above keyboard + Done button is visible.
 */
@Composable
fun KeyboardAttachedToolbar(
    state: WhiteBoardState,
    visible: Boolean,
    modifier: Modifier = Modifier,
    onColorClick: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontFamilyChange: (FontFamily) -> Unit,
    onFontWeightChange: (FontWeight) -> Unit,
    onFontStyleChange: (FontStyle) -> Unit,
    onDoneClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime) // CRITICAL: Anchors above keyboard
    ) {
        // Use the UNIFIED bar - exact same component as floating HUD
        UnifiedTextSettingsBar(
            state = state,
            onColorClick = onColorClick,
            onFontSizeChange = onFontSizeChange,
            onFontFamilyChange = onFontFamilyChange,
            onFontWeightChange = onFontWeightChange,
            onFontStyleChange = onFontStyleChange,
            onDoneClick = onDoneClick,
            showDoneButton = true // Show Done button when editing
        )
    }
}
