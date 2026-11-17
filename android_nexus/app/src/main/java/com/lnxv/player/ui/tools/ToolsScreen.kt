package com.lnxv.player.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnxv.player.core.design.*

@Composable
fun ToolsScreen(
    onNavigateToAdb: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToAnalyzer: () -> Unit,
    onNavigateToToolkit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AmbientGlassBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Professional Tools",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
                Text(
                    text = "ADB center, system shell, vault, utilities & dev toolkit",
                    fontSize = 13.sp,
                    color = AppColors.textMuted
                )
            }

            item {
                ToolOptionCard(
                    title = "ADB & Shell Terminal",
                    subtitle = "On-device wireless debugging, command execution, logcat",
                    icon = Icons.Default.Terminal,
                    accentColor = RadiantLavender,
                    onClick = onNavigateToAdb
                )
            }

            item {
                ToolOptionCard(
                    title = "Developer & Technician Toolkit",
                    subtitle = "Hex viewer, JSON/XML formatter, Base64, Checksums, UUIDs",
                    icon = Icons.Default.Build,
                    accentColor = LuminousCyan,
                    onClick = onNavigateToToolkit
                )
            }

            item {
                ToolOptionCard(
                    title = "Encrypted Vault",
                    subtitle = "Hardware-backed AES-GCM file protection and biometrics",
                    icon = Icons.Default.Lock,
                    accentColor = CrimsonDanger,
                    onClick = onNavigateToVault
                )
            }

            item {
                ToolOptionCard(
                    title = "Application Manager",
                    subtitle = "Extract APKs, inspect manifests, app permissions & storage",
                    icon = Icons.Default.Apps,
                    accentColor = PrimarySkyBlue,
                    onClick = onNavigateToApps
                )
            }

            item {
                ToolOptionCard(
                    title = "Storage Deep Cleaner",
                    subtitle = "Detect duplicate hashes, large media, and empty directories",
                    icon = Icons.Default.CleaningServices,
                    accentColor = AmberWarning,
                    onClick = onNavigateToAnalyzer
                )
            }
        }
    }
}

@Composable
private fun ToolOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AppColors.textMuted
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SlateSubtleText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
