package com.sapraliev.studedu.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sapraliev.studedu.data.local.entity.LessonRecordEntity
import com.sapraliev.studedu.data.local.entity.PaymentEntity
import com.sapraliev.studedu.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Проекция «ученик → полный баланс» (Σ платежей − Σ начислений). */
data class StudentBalance(
    @ColumnInfo(name = "student_id") val studentId: String,
    val balance: Double,
)

@Dao
interface StudentDao {

    // ---------- ученики ----------

    @Query("SELECT * FROM students WHERE active = 1 ORDER BY name")
    fun observeActiveStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY active DESC, name")
    fun observeAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: String): StudentEntity?

    @Query("SELECT * FROM students WHERE id = :id")
    fun observeStudentById(id: String): Flow<StudentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    // ---------- записи занятий ----------

    @Query("SELECT * FROM lesson_records WHERE student_id = :studentId ORDER BY date DESC")
    fun observeLessonRecords(studentId: String): Flow<List<LessonRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLessonRecord(record: LessonRecordEntity)

    @Delete
    suspend fun deleteLessonRecord(record: LessonRecordEntity)

    // ---------- оплаты (леджер) ----------

    @Query("SELECT * FROM payments WHERE student_id = :studentId ORDER BY date DESC")
    fun observePayments(studentId: String): Flow<List<PaymentEntity>>

    /**
     * Баланс ученика за период: SUM(payment) − SUM(charge).
     * direction хранится в нижнем регистре ('payment' / 'charge'),
     * как в Postgres — см. Converters.
     */
    @Query(
        """
        SELECT COALESCE(SUM(
            CASE direction WHEN 'payment' THEN amount ELSE -amount END
        ), 0)
        FROM payments
        WHERE student_id = :studentId AND date BETWEEN :from AND :to
        """
    )
    fun observeBalance(studentId: String, from: LocalDate, to: LocalDate): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    // ---------- статистика ----------

    /** Полные балансы всех учеников одним запросом (для списка). */
    @Query(
        """
        SELECT student_id, COALESCE(SUM(
            CASE direction WHEN 'payment' THEN amount ELSE -amount END
        ), 0) AS balance
        FROM payments GROUP BY student_id
        """
    )
    fun observeBalances(): Flow<List<StudentBalance>>

    @Query(
        """
        SELECT * FROM lesson_records
        WHERE student_id = :studentId AND date BETWEEN :from AND :to
        ORDER BY date DESC
        """
    )
    fun observeLessonRecordsBetween(
        studentId: String,
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<LessonRecordEntity>>

    @Query(
        """
        SELECT * FROM payments
        WHERE student_id = :studentId AND date BETWEEN :from AND :to
        ORDER BY date DESC
        """
    )
    fun observePaymentsBetween(
        studentId: String,
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<PaymentEntity>>

    // ---------- сводная статистика (для настроек) ----------

    /** Начислено и получено за период по всем ученикам. */
    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN direction = 'charge' THEN amount END), 0) AS charged,
            COALESCE(SUM(CASE WHEN direction = 'payment' THEN amount END), 0) AS paid
        FROM payments WHERE date BETWEEN :from AND :to
        """
    )
    fun observePeriodTotals(from: LocalDate, to: LocalDate): Flow<PeriodTotals>

    /** Проведено занятий за период по всем ученикам. */
    @Query(
        "SELECT COUNT(*) FROM lesson_records WHERE attended = 1 AND date BETWEEN :from AND :to"
    )
    fun observeLessonsCount(from: LocalDate, to: LocalDate): Flow<Int>

    /** Топ учеников по полученным оплатам за период. */
    @Query(
        """
        SELECT student_id, COALESCE(SUM(amount), 0) AS paid
        FROM payments
        WHERE direction = 'payment' AND date BETWEEN :from AND :to
        GROUP BY student_id ORDER BY paid DESC LIMIT :limit
        """
    )
    fun observeTopPaying(from: LocalDate, to: LocalDate, limit: Int): Flow<List<StudentPaid>>
}

/** Итоги периода: начислено/получено. */
data class PeriodTotals(
    val charged: Double,
    val paid: Double,
)

/** Проекция «ученик → получено за период». */
data class StudentPaid(
    @ColumnInfo(name = "student_id") val studentId: String,
    val paid: Double,
)
