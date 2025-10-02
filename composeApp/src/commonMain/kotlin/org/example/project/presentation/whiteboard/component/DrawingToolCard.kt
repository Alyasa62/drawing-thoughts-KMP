package org.example.project.presentation.whiteboard.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.project.domain.model.DrawingTool
import org.jetbrains.compose.resources.painterResource

@Composable
fun DrawingToolCard(
    modifier: Modifier = Modifier,
    selectedTool: DrawingTool,
    onToolSelected: (DrawingTool) -> Unit,
    onClosedIconClick: () -> Unit,
    isVisible: Boolean
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = slideInVertically(tween(durationMillis = 500) ) { h -> h },
        exit = slideOutVertically(tween(durationMillis = 500) ) { h -> h }

    ) {
        ElevatedCard {
            Row(
                modifier = modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ){
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(
                        DrawingTool.entries
                    ){
                            drawingTool ->
                        DrawingToolItem(
                            drawingTool= drawingTool,
                            isSelected = selectedTool == drawingTool,
                            onToolClick = { onToolSelected(drawingTool) }
                        )

                    }
                }
                FilledIconButton(onClick = { onClosedIconClick() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

    }

}

@Composable
private fun DrawingToolItem(
    modifier: Modifier = Modifier,
    drawingTool: DrawingTool,
    isSelected: Boolean,
    onToolClick: () -> Unit
) {
    val imageIcon = listOf(
        DrawingTool.PEN,
        DrawingTool.ERASER,
        DrawingTool.HIGHLIGHTER,
        DrawingTool.LASER_PEN
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = { onToolClick() }
        ) {
            Icon(
                modifier = Modifier.size(25.dp),
                painter = painterResource(drawingTool.res),
                contentDescription = drawingTool.name,
                tint = if(imageIcon.contains(drawingTool)) {
                    Color.Unspecified
                } else {
                    LocalContentColor.current
                }

            )
        }
        if(isSelected) {
            Box(
                modifier = Modifier.
                background(LocalContentColor.current)
                    .size(25.dp, 1 .dp)
            )
        }
    }
}
