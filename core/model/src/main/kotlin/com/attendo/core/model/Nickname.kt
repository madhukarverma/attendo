package com.attendo.core.model

/**
 * The name the app calls the user by. Names longer than [MAX_LENGTH] characters are shortened to
 * their first [SHORT_LETTERS] letters ("Prabhanshu Kumar" -> "Prab") so greetings stay on one line.
 * The full name is still what's stored in settings.csv.
 */
object Nickname {
    const val MAX_LENGTH = 10
    const val SHORT_LETTERS = 4

    fun of(name: String): String {
        val trimmed = name.trim()
        if (trimmed.length <= MAX_LENGTH) return trimmed
        val letters = trimmed.filter(Char::isLetter).take(SHORT_LETTERS).ifEmpty { trimmed.take(SHORT_LETTERS) }
        return letters.lowercase().replaceFirstChar(Char::uppercaseChar)
    }

    /** True when [of] shortens [name]. */
    fun isShortened(name: String): Boolean = name.trim().length > MAX_LENGTH
}
