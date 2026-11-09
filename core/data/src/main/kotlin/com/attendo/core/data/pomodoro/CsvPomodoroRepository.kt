package com.attendo.core.data.pomodoro

import com.attendo.core.data.AttendoData
import com.attendo.core.data.PomodoroRepository
import com.attendo.core.data.csv.CsvStore
import com.attendo.core.data.csv.CsvTable
import com.attendo.core.model.PomodoroKind
import com.attendo.core.model.PomodoroSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import java.io.File
import java.time.LocalDate

/** `pomodoro.csv`: id,date,started_at,kind,subject_id,planned_seconds,actual_seconds,completed */
internal class CsvPomodoroRepository(
    dir: File,
    scope: CoroutineScope,
    io: CoroutineDispatcher,
) : PomodoroRepository {

    private val store = CsvStore(
        table = CsvTable(
            File(dir, AttendoData.POMODORO_FILE),
            listOf("id", "date", "started_at", "kind", "subject_id", "planned_seconds", "actual_seconds", "completed"),
        ),
        scope = scope,
        io = io,
        decode = { r ->
            runCatching {
                PomodoroSession(
                    id = r.getValue("id"),
                    date = LocalDate.parse(r.getValue("date")),
                    startedAt = r["started_at"]?.toLongOrNull() ?: 0L,
                    kind = PomodoroKind.valueOf(r.getValue("kind")),
                    subjectId = r["subject_id"]?.takeIf { it.isNotBlank() },
                    plannedSeconds = r["planned_seconds"]?.toIntOrNull() ?: 0,
                    actualSeconds = r["actual_seconds"]?.toIntOrNull() ?: 0,
                    completed = r["completed"] == "true",
                )
            }.getOrNull()
        },
        encode = { s ->
            mapOf(
                "id" to s.id,
                "date" to s.date.toString(),
                "started_at" to s.startedAt.toString(),
                "kind" to s.kind.name,
                "subject_id" to s.subjectId.orEmpty(),
                "planned_seconds" to s.plannedSeconds.toString(),
                "actual_seconds" to s.actualSeconds.toString(),
                "completed" to s.completed.toString(),
            )
        },
    )

    override val sessions: Flow<List<PomodoroSession>> = store.items.filterNotNull()

    override suspend fun add(session: PomodoroSession) = store.update { it + session }
}
