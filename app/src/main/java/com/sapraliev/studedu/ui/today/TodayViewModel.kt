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

enum class ViewMode { DAY, WEEK, MONTH }

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

    /**
     * Тяжёлая часть (генерация вхождений, видимость пар, детектор конфликтов)
     * зависит только от диапазона дат и данных репозиториев — не от `now`.
     * Отдельный StateFlow, чтобы минутный тикер не пересчитывал конфликты.
     */
    private data class TodayContent(
        val date: LocalDate,
        val mode: ViewMode,
        val group: String?,
        val days: List<DaySection>,
        val cards: List<ScheduleCard>,
    )

    private val content: StateFlow<TodayContent> =
        combine(selectedDate, mode, settings.universityGroup) { date, m, group ->
            Triple(date, m, group)
        }
            .flatMapLatest { (date, m, group) ->
                val fromDate = when (m) {
                    ViewMode.DAY -> date
                    ViewMode.WEEK -> mondayOf(date)
                    ViewMode.MONTH -> firstDayOfMonth(date)
                }
                val dayCount = when (m) {
                    ViewMode.DAY -> 1
                    ViewMode.WEEK -> 7
                    ViewMode.MONTH -> daysInMonth(date)
                }
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
                ) { occurrences, lessons, rules ->
                    val groupLessons = lessons.filter { group != null && it.group == group }
                    buildContent(date, m, fromDate, dayCount, group, occurrences, groupLessons, rules)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TodayContent(todayDate(), ViewMode.DAY, group = null, days = emptyList(), cards = emptyList()),
            )

    val uiState: StateFlow<TodayUiState> =
        combine(content, ticker) { c, now -> buildUiState(c, now) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TodayUiState(selectedDate = todayDate(), now = Clock.System.now()),
            )

    private fun buildContent(
        date: LocalDate,
        mode: ViewMode,
        fromDate: LocalDate,
        dayCount: Int,
        group: String?,
        occurrences: List<Occurrence>,
        lessons: List<UniversityScheduleCacheEntity>,
        rules: List<com.sapraliev.studedu.data.local.entity.HiddenLessonRuleEntity>,
    ): TodayContent {
        // Видимость пар: HIDDEN выпадают совсем, DIMMED видны, но без конфликтов.
        val visibleLessons = mutableListOf<UniversityScheduleCacheEntity>()
        val dimmedLessons = mutableListOf<UniversityScheduleCacheEntity>()
        for (lesson in lessons) {
            when (visibilityFilter.visibilityFor(lesson.subject, lesson.lessonType, rules)) {
                LessonVisibility.VISIBLE -> visibleLessons += lesson
                LessonVisibility.DIMMED -> dimmedLessons += lesson
                LessonVisibility.HIDDEN -> Unit
            }
        }

        // Конфликты: личные (кроме «весь день») + видимые пары.
        val participants: List<Any> =
            occurrences.filter { !it.isAllDay } + visibleLessons
        val conflicts = detector.findConflicts(participants, ::startOf, ::endOf)

        fun titlesFor(item: Any): List<String> =
            conflicts[item]?.map(::titleOf) ?: emptyList()

        val cards: List<ScheduleCard> =
            occurrences.map { ScheduleCard.Personal(it, titlesFor(it)) } +
                visibleLessons.map { ScheduleCard.University(it, dimmed = false, titlesFor(it)) } +
                dimmedLessons.map { ScheduleCard.University(it, dimmed = true, emptyList()) }

        val days = (0 until dayCount).map { offset ->
            val day = fromDate.plus(DatePeriod(days = offset))
            DaySection(
                date = day,
                cards = cards
                    .filter { it.start.toLocalDateTime(zone).date == day }
                    .sortedBy { it.start },
            )
        }

        return TodayContent(date = date, mode = mode, group = group, days = days, cards = cards)
    }

    private fun buildUiState(content: TodayContent, now: Instant): TodayUiState {
        val next = content.cards
            .filter { it.start > now && !(it is ScheduleCard.University && it.dimmed) }
            .minByOrNull { it.start }

        return TodayUiState(
            selectedDate = content.date,
            mode = content.mode,
            now = now,
            days = content.days,
            untilNext = next?.let { it.start - now },
            nextTitle = next?.title,
            universityGroup = content.group,
        )
    }

    private fun startOf(item: Any): Instant = when (item) {
        is Occurrence -> item.start
        is UniversityScheduleCacheEntity -> item.startAt
        else -> error("unknown item")
    }

    private fun endOf(item: Any): Instant = when (item) {
        is Occurrence -> item.end
        is UniversityScheduleCacheEntity -> item.endAt
        else -> error("unknown item")
    }

    private fun titleOf(item: Any): String = when (item) {
        is Occurrence -> item.title
        is UniversityScheduleCacheEntity -> item.subject
        else -> "?"
    }

    // ---------- действия ----------

    fun setMode(newMode: ViewMode) {
        mode.value = newMode
    }

    /** [direction] — -1 (назад) или 1 (вперёд); шаг зависит от текущего режима. */
    fun shiftDate(direction: Int) {
        selectedDate.value = when (mode.value) {
            ViewMode.DAY -> selectedDate.value.plus(DatePeriod(days = direction))
            ViewMode.WEEK -> selectedDate.value.plus(DatePeriod(days = direction * 7))
            ViewMode.MONTH -> selectedDate.value.plus(DatePeriod(months = direction))
        }
    }

    fun goToday() {
        selectedDate.value = todayDate()
    }

    /** Тап по дню в сетке месяца — переход в режим «День» на эту дату. */
    fun selectDay(date: LocalDate) {
        selectedDate.value = date
        mode.value = ViewMode.DAY
    }

    fun createEvent(
        title: String,
        comment: String?,
        type: EventType,
        start: Instant,
        end: Instant,
        recurrence: NewRecurrence?,
        enrollment: EnrollmentOption? = null,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            eventRepository.createEvent(
                title = title,
                comment = comment,
                type = type,
                start = start,
                end = end,
                recurrence = recurrence,
                studentId = enrollment?.studentId,
                enrollmentId = enrollment?.enrollmentId,
            )
        }
    }

    /**
     * «Проведено»: запись занятия + начисление.
     * [amountOverride] — скидка/пробное; null — по текущей ставке
     * (для фикс-месяца поурочное начисление не делается).
     */
    fun markLessonDone(
        card: ScheduleCard.Personal,
        amountOverride: Double?,
        topics: String?,
        homework: String?,
    ) {
        val enrollmentId = card.occurrence.enrollmentId ?: return
        viewModelScope.launch {
            val enrollment = studentsRepository.getEnrollment(enrollmentId) ?: return@launch
            val amount = amountOverride ?: when (enrollment.billingMode) {
                com.sapraliev.studedu.data.local.entity.BillingMode.MONTHLY -> 0.0
                else -> enrollment.pricePerLesson ?: 0.0
            }
            studentsRepository.markLessonDone(
                studentId = enrollment.studentId,
                enrollmentId = enrollment.id,
                eventId = card.occurrence.eventId,
                date = card.start.toLocalDateTime(zone).date,
                chargeAmount = amount,
                topics = topics?.takeIf { it.isNotBlank() },
                homework = homework?.takeIf { it.isNotBlank() },
            )
        }
    }

    fun cancelOccurrence(card: ScheduleCard.Personal) {
        val original = card.occurrence.originalStart ?: return
        viewModelScope.launch {
            eventRepository.cancelOccurrence(card.occurrence.eventId, original)
        }
    }

    fun deleteEvent(card: ScheduleCard.Personal) {
        viewModelScope.launch {
            eventRepository.deleteEvent(card.occurrence.eventId)
        }
    }

    /** «Не хожу»: скрыть предмет (или только тип) либо приглушить. */
    fun hideLesson(card: ScheduleCard.University, onlyThisType: Boolean, dim: Boolean) {
        val group = uiState.value.universityGroup ?: return
        viewModelScope.launch {
            scheduleRepository.hideLesson(
                group = group,
                subject = card.lesson.subject,
                lessonType = if (onlyThisType) card.lesson.lessonType else null,
                dim = dim,
            )
        }
    }

    private fun todayDate(): LocalDate =
        Clock.System.now().toLocalDateTime(zone).date

    private fun mondayOf(date: LocalDate): LocalDate =
        date.minus(DatePeriod(days = date.dayOfWeek.ordinal))

    private fun firstDayOfMonth(date: LocalDate): LocalDate =
        LocalDate(date.year, date.monthNumber, 1)

    private fun daysInMonth(date: LocalDate): Int {
        val first = firstDayOfMonth(date)
        val nextMonthFirst = first.plus(DatePeriod(months = 1))
        return (nextMonthFirst.toEpochDays() - first.toEpochDays())
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = context.applicationContext
                val db = AppDatabase.get(app)
                TodayViewModel(
                    eventRepository = EventRepository(db.eventDao()),
                    scheduleRepository = ScheduleRepository(
                        provider = MospolytechProvider(),
                        cacheDao = db.scheduleCacheDao(),
                        hiddenLessonDao = db.hiddenLessonDao(),
                    ),
                    studentsRepository = StudentsRepository(
                        db.studentDao(),
                        db.enrollmentDao(),
                    ),
                    settings = AppSettings.get(app),
                )
            }
        }
    }
}
