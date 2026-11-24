/*
 * MiniGestures.kt -- bo cu chi cua mini-player (4 huong, khoa truc).
 *
 *   Vuot LEN     -> bung player (morph, theo sat ngon tay).
 *   Vuot XUONG   -> cat o mini-player di, AM THANH VAN CHAY BINH THUONG.
 *   Vuot PHAI    -> CHUONG TIEP THEO.
 *   Vuot TRAI    -> LUI LAI MOT CHUONG.
 *
 * Truc duoc khoa ngay o vai pixel dau tien: da di ngang thi khong con keo doc
 * nua va nguoc lai -> khong bao gio lo tay kich nham hai hanh dong cung luc.
 * Tuyet doi khong co duong nao trong day dung/tam dung am thanh.
 *
 * ------------------------- CACH LAM PHAN VUOT NGANG -------------------------
 * Lam theo dung kieu cua ArchiveTune, vi kieu do don gian hon va chac hon nhieu:
 *
 *   1. The di THEO TAY 1:1, khong giam luc, khong gioi han. Ngon tay di bao nhieu
 *      thi the di bay nhieu.
 *   2. Huong nao khong con chuong de di thi the DUNG IM HOAN TOAN -- khong nhich,
 *      khong keo cang. Mot buc tuong that, cam nhan duoc ngay lap tuc.
 *   3. Co HAI duong de an: keo du xa, HOAC hat nhanh nhung phai di duoc mot doan
 *      toi thieu. Doan toi thieu la de mot cu cham run tay khong bi tinh la vuot.
 *   4. Sau khi doi chuong, the CHI BAT VE GIUA bang lo xo. Khong bay ra khoi canh
 *      roi truot vao tu phia doi dien nua -- kieu do phai giau mot buoc nhay
 *      (snapTo) dang sau lop mo dan, va phai doan truoc bao lau thi chuong moi
 *      nap xong (delay co dinh). Doan sai mot cai la thay ngay the nhay hoac thay
 *      hai ten chuong cung luc. Bat ve giua thi khong co gi phai giau ca.
 *   5. Rung dung luc GOI DOI CHUONG, tuc dung khoanh khac ten chuong doi -- khong
 *      the lech nhip voi cai mat thay.
 *
 * ============================ CAC BAY DA SUA ============================
 *
 * BAY 1 -- pointerInput(Unit) chi chay DUNG MOT LAN.
 * Khoi lenh ben trong khong bao gio khoi dong lai, nen no giu mai cac lambda cua
 * lan compose DAU TIEN. Neu doc thang tham so lambda, chung om theo trang thai cu
 * mai mai: co hasNext/hasPrevious bi dong bang tu luc chua nap chuong nao (deu
 * false) nen cu chi doi chuong KHONG BAO GIO chay, con vuot len thi mo lai dung
 * truyen cu. Vi vay moi callback va moi co trang thai o day deu di qua
 * rememberUpdatedState -- doc .value ngay tai thoi diem cham de luon lay ban moi.
 *
 * BAY 2 -- KHONG BAO GIO quyet dinh dua tren gesture.offsetX.value.
 * Animatable chi nhan gia tri qua ham suspend (snapTo), tuc la phai di qua
 * scope.launch, tuc la CHAY SAU khung hinh hien tai. Luc onDragEnd goi thi cac
 * coroutine do co the con chua kip chay, nen offsetX.value dang thut lui phia sau
 * ngon tay -- vuot mot cu dai va dut khoat van co the doc ra mot con so nho hon
 * nguong va khong doi chuong. Do la nguyen nhan "co luc duoc co luc khong".
 * => dxAcc/dyAcc la NGUON SU THAT DUY NHAT cho moi phep so sanh; Animatable chi
 *    con nhiem vu duy nhat la VE RA.
 *
 * BAY 3 -- chot truc phai luon chot duoc.
 * Truoc day dieu kien la "huong nay phai troi hon huong kia 1.25 lan", nen mot cu
 * vuot cheo roi vao khoang giua (vi du 300x260 px) khong thoa man ca hai chieu,
 * truc KHONG BAO GIO duoc chot va CA CU VUOT BI BO QUA IM LANG. Gio chi so ai lon
 * hon ai theo tong quang duong, nen truc luon chot duoc.
 */
