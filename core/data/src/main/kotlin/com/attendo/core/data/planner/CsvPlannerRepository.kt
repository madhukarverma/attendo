package com.attendo.core.data.planner

import com.attendo.core.data.AttendoData
import com.attendo.core.data.PlannerRepository
import com.attendo.core.data.csv.CsvStore
import com.attendo.core.data.csv.CsvTable
import com.attendo.core.model.AcademicCalendar
import com.attendo.core.model.DayKind
import com.attendo.core.model.PersonalEvent
import com.attendo.core.model.TimetableSlot
import com.attendo.core.model.withPersonal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * `timetable.csv`: id,subject_id,day,start,end,room (day = MONDAY..SUNDAY, times HH:mm)
 * `events.csv`: id,title,start,end,kind,note,created_at (kind = HOLIDAY, EXAM, DEADLINE or EVENT)
 */
internal class CsvPlannerRepository(
    dir: File,
    scope: CoroutineScope,
    io: CoroutineDispatcher,
    private val clock: () -> Long,
) : PlannerRepository {

    internal val slotStore = CsvStore(
        table = CsvTable(File(dir, AttendoData.TIMETABLE_FILE), listOf("id", "subject_id", "day", "start", "end", "room")),
        scope = scope,
        io = io,
        decode = { r ->
            runCatching {
                val start = LocalTime.parse(r.getValue("start"))
                val end = LocalTime.parse(r.getValue("end"))
                TimetableSlot(
                    id = r.getValue("id").ifEmpty { return@runCatching null },
                    subjectId = r.getValue("subject_id").ifEmpty { return@runCatching null },
                    day = DayOfWeek.valueOf(r.getValue("day").trim().uppercase()),
                    start = start,
                    end = if (end > start) end else start.plusHours(1),
                    room = r["room"].orEmpty(),
                )
            }.getOrNull()
        },
        encode = { s ->
            mapOf(
                "id" to s.id,
                "subject_id" to s.subjectId,
                "day" to s.day.name,
                "start" to s.start.withSecond(0).withNano(0).toString(),
                "end" to s.end.withSecond(0).withNano(0).toString(),
                "room" to s.room,
            )
        },
    )

    internal val eventStore = CsvStore(
        table = CsvTable(File(dir, AttendoData.EVENTS_FILE), listOf("id", "title", "start", "end", "kind", "note", "created_at")),
        scope = scope,
        io = io,
        decode = { r ->
            runCatching {
                val start = LocalDate.parse(r.getValue("start"))
                val end = r["end"]?.takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: start
                PersonalEvent(
                    id = r.getValue("id").ifEmpty { return@runCatching null },
                    title = r["title"].orEmpty(),
                    start = start,
                    end = maxOf(start, end),
                    kind = r["kind"]?.let { k -> runCatching { DayKind.valueOf(k) }.getOrNull() }
                        ?.takeIf { it in USER_KINDS } ?: DayKind.EVENT,
                    note = r["note"].orEmpty(),
                    createdAt = r["created_at"]?.toLongOrNull() ?: 0L,
                )
            }.getOrNull()
        },
        encode = { e ->
            mapOf(
                "id" to e.id,
                "title" to e.title,
                "start" to e.start.toString(),
                "end" to e.end.toString(),
                "kind" to e.kind.name,
                "note" to e.note,
                "created_at" to e.createdAt.toString(),
            )
        },
    )

    override val slots: Flow<List<TimetableSlot>> = slotStore.items.filterNotNull()
    override val events: Flow<List<PersonalEvent>> = eventStore.items.filterNotNull()

    override fun calendar(base: AcademicCalendar): Flow<AcademicCalendar> = events.map(base::withPersonal)

    override suspend fun saveSlot(slot: TimetableSlot): TimetableSlot {
        val saved = slot.copy(id = slot.id.ifEmpty { UUID.randomUUID().toString() }, room = slot.room.trim())
        slotStore.update { list -> if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it } else list + saved }
        return saved
    }

    override suspend fun deleteSlot(id: String) = slotStore.update { list -> list.filterNot { it.id == id } }

    override suspend fun deleteSlotsForSubject(subjectId: String) =
        slotStore.update { list -> list.filterNot { it.subjectId == subjectId } }

    override suspend fun saveEvent(event: PersonalEvent): PersonalEvent {
        val saved = event.copy(
            id = event.id.ifEmpty { UUID.randomUUID().toString() },
            title = event.title.trim(),
            note = event.note.trim(),
            end = maxOf(event.start, event.end),
            kind = event.kind.takeIf { it in USER_KINDS } ?: DayKind.EVENT,
            createdAt = event.createdAt.takeIf { it > 0 } ?: clock(),
        )
        eventStore.update { list -> if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it } else list + saved }
        return saved
    }

    override suspend fun deleteEvent(id: String) = eventStore.update { list -> list.filterNot { it.id == id } }

    private companion object {
        /** Kinds a student can add (term start/end come only from the official calendar). */
        val USER_KINDS = setOf(DayKind.EXAM, DayKind.DEADLINE, DayKind.EVENT, DayKind.HOLIDAY)
    }
}
