package com.sapraliev.studedu.domain.reminders

import com.sapraliev.studedu.data.local.entity.EventType
import com.sapraliev.studedu.data.local.entity.TaskEntity
import com.sapraliev.studedu.domain.occurrence.Occurrence
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/** Одно запланированное напоминание: когда сработает и что показать. */
data class PlannedReminder(
    val requestCode: Int,
    val triggerAt: Instant,
    val title: String,
    val body: String,
    /**
     * Напоминания о занятии минутно точны (за 30 мин / в момент начала) — обычный
     * `setExactAndAllowWhileIdle` на части устройств (агрессивный Doze/производитель)
     * всё равно откладывается на час-два. `AlarmManager.setAlarmClock` не подчиняется
     * Doze/App Standby вообще (тот же механизм, что у будильника в часах), поэтому
     * тиры занятия помечаются этим флагом; для дедлайнов/личных событий (точность
     * до дня) разница не критична — оставляем как есть, чтобы не плодить иконку
     * будильника в статус-баре на каждый дальний тир.
     */
    val alarmClock: Boolean = false,
)

/**
 * Дефолтные интервалы напоминаний — чистая функция без Android-зависимостей
 * (юнит-тестируется). Все интервалы фиксированы для v1: тонкая настройка
 * по конкретному событию — только вкл/выкл целиком ([remindersEnabled]),
 * подбор отдельных тиров — после появления экрана редактирования события.
 *
 * requestCode стабильно выводится из состава (тип/id[/старт вхождения]/тир),
 * а не из даты, — отмена уже запланированного будильника не ломается при
 * смене даты дедлайна или переносе вхождения (см. ScheduledReminderEntity).
 */
object ReminderPlanner {

    /** Дни до дедлайна, за которые шлём напоминание (плюс «в этот же день» = 0). */
    private val deadlineOffsetDays = listOf(30, 14, 7, 2, 0)

    /** Час дня, в который стреляют напоминания о дедлайнах без точного времени (задачи). */
    private const val DEADLINE_HOUR = 9

    fun forTask(task: TaskEntity, zone: TimeZone, now: Instant): List<PlannedReminder> {
        val due = task.dueDate ?: return emptyList()
        if (task.done || !task.remindersEnabled) return emptyList()
        return deadlineOffsetDays.mapIndexedNotNull { tierIndex, offsetDays ->
            val fireDate = due.minus(DatePeriod(days = offsetDays))
            val triggerAt = fireDate.atTime(DEADLINE_HOUR, 0).toInstant(zone)
            if (triggerAt <= now) return@mapIndexedNotNull null
            PlannedReminder(
                requestCode = requestCode(KIND_TASK, task.id, tierIndex),
                triggerAt = triggerAt,
                title = "Дедлайн: ${task.text}",
                body = deadlineBody(offsetDays),
            )
        }
    }

    fun forOccurrence(occurrence: Occurrence, zone: TimeZone, now: Instant): List<PlannedReminder> {
        if (!occurrence.remindersEnabled) return emptyList()
        val occurrenceKey = occurrence.originalStart ?: occurrence.start

        val tiers: List<Pair<Int, Instant>> = when (occurrence.type) {
            EventType.LESSON -> listOf(
                0 to occurrence.start - 30.minutes,
                1 to occurrence.start,
            )
            EventType.PERSONAL -> listOf(0 to occurrence.start - 1.days)
            EventType.DEADLINE -> deadlineOffsetDays.mapIndexed { tierIndex, offsetDays ->
                tierIndex to occurrence.start - offsetDays.days
            }
        }

        return tiers.mapNotNull { (tierIndex, triggerAt) ->
            if (triggerAt <= now) return@mapNotNull null
            PlannedReminder(
                requestCode = requestCode(KIND_EVENT, "${occurrence.eventId}|${occurrenceKey.epochSeconds}", tierIndex),
                triggerAt = triggerAt,
                title = titleFor(occurrence),
                body = bodyFor(occurrence, tierIndex),
                alarmClock = occurrence.type == EventType.LESSON,
            )
        }
    }

    private fun titleFor(occurrence: Occurrence): String = when (occurrence.type) {
        EventType.LESSON -> "Занятие: ${occurrence.title}"
        EventType.DEADLINE -> "Дедлайн: ${occurrence.title}"
        EventType.PERSONAL -> occurrence.title
    }

    private fun bodyFor(occurrence: Occurrence, tierIndex: Int): String = when (occurrence.type) {
        EventType.LESSON -> if (tierIndex == 0) "через 30 минут" else "начинается сейчас"
        EventType.DEADLINE -> deadlineBody(deadlineOffsetDays[tierIndex])
        EventType.PERSONAL -> "завтра в это же время"
    }

    private fun deadlineBody(offsetDays: Int): String = when (offsetDays) {
        30 -> "через месяц"
        14 -> "через 2 недели"
        7 -> "через неделю"
        2 -> "через 2 дня"
        else -> "сегодня"
    }

    private const val KIND_TASK = "task"
    private const val KIND_EVENT = "event"

    private fun requestCode(kind: String, id: String, tierIndex: Int): Int =
        "$kind|$id|$tierIndex".hashCode()
}
