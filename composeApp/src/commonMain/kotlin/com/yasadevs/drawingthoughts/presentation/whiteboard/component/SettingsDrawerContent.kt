package com.yasadevs.drawingthoughts.presentation.whiteboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDrawerContent(
    modifier: Modifier = Modifier,
    onRateUsClick: () -> Unit,
    onShareClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    versionText: String = "1.0.0"
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp),
        color = Color(0xFF161618) // Dark sleek background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Others",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
            )

            DrawerMenuItem(
                icon = Icons.Default.StarRate,
                title = "Rate Us",
                subtitle = "Share your Experience",
                iconTint = Color(0xFFFFB800), // Gold
                onClick = onRateUsClick
            )

            DrawerMenuItem(
                icon = Icons.Default.Share,
                title = "Share",
                subtitle = "Share App with your Friends",
                iconTint = Color(0xFF4285F4), // Blue
                onClick = onShareClick
            )

            DrawerMenuItem(
                icon = Icons.Default.Email,
                title = "Feedback",
                subtitle = "Add your Suggestions",
                iconTint = Color(0xFF34A853), // Green
                onClick = onFeedbackClick
            )

            DrawerMenuItem(
                icon = Icons.Default.Lock,
                title = "Privacy Policy",
                subtitle = "Read How We Protect You",
                iconTint = Color(0xFFEA4335), // Red
                onClick = onPrivacyPolicyClick
            )

            DrawerMenuItem(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = versionText,
                iconTint = Color(0xFF9AA0A6), // Gray
                showChevron = false,
                onClick = { /* Do nothing or show a toast */ }
            )
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Icon Area
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text area
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        // Trailing Chevron
        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
