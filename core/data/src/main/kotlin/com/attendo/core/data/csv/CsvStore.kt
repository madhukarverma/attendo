package com.attendo.core.data.csv

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * In-memory cache of a CSV table, exposed as a [StateFlow]. The file is the source of truth on
 * start-up; every update is written through to disk before the flow emits.
 *
 * Storage problems never crash the app or lose data: a file that can't be read is copied aside
 * before anything is written over it, and a save that fails keeps the previous data. Both are
 * reported on [StorageProblems].
 */
internal class CsvStore<T>(
    private val table: CsvTable,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val decode: (Map<String, String>) -> T?,
    private val encode: (T) -> Map<String, String>,
) {
    private val mutex = Mutex()
    private val _items = MutableStateFlow<List<T>?>(null)

    /** `null` until the file has been loaded. */
    val items: StateFlow<List<T>?> = _items.asStateFlow()

    /** False when the file couldn't be read and no copy of it could be kept: saving would lose it. */
    private var writable = true

    init {
        scope.launch(io) {
            mutex.withLock {
                if (_items.value == null) _items.value = load()
            }
        }
    }

    /**
     * Replaces the whole table with [rows] (header-keyed, e.g. from an imported CSV). Rows that don't
     * decode are dropped. Returns how many were kept (0 if saving failed).
     */
    suspend fun replaceRows(rows: List<Map<String, String>>): Int {
        val decoded = rows.mapNotNull { runCatching { decode(it) }.getOrNull() }
        return if (save { decoded }) decoded.size else 0
    }

    suspend fun update(transform: (List<T>) -> List<T>) {
        save(transform)
    }

    /** Applies [transform] and writes the result. Returns false, with the data unchanged, if saving failed. */
    private suspend fun save(transform: (List<T>) -> List<T>): Boolean = mutex.withLock {
        val current = _items.value ?: withContext(io) { load() }.also { _items.value = it }
        if (!writable) {
            StorageProblems.report("Changes to ${table.file.name} can't be saved because the file couldn't be read. Restart the app.")
            return@withLock false
        }
        val next = transform(current)
        try {
            withContext(io) { table.write(next.map(encode)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            StorageProblems.report("Couldn't save your change to ${table.file.name}. Your phone may be out of storage space.")
            return@withLock false
        }
        _items.value = next
        true
    }

    /** Reads the file. If that fails, keeps a copy of it and starts empty. Call with [mutex] held. */
    private fun load(): List<T> = try {
        table.read().mapNotNull { row -> runCatching { decode(row) }.getOrNull() }
    } catch (e: Exception) {
        val kept = runCatching { table.keepUnreadableCopy() }
        writable = kept.isSuccess
        val copy = kept.getOrNull()
        StorageProblems.report(
            when {
                kept.isFailure -> "${table.file.name} couldn't be read. Changes to it won't be saved until the app is restarted."
                copy == null -> "${table.file.name} couldn't be read, so it starts empty."
                else -> "${table.file.name} couldn't be read, so it starts empty. The old file was kept as ${copy.name}."
            },
        )
        emptyList()
    }
}

/** Storage problems the user should hear about: a data file that couldn't be read or saved. */
object StorageProblems {
    private val channel = Channel<String>(capacity = 16)

    /** Each message is delivered once; messages wait here until the app collects them. */
    val messages: Flow<String> = channel.receiveAsFlow()

    internal fun report(message: String) {
        channel.trySend(message)
    }
}
