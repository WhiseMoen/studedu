package com.sapraliev.studedu.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Учёт уже запланированных в `AlarmManager` напоминаний — только для
 * диффа при пересчёте окна планирования ([ReminderScheduler]): что
 * запланировано сейчас, но пропало из нового расчёта, — отменяется.
 *
 * [requestCode] стабильно выводится из (тип, id задачи/события [+ старт
 * вхождения], номер тира) — см. `ReminderPlanner` — не зависит от даты,
 * поэтому отмена работает даже если исходная дата уже изменилась.
 */
@Entity(tableName = "scheduled_reminders")
data class ScheduledReminderEntity(
    @PrimaryKey @ColumnInfo(name = "request_code") val requestCode: Int,
    @ColumnInfo(name = "trigger_at") val triggerAtEpochMillis: Long,
    val title: String,
    val body: String,
)
