package vn.tramtruyen.reader.presentation.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import kotlin.math.hypot
import kotlin.math.max

/**
 * Circular theme reveal: the old surface remains above the newly selected
 * theme, then a transparent circle grows from the tapped tile.
 */
@Stable
class ThemeRevealState {
    private val progress = Animatable(1f)
    private var generation = 0L
    var active by mutableStateOf(false)
        private set
    var origin by mutableStateOf(Offset.Zero)
        private set
    var oldSurface by mutableStateOf(Color.Transparent)
        private set

    suspend fun reveal(
        from: Offset,
        oldColor: Color,
        switchTheme: () -> Unit,
    ) {
        val ticket = ++generation
        progress.stop()
        origin = from
        oldSurface = oldColor
        progress.snapTo(0f)
        active = true
        switchTheme()
        try {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 520,
                    easing = Motion.EnterEasing,
                ),
            )
        } finally {
            if (ticket == generation) active = false
        }
    }

    internal fun fraction(): Float = progress.value
}

val LocalThemeRevealState = staticCompositionLocalOf<ThemeRevealState?> { null }

@Composable
fun rememberThemeRevealState(): ThemeRevealState = remember { ThemeRevealState() }

@Composable
fun ThemeRevealOverlay(
    state: ThemeRevealState,
    modifier: Modifier = Modifier,
) {
    if (!state.active) return
    val mask = remember { Path().apply { fillType = PathFillType.EvenOdd } }
    Canvas(modifier.fillMaxSize()) {
        val x = state.origin.x.coerceIn(0f, size.width)
        val y = state.origin.y.coerceIn(0f, size.height)
        val farX = max(x, size.width - x)
        val farY = max(y, size.height - y)
        val maxRadius = hypot(farX, farY)
        val radius = maxRadius * state.fraction()
        mask.rewind()
        mask.fillType = PathFillType.EvenOdd
        mask.addRect(Rect(Offset.Zero, size))
        mask.addOval(
            Rect(
                left = x - radius,
                top = y - radius,
                right = x + radius,
                bottom = y + radius,
            ),
        )
        // EvenOdd punches the circular opening directly. No full-screen
        // offscreen layer and no BlendMode.Clear pass are needed.
        drawPath(mask, state.oldSurface)
    }
}