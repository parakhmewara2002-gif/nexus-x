package com.lnxv.player.core.design

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
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
// Per-element cache (outline path, brushes, paints, compiled effect, last-recorded state)
// ---------------------------------------------------------------------------------------------
private class GlassCache {
    var path = Path()
    var inverse = Path()
    var radiusPx = 0f
    var strokeBrush: Brush? = null
    var reflectBrush: Brush? = null
    var sheenBrush: Brush? = null
    var fillBrush: Brush? = null
    val effects = LiquidEffectFactory()
    val blurPaint = Paint().apply { isAntiAlias = true }
    val reflectPaint = Paint().apply {
        isAntiAlias = true
        style = PaintingStyle.Stroke
    }

    // what the element layer currently contains (lets us skip re-recording)
    var recordedCount = -1
    var recordedPos = Offset.Unspecified
    var recordedSize = Size.Unspecified
    var appliedEffect: RenderEffect? = null
    var effectApplied = false

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

        // milky fallback (ADAPTIVE / no backdrop): gradient tint + soft diagonal sheen
        val f = style.fallbackFill
        fillBrush = Brush.verticalGradient(
            listOf(
                f.copy(alpha = (f.alpha * 1.10f).coerceAtMost(1f)),
                f.copy(alpha = (f.alpha * 0.92f).coerceAtLeast(0f))
            )
        )
        sheenBrush = Brush.linearGradient(
            colors = listOf(
                style.reflectionColor.copy(alpha = 0.30f * style.reflectionAlpha),
                Color.Transparent
            ),
            start = Offset.Zero,
            end = Offset(size.width * 0.8f, size.height * 0.9f)
        )
        // geometry changed -> any recorded slice is stale
        recordedCount = -1
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
 * Rendering path depends on the effective [LiquidQuality]:
 *  FULL / LITE : live backdrop refraction (needs [backdrop], API 31+)
 *  ADAPTIVE    : milky gradient glass, fine border, cheap sheen. No shader, no backdrop sampling.
 *  OFF         : same as ADAPTIVE but without sheen/shadows work.
 *
 * Implemented as a Modifier.Node: no Compose state is read or written while drawing, redraws are
 * requested explicitly (backdrop re-recorded / element moved / parameters changed).
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
    val live = sameWindow && tier != LiquidTier.FLAT &&
        (quality == LiquidQuality.FULL || quality == LiquidQuality.LITE)
    val elementLayer = if (live) rememberGraphicsLayer() else null
    return this.then(
        LiquidGlassElement(
            backdrop = if (live) backdrop else null,
            elementLayer = elementLayer,
            shape = shape,
            style = style,
            compactStyle = compactStyle,
            compactBelow = compactBelow,
            tier = tier,
            quality = quality
        )
    )
}

private data class LiquidGlassElement(
    val backdrop: LiquidBackdrop?,
    val elementLayer: GraphicsLayer?,
    val shape: Shape,
    val style: LiquidGlassStyle,
    val compactStyle: LiquidGlassStyle?,
    val compactBelow: Dp,
    val tier: LiquidTier,
    val quality: LiquidQuality
) : ModifierNodeElement<LiquidGlassNode>() {
    override fun create() = LiquidGlassNode(
        backdrop, elementLayer, shape, style, compactStyle, compactBelow, tier, quality
    )

    override fun update(node: LiquidGlassNode) {
        node.updateParams(backdrop, elementLayer, shape, style, compactStyle, compactBelow, tier, quality)
    }
}

