package com.lnxv.player.core.design

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassMathTest {

    private val white = Color.White

    @Test
    fun stops_areSortedAndCoverFullRange() {
        val stops = angularStops(white, dimAlpha = 0.4f, rotationDeg = 0f)
        assertEquals(0f, stops.first().first, 0f)
        assertEquals(1f, stops.last().first, 0f)
        assertTrue(stops.zipWithNext().all { (a, b) -> a.first <= b.first })
    }

    @Test
    fun pluginStops_matchSpecAtZeroRotation() {
        val stops = angularStops(white, dimAlpha = 0.4f, rotationDeg = 0f)
        fun alphaAt(pos: Float) = stops.first { kotlin.math.abs(it.first - pos) < 1e-4f }.second.alpha
        assertEquals(1.0f, alphaAt(0.12f), 1e-3f)
        assertEquals(0.4f, alphaAt(0.28f), 1e-3f)
        assertEquals(0.4f, alphaAt(0.36f), 1e-3f)
        assertEquals(1.0f, alphaAt(0.64f), 1e-3f)
        assertEquals(0.4f, alphaAt(0.78f), 1e-3f)
        assertEquals(0.4f, alphaAt(0.89f), 1e-3f)
    }

    @Test
    fun seam_isContinuous() {
        val stops = angularStops(white, dimAlpha = 0f, rotationDeg = 0f)
        assertEquals(stops.first().second.alpha, stops.last().second.alpha, 1e-6f)
        // value on the seam sits between the dim stop (0.89) and the bright stop (0.12)
        val wrap = stops.first().second.alpha
        assertTrue(wrap in 0f..1f)
    }

    @Test
    fun rotation_shiftsBrightArcs() {
        val rotated = angularStops(white, dimAlpha = 0f, rotationDeg = 90f)
        // bright stop at 12% moves to 37%
        val bright = rotated.first { it.second.alpha > 0.999f }
        assertEquals(0.37f, bright.first, 1e-3f)
    }

    @Test
    fun negativeRotation_wraps() {
        val a = angularStops(white, 0.4f, -90f)
        val b = angularStops(white, 0.4f, 270f)
        assertEquals(a.size, b.size)
        a.zip(b).forEach { (x, y) ->
            assertEquals(x.first, y.first, 1e-4f)
            assertEquals(x.second.alpha, y.second.alpha, 1e-4f)
        }
    }

    @Test
    fun lite_style_removesAberration() {
        val full = liquidStyleFor(GlassKind.Dock, dark = true, amoled = false, lite = false)
        val lite = liquidStyleFor(GlassKind.Dock, dark = true, amoled = false, lite = true)
        assertTrue(full.chromaticAberration.value > 0f)
        assertEquals(0f, lite.chromaticAberration.value, 0f)
        assertTrue(lite.refractionStrength.value < full.refractionStrength.value)
    }
}
