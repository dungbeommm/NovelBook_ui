package vn.tramtruyen.reader.presentation.theme

import androidx.compose.ui.graphics.Color

/** Parse a #rrggbb / #aarrggbb hex string into a Compose Color (safe fallback). */
fun hex(value: String, fallback: Color = Color(0xFF9575CD)): Color = try {
    val s = value.removePrefix("#")
    when (s.length) {
        6 -> Color(0xFF000000 or s.toLong(16))
        8 -> Color(s.toLong(16))
        else -> fallback
    }
} catch (e: Exception) {
    fallback
}

