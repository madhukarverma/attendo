package com.attendo.core.model

/** A subject offered in the student's programme, used to pre-fill subjects. */
data class CatalogSubject(
    val code: String,
    val name: String,
    val isLab: Boolean = false,
    /** Short everyday name, e.g. "DSA". */
    val short: String = "",
    /** Total lecture hours, when known (pre-fills [Subject.lectureHours]). */
    val lectureHours: Int? = null,
)

/**
 * Built-in course list. v0.4 ships the current semester's CA subjects; more semesters/programmes
 * can be added here (or loaded from a CSV) later.
 */
object CourseCatalog {
    val CURRENT_SEMESTER: List<CatalogSubject> = listOf(
        // Lecture hours: "Contact Hours" in the MCA Detailed Curriculum (NIT Trichy, Semester I).
        CatalogSubject("CA711", "Problem Solving and Programming", short = "Python", lectureHours = 40),
        CatalogSubject("CA713", "Mathematical Foundations of Computer Applications", short = "Maths", lectureHours = 40),
        CatalogSubject("CA715", "Digital Logic and Computer Organization", short = "DLCO", lectureHours = 40),
        CatalogSubject("CA717", "Data Structures and Applications", short = "DSA", lectureHours = 40),
        CatalogSubject("CA719", "Operating Systems", short = "OS", lectureHours = 40),
        CatalogSubject("CA701", "Problem Solving Lab using Python", isLab = true, short = "Python Lab"),
        CatalogSubject("CA703", "Data Structures Lab using C", isLab = true, short = "DS Lab"),
    )

    private val byCode = CURRENT_SEMESTER.associateBy { it.code.uppercase() }
    private val stopWords = setOf("and", "of", "using", "for", "the", "in", "to", "with")

    /** Catalog entries whose code is not already used by an existing subject. */
    fun missing(existing: List<Subject>, catalog: List<CatalogSubject> = CURRENT_SEMESTER): List<CatalogSubject> {
        val codes = existing.mapTo(HashSet()) { it.code.trim().uppercase() }
        return catalog.filterNot { it.code.uppercase() in codes }
    }

    /** Short name: the catalog's abbreviation, otherwise the initials of the significant words. */
    fun shortName(subject: Subject): String {
        byCode[subject.code.trim().uppercase()]?.short?.takeIf { it.isNotBlank() }?.let { return it }
        val words = subject.name.split(" ", "-").filter { it.isNotBlank() }
        if (words.size == 1) return words.first().take(8)
        val lab = isLab(subject)
        val initials = words.filterNot { it.lowercase() in stopWords || it.equals("lab", true) }
            .joinToString("") { it.first().uppercase() }
            .take(5)
        return if (lab) "$initials Lab" else initials
    }

    fun isLab(subject: Subject): Boolean =
        byCode[subject.code.trim().uppercase()]?.isLab ?: subject.name.split(" ").any { it.equals("lab", true) }
}
