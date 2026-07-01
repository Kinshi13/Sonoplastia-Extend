package com.escalachurch.app.data.local.converter

import androidx.room.TypeConverter
import com.escalachurch.app.domain.model.ProgramType
import com.escalachurch.app.domain.model.RepeatRule
import java.time.LocalDate
import java.time.LocalTime

class Converters {

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fromProgramType(value: ProgramType?): String? = value?.name

    @TypeConverter
    fun toProgramType(value: String?): ProgramType? = value?.let { ProgramType.valueOf(it) }

    @TypeConverter
    fun fromRepeatRule(value: RepeatRule?): String? = value?.name

    @TypeConverter
    fun toRepeatRule(value: String?): RepeatRule? = value?.let { RepeatRule.valueOf(it) }
}
