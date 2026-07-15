package com.sapraliev.studedu.ui.students

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sapraliev.studedu.core.AppGraph
import com.sapraliev.studedu.data.local.entity.EnrollmentEntity
import com.sapraliev.studedu.data.local.entity.LessonRecordEntity
import com.sapraliev.studedu.data.local.entity.PaymentDirection
import com.sapraliev.studedu.data.local.entity.PaymentEntity
import com.sapraliev.studedu.data.local.entity.StudentEntity
import com.sapraliev.studedu.data.repository.StudentOverview
import com.sapraliev.studedu.data.repository.StudentsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

/** Строка истории: занятие или движение денег. */
sealed interface HistoryItem {
    val date: LocalDate

    data class Lesson(
        override val date: LocalDate,
        val subject: String,
        val topics: String?,
        val homework: String?,
    ) : HistoryItem

    data class Money(
        override val date: LocalDate,
        val amount: Double,
        val isIncome: Boolean,
        val subject: String?,
        val comment: String?,
    ) : HistoryItem
}

data class MonthSummary(
    val monthStart: LocalDate,
    val lessonsTotal: Int = 0,
    val lessonsBySubject: List<Pair<String, Int>> = emptyList(),
    val charged: Double = 0.0,
    val paid: Double = 0.0,
    val history: List<HistoryItem> = emptyList(),
)

data class StudentDetailState(
    val student: StudentEntity,
    val enrollments: List<EnrollmentEntity>,
    val balance: Double,
    val prepaidLessons: Int?,
    val summary: MonthSummary,
    /** Предметы, у которых просматриваемый месяц ([MonthSummary.monthStart]) оплачен целиком. */
    val monthCoveredEnrollmentIds: Set<String> = emptySet(),
)

