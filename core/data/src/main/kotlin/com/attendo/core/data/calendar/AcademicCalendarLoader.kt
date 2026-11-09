package com.attendo.core.data.calendar

import com.attendo.core.data.csv.Csv
import com.attendo.core.model.AcademicCalendar
import com.attendo.core.model.CalendarEvent
import com.attendo.core.model.DayKind
import java.time.LocalDate

/**
 * Loads the bundled `calendar/academic_calendar.csv` (date,end_date,kind,title). Swap that file to
 * support a new semester; no code changes needed.
 */
object AcademicCalendarLoader {

    private const val RESOURCE = "calendar/academic_calendar.csv"

    fun bundled(): AcademicCalendar {
        val text = AcademicCalendarLoader::class.java.classLoader
            ?.getResourceAsStream(RESOURCE)
            ?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: return AcademicCalendar.EMPTY
        return parse(text)
    }

    fun parse(csv: String): AcademicCalendar {
        val rows = Csv.parse(csv)
        if (rows.isEmpty()) return AcademicCalendar.EMPTY
        val header = rows.first()
        val events = rows.drop(1).mapNotNull { row ->
            val r = header.indices.associate { header[it] to row.getOrElse(it) { "" } }
            runCatching {
                val start = LocalDate.parse(r.getValue("date"))
                CalendarEvent(
                    start = start,
                    end = r["end_date"]?.takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: start,
                    kind = DayKind.valueOf(r.getValue("kind").trim()),
                    title = r.getValue("title").trim(),
                )
            }.getOrNull()
        }
        return AcademicCalendar(events.sortedBy { it.start })
    }
}
