package com.example.data.network

import com.example.data.model.ActionType
import com.example.data.model.AiProvider
import com.example.data.model.CalendarEventItem
import com.example.data.model.DeviceDiagnostics
import com.example.data.model.GoogleAccountInfo
import com.example.data.model.OpenClawConfig
import com.example.data.util.CalendarActionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PingResponse(
    val isSuccess: Boolean,
    val latencyMs: Long,
    val message: String,
    val models: List<String> = emptyList()
)

data class AiResponseResult(
    val cleanText: String,
    val actionType: ActionType = ActionType.NONE,
    val createdEventTitle: String? = null,
    val createdEventStart: Long = 0L,
    val createdEventEnd: Long = 0L,
    val createdEventLocation: String = "",
    val webSearchQuery: String? = null,
    val memoryToStore: String? = null
)

class OpenClawClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(config: OpenClawConfig): PingResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        if (config.provider == AiProvider.LM_STUDIO) {
            // LM Studio provides OpenAI-compatible /v1/models
            val url = "${config.baseUrl}/v1/models"
            val request = Request.Builder()
                .url(url)
                .get()
                .apply {
                    if (config.apiToken.isNotBlank()) {
                        addHeader("Authorization", "Bearer ${config.apiToken.trim()}")
                    }
                }
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val modelsList = mutableListOf<String>()
                        try {
                            val json = JSONObject(body)
                            val dataArr = json.optJSONArray("data")
                            if (dataArr != null) {
                                for (i in 0 until dataArr.length()) {
                                    val item = dataArr.getJSONObject(i)
                                    val id = item.optString("id")
                                    if (id.isNotBlank()) modelsList.add(id)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        val modelDesc = if (modelsList.isNotEmpty()) {
                            "Geladene Modelle: ${modelsList.joinToString(", ")}"
                        } else {
                            "LM Studio Server aktiv (kein Modell geladen)"
                        }

                        PingResponse(
                            isSuccess = true,
                            latencyMs = latency,
                            message = "LM Studio online (${latency}ms). $modelDesc",
                            models = modelsList
                        )
                    } else {
                        PingResponse(
                            isSuccess = false,
                            latencyMs = latency,
                            message = "LM Studio meldet HTTP ${response.code}. Prüfe den Server-Status in LM Studio."
                        )
                    }
                }
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                PingResponse(
                    isSuccess = false,
                    latencyMs = latency,
                    message = "LM Studio nicht erreichbar unter ${config.baseUrl} (${e.localizedMessage ?: "Timeout"}). Bitte 'Start Server' in LM Studio prüfen."
                )
            }
        } else {
            // OpenClaw / Generic HTTP endpoint
            val url = "${config.baseUrl}/health"
            val request = Request.Builder().url(url).get().build()
            try {
                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    PingResponse(
                        isSuccess = response.isSuccessful,
                        latencyMs = latency,
                        message = if (response.isSuccessful) "OpenClaw online (${latency}ms)" else "OpenClaw HTTP ${response.code}"
                    )
                }
            } catch (e: Exception) {
                // Fallback root
                try {
                    val rootReq = Request.Builder().url(config.baseUrl).get().build()
                    client.newCall(rootReq).execute().use { response ->
                        val latency = System.currentTimeMillis() - startTime
                        PingResponse(
                            isSuccess = true,
                            latencyMs = latency,
                            message = "OpenClaw Root online (${latency}ms)"
                        )
                    }
                } catch (ex2: Exception) {
                    val latency = System.currentTimeMillis() - startTime
                    PingResponse(
                        isSuccess = false,
                        latencyMs = latency,
                        message = "OpenClaw PC nicht erreichbar (${ex2.localizedMessage ?: "Timeout"})."
                    )
                }
            }
        }
    }

    suspend fun sendChatMessage(
        config: OpenClawConfig,
        userPrompt: String,
        upcomingEvents: List<CalendarEventItem>,
        deviceInfo: DeviceDiagnostics,
        googleAccount: GoogleAccountInfo
    ): Result<AiResponseResult> = withContext(Dispatchers.IO) {
        if (config.provider == AiProvider.LM_STUDIO) {
            sendLmStudioCompletions(config, userPrompt, upcomingEvents, deviceInfo, googleAccount)
        } else {
            sendOpenClawMessage(config, userPrompt, upcomingEvents, deviceInfo)
        }
    }

    private fun sendLmStudioCompletions(
        config: OpenClawConfig,
        userPrompt: String,
        upcomingEvents: List<CalendarEventItem>,
        deviceInfo: DeviceDiagnostics,
        googleAccount: GoogleAccountInfo
    ): Result<AiResponseResult> {
        val url = "${config.baseUrl}/v1/chat/completions"
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        val nowFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.GERMANY)
        val nowReadable = SimpleDateFormat("EEEE, dd. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMANY).format(Date())

        val eventsContext = StringBuilder()
        if (upcomingEvents.isEmpty()) {
            eventsContext.append("Keine anstehenden Termine eingetragen.\n")
        } else {
            upcomingEvents.take(8).forEach { ev ->
                val startStr = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY).format(Date(ev.startTimeMillis))
                eventsContext.append("- ${ev.title} ($startStr, Ort: ${ev.location.ifBlank { "keiner" }})\n")
            }
        }

        val enrichedSystemPrompt = """
${config.effectiveSystemPrompt}

KONTEXT-DATEN:
- Aktuelle Systemzeit: $nowReadable (ISO: ${nowFmt.format(Date())})
- Google Konto: ${googleAccount.email}
- Smartphone: ${deviceInfo.deviceModel}, Android ${deviceInfo.androidVersion}, Akku: ${deviceInfo.batteryPct}%
- Anstehende Google Kalender Termine:
$eventsContext
        """.trimIndent()

        val messagesArr = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", enrichedSystemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }

        val modelName = config.modelName.ifBlank { "local-model" }
        val payload = JSONObject().apply {
            put("model", modelName)
            put("messages", messagesArr)
            put("temperature", 0.7)
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .apply {
                if (config.apiToken.isNotBlank()) {
                    addHeader("Authorization", "Bearer ${config.apiToken.trim()}")
                }
            }
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(rawBody)
                    val choices = json.optJSONArray("choices")
                    val firstChoice = choices?.optJSONObject(0)
                    val messageObj = firstChoice?.optJSONObject("message")
                    val replyContent = messageObj?.optString("content") ?: rawBody

                    val parsed = CalendarActionParser.parseAiResponse(replyContent, userPrompt)
                    Result.success(parsed)
                } else {
                    Result.failure(Exception("LM Studio HTTP Fehler: ${response.code} ($rawBody)"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sendOpenClawMessage(
        config: OpenClawConfig,
        userPrompt: String,
        upcomingEvents: List<CalendarEventItem>,
        deviceInfo: DeviceDiagnostics
    ): Result<AiResponseResult> {
        val url = "${config.baseUrl}/api/chat"
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        val jsonBody = JSONObject().apply {
            put("message", userPrompt)
            put("device", JSONObject().apply {
                put("model", deviceInfo.deviceModel)
                put("android", deviceInfo.androidVersion)
                put("battery", deviceInfo.batteryPct)
                put("ip", deviceInfo.localIp)
            })
            val eventsArray = JSONArray()
            upcomingEvents.take(5).forEach { ev ->
                eventsArray.put(JSONObject().apply {
                    put("title", ev.title)
                    put("start", ev.startTimeMillis)
                    put("end", ev.endTimeMillis)
                    put("location", ev.location)
                })
            }
            put("calendar_events", eventsArray)
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val reply = json.optString("reply")
                        .ifBlank { json.optString("response") }
                        .ifBlank { json.optString("message") }
                        .ifBlank { bodyStr }
                    val parsed = CalendarActionParser.parseAiResponse(reply, userPrompt)
                    Result.success(parsed)
                } else {
                    Result.failure(Exception("OpenClaw HTTP Fehler: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseAiActions(rawText: String): AiResponseResult {
        return CalendarActionParser.parseAiResponse(rawText)
    }

    suspend fun syncCalendarEvents(
        config: OpenClawConfig,
        events: List<CalendarEventItem>
    ): Result<String> = withContext(Dispatchers.IO) {
        if (config.provider == AiProvider.LM_STUDIO) {
            // LM Studio provides an OpenAI-compatible API and does not have an /api/calendar/sync endpoint.
            // Current events are already fed into LM Studio prompts automatically as context.
            return@withContext Result.success("${events.size} Termine sind direkt im KI-Kontext verfügbar.")
        }

        val url = "${config.baseUrl}/api/calendar/sync"
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        val jsonBody = JSONObject().apply {
            val eventsArray = JSONArray()
            events.forEach { ev ->
                eventsArray.put(JSONObject().apply {
                    put("id", ev.id)
                    put("title", ev.title)
                    put("start", ev.startTimeMillis)
                    put("end", ev.endTimeMillis)
                    put("location", ev.location)
                    put("description", ev.description)
                })
            }
            put("events", eventsArray)
            put("sync_timestamp", System.currentTimeMillis())
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("${events.size} Termine erfolgreich an den PC übertragen.")
                } else {
                    Result.failure(Exception("PC meldet HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
