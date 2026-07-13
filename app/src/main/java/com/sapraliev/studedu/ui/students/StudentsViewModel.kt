package com.sapraliev.studedu.ui.students

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sapraliev.studedu.data.local.AppDatabase
import com.sapraliev.studedu.data.local.entity.BillingMode
import com.sapraliev.studedu.data.local.entity.EnrollmentEntity
import com.sapraliev.studedu.data.local.entity.PaymentDirection
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
)

data class StudentsUiState(
    val students: List<StudentOverview> = emptyList(),
    val detail: StudentDetailState? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModel(
    private val repository: StudentsRepository,
) : ViewModel() {

    private val zone = TimeZone.currentSystemDefault()

    private val selectedStudentId = MutableStateFlow<String?>(null)

    /** 0 — текущий месяц, 1 — прошлый и т.д. */
    private val monthOffset = MutableStateFlow(0)

    private val detailFlow = combine(selectedStudentId, monthOffset) { id, offset -> id to offset }
        .flatMapLatest { (id, offset) ->
            if (id == null) return@flatMapLatest flowOf(null)

            val today = Clock.System.todayIn(zone)
            val monthStart = LocalDate(today.year, today.monthNumber, 1)
                .minus(DatePeriod(months = offset))
            val monthEnd = monthStart.plus(DatePeriod(months = 1))
                .minus(DatePeriod(days = 1))

            combine(
                repository.observeStudent(id),
                repository.observeEnrollments(id),
                repository.observeLessonRecords(id, monthStart, monthEnd),
                repository.observePayments(id, monthStart, monthEnd),
                repository.observePayments(id, EPOCH, FAR_FUTURE),
            ) { student, enrollments, records, monthPayments, allPayments ->
                if (student == null) return@combine null

                val subjectByEnrollment = enrollments.associate { it.id to it.subject }
                val balance = allPayments.sumOf {
                    if (it.direction == PaymentDirection.PAYMENT) it.amount else -it.amount
                }

                val lessons = records.map {
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
        combine(repository.observeOverview(), detailFlow) { students, detail ->
            StudentsUiState(students = students, detail = detail)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentsUiState())

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
        mode: BillingMode,
        monthlyFee: Double?,
    ) {
        if (name.isBlank() || subject.isBlank()) return
        viewModelScope.launch {
            repository.addStudent(name, contact, subject, price, mode, monthlyFee)
        }
    }

    fun addEnrollment(subject: String, price: Double?, mode: BillingMode, monthlyFee: Double?) {
        val studentId = selectedStudentId.value ?: return
        if (subject.isBlank()) return
        viewModelScope.launch {
            repository.addEnrollment(studentId, subject, price, mode, monthlyFee)
        }
    }

    fun addPayment(amount: Double, comment: String?) {
        val studentId = selectedStudentId.value ?: return
        viewModelScope.launch {
            repository.addPayment(studentId, enrollmentId = null, amount = amount, comment = comment)
        }
    }

    companion object {
        private val EPOCH = LocalDate(2000, 1, 1)
        private val FAR_FUTURE = LocalDate(2100, 1, 1)

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val db = AppDatabase.get(context.applicationContext)
                StudentsViewModel(
                    StudentsRepository(db.studentDao(), db.enrollmentDao()),
                )
            }
        }
    }
}
