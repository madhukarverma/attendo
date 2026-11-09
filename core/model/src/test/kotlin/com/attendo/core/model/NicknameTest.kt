package com.attendo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NicknameTest {

    @Test
    fun shortNamesAreKept() {
        assertEquals("Madhukar", Nickname.of("Madhukar"))
        assertEquals("Ten Chars!", Nickname.of("  Ten Chars!  ")) // exactly 10 after trimming
        assertFalse(Nickname.isShortened("Ten Chars!"))
    }

    @Test
    fun longNamesBecomeFirstFourLetters() {
        assertEquals("Prab", Nickname.of("Prabhanshu Kumar"))
        assertEquals("Madh", Nickname.of("MADHUKAR VERMA"))
        assertEquals("Alpa", Nickname.of("Al Pacino Junior")) // letters only, spaces skipped
        assertTrue(Nickname.isShortened("Prabhanshu Kumar"))
    }
}
