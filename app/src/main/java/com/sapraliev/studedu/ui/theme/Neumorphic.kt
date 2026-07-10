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
 * «до события», FAB) — базовые списки остаются плоским Material 3,
 * иначе страдает читаемость (см. docs/PLAN.md).
 *
 * Элемент должен лежать на фоне того же тона, что и его поверхность, —
 * иначе иллюзия объёма ломается.
 */
fun Modifier.neumorphic(
    shadows: NeuShadows,
    cornerRadius: Dp = 24.dp,
    blur: Dp = 14.dp,
    offset: Dp = 6.dp,
): Modifier = drawBehind {
    val radiusPx = cornerRadius.toPx()
    val blurPx = blur.toPx()
    val offsetPx = offset.toPx()
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.isAntiAlias = true
        frameworkPaint.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)

        frameworkPaint.color = shadows.dark.toArgb()
        canvas.drawRoundRect(
            left = offsetPx, top = offsetPx,
            right = size.width + offsetPx, bottom = size.height + offsetPx,
            radiusX = radiusPx, radiusY = radiusPx, paint = paint,
        )
        frameworkPaint.color = shadows.light.toArgb()
        canvas.drawRoundRect(
            left = -offsetPx, top = -offsetPx,
            right = size.width - offsetPx, bottom = size.height - offsetPx,
            radiusX = radiusPx, radiusY = radiusPx, paint = paint,
        )
    }
}
