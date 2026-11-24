package vn.tramtruyen.reader.presentation.theme

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import vn.tramtruyen.reader.presentation.motion.UiPerformancePolicy
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Small process cache shared by the app theme and full player. */
private val coverSeedCache = ConcurrentHashMap<String, Color>()
private val coverSeedMutex = Mutex()

/**
 * Trich mau chu dao cua anh bia [imageUrl] de lam mau goc cho toan bo giao dien.
 *
 * Luon tra [fallback] ngay lap tuc (va mai mai, neu that bai), roi thay bang tone
 * lay duoc tu anh khi giai ma xong -- nen giao dien khong bao gio phai cho, cung
 * khong nhap nhay mot khung hinh khong mau.
 *
 * Chọn swatch muted/dominant trước để tránh màu bão hòa gắt từ ảnh bìa. Màu này
 * chỉ là seed; Material 3 sẽ tạo lại đầy đủ tonal roles từ seed đó.
 */
@Composable
fun rememberCoverSeedColor(imageUrl: String?, fallback: Color): Color {
    val context = LocalContext.current
    val cached = imageUrl?.let(coverSeedCache::get)
    val state = produceState(initialValue = cached ?: fallback, imageUrl, fallback) {
        value = cached ?: fallback
        if (!imageUrl.isNullOrBlank()) {
            coverSeedCache[imageUrl]?.let {
                value = it
                return@produceState
            }
            delay(UiPerformancePolicy.DEFER_HEAVY_WORK_MS)
            val sampled = coverSeedMutex.withLock {
                coverSeedCache[imageUrl] ?: runCatching {
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .size(64, 64)
                        .allowHardware(false)
                        .build()
                    val bitmap = (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
                        ?: return@runCatching null
                    withContext(Dispatchers.Default.limitedParallelism(1)) {
                        paletteColor(bitmap, fallback)
                    }
                }.getOrNull()?.also { color ->
                    if (coverSeedCache.size >= 12) coverSeedCache.clear()
                    coverSeedCache[imageUrl] = color
                }
            }
            if (sampled != null) {
                value = sampled
            }
        }
    }
    return state.value
}

/** Chon tone dai dien bang Palette, co duong lui ve mau trung binh. */
private fun paletteColor(bitmap: Bitmap, fallback: Color): Color {
    val rgb = runCatching {
        val palette = Palette.from(bitmap).maximumColorCount(12).clearFilters().generate()
        palette.mutedSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: palette.lightMutedSwatch?.rgb
            ?: palette.darkMutedSwatch?.rgb
            ?: palette.vibrantSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.darkVibrantSwatch?.rgb
    }.getOrNull() ?: return averageColor(bitmap, fallback)
    return Color(rgb)
}

/** Trung binh mau theo luoi thua -- luoi an toan khi Palette khong tim duoc gi. */
private fun averageColor(bitmap: Bitmap, fallback: Color): Color {
    val w = bitmap.width.coerceAtLeast(1)
    val h = bitmap.height.coerceAtLeast(1)
    var r = 0L
    var g = 0L
    var b = 0L
    var count = 0L
    val stepX = (w / 12).coerceAtLeast(1)
    val stepY = (h / 12).coerceAtLeast(1)
    var y = 0
    while (y < h) {
        var x = 0
        while (x < w) {
            val px = bitmap.getPixel(x, y)
            r += (px shr 16) and 0xFF
            g += (px shr 8) and 0xFF
            b += px and 0xFF
            count++
            x += stepX
        }
        y += stepY
    }
    if (count == 0L) return fallback
    return Color(
        red = ((r / count).toInt().coerceIn(0, 255)) / 255f,
        green = ((g / count).toInt().coerceIn(0, 255)) / 255f,
        blue = ((b / count).toInt().coerceIn(0, 255)) / 255f,
    )
}
