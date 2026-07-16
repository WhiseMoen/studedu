package com.sapraliev.studedu.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.sapraliev.studedu.MainActivity
import com.sapraliev.studedu.domain.reminders.PlannedReminder

/**
 * Тонкая обёртка над `AlarmManager`: планирует/отменяет один будильник на
 * [PlannedReminder.requestCode]. Заголовок и текст кладутся прямо в extras
 * интента — `ReminderReceiver` не обращается к базе в момент срабатывания.
 */
object ReminderAlarmScheduler {

    const val EXTRA_REQUEST_CODE = "request_code"
    const val EXTRA_TITLE = "title"
    const val EXTRA_BODY = "body"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService<AlarmManager>() ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun schedule(context: Context, reminder: PlannedReminder) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent = pendingIntentFor(context, reminder)
        val triggerAtMillis = reminder.triggerAt.toEpochMilliseconds()
        try {
            if (reminder.alarmClock && canScheduleExact(context)) {
                // Занятие: setAlarmClock не подчиняется Doze/App Standby вообще
                // (в отличие от setExactAndAllowWhileIdle, который на части
                // устройств/бакетов всё равно откладывается на час-два).
                val showIntent = PendingIntent.getActivity(
                    context,
                    reminder.requestCode,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                    pendingIntent,
                )
            } else if (canScheduleExact(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Право на точные будильники отозвали между проверкой и вызовом — пропускаем тир.
        }
    }

    fun cancel(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun pendingIntentFor(context: Context, reminder: PlannedReminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_REQUEST_CODE, reminder.requestCode)
            putExtra(EXTRA_TITLE, reminder.title)
            putExtra(EXTRA_BODY, reminder.body)
        }
        return PendingIntent.getBroadcast(
            context,
            reminder.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
