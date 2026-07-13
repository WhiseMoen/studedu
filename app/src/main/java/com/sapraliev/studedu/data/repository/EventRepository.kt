package com.sapraliev.studedu.data.repository

import com.sapraliev.studedu.data.local.dao.EventDao
import com.sapraliev.studedu.data.local.entity.EventEntity
import com.sapraliev.studedu.data.local.entity.EventType
import com.sapraliev.studedu.data.local.entity.ExceptionType
import com.sapraliev.studedu.data.local.entity.RecurrenceExceptionEntity
import com.sapraliev.studedu.data.local.entity.RecurrenceFreq
import com.sapraliev.studedu.data.local.entity.RecurrenceRuleEntity
import com.sapraliev.studedu.domain.occurrence.Occurrence
import com.sapraliev.studedu.domain.occurrence.OccurrenceGenerator
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** Параметры повторения при создании события. */
data class NewRecurrence(
    val freq: RecurrenceFreq,
    val interval: Int = 1,
    /** ["MO","WE"] — для weekly. */
    val byweekday: List<String>? = null,
    val count: Int? = null,
    val until: LocalDate? = null,
)

/**
 * Репозиторий событий: Room — источник правды, вхождения серий
 * вычисляются на лету генератором. Синк с Supabase появится на Этапе 5
 * и будет жить рядом, не меняя этот контракт.
 */
class EventRepository(
    private val eventDao: EventDao,
    private val generator: OccurrenceGenerator = OccurrenceGenerator(),
) {

    /** Реактивные вхождения для окна дат: правки в базе мгновенно обновляют ленту. */
    fun observeOccurrences(from: Instant, to: Instant): Flow<List<Occurrence>> =
        combine(
            eventDao.observeEventsAround(from, to),
            eventDao.observeAllRules(),
            eventDao.observeAllExceptions(),
        ) { events, rules, exceptions ->
            generator.generate(
                events = events,
                rules = rules.associateBy { it.id },
                exceptions = exceptions,
                from = from,
                to = to,
            )
        }

    suspend fun createEvent(
        title: String,
        comment: String?,
        type: EventType,
        start: Instant,
        end: Instant,
        recurrence: NewRecurrence? = null,
        studentId: String? = null,
        enrollmentId: String? = null,
    ) {
        val now = Clock.System.now()
        val eventId = UUID.randomUUID().toString()
        val event = EventEntity(
            id = eventId,
            userId = LOCAL_USER_ID,
            title = title.trim(),
            comment = comment?.trim()?.takeIf { it.isNotEmpty() },
            type = type,
            startAt = start,
            endAt = end,
            isAllDay = false,
            studentId = studentId,
            enrollmentId = enrollmentId,
            recurrenceRuleId = null,
            color = null,
            source = null,
            createdAt = now,
            updatedAt = now,
        )
        if (recurrence == null) {
            eventDao.upsertEvent(event)
        } else {
            val rule = RecurrenceRuleEntity(
                id = UUID.randomUUID().toString(),
                userId = LOCAL_USER_ID,
                freq = recurrence.freq,
                interval = recurrence.interval,
                byweekday = recurrence.byweekday?.takeIf { it.isNotEmpty() },
                count = recurrence.count,
                until = recurrence.until,
                createdAt = now,
                updatedAt = now,
            )
            eventDao.insertEventWithRule(rule, event.copy(recurrenceRuleId = rule.id))
        }
    }

    /** «Отменить только это вхождение» серии. */
    suspend fun cancelOccurrence(eventId: String, originalStart: Instant) {
        eventDao.upsertException(
            RecurrenceExceptionEntity(
                id = UUID.randomUUID().toString(),
                userId = LOCAL_USER_ID,
                eventId = eventId,
                originalDate = originalStart,
                type = ExceptionType.CANCELLED,
                newStartAt = null,
                newEndAt = null,
                createdAt = Clock.System.now(),
            ),
        )
    }

    /** «Перенести только это вхождение» серии. */
    suspend fun moveOccurrence(
        eventId: String,
        originalStart: Instant,
        newStart: Instant,
        newEnd: Instant,
    ) {
        eventDao.upsertException(
            RecurrenceExceptionEntity(
                id = UUID.randomUUID().toString(),
                userId = LOCAL_USER_ID,
                eventId = eventId,
                originalDate = originalStart,
                type = ExceptionType.MOVED,
                newStartAt = newStart,
                newEndAt = newEnd,
                createdAt = Clock.System.now(),
            ),
        )
    }

    /** Удалить событие целиком (для серии — всю серию, исключения уйдут каскадом). */
    suspend fun deleteEvent(eventId: String) {
        eventDao.getEventById(eventId)?.let { eventDao.deleteEvent(it) }
    }

    companion object {
        /**
         * До появления авторизации (Этап 5) все строки принадлежат
         * локальному пользователю; при первом входе синк проставит
         * реальный uuid из Supabase Auth.
         */
        const val LOCAL_USER_ID = "local"
    }
}
