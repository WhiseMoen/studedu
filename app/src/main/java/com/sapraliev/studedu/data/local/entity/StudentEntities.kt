package com.sapraliev.studedu.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** Направление движения по леджеру (enum payment_direction). */
enum class PaymentDirection {
    /** Начислено за занятие. */
    CHARGE,
    /** Ученик заплатил. */
    PAYMENT,
}

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val name: String,
    /** Телефон / телеграм. */
    val contact: String? = null,
    val subject: String? = null,
    @ColumnInfo(name = "price_per_lesson") val pricePerLesson: Double? = null,
    @ColumnInfo(name = "monthly_fee") val monthlyFee: Double? = null,
    val active: Boolean = true,
    val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)

/** Факт проведённого занятия: дз, пройденные темы. */
@Entity(
    tableName = "lesson_records",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("student_id"), Index("event_id"), Index("date")],
)
data class LessonRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    /** Привязка к событию в расписании (опционально). */
    @ColumnInfo(name = "event_id") val eventId: String? = null,
    val date: LocalDate,
    @ColumnInfo(name = "topics_covered") val topicsCovered: String? = null,
    val homework: String? = null,
    val attended: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

/** Леджер оплат: баланс = SUM(payment) − SUM(charge). */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("student_id"), Index("date")],
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    val amount: Double,
    val direction: PaymentDirection,
    val date: LocalDate,
    val comment: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
