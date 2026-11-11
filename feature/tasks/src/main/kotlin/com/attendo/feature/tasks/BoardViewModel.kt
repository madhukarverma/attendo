package com.attendo.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.core.data.KanbanRepository
import com.attendo.core.model.CardPriority
import com.attendo.core.model.KanbanBoard
import com.attendo.core.model.KanbanCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Everything the board UI can ask for; implemented by [BoardViewModel]. */
internal interface BoardActions {
    fun addColumn(title: String)
    fun renameColumn(columnId: String, title: String)
    fun setColumnDone(columnId: String, done: Boolean)
    fun moveColumn(columnId: String, toIndex: Int)
    fun deleteColumn(columnId: String)
    fun addCard(columnId: String, title: String, notes: String, due: LocalDate?, priority: CardPriority)
    fun updateCard(card: KanbanCard)
    fun moveCard(cardId: String, toColumnId: String, toIndex: Int)
    fun deleteCard(cardId: String)
}

/** `null` board = still loading. */
internal class BoardViewModel(private val kanban: KanbanRepository) : ViewModel(), BoardActions {

    val board: StateFlow<KanbanBoard?> = kanban.board
        .map<KanbanBoard, KanbanBoard?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // A new board starts with To do / Doing / Done.
        viewModelScope.launch { kanban.ensureDefaultColumns() }
    }

    override fun addColumn(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { kanban.addColumn(title) }
    }

    override fun renameColumn(columnId: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { kanban.renameColumn(columnId, title) }
    }

    override fun setColumnDone(columnId: String, done: Boolean) {
        viewModelScope.launch { kanban.setColumnDone(columnId, done) }
    }

    override fun moveColumn(columnId: String, toIndex: Int) {
        viewModelScope.launch { kanban.moveColumn(columnId, toIndex) }
    }

    override fun deleteColumn(columnId: String) {
        viewModelScope.launch { kanban.deleteColumn(columnId) }
    }

    override fun addCard(columnId: String, title: String, notes: String, due: LocalDate?, priority: CardPriority) {
        if (title.isBlank()) return
        viewModelScope.launch { kanban.addCard(columnId, title, notes, due, priority) }
    }

    override fun updateCard(card: KanbanCard) {
        if (card.title.isBlank()) return
        viewModelScope.launch { kanban.updateCard(card.copy(title = card.title.trim(), notes = card.notes.trim())) }
    }

    override fun moveCard(cardId: String, toColumnId: String, toIndex: Int) {
        viewModelScope.launch { kanban.moveCard(cardId, toColumnId, toIndex) }
    }

    override fun deleteCard(cardId: String) {
        viewModelScope.launch { kanban.deleteCard(cardId) }
    }
}
