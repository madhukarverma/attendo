package com.attendo.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.CardPriority
import com.attendo.core.model.KanbanBoard
import com.attendo.core.model.KanbanCard
import com.attendo.core.model.KanbanColumn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Where a dragged card would land: [index] counts the column's other cards above it. */
private data class DropTarget(val columnId: String, val index: Int)

private sealed interface CardEdit {
    data class New(val columnId: String) : CardEdit
    data class Existing(val cardId: String) : CardEdit
}

internal sealed interface ColumnEdit {
    data object Add : ColumnEdit
    data class Rename(val column: KanbanColumn) : ColumnEdit
    data class Delete(val column: KanbanColumn, val cardCount: Int) : ColumnEdit
}

/**
 * Kanban board: columns side by side (scroll sideways), cards top to bottom.
 *
 * - Tap a card to edit it (title, notes, due date, priority, column) or delete it.
 * - Long-press a card and drag to move it within or between columns; near the left/right edge the
 *   board scrolls. The arrow on a card sends it straight to the next column.
 * - A column's menu renames, reorders or deletes it, or marks it as the "done" column.
 */
@Composable
internal fun BoardPage(board: KanbanBoard?, today: LocalDate, actions: BoardActions, bottomPadding: Dp) {
    if (board == null) {
        Box(Modifier.fillMaxSize())
        return
    }
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scroll = rememberScrollState()
    val currentBoard by rememberUpdatedState(board)

    var cardEdit by remember { mutableStateOf<CardEdit?>(null) }
    var columnEdit by remember { mutableStateOf<ColumnEdit?>(null) }

    // Drag state. Positions are in root coordinates so they stay valid while the board scrolls.
    val cardCoords = remember { HashMap<String, LayoutCoordinates>() }
    val columnCoords = remember { HashMap<String, LayoutCoordinates>() }
    var pageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragging by remember { mutableStateOf<KanbanCard?>(null) }
    var pointer by remember { mutableStateOf(Offset.Zero) }
    var grab by remember { mutableStateOf(Offset.Zero) }
    var dragSize by remember { mutableStateOf(IntSize.Zero) }
    var target by remember { mutableStateOf<DropTarget?>(null) }

    /** The column under (or nearest to) the pointer, and the slot among its cards. */
    fun locate(at: Offset): DropTarget? {
        val dragged = dragging ?: return null
        var best: KanbanColumn? = null
        var bestDistance = Float.MAX_VALUE
        for (column in currentBoard.sortedColumns) {
            val coords = columnCoords[column.id]?.takeIf { it.isAttached } ?: continue
            val left = coords.positionInRoot().x
            val right = left + coords.size.width
            val distance = when {
                at.x < left -> left - at.x
                at.x > right -> at.x - right
                else -> 0f
            }
            if (distance < bestDistance) {
                bestDistance = distance
                best = column
            }
        }
        val column = best ?: return null
        var index = 0
        for (card in currentBoard.cardsIn(column.id)) {
            if (card.id == dragged.id) continue
            val coords = cardCoords[card.id]?.takeIf { it.isAttached } ?: continue
            if (at.y > coords.positionInRoot().y + coords.size.height / 2f) index++
        }
        return DropTarget(column.id, index)
    }

    fun finishDrag(commit: Boolean) {
        val card = dragging
        val drop = target
        dragging = null
        target = null
        if (!commit || card == null || drop == null) return
        // Dropping a card back where it was changes nothing.
        val sameSpot = drop.columnId == card.columnId &&
            currentBoard.cardsIn(card.columnId).indexOfFirst { it.id == card.id } == drop.index
        if (!sameSpot) actions.moveCard(card.id, drop.columnId, drop.index)
    }

    // While dragging near the left/right edge, scroll the board and keep the drop slot current.
    LaunchedEffect(dragging?.id) {
        if (dragging == null) return@LaunchedEffect
        val edge = with(density) { 56.dp.toPx() }
        val step = with(density) { 12.dp.toPx() }
        while (true) {
            withFrameNanos { }
            val page = pageCoords?.takeIf { it.isAttached } ?: continue
            val left = page.positionInRoot().x
            val right = left + page.size.width
            val delta = when {
                pointer.x < left + edge -> -step
                pointer.x > right - edge -> step
                else -> 0f
            }
            if (delta != 0f && scroll.scrollBy(delta) != 0f) target = locate(pointer)
        }
    }

    @Composable
    fun dragModifier(card: KanbanCard): Modifier {
        val latest by rememberUpdatedState(card)
        DisposableEffect(card.id) { onDispose { cardCoords.remove(card.id) } }
        return Modifier
            .onGloballyPositioned { cardCoords[card.id] = it }
            .pointerInput(card.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val coords = cardCoords[card.id]?.takeIf { it.isAttached }
                        if (coords != null) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            grab = offset
                            dragSize = coords.size
                            pointer = coords.localToRoot(offset)
                            dragging = latest
                            target = locate(pointer)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val coords = cardCoords[card.id]?.takeIf { it.isAttached }
                        if (dragging != null && coords != null) {
                            // Recomputed from the card's current place, so it's right even while scrolling.
                            pointer = coords.localToRoot(change.position)
                            target = locate(pointer)
                        }
                    },
                    onDragEnd = { finishDrag(commit = true) },
                    onDragCancel = { finishDrag(commit = false) },
                )
            }
    }

    val columns = board.sortedColumns
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { pageCoords = it }) {
        val columnWidth = minOf(300.dp, maxWidth * 0.8f)
        Row(
            Modifier
                .fillMaxSize()
                .horizontalScroll(scroll)
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = bottomPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            columns.forEachIndexed { columnIndex, column ->
                key(column.id) {
                    DisposableEffect(column.id) { onDispose { columnCoords.remove(column.id) } }
                    val cards = board.cardsIn(column.id)
                    val next = board.nextColumn(column.id)
                    ColumnShell(
                        column = column,
                        cardCount = cards.size,
                        isFirst = columnIndex == 0,
                        isLast = columnIndex == columns.lastIndex,
                        isDropTarget = target?.columnId == column.id,
                        modifier = Modifier.width(columnWidth).onGloballyPositioned { columnCoords[column.id] = it },
                        onAddCard = { cardEdit = CardEdit.New(column.id) },
                        onRename = { columnEdit = ColumnEdit.Rename(column) },
                        onMove = { by -> actions.moveColumn(column.id, columnIndex + by) },
                        onToggleDone = { actions.setColumnDone(column.id, !column.done) },
                        onDelete = { columnEdit = ColumnEdit.Delete(column, cards.size) },
                    ) {
                        val dropIndex = target?.takeIf { it.columnId == column.id }?.index
                        var slot = 0
                        cards.forEach { card ->
                            val isDragged = card.id == dragging?.id
                            if (!isDragged) {
                                if (dropIndex == slot) DropLine()
                                slot++
                            }
                            key(card.id) {
                                CardFace(
                                    card = card,
                                    overdue = board.isOverdue(card, today),
                                    today = today,
                                    modifier = Modifier
                                        .alpha(if (isDragged) 0.3f else 1f)
                                        .clip(CardShape)
                                        .iosPress(PressStyle.Highlight, onClickLabel = "Edit ${card.title}") {
                                            cardEdit = CardEdit.Existing(card.id)
                                        }
                                        // After the click modifier, so a long-press drag wins over the tap.
                                        .then(dragModifier(card)),
                                    nextColumnTitle = next?.title,
                                    onNext = { if (next != null) actions.moveCard(card.id, next.id, Int.MAX_VALUE) },
                                )
                            }
                        }
                        if (dropIndex != null && dropIndex >= slot) DropLine()
                        if (cards.isEmpty() && dropIndex == null) {
                            Text(
                                "No cards yet",
                                style = MaterialTheme.typography.labelSmall,
                                color = AttendoColors.Muted,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
            AddColumnTile(Modifier.width(columnWidth * 0.75f)) { columnEdit = ColumnEdit.Add }
        }

        // The card under the finger, drawn above the board.
        dragging?.let { card ->
            val origin = pageCoords?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero
            CardFace(
                card = card,
                overdue = board.isOverdue(card, today),
                today = today,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (pointer.x - grab.x - origin.x).roundToInt(),
                            (pointer.y - grab.y - origin.y).roundToInt(),
                        )
                    }
                    .width(with(density) { dragSize.width.toDp() })
                    .graphicsLayer {
                        rotationZ = 2f
                        shadowElevation = 16.dp.toPx()
                        shape = CardShape
                        clip = true
                    },
            )
        }
    }

    when (val edit = cardEdit) {
        is CardEdit.New -> CardEditorDialog(
            card = null,
            columns = columns,
            initialColumnId = edit.columnId,
            onDismiss = { cardEdit = null },
            onSave = { title, notes, due, priority, columnId ->
                cardEdit = null
                actions.addCard(columnId, title, notes, due, priority)
            },
            onDelete = null,
        )
        is CardEdit.Existing -> {
            val card = board.cards.firstOrNull { it.id == edit.cardId }
            if (card == null) {
                // The card was deleted (e.g. its column was removed) while the editor was open.
                SideEffect { cardEdit = null }
            } else {
                CardEditorDialog(
                    card = card,
                    columns = columns,
                    initialColumnId = card.columnId,
                    onDismiss = { cardEdit = null },
                    onSave = { title, notes, due, priority, columnId ->
                        cardEdit = null
                        actions.updateCard(card.copy(title = title, notes = notes, due = due, priority = priority))
                        if (columnId != card.columnId) actions.moveCard(card.id, columnId, Int.MAX_VALUE)
                    },
                    onDelete = {
                        cardEdit = null
                        actions.deleteCard(card.id)
                    },
                )
            }
        }
        null -> Unit
    }

    columnEdit?.let { edit ->
        ColumnDialog(
            edit = edit,
            onDismiss = { columnEdit = null },
            onAdd = { title -> columnEdit = null; actions.addColumn(title) },
            onRename = { column, title -> columnEdit = null; actions.renameColumn(column.id, title) },
            onDelete = { column -> columnEdit = null; actions.deleteColumn(column.id) },
        )
    }
}

private val CardShape = RoundedCornerShape(16.dp)
private val DueFormat = DateTimeFormatter.ofPattern("d MMM")

internal fun CardPriority.color(): Color = when (this) {
    CardPriority.NONE -> AttendoColors.CreamDeep
    CardPriority.LOW -> AttendoColors.Blue
    CardPriority.MEDIUM -> AttendoColors.Yellow
    CardPriority.HIGH -> AttendoColors.Pink
}

internal fun CardPriority.label(): String = when (this) {
    CardPriority.NONE -> "None"
    CardPriority.LOW -> "Low"
    CardPriority.MEDIUM -> "Medium"
    CardPriority.HIGH -> "High"
}

private fun dueLabel(due: LocalDate, today: LocalDate, overdue: Boolean): String = when {
    overdue -> "Overdue · ${due.format(DueFormat)}"
    due == today -> "Due today"
    due == today.plusDays(1) -> "Due tomorrow"
    else -> "Due ${due.format(DueFormat)}"
}

/** A column's frame: header with menu, the cards ([content]) and "Add a card". */
@Composable
private fun ColumnShell(
    column: KanbanColumn,
    cardCount: Int,
    isFirst: Boolean,
    isLast: Boolean,
    isDropTarget: Boolean,
    modifier: Modifier,
    onAddCard: () -> Unit,
    onRename: () -> Unit,
    onMove: (by: Int) -> Unit,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AttendoColors.CreamCard)
            .then(if (isDropTarget) Modifier.border(2.dp, AttendoColors.Green, RoundedCornerShape(20.dp)) else Modifier),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Title and count share the space left of the menu; a long title is cut, not the count.
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    column.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (column.done) "$cardCount ✓" else "$cardCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (column.done) AttendoColors.Green else AttendoColors.Muted,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AttendoColors.Cream)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Box {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .iosPress(PressStyle.Dim, onClickLabel = "${column.title} options") { menu = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null, tint = AttendoColors.Muted, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = AttendoColors.CreamCard) {
                    MenuItem("Rename") { menu = false; onRename() }
                    if (!isFirst) MenuItem("Move left") { menu = false; onMove(-1) }
                    if (!isLast) MenuItem("Move right") { menu = false; onMove(1) }
                    MenuItem(if (column.done) "Not a \"done\" column" else "Use as \"done\" column") { menu = false; onToggleDone() }
                    MenuItem("Delete column", AttendoColors.Pink) { menu = false; onDelete() }
                }
            }
        }
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
        Row(
            Modifier
                .fillMaxWidth()
                .iosPress(PressStyle.Highlight, onClickLabel = "Add a card to ${column.title}", onClick = onAddCard)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = AttendoColors.Green, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add a card", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Green)
        }
    }
}

