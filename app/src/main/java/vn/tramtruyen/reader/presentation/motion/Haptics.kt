/*
 * App-wide haptic feedback. Native/OEM haptics are always preferred so the
 * feedback follows the device motor, system strength and accessibility setting.
 */
package vn.tramtruyen.reader.presentation.motion

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

@Immutable
data class HapticConfig(
    val enabled: Boolean = true,
    val intensity: Int = 55,
)

val LocalHapticConfig = staticCompositionLocalOf { HapticConfig() }

class Haptics internal constructor(
    private val view: View,
    private val config: HapticConfig,
) {
    private val context = view.context.applicationContext
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    fun tap() = nativePulse(HapticFeedbackConstants.KEYBOARD_TAP, VibrationEffect.EFFECT_CLICK, 8L, .55f)
    fun tick() = nativePulse(HapticFeedbackConstants.CLOCK_TICK, VibrationEffect.EFFECT_TICK, 7L, .40f)

    /** A light detent while dragging sliders, matching Android's native seek controls. */
    fun sliderTick() = nativePulse(
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        else HapticFeedbackConstants.CLOCK_TICK,
        VibrationEffect.EFFECT_TICK,
        6L,
        .32f,
    )

    fun swipe() = nativePulse(HapticFeedbackConstants.GESTURE_END, VibrationEffect.EFFECT_TICK, 9L, .45f)
    fun back() = nativePulse(HapticFeedbackConstants.CONFIRM, VibrationEffect.EFFECT_CLICK, 11L, .70f)
    fun snap() = nativePulse(HapticFeedbackConstants.CONFIRM, VibrationEffect.EFFECT_CLICK, 12L, .78f)
    fun drop() = nativePulse(HapticFeedbackConstants.KEYBOARD_TAP, VibrationEffect.EFFECT_TICK, 7L, .42f)

    /** Preview keeps the app strength control meaningful without changing normal native haptics. */
    fun preview(intensity: Int, force: Boolean = false) {
        if (!force && !config.enabled) return
        if (!systemHapticsEnabled()) return
        val level = intensity.coerceIn(1, 100)
        runCatching {
            val target = vibrator ?: return@runCatching
            if (!target.hasVibrator()) return@runCatching
            val amplitude = (28f + level * 1.8f).roundToInt().coerceIn(1, 220)
            target.vibrate(VibrationEffect.createOneShot(14L, amplitude))
        }
    }

    private fun nativePulse(
        constant: Int,
        fallbackEffect: Int,
        fallbackDurationMs: Long,
        fallbackStrength: Float,
    ) {
        if (!config.enabled || !systemHapticsEnabled()) return
        val now = SystemClock.uptimeMillis()
        val previous = lastPulseAt.get()
        if (now - previous < MIN_PULSE_GAP_MS || !lastPulseAt.compareAndSet(previous, now)) return

        // OEM-tuned feedback is more natural than a hand-authored one-shot pulse and
        // automatically follows the device's haptic-strength setting.
        val handled = runCatching { view.performHapticFeedback(constant) }.getOrDefault(false)
        if (!handled) fallbackVibrate(fallbackEffect, fallbackDurationMs, fallbackStrength)
    }

    private fun fallbackVibrate(effect: Int, durationMs: Long, strength: Float) {
        runCatching {
            val target = vibrator ?: return@runCatching
            if (!target.hasVibrator()) return@runCatching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                target.vibrate(VibrationEffect.createPredefined(effect))
            } else {
                val level = config.intensity.coerceIn(1, 100)
                val amplitude = (24f + level * 1.45f * strength)
                    .roundToInt()
                    .coerceIn(1, 180)
                target.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            }
        }
    }

    private fun systemHapticsEnabled(): Boolean = runCatching {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.HAPTIC_FEEDBACK_ENABLED,
            1,
        ) != 0
    }.getOrDefault(true)

    companion object {
        private const val MIN_PULSE_GAP_MS = 24L
        private val lastPulseAt = AtomicLong(0L)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val config = LocalHapticConfig.current
    return remember(view, config) { Haptics(view, config) }
}

/**
 * Material slider with native-feeling detents. Continuous sliders use 20 tactile
 * segments; stepped sliders vibrate exactly once whenever they cross a real step.
 */
@Composable
fun HapticSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val haptics = rememberHaptics()
    val segments = if (steps > 0) steps + 1 else 20
    fun bucket(v: Float): Int {
        val span = valueRange.endInclusive - valueRange.start
        if (span <= 0f) return 0
        return (((v - valueRange.start) / span).coerceIn(0f, 1f) * segments).roundToInt()
    }
    var lastBucket by remember { mutableIntStateOf(bucket(value)) }

    Slider(
        value = value,
        onValueChange = { next ->
            val nextBucket = bucket(next)
            if (nextBucket != lastBucket) {
                lastBucket = nextBucket
                haptics.sliderTick()
            }
            onValueChange(next)
        },
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = {
            haptics.snap()
            onValueChangeFinished?.invoke()
        },
    )
}

/**
 * Observes releases at the root in Final pass and never consumes touch events.
 * A drag/scroll produces one pulse on release, not one pulse per pixel.
 */
fun Modifier.globalHapticFeedback(haptics: Haptics): Modifier = pointerInput(haptics) {
    awaitPointerEventScope {
        while (true) {
            val firstEvent = awaitPointerEvent(PointerEventPass.Final)
            val down = firstEvent.changes.firstOrNull {
                it.pressed && !it.previousPressed
            } ?: continue
            val pointerId = down.id
            val origin = down.position
            var maxDistance = 0f
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                maxDistance = maxOf(maxDistance, (change.position - origin).getDistance())
                if (!change.pressed) {
                    if (maxDistance > viewConfiguration.touchSlop * 1.15f) haptics.swipe()
                    else haptics.tap()
                    break
                }
            }
        }
    }
}
