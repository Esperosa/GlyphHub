package com.pelikan.glyphhub.schoolonline

import com.pelikan.glyphhub.toys.SchoolClassTimerLogic
import com.pelikan.glyphhub.toys.SchoolTimerState
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SkolaOnlineTimetableParserTest {
    @Test
    fun parsesRegularAndSubstitutionLessons() {
        val snapshot = SkolaOnlineTimetableParser.parse(sampleTimetable)

        assertEquals(3, snapshot.lessons.size)
        assertEquals(2, snapshot.activeSchoolLessons().size)
        assertEquals(1, snapshot.daySummary(java.time.LocalDate.of(2026, 6, 2)).regularCount)
        assertEquals(1, snapshot.daySummary(java.time.LocalDate.of(2026, 6, 2)).substitutionCount)
        assertEquals(listOf("Tv"), snapshot.daySummary(java.time.LocalDate.of(2026, 6, 2)).substitutionLabels)
    }

    @Test
    fun substitutionStateCarriesVisualFlag() {
        val snapshot = SkolaOnlineTimetableParser.parse(sampleTimetable)
        val state = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 2, 10, 50),
            snapshot.activeSchoolLessons()
        )

        assertIs<SchoolTimerState.Class>(state)
        assertEquals("Tv", state.label)
        assertEquals(true, state.isSubstitution)
    }

    private val sampleTimetable = """
        {
          "days": [
            {
              "date": "2026-06-02T00:00:00",
              "schedules": [
                {
                  "scheduledHourId": "D1",
                  "beginTime": "2026-06-02T09:50:00",
                  "endTime": "2026-06-02T10:35:00",
                  "hourType": {"id": "ROZVRH", "description": "Rozvrh"},
                  "hourKind": {"id": "", "description": ""},
                  "subject": {"abbrev": "Ikt", "name": "Informatika"}
                },
                {
                  "scheduledHourId": "D2",
                  "beginTime": "2026-06-02T10:45:00",
                  "endTime": "2026-06-02T11:30:00",
                  "hourType": {"id": "SUPLOVANI", "description": "Suplování"},
                  "hourKind": {"id": "SUPLOVANI", "description": "Suplování předmětu"},
                  "subject": {"abbrev": "Tv", "name": "Tělesná výchova"}
                },
                {
                  "scheduledHourId": "D3",
                  "beginTime": "2026-06-02T11:40:00",
                  "endTime": "2026-06-02T12:25:00",
                  "hourType": {"id": "SUPLOVANA", "description": "Suplovaná"},
                  "hourKind": {"id": "", "description": ""},
                  "subject": {"abbrev": "F", "name": "Fyzika"}
                }
              ]
            }
          ]
        }
    """.trimIndent()
}
