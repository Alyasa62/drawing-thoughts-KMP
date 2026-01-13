package org.example.project.presentation.whiteboard.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.project.domain.model.DrawingTool
import org.jetbrains.compose.resources.painterResource

/**
 * Shape Variants Menu
 *
 * Shows related shape variants when a base shape is selected.
 * Clean, minimal UI with smooth animations.
 */
@Composable
fun ShapeVariantsMenu(
    currentTool: DrawingTool,
    isVisible: Boolean,
    onToolSelected: (DrawingTool) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Show only variants from the current tool's family
                val variants = getShapeVariants(currentTool)
                variants.forEach { shape ->
                    ShapeVariantItem(
                        tool = shape,
                        isSelected = shape == currentTool,
                        onClick = { onToolSelected(shape) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShapeVariantItem(
    tool: DrawingTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(tool.res),
            contentDescription = tool.name,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                   else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun getShapeVariants(tool: DrawingTool): List<DrawingTool> {
    return when (tool.getShapeFamily()) {
        "rectangle" -> listOf(
            DrawingTool.RECTANGLE_OUTLINED,
            DrawingTool.RECTANGLE_FILLED,
            DrawingTool.RECTANGLE_ROUNDED
        )
        "circle" -> listOf(
            DrawingTool.ELLIPSE_OUTLINED,
            DrawingTool.ELLIPSE_FILLED
        )
        "triangle" -> listOf(
            DrawingTool.TRIANGLE_OUTLINED,
            DrawingTool.TRIANGLE_FILLED
        )
        "line" -> listOf(
            DrawingTool.LINE_PLANE,
            DrawingTool.LINE_DOTTED
        )
        "arrow" -> listOf(
            DrawingTool.ARROW_ONE_SIDED,
            DrawingTool.ARROW_TWO_SIDED
        )
        "polygon" -> listOf(
            DrawingTool.DIAMOND,
            DrawingTool.STAR_OUTLINED,
            DrawingTool.STAR_FILLED,
            DrawingTool.PENTAGON,
            DrawingTool.HEXAGON
        )
        else -> emptyList()
    }
}
