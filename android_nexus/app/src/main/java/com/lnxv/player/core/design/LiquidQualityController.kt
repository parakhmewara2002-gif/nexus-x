package com.lnxv.player.core.design

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.view.Display
import android.view.Window
import androidx.metrics.performance.JankStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Pure decision logic for the adaptive renderer (no Android types, unit-tested).
 *
 * Levels: 3 = FULL, 2 = LITE, 1 = ADAPTIVE.
 *
 * Anti-flapping rules:
 *  - one decision per measurement WINDOW (~2 s of frames), never per frame
 *  - downgrade needs 2 bad windows in a row and respects a cooldown
 *  - upgrade needs 10 good windows in a row (~20 s), a longer cooldown, and is blocked for an
 *    exponentially growing time after a level has just failed
 *  - severe thermal state or memory pressure apply immediate hard caps
 */
internal class QualityGovernor(
    private val maxLevel: Int,
    startLevel: Int
) {
    var level: Int = startLevel.coerceIn(1, maxLevel)
        private set

    private var lastChangeMs = Long.MIN_VALUE / 2
    private var badWindows = 0
    private var goodWindows = 0
    private val blockedUntil = LongArray(4)
    private val backoffMs = LongArray(4) { BACKOFF_START_MS }

    /** @return the new level if it changed, otherwise null. */
    fun onWindow(
        jankRatio: Float,
        thermalSevere: Boolean,
        thermalModerate: Boolean,
        memoryPressure: Boolean,
        nowMs: Long
    ): Int? {
        val cap = if (memoryPressure) minOf(maxLevel, 2) else maxLevel

        if (thermalSevere && level > 1) return change(1, nowMs, failedLevel = 0)
        if (level > cap) return change(cap, nowMs, failedLevel = 0)

        when {
            jankRatio >= BAD_RATIO -> { badWindows++; goodWindows = 0 }
            jankRatio <= GOOD_RATIO -> { goodWindows++; badWindows = 0 }
            else -> { badWindows = 0; goodWindows = 0 }
        }

        if (badWindows >= 2 && level > 1 && nowMs - lastChangeMs >= DOWN_COOLDOWN_MS) {
            return change(level - 1, nowMs, failedLevel = level)
        }
        if (goodWindows >= 10 && level < cap && !thermalModerate &&
            nowMs - lastChangeMs >= UP_COOLDOWN_MS && nowMs >= blockedUntil[level + 1]
        ) {
            return change(level + 1, nowMs, failedLevel = 0)
        }
        return null
    }

    private fun change(newLevel: Int, nowMs: Long, failedLevel: Int): Int {
        if (failedLevel > 0) {
            blockedUntil[failedLevel] = nowMs + backoffMs[failedLevel]
            backoffMs[failedLevel] = minOf(backoffMs[failedLevel] * 2, BACKOFF_MAX_MS)
        }
        level = newLevel
        lastChangeMs = nowMs
        badWindows = 0
        goodWindows = 0
        return newLevel
    }

    companion object {
        const val BAD_RATIO = 0.12f
        const val GOOD_RATIO = 0.03f
        const val DOWN_COOLDOWN_MS = 8_000L
        const val UP_COOLDOWN_MS = 45_000L
        const val BACKOFF_START_MS = 60_000L
        const val BACKOFF_MAX_MS = 20 * 60_000L

        fun toQuality(level: Int): LiquidQuality = when (level) {
            3 -> LiquidQuality.FULL
            2 -> LiquidQuality.LITE
            else -> LiquidQuality.ADAPTIVE
        }
    }
}

/**
 * Chooses the effective [LiquidQuality] when the user picked "Auto".
 *
 * Inputs (none of them is polled per frame):
 *  - JankStats frame data (real deadline misses), aggregated into ~2 s windows
 *  - ActivityManager.isLowRamDevice + onTrimMemory pressure
 *  - PowerManager thermal status callbacks (API 29+)
 *  - display refresh rate (sets the window length; it is only READ, never forced)
 *
 * The controller never writes user settings; it only publishes [effective].
 * Tracking is switched off whenever the user chooses a fixed mode.
 */
class LiquidQualityController(context: Context) {

    private val appContext = context.applicationContext
    private val activityManager = appContext.getSystemService(ActivityManager::class.java)
    private val lowRam = activityManager?.isLowRamDevice == true

    private val governor = QualityGovernor(
        maxLevel = if (lowRam) 1 else 3,
        // Mid-range GPUs start on LITE and earn FULL by proving they keep their frame deadlines.
        startLevel = if (lowRam) 1 else 2
    )

    private val _effective = MutableStateFlow(QualityGovernor.toQuality(governor.level))
    val effective: StateFlow<LiquidQuality> = _effective.asStateFlow()

    @Volatile private var thermalStatus = 0
    @Volatile private var memoryPressureUntilMs = 0L

    private var executor: ExecutorService? = null
    private var jankStats: JankStats? = null
    private var framesInWindow = 0
    private var jankInWindow = 0
    private var windowFrames = 120

    fun attach(window: Window, display: Display?) {
        if (jankStats != null) return
        val refresh = display?.refreshRate?.takeIf { it > 1f } ?: 60f
        windowFrames = (refresh * 2f).toInt().coerceIn(90, 300)

        val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "lnxv-jank").apply { isDaemon = true } }
        executor = exec
        jankStats = JankStats.createAndTrack(window, exec) { frame -> onFrame(frame.isJank) }.apply {
            jankHeuristicMultiplier = 1.5f
            isTrackingEnabled = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = appContext.getSystemService(PowerManager::class.java)
            thermalStatus = pm?.currentThermalStatus ?: 0
            pm?.addThermalStatusListener(exec) { status -> thermalStatus = status }
        }

        appContext.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                    memoryPressureUntilMs = SystemClock.elapsedRealtime() + 60_000L
                }
            }
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            @Deprecated("Deprecated in Java")
            override fun onLowMemory() {
                memoryPressureUntilMs = SystemClock.elapsedRealtime() + 60_000L
            }
        })
    }

    /** True only while the user setting is "Auto"; otherwise no frame tracking happens at all. */
    fun setActive(active: Boolean) {
        jankStats?.isTrackingEnabled = active
        if (!active) {
            framesInWindow = 0
            jankInWindow = 0
        }
    }

    fun release() {
        jankStats?.isTrackingEnabled = false
        executor?.shutdown()
        executor = null
    }

    // runs on the single "lnxv-jank" thread
    private fun onFrame(isJank: Boolean) {
        framesInWindow++
        if (isJank) jankInWindow++
        if (framesInWindow < windowFrames) return

        val ratio = jankInWindow.toFloat() / framesInWindow
        framesInWindow = 0
        jankInWindow = 0

        val now = SystemClock.elapsedRealtime()
        val status = thermalStatus
        val changed = governor.onWindow(
            jankRatio = ratio,
            thermalSevere = status >= 3,     // THERMAL_STATUS_SEVERE
            thermalModerate = status >= 2,   // THERMAL_STATUS_MODERATE
            memoryPressure = now < memoryPressureUntilMs,
            nowMs = now
        )
        if (changed != null) _effective.value = QualityGovernor.toQuality(changed)
    }
}
