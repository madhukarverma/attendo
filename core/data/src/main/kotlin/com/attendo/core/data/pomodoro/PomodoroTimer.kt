package com.attendo.core.data.pomodoro

import com.attendo.core.data.PomodoroRepository
import com.attendo.core.model.PomodoroKind
import com.attendo.core.model.PomodoroSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class PomodoroConfig(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    /** A long break follows every N completed focus sessions. */
    val longBreakEvery: Int = 4,
) {
    fun minutesFor(kind: PomodoroKind) = when (kind) {
        PomodoroKind.FOCUS -> focusMinutes
        PomodoroKind.SHORT_BREAK -> shortBreakMinutes
        PomodoroKind.LONG_BREAK -> longBreakMinutes
    }
}

data class PomodoroTimerState(
    val kind: PomodoroKind = PomodoroKind.FOCUS,
    val running: Boolean = false,
    val totalMillis: Long,
    val remainingMillis: Long,
    val subjectId: String? = null,
    /** Completed focus sessions in the current cycle (0 until longBreakEvery). */
    val focusDoneInCycle: Int = 0,
    val config: PomodoroConfig = PomodoroConfig(),
) {
    val progress: Float get() = if (totalMillis == 0L) 0f else 1f - remainingMillis.toFloat() / totalMillis
    val isFresh: Boolean get() = !running && remainingMillis == totalMillis
}

/**
 * App-scoped Pomodoro engine. Time is derived from the wall clock (end timestamp), so the countdown
 * stays correct while the screen is closed or the app is in the background. Each run is written to
 * pomodoro.csv when it finishes, or when a focus run of at least [MIN_RECORD_SECONDS] is stopped early.
 */
class PomodoroTimer(
    private val scope: CoroutineScope,
    private val repository: PomodoroRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
    config: PomodoroConfig = PomodoroConfig(),
) {
    private val _state = MutableStateFlow(freshState(PomodoroKind.FOCUS, config, null, 0))
    val state: StateFlow<PomodoroTimerState> = _state.asStateFlow()

    private val _finished = MutableSharedFlow<PomodoroKind>(extraBufferCapacity = 4)

    /** Emits the kind of run that just finished naturally (for haptics/sounds). */
    val finished: SharedFlow<PomodoroKind> = _finished.asSharedFlow()

    private var endsAt: Long = 0L
    private var startedAt: Long = 0L
    private var ticker: Job? = null

    fun start() {
        val s = _state.value
        if (s.running) return
        val now = clock()
        if (s.isFresh) startedAt = now
        endsAt = now + s.remainingMillis
        _state.value = s.copy(running = true)
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val remaining = endsAt - clock()
                if (remaining <= 0) {
                    complete()
                    break
                }
                _state.value = _state.value.copy(remainingMillis = remaining)
                delay(TICK_MILLIS)
            }
        }
    }

    fun pause() {
        val s = _state.value
        if (!s.running) return
        ticker?.cancel()
        _state.value = s.copy(running = false, remainingMillis = (endsAt - clock()).coerceAtLeast(0))
    }

    fun toggle() = if (_state.value.running) pause() else start()

    /** Stops the current run (recording it if long enough) and resets the same phase. */
    fun reset() {
        recordStopped()
        val s = _state.value
        ticker?.cancel()
        _state.value = freshState(s.kind, s.config, s.subjectId, s.focusDoneInCycle)
    }

    /** Ends the current phase early and moves on to the next one. */
    fun skip() {
        recordStopped()
        ticker?.cancel()
        advance(countFocus = false)
    }

    fun selectSubject(subjectId: String?) {
        _state.value = _state.value.copy(subjectId = subjectId)
    }

    /** Changes the focus length; only applied while the timer isn't running. */
    fun setFocusMinutes(minutes: Int) {
        val s = _state.value
        if (s.running) return
        val config = s.config.copy(focusMinutes = minutes)
        _state.value = if (s.kind == PomodoroKind.FOCUS && s.isFresh) {
            freshState(s.kind, config, s.subjectId, s.focusDoneInCycle)
        } else {
            s.copy(config = config)
        }
    }

    private suspend fun complete() {
        val s = _state.value
        save(s, actualSeconds = (s.totalMillis / 1000).toInt(), completed = true)
        _finished.tryEmit(s.kind)
        advance(countFocus = s.kind == PomodoroKind.FOCUS)
    }

    private fun advance(countFocus: Boolean) {
        val s = _state.value
        val done = if (countFocus) s.focusDoneInCycle + 1 else s.focusDoneInCycle
        val (next, cycle) = when (s.kind) {
            PomodoroKind.FOCUS ->
                if (countFocus && done >= s.config.longBreakEvery) PomodoroKind.LONG_BREAK to done
                else PomodoroKind.SHORT_BREAK to done
            PomodoroKind.LONG_BREAK -> PomodoroKind.FOCUS to 0
            PomodoroKind.SHORT_BREAK -> PomodoroKind.FOCUS to done
        }
        _state.value = freshState(next, s.config, s.subjectId, cycle)
    }

    private fun recordStopped() {
        val s = _state.value
        if (s.isFresh) return
        val elapsed = s.totalMillis - (if (s.running) (endsAt - clock()).coerceAtLeast(0) else s.remainingMillis)
        val seconds = (elapsed / 1000).toInt()
        if (s.kind == PomodoroKind.FOCUS && seconds >= MIN_RECORD_SECONDS) {
            scope.launch { save(s, seconds, completed = false) }
        }
    }

    private suspend fun save(s: PomodoroTimerState, actualSeconds: Int, completed: Boolean) {
        val start = if (startedAt == 0L) clock() - actualSeconds * 1000L else startedAt
        repository.add(
            PomodoroSession(
                id = UUID.randomUUID().toString(),
                date = Instant.ofEpochMilli(start).atZone(zone).toLocalDate(),
                startedAt = start,
                kind = s.kind,
                subjectId = if (s.kind == PomodoroKind.FOCUS) s.subjectId else null,
                plannedSeconds = (s.totalMillis / 1000).toInt(),
                actualSeconds = actualSeconds,
                completed = completed,
            ),
        )
        startedAt = 0L
    }

    private fun freshState(kind: PomodoroKind, config: PomodoroConfig, subjectId: String?, done: Int): PomodoroTimerState {
        val total = config.minutesFor(kind) * 60_000L
        return PomodoroTimerState(
            kind = kind,
            running = false,
            totalMillis = total,
            remainingMillis = total,
            subjectId = subjectId,
            focusDoneInCycle = done,
            config = config,
        )
    }

    companion object {
        const val TICK_MILLIS = 250L
        const val MIN_RECORD_SECONDS = 60
    }
}
