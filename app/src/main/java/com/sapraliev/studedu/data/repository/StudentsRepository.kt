package com.sapraliev.studedu.data.repository

import com.sapraliev.studedu.data.local.dao.EnrollmentDao
import com.sapraliev.studedu.data.local.dao.StudentDao
import com.sapraliev.studedu.data.local.entity.EnrollmentEntity
import com.sapraliev.studedu.data.local.entity.LessonRecordEntity
import com.sapraliev.studedu.data.local.entity.PaymentDirection
import com.sapraliev.studedu.data.local.entity.PaymentEntity
import com.sapraliev.studedu.data.local.entity.StudentEntity
import com.sapraliev.studedu.data.local.entity.StudentTint
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

/** Результат создания ученика сразу с предметом — id обоих нужны, чтобы сразу привязать занятие. */
data class NewStudentResult(val studentId: String, val enrollmentId: String)

/**
 * Ученики, предметы (enrollments) и леджер оплат.
 *
 * Ключевое правило леджера: сумма начисления копируется в момент
 * «Проведено» и дальше живёт своей жизнью — смена ставки в enrollment
 * прошлое не переписывает. Способ оплаты не фиксирован на предмете —
 * выбирается при каждом платеже: разовый (просто пополняет баланс),
 * за месяц (все занятия этого месяца по предмету бесплатны) или
 * пакетом N занятий (списывается по одному при «Проведено»).
 */
