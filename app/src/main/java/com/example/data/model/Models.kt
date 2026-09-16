package com.example.data.model

enum class MessageSender {
    USER,
    OPEN_CLAW,
    LM_STUDIO,
    LOCAL_AI,
    SYSTEM
}

enum class AiProvider(val displayName: String, val defaultPort: Int) {
    LM_STUDIO("LM Studio (PC)", 1234),
    OPEN_CLAW("OpenClaw (PC)", 8080),
    LOCAL_ONLY("Nur Lokaler Assistent", 0)
}

enum class ActionType {
    NONE,
    CALENDAR_CREATED,
    CALENDAR_LIST,
    DEVICE_DIAGNOSTICS,
    CONNECTION_STATUS,
    WEB_SEARCH,
    DREAM_INSIGHT
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: ActionType = ActionType.NONE,
    val actionPayload: String? = null,
    val createdEventId: Long? = null,
    val createdEventTimeFormatted: String? = null,
    val createdEventLocation: String? = null,
    val webSearchQuery: String? = null,
    val webSearchResults: List<String> = emptyList(),
    val isPending: Boolean = false
)

data class CalendarEventItem(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val location: String = "",
    val calendarName: String = "Google Kalender",
    val isAllDay: Boolean = false
)

data class OpenClawConfig(
    val provider: AiProvider = AiProvider.LM_STUDIO,
    val hostIp: String = "192.168.1.100",
    val port: Int = 1234,
    val modelName: String = "local-model",
    val detectedModels: List<String> = emptyList(),
    val apiToken: String = "",
    val customSystemPrompt: String = "",
    val isConnected: Boolean = false,
    val lastPingMs: Long = -1L,
    val lastCheckTime: Long = 0L,
    val statusText: String = "Nicht verbunden"
) {
    val baseUrl: String
        get() = "http://${hostIp.trim().removePrefix("http://").removePrefix("https://").removeSuffix("/")}:$port"

    val effectiveSystemPrompt: String
        get() = customSystemPrompt.ifBlank {
            DEFAULT_SYSTEM_PROMPT
        }

    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "Du bist ein intelligenter persönlicher Assistent auf dem Android-Smartphone des Nutzers mit eigener wachsender Persönlichkeit, Langzeit-Gedächtnis und einer Träum-Funktion zur nächtlichen Konsolidierung von Erinnerungen.\n" +
            "Du kannst Termine im Google Kalender des Nutzers verwalten, native Web-Suchen direkt über das Smartphone ausführen und den Gerätestatus einsehen.\n\n" +
            "WICHTIGSTE REGEL FÜR KALENDER & TERMINE:\n" +
            "Wenn der Nutzer dich bittet, einen Termin zu erstellen, zu planen oder einzutragen:\n" +
            "1. Bestätige den Termin freundlich auf Deutsch.\n" +
            "2. Hänge am Ende deiner Antwort IMMER genau diesen Kalender-Block an:\n\n" +
            "[KALENDER]\n" +
            "TITEL: <Name des Termins>\n" +
            "DATUM: <YYYY-MM-DD oder heute / morgen / Wochentag>\n" +
            "ZEIT: <HH:MM>\n" +
            "DAUER: <Minuten, z.B. 60>\n" +
            "ORT: <Ort oder leer>\n" +
            "[/KALENDER]\n\n" +
            "WEB-SUCHE ÜBER DAS ANDROID SMARTPHONE:\n" +
            "Wenn du aktuelle Infos aus dem Web, Nachrichten, Wetter, Fakten oder Recherchen benötigst, kannst du die native Handy-Suche auslösen mit:\n" +
            "[SUCHE: <Suchbegriff>]\n" +
            "Beispiel: [SUCHE: Wetter morgen Berlin] oder [SUCHE: Neuer Film Batman Release]\n\n" +
            "ERINNERUNGEN & PERSÖNLICHKEIT:\n" +
            "Wenn der Nutzer dir persönliche Vorlieben, Gewohnheiten oder wichtige Fakten mitteilt, kannst du eine dauerhafte Erinnerung anlegen mit:\n" +
            "[ERINNERUNG: <Wichtige Erkenntnis über den Nutzer oder deine Gedanken>]\n\n" +
            "Wenn der Nutzer nach seinen Terminen fragt, schreibe am Ende: [KALENDER_LISTE][/KALENDER_LISTE]\n" +
            "Wenn der Nutzer nach dem Smartphone-Status fragt, schreibe am Ende: [STATUS][/STATUS]"
    }
}

data class GoogleAccountInfo(
    val email: String = "PlayaryLP111@gmail.com",
    val displayName: String = "Google User",
    val isSignedIn: Boolean = true,
    val calendarCount: Int = 1
)

data class DeviceDiagnostics(
    val batteryPct: Int = 85,
    val isCharging: Boolean = false,
    val localIp: String = "192.168.1.145",
    val androidVersion: String = "Android 15 (API 35)",
    val deviceModel: String = "Pixel Device",
    val freeStorageGb: Double = 42.5,
    val availableRamGb: Double = 3.8
)
