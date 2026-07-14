package com.sapraliev.studedu.core

import android.content.Context
import com.sapraliev.studedu.data.local.AppDatabase
import com.sapraliev.studedu.data.repository.EventRepository
import com.sapraliev.studedu.data.repository.ScheduleRepository
import com.sapraliev.studedu.data.repository.StudentsRepository
import com.sapraliev.studedu.data.schedule.MospolytechProvider
import com.sapraliev.studedu.data.settings.AppSettings
import com.sapraliev.studedu.notifications.NotificationChannels
import com.sapraliev.studedu.notifications.ReminderRefreshWorker
import com.sapraliev.studedu.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Композиционный корень приложения (сервис-локатор).
 *
 * Один экземпляр базы, настроек, HTTP-клиента и репозиториев на всё
 * приложение — вместо пересоздания в каждой ViewModel-фабрике.
 * Если появится DI (Hilt/Koin), меняется только этот файл.
 */
object AppGraph {

    @Volatile
    private var appContext: Context? = null

    private fun ctx(): Context =
        requireNotNull(appContext) { "AppGraph.init(context) не вызван" }

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext

        NotificationChannels.ensureCreated(ctx())
        ReminderRefreshWorker.enqueue(ctx())
        CoroutineScope(Dispatchers.IO).launch { reminderScheduler.refresh() }
    }

    val database: AppDatabase by lazy { AppDatabase.get(ctx()) }

    val settings: AppSettings by lazy { AppSettings.get(ctx()) }

    val eventRepository: EventRepository by lazy {
        EventRepository(database.eventDao())
    }

    val scheduleRepository: ScheduleRepository by lazy {
        ScheduleRepository(
            provider = MospolytechProvider(),
            cacheDao = database.scheduleCacheDao(),
            hiddenLessonDao = database.hiddenLessonDao(),
        )
    }

    val studentsRepository: StudentsRepository by lazy {
        StudentsRepository(database.studentDao(), database.enrollmentDao())
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(ctx(), database.eventDao(), database.taskDao(), database.reminderDao())
    }
}
