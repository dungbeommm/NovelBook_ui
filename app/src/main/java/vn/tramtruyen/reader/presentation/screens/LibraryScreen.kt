package vn.tramtruyen.reader.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.tramtruyen.reader.model.Story
import vn.tramtruyen.reader.model.StorySourceType
import vn.tramtruyen.reader.presentation.LocalBottomBarInset
import vn.tramtruyen.reader.presentation.PlayerViewModel
import vn.tramtruyen.reader.presentation.UiState
import vn.tramtruyen.reader.presentation.components.CoverArt
import vn.tramtruyen.reader.presentation.components.Radii
import vn.tramtruyen.reader.presentation.components.noGrayCombinedClick
import vn.tramtruyen.reader.presentation.motion.rememberHaptics

@Composable
fun LibraryScreen(vm: PlayerViewModel, ui: UiState) {
    val cs = MaterialTheme.colorScheme
    val epubPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importEpub)
    }
    val stories = remember(ui.libraryItems, ui.libFilter, ui.historyItems) { vm.libraryStories() }
    val readingCount = remember(ui.historyItems) { ui.historyItems.count { it.progress > 0f } }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Thư viện",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${ui.libraryItems.size} truyện · $readingCount đang nghe",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                }
                FilledTonalButton(
                    onClick = {
                        epubPicker.launch(arrayOf("application/epub+zip", "application/zip"))
                    },
                    shape = RoundedCornerShape(15.dp),
                    contentPadding = PaddingValues(horizontal = 13.dp),
                    modifier = Modifier.height(44.dp),
                ) {
                    Icon(Icons.Outlined.UploadFile, null, Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Nhập EPUB", maxLines = 1)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LibraryFilter("Tất cả", ui.libraryItems.size, ui.libFilter == "all") {
                    vm.setLibFilter("all")
                }
                LibraryFilter("Đang nghe", readingCount, ui.libFilter == "reading") {
                    vm.setLibFilter("reading")
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (stories.isEmpty()) {
                EmptyLibrary(Modifier.fillMaxSize(), vm::openPaste) {
                    epubPicker.launch(arrayOf("application/epub+zip", "application/zip"))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 96.dp + LocalBottomBarInset.current,
                    ),
                ) {
                    items(stories, key = Story::id, contentType = { "book" }) { story ->
                        BookCard(
                            story = story,
                            onClick = { vm.openPlayer(story) },
                            onDelete = { vm.askDelete(story) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = vm::openPaste,
                    icon = { Icon(Icons.Outlined.ContentPaste, null) },
                    text = { Text("Dán link") },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp + LocalBottomBarInset.current),
                )
            }
        }
    }
}

@Composable
private fun LibraryFilter(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("$label  $count") },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, null, Modifier.size(17.dp)) }
        } else {
            null
        },
    )
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier, onPaste: () -> Unit, onEpub: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier.fillMaxWidth().padding(
            start = 24.dp,
            end = 24.dp,
            top = 8.dp,
            bottom = 8.dp + LocalBottomBarInset.current,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = cs.surfaceVariant.copy(alpha = 0.42f),
            border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.55f)),
        ) {
            Column(
                Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(76.dp).clip(RoundedCornerShape(24.dp))
                        .background(cs.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.AutoStories,
                        null,
                        Modifier.size(34.dp),
                        tint = cs.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "Bắt đầu thư viện của bạn",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Dán liên kết truyện online hoặc nhập sách EPUB từ thiết bị.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onPaste, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.ContentPaste, null, Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Dán liên kết")
                }
                OutlinedButton(onClick = onEpub, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.UploadFile, null, Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Nhập EPUB")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCard(
    story: Story,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    val shape = RoundedCornerShape(Radii.lg)
    var menuExpanded by remember(story.id) { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .noGrayCombinedClick(
                onClick = onClick,
                onLongClick = {
                    haptics.tick()
                    menuExpanded = true
                },
            ),
        shape = shape,
        color = cs.surface,
    ) {
        Column {
            Box {
                CoverArt(
                    story.color,
                    story.coverUrl,
                    Modifier.fillMaxWidth().aspectRatio(0.70f),
                    corner = Radii.lg,
                    sizeHintPx = 420,
                    fallbackKey = story.id,
                    shape = RoundedCornerShape(Radii.lg),
                )
                if (story.sourceType == StorySourceType.EPUB) {
                    Surface(
                        Modifier.align(Alignment.TopStart).padding(9.dp),
                        shape = RoundedCornerShape(9.dp),
                        color = cs.surface.copy(alpha = 0.92f),
                        contentColor = cs.onSurface,
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.MenuBook, null, Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("EPUB", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Box(Modifier.align(Alignment.TopEnd).padding(7.dp)) {
                    FilledTonalIconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Icon(Icons.Outlined.MoreVert, "Tuỳ chọn truyện", Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Xoá khỏi thư viện", color = cs.error) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Delete, null, tint = cs.error)
                            },
                            onClick = {
                                haptics.tick()
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
            Column(Modifier.padding(top = 10.dp, start = 2.dp, end = 2.dp, bottom = 8.dp)) {
                Text(
                    story.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}