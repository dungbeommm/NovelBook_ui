package vn.tramtruyen.reader

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import vn.tramtruyen.reader.model.FrameRateMode
import vn.tramtruyen.reader.presentation.ReaderNavigation
import vn.tramtruyen.reader.presentation.PlayerViewModel
import vn.tramtruyen.reader.presentation.collectAsStateSafe
import vn.tramtruyen.reader.presentation.motion.HapticConfig
import vn.tramtruyen.reader.presentation.motion.LocalHapticConfig
import vn.tramtruyen.reader.presentation.motion.LocalThemeRevealState
import vn.tramtruyen.reader.presentation.motion.ThemeRevealOverlay
import vn.tramtruyen.reader.presentation.motion.rememberThemeRevealState
import vn.tramtruyen.reader.presentation.theme.ReaderTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReaderActivity : ComponentActivity() {

    private val vm: PlayerViewModel by viewModels()
    private var mediaNotifier: ReadingMediaNotifier? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            // Cho frame Compose dau tien duoc ve xong roi moi mo dialog he thong;
            // tranh chen viec WindowManager vao cold-start tren Android 16.
            window.decorView.post {
                if (!isFinishing && !isDestroyed) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001
                    )
                }
            }
        }

        mediaNotifier = ReadingMediaNotifier(this)

        setContent {
            // Theme chỉ theo dõi field màu/giao diện — không dính positionMs.
            // seed Monet lấy từ cài đặt (currentSeedColor), KHÔNG bị story.color ghi đè.
            val ui by vm.contentUi.collectAsStateSafe()
            val activeStory = ui.activeStory?.takeIf { story ->
                story.id != vn.tramtruyen.reader.model.placeholderStory.id
            }
            val followCover = ui.colorMode == vn.tramtruyen.reader.model.ColorMode.COVER_ART &&
                activeStory != null
            val themeReveal = rememberThemeRevealState()
            CompositionLocalProvider(LocalThemeRevealState provides themeReveal) {
                ReaderTheme(
                    uiTheme = ui.uiTheme,
                    colorMode = ui.colorMode,
                    seedColor = vm.currentSeedColor(),
                    followActiveCover = followCover,
                    coverImageUrl = if (followCover) activeStory?.coverUrl else null,
                ) {
                    CompositionLocalProvider(
                        LocalHapticConfig provides HapticConfig(
                            enabled = ui.hapticEnabled,
                            intensity = ui.hapticIntensity,
                        ),
                    ) {
                        Box(Modifier) {
                            ReaderNavigation(vm)
                            ThemeRevealOverlay(themeReveal)
                        }
                    }
                }
            }
        }

        // Apply preferred refresh rate from settings
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.ui.map { it.frameRateMode }.distinctUntilChanged().collect { mode ->
                    applyFrameRate(mode)
                }
            }
        }

        // The ViewModel now drives ReadingPlaybackService directly, including while this
        // Activity is STOPPED behind the lock screen. This collector only owns the
        // Activity-specific keep-screen-on flag.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.ui.map { it.playback }.distinctUntilChanged().collect { meta ->
                    setKeepScreenOn(meta.isPlaying)
                }
            }
        }

        handleMediaIntent(intent)
    }

    /** Che do khung hinh dang ap dung -- dat lai moi lan cua so lay lai focus. */
    private var frameRateMode: FrameRateMode = FrameRateMode.AUTO

    /**
     * Ep man hinh chay o TAN SO QUET CAO NHAT ma phan cung ho tro.
     *
     * Ba tang chong len nhau, tang sau bu cho tang truoc khi ROM bo qua:
     *   1. preferredDisplayModeId -- chon dung display mode 90/120/144Hz.
     *   2. preferredRefreshRate -- nhac lai muc Hz mong muon cho cac ROM chi doc
     *      truong nay ma bo qua modeId.
     *   3. requestedFrameRate = CATEGORY_HIGH (Android 15+) -- bao voi lop ghep
     *      hinh moi cua he thong rang cua so nay can nhip cao, chong viec he
     *      thong tu ha ve 60Hz khi thay man hinh "it thay doi".
     *
     * Luu y: preferredMin/MaxDisplayRefreshRate KHONG nam trong SDK cong khai
     * (chi la truong @hide cua AOSP) nen khong dung duoc o day.
     */
    private fun applyFrameRate(mode: FrameRateMode) {
        frameRateMode = mode
        try {
            // AUTO means adaptive/system managed. Forcing the highest panel mode
            // permanently shrinks the frame budget to 6.9-8.3 ms on 120/144 Hz
            // devices and makes otherwise small Compose work visibly janky.
            if (mode == FrameRateMode.AUTO) {
                val lp = window.attributes
                lp.preferredDisplayModeId = 0
                @Suppress("DEPRECATION")
                lp.preferredRefreshRate = 0f
                window.attributes = lp
                applyAdaptiveFrameRateCategory()
                return
            }
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return

            val modes = display.supportedModes
            if (modes.isNullOrEmpty()) return

            // Chi xet cac mode CUNG do phan giai voi mode dang chay, de khong vo
            // tinh ha net man hinh chi de doi lay Hz cao.
            val active = display.mode
            val candidates = modes.filter {
                it.physicalWidth == active.physicalWidth && it.physicalHeight == active.physicalHeight
            }.ifEmpty { modes.toList() }

            val maxHz = candidates.maxOf { it.refreshRate }
            val wantHz = when (mode) {
                FrameRateMode.FPS_60 -> 60f
                FrameRateMode.FPS_90 -> 90f
                FrameRateMode.FPS_120 -> 120f
                FrameRateMode.AUTO -> maxHz
            }.coerceAtMost(maxHz)

            // Tran FPS la TRAN THAT: lay mode CAO NHAT khong vuot muc mong muon
            // (cong dung sai 0.5Hz). Cach cu lay mode thap nhat >= wantHz - 0.5
            // nen xin 90Hz tren panel {60,120} lai chon 120Hz -- vuot tran nguoi
            // dung dat ma khong bao. Khong co mode nao duoi tran thi moi lay
            // mode thap nhat san co.
            val best = candidates
                .filter { it.refreshRate <= wantHz + 0.5f }
                .maxByOrNull { it.refreshRate }
                ?: candidates.minByOrNull { it.refreshRate }
                ?: return

            val lp = window.attributes
            lp.preferredDisplayModeId = best.modeId
            @Suppress("DEPRECATION")
            lp.preferredRefreshRate = best.refreshRate
            window.attributes = lp
            applyHighFrameRateCategory()
        } catch (_: Exception) {}
    }

    /** Android 15+: danh dau cua so nay thuoc nhom can nhip khung hinh cao. */
    @Suppress("NewApi")
    private fun applyHighFrameRateCategory() {
        if (Build.VERSION.SDK_INT < 35) return
        try {
            window.decorView.requestedFrameRate = View.REQUESTED_FRAME_RATE_CATEGORY_HIGH
        } catch (_: Throwable) {}
    }

    /** Android 15+: return AUTO mode to the platform's adaptive/default policy. */
    @Suppress("NewApi")
    private fun applyAdaptiveFrameRateCategory() {
        if (Build.VERSION.SDK_INT < 35) return
        try {
            window.decorView.requestedFrameRate = View.REQUESTED_FRAME_RATE_CATEGORY_NORMAL
        } catch (_: Throwable) {}
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Nhieu ROM tra cua so ve 60Hz sau khi bi che khuat (thong bao, da nhiem,
        // khoa man hinh) -> dat lai moi lan quay tro lai.
        if (hasFocus) applyFrameRate(frameRateMode)
    }

    private fun setKeepScreenOn(on: Boolean) = runOnUiThread {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun jsPlay() { runOnUiThread { vm.togglePlayIfPaused() } }
    fun jsPause() { runOnUiThread { vm.pauseFromMedia() } }
    fun jsNextChapter() { runOnUiThread { vm.nextChapter() } }
    fun jsPrevChapter() { runOnUiThread { vm.prevChapter() } }
    fun jsSeek(fraction: Double) { runOnUiThread { vm.seekTo(fraction.toFloat()) } }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleMediaIntent(intent)
    }

    private fun handleMediaIntent(intent: Intent?) {
        // Duong du phong neu co Intent nao do van mo Activity voi action cu.
        // Duong chinh (MediaSession + nut notification) di qua ReadingMediaBridge,
        // khong can toi day.
        when (intent?.action) {
            ReadingMediaNotifier.ACTION_PLAY -> ReadingMediaBridge.play()
            ReadingMediaNotifier.ACTION_PAUSE -> ReadingMediaBridge.pause()
            ReadingMediaNotifier.ACTION_STOP -> ReadingMediaBridge.stop()
            ReadingMediaNotifier.ACTION_NEXT -> ReadingMediaBridge.next()
            ReadingMediaNotifier.ACTION_PREV -> ReadingMediaBridge.prev()
            ACTION_LISTEN_BROWSER_CHAPTER -> {
                val ticket = intent.getStringExtra(EXTRA_BROWSER_TICKET)
                val chapter = pendingBrowserChapter
                if (chapter != null && chapter.ticket == ticket) {
                    pendingBrowserChapter = null
                    vm.listenFromBrowser(chapter.url, chapter.response)
                }
                intent.action = null
            }
        }
    }

    override fun onStop() {
        // Persist progress when leaving foreground (do NOT pause playback).
        try { vm.forceSaveProgress() } catch (_: Exception) {}
        super.onStop()
    }

    override fun onDestroy() {
        mediaNotifier?.release()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_BROWSER_TICKET = "browser_chapter_ticket"

        // The rendered chapter can exceed the size of an Activity Intent.
        private data class BrowserChapter(val ticket: String, val url: String, val response: String)
        private var pendingBrowserChapter: BrowserChapter? = null

        fun browserChapterIntent(context: android.content.Context, url: String, response: String): Intent {
            val ticket = java.util.UUID.randomUUID().toString()
            pendingBrowserChapter = BrowserChapter(ticket, url, response)
            return Intent(context, ReaderActivity::class.java)
                .setAction(ACTION_LISTEN_BROWSER_CHAPTER)
                .putExtra(EXTRA_BROWSER_TICKET, ticket)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        const val ACTION_LISTEN_BROWSER_CHAPTER =
            "com.novel.reader.app.action.LISTEN_BROWSER_CHAPTER"
    }
}
