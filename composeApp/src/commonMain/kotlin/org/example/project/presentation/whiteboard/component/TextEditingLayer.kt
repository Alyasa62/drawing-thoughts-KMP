package org.example.project.presentation.whiteboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-screen text editing layer that appears when editing text.
 * Provides an immersive editing experience with the keyboard and toolbar.
 */
@Composable
fun TextEditingLayer(
    text: String,
    fontSize: Float,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontStyle: FontStyle,
    color: androidx.compose.ui.graphics.Color,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    // Request focus when the layer appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Ensure good contrast - use white background for better visibility
    val backgroundColor = Color.White

    // Calculate contrasting text color if needed
    // Use black text if color is too light, too transparent, or white
    val displayColor = if (color == Color.White ||
                           color.alpha < 0.3f ||
                           (color.red > 0.9f && color.green > 0.9f && color.blue > 0.9f)) {
        Color.Black
    } else {
        color
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                // Tap outside to dismiss (commit)
                if (text.isNotBlank()) {
                    onDismiss()
                }
            },
        contentAlignment = Alignment.TopCenter // Changed from Center to TopCenter
    ) {
        // Text input field - positioned at top-middle
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp) // Added top padding
                .focusRequester(focusRequester)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Prevent tap from dismissing */ },
            textStyle = TextStyle(
                fontSize = fontSize.sp,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                color = displayColor,
                lineHeight = (fontSize * 1.4f).sp,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(displayColor),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "Tap to type...",
                            style = TextStyle(
                                fontSize = fontSize.sp,
                                fontFamily = fontFamily,
                                fontWeight = fontWeight,
                                fontStyle = fontStyle,
                                color = displayColor.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
