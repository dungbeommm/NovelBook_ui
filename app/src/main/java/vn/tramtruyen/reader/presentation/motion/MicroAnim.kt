package vn.tramtruyen.reader.presentation.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.abs

/**
 * Scale ở draw phase: giữ chuyển động nhưng không tạo RenderNode/graphicsLayer
 * thường trực và không remeasure/recompose theo từng frame.
 */
fun Modifier.renderScale(
    pivotFractionX: Float = 0.5f,
    pivotFractionY: Float = 0.5f,
    scaleProvider: () -> Float,
): Modifier = drawWithContent {
    val value = scaleProvider()
    if (abs(value - 1f) < 0.0001f) {
        drawContent()
    } else {
        scale(
            scaleX = value,
            scaleY = value,
            pivot = Offset(size.width * pivotFractionX, size.height * pivotFractionY),
        ) {
            this@drawWithContent.drawContent()
        }
    }
}

/** Lightweight press feedback used by buttons and cards. */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, down: Float = 0.98f): Modifier {
    val pressed = interactionSource.collectIsPressedAsState()
    val animatedScale = animateFloatAsState(
        targetValue = if (pressed.value) down else 1f,
        animationSpec = Motion.PressSpring,
        label = "pressScale",
    )
    return renderScale { animatedScale.value }
}
