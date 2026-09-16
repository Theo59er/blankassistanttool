package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.device.DeviceManager
import com.example.data.model.ActionType
import com.example.data.model.AiProvider
import com.example.data.model.CalendarEventItem
import com.example.data.model.ChatMessage
import com.example.data.model.DeviceDiagnostics
import com.example.data.model.GoogleAccountInfo
import com.example.data.model.MessageSender
import com.example.data.model.OpenClawConfig
import com.example.data.network.OpenClawClient
import com.example.data.repository.CalendarRepository
import com.example.data.util.CalendarActionParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppUiState(
    val openClawConfig: OpenClawConfig = OpenClawConfig(),
    val googleAccount: GoogleAccountInfo = GoogleAccountInfo(),
    val calendarEvents: List<CalendarEventItem> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val hasCalendarPermission: Boolean = false,
    val isTestingConnection: Boolean = false,
    val isChatLoading: Boolean = false,
    val isSyncingCalendar: Boolean = false,
    val deviceDiagnostics: DeviceDiagnostics = DeviceDiagnostics(),
    val statusBanner: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val calendarRepo = CalendarRepository(application)
    private val openClawClient = OpenClawClient()
    private val deviceManager = DeviceManager(application)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        refreshDiagnostics()
        refreshCalendarEvents()
        initWelcomeChat()
    }

    private fun initWelcomeChat() {
        val welcome = ChatMessage(
            sender = MessageSender.LM_STUDIO,
            text = "Hallo! Ich bin dein persönlicher KI-Assistent auf deinem Smartphone mit direkter Anbindung an deinen Google Kalender (${uiState.value.googleAccount.email}).\n\n" +
                    "✨ Du kannst LM Studio auf deinem PC nutzen (Standard-Port 1234) oder OpenClaw.\n" +
                    "📅 Gib mir einfach Befehle wie:\n" +
                    "• 'Neuer Termin: Zahnarzt am Freitag um 14 Uhr'\n" +
                    "• 'Was steht morgen an?'\n" +
                    "• 'Wie ist mein Smartphone-Status?'",
            actionType = ActionType.NONE
        )
        _uiState.update { it.copy(chatMessages = listOf(welcome)) }
    }

    fun refreshDiagnostics() {
        val diag = deviceManager.getDeviceDiagnostics()
        val hasPerm = calendarRepo.hasCalendarPermission()
        _uiState.update {
            it.copy(
                deviceDiagnostics = diag,
                hasCalendarPermission = hasPerm
            )
        }
    }

    fun refreshCalendarEvents() {
        viewModelScope.launch {
            val events = calendarRepo.getUpcomingEvents()
            val hasPerm = calendarRepo.hasCalendarPermission()
            _uiState.update {
                it.copy(
                    calendarEvents = events,
                    hasCalendarPermission = hasPerm
                )
            }
        }
    }

    fun onCalendarPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasCalendarPermission = granted) }
        refreshCalendarEvents()
        if (granted) {
            postSystemMessage("Google Kalender Zugriff erteilt! Live-Termine wurden synchronisiert.")
        }
    }

    fun updateServerConfig(
        provider: AiProvider,
        hostIp: String,
        port: Int,
        modelName: String,
        apiToken: String,
        systemPrompt: String
    ) {
        _uiState.update {
            it.copy(
                openClawConfig = it.openClawConfig.copy(
                    provider = provider,
                    hostIp = hostIp.trim(),
                    port = port,
                    modelName = modelName.trim(),
                    apiToken = apiToken.trim(),
                    customSystemPrompt = systemPrompt
                )
            )
        }
    }

    fun setAiProvider(provider: AiProvider) {
        val currentPort = _uiState.value.openClawConfig.port
        val newPort = if (provider == AiProvider.LM_STUDIO && currentPort == 8080) 1234
        else if (provider == AiProvider.OPEN_CLAW && currentPort == 1234) 8080
        else currentPort

        _uiState.update {
            it.copy(
                openClawConfig = it.openClawConfig.copy(
                    provider = provider,
                    port = newPort
                )
            )
        }
    }

    fun selectModel(modelName: String) {
        _uiState.update {
            it.copy(
                openClawConfig = it.openClawConfig.copy(modelName = modelName)
            )
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true) }
            val config = _uiState.value.openClawConfig
            val response = openClawClient.testConnection(config)

            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    openClawConfig = it.openClawConfig.copy(
                        isConnected = response.isSuccess,
                        lastPingMs = response.latencyMs,
                        lastCheckTime = System.currentTimeMillis(),
                        statusText = response.message,
                        detectedModels = if (response.models.isNotEmpty()) response.models else it.openClawConfig.detectedModels,
                        modelName = if (it.openClawConfig.modelName == "local-model" && response.models.isNotEmpty()) response.models.first() else it.openClawConfig.modelName
                    )
                )
            }

            postSystemMessage("📡 Verbindungstest zu ${config.provider.displayName} (${config.baseUrl}): ${response.message}")
        }
    }

    fun createCalendarEvent(
        title: String,
        description: String,
        startTime: Long,
        endTime: Long,
        location: String
    ) {
        viewModelScope.launch {
            val created = calendarRepo.insertEvent(title, description, startTime, endTime, location)
            refreshCalendarEvents()
            val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
            postSystemMessage("✅ Neuer Google Kalender Eintrag: '$title' am ${fmt.format(Date(startTime))}.")

            if (_uiState.value.openClawConfig.isConnected && _uiState.value.openClawConfig.provider == AiProvider.OPEN_CLAW) {
                openClawClient.syncCalendarEvents(_uiState.value.openClawConfig, listOf(created))
            }
        }
    }

    fun deleteCalendarEvent(id: Long) {
        viewModelScope.launch {
            val success = calendarRepo.deleteEvent(id)
            if (success) {
                refreshCalendarEvents()
                postSystemMessage("Termin erfolgreich aus dem Kalender gelöscht.")
            }
        }
    }

    fun sendUserMessage(text: String) {
        val prompt = text.trim()
        if (prompt.isBlank()) return

        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = prompt
        )

        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMsg,
                isChatLoading = true
            )
        }

        viewModelScope.launch {
            val config = _uiState.value.openClawConfig
            val upcoming = _uiState.value.calendarEvents
            val diag = _uiState.value.deviceDiagnostics
            val googleAcc = _uiState.value.googleAccount

            // 1. Try Remote KI (LM Studio / OpenClaw)
            val remoteResult = openClawClient.sendChatMessage(
                config = config,
                userPrompt = prompt,
                upcomingEvents = upcoming,
                deviceInfo = diag,
                googleAccount = googleAcc
            )

            if (remoteResult.isSuccess) {
                val aiRes = remoteResult.getOrThrow()
                var finalActionType = aiRes.actionType
                var finalEventTitle = aiRes.createdEventTitle
                var finalEventStart = aiRes.createdEventStart
                var finalEventEnd = aiRes.createdEventEnd
                var finalEventLoc = aiRes.createdEventLocation
                var finalCleanText = aiRes.cleanText

                // Dual-Safety Net: If KI responded in natural language without tags,
                // check if the user asked for an appointment or if the AI confirmed one!
                if (finalActionType == ActionType.NONE) {
                    val fromPrompt = CalendarActionParser.extractEventFromText(prompt)
                    val fromReply = CalendarActionParser.extractEventFromText(finalCleanText)
                    val extracted = fromPrompt ?: fromReply
                    if (extracted != null && extracted.title.length >= 2 && extracted.title != "Geplanter Termin") {
                        finalActionType = ActionType.CALENDAR_CREATED
                        finalEventTitle = extracted.title
                        finalEventStart = extracted.startTimeMillis
                        finalEventEnd = extracted.endTimeMillis
                        finalEventLoc = extracted.location
                    }
                }

                // If KI instructed or intent recognized to create event:
                if (finalActionType == ActionType.CALENDAR_CREATED && !finalEventTitle.isNullOrBlank()) {
                    val created = calendarRepo.insertEvent(
                        title = finalEventTitle,
                        description = "Erstellt via ${config.provider.displayName}",
                        startTimeMillis = finalEventStart,
                        endTimeMillis = finalEventEnd,
                        location = finalEventLoc
                    )
                    refreshCalendarEvents()

                    val dateFmt = SimpleDateFormat("EEEE, dd.MM.yyyy, HH:mm", Locale.GERMANY)
                    val timeEndFmt = SimpleDateFormat("HH:mm", Locale.GERMANY)
                    val formattedTime = "${dateFmt.format(Date(created.startTimeMillis))} - ${timeEndFmt.format(Date(created.endTimeMillis))} Uhr"

                    val botMsg = ChatMessage(
                        sender = if (config.provider == AiProvider.LM_STUDIO) MessageSender.LM_STUDIO else MessageSender.OPEN_CLAW,
                        text = finalCleanText.ifBlank { "Ich habe den Termin '${created.title}' im Google Kalender eingetragen." },
                        actionType = ActionType.CALENDAR_CREATED,
                        actionPayload = created.title,
                        createdEventId = created.id,
                        createdEventTimeFormatted = formattedTime,
                        createdEventLocation = created.location
                    )
                    _uiState.update { it.copy(chatMessages = it.chatMessages + botMsg, isChatLoading = false) }
                    return@launch
                }

                // If KI instructed to list events:
                if (finalActionType == ActionType.CALENDAR_LIST) {
                    val events = calendarRepo.getUpcomingEvents()
                    val fmt = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY)
                    val sb = StringBuilder(finalCleanText.ifBlank { "Hier sind deine aktuellen Termine:" })
                    sb.append("\n\n")
                    events.take(6).forEach { ev ->
                        sb.append("📅 ${fmt.format(Date(ev.startTimeMillis))} - ${ev.title}\n")
                    }
                    val botMsg = ChatMessage(
                        sender = if (config.provider == AiProvider.LM_STUDIO) MessageSender.LM_STUDIO else MessageSender.OPEN_CLAW,
                        text = sb.toString().trim(),
                        actionType = ActionType.CALENDAR_LIST
                    )
                    _uiState.update { it.copy(chatMessages = it.chatMessages + botMsg, isChatLoading = false) }
                    return@launch
                }

                // If KI requested device diagnostics:
                if (finalActionType == ActionType.DEVICE_DIAGNOSTICS) {
                    refreshDiagnostics()
                    val d = _uiState.value.deviceDiagnostics
                    val info = "\n\n📱 Status: ${d.deviceModel}, Akku: ${d.batteryPct}%, IP: ${d.localIp}, Freier RAM: ${d.availableRamGb} GB"
                    val botMsg = ChatMessage(
                        sender = if (config.provider == AiProvider.LM_STUDIO) MessageSender.LM_STUDIO else MessageSender.OPEN_CLAW,
                        text = finalCleanText + info,
                        actionType = ActionType.DEVICE_DIAGNOSTICS
                    )
                    _uiState.update { it.copy(chatMessages = it.chatMessages + botMsg, isChatLoading = false) }
                    return@launch
                }

                // Standard message response from LM Studio
                val botMsg = ChatMessage(
                    sender = if (config.provider == AiProvider.LM_STUDIO) MessageSender.LM_STUDIO else MessageSender.OPEN_CLAW,
                    text = finalCleanText
                )
                _uiState.update { it.copy(chatMessages = it.chatMessages + botMsg, isChatLoading = false) }
                return@launch
            }

            // 2. Fallback: Local parsing engine
            handleLocalFallback(prompt, remoteResult.exceptionOrNull()?.message)
        }
    }

    private suspend fun handleLocalFallback(prompt: String, errorReason: String?) {
        val parsedDraft = calendarRepo.parseCalendarCommand(prompt)
        if (parsedDraft != null) {
            val created = calendarRepo.insertEvent(
                title = parsedDraft.title,
                description = "Erstellt via lokaler Sprachverarbeitung",
                startTimeMillis = parsedDraft.startTimeMillis,
                endTimeMillis = parsedDraft.endTimeMillis,
                location = ""
            )
            refreshCalendarEvents()

            val dateFmt = SimpleDateFormat("EEEE, dd.MM.yyyy, HH:mm", Locale.GERMANY)
            val timeEndFmt = SimpleDateFormat("HH:mm", Locale.GERMANY)
            val formattedTime = "${dateFmt.format(Date(created.startTimeMillis))} - ${timeEndFmt.format(Date(created.endTimeMillis))} Uhr"
            val note = if (errorReason != null) "\n\n(Hinweis: PC-KI war nicht erreichbar, daher lokal eingetragen)" else ""

            val botMsg = ChatMessage(
                sender = MessageSender.LOCAL_AI,
                text = "Termin wurde im Google Kalender eingetragen:$note",
                actionType = ActionType.CALENDAR_CREATED,
                actionPayload = created.title,
                createdEventId = created.id,
                createdEventTimeFormatted = formattedTime,
                createdEventLocation = created.location
            )
            _uiState.update { it.copy(chatMessages = it.chatMessages + botMsg, isChatLoading = false) }
            return
        }

        val lower = prompt.lowercase()
        if (lower.contains("termine") || lower.contains("kalender") || lower.contains("plan")) {
            val events = calendarRepo.getUpcomingEvents()
            val fmt = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY)
            val sb = StringBuilder("Google Kalender Termine:\n\n")
            if (events.isEmpty()) {
                sb.append("Keine anstehenden Termine gefunden.")
            } else {
                events.take(6).forEach { ev ->
                    sb.append("• ${fmt.format(Date(ev.startTimeMillis))} - ${ev.title}\n")
                }
            }
            val botMsg = ChatMessage(
                sender = MessageSender.LOCAL_AI,
                text = sb.toString().trim(),
                actionType = ActionType.CALENDAR_LIST
            )
            _uiState.update { it.copy(chatMessages = it.chatMessages + botMsg, isChatLoading = false) }
            return
        }

        if (lower.contains("handy") || lower.contains("akku") || lower.contains("status")) {
            refreshDiagnostics()
            val diag = _uiState.value.deviceDiagnostics
            val replyText = "📱 Smartphone Status:\n" +
                    "• Akku: ${diag.batteryPct}%\n" +
                    "• Lokale IP: ${diag.localIp}\n" +
                    "• Speicher frei: ${diag.freeStorageGb} GB\n" +
                    "• Freier RAM: ${diag.availableRamGb} GB"
            val botMsg = ChatMessage(
                sender = MessageSender.LOCAL_AI,
                text = replyText,
                actionType = ActionType.DEVICE_DIAGNOSTICS
            )
            _uiState.update { it.copy(chatMessages = it.chatMessages + botMsg, isChatLoading = false) }
            return
        }

        val config = _uiState.value.openClawConfig
        val fallbackText = "Ich konnte die Anfrage nicht an ${config.provider.displayName} weiterleiten (${errorReason ?: "Offline"}).\n\n" +
                "Tipp für LM Studio:\n" +
                "1. Starte LM Studio auf deinem PC.\n" +
                "2. Lade ein Modell (z. B. Llama 3, Mistral, Qwen).\n" +
                "3. Klicke im Bereich 'Developer / Local Server' auf 'Start Server' (Port 1234).\n" +
                "4. Prüfe in den Einstellungen der App die PC-IP (${config.hostIp}).\n\n" +
                "Du kannst mir aber auch jetzt direkt sagen: 'Termin Meeting morgen um 15 Uhr' und ich trage ihn sofort lokal in deinen Google Kalender ein!"

        val botMsg = ChatMessage(
            sender = MessageSender.LOCAL_AI,
            text = fallbackText
        )
        _uiState.update { it.copy(chatMessages = it.chatMessages + botMsg, isChatLoading = false) }
    }

    fun syncCalendarWithOpenClaw() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingCalendar = true) }
            val config = _uiState.value.openClawConfig
            val events = _uiState.value.calendarEvents
            val result = openClawClient.syncCalendarEvents(config, events)

            _uiState.update { it.copy(isSyncingCalendar = false) }

            if (result.isSuccess) {
                postSystemMessage("🔄 Kalender synchronisiert: ${result.getOrThrow()}")
            } else {
                postSystemMessage("⚠️ PC-Synchronisation: ${result.exceptionOrNull()?.message ?: "PC nicht erreichbar."}")
            }
        }
    }

    fun sendDeviceReportToOpenClaw() {
        viewModelScope.launch {
            refreshDiagnostics()
            val diag = _uiState.value.deviceDiagnostics
            postSystemMessage("Handy-Telemetrie erfasst: Akku ${diag.batteryPct}%, IP ${diag.localIp}, Freier Speicher ${diag.freeStorageGb} GB.")
        }
    }

    fun updateGoogleEmail(newEmail: String) {
        _uiState.update {
            it.copy(
                googleAccount = it.googleAccount.copy(email = newEmail.trim())
            )
        }
        postSystemMessage("Google-Konto geändert zu: ${newEmail.trim()}")
    }

    private fun postSystemMessage(text: String) {
        val sysMsg = ChatMessage(
            sender = MessageSender.SYSTEM,
            text = text
        )
        _uiState.update { it.copy(chatMessages = it.chatMessages + sysMsg) }
    }
}
