package com.yasaDevs.drawingthoughts.presentation.whiteboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasaDevs.drawingthoughts.domain.model.DrawnShape
import com.yasaDevs.drawingthoughts.presentation.whiteboard.WhiteBoardState
import kotlinx.coroutines.delay

/**
 * Unified Text Editing Overlay.
 *
 * This component solves the UI overlap bug by merging the BasicTextField and
 * the formatting toolbar into a SINGLE unified bottom-anchored panel.
 */
@Composable
fun TextEditingLayer(
    state: WhiteBoardState,
    text: String,
    shape: DrawnShape.Text,
    zoom: Float,
    pan: androidx.compose.ui.geometry.Offset,
    color: Color,
    onTextChange: (String) -> Unit,
    onColorClick: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontFamilyChange: (androidx.compose.ui.text.font.FontFamily) -> Unit,
    onFontWeightChange: (androidx.compose.ui.text.font.FontWeight) -> Unit,
    onFontStyleChange: (androidx.compose.ui.text.font.FontStyle) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(shape.id) {
        delay(50L)
        focusRequester.requestFocus()
    }

    val displayColor = when {
        color == Color.White && shape.color == Color.White -> MaterialTheme.colorScheme.onSurface
        else -> color
    }

    val editFontSize = (shape.fontSize * zoom).coerceIn(14f, 42f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        // Unified Modal Panel above keyboard
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.ime)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures { /* consume */ }
                },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Minimal Input Area
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            fontSize = editFontSize.sp,
                            fontFamily = shape.fontFamily,
                            fontWeight = shape.fontWeight,
                            fontStyle = shape.fontStyle,
                            color = displayColor,
                            lineHeight = (editFontSize * 1.3f).sp
                        ),
                        cursorBrush = SolidColor(displayColor),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onDismiss() }
                        ),
                        decorationBox = { innerTextField ->
                            if (text.isEmpty()) {
                                Text(
                                    text = "Start typing...",
                                    style = TextStyle(
                                        fontSize = editFontSize.sp,
                                        fontFamily = shape.fontFamily,
                                        color = displayColor.copy(alpha = 0.3f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Unified Formatting Bar (No extra Surface here, sits directly on parent)
                UnifiedTextSettingsBar(
                    state = state,
                    onColorClick = onColorClick,
                    onFontSizeChange = onFontSizeChange,
                    onFontFamilyChange = onFontFamilyChange,
                    onFontWeightChange = onFontWeightChange,
                    onFontStyleChange = onFontStyleChange,
                    onDoneClick = onDismiss,
                    showDoneButton = true,
                    // Use transparent background since we are already inside a Surface
                    modifier = Modifier.background(Color.Transparent)
                )

                // Extra safety spacing for system bars
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
