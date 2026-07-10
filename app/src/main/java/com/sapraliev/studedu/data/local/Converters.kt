package com.sapraliev.studedu.data.local

import androidx.room.TypeConverter
import com.sapraliev.studedu.data.local.entity.EventType
import com.sapraliev.studedu.data.local.entity.ExceptionType
import com.sapraliev.studedu.data.local.entity.PaymentDirection
import com.sapraliev.studedu.data.local.entity.RecurrenceFreq
import com.sapraliev.studedu.data.local.entity.TaskSource
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Конвертеры типов для Room.
 *
 * Instant (timestamptz) храним как epoch millis (Long),
 * LocalDate (date) — как ISO-строку "2026-07-09",
 * список строк (tags, byweekday) — через разделитель U+001F.
 *
 * Enum'ы храним в нижнем регистре — ровно так, как значения
 * enum-типов в Postgres ('personal', 'charge', …). Благодаря этому
 * слой синхронизации с Supabase передаёт значения как есть,
 * без маппинга в обе стороны.
 */
class Converters {

    // ---------- даты ----------

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

    // ---------- списки строк ----------

    @TypeConverter
    fun stringListToString(value: List<String>?): String? =
        value?.joinToString(SEPARATOR)

    @TypeConverter
    fun stringToStringList(value: String?): List<String>? = when {
        value == null -> null
        value.isEmpty() -> emptyList()
        else -> value.split(SEPARATOR)
    }

    // ---------- enum'ы (нижний регистр, как в Postgres) ----------

    @TypeConverter
    fun eventTypeToString(value: EventType?): String? = value?.name?.lowercase()

    @TypeConverter
    fun stringToEventType(value: String?): EventType? =
        value?.let { EventType.valueOf(it.uppercase()) }

    @TypeConverter
    fun recurrenceFreqToString(value: RecurrenceFreq?): String? = value?.name?.lowercase()

    @TypeConverter
    fun stringToRecurrenceFreq(value: String?): RecurrenceFreq? =
        value?.let { RecurrenceFreq.valueOf(it.uppercase()) }

    @TypeConverter
    fun exceptionTypeToString(value: ExceptionType?): String? = value?.name?.lowercase()

    @TypeConverter
    fun stringToExceptionType(value: String?): ExceptionType? =
        value?.let { ExceptionType.valueOf(it.uppercase()) }

    @TypeConverter
    fun paymentDirectionToString(value: PaymentDirection?): String? = value?.name?.lowercase()

    @TypeConverter
    fun stringToPaymentDirection(value: String?): PaymentDirection? =
        value?.let { PaymentDirection.valueOf(it.uppercase()) }

    @TypeConverter
    fun taskSourceToString(value: TaskSource?): String? = value?.name?.lowercase()

    @TypeConverter
    fun stringToTaskSource(value: String?): TaskSource? =
        value?.let { TaskSource.valueOf(it.uppercase()) }

    private companion object {
        /** U+001F (unit separator) — не встречается в обычном тексте. */
        const val SEPARATOR = "\u001F"
    }
}
