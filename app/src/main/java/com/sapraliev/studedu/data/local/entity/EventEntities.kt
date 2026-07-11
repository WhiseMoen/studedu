package com.sapraliev.studedu.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** Тип события. Значения совпадают с enum event_type в Postgres. */
enum class EventType { PERSONAL, LESSON, DEADLINE }

/** Частота повторения (enum recurrence_freq). */
enum class RecurrenceFreq { DAILY, WEEKLY, MONTHLY }

/** Тип исключения серии (enum exception_type). */
enum class ExceptionType { CANCELLED, MOVED }

/**
 * Правило повторения (модель RRULE).
 * Никакого дублирования строк: вхождения генерируются на лету (dmfs/lib-recur).
 */
@Entity(tableName = "recurrence_rules")
data class RecurrenceRuleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val freq: RecurrenceFreq,
    val interval: Int = 1,
    /** Дни недели, например ["MO","WE","FR"]; null — не задано. */
    val byweekday: List<String>? = null,
    /** Сколько раз повторить (взаимоисключимо с until). */
    val count: Int? = null,
    /** До какой даты (включительно). */
    val until: LocalDate? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)

/**
 * Пользовательское событие: личное, занятие с учеником или дедлайн.
 * Пары вуза здесь НЕ хранятся — они в university_schedule_cache.
 * Для повторяющихся событий start/end — первое вхождение (шаблон).
 */
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = RecurrenceRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurrence_rule_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("start_at"),
        Index("student_id"),
        Index("recurrence_rule_id"),
    ],
)
data class EventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val title: String,
    val comment: String? = null,
    val type: EventType = EventType.PERSONAL,
    @ColumnInfo(name = "start_at") val startAt: Instant,
    @ColumnInfo(name = "end_at") val endAt: Instant,
    @ColumnInfo(name = "is_all_day") val isAllDay: Boolean = false,
    @ColumnInfo(name = "student_id") val studentId: String? = null,
    /** Для занятий: связка «ученик × предмет» (без FK — колонка добавлена миграцией). */
    @ColumnInfo(name = "enrollment_id") val enrollmentId: String? = null,
    @ColumnInfo(name = "recurrence_rule_id") val recurrenceRuleId: String? = null,
    val color: String? = null,
    val source: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)

/** Перенос или отмена конкретного вхождения серии. */
@Entity(
    tableName = "recurrence_exceptions",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("event_id")],
)
data class RecurrenceExceptionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "event_id") val eventId: String,
    /** Какое вхождение серии затронуто (его исходный старт). */
    @ColumnInfo(name = "original_date") val originalDate: Instant,
    va