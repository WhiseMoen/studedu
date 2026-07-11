package com.sapraliev.studedu.domain.schedule

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** Одна пара из расписания вуза (уже с конкретными датой и временем). */
data class ScheduleEntry(
    val subject: String,
    val teacher: String?,
    val place: String?,
    val lessonType: String?,
    val start: Instant,
    val end: Instant,
)

/**
 * Абстракция источника расписания вуза.
 *
 * Вся сеть и парсинг конкретного вуза живут в одной реализации:
 * источник сломался — чинится один адаптер; новый вуз — новая
 * реализация, остальное приложение не меняется.
 */
interface ScheduleProvider {
    /** Идентификатор провайдера, например "mospolytech". */
    val id: String

    /**
     * Расписание группы на диапазон дат.
     * @throws ScheduleSyncException при сетевой ошибке или смене формата.
     */
    suspend fun getSchedule(group: String, from: LocalDate, to: LocalDate): List<ScheduleEntry>
}

class ScheduleSyncException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
