package com.yasaDevs.drawingthoughts.presentation.whiteboard.component

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

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import com.yasaDevs.drawingthoughts.domain.model.DrawnShape
import kotlinx.coroutines.delay

/**
 * In-place text editing layer.
 * Renders a completely transparent BasicTextField floating at the exact X,Y Canvas offset.
 */
@Composable
fun TextEditingLayer(
    text: String,
    shape: DrawnShape.Text,
    zoom: Float,
    pan: Offset,
    color: androidx.compose.ui.graphics.Color,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    // Request focus AFTER layout pass so keyboard slides up smoothly (Fix 2: Choppy keyboard).
    // Keyed on shape.id so re-triggers correctly when switching between text shapes.
    LaunchedEffect(shape.id) {
        delay(50L) // give Compose one frame to measure the offset before asking for IME
        focusRequester.requestFocus()
    }

    // Calculate Screen Coordinates precisely based on Canvas viewport states
    val screenX = (shape.position.x * zoom) + pan.x
    val screenY = (shape.position.y * zoom) + pan.y
    val displayFontSize = shape.fontSize * zoom

    // Use black text if color is too light against standard canvas, or just use what they chose
    val displayColor = if (color == Color.White && shape.color == Color.White) {
        Color.Black
    } else {
        color
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onDismiss()
                    }
                )
            },
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .offset { IntOffset(screenX.toInt(), screenY.toInt()) }
                .width(IntrinsicSize.Min) // Wrap to content
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                fontSize = displayFontSize.sp,
                fontFamily = shape.fontFamily,
                fontWeight = shape.fontWeight,
                fontStyle = shape.fontStyle,
                color = displayColor,
                lineHeight = (displayFontSize * 1.4f).sp,
                textAlign = TextAlign.Start // Canvas renders from TopLeft usually
            ),
            cursorBrush = SolidColor(displayColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { onDismiss() }
            )
        )
    }
}

