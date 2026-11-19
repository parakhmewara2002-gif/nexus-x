package com.lnxv.player.core.design

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Angular gradient stops straight from the plugin spec (documentation/tech-spec.md):
//   stop%  : 12   28   36   64   78   89
//   bright : 1    dim  dim  1    dim  dim
// ---------------------------------------------------------------------------------------------
private val PLUGIN_STOP_POSITIONS = floatArrayOf(0.12f, 0.28f, 0.36f, 0.64f, 0.78f, 0.89f)

/**
 * Builds sweep-gradient stops for the plugin's angular strokes, rotated by [rotationDeg]
 * and wrapped so the gradient is continuous across the 0/1 seam.
 *
 * @param dimAlpha alpha of the "dim" stops (stroke: 0.4, reflection: 0.0)
 */
internal fun angularStops(
    color: Color,
    dimAlpha: Float,
    rotationDeg: Float
): List<Pair<Float, Color>> {
    val bright = 1f
    val alphas = floatArrayOf(bright, dimAlpha, dimAlpha, bright, dimAlpha, dimAlpha)
    val shift = (((rotationDeg % 360f) + 360f) % 360f) / 360f
    val moved = PLUGIN_STOP_POSITIONS.indices
        .map { i -> ((PLUGIN_STOP_POSITIONS[i] + shift) % 1f) to alphas[i] }
        .sortedBy { it.first }

    val first = moved.first()
    val last = moved.last()
    val span = (first.first + 1f) - last.first
    val t = if (span <= 0f) 0f else (1f - last.first) / span
    val wrapAlpha = last.second + (first.second - last.second) * t

    val out = ArrayList<Pair<Float, Color>>(moved.size + 2)
    if (first.first > 0f) out.add(0f to color.copy(alpha = color.alpha * wrapAlpha))
    moved.forEach { out.add(it.first to color.copy(alpha = color.alpha * it.second)) }
    out.add(1f to color.copy(alpha = color.alpha * wrapAlpha))
    return out
}

private fun angularBrush(center: Offset, color: Color, dimAlpha: Float, rotationDeg: Float): Brush =
    Brush.sweepGradient(*angularStops(color, dimAlpha, rotationDeg).toTypedArray(), center = center)

private fun Outline.toComposePath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}

// ---------------------------------------------------------------------------------------------
// Per-element cache (outline path, brushes, paints, compiled effect)
// ---------------------------------------------------------------------------------------------
private class GlassCache {
    var path = Path()
    var inverse = Path()
    var radiusPx = 0f
    var strokeBrush: Brush? = null
    var reflectBrush: Brush? = null
    val effects = LiquidEffectFactory()
    val blurPaint = Paint().apply { isAntiAlias = true }
    val reflectPaint = Paint().apply {
        isAntiAlias = true
        style = PaintingStyle.Stroke
    }

    private var lastShape: Shape? = null
    private var lastSize: Size = Size.Zero
    private var lastDirection: LayoutDirection? = null
    private var lastStyle: LiquidGlassStyle? = null

    fun update(
        shape: Shape,
        size: Size,
        direction: LayoutDirection,
        density: Density,
        style: LiquidGlassStyle
    ) {
        if (shape == lastShape && size == lastSize && direction == lastDirection && style == lastStyle) return
        lastShape = shape
        lastSize = size
        lastDirection = direction
        lastStyle = style

        val outline = shape.createOutline(size, direction, density)
        path = outline.toComposePath()
        radiusPx = if (outline is Outline.Rounded) outline.roundRect.topLeftCornerRadius.x else 0f

        val pad = max(size.width, size.height) + 64f
        inverse = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(-pad, -pad, size.width + pad, size.height + pad))
            addPath(path)
        }

        val center = Offset(size.width / 2f, size.height / 2f)
        strokeBrush = angularBrush(center, style.strokeColor, style.strokeDimAlpha, style.strokeAngle)
        reflectBrush = angularBrush(center, style.reflectionColor, 0f, style.reflectionAngle)
    }
}

