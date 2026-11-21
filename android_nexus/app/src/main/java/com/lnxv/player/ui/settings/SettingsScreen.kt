package com.lnxv.player.ui.settings

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Sync
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
import com.lnxv.player.LNXVApplication
import com.lnxv.player.core.design.*
import com.lnxv.player.core.updater.AppUpdateInfo
import com.lnxv.player.core.updater.SecureUpdateManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as LNXVApplication
    val scope = rememberCoroutineScope()
    val updateManager = remember { SecureUpdateManager(context) }

    val userPrefs by app.dataStore.userPreferencesFlow.collectAsStateWithLifecycle(initialValue = null)
    val currentTheme = userPrefs?.themeMode ?: "OBSIDIAN"
    val currentLiquid = userPrefs?.liquidGlassQuality ?: "AUTO"

    var showHiddenFiles by remember { mutableStateOf(false) }
    var hardwareAcceleration by remember { mutableStateOf(true) }

    // Update Check state
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Real installed version (never hardcode it, or the update check compares against a wrong number)
    val currentVersionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }

    fun checkAppUpdates() {
        scope.launch {
            isCheckingUpdate = true
            val res = updateManager.checkUpdate(currentVersionName)
            updateInfo = res.getOrNull()
            isCheckingUpdate = false
            showUpdateDialog = true
        }
    }

    AmbientGlassBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            item(key = "section_1") {
                Text(
                    text = "Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
                Text(
                    text = "Preferences, visual style, security and GitHub updates",
                    fontSize = 13.sp,
                    color = AppColors.textMuted
                )
            }

            // In-App Secure Update Section (GitHub Releases)
            item(key = "section_2") {
                Text(
                    text = "Updates & Maintenance",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
            }

            item(key = "section_3") {
                val isObsidian = LocalIsObsidianTheme.current
                val isAmoled = LocalIsAmoledTheme.current

                val iconBoxBg = when {
                    isAmoled -> Color(0xFF18181B)
                    isObsidian -> Color(0xFF2E2E31)
                    else -> Color(0xFFE0F2FE)
                }
                val iconTint = when {
                    isAmoled || isObsidian -> Color(0xFF38BDF8)
                    else -> PrimarySkyBlue
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    onClick = { checkAppUpdates() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(iconBoxBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = "GitHub Updates",
                                tint = iconTint,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Check for GitHub Updates",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AppColors.textPrimary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                GlassBadge(text = "v$currentVersionName", color = PrimarySkyBlue)
                            }
                            Text(
                                text = "Secure in-app OTA binary checking directly via GitHub Releases",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                maxLines = 2,
                                color = AppColors.textMuted
                            )
                        }

                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = PrimarySkyBlue
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = AppColors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Theme section
            item(key = "section_4") {
                Text(
                    text = "Appearance & Theme",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item(key = "section_5") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Color Theme Style",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // 1. Obsidian Studio Dark (From uploaded theme)
                        ThemeOptionRow(
                            title = "Obsidian Studio Dark",
                            subtitle = "Deep matte slate (#1A1A1B) with crisp gray accents",
                            isSelected = currentTheme == "OBSIDIAN",
                            onClick = {
                                scope.launch { app.dataStore.updateThemeMode("OBSIDIAN") }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // 2. iOS 26 Light Liquid Glass
                        ThemeOptionRow(
                            title = "iOS 26 Light Liquid Glass",
                            subtitle = "Translucent specular glass with pastel mesh gradients",
                            isSelected = currentTheme == "LIGHT_GLASS",
                            onClick = {
                                scope.launch { app.dataStore.updateThemeMode("LIGHT_GLASS") }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // 3. AMOLED Pitch Black
                        ThemeOptionRow(
                            title = "AMOLED Pitch Black",
                            subtitle = "Pure #000000 true black for OLED battery saving",
                            isSelected = currentTheme == "AMOLED",
                            onClick = {
                                scope.launch { app.dataStore.updateThemeMode("AMOLED") }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Text(
                            text = "Liquid Glass Effect",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        ThemeOptionRow(
                            title = "Auto (recommended)",
                            subtitle = "Starts light and upgrades or downgrades by watching real frame timing, heat and memory",
                            isSelected = currentLiquid == "AUTO",
                            onClick = {
                                scope.launch { app.dataStore.updateLiquidGlassQuality("AUTO") }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ThemeOptionRow(
                            title = "Full",
                            subtitle = "Refraction, colour fringing and frost on every surface. Prettiest, heaviest",
                            isSelected = currentLiquid == "FULL",
                            onClick = {
                                scope.launch { app.dataStore.updateLiquidGlassQuality("FULL") }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ThemeOptionRow(
                            title = "Lite",
                            subtitle = "Simplified refraction on dock and buttons only, smooth scrolling",
                            isSelected = currentLiquid == "LITE",
                            onClick = {
                                scope.launch { app.dataStore.updateLiquidGlassQuality("LITE") }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ThemeOptionRow(
                            title = "Adaptive",
                            subtitle = "Milky gradient glass with fine borders. No live shader, very light",
                            isSelected = currentLiquid == "ADAPTIVE",
                            onClick = {
                                scope.launch { app.dataStore.updateLiquidGlassQuality("ADAPTIVE") }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ThemeOptionRow(
                            title = "Off",
                            subtitle = "Classic translucent panels, best battery and performance",
                            isSelected = currentLiquid == "OFF",
                            onClick = {
                                scope.launch { app.dataStore.updateLiquidGlassQuality("OFF") }
                            }
                        )
                    }
                }
            }

            // File Manager Behavior
            item(key = "section_6") {
                Text(
                    text = "File Manager Preferences",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
            }

            item(key = "section_7") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Show Hidden Files",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = AppColors.textPrimary
                                )
                                Text(
                                    text = "Display dotfiles and system folders",
                                    fontSize = 12.sp,
                                    color = AppColors.textMuted
                                )
                            }
                            Switch(
                                checked = showHiddenFiles,
                                onCheckedChange = { showHiddenFiles = it }
                            )
                        }

                        HorizontalDivider(color = Color(0x1F000000))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Hardware Decoder",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = AppColors.textPrimary
                                )
                                Text(
                                    text = "Use MediaCodec GPU pipeline for video",
                                    fontSize = 12.sp,
                                    color = AppColors.textMuted
                                )
                            }
                            Switch(
                                checked = hardwareAcceleration,
                                onCheckedChange = { hardwareAcceleration = it }
                            )
                        }
                    }
                }
            }

            // About application
            item(key = "section_8") {
                Text(
                    text = "About NEXUS X",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
            }

            item(key = "section_9") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "NEXUS X — Ultimate Android File Manager & ADB Suite",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppColors.textPrimary
                        )
                        Text(
                            text = "Version $currentVersionName\nArchitecture: Clean MVVM + Jetpack Compose\nDesign Authority: Stitch Light Liquid Glass iOS 26\nOTA Channel: GitHub Verified Releases",
                            fontSize = 12.sp,
                            color = AppColors.textMuted,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Update Dialog
        if (showUpdateDialog && updateInfo != null) {
            val u = updateInfo!!
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (u.hasUpdate) "New Update Available!" else "App is Up to Date",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppColors.textPrimary
                        )
                        GlassBadge(
                            text = if (u.hasUpdate) "NEW: v${u.latestVersion}" else "LATEST",
                            color = if (u.hasUpdate) EmeraldSuccess else PrimarySkyBlue
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Current Version: v${u.currentVersion}", fontSize = 12.sp, color = AppColors.textMuted)
                        Text("Latest GitHub Version: v${u.latestVersion}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Release Changelog:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AppColors.textPrimary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(10.dp)
                        ) {
                            Text(u.releaseNotes, fontSize = 11.sp, color = AppColors.textPrimary, lineHeight = 16.sp)
                        }
                    }
                },
                confirmButton = {
                    if (u.hasUpdate && u.downloadUrl.isNotEmpty()) {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = PrimarySkyBlue),
                            onClick = {
                                updateManager.downloadAndInstall(u.downloadUrl)
                                showUpdateDialog = false
                            }
                        ) {
                            Text("Download & Install", color = Color.White)
                        }
                    } else {
                        Button(onClick = { showUpdateDialog = false }) {
                            Text("Close")
                        }
                    }
                },
                dismissButton = {
                    if (u.hasUpdate) {
                        TextButton(onClick = { showUpdateDialog = false }) {
                            Text("Later", color = AppColors.textPrimary)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tick = com.lnxv.player.core.design.rememberTick()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = { tick(); onClick() })
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            GlassBadge(text = "ACTIVE", color = PrimarySkyBlue)
        } else {
            RadioButton(
                selected = false,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    unselectedColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}
