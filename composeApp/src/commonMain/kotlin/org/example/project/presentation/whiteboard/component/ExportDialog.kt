package org.example.project.presentation.whiteboard.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

/**
 * Export Dialog - Smart Export Workflow
 *
 * Provides two export modes:
 * 1. Whole Canvas: Exports all content with automatic bounding box calculation
 * 2. Visible Screen: Exports exactly what's visible in the current viewport
 */
@Composable
fun ExportDialog(
    onWholeCanvasExport: () -> Unit,
    onVisibleScreenExport: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export Drawing",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Choose how you want to export your drawing:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "• Whole Canvas: Exports all strokes, fitting content automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Visible Screen: Exports exactly what you see on screen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {

                TextButton(
                    onClick = {
                        onVisibleScreenExport()
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "Visible Screen",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Visible Screen")
                }

                TextButton(
                    onClick = {
                        onWholeCanvasExport()
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Whole Canvas",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Whole Canvas")
                }

                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }

        },
        dismissButton = null,
        modifier = modifier
    )
}
