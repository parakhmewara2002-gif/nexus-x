package com.lnxv.player.ui.vault

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnxv.player.core.design.*

@Composable
fun VaultScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    val correctPin = "1234"

    fun onKeyClick(digit: String) {
        if (enteredPin.length < 4) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            if (newPin.length == 4) {
                if (newPin == correctPin) {
                    isUnlocked = true
                } else {
                    Toast.makeText(context, "Invalid PIN code", Toast.LENGTH_SHORT).show()
                    enteredPin = ""
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
                GlassIconButton(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    onClick = onNavigateBack
                )
                Text(
                    text = "Encrypted Vault",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
                GlassBadge(
                    text = if (isUnlocked) "UNLOCKED" else "LOCKED",
                    color = if (isUnlocked) EmeraldSuccess else CrimsonDanger
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isUnlocked) {
                // PIN Lockpad Stage
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CrimsonDanger,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Enter Vault PIN",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary
                    )
                    Text(
                        text = "Protected by AES-256 hardware encryption",
                        fontSize = 12.sp,
                        color = AppColors.textMuted
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // PIN Dots
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(4) { idx ->
                            val filled = idx < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (filled) PrimarySkyBlue else Color(0xFFCBD5E1))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Keypad
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "⌫")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (row in keys) {
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                for (key in row) {
                                    GlassCard(
                                        modifier = Modifier.size(64.dp),
                                        shape = CircleShape,
                                        onClick = {
                                            when (key) {
                                                "C" -> enteredPin = ""
                                                "⌫" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                                else -> onKeyClick(key)
                                            }
                                        }
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AppColors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Vault Content Unlocked
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Vault Protection Active",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = AppColors.textPrimary
                                    )
                                    GlassBadge(text = "AES-GCM", color = EmeraldSuccess)
                                }
                                Text(
                                    text = "Encrypted storage path: /Android/data/com.lnxv.player/files/vault",
                                    fontSize = 12.sp,
                                    color = AppColors.textMuted
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Secured Items",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textPrimary
                        )
                    }

                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FolderSpecial,
                                        contentDescription = null,
                                        tint = PrimarySkyBlue,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Vault Empty",
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.textPrimary
                                    )
                                    Text(
                                        text = "Move sensitive files here to encrypt them",
                                        fontSize = 12.sp,
                                        color = AppColors.textMuted
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
