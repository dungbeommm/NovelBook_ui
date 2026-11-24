package vn.tramtruyen.reader.presentation.screens

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import vn.tramtruyen.reader.model.Tab
import kotlin.math.abs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import vn.tramtruyen.reader.model.ColorMode
import vn.tramtruyen.reader.model.MONET
import vn.tramtruyen.reader.model.UiTheme
import vn.tramtruyen.reader.presentation.LocalBottomBarInset
import vn.tramtruyen.reader.presentation.PlayerViewModel
import vn.tramtruyen.reader.presentation.UiState
import vn.tramtruyen.reader.presentation.TtsSourceUi
import vn.tramtruyen.reader.presentation.components.Radii
import vn.tramtruyen.reader.presentation.components.noGrayClick
import vn.tramtruyen.reader.presentation.motion.Motion
import vn.tramtruyen.reader.presentation.motion.rememberHaptics
import vn.tramtruyen.reader.presentation.motion.HapticSlider
import vn.tramtruyen.reader.presentation.motion.LocalThemeRevealState
import vn.tramtruyen.reader.presentation.theme.hex
import vn.tramtruyen.reader.SourceBrowserActivity
import vn.tramtruyen.reader.engine.ContentExtractionMode
import vn.tramtruyen.reader.engine.ReadingLanguage
import vn.tramtruyen.reader.engine.sources.AdBlockManager
import vn.tramtruyen.reader.engine.catalog.AdBlockExtensionInstaller
import vn.tramtruyen.reader.engine.catalog.WebParserPackageInstaller
import vn.tramtruyen.reader.engine.network.AppDns
import vn.tramtruyen.reader.engine.sources.GeckoFeature
import vn.tramtruyen.reader.engine.sources.GeckoModuleInstaller
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SettingsOuterPadding = 16.dp
private val SettingsCardPadding = 16.dp
private val SettingsSectionGap = 22.dp
private val SettingsLabelGap = 10.dp

private data class SettingsCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val SETTINGS_CATEGORY_GROUPS = listOf(
listOf(
SettingsCategory("appearance", "Giao diện", "Chủ đề tối, màu sắc, hiệu ứng", Icons.Outlined.Palette),
SettingsCategory("language_content", "Ngôn ngữ & nội dung", "Ngôn ngữ đọc, trích xuất chương", Icons.Outlined.Language),
),
listOf(
SettingsCategory("voice_sources", "Giọng đọc AI", "Runtime, mô hình giọng ngoại tuyến", Icons.Outlined.GraphicEq),
SettingsCategory("voice_providers", "Nguồn giọng đọc", "Edge, TikTok, Gemini, Zalo", Icons.Outlined.RecordVoiceOver),
SettingsCategory("pronunciation", "Cách đọc", "Quy tắc phát âm tùy chỉnh", Icons.Outlined.Spellcheck),
SettingsCategory("browser", "Trình duyệt & kết nối", "Chặn quảng cáo, DNS, WARP", Icons.Outlined.Public),
),
listOf(
SettingsCategory("backup", "Sao lưu & khôi phục", "Xuất, nhập dữ liệu ứng dụng", Icons.Outlined.Backup),
),
)

private val SETTINGS_CATEGORIES = SETTINGS_CATEGORY_GROUPS.flatten()


