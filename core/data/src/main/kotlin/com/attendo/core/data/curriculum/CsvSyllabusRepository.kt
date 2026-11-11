package com.attendo.core.data.curriculum

import com.attendo.core.data.AttendoData
import com.attendo.core.data.SyllabusRepository
import com.attendo.core.data.csv.CsvStore
import com.attendo.core.data.csv.CsvTable
import com.attendo.core.model.UnitKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.io.File

/** `syllabus.csv`: code,unit,covered_at — one row per unit the student has covered. */
internal class CsvSyllabusRepository(
    dir: File,
    scope: CoroutineScope,
    io: CoroutineDispatcher,
    private val clock: () -> Long,
) : SyllabusRepository {

    internal data class Row(val key: UnitKey, val coveredAt: Long)

    internal val store = CsvStore(
        table = CsvTable(File(dir, AttendoData.SYLLABUS_FILE), listOf("code", "unit", "covered_at")),
        scope = scope,
        io = io,
        decode = { r ->
            val code = r["code"].orEmpty().trim().uppercase()
            val unit = r["unit"].orEmpty().trim().uppercase()
            if (code.isEmpty() || unit.isEmpty()) null else Row(UnitKey(code, unit), r["covered_at"]?.toLongOrNull() ?: 0L)
        },
        encode = { row -> mapOf("code" to row.key.code, "unit" to row.key.unit, "covered_at" to row.coveredAt.toString()) },
    )

    override val covered: Flow<Set<UnitKey>> = store.items.filterNotNull().map { rows -> rows.mapTo(HashSet()) { it.key } }

    override suspend fun setCovered(code: String, unit: String, covered: Boolean) {
        val key = UnitKey(code.trim().uppercase(), unit.trim().uppercase())
        store.update { rows ->
            val others = rows.filterNot { it.key == key }
            if (covered) others + Row(key, clock()) else others
        }
    }
}
