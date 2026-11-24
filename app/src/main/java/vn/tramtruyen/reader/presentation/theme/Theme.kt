package vn.tramtruyen.reader.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import vn.tramtruyen.reader.model.ColorMode
import vn.tramtruyen.reader.model.UiTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun ReaderTheme(
    uiTheme: UiTheme,
    colorMode: ColorMode,
    seedColor: Color,
    /** Chỉ bật khi người dùng chọn chế độ “Màu ảnh bìa”. */
    followActiveCover: Boolean = false,
    coverImageUrl: String? = null,
    content: @Composable () -> Unit,
) {
    val dark = when (uiTheme) {
        UiTheme.DARK -> true
        UiTheme.LIGHT -> false
        UiTheme.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current

    // Tạo mọi chế độ màu qua cùng một bộ sinh palette Material 3 (Tonal Spot).
    // Trước đây Android 12+ dùng scheme hệ thống trực tiếp, còn màu tự chọn/ảnh
    // bìa dùng MaterialKolor; hai bộ scheme có role/tone khác nhau nên UI lệch màu.
    val useCover = followActiveCover && colorMode == ColorMode.COVER_ART
    val coverSeed = rememberCoverSeedColor(
        imageUrl = if (useCover) coverImageUrl else null,
        fallback = seedColor,
    )
    val wallpaperSeed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching {
            Color(ContextCompat.getColor(context, android.R.color.system_accent1_500))
        }.getOrDefault(seedColor)
    } else {
        seedColor
    }
    val activeSeed = when (colorMode) {
        ColorMode.SYSTEM_WALLPAPER -> wallpaperSeed
        ColorMode.CUSTOM_SEED -> seedColor
        ColorMode.COVER_ART -> coverSeed
    }
    val colorScheme: ColorScheme = rememberDynamicColorScheme(
        seedColor = activeSeed,
        isDark = dark,
        isAmoled = false,
        style = PaletteStyle.TonalSpot,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = PlayfulShapes,
        content = content,
    )
}
