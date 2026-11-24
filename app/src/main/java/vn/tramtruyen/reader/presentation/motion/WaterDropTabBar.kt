package vn.tramtruyen.reader.presentation.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 120 Hz tab indicator. Position changes are applied only on the render layer,
 * so a frame never remeasures or replaces the navigation row.
 */
@Composable
fun WaterDropTabBar(
    itemCount: Int,
    selectedIndex: Int,
    positionProvider: (() -> Float)? = null,
    blobColor: Color,
    modifier: Modifier = Modifier,
    blobWidth: Dp = 58.dp,
    blobHeight: Dp = 34.dp,
    blobTopOffset: Dp = 2.dp,
    content: @Composable RowScope.() -> Unit,
) {
    var width by remember { mutableIntStateOf(0) }
    var initialized by remember { mutableStateOf(false) }
    val indicatorX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val targetX = if (selectedIndex in 0 until itemCount && width > 0) {
        with(density) {
            width * (selectedIndex + 0.5f) / itemCount - blobWidth.toPx() / 2f
        }
    } else null
    val blobWidthPx = with(density) { blobWidth.toPx() }

    LaunchedEffect(targetX, positionProvider) {
        // Khi gắn với Pager, vị trí được đọc thẳng ở graphicsLayer bên dưới.
        // Không chạy một Animatable thứ hai vì hai clock lệch nhau gây giật tab.
        if (positionProvider != null) return@LaunchedEffect
        targetX ?: return@LaunchedEffect
        if (!initialized) {
            indicatorX.snapTo(targetX)
            initialized = true
        } else {
            indicatorX.animateTo(targetX, Motion.TabIndicatorSpring)
        }
    }

    Box(modifier.onSizeChanged { width = it.width }) {
        if (targetX != null || positionProvider != null) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(0, with(density) { blobTopOffset.roundToPx() }) }
                    .size(blobWidth, blobHeight)
                    .graphicsLayer {
                        val continuous = positionProvider?.invoke()
                        if (continuous != null && continuous.isFinite() && itemCount > 0) {
                            val clamped = continuous.coerceIn(0f, (itemCount - 1).toFloat())
                            translationX = width * (clamped + 0.5f) / itemCount - blobWidthPx / 2f
                            alpha = if (selectedIndex < 0) 0f else 1f
                        } else {
                            translationX = indicatorX.value
                            alpha = if (selectedIndex < 0) 0f else 1f
                        }
                    }
                    .background(blobColor, RoundedCornerShape(50)),
            )
        }
        Row(Modifier.fillMaxWidth(), content = content)
    }
}
