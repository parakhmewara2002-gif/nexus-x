package com.lnxv.player.ui.preview

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.lnxv.player.core.design.*
import com.lnxv.player.core.storage.FileCategory
import com.lnxv.player.core.storage.FileItem
import com.lnxv.player.core.storage.FileManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FilePreviewScreen(
    filePath: String,
    onNavigateBack: () -> Unit,
    onOpenInPlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fileManager = remember { FileManager(context) }
    val file = remember(filePath) { File(filePath) }
    val item = remember(file) { FileItem(file) }

    var sha256 by remember { mutableStateOf("Calculating...") }
    var textPreviewContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        scope.launch {
            sha256 = fileManager.calculateSha256(file)
            if (item.category == FileCategory.DOCUMENTS || item.extension in listOf("txt", "md", "json", "xml", "log", "kt", "java", "gradle")) {
                textPreviewContent = try {
                    file.readLines().take(100).joinToString("\n")
                } catch (_: Exception) {
                    "(Unable to read text file directly)"
                }
            }
        }
    }

    AmbientGlassBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(44.dp))

            // Header
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
                            text = item.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = item.extension.uppercase(),
                            fontSize = 12.sp,
                            color = AppColors.textMuted
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share",
                        onClick = {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "*/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share File"))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Preview Stage
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (item.category) {
                        FileCategory.IMAGES -> {
                            AsyncImage(
                                model = file,
                                contentDescription = item.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        FileCategory.VIDEOS -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GlassIconButton(
                                    icon = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = PrimarySkyBlue,
                                    size = 64.dp,
                                    onClick = { onOpenInPlayer(file.toURI().toString()) }
                                )
                                Text(
                                    text = "Play in Video Player",
                                    color = AppColors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        else -> {
                            if (textPreviewContent != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = textPreviewContent!!,
                                        fontSize = 11.sp,
                                        color = AppColors.textPrimary,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = PrimarySkyBlue,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Text(
                                        text = item.extension.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = AppColors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // File Details & Hash Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Metadata & Integrity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AppColors.textPrimary
                    )

                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(item.lastModified))

                    DetailRow(label = "Size", value = item.formattedSize())
                    DetailRow(label = "Path", value = item.path)
                    DetailRow(label = "Modified", value = dateStr)
                    DetailRow(label = "SHA-256", value = sha256)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Action Button
            if (item.category == FileCategory.VIDEOS) {
                GlassButton(
                    text = "Launch Liquid Player",
                    onClick = { onOpenInPlayer(file.toURI().toString()) },
                    icon = Icons.Default.PlayArrow,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = AppColors.textMuted, fontWeight = FontWeight.Medium)
        Text(
            text = value,
            fontSize = 12.sp,
            color = AppColors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
