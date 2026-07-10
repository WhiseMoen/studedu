package com.sapraliev.studedu.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// База — читаемый, контрастный Material 3.
// Неоморфизм — только точечные акценты на главном экране (Этап 5).

private val LightColors = lightColorScheme(
    primary = Color(0xFF2B2D42),
    secondary = Color(0xFFEF8354),
    tertiary = Color(0xFF4F5D75),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBFC0D9),
    secondary = Color(0xFFEF8354),
    tertiary = Color(0xFFBFC9DB),
)

@Composable
fun StudeduTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color (Material You) выключен: он перекрасил бы нашу
    // пастельную палитру в цвета обоев пользователя. Вернуть как опцию
    // в настройках — если захочется.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalConte