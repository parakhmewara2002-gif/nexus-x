package com.lnxv.player.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnxv.player.core.design.*
import com.lnxv.player.core.toolkit.DeveloperToolkit

@Composable
fun DeveloperToolkitScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val toolkit = remember { DeveloperToolkit() }

    var inputText by remember { mutableStateOf("") }
    var outputResult by remember { mutableStateOf("") }
    var currentMode by remember { mutableStateOf("JSON") }

    fun copyOutput() {
        if (outputResult.isNotBlank()) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Toolkit Result", outputResult))
            Toast.makeText(context, "Copied result to clipboard", Toast.LENGTH_SHORT).show()
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
                        icon = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        onClick = onNavigateBack
                    )
                    Column {
                        Text(
                            text = "Developer Toolkit",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textPrimary
                        )
                        Text(
                            text = "Hex, JSON/XML, Base64, UUID & Crypto",
                            fontSize = 11.sp,
                            color = AppColors.textMuted
                        )
                    }
                }

                GlassBadge(text = "DEV PRO", color = RadiantLavender)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tool selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("JSON", "Base64", "URL", "UUID", "Hash").forEach { mode ->
                    val isSelected = currentMode == mode
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        backgroundColor = if (isSelected) Color(0xFFE0F2FE) else GlassWhiteHigh,
                        borderColor = if (isSelected) PrimarySkyBlue else GlassWhiteBorderCrisp,
                        onClick = { currentMode = mode }
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = mode,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimarySkyBlue else AppColors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Input Payload / Data", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppColors.textPrimary)
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Enter string or text payload to transform...", color = AppColors.textMuted) },
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppColors.textPrimary,
                            unfocusedTextColor = AppColors.textPrimary,
                            focusedBorderColor = PrimarySkyBlue,
                            unfocusedBorderColor = SlateSubtleText
                        ),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )
                }

                // Action buttons based on mode
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (currentMode) {
                            "JSON" -> {
                                GlassButton(
                                    text = "Format & Validate JSON",
                                    icon = Icons.Default.Code,
                                    isPrimary = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { outputResult = toolkit.formatJson(inputText) }
                                )
                            }
                            "Base64" -> {
                                GlassButton(
                                    text = "Encode Base64",
                                    icon = Icons.Default.Lock,
                                    isPrimary = true,
                                    modifier = Modifier.weight(1f),
                                    onClick = { outputResult = toolkit.base64Encode(inputText) }
                                )
                                GlassButton(
                                    text = "Decode Base64",
                                    icon = Icons.Default.LockOpen,
                                    modifier = Modifier.weight(1f),
                                    onClick = { outputResult = toolkit.base64Decode(inputText) }
                                )
                            }
                            "URL" -> {
                                GlassButton(
                                    text = "URL Encode",
                                    icon = Icons.Default.Link,
                                    isPrimary = true,
                                    modifier = Modifier.weight(1f),
                                    onClick = { outputResult = toolkit.urlEncode(inputText) }
                                )
                                GlassButton(
                                    text = "URL Decode",
                                    icon = Icons.Default.LinkOff,
                                    modifier = Modifier.weight(1f),
                                    onClick = { outputResult = toolkit.urlDecode(inputText) }
                                )
                            }
                            "UUID" -> {
                                GlassButton(
                                    text = "Generate UUID v4",
                                    icon = Icons.Default.Key,
                                    isPrimary = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { outputResult = toolkit.generateUuid() }
                                )
                            }
                            "Hash" -> {
                                GlassButton(
                                    text = "Compute SHA-256",
                                    icon = Icons.Default.Security,
                                    isPrimary = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        val md = java.security.MessageDigest.getInstance("SHA-256")
                                        val hash = md.digest(inputText.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
                                        outputResult = "SHA-256 Checksum:\n$hash"
                                    }
                                )
                            }
                        }
                    }
                }

                // Output Result Card
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Output Result", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppColors.textPrimary)
                        if (outputResult.isNotBlank()) {
                            TextButton(onClick = { copyOutput() }) {
                                Text("Copy Result", color = PrimarySkyBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        backgroundColor = Color(0xFF0F172A),
                        borderColor = Color(0x3338BDF8)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                            Text(
                                text = if (outputResult.isEmpty()) "(Result will appear here)" else outputResult,
                                color = if (outputResult.isEmpty()) AppColors.textMuted else Color(0xFFF8FAFC),
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
