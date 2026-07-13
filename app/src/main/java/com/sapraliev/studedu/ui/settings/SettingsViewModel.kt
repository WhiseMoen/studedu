package com.sapraliev.studedu.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sapraliev.studedu.core.AppGraph
import com.sapraliev.studedu.data.local.dao.StudentDao
import com.sapraliev.studedu.data.local.entity.HiddenLessonRuleEntity
import com.sapraliev.studedu.data.repository.ScheduleRepository
import com.sapraliev.studedu.data.settings.AppSettings
import com.sapraliev.studedu.data.settings.ThemeMode
import com.sapraliev.studedu.domain.schedule.ScheduleSyncException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

enum class SyncStatus { IDLE, RUNNING, SUCCESS, ERROR }

/** Сводная статистика по всем ученикам. */
data class StatsBlock(
    val monthCharged: Double = 0.0,
    val monthPaid: Double = 0.0,
    val monthLessons: Int = 0,
    val prevCharged: Double = 0.0,
    val prevPaid: Double = 0.0,
    val prevLessons: Int = 0,
    /** Топ учеников месяца по оплатам: имя → сумма. */
    val topStudents: List<Pair<String, Double>> = emptyList(),
)

data class SettingsUiState(
    val groups: List<String> = emptyList(),
    val activeGroup: String? = null,
    val lastSyncAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val syncError: String? = null,
    val hiddenRules: List<HiddenLessonRuleEntity> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lkUrl: String = AppSettings.DEFAULT_LK,
    val sdoUrl: String = AppSettings.DEFAULT_SDO,
    val stats: StatsBlock = StatsBlock(),
)

class SettingsViewModel(
    private val settings: AppSettings,
    private val scheduleRepository: ScheduleRepository,
    private val studentDao: StudentDao,
) : ViewModel() {

    private val syncStatus = MutableStateFlow(SyncStatus.IDLE)
    private val syncError = MutableStateFlow<String?>(null)

    private data class GroupsPart(
        val groups: List<String>,
        val active: String?,
        val lastSync: Long?,
        val status: SyncStatus,
        val error: String?,
    )

    private val groupsPart = combine(
        settings.groups,
        settings.universityGroup,
        settings.lastSyncAt,
        syncStatus,
        syncError,
        ::GroupsPart,
    )

    private data class MiscPart(
        val hidden: List<HiddenLessonRuleEntity>,
        val theme: ThemeMode,
        val lk: String,
        val sdo: String,
    )

    private val miscPart = combine(
        scheduleRepository.observeAllHiddenRules(),
        settings.themeMode,
        settings.lkUrl,
        settings.sdoUrl,
        ::MiscPart,
    )

    private val statsPart: kotlinx.coroutines.flow.Flow<StatsBlock> = run {
        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(zone)
        val monthStart = LocalDate(today.year, today.monthNumber, 1)
        val monthEnd = monthStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        val prevStart = monthStart.minus(DatePeriod(months = 1))
        val prevEnd = monthStart.minus(DatePeriod(days = 1))

        val base = combine(
            studentDao.observePeriodTotals(monthStart, monthEnd),
            studentDao.observePeriodTotals(prevStart, prevEnd),
            studentDao.observeLessonsCount(monthStart, monthEnd),
            studentDao.observeLessonsCount(prevStart, prevEnd),
        ) { month, prev, monthCount, prevCount ->
            StatsBlock(
                monthCharged = month.charged,
                monthPaid = month.paid,
                monthLessons = monthCount,
                prevCharged = prev.charged,
                prevPaid = prev.paid,
                prevLessons = prevCount,
            )
        }
        combine(
            base,
            studentDao.observeTopPaying(monthStart, monthEnd, 3),
            studentDao.observeAllStudents(),
        ) { stats, top, students ->
            val names = students.associate { it.id to it.name }
            stats.copy(
                topStudents = top.map { (names[it.studentId] ?: "?") to it.paid },
            )
        }
    }

    val uiState: StateFlow<SettingsUiState> =
        combine(groupsPart, miscPart, statsPart) { g, m, stats ->
            SettingsUiState(
                groups = g.groups,
                activeGroup = g.active,
                lastSyncAt = g.lastSync,
                syncStatus = g.status,
                syncError = g.error,
                hiddenRules = m.hidden,
                themeMode = m.theme,
                lkUrl = m.lk,
                sdoUrl = m.sdo,
                stats = stats,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    // ---------- группы ----------

    /** Добавляет группу, делает её активной и сразу синкает. */
    fun addGroup(group: String) {
        val normalized = group.trim()
        if (normalized.isEmpty()) return
        settings.addGroup(normalized)
        syncNow()
    }

    fun switchGroup(group: String) {
        settings.setActiveGroup(group)
        syncStatus.value = SyncStatus.IDLE
        syncError.value = null
    }

    fun removeGroup(group: String) {
        settings.removeGroup(group)
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

    // ---------- прочее ----------

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)

    fun saveLinks(lk: String, sdo: String) {
        settings.setLkUrl(lk)
        settings.setSdoUrl(sdo)
    }

    fun removeHiddenRule(rule: HiddenLessonRuleEntity) {
        viewModelScope.launch {
            scheduleRepository.removeHiddenRule(rule)
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settings = AppGraph.settings,
                    scheduleRepository = AppGraph.scheduleRepository,
                    studentDao = AppGraph.database.studentDao(),
                )
            }
        }
    }
}
