package com.sapraliev.studedu.domain.schedule

import com.sapraliev.studedu.data.local.entity.HiddenLessonMode
import com.sapraliev.studedu.data.local.entity.HiddenLessonRuleEntity

/** Видимость пары в ленте после применения правил скрытия. */
enum class LessonVisibility {
    /** Обычная карточка, участвует в конфликтах. */
    VISIBLE,

    /** Серая «призрачная» карточка; в конфликтах НЕ участвует. */
    DIMMED,

    /** Не отображается и в конфликтах не участвует. */
    HIDDEN,
}

/**
 * Применение правил скрытия к парам вуза.
 *
 * Правило матчит по предмету (без учёта регистра и краевых пробелов)
 * и, если в правиле задан тип занятия, — ещё и по типу.
 * Если элемент попадает под несколько правил, побеждает более строгое:
 * HIDE > DIM.
 */
class LessonVisibilityFilter {

    fun visibilityFor(
        subject: String,
        lessonType: String?,
        rules: List<HiddenLessonRuleEntity>,
    ): LessonVisibility {
        var result = LessonVisibility.VISIBLE
        for (rule in rules) {
            if (!rule.subject.trim().equals(subject.trim(), ignoreCase = true)) continue
            val typeMatches = rule.lessonType == null ||
                rule.lessonType.trim().equals(lessonType?.trim() ?: "", ignoreCase = true)
            if (!typeMatches) continue
            when (rule.mode) {
                HiddenLessonMode.HIDE -> return LessonVisibility.HIDDEN
                HiddenLessonMode.DIM -> result = LessonVisibility.DIMMED
            }
        }
        return result
    }

    /** Участвует ли элемент с данной видимостью в детекторе конфликтов. */
    fun conflictsEnabled(visibility: LessonVisibility): Boolean =
        visibility == LessonVisibility.VISIBLE
}