class StudentsRepository(
    private val studentDao: StudentDao,
    private val enrollmentDao: EnrollmentDao,
) {

    /** [activeOnly] прячет учеников со статусом «не активен» (экран «Ученики», выбор в расписании). */
    fun observeOverview(activeOnly: Boolean): Flow<List<StudentOverview>> = combine(
        if (activeOnly) studentDao.observeActiveStudents() else studentDao.observeAllStudents(),
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

    /** Полный баланс (SUM в SQL) — не тянет всю историю платежей в память ради одного числа. */
    fun observeBalance(studentId: String, from: LocalDate, to: LocalDate) =
        studentDao.observeBalance(studentId, from, to)

    /** Предметы, у которых [month] оплачен целиком — точечный запрос, не вся история платежей. */
    fun observeMonthCoveredEnrollmentIds(studentId: String, month: LocalDate) =
        studentDao.observeMonthCoveredEnrollmentIds(studentId, month)

    suspend fun getEnrollment(id: String): EnrollmentEntity? = enrollmentDao.getById(id)

    /** Создаёт ученика сразу с первым предметом. */
    suspend fun addStudent(
        name: String,
        contact: String?,
        subject: String,
        pricePerLesson: Double?,
        monthlyFee: Double?,
    ): NewStudentResult {
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
        val enrollmentId = addEnrollment(studentId, subject, pricePerLesson, monthlyFee)
        return NewStudentResult(studentId, enrollmentId)
    }

    suspend fun addEnrollment(
        studentId: String,
        subject: String,
        pricePerLesson: Double?,
        monthlyFee: Double?,
    ): String {
        val now = Clock.System.now()
        val enrollmentId = UUID.randomUUID().toString()
        enrollmentDao.upsert(
            EnrollmentEntity(
                id = enrollmentId,
                userId = EventRepository.LOCAL_USER_ID,
                studentId = studentId,
                subject = subject.trim(),
                pricePerLesson = pricePerLesson,
                monthlyFee = monthlyFee,
                remainingPackageLessons = 0,
                active = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return enrollmentId
    }

    /** Меняет имя/контакт ученика. Ставки и история занятий не затрагиваются. */
    suspend fun updateStudent(id: String, name: String, contact: String?) {
        val existing = studentDao.getStudentById(id) ?: return
        studentDao.upsertStudent(
            existing.copy(
                name = name.trim(),
                contact = contact?.trim()?.takeIf { it.isNotEmpty() },
                updatedAt = Clock.System.now(),
            ),
        )
    }

    /** Удаляет ученика целиком: занятия и платежи каскадно удаляются по FK. */
    suspend fun deleteStudent(id: String) {
        studentDao.deleteStudentById(id)
    }

    /** «Не активен» вместо удаления: пропадает из «Учеников» и расписания, статистика остаётся. */
    suspend fun setStudentActive(id: String, active: Boolean) {
        studentDao.setActive(id, active, Clock.System.now())
    }

    /** Оттенок карточек занятий этого ученика в ленте; `null` — обычный цвет типа события. */
    suspend fun setStudentTint(id: String, tint: StudentTint?) {
        studentDao.setColorTint(id, tint, Clock.System.now())
    }

    /** id ученика → оттенок, для подсветки карточек занятий в ленте «Сегодня». */
    fun observeStudentTints(): Flow<Map<String, StudentTint?>> =
        studentDao.observeTints().map { rows -> rows.associate { it.id to it.colorTint } }

    /** Меняет предмет/ставку. История начислений не переписывается — суммы уже скопированы в payments. */
    suspend fun updateEnrollment(
        id: String,
        subject: String,
        pricePerLesson: Double?,
        monthlyFee: Double?,
    ) {
        val existing = enrollmentDao.getById(id) ?: return
        enrollmentDao.upsert(
            existing.copy(
                subject = subject.trim(),
                pricePerLesson = pricePerLesson,
                monthlyFee = monthlyFee,
                updatedAt = Clock.System.now(),
            ),
        )
    }

    /** Удаляет предмет. Записи занятий/платежей не FK-связаны с enrollment — история остаётся. */
    suspend fun deleteEnrollment(id: String) {
        enrollmentDao.delete(id)
    }

    /** Разовый платёж: просто пополняет баланс, ни на что не влияет. */
    suspend fun addPayment(
        studentId: String,
        enrollmentId: String?,
        amount: Double,
        comment: String?,
    ) {
        if (amount <= 0) return
        recordPayment(
            studentId = studentId,
            enrollmentId = enrollmentId,
            amount = amount,
            direction = PaymentDirection.PAYMENT,
            comment = comment,
        )
    }

    /**
     * «Оплата месяца»: [month] (любое число внутри месяца — берётся первое
     * число) закрывается целиком для этого предмета — сколько бы занятий
     * ни было, «Проведено» их не начислит.
     */
    suspend fun addMonthPayment(
        studentId: String,
        enrollmentId: String,
        amount: Double,
        month: LocalDate,
        comment: String?,
    ) {
        if (amount <= 0) return
        recordPayment(
            studentId = studentId,
            enrollmentId = enrollmentId,
            amount = amount,
            direction = PaymentDirection.PAYMENT,
            comment = comment,
            coversMonth = LocalDate(month.year, month.monthNumber, 1),
        )
    }

    /** «Оплата пакета N занятий»: счётчик пополняется, списывается по одному при «Проведено». */
    suspend fun addPackagePayment(
        studentId: String,
        enrollmentId: String,
        amount: Double,
        lessonsCount: Int,
        comment: String?,
    ) {
        if (amount <= 0 || lessonsCount <= 0) return
        recordPayment(
            studentId = studentId,
            enrollmentId = enrollmentId,
            amount = amount,
            direction = PaymentDirection.PAYMENT,
            comment = comment,
        )
        enrollmentDao.addPackageLessons(enrollmentId, lessonsCount, Clock.System.now())
    }

    /**
     * «Проведено»: запись занятия + начисление.
     * [amountOverride] — ручная сумма (скидка/пробное, 0 — бесплатно);
     * null — начислить по умолчанию: 0, если месяц оплачен целиком или
     * есть остаток пакета (тогда он же списывается на 1), иначе ставка.
     */
    suspend fun markLessonDone(
        studentId: String,
        enrollmentId: String?,
        eventId: String?,
        date: LocalDate,
        amountOverride: Double?,
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
        val chargeAmount = amountOverride ?: computeDefaultCharge(enrollmentId, date)
        if (chargeAmount > 0) {
            recordPayment(
                studentId = studentId,
                enrollmentId = enrollmentId,
                amount = chargeAmount,
                direction = PaymentDirection.CHARGE,
                comment = null,
                date = date,
                lessonRecordId = recordId,
            )
        }
    }

    private suspend fun computeDefaultCharge(enrollmentId: String?, date: LocalDate): Double {
        if (enrollmentId == null) return 0.0
        val enrollment = enrollmentDao.getById(enrollmentId) ?: return 0.0
        val monthStart = LocalDate(date.year, date.monthNumber, 1)
        if (studentDao.countMonthCoverage(enrollmentId, monthStart) > 0) return 0.0
        if (enrollment.remainingPackageLessons > 0) {
            enrollmentDao.consumePackageLesson(enrollmentId, Clock.System.now())
            return 0.0
        }
        return enrollment.pricePerLesson ?: 0.0
    }

    /** Общий конструктор строки леджера — раньше был продублирован в каждом из методов выше. */
    private suspend fun recordPayment(
        studentId: String,
        enrollmentId: String?,
        amount: Double,
        direction: PaymentDirection,
        comment: String?,
        date: LocalDate = today(),
        lessonRecordId: String? = null,
        coversMonth: LocalDate? = null,
    ) {
        studentDao.upsertPayment(
            PaymentEntity(
                id = UUID.randomUUID().toString(),
                userId = EventRepository.LOCAL_USER_ID,
                studentId = studentId,
                enrollmentId = enrollmentId,
                lessonRecordId = lessonRecordId,
                amount = amount,
                direction = direction,
                date = date,
                comment = comment?.trim()?.takeIf { it.isNotEmpty() },
                coversMonth = coversMonth,
                createdAt = Clock.System.now(),
            ),
        )
    }

    private fun today(): LocalDate =
        Clock.System.todayIn(TimeZone.currentSystemDefault())
}
