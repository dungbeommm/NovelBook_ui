package vn.tramtruyen.reader.presentation.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import vn.tramtruyen.reader.model.VoiceOption
import vn.tramtruyen.reader.engine.speech.VBookModelRow
import vn.tramtruyen.reader.engine.speech.VBookTtsRuntime
import vn.tramtruyen.reader.presentation.LocalBottomBarInset
import vn.tramtruyen.reader.presentation.PlayerViewModel
import vn.tramtruyen.reader.presentation.UiState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceManagerPage(vm: PlayerViewModel, ui: UiState, onOpenClone: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val catalog by VBookTtsRuntime.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf(AllVoices) }
    var tool by rememberSaveable { mutableStateOf(AllVoices) }
    var gender by rememberSaveable { mutableStateOf(AllVoices) }
    var availability by rememberSaveable { mutableStateOf(AllVoices) }
    var singleVoiceDownloads by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var deleteModelId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        VBookTtsRuntime.loadCatalog(context)
        vm.refreshVoiceCatalog()
    }

    val groups = remember(catalog.models, ui.voiceGroups) {
        val voices = ui.voiceGroups.flatMap { it.voices }
        val byId = voices.associateBy { it.id }
        val modelVariants = catalog.models.flatMap { row ->
            val model = row.model
            val engine = VBookTtsRuntime.engineLabel(model.modelType)
            val label = if (model.speakerCount > 1) "$engine · ${model.name}" else engine
            val knownNames = model.catalogSpeakerNames
            (0 until row.voiceCount).mapNotNull { index ->
                val key = "vbook:${model.id}:$index"
                val voice = byId[key]
                val name = voice?.label?.removePrefix("${model.name} · ")
                    ?: knownNames.getOrNull(index)
                    ?: model.name.takeIf { row.voiceCount == 1 }
                name?.let {
                    VoiceVariant(it, label, engine, model.language, model.gender, key, voice, row)
                }
            }.ifEmpty {
                // Still expose packages without a published speaker list.
                listOf(VoiceVariant(model.name, label, engine, model.language, model.gender, model.id, null, row))
            }
        }
        val serviceVariants = voices.filterNot { it.id.startsWith("vbook:") }.map {
            VoiceVariant(it.label, it.provider, it.provider, it.language, it.gender, it.id, it, null)
        }
        (modelVariants + serviceVariants)
            .groupBy { voiceGroupKey(it.name) }
            .map { (key, variants) -> NamedVoiceGroup(key, variants.first().name, variants) }
            .sortedWith(compareBy(java.text.Collator.getInstance(Locale.forLanguageTag("vi"))) { it.name })
    }
    val variants = groups.flatMap { it.variants }
    val languageOptions = variants.map { languageLabel(it.language) }.distinct().sorted()
    val toolOptions = variants.map { it.tool }.distinct().sorted()
    val genderOptions = variants.map { genderLabel(it.gender) }.distinct().sorted()
    val search = voiceSearchKey(query.trim())
    fun matches(value: String, filter: String) = filter == AllVoices || value == filter
    val visible = groups.mapNotNull { group ->
        val filtered = group.variants.filter { variant ->
            matches(languageLabel(variant.language), language) && matches(variant.tool, tool) &&
                matches(genderLabel(variant.gender), gender) && matches(variant.availability, availability) &&
                (!singleVoiceDownloads || variant.availability != Downloadable || variant.model?.model?.speakerCount == 1) &&
                (search.isEmpty() || listOf(group.name, variant.label, variant.model?.model?.description.orEmpty())
                    .any { voiceSearchKey(it).contains(search) })
        }
        if (filtered.isEmpty()) null else group.copy(variants = filtered)
    }
    val sections = visible.groupBy { group ->
        when {
            group.variants.any { it.availability == Installed } -> Installed
            group.variants.any { it.availability == Downloadable } -> Downloadable
            else -> Services
        }
    }

    val firstInstalledKey = sections[Installed]?.firstOrNull()?.key

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 24.dp + LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item("search") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VoicePageHeading("Quản lý giọng đọc")
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = cs.surfaceContainerHigh,
                        unfocusedContainerColor = cs.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text(if (groups.isEmpty()) "Tìm giọng đọc" else "Tìm trong ${groups.size} giọng đọc") },
                    trailingIcon = if (query.isEmpty()) null else {
                        { IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Xóa tìm kiếm") } }
                    },
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VoiceFilterMenu("Ngôn ngữ", language, listOf(AllVoices) + languageOptions) { language = it }
                    VoiceFilterMenu("Công cụ", tool, listOf(AllVoices) + toolOptions) { tool = it }
                    VoiceFilterMenu("Giới tính", gender, listOf(AllVoices) + genderOptions) { gender = it }
                    VoiceFilterMenu("Trạng thái", availability, listOf(AllVoices, Installed, Downloadable, Services)) { availability = it }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = singleVoiceDownloads,
                        onClick = { singleVoiceDownloads = true },
                        label = { Text("Tải một giọng") },
                    )
                    FilterChip(
                        selected = !singleVoiceDownloads,
                        onClick = { singleVoiceDownloads = false },
                        label = { Text("Hiện cả gói dùng chung") },
                    )
                }
            }
        }
        val status = ui.voiceActionStatus ?: catalog.message
        if (!catalog.runtimeReady || !status.isNullOrBlank() || catalog.loadingCatalog && catalog.models.isEmpty()) {
            item("status") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!catalog.runtimeReady) Text("Giọng ngoại tuyến cần bản cài ARM64.", color = cs.error)
                    if (!status.isNullOrBlank()) Text(status, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    if (catalog.loadingCatalog && catalog.models.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
        item("my_voices") {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                VoiceSectionTitle("Giọng của tôi", 0)
                Surface(onClick = onOpenClone, shape = RoundedCornerShape(20.dp), color = cs.tertiaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        VoiceMark(null) { Icon(Icons.Outlined.Add, null) }
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text("Tạo giọng từ bản ghi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Chọn một đoạn ghi âm làm mẫu", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = cs.onTertiaryContainer)
                    }
                }
            }
        }
        for (section in listOf(Installed, Downloadable, Services)) {
            val named = sections[section].orEmpty()
            if (named.isEmpty()) continue
            item("section:$section") { VoiceSectionTitle(section, named.size) }
            items(named, key = { "voice:${it.key}" }) { group ->
                val defaultOpen = group.key == firstInstalledKey ||
                    group.variants.any { it.key == ui.selectedVoice } || search.isNotEmpty()
                val open = defaultOpen xor (group.key in expanded)
                NamedVoiceCard(
                    group, open, ui.selectedVoice, catalog.runtimeReady, vm,
                    onToggle = { expanded = if (group.key in expanded) expanded - group.key else expanded + group.key },
                    onDelete = { deleteModelId = it },
                )
            }
        }
        if (visible.isEmpty() && !catalog.loadingCatalog) {
            item("empty") {
                Text("Không tìm thấy giọng phù hợp.", color = cs.onSurfaceVariant)
            }
        }
    }

    val pending = catalog.models.firstOrNull { it.model.id == deleteModelId && it.installed }
    if (pending != null) {
        val selected = ui.selectedVoice.startsWith("vbook:${pending.model.id}:")
        AlertDialog(
            onDismissRequest = { deleteModelId = null },
            title = { Text(if (selected) "Gói giọng đang được chọn" else "Xóa ${pending.model.name}?") },
            text = {
                Text(if (selected) "Chọn giọng khác trước khi xóa gói này."
                else "Thao tác này xóa toàn bộ ${pending.voiceCount} giọng trong gói. Bạn có thể tải lại sau.")
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!selected) vm.updateVoiceModel(pending.model.id, install = false)
                    deleteModelId = null
                }) { Text(if (selected) "Đóng" else "Xóa gói") }
            },
            dismissButton = if (selected) null else {
                { TextButton(onClick = { deleteModelId = null }) { Text("Hủy") } }
            },
        )
    }
}

