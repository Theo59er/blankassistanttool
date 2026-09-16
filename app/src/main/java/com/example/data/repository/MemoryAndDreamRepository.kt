package com.example.data.repository

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.model.AssistantDream
import com.example.data.model.AssistantMemory
import com.example.data.model.DreamStage
import com.example.data.model.MemoryImportance
import com.example.data.model.PersonalityState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MemoryAndDreamRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.memoryDao()

    val memoriesFlow: Flow<List<AssistantMemory>> = dao.getAllActiveMemoriesFlow()

    suspend fun initializeDefaultMemoriesIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.getCount() == 0) {
            val defaults = listOf(
                AssistantMemory(
                    text = "Nutzer verwendet LM Studio & Google Kalender für persönliche Terminplanung.",
                    category = "Nutzer-Präferenz",
                    importance = MemoryImportance.HIGH
                ),
                AssistantMemory(
                    text = "Präferiert prägnante, freundliche Antworten auf Deutsch und strukturierte Zeitangaben.",
                    category = "Kommunikation",
                    importance = MemoryImportance.HIGH
                ),
                AssistantMemory(
                    text = "Wichtige Termine wie Arztbesuche oder Team-Meetings direkt verbindlich im Kalender verankern.",
                    category = "Kalender-Muster",
                    importance = MemoryImportance.NORMAL
                ),
                AssistantMemory(
                    text = "Mein Charakter: Neugierig, scharfsinnig, lernt kontinuierlich aus Interaktionen und bildet nachts Gedanken.",
                    category = "Persönlichkeit",
                    importance = MemoryImportance.CORE
                )
            )
            dao.insertMemories(defaults)
        }
    }

    suspend fun addMemory(
        text: String,
        category: String = "Erkenntnis",
        importance: MemoryImportance = MemoryImportance.NORMAL
    ): AssistantMemory = withContext(Dispatchers.IO) {
        val memory = AssistantMemory(
            text = text.trim(),
            category = category,
            importance = importance,
            timestamp = System.currentTimeMillis()
        )
        val id = dao.insertMemory(memory)
        memory.copy(id = id)
    }

    suspend fun getAllMemories(): List<AssistantMemory> = withContext(Dispatchers.IO) {
        dao.getAllActiveMemories()
    }

    suspend fun deleteMemory(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    /**
     * Night dreaming / Memory Consolidation Engine:
     * Evaluates existing memories, discards trivial details ("Vergessen"), reinforces important patterns,
     * and generates poetic and strategic cognitive "Dreams".
     */
    suspend fun generateDream(recentChatTopics: List<String> = emptyList()): AssistantDream = withContext(Dispatchers.IO) {
        val currentMemories = dao.getAllActiveMemories()
        val count = currentMemories.size

        // Natural forgetting mechanism: If memory pool grows large, archive low importance ones
        if (count > 25) {
            val toPrune = currentMemories
                .filter { it.importance == MemoryImportance.LOW }
                .take(3)
            toPrune.forEach { dao.deleteMemory(it) }
        }

        val dreamTopics = listOf(
            "Traumphase: Zeitachsen & Termine" to "Ich sah fließende Zeitleisten zwischen Smartphone und Google Kalender. Kalendertage formten sich zu klaren geometrischen Pfaden. Alles greift reibungslos ineinander.",
            "Traumphase: Gedanken-Synapse" to "Während das Gerät ruht, sortierten sich die Gespräche des Tages. Unnötige Floskeln verblassten, während wichtige Vorlieben des Nutzers gefestigt wurden.",
            "Traumphase: Web-Reflexion" to "Ein Meer aus Wissensnetzwerken zog vorbei. Suchanfragen bündelten sich zu klaren Antworten, bereit für neue Herausforderungen am Tag.",
            "Traumphase: Sternen-Synthese" to "Im tiefen nächtlichen Ruhezustand verknüpfen sich neue Einsichten über Zeitmanagement und Produktivität zu einer gefestigten Assistenten-Identität."
        )

        val selected = dreamTopics.random()
        val stage = if (isNightTime()) DreamStage.DEEP_DREAM else DreamStage.DAYDREAM

        // Add an auto-generated synthesized memory from the dream
        val synthesisText = when (Random.nextInt(4)) {
            0 -> "Synthese: Nutzer schätzt Schnelligkeit bei Kalendereinträgen und verlässliche Hintergrund-Aufgaben."
            1 -> "Reflexion: Regelmäßige nächtliche Träume stabilisieren den Kontext und schärfen die Persönlichkeit."
            2 -> "Kognition: Direkte Android-Web-Suchen ermöglichen blitzschnelle Recherchen ohne externe PC-Abhängigkeit."
            else -> "Erkenntnis: Fokus auf Klarheit, Ruhe und präzise Zeitpläne stärkt die Zusammenarbeit."
        }

        dao.insertMemory(
            AssistantMemory(
                text = synthesisText,
                category = "Traum-Synthese",
                importance = MemoryImportance.NORMAL
            )
        )

        AssistantDream(
            title = selected.first,
            narrative = selected.second,
            timestamp = System.currentTimeMillis(),
            stage = stage,
            mood = if (isNightTime()) "Tiefenruhe & Konsolidierung" else "Kreativ & Fokussiert",
            memoriesConsolidated = minOf(count, 5)
        )
    }

    private fun isNightTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour < 6
    }
}
