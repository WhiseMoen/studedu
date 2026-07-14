package com.sapraliev.studedu.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

/** Единственный канал напоминаний — тонкая настройка важности живёт в системных настройках. */
object NotificationChannels {

    const val REMINDERS_CHANNEL_ID = "reminders"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            REMINDERS_CHANNEL_ID,
            "Напоминания",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Дедлайны, занятия и события из StudEdu"
        }
        manager.createNotificationChannel(channel)
    }
}