private data class VoiceVariant(
    val name: String,
    val label: String,
    val tool: String,
    val language: String,
    val gender: String,
    val key: String,
    val voice: VoiceOption?,
    val model: VBookModelRow?,
) {
    val availability: String get() = when (model?.installed) {
        true -> Installed
        false -> Downloadable
        null -> Services
    }
}

private data class NamedVoiceGroup(val key: String, val name: String, val variants: List<VoiceVariant>)

private const val AllVoices = "Tất cả"
private const val Installed = "Đã cài"
private const val Downloadable = "Có thể tải"
private const val Services = "Nguồn giọng khác"
private val voiceDiacritics = Regex("\\p{M}+")
private val voiceNameSpace = Regex("\\s+")

private fun voiceGroupKey(value: String): String =
    java.text.Normalizer.normalize(value.trim().replace(voiceNameSpace, " ").lowercase(Locale.ROOT), java.text.Normalizer.Form.NFC)

private fun voiceSearchKey(value: String): String =
    java.text.Normalizer.normalize(value.lowercase(Locale.ROOT).replace('đ', 'd'), java.text.Normalizer.Form.NFD)
        .replace(voiceDiacritics, "")

@Composable
private fun VoicePageHeading(title: String) {
    Text(
        title,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
    )
}

@Composable
private fun VoiceSectionTitle(title: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        CountBadge(count)
    }
}

