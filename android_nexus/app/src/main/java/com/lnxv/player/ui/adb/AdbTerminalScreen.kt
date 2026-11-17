package com.lnxv.player.ui.adb

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnxv.player.core.adb.*
import com.lnxv.player.core.design.*
import kotlinx.coroutines.launch

enum class AdbSubTab(val label: String, val icon: ImageVector) {
    GUI_OPS("GUI Control", Icons.Rounded.Dashboard),
    TERMINAL("Terminal", Icons.Rounded.Terminal),
    WIRELESS("Wireless ADB", Icons.Rounded.Wifi),
    PROCESSES("Processes", Icons.AutoMirrored.Rounded.FormatListBulleted),
    LOGCAT("Live Logcat", Icons.AutoMirrored.Rounded.Article),
    PROPERTIES("Properties", Icons.Rounded.Tune)
}

@Composable
fun AdbTerminalScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engine = remember { AdbTerminalEngine(context) }
    val scope = rememberCoroutineScope()

    var activeSubTab by remember { mutableStateOf(AdbSubTab.GUI_OPS) }
    var adbState by remember { mutableStateOf<AdbDeviceState?>(null) }
    var inputCmd by remember { mutableStateOf("") }
    var history by remember { mutableStateOf<List<CommandResult>>(emptyList()) }
    var isExecuting by remember { mutableStateOf(false) }

    // GUI Commands
    var commandOutputModal by remember { mutableStateOf<CommandResult?>(null) }
    var pendingExecutionCommand by remember { mutableStateOf<GuiAdbCommand?>(null) }

    // Processes
    var processList by remember { mutableStateOf<List<ProcessItem>>(emptyList()) }
    var isFetchingProcesses by remember { mutableStateOf(false) }

    // Logcat
    var logcatOutput by remember { mutableStateOf("") }
    var logcatFilter by remember { mutableStateOf("") }
    var isFetchingLogcat by remember { mutableStateOf(false) }

    // Properties
    var propertySearch by remember { mutableStateOf("") }

    fun refreshAdbState() {
        scope.launch {
            adbState = engine.inspectAdbState()
        }
    }

    LaunchedEffect(Unit) {
        refreshAdbState()
    }

    fun runCommand(cmd: String) {
        if (cmd.isBlank()) return
        scope.launch {
            isExecuting = true
            val res = engine.executeCommand(cmd)
            history = listOf(res) + history
            commandOutputModal = res
            isExecuting = false
            inputCmd = ""
        }
    }

    fun fetchLogs() {
        scope.launch {
            isFetchingLogcat = true
            logcatOutput = engine.readLogcat(lines = 150, filterTag = logcatFilter.ifBlank { null })
            isFetchingLogcat = false
        }
    }

    fun fetchProcesses() {
        scope.launch {
            isFetchingProcesses = true
            processList = engine.listRunningProcesses()
            isFetchingProcesses = false
        }
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
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = onNavigateBack
                    )
                    Column {
                        Text(
                            text = "ADB Control Suite",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textPrimary
                        )
                        Text(
                            text = "GUI Device Operations, Shell & Wireless Studio",
                            fontSize = 11.sp,
                            color = AppColors.textMuted
                        )
                    }
                }

                val isAdbRunning = adbState?.isUsbDebuggingEnabled == true
                GlassBadge(
                    text = if (isAdbRunning) "ADBD ACTIVE" else "ADBD LOCAL",
                    color = if (isAdbRunning) EmeraldSuccess else PrimarySkyBlue
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Premium Scrollable Sub-Tab Switcher Bar with Icons
            val subTabScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(subTabScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdbSubTab.values().forEach { tab ->
                    val isSelected = activeSubTab == tab
                    GlassCard(
                        modifier = Modifier.wrapContentWidth(),
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = if (isSelected) Color(0xFFE0F2FE) else GlassWhiteHigh,
                        borderColor = if (isSelected) PrimarySkyBlue else GlassWhiteBorderCrisp,
                        onClick = {
                            activeSubTab = tab
                            if (tab == AdbSubTab.LOGCAT && logcatOutput.isEmpty()) fetchLogs()
                            if (tab == AdbSubTab.WIRELESS) refreshAdbState()
                            if (tab == AdbSubTab.PROCESSES && processList.isEmpty()) fetchProcesses()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) PrimarySkyBlue else AppColors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimarySkyBlue else AppColors.textPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (activeSubTab) {
                // 1. GUI Operations Control Center (No manual typing required)
                AdbSubTab.GUI_OPS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text("Graphical ADB Command Center", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppColors.textPrimary)
                            Text("Tap any operation to preview command and run safely.", fontSize = 11.sp, color = AppColors.textMuted)
                        }

                        items(engine.builtInGuiCommands) { cmd ->
                            val cmdIcon: ImageVector = when (cmd.iconType) {
                                "wifi" -> Icons.Rounded.Wifi
                                "battery" -> Icons.Rounded.BatteryChargingFull
                                "cpu" -> Icons.Rounded.Memory
                                "ram" -> Icons.Rounded.Storage
                                "display" -> Icons.Rounded.Screenshot
                                "apps" -> Icons.Rounded.Apps
                                "features" -> Icons.Rounded.Tune
                                "diagnostics" -> Icons.Rounded.BugReport
                                "clean" -> Icons.Rounded.CleaningServices
                                else -> Icons.Rounded.Terminal
                            }

                            val iconBgColor = when (cmd.category) {
                                "Network" -> Color(0xFFE0F2FE)
                                "Hardware" -> Color(0xFFFEF3C7)
                                "Storage" -> Color(0xFFEDE9FE)
                                "Apps" -> Color(0xFFDCFCE7)
                                "Diagnostics" -> Color(0xFFFFEDD5)
                                "Maintenance" -> Color(0xFFF1F5F9)
                                else -> Color(0xFFE2E8F0)
                            }
                            val iconTintColor = when (cmd.category) {
                                "Network" -> PrimarySkyBlue
                                "Hardware" -> Color(0xFFD97706)
                                "Storage" -> RadiantLavender
                                "Apps" -> EmeraldSuccess
                                "Diagnostics" -> Color(0xFFEA580C)
                                "Maintenance" -> AppColors.textPrimary
                                else -> PrimarySkyBlue
                            }

                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                onClick = { pendingExecutionCommand = cmd }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Custom rounded icon box
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(iconBgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = cmdIcon,
                                            contentDescription = cmd.title,
                                            tint = iconTintColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = cmd.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = AppColors.textPrimary,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            GlassBadge(
                                                text = cmd.compatibilityStatus,
                                                color = EmeraldSuccess
                                            )
                                        }

                                        Text(
                                            text = cmd.description,
                                            fontSize = 11.sp,
                                            color = AppColors.textMuted,
                                            lineHeight = 15.sp
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFF1F5F9))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "adb shell ${cmd.rawCommand}",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = RadiantLavender,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(PrimarySkyBlue.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "Run",
                                            tint = PrimarySkyBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Terminal
                AdbSubTab.TERMINAL -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputCmd,
                                onValueChange = { inputCmd = it },
                                placeholder = { Text("Enter command (e.g. getprop, pm list, uptime)...", color = AppColors.textMuted, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimarySkyBlue,
                                    focusedTextColor = AppColors.textPrimary,
                                    unfocusedTextColor = AppColors.textPrimary
                                ),
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
                            GlassIconButton(
                                icon = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Run",
                                onClick = { runCommand(inputCmd) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isExecuting) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimarySkyBlue)
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (history.isEmpty()) {
                                item {
                                    Text("Terminal session ready. Shell access is active.", color = AppColors.textMuted, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                                }
                            } else {
                                items(history) { res ->
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        backgroundColor = Color(0xFF0F172A),
                                        borderColor = if (res.exitCode == 0) Color(0x3310B981) else Color(0x33EF4444)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("$ ${res.command}", fontWeight = FontWeight.Bold, color = PrimarySkyBlue, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                                Text("${res.executionTimeMs}ms • Code ${res.exitCode}", color = if (res.exitCode == 0) EmeraldSuccess else CrimsonDanger, fontSize = 10.sp)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = res.output,
                                                color = Color(0xFFF8FAFC),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Wireless ADB Manager
                AdbSubTab.WIRELESS -> {
                    val w = adbState?.wirelessDebugInfo
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(PrimarySkyBlue.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = PrimarySkyBlue, modifier = Modifier.size(20.dp))
                                        }
                                        Text("Wireless Debugging Pairing", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppColors.textPrimary)
                                    }

                                    InfoRow("Device IP Address", w?.ipAddress ?: "127.0.0.1")
                                    InfoRow("ADB TCP Port", "${w?.port ?: 5555}")
                                    InfoRow("Pairing Status", if (w?.isEnabled == true) "PAIRED / ACTIVE" else "NOT CONNECTED")
                                    InfoRow("SELinux Mode", adbState?.selinuxMode ?: "Enforcing")

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Connect PC via terminal:\nadb connect ${w?.ipAddress}:${w?.port ?: 5555}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = RadiantLavender
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Process Manager
                AdbSubTab.PROCESSES -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Running Processes (${processList.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppColors.textPrimary)
                            IconButton(onClick = { fetchProcesses() }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = PrimarySkyBlue)
                            }
                        }

                        if (isFetchingProcesses) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PrimarySkyBlue)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 90.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(processList) { proc ->
                                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(proc.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AppColors.textPrimary, maxLines = 1)
                                                Text("PID ${proc.pid} • User: ${proc.user}", fontSize = 10.sp, color = AppColors.textMuted)
                                            }
                                            Text(proc.memRss, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = RadiantLavender)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Live Logcat Studio
                AdbSubTab.LOGCAT -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = logcatFilter,
                                onValueChange = { logcatFilter = it },
                                placeholder = { Text("Filter logcat tag/keyword...", color = AppColors.textMuted, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(onClick = { fetchLogs() }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = PrimarySkyBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        GlassCard(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
                            backgroundColor = Color(0xFF0F172A),
                            borderColor = Color(0x3338BDF8)
                        ) {
                            LazyColumn(modifier = Modifier.padding(12.dp)) {
                                item {
                                    Text(
                                        text = if (logcatOutput.isEmpty()) "Fetching logcat..." else logcatOutput,
                                        color = Color(0xFFF8FAFC),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Properties Explorer
                AdbSubTab.PROPERTIES -> {
                    val props = adbState?.activeProperties ?: emptyMap()
                    val filteredProps = if (propertySearch.isBlank()) props.entries.toList() else props.entries.filter {
                        it.key.contains(propertySearch, ignoreCase = true) || it.value.contains(propertySearch, ignoreCase = true)
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = propertySearch,
                            onValueChange = { propertySearch = it },
                            placeholder = { Text("Search getprop (e.g. ro.build, ro.product)...", color = AppColors.textMuted, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredProps) { entry ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Prop", "${entry.key}=${entry.value}"))
                                        Toast.makeText(context, "Copied property", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(entry.key, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimarySkyBlue)
                                        Text(entry.value, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = AppColors.textPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Preview & Confirmation Dialog before running GUI ADB Command
        if (pendingExecutionCommand != null) {
            val cmd = pendingExecutionCommand!!
            AlertDialog(
                onDismissRequest = { pendingExecutionCommand = null },
                title = { Text(cmd.title, fontWeight = FontWeight.Bold, color = AppColors.textPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(cmd.description, fontSize = 12.sp, color = AppColors.textMuted)
                        Text("Command Preview:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AppColors.textPrimary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .padding(10.dp)
                        ) {
                            Text("adb shell ${cmd.rawCommand}", color = Color(0xFFF8FAFC), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = PrimarySkyBlue),
                        onClick = {
                            val toRun = cmd.rawCommand
                            pendingExecutionCommand = null
                            runCommand(toRun)
                        }
                    ) {
                        Text("Execute Command", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingExecutionCommand = null }) {
                        Text("Cancel", color = AppColors.textPrimary)
                    }
                }
            )
        }

        // Output Result Modal
        if (commandOutputModal != null) {
            val res = commandOutputModal!!
            AlertDialog(
                onDismissRequest = { commandOutputModal = null },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Command Output", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppColors.textPrimary)
                        GlassBadge(
                            text = if (res.exitCode == 0) "SUCCESS" else "CODE ${res.exitCode}",
                            color = if (res.exitCode == 0) EmeraldSuccess else CrimsonDanger
                        )
                    }
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .padding(12.dp)
                    ) {
                        LazyColumn {
                            item {
                                Text(
                                    text = res.output,
                                    color = Color(0xFFF8FAFC),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { commandOutputModal = null }) {
                        Text("Done")
                    }
                }
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = AppColors.textMuted)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.textPrimary)
    }
}
