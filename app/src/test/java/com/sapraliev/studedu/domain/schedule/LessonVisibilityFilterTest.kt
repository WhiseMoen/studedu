package com.sapraliev.studedu.domain.schedule

import com.sapraliev.studedu.data.local.entity.HiddenLessonMode
import com.sapraliev.studedu.data.local.entity.HiddenLessonRuleEntity
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonVisibilityFilterTest {

    private val filter = LessonVisibilityFilter()
    private val now = Instant.parse("2026-07-01T00:00:00Z")

    private fun rule(
        subject: String,
        lessonType: String? = null,
        mode: HiddenLessonMode = HiddenLessonMode.HIDE,
    ) = HiddenLessonRuleEntity(
        id = "r-$subject-$lessonType", userId = "u",
        provider = "mospolytech", group = "221-321",
        subject = subject, lessonType = lessonType, mode = mode, createdAt = now,
    )

    @Test
    fun `без правил всё видимо`() {
        assertEquals(
            LessonVisibility.VISIBLE,
            filter.visibilityFor("Физкультура", "практика", emptyList()),
        )
    }

    @Test
    fun `правило по предмету скрывает любой тип занятия`() {
        val rules = listOf(rule("Физкультура"))
        assertEquals(LessonVisibility.HIDDEN, filter.visibilityFor("Физкультура", "практика", rules))
        assertEquals(LessonVisibility.HIDDEN, filter.visibilityFor("Физкультура", null, rules))
    }

    @Test
    fun `правило с типом не трогает другой тип`() {
        val rules = listOf(rule("Матанализ", lessonType = "лекция"))
        assertEquals(LessonVisibility.HIDDEN, filter.visibilityFor("Матанализ", "лекция", rules))
        assertEquals(LessonVisibility.VISIBLE, filter.visibilityFor("Матанализ", "практика", rules))
    }

    @Test
    fun `сравнение без учёта регистра и пробелов`() {
        val rules = listOf(rule(" физкультура "))
        assertEquals(LessonVisibility.HIDDEN, filter.visibilityFor("ФИЗКУЛЬТУРА", "практика", rules))
    }

    @Test
    fun `dim приглушает, а не скрывает`() {
        val rules = listOf(rule("История", mode = HiddenLessonMode.DIM))
        assertEquals(LessonVisibility.DIMMED, filter.visibilityFor("История", "лекция", rules))
    }

    @Test
    fun `hide побеждает dim`() {
        val rules = listOf(
            rule("История", mode = HiddenLessonMode.DIM),
            rule("История", lessonType = "лекция", mode = HiddenLessonMode.HIDE),
        )
        assertEquals(LessonVisibility.HIDDEN, filter.visibilityFor("История", "лекция", rules))
        assertEquals(LessonVisibility.DIMMED, filter.visibilityFor("История", "семинар", rules))
    }

    @Test
    fun `чужой предмет не затрагивается`() {
        val rules = listOf(rule("Физкультура"))
        assertEquals(LessonVisibility.VISIBLE, filter.visibilityFor("Матанализ", "лекция", rules))
    }

    @Test
    fun `в конфликтах участвует только видимое`() {
        assertTrue(filter.conflictsEnabled(LessonVisibility.VISIBLE))
        assertFalse(filter.conflictsEnabled(LessonVisibility.DIMMED))
        assertFalse(filter.conflictsEnabled(LessonVisibility.HIDDEN))
    }
}
