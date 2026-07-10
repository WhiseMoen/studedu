package com.sapraliev.studedu.ui.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sapraliev.studedu.data.local.AppDatabase
import com.sapraliev.studedu.data.repository.EventRepository
import com.sapraliev.studedu.data.repository.NewRecurrence
import com.sapraliev.studedu.data.local.entity.EventType
import com.sapraliev.studedu.domain.conflict.ConflictDetector
import com.sapraliev.studedu.domain.occurrence.Occurrence
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

enum class ViewMode { DAY, WEEK }

data class TodayItem(
    val occurrence: Occurrence,
    /** С чем пересекается (заголовки) — пусто, если конфликтов нет. */
    val conflictTitles: List<String>,
)

data class DaySection(
    val date: LocalDate,
    val items: List<TodayItem>,
)

data class TodayUiState(
    val selectedDate: LocalDate,
    val mode: ViewMode = ViewMode.DAY,
    val now: Instant,
    val days: List<DaySection> = emptyList(),
    /** До ближайшего будущего события в загруженном окне. */
    val untilNext: Duration? = null,
    val nextTitle: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: EventRepository,
) : ViewModel() {

    private val zone = TimeZone.currentSystemDefault()
    private val detector = ConflictDetector()

    private val selectedDate = MutableStateFlow(todayDate())
    private val mode = MutableStateFlow(ViewMode.DAY)

    /** Минутный тикер: часы, линия «сейчас», «до события». */
    private val ticker = flow {
        while (true) {
            emit(Clock.System.now())
            delay(60_000)
        }
    }

    val uiState: StateFlow<TodayUiState> =
        combine(selectedDate, mode) { date, m -> date to m }
            .flatMapLatest { (date, m) ->
                val fromDate = if (m == ViewMode.DAY) date else mondayOf(date)
                val dayCount = if (m == ViewMode.DAY) 1 else 7
                val from = fromDate.atTime(0, 0).toInstant(zone)
                val to = fromDate.plus(DatePeriod(days = dayCount)).atTime(0, 0).toInstant(zone)

                combine(repository.observeOccurrences(from, to), ticker) { occurrences, now ->
                    buildState(date, m, fromDate, dayCount, occurrences, now)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TodayUiState(selectedDate = todayDate(), now = Clock.System.now()),
            )

    private fun buildState(
        date: LocalDate,
        mode: ViewMode,
        fromDate: LocalDate,
        dayCount: Int,
        occurrences: List<Occurrence>,
        now: Instant,
    ): TodayUiState {
        val timed = occurrences.filter { !it.isAllDay }
        val conflicts = detector.findConflicts(timed, { it.start }, { it.end })

        val days = (0 until dayCount).map { offset ->
            val day = fromDate.plus(DatePeriod(days = offset))
            val items = occurrences
                .filter { it.start.toLocalDateTime(zone).date == day }
                .map { occ ->
                    TodayItem(
                        occurrence = occ,
                        conflictTitles = conflicts[occ]?.map { it.title } ?: emptyList(),
                    )
                }
            DaySection(day, items)
        }

        val next = occurrences.filter { it.start > now }.minByOrNull { it.start }
        return TodayUiState(
            selectedDate = date,
            mode = mode,
            now = now,
            days = days,
            untilNext = next?.let { it.start - now },
            nextTitle = next?.title,
        )
    }

    fun setMode(newMode: ViewMode) {
        mode.value = newMode
    }

    fun shiftDate(days: Int) {
        selectedDate.value = selectedDate.value.plus(DatePeriod(days = days))
    }

    fun goToday() {
        selectedDate.value = todayDate()
    }

    fun createEvent(
        title: String,
        comment: String?,
        type: EventType,
        start: Instant,
        end: Instant,
        recurrence: NewRecurrence?,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createEvent(title, comment, type, start, end, recurrence)
        }
    }

    fun cancelOccurrence(item: TodayItem) {
        val original = item.occurrence.originalStart ?: return
        viewModelScope.launch {
            repository.cancelOccurrence(item.occurrence.eventId, original)
        }
    }

    fun deleteEvent(item: TodayItem) {
        viewModelScope.launch {
            repository.deleteEvent(item.occurrence.eventId)
        }
    }

    private fun todayDate(): LocalDate =
        Clock.System.now().toLocalDateTime(zone).date

    private fun mondayOf(date: LocalDate): LocalDate =
        date.minus(DatePeriod(days = date.dayOfWeek.ordinal))

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val db = AppDatabase.get(context.applicationContext)
                TodayViewModel(EventRepository(db.eventDao()))
            }
        }
    }
}
