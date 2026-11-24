package vn.tramtruyen.reader.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.tramtruyen.reader.model.Story
import vn.tramtruyen.reader.model.Tab
import vn.tramtruyen.reader.model.placeholderStory
import vn.tramtruyen.reader.presentation.LocalBottomBarInset
import vn.tramtruyen.reader.presentation.PlayerViewModel
import vn.tramtruyen.reader.presentation.UiState
import vn.tramtruyen.reader.presentation.components.CoverArt
import vn.tramtruyen.reader.presentation.components.Radii
import vn.tramtruyen.reader.presentation.components.SectionHeader
import vn.tramtruyen.reader.presentation.components.noGrayClick

@Composable
fun HomeScreen(vm: PlayerViewModel, ui: UiState) {
    val cs = MaterialTheme.colorScheme
    val continuation = remember(ui.historyItems, ui.activeStory, ui.libraryItems) {
        ui.activeStory?.takeIf { it.id != placeholderStory.id }
            ?: vm.historyStories().firstOrNull()
            ?: vm.allStories().firstOrNull()
            ?: placeholderStory
    }
    val explore = remember(ui.historyItems, ui.libraryItems) { vm.allStories() }
    val trending = remember(explore) { vm.trendingStories() }
    val clipboard = LocalClipboardManager.current
    var clipboardLink by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        clipboardLink = clipboard.getText()?.text?.trim()?.takeIf { vm.isLinkQuery(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 14.dp,
            bottom = 24.dp + LocalBottomBarInset.current,
        ),
    ) {
        item(key = "home_header", contentType = "header") {
            HomeHeader(onSettings = { vm.setTab(Tab.SETTINGS) })
        }

        clipboardLink?.let { link ->
            item(key = "clipboard_link", contentType = "action") {
                Spacer(Modifier.height(16.dp))
                ClipboardLinkCard(
                    onOpen = { vm.analyzeFromSearch(link); clipboardLink = null },
                    onDismiss = { clipboardLink = null },
                )
            }
        }

        item(key = "add_story", contentType = "action") {
            Spacer(Modifier.height(if (clipboardLink == null) 24.dp else 12.dp))
            AddStoryCard(onClick = vm::openPaste)
        }

        item(key = "continue_listening", contentType = "hero") {
            SectionHeader("Tiếp tục nghe")
            ContinueListeningCard(
                story = continuation,
                onClick = {
                    if (continuation.id == placeholderStory.id) vm.openPaste()
                    else vm.openPlayer(continuation)
                },
            )
        }

        if (explore.isNotEmpty()) {
            item(key = "library_shelf", contentType = "shelf") {
                SectionHeader("Tủ sách của bạn") {
                    TextButton(onClick = {
                        vm.setLibFilter("all")
                        vm.setTab(Tab.LIBRARY)
                    }) {
                        Text("Xem tất cả")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Outlined.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    items(explore, key = { it.id }, contentType = { "story" }) { story ->
                        ExploreCard(story) { vm.openPlayer(story) }
                    }
                }
            }
        }

        if (trending.isNotEmpty()) {
            item(key = "trending_header", contentType = "header") { SectionHeader("Được nghe gần đây") }
            item(key = "trending_list", contentType = "story_list") {
                Surface(
                    shape = RoundedCornerShape(Radii.lg),
                    color = cs.surfaceVariant.copy(alpha = 0.42f),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        trending.take(5).forEachIndexed { index, story ->
                            TrendingRow(index + 1, story) { vm.openPlayer(story) }
                            if (index < minOf(trending.lastIndex, 4)) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 48.dp),
                                    color = cs.outlineVariant.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipboardLinkCard(onOpen: () -> Unit, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(18.dp), color = cs.secondaryContainer,
        contentColor = cs.onSecondaryContainer) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Link, null, Modifier.size(21.dp))
            Spacer(Modifier.width(9.dp))
            Text("Đã tìm thấy liên kết trong bộ nhớ", style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onOpen) { Text("Mở") }
            IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Close, "Bỏ qua")
            }
        }
    }
}

@Composable
private fun HomeHeader(onSettings: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(cs.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(vn.tramtruyen.reader.R.drawable.ic_logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(cs.onPrimaryContainer),
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Text(
            stringResource(vn.tramtruyen.reader.R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = cs.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        FilledTonalIconButton(
            onClick = onSettings,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Outlined.Settings, "Cài đặt")
        }
    }
}

@Composable
private fun AddStoryCard(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.xl)).noGrayClick(onClick = onClick),
        shape = RoundedCornerShape(Radii.xl),
        color = cs.primaryContainer,
        contentColor = cs.onPrimaryContainer,
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                    .background(cs.onPrimaryContainer.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Link, null, Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Thêm truyện từ liên kết", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text("Dán link, app tự tải chương và chuẩn bị giọng đọc.", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ArrowForward, null, Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ContinueListeningCard(story: Story, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val isEmpty = story.id == placeholderStory.id
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radii.xl),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceColorAtElevation(2.dp)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverArt(
                story.color,
                story.coverUrl,
                modifier = Modifier.width(96.dp).height(122.dp),
                corner = Radii.md,
                sizeHintPx = 256,
                fallbackKey = story.id,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Headphones, null, Modifier.size(16.dp), tint = cs.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isEmpty) "BẮT ĐẦU" else "ĐANG NGHE",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    story.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = cs.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isEmpty) "Dán liên kết truyện đầu tiên" else "Chương ${story.currentChapter}${story.totalChapters.takeIf { it > 0 }?.let { "/$it" }.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { if (isEmpty) 0f else story.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                )
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 14.dp)) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isEmpty) "Thêm truyện" else "Nghe tiếp")
                }
            }
        }
    }
}

@Composable
private fun ExploreCard(story: Story, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.width(144.dp).noGrayClick(onClick = onClick)) {
        CoverArt(
            story.color,
            story.coverUrl,
            Modifier.width(144.dp).height(184.dp),
            Radii.md,
            sizeHintPx = 320,
            fallbackKey = story.id,
        )
        Spacer(Modifier.height(10.dp))
        Text(story.title, style = MaterialTheme.typography.titleSmall, color = cs.onSurface,
            fontWeight = FontWeight.SemiBold, minLines = 2, maxLines = 2,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(story.author, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TrendingRow(rank: Int, story: Story, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().noGrayClick(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            rank.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelLarge,
            color = cs.primary,
            modifier = Modifier.width(34.dp),
            fontWeight = FontWeight.Bold,
        )
        CoverArt(story.color, story.coverUrl, Modifier.size(48.dp), Radii.sm,
            sizeHintPx = 128, fallbackKey = story.id)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(story.title, style = MaterialTheme.typography.titleSmall, color = cs.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(story.author, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onClick) { Icon(Icons.Filled.PlayArrow, "Phát", tint = cs.primary) }
    }
}
