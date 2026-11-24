package vn.tramtruyen.reader.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.tramtruyen.reader.model.*
import kotlinx.coroutines.flow.StateFlow

@Immutable
data class PasteState(val url: String = "", val analyzing: Boolean = false, val step: Int = 1, val result: PasteResult? = null, val clipErr: Boolean = false, val error: String? = null)
@Immutable
data class EpubImportState(val importing: Boolean = false, val fileName: String = "", val result: EpubImportResult? = null, val error: String? = null)

/**
 * State nóng, nhỏ, chỉ dành cho đồng hồ và thanh tiến độ.
 * Tách khỏi UiState để tick phát lại không làm invalid toàn bộ app shell/list.
 */
@Immutable
data class PlaybackProgressState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val progress: Float = 0f,
)

@Immutable
data class UiState(
    val currentTab: Tab = Tab.HOME,
    val tabHistory: List<Tab> = listOf(Tab.HOME),
    val uiTheme: UiTheme = UiTheme.SYSTEM,
    val colorMode: ColorMode = ColorMode.SYSTEM_WALLPAPER,
    @Deprecated("Use colorMode") val monetTheme: Boolean = true,
    val monetSeed: String = DEFAULT_SEED,
    val accentColor: String = DEFAULT_ACCENT,
    val selectedVoice: String = vn.tramtruyen.reader.engine.speech.TtsVoicePolicy.DEFAULT_VOICE,
    val geminiApiKey: String = "",
    val geminiKeyChecking: Boolean = false,
    val geminiKeyOk: Boolean = false,
    val geminiKeyStatus: String? = null,
    val zaloApiKey: String = "",
    val zaloKeyChecking: Boolean = false,
    val zaloKeyOk: Boolean = false,
    val zaloKeyStatus: String? = null,
    val voiceActionStatus: String? = null,
    val autoNextChapter: Boolean = true,
    val karaokeFontSize: Int = 24,
    val playerVolume: Float = 1f,
    val frameRateMode: FrameRateMode = FrameRateMode.AUTO,
    val playerBlur: Int = 8,
    val hapticEnabled: Boolean = true,
    val hapticIntensity: Int = 55,
    val voiceProfileEnabled: Boolean = true,
    val contentLanguage: String = "AUTO",
    val contentExtractionMode: String = "SMART",
    val disabledTtsSources: Set<String> = emptySet(),
    val ttsSources: List<TtsSourceUi> = emptyList(),
    val pronunciationRules: List<PronunciationRule> = emptyList(),
    val backupStatus: String? = null,
    val libFilter: String = "all",
    val chapterFilter: ChapterFilter = ChapterFilter.ALL,
    val historyItems: List<Story> = emptyList(),
    val libraryItems: List<Story> = emptyList(),
    val searchQuery: String = "",
    val webResults: List<WebSearchHit> = emptyList(),
    val webSuggestions: List<WebSearchHit> = emptyList(),
    val webSuggestionsSource: String = "",
    val webSuggestionsLoading: Boolean = false,
    val webSuggestionsError: String? = null,
    /** Search-enabled sites available from parser configuration. */
    val webSearchSources: List<String> = emptyList(),
    /** A single selected site; web search never fans out across multiple sources. */
    val selectedWebSearchSource: String = "",
    val webSearching: Boolean = false,
    val webError: String? = null,
    /** URL ket qua dang duoc mo chuong 1 (hien vong xoay tren dung dong do). */
    val webOpeningUrl: String? = null,
    /** Da tu dong tim tren web cho tu khoa nao (tranh tim lai y het). */
    val webSearchedQuery: String = "",
    val webSearchedSource: String = "",
    /** So nguon da quet trong lan tim gan nhat. */
    val webSitesSearched: Int = 0,
    val webUnavailableSources: Set<String> = emptySet(),
    val voiceGroups: List<VoiceGroup> = VOICE_GROUPS,
    val activeStory: Story? = null,
    val chapterNumber: Int = 1,
    val chapterTitle: String = "",
    val isPlayerOpen: Boolean = false,
    val hiddenMiniPlayerStoryId: String? = null,
    val isKaraoke: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val ttsProvider: String = "Google TTS",
    val ttsLatencyMs: Long? = null,
    val ttsFallbackReason: String? = null,
    val prefetchStatus: String? = null,
    val errorMessage: String? = null,
    val chapterLoadError: String? = null,
    val progress: Float = 0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val loop: Boolean = false,
    val sleepTimer: Int? = null,
    val sleepMode: SleepMode = SleepMode.OFF,
    val sentences: List<String> = emptyList(),
    val activeSentenceIndex: Int = 0,
    val tocUrls: List<String> = emptyList(),
    val tocTitles: List<String> = emptyList(),
    val chapterItems: List<ChapterUiModel> = emptyList(),
    val playback: PlaybackMetadata = PlaybackMetadata(),
    val sheet: String? = null,
    val storyToDelete: Story? = null,
    val pasteOpen: Boolean = false,
    val paste: PasteState = PasteState(),
    val epubImport: EpubImportState = EpubImportState(),
)

@Composable
fun <T> StateFlow<T>.collectAsStateSafe(): State<T> = collectAsStateWithLifecycle()

/** Mot nguon giong doc trong man Cai dat: so giong, da cau hinh chua, dang bat hay tat. */
@Immutable
data class TtsSourceUi(
    val key: String,
    val name: String,
    val voices: Int,
    val configured: Boolean,
    val enabled: Boolean,
)
