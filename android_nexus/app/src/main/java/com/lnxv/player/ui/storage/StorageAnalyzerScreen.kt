package com.lnxv.player.ui.storage

import android.os.Environment
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnxv.player.core.design.*
import com.lnxv.player.core.storage.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun StorageAnalyzerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val analyzer = remember { StorageAnalyzer(context) }
    val fileManager = remember { FileManager(context) }

    var distribution by remember { mutableStateOf<StorageDistribution?>(null) }
    var largeFiles by remember { mutableStateOf<List<LargeFileInfo>>(emptyList()) }
    var duplicates by remember { mutableStateOf<List<DuplicateFileGroup>>(emptyList()) }
    var emptyFolders by remember { mutableStateOf<List<File>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            isScanning = true
            distribution = analyzer.analyzeStorage()
            largeFiles = analyzer.findLargeFiles()
            emptyFolders = analyzer.findEmptyFolders()
            duplicates = analyzer.findDuplicates()
            isScanning = false
        }
    }

    val dist = distribution ?: StorageDistribution(
        totalBytes = 128_000_000_000L,
        freeBytes = 42_000_000_000L,
        usedBytes = 86_000_000_000L,
        videoBytes = 45_000_000_000L,
        imageBytes = 22_000_000_000L,
        audioBytes = 8_000_000_000L,
        documentBytes = 4_000_000_000L,
        apkBytes = 3_000_000_000L,
        otherBytes = 4_000_000_000L
    )

    val usedPct = if (dist.totalBytes > 0) (dist.usedBytes.toFloat() / dist.totalBytes.toFloat()) else 0.65f

    AmbientGlassBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassIconButton(
                            icon = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            onClick = onNavigateBack
                        )
                        Column {
                            Text(
                                text = "Storage Analyzer",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.textPrimary
                            )
                            Text(
                                text = if (isScanning) "Scanning internal storage..." else "Scan complete",
                                fontSize = 12.sp,
                                color = AppColors.textMuted
                            )
                        }
                    }

                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimarySkyBlue,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Overview Card with segmented bar
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                text = "Internal Storage",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.textPrimary
                            )
                            Text(
                                text = "${formatSize(dist.usedBytes)} / ${formatSize(dist.totalBytes)}",
                                fontSize = 13.sp,
                                color = AppColors.textMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Segmented bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                if (dist.totalBytes > 0) {
                                    val vidWeight = (dist.videoBytes.toFloat() / dist.totalBytes).coerceAtLeast(0.01f)
                                    val imgWeight = (dist.imageBytes.toFloat() / dist.totalBytes).coerceAtLeast(0.01f)
                                    val audWeight = (dist.audioBytes.toFloat() / dist.totalBytes).coerceAtLeast(0.01f)
                                    val docWeight = (dist.documentBytes.toFloat() / dist.totalBytes).coerceAtLeast(0.01f)
                                    val apkWeight = (dist.apkBytes.toFloat() / dist.totalBytes).coerceAtLeast(0.01f)

                                    Spacer(modifier = Modifier.weight(vidWeight).fillMaxHeight().background(RadiantLavender))
                                    Spacer(modifier = Modifier.weight(imgWeight).fillMaxHeight().background(PrimarySkyBlue))
                                    Spacer(modifier = Modifier.weight(audWeight).fillMaxHeight().background(EmeraldSuccess))
                                    Spacer(modifier = Modifier.weight(docWeight).fillMaxHeight().background(AmberWarning))
                                    Spacer(modifier = Modifier.weight(apkWeight).fillMaxHeight().background(Color(0xFF14B8A6)))
                                }
                            }
                        }

                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LegendItem(color = RadiantLavender, title = "Videos", size = formatSize(dist.videoBytes))
                            LegendItem(color = PrimarySkyBlue, title = "Images", size = formatSize(dist.imageBytes))
                            LegendItem(color = EmeraldSuccess, title = "Audio", size = formatSize(dist.audioBytes))
                            LegendItem(color = AmberWarning, title = "Docs", size = formatSize(dist.documentBytes))
                            LegendItem(color = Color(0xFF14B8A6), title = "APKs", size = formatSize(dist.apkBytes))
                        }
                    }
                }
            }

            // Cleanup Recommendations
            item {
                Text(
                    text = "Cleanup Suggestions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        onClick = {}
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, tint = RadiantLavender)
                            Text("Duplicates", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppColors.textPrimary)
                            Text("${duplicates.size} groups found", fontSize = 11.sp, color = AppColors.textMuted)
                        }
                    }

                    GlassCard(
                        modifier = Modifier.weight(1f),
                        onClick = {}
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.FolderOff, null, tint = AmberWarning)
                            Text("Empty Folders", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppColors.textPrimary)
                            Text("${emptyFolders.size} folders", fontSize = 11.sp, color = AppColors.textMuted)
                        }
                    }
                }
            }

            // Large Files section
            item {
                Text(
                    text = "Largest Files (${largeFiles.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
            }

            items(largeFiles.take(15)) { lf ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.InsertDriveFile, null, tint = PrimarySkyBlue)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lf.file.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.textPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = lf.file.parent ?: "",
                                    fontSize = 11.sp,
                                    color = AppColors.textMuted,
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = formatSize(lf.size),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimarySkyBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, title: String, size: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, fontSize = 10.sp, color = AppColors.textMuted)
        Text(text = size, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppColors.textPrimary)
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
