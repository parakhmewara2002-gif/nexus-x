package com.lnxv.player.ui.home

import android.os.Environment
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnxv.player.core.design.*
import com.lnxv.player.core.storage.FileManager
import com.lnxv.player.core.storage.StorageVolumeInfo
import java.io.File

@Composable
fun HomeScreen(
    onNavigateToFiles: (String) -> Unit,
    onNavigateToAnalyzer: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fileManager = remember { FileManager(context) }
    var volumes by remember { mutableStateOf<List<StorageVolumeInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        volumes = fileManager.getStorageVolumes()
    }

    val primaryVol = volumes.firstOrNull() ?: StorageVolumeInfo(
        name = "Internal Storage",
        path = Environment.getExternalStorageDirectory().absolutePath,
        totalBytes = 128_000_000_000L,
        freeBytes = 42_000_000_000L
    )

    val usedPct = if (primaryVol.totalBytes > 0) {
        (primaryVol.usedBytes.toFloat() / primaryVol.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0.5f

    val animProgress by animateFloatAsState(
        targetValue = usedPct,
        animationSpec = tween(durationMillis = 1000),
        label = "storage_ring"
    )

    AmbientGlassBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: Greeting & Profile
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE))
                                .liquidGlassSurface(CircleShape, GlassWhiteHigh, GlassWhiteBorderCrisp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimarySkyBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Good day",
                                fontSize = 12.sp,
                                color = AppColors.textMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "NEXUS X",
                                fontSize = 22.sp,
                                color = AppColors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassIconButton(
                            icon = Icons.Default.Search,
                            contentDescription = "Search",
                            onClick = onNavigateToSearch
                        )
                        GlassIconButton(
                            icon = Icons.Default.Lock,
                            contentDescription = "Vault",
                            onClick = onNavigateToVault
                        )
                    }
                }
            }

            // Storage Overview Card (Liquid Glass with Circular Gauge)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToAnalyzer
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Storage Capacity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )
                            GlassBadge(text = "${(usedPct * 100).toInt()}% Used", color = PrimarySkyBlue)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Circular Storage Ring
                            Box(
                                modifier = Modifier.size(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val isDark = LocalIsDarkTheme.current
                                val trackColor = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
                                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                    drawArc(
                                        color = trackColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = PrimarySkyBlue,
                                        startAngle = -90f,
                                        sweepAngle = 360f * animProgress,
                                        useCenter = false,
                                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(usedPct * 100).toInt()}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = AppColors.textPrimary
                                    )
                                    Text(
                                        text = "Used",
                                        fontSize = 10.sp,
                                        color = AppColors.textMuted
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = primaryVol.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.textPrimary
                                )
                                Text(
                                    text = "Used: ${formatSize(primaryVol.usedBytes)}",
                                    fontSize = 13.sp,
                                    color = AppColors.textMuted
                                )
                                Text(
                                    text = "Free: ${formatSize(primaryVol.freeBytes)} of ${formatSize(primaryVol.totalBytes)}",
                                    fontSize = 13.sp,
                                    color = AppColors.textMuted
                                )
                            }
                        }
                    }
                }
            }

            // Quick Category Shortcuts (Images, Videos, Audio, Docs, APKs, Downloads, Vault, Archives)
            item {
                Text(
                    text = "Categories",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
            }

            item {
                val base = Environment.getExternalStorageDirectory().absolutePath
                val categories = listOf(
                    CategoryItem("Images", Icons.Default.Image, Color(0xFF0284C7), "$base/Pictures"),
                    CategoryItem("Videos", Icons.Default.VideoLibrary, Color(0xFF8B5CF6), "$base/Movies"),
                    CategoryItem("Audio", Icons.Default.Audiotrack, Color(0xFF10B981), "$base/Music"),
                    CategoryItem("Documents", Icons.Default.Description, Color(0xFFF59E0B), "$base/Documents"),
                    CategoryItem("Downloads", Icons.Default.Download, Color(0xFF06B6D4), "$base/Download"),
                    CategoryItem("APKs", Icons.Default.Android, Color(0xFF14B8A6), base),
                    CategoryItem("Archives", Icons.Default.FolderZip, Color(0xFF6366F1), base),
                    CategoryItem("Vault", Icons.Default.Lock, Color(0xFFEC4899), "vault")
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (row in categories.chunked(4)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (cat in row) {
                                GlassCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (cat.targetPath == "vault") onNavigateToVault()
                                        else onNavigateToFiles(cat.targetPath)
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        AnimatedCategoryIcon(
                                            icon = cat.icon,
                                            color = cat.color,
                                            size = 42.dp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = cat.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppColors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Storage Locations (Internal, SD Card, OTG)
            item {
                Text(
                    text = "Storage Locations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    volumes.forEach { vol ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onNavigateToFiles(vol.path) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFE0F2FE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (vol.isRemovable) Icons.Default.SdStorage else Icons.Default.Smartphone,
                                            contentDescription = null,
                                            tint = PrimarySkyBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = vol.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = AppColors.textPrimary
                                        )
                                        Text(
                                            text = "${formatSize(vol.freeBytes)} free of ${formatSize(vol.totalBytes)}",
                                            fontSize = 12.sp,
                                            color = AppColors.textMuted
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = SlateSubtleText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class CategoryItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val targetPath: String
)

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
