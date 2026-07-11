package com.sapraliev.studedu.domain.occurrence

import com.sapraliev.studedu.data.local.entity.EventType
import kotlinx.datetime.Instant

/**
 * Конкретное вхождение события — то, что рисуется в ленте.
 * Для одиночного события это само событие, для серии — одно её повторение.
 */
data class Occurrence(
    val eventId: String,
    val title: String,
    val comment: String?,
    val type: EventType,
    val start: Instant,
    val end: Instant,
    val isAllDay: Boolean,
    val studentId: String?,
    val enrollmentId: String?,
    val color: String?,
    /** Вхождение перенесено исключением (recurrence_exceptions.moved). */
    val isMoved: Boolean = false,
    /**
     * Исходный старт вхождения в серии — ключ для создания исключения
     * («изменить только это вхождение»). Для одиночных событий null.
     */
    val ori