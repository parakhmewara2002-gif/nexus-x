package com.lnxv.player.core.design

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * A recorded snapshot of "whatever is drawn behind the glass".
 *
 * A container marks itself with [liquidBackdropSource]; glass elements drawn AFTER it can
 * sample that recording. Glass elements must never live inside their own source.
 *
 * Invalidation model (no Compose state is ever written from a draw pass):
 *  - the source node records, then calls [dispatchRecorded];
 *  - glass nodes register a plain listener that only calls `invalidateDraw()` on themselves;
 *  - [recordCount] is a plain counter glass nodes use to skip work when nothing changed.
 */
@Stable
class LiquidBackdrop internal constructor(
    internal val layer: GraphicsLayer,
    /** The Compose host view; glass in other windows (Popup / Dialog) must not use this backdrop. */
    internal val hostView: View
) {
    /** Position of the source in window coordinates (plain field, never observed). */
    internal var origin: Offset = Offset.Zero

    /** Incremented every time the backdrop content or position changed. */
    internal var recordCount: Int = 0
        private set

    private val listeners = ArrayList<() -> Unit>(4)

    internal fun addListener(l: () -> Unit) {
        if (!listeners.contains(l)) listeners.add(l)
    }

    internal fun removeListener(l: () -> Unit) {
        listeners.remove(l)
    }

    internal fun dispatchRecorded() {
        recordCount++
        // index loop: listeners only invalidate themselves, they never mutate this list while we iterate
        for (i in 0 until listeners.size) listeners[i]()
    }
}

@Composable
fun rememberLiquidBackdrop(): LiquidBackdrop {
    val layer = rememberGraphicsLayer()
    val view = LocalView.current
    return remember(layer, view) { LiquidBackdrop(layer, view) }
}

/** The backdrop glass components should refract. Null => flat translucent glass. */
val LocalLiquidBackdrop = compositionLocalOf<LiquidBackdrop?> { null }

/**
 * Rendering pipeline in use (the effective mode, already resolved from AUTO):
 *  - FULL     : AGSL refraction + chromatic aberration + frost on every glass surface
 *  - LITE     : simplified refraction only on small static surfaces (dock, lens, buttons)
 *  - ADAPTIVE : milky gradient glass, no backdrop recording, no shader
 *  - OFF      : classic flat translucent panels
 */
enum class LiquidQuality { FULL, LITE, ADAPTIVE, OFF }

val LocalLiquidQuality = compositionLocalOf { LiquidQuality.LITE }

/**
 * Marks this node as the content that glass should see through.
 * No-op (and nothing is recorded) for ADAPTIVE / OFF and on API < 31.
 */
@Composable
fun Modifier.liquidBackdropSource(backdrop: LiquidBackdrop): Modifier {
    val quality = LocalLiquidQuality.current
    if (LiquidGlassSupport.tier == LiquidTier.FLAT ||
        quality == LiquidQuality.ADAPTIVE ||
        quality == LiquidQuality.OFF
    ) return this
    return this.then(LiquidBackdropSourceElement(backdrop))
}

private data class LiquidBackdropSourceElement(val backdrop: LiquidBackdrop) :
    ModifierNodeElement<LiquidBackdropSourceNode>() {
    override fun create() = LiquidBackdropSourceNode(backdrop)
    override fun update(node: LiquidBackdropSourceNode) {
        node.backdrop = backdrop
    }
}

private class LiquidBackdropSourceNode(var backdrop: LiquidBackdrop) :
    Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val p = coordinates.positionInWindow()
        if (p != backdrop.origin) {
            backdrop.origin = p
            backdrop.dispatchRecorded()
        }
    }

    override fun ContentDrawScope.draw() {
        val layer = backdrop.layer
        layer.record(
            this,
            layoutDirection,
            IntSize(size.width.roundToInt(), size.height.roundToInt())
        ) {
            this@draw.drawContent()
        }
        drawLayer(layer)
        backdrop.dispatchRecorded()
    }
}
