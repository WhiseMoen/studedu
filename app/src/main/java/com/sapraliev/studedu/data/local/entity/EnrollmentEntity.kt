package com.sapraliev.studedu.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Enrollment — связка «ученик × предмет» со своей ставкой.
 * У одного ученика может быть несколько предметов с разными ставками,
 * у разных учеников один предмет может стоить по-разному.
 *
 * Важно: ставка здесь — «цена по умолчанию на сегодня», в любой момент
 * редактируемая отдельно от способа оплаты. Способ оплаты выбирается
 * не здесь, а в момент платежа (разовый / за месяц / пакетом занятий —
 * см. `StudentsRepository.addPayment/addMonthPayment/addPackagePayment`):
 * оплаченный месяц освобождает все занятия в нём от начисления,
 * пакет — списывает [remainingPackageLessons] по одному занятию.
 * При начислении сумма КОПИРУЕТСЯ в payments и больше не зависит ни от
 * ставки, ни от последующих правок покрытия — повышение цены или новый
 * платёж не переписывают историю.
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
    /** Ставка за занятие — всегда видна и редактируется независимо от способа оплаты. */
    @ColumnInfo(name = "price_per_lesson") val pricePerLesson: Double? = null,
    /** Обычная сумма за месяц — подсказка для быстрой оплаты месяца, тоже независима от ставки. */
    @ColumnInfo(name = "monthly_fee") val monthlyFee: Double? = null,
    /** Сколько занятий ещё числится оплаченными пакетом (списывается по одному при «Проведено»). */
    @ColumnInfo(name = "remaining_package_lessons") val remainingPackageLessons: Int = 0,
    val active: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
