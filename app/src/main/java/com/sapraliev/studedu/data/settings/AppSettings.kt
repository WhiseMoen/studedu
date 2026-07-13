package com.sapraliev.studedu.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Режим темы приложения. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Простые настройки приложения (SharedPreferences).
 * DataStore не тянем: настроек мало, а зависимость лишняя.
 */
class AppSettings private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("studedu_settings", Context.MODE_PRIVATE)

    // ---------- группы вуза ----------

    private val _groups = MutableStateFlow(loadGroups())

    /** Сохранённые группы (своя + потока и т.п.), в порядке добавления. */
    val groups: StateFlow<List<String>> = _groups

    private val _universityGroup = MutableStateFlow(prefs.getString(KEY_ACTIVE_GROUP, null))

    /** Активная группа — её пары показывает лента. */
    val universityGroup: StateFlow<String?> = _universityGroup

    fun addGroup(group: String) {
        val normalized = group.trim()
        if (normalized.isEmpty()) return
        val updated = (_groups.value + normalized).distinct()
        persistGroups(updated)
        setActiveGroup(normalized)
    }

    fun removeGroup(group: String) {
        val updated = _groups.value - group
        persistGroups(updated)
        if (_universityGroup.value == group) {
            setActiveGroup(updated.firstOrNull())
        }
    }

    fun setActiveGroup(group: String?) {
        prefs.edit().putString(KEY_ACTIVE_GROUP, group).apply()
        _universityGroup.value = group
    }

    private fun loadGroups(): List<String> =
        prefs.getString(KEY_GROUPS, null)
            ?.split('\n')
            ?.filter { it.isNotBlank() }
            ?: legacyGroup()

    private fun legacyGroup(): List<String> =
        prefs.getString(KEY_LEGACY_GROUP, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { listOf(it) }
            ?: emptyList()

    private fun persistGroups(groups: List<String>) {
        prefs.edit().putString(KEY_GROUPS, groups.joinToString("\n")).apply()
        _groups.value = groups
    }

    // ---------- синк ----------

    private val _lastSyncAt = MutableStateFlow(
        prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 },
    )

    /** Epoch millis последнего успешного синка расписания. */
    val lastSyncAt: StateFlow<Long?> = _lastSyncAt

    fun setLastSyncAt(epochMillis: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, epochMillis).apply()
        _lastSyncAt.value = epochMillis
    }

    // ---------- тема ----------

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }
            .getOrDefault(ThemeMode.SYSTEM),
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    // ---------- ссылки вуза (ЛК и СДО) ----------

    private val _lkUrl = MutableStateFlow(prefs.getString(KEY_LK_URL, DEFAULT_LK) ?: DEFAULT_LK)

    /** Личный кабинет вуза. */
    val lkUrl: StateFlow<String> = _lkUrl

    private val _sdoUrl = MutableStateFlow(prefs.getString(KEY_SDO_URL, DEFAULT_SDO) ?: DEFAULT_SDO)

    /** СДО / LMS вуза. */
    val sdoUrl: StateFlow<String> = _sdoUrl

    fun setLkUrl(url: String) {
        val normalized = normalizeUrl(url) ?: return
        prefs.edit().putString(KEY_LK_URL, normalized).apply()
        _lkUrl.value = normalized
    }

    fun setSdoUrl(url: String) {
        val normalized = normalizeUrl(url) ?: return
        prefs.edit().putString(KEY_SDO_URL, normalized).apply()
        _sdoUrl.value = normalized
    }

    private fun normalizeUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    companion object {
        private const val KEY_LEGACY_GROUP = "university_group"
        private const val KEY_GROUPS = "university_groups"
        private const val KEY_ACTIVE_GROUP = "active_group"
        private const val KEY_LAST_SYNC = "last_schedule_sync"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LK_URL = "university_lk_url"
        private const val KEY_SDO_URL = "university_sdo_url"

        /** Дефолты Московского Политеха; для другого вуза меняются в настройках. */
        const val DEFAULT_LK = "https://e.mospolytech.ru/"
        const val DEFAULT_SDO = "https://online.mospolytech.ru/"

        @Volatile
        private var instance: AppSettings? = null

        fun get(context: Context): AppSettings =
            instance ?: synchronized(this) {
                instance ?: AppSettings(context.applicationContext).also { instance = it }
            }
    }
}
