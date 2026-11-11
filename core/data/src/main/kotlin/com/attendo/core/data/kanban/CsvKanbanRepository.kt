package com.attendo.core.data.kanban

import com.attendo.core.data.AttendoData
import com.attendo.core.data.KanbanRepository
import com.attendo.core.data.csv.CsvStore
import com.attendo.core.data.csv.CsvTable
import com.attendo.core.model.CardPriority
import com.attendo.core.model.KanbanBoard
import com.attendo.core.model.KanbanCard
import com.attendo.core.model.KanbanColumn
import com.attendo.core.model.KanbanOps
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import java.io.File
import java.time.LocalDate
import java.util.UUID

/**
 * `kanban_columns.csv`: id,title,position,done
 * `kanban_cards.csv`: id,column_id,title,notes,due,priority,position,created_at
 */
internal class CsvKanbanRepository(
    dir: File,
    scope: CoroutineScope,
    io: CoroutineDispatcher,
    private val clock: () -> Long,
) : KanbanRepository {

    internal val columnStore = CsvStore(
        table = CsvTable(File(dir, AttendoData.KANBAN_COLUMNS_FILE), listOf("id", "title", "position", "done")),
        scope = scope,
        io = io,
        decode = { r ->
            val id = r["id"].orEmpty()
            if (id.isEmpty()) {
                null
            } else {
                KanbanColumn(id, r["title"].orEmpty(), r["position"]?.toIntOrNull() ?: 0, r["done"] == "true")
            }
        },
        encode = { c ->
            mapOf("id" to c.id, "title" to c.title, "position" to c.position.toString(), "done" to c.done.toString())
        },
    )

    internal val cardStore = CsvStore(
        table = CsvTable(
            File(dir, AttendoData.KANBAN_CARDS_FILE),
            listOf("id", "column_id", "title", "notes", "due", "priority", "position", "created_at"),
        ),
        scope = scope,
        io = io,
        decode = { r ->
            val id = r["id"].orEmpty()
            val columnId = r["column_id"].orEmpty()
            if (id.isEmpty() || columnId.isEmpty()) {
                null
            } else {
                KanbanCard(
                    id = id,
                    columnId = columnId,
                    title = r["title"].orEmpty(),
                    notes = r["notes"].orEmpty(),
                    due = r["due"]?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                    priority = r["priority"]?.let { runCatching { CardPriority.valueOf(it) }.getOrNull() } ?: CardPriority.NONE,
                    position = r["position"]?.toIntOrNull() ?: 0,
                    createdAt = r["created_at"]?.toLongOrNull() ?: 0L,
                )
            }
        },
        encode = { c ->
            mapOf(
                "id" to c.id,
                "column_id" to c.columnId,
                "title" to c.title,
                "notes" to c.notes,
                "due" to (c.due?.toString() ?: ""),
                "priority" to c.priority.name,
                "position" to c.position.toString(),
                "created_at" to c.createdAt.toString(),
            )
        },
    )

    override val board: Flow<KanbanBoard> =
        combine(columnStore.items.filterNotNull(), cardStore.items.filterNotNull()) { columns, cards ->
            // Cards whose column no longer exists (e.g. a partial CSV restore) are left out, not lost.
            val known = columns.mapTo(HashSet()) { it.id }
            KanbanBoard(columns, cards.filter { it.columnId in known })
        }

    override suspend fun ensureDefaultColumns() {
        columnStore.update { columns ->
            if (columns.isNotEmpty()) {
                columns
            } else {
                KanbanBoard.DEFAULT_COLUMNS.mapIndexed { i, title ->
                    KanbanColumn(newId(), title, i, done = i == KanbanBoard.DEFAULT_COLUMNS.lastIndex)
                }
            }
        }
    }

    override suspend fun addColumn(title: String): KanbanColumn {
        var created: KanbanColumn? = null
        columnStore.update { columns ->
            val column = KanbanColumn(newId(), title.trim(), (columns.maxOfOrNull { it.position } ?: -1) + 1)
            created = column
            columns + column
        }
        return created!!
    }

    override suspend fun renameColumn(columnId: String, title: String) =
        columnStore.update { list -> list.map { if (it.id == columnId) it.copy(title = title.trim()) else it } }

    override suspend fun setColumnDone(columnId: String, done: Boolean) =
        columnStore.update { list -> list.map { if (it.id == columnId) it.copy(done = done) else it } }

    override suspend fun moveColumn(columnId: String, toIndex: Int) =
        columnStore.update { KanbanOps.moveColumn(it, columnId, toIndex) }

    override suspend fun deleteColumn(columnId: String) {
        cardStore.update { cards -> cards.filterNot { it.columnId == columnId } }
        columnStore.update { columns ->
            columns.filterNot { it.id == columnId }.sortedBy { it.position }.mapIndexed { i, c -> c.copy(position = i) }
        }
    }

    override suspend fun addCard(columnId: String, title: String, notes: String, due: LocalDate?, priority: CardPriority): KanbanCard {
        var created: KanbanCard? = null
        cardStore.update { cards ->
            val card = KanbanCard(
                id = newId(),
                columnId = columnId,
                title = title.trim(),
                notes = notes.trim(),
                due = due,
                priority = priority,
                position = KanbanOps.nextPosition(cards, columnId),
                createdAt = clock(),
            )
            created = card
            cards + card
        }
        return created!!
    }

    override suspend fun updateCard(card: KanbanCard) =
        cardStore.update { list ->
            // Column and position only change through moveCard, so ordering stays consistent.
            list.map { if (it.id == card.id) card.copy(columnId = it.columnId, position = it.position) else it }
        }

    override suspend fun moveCard(cardId: String, toColumnId: String, toIndex: Int) =
        cardStore.update { KanbanOps.moveCard(it, cardId, toColumnId, toIndex) }

    override suspend fun deleteCard(cardId: String) = cardStore.update { list -> list.filterNot { it.id == cardId } }

    private fun newId() = UUID.randomUUID().toString()
}
