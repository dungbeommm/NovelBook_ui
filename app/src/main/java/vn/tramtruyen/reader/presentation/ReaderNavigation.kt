@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)
package vn.tramtruyen.reader.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import vn.tramtruyen.reader.model.*
import vn.tramtruyen.reader.presentation.components.CoverArt
import vn.tramtruyen.reader.presentation.components.Radii
import vn.tramtruyen.reader.presentation.components.noGrayClick
import vn.tramtruyen.reader.presentation.screens.*
import vn.tramtruyen.reader.presentation.motion.Motion
import vn.tramtruyen.reader.presentation.motion.UiPerformancePolicy
import vn.tramtruyen.reader.presentation.motion.miniPlayerGestures
import vn.tramtruyen.reader.presentation.motion.pressScale
import vn.tramtruyen.reader.presentation.motion.rememberMiniGestureState
import vn.tramtruyen.reader.presentation.motion.PlayerMorphHost
import vn.tramtruyen.reader.presentation.motion.PlayerMorphState
import vn.tramtruyen.reader.presentation.motion.WaterDropTabBar
import vn.tramtruyen.reader.presentation.motion.rememberHaptics
import vn.tramtruyen.reader.presentation.motion.HapticSlider
import vn.tramtruyen.reader.presentation.motion.globalHapticFeedback
import vn.tramtruyen.reader.presentation.motion.rememberPlayerMorphState
import vn.tramtruyen.reader.presentation.motion.rampF
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import vn.tramtruyen.reader.engine.UrlPolicy

/** Thu tu tab -- hang so o cap file, khong cap phat lai moi lan recompose. */
private val MAIN_TABS = listOf(Tab.HOME, Tab.LIBRARY, Tab.SEARCH)

/** Bottom space each page reserves so the floating dock never covers content. */
val LocalBottomBarInset = staticCompositionLocalOf { 0.dp }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderNavigation(vm: PlayerViewModel) {
    // The root observes content/navigation only. Live playback time is collected
    // inside MiniPlayer/PlayerView, preventing every tick from invalidating Scaffold.
    val ui by vm.contentUi.collectAsStateSafe()
    val contentUi = ui
    val appHaptics = rememberHaptics()
    // Navigation-only state must not invalidate all three precomposed screen
    // trees at the end of every pager animation. Screens do not render these
    // fields; Search prewarms its sources from LaunchedEffect(Unit).
    val pageUi = remember(contentUi) {
        contentUi.copy(currentTab = Tab.HOME, tabHistory = emptyList())
    }
    val cs = MaterialTheme.colorScheme
    val active = ui.activeStory
    // Morph: mini-player va player la MOT khoi duy nhat. Vi the o mini van ton tai
    // (chi mo dan di) khi player bung ra, de morph luon co diem xuat phat/ha canh.
    val morph = rememberPlayerMorphState(ui.isPlayerOpen)
    val hasMini = active != null && active.id != placeholderStory.id && ui.hiddenMiniPlayerStoryId != active.id
    // Reserve empty space at the end of each page so the floating dock never
    // covers content; grow the reserve when the mini-player is also showing.
    // Ti le tu dieu chinh theo kich thuoc man hinh (responsive tu dong): tab bar +
    // mini-player cao/to theo may, can bang giua chieu rong & chieu cao, kep an toan
    // trong [0.9 .. 1.30] de khong bao gio qua nho hay chiem het man hinh.
    val cfg = LocalConfiguration.current
    val uiScale = (((cfg.screenWidthDp / 411f) + (cfg.screenHeightDp / 891f)) / 2f)
        .coerceIn(0.9f, 1.30f)
    val bottomBarInset = (if (hasMini) 188.dp else 100.dp) * uiScale
    val settingsOpen = contentUi.currentTab == Tab.SETTINGS
    // Settings hides only the tabs; keep playback controls and reserve their space.
    val settingsBottomInset = (if (hasMini) 88.dp else 0.dp) * uiScale
    val tabStateHolder = rememberSaveableStateHolder()
    val firstMainPage = remember {
        MAIN_TABS.indexOf(ui.currentTab).takeIf { it >= 0 } ?: 0
    }
    val tabPager = rememberPagerState(
        initialPage = firstMainPage,
        pageCount = { MAIN_TABS.size },
    )
    val tabNavigationScope = rememberCoroutineScope()
    var tabNavigationJob by remember { mutableStateOf<Job?>(null) }
    val navigateToMainTab: (Tab) -> Unit = { tab ->
        val page = MAIN_TABS.indexOf(tab)
        if (page >= 0 && !settingsOpen) {
            tabNavigationJob?.cancel()
            tabNavigationJob = tabNavigationScope.launch {
                // Commit the heavy screen state only after the pager lands. This
                // prevents the destination list from recomposing underneath the
                // outgoing page during the transition. Repeated taps cancel and
                // retarget the same animation instead of stacking coroutines.
                if (tabPager.settledPage != page || tabPager.isScrollInProgress) {
                    tabPager.animateScrollToPage(
                        page = page,
                        animationSpec = Motion.TabPageSpring,
                    )
                }
                vm.setTab(tab)
            }
        }
    }
    // Đọc trực tiếp ở graphicsLayer của indicator: pager và giọt nước dùng cùng
    // một tiến trình, không còn hai animation đuổi theo nhau ở 120 Hz.
    val tabIndicatorPosition = remember(tabPager) {
        {
            UiPerformancePolicy.continuousTabPosition(
                tabPager.currentPage,
                tabPager.currentPageOffsetFraction,
                MAIN_TABS.size,
            )
        }
    }

    LaunchedEffect(contentUi.currentTab) {
        val target = MAIN_TABS.indexOf(contentUi.currentTab)
        if (target >= 0 && target != tabPager.settledPage) {
            tabPager.animateScrollToPage(
                page = target,
                animationSpec = Motion.TabPageSpring,
            )
        }
    }
    LaunchedEffect(tabPager) {
        snapshotFlow { tabPager.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val target = MAIN_TABS.getOrNull(page) ?: return@collect
                if (vm.contentUi.value.currentTab != target &&
                    vm.contentUi.value.currentTab != Tab.SETTINGS
                ) {
                    vm.setTab(target)
                }
            }
    }

    BackHandler(enabled = ui.isPlayerOpen || ui.pasteOpen || ui.sheet != null || ui.currentTab != Tab.HOME) {
        appHaptics.back()
        vm.goBack()
    }

    Scaffold(
        modifier = Modifier.globalHapticFeedback(appHaptics),
        containerColor = cs.surface,
        snackbarHost = {
            ui.errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    action = { TextButton(onClick = { vm.clearError() }) { Text("Đóng") } },
                ) {
                    Text(message, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            CompositionLocalProvider(LocalBottomBarInset provides bottomBarInset) {
                Box(Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = tabPager,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = UiPerformancePolicy.PAGER_PRECOMPOSE_PAGES,
                        // Library/Search vẫn swipe tab đầy đủ. Ở Home, Pager nhường
                        // gesture ngang cho shelf truyện để hai scroller không tranh nhau.
                        userScrollEnabled = UiPerformancePolicy.pagerTouchSwipeEnabled(
                            hasNestedHorizontalContent = contentUi.currentTab == Tab.HOME,
                            overlayVisible = contentUi.currentTab == Tab.SETTINGS ||
                                contentUi.isPlayerOpen || contentUi.sheet != null ||
                                contentUi.pasteOpen,
                        ),
                        key = { page -> MAIN_TABS[page].name },
                    ) { page ->
                        val targetTab = MAIN_TABS[page]
                        tabStateHolder.SaveableStateProvider(targetTab.name) {
                            when (targetTab) {
                                Tab.HOME -> HomeScreen(vm, pageUi)
                                Tab.LIBRARY -> LibraryScreen(vm, pageUi)
                                Tab.SEARCH -> SearchScreen(vm, pageUi)
                                Tab.SETTINGS -> Unit
                            }
                        }
                    }

                    // Settings không phải tab đáy: phủ lên pager thay vì bắt pager
                    // chạy xuyên ba trang. Trang chính bên dưới giữ nguyên layout/list.
                    AnimatedVisibility(
                        visible = settingsOpen,
                        enter = slideInHorizontally(
                            animationSpec = tween(Motion.NavEnterMs, easing = Motion.EnterEasing),
                        ) { it / 10 } + fadeIn(
                            animationSpec = tween(Motion.NavFadeInMs, easing = Motion.EnterEasing),
                        ),
                        exit = slideOutHorizontally(
                            animationSpec = tween(Motion.NavExitMs, easing = Motion.ExitEasing),
                        ) { it / 10 } + fadeOut(
                            animationSpec = tween(Motion.NavFadeOutMs, easing = Motion.ExitEasing),
                        ),
                    ) {
                        Surface(Modifier.fillMaxSize(), color = cs.surface) {
                            CompositionLocalProvider(LocalBottomBarInset provides settingsBottomInset) {
                                tabStateHolder.SaveableStateProvider(Tab.SETTINGS.name) {
                                    SettingsScreen(vm, contentUi)
                                }
                            }
                        }
                    }
                }
            }

            // Giữ dock trong composition suốt cú morph. Giá trị progress chỉ được
            // đọc ở graphicsLayer nên mỗi frame không còn recompose cả app shell;
            // player nằm trên cùng sẽ chặn tương tác khi dock đã mờ hoàn toàn.
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        // Dock chim xuong va mo di trong luc player bung ra.
                        val f = morph.progress
                        alpha = (1f - f * 1.6f).coerceIn(0f, 1f)
                        translationY = size.height * 0.25f * f
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Outer corners (mini-player top, dock bottom) stay large and
                // pill-like. When the mini-player is present, the corners where the
                // two surfaces meet tighten so the stack reads as ONE linked unit
                // instead of two separate floating cards. With no mini-player the
                // dock keeps its original fully-rounded shape.
                // Khi CA HAI cung hien: goc noi giua hai the thu nho lai va goc phia
                // ngoai no ra, de ca cum doc thanh MOT khoi lien mach thay vi hai
                // the roi rac. Bo goc doi muot khi mini-player xuat hien/bien mat.
                val bigCorner = if (hasMini) 34.dp else 30.dp
                // Chan an toan: bo goc am se lam RoundedCornerShape nem exception.
                val linkCorner by animateDpAsState(
                    targetValue = if (hasMini && !settingsOpen) 16.dp else bigCorner,
                    animationSpec = tween(Motion.NavEnterMs, easing = Motion.EnterEasing),
                    label = "dockLinkCorner",
                )
                if (hasMini) {
                    MiniPlayer(
                        vm,
                        ui,
                        morph,
                        scale = uiScale,
                        shape = RoundedCornerShape(
                            topStart = bigCorner,
                            topEnd = bigCorner,
                            bottomStart = linkCorner,
                            bottomEnd = linkCorner,
                        ),
                    )
                }
                AnimatedVisibility(
                    visible = !settingsOpen,
                    enter = slideInVertically(
                        animationSpec = tween(Motion.NavEnterMs, easing = Motion.EnterEasing),
                    ) { it } + fadeIn(
                        animationSpec = tween(Motion.NavFadeInMs, easing = Motion.EnterEasing),
                    ) + expandVertically(
                        animationSpec = tween(Motion.NavEnterMs, easing = Motion.EnterEasing),
                        expandFrom = Alignment.Bottom,
                    ),
                    exit = slideOutVertically(
                        animationSpec = tween(Motion.NavExitMs, easing = Motion.ExitEasing),
                    ) { it } + fadeOut(
                        animationSpec = tween(Motion.NavFadeOutMs, easing = Motion.ExitEasing),
                    ) + shrinkVertically(
                        animationSpec = tween(Motion.NavExitMs, easing = Motion.ExitEasing),
                        shrinkTowards = Alignment.Bottom,
                    ),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (hasMini) Spacer(Modifier.height(5.dp))
                        val dockShape = if (hasMini) {
                            RoundedCornerShape(
                                topStart = linkCorner,
                                topEnd = linkCorner,
                                bottomStart = bigCorner,
                                bottomEnd = bigCorner,
                            )
                        } else {
                            RoundedCornerShape(bigCorner)
                        }
                        Surface(
                            // 85% width: 15% shorter horizontally, centered as a floating dock.
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(top = if (hasMini) 0.dp else 7.dp, bottom = 7.dp),
                            shape = dockShape,
                            color = cs.surfaceColorAtElevation(3.dp),
                            // Khong do bong: chi dung tonalElevation (sac do dam hon) de
                            // tach dock khoi nen, vien phang hoan toan.
                            tonalElevation = 3.dp,
                            shadowElevation = 0.dp,
                        ) {
                            WaterDropTabBar(
                                itemCount = 3,
                                selectedIndex = when (ui.currentTab) {
                                    Tab.HOME -> 0
                                    Tab.LIBRARY -> 1
                                    Tab.SEARCH -> 2
                                    else -> -1
                                },
                                positionProvider = tabIndicatorPosition,
                                blobColor = cs.primaryContainer,
                                blobWidth = 58.dp * uiScale,
                                blobHeight = 34.dp * uiScale,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 7.dp * uiScale),
                            ) {
                                PillNavItem("Trang chủ", Icons.Filled.Home, Icons.Outlined.Home, ui.currentTab == Tab.HOME, uiScale) { navigateToMainTab(Tab.HOME) }
                                PillNavItem("Thư viện", Icons.Filled.LibraryBooks, Icons.Outlined.LibraryBooks, ui.currentTab == Tab.LIBRARY, uiScale) { navigateToMainTab(Tab.LIBRARY) }
                                PillNavItem("Tìm kiếm", Icons.Filled.Search, Icons.Outlined.Search, ui.currentTab == Tab.SEARCH, uiScale) { navigateToMainTab(Tab.SEARCH) }
                            }
                        }
                    }
                }
            }
        }
    }

    // MORPH mini-player <-> player: mot khoi duy nhat lon dan tu o mini ra full
    // man hinh, va vuot xuong thi thu nho nguoc lai dung ve cho o mini.
    if (ui.activeStory != null) {
        PlayerMorphHost(
            state = morph,
            surfaceColor = cs.surface,
            // Khoi morph xuat phat dung mau + bo goc cua the mini, roi moi chuyen
            // dan sang dien mao player. Truoc day no ve thang bang mau surface phang
            // nen ngay giay dau tien cua cu vuot da thay mot mieng mau khac de len
            // the mini -- do chinh la cho "chua dong bo".
            collapsedColor = cs.surfaceColorAtElevation(4.dp),
            collapsedCorner = 34f,
            // The mini da khong con do bong nen khoi morph cung phai bat dau tu 0,
            // neu khong thi giay dau cu vuot se thay bong hien ra roi tat di.
            collapsedShadow = 0f,
            onCollapse = { vm.closePlayer() },
        ) {
            PlayerScreen(vm, ui)
        }
    }

    if (ui.sheet != null && ui.isPlayerOpen) PlayerSheet(vm, ui)
    if (ui.pasteOpen) PasteSheet(vm, ui)
    if(ui.epubImport.importing||ui.epubImport.result!=null||ui.epubImport.error!=null) EpubImportDialog(vm,ui)
    DeleteDialog(vm, ui.storyToDelete)
}

@Composable
private fun RowScope.PillNavItem(
    label: String,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    scale: Float = 1f,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    // Nen (giot nuoc) do WaterDropTabBar ve; o day chi con icon + nhan chu.
    val fg by animateColorAsState(
        if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
        animationSpec = tween(Motion.QuickFadeMs, easing = Motion.EnterEasing),
        label = "tabForeground",
    )
    val iconScale by animateFloatAsState(
        if (selected) 1.08f else 1f,
        animationSpec = Motion.TabIconSpring,
        label = "tabIconScale",
    )
    val iconBlend by animateFloatAsState(
        if (selected) 1f else 0f,
        animationSpec = tween(Motion.QuickFadeMs, easing = Motion.EnterEasing),
        label = "tabIconBlend",
    )
    // Keep the whole column tappable, but draw the press ripple ONLY inside the
    // pill (indication = null on the column) so no dark rectangle appears behind
    // the item.
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .weight(1f)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = {
                    // Rung NGAY luc cham, khong doi giot nuoc ha canh (~400ms sau).
                    // Rung phai di cung cu bam thi nao moi ghep hai cai lam mot; tre
                    // hon mot chut la lap tuc cam thay thanh hai su kien roi rac.
                    haptics.tick()
                    onClick()
                },
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(width = 58.dp * scale, height = 34.dp * scale)
                .clip(CircleShape)
                .indication(interaction, ripple()),
            contentAlignment = Alignment.Center,
        ) {
            // Both vector paths stay composed and only their render-layer alpha
            // changes. This avoids Crossfade creating/discarding subcompositions
            // exactly while the pager and water-drop indicator are moving.
            Icon(
                unselectedIcon,
                null,
                tint = fg,
                modifier = Modifier
                    .size(22.dp * scale)
                    .graphicsLayer {
                        alpha = 1f - iconBlend
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )
            Icon(
                selectedIcon,
                label,
                tint = fg,
                modifier = Modifier
                    .size(22.dp * scale)
                    .graphicsLayer {
                        alpha = iconBlend
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MiniPlayer(
    vm: PlayerViewModel,
    ui: UiState,
    morph: PlayerMorphState,
    scale: Float = 1f,
    shape: RoundedCornerShape = RoundedCornerShape(34.dp),
) {
    val st = ui.activeStory ?: return
    val cs = MaterialTheme.colorScheme
    val gesture = rememberMiniGestureState()
    // Bon cu chi, co khoa truc, va KHONG cu chi nao dung am thanh:
    //   len   -> mo player (morph)      xuong -> cat the mini di, van doc tiep
    //   trai  -> lui mot chuong         phai  -> sang chuong moi
    Surface(
        color = cs.surfaceColorAtElevation(4.dp),
        shape = shape,
        // Khong do bong -- xem chu thich o dock tabbar.
        tonalElevation = 4.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            // Match the nav dock width (85%) so the floating stack stays aligned.
            .fillMaxWidth(0.85f)
            .padding(top = 4.dp)
            // Bao cho morph biet o mini dang nam o dau -> do la diem xuat phat cua
            // hieu ung. Chi do khi con o trang thai mini de so lieu khong bi nhieu.
            .onGloballyPositioned {
                // Chi do khi the dang dung yen o vi tri mini -> diem xuat phat cua
                // morph khong bao gio bi nhieu boi chinh cu chi vuot.
                if (morph.progress < 0.02f &&
                    gesture.offsetX.value == 0f &&
                    gesture.offsetY.value == 0f
                ) {
                    morph.miniBounds = it.boundsInWindow()
                }
            }
            .graphicsLayer {
                val f = morph.progress
                val dx = gesture.offsetX.value
                val dy = gesture.offsetY.value
                // Vuot ngang la chuyen dong ngang THUAN, theo tay 1:1, dung kieu
                // ArchiveTune. Truoc day the con nghieng theo tay va mo dan di, nhung
                // ca hai thu do chi de phuc vu kieu "bay ra khoi canh roi truot vao tu
                // phia doi dien": lop mo dan la de GIAU buoc nhay (snapTo) giua hai
                // doan. Nay the chi bat ve giua, khong con buoc nhay nao phai giau, nen
                // giu the ro net va khong nghieng -- vuot bao nhieu thay dung bay nhieu.
                translationX = dx
                translationY = dy
                val dropFade = (1f - dy / (size.height * 2.2f)).coerceIn(0f, 1f)
                // The mini tat han ngay trong doan dau cua morph. Khoi morph luc do van
                // con dung bang o mini va da mang dung mau cua no, nen mat nguoi dung
                // chi thay MOT vat the duy nhat -- khong con hai lop chong nhau.
                alpha = (1f - rampF(f, 0.02f, 0.16f)) * dropFade
                val s = (1f - 0.04f * f.coerceIn(0f, 1f)) * (1f - 0.05f * (1f - dropFade))
                scaleX = s
                scaleY = s
            }
            .miniPlayerGestures(
                morph = morph,
                gesture = gesture,
                // Hai co nay phai di vao trong duoi dang THAM SO chu khong duoc
                // giau trong than lambda: khoi pointerInput chi chay mot lan nen
                // moi thu no doc truc tiep deu bi dong bang o lan compose dau tien.
                hasPrevChapter = ui.playback.hasPrevious,
                hasNextChapter = ui.playback.hasNext,
                onExpand = { vm.openPlayer(st) },
                onDismiss = { vm.hideMiniPlayer() },
                onPrevChapter = { vm.prevChapter() },
                onNextChapter = { vm.nextChapter() },
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .noGrayClick { vm.openPlayer(st) }
                .padding(horizontal = 10.dp, vertical = 9.dp * scale),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(60.dp * scale), contentAlignment = Alignment.Center) {
                MiniProgressRing(vm, cs.surfaceVariant, cs.primary)
                CoverArt(
                    st.color,
                    st.coverUrl,
                    Modifier.size(50.dp * scale),
                    Radii.xs,
                    sizeHintPx = 160,
                    fallbackKey = st.id,
                    shape = CircleShape,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(st.title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Nhãn chương đổi bằng cùng token chuyển nội dung của toàn app;
                // Compose tiếp tục từ trạng thái đang hiển thị nếu chương đổi nhanh.
                val chapterLabel = ui.chapterTitle.ifBlank {
                    "Chương ${ui.chapterNumber}" + if (st.totalChapters > 0) "/${st.totalChapters}" else ""
                }
                Crossfade(
                    targetState = chapterLabel,
                    animationSpec = tween(Motion.ContentChangeMs, easing = Motion.EnterEasing),
                    label = "miniChapterLabel",
                ) { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(
                onClick = { vm.prevChapter() },
                enabled = ui.playback.hasPrevious,
                modifier = Modifier.size(42.dp * scale),
            ) {
                Icon(Icons.Filled.FastRewind, "Chương trước", Modifier.size(28.dp * scale))
            }
            val playInteraction = remember { MutableInteractionSource() }
            FilledIconButton(
                onClick = { vm.togglePlay() },
                interactionSource = playInteraction,
                modifier = Modifier
                    .size(54.dp * scale)
                    .clip(CircleShape)
                    .pressScale(playInteraction),
            ) {
                if (ui.isBuffering) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = cs.onPrimary)
                } else {
                    Crossfade(
                        targetState = ui.isPlaying,
                        animationSpec = tween(Motion.StateChangeMs, easing = Motion.EnterEasing),
                        label = "miniPlayPause",
                    ) { playing ->
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            if (playing) "Tạm dừng" else "Phát",
                            Modifier.size(28.dp * scale),
                        )
                    }
                }
            }
            IconButton(
                onClick = { vm.nextChapter() },
                enabled = ui.playback.hasNext,
                modifier = Modifier.size(42.dp * scale),
            ) {
                Icon(Icons.Filled.FastForward, "Chương sau", Modifier.size(28.dp * scale))
            }
        }
    }
}

/**
 * Leaf nóng duy nhất của mini-player. Tick 10 Hz chỉ invalid Canvas 60dp này;
 * ảnh bìa, chữ, nút, gesture và Surface không còn recompose theo đồng hồ.
 */
@Composable
private fun MiniProgressRing(vm: PlayerViewModel, trackColor: Color, progressColor: Color) {
    val liveProgress by vm.progressUi.collectAsStateSafe()
    Canvas(Modifier.fillMaxSize()) {
        val ring = 4.dp.toPx()
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = ring, cap = StrokeCap.Round),
        )
        val progress = liveProgress.progress.coerceIn(0f, 1f)
        if (progress > 0f) {
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = ring, cap = StrokeCap.Round),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSheet(vm: PlayerViewModel, ui: UiState) {
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = { vm.closeSheet() },
        containerColor = cs.surfaceColorAtElevation(2.dp),
    ) {
        when (ui.sheet) {
            "chapters" -> ChapterListSheet(vm, ui)
            "speed" -> {
                Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState())) {
                    SheetTitle("Tốc độ đọc")
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f).forEach { sp ->
                        ListItem(
                            headlineContent = { Text("${sp}x") },
                            trailingContent = { if (sp == ui.speed) Icon(Icons.Filled.Check, null, tint = cs.primary) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { vm.setSpeed(sp) },
                        )
                    }
                }
            }
            "voice" -> {
                Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState())) {
                    SheetTitle("Giọng đọc")
                    if (ui.voiceGroups.isEmpty()) {
                        Text("Không tìm thấy giọng đang bật. Vào Cài đặt → Quản lý giọng đọc để bật hoặc cấu hình provider.",
                            style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                    ui.voiceGroups.forEach { g ->
                        Text(
                            g.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.primary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        )
                        g.voices.forEach { v ->
                            ListItem(
                                headlineContent = { Text(v.label) },
                                supportingContent = { Text(v.sub) },
                                trailingContent = {
                                    if (v.id == ui.selectedVoice) Icon(Icons.Filled.Check, null, tint = cs.primary)
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { vm.setVoice(v.id) },
                            )
                        }
                    }
                }
            }
            "reading" -> {
                var customMinutes by remember(ui.sheet) { mutableIntStateOf((ui.sleepTimer ?: 30).coerceIn(5, 180)) }
                Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Tune, null, tint = cs.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Điều khiển phát", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Âm thanh, lời truyện và hẹn giờ", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(18.dp))

                    PlayerMenuCard(Icons.Filled.VolumeUp, "Âm thanh & lời truyện") {
                        Text("Âm lượng ${(ui.playerVolume * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VolumeDown, null, Modifier.size(20.dp), tint = cs.onSurfaceVariant)
                            HapticSlider(value = ui.playerVolume, onValueChange = vm::setPlayerVolume, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.VolumeUp, null, Modifier.size(20.dp), tint = cs.onSurfaceVariant)
                        }
                        Text("Cỡ chữ lời truyện ${ui.karaokeFontSize}px", style = MaterialTheme.typography.labelLarge)
                        HapticSlider(value = ui.karaokeFontSize.toFloat(), onValueChange = { vm.setKaraokeFont(it.roundToInt()) }, valueRange = 16f..48f)
                    }
                    Spacer(Modifier.height(12.dp))

                    PlayerMenuCard(Icons.Filled.FastForward, "Thao tác nhanh") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickActionButton(Icons.Filled.SkipPrevious, "Câu trước") { vm.skipSentence(-1) }
                            QuickActionButton(Icons.Filled.Replay, "Nghe lại câu hiện tại") { vm.replay() }
                            QuickActionButton(Icons.Filled.SkipNext, "Câu sau") { vm.skipSentence(1) }
                        }
                        Spacer(Modifier.height(10.dp))
                        ListItem(headlineContent={Text("Tự động chuyển chương")}, supportingContent={Text("Phát tiếp khi hết chương")}, trailingContent={Switch(ui.autoNextChapter,{vm.toggleAutoNext()})}, colors=ListItemDefaults.colors(containerColor=Color.Transparent))
                        ListItem(headlineContent={Text("Lặp chương")}, supportingContent={Text("Phát lại từ đầu khi kết thúc")}, trailingContent={Switch(ui.loop,{vm.toggleLoop()})}, colors=ListItemDefaults.colors(containerColor=Color.Transparent))
                        ListItem(headlineContent={Text("Nhớ giọng theo truyện")}, trailingContent={Switch(ui.voiceProfileEnabled,{vm.toggleVoiceProfile()})}, colors=ListItemDefaults.colors(containerColor=Color.Transparent))
                    }
                    Spacer(Modifier.height(12.dp))

                    PlayerMenuCard(Icons.Filled.Bedtime, "Hẹn giờ ngủ tùy chỉnh") {
                        FlowRowSimple {
                            FilterChip(selected=ui.sleepMode==SleepMode.END_SENTENCE,onClick={vm.setSmartSleep(if(ui.sleepMode==SleepMode.END_SENTENCE)SleepMode.OFF else SleepMode.END_SENTENCE)},label={Text("Dừng cuối câu")})
                            FilterChip(selected=ui.sleepMode==SleepMode.END_CHAPTER,onClick={vm.setSmartSleep(if(ui.sleepMode==SleepMode.END_CHAPTER)SleepMode.OFF else SleepMode.END_CHAPTER)},label={Text("Dừng cuối chương")})
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            FilledTonalIconButton(onClick={customMinutes=(customMinutes-5).coerceAtLeast(5)}) { Icon(Icons.Filled.Remove,"Giảm") }
                            Column(Modifier.width(120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$customMinutes", style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold)
                                Text("phút", style=MaterialTheme.typography.labelMedium, color=cs.onSurfaceVariant)
                            }
                            FilledTonalIconButton(onClick={customMinutes=(customMinutes+5).coerceAtMost(180)}) { Icon(Icons.Filled.Add,"Tăng") }
                        }
                        HapticSlider(value=customMinutes.toFloat(),onValueChange={customMinutes=((it/5f).roundToInt()*5).coerceIn(5,180)},valueRange=5f..180f,steps=34)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick={vm.setSleepTimer(null)},modifier=Modifier.weight(1f)){Text("Tắt hẹn giờ")}
                            Button(onClick={vm.setSleepTimer(customMinutes)},modifier=Modifier.weight(1f)){Icon(Icons.Filled.Timer,null);Spacer(Modifier.width(6.dp));Text("Bắt đầu")}
                        }
                        ui.sleepTimer?.let { Text("Còn khoảng $it phút", style=MaterialTheme.typography.labelLarge, color=cs.primary, modifier=Modifier.padding(top=8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(icon, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlayerMenuCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(shape=RoundedCornerShape(24.dp),color=cs.surfaceContainerHigh) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(icon,null,tint=cs.primary)
                Spacer(Modifier.width(9.dp))
                Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ChapterListSheet(vm: PlayerViewModel, ui: UiState) {
    val cs = MaterialTheme.colorScheme
    val items = remember(ui.chapterItems, ui.chapterFilter, ui.tocTitles, ui.chapterNumber) {
        val base = vm.filteredChapterItems().ifEmpty {
            if (ui.tocTitles.isNotEmpty()) {
                ui.tocTitles.mapIndexed { i, t ->
                    ChapterUiModel(
                        index = i + 1,
                        title = t.ifBlank { chapTitle(i + 1) },
                        url = ui.tocUrls.getOrNull(i) ?: "",
                        chapterId = StoryIds.chapterId(ui.tocUrls.getOrNull(i), i + 1),
                        isCurrent = i + 1 == ui.chapterNumber,
                    )
                }
            } else {
                val start = (ui.chapterNumber - 3).coerceAtLeast(1)
                (start until start + 8).map { n ->
                    ChapterUiModel(
                        index = n,
                        title = chapTitle(n),
                        url = "",
                        chapterId = "ch:idx:$n",
                        isCurrent = n == ui.chapterNumber,
                    )
                }
            }
        }
        base
    }
    val listState = rememberLazyListState()
    val currentIdx = items.indexOfFirst { it.isCurrent }.coerceAtLeast(0)

    LaunchedEffect(currentIdx, ui.sheet) {
        if (ui.sheet == "chapters" && items.isNotEmpty()) {
            listState.scrollToItem(currentIdx.coerceIn(0, items.lastIndex))
        }
    }

    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            "Danh sách chương",
            style = MaterialTheme.typography.titleLarge,
            color = cs.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Row(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(ChapterFilter.ALL to "Tất cả",ChapterFilter.LISTENING to "Đang nghe",ChapterFilter.HEARD to "Đã nghe").forEach { (id, label) ->
                FilterChip(
                    selected = ui.chapterFilter == id,
                    onClick = { vm.setChapterFilter(id) },
                    label = { Text(label) },
                )
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(items, key = { it.chapterId + it.index }, contentType = { "chapter" }) { item ->
                val leading: @Composable () -> Unit = {
                    when {
                        item.isCurrent -> Icon(Icons.Filled.PlayArrow, null, tint = cs.primary)
                        item.status == ChapterListenStatus.COMPLETED ->
                            Icon(Icons.Filled.CheckCircle, null, tint = cs.primary)
                        item.status == ChapterListenStatus.IN_PROGRESS ->
                            Icon(Icons.Filled.Timelapse, null, tint = cs.tertiary)
                        else -> Icon(Icons.AutoMirrored.Filled.List, null, tint = cs.onSurfaceVariant)
                    }
                }
                val sub = buildString {
                    append(item.title.ifBlank { chapTitle(item.index) })
                    when {
                        item.isCurrent && ui.isPlaying -> append(" · Đang phát")
                        item.status == ChapterListenStatus.IN_PROGRESS && item.progress > 0f ->
                            append(" · ${(item.progress * 100).toInt()}%")
                        item.status == ChapterListenStatus.COMPLETED -> append(" · Đã nghe")
                    }
                }
                ListItem(
                    headlineContent = {
                        Text(
                            "Chương ${item.index}",
                            color = if (item.status == ChapterListenStatus.COMPLETED && !item.isCurrent)
                                cs.onSurfaceVariant else cs.onSurface,
                        )
                    },
                    supportingContent = {
                        Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    leadingContent = leading,
                    trailingContent = {
                        if (item.isCurrent || item.status == ChapterListenStatus.IN_PROGRESS) {
                            ChapterRowProgress(vm, item.isCurrent, item.progress)
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (item.isCurrent) cs.primaryContainer.copy(alpha = 0.35f)
                        else Color.Transparent
                    ),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { vm.setChapter(item.index) },
                )
            }
        }
    }
}

/** Chỉ đúng progress bar của chương hiện tại nhận tick; sheet/list không recompose. */
@Composable
private fun ChapterRowProgress(vm: PlayerViewModel, live: Boolean, savedProgress: Float) {
    if (live) {
        val progress by vm.progressUi.collectAsStateSafe()
        LinearProgressIndicator(
            progress = { progress.progress.coerceIn(0f, 1f) },
            modifier = Modifier.width(48.dp).height(4.dp),
        )
    } else {
        LinearProgressIndicator(
            progress = { savedProgress.coerceIn(0f, 1f) },
            modifier = Modifier.width(48.dp).height(4.dp),
        )
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowSimple(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasteSheet(vm: PlayerViewModel, ui: UiState) {
    val cs = MaterialTheme.colorScheme
    val p = ui.paste
    val clipboard = LocalClipboardManager.current
    val url = p.url.trim()
    val validUrl = UrlPolicy.isAllowed(url)
    val host = url.substringAfter("://", "").substringBefore('/').removePrefix("www.")

    fun pasteFromClipboard() {
        val clip = clipboard.getText()?.text?.trim().orEmpty()
        if (UrlPolicy.isAllowed(clip)) vm.setPasteUrl(clip)
        else vm.markClipboardEmpty()
    }

    ModalBottomSheet(
        onDismissRequest = vm::closePaste,
        containerColor = cs.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp, bottom = 2.dp).size(width = 34.dp, height = 4.dp)
                    .clip(CircleShape).background(cs.outlineVariant),
            )
        },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                .navigationBarsPadding().padding(bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(cs.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Link, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(23.dp)) }
                Spacer(Modifier.width(12.dp))
                Text(
                    when {
                        p.result != null -> "Truyện đã sẵn sàng"
                        p.analyzing -> "Đang mở liên kết"
                        else -> "Mở truyện từ liên kết"
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = vm::closePaste) { Icon(Icons.Filled.Close, "Đóng") }
            }
            Spacer(Modifier.height(18.dp))

            when {
                p.result != null -> {
                    val result = p.result
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = cs.surfaceContainer,
                        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.65f)),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CoverArt(
                                result.story.color, result.story.coverUrl,
                                Modifier.size(width = 82.dp, height = 112.dp), Radii.lg,
                                sizeHintPx = 260, fallbackKey = result.story.id,
                                shape = RoundedCornerShape(18.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = cs.primaryContainer,
                                    contentColor = cs.onPrimaryContainer,
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(14.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("Đã nhận diện", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                Spacer(Modifier.height(9.dp))
                                Text(
                                    result.story.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    result.story.author.ifBlank { result.source },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cs.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.closePaste(); vm.openPlayer(result.story) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(23.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Nghe ngay", style = MaterialTheme.typography.titleSmall)
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = vm::clearPasteResult) { Text("Liên kết khác") }
                        TextButton(onClick = { vm.saveToLibrary(result.story) }) {
                            Icon(Icons.Filled.BookmarkAdd, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Lưu thư viện")
                        }
                    }
                }

                p.analyzing -> {
                    Surface(shape = RoundedCornerShape(24.dp), color = cs.surfaceContainer) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                            Spacer(Modifier.height(15.dp))
                            Text("Đang tìm nội dung truyện", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (host.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(host, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(18.dp))
                            LinearProgressIndicator(
                                progress = { (p.step.toFloat() / PASTE_STEPS.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                trackColor = cs.surfaceVariant,
                            )
                        }
                    }
                }

                else -> {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = cs.surfaceContainer,
                        border = BorderStroke(
                            1.dp,
                            if (p.error != null || p.clipErr || (url.isNotBlank() && !validUrl))
                                cs.error.copy(alpha = 0.65f) else cs.outlineVariant.copy(alpha = 0.7f),
                        ),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            OutlinedTextField(
                                value = p.url,
                                onValueChange = vm::setPasteUrl,
                                placeholder = { Text("Dán liên kết truyện hoặc chương") },
                                leadingIcon = { Icon(Icons.Outlined.Link, null) },
                                trailingIcon = {
                                    if (p.url.isNotBlank()) {
                                        IconButton(onClick = { vm.setPasteUrl("") }) { Icon(Icons.Filled.Close, "Xóa") }
                                    } else {
                                        IconButton(onClick = ::pasteFromClipboard) { Icon(Icons.Outlined.ContentPaste, "Dán") }
                                    }
                                },
                                supportingText = {
                                    val message = when {
                                        p.error != null -> p.error
                                        p.clipErr -> "Bộ nhớ tạm không có liên kết hợp lệ"
                                        url.isNotBlank() && !validUrl -> "Chỉ hỗ trợ liên kết HTTPS hợp lệ"
                                        host.isNotBlank() -> host
                                        else -> "App tự nhận diện nguồn và chương đầu"
                                    }
                                    Text(message.orEmpty(), color = if (p.error != null || p.clipErr || (url.isNotBlank() && !validUrl)) cs.error else cs.onSurfaceVariant)
                                },
                                isError = p.error != null || p.clipErr || (url.isNotBlank() && !validUrl),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = { if (validUrl) vm.analyzeLink(url) }),
                            )
                            if (p.url.isBlank()) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(
                                    onClick = ::pasteFromClipboard,
                                    modifier = Modifier.align(Alignment.End),
                                ) {
                                    Icon(Icons.Outlined.ContentPaste, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Dán từ bộ nhớ")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { vm.analyzeLink(url) },
                        enabled = validUrl,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mở truyện", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun EpubImportDialog(vm: PlayerViewModel, ui: UiState) {
    val e = ui.epubImport
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = { if (!e.importing) vm.clearEpubImport() },
        containerColor = cs.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp, bottom = 2.dp)
                    .size(width = 34.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(cs.outlineVariant),
            )
        },
    ) {
        Column(
            Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (e.error != null) cs.errorContainer else cs.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (e.importing) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    } else {
                        Icon(
                            if (e.error != null) Icons.Filled.ErrorOutline else Icons.Filled.MenuBook,
                            null,
                            tint = if (e.error != null) cs.onErrorContainer else cs.onPrimaryContainer,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            e.importing -> "Đang nhập EPUB"
                            e.error != null -> "Không thể nhập EPUB"
                            else -> "Sách đã sẵn sàng"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        e.fileName.ifBlank { "Sách ngoại tuyến" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!e.importing) {
                    IconButton(onClick = vm::clearEpubImport) {
                        Icon(Icons.Filled.Close, "Đóng")
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            when {
                e.importing -> {
                    Surface(shape = RoundedCornerShape(24.dp), color = cs.surfaceContainer) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                trackColor = cs.surfaceVariant,
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Đang đọc mục lục và trích ảnh bìa…",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                "Tệp được kiểm tra an toàn trước khi thêm vào thư viện.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                }

                e.error != null -> {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = cs.errorContainer,
                        contentColor = cs.onErrorContainer,
                    ) {
                        Text(
                            e.error,
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = vm::clearEpubImport,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(17.dp),
                    ) {
                        Text("Đóng")
                    }
                }

                e.result != null -> {
                    val story = e.result.story
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = cs.surfaceContainer,
                        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.65f)),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CoverArt(
                                story.color,
                                story.coverUrl,
                                Modifier.size(width = 92.dp, height = 132.dp),
                                Radii.lg,
                                sizeHintPx = 300,
                                label = story.title,
                                fallbackKey = story.id,
                                shape = RoundedCornerShape(18.dp),
                            )
                            Spacer(Modifier.width(15.dp))
                            Column(Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = cs.primaryContainer,
                                    contentColor = cs.onPrimaryContainer,
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(14.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            "EPUB ngoại tuyến",
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    story.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    story.author.ifBlank { "Không rõ tác giả" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cs.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    "${story.totalChapters} chương",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = cs.primary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = vm::listenImportedEpub,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(23.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Nghe ngay", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { vm.saveImportedEpub(true) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(17.dp),
                    ) {
                        Icon(Icons.Filled.BookmarkAdd, null, Modifier.size(19.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Lưu vào thư viện")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteDialog(vm: PlayerViewModel, story: Story?) {
    val cs = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    var renderedStory by remember { mutableStateOf(story) }
    val visibility = remember { MutableTransitionState(false) }

    LaunchedEffect(story) {
        if (story != null) {
            renderedStory = story
            visibility.targetState = true
        } else {
            visibility.targetState = false
        }
    }
    LaunchedEffect(visibility.isIdle, visibility.currentState, story) {
        if (visibility.isIdle && !visibility.currentState && story == null) {
            renderedStory = null
        }
    }

    val current = renderedStory ?: return
    Dialog(onDismissRequest = { vm.askDelete(null) }) {
        AnimatedVisibility(
            visibleState = visibility,
            enter = fadeIn(tween(Motion.NavFadeInMs)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = Motion.SelectionSpring,
            ),
            exit = fadeOut(tween(Motion.NavFadeOutMs)) + scaleOut(
                targetScale = 0.96f,
                animationSpec = tween(Motion.NavExitMs, easing = Motion.ExitEasing),
            ),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = cs.surfaceColorAtElevation(6.dp),
                tonalElevation = 6.dp,
            ) {
                Column(Modifier.padding(24.dp)) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(17.dp))
                            .background(cs.errorContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = cs.onErrorContainer)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Xoá toàn bộ dữ liệu truyện?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Bạn có chắc chắn muốn xóa truyện “${current.title}”? Thao tác sẽ xoá lịch sử, tiến độ, lịch sử chương, dữ liệu tải trước, cache và tệp EPUB trong app. Không thể hoàn tác.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(22.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    ) {
                        TextButton(onClick = {
                            haptics.tap()
                            vm.askDelete(null)
                        }) { Text("Hủy") }
                        Button(
                            onClick = {
                                haptics.snap()
                                vm.deleteFromLibrary(current.id)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cs.errorContainer,
                                contentColor = cs.onErrorContainer,
                            ),
                        ) {
                            Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Xóa")
                        }
                    }
                }
            }
        }
    }
}
