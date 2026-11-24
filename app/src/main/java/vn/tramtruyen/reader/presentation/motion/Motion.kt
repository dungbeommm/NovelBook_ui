/*
 * Motion.kt -- nhịp chuyển động dùng chung cho toàn app.
 *
 * Mọi chuyển động đều có đủ thời gian vào/ra và dùng cùng một họ easing/spring.
 * Không cắt ngắn animation để "giả mượt"; thay vào đó state nóng được đọc ở
 * graphicsLayer/draw để mỗi khung hình chỉ chạm tầng render.
 */
package vn.tramtruyen.reader.presentation.motion

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

object Motion {
    /** Tấm player lớn: không nảy quá đích, giữ vận tốc khi bị đổi hướng giữa chừng. */
    val MorphSpring: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    val SwipeSettle: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 260f,
    )

    /** Phản hồi nhấn mềm, không giật bật. */
    val Bouncy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 700f,
    )

    // 250/200/350 ms tương ứng chính xác 15/12/21 frame ở 60 Hz và gấp đôi ở
    // 120 Hz. Cả chiều vào và ra dùng cùng nhịp nên không có cảm giác bị cắt.
    const val NavEnterMs = 250
    const val NavExitMs = 250
    const val NavFadeInMs = 200
    const val NavFadeOutMs = 200
    const val QuickFadeMs = 200
    const val StateChangeMs = 200
    const val ContentChangeMs = 250
    const val ColorChangeMs = 350
    const val WaveCycleMs = 2400

    val EnterEasing = LinearOutSlowInEasing
    val ExitEasing = FastOutLinearInEasing

    val TabIndicatorSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 420f,
    )
    /** Pager navigation preserves velocity when a rapid second tab tap retargets it. */
    val TabPageSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 380f,
    )
    val TabIconSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 440f,
    )
    val PressSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 520f,
    )
    val GestureSettle: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 360f,
    )
    val SelectionSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 360f,
    )
}

fun lerpF(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

fun rampF(value: Float, from: Float, to: Float): Float =
    if (to <= from) 0f else ((value - from) / (to - from)).coerceIn(0f, 1f)
