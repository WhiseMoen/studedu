package com.sapraliev.studedu.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/** Режим оплаты для конкретной пары «ученик × предмет». */
enum class BillingMode {
    /** Начисление за каждое проведённое занятие. */
    PER_LESSON,

    /** Предоплата пакетом: баланс пополняется крупно, занятия списывают. */
    PACKAGE,

    /** Фиксированная сумма в месяц. */
    MONTHLY,
}

/**
 * Enrollment — связка «ученик × предмет» со своей ценой и режимом оплаты.
 * У одного ученика может быть несколько предметов с разными ставками,
 * у разных учеников один предмет может стоить по-разному.
 *
 * Важно: ставка здесь — «цена по умолчанию на сегодня». При начислении
 * сумма КОПИРУЕТСЯ в payments и больше не зависит от этой ставки —
 * повышение цены не переписывает историю.
 */
@Entity(
    tableName = "enrollments",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("student_id")],
)
data class EnrollmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    val subject: String,
    @ColumnInfo(name = "price_per_lesson") val pricePerLesson: Double? = null,
    @ColumnInfo(name = "billing_mode") val billingMode: BillingMode = BillingMode.PER_LESSON,
    @ColumnInfo(name = "monthly_fee") val monthlyFee: Double? = null,
    val active: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
