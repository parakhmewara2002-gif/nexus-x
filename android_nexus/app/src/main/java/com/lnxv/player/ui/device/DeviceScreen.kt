package com.lnxv.player.ui.device

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnxv.player.core.design.*
import com.lnxv.player.core.device.DeviceDiagnostics
import com.lnxv.player.core.device.DeviceSpec

@Composable
fun DeviceScreen(
    onNavigateToApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val diagnostics = remember { DeviceDiagnostics(context) }
    var spec by remember { mutableStateOf<DeviceSpec?>(null) }
    var shizukuAvailable by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        spec = diagnostics.getDeviceSpec()
        shizukuAvailable = diagnostics.checkShizukuAvailable()
    }

    val s = spec ?: DeviceSpec(
        totalRamBytes = 8_000_000_000L,
        availRamBytes = 3_500_000_000L,
        totalStorageBytes = 128_000_000_000L,
        availStorageBytes = 42_000_000_000L,
        batteryPct = 85,
        isCharging = true,
        networkType = "Wi-Fi",
        isRooted = false,
        sensorCount = 32
    )

    AmbientGlassBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Device Center",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textPrimary
                        )
                        Text(
                            text = "${s.manufacturer} ${s.model}",
                            fontSize = 13.sp,
                            color = AppColors.textMuted
                        )
                    }
                    GlassBadge(
                        text = if (s.isRooted) "ROOTED" else "UNROOTED",
                        color = if (s.isRooted) CrimsonDanger else PrimarySkyBlue
                    )
                }
            }

            // Privileged Status Card (Root & Shizuku)
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Privilege & Security Architecture",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppColors.textPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatusItem(
                                icon = Icons.Default.Security,
                                title = "Root Access",
                                subtitle = if (s.isRooted) "Available (su)" else "Not Detected",
                                isPositive = s.isRooted
                            )
                            StatusItem(
                                icon = Icons.Default.Bolt,
                                title = "Shizuku API",
                                subtitle = if (shizukuAvailable) "Installed" else "Not Installed",
                                isPositive = shizukuAvailable
                            )
                        }
                    }
                }
            }

            // Specs Grid (OS, CPU, RAM, Battery)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpecCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Android,
                            title = "Android OS",
                            value = "Android ${s.androidVersion}",
                            subtext = "SDK ${s.sdkInt}"
                        )
                        SpecCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Memory,
                            title = "Processor",
                            value = s.cpuAbi.take(12),
                            subtext = "64-bit Architecture"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpecCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Straighten,
                            title = "RAM Memory",
                            value = "${formatSize(s.availRamBytes)} free",
                            subtext = "of ${formatSize(s.totalRamBytes)}"
                        )
                        SpecCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.BatteryChargingFull,
                            title = "Battery",
                            value = "${s.batteryPct}%",
                            subtext = if (s.isCharging) "Charging" else "Discharging"
                        )
                    }
                }
            }

            // Quick App Manager Shortcut
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToApps
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
                                Icon(Icons.Default.Apps, null, tint = PrimarySkyBlue)
                            }
                            Column {
                                Text(
                                    text = "Application Manager",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AppColors.textPrimary
                                )
                                Text(
                                    text = "Inspect, backup, and manage installed apps",
                                    fontSize = 12.sp,
                                    color = AppColors.textMuted
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = SlateSubtleText)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusItem(icon: ImageVector, title: String, subtitle: String, isPositive: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isPositive) Color(0xFFD1FAE5) else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPositive) EmeraldSuccess else AppColors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.textPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = AppColors.textMuted)
        }
    }
}

@Composable
private fun SpecCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subtext: String
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimarySkyBlue, modifier = Modifier.size(24.dp))
            Text(text = title, fontSize = 11.sp, color = AppColors.textMuted, fontWeight = FontWeight.Medium)
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.textPrimary)
            Text(text = subtext, fontSize = 11.sp, color = SlateSubtleText)
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
