package com.sapraliev.studedu.data.repository

import com.sapraliev.studedu.data.local.dao.EnrollmentDao
import com.sapraliev.studedu.data.local.dao.StudentDao
import com.sapraliev.studedu.data.local.entity.BillingMode
import com.sapraliev.studedu.data.local.entity.EnrollmentEntity
import com.sapraliev.studedu.data.local.entity.LessonRecordEntity
import com.sapraliev.studedu.data.local.entity.PaymentDirection
import com.sapraliev.studedu.data.local.entity.PaymentEntity
import com.sapraliev.studedu.data.local.entity.StudentEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** Ученик для списка: предметы и полный баланс. */
data class StudentOverview(
    val student: StudentEntity,
    val enrollments: List<EnrollmentEntity>,
    val balance: Double,
)

/**
 * Ученики, предметы (enrollments) и леджер оплат.
 *
 * Ключевое правило леджера: сумма начисления копируется в момент
 * «Проведено» и дальше живёт своей жизнью — смена ставки в enrollment
 * прошлое не переписывает.
 */
class StudentsRepository(
    private val studentDao: StudentDao,
    private val enrollmentDao: EnrollmentDao,
) {

    fun observeOverview(): Flow<List<StudentOverview>> = combine(
        studentDao.observeAllStudents(),
        enrollmentDao.observeAllActive(),
        studentDao.observeBalances(),
    ) { students, enrollments, balances ->
        val enrollmentsByStudent = enrollments.groupBy { it.studentId }
        val balanceByStudent = balances.associate { it.studentId to it.balance }
        students.map { student ->
            StudentOverview(
                student = student,
                enrollments = enrollmentsByStudent[student.id].orEmpty(),
                balance = balanceByStudent[student.id] ?: 0.0,
            )
        }
    }

    fun observeStudent(id: String) = studentDao.observeStudentById(id)

    fun observeEnrollments(studentId: String) = enrollmentDao.observeByStudent(studentId)

    fun observeAllActiveEnrollments() = enrollmentDao.observeAllActive()

    fun observeLessonRecords(studentId: String, from: LocalDate, to: LocalDate) =
        studentDao.observeLessonRecordsBetween(studentId, from, to)

    fun observePayments(studentId: String, from: LocalDate, to: LocalDate) =
        studentDao.observePaymentsBetween(studentId, from, to)

    suspend fun getEnrollment(id: String): EnrollmentEntity? = enrollmentDao.getById(id)

    /** Создаёт ученика сразу с первым предметом. */
    suspend fun addStudent(
        name: String,
        contact: String?,
        subject: String,
        pricePerLesson: Double?,
        billingMode: BillingMode,
        monthlyFee: Double?,
    ): String {
        val now = Clock.System.now()
        val studentId = UUID.randomUUID().toString()
        studentDao.upsertStudent(
            StudentEntity(
                id = studentId,
                userId = EventRepository.LOCAL_USER_ID,
                name = name.trim(),
                contact = contact?.trim()?.takeIf { it.isNotEmpty() },
                active = true,
                notes = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        addEnrollment(studentId, subject, pricePerLesson, billingMode, monthlyFee)
        return studentId
    }

    suspend fun addEnrollment(
        studentId: String,
        subject: String,
        pricePerLesson: Double?,
        billingMode: BillingMode,
        monthlyFee: Double?,
    ) {
        val now = Clock.System.now()
        enrollmentDao.upsert(
            EnrollmentEntity(
                id = UUID.randomUUID().toString(),
                userId = EventRepository.LOCAL_USER_ID,
                studentId = studentId,
                subject = subject.trim(),
                pricePerLesson = pricePerLesson,
                billingMode = billingMode,
                monthlyFee = monthlyFee,
                active = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    /** Платёж от ученика. */
    suspend fun addPayment(
        studentId: String,
        enrollmentId: String?,
        amount: Double,
        comment: String?,
    ) {
        if (amount <= 0) return
        studentDao.upsertPayment(
            PaymentEntity(
                id = UUID.randomUUID().toString(),
                userId = EventRepository.LOCAL_USER_ID,
                studentId = studentId,
                enrollmentId = enrollmentId,
                lessonRecordId = null,
                amount = amount,
                direction = PaymentDirection.PAYMENT,
                date = today(),
                comment = comment?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = Clock.System.now(),
            ),
        )
    }

    /**
     * «Проведено»: запись занятия + начисление на сумму [chargeAmount]
     * (0 — бесплатное/пробное: запись остаётся, начисления нет).
     */
    suspend fun markLessonDone(
        studentId: String,
        enrollmentId: String?,
        eventId: String?,
        date: LocalDate,
        chargeAmount: Double,
        topics: String? = null,
        homework: String? = null,
    ) {
        val now = Clock.System.now()
        val recordId = UUID.randomUUID().toString()
        studentDao.upsertLessonRecord(
            LessonRecordEntity(
                id = recordId,
                userId = EventRepository.LOCAL_USER_ID,
                studentId = studentId,
                enrollmentId = enrollmentId,
                eventId = eventId,
                date = date,
                topicsCovered = topics,
                homework = homework,
                attended = true,
                createdAt = now,
            ),
        )
        if (chargeAmount > 0) {
            studentDao.upsertPayment(
                PaymentEntity(
                    id = UUID.randomUUID().toString(),
                    userId = EventRepository.LOCAL_USER_ID,
                    studentId = studentId,
                    enrollmentId = enrollmentId,
                    lessonRecordId = recordId,
                    amount = chargeAmount,
                    direction = PaymentDirection.CHARGE,
                    date = date,
                    comment = null,
                    createdAt = now,
                ),
            )
        }
    }

    private fun today(): LocalDate =
        Clock.System.todayIn(TimeZone.currentSystemDefault())
}
