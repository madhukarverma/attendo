package com.attendo.core.data.grades

import com.attendo.core.data.AttendoData
import com.attendo.core.data.GradeRepository
import com.attendo.core.data.csv.CsvStore
import com.attendo.core.data.csv.CsvTable
import com.attendo.core.model.Assessment
import com.attendo.core.model.Grades
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** `grades.csv`: id,subject_id,title,scored,max_marks,date,created_at */
internal class CsvGradeRepository(
    dir: File,
    scope: CoroutineScope,
    io: CoroutineDispatcher,
    private val clock: () -> Long,
) : GradeRepository {

    internal val store = CsvStore(
        table = CsvTable(
            File(dir, AttendoData.GRADES_FILE),
            listOf("id", "subject_id", "title", "scored", "max_marks", "date", "created_at"),
        ),
        scope = scope,
        io = io,
        decode = { r ->
            val id = r["id"].orEmpty()
            val subjectId = r["subject_id"].orEmpty()
            val scored = r["scored"]?.toDoubleOrNull()
            val max = r["max_marks"]?.toDoubleOrNull()
            if (id.isEmpty() || subjectId.isEmpty() || scored == null || max == null || !scored.isFinite() || !max.isFinite()) {
                null
            } else {
                Assessment(
                    id = id,
                    subjectId = subjectId,
                    title = r["title"].orEmpty(),
                    scored = scored.coerceIn(0.0, Grades.MAX_MARKS_LIMIT),
                    maxMarks = max.coerceIn(0.0, Grades.MAX_MARKS_LIMIT),
                    date = r["date"]?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                    createdAt = r["created_at"]?.toLongOrNull() ?: 0L,
                )
            }
        },
        encode = { a ->
            mapOf(
                "id" to a.id,
                "subject_id" to a.subjectId,
                "title" to a.title,
                "scored" to Grades.format(a.scored),
                "max_marks" to Grades.format(a.maxMarks),
                "date" to (a.date?.toString() ?: ""),
                "created_at" to a.createdAt.toString(),
            )
        },
    )

    override val assessments: Flow<List<Assessment>> = store.items.filterNotNull()

    override suspend fun add(subjectId: String, title: String, scored: Double, maxMarks: Double, date: LocalDate?): Assessment {
        val assessment = Assessment(
            id = UUID.randomUUID().toString(),
            subjectId = subjectId,
            title = title.trim(),
            scored = scored.coerceIn(0.0, Grades.MAX_MARKS_LIMIT),
            maxMarks = maxMarks.coerceIn(0.0, Grades.MAX_MARKS_LIMIT),
            date = date,
            createdAt = clock(),
        )
        store.update { it + assessment }
        return assessment
    }

    override suspend fun update(assessment: Assessment) =
        store.update { list -> list.map { if (it.id == assessment.id) assessment.copy(title = assessment.title.trim()) else it } }

    override suspend fun delete(id: String) = store.update { list -> list.filterNot { it.id == id } }

    override suspend fun deleteForSubject(subjectId: String) = store.update { list -> list.filterNot { it.subjectId == subjectId } }
}
