package com.attendo.core.model

import java.time.LocalDate

/** A column ("list") of the Kanban board. */
data class KanbanColumn(
    val id: String,
    val title: String,
    val position: Int,
    /** Cards here are finished: they're never shown as overdue and don't count as open work. */
    val done: Boolean = false,
)

enum class CardPriority { NONE, LOW, MEDIUM, HIGH }

/** A task card on the board. */
data class KanbanCard(
    val id: String,
    val columnId: String,
    val title: String,
    val notes: String = "",
    val due: LocalDate? = null,
    val priority: CardPriority = CardPriority.NONE,
    /** Order within its column, 0 at the top. */
    val position: Int = 0,
    val createdAt: Long = 0L,
)

/** The whole board: columns left to right, each with its cards top to bottom. */
data class KanbanBoard(val columns: List<KanbanColumn>, val cards: List<KanbanCard>) {

    private val byColumn: Map<String, List<KanbanCard>> =
        cards.sortedWith(compareBy({ it.position }, { it.createdAt })).groupBy { it.columnId }

    val sortedColumns: List<KanbanColumn> = columns.sortedBy { it.position }

    fun cardsIn(columnId: String): List<KanbanCard> = byColumn[columnId].orEmpty()

    private val doneColumns: Set<String> = columns.filter { it.done }.mapTo(HashSet()) { it.id }

    /** Cards that still need work (not in a "done" column). */
    val openCards: List<KanbanCard> get() = cards.filterNot { it.columnId in doneColumns }

    fun isOverdue(card: KanbanCard, today: LocalDate): Boolean =
        card.due != null && card.due < today && card.columnId !in doneColumns

    /** Open cards with a due date, soonest first (overdue ones lead). */
    fun dueSoon(): List<KanbanCard> = openCards.filter { it.due != null }.sortedBy { it.due }

    /** The column a card moves to with the "next" button, or null when it's in the last one. */
    fun nextColumn(columnId: String): KanbanColumn? {
        val i = sortedColumns.indexOfFirst { it.id == columnId }
        return if (i < 0) null else sortedColumns.getOrNull(i + 1)
    }

    companion object {
        val EMPTY = KanbanBoard(emptyList(), emptyList())

        /** Titles of the columns a new board starts with; the last one is the "done" column. */
        val DEFAULT_COLUMNS = listOf("To do", "Doing", "Done")
    }
}

/** Pure list operations behind the board, so ordering rules are testable without storage. */
object KanbanOps {

    /**
     * Moves [cardId] to [toColumnId] at [toIndex] (index among that column's other cards, clamped)
     * and renumbers positions in the columns it left and entered. Unknown cards are a no-op.
     */
    fun moveCard(cards: List<KanbanCard>, cardId: String, toColumnId: String, toIndex: Int): List<KanbanCard> {
        val card = cards.firstOrNull { it.id == cardId } ?: return cards
        val fromColumnId = card.columnId
        fun ordered(columnId: String) = cards.filter { it.columnId == columnId && it.id != cardId }
            .sortedWith(compareBy({ it.position }, { it.createdAt }))

        val target = ordered(toColumnId).toMutableList()
        target.add(toIndex.coerceIn(0, target.size), card.copy(columnId = toColumnId))
        val renumbered = HashMap<String, KanbanCard>()
        target.forEachIndexed { i, c -> renumbered[c.id] = c.copy(position = i) }
        if (fromColumnId != toColumnId) {
            ordered(fromColumnId).forEachIndexed { i, c -> renumbered[c.id] = c.copy(position = i) }
        }
        return cards.map { renumbered[it.id] ?: it }
    }

    /** Moves a column to [toIndex] and renumbers every column. */
    fun moveColumn(columns: List<KanbanColumn>, columnId: String, toIndex: Int): List<KanbanColumn> {
        val ordered = columns.sortedBy { it.position }.toMutableList()
        val from = ordered.indexOfFirst { it.id == columnId }
        if (from < 0) return columns
        val column = ordered.removeAt(from)
        ordered.add(toIndex.coerceIn(0, ordered.size), column)
        return ordered.mapIndexed { i, c -> c.copy(position = i) }
    }

    /** Position for a card appended to the bottom of [columnId]. */
    fun nextPosition(cards: List<KanbanCard>, columnId: String): Int =
        (cards.filter { it.columnId == columnId }.maxOfOrNull { it.position } ?: -1) + 1
}
