package com.attendo.core.data.curriculum

import com.attendo.core.data.csv.Csv
import com.attendo.core.model.Curriculum
import com.attendo.core.model.CurriculumCourse
import com.attendo.core.model.CurriculumUnit

/**
 * Loads the bundled curriculum, `curriculum/mca_semester1.csv` (one row per unit:
 * code,name,hours,type,assessment,outcomes,unit,unit_title,topics), taken from
 * "MCA Detailed Curriculum", NIT Tiruchirappalli, Semester I.
 */
object CurriculumLoader {
    const val RESOURCE = "curriculum/mca_semester1.csv"
    private const val TITLE = "MCA · Semester I"

    fun bundled(): Curriculum {
        val text = CurriculumLoader::class.java.classLoader?.getResourceAsStream(RESOURCE)
            ?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: return Curriculum.EMPTY
        return parse(text)
    }

    fun parse(text: String, title: String = TITLE): Curriculum {
        val rows = Csv.parse(text.removePrefix("﻿"))
        val header = rows.firstOrNull()?.map { it.trim() } ?: return Curriculum.EMPTY
        val maps = rows.drop(1).map { row -> header.indices.associate { header[it] to row.getOrElse(it) { "" } } }
        val courses = maps.filter { it["code"].orEmpty().isNotBlank() }
            .groupBy { it.getValue("code").trim() }
            .map { (code, unitRows) ->
                val first = unitRows.first()
                CurriculumCourse(
                    code = code,
                    name = first["name"].orEmpty(),
                    contactHours = first["hours"]?.toIntOrNull() ?: 0,
                    type = first["type"].orEmpty(),
                    assessment = first["assessment"].orEmpty(),
                    units = unitRows.map { r ->
                        CurriculumUnit(
                            number = r["unit"].orEmpty(),
                            title = r["unit_title"].orEmpty(),
                            topics = Curriculum.splitTopics(r["topics"].orEmpty()),
                        )
                    },
                    outcomes = Curriculum.splitOutcomes(first["outcomes"].orEmpty()),
                )
            }
        return Curriculum(title, courses)
    }
}
