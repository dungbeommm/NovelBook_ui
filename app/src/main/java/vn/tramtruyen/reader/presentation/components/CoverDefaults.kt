package vn.tramtruyen.reader.presentation.components

import androidx.annotation.DrawableRes
import vn.tramtruyen.reader.R
import kotlin.math.absoluteValue

object CoverDefaults {
    private val covers=intArrayOf(R.drawable.nr_cover_01,R.drawable.nr_cover_02,R.drawable.nr_cover_03,R.drawable.nr_cover_04,R.drawable.nr_cover_05,R.drawable.nr_cover_06,R.drawable.nr_cover_07,R.drawable.nr_cover_08,R.drawable.nr_cover_09,R.drawable.nr_cover_10)
    // Average tones sampled from the bundled fallback covers, in matching order.
    private val coverColors = arrayOf("#B3AAA0", "#49261B", "#7E6047", "#1F1F1F",
        "#575757", "#796056", "#746C63", "#544644", "#788551", "#554644")
    private fun index(key: String) =
        (key.ifBlank { "novelreader" }.hashCode().toLong().absoluteValue % covers.size).toInt()
    @DrawableRes fun forKey(key:String):Int=covers[index(key)]
    fun colorForKey(key: String): String = coverColors[index(key)]
}
