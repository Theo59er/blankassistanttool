package com.example.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.data.model.CalendarEventItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

class CalendarRepository(private val context: Context) {

    private val sampleEvents = mutableListOf(
        CalendarEventItem(
            id = 101L,
            title = "OpenClaw Sync & Setup",
            description = "Lokale Netzwerkverbindung mit OpenClaw auf PC prüfen",
            startTimeMillis = System.currentTimeMillis() + 3600_000L * 2,
            endTimeMillis = System.currentTimeMillis() + 3600_000L * 3,
            location = "Home Office",
            calendarName = "Google Kalender"
        ),
        CalendarEventItem(
            id = 102L,
            title = "Projektbesprechung & Backlog",
            description = "Statusupdate mit dem Entwicklerteam",
            startTimeMillis = System.currentTimeMillis() + 86400_000L + 3600_000L * 10,
            endTimeMillis = System.currentTimeMillis() + 86400_000L + 3600_000L * 11,
            location = "Google Meet",
            calendarName = "Google Kalender"
        ),
        CalendarEventItem(
            id = 103L,
            title = "Zahnarzt Kontrolltermin",
            description = "Halbjährlicher Check",
            startTimeMillis = System.currentTimeMillis() + 86400_000L * 3 + 3600_000L * 14,
            endTimeMillis = System.currentTimeMillis() + 86400_000L * 3 + 3600_000L * 15,
            location = "Praxis Dr. Schmidt",
            calendarName = "Google Kalender"
        )
    )

    fun hasCalendarPermission(): Boolean {
        val read = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return read && write
    }

