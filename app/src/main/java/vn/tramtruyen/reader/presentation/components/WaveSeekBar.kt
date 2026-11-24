/*
 * WaveSeekBar -- thanh tua dạng sóng.
 *
 * Pha sóng chỉ được đọc trong draw phase, vì vậy animation không recompose hay
 * remeasure cây Player. Khi tạm dừng coroutine dừng hẳn; khi phát, vòng lặp nối
 * 2π về 0 (cùng một hình) nên không có khung hình bị cắt.
 */
package vn.tramtruyen.reader.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.tramtruyen.reader.presentation.motion.Motion
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    thumbColor: Color,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    waveLength: Dp = 22.dp,
    amplitude: Dp = 3.dp,
    stroke: Dp = 4.dp,
    thumbRadius: Dp = 7.dp,
) {
    val v = value.coerceIn(0f, 1f)
    val density = LocalDensity.current
    val onChange by rememberUpdatedState(onValueChange)
    val phase = remember { Animatable(0f) }
    val path = remember { Path() }
    val amplitudePx = remember(density, amplitude) { with(density) { amplitude.toPx() } }
    val waveLengthPx = remember(density, waveLength) {
        with(density) { waveLength.toPx() }.coerceAtLeast(1f)
    }
    val strokePx = remember(density, stroke) { with(density) { stroke.toPx() } }
    val thumbRadiusPx = remember(density, thumbRadius) { with(density) { thumbRadius.toPx() } }
    val minimumStepPx = remember(density) { with(density) { 2.dp.toPx() }.coerceAtLeast(3f) }

    LaunchedEffect(animate) {
        if (!animate) {
            phase.stop()
            phase.snapTo(0f)
            return@LaunchedEffect
        }
        phase.snapTo(0f)
        while (isActive) {
            phase.animateTo(
                targetValue = (2.0 * PI).toFloat(),
                animationSpec = tween(Motion.WaveCycleMs, easing = LinearEasing),
            )
            // 2π và 0 cho đúng cùng vị trí sóng nên bước nối này không nhìn thấy.
            phase.snapTo(0f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (size.width > 0) onChange((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (size.width > 0) onChange((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            },
    ) {
        val w = size.width
        val cy = size.height / 2f
        val activeEnd = w * v
        val livePhase = if (animate) phase.value else 0f

        path.rewind()
        path.moveTo(0f, cy)
        var x = 0f
        // Tối đa khoảng 180 đoạn cho phần active. Trên tablet/1440p cách cũ có
        // thể tính sin và nối Path hơn 500 lần mỗi frame ở 120 Hz.
        val stepPx = maxOf(minimumStepPx, activeEnd / 180f)
        while (x <= activeEnd) {
            val fade = ((activeEnd - x) / (waveLengthPx * 0.6f)).coerceIn(0f, 1f)
            val y = cy + amplitudePx * fade *
                sin((x / waveLengthPx) * 2.0 * PI + livePhase).toFloat()
            path.lineTo(x, y)
            x += stepPx
        }
        drawPath(path, color = activeColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))

        if (activeEnd < w) {
            drawLine(
                color = inactiveColor,
                start = Offset(activeEnd, cy),
                end = Offset(w, cy),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(
            color = thumbColor,
            radius = thumbRadiusPx,
            center = Offset(activeEnd, cy),
        )
    }
}
