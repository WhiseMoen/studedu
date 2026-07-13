package com.sapraliev.studedu.ui.theme

import androidx.compose.ui.graphics.Color

// Пастельная палитра StudEdu.
// Светлая — молочно-голубая; тёмная — в духе Darcula/Obsidian:
// глубокий графит без чистого чёрного, приглушённые пастельные акценты,
// ничего не «светится» и не режет глаз.

// Светлая тема
val CreamBackground = Color(0xFFEEF1F6)
val CreamSurface = Color(0xFFF6F8FB)
val InkPrimary = Color(0xFF5B72C4)      // приглушённый синий
val InkOnPrimary = Color(0xFFFFFFFF)
val PeachSecondary = Color(0xFFE8A587)  // пастельный персик
val SageTertiary = Color(0xFF8FBC9F)    // шалфей

// Тёмная тема (Darcula-грейд: #1E1F22 фон, #26282E поверхности)
val NightBackground = Color(0xFF1E1F22)
val NightSurface = Color(0xFF26282E)
val NightPrimary = Color(0xFF8FA3DD)    // выцветший синий, как ссылки в Darcula
val NightOnPrimary = Color(0xFF15161A)
val NightSecondary = Color(0xFFD8A184)  // тёплый приглушённый персик
val NightTertiary = Color(0xFF8FB89E)   // мягкий шалфей
val NightOnBackground = Color(0xFFCFD2D9) // светло-серый текст, не белый

// Цвета категорий событий (фон карточек)
val PersonalPastel = Color(0xFFCBDDF3)   // личное — голубой
val LessonPastel = Color(0xFFCDE8D5)     // занятие с учеником — зелёный
val DeadlinePastel = Color(0xFFF6D7CE)   // дедлайн — персиковый
val UniversityPastel = Color(0xFFDED3F0) // пара вуза — сиреневый

// Тёмные варианты карточек: тон чуть выше поверхности, малая сатурация
val PersonalPastelDark = Color(0xFF313A4D)
val LessonPastelDark = Color(0xFF2F4237)
val DeadlinePastelDark = Color(0xFF473530)
val UniversityPastelDark = Color(0xFF3B3450)

// Конфликт
val ConflictRed = Color(0xFFD4574E)
val ConflictRedDark = Color(0xFFC96B63) // мягче для тёмного фона

// Неоморфные тени
val NeuLightShadowLight = Color(0xCCFFFFFF)
val NeuDarkShadowLight = Color(0xFFC8CEDA)
val NeuLightShadowDark = Color(0x14FFFFFF)
val NeuDarkShadowDark = Color(0xB3121316)
