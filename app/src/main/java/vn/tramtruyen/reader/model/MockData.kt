package vn.tramtruyen.reader.model

val placeholderStory = Story(
    id = "__empty__",
    title = "Chưa có truyện nào",
    author = "Dán link để bắt đầu nghe",
    coverUrl = "",
    totalChapters = 0,
    tags = listOf("Bắt đầu"),
    color = "#9575cd",
    currentChapter = 1,
)

val mockStories: List<Story> = listOf(placeholderStory)

val CHAP_TITLES = listOf(
    "Chương mới", "Tiếp tục", "Diễn biến", "Cao trào",
    "Bí ẩn", "Kỳ ngộ", "Đối đầu", "Bước ngoặt",
)

fun chapTitle(n: Int): String = CHAP_TITLES[((maxOf(1, n) - 1) % CHAP_TITLES.size)]

val VOICE_GROUPS = listOf(
    VoiceGroup(
        "Google TTS (trực tuyến)", listOf(
            VoiceOption("gtts:vi", "Google Translate · Tiếng Việt", "Trực tuyến · cần mạng"),
            VoiceOption("gtts:vi-tld-com.vn", "Google TTS · Tiếng Việt (VN)", "Trực tuyến · cần mạng"),
        )
    ),
    VoiceGroup(
        "Edge TTS", listOf(
            VoiceOption("edge:vi-VN-HoaiMyNeural", "Hoài My", "Nữ · tự nhiên"),
            VoiceOption("edge:vi-VN-NamMinhNeural", "Nam Minh", "Nam · trầm ấm"),
        )
    ),
)

val SUGGESTIONS = listOf(
    Suggestion("Mê Truyện Chữ", "https://metruyenchu.com.vn/truyen/the-gioi-hoan-my"),
    Suggestion("Wikicv", "https://wikicv.net/truyen/linh-vu-thien-ha"),
    Suggestion("TruyệnFull", "https://truyenfull.vn/truyen/dau-la-dai-luc"),
)

val PASTE_STEPS = listOf(
    "Đang kết nối nguồn truyện…",
    "Đang tải nội dung chương…",
    "Đang chuẩn bị giọng đọc AI…",
)

val MONET: Map<String, String> = linkedMapOf(
    "salmon" to "#e79596", "rose" to "#e2758f", "pink" to "#e6a7cf", "mauve" to "#b07d84",
    "coral" to "#e39d84", "peach" to "#e6a58c", "apricot" to "#e8b57f", "tan" to "#d99f6f",
    "sand" to "#d9a86f", "clay" to "#c98f5f", "amber" to "#e8c27f", "gold" to "#e6cf6f",
    "lemon" to "#e6da7f", "lime" to "#c9d97f", "olive" to "#b0c98f", "sage" to "#a8d9a8",
    "mint" to "#8fd9b0", "seafoam" to "#7fd9c2", "teal" to "#6fb3ad", "jade" to "#7fc9a8",
    "emerald" to "#8fd99f", "spring" to "#a8d98f", "aqua" to "#7fd9d9", "cyan" to "#7fc9d9",
    "sky" to "#8fbdd9", "azure" to "#7fa8d9", "blue" to "#7f9cd9", "denim" to "#8f9cc9",
    "periwinkle" to "#a8a8e6", "lavender" to "#b0a8e6", "wisteria" to "#c3b0ec", "violet" to "#b08fd9",
    "purple" to "#a87fd9", "grape" to "#b07fd9", "orchid" to "#c78fd9", "magenta" to "#d98fd9",
    "fuchsia" to "#e68fd9", "cerise" to "#e68fc2", "blush" to "#e6a8c2",
)

const val DEFAULT_SEED = "wisteria"
const val DEFAULT_ACCENT = "#9575cd"
