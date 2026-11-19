package com.lnxv.player.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QualityGovernorTest {

    private val bad = 0.30f
    private val good = 0.0f
    private fun QualityGovernor.window(r: Float, now: Long, severe: Boolean = false, moderate: Boolean = false, mem: Boolean = false) =
        onWindow(r, severe, moderate, mem, now)

    @Test fun oneBadWindow_doesNotDowngrade() {
        val g = QualityGovernor(maxLevel = 3, startLevel = 3)
        assertNull(g.window(bad, 20_000))
        assertEquals(3, g.level)
    }

    @Test fun twoBadWindows_downgradeOneStep() {
        val g = QualityGovernor(3, 3)
        g.window(bad, 20_000)
        assertEquals(2, g.window(bad, 22_000))
    }

    @Test fun downgrade_respectsCooldown() {
        val g = QualityGovernor(3, 3)
        g.window(bad, 20_000); g.window(bad, 22_000)          // -> 2 at t=22s
        g.window(bad, 23_000)
        assertNull(g.window(bad, 24_000))                      // cooldown (8s) not over
        assertEquals(1, g.window(bad, 31_000).let { g.window(bad, 32_000) ?: it })
    }

    @Test fun neverGoesBelowAdaptive() {
        val g = QualityGovernor(3, 1)
        repeat(20) { g.window(bad, 100_000L + it * 10_000) }
        assertEquals(1, g.level)
    }

    @Test fun upgrade_needsTenGoodWindowsAndCooldown() {
        val g = QualityGovernor(3, 2)
        var t = 100_000L
        repeat(9) { assertNull(g.window(good, t)); t += 2_000 }
        assertEquals(3, g.window(good, t))
    }

    @Test fun failedLevel_isBlockedAfterDowngrade() {
        val g = QualityGovernor(3, 3)
        g.window(bad, 20_000); g.window(bad, 22_000)           // FULL failed -> LITE, FULL blocked 60 s
        var t = 80_000L                                         // cooldown passed (58 s) but block until 82 s
        var up: Int? = null
        repeat(10) { up = g.window(good, t) ?: up; t += 1_000 }
        assertEquals(3, up)
        assertEquals(3, g.level)
        // ...and the very first attempt inside the block must not have upgraded
        val g2 = QualityGovernor(3, 3)
        g2.window(bad, 20_000); g2.window(bad, 22_000)
        var early: Int? = null
        var t2 = 70_000L
        repeat(10) { early = g2.window(good, t2) ?: early; t2 += 100 }
        assertNull(early)
    }

    @Test fun severeThermal_dropsToAdaptiveImmediately() {
        val g = QualityGovernor(3, 3)
        assertEquals(1, g.window(good, 10_000, severe = true))
    }

    @Test fun memoryPressure_capsAtLite() {
        val g = QualityGovernor(3, 3)
        assertEquals(2, g.window(good, 10_000, mem = true))
    }

    @Test fun lowRamDevice_isCappedAtAdaptive() {
        val g = QualityGovernor(maxLevel = 1, startLevel = 3)
        assertEquals(1, g.level)
        repeat(12) { assertNull(g.window(good, 100_000L + it * 10_000)) }
    }

    @Test fun moderateThermal_blocksUpgrade() {
        val g = QualityGovernor(3, 2)
        var t = 100_000L
        repeat(15) { assertNull(g.window(good, t, moderate = true)); t += 2_000 }
        assertEquals(2, g.level)
    }
}
