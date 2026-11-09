package com.attendo.core.model

/** A subject offered in the student's programme, used to pre-fill subjects. */
data class CatalogSubject(
    val code: String,
    val name: String,
    val isLab: Boolean = false,
)

/**
 * Built-in course list. v0.4 ships the current semester's CA subjects; more semesters/programmes
 * can be added here (or loaded from a CSV) later.
 */
object CourseCatalog {
    val CURRENT_SEMESTER: List<CatalogSubject> = listOf(
        CatalogSubject("CA711", "Problem Solving and Programming"),
        CatalogSubject("CA713", "Mathematical Foundations of Computer Applications"),
        CatalogSubject("CA715", "Digital Logic and Computer Organization"),
        CatalogSubject("CA717", "Data Structures and Applications"),
        CatalogSubject("CA719", "Operating Systems"),
        CatalogSubject("CA701", "Problem Solving Lab using Python", isLab = true),
        CatalogSubject("CA703", "Data Structures Lab using C", isLab = true),
    )

    /** Catalog entries whose code is not already used by an existing subject. */
    fun missing(existing: List<Subject>, catalog: List<CatalogSubject> = CURRENT_SEMESTER): List<CatalogSubject> {
        val codes = existing.mapTo(HashSet()) { it.code.trim().uppercase() }
        return catalog.filterNot { it.code.uppercase() in codes }
    }
}
