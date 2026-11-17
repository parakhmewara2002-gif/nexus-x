package com.lnxv.player.ui.apk

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.lnxv.player.core.apk.ApkDetails
import com.lnxv.player.core.apk.ApkEditorEngine
import com.lnxv.player.core.apk.ApkEntryItem
import com.lnxv.player.core.design.*
import kotlinx.coroutines.launch
import java.io.File

enum class ApkTab(val label: String) {
    OVERVIEW("Overview"),
    STRUCTURE("ZIP & DEX"),
    MANIFEST("Manifest"),
    DEX_STRINGS("DEX Pool"),
    SIGNATURES("Certificates"),
    MT_TOOLS("MT Suite")
}

@Composable
fun ApkEditorScreen(
    apkPath: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apkFile = remember(apkPath) { File(apkPath) }
    val engine = remember { ApkEditorEngine(context) }

    var selectedTab by remember { mutableStateOf(ApkTab.OVERVIEW) }
    var apkDetails by remember { mutableStateOf<ApkDetails?>(null) }
    var apkEntries by remember { mutableStateOf<List<ApkEntryItem>>(emptyList()) }
    var selectedEntryContent by remember { mutableStateOf<String?>(null) }
    var selectedEntryName by remember { mutableStateOf<String?>(null) }
    var isEditingEntry by remember { mutableStateOf(false) }
    var editableContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var searchFilter by remember { mutableStateOf("") }

    // DEX String pool query state
    var dexStringQuery by remember { mutableStateOf("") }
    var dexStringResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearchingDex by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            isLoading = true
            val res = engine.inspectApk(apkFile)
            apkDetails = res.getOrNull()
            apkEntries = engine.listApkEntries(apkFile)
            isLoading = false
        }
    }

    LaunchedEffect(apkFile) {
        refresh()
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
                            text = apkDetails?.appName ?: apkFile.nameWithoutExtension,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepSlateText,
                            maxLines = 1
                        )
                        Text(
                            text = apkDetails?.packageName ?: apkFile.name,
                            fontSize = 11.sp,
                            color = SlateMutedText,
                            maxLines = 1
                        )
                    }
                }

                GlassBadge(text = "MT PRO", color = PrimarySkyBlue)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab bar (Overview, ZIP & DEX, Manifest, DEX Pool, Certificates, MT Suite)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ApkTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        backgroundColor = if (isSelected) Color(0xFFE0F2FE) else GlassWhiteHigh,
                        borderColor = if (isSelected) PrimarySkyBlue else GlassWhiteBorderCrisp,
                        onClick = { selectedTab = tab }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.label,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimarySkyBlue else DeepSlateText,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimarySkyBlue)
                }
            } else {
                when (selectedTab) {
                    ApkTab.OVERVIEW -> {
                        val d = apkDetails
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("Package Architecture", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepSlateText)
                                        InfoRow("Version Name", d?.versionName ?: "1.0")
                                        InfoRow("Version Code", "${d?.versionCode ?: 1}")
                                        InfoRow("Minimum SDK", "Android ${d?.minSdk ?: 21} (API ${d?.minSdk ?: 21})")
                                        InfoRow("Target SDK", "Android ${d?.targetSdk ?: 33} (API ${d?.targetSdk ?: 33})")
                                        InfoRow("DEX Files", "${d?.dexCount ?: 1} classes.dex")
                                        InfoRow("Native ABIs", d?.nativeLibraries?.joinToString(", ")?.ifEmpty { "None (Pure Java/Kotlin)" } ?: "None")
                                        InfoRow("Total Packed Assets", "${d?.totalEntries ?: 0} entries")
                                        InfoRow("File Size", formatSize(apkFile.length()))
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GlassButton(
                                        text = "Install APK",
                                        icon = Icons.Default.InstallMobile,
                                        isPrimary = true,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
                                            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/vnd.android.package-archive")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(installIntent)
                                        }
                                    )

                                    GlassButton(
                                        text = "Clone Backup",
                                        icon = Icons.Default.ContentCopy,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            scope.launch {
                                                val dest = File(Environment.getExternalStorageDirectory(), "NEXUS_APKs/${apkFile.nameWithoutExtension}_clone.apk")
                                                val res = engine.cloneApk(apkFile, dest)
                                                if (res.isSuccess) {
                                                    Toast.makeText(context, "Cloned to NEXUS_APKs", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    ApkTab.STRUCTURE -> {
                        val filteredEntries = if (searchFilter.isBlank()) apkEntries else apkEntries.filter {
                            it.name.contains(searchFilter, ignoreCase = true)
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                TextField(
                                    value = searchFilter,
                                    onValueChange = { searchFilter = it },
                                    placeholder = { Text("Filter ZIP tree (e.g. .dex, .xml, assets/)...", color = SlateMutedText) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = DeepSlateText,
                                        unfocusedTextColor = DeepSlateText
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 90.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredEntries) { entry ->
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        onClick = {
                                            scope.launch {
                                                selectedEntryName = entry.name
                                                val content = engine.readEntryContent(apkFile, entry.name)
                                                selectedEntryContent = content
                                                editableContent = content
                                                isEditingEntry = false
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                                    contentDescription = null,
                                                    tint = when {
                                                        entry.name.endsWith(".dex") -> RadiantLavender
                                                        entry.name.endsWith(".xml") -> EmeraldSuccess
                                                        entry.name.startsWith("lib/") -> CrimsonDanger
                                                        else -> PrimarySkyBlue
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = entry.name,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = DeepSlateText,
                                                    maxLines = 1
                                                )
                                            }
                                            Text(
                                                text = formatSize(entry.size),
                                                fontSize = 10.sp,
                                                color = SlateMutedText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ApkTab.MANIFEST -> {
                        var manifestContent by remember { mutableStateOf("Decompiling AndroidManifest.xml...") }
                        LaunchedEffect(Unit) {
                            manifestContent = engine.readEntryContent(apkFile, "AndroidManifest.xml")
                        }

                        // Code viewer with ultra-high contrast dark slate & cyan accent
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.85f),
                            backgroundColor = Color(0xFF0F172A),
                            borderColor = Color(0x4038BDF8)
                        ) {
                            LazyColumn(modifier = Modifier.padding(14.dp)) {
                                item {
                                    Text(
                                        text = manifestContent,
                                        color = Color(0xFFF8FAFC),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    ApkTab.DEX_STRINGS -> {
                        // MT Manager DEX String Searcher
                        Column(modifier = Modifier.fillMaxSize()) {
                            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = dexStringQuery,
                                        onValueChange = { dexStringQuery = it },
                                        placeholder = { Text("Search strings/classes in DEX pool...", color = SlateMutedText) },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedTextColor = DeepSlateText,
                                            unfocusedTextColor = DeepSlateText
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        scope.launch {
                                            isSearchingDex = true
                                            dexStringResults = engine.extractDexStrings(apkFile, dexStringQuery)
                                            isSearchingDex = false
                                        }
                                    }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimarySkyBlue)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isSearchingDex) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = RadiantLavender)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 90.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (dexStringResults.isEmpty()) {
                                        item {
                                            Text(
                                                "Tap search icon to extract constants from DEX string pool.",
                                                color = SlateMutedText,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    } else {
                                        items(dexStringResults) { str ->
                                            GlassCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("DEX String", str))
                                                    Toast.makeText(context, "Copied string to clipboard", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text(
                                                    text = str,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = DeepSlateText,
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ApkTab.SIGNATURES -> {
                        val s = apkDetails?.signatureInfo
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("APK Signature Verification", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepSlateText)
                                            GlassBadge(
                                                text = if (s?.hasV1Signature == true) "V1 SIGNED" else "UNSIGNED / V2+",
                                                color = if (s?.hasV1Signature == true) EmeraldSuccess else AmberWarning
                                            )
                                        }

                                        InfoRow("Signer Subject", s?.signers?.firstOrNull() ?: "Debug / Unknown")
                                        InfoRow("Issuer", s?.issuer ?: "Unknown")
                                        InfoRow("Valid Until", s?.validUntil ?: "N/A")
                                        InfoRow("SHA-1 Fingerprint", s?.certSha1 ?: "None")
                                        InfoRow("SHA-256 Fingerprint", s?.certSha256 ?: "None")
                                    }
                                }
                            }
                        }
                    }

                    ApkTab.MT_TOOLS -> {
                        // MT Manager Comprehensive Feature Center
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text("MT Manager APK Utilities", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DeepSlateText)
                            }

                            // 1. Sign APK (RSA-2048 V1)
                            item {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            val signed = File(Environment.getExternalStorageDirectory(), "NEXUS_APKs/${apkFile.nameWithoutExtension}_signed.apk")
                                            val res = engine.signApk(apkFile, signed)
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Signed APK created in NEXUS_APKs", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Sign failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(28.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Sign APK (V1 TestKey)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepSlateText)
                                            Text("Inject RSA-2048 test key signature & generate SHA-256 MANIFEST.MF", fontSize = 11.sp, color = SlateMutedText)
                                        }
                                    }
                                }
                            }

                            // 2. Zipalign APK (4-byte alignment)
                            item {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            val aligned = File(Environment.getExternalStorageDirectory(), "NEXUS_APKs/${apkFile.nameWithoutExtension}_aligned.apk")
                                            val res = engine.zipalignApk(apkFile, aligned)
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Zipaligned APK created in NEXUS_APKs", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Zipalign failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Icon(Icons.Default.Speed, contentDescription = null, tint = PrimarySkyBlue, modifier = Modifier.size(28.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Zipalign APK Archive", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepSlateText)
                                            Text("Optimize zip uncompressed page boundaries for Android runtime", fontSize = 11.sp, color = SlateMutedText)
                                        }
                                    }
                                }
                            }

                            // 3. Clone APK with New Identity
                            item {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            val clone = File(Environment.getExternalStorageDirectory(), "NEXUS_APKs/${apkFile.nameWithoutExtension}_clone.apk")
                                            val res = engine.cloneApk(apkFile, clone)
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Cloned to ${clone.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Icon(Icons.Default.ControlPointDuplicate, contentDescription = null, tint = RadiantLavender, modifier = Modifier.size(28.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Clone APK Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepSlateText)
                                            Text("Make an exact duplicable standalone copy of this package", fontSize = 11.sp, color = SlateMutedText)
                                        }
                                    }
                                }
                            }

                            // 4. Extract All DEX Files
                            item {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            val outDir = File(Environment.getExternalStorageDirectory(), "NEXUS_APKs/dex_${apkFile.nameWithoutExtension}")
                                            outDir.mkdirs()
                                            var extracted = 0
                                            apkEntries.filter { it.name.endsWith(".dex") }.forEach {
                                                engine.extractEntryToFile(apkFile, it.name, File(outDir, it.name))
                                                extracted++
                                            }
                                            Toast.makeText(context, "Extracted $extracted DEX files to ${outDir.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(28.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Batch Extract All DEX", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepSlateText)
                                            Text("Extract classes*.dex into a folder for Smali/Baksmali analysis", fontSize = 11.sp, color = SlateMutedText)
                                        }
                                    }
                                }
                            }

                            // 5. Inspect resources.arsc
                            item {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            selectedEntryName = "resources.arsc"
                                            val content = engine.readEntryContent(apkFile, "resources.arsc")
                                            selectedEntryContent = content
                                            editableContent = content
                                            isEditingEntry = false
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Icon(Icons.Default.Layers, contentDescription = null, tint = LuminousCyan, modifier = Modifier.size(28.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("ARSC Resource Table Viewer", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepSlateText)
                                            Text("Inspect compiled resource string pools, styles, and configs", fontSize = 11.sp, color = SlateMutedText)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog for viewing / editing file content inside APK with High-Contrast Text
        if (selectedEntryContent != null) {
            AlertDialog(
                onDismissRequest = { selectedEntryContent = null },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selectedEntryName ?: "Entry",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepSlateText,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        val isEditable = selectedEntryName?.endsWith(".dex") == false && selectedEntryName != "resources.arsc"
                        if (isEditable) {
                            TextButton(onClick = { isEditingEntry = !isEditingEntry }) {
                                Text(
                                    if (isEditingEntry) "Cancel" else "Edit",
                                    color = PrimarySkyBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                text = {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        if (isEditingEntry) {
                            OutlinedTextField(
                                value = editableContent,
                                onValueChange = { editableContent = it },
                                modifier = Modifier.fillMaxSize(),
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF0F172A) // Crisp dark text
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color(0xFFFFFFFF),
                                    unfocusedContainerColor = Color(0xFFF8FAFC),
                                    focusedBorderColor = PrimarySkyBlue,
                                    unfocusedBorderColor = SlateSubtleText
                                )
                            )
                        } else {
                            // Ultra crisp readable viewer container with dark slate background
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(12.dp)
                            ) {
                                LazyColumn {
                                    item {
                                        Text(
                                            text = selectedEntryContent!!,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFF8FAFC), // Pure bright crisp white
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (isEditingEntry) {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = PrimarySkyBlue),
                            onClick = {
                                scope.launch {
                                    val outputApk = File(Environment.getExternalStorageDirectory(), "NEXUS_APKs/${apkFile.nameWithoutExtension}_modified.apk")
                                    val res = engine.replaceOrAddAsset(apkFile, selectedEntryName!!, editableContent.toByteArray(Charsets.UTF_8), outputApk)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Saved modified APK to NEXUS_APKs", Toast.LENGTH_SHORT).show()
                                        selectedEntryContent = null
                                    } else {
                                        Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Text("Save As Modified APK", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                scope.launch {
                                    val outDir = File(Environment.getExternalStorageDirectory(), "NEXUS_APKs/extracted_${apkFile.nameWithoutExtension}")
                                    val outFile = File(outDir, selectedEntryName!!)
                                    val res = engine.extractEntryToFile(apkFile, selectedEntryName!!, outFile)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Extracted to ${outFile.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Text("Extract File", color = PrimarySkyBlue, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { selectedEntryContent = null }) {
                                Text("Close", color = DeepSlateText)
                            }
                        }
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
        Text(text = label, fontSize = 12.sp, color = SlateMutedText)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepSlateText)
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
