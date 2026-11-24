package vn.tramtruyen.reader.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import vn.tramtruyen.reader.presentation.motion.Motion
import vn.tramtruyen.reader.presentation.motion.renderScale

/**
 * Vung bam khong ve lop indication hinh chu nhat mac dinh.
 * Trang thai bam da duoc the/nut Material tu xu ly theo dung shape; custom row/card
 * chi can phan hoi thao tac, tranh mieng xam vuong lo ra ngoai goc bo tron.
 */
fun Modifier.noGrayClick(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed = interaction.collectIsPressedAsState()
    val animatedScale = animateFloatAsState(
        targetValue = if (enabled && pressed.value) 0.985f else 1f,
        animationSpec = Motion.PressSpring,
        label = "surfacePress",
    )
    // Có đầy đủ press animation, nhưng scale chỉ chạy ở draw phase. Không tạo
    // graphicsLayer riêng cho từng cell và không remeasure danh sách.
    renderScale { animatedScale.value }.clickable(
        enabled = enabled,
        interactionSource = interaction,
        indication = null,
        onClick = onClick,
    )
}

/**
 * Long-press variant for library cards. The stock combinedClickable indication
 * is rectangular when attached outside a shaped Surface, which produced the
 * dark blocks seen on some OEM Compose/WebView renderers.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.noGrayCombinedClick(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed = interaction.collectIsPressedAsState()
    val animatedScale = animateFloatAsState(
        targetValue = if (enabled && pressed.value) 0.975f else 1f,
        animationSpec = Motion.PressSpring,
        label = "surfaceCombinedPress",
    )
    renderScale { animatedScale.value }.combinedClickable(
        enabled = enabled,
        interactionSource = interaction,
        indication = null,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}
