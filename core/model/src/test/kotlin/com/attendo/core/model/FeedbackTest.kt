package com.attendo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackTest {

    @Test
    fun aRatingAloneIsEnough() {
        assertNull(Feedback(rating = 5, category = FeedbackCategory.PRAISE, message = "").problem())
    }

    @Test
    fun withoutARatingTheMessageMustSaySomething() {
        assertEquals(ErrorCode.FEEDBACK_EMPTY, Feedback(0, FeedbackCategory.BUG, "  crash  ").problem())
        assertNull(Feedback(0, FeedbackCategory.BUG, "It crashes when I open Stats").problem())
    }

    @Test
    fun contactMustLookLikeAnEmailWhenGiven() {
        assertEquals(ErrorCode.FEEDBACK_BAD_EMAIL, Feedback(4, FeedbackCategory.IDEA, "", contact = "not an email").problem())
        assertNull(Feedback(4, FeedbackCategory.IDEA, "", contact = " student@example.com ").problem())
        assertTrue(Feedback.looksLikeEmail("a.b+c@college.ac.in"))
        assertFalse(Feedback.looksLikeEmail("a@b"))
    }

    @Test
    fun ratingsOutsideOneToFiveAreRejected() {
        assertEquals(ErrorCode.FEEDBACK_EMPTY, Feedback(6, FeedbackCategory.OTHER, "A long enough message").problem())
    }

    @Test
    fun errorMessagesEndWithTheirCode() {
        assertEquals("No internet connection. (FR-404)", ErrorCode.FRIENDS_OFFLINE.message())
        assertEquals(
            "Couldn't save your change to todos.csv. Your phone may be out of storage space. (ST-105)",
            ErrorCode.SAVE_FAILED.message("todos.csv"),
        )
        assertEquals("Restore failed: unknown error (IM-202)", ErrorCode.IMPORT_FAILED.message(null))
    }

    @Test
    fun everyCodeIsUniqueAndWellFormed() {
        val codes = ErrorCode.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
        assertTrue(codes.all { Regex("^(ST|IM|BK|FR|FB)-\\d{3}$").matches(it) })
    }
}