@Composable
private fun MenuItem(text: String, color: Color = AttendoColors.Ink, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text, color = color, style = MaterialTheme.typography.labelLarge) }, onClick = onClick)
}

/** Where a dragged card will be inserted. */
@Composable
private fun DropLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(AttendoColors.Green),
    )
}

@Composable
private fun AddColumnTile(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .iosPress(PressStyle.Highlight, onClickLabel = "Add a column", onClick = onClick)
            .border(1.5.dp, AttendoColors.CreamDeep, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = AttendoColors.Muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Add a column", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Muted)
    }
}

/** What a card looks like, on the board and while it's being dragged. */
@Composable
private fun CardFace(
    card: KanbanCard,
    overdue: Boolean,
    today: LocalDate,
    modifier: Modifier = Modifier,
    /** When set, the card shows an arrow button that sends it to that column. */
    nextColumnTitle: String? = null,
    onNext: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(AttendoColors.Cream, CardShape)
            .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (card.priority != CardPriority.NONE) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(card.priority.color()))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                card.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (nextColumnTitle != null) {
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .iosPress(PressStyle.Scale, onClickLabel = "Move to $nextColumnTitle", onClick = onNext)
                        .background(AttendoColors.CreamCard),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = AttendoColors.Ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        if (card.notes.isNotBlank()) {
            Text(
                card.notes,
                style = MaterialTheme.typography.bodySmall,
                color = AttendoColors.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        card.due?.let { due ->
            val color = if (overdue) AttendoColors.Pink else AttendoColors.Muted
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DateRange, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(dueLabel(due, today, overdue), style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}
