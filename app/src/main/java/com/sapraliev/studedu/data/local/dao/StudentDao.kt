package com.sapraliev.studedu.data.local.dao

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
}
