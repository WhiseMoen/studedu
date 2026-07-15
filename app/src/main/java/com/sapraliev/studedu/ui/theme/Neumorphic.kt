package com.sapraliev.studedu.ui.theme

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.RectF
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor

/** Пара теней для неоморфизма: свет сверху-слева, тень снизу-справа. */
data class NeuShadows(val light: Color, val dark: Color)

val LocalNeuShadows = staticCompositionLocalOf {
    NeuShadows(NeuLightShadowLight, NeuDarkShadowLight)
}

/**
 * Неоморфная «выпуклость». Используется точечно (часы, календарь, FAB,
 * акцентные кнопки) — базовые списки остаются плоским Material 3, иначе
 * страдает читаемость (см. docs/PLAN.md).
 *
 * Элемент должен лежать на фоне того же тона, что и его поверхность, —
 * иначе иллюзия объёма ломается.
 *
 * [pressed] — эффект «вдавливания»: тени меняются местами и смещение
 * уменьшается вдвое. Переход анимированный (а не мгновенный переброс),
 * чтобы нажатие не выглядело резким скачком.
 *
 * Тени рисуются на отдельном software-битмапе: `BlurMaskFilter` не
 * работает на аппаратно ускоренном канвасе Compose (рисует чёткий
 * нерасмытый край вместо мягкой тени — источник «пикселей» по краям).
 * Софтверный `Bitmap`/`Canvas` гарантированно растеризует блюр как надо;
 * сам битмап переиспользуется между кадрами ([remember]), пересоздаётся
 * только при смене размера.
 */
fun Modifier.neumorphic(
    shadows: NeuShadows,
    cornerRadius: Dp = 24.dp,
    blur: Dp = 14.dp,
    offset: Dp = 6.dp,
    pressed: Boolean = false,
): Modifier = composed {
    val animatedOffset by animateDpAsState(
        targetValue = if (pressed) offset / 2 else offset,
        animationSpec = tween(durationMillis = 180),
        label = "neu-offset",
    )
    val topLeftColor by animateColorAsState(
        targetValue = if (pressed) shadows.dark else shadows.light,
        animationSpec = tween(durationMillis = 180),
        label = "neu-top-left",
    )
    val bottomRightColor by animateColorAsState(
        targetValue = if (pressed) shadows.light else shadows.dark,
        animationSpec = tween(durationMillis = 180),
        label = "neu-bottom-right",
    )

    var cachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cachedWidth by remember { mutableStateOf(0) }
    var cachedHeight by remember { mutableStateOf(0) }

    // pad/width/height считаются от статичного [offset] (не от animatedOffset) —
    // иначе округление анимированного смещения меняется почти каждый кадр и
    // бьёт кэш ниже, пересоздавая Bitmap на каждый тик анимации нажатия.
    val staticPadDp = remember(blur, offset) { (blur.value * 2.5f + offset.value).dp }

    drawBehind {
        val radiusPx = cornerRadius.toPx()
        val blurPx = blur.toPx()
        val offsetPx = animatedOffset.toPx()
        val pad = staticPadDp.toPx().roundToInt().coerceAtLeast(1)
        val width = size.width.roundToInt() + pad * 2
        val height = size.height.roundToInt() + pad * 2
        if (width <= 0 || height <= 0) return@drawBehind

        var bitmap = cachedBitmap
        if (bitmap == null || cachedWidth != width || cachedHeight != height) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            cachedBitmap = bitmap
            cachedWidth = width
            cachedHeight = height
        } else {
            bitmap.eraseColor(AndroidColor.TRANSPARENT)
        }

        val softCanvas = AndroidCanvas(bitmap)
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.isAntiAlias = true
        frameworkPaint.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)

        frameworkPaint.color = bottomRightColor.toArgb()
        softCanvas.drawRoundRect(
            RectF(
                pad + offsetPx, pad + offsetPx,
                pad + size.width + offsetPx, pad + size.height + offsetPx,
            ),
            radiusPx, radiusPx, frameworkPaint,
        )
        frameworkPaint.color = topLeftColor.toArgb()
        softCanvas.drawRoundRect(
            RectF(
                pad - offsetPx, pad - offsetPx,
                pad + size.width - offsetPx, pad + size.height - offsetPx,
            ),
            radiusPx, radiusPx, frameworkPaint,
        )

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawBitmap(bitmap, -pad.toFloat(), -pad.toFloat(), null)
        }
    }
}
