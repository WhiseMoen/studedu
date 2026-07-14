package com.sapraliev.studedu.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sapraliev.studedu.core.AppGraph
import com.sapraliev.studedu.data.local.dao.StudentDao
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

/** Строка статистики по одному ученику: занятия и деньги за два периода. */
data class StudentStatsRow(
    val studentId: String,
    val name: String,
    val monthLessons: Int,
    val monthPaid: Double,
    val monthCharged: Double,
    val prevLessons: Int,
    val prevPaid: Double,
    val prevCharged: Double,
)

data class StatsScreenState(
    val totalMonthPaid: Double = 0.0,
    val totalMonthCharged: Double = 0.0,
    val totalMonthLessons: Int = 0,
    val totalPrevPaid: Double = 0.0,
    val totalPrevCharged: Double = 0.0,
    val totalPrevLessons: Int = 0,
    val students: List<StudentStatsRow> = emptyList(),
)

/** Полная статистика по всем ученикам: этот месяц и прошлый, по каждому и в сумме. */
class StatsViewModel(private val studentDao: StudentDao) : ViewModel() {

    val uiState: StateFlow<StatsScreenState> = run {
        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(zone)
        val monthStart = LocalDate(today.year, today.monthNumber, 1)
        val monthEnd = monthStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        val prevStart = monthStart.minus(DatePeriod(months = 1))
        val prevEnd = monthStart.minus(DatePeriod(days = 1))

        combine(
            studentDao.observeAllStudents(),
            studentDao.observePeriodTotalsByStudent(monthStart, monthEnd),
            studentDao.observePeriodTotalsByStudent(prevStart, prevEnd),
            studentDao.observeLessonsCountByStudent(monthStart, monthEnd),
            studentDao.observeLessonsCountByStudent(prevStart, prevEnd),
        ) { students, monthTotals, prevTotals, monthCounts, prevCounts ->
            val monthByStudent = monthTotals.associateBy { it.studentId }
            val prevByStudent = prevTotals.associateBy { it.studentId }
            val monthCountByStudent = monthCounts.associateBy { it.studentId }
            val prevCountByStudent = prevCounts.associateBy { it.studentId }

            val rows = students.map { student ->
                val month = monthByStudent[student.id]
                val prev = prevByStudent[student.id]
                StudentStatsRow(
                    studentId = student.id,
                    name = student.name,
                    monthLessons = monthCountByStudent[student.id]?.count ?: 0,
                    monthPaid = month?.paid ?: 0.0,
                    monthCharged = month?.charged ?: 0.0,
                    prevLessons = prevCountByStudent[student.id]?.count ?: 0,
                    prevPaid = prev?.paid ?: 0.0,
                    prevCharged = prev?.charged ?: 0.0,
                )
            }

            StatsScreenState(
                totalMonthPaid = rows.sumOf { it.monthPaid },
                totalMonthCharged = rows.sumOf { it.monthCharged },
                totalMonthLessons = rows.sumOf { it.monthLessons },
                totalPrevPaid = rows.sumOf { it.prevPaid },
                totalPrevCharged = rows.sumOf { it.prevCharged },
                totalPrevLessons = rows.sumOf { it.prevLessons },
                students = rows,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsScreenState())
    }

    /** Удаление ученика прямо из статистики — независимо от экрана «Ученики». */
    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            studentDao.deleteStudentById(studentId)
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer { StatsViewModel(AppGraph.database.studentDao()) }
        }
    }
}
