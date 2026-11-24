package vn.tramtruyen.reader.presentation.motion

import kotlin.math.abs

/** Pure, testable frame-budget decisions shared by the Compose UI. */
object UiPerformancePolicy {
    /**
     * Keep the adjacent destination measured before a swipe/tab animation starts.
     * The old value 0 saved a small amount of memory but forced Library/Search
     * (including their first image requests) to compose during the transition,
     * which is exactly where users perceived a hitch. One adjacent page is the
     * best latency/memory trade-off for this three-page app.
     */
    const val PAGER_PRECOMPOSE_PAGES = 1

    /**
     * Giữ swipe + spring animation ở các trang không có scroller ngang. Riêng Home
     * nhường gesture cho LazyRow, và overlay luôn khóa Pager phía dưới.
     */
    fun pagerTouchSwipeEnabled(
        hasNestedHorizontalContent: Boolean,
        overlayVisible: Boolean,
    ): Boolean = !hasNestedHorizontalContent && !overlayVisible

    /** Let navigation/morph finish before DNS, image decode or Palette work begins. */
    const val DEFER_HEAVY_WORK_MS = 330L

    /** Only a large hero image may blend; list/grid covers swap without extra layers. */
    const val HERO_CROSSFADE_MIN_PX = 640
    const val HERO_CROSSFADE_MS = 160

    /** Long LazyColumn animations create many intermediate items; pre-jump near target. */
    const val KARAOKE_DIRECT_SCROLL_THRESHOLD = 12
    const val KARAOKE_PRE_ROLL_ITEMS = 4

    fun continuousTabPosition(currentPage: Int, offsetFraction: Float, pageCount: Int): Float {
        if (pageCount <= 0) return 0f
        return (currentPage.toFloat() + offsetFraction)
            .coerceIn(0f, (pageCount - 1).toFloat())
    }

    fun coverCrossfadeMs(sizeHintPx: Int): Int =
        if (sizeHintPx >= HERO_CROSSFADE_MIN_PX) HERO_CROSSFADE_MS else 0

    fun karaokePreJumpIndex(target: Int, firstVisible: Int): Int? =
        if (abs(target - firstVisible) > KARAOKE_DIRECT_SCROLL_THRESHOLD) {
            (target - KARAOKE_PRE_ROLL_ITEMS).coerceAtLeast(0)
        } else null
}
