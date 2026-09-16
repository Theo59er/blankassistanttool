package com.example.data.util

import com.example.data.model.ActionType
import com.example.data.network.AiResponseResult
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CalendarActionParser {

    /**
     * Parses an AI response text (and optionally the user prompt as safety fallback)
     * to detect whether a calendar event should be created or an action was triggered.
     */
    fun parseAiResponse(rawText: String, userPrompt: String = ""): AiResponseResult {
        // 1. Try Key-Value Block: [KALENDER] ... [/KALENDER] (or [CALENDAR] / <<<KALENDER>>>)
        parseKeyValueBlock(rawText)?.let { return it }

        // 2. Try JSON Block: ```json { ... } ``` or bare { ... }
        parseJsonBlock(rawText)?.let { return it }

        // 3. Try Legacy Tag: [ACTION:CREATE_EVENT]{...}[/ACTION]
        parseLegacyTag(rawText)?.let { return it }

        // 4. Web Search tag: [SUCHE: <Query>] or [SEARCH: <Query>]
        val searchRegex = Regex("""\[(?:SUCHE|SEARCH|WEBSEARCH|RECHERCHE):\s*([^\]]+)\]""", RegexOption.IGNORE_CASE)
        val searchMatch = searchRegex.find(rawText)
        if (searchMatch != null) {
            val query = searchMatch.groupValues[1].trim()
            val clean = rawText.replace(searchMatch.value, "").trim()
            return AiResponseResult(
                cleanText = clean.ifBlank { "Ich starte die Web-Suche nach: \"$query\"" },
                actionType = ActionType.WEB_SEARCH,
                webSearchQuery = query
            )
        }

        // 5. Memory tag: [ERINNERUNG: <Text>] or [MEMORY: <Text>]
        val memoryRegex = Regex("""\[(?:ERINNERUNG|MEMORY|MERKEN):\s*([^\]]+)\]""", RegexOption.IGNORE_CASE)
        val memoryMatch = memoryRegex.find(rawText)
        if (memoryMatch != null) {
            val memText = memoryMatch.groupValues[1].trim()
            val clean = rawText.replace(memoryMatch.value, "").trim()
            return AiResponseResult(
                cleanText = clean.ifBlank { "Ich habe mir notiert: \"$memText\"" },
                memoryToStore = memText
            )
        }

        // 6. Status actions
        if (rawText.contains("[ACTION:LIST_EVENTS]") || rawText.contains("[KALENDER_LISTE]")) {
            val clean = rawText
                .replace("[ACTION:LIST_EVENTS][/ACTION]", "")
                .replace("[ACTION:LIST_EVENTS]", "")
                .replace("[KALENDER_LISTE][/KALENDER_LISTE]", "")
                .replace("[KALENDER_LISTE]", "")
                .trim()
            return AiResponseResult(cleanText = clean, actionType = ActionType.CALENDAR_LIST)
        }

        if (rawText.contains("[ACTION:DEVICE_STATUS]") || rawText.contains("[STATUS]")) {
            val clean = rawText
                .replace("[ACTION:DEVICE_STATUS][/ACTION]", "")
                .replace("[ACTION:DEVICE_STATUS]", "")
                .replace("[STATUS][/STATUS]", "")
                .replace("[STATUS]", "")
                .trim()
            return AiResponseResult(cleanText = clean, actionType = ActionType.DEVICE_DIAGNOSTICS)
        }

        // 7. Safety Net / Hybrid Fallback:
        // If the AI responded conversationally without tags, but the user prompt or AI response clearly
        // specified an appointment creation:
        parseNaturalLanguageFallback(rawText, userPrompt)?.let { return it }

        return AiResponseResult(cleanText = rawText.trim())
    }

    /**
     * Handles key-value blocks like:
     * [KALENDER]
     * TITEL: Zahnarzt Kontrolltermin
     * DATUM: 2026-09-17
     * ZEIT: 14:30
     * DAUER: 45
     * ORT: Praxis Dr. Schmidt
     * [/KALENDER]
     */
    private fun parseKeyValueBlock(text: String): AiResponseResult? {
        val blockRegexes: List<Regex> = listOf(
            Regex(
                """\[(?:KALENDER|CALENDAR|EVENT|TERMIN|KALENDER_AKTION)\](.*?)\[/(?:KALENDER|CALENDAR|EVENT|TERMIN|KALENDER_AKTION)\]""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            ),
            Regex(
                """<<<(?:KALENDER|CALENDAR|EVENT|TERMIN)>>>(.*?)<<<[/](?:KALENDER|CALENDAR|EVENT|TERMIN)>>>""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            )
        )

        for (regex in blockRegexes) {
            val match = regex.find(text) ?: continue
            val blockContent = match.groupValues[1]

            val lines = blockContent.lines().map { it.trim() }.filter { it.isNotBlank() }
            val map = mutableMapOf<String, String>()

            for (line in lines) {
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase(Locale.GERMANY)
                    val value = line.substring(colonIdx + 1).trim()
                    map[key] = value
                }
            }

            val title = map["titel"] ?: map["title"] ?: map["termin"] ?: map["name"] ?: "Geplanter Termin"
            val dateStr = map["datum"] ?: map["date"] ?: map["tag"] ?: "heute"
            val timeStr = map["zeit"] ?: map["time"] ?: map["uhrzeit"] ?: map["start"] ?: "10:00"
            val durationStr = map["dauer"] ?: map["duration"] ?: "60"
            val location = map["ort"] ?: map["location"] ?: map["wo"] ?: ""

            val durationMinutes = parseDurationMinutes(durationStr)
            val startTime = resolveDateTime(dateStr, timeStr)
            val endTime = startTime + (durationMinutes * 60_000L)

            val cleanText = text.replace(match.value, "").trim().ifBlank {
                "Ich habe den Termin '$title' im Google Kalender eingetragen."
            }

            return AiResponseResult(
                cleanText = cleanText,
                actionType = ActionType.CALENDAR_CREATED,
                createdEventTitle = title,
                createdEventStart = startTime,
                createdEventEnd = endTime,
                createdEventLocation = location
            )
        }
        return null
    }

    /**
     * Parses JSON inside markdown code blocks or plain JSON objects.
     */
    private fun parseJsonBlock(text: String): AiResponseResult? {
        // Pattern 1: ```json { ... } ```
        val codeBlockRegex = Regex("""```(?:json)?\s*(\{.*?\})\s*```""", RegexOption.DOT_MATCHES_ALL)
        val codeMatch = codeBlockRegex.find(text)
        val jsonCandidate = codeMatch?.groupValues?.get(1)

        val jsonStr = jsonCandidate ?: run {
            // Pattern 2: bare JSON with "titel" or "title"
            if (text.contains("\"title\"", ignoreCase = true) || text.contains("\"titel\"", ignoreCase = true)) {
                val firstBrace = text.indexOf('{')
                val lastBrace = text.lastIndexOf('}')
                if (firstBrace in 0 until lastBrace) {
                    text.substring(firstBrace, lastBrace + 1)
                } else null
            } else null
        } ?: return null

        // Extract with resilient regex (avoids org.json android stub crashes in unit tests & handles LLM formatting issues)
        val titleRegex = Regex("""\"(?:title|titel|termin|name)\"\s*:\s*\"([^\"]+)\"""", RegexOption.IGNORE_CASE)
        val dateRegex = Regex("""\"(?:date|datum|tag)\"\s*:\s*\"([^\"]+)\"""", RegexOption.IGNORE_CASE)
        val timeRegex = Regex("""\"(?:time|zeit|uhrzeit|start)\"\s*:\s*\"([^\"]+)\"""", RegexOption.IGNORE_CASE)
        val durationRegex = Regex("""\"(?:duration|dauer)\"\s*:\s*\"?(\d+)\"?""", RegexOption.IGNORE_CASE)
        val locationRegex = Regex("""\"(?:location|ort|wo)\"\s*:\s*\"([^\"]*)\"""", RegexOption.IGNORE_CASE)

        val title = titleRegex.find(jsonStr)?.groupValues?.get(1) ?: return null
        val dateStr = dateRegex.find(jsonStr)?.groupValues?.get(1) ?: "heute"
        val timeStr = timeRegex.find(jsonStr)?.groupValues?.get(1) ?: "10:00"
        val durationStr = durationRegex.find(jsonStr)?.groupValues?.get(1) ?: "60"
        val location = locationRegex.find(jsonStr)?.groupValues?.get(1) ?: ""

        val durationMinutes = parseDurationMinutes(durationStr)
        val startTime = resolveDateTime(dateStr, timeStr)
        val endTime = startTime + (durationMinutes * 60_000L)

        val fullMatchToRemove = codeMatch?.value ?: jsonStr
        val cleanText = text.replace(fullMatchToRemove, "").trim().ifBlank {
            "Ich habe den Termin '$title' im Google Kalender eingetragen."
        }

        return AiResponseResult(
            cleanText = cleanText,
            actionType = ActionType.CALENDAR_CREATED,
            createdEventTitle = title,
            createdEventStart = startTime,
            createdEventEnd = endTime,
            createdEventLocation = location
        )
    }

    /**
     * Handles legacy [ACTION:CREATE_EVENT]{"title":...}[/ACTION]
     */
    private fun parseLegacyTag(text: String): AiResponseResult? {
        val actionRegex = Regex("""\[ACTION:CREATE_EVENT\]\s*(\{.*?\})\s*\[/ACTION\]""", RegexOption.DOT_MATCHES_ALL)
        val match = actionRegex.find(text) ?: return null
        val jsonStr = match.groupValues[1]

        val titleRegex = Regex("""\"(?:title|titel)\"\s*:\s*\"([^\"]+)\"""", RegexOption.IGNORE_CASE)
        val startRegex = Regex("""\"start\"\s*:\s*\"([^\"]+)\"""", RegexOption.IGNORE_CASE)
        val endRegex = Regex("""\"end\"\s*:\s*\"([^\"]+)\"""", RegexOption.IGNORE_CASE)
        val locRegex = Regex("""\"location\"\s*:\s*\"([^\"]*)\"""", RegexOption.IGNORE_CASE)

        val title = titleRegex.find(jsonStr)?.groupValues?.get(1) ?: "Neuer Termin"
        val startRaw = startRegex.find(jsonStr)?.groupValues?.get(1) ?: ""
        val endRaw = endRegex.find(jsonStr)?.groupValues?.get(1) ?: ""
        val loc = locRegex.find(jsonStr)?.groupValues?.get(1) ?: ""

        val startMillis = parseTimeToMillis(startRaw) ?: (System.currentTimeMillis() + 3600_000L)
        val endMillis = parseTimeToMillis(endRaw) ?: (startMillis + 3600_000L)

        val cleanText = text.replace(match.value, "").trim().ifBlank {
            "Ich habe den Termin '$title' im Google Kalender eingetragen."
        }

        return AiResponseResult(
            cleanText = cleanText,
            actionType = ActionType.CALENDAR_CREATED,
            createdEventTitle = title,
            createdEventStart = startMillis,
            createdEventEnd = endMillis,
            createdEventLocation = loc
        )
    }

    /**
     * Fallback that inspects the user prompt or conversational AI reply.
     * E.g. "Termin: Zahnarzt morgen 14 Uhr" or "Ich habe für morgen 14:00 Uhr Zahnarzt eingetragen"
     */
    private fun parseNaturalLanguageFallback(aiText: String, userPrompt: String): AiResponseResult? {
        val triggerWords = listOf("termin", "eintragen", "kalender", "meeting", "erinnerung", "planen")
        val lowerPrompt = userPrompt.lowercase(Locale.GERMANY)
        val lowerAi = aiText.lowercase(Locale.GERMANY)

        val hasTrigger = triggerWords.any { lowerPrompt.contains(it) } ||
                lowerAi.contains("habe den termin") ||
                lowerAi.contains("termin eingetragen") ||
                lowerAi.contains("termin geplant")

        if (!hasTrigger) return null

        val sourceText = if (triggerWords.any { lowerPrompt.contains(it) }) userPrompt else aiText
        val parsed = extractEventFromText(sourceText) ?: return null

        return AiResponseResult(
            cleanText = aiText.trim(),
            actionType = ActionType.CALENDAR_CREATED,
            createdEventTitle = parsed.title,
            createdEventStart = parsed.startTimeMillis,
            createdEventEnd = parsed.endTimeMillis,
            createdEventLocation = parsed.location
        )
    }

    data class ExtractedEvent(
        val title: String,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val location: String = ""
    )

    fun extractEventFromText(text: String): ExtractedEvent? {
        val lower = text.lowercase(Locale.GERMANY)

        // Find hour & minute
        val hourRegex = Regex("""(\b[0-2]?[0-9])(?::([0-5][0-9]))?\s*(?:uhr|pm|am|\b)""")
        val hourMatch = hourRegex.findAll(lower).firstOrNull { m ->
            val v = m.groupValues[1].toIntOrNull() ?: -1
            v in 0..23 && (m.value.contains("uhr") || m.value.contains(":") || v in 7..22)
        }

        var hour = 10
        var minute = 0
        if (hourMatch != null) {
            hour = (hourMatch.groupValues[1].toIntOrNull() ?: 10).coerceIn(0, 23)
            minute = (hourMatch.groupValues[2].toIntOrNull() ?: 0).coerceIn(0, 59)
        }

        val cal = Calendar.getInstance()
        when {
            lower.contains("übermorgen") -> cal.add(Calendar.DAY_OF_YEAR, 2)
            lower.contains("morgen") -> cal.add(Calendar.DAY_OF_YEAR, 1)
            lower.contains("montag") -> setNextDayOfWeek(cal, Calendar.MONDAY)
            lower.contains("dienstag") -> setNextDayOfWeek(cal, Calendar.TUESDAY)
            lower.contains("mittwoch") -> setNextDayOfWeek(cal, Calendar.WEDNESDAY)
            lower.contains("donnerstag") -> setNextDayOfWeek(cal, Calendar.THURSDAY)
            lower.contains("freitag") -> setNextDayOfWeek(cal, Calendar.FRIDAY)
            lower.contains("samstag") -> setNextDayOfWeek(cal, Calendar.SATURDAY)
            lower.contains("sonntag") -> setNextDayOfWeek(cal, Calendar.SUNDAY)
        }

        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Clean title
        var title = text
            .replace(Regex("""(?i)\b(erstell(e)?|trag(e)?|mach(e)?|neuer termin|termin|eintragen|kalender|bitte|für|am|um)\b"""), " ")
            .replace(Regex("""(?i)\b(morgen|übermorgen|montag|dienstag|mittwoch|donnerstag|freitag|samstag|sonntag|uhr)\b"""), " ")
            .replace(Regex("""\d{1,2}(:\d{2})?"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (title.isBlank() || title.length < 2) {
            title = "Geplanter Termin"
        } else {
            title = title.replaceFirstChar { it.uppercase() }
        }

        val start = cal.timeInMillis
        val end = start + 3600_000L

        return ExtractedEvent(
            title = title,
            startTimeMillis = start,
            endTimeMillis = end
        )
    }

    fun resolveDateTime(dateStr: String, timeStr: String): Long {
        val cal = Calendar.getInstance()
        val lowerDate = dateStr.lowercase(Locale.GERMANY).trim()

        // 1. Resolve date
        when {
            lowerDate.contains("übermorgen") -> cal.add(Calendar.DAY_OF_YEAR, 2)
            lowerDate.contains("morgen") -> cal.add(Calendar.DAY_OF_YEAR, 1)
            lowerDate.contains("heute") -> { /* keep today */ }
            lowerDate.contains("montag") -> setNextDayOfWeek(cal, Calendar.MONDAY)
            lowerDate.contains("dienstag") -> setNextDayOfWeek(cal, Calendar.TUESDAY)
            lowerDate.contains("mittwoch") -> setNextDayOfWeek(cal, Calendar.WEDNESDAY)
            lowerDate.contains("donnerstag") -> setNextDayOfWeek(cal, Calendar.THURSDAY)
            lowerDate.contains("freitag") -> setNextDayOfWeek(cal, Calendar.FRIDAY)
            lowerDate.contains("samstag") -> setNextDayOfWeek(cal, Calendar.SATURDAY)
            lowerDate.contains("sonntag") -> setNextDayOfWeek(cal, Calendar.SUNDAY)
            else -> {
                // Try parse YYYY-MM-DD or DD.MM.YYYY
                val dateFormats = listOf("yyyy-MM-dd", "dd.MM.yyyy", "dd.MM.", "yyyy/MM/dd")
                var parsedDate: Date? = null
                for (fmt in dateFormats) {
                    try {
                        val sdf = SimpleDateFormat(fmt, Locale.GERMANY)
                        val d = sdf.parse(dateStr.trim())
                        if (d != null) {
                            parsedDate = d
                            break
                        }
                    } catch (_: Exception) {}
                }
                if (parsedDate != null) {
                    val parsedCal = Calendar.getInstance().apply { time = parsedDate }
                    cal.set(Calendar.YEAR, parsedCal.get(Calendar.YEAR))
                    cal.set(Calendar.MONTH, parsedCal.get(Calendar.MONTH))
                    cal.set(Calendar.DAY_OF_MONTH, parsedCal.get(Calendar.DAY_OF_MONTH))
                }
            }
        }

        // 2. Resolve time
        var hour = 10
        var minute = 0
        val cleanTime = timeStr.lowercase(Locale.GERMANY).replace("uhr", "").trim()
        val timeRegex = Regex("""(\d{1,2})[:.](\d{2})""")
        val m = timeRegex.find(cleanTime)
        if (m != null) {
            hour = (m.groupValues[1].toIntOrNull() ?: 10).coerceIn(0, 23)
            minute = (m.groupValues[2].toIntOrNull() ?: 0).coerceIn(0, 59)
        } else {
            val simpleHour = cleanTime.filter { it.isDigit() }.toIntOrNull()
            if (simpleHour != null) {
                hour = simpleHour.coerceIn(0, 23)
            }
        }

        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal.timeInMillis
    }

    fun parseDurationMinutes(durationStr: String): Long {
        val lower = durationStr.lowercase(Locale.GERMANY).trim()
        if (lower.contains("stunde") || lower.contains("h")) {
            val num = Regex("""\d+""").find(lower)?.value?.toLongOrNull() ?: 1L
            return num * 60L
        }
        val num = Regex("""\d+""").find(lower)?.value?.toLongOrNull()
        return num?.coerceAtLeast(5L) ?: 60L
    }

    private fun setNextDayOfWeek(cal: Calendar, targetDayOfWeek: Int) {
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = targetDayOfWeek - currentDay
        if (daysToAdd <= 0) {
            daysToAdd += 7
        }
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
    }

    private fun parseTimeToMillis(timeStr: String): Long? {
        if (timeStr.isBlank()) return null
        timeStr.toLongOrNull()?.let { return it }

        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm"
        )
        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.GERMANY)
                val date = sdf.parse(timeStr)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return null
    }
}
