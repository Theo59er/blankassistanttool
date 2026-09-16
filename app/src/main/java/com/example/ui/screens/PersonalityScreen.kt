package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantDream
import com.example.data.model.AssistantMemory
import com.example.data.model.MemoryImportance
import com.example.data.model.PersonalityState
import com.example.ui.components.EpicEmblem
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberPanel
import com.example.ui.theme.DreamCosmic
import com.example.ui.theme.DreamIndigo
import com.example.ui.theme.DreamViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.viewmodel.AppUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PersonalityScreen(
    uiState: AppUiState,
    onTriggerDream: () -> Unit,
    onAddMemory: (text: String, category: String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onToggleNightMode: (Boolean) -> Unit,
    onSetDreamInterval: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var newMemoryText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Erkenntnis") }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("Erkenntnis", "Nutzer-Präferenz", "Persönlichkeit", "Kalender-Muster")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CyberDark,
                        CyberPanel,
                        DreamCosmic
                    )
                )
            )
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card: Personality & Soul Level
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CyberPanel.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(NeonCyan, NeonPurple, NeonPink)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .testTag("personality_hero_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Epic Animated Emblem
                    EpicEmblem(
                        size = 84.dp,
                        isActive = true,
                        isDreaming = uiState.isDreamingActive,
                        iconColor = NeonCyan
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "KOGNITIVE ASSISTENTEN-SEELE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Level ${uiState.personalityState.level} • ${uiState.personalityState.affinity}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // XP Progress bar
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Kognitions-XP",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${uiState.personalityState.experiencePoints} / 500 XP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (uiState.personalityState.experiencePoints % 500) / 500f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = NeonCyan,
                            trackColor = CyberBorder
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatPill(
                            label = "Erinnerungen",
                            value = "${uiState.memories.size}",
                            color = NeonCyan
                        )
                        StatPill(
                            label = "Träume",
                            value = "${uiState.dreams.size}",
                            color = NeonPurple
                        )
                        StatPill(
                            label = "Retention",
                            value = "${(uiState.personalityState.memoryRetentionRate * 100).toInt()}%",
                            color = NeonPink
                        )
                    }
                }
            }
        }

        // Night Dreaming / Cognitive Reflection Control
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberPanel),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DreamViolet.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = NeonPurple
                                )
                            }
                            Column {
                                Text(
                                    text = "Nächtliche Träum-Funktion",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Konsolidiert Erinnerungen & lernt alle 2 Std.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }

                        Switch(
                            checked = uiState.personalityState.isNightModeDreamingActive,
                            onCheckedChange = onToggleNightMode,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = DreamViolet,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = CyberBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = CyberBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Action button: Trigger dream now
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onTriggerDream,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("trigger_dream_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DreamViolet,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Jetzt träumen & lernen", fontWeight = FontWeight.Bold)
                        }

                        // Cycle interval button (e.g. 2h)
                        OutlinedButton(
                            onClick = {
                                val next = if (uiState.personalityState.autoDreamIntervalHours == 2) 4 else 2
                                onSetDreamInterval(next)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text(
                                text = "⏱ ${uiState.personalityState.autoDreamIntervalHours}h Takt",
                                color = NeonCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Recent Dream Card (if any)
        if (uiState.dreams.isNotEmpty()) {
            item {
                val latestDream = uiState.dreams.first()
                val dateStr = SimpleDateFormat("dd.MM. HH:mm 'Uhr'", Locale.GERMANY).format(Date(latestDream.timestamp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DreamCosmic.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "JÜNGSTER TRAUM ($dateStr)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = latestDream.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = latestDream.narrative,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Stimmung: ${latestDream.mood}",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                            Text(
                                text = "•  ${latestDream.memoriesConsolidated} Muster konsolidiert",
                                fontSize = 11.sp,
                                color = NeonGold
                            )
                        }
                    }
                }
            }
        }

        // Long-term Memories Header + Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Langzeit-Gedächtnis (${uiState.memories.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = { showAddDialog = !showAddDialog },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberBorder,
                        contentColor = NeonCyan
                    )
                ) {
                    Text(if (showAddDialog) "Schließen" else "+ Erinnerung", fontSize = 13.sp)
                }
            }
        }

        // Add Memory expansion box
        if (showAddDialog) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberPanel),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Neue Erinnerung für die KI manuell anlegen:",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newMemoryText,
                            onValueChange = { newMemoryText = it },
                            placeholder = { Text("z.B. Nutzer trinkt morgens gerne Espresso...", color = Color.Gray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("memory_input_field"),
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = NeonCyan
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (newMemoryText.isNotBlank()) {
                                    onAddMemory(newMemoryText, selectedCategory)
                                    newMemoryText = ""
                                    showAddDialog = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_memory_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberDark)
                        ) {
                            Text("Im Langzeitgedächtnis speichern", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Memories List
        if (uiState.memories.isEmpty()) {
            item {
                Text(
                    text = "Noch keine Erinnerungen vorhanden. Die KI lernt automatisch bei jedem Gespräch oder Traum!",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(uiState.memories, key = { it.id }) { memory ->
                MemoryItemCard(
                    memory = memory,
                    onDelete = { onDeleteMemory(memory.id) }
                )
            }
        }
    }
}

@Composable
fun MemoryItemCard(
    memory: AssistantMemory,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(memory.timestamp))
    val badgeColor = when (memory.category) {
        "Nutzer-Präferenz" -> NeonPink
        "Persönlichkeit" -> NeonCyan
        "Traum-Synthese" -> NeonPurple
        else -> NeonGold
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberPanel.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = memory.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = memory.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 19.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Löschen / Vergessen",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun StatPill(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}
