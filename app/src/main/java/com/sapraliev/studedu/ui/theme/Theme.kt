package com.sapraliev.studedu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.sapraliev.studedu.data.settings.ThemeMode

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
    onPrimary = NightOnPrimary,
    secondary = NightSecondary,
    tertiary = NightTertiary,
    background = NightBackground,
    surface = NightSurface,
    onBackground = NightOnBackground,
    onSurface = NightOnBackground,
    error = ConflictRedDark,
)

/** Тёмная ли тема сейчас — для выбора пастелей карточек. */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun StudeduTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val neuShadows = if (darkTheme) {
        NeuShadows(light = NeuLightShadowDark, dark = NeuDarkShadowDark)
    } else {
        NeuShadows(light = NeuLightShadowLight, dark = NeuDarkShadowLight)
    }

    CompositionLocalProvider(
        LocalNeuShadows provides neuShadows,
        LocalIsDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

/**
 * Пастельный цвет карточки по типу события.
 * Читает LocalIsDarkTheme — уважает выбранный в настройках режим,
 * а не только системный.
 */
object EventPalette {
    @Composable
    fun personal(): Color =
        if (LocalIsDarkTheme.current) PersonalPastelDark else PersonalPastel

    @Composable
    fun lesson(): Color =
        if (LocalIsDarkTheme.current) LessonPastelDark else LessonPastel

    @Composable
    fun deadline(): Color =
        if (LocalIsDarkTheme.current) DeadlinePastelDark else DeadlinePastel

    @Composable
    fun university(): Color =
        if (LocalIsDarkTheme.current) UniversityPastelDark else UniversityPastel
}
