package com.sapraliev.studedu.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/** Режим правила: скрыть пару полностью или показывать приглушённой. */
enum class HiddenLessonMode { HIDE, DIM }

/**
 * Правило скрытия пар вуза: «этот предмет (опционально: только этот тип
 * занятия) — не хожу». Работает поверх read-only кэша расписания,
 * сам кэш не трогается.
 *
 * Важно: скрытые и приглушённые пары НЕ участвуют в детекторе конфликтов —
 * «не хожу» значит, что слот реально свободен (например, для ученика).
 */
@Entity(
    tableName = "hidden_lesson_rules",
    indices = [Index("provider", "group_name")],
)
data class HiddenLessonRuleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    /** Например "mospolytech". */
    val provider: String,
    @ColumnInfo(name = "group_name") val group: String,
    /** Название предмета, как в расписании (сравнение без учёта регистра). */
    val subject: String,
    /** Тип занятия ("лекция", "практика"...); null — любой тип. */
    @ColumnInfo(name = "lesson_type") val lessonType: String? = null,
    val mode: HiddenLessonMode = HiddenLessonMode.HIDE,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
