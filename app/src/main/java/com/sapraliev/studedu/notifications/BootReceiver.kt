package com.sapraliev.studedu.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sapraliev.studedu.core.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** `AlarmManager` теряет все будильники при перезагрузке — восстанавливаем окно. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppGraph.init(context)
                AppGraph.reminderScheduler.refresh()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
