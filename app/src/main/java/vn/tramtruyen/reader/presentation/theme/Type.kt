package vn.tramtruyen.reader.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * MD3 type scale tuned to match the web app (Roboto Flex, weights 400/500).
 * Uses the platform default sans (Roboto on Android) which is exactly Roboto Flex's base.
 */
val AppTypography = Typography().run {
    val f = FontFamily.Default
    copy(
        headlineMedium = headlineMedium.copy(fontFamily = f, fontWeight = FontWeight.Normal, fontSize = 26.sp),
        headlineSmall = headlineSmall.copy(fontFamily = f, fontWeight = FontWeight.Normal, fontSize = 24.sp),
        titleLarge = titleLarge.copy(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 22.sp),
        titleMedium = titleMedium.copy(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 16.sp),
        titleSmall = titleSmall.copy(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        bodyLarge = bodyLarge.copy(fontFamily = f, fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium = bodyMedium.copy(fontFamily = f, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        bodySmall = bodySmall.copy(fontFamily = f, fontWeight = FontWeight.Normal, fontSize = 12.5f.sp),
        labelLarge = labelLarge.copy(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        labelMedium = labelMedium.copy(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 12.sp),
        labelSmall = labelSmall.copy(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 11.sp),
    )
}

val OverlineLabel = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 1.4.sp,
)
