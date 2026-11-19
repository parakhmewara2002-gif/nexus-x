package com.lnxv.player.core.design

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * Which flavour of the Liquid Glass effect this device can render.
 *
 *  - REFRACTION (API 33+): full AGSL refraction + chromatic aberration + frost
 *  - FROST      (API 31-32): backdrop blur only (RenderEffect), no refraction
 *  - FLAT       (API 26-30): classic translucent glass, no backdrop sampling
 */
internal enum class LiquidTier { REFRACTION, FROST, FLAT }

internal object LiquidGlassSupport {
    val tier: LiquidTier = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> LiquidTier.REFRACTION
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> LiquidTier.FROST
        else -> LiquidTier.FLAT
    }
}

/**
 * AGSL port of the fragment shader from the "Liquid Glass" Figma plugin
 * (src/ui/webgl/shaders.ts).
 *
 * Differences vs. the WebGL original:
 *  - The Gaussian frost is NOT done here. It is applied by a native blur
 *    RenderEffect chained *before* this shader (far cheaper on the GPU).
 *  - The refraction direction uses the SDF gradient (true edge normal) instead of
 *    "direction from centre", so wide pills / cards refract correctly along their
 *    long edges.
 *
 * Math kept from the original: rounded-box / ellipse SDF, edge band of
 * `uEdge` px, distortion = strength * edgeAmount^2, R/G/B sampled at
 * distortion -/+ aberration*0.5.
 */
internal const val LIQUID_GLASS_AGSL = """
uniform shader background;
uniform float2 uSize;
uniform float uRadius;
uniform float uEdge;
uniform float uStrength;
uniform float uAberration;
uniform float uEllipse;

float sdRoundedBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - r;
}

float sdEllipse(float2 p, float2 r) {
    float k0 = length(p / r);
    float k1 = length(p / (r * r));
    return k0 * (k0 - 1.0) / max(k1, 0.0001);
}

float shapeSdf(float2 p) {
    if (uEllipse > 0.5) {
        return sdEllipse(p, uSize * 0.5);
    }
    float r = min(uRadius, min(uSize.x, uSize.y) * 0.5);
    return sdRoundedBox(p, uSize * 0.5, r);
}

float2 edgeNormal(float2 p) {
    float e = 1.0;
    float dx = shapeSdf(p + float2(e, 0.0)) - shapeSdf(p - float2(e, 0.0));
    float dy = shapeSdf(p + float2(0.0, e)) - shapeSdf(p - float2(0.0, e));
    float2 g = float2(dx, dy);
    float l = length(g);
    if (l < 0.0001) {
        return float2(0.0, -1.0);
    }
    return g / l;
}

float4 sampleBg(float2 c) {
    return float4(background.eval(clamp(c, float2(0.5), uSize - float2(0.5))));
}

half4 main(float2 fragCoord) {
    float2 p = fragCoord - uSize * 0.5;
    float sdf = shapeSdf(p);
    if (sdf > 0.0) {
        return half4(0.0);
    }

    float edge = max(uEdge, 0.001);
    float amount = clamp((sdf + edge) / edge, 0.0, 1.0);
    float distortion = uStrength * amount * amount;

    if (distortion < 0.01) {
        return half4(sampleBg(fragCoord));
    }

    float2 dir = edgeNormal(p);
    float4 color;
    if (uAberration > 0.001) {
        float rD = max(0.0, distortion - uAberration * 0.5);
        float bD = distortion + uAberration * 0.5;
        float4 cr = sampleBg(fragCoord - dir * rD);
        float4 cg = sampleBg(fragCoord - dir * distortion);
        float4 cb = sampleBg(fragCoord - dir * bD);
        color = float4(cr.r, cg.g, cb.b, cg.a);
    } else {
        color = sampleBg(fragCoord - dir * distortion);
    }
    return half4(color);
}
"""

/** Parameters that fully describe one compiled effect (used as a cache key). */
internal data class RefractionKey(
    val width: Float,
    val height: Float,
    val radiusPx: Float,
    val ellipse: Boolean,
    val edgePx: Float,
    val strengthPx: Float,
    val aberrationPx: Float,
    val frostPx: Float,
    val tier: LiquidTier
)

/**
 * Builds + caches the RenderEffect for one glass element. One instance per element.
 * Only instantiates the AGSL RuntimeShader on API 33+.
 */
internal class LiquidEffectFactory {
    private var lastKey: RefractionKey? = null
    private var lastEffect: RenderEffect? = null
    private var agsl: AgslHolder? = null

    fun effectFor(key: RefractionKey): RenderEffect? {
        if (key == lastKey) return lastEffect
        val effect = build(key)
        lastKey = key
        lastEffect = effect
        return effect
    }

    private fun build(key: RefractionKey): RenderEffect? = when (key.tier) {
        LiquidTier.FLAT -> null
        LiquidTier.FROST -> frostOnly(key)
        LiquidTier.REFRACTION ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val holder = agsl ?: AgslHolder().also { agsl = it }
                holder.build(key)
            } else frostOnly(key)
    }

    private fun frostOnly(key: RefractionKey): RenderEffect? =
        if (key.frostPx > 0.5f) BlurEffect(key.frostPx, key.frostPx, TileMode.Clamp) else null
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AgslHolder {
    private val shader = RuntimeShader(LIQUID_GLASS_AGSL)

    fun build(key: RefractionKey): RenderEffect {
        shader.setFloatUniform("uSize", key.width, key.height)
        shader.setFloatUniform("uRadius", key.radiusPx)
        shader.setFloatUniform("uEdge", key.edgePx)
        shader.setFloatUniform("uStrength", key.strengthPx)
        shader.setFloatUniform("uAberration", key.aberrationPx)
        shader.setFloatUniform("uEllipse", if (key.ellipse) 1f else 0f)

        val refraction = AndroidRenderEffect.createRuntimeShaderEffect(shader, "background")
        val chained = if (key.frostPx > 0.5f) {
            // inner = blur (applied first), outer = refraction shader
            val blur = AndroidRenderEffect.createBlurEffect(key.frostPx, key.frostPx, Shader.TileMode.CLAMP)
            AndroidRenderEffect.createChainEffect(refraction, blur)
        } else refraction
        return chained.asComposeRenderEffect()
    }
}

/** True when this device can sample a backdrop at all (API 31+). */
fun isLiquidGlassAvailable(): Boolean = LiquidGlassSupport.tier != LiquidTier.FLAT

/** True when the full AGSL refraction path (API 33+) is active. */
fun isLiquidRefractionAvailable(): Boolean = LiquidGlassSupport.tier == LiquidTier.REFRACTION