package vn.tramtruyen.reader.presentation.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.abs

/* ------------------------- Nguong, dat ten ro rang ------------------------- */

/** Keo ngang qua bao nhieu dp thi tu no tinh la mot cu doi chuong. */
private val SWIPE_DP = 72.dp

/**
 * Hat tay nhanh hon muc nay (px/s) thi doi chuong ngay, khong can keo du xa --
 * nhung van phai di duoc MIN_DIST_DP. Hai dieu kien di chung nhu ArchiveTune, de
 * mot cu cham run tay (nhanh nhung gan nhu khong di duoc doan nao) khong bi tinh
 * la mot cu vuot.
 */
private const val FLICK_X = 900f

/** Doan toi thieu phai di duoc de duong "hat nhanh" duoc tinh. */
private val MIN_DIST_DP = 18.dp

/** Keo doc xuong qua bao nhieu dp thi cat the mini di. */
private val DISMISS_DP = 64.dp

private const val FLICK_Y = 1200f

/** Di duoc bao nhieu dp thi CHOT truc. Xem BAY 3 o dau file. */
private val LOCK_DP = 6.dp

/**
 * Lo xo dua the ve giua: khong nay mot chut nao va mem, dung nhu ArchiveTune
 * (DampingRatioNoBouncy + StiffnessLow). The dung han o giua chu khong vong qua
 * vong lai quanh diem giua.
 */
private val SettleSpec = Motion.GestureSettle

/** Do lech hien tai cua the mini (px), dung chung cho gesture va phan ve. */
class MiniGestureState internal constructor(
	val offsetX: Animatable<Float, AnimationVector1D>,
	val offsetY: Animatable<Float, AnimationVector1D>,
) {
	/**
	 * Tra the ve giua bang lo xo. Day la duong ket THUC DUY NHAT cua moi cu vuot
	 * ngang -- doi chuong duoc hay khong doi duoc cung deu ve giua. Nho vay khong
	 * con buoc nhay nao phai giau, va khong can doan truoc bao lau thi chuong moi
	 * nap xong.
	 */
	suspend fun settleX() = offsetX.animateTo(0f, SettleSpec)

	suspend fun settleY() = offsetY.animateTo(0f, Motion.SwipeSettle)

	suspend fun resetNow() {
		offsetX.snapTo(0f)
		offsetY.snapTo(0f)
	}
}

@Composable
fun rememberMiniGestureState(): MiniGestureState = remember {
	MiniGestureState(Animatable(0f), Animatable(0f))
}

/**
 * Gan bo cu chi 4 huong len the mini-player.
 *
 * @param hasPrevChapter con chuong truoc de lui hay khong. Het chuong thi the
 *        DUNG IM theo huong do, khong nhich mot pixel nao.
 * @param hasNextChapter con chuong sau de sang hay khong.
 * @param onExpand mo player (chi mo giao dien, khong nap lai noi dung).
 * @param onDismiss cat the mini di nhung VAN doc tiep.
 */
