package vn.tramtruyen.reader.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subject
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import vn.tramtruyen.reader.presentation.PlayerViewModel
import vn.tramtruyen.reader.presentation.UiState
import vn.tramtruyen.reader.presentation.collectAsStateSafe
import vn.tramtruyen.reader.presentation.components.CoverArt
import vn.tramtruyen.reader.presentation.components.Radii
import vn.tramtruyen.reader.presentation.components.coverBrush
import vn.tramtruyen.reader.presentation.components.noGrayClick
import vn.tramtruyen.reader.presentation.theme.hex
import vn.tramtruyen.reader.presentation.theme.rememberCoverSeedColor
import vn.tramtruyen.reader.presentation.theme.tuckedCornerShape
import vn.tramtruyen.reader.presentation.motion.Motion
import vn.tramtruyen.reader.presentation.motion.UiPerformancePolicy
import vn.tramtruyen.reader.presentation.motion.renderScale
import kotlinx.coroutines.delay

private fun fmtMs(ms: Long): String {
    val sec = (ms / 1000).toInt().coerceAtLeast(0)
    return "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"
}

@Composable
fun PlayerScreen(vm: PlayerViewModel, ui: UiState) {
    val story = ui.activeStory ?: return
    val cs = MaterialTheme.colorScheme

    // MAU NEN LAY TU ANH BIA.
    //
    // Palette trich tone ruc ro nhat cua bia (xem CoverColor.kt); trong luc anh
    // chua tai xong thi dung mau san co cua truyen nen khong bao gio thay mot
    // khung hinh khong mau. Doi truyen -> mau CHUYEN DAN chu khong nhay cai,
    // nen cam giac la khong gian doi sac chu khong phai man hinh bi thay.
    val tintTarget = rememberCoverSeedColor(
        imageUrl = story.coverUrl.ifBlank { null },
        fallback = hex(story.color),
    )
    val tint by animateColorAsState(
        targetValue = tintTarget,
        animationSpec = tween(Motion.ColorChangeMs, easing = Motion.EnterEasing),
        label = "playerAmbientTint",
    )
    val ambientBrush = remember(tint) {
        Brush.verticalGradient(
            colors = listOf(
                tint.copy(alpha = 0.34f),
                tint.copy(alpha = 0.12f),
                Color.Transparent,
            ),
        )
    }

    // Cu chi vuot xuong de thu ve mini-player do PlayerMorphHost dam nhan: no dieu
    // khien ca vi tri, kich thuoc va bo goc trong suot qua trinh vuot. O day chi
    // con phan noi dung.
    Surface(Modifier.fillMaxSize(), color = cs.surface) {
        Box(Modifier.fillMaxSize()) {
            if (ui.playerBlur > 0 && story.coverUrl.isNotBlank()) {
                // Không dùng RenderEffect full-screen: layer blur cỡ màn hình phải
                // được GPU ghép lại trong mọi frame morph/scroll. Decode ảnh xuống
                // 96 px rồi phóng bilinear cho nền khuếch tán tương tự với một
                // texture tĩnh đã cache.
                val backgroundAlpha = (0.08f + ui.playerBlur * 0.0035f).coerceIn(0.08f, 0.36f)
                CoverArt(story.color, story.coverUrl,
                    Modifier.fillMaxSize().graphicsLayer {
                        alpha = backgroundAlpha
                        scaleX = 1.12f
                        scaleY = 1.12f
                    },
                    0.dp, label = null, sizeHintPx = 96, fallbackKey = story.id,
                    shape = RoundedCornerShape(0.dp))
            }
            // Quang sang mau bia do tu tren xuong, nhat dan roi tan han vao nen.
            // Dat DUOI noi dung nen khong anh huong gi toi chu va nut.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(ambientBrush)
            )
            Crossfade(
                targetState = ui.isKaraoke,
                animationSpec = tween(Motion.ContentChangeMs, easing = Motion.EnterEasing),
                label = "playerModeTransition",
            ) { karaoke ->
                if (karaoke) KaraokeView(vm, ui) else PlayerView(vm, ui)
            }
        }
    }
}