/** Screen-local type scale: improve readability without changing the reader/player. */
@Composable
fun SettingsScreen(vm: PlayerViewModel, ui: UiState) {
    val base = MaterialTheme.typography
    val typography = base.copy(
        headlineSmall = base.headlineSmall.copy(fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = base.titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
        titleSmall = base.titleSmall.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodySmall = base.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = base.labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = base.labelMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelSmall = base.labelSmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
    )
    MaterialTheme(typography = typography) {
        SettingsContent(vm, ui)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsContent(vm: PlayerViewModel, ui: UiState) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val adBlock by AdBlockManager.state.collectAsState()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val pageStateHolder = rememberSaveableStateHolder()
    val pageTransition = updateTransition(targetState = selectedId, label = "settingsPage")
    val haptics = rememberHaptics()
    val navigationActive = ui.currentTab == Tab.SETTINGS &&
        !ui.isPlayerOpen && ui.sheet == null && !ui.pasteOpen
    // One back path for toolbar, system Back and left-edge swipes.
    // Ignore repeat requests until the current page transition has settled.
    val navigateBack: () -> Unit = {
        if (navigationActive && !pageTransition.isRunning) {
            haptics.back()
            when (selectedId) {
                "voice_clone" -> selectedId = "voice_sources"
                null -> vm.goBack()
                else -> selectedId = null
            }
        }
    }
    BackHandler(enabled = navigationActive) { navigateBack() }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { w -> w.write(vm.exportBackupJson()) } }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> vm.importBackupJson(r.readText()) } }
    }

    pageTransition.AnimatedContent(
        modifier = Modifier.fillMaxSize().clipToBounds()
            .settingsBackSwipe(
                enabled = navigationActive && !pageTransition.isRunning,
                onBack = navigateBack,
            ),
        transitionSpec = {
            // Forward: new section from the right. Back: current section to the right.
            val returning = targetState == null || initialState == "voice_clone"
            (slideInHorizontally(
                animationSpec = tween(Motion.NavEnterMs, easing = Motion.EnterEasing),
            ) { width -> if (returning) -width / 4 else width } + fadeIn(
                animationSpec = tween(Motion.NavFadeInMs, easing = Motion.EnterEasing),
            )).togetherWith(
                slideOutHorizontally(
                    animationSpec = tween(Motion.NavExitMs, easing = Motion.ExitEasing),
                ) { width -> if (returning) width else -width / 4 } + fadeOut(
                    animationSpec = tween(Motion.NavFadeOutMs, easing = Motion.ExitEasing),
                ),
            )
        },
        contentKey = { route -> route ?: "settings_categories" },
    ) { route ->
        val selected = SETTINGS_CATEGORIES.firstOrNull { it.id == route }
        // Keep independent scroll positions and saveable form state per section.
        pageStateHolder.SaveableStateProvider(route ?: "settings_categories") {
            Column(Modifier.fillMaxSize()) {
                val voicePage = route == "voice_sources" || route == "voice_clone"
               Surface(color = cs.surface, modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SettingsOuterPadding, vertical = if (selected == null) 12.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected != null) {
            FilledTonalIconButton(
                onClick = navigateBack,
                enabled = navigationActive && !pageTransition.isRunning,
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Quay lại")
            }
        } else {
            IconButton(
                onClick = navigateBack,
                enabled = navigationActive && !pageTransition.isRunning,
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Quay lại")
            }
        }
        if (!voicePage) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = selected?.title ?: "Cài đặt",
                style = if (selected == null) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
               }
                when (route) {
                    "voice_sources" -> VoiceManagerPage(vm, ui) {
                        if (navigationActive && !pageTransition.isRunning) selectedId = "voice_clone"
                    }
                    "voice_clone" -> VoiceClonePage()
                    else -> LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = SettingsOuterPadding,
                            top = 16.dp,
                            end = SettingsOuterPadding,
                            bottom = 28.dp + LocalBottomBarInset.current,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (selected == null) {
                        
                        item {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SETTINGS_CATEGORY_GROUPS.forEach { group ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                group.forEachIndexed { index, category ->
                    val topRadius = if (index == 0) 20.dp else 4.dp
                    val bottomRadius = if (index == group.lastIndex) 20.dp else 4.dp
                    SettingsCategoryTile(
                        category = category,
                        shape = RoundedCornerShape(
                            topStart = topRadius,
                            topEnd = topRadius,
                            bottomStart = bottomRadius,
                            bottomEnd = bottomRadius,
                        ),
                        onClick = {
                            if (navigationActive && !pageTransition.isRunning) {
                                haptics.tap()
                                selectedId = category.id
                            }
                        },
                    )
                }
            }
        }
    }
                        }} else {
                            item(key = "settings_content_${selected.id}") {
                                when (selected.id) {
                                    "browser" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        BrowserSettingsCard(
                                            adBlock = adBlock,
                                            onOpenBrowser = {
                                                context.startActivity(
                                                    Intent(context, SourceBrowserActivity::class.java)
                                                        .putExtra(SourceBrowserActivity.EXTRA_URL, vm.browserUrl())
                                                )
                                            },
                                            onToggle = { AdBlockManager.setEnabled(context, it) },
                                        )
                                        SectionLabel(Icons.Outlined.Dns, "DNS")
                                        DnsSettingsCard()
                                        SectionLabel(Icons.Outlined.Security, "VPN · 1.1.1.1 / WARP")
                                        WarpSettingsCard()
                                        SectionLabel(Icons.Outlined.Extension, "Bộ mở rộng")
                                        AdBlockExtensionCard()
                                        WebParserCard()
                                        GeckoCard()
                                    }
                                    "appearance" -> AppearanceCard(vm, ui)
                                    "language_content" -> LanguageContentCard(vm, ui)
                                    "pronunciation" -> PronunciationCard(vm, ui)
                                    "backup" -> BackupCard(
                                        ui,
                                        onExport = { export.launch("TramTruyen-backup.json") },
                                        onRestore = { restore.launch(arrayOf("application/json", "text/plain")) },
                                    )
                                    "voice_providers" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        TtsSourcesCard(vm, ui)
                                        TikTokTtsCard(vm, ui)
                                        GeminiCard(vm, ui)
                                        ZaloCard(vm, ui)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Left-edge back gesture; a tap, vertical scroll or short/cancelled drag never navigates. */
@Composable
private fun Modifier.settingsBackSwipe(enabled: Boolean, onBack: () -> Unit): Modifier {
    val currentOnBack by rememberUpdatedState(onBack)
    return pointerInput(enabled) {
        if (!enabled) return@pointerInput
        val edgeWidth = 32.dp.toPx()
        val minimumDistance = 72.dp.toPx()
        val maximumDistance = 120.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.position.x > edgeWidth || down.isConsumed) return@awaitEachGesture
            val threshold = (size.width * 0.22f).coerceIn(minimumDistance, maximumDistance)
            var claimed = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                // Multi-touch belongs to the child/system, never to back navigation.
                if (event.changes.size != 1) return@awaitEachGesture
                val change = event.changes.firstOrNull { it.id == down.id }
                    ?: return@awaitEachGesture
                if (change.isConsumed) return@awaitEachGesture
                val delta = change.position - down.position
                if (!claimed) {
                    // Decide direction only after touch slop; don't steal vertical scrolling.
                    if (abs(delta.y) > viewConfiguration.touchSlop && abs(delta.y) >= abs(delta.x)) {
                        return@awaitEachGesture
                    }
                    if (delta.x < -viewConfiguration.touchSlop) return@awaitEachGesture
                    claimed = delta.x > viewConfiguration.touchSlop && delta.x > abs(delta.y) * 1.2f
                }
                if (claimed) change.consume()
                if (!change.pressed) {
                    if (claimed && delta.x >= threshold && delta.x > abs(delta.y) * 1.2f) {
                        currentOnBack()
                    }
                    return@awaitEachGesture
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryTile(
    category: SettingsCategory,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = shape,
        color = cs.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                )
                Text(
                    category.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageContentCard(vm: PlayerViewModel, ui: UiState) {
    val cs = MaterialTheme.colorScheme
    val languages = listOf(
        ReadingLanguage.AUTO to "Tự động",
        ReadingLanguage.VIETNAMESE to "Tiếng Việt",
        ReadingLanguage.ENGLISH to "English",
        ReadingLanguage.CHINESE to "中文",
    )
    val modes = listOf(
        ContentExtractionMode.SMART to Pair("Chính xác tự động", "So sánh parser nguồn với DOM và chọn nội dung sạch, đầy đủ hơn"),
        ContentExtractionMode.SOURCE_FIRST to Pair("Ưu tiên nguồn", "Dùng selector riêng của website; fallback khi nội dung bị rỗng"),
        ContentExtractionMode.GENERIC_FIRST to Pair("Ưu tiên DOM", "Phân tích trang đã render; phù hợp link lạ hoặc website đổi giao diện"),
    )
    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = cs.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Column(Modifier.padding(SettingsCardPadding)) {
            Text("Ngôn ngữ trang truyện", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Điều chỉnh ngôn ngữ yêu cầu HTTP/WebView và cách xử lý văn bản.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Từ điển mở rộng: 17.713 từ nước ngoài · 38 chữ viết tắt",
                style = MaterialTheme.typography.labelMedium,
                color = cs.primary,
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEach { (value, label) ->
                    FilterChip(
                        selected = ui.contentLanguage == value.name,
                        onClick = { vm.setContentLanguage(value.name) },
                        label = { Text(label) },
                        leadingIcon = if (ui.contentLanguage == value.name) {
                            { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = cs.outlineVariant)
            Text("Cách lấy nội dung chương", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            modes.forEach { (mode, text) ->
                val selected = ui.contentExtractionMode == mode.name
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .noGrayClick { vm.setContentExtractionMode(mode.name) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { vm.setContentExtractionMode(mode.name) },
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(text.first, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowserSettingsCard(
    adBlock: AdBlockManager.State,
    onOpenBrowser: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = cs.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth()
                    .noGrayClick(onClick = onOpenBrowser)
                    .padding(SettingsCardPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Language, null, tint = cs.onPrimaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mở trình duyệt nội dung", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Dùng chung trang và phiên đăng nhập với bộ tải truyện",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                Icon(Icons.Outlined.OpenInNew, null, tint = cs.onSurfaceVariant)
            }

            HorizontalDivider(
                Modifier.padding(horizontal = SettingsCardPadding),
                color = cs.outlineVariant,
            )

            Row(
                Modifier.fillMaxWidth().padding(SettingsCardPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Security, null, tint = cs.onSecondaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).padding(end = 10.dp)) {
                    Text("Trình chặn quảng cáo", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    val error = adBlock.error
                    val detail = when {
                        error != null -> error
                        adBlock.installed -> {
                            val rules = "%,d".format(adBlock.ruleCount).replace(',', '.')
                            "$rules mẫu chặn URL và ẩn quảng cáo trong WebView, Gecko"
                        }
                        else -> "Luật chặn chưa sẵn sàng"
                    }
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (error != null) cs.error else cs.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = adBlock.enabled,
                    onCheckedChange = onToggle,
                )
            }
        }
    }
}

@Composable
private fun WebParserCard() {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var installed by remember { mutableStateOf(WebParserPackageInstaller.installedInfo(context)) }
    var phase by remember { mutableIntStateOf(0) }
    var progress by remember { mutableIntStateOf(-1) }
    var error by remember { mutableStateOf<String?>(null) }
    val err = error

    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = cs.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Column(Modifier.padding(SettingsCardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Extension, null, tint = cs.onPrimaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).padding(end = 10.dp)) {
                    Text("Bộ mở rộng parser web", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            err != null -> err
                            phase == 1 -> "Đang tải bộ parser… " + if (progress >= 0) "$progress%" else ""
                            installed != null -> "Đã tải ${installed!!.sourceCount} parser · bản ${installed!!.version}"
                            else -> "Tải parser website, cấu hình cũ và quy tắc xử lý trong một gói JSON."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (err != null) cs.error else cs.onSurfaceVariant,
                    )
                }
                if (installed != null) Icon(Icons.Outlined.CheckCircle, null, tint = cs.primary)
            }
            if (phase == 1) {
                Spacer(Modifier.height(12.dp))
                if (progress >= 0) {
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                enabled = phase == 0,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    error = null
                    progress = -1
                    phase = 1
                    scope.launch {
                        try {
                            WebParserPackageInstaller.downloadAndInstall(context) { progress = it }
                            installed = WebParserPackageInstaller.installedInfo(context)
                            phase = 0
                        } catch (cancelled: CancellationException) {
                            phase = 0
                            throw cancelled
                        } catch (failure: Exception) {
                            error = failure.message ?: "Không tải được bộ parser web."
                            phase = 0
                        }
                    }
                },
            ) {
                Icon(Icons.Outlined.CloudDownload, null)
                Spacer(Modifier.width(6.dp))
                Text(if (err != null) "Thử tải lại" else if (installed == null) "Tải bộ parser web" else "Cập nhật parser web")
            }
        }
    }
}

@Composable
private fun AdBlockExtensionCard() {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var installed by remember { mutableStateOf(AdBlockExtensionInstaller.installedInfo(context)) }
    var phase by remember { mutableIntStateOf(0) }
    var progress by remember { mutableIntStateOf(-1) }
    var error by remember { mutableStateOf<String?>(null) }
    val err = error

    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = cs.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Column(Modifier.padding(SettingsCardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Security, null, tint = cs.onPrimaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).padding(end = 10.dp)) {
                    Text("Extension chặn quảng cáo", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            err != null -> err
                            phase == 1 -> "Đang tải extension… " + if (progress >= 0) "$progress%" else ""
                            installed != null -> "Đã tải bản ${installed!!.version} · ${installed!!.ruleCount} quy tắc"
                            else -> "Tải luật URL và script ẩn quảng cáo; bộ có sẵn vẫn hoạt động khi chưa tải."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (err != null) cs.error else cs.onSurfaceVariant,
                    )
                }
                if (installed != null) Icon(Icons.Outlined.CheckCircle, null, tint = cs.primary)
            }
            if (phase == 1) {
                Spacer(Modifier.height(12.dp))
                if (progress >= 0) {
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                enabled = phase == 0,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    error = null
                    progress = -1
                    phase = 1
                    scope.launch {
                        try {
                            AdBlockExtensionInstaller.downloadAndInstall(context) { progress = it }
                            installed = AdBlockExtensionInstaller.installedInfo(context)
                            phase = 0
                        } catch (cancelled: CancellationException) {
                            phase = 0
                            throw cancelled
                        } catch (failure: Exception) {
                            error = failure.message ?: "Không tải được extension chặn quảng cáo."
                            phase = 0
                        }
                    }
                },
            ) {
                Icon(Icons.Outlined.CloudDownload, null)
                Spacer(Modifier.width(6.dp))
                Text(if (err != null) "Thử tải lại" else if (installed == null) "Tải extension chặn quảng cáo" else "Cập nhật extension chặn quảng cáo")
            }
        }
    }
}

@Composable
private fun GeckoCard() {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var installed by remember { mutableStateOf(GeckoFeature.isInstalled(context)) }
    // 0 = nghi, 1 = dang tai, 2 = dang cai, 3 = da cai (cho app nap lai)
    var phase by remember {
        mutableStateOf(if (!installed && GeckoModuleInstaller.isPackageInstalled(context)) 3 else 0)
    }
    var progress by remember { mutableStateOf(-1) }
    var error by remember { mutableStateOf<String?>(null) }
    val err = error
    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = cs.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Column(Modifier.padding(SettingsCardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Language, null, tint = cs.onPrimaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).padding(end = 10.dp)) {
                    Text("Trình duyệt Gecko (module phụ)", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            installed -> "Đã cài module — các web cần Gecko (như Sáng Tác Việt) sẽ tự dùng"
                            err != null -> err
                            phase == 1 -> "Đang tải module Gecko… " + if (progress >= 0) "$progress%" else ""
                            phase == 2 -> "Đang cài module — nếu máy hiện hộp thoại xác nhận, chọn Cài đặt"
                            phase == 3 -> "Đã cài module. Nếu app chưa nhận Gecko ngay, hãy mở lại app"
                            else -> "Tải thêm Gecko để mở các nguồn cần bộ máy này, như Sáng Tác Việt."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (err != null) cs.error else cs.onSurfaceVariant,
                    )
                }
                if (installed) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = cs.primary)
                }
            }
            if (!installed) {
                if (phase == 1 || phase == 2) {
                    Spacer(Modifier.height(12.dp))
                    if (phase == 1 && progress >= 0) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = {
                        val activity = context as? Activity
                        if (activity == null) {
                            error = "Không mở được trình cài đặt"
                            return@FilledTonalButton
                        }
                        if (!GeckoModuleInstaller.canInstall(activity)) {
                            error = runCatching {
                                GeckoModuleInstaller.openInstallPermission(activity)
                                "Bật quyền cài từ nguồn này, quay lại rồi bấm tải Gecko."
                            }.getOrElse { "Hãy cấp quyền cài từ nguồn ngoài cho NovelBook trong cài đặt Android." }
                            return@FilledTonalButton
                        }
                        error = null
                        phase = 1
                        progress = -1
                        scope.launch {
                            try {
                                GeckoModuleInstaller.install(
                                    activity,
                                    onProgress = { progress = it },
                                    onInstalling = { phase = 2 },
                                )
                                installed = GeckoFeature.isInstalled(context)
                                phase = if (installed) 0 else 3
                            } catch (cancelled: CancellationException) {
                                phase = 0
                                throw cancelled
                            } catch (failure: Exception) {
                                error = failure.message ?: "Tải module thất bại"
                                phase = 0
                            }
                        }
                    },
                    enabled = phase == 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.CloudDownload, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (err != null) "Thử tải lại" else "Tải module Gecko")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceCard(vm: PlayerViewModel, ui: UiState) {
    val cs = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    val themeReveal = LocalThemeRevealState.current
    val scope = rememberCoroutineScope()
    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = cs.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Column(Modifier.padding(SettingsCardPadding)) {
            Text("Sáng tối", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))
            ThemeModeSelector(
                selected = ui.uiTheme,
                onSelect = { mode, origin ->
                    if (ui.uiTheme != mode) {
                        val reveal = themeReveal
                        if (reveal == null) vm.setUiTheme(mode)
                        else scope.launch {
                            reveal.reveal(origin, cs.surface) { vm.setUiTheme(mode) }
                        }
                    }
                },
            )

            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = cs.outlineVariant)
            Text("Nền Player", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))
            Text("Độ mờ nền: ${ui.playerBlur}%  ·  0 = tắt nền", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            var blurDraft by remember(ui.playerBlur) { mutableStateOf(ui.playerBlur.toFloat()) }
            HapticSlider(
                value = blurDraft,
                // Giữ kéo slider ở state cục bộ; chỉ dựng lại blur toàn màn hình
                // khi thả tay để không làm giật mọi khung hình của cử chỉ.
                onValueChange = { blurDraft = it },
                onValueChangeFinished = { vm.setPlayerBlur(blurDraft.toInt()) },
                valueRange = 0f..60f,
                steps = 11,
            )
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = cs.outlineVariant)
            Text("Phản hồi rung", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Rung khi thao tác", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Nút, vuốt, cuộn, đổi tab và thao tác quay lại",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = ui.hapticEnabled,
                    onCheckedChange = { enabled ->
                        vm.setHapticEnabled(enabled)
                        if (enabled) haptics.preview(ui.hapticIntensity, force = true)
                    },
                )
            }
            AnimatedVisibility(
                visible = ui.hapticEnabled,
                enter = fadeIn(tween(Motion.StateChangeMs)) + slideInVertically(
                    animationSpec = tween(Motion.ContentChangeMs, easing = Motion.EnterEasing),
                    initialOffsetY = { it.coerceAtMost(32) },
                ),
                exit = fadeOut(tween(Motion.QuickFadeMs)),
            ) {
                var intensityDraft by remember(ui.hapticIntensity) {
                    mutableStateOf(ui.hapticIntensity.toFloat())
                }
                Column {
                    val label = when {
                        intensityDraft < 34f -> "Nhẹ"
                        intensityDraft < 68f -> "Vừa"
                        else -> "Mạnh"
                    }
                    Text(
                        "Cường độ: $label · ${intensityDraft.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                    HapticSlider(
                        value = intensityDraft,
                        onValueChange = { intensityDraft = it },
                        onValueChangeFinished = {
                            val level = intensityDraft.roundToInt().coerceIn(1, 100)
                            vm.setHapticIntensity(level)
                            haptics.preview(level)
                        },
                        valueRange = 1f..100f,
                        steps = 9,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Nhớ giọng theo từng truyện",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                Switch(ui.voiceProfileEnabled, { vm.toggleVoiceProfile() })
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = cs.outlineVariant)
            Text("Nguồn màu", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))
            SourceTile(
                Icons.Outlined.Wallpaper,
                "Hình nền điện thoại",
                ui.colorMode == ColorMode.SYSTEM_WALLPAPER,
            ) { vm.setColorMode(ColorMode.SYSTEM_WALLPAPER) }
            Spacer(Modifier.height(8.dp))
            SourceTile(
                Icons.Outlined.ColorLens,
                "Tự chọn màu",
                ui.colorMode == ColorMode.CUSTOM_SEED,
            ) { vm.setColorMode(ColorMode.CUSTOM_SEED) }
            Spacer(Modifier.height(8.dp))
            SourceTile(
                Icons.Outlined.Image,
                "Theo ảnh bìa truyện",
                ui.colorMode == ColorMode.COVER_ART,
            ) { vm.setColorMode(ColorMode.COVER_ART) }

            AnimatedVisibility(
                visible = ui.colorMode == ColorMode.CUSTOM_SEED,
                enter = fadeIn(tween(Motion.StateChangeMs)) + slideInVertically(
                    animationSpec = tween(Motion.ContentChangeMs, easing = Motion.EnterEasing),
                    initialOffsetY = { it.coerceAtMost(48) },
                ),
                exit = fadeOut(tween(Motion.QuickFadeMs)) + slideOutVertically(
                    animationSpec = tween(Motion.ContentChangeMs, easing = Motion.ExitEasing),
                    targetOffsetY = { it.coerceAtMost(32) },
                ),
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    Text("Chọn màu gốc", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MONET.entries.forEach { (name, colorHex) ->
                            val selected = name == ui.monetSeed
                            ColorSeedDot(
                                color = hex(colorHex),
                                selected = selected,
                                onClick = { vm.setSeed(name) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selected: UiTheme,
    onSelect: (UiTheme, Offset) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val options = remember {
        listOf(
            Triple(UiTheme.DARK, Icons.Outlined.DarkMode, "Tối"),
            Triple(UiTheme.LIGHT, Icons.Outlined.LightMode, "Sáng"),
            Triple(UiTheme.SYSTEM, Icons.Outlined.PhoneAndroid, "Máy"),
        )
    }
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val indicatorPosition by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = Motion.TabPageSpring,
        label = "themeModeIndicator",
    )
    var innerWidthPx by remember { mutableIntStateOf(0) }
    val selectorHeight = maxOf(80.dp, (56f + 20f * LocalDensity.current.fontScale).dp)
    val outerShape = RoundedCornerShape(22.dp)
    val indicatorShape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = outerShape,
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Box(Modifier.fillMaxWidth().height(selectorHeight).padding(4.dp)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { innerWidthPx = it.width },
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(1f / options.size)
                        .graphicsLayer {
                            translationX = innerWidthPx * indicatorPosition / options.size
                        }
                        .clip(indicatorShape)
                        .background(cs.primaryContainer),
                )
                Row(Modifier.fillMaxSize()) {
                    options.forEach { (mode, icon, label) ->
                        ThemeModeItem(
                            icon = icon,
                            label = label,
                            selected = mode == selected,
                            modifier = Modifier.weight(1f),
                            onClick = { origin -> onSelect(mode, origin) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Offset) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var centerInWindow by remember { mutableStateOf(Offset.Zero) }
    val contentColor by animateColorAsState(
        targetValue = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
        animationSpec = tween(Motion.StateChangeMs, easing = Motion.EnterEasing),
        label = "themeModeContent",
    )
    val iconScale = animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = Motion.SelectionSpring,
        label = "themeModeIcon",
    )
    Column(
        modifier
            .fillMaxHeight()
            .onGloballyPositioned { centerInWindow = it.boundsInWindow().center }
            .clip(RoundedCornerShape(18.dp))
            .noGrayClick { onClick(centerInWindow) }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            null,
            tint = contentColor,
            modifier = Modifier.size(23.dp).graphicsLayer {
                scaleX = iconScale.value
                scaleY = iconScale.value
            },
        )
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun ColorSeedDot(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 3.dp else 1.dp,
        animationSpec = tween(Motion.StateChangeMs, easing = Motion.EnterEasing),
        label = "seedBorderWidth",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) cs.onSurface else cs.outlineVariant,
        animationSpec = tween(Motion.StateChangeMs, easing = Motion.EnterEasing),
        label = "seedBorderColor",
    )
    val scale = animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = Motion.SelectionSpring,
        label = "seedScale",
    )
    Box(
        Modifier
            .size(42.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(CircleShape)
            .background(color)
            .border(borderWidth, borderColor, CircleShape)
            .noGrayClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(Motion.QuickFadeMs)),
            exit = fadeOut(tween(Motion.QuickFadeMs)),
        ) {
            Icon(Icons.Filled.Check, "Đã chọn", tint = Color.White, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun SourceTile(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val containerColor by animateColorAsState(
        if (selected) cs.secondaryContainer else cs.surface,
        tween(Motion.StateChangeMs, easing = Motion.EnterEasing),
        label = "sourceTileColor",
    )
    val borderColor by animateColorAsState(
        if (selected) cs.secondary else cs.outlineVariant,
        tween(Motion.StateChangeMs, easing = Motion.EnterEasing),
        label = "sourceTileBorder",
    )
    val iconScale = animateFloatAsState(
        if (selected) 1.08f else 1f,
        Motion.SelectionSpring,
        label = "sourceTileIcon",
    )
    val iconContainerColor by animateColorAsState(
        if (selected) cs.secondary else cs.surfaceVariant,
        tween(Motion.StateChangeMs, easing = Motion.EnterEasing),
        label = "sourceIconContainer",
    )
    val iconTint by animateColorAsState(
        if (selected) cs.onSecondary else cs.onSurfaceVariant,
        tween(Motion.StateChangeMs, easing = Motion.EnterEasing),
        label = "sourceIconTint",
    )
    val checkProgress = animateFloatAsState(
        if (selected) 1f else 0f,
        Motion.SelectionSpring,
        label = "sourceCheck",
    )
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).noGrayClick(onClick = onClick),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                .background(iconContainerColor),
                contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    null,
                    tint = iconTint,
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    },
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Check,
                    "Đã chọn",
                    tint = cs.primary,
                    modifier = Modifier.size(21.dp).graphicsLayer {
                        alpha = checkProgress.value
                        scaleX = checkProgress.value
                        scaleY = checkProgress.value
                    },
                )
            }
        }
    }
}


@Composable
private fun GeminiCard(vm: PlayerViewModel, ui: UiState) {
    var visible by remember { mutableStateOf(false) }
    CredentialCard(Icons.Outlined.AutoAwesome, "Gemini TTS", ui.geminiKeyOk) {
        OutlinedTextField(
            value = ui.geminiApiKey,
            onValueChange = vm::onGeminiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !ui.geminiKeyChecking,
            label = { Text("API key") },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = { IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
            } },
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::applyGeminiApiKey,
                enabled = !ui.geminiKeyChecking && ui.geminiApiKey.isNotBlank()) {
                Text(if (ui.geminiKeyChecking) "Đang kiểm tra" else "Lưu")
            }
            TextButton(onClick = vm::clearGeminiApiKey, enabled = !ui.geminiKeyChecking) { Text("Xoá") }
        }
        StatusText(ui.geminiKeyStatus, ui.geminiKeyOk)
    }
}


@Composable
private fun ZaloCard(vm: PlayerViewModel, ui: UiState) {
    var visible by remember { mutableStateOf(false) }
    CredentialCard(Icons.Outlined.RecordVoiceOver, "Zalo AI TTS", ui.zaloKeyOk) {
        OutlinedTextField(
            value = ui.zaloApiKey,
            onValueChange = vm::onZaloKeyChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !ui.zaloKeyChecking,
            label = { Text("API key Zalo AI") },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = { IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
            } },
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::applyZaloApiKey,
                enabled = !ui.zaloKeyChecking && ui.zaloApiKey.isNotBlank()) {
                Text(if (ui.zaloKeyChecking) "Đang kiểm tra" else "Lưu")
            }
            TextButton(onClick = vm::clearZaloApiKey, enabled = !ui.zaloKeyChecking) { Text("Xoá") }
        }
        StatusText(ui.zaloKeyStatus, ui.zaloKeyOk)
    }
}

@Composable
private fun PronunciationCard(vm:PlayerViewModel,ui:UiState){
    var from by remember{mutableStateOf("")};var to by remember{mutableStateOf("")};var current by remember{mutableStateOf(false)}
    Surface(shape=RoundedCornerShape(Radii.xl),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.42f)){
        Column(Modifier.padding(SettingsCardPadding)){
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(from,{from=it},Modifier.weight(1f),label={Text("Từ gốc")},singleLine=true);OutlinedTextField(to,{to=it},Modifier.weight(1f),label={Text("Đọc thành")},singleLine=true)}
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(current,{current=it});Text("Chỉ truyện đang nghe",Modifier.weight(1f));Button({vm.addPronunciation(from,to,current);from="";to=""},enabled=from.isNotBlank()&&to.isNotBlank()){Text("Thêm")}}
            ui.pronunciationRules.takeLast(8).reversed().forEach{r->ListItem(headlineContent={Text("${r.from} → ${r.to}")},supportingContent={if(r.storyId!=null)Text("Riêng một truyện")},trailingContent={IconButton({vm.removePronunciation(r.id)}){Icon(Icons.Outlined.Delete,"Xóa")}},colors=ListItemDefaults.colors(containerColor=Color.Transparent))}
        }
    }
}

