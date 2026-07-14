package com.sapraliev.studedu.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.sapraliev.studedu.core.AppGraph
import java.util.concurrent.TimeUnit

/**
 * Раз в сутки сдвигает окно запланированных напоминаний вперёд — без
 * этого будильники за пределами текущего окна ([ReminderScheduler])
 * никогда бы не появились, если приложение долго не открывали.
 */
class ReminderRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        AppGraph.init(applicationContext)
        AppGraph.reminderScheduler.refresh()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "reminder-refresh"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderRefreshWorker>(24, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
