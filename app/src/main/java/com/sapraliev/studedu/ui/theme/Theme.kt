package com.sapraliev.studedu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// База — читаемый пастельный Material 3.
// Dynamic color (Material You) сознательно выключен: он перекрасил бы
// палитру в цвета обоев. Неоморфизм — только точечные акценты.

private val LightColors = lightColorScheme(
    primary = InkPrimary,
    onPrimary = InkOnPrimary,
    secondary = PeachSecondary,
    tertiary = SageTertiary,
    background = CreamBackground,
    surface = CreamSurface,
    error = ConflictRed,
)

private val DarkColors = darkColorScheme(
    primary = NightPrimary,
    secondary = NightSecondary,
    tertiary = NightTertiary,
    background = NightBackground,
    surface = NightSurface,
    error = ConflictRed,
)

@Composable
fun StudeduTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val neuShadows = if (darkTheme) {
        NeuShadows(light = NeuLightShadowDark, dark = NeuDarkShadowDark)
    } else {
        NeuShadows(light = NeuLightShadowLight, dark = NeuDarkShadowLight)
    }

    CompositionLocalProvider(LocalNeuShadows provides neuShadows) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

/** Пастельный цвет карточки по типу события. */
object EventPalette {
    @Composable
    fun personal(darkTheme: Boolean = isSystemInDarkTheme()) =
        if (darkTheme) PersonalPastelDark else PersonalPastel

    @Composable
    fun lesson(darkTheme: Boolean = isSystemInDarkTheme()) =
        if (darkTheme) LessonPastelDark else LessonPastel

    @Composable
    fun deadline(darkTheme: Boolean = isSystemInDarkTheme()) =
        if (darkTheme) DeadlinePastelDark else DeadlinePastel

    @Composable
    fun university(darkTheme: Boolean = isSystemInDarkTheme()) =
        if (darkTheme) UniversityPastelDark else UniversityPastel
}