// ---------------------------------------------------------------------------------------------
// Public modifier
// ---------------------------------------------------------------------------------------------

/**
 * Liquid Glass surface (port of the Figma plugin's layer stack):
 *
 *   drop shadow  ->  refraction layer (backdrop refracted + frosted)  ->  inner shadow
 *   ->  1dp angular edge stroke  ->  tint  ->  YOUR CONTENT  ->  blurred angular reflection
 *
 * Needs a [backdrop] to refract. Without one (or on API < 31, or with quality OFF) it draws the
 * classic translucent glass so nothing ever renders blank.
 *
 * @param compactStyle optional lighter style used automatically when the element is smaller than
 *                     [compactBelow] (keeps tiny chips/avatars cheap and legible).
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: LiquidBackdrop?,
    shape: Shape,
    style: LiquidGlassStyle,
    compactStyle: LiquidGlassStyle? = null,
    compactBelow: Dp = Dp.Unspecified
): Modifier {
    val tier = LiquidGlassSupport.tier
    val quality = LocalLiquidQuality.current
    val sameWindow = backdrop != null && backdrop.hostView === LocalView.current
    val useBackdrop = sameWindow && tier != LiquidTier.FLAT && quality != LiquidQuality.OFF
    val cache = remember { GlassCache() }
    var position by remember { mutableStateOf(Offset.Zero) }
    val elementLayer = if (useBackdrop) rememberGraphicsLayer() else null

    val tracked = if (useBackdrop) {
        this.onGloballyPositioned { position = it.positionInWindow() }
    } else this

    return tracked.drawWithContent {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) {
            drawContent()
            return@drawWithContent
        }

        val st = if (compactStyle != null &&
            compactBelow != Dp.Unspecified &&
            min(w, h) < compactBelow.toPx()
        ) compactStyle else style

        cache.update(shape, size, layoutDirection, this, st)
        val path = cache.path
        val fancy = tier != LiquidTier.FLAT

        // 1. Drop shadow (outside the glass only)
        if (fancy) st.dropShadow?.let { drawDropShadow(cache, it) }

        // 2. Refraction layer
        if (useBackdrop && elementLayer != null && backdrop != null) {
            val origin = backdrop.origin
            @Suppress("UNUSED_VARIABLE")
            val subscribe = backdrop.version // redraw whenever the backdrop re-records

            val effect = cache.effects.effectFor(
                RefractionKey(
                    width = w,
                    height = h,
                    radiusPx = cache.radiusPx,
                    ellipse = false,
                    edgePx = st.edgeThickness.toPx(),
                    strengthPx = st.refractionStrength.toPx(),
                    aberrationPx = st.chromaticAberration.toPx(),
                    frostPx = st.backgroundBlur.toPx(),
                    tier = tier
                )
            )
            elementLayer.record(this, layoutDirection, IntSize(w.roundToInt(), h.roundToInt())) {
                translate(left = origin.x - position.x, top = origin.y - position.y) {
                    drawLayer(backdrop.layer)
                }
            }
            elementLayer.renderEffect = effect
            clipPath(path) { drawLayer(elementLayer) }

            // 3. Inner shadow (belongs to the refraction layer in the plugin)
            if (fancy) st.innerShadow?.let { drawInnerShadow(cache, it) }

            // 4. Angular edge stroke, 1dp "inside"
            cache.strokeBrush?.let { brush ->
                clipPath(path) {
                    drawPath(path, brush, style = Stroke(width = st.strokeWidth.toPx() * 2f))
                }
            }

            // 5. Tint
            drawPath(path, st.tint)
        } else {
            // Fallback: classic milky/dark translucent glass
            val f = st.fallbackFill
            drawPath(
                path,
                Brush.verticalGradient(
                    listOf(
                        f.copy(alpha = (f.alpha * 1.10f).coerceAtMost(1f)),
                        f.copy(alpha = (f.alpha * 0.92f).coerceAtLeast(0f))
                    )
                )
            )
            cache.strokeBrush?.let { brush ->
                clipPath(path) {
                    drawPath(path, brush, style = Stroke(width = st.strokeWidth.toPx() * 2f))
                }
            }
        }

        // 6. Content
        drawContent()

        // 7. Highlight reflection (above content)
        if (fancy) drawReflection(cache, st)
    }
}

/**
 * Drop-in upgrade for the old flat `liquidGlassSurface`: every existing call site keeps its
 * signature but now refracts whatever [LocalLiquidBackdrop] provides (set up by
 * [AmbientGlassBackground] and the navigation host). Falls back to the previous look otherwise.
 */
