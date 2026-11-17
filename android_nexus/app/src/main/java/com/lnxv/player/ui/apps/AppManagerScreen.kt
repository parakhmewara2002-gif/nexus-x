package com.lnxv.player.ui.apps

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.lnxv.player.core.device.AppInfoItem
import com.lnxv.player.core.device.AppManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AppManagerScreen(
    onNavigateBack: () -> Unit,
    onOpenApkEditor: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appManager = remember { AppManager(context) }
    var apps by remember { mutableStateOf<List<AppInfoItem>>(emptyList()) }
    var includeSystem by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    fun loadApps() {
        scope.launch {
            isLoading = true
            apps = appManager.getInstalledApps(includeSystem = includeSystem)
            isLoading = false
        }
    }

    LaunchedEffect(includeSystem) {
        loadApps()
    }

    val filteredApps = apps.filter {
        it.appName.contains(searchQuery, ignoreCase = true) ||
        it.packageName.contains(searchQuery, ignoreCase = true)
    }

    AmbientGlassBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
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
                            text = "App & APK Manager",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textPrimary
                        )
                        Text(
                            text = "${filteredApps.size} applications",
                            fontSize = 12.sp,
                            color = AppColors.textMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "System",
                        fontSize = 12.sp,
                        color = AppColors.textMuted,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Switch(
                        checked = includeSystem,
                        onCheckedChange = { includeSystem = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed packages...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = PrimarySkyBlue) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimarySkyBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onOpenApkEditor?.invoke(app.sourceDir)
                            }
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
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (app.isSystemApp) Color(0xFFF1F5F9) else Color(0xFFE0F2FE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Android,
                                            contentDescription = null,
                                            tint = if (app.isSystemApp) AppColors.textMuted else PrimarySkyBlue
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.appName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AppColors.textPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${app.packageName} • v${app.versionName}",
                                            fontSize = 11.sp,
                                            color = AppColors.textMuted,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GlassIconButton(
                                        icon = Icons.Default.Build,
                                        contentDescription = "Inspect APK",
                                        size = 36.dp,
                                        onClick = {
                                            onOpenApkEditor?.invoke(app.sourceDir)
                                        }
                                    )
                                    GlassIconButton(
                                        icon = Icons.Default.Download,
                                        contentDescription = "Extract APK",
                                        size = 36.dp,
                                        onClick = {
                                            scope.launch {
                                                val backupDir = File(Environment.getExternalStorageDirectory(), "NEXUS_APKs")
                                                val res = appManager.backupApk(app, backupDir)
                                                if (res.isSuccess) {
                                                    Toast.makeText(context, "APK exported to NEXUS_APKs", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Export error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
