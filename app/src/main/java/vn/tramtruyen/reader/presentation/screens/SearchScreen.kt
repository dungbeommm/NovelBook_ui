package vn.tramtruyen.reader.presentation.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.selected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil.compose.AsyncImage
import vn.tramtruyen.reader.model.ChapterUiModel
import vn.tramtruyen.reader.model.Story
import vn.tramtruyen.reader.model.WebSearchHit
import vn.tramtruyen.reader.presentation.LocalBottomBarInset
import vn.tramtruyen.reader.presentation.PlayerViewModel
import vn.tramtruyen.reader.presentation.UiState
import vn.tramtruyen.reader.presentation.components.CoverArt
import vn.tramtruyen.reader.presentation.components.CoverDefaults
import vn.tramtruyen.reader.presentation.components.Radii
import vn.tramtruyen.reader.presentation.components.noGrayClick
import vn.tramtruyen.reader.presentation.motion.rememberHaptics
import vn.tramtruyen.reader.SourceBrowserActivity
import vn.tramtruyen.reader.WikiCvLoginActivity
import vn.tramtruyen.reader.engine.sources.BrowserSession
import vn.tramtruyen.reader.engine.sources.WikiCvSession

/**
 * Man TIM KIEM.
 *
 * O nhap la "o thong minh":
 *  - Go chu   -> tim tren trang web dang chon.
 *  - Dan link -> nhan ra ngay va chuyen sang luong phan tich link.
 *
 * Ket qua web hien DAN theo tung nguon tra ve, va KHONG BAO GIO bi xoa trang giua
 * chung: het gio hay loi mang thi phan da tim duoc van nam nguyen tren man hinh.
 */
