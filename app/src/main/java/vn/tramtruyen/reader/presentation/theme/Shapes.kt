package vn.tramtruyen.reader.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Playful shape language for the app's Material theme: rounder and roomier
 * than stock Material 3 defaults so every default-shaped surface (buttons,
 * dialogs, chips, cards) already reads friendlier without a per-component
 * override.
 */
val PlayfulShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Signature asymmetric "tucked corner" shape used for hero cover art and the
 * player's primary action — one corner stays tight while the rest stay
 * roomy, giving the app a distinct, hand-drawn silhouette instead of a
 * uniform rounded rectangle.
 */
fun tuckedCornerShape(radius: Dp, tuck: Dp = radius * 0.32f): Shape = RoundedCornerShape(
    topStart = radius,
    topEnd = radius,
    bottomEnd = tuck,
    bottomStart = radius,
)
