package com.attendo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class KanbanOpsTest {
    private fun card(id: String, column: String, position: Int) = KanbanCard(id, column, "Card $id", position = position)

    private val cards = listOf(
        card("a", "todo", 0), card("b", "todo", 1), card("c", "todo", 2),
        card("x", "doing", 0), card("y", "doing", 1),
    )

    private fun order(cards: List<KanbanCard>, column: String) =
        cards.filter { it.columnId == column }.sortedBy { it.position }.map { it.id }

    @Test
    fun moveToAnotherColumnRenumbersBoth() {
        val moved = KanbanOps.moveCard(cards, "a", "doing", 1)
        assertEquals(listOf("b", "c"), order(moved, "todo"))
        assertEquals(listOf("x", "a", "y"), order(moved, "doing"))
        assertEquals(listOf(0, 1), moved.filter { it.columnId == "todo" }.map { it.position }.sorted())
        assertEquals(listOf(0, 1, 2), moved.filter { it.columnId == "doing" }.map { it.position }.sorted())
    }

    @Test
    fun reorderWithinColumnAndClampIndex() {
        assertEquals(listOf("b", "c", "a"), order(KanbanOps.moveCard(cards, "a", "todo", 99), "todo"))
        assertEquals(listOf("c", "a", "b"), order(KanbanOps.moveCard(cards, "c", "todo", -3), "todo"))
        assertEquals(cards, KanbanOps.moveCard(cards, "missing", "todo", 0))
    }

    @Test
    fun moveIntoEmptyColumn() {
        val moved = KanbanOps.moveCard(cards, "y", "done", 0)
        assertEquals(listOf("y"), order(moved, "done"))
        assertEquals(listOf("x"), order(moved, "doing"))
        assertEquals(1, KanbanOps.nextPosition(moved, "done"))
        assertEquals(0, KanbanOps.nextPosition(moved, "empty"))
    }

    @Test
    fun moveColumnRenumbers() {
        val columns = listOf(KanbanColumn("todo", "To do", 0), KanbanColumn("doing", "Doing", 1), KanbanColumn("done", "Done", 2, done = true))
        val moved = KanbanOps.moveColumn(columns, "done", 0)
        assertEquals(listOf("done", "todo", "doing"), moved.sortedBy { it.position }.map { it.id })
    }

    @Test
    fun boardKnowsOpenAndOverdueCards() {
        val today = LocalDate.of(2026, 11, 11)
        val columns = listOf(KanbanColumn("todo", "To do", 0), KanbanColumn("done", "Done", 1, done = true))
        val late = KanbanCard("late", "todo", "Late", due = today.minusDays(1))
        val finishedLate = KanbanCard("fin", "done", "Finished", due = today.minusDays(3))
        val later = KanbanCard("later", "todo", "Later", due = today.plusDays(2), position = 1)
        val board = KanbanBoard(columns, listOf(later, finishedLate, late))
        assertTrue(board.isOverdue(late, today))
        assertFalse(board.isOverdue(finishedLate, today))
        assertEquals(listOf("late", "later"), board.dueSoon().map { it.id })
        assertEquals("done", board.nextColumn("todo")?.id)
        assertEquals(null, board.nextColumn("done"))
    }
}