@Composable
fun Modifier.miniPlayerGestures(
	morph: PlayerMorphState,
	gesture: MiniGestureState,
	hasPrevChapter: Boolean,
	hasNextChapter: Boolean,
	onExpand: () -> Unit,
	onDismiss: () -> Unit,
	onPrevChapter: () -> Unit,
	onNextChapter: () -> Unit,
): Modifier {
	val scope = rememberCoroutineScope()
	val haptics = rememberHaptics()
	// Ban MOI NHAT cua moi thu ma khoi pointerInput ben duoi can doc. Xem BAY 1 o
	// dau file: khong co lop nay thi toan bo cu chi lam viec tren trang thai chet.
	val canPrev by rememberUpdatedState(hasPrevChapter)
	val canNext by rememberUpdatedState(hasNextChapter)
	val expand by rememberUpdatedState(onExpand)
	val dismiss by rememberUpdatedState(onDismiss)
	val prevChapter by rememberUpdatedState(onPrevChapter)
	val nextChapter by rememberUpdatedState(onNextChapter)

	return this.pointerInput(Unit) {
		val tracker = VelocityTracker()
		// 0 = chua biet huong, 1 = ngang, 2 = doc.
		var axis = 0
		// NGUON SU THAT: cong don ngay trong callback nen luon dung o dung frame
		// ngon tay vua di qua. Xem BAY 2 o dau file.
		var dxAcc = 0f
		var dyAcc = 0f
		// Da rung bao "an nac" cho lan keo doc nay chua.
		var tickedY = false
		// Tong quang duong da di ke tu luc dat tay xuong. Chi dung de CHOT TRUC.
		// Phai cong don chu khong duoc xet tung frame roi le: mot frame le chi dai vai
		// pixel nen ty so giua x va y trong frame do gan nhu la nhieu.
		var totX = 0f
		var totY = 0f
		var lockSlop = 0f
		// Do theo kich thuoc THUC TE moi lan dat tay xuong, de xoay ngang man hinh
		// hay doi co chu cung khong lam sai nguong.
		var swipeX = 0f
		var minDist = 0f
		var dismissY = 0f
		var hostH = 2000f

		detectDragGestures(
			onDragStart = {
				tracker.resetTracking()
				axis = 0
				dxAcc = 0f
				dyAcc = 0f
				totX = 0f
				totY = 0f
				tickedY = false
				lockSlop = LOCK_DP.toPx()
				swipeX = SWIPE_DP.toPx()
				minDist = MIN_DIST_DP.toPx()
				dismissY = DISMISS_DP.toPx()
				hostH = if (morph.hostSize.height > 0) morph.hostSize.height.toFloat() else 2000f
			},
			onDragCancel = {
				axis = 0
				morph.dragging = false
				scope.launch { gesture.settleX() }
				scope.launch { gesture.settleY() }
				scope.launch { morph.settle(0f) }
			},
			onDragEnd = {
				val v = tracker.calculateVelocity()
				val lockedAxis = axis
				axis = 0
				morph.dragging = false

				if (lockedAxis == 1) {
					// --------- CHOT HUONG: mot lan, ro rang, khong the nhap nhang ---------
					// Quy uoc bang chuyen, giong ArchiveTune: day the sang phai la keo noi
					// dung dang o truoc ve, nen ra chuong TRUOC.
					// +1 = vuot sang PHAI = LUI mot chuong
					// -1 = vuot sang TRAI = chuong TIEP THEO
					// Hai duong de an, dung nhu ArchiveTune: keo du xa, hoac hat nhanh ma
					// van di duoc doan toi thieu. Neu ca hai deu khong dat thi thoi.
					val far = abs(dxAcc) >= swipeX
					val flick = abs(dxAcc) >= minDist && abs(v.x) >= FLICK_X
					// Huong lay theo doan da keo, khong lay theo huong hat. Vi the da bi
					// chan cung o huong khong di duoc (xem than vong lap keo), dxAcc luon
					// nam dung ve phia ma nguoi dung that su di duoc.
					val dir = if (!far && !flick) 0 else if (dxAcc > 0f) 1 else -1
					// Kiem lai lan cuoi bang so lieu MOI NHAT: trong luc ngon tay dang di,
					// chuong co the vua nap xong hoac vua het.
					val allowed = (dir == 1 && canPrev) || (dir == -1 && canNext)
					if (allowed) {
						// Rung DUNG khoanh khac ten chuong doi -> khong the lech nhip voi
						// cai mat nhin thay.
						haptics.tick()
						if (dir == 1) prevChapter() else nextChapter()
					}
					// Doi chuong duoc hay khong, the cung chi bat ve giua. Mot duong ket
					// thuc duy nhat nen khong con buoc nhay nao phai giau.
					scope.launch { gesture.settleX() }
				} else if (lockedAxis == 2) {
					when {
						// Da keo len du xa (hoac hat len nhanh) -> bung player.
						morph.progress > 0.22f || v.y < -FLICK_Y -> {
							expand()
							scope.launch { morph.settle(1f) }
							scope.launch { gesture.settleY() }
						}
						// Keo xuong du xa -> cat the di, nhung van doc tiep.
						dyAcc > dismissY || v.y > FLICK_Y -> {
							dismiss()
							scope.launch { gesture.resetNow() }
						}
						else -> {
							scope.launch { morph.settle(0f) }
							scope.launch { gesture.settleY() }
						}
					}
				}
			},
		) { change, amount ->
			change.consume()
			tracker.addPosition(change.uptimeMillis, change.position)

			totX += amount.x
			totY += amount.y
			// Chot truc theo TONG quang duong, bang mot phep so sanh don gian, nen bao
			// gio cung chot duoc mot trong hai huong -> khong con cu vuot nao bi bo qua
			// im lang. Bang nhau thi uu tien ngang (doi chuong).
			if (axis == 0 && (abs(totX) >= lockSlop || abs(totY) >= lockSlop)) {
				// BAY 4 (bo test bat duoc) -- doan tay da di TRUOC khi chot truc khong
				// duoc phep bo di. Truoc day bo dem chi bat dau chay tu luc chot xong, nen
				// mat trang doan dau. Voi mot cu vuot ngan thi doan mat do la mot phan dang
				// ke (vuot 60 px thi mat 15 px, tuc 25%) -- du de khong dat doan toi thieu
				// va CA DUONG "hat nhanh" khong bao gio an. The cung vinh vien di cham hon
				// ngon tay dung bang doan bi mat do.
				// Nen: nap san bang quang duong da di den TRUOC frame nay, roi de doan
				// duoi cong frame nay vao nhu binh thuong -> khong mat, khong dem hai lan.
				if (abs(totX) >= abs(totY)) {
					axis = 1
					val preX = totX - amount.x
					val seedOk = if (preX > 0f) canPrev else canNext
					if (seedOk) dxAcc = preX
				} else {
					axis = 2
					morph.dragging = true
					dyAcc = totY - amount.y
				}
			}

			if (axis == 1) {
				// BUC TUONG THAT: huong dang di khong con chuong thi bo qua han doan tay
				// nay, the khong nhich mot pixel. Khong keo cang, khong nhich he -- giong
				// ArchiveTune. Nguoi dung biet ngay la het chuong ma khong phai cho den
				// luc tha tay.
				val usable = if (amount.x > 0f) canPrev else canNext
				if (usable) {
					// THEO TAY 1:1 -- khong giam luc, khong chan tren. Tay di bao nhieu thi
					// the di bay nhieu.
					dxAcc += amount.x
					// UNDISPATCHED: chay ngay tai cho thay vi xep hang sang frame sau,
					// nen the di dung frame ngon tay di chu khong bam duoi mot nhip.
					scope.launch(start = CoroutineStart.UNDISPATCHED) {
						gesture.offsetX.snapTo(dxAcc)
					}
				}
			} else if (axis == 2) {
				dyAcc += amount.y
				if (!tickedY && morph.progress <= 0.001f && dyAcc >= dismissY) {
					haptics.tick()
					tickedY = true
				} else if (tickedY && dyAcc < dismissY * 0.8f) {
					tickedY = false
				}
				val dy = amount.y
				scope.launch(start = CoroutineStart.UNDISPATCHED) {
					if (dy < 0f) {
						if (gesture.offsetY.value > 0f) {
							gesture.offsetY.snapTo((gesture.offsetY.value + dy).coerceAtLeast(0f))
						} else {
							morph.snapTo(morph.progress - dy / hostH)
						}
					} else {
						if (morph.progress > 0.001f) {
							morph.snapTo(morph.progress - dy / hostH)
						} else {
							gesture.offsetY.snapTo(gesture.offsetY.value + dy * 0.85f)
						}
					}
				}
			}
		}
	}
}
