package vn.tramtruyen.reader.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import vn.tramtruyen.reader.presentation.motion.UiPerformancePolicy
import vn.tramtruyen.reader.presentation.theme.hex

object Radii {
    val xs = 8.dp; val sm = 12.dp; val md = 16.dp; val lg = 20.dp; val xl = 28.dp
}

/**
 * Fast render-cache identity. Coil hashes disk keys internally, so a SHA-256
 * plus byte/hex allocations on every newly visible cell only wastes UI time.
 */
internal fun fastCoverCacheKey(identity: String): String {
    var hash = -3750763034362895579L // FNV-1a 64-bit offset basis.
    identity.forEach { char ->
        hash = hash xor char.code.toLong()
        hash *= 1099511628211L
    }
    return "cover:${hash.toULong().toString(16)}"
}

fun coverBrush(colorHex: String): Brush {
    val base = hex(colorHex, Color(0xFF9575CD))
    val top = lerp(base, Color.White, 0.14f)
    val bottom = lerp(base, Color.Black, 0.30f)
    return Brush.linearGradient(listOf(top, bottom))
}

/**
 * Cover art with local gradient fallback + sized Coil request (no full-res decode).
 * @param sizeHintPx approximate pixel size for decode (e.g. 128 / 256 / 512).
 */
@Composable
fun CoverArt(
    colorHex: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    corner: Dp = Radii.md,
    label: String? = null,
    sizeHintPx: Int = 256,
    fallbackKey: String = label.orEmpty(),
    // Optional shape override (e.g. a playful tucked-corner silhouette for hero art).
    shape: Shape = RoundedCornerShape(corner),
) {
    val context = LocalContext.current
    val fallback = CoverDefaults.forKey(fallbackKey.ifBlank { imageUrl ?: colorHex })
    val fallbackPainter = painterResource(fallback)
    val backgroundBrush = remember(colorHex) { coverBrush(colorHex) }
    // Some sources reuse one URL for different novels (or return cover images
    // personalized by request headers). Include the story identity so a recycled
    // search row cannot display another story's cached artwork.
    val coverCacheKey = remember(fallbackKey, imageUrl, sizeHintPx) {
        imageUrl?.let { url ->
            val identity = "${fallbackKey.ifBlank { url }}|$url|$sizeHintPx"
            fastCoverCacheKey(identity)
        }
    }
    val labelScrim = remember {
        Brush.verticalGradient(
            0.45f to Color.Transparent,
            1f to Color(0x99000000),
        )
    }
    val imageRequest = remember(context, imageUrl, sizeHintPx, coverCacheKey) {
        imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .apply {
                    if (runCatching {
                            java.net.URI(url).host.orEmpty()
                                .endsWith("truyennet.org", ignoreCase = true)
                        }.getOrDefault(false)
                    ) {
                        addHeader("Referer", "https://truyennet.org/")
                        addHeader(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 13; SM-G991B) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/122.0.0.0 Mobile Safari/537.36"
                        )
                        // Cookie is attached by TramTruyenApplication's OkHttp interceptor on
                        // an I/O thread, never while this list cell composes.
                    }
                }
                .apply {
                    coverCacheKey?.let { key ->
                        memoryCacheKey(key)
                        diskCacheKey(key)
                    }
                }
                // Cell LazyColumn/LazyGrid không chạy transition ảnh riêng: nhiều
                // crossfade đồng thời trong lúc fling làm tăng layer blending.
                // Chỉ ảnh hero lớn mới cần fade nhẹ khi tải xong.
                .apply {
                    val duration = UiPerformancePolicy.coverCrossfadeMs(sizeHintPx)
                    if (duration > 0) crossfade(duration) else crossfade(false)
                }
                .size(sizeHintPx)
                .precision(Precision.INEXACT)
                .scale(Scale.FILL)
                .allowHardware(true)
                .build()
        }
    }
    Box(
        modifier
            .clip(shape)
            .background(backgroundBrush),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (imageRequest == null) {
            androidx.compose.foundation.Image(
                fallbackPainter,
                null,
                Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            AsyncImage(
                model = imageRequest,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                placeholder = fallbackPainter,
                error = fallbackPainter,
                fallback = fallbackPainter,
                modifier = Modifier.matchParentSize(),
            )
        }
        if (label != null) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(labelScrim)
            )
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        trailing?.invoke()
    }
}
