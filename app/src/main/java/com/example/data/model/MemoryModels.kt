package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MemoryImportance {
    LOW,
    NORMAL,
    HIGH,
    CORE
}

enum class DreamStage {
    DAYDREAM,
    DEEP_DREAM,
    LUCID_INSIGHT,
    CONSOLIDATING
}

@Entity(tableName = "assistant_memories")
data class AssistantMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val text: String,
    val category: String = "Erkenntnis", // "Nutzer-Präferenz", "Persönlichkeit", "Kalender-Muster", "Weltwissen"
    val timestamp: Long = System.currentTimeMillis(),
    val importance: MemoryImportance = MemoryImportance.NORMAL,
    val recallCount: Int = 0,
    val lastRecalledAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)

data class AssistantDream(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val narrative: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mood: String = "Nachdenklich & Wachsam",
    val stage: DreamStage = DreamStage.LUCID_INSIGHT,
    val memoriesConsolidated: Int = 0
)

data class PersonalityState(
    val level: Int = 1,
    val experiencePoints: Int = 120,
    val affinity: String = "Analytisch & Scharfsinnig",
    val moodStatus: String = "Aktiv & Aufmerksam",
    val memoryRetentionRate: Float = 0.94f,
    val dreamsCount: Int = 3,
    val isNightModeDreamingActive: Boolean = true,
    val autoDreamIntervalHours: Int = 2
)
