package com.attendo.core.data.habit

import com.attendo.core.data.AttendoData
import com.attendo.core.data.HabitRepository
import com.attendo.core.data.csv.CsvStore
import com.attendo.core.data.csv.CsvTable
import com.attendo.core.model.Habit
import com.attendo.core.model.HabitCheck
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

/**
 * `habits.csv`: id,title,days,position,created_at (days like "MON|WED|FRI")
 * `habit_log.csv`: habit_id,date (one row per tick)
 */
internal class CsvHabitRepository(
    dir: File,
    scope: CoroutineScope,
    io: CoroutineDispatcher,
    private val clock: () -> Long,
) : HabitRepository {

    internal val habitStore = CsvStore(
        table = CsvTable(File(dir, AttendoData.HABITS_FILE), listOf("id", "title", "days", "position", "created_at")),
        scope = scope,
        io = io,
        decode = { r ->
            val id = r["id"].orEmpty()
            if (id.isEmpty()) {
                null
            } else {
                Habit(
                    id = id,
                    title = r["title"].orEmpty(),
                    days = decodeDays(r["days"].orEmpty()),
                    position = r["position"]?.toIntOrNull() ?: 0,
                    createdAt = r["created_at"]?.toLongOrNull() ?: 0L,
                )
            }
        },
        encode = { h ->
            mapOf(
                "id" to h.id,
                "title" to h.title,
                "days" to encodeDays(h.days),
                "position" to h.position.toString(),
                "created_at" to h.createdAt.toString(),
            )
        },
    )

    internal val logStore = CsvStore(
        table = CsvTable(File(dir, AttendoData.HABIT_LOG_FILE), listOf("habit_id", "date")),
        scope = scope,
        io = io,
        decode = { r ->
            val id = r["habit_id"].orEmpty()
            val date = r["date"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (id.isEmpty() || date == null) null else HabitCheck(id, date)
        },
        encode = { c -> mapOf("habit_id" to c.habitId, "date" to c.date.toString()) },
    )

    override val habits: Flow<List<Habit>> = habitStore.items.filterNotNull()
    override val checks: Flow<List<HabitCheck>> = logStore.items.filterNotNull()

    override suspend fun add(title: String, days: Set<DayOfWeek>): Habit {
        var created: Habit? = null
        habitStore.update { list ->
            val habit = Habit(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                days = days,
                position = (list.maxOfOrNull { it.position } ?: -1) + 1,
                createdAt = clock(),
            )
            created = habit
            list + habit
        }
        return created!!
    }

    override suspend fun update(habit: Habit) =
        habitStore.update { list -> list.map { if (it.id == habit.id) habit.copy(title = habit.title.trim()) else it } }

    override suspend fun delete(habitId: String) {
        logStore.update { list -> list.filterNot { it.habitId == habitId } }
        habitStore.update { list -> list.filterNot { it.id == habitId } }
    }

    override suspend fun setChecked(habitId: String, date: LocalDate, checked: Boolean) =
        logStore.update { list ->
            val others = list.filterNot { it.habitId == habitId && it.date == date }
            if (checked) others + HabitCheck(habitId, date) else others
        }

    private companion object {
        /** Stable three-letter codes, independent of the phone's language. */
        val CODES = mapOf(
            DayOfWeek.MONDAY to "MON", DayOfWeek.TUESDAY to "TUE", DayOfWeek.WEDNESDAY to "WED",
            DayOfWeek.THURSDAY to "THU", DayOfWeek.FRIDAY to "FRI", DayOfWeek.SATURDAY to "SAT",
            DayOfWeek.SUNDAY to "SUN",
        )
        val BY_CODE = CODES.entries.associate { (day, code) -> code to day }

        fun encodeDays(days: Set<DayOfWeek>): String = DayOfWeek.entries.filter { it in days }.joinToString("|") { CODES.getValue(it) }

        fun decodeDays(text: String): Set<DayOfWeek> =
            text.split('|', ',', ' ').mapNotNullTo(LinkedHashSet()) { BY_CODE[it.trim().uppercase()] }
    }
}
