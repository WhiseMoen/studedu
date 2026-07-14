package com.sapraliev.studedu.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Пара теней для неоморфизма: свет сверху-слева, тень снизу-справа. */
data class NeuShadows(val light: Color, val dark: Color)

val LocalNeuShadows = staticCompositionLocalOf {
    NeuShadows(NeuLightShadowLight, NeuDarkShadowLight)
}

/**
 * Неоморфная «выпуклость». Используется точечно (часы, виджет
 * «до события», FAB, акцентные кнопки) — базовые списки остаются плоским
 * Material 3, иначе страдает читаемость (см. docs/PLAN.md).
 *
 * Элемент должен лежать на фоне того же тона, что и его поверхность, —
 * иначе иллюзия объёма ломается.
 *
 * [pressed] — эффект «вдавливания»: тени меняются местами (тёмная
 * сверху-слева, светлая снизу-справа) и смещение уменьшается вдвое,
 * имитируя нажатую кнопку.
 */
fun Modifier.neumorphic(
    shadows: NeuShadows,
    cornerRadius: Dp = 24.dp,
    blur: Dp = 14.dp,
    offset: Dp = 6.dp,
    pressed: Boolean = false,
): Modifier = drawBehind {
    val radiusPx = cornerRadius.toPx()
    val blurPx = blur.toPx()
    val offsetPx = (if (pressed) offset / 2 else offset).toPx()
    val bottomRightColor = if (pressed) shadows.light else shadows.dark
    val topLeftColor = if (pressed) shadows.dark else shadows.light
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.isAntiAlias = true
        frameworkPaint.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)

        frameworkPaint.color = bottomRightColor.toArgb()
        canvas.drawRoundRect(
            left = offsetPx, top = offsetPx,
            right = size.width + offsetPx, bottom = size.height + offsetPx,
            radiusX = radiusPx, radiusY = radiusPx, paint = paint,
        )
        frameworkPaint.color = topLeftColor.toArgb()
        canvas.drawRoundRect(
            left = -offsetPx, top = -offsetPx,
            right = size.width - offsetPx, bottom = size.height - offsetPx,
            radiusX = radiusPx, radiusY = radiusPx, paint = paint,
        )
    }
}
