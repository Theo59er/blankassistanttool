package com.example

import com.example.data.model.ActionType
import com.example.data.util.CalendarActionParser
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testParseKeyValueCalendarBlock() {
        val aiText = """
            Alles klar! Ich habe den Termin für dich eingetragen.
            [KALENDER]
            TITEL: Zahnarzt Kontrolltermin
            DATUM: morgen
            ZEIT: 14:30
            DAUER: 45
            ORT: Praxis Dr. Schmidt
            [/KALENDER]
        """.trimIndent()

        val result = CalendarActionParser.parseAiResponse(aiText)
        assertEquals(ActionType.CALENDAR_CREATED, result.actionType)
        assertEquals("Zahnarzt Kontrolltermin", result.createdEventTitle)
        assertEquals("Praxis Dr. Schmidt", result.createdEventLocation)
        assertTrue(result.createdEventStart > System.currentTimeMillis())
        assertEquals(45 * 60_000L, result.createdEventEnd - result.createdEventStart)
        assertEquals("Alles klar! Ich habe den Termin für dich eingetragen.", result.cleanText)
    }

    @Test
    fun testParseJsonCalendarBlock() {
        val aiText = """
            Gerne, hier ist dein Termin:
            ```json
            {
              "title": "Team Meeting",
              "date": "2026-09-20",
              "time": "10:00",
              "duration": "60",
              "location": "Konferenzraum 2"
            }
            ```
        """.trimIndent()

        val result = CalendarActionParser.parseAiResponse(aiText)
        assertEquals(ActionType.CALENDAR_CREATED, result.actionType)
        assertEquals("Team Meeting", result.createdEventTitle)
        assertEquals("Konferenzraum 2", result.createdEventLocation)
        assertEquals(60 * 60_000L, result.createdEventEnd - result.createdEventStart)
    }

    @Test
    fun testParseNaturalLanguageFallback() {
        val userPrompt = "Neuer Termin: Zahnarzt morgen 14 Uhr"
        val aiText = "Ich trage das sofort für dich ein!"

        val result = CalendarActionParser.parseAiResponse(aiText, userPrompt)
        assertEquals(ActionType.CALENDAR_CREATED, result.actionType)
        assertTrue(result.createdEventTitle?.contains("Zahnarzt", ignoreCase = true) == true)
        assertTrue(result.createdEventStart > 0L)
    }
}
