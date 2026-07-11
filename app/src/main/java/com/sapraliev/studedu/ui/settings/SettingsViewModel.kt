package com.sapraliev.studedu.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sapraliev.studedu.data.local.AppDatabase
import com.sapraliev.studedu.data.local.entity.HiddenLessonRuleEntity
import com.sapraliev.studedu.data.repository.ScheduleRepository
import com.sapraliev.studedu.data.schedule.MospolytechProvider
import com.sapraliev.studedu.data.settings.AppSettings
import com.sapraliev.studedu.domain.schedule.ScheduleSyncException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

enum class SyncStatus { IDLE, RUNNING, SUCCESS, ERROR }

data class SettingsUiState(
    val group: String? = null,
    val lastSyncAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val syncError: String? = null,
    val hiddenRules: List<HiddenLessonRuleEntity> = emptyList(),
)

class SettingsViewModel(
    private val settings: AppSettings,
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {

    private val syncStatus = MutableStateFlow(SyncStatus.IDLE)
    private val syncError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.universityGroup,
        settings.lastSyncAt,
        syncStatus,
        syncError,
        scheduleRepository.observeAllHiddenRules(),
    ) { group, lastSync, status, error, rules ->
        SettingsUiState(
            group = group,
            lastSyncAt = lastSync,
            syncStatus = status,
            syncError = error,
            hiddenRules = rules,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun saveGroup(group: String) {
        settings.setUniversityGroup(group)
        syncStatus.value = SyncStatus.IDLE
        syncError.value = null
    }

    fun syncNow() {
        val group = settings.universityGroup.value ?: return
        if (syncStatus.value == SyncStatus.RUNNING) return
        syncStatus.value = SyncStatus.RUNNING
        syncError.value = null
        viewModelScope.launch {
            try {
                scheduleRepository.syncNow(group)
                settings.setLastSyncAt(Clock.System.now().toEpochMilliseconds())
                syncStatus.value = SyncStatus.SUCCESS
            } catch (e: ScheduleSyncException) {
                syncStatus.value = SyncStatus.ERROR
                syncError.value = e.message
            } catch (e: Exception) {
                syncStatus.value = SyncStatus.ERROR
                syncError.value = "Неожиданная ошибка: ${e.message}"
            }
        }
    }

    fun removeHiddenRule(rule: HiddenLessonRuleEntity) {
        viewModelScope.launch {
            scheduleRepository.removeHiddenRule(rule)
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = context.applicationContext
                val db = AppDatabase.get(app)
                SettingsViewModel(
                    settings = AppSettings.get(app),
                    scheduleRepository = ScheduleRepository(
                        provider = MospolytechProvider(),
                        cacheDao = db.scheduleCacheDao(),
                        hiddenLessonDao = db.hiddenLessonDao(),
                    ),
                )
            }
        }
    }
}
