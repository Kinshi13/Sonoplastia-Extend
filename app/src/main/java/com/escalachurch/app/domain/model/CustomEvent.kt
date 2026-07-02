package com.escalachurch.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** Represents a custom program created by the user (special events, prayer week, etc.). */
data class CustomEvent(
    val id: Long = 0L,
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime? = null,
    val description: String = "",
    val eventType: ProgramType = ProgramType.OTHER,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    /** Id linking recurring/duplicated occurrences generated together, for grouped edits. */
    val groupId: String? = null,
    /** PERSONAL by default (created via Programar by a member); an admin can mark one OFFICIAL. */
    val sourceType: SourceType = SourceType.PERSONAL,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
