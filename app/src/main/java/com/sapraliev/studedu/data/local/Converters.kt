package com.sapraliev.studedu.data.local

import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Конвертеры типов для Room.
 * Instant (timestamptz) храним как epoch millis (Long),
 * LocalDate (date) — как ISO-строку "2026-07-09",
 * список строк (tags, byweekday) — через разделитель U+001F.
 */
class Converters {

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? =
        value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? =
        value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun stringListToString(value: List<String>?): String? =
        value?.joinToString(SEPARATOR)

    @TypeConverter
    fun stringToStringList(value: String?): List<String>? = when {
        value == null -> null
        value.isEmpty() -> emptyList()
        else -> value.split(SEPARATOR)
    }

    private companion object {
        /** U+001F (unit separator) — не встречается в обычном тексте. */
        const val SEPARATOR = "\u001F"
    }
}
