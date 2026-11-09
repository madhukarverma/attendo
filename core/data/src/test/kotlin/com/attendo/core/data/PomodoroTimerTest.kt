package com.attendo.core.data

import com.attendo.core.data.pomodoro.PomodoroConfig
import com.attendo.core.data.pomodoro.PomodoroTimer
import com.attendo.core.model.PomodoroKind
import com.attendo.core.model.PomodoroSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class PomodoroTimerTest {

    private class FakeRepo : PomodoroRepository {
        val saved = MutableStateFlow<List<PomodoroSession>>(emptyList())
        override val sessions: Flow<List<PomodoroSession>> = saved
        override suspend fun add(session: PomodoroSession) {
            saved.value = saved.value + session
        }
    }

    @Test
    fun completedFocusIsRecordedAndMovesToBreak() = runTest {
        val repo = FakeRepo()
        val timer = PomodoroTimer(backgroundScope, repo, { testScheduler.currentTime }, ZoneOffset.UTC, PomodoroConfig(focusMinutes = 25))
        timer.selectSubject("os")
        timer.start()
        runCurrent()
        assertTrue(timer.state.value.running)

        advanceTimeBy(25 * 60_000L + 500)
        runCurrent()

        val session = repo.saved.value.single()
        assertEquals(PomodoroKind.FOCUS, session.kind)
        assertEquals("os", session.subjectId)
        assertEquals(1500, session.actualSeconds)
        assertTrue(session.completed)
        assertEquals(PomodoroKind.SHORT_BREAK, timer.state.value.kind)
        assertEquals(1, timer.state.value.focusDoneInCycle)
        assertFalse(timer.state.value.running)
    }

    @Test
    fun pauseKeepsRemainingAndShortResetIsNotRecorded() = runTest {
        val repo = FakeRepo()
        val timer = PomodoroTimer(backgroundScope, repo, { testScheduler.currentTime }, ZoneOffset.UTC)
        timer.start()
        advanceTimeBy(30_000)
        timer.pause()
        val remaining = timer.state.value.remainingMillis
        advanceTimeBy(60_000)
        assertEquals(remaining, timer.state.value.remainingMillis)
        timer.reset() // only 30 s of focus: below the 60 s threshold
        runCurrent()
        assertTrue(repo.saved.value.isEmpty())
        assertTrue(timer.state.value.isFresh)
    }

    @Test
    fun longBreakAfterFourFocusSessions() = runTest {
        val repo = FakeRepo()
        val timer = PomodoroTimer(backgroundScope, repo, { testScheduler.currentTime }, ZoneOffset.UTC,
            PomodoroConfig(focusMinutes = 1, shortBreakMinutes = 1, longBreakMinutes = 1))
        repeat(7) {
            timer.start()
            advanceTimeBy(60_500)
            runCurrent()
        }
        assertEquals(PomodoroKind.LONG_BREAK, timer.state.value.kind)
        assertEquals(4, repo.saved.value.count { it.kind == PomodoroKind.FOCUS })
    }
}
