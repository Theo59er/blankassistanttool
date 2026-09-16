package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiProvider
import com.example.data.model.OpenClawConfig
import com.example.ui.viewmodel.AppUiState

@Composable
fun SettingsScreen(
    uiState: AppUiState,
    onSaveConfig: (provider: AiProvider, hostIp: String, port: Int, modelName: String, apiToken: String, systemPrompt: String) -> Unit,
    onSelectProvider: (AiProvider) -> Unit,
    onSelectModel: (String) -> Unit,
    onTestConnection: () -> Unit,
    onRefreshDiagnostics: () -> Unit,
    onSendDeviceReport: () -> Unit,
    onUpdateEmail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cfg = uiState.openClawConfig
    var hostIp by remember(cfg.hostIp) { mutableStateOf(cfg.hostIp) }
    var portText by remember(cfg.port) { mutableStateOf(cfg.port.toString()) }
    var modelName by remember(cfg.modelName) { mutableStateOf(cfg.modelName) }
    var apiToken by remember(cfg.apiToken) { mutableStateOf(cfg.apiToken) }
    var systemPrompt by remember(cfg.customSystemPrompt) { mutableStateOf(cfg.customSystemPrompt) }

    var showPromptEditor by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var tempEmail by remember { mutableStateOf(uiState.googleAccount.email) }

    fun commitConfig(
        p: AiProvider = cfg.provider,
        h: String = hostIp,
        pt: String = portText,
        m: String = modelName,
        t: String = apiToken,
        sp: String = systemPrompt
    ) {
        val parsedPort = pt.toIntOrNull() ?: p.defaultPort
        onSaveConfig(p, h, parsedPort, m, t, sp)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: KI Provider Auswahl (LM Studio vs OpenClaw)
        Text(
            text = "KI-Schnittstelle & Server-Setup",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Wähle deine PC KI-Schnittstelle:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = cfg.provider == AiProvider.LM_STUDIO,
                        onClick = {
                            onSelectProvider(AiProvider.LM_STUDIO)
                            portText = "1234"
                            commitConfig(p = AiProvider.LM_STUDIO, pt = "1234")
                        },
                        label = { Text("🟣 LM Studio (PC)") },
                        modifier = Modifier.weight(1f).testTag("provider_chip_lmstudio")
                    )
                    FilterChip(
                        selected = cfg.provider == AiProvider.OPEN_CLAW,
                        onClick = {
                            onSelectProvider(AiProvider.OPEN_CLAW)
                            portText = "8080"
                            commitConfig(p = AiProvider.OPEN_CLAW, pt = "8080")
                        },
                        label = { Text("🔵 OpenClaw") },
                        modifier = Modifier.weight(1f).testTag("provider_chip_openclaw")
                    )
                }

                if (cfg.provider == AiProvider.LM_STUDIO) {
                    Text(
                        "LM Studio nutzt die OpenAI-kompatible Schnittstelle (/v1/chat/completions) auf Standardport 1234. Alle Prompts und Werkzeuge (Google Kalender etc.) liegen auf deinem Handy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // PC IP Address Input
                OutlinedTextField(
                    value = hostIp,
                    onValueChange = {
                        hostIp = it
                        commitConfig(h = it)
                    },
                    label = { Text("PC IP-Adresse im Heimnetz (WLAN)") },
                    leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pc_ip_input")
                )

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("192.168.1.100", "192.168.0.100", "192.168.178.50").forEach { preset ->
                        FilterChip(
                            selected = hostIp == preset,
                            onClick = {
                                hostIp = preset
                                commitConfig(h = preset)
                            },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }

                // Port and Model Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = portText,
                        onValueChange = {
                            portText = it
                            commitConfig(pt = it)
                        },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.35f)
                            .testTag("pc_port_input")
                    )

                    OutlinedTextField(
                        value = modelName,
                        onValueChange = {
                            modelName = it
                            commitConfig(m = it)
                        },
                        label = { Text("Modell-Name") },
                        leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.65f)
                            .testTag("model_name_input")
                    )
                }

                // If detected models available from LM Studio, display chips
                if (cfg.detectedModels.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "In LM Studio erkannte Modelle (tippen zum Auswählen):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            cfg.detectedModels.forEach { m ->
                                FilterChip(
                                    selected = modelName == m,
                                    onClick = {
                                        modelName = m
                                        onSelectModel(m)
                                        commitConfig(m = m)
                                    },
                                    label = { Text(m.take(24), fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Connection Test Button & Live Feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Status: ${cfg.statusText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (cfg.isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (cfg.lastPingMs > 0) {
                            Text(
                                text = "Latenz: ${cfg.lastPingMs} ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Button(
                        onClick = onTestConnection,
                        enabled = !uiState.isTestingConnection,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("settings_test_connection_button")
                    ) {
                        if (uiState.isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Prüfe...")
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verbindung testen")
                        }
                    }
                }
            }
        }

        // Section: System-Prompt & Handy-Instruktionen
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("System-Prompt & Instruktionen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Liegt auf deinem Handy und steuert die PC-KI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    IconButton(onClick = { showPromptEditor = !showPromptEditor }) {
                        Icon(
                            if (showPromptEditor) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Aufklappen"
                        )
                    }
                }

                AnimatedVisibility(visible = showPromptEditor) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Diese Anweisungen werden vor jeder Chat-Nachricht an LM Studio gesendet. Sie bringen dem lokalen KI-Modell bei, wie es Google Kalender Befehle für dein Smartphone formatiert:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = systemPrompt.ifBlank { OpenClawConfig.DEFAULT_SYSTEM_PROMPT },
                            onValueChange = {
                                systemPrompt = it
                                commitConfig(sp = it)
                            },
                            label = { Text("System-Prompt") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            maxLines = 10
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    systemPrompt = ""
                                    commitConfig(sp = "")
                                }
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auf Standard zurücksetzen")
                            }
                        }
                    }
                }
            }
        }

        // Section: Google Konto & Kalender Status
        Text(
            text = "Google Konto & Kalender",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text(uiState.googleAccount.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(uiState.googleAccount.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            tempEmail = uiState.googleAccount.email
                            showEmailDialog = true
                        }
                    ) {
                        Text("Ändern")
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Android Kalender-Zugriff", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (uiState.hasCalendarPermission) "Aktiviert ✅" else "Nicht erteilt ⚠️",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.hasCalendarPermission) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Section: Handy-Diagnose & Telemetrie
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Smartphone Diagnose & Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onRefreshDiagnostics) {
                Text("Aktualisieren")
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Gerät & OS", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("${uiState.deviceDiagnostics.deviceModel} • ${uiState.deviceDiagnostics.androidVersion}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            if (uiState.deviceDiagnostics.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Akkustand", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("${uiState.deviceDiagnostics.batteryPct}% ${if (uiState.deviceDiagnostics.isCharging) "(Laden)" else ""}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Handy-IP im WLAN", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(uiState.deviceDiagnostics.localIp, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Freier Speicher", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("${uiState.deviceDiagnostics.freeStorageGb} GB", style = MaterialTheme.typography.labelMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Verfügbarer RAM", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("${uiState.deviceDiagnostics.availableRamGb} GB", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onSendDeviceReport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gerätestatus aktualisieren")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Google Konto anpassen") },
            text = {
                OutlinedTextField(
                    value = tempEmail,
                    onValueChange = { tempEmail = it },
                    label = { Text("Google E-Mail-Adresse") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempEmail.isNotBlank()) {
                            onUpdateEmail(tempEmail.trim())
                        }
                        showEmailDialog = false
                    }
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
