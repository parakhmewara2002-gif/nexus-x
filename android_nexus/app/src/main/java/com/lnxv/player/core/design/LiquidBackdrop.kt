package com.lnxv.player.core.design

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * A recorded snapshot of "whatever is drawn behind the glass".
 *
 * A container marks itself with [liquidBackdropSource]; every glass element that is
 * drawn AFTER it (siblings / descendants outside the source) can sample that
 * recording and refract it. Glass elements must never live inside their own source.
 */
@Stable
class LiquidBackdrop internal constructor(
    internal val layer: GraphicsLayer,
    /** The Compose host view; glass in other windows (Popup / Dialog) must not use this backdrop. */
    internal val hostView: View
) {
    /** Position of the source in window coordinates. */
    internal var origin by mutableStateOf(Offset.Zero)

    /** Bumped every time the source re-records so dependent glass redraws. */
    internal var version by mutableIntStateOf(0)
}

@Composable
fun rememberLiquidBackdrop(): LiquidBackdrop {
    val layer = rememberGraphicsLayer()
    val view = LocalView.current
    return remember(layer, view) { LiquidBackdrop(layer, view) }
}

/** The backdrop glass components should refract. Null => flat translucent glass. */
val LocalLiquidBackdrop = compositionLocalOf<LiquidBackdrop?> { null }

/** User-selectable cost of the effect (Settings -> Liquid Glass). */
enum class LiquidQuality { FULL, LITE, OFF }

val LocalLiquidQuality = compositionLocalOf { LiquidQuality.FULL }

/**
 * Marks this node as the content that glass should see through.
 * No-op on API < 31, where glass falls back to the classic translucent look.
 */
fun Modifier.liquidBackdropSource(backdrop: LiquidBackdrop): Modifier {
    if (LiquidGlassSupport.tier == LiquidTier.FLAT) return this
    return this
        .onGloballyPositioned { backdrop.origin = it.positionInWindow() }
        .drawWithContent {
            val layer = backdrop.layer
            layer.record(
                this,
                layoutDirection,
                IntSize(size.width.roundToInt(), size.height.roundToInt())
            ) {
                this@drawWithContent.drawContent()
            }
            drawLayer(layer)
            backdrop.version = backdrop.version + 1
        }
}