    suspend fun getUpcomingEvents(): List<CalendarEventItem> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext sampleEvents.sortedBy { it.startTimeMillis }
        }

        val events = mutableListOf<CalendarEventItem>()
        try {
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.CALENDAR_DISPLAY_NAME,
                CalendarContract.Events.ALL_DAY
            )

            val now = System.currentTimeMillis()
            val selection = "${CalendarContract.Events.DTSTART} >= ?"
            val selectionArgs = arrayOf((now - 3600_000L * 24).toString())
            val sortOrder = "${CalendarContract.Events.DTSTART} ASC LIMIT 50"

            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = c.getColumnIndex(CalendarContract.Events.TITLE)
                val descIdx = c.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val startIdx = c.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIdx = c.getColumnIndex(CalendarContract.Events.DTEND)
                val locIdx = c.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val calNameIdx = c.getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME)
                val allDayIdx = c.getColumnIndex(CalendarContract.Events.ALL_DAY)

                while (c.moveToNext()) {
                    val id = if (idIdx >= 0) c.getLong(idIdx) else 0L
                    val title = if (titleIdx >= 0) c.getString(titleIdx) ?: "Termin" else "Termin"
                    val desc = if (descIdx >= 0) c.getString(descIdx) ?: "" else ""
                    val start = if (startIdx >= 0) c.getLong(startIdx) else now
                    val end = if (endIdx >= 0) c.getLong(endIdx) else start + 3600_000L
                    val loc = if (locIdx >= 0) c.getString(locIdx) ?: "" else ""
                    val calName = if (calNameIdx >= 0) c.getString(calNameIdx) ?: "Google Kalender" else "Google Kalender"
                    val allDay = if (allDayIdx >= 0) c.getInt(allDayIdx) == 1 else false

                    events.add(
                        CalendarEventItem(
                            id = id,
                            title = title,
                            description = desc,
                            startTimeMillis = start,
                            endTimeMillis = end,
                            location = loc,
                            calendarName = calName,
                            isAllDay = allDay
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (events.isEmpty()) {
            return@withContext sampleEvents.sortedBy { it.startTimeMillis }
        }
        events.sortedBy { it.startTimeMillis }
    }

    suspend fun insertEvent(
        title: String,
        description: String = "",
        startTimeMillis: Long,
        endTimeMillis: Long,
        location: String = ""
    ): CalendarEventItem = withContext(Dispatchers.IO) {
        val duration = if (endTimeMillis > startTimeMillis) endTimeMillis else startTimeMillis + 3600_000L

        if (!hasCalendarPermission()) {
            val newEvent = CalendarEventItem(
                id = System.currentTimeMillis(),
                title = title,
                description = description,
                startTimeMillis = startTimeMillis,
                endTimeMillis = duration,
                location = location,
                calendarName = "Google Kalender"
            )
            sampleEvents.add(0, newEvent)
            return@withContext newEvent
        }

        var calendarId = 1L
        try {
            val calCursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY),
                null,
                null,
                null
            )
            calCursor?.use { c ->
                if (c.moveToFirst()) {
                    val idIdx = c.getColumnIndex(CalendarContract.Calendars._ID)
                    if (idIdx >= 0) {
                        calendarId = c.getLong(idIdx)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.EVENT_LOCATION, location)
                put(CalendarContract.Events.DTSTART, startTimeMillis)
                put(CalendarContract.Events.DTEND, duration)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val newId = if (uri != null) ContentUris.parseId(uri) else System.currentTimeMillis()

            val created = CalendarEventItem(
                id = newId,
                title = title,
                description = description,
                startTimeMillis = startTimeMillis,
                endTimeMillis = duration,
                location = location,
                calendarName = "Google Kalender"
            )
            sampleEvents.add(0, created)
            return@withContext created
        } catch (e: Exception) {
            e.printStackTrace()
            val fallback = CalendarEventItem(
                id = System.currentTimeMillis(),
                title = title,
                description = description,
                startTimeMillis = startTimeMillis,
                endTimeMillis = duration,
                location = location,
                calendarName = "Google Kalender"
            )
            sampleEvents.add(0, fallback)
            return@withContext fallback
        }
    }

    suspend fun deleteEvent(eventId: Long): Boolean = withContext(Dispatchers.IO) {
        sampleEvents.removeAll { it.id == eventId }
        if (!hasCalendarPermission()) {
            return@withContext true
        }
        try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rows = context.contentResolver.delete(deleteUri, null, null)
            rows > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Parses free-form text to extract a calendar event.
     * Examples:
     * - "Neuer Termin: Zahnarzt morgen um 14 Uhr"
     * - "Trage Meeting heute um 16:30 ein"
     * - "Termin: Gym am Samstag 18 Uhr"
     */
    fun parseCalendarCommand(input: String): ParsedEventDraft? {
        val lower = input.lowercase().trim()
        val triggers = listOf("termin", "eintragen", "kalender", "meeting", "erinnerung", "planen")
        if (!triggers.any { lower.contains(it) }) {
            return null
        }

        // Try extracting time
        val hourRegex = Regex("""(\b[0-2]?[0-9])(?::([0-5][0-9]))?\s*(uhr|pm|am|\b)""")
        val hourMatch = hourRegex.find(lower)

        val cal = Calendar.getInstance()
        if (lower.contains("morgen")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        } else if (lower.contains("übermorgen")) {
            cal.add(Calendar.DAY_OF_YEAR, 2)
        } else if (lower.contains("montag")) {
            setNextDayOfWeek(cal, Calendar.MONDAY)
        } else if (lower.contains("dienstag")) {
            setNextDayOfWeek(cal, Calendar.TUESDAY)
        } else if (lower.contains("mittwoch")) {
            setNextDayOfWeek(cal, Calendar.WEDNESDAY)
        } else if (lower.contains("donnerstag")) {
            setNextDayOfWeek(cal, Calendar.THURSDAY)
        } else if (lower.contains("freitag")) {
            setNextDayOfWeek(cal, Calendar.FRIDAY)
        } else if (lower.contains("samstag")) {
            setNextDayOfWeek(cal, Calendar.SATURDAY)
        } else if (lower.contains("sonntag")) {
            setNextDayOfWeek(cal, Calendar.SUNDAY)
        }

        var hour = 10
        var minute = 0
        if (hourMatch != null) {
            val h = hourMatch.groupValues[1].toIntOrNull() ?: 10
            val m = hourMatch.groupValues[2].toIntOrNull() ?: 0
            hour = h.coerceIn(0, 23)
            minute = m.coerceIn(0, 59)
        }

        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Clean up title
        var title = input
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

        return ParsedEventDraft(
            title = title,
            startTimeMillis = start,
            endTimeMillis = end
        )
    }

    private fun setNextDayOfWeek(cal: Calendar, targetDayOfWeek: Int) {
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = targetDayOfWeek - currentDay
        if (daysToAdd <= 0) {
            daysToAdd += 7
        }
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
    }
}

data class ParsedEventDraft(
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long
)