@Composable
private fun PlayerView(vm: PlayerViewModel, ui: UiState) {
    val st = ui.activeStory!!
    val cs = MaterialTheme.colorScheme
    val chapterName = ui.chapterTitle.ifBlank { "Chương ${ui.chapterNumber}" }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 22.dp)
            .padding(bottom = 18.dp),
    ) {
        // Centered now-playing header, with a discreet way back to the app.
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.closePlayer() }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, "Đóng")
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Đang phát", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Text(
                    st.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(44.dp))
        }

        Box(
            Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            CoverArt(
                st.color,
                st.coverUrl,
                Modifier.fillMaxWidth(0.9f).aspectRatio(1f),
                Radii.lg,
                label = st.title,
                sizeHintPx = 900,
                fallbackKey = st.id,
                shape = RoundedCornerShape(22.dp),
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    chapterName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val supporting = if (st.author.isNotBlank()) st.author else st.title
                Text(
                    supporting,
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Voice picker: icon only (rounded-square block), no label.
            FilledTonalIconButton(
                onClick = { vm.openSheet("voice") },
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.Mic, "Chọn giọng đọc", Modifier.size(20.dp))
            }
            Spacer(Modifier.width(6.dp))
            FilledTonalIconButton(
                onClick = { vm.openSheet("reading") },
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.MoreVert, "Thêm")
            }
        }

        // Tach dong ho nong thanh leaf rieng: tick vi tri khong recompose anh bia,
        // tieu de, nut dieu khien va toan bo bo cuc Player.
        PlayerProgress(vm, animateWave = ui.isPlaying)
        ui.chapterLoadError?.let { message ->
            Text(
                message,
                color = cs.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = { vm.replay() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Filled.Replay, "Nghe lại")
            }
            FilledTonalIconButton(
                onClick = { vm.prevChapter() },
                enabled = ui.playback.hasPrevious,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.FastRewind, "Chương trước", Modifier.size(20.dp))
            }
            // Play/pause: solid circular button, exactly like the reference image.
            FloatingActionButton(
                onClick = { vm.togglePlay() },
                containerColor = cs.primary,
                contentColor = cs.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier.size(84.dp),
            ) {
                if (ui.isBuffering) {
                    CircularProgressIndicator(Modifier.size(32.dp), color = cs.onPrimary, strokeWidth = 3.dp)
                } else {
                    // Doi icon play/pause bang crossfade ngan cho do giat mat.
                    Crossfade(
                        targetState = ui.isPlaying,
                        animationSpec = tween(Motion.StateChangeMs, easing = Motion.EnterEasing),
                        label = "mainPlayPause",
                    ) { playing ->
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            when {
                                ui.chapterLoadError != null -> "Thử tải lại chương"
                                playing -> "Tạm dừng"
                                else -> "Phát"
                            },
                            Modifier.size(34.dp),
                        )
                    }
                }
            }
            FilledTonalIconButton(
                onClick = { vm.nextChapter() },
                enabled = ui.playback.hasNext,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.FastForward, "Chương sau", Modifier.size(20.dp))
            }
            FilledTonalIconButton(
                onClick = { vm.toggleLoop() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (ui.loop) cs.primaryContainer else cs.secondaryContainer,
                    contentColor = if (ui.loop) cs.onPrimaryContainer else cs.onSecondaryContainer,
                ),
            ) {
                Icon(Icons.Filled.Repeat, "Lặp")
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerActionPill(
                icon = Icons.AutoMirrored.Filled.List,
                label = "Danh sách",
                modifier = Modifier.weight(1f),
            ) { vm.openSheet("chapters") }
            FilledTonalIconButton(
                onClick = { vm.openSheet("speed") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(54.dp),
            ) {
                Icon(Icons.Outlined.Speed, "Tốc độ đọc")
            }
            PlayerActionPill(
                icon = Icons.Outlined.Subject,
                label = "Lời truyện",
                modifier = Modifier.weight(1f),
            ) { vm.toggleKaraoke() }
        }
    }
}