@Composable
fun SearchScreen(vm: PlayerViewModel, ui: UiState) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val q = ui.searchQuery
    var wikiLoggedIn by remember { mutableStateOf(WikiCvSession.isLoggedIn()) }
    val wikiLogin = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        wikiLoggedIn = WikiCvSession.isLoggedIn()
        if (ui.selectedWebSearchSource.removePrefix("www.") == "wikicv.org" && q.isNotBlank()) {
            vm.searchWeb(q)
        }
    }
    val isLink = remember(q) { vm.isLinkQuery(q) }
    val keyboard = LocalSoftwareKeyboardController.current
    val submitSearch: () -> Unit = {
        keyboard?.hide()
        if (isLink) vm.analyzeFromSearch(q)
        else if (q.isNotBlank()) vm.searchWeb(q)
    }
    // Results belong only to the source selected beside the search field.
    val webList = ui.webResults

    val webSources = ui.webSearchSources

    // Load only local parser metadata; do not prewarm/network all sites.
    // Pager now precomposes the adjacent page. Prepare lightweight source
    // metadata at that moment instead of waiting for the first visible frame.
    LaunchedEffect(Unit) { vm.prepareWebSearchSources() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp,
            bottom = 24.dp + LocalBottomBarInset.current),
    ) {
        item(key = "search_header", contentType = "header") {
            SearchHeader()
            Spacer(Modifier.height(16.dp))
            SearchField(
                value = q,
                onValueChange = vm::setSearchQuery,
                onClear = { vm.setSearchQuery("") },
                onSearch = submitSearch,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SearchSourcePicker(
                    sources = webSources,
                    selected = ui.selectedWebSearchSource,
                    onSelect = vm::selectWebSearchSource,
                    onOpenBrowser = {
                        context.startActivity(Intent(context, SourceBrowserActivity::class.java)
                            .putExtra(SourceBrowserActivity.EXTRA_URL, BrowserSession.HOME_URL))
                    },
                    modifier = Modifier.weight(1f),
                )
                // Browser shortcut replaces the square search button beside sources.
                // Keep it available even when the search field is empty.
                FilledTonalIconButton(
                    onClick = {
                        keyboard?.hide()
                        haptics.tap()
                        context.startActivity(Intent(context, SourceBrowserActivity::class.java)
                            .putExtra(SourceBrowserActivity.EXTRA_URL, vm.browserUrl()))
                    },
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = cs.primaryContainer,
                        contentColor = cs.onPrimaryContainer,
                    ),
                ) {
                    Icon(Icons.Outlined.Public, "Mở trình duyệt", modifier = Modifier.size(30.dp))
                }
            }
            if (ui.selectedWebSearchSource.removePrefix("www.") == "wikicv.org") {
                val source = ui.selectedWebSearchSource.removePrefix("www.")
                Column(Modifier.padding(start = 8.dp, bottom = 8.dp)) {
                    if (source == "wikicv.org" && !wikiLoggedIn) {
                        TextButton(
                            onClick = {
                                wikiLogin.launch(
                                    Intent(context, WikiCvLoginActivity::class.java)
                                        .putExtra(WikiCvLoginActivity.EXTRA_QUERY, q.trim())
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        ) {
                            Icon(Icons.Outlined.Public, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Đăng nhập / quản lý tài khoản trong app")
                        }
                        Text(
                            "Phiên đăng nhập WikiCV được lưu trong app và dùng chung khi tìm, mở và đọc truyện.",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                        )
                    } else if (source == "wikicv.org") {
                        Text(
                            "Đã kết nối tài khoản WikiCV",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.primary,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        when {
            isLink -> item(key = "link_result", contentType = "link") { LinkCard(q) { vm.analyzeFromSearch(q) } }

            q.isBlank() -> {
                item(key = "suggestions_header", contentType = "header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Đề xuất từ ${ui.selectedWebSearchSource.removePrefix("www.").ifBlank { "web" }}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.onSurface,
                        )
                        if (!ui.webSuggestionsLoading && ui.selectedWebSearchSource.isNotBlank()) {
                            IconButton(onClick = { vm.loadWebSuggestions() }) {
                                Icon(Icons.Filled.Refresh, "Tải lại đề xuất")
                            }
                        }
                    }
                }
                if (ui.webSuggestionsLoading) {
                    items(3, key = { "suggestion_skeleton_$it" }) { SkeletonHitRow(it) }
                } else if (ui.webSuggestionsSource == ui.selectedWebSearchSource && ui.webSuggestions.isNotEmpty()) {
                    items(ui.webSuggestions, key = { "suggestion_${it.url}" }) { hit ->
                        WebHitCard(hit, ui.webOpeningUrl == hit.url,
                            onPlay = { vm.playFirstChapter(hit) },
                            onOpenLink = { vm.analyzeFromSearch(hit.url) })
                    }
                } else if (ui.webSuggestionsError != null) {
                    item(key = "suggestions_error") {
                        EmptyState(ui.webSuggestionsError) { vm.loadWebSuggestions() }
                    }
                }
            }

            else -> {
                // ----- KHU VUC KET QUA TU WEB -----
                item(key = "web_header", contentType = "header") {
                    WebSectionHeader(
                        searching = ui.webSearching,
                        sites = ui.webSitesSearched,
                        total = webList.size,
                        exact = webList.count { hit -> hit.exact },
                        onRetry = { vm.searchWeb(q) },
                    )
                }

                // Dang tim ma chua co gi -> hien khung xam nhap nhay cho do trong.
                if (webList.isEmpty() && ui.webSearching) {
                    items(3, key = { index -> "web_skeleton_$index" }, contentType = { "skeleton" }) { index ->
                        SkeletonHitRow(index)
                    }
                }

                items(
                    webList,
                    key = { hit -> hit.url },
                    // Cho Compose tái dùng đúng loại ô khi danh sách đổi liên tục -> ít giật hơn.
                    contentType = { "webHit" },
                ) { hit ->
                    // Ket qua ve dan tung dot -> moi the truot vao cho cua no.
                    // While sources are streaming, placement animation makes every
                    // batch relayout the list. Animate only after the result set settles.
                    Box {
                        WebHitCard(
                            hit = hit,
                            opening = ui.webOpeningUrl == hit.url,
                            onPlay = { vm.playFirstChapter(hit) },
                            onOpenLink = { vm.analyzeFromSearch(hit.url) },
                        )
                    }
                }

                val err = ui.webError
                if (!ui.webSearching && webList.isEmpty()) {
                    item(key = "web_empty", contentType = "empty") {
                        when {
                            err != null -> EmptyState(err) { vm.searchWeb(q) }

                            else -> EmptyState("Không có kết quả cho “$q”") { vm.searchWeb(q) }
                        }
                    }
                }
            }
        }
    }
}

/** O nhap tim kiem bo tron kieu "vien thuoc", nhin nhe hon OutlinedTextField mac dinh. */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val keyboard = LocalSoftwareKeyboardController.current
    var field by remember { mutableStateOf(TextFieldValue(value)) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(value) {
        if (field.text != value) field = TextFieldValue(value, TextRange(value.length))
    }
    fun selectAll() {
        // Run after TextField's own pointer handler, without consuming its gesture.
        scope.launch { yield(); field = field.copy(selection = TextRange(0, field.text.length)) }
    }
    TextField(
        value = field,
        onValueChange = { next ->
            field = next
            if (next.text != value) onValueChange(next.text)
        },
        modifier = modifier.height(56.dp)
            .onFocusChanged { if (it.isFocused) selectAll() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                    if (up != null && (up.position - down.position).getDistance() <= viewConfiguration.touchSlop &&
                        up.uptimeMillis - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis) {
                        selectAll()
                    }
                }
            },
        singleLine = true,
        shape = CircleShape,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            keyboard?.hide()
            onSearch()
        }),
        placeholder = {
            Text(
                "Nhập tên truyện, tác giả hoặc dán link",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = { Icon(Icons.Filled.Search, null, tint = cs.primary) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear) { Icon(Icons.Filled.Close, "Xoá") }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = cs.surfaceColorAtElevation(3.dp),
            unfocusedContainerColor = cs.surfaceColorAtElevation(2.dp),
            disabledContainerColor = cs.surfaceColorAtElevation(2.dp),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

/** Title only; the browser shortcut lives beside the source picker. */
@Composable
private fun SearchHeader() {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Tìm kiếm", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = cs.onSurface, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SearchSourcePicker(
    sources: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onOpenBrowser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    val keyboard = LocalSoftwareKeyboardController.current
    var expanded by remember { mutableStateOf(false) }
    val label = selected.removePrefix("www.").ifBlank { "Chọn web" }
    Surface(
        modifier = modifier.height(56.dp),
        onClick = { keyboard?.hide(); haptics.tap(); expanded = true },
        shape = RoundedCornerShape(16.dp),
        color = cs.secondaryContainer,
        contentColor = cs.onSecondaryContainer,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceLogo(selected, Modifier.size(28.dp))
            Text("Nguồn: $label", modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Filled.KeyboardArrowDown, "Chọn nguồn tìm kiếm", Modifier.size(20.dp))
        }
    }
    if (expanded) {
        SearchSourceSheet(
            sources = sources,
            selected = selected,
            onDismiss = { keyboard?.hide(); expanded = false },
            onSelect = { domain ->
                haptics.tick()
                expanded = false
                onSelect(domain)
            },
            onOpenBrowser = {
                haptics.tap()
                expanded = false
                onOpenBrowser()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSourceSheet(
    sources: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onOpenBrowser: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val keyboard = LocalSoftwareKeyboardController.current
    var filter by remember { mutableStateOf("") }
    val filtered = remember(sources, filter, selected) {
        sources.distinct().filter { it.contains(filter.trim(), ignoreCase = true) }
            .sortedBy { if (it == selected) 0 else 1 }
    }
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.76f
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cs.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.imePadding(),
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)
            .padding(start = 18.dp, end = 18.dp, bottom = 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Chọn nguồn tìm kiếm", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text("${sources.distinct().size} nguồn · Chạm để chọn nhanh",
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
                IconButton(onClick = { keyboard?.hide(); onDismiss() }) {
                    Icon(Icons.Filled.Close, "Đóng chọn nguồn")
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                label = { Text("Lọc nguồn") },
                placeholder = { Text("Nhập tên web hoặc tên miền") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (filter.isNotEmpty()) IconButton(onClick = { filter = "" }) {
                        Icon(Icons.Filled.Close, "Xoá bộ lọc nguồn")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
            )
            Spacer(Modifier.height(16.dp))
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text(if (sources.isEmpty()) "Chưa có nguồn tìm kiếm" else "Không tìm thấy nguồn phù hợp",
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    gridItems(filtered, key = { it }) { domain ->
                        val isSelected = domain == selected
                        Surface(
                            onClick = { keyboard?.hide(); onSelect(domain) },
                            modifier = Modifier.fillMaxWidth().semantics { this.selected = isSelected },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) cs.primary else cs.outlineVariant),
                            color = if (isSelected) cs.primaryContainer else cs.surfaceContainerLow,
                            contentColor = if (isSelected) cs.onPrimaryContainer else cs.onSurface,
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    SourceLogo(domain, Modifier.size(32.dp))
                                    Spacer(Modifier.weight(1f))
                                    if (isSelected) Icon(Icons.Filled.Check, "Đang chọn", Modifier.size(22.dp))
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(domain.removePrefix("www."), maxLines = 2,
                                    minLines = 2, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = cs.outlineVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { keyboard?.hide(); onOpenBrowser() },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Thêm nguồn bằng trình duyệt")
            }
        }
    }
}

@Composable
private fun SourceLogo(domain: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    var failed by remember(domain) { mutableStateOf(domain.isBlank()) }
    var loaded by remember(domain) { mutableStateOf(false) }
    val initial = domain.removePrefix("www.").firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier
            .clip(shape)
            .background(cs.primaryContainer)
            .then(if (failed || !loaded) Modifier.border(1.5.dp, cs.onPrimaryContainer, shape)
                else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (failed || !loaded) {
            Box(
                Modifier.fillMaxSize(0.74f).border(1.5.dp, cs.onPrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial, color = cs.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        if (!failed && domain.isNotBlank()) {
            AsyncImage(
                model = "https://$domain/favicon.ico",
                contentDescription = "Logo $domain",
                modifier = Modifier.fillMaxSize(0.76f).clip(CircleShape)
                    .alpha(if (loaded) 1f else 0f),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(cs.onPrimaryContainer),
                onSuccess = { loaded = true },
                onError = { failed = true },
            )
        }
    }
}

/**
 * Dau muc "Ket qua tu web" kem trang thai.
 *
 * Dang tim  -> thanh tien do mo + chu "Dang quet N nguon..."
 * Xong roi  -> so ket qua, so ban trung ten, va nut tim lai.
 */
@Composable
private fun WebSectionHeader(
    searching: Boolean,
    sites: Int,
    total: Int,
    exact: Int,
    onRetry: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Public, null, tint = cs.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Kết quả từ web",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
            )
            Spacer(Modifier.weight(1f))
            if (searching) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRetry, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Refresh,
                        "Tìm lại",
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (exact > 0) {
                Pill("$exact đúng tên", cs.primary)
                Spacer(Modifier.width(6.dp))
            }
            if (total > 0) {
                Pill("$total kết quả", cs.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
            }
            if (searching) {
                Pill(if (sites > 0) "Đang quét $sites nguồn" else "Đang quét…", cs.tertiary)
            } else if (sites > 0) {
                Pill("$sites nguồn", cs.onSurfaceVariant)
            }
        }

        if (searching) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp))
        }
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.14f)) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

/**
 * 1 ket qua web, dang the.
 *
 * - Bam CA THE (hoac nut Play) -> tu tim chuong 1 va NGHE NGAY.
 * - Bam bieu tuong link -> mo luong phan tich link (khi muon xem truoc).
 * - Nhan "Dung ten / Rat gan / Gan dung" cho biet khop den dau.
 */
@Composable
private fun WebHitCard(
    hit: WebSearchHit,
    opening: Boolean,
    onPlay: () -> Unit,
    onOpenLink: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val highlight = hit.exact
    Surface(
        shape = RoundedCornerShape(Radii.lg),
        color = if (highlight) cs.primary.copy(alpha = 0.07f) else cs.surfaceColorAtElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .noGrayClick(enabled = !opening, onClick = onPlay),
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(
                colorHex = CoverDefaults.colorForKey(hit.url),
                imageUrl = hit.coverUrl,
                modifier = Modifier.width(64.dp).height(84.dp),
                corner = Radii.sm,
                sizeHintPx = 192,
                fallbackKey = hit.url,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    hit.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hit.author.isNotBlank()) {
                    Text(
                        hit.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hit.exact) {
                        Pill("Đúng tên", cs.primary)
                    } else if (hit.matchLabel.isNotBlank()) {
                        Pill(hit.matchLabel, cs.onSurfaceVariant)
                    }
                    if (hit.source.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            hit.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    if (hit.mirrors.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "+${hit.mirrors.size} nguồn",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onOpenLink, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Outlined.Link,
                    "Xem liên kết",
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(2.dp))
            if (opening) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            } else {
                FilledIconButton(onClick = onPlay, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.PlayArrow, "Nghe chương 1")
                }
            }
        }
    }
}

/** Khung cho — hien khi dang quet ma chua nguon nao kip tra ve. */
@Composable
private fun SkeletonHitRow(index: Int) {
    val cs = MaterialTheme.colorScheme
    // Static placeholder: three independent infinite transitions were keeping
    // the render loop hot while network parsing and list updates happened.
    val a = 0.58f
    Surface(
        shape = RoundedCornerShape(Radii.md),
        color = cs.surfaceColorAtElevation(2.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).alpha(a),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(Radii.xs),
                color = cs.onSurface.copy(alpha = 0.10f),
                content = {},
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = cs.onSurface.copy(alpha = 0.10f),
                    content = {},
                    modifier = Modifier.fillMaxWidth(0.75f).height(13.dp),
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = CircleShape,
                    color = cs.onSurface.copy(alpha = 0.10f),
                    content = {},
                    modifier = Modifier.fillMaxWidth(0.45f).height(11.dp),
                )
            }
        }
    }
}

@Composable
private fun LinkCard(url: String, onAnalyze: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(Radii.md),
        color = cs.surfaceColorAtElevation(2.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Link, null, tint = cs.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Đây là một liên kết",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                url,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAnalyze,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Phân tích và nghe ngay")
            }
        }
    }
}

@Composable
private fun StoryRow(st: Story, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().noGrayClick(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(st.color, st.coverUrl, Modifier.size(48.dp), Radii.xs, sizeHintPx = 128, fallbackKey = st.id)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                st.title,
                style = MaterialTheme.typography.titleSmall,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val line = buildString {
                append("Chương ${st.currentChapter}")
                if (st.totalChapters > 0) append("/${st.totalChapters}")
                if (st.author.isNotBlank()) append(" · ${st.author}")
            }
            Text(
                line,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ChapterRow(ch: ChapterUiModel, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().noGrayClick(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${ch.index}",
            style = MaterialTheme.typography.labelLarge,
            color = cs.primary,
            modifier = Modifier.width(40.dp),
        )
        Text(
            ch.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (ch.isCurrent) cs.primary else cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Mau nen mac dinh cho o bia khi ket qua web khong co anh. */

/** Trang thai rong / loi, co nut thu lai neu can. */
@Composable
private fun EmptyState(title: String, onRetry: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = CircleShape, color = cs.primary.copy(alpha = 0.10f)) {
            Icon(
                Icons.Outlined.Info,
                null,
                tint = cs.primary,
                modifier = Modifier.padding(12.dp).size(22.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(28.dp)) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tìm lại")
            }
        }
    }
}
