package vn.tramtruyen.reader.model

import androidx.compose.runtime.Immutable

enum class StorySourceType { ONLINE, EPUB }

@Immutable
data class Story(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val totalChapters: Int,
    val tags: List<String>,
    val color: String,
    val currentChapter: Int = 1,
    val source: String? = null,
    val sourceType: StorySourceType = StorySourceType.ONLINE,
    val localBookId: String? = null,
    val localFileName: String? = null,
    val fileSizeBytes: Long = 0L,
    val storyUrl: String? = null,
    val chapterUrl: String? = null,
    val nextUrl: String? = null,
    val prevUrl: String? = null,
    val chapterTitle: String = "",
    val sentenceIndex: Int = 0,
    val sentenceCount: Int = 0,
    val progress: Float = 0f,
    val positionMs: Long = 0L,
    val updatedAt: Long = 0L,
    val lastListenedAt: Long = 0L,
    val mirrorUrls: List<String> = emptyList(),
)

@Immutable data class VoiceOption(
    val id: String,
    val label: String,
    val sub: String,
    val provider: String = "Khác",
    val language: String = "",
    val gender: String = "",
)
@Immutable data class VoiceGroup(val name: String, val voices: List<VoiceOption>)
@Immutable data class Suggestion(val label: String, val url: String)

enum class Tab { HOME, LIBRARY, SEARCH, SETTINGS }
enum class UiTheme { DARK, LIGHT, SYSTEM }
enum class ColorMode { SYSTEM_WALLPAPER, CUSTOM_SEED, COVER_ART }
enum class FrameRateMode { AUTO, FPS_60, FPS_90, FPS_120 }
enum class SleepMode { OFF, MINUTES, END_SENTENCE, END_CHAPTER }
@Immutable data class PronunciationRule(val id: String, val from: String, val to: String, val storyId: String? = null)
enum class ChapterListenStatus { UNPLAYED, IN_PROGRESS, COMPLETED }
enum class ChapterFilter { ALL, LISTENING, HEARD }

/** 1 ket qua tim tren trang nguon (chua luu vao thu vien). */
@Immutable
data class WebSearchHit(
    val title: String,
    val url: String,
    val author: String = "",
    val coverUrl: String = "",
    val source: String = "",
    /** Do giong ten 0..1 so voi tu khoa da go. */
    val score: Double = 0.0,
    /** Dung y nguyen ten truyen (sau khi bo dau). */
    val exact: Boolean = false,
    /** Nhan hien thi: "Trung khop" / "Rat gan" / "Gan dung". */
    val matchLabel: String = "",
    /** Cac nguon khac cung co bo truyen nay (du phong khi nguon chinh loi). */
    val mirrors: List<String> = emptyList(),
)

@Immutable
data class ChapterUiModel(val index: Int, val title: String, val url: String, val chapterId: String, val status: ChapterListenStatus = ChapterListenStatus.UNPLAYED, val progress: Float = 0f, val isCurrent: Boolean = false, val lastListenedAt: Long = 0L)

@Immutable
data class PlaybackMetadata(
    val storyId: String = "", val storyTitle: String = "NovelBook", val author: String = "", val chapterTitle: String = "", val chapterIndex: Int = 1, val totalChapters: Int = 0, val coverUrl: String = "", val source: String? = null, val positionMs: Long = 0L, val durationMs: Long = 0L, val speed: Float = 1f, val voiceId: String = vn.tramtruyen.reader.engine.speech.TtsVoicePolicy.DEFAULT_VOICE, val isPlaying: Boolean = false, val isBuffering: Boolean = false, val hasPrevious: Boolean = false, val hasNext: Boolean = false,
)

@Immutable data class PasteResult(val story: Story, val source: String)
@Immutable data class EpubImportResult(val story: Story, val chapters: List<ChapterUiModel>)
