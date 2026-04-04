package com.yasaDevs.drawingthoughts.presentation.whiteboard.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.yasaDevs.drawingthoughts.presentation.whiteboard.WhiteBoardState

/**
 * Floating HUD for Text Tool (shown when TEXT tool is selected)
 * Uses the EXACT SAME UnifiedTextSettingsBar as the keyboard-attached toolbar.
 * Only difference: Different animation (fadeIn + scaleIn) + NO Done button.
 */
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
        // Use the UNIFIED bar - exact same component as keyboard toolbar
        UnifiedTextSettingsBar(
            state = state,
            onColorClick = onColorClick,
            onFontSizeChange = onFontSizeChange,
            onFontFamilyChange = onFontFamilyChange,
            onFontWeightChange = onFontWeightChange,
            onFontStyleChange = onFontStyleChange,
            showDoneButton = false // NO Done button when tool is selected (not editing)
        )
    }
}
