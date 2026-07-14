package com.sapraliev.studedu.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** Источник задачи (enum task_source). */
enum class TaskSource { PERSONAL, UNIVERSITY_DEADLINE }

/** Объединённые заметки, чек-листы и дедлайны. Задача с dueDate = дедлайн. */
@Entity(tableName = "tasks", indices = [Index("due_date")])
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val text: String,
    val done: Boolean = false,
    /** Теги для группировки по тематике. */
    val tags: List<String> = emptyList(),
    @ColumnInfo(name = "due_date") val dueDate: LocalDate? = null,
    val source: TaskSource = TaskSource.PERSONAL,
    /** Напоминания о дедлайне по умолчанию можно отключить для конкретной задачи. */
    @ColumnInfo(name = "reminders_enabled") val remindersEnabled: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)

/**
 * Read-only кэш пар вуза. Заполняется ScheduleProvider'ом при синке,
 * UI никогда не пишет сюда напрямую.
 */
@Entity(
    tableName = "university_schedule_cache",
    indices = [Index("start_at"), Index("provider")],
)
data class UniversityScheduleCacheEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    /** Например "mospolytech". */
    val provider: String,
    @ColumnInfo(name = "group_name") val group: String,
    val subject: String,
    val teacher: String? = null,
    val place: String? = null,
    @ColumnInfo(name = "lesson_type") val lessonType: String? = null,
    @ColumnInfo(name = "start_at") val startAt: Instant,
    @ColumnInfo(name = "end_at") val endAt: Instant,
    @ColumnInfo(name = "synced_at") val syncedAt: Instant,
)