@Composable
private fun PlayerProgress(vm: PlayerViewModel, animateWave: Boolean) {
    val cs = MaterialTheme.colorScheme
    val liveProgress by vm.progressUi.collectAsStateSafe()
    // Keo thanh tua: chi cap nhat gia tri keo o local, commit seekTo MOT lan
    // khi nguoi dung ngung keo (debounce). Goi seekTo theo tung tick keo se
    // reset engine + tong hop lai hang chuc lan cho mot thao tac keo.
    var dragValue by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(dragValue) {
        val v = dragValue ?: return@LaunchedEffect
        delay(280)
        vm.seekTo(v)
        if (dragValue == v) dragValue = null
    }
    vn.tramtruyen.reader.presentation.components.WaveSeekBar(
        value = (dragValue ?: liveProgress.progress).coerceIn(0f, 1f),
        onValueChange = { dragValue = it },
        activeColor = cs.primary,
        inactiveColor = cs.surfaceVariant,
        thumbColor = cs.primary,
        // Pha sóng chỉ invalid draw phase và dừng hẳn khi tạm dừng.
        animate = animateWave,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(fmtMs(liveProgress.positionMs), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
        Text(fmtMs(liveProgress.durationMs.coerceAtLeast(1L)), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun PlayerActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).noGrayClick(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = cs.secondaryContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(21.dp), tint = cs.onSecondaryContainer)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = cs.onSecondaryContainer, maxLines = 1)
        }
    }
}

