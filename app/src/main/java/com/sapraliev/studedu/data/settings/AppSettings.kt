package com.sapraliev.studedu.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Простые настройки приложения (SharedPreferences).
 * DataStore не тянем: настроек мало, а зависимость лишняя.
 */
class AppSettings private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("studedu_settings", Context.MODE_PRIVATE)

    private val _universityGroup = MutableStateFlow(prefs.getString(KEY_GROUP, null))
    val universityGroup: StateFlow<String?> = _universityGroup

    private val _lastSyncAt = MutableStateFlow(
        prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 },
    )

    /** Epoch millis последнего успешного синка расписания. */
    val lastSyncAt: StateFlow<Long?> = _lastSyncAt

    fun setUniversityGroup(group: String?) {
        val normalized = group?.trim()?.takeIf { it.isNotEmpty() }
        prefs.edit().putString(KEY_GROUP, normalized).apply()
        _universityGroup.value = normalized
    }

    fun setLastSyncAt(epochMillis: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, epochMillis).apply()
        _lastSyncAt.value = epochMillis
    }

    companion object {
        private const val KEY_GROUP = "university_group"
        private const val KEY_LAST_SYNC = "last_schedule_sync"

        @Volatile
        private var instance: AppSettings? = null

        fun get(context: Context): AppSettings =
            instance ?: synchronized(this) {
                instance ?: AppSettings(context.applicationContext).also { instance = it }
            }
    }
}
