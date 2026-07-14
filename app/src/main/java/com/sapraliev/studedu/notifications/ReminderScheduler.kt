package com.sapraliev.studedu.notifications

import android.content.Context
import com.sapraliev.studedu.data.local.dao.EventDao
import com.sapraliev.studedu.data.local.dao.ReminderDao
import com.sapraliev.studedu.data.local.dao.TaskDao
import com.sapraliev.studedu.data.local.entity.ScheduledReminderEntity
import com.sapraliev.studedu.domain.occurrence.OccurrenceGenerator
import com.sapraliev.studedu.domain.reminders.PlannedReminder
import com.sapraliev.studedu.domain.reminders.ReminderPlanner
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

/**
 * Пересчитывает окно напоминаний на ближайшие [windowDays] и приводит
 * будильники `AlarmManager` в соответствие: лишние (пропавшие из нового
 * расчёта — событие удалено/отменено/дедлайн снят) отменяются, новые —
 * планируются. Вызывается при любой правке события/задачи и раз в сутки
 * фоново ([ReminderRefreshWorker]), чтобы окно не «протухало», если
 * приложение долго не открывали.
 */
class ReminderScheduler(
    private val context: Context,
    private val eventDao: EventDao,
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val generator: OccurrenceGenerator = OccurrenceGenerator(),
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) {

    suspend fun refresh(windowDays: Int = 35) {
        val now = Clock.System.now()
        val windowEnd = now.plus(DateTimePeriod(days = windowDays), zone)

        val tasks = taskDao.getRemindableDeadlines()
        val taskReminders = tasks.flatMap { ReminderPlanner.forTask(it, zone, now) }

        val events = eventDao.getEventsAroundOnce(now, windowEnd)
        val rules = eventDao.getAllRulesOnce().associateBy { it.id }
        val exceptions = eventDao.getAllExceptionsOnce()
        val occurrences = generator.generate(events, rules, exceptions, now, windowEnd)
        val eventReminders = occurrences.flatMap { ReminderPlanner.forOccurrence(it, zone, now) }

        val desired = (taskReminders + eventReminders).associateBy { it.requestCode }
        val previous = reminderDao.getAll().associateBy { it.requestCode }

        for (code in previous.keys - desired.keys) {
            ReminderAlarmScheduler.cancel(context, code)
        }
        for ((code, reminder) in desired) {
            val prev = previous[code]
            if (prev == null || prev.triggerAtEpochMillis != reminder.triggerAt.toEpochMilliseconds()) {
                ReminderAlarmScheduler.schedule(context, reminder)
            }
        }

        reminderDao.replaceAll(desired.values.map { it.toEntity() })
    }

    private fun PlannedReminder.toEntity() = ScheduledReminderEntity(
        requestCode = requestCode,
        triggerAtEpochMillis = triggerAt.toEpochMilliseconds(),
        title = title,
        body = body,
    )
}