@Composable
private fun KaraokeView(vm: PlayerViewModel, ui: UiState) {
    val st = ui.activeStory!!
    val cs = MaterialTheme.colorScheme
    val lines = ui.sentences.ifEmpty {
        when {
            ui.isBuffering -> listOf("Đang tải lời truyện…")
            else -> listOf(ui.chapterLoadError ?: "Không có nội dung chương")
        }
    }
    val listState = rememberLazyListState()
    var chromeVisible by remember { mutableStateOf(true) }
    // Read this state inside graphicsLayer below: alpha changes invalidate only
    // the layer, not the karaoke layout tree.
    val chromeAlpha = animateFloatAsState(
        targetValue = if (chromeVisible) 1f else 0f,
        animationSpec = tween(Motion.StateChangeMs, easing = Motion.EnterEasing),
        label = "karaokeChrome",
    )

    // ArchiveTune-style focus anchor: giữ câu đang đọc hơi cao hơn tâm. Mỗi
    // lần đổi câu tiếp tục cuộn từ vị trí hiện tại, không nhảy/cắt animation.
    LaunchedEffect(ui.activeSentenceIndex, ui.isPlaying) {
        if (!ui.isPlaying) return@LaunchedEffect
        val target = ui.activeSentenceIndex
        if (target !in lines.indices) return@LaunchedEffect
        if (listState.isScrollInProgress) return@LaunchedEffect
        val viewport = listState.layoutInfo.viewportSize.height
        val anchor = if (viewport > 0) -(viewport * 0.34f).toInt() else -180
        UiPerformancePolicy.karaokePreJumpIndex(
            target = target,
            firstVisible = listState.firstVisibleItemIndex,
        )?.let { preJump ->
            // Không animate xuyên hàng trăm câu vì LazyColumn sẽ dựng nhiều item
            // trung gian. Nhảy tới vùng lân cận rồi animate đoạn cuối ngắn.
            listState.scrollToItem(preJump)
        }
        listState.animateScrollToItem(target, anchor)
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp).padding(bottom = 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)
                .graphicsLayer { alpha = chromeAlpha.value },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = vm::toggleKaraoke,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
            ) { Icon(Icons.Filled.KeyboardArrowDown, "Đóng lời truyện") }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    st.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    ui.chapterTitle.ifBlank { "Chương ${ui.chapterNumber}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(top = 138.dp, bottom = 190.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(lines, key = { i, _ -> i }, contentType = { _, _ -> "archive_lyric" }) { i, line ->
                val active = i == ui.activeSentenceIndex
                val lineColor by animateColorAsState(
                    targetValue = if (active) cs.onSurface else cs.onSurface.copy(alpha = 0.34f),
                    animationSpec = tween(Motion.StateChangeMs, easing = Motion.EnterEasing),
                    label = "karaokeLineColor",
                )
                val lineScale = animateFloatAsState(
                    targetValue = if (active) 1f else 0.96f,
                    animationSpec = Motion.SelectionSpring,
                    label = "karaokeLineScale",
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        // Phục hồi scale animation nhưng đọc state ở draw phase:
                        // không graphicsLayer và không remeasure LazyColumn.
                        .renderScale(pivotFractionX = 0f) { lineScale.value }
                        .clip(RoundedCornerShape(18.dp))
                        .noGrayClick {
                            if (active) chromeVisible = !chromeVisible
                            else vm.seekToSentence(i)
                        }
                        // Giữ kích thước hàng ổn định: không remeasure danh sách
                        // mỗi lần TTS chuyển câu; chỉ scale/màu ở tầng render.
                        .padding(vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(if (active) cs.primary else Color.Transparent),
                    )
                    Spacer(Modifier.width(13.dp))
                    Text(
                        line,
                        modifier = Modifier.weight(1f),
                        fontSize = ui.karaokeFontSize.sp,
                        lineHeight = (ui.karaokeFontSize * 1.34f).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = lineColor,
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = chromeAlpha.value },
            shape = RoundedCornerShape(28.dp),
            color = cs.surfaceColorAtElevation(4.dp),
            tonalElevation = 4.dp,
            shadowElevation = 0.dp,
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(60.dp),contentAlignment=Alignment.Center) {
                    KaraokeProgressRing(vm,cs.surfaceVariant,cs.primary)
                    CoverArt(st.color,st.coverUrl,Modifier.size(50.dp),Radii.xs,sizeHintPx=160,fallbackKey=st.id,shape=CircleShape)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(st.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text(ui.chapterTitle.ifBlank{"Chương ${ui.chapterNumber}"},style=MaterialTheme.typography.bodySmall,color=cs.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                IconButton(onClick=vm::prevChapter,enabled=ui.playback.hasPrevious,modifier=Modifier.size(42.dp)){Icon(Icons.Filled.FastRewind,"Chương trước",Modifier.size(27.dp))}
                FilledIconButton(onClick=vm::togglePlay,modifier=Modifier.size(54.dp)) {
                    if(ui.isBuffering) CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.dp,color=cs.onPrimary)
                    else Crossfade(
                        targetState = ui.isPlaying,
                        animationSpec = tween(Motion.StateChangeMs, easing = Motion.EnterEasing),
                        label = "karaokePlayPause",
                    ) { playing ->
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            when {
                                ui.chapterLoadError != null -> "Thử tải lại chương"
                                playing -> "Tạm dừng"
                                else -> "Phát"
                            },
                            Modifier.size(28.dp),
                        )
                    }
                }
                IconButton(onClick=vm::nextChapter,enabled=ui.playback.hasNext,modifier=Modifier.size(42.dp)){Icon(Icons.Filled.FastForward,"Chương sau",Modifier.size(27.dp))}
            }
        }
    }
}

@Composable
private fun KaraokeProgressRing(vm:PlayerViewModel,trackColor:Color,progressColor:Color){
    val progress by vm.progressUi.collectAsStateSafe()
    Canvas(Modifier.fillMaxSize()){
        val ring=4.dp.toPx()
        drawArc(trackColor,-90f,360f,false,style=Stroke(ring,cap=StrokeCap.Round))
        val value=progress.progress.coerceIn(0f,1f)
        if(value>0f)drawArc(progressColor,-90f,360f*value,false,style=Stroke(ring,cap=StrokeCap.Round))
    }
}
