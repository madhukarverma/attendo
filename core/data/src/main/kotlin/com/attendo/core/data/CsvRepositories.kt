package com.attendo.core.data

import com.attendo.core.data.csv.CsvStore
import com.attendo.core.data.csv.CsvTable
import com.attendo.core.model.AppSettings
import com.attendo.core.model.AttendanceRecord
import com.attendo.core.model.AttendanceStatus
import com.attendo.core.model.CatalogSubject
import com.attendo.core.model.CourseCatalog
import com.attendo.core.model.Subject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.LocalDate
import java.util.UUID

internal class CsvSettingsRepository(
    dir: File,
    scope: CoroutineScope,
    io: CoroutineDispatcher,
) : SettingsRepository {

    private val store = CsvStore(
        table = CsvTable(File(dir, AttendoData.SETTINGS_FILE), listOf("key", "value")),
        scope = scope,
        io = io,
        decode = { row -> row["key"].orEmpty().takeIf { it.isNotEmpty() }?.let { it to row["value"].orEmpty() } },
        encode = { (k, v) -> mapOf("key" to k, "value" to v) },
    )

    override val settings: Flow<AppSettings?> = store.items.map { pairs ->
        pairs?.toMap()?.let { m ->
            AppSettings(
                userName = m[KEY_NAME].orEmpty(),
                onboarded = m[KEY_ONBOARDED] == "true",
                iosMotion = m[KEY_IOS_MOTION] != "false",
            )
        }
    }

    override suspend fun setIosMotion(enabled: Boolean) = put(KEY_IOS_MOTION, enabled.toString())

    override suspend fun setUserName(name: String) = put(KEY_NAME, name.trim())

    override suspend fun setOnboarded(onboarded: Boolean) = put(KEY_ONBOARDED, onboarded.toString())

    private suspend fun put(key: String, value: String) =
        store.update { list -> list.filterNot { it.first == key } + (key to value) }

    private companion object {
        const val KEY_NAME = "user_name"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_IOS_MOTION = "ios_motion"
    }
}

internal class CsvSubjectRepository(
    dir: File,
    scope: CoroutineScope,
    io: CoroutineDispatcher,
    private val clock: () -> Long,
) : SubjectRepository {

    private val store = CsvStore(
        table = CsvTable(
            File(dir, AttendoData.SUBJECTS_FILE),
            listOf("id", "name", "code", "target_percent", "created_at"),
        ),
        scope = scope,
        io = io,
        decode = { r ->
            val id = r["id"].orEmpty()
            if (id.isEmpty()) {
                null
            } else {
                Subject(
                    id = id,
                    name = r["name"].orEmpty(),
                    code = r["code"].orEmpty(),
                    targetPercent = r["target_percent"]?.toIntOrNull() ?: Subject.DEFAULT_TARGET_PERCENT,
                    createdAt = r["created_at"]?.toLongOrNull() ?: 0L,
                )
            }
        },
        encode = { s ->
            mapOf(
                "id" to s.id,
                "name" to s.name,
                "code" to s.code,
                "target_percent" to s.targetPercent.toString(),
                "created_at" to s.createdAt.toString(),
            )
        },
    )

    override val subjects: Flow<List<Subject>> = store.items.filterNotNull()

    override suspend fun add(name: String, code: String, targetPercent: Int): Subject {
        val subject = Subject(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            code = code.trim(),
            targetPercent = targetPercent.coerceIn(0, 100),
            createdAt = clock(),
        )
        store.update { it + subject }
        return subject
    }

    override suspend fun addFromCatalog(items: List<CatalogSubject>, targetPercent: Int): Int {
        var added = 0
        store.update { list ->
            val toAdd = CourseCatalog.missing(list, items).mapIndexed { i, c ->
                Subject(
                    id = UUID.randomUUID().toString(),
                    name = c.name,
                    code = c.code,
                    targetPercent = targetPercent.coerceIn(0, 100),
                    createdAt = clock() + i, // keeps catalog order stable
                )
            }
            added = toAdd.size
            list + toAdd
        }
        return added
    }

    override suspend fun update(subject: Subject) =
        store.update { list -> list.map { if (it.id == subject.id) subject else it } }

    override suspend fun delete(subjectId: String) =
        store.update { list -> list.filterNot { it.id == subjectId } }
}

internal class CsvAttendanceRepository(
    dir: File,
    scope: CoroutineScope,
    io: CoroutineDispatcher,
    private val clock: () -> Long,
) : AttendanceRepository {

    private val store = CsvStore(
        table = CsvTable(
            File(dir, AttendoData.ATTENDANCE_FILE),
            listOf("subject_id", "date", "status", "updated_at"),
        ),
        scope = scope,
        io = io,
        decode = { r ->
            runCatching {
                AttendanceRecord(
                    subjectId = r.getValue("subject_id"),
                    date = LocalDate.parse(r.getValue("date")),
                    status = AttendanceStatus.valueOf(r.getValue("status")),
                    updatedAt = r["updated_at"]?.toLongOrNull() ?: 0L,
                )
            }.getOrNull()
        },
        encode = { a ->
            mapOf(
                "subject_id" to a.subjectId,
                "date" to a.date.toString(),
                "status" to a.status.name,
                "updated_at" to a.updatedAt.toString(),
            )
        },
    )

    override val records: Flow<List<AttendanceRecord>> = store.items.filterNotNull()

    override suspend fun mark(subjectId: String, date: LocalDate, status: AttendanceStatus?) =
        store.update { list ->
            val others = list.filterNot { it.subjectId == subjectId && it.date == date }
            if (status == null) others else others + AttendanceRecord(subjectId, date, status, clock())
        }

    override suspend fun deleteForSubject(subjectId: String) =
        store.update { list -> list.filterNot { it.subjectId == subjectId } }
}