data class StudentsUiState(
    val students: List<StudentOverview> = emptyList(),
    val detail: StudentDetailState? = null,
    val showInactive: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModel(
    private val repository: StudentsRepository,
) : ViewModel() {

    private val zone = TimeZone.currentSystemDefault()

    private val selectedStudentId = MutableStateFlow<String?>(null)

    /** 0 — текущий месяц, 1 — прошлый и т.д. */
    private val monthOffset = MutableStateFlow(0)

    private val showInactive = MutableStateFlow(false)

    private val overviewFlow = showInactive.flatMapLatest { includeInactive ->
        repository.observeOverview(activeOnly = !includeInactive)
    }

    /** Промежуточный пакет данных детали — сузить до типобезопасной 5-местной combine(). */
    private data class DetailBase(
        val student: StudentEntity?,
        val enrollments: List<EnrollmentEntity>,
        val records: List<LessonRecordEntity>,
        val monthPayments: List<PaymentEntity>,
        val balance: Double,
    )

    private val detailFlow = combine(selectedStudentId, monthOffset) { id, offset -> id to offset }
        .flatMapLatest { (id, offset) ->
            if (id == null) return@flatMapLatest flowOf(null)

            val today = Clock.System.todayIn(zone)
            val monthStart = LocalDate(today.year, today.monthNumber, 1)
                .minus(DatePeriod(months = offset))
            val monthEnd = monthStart.plus(DatePeriod(months = 1))
                .minus(DatePeriod(days = 1))

            val baseFlow = combine(
                repository.observeStudent(id),
                repository.observeEnrollments(id),
                repository.observeLessonRecords(id, monthStart, monthEnd),
                repository.observePayments(id, monthStart, monthEnd),
                // Полный баланс — SQL SUM, а не выгрузка всей истории платежей в память.
                repository.observeBalance(id, EPOCH, FAR_FUTURE),
            ) { student, enrollments, records, monthPayments, balance ->
                DetailBase(student, enrollments, records, monthPayments, balance)
            }

            combine(
                baseFlow,
                // Точечный запрос вместо фильтрации всей истории платежей по covers_month.
                repository.observeMonthCoveredEnrollmentIds(id, monthStart),
            ) { base, monthCoveredIds ->
                val student = base.student ?: return@combine null
                val enrollments = base.enrollments
                val monthPayments = base.monthPayments
                val balance = base.balance

                val subjectByEnrollment = enrollments.associate { it.id to it.subject }

                val lessons = base.records.map {
                    HistoryItem.Lesson(
                        date = it.date,
                        subject = subjectByEnrollment[it.enrollmentId] ?: "занятие",
                        topics = it.topicsCovered,
                        homework = it.homework,
                    )
                }
                val money = monthPayments.map {
                    HistoryItem.Money(
                        date = it.date,
                        amount = it.amount,
                        isIncome = it.direction == PaymentDirection.PAYMENT,
                        subject = it.enrollmentId?.let(subjectByEnrollment::get),
                        comment = it.comment,
                    )
                }

                val perLessonPrice = enrollments
                    .firstOrNull { it.pricePerLesson != null && it.pricePerLesson > 0 }
                    ?.pricePerLesson
                val prepaid = if (balance > 0 && perLessonPrice != null) {
                    (balance / perLessonPrice).toInt()
                } else {
                    null
                }

                StudentDetailState(
                    student = student,
                    enrollments = enrollments,
                    balance = balance,
                    prepaidLessons = prepaid,
                    monthCoveredEnrollmentIds = monthCoveredIds.toSet(),
                    summary = MonthSummary(
                        monthStart = monthStart,
                        lessonsTotal = lessons.size,
                        lessonsBySubject = lessons.groupBy { it.subject }
                            .map { (subject, list) -> subject to list.size }
                            .sortedByDescending { it.second },
                        charged = monthPayments
                            .filter { it.direction == PaymentDirection.CHARGE }
                            .sumOf { it.amount },
                        paid = monthPayments
                            .filter { it.direction == PaymentDirection.PAYMENT }
                            .sumOf { it.amount },
                        history = (lessons + money).sortedByDescending { it.date },
                    ),
                )
            }
        }

    val uiState: StateFlow<StudentsUiState> =
        combine(overviewFlow, detailFlow, showInactive) { students, detail, showInactiveValue ->
            StudentsUiState(students = students, detail = detail, showInactive = showInactiveValue)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentsUiState())

    fun setShowInactive(value: Boolean) {
        showInactive.value = value
    }

    fun openStudent(id: String) {
        monthOffset.value = 0
        selectedStudentId.value = id
    }

    fun closeStudent() {
        selectedStudentId.value = null
    }

    fun shiftMonth(delta: Int) {
        monthOffset.value = (monthOffset.value + delta).coerceAtLeast(0)
    }

    fun addStudent(
        name: String,
        contact: String?,
        subject: String,
        price: Double?,
        monthlyFee: Double?,
    ) {
        if (name.isBlank() || subject.isBlank()) return
        viewModelScope.launch {
            repository.addStudent(name, contact, subject, price, monthlyFee)
        }
    }

    fun addEnrollment(subject: String, price: Double?, monthlyFee: Double?) {
        val studentId = selectedStudentId.value ?: return
        if (subject.isBlank()) return
        viewModelScope.launch {
            repository.addEnrollment(studentId, subject, price, monthlyFee)
        }
    }

    fun updateStudent(name: String, contact: String?) {
        val studentId = selectedStudentId.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.updateStudent(studentId, name, contact)
        }
    }

    /** Удаляет открытого ученика целиком (занятия и платежи — каскадом) и закрывает деталь. */
    fun deleteStudent() {
        val studentId = selectedStudentId.value ?: return
        viewModelScope.launch {
            repository.deleteStudent(studentId)
            closeStudent()
        }
    }

    /** «Не активен»: пропадает из «Учеников» и расписания, статистика остаётся. */
    fun setStudentActive(active: Boolean) {
        val studentId = selectedStudentId.value ?: return
        viewModelScope.launch {
            repository.setStudentActive(studentId, active)
        }
    }

    fun updateEnrollment(id: String, subject: String, price: Double?, monthlyFee: Double?) {
        if (subject.isBlank()) return
        viewModelScope.launch {
            repository.updateEnrollment(id, subject, price, monthlyFee)
        }
    }

    fun deleteEnrollment(id: String) {
        viewModelScope.launch {
            repository.deleteEnrollment(id)
        }
    }

    /** Разовый платёж: не привязан к предмету, просто пополняет общий баланс. */
    fun addPayment(amount: Double, comment: String?) {
        val studentId = selectedStudentId.value ?: return
        viewModelScope.launch {
            repository.addPayment(studentId, enrollmentId = null, amount = amount, comment = comment)
        }
    }

    /** «Оплата месяца» по конкретному предмету — освобождает все его занятия в текущем просматриваемом месяце. */
    fun addMonthPayment(enrollmentId: String, amount: Double, comment: String?) {
        val studentId = selectedStudentId.value ?: return
        val monthStart = detailFlowMonthStart() ?: return
        viewModelScope.launch {
            repository.addMonthPayment(studentId, enrollmentId, amount, monthStart, comment)
        }
    }

    /** «Оплата пакета N занятий» по конкретному предмету. */
    fun addPackagePayment(enrollmentId: String, amount: Double, lessonsCount: Int, comment: String?) {
        val studentId = selectedStudentId.value ?: return
        viewModelScope.launch {
            repository.addPackagePayment(studentId, enrollmentId, amount, lessonsCount, comment)
        }
    }

    private fun detailFlowMonthStart(): LocalDate? {
        val today = Clock.System.todayIn(zone)
        return LocalDate(today.year, today.monthNumber, 1).minus(DatePeriod(months = monthOffset.value))
    }

    companion object {
        private val EPOCH = LocalDate(2000, 1, 1)
        private val FAR_FUTURE = LocalDate(2100, 1, 1)

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppGraph.init(context.applicationContext)
                StudentsViewModel(AppGraph.studentsRepository)
            }
        }
    }
}
