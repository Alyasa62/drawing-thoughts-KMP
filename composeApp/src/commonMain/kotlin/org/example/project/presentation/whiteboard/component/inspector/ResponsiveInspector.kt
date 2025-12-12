package org.example.project.presentation.whiteboard.component.inspector

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import org.example.project.presentation.whiteboard.WhiteBoardEvent
import org.example.project.presentation.whiteboard.WhiteBoardState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun ResponsiveInspector(
    state: WhiteBoardState,
    onEvent: (WhiteBoardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val isWide = maxWidth > 600.dp
        
        if (isWide) {
            // --- DESKTOP / TABLET MODE (Right Sidebar) ---
            Row(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()) {
                UnifiedInspectorPanel(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .shadow(elevation = 8.dp)
                )
            }
        } else {
            // --- MOBILE MODE (Bottom Sheet) ---
            // Only show if needed? For now, always show docked at bottom or triggered?
            // "Expanded State: Pull up". For MVP, fixed height bottom bar (collapsed) or full.
            // Let's make it a fixed bottom panel for now 
            // that sits ABOVE the other UI or integrates with it.
            // User: "Draggable Bottom Sheet".
            // Implementation: Simple Surface at bottom alignment.
            
            // --- MOBILE MODE (Bottom Sheet) ---
            var isExpanded by remember { mutableStateOf(true) }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .shadow(16.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                tonalElevation = 8.dp
            ) {
                Column(
                     horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                         onClick = { isExpanded = !isExpanded },
                         modifier = Modifier.fillMaxWidth().height(32.dp)
                    ) {
                         Icon(
                             imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                             contentDescription = "Expand/Collapse"
                         )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        UnifiedInspectorPanel(
                            state = state,
                            onEvent = onEvent,
                            modifier = Modifier.fillMaxWidth() 
                        )
                    }
                }
            }
        }
    }
}
