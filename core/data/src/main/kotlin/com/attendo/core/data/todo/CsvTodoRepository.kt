package com.attendo.core.data.todo

import com.attendo.core.data.AttendoData
import com.attendo.core.data.TodoRepository
import com.attendo.core.data.csv.CsvStore
import com.attendo.core.data.csv.CsvTable
import com.attendo.core.model.Todo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** `todos.csv`: id,title,date,remind_at,done,created_at (remind_at is HH:mm or empty). */
internal class CsvTodoRepository(
    dir: File,
    scope: CoroutineScope,
    io: CoroutineDispatcher,
    private val clock: () -> Long,
) : TodoRepository {

    internal val store = CsvStore(
        table = CsvTable(
            File(dir, AttendoData.TODOS_FILE),
            listOf("id", "title", "date", "remind_at", "done", "created_at"),
        ),
        scope = scope,
        io = io,
        decode = { r ->
            runCatching {
                Todo(
                    id = r.getValue("id").ifEmpty { return@runCatching null },
                    title = r["title"].orEmpty(),
                    date = LocalDate.parse(r.getValue("date")),
                    remindAt = r["remind_at"]?.takeIf { it.isNotBlank() }?.let(LocalTime::parse),
                    done = r["done"] == "true",
                    createdAt = r["created_at"]?.toLongOrNull() ?: 0L,
                )
            }.getOrNull()
        },
        encode = { t ->
            mapOf(
                "id" to t.id,
                "title" to t.title,
                "date" to t.date.toString(),
                "remind_at" to (t.remindAt?.withSecond(0)?.withNano(0)?.toString() ?: ""),
                "done" to t.done.toString(),
                "created_at" to t.createdAt.toString(),
            )
        },
    )

    override val todos: Flow<List<Todo>> = store.items.filterNotNull()

    override suspend fun add(title: String, date: LocalDate, remindAt: LocalTime?): Todo {
        val todo = Todo(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            date = date,
            remindAt = remindAt?.withSecond(0)?.withNano(0),
            createdAt = clock(),
        )
        store.update { it + todo }
        return todo
    }

    override suspend fun update(todo: Todo) =
        store.update { list -> list.map { if (it.id == todo.id) todo else it } }

    override suspend fun setDone(id: String, done: Boolean) =
        store.update { list -> list.map { if (it.id == id) it.copy(done = done) else it } }

    override suspend fun delete(id: String) = store.update { list -> list.filterNot { it.id == id } }

    override suspend fun clearDone() = store.update { list -> list.filterNot { it.done } }
}
