package com.sapraliev.studedu.domain.occurrence

import com.sapraliev.studedu.data.local.entity.EventEntity
import com.sapraliev.studedu.data.local.entity.ExceptionType
import com.sapraliev.studedu.data.local.entity.RecurrenceExceptionEntity
import com.sapraliev.studedu.data.local.entity.RecurrenceRuleEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.dmfs.rfc5545.DateTime

/**
 * Генерация конкретных вхождений событий для диапазона дат.
 *
 * Чистая JVM-логика без Android-зависимостей — покрыта unit-тестами.
 * Серии никогда не хранятся построчно: только правило + исключения,
 * вхождения вычисляются здесь на лету.
 *
 * @param zone зона пользователя: повторение «каждый день в 10:00» —
 * понятие локального времени, итерируем правило именно в этой зоне.
 */
class OccurrenceGenerator(
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) {

    /**
     * @param events   события (одиночные и шаблоны серий)
     * @param rules    правила повторения по id
     * @param exceptions исключения серий (отмены/переносы)
     * @param from     начало окна (включительно)
     * @param to       конец окна (не включительно)
     * @return вхождения, пересекающие окно, по возрастанию start
     */
    fun generate(
        events: List<EventEntity>,
        rules: Map<String, RecurrenceRuleEntity>,
        exceptions: List<RecurrenceExceptionEntity>,
        from: Instant,
        to: Instant,
    ): List<Occurrence> {
        require(from < to) { "from must be before to" }
        val exceptionsByEvent = exceptions.groupBy { it.eventId }
        val result = mutableListOf<Occurrence>()

        for (event in events) {
            val rule = event.recurrenceRuleId?.let(rules::get)
            if (rule == null) {
                if (event.startAt < to && event.endAt > from) {
                    result += event.toOccurrence(event.startAt, event.endAt)
                }
            } else {
                result += generateSeries(
                    event, rule, exceptionsByEvent[event.id].orEmpty(), from, to,
                )
            }
        }
        return result.sortedBy { it.start }
    }

    private fun generateSeries(
        event: EventEntity,
        rule: RecurrenceRuleEntity,
        exceptions: List<RecurrenceExceptionEntity>,
        from: Instant,
        to: Instant,
    ): List<Occurrence> {
        val result = mutableListOf<Occurrence>()
        val duration = event.endAt - event.startAt
        val exceptionByOriginal = exceptions.associateBy { it.originalDate }

        val recurrence = RRuleMapper.toRRule(rule, zone)
        val javaZone = java.util.TimeZone.getTimeZone(zone.id)
        val iterator = recurrence.iterator(
            DateTime(javaZone, event.startAt.toEpochMilliseconds()),
        )
        // Не мотаем серию с самого начала: пропускаем всё, что заведомо
        // закончилось до окна. fastForward корректно учитывает COUNT.
        iterator.fastForward(from.toEpochMilliseconds() - duration.inWholeMilliseconds)

        var iterations = 0
        while (iterator.hasNext() && iterations < MAX_ITERATIONS) {
            iterations++
            val start = Instant.fromEpochMilliseconds(iterator.nextMillis())
            if (start >= to) break
            val end = start + duration

            val exception = exceptionByOriginal[start]
            when {
                exception != null -> Unit // cancelled — выкинуто; moved — добавится вторым проходом
                end > from -> result += event.toOccurrence(start, end, originalStart = start)
            }
        }

        // Переносы обрабатываются отдельно от генерации: вхождение могло
        // уехать как из окна, так и В окно (original вне окна, new внутри).
        for (exception in exceptions) {
            if (exception.type != ExceptionType.MOVED) continue
            val newStart = exception.newStartAt ?: continue
            val newEnd = exception.newEndAt ?: (newStart + duration)
            if (newStart < to && newEnd > from) {
                result += event.toOccurrence(
                    newStart, newEnd,
                    isMoved = true,
                    originalStart = exception.originalDate,
                )
            }
        }
        return result
    }

    private fun EventEntity.toOccurrence(
        start: Instant,
        end: Instant,
        isMoved: Boolean = false,
        originalStart: Instant? = null,
    ) = Occurrence(
        eventId = id,
        title = title,
        comment = comment,
        type = type,
        start = start,
        end = end,
        isAllDay = isAllDay,
        studentId = studentId,
        enrollmentId = enrollmentId,
        color = color,
        isMoved = isMoved,
        originalStart = originalStart,
    )

    private companion object {
        /** Предохранитель от вечного цикла на некорректном правиле. */
        con