/*
 * PlayerMorph.kt -- tam player truot len/xuong giua mini-player va toan man hinh.
 *
 * ================== VI SAO PHAI VIET LAI TOAN BO FILE NAY ==================
 *
 * Ban cu chay theo kieu "MORPH": moi khung hinh deu tinh lai mot hinh chu nhat
 * noi suy giua o mini-player va ca man hinh, roi ep ca cay PlayerScreen do lai
 * dung kich thuoc do bang Constraints.fixed(w, h).
 *
 * Do chinh la nut that. Modifier.layout { measure(Constraints.fixed(...)) } voi
 * kich thuoc DOI MOI FRAME co nghia la: 60 lan moi giay, Compose phai DO LAI va
 * XEP LAI toan bo cay con ben trong player -- ca danh sach karaoke, ca cum nut,
 * ca phan chu. Do lai la viec dat nhat trong ba tang (composition / layout /
 * draw). Chu thich cu tu hao rang "khong recompose lay mot lan", va dieu do dung,
 * nhung no bo qua mat rang tranh recompose ma van do lai moi frame thi hau nhu
 * khong tiet kiem duoc gi.
 *
 * ArchiveTune khong lam vay. No do noi dung DUNG MOT LAN o kich thuoc that, roi
 * chi TRUOT khoi do len xuong bang Modifier.offset. Truot chi cham vao tang layout
 * o muc re nhat (doi vi tri dat, khong do lai), con lai deu la tang ve. Vi the no
 * muot ngay ca tren may yeu, va vi the tam nay gio lam y het:
 *
 *   - Noi dung do MOT lan o kich thuoc full man hinh, khong bao gio do lai.
 *   - Ca tam truot bang offset{} -- doc tien trinh o tang layout.
 *   - Bo goc, do mo, mau nen doc o tang ve.
 *   - Khong con phep noi suy hinh chu nhat, khong con miniBounds trong duong ve.
 *
 * ------------------------- HAI THU KHAC LAY TU ARCHIVETUNE -------------------------
 *
 * 1. CoroutineStart.UNDISPATCHED.
 *    Animatable chi nhan gia tri qua ham suspend, nen moi lan ngon tay nhich la
 *    phai scope.launch { snapTo(...) }. Mac dinh, launch XEP HANG coroutine do lai
 *    cho vong lap sau. Ngon tay di o frame N nhung tam moi nhuc o frame N+1 --
 *    lech mot frame, va do la thu cam nhan duoc ngay: tam luon bam duoi ngon tay.
 *    UNDISPATCHED cho khoi lenh chay NGAY tai cho, dong bo trong chinh callback
 *    cham, nen tam di dung frame ngon tay di.
 *
 * 2. Ban giao cuon -> keo (nested scroll).
 *    Truoc day, dang o trong player ma vuot xuong tren danh sach karaoke thi chi
 *    cuon danh sach; cuon het len dinh roi keo tiep thi KHONG co gi xay ra, phai
 *    nhac tay ra dat lai vao cho khac moi dong duoc player. Gio danh sach cuon
 *    den dinh se tu dong TRAO lai phan keo con thua cho tam nay, nen mot cu vuot
 *    lien mach tu giua danh sach keo thang xuong la dong player -- dung nhu moi
 *    app nghe nhac tu te.
 */
package vn.tramtruyen.reader.presentation.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Nguong tha tay: duoi muc nay thi thu ve mini, tren thi bung lai full. */
private const val COLLAPSE_BELOW = 0.58f

/** Van toc vuot xuong (px/s) du manh de dong ngay du chua qua nguong. */
private const val FLING_DOWN = 1050f

/** Trang thai dung chung. Mini-player va player cung doc/ghi vao day. */
class PlayerMorphState internal constructor(
	internal val anim: Animatable<Float, AnimationVector1D>,
) {
	/**
	 * O mini-player dang nam o dau tren man hinh (toa do cua so, px).
	 *
	 * Khong con tham gia vao duong ve nua -- tam gio truot len tu day man hinh chu
	 * khong phinh ra tu o mini. Van giu lai vi mini-player dang ghi vao day va so
	 * lieu nay huu ich neu sau nay can biet o mini o dau.
	 */
	var miniBounds by mutableStateOf(Rect.Zero)

	/** Goc tren-trai cua vung chua (toa do cua so). */
	var hostOrigin by mutableStateOf(Offset.Zero)

	/** Kich thuoc vung chua (px). Dung de quy doi px -> ti le tien trinh. */
	var hostSize by mutableStateOf(IntSize.Zero)

	/** Dang co ngon tay dieu khien -> tam khoa animation tu dong. */
	var dragging by mutableStateOf(false)

	/**
	 * Goi dung mot lan khi tam vua khop tron vao full man hinh (de rung nhe).
	 * Neu animation bi cat ngang giua chung thi animateTo nem CancellationException
	 * nen callback KHONG chay -- dung y muon: chi "khop" that moi rung.
	 */
	internal var onSnapFull: (() -> Unit)? = null

	/** 0 = dang la mini-player, 1 = da bung full man hinh. */
	val progress: Float get() = anim.value

	/** Chieu cao vung chua, co chan duoi de khong bao gio chia cho 0. */
	internal val hostHeightPx: Float
		get() = if (hostSize.height > 0) hostSize.height.toFloat() else 2000f

	suspend fun snapTo(value: Float) = anim.snapTo(value.coerceIn(0f, 1f))

	suspend fun settle(target: Float) {
		anim.animateTo(target, Motion.MorphSpring)
		if (target >= 0.999f) onSnapFull?.invoke()
	}
}

@Composable
fun rememberPlayerMorphState(open: Boolean): PlayerMorphState {
	val state = remember { PlayerMorphState(Animatable(if (open) 1f else 0f)) }
	val haptics = rememberHaptics()
	SideEffect { state.onSnapFull = { haptics.snap() } }
	LaunchedEffect(open) {
		// Chi chay khi khong co ngon tay tren man hinh: tranh giat cuop quyen dieu khien.
		if (!state.dragging) state.settle(if (open) 1f else 0f)
	}
	return state
}

/**
 * Tam player. Dat o lop tren cung, phu toan man hinh. Khi [state] o muc 0 thi tam
 * nam tron ben duoi canh duoi man hinh va khong ve gi ca (mini-player ben duoi van
 * bam duoc binh thuong).
 *
 * @param surfaceColor mau nen cua player khi da bung het.
 * @param collapsedColor mau nen luc bat dau keo. Tam xuat phat dung mau nay roi
 *        chuyen dan sang [surfaceColor], nen giay dau tien cua cu vuot khong he co
 *        cu doi mau lo lieu.
 * @param collapsedCorner bo goc (dp) o mep tren cua tam khi chua bung het.
 * @param collapsedShadow do do bong (dp) luc chua bung het, tat dan khi bung ra.
 * @param onCollapse goi khi nguoi dung vuot xuong du de dong player.
 */