private class LiquidGlassNode(
    private var backdrop: LiquidBackdrop?,
    private var elementLayer: GraphicsLayer?,
    private var shape: Shape,
    private var style: LiquidGlassStyle,
    private var compactStyle: LiquidGlassStyle?,
    private var compactBelow: Dp,
    private var tier: LiquidTier,
    private var quality: LiquidQuality
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private val cache = GlassCache()
    private var position = Offset.Zero
    private var registeredWith: LiquidBackdrop? = null
    private val listener: () -> Unit = { if (isAttached) invalidateDraw() }

    override fun onAttach() = syncRegistration()

    override fun onDetach() {
        registeredWith?.removeListener(listener)
        registeredWith = null
    }

    private fun syncRegistration() {
        if (registeredWith !== backdrop) {
            registeredWith?.removeListener(listener)
            backdrop?.addListener(listener)
            registeredWith = backdrop
        }
    }

    fun updateParams(
        backdrop: LiquidBackdrop?,
        elementLayer: GraphicsLayer?,
        shape: Shape,
        style: LiquidGlassStyle,
        compactStyle: LiquidGlassStyle?,
        compactBelow: Dp,
        tier: LiquidTier,
        quality: LiquidQuality
    ) {
        this.backdrop = backdrop
        if (this.elementLayer !== elementLayer) {
            this.elementLayer = elementLayer
            cache.effectApplied = false
            cache.recordedCount = -1
        }
        this.shape = shape
        this.style = style
        this.compactStyle = compactStyle
        this.compactBelow = compactBelow
        this.tier = tier
        this.quality = quality
        if (isAttached) {
            syncRegistration()
            invalidateDraw()
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (backdrop == null) return
        val p = coordinates.positionInWindow()
        if (p != position) {
            position = p
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) {
            drawContent()
            return
        }

        val st = if (compactStyle != null &&
            compactBelow != Dp.Unspecified &&
            min(w, h) < compactBelow.toPx()
        ) compactStyle!! else style

        cache.update(shape, size, layoutDirection, this, st)
        val path = cache.path
        val heavy = tier != LiquidTier.FLAT &&
            (quality == LiquidQuality.FULL || quality == LiquidQuality.LITE)

        // 1. Drop shadow (outside the glass only)
        if (heavy) st.dropShadow?.let { drawDropShadow(cache, it) }

        val bd = backdrop
        val layer = elementLayer
        if (bd != null && layer != null) {
            // 2. Refraction layer: re-record the backdrop slice only if something changed
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
            val needsRecord = cache.recordedCount != bd.recordCount ||
                cache.recordedPos != position ||
                cache.recordedSize != size
            if (needsRecord) {
                val origin = bd.origin
                val pos = position
                layer.record(this, layoutDirection, IntSize(w.roundToInt(), h.roundToInt())) {
                    translate(left = origin.x - pos.x, top = origin.y - pos.y) {
                        drawLayer(bd.layer)
                    }
                }
                cache.recordedCount = bd.recordCount
                cache.recordedPos = pos
                cache.recordedSize = size
            }
            if (!cache.effectApplied || cache.appliedEffect !== effect) {
                layer.renderEffect = effect
                cache.appliedEffect = effect
                cache.effectApplied = true
            }
            clipPath(path) { drawLayer(layer) }

            // 3. Inner shadow (belongs to the refraction layer in the plugin)
            st.innerShadow?.let { drawInnerShadow(cache, it) }

            // 4. Angular edge stroke, 1dp "inside"
            cache.strokeBrush?.let { brush ->
                clipPath(path) {
                    drawPath(path, brush, style = Stroke(width = st.strokeWidth.toPx() * 2f))
                }
            }

            // 5. Tint
            drawPath(path, st.tint)
        } else {
            // Milky gradient glass (ADAPTIVE / OFF / no backdrop): zero shader work
            cache.fillBrush?.let { drawPath(path, it) }
            if (quality != LiquidQuality.OFF) cache.sheenBrush?.let { drawPath(path, it) }
            cache.strokeBrush?.let { brush ->
                clipPath(path) {
                    drawPath(path, brush, style = Stroke(width = st.strokeWidth.toPx() * 2f))
                }
            }
        }

        // 6. Content
        drawContent()

        // 7. Highlight reflection (above content) - blurred, so only for FULL / LITE with backdrop
        if (heavy && bd != null) drawReflection(cache, st)
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
    val sameWindow = backdrop != null && backdrop.hostView === LocalView.current
    val canSample = sameWindow && LiquidGlassSupport.tier != LiquidTier.FLAT

    // FULL : live refraction everywhere        LITE : live refraction on buttons only
    // ADAPTIVE : milky gradient glass (no sampling)      OFF : classic flat panel
    val mode = when (quality) {
        LiquidQuality.FULL -> if (canSample) 2 else 0
        LiquidQuality.LITE -> if (canSample && kind == GlassKind.Button) 2 else 0
        LiquidQuality.ADAPTIVE -> 1
        LiquidQuality.OFF -> 0
    }

    if (mode >= 1) {
        val main = rememberLiquidStyle(
            kind ?: GlassKind.Panel,
            tintOverride = if (backgroundColor == GlassWhiteMedium || backgroundColor == GlassWhiteHigh) Color.Unspecified else backgroundColor
        )
        val compact = rememberLiquidStyle(GlassKind.Chip)
        Modifier
            .liquidGlass(if (mode == 2) backdrop else null, shape, main, compact, 48.dp)
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