@Composable
private fun BackupCard(ui:UiState,onExport:()->Unit,onRestore:()->Unit){
    Surface(shape=RoundedCornerShape(Radii.xl),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.42f)){
        Column(Modifier.padding(SettingsCardPadding)){Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Button(onExport,Modifier.weight(1f)){Icon(Icons.Outlined.FileUpload,null);Spacer(Modifier.width(6.dp));Text("Xuất")};FilledTonalButton(onRestore,Modifier.weight(1f)){Icon(Icons.Outlined.FileDownload,null);Spacer(Modifier.width(6.dp));Text("Khôi phục")}};ui.backupStatus?.let{Text(it,Modifier.padding(top=10.dp),color=MaterialTheme.colorScheme.primary)}}
    }
}

@Composable
private fun CredentialCard(icon: ImageVector, title: String, connected: Boolean,
    content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(Radii.xl), color = cs.surfaceVariant.copy(alpha = 0.42f)) {
        Column(Modifier.padding(SettingsCardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (connected) cs.primaryContainer else cs.surfaceVariant),
                    contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = if (connected) cs.onPrimaryContainer else cs.onSurfaceVariant)
                }
                Spacer(Modifier.width(13.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (connected) Icon(Icons.Filled.Check, "Đã kết nối", tint = cs.primary)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun StatusText(message: String?, ok: Boolean) {
    if (message == null) return
    val cs = MaterialTheme.colorScheme
    Spacer(Modifier.height(8.dp))
    Text(message, style = MaterialTheme.typography.bodySmall,
        color = if (ok) cs.primary else cs.error)
}

@Composable
private fun DnsSettingsCard() {
    val context = LocalContext.current
    val selected by AppDns.provider.collectAsState()
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(Radii.xl), color = colors.surfaceVariant.copy(alpha = 0.42f)) {
        Column(Modifier.fillMaxWidth().padding(SettingsCardPadding)) {
            Column(Modifier.selectableGroup()) {
                AppDns.Provider.entries.forEach { provider ->
                    val checked = selected == provider
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(checked, role = Role.RadioButton) { AppDns.select(context, provider) }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = checked, onClick = null)
                        Text(provider.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Text(
                "Áp dụng cho tải nội dung trong app và Gecko. WebView dùng DNS của Android.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                Text(
                    "DNS riêng tư trên Android dùng one.one.one.one hoặc dns.google.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }) {
                    Text("Mở cài đặt mạng Android")
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(icon: ImageVector, title: String) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = cs.primary, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(9.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}


@Composable
private fun WarpSettingsCard() {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var installed by remember { mutableStateOf(vn.tramtruyen.reader.engine.WarpIntegration.installed(context)) }
    var vpnActive by remember { mutableStateOf(vn.tramtruyen.reader.engine.WarpIntegration.systemVpnActive(context)) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                installed = vn.tramtruyen.reader.engine.WarpIntegration.installed(context)
                vpnActive = vn.tramtruyen.reader.engine.WarpIntegration.systemVpnActive(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Surface(shape = RoundedCornerShape(20.dp), color = cs.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(SettingsCardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Kết nối an toàn khi đọc truyện", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text(if (installed) "Đã cài ứng dụng 1.1.1.1" else "Chưa cài ứng dụng 1.1.1.1",
                style = MaterialTheme.typography.bodyMedium)
            Text(if (vpnActive) "VPN hệ thống: đang kết nối (không xác định nhà cung cấp)"
                else "VPN hệ thống: chưa kết nối", color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
            Text("Tải ứng dụng chính thức, chọn chế độ WARP và bật kết nối trước khi đọc/nghe. " +
                "Bạn cần xác nhận quyền VPN trong ứng dụng 1.1.1.1. NovelBook không tự bật hoặc tắt WARP.",
                color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { vn.tramtruyen.reader.engine.WarpIntegration.open(context) },
                modifier = Modifier.fillMaxWidth()) {
                Text(if (installed) "Mở 1.1.1.1 để bật WARP" else "Tải 1.1.1.1 / WARP")
            }
        }
    }
}


@Composable
private fun TtsSourcesCard(vm: PlayerViewModel, ui: UiState) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(Radii.xl), color = cs.surfaceVariant.copy(alpha = 0.42f)) {
        Column(Modifier.padding(SettingsCardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Nguồn giọng đọc", style = MaterialTheme.typography.titleMedium)
            Text("Tắt một nguồn sẽ ẩn toàn bộ giọng của nguồn đó khỏi danh sách chọn giọng và ngừng dùng nguồn đó khi phát. Giọng đang chọn thuộc nguồn vừa tắt sẽ tự chuyển sang một nguồn còn bật.",
                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            if (ui.ttsSources.isEmpty()) {
                Text("Đang tải danh sách nguồn…", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
            ui.ttsSources.forEach { src ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(src.name, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when {
                                !src.configured -> "Chưa cấu hình"
                                src.voices > 0 -> "${src.voices} giọng"
                                else -> "Chưa có giọng khả dụng"
                            },
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                    Switch(checked = src.enabled, onCheckedChange = { vm.toggleTtsSource(src.key, it) })
                }
            }
        }
    }
}


@Composable
private fun TikTokTtsCard(vm: PlayerViewModel, ui: UiState) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(Radii.xl), color = cs.surfaceVariant.copy(alpha = 0.42f)) {
        Column(Modifier.padding(SettingsCardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("TikTok TTS · trực tuyến", style = MaterialTheme.typography.titleMedium)
            Text("Có giọng tiếng Việt nam/nữ và tiếng Anh trong danh sách Giọng đọc AI. Không đổi giọng đang chọn nếu bạn chưa bấm chọn TikTok.",
                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Text("Khi chọn TikTok, nội dung đoạn đọc được gửi qua dịch vụ bên thứ ba ottsy.weilbyte.dev. Cần mạng; không nhập tài khoản/cookie TikTok. Dịch vụ có thể giới hạn hoặc tạm ngừng. Tốc độ nghe do bộ phát điều khiển.",
                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.setVoice("tiktok:BV074_streaming") }, modifier = Modifier.weight(1f)) {
                    Text(if (ui.selectedVoice == "tiktok:BV074_streaming") "Đang dùng · Nữ" else "Chọn giọng nữ")
                }
                OutlinedButton(onClick = { vm.setVoice("tiktok:BV075_streaming") }, modifier = Modifier.weight(1f)) {
                    Text(if (ui.selectedVoice == "tiktok:BV075_streaming") "Đang dùng · Nam" else "Chọn giọng nam")
                }
            }
        }
    }
}