@Composable
private fun VoiceMark(name: String?, icon: (@Composable () -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.size(46.dp),
        shape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp, bottomStart = 15.dp, bottomEnd = 4.dp),
        color = if (name == null) cs.tertiary else cs.secondaryContainer,
        contentColor = if (name == null) cs.onTertiary else cs.onSecondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) icon()
            else Text(name.orEmpty().trim().split(voiceNameSpace).take(2).joinToString("") { it.take(1) }.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NamedVoiceCard(
    group: NamedVoiceGroup,
    open: Boolean,
    selectedVoice: String,
    runtimeReady: Boolean,
    vm: PlayerViewModel,
    onToggle: () -> Unit,
    onDelete: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val versions = group.variants.sortedBy { if (it.availability == Installed) 0 else 1 }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (open) cs.surfaceContainerHigh else cs.surfaceContainerLow,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VoiceMark(group.name)
                Column(Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${versions.size} phiên bản · ${versions.map { it.tool }.distinct().joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (open) "Thu gọn" else "Mở phiên bản", tint = cs.primary)
            }
            AnimatedVisibility(open) {
                Column(Modifier.padding(horizontal = 10.dp).padding(bottom = 8.dp)) {
                    versions.forEach { variant ->
                        key(variant.key) {
                            HorizontalDivider(color = cs.outlineVariant.copy(alpha = .55f))
                            VoiceVariantRow(variant, selectedVoice, runtimeReady, vm, onDelete)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceVariantRow(
    variant: VoiceVariant,
    selectedVoice: String,
    runtimeReady: Boolean,
    vm: PlayerViewModel,
    onDelete: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val row = variant.model
    val voice = variant.voice
    val current = voice?.id == selectedVoice
    val onlineSample = row?.model?.previewUrl?.takeIf { row.model.speakerCount == 1 }
    val preview: (() -> Unit)? = when {
        voice != null -> { { vm.previewVoice(voice.id) } }
        onlineSample != null -> { { vm.previewVoiceModel(onlineSample) } }
        else -> null
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { preview?.invoke() }, enabled = preview != null) {
            Icon(Icons.Outlined.PlayArrow, "Nghe thử ${variant.name}, ${variant.label}")
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(variant.label, style = MaterialTheme.typography.titleSmall)
            val details = listOfNotNull(
                languageLabel(variant.language),
                row?.model?.sizeBytes?.let(::formatModelSize),
                row?.model?.speakerCount?.takeIf { it > 1 }?.let { "Gói $it giọng" },
                if (current) "Đang dùng" else null,
            ).joinToString(" · ")
            Text(details, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        if (voice != null) {
            if (current) Icon(Icons.Filled.Check, "Đang dùng", tint = cs.primary, modifier = Modifier.padding(10.dp))
            else IconButton(onClick = { vm.setVoice(voice.id) }) {
                Icon(Icons.Outlined.Check, "Dùng ${variant.name}, ${variant.label}", tint = cs.primary)
            }
        }
        if (row != null) {
            when {
                row.progress != null -> CircularProgressIndicator(
                    progress = { row.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.padding(10.dp).size(22.dp),
                    strokeWidth = 2.dp,
                )
                row.installed -> IconButton(onClick = { onDelete(row.model.id) }) {
                    Icon(Icons.Outlined.DeleteOutline, "Xóa gói ${row.model.name}", tint = cs.error)
                }
                else -> FilledTonalIconButton(
                    onClick = { vm.updateVoiceModel(row.model.id, install = true) },
                    enabled = runtimeReady,
                ) { Icon(Icons.Outlined.Download, "Tải gói ${row.model.name}") }
            }
        }
    }
}
private fun formatModelSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "${bytes / 1024L} KB"
    else -> "$bytes B"
}

@Composable
private fun VoiceFilterMenu(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(if (selected == "Tất cả") label else selected, maxLines = 1)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (option == "Tất cả") "Tất cả $label" else option) },
                    trailingIcon = { if (option == selected) Icon(Icons.Filled.Check, null) },
                    onClick = { onSelected(option); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = CircleShape, color = cs.secondaryContainer) {
        Text(
            count.toString(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            color = cs.onSecondaryContainer,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun languageLabel(language: String): String = when (language.lowercase().substringBefore('-')) {
    "vi", "vie" -> "Tiếng Việt"
    "en", "eng" -> "Tiếng Anh"
    "zh", "cmn" -> "Tiếng Trung"
    "ja", "jpn" -> "Tiếng Nhật"
    "ko", "kor" -> "Tiếng Hàn"
    "mul" -> "Đa ngôn ngữ"
    "khác", "" -> "Khác"
    else -> language
}

private fun genderLabel(gender: String): String = when (gender.uppercase()) {
    "MALE" -> "Nam"
    "FEMALE" -> "Nữ"
    "NEUTRAL" -> "Trung tính"
    "MULTI" -> "Nhiều giọng"
    "OTHER", "" -> "Khác"
    else -> "Khác"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceClonePage() {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var recording by rememberSaveable { mutableStateOf("") }
    var showTips by rememberSaveable { mutableStateOf(true) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) recording = displayName(context, uri)
    }
    Column(Modifier.fillMaxSize().padding(bottom = LocalBottomBarInset.current)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            VoicePageHeading("Nhân bản giọng nói")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 10.dp, bottomEnd = 28.dp),
                color = cs.tertiaryContainer,
                contentColor = cs.onTertiaryContainer,
            ) {
                Column(
                    Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        Modifier.height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        listOf(12, 22, 34, 46, 30, 18, 26, 12).forEach { size ->
                            Box(Modifier.width(5.dp).height(size.dp).background(cs.tertiary, CircleShape))
                        }
                    }
                    Text("Dùng một đoạn ghi âm rõ tiếng làm mẫu cho giọng đọc.", textAlign = TextAlign.Center)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("10–30 giây", "Một người nói", "Nơi yên tĩnh").forEach { tip ->
                            Surface(shape = CircleShape, color = cs.surface, contentColor = cs.onSurface) {
                                Text(tip, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            Text("Bản ghi âm", color = cs.primary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Surface(
                onClick = { picker.launch(arrayOf("audio/*")) },
                shape = RoundedCornerShape(18.dp),
                color = cs.surfaceContainerHigh,
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    VoiceMark(null) { Icon(Icons.Outlined.UploadFile, null) }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(recording.ifBlank { "Chọn tệp ghi âm" }, style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (recording.isEmpty()) "MP3, WAV, M4A, FLAC" else "Chạm để đổi bản ghi",
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = cs.primary)
                }
            }
            Surface(shape = RoundedCornerShape(18.dp), color = cs.surfaceContainerLow) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().clickable { showTips = !showTips }.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Outlined.Info, null, tint = cs.primary)
                        Text("Mẹo thu âm", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Icon(if (showTips) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            if (showTips) "Ẩn mẹo" else "Hiện mẹo")
                    }
                    AnimatedVisibility(showTips) {
                        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 15.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("• Đọc một đoạn ngắn với tốc độ và giọng nói tự nhiên.")
                            Text("• Tắt nhạc, tránh tiếng quạt và tiếng vọng.")
                            Text("• Giữ khoảng cách với micro để tiếng rõ và không bị vỡ.")
                        }
                    }
                }
            }
        }
        Surface(color = cs.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tính năng tạo giọng chưa khả dụng.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Tạo giọng") }
            }
        }
    }
}
private fun displayName(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Tệp ghi âm"
}