@Composable
fun PlayerMorphHost(
	state: PlayerMorphState,
	surfaceColor: Color,
	collapsedColor: Color = surfaceColor,
	collapsedCorner: Float = 34f,
	collapsedShadow: Float = 8f,
	onCollapse: () -> Unit,
	content: @Composable () -> Unit,
) {
	val scope = rememberCoroutineScope()
	// Ban moi nhat cua callback. Khoi nested-scroll ben duoi chi duoc tao mot lan
	// nen khong duoc phep om cung lambda cua lan compose dau tien.
	val collapseNow by rememberUpdatedState(onCollapse)

	/** Quyet dinh chung cho ca hai duong tha tay: keo truc tiep va ban giao cuon. */
	val finish: (Float) -> Unit = { velocityDown ->
		scope.launch(start = CoroutineStart.UNDISPATCHED) {
			if (velocityDown > FLING_DOWN ||
				(velocityDown > -FLING_DOWN && state.progress < COLLAPSE_BELOW)
			) {
				collapseNow()
				state.settle(0f)
			} else {
				state.settle(1f)
			}
		}
	}
	val finishRef by rememberUpdatedState(finish)

	// Ban giao cuon -> keo. Danh sach ben trong player cuon den dinh roi ma nguoi
	// dung van keo xuong tiep, thi phan keo thua do duoc trao cho tam nay.
	val handover = remember(state) {
		object : NestedScrollConnection {
			/** Danh sach ben trong da cham dinh chua. */
			var atTop = false

			override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
				// Bat dau cuon nguoc len -> khong con o dinh nua, tra quyen lai cho danh sach.
				if (available.y < 0f) atTop = false
				return Offset.Zero
			}

			override fun onPostScroll(
				consumed: Offset,
				available: Offset,
				source: NestedScrollSource,
			): Offset {
				// Danh sach khong an duoc chut nao ma van con phan keo xuong -> da cham dinh.
				if (!atTop) atTop = consumed.y >= -0.5f && available.y > 0f
				if (!atTop || available.y <= 0f) return Offset.Zero
				state.dragging = true
				scope.launch(start = CoroutineStart.UNDISPATCHED) {
					state.snapTo(state.progress - available.y / state.hostHeightPx)
				}
				return Offset(0f, available.y)
			}

			override suspend fun onPreFling(available: Velocity): Velocity {
				if (!atTop) return Velocity.Zero
				atTop = false
				state.dragging = false
				finishRef(available.y)
				return available
			}

			override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
				atTop = false
				return Velocity.Zero
			}
		}
	}

	Box(
		Modifier
			.fillMaxSize()
			.onGloballyPositioned {
				state.hostOrigin = it.positionInWindow()
				state.hostSize = it.size
			},
	) {
		val dragEnabled by remember {
			derivedStateOf { state.progress > 0.999f || state.dragging }
		}

		Box(
			Modifier
				// KICH THUOC CO DINH: do dung mot lan, khong bao gio do lai. Day la
				// diem khac han ban cu va la ly do chinh khien tam nay muot.
				.fillMaxSize()
				// Keep PlayerScreen composed and measured while collapsed. Previously
				// it was created at progress > 0, so the first morph frame also had
				// to compose the whole player, decode painters and measure Karaoke.
				// offset{} only changes placement (never child measurement), and also
				// moves the hit target below the screen while collapsed.
				.offset {
					val p = state.progress.coerceIn(0f, 1f)
					IntOffset(0, ((1f - p) * state.hostHeightPx).roundToInt())
				}
				.graphicsLayer {
					val p = state.progress.coerceIn(0f, 1f)
					alpha = if (p <= 0.001f) 0f else 1f
					clip = false
					shadowElevation = 0f
					compositingStrategy = CompositingStrategy.ModulateAlpha
				}
				// Mau nen: chuyen sang mau player ngay trong doan dau, de khoanh khac
				// "ban giao" giua the mini va tam nay khong nhin thay duoc.
				.drawBehind {
					val p = state.progress.coerceIn(0f, 1f)
					drawRect(lerp(collapsedColor, surfaceColor, rampF(p, 0f, 0.30f)))
				}
				// Nhan phan keo thua tu danh sach ben trong khi no da cuon den dinh.
				// Phai dat TRUOC draggable de danh sach duoc uu tien an truoc.
				.nestedScroll(handover)
				.draggable(
					state = rememberDraggableState { delta ->
						// UNDISPATCHED: chay ngay tai cho, khong xep hang sang frame sau,
						// nen tam di dung frame ngon tay di.
						scope.launch(start = CoroutineStart.UNDISPATCHED) {
							state.snapTo(state.progress - delta / state.hostHeightPx)
						}
					},
					orientation = Orientation.Vertical,
					enabled = dragEnabled,
					onDragStarted = { state.dragging = true },
					onDragStopped = { velocity ->
						state.dragging = false
						finish(velocity)
					},
				),
		) {
			// Noi dung: kich thuoc co dinh bang ca vung chua, do dung MOT lan. Trong
			// suot cu vuot khong co mot phep do lai nao xay ra o day.
			Box(Modifier.fillMaxSize()) {
				content()
			}
		}
	}
}
