package com.sapraliev.studedu.ui.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sapraliev.studedu.data.local.AppDatabase
import com.sapraliev.studedu.data.local.entity.EventType
import com.sapraliev.studedu.data.local.entity.UniversityScheduleCacheEntity
import com.sapraliev.studedu.data.repository.EventRepository
import com.sapraliev.studedu.data.repository.NewRecurrence
import com.sapraliev.studedu.data.repository.ScheduleRepository
import com.sapraliev.studedu.data.repository.StudentsRepository
import com.sapraliev.studedu.data.schedule.MospolytechProvider
import com.sapraliev.studedu.data.settings.AppSettings
import com.sapraliev.studedu.domain.conflict.ConflictDetector
import com.sapraliev.studedu.domain.occurrence.Occurrence
import com.sapraliev.studedu.domain.schedule.LessonVisibility
import com.sapraliev.studedu.domain.schedule.LessonVisibilityFilter
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

/** Единая карточка ленты: личное событие или пара вуза. */
sealed interface ScheduleCard {
    val key: String
    val title: String
    val start: Instant
    val end: Instant
    val conflictTitles: List<String>

    data class Personal(
        val occurrence: Occurrence,
        override val conflictTitles: List<String>,
    ) : ScheduleCard {
        override val key get() = "p-${occurrence.eventId}-${occurrence.start}"
        override val title get() = occurrence.title
        override val start get() = occurrence.start
        override val end get() = occurrence.end
    }

    data class University(
        val lesson: UniversityScheduleCacheEntity,
        val dimmed: Boolean,
        override val conflictTitles: List<String>,
    ) : ScheduleCard {
        override val key get() = "u-${lesson.id}"
        override val title get() = lesson.subject
        override val start get() = lesson.startAt
        override val end get() = lesson.endAt
    }
}

data class DaySection(
    val date: LocalDate,
    val cards: List<ScheduleCard>,
)

data class TodayUiState(
    val selectedDate: LocalDate,
    val mode: ViewMode = ViewMode.DAY,
    val now: Instant,
    val days: List<DaySection> = emptyList(),
    val untilNext: Duration? = null,
    val nextTitle: String? = null,
    val universityGroup: String? = null,
)

/** Вариант «ученик × предмет» для привязки занятия. */
data class EnrollmentOption(
    val enrollmentId: String,
    val studentId: String,
    val label: String,
    val pricePerLesson: Double?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val eventRepository: EventRepository,
    private val scheduleRepository: ScheduleRepository,
    private val studentsRepository: StudentsRepository,
    private val settings: AppSettings,
) : ViewModel() {

    private val zone = TimeZone.currentSystemDefault()
    private val detector = ConflictDetector()
    private val visibilityFilter = LessonVisibilityFilter()

    private val selectedDate = MutableStateFlow(todayDate())
    private val mode = MutableStateFlow(ViewMode.DAY)

    private val ticker = flow {
        while (true) {
            emit(Clock.System.now())
            delay(60_000)
        }
    }

    /** Опции «ученик — предмет» для формы создания занятия. */
    val enrollmentOptions: StateFlow<List<EnrollmentOption>> =
        studentsRepository.observeOverview()
            .map { overviews ->
                overviews.flatMap { overview ->
                    overview.enrollments.map { enrollment ->
                        EnrollmentOption(
                            enrollmentId = enrollment.id,
                            studentId = overview.student.id,
                            label = "${overview.student.name} — ${enrollment.subject}",
                            pricePerLesson = enrollment.pricePerLesson,
                        )
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<TodayUiState> =
        combine(selectedDate, mode, settings.universityGroup) { date, m, group ->
            Triple(date, m, group)
        }
            .flatMapLatest { (date, m, group) ->
                val fromDate = if (m == ViewMode.DAY) date else mondayOf(date)
                val dayCount = if (m == ViewMode.DAY) 1 else 7
                val from = fromDate.atTime(0, 0).toInstant(zone)
                val to = fromDate.plus(DatePeriod(days = dayCount)).atTime(0, 0).toInstant(zone)

                val rulesFlow = if (group != null) {
                    scheduleRepository.observeHiddenRules(group)
                } else {
                    flowOf(emptyList())
                }

                combine(
                    eventRepository.observeOccurrences(from, to),
                    scheduleRepository.observeLessons(from, to),
                    rulesFlow,
                    ticker,
                ) { occurrences, lessons, rules, now ->
                    val groupLessons = lessons.filter { group != null &&