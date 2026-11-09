package com.attendo.core.data.csv

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * In-memory cache of a CSV table, exposed as a [StateFlow]. The file is the source of truth on
 * start-up; every update is written through to disk before the flow emits.
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

    init {
        scope.launch(io) {
            mutex.withLock {
                val loaded = runCatching { table.read().mapNotNull(decode) }.getOrDefault(emptyList())
                _items.value = loaded
            }
        }
    }

    suspend fun update(transform: (List<T>) -> List<T>) {
        mutex.withLock {
            val current = _items.value ?: withContext(io) { table.read().mapNotNull(decode) }
            val next = transform(current)
            withContext(io) { table.write(next.map(encode)) }
            _items.value = next
        }
    }
}