fun Modifier.liquidGlassSurface(
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = GlassWhiteMedium,
    borderColor: Color = GlassWhiteBorder,
    borderWidth: Dp = 1.dp,
    kind: GlassKind? = null
): Modifier = composed {
    val backdrop = LocalLiquidBackdrop.current
    val quality = LocalLiquidQuality.current
    val active = backdrop != null &&
        LiquidGlassSupport.tier != LiquidTier.FLAT &&
        quality != LiquidQuality.OFF &&
        backdrop.hostView === LocalView.current

    if (active) {
        val main = rememberLiquidStyle(
            kind ?: GlassKind.Panel,
            tintOverride = if (backgroundColor == GlassWhiteMedium || backgroundColor == GlassWhiteHigh) Color.Unspecified else backgroundColor
        )
        val compact = rememberLiquidStyle(GlassKind.Chip)
        Modifier
            .liquidGlass(backdrop, shape, main, compact, 48.dp)
            .clip(shape)
    } else {
        Modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = (backgroundColor.alpha * 1.10f).coerceAtMost(1f)),
                        backgroundColor.copy(alpha = (backgroundColor.alpha * 0.92f).coerceAtLeast(0f))
                    )
                ),
                shape = shape
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(borderColor, borderColor.copy(alpha = 0.25f), borderColor.copy(alpha = 0.70f)),
                    start = Offset.Zero,
                    end = Offset.Infinite
                ),
                shape = shape
            )
    }
}

// ---------------------------------------------------------------------------------------------
// Draw helpers
// ---------------------------------------------------------------------------------------------
private fun blurFilter(radiusPx: Float): BlurMaskFilter? =
    if (radiusPx > 0.5f) BlurMaskFilter(radiusPx, BlurMaskFilter.Blur.NORMAL) else null

private fun DrawScope.drawDropShadow(cache: GlassCache, s: LiquidDropShadow) {
    val blur = s.blur.toPx()
    clipPath(cache.path, clipOp = ClipOp.Difference) {
        drawIntoCanvas { canvas ->
            val paint = cache.blurPaint
            paint.color = s.color
            paint.asFrameworkPaint().maskFilter = blurFilter(blur)
            canvas.save()
            canvas.translate(s.x.toPx(), s.y.toPx())
            canvas.drawPath(cache.path, paint)
            canvas.restore()
        }
    }
}

private fun DrawScope.drawInnerShadow(cache: GlassCache, s: LiquidInnerShadow) {
    val blur = s.blur.toPx()
    clipPath(cache.path) {
        drawIntoCanvas { canvas ->
            val paint = cache.blurPaint
            paint.color = s.color
            paint.asFrameworkPaint().maskFilter = blurFilter(blur)
            canvas.save()
            canvas.translate(s.x.toPx(), s.y.toPx())
            canvas.drawPath(cache.inverse, paint)
            canvas.restore()
        }
    }
}

private fun DrawScope.drawReflection(cache: GlassCache, st: LiquidGlassStyle) {
    val brush = cache.reflectBrush ?: return
    if (st.reflectionAlpha <= 0f) return
    clipPath(cache.path) {
        drawIntoCanvas { canvas ->
            val paint = cache.reflectPaint
            paint.strokeWidth = st.reflectionWeight.toPx()
            brush.applyTo(size, paint, st.reflectionAlpha)
            paint.asFrameworkPaint().maskFilter = blurFilter(st.reflectionBlur.toPx())
            canvas.drawPath(cache.path, paint)
        }
    }
}
