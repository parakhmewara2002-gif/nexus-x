package com.lnxv.player.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Mirrors the plugin's "Inner shadow" controls (colour fixed to black in the plugin). */
@Immutable
data class LiquidInnerShadow(
    val x: Dp,
    val y: Dp,
    val blur: Dp,
    val color: Color
)

@Immutable
data class LiquidDropShadow(
    val x: Dp = 0.dp,
    val y: Dp = 6.dp,
    val blur: Dp = 10.dp,
    val color: Color = Color(0x40000000)
)

/**
 * Every knob of the Figma "Liquid Glass" plugin, in dp.
 *
 *  Refraction tab : edgeThickness (ET), refractionStrength (RS), chromaticAberration (CA), backgroundBlur (BB)
 *  Effects tab    : innerShadow, stroke*, reflection*, tint
 */
@Immutable
data class LiquidGlassStyle(
    // --- Refraction tab ---
    val edgeThickness: Dp = 16.dp,
    val refractionStrength: Dp = 22.dp,
    val chromaticAberration: Dp = 3.dp,
    val backgroundBlur: Dp = 3.dp,
    // --- Effects tab: refraction layer ---
    val innerShadow: LiquidInnerShadow? = null,
    val strokeColor: Color = Color.White,
    val strokeWidth: Dp = 1.dp,
    /** Opacity of the dim arcs of the angular stroke (plugin: 40%). */
    val strokeDimAlpha: Float = 0.4f,
    val strokeAngle: Float = 0f,
    // --- Effects tab: highlight reflection ---
    val reflectionColor: Color = Color.White,
    val reflectionWeight: Dp = 8.dp,
    val reflectionBlur: Dp = 10.dp,
    val reflectionAlpha: Float = 0.9f,
    val reflectionAngle: Float = 0f,
    // --- Effects tab: tint layer ---
    val tint: Color = Color.White.copy(alpha = 0.20f),
    // --- Element-level ---
    val dropShadow: LiquidDropShadow? = null,
    /** Fill used when the backdrop cannot be sampled (API < 31 / no backdrop / quality OFF). */
    val fallbackFill: Color = GlassWhiteHigh
)

enum class GlassKind { Dock, Card, Panel, Button, Lens, Chip }

/** Resolves the right preset for the active theme (Light Glass / Obsidian / AMOLED). */
@Composable
fun rememberLiquidStyle(kind: GlassKind, tintOverride: Color = Color.Unspecified): LiquidGlassStyle {
    val amoled = LocalIsAmoledTheme.current
    val obsidian = LocalIsObsidianTheme.current
    val dark = LocalIsDarkTheme.current || amoled || obsidian
    val quality = LocalLiquidQuality.current
    return liquidStyleFor(kind, dark, amoled, quality == LiquidQuality.LITE, tintOverride)
}

internal fun liquidStyleFor(
    kind: GlassKind,
    dark: Boolean,
    amoled: Boolean,
    lite: Boolean,
    tintOverride: Color = Color.Unspecified
): LiquidGlassStyle {
    val tintDark = if (amoled) Color(0xFF0C0C0C) else Color(0xFF1C1C1E)
    val strokeDim = if (dark) 0.14f else 0.45f
    val reflect = if (dark) 0.55f else 0.95f

    val base = when (kind) {
        GlassKind.Dock -> LiquidGlassStyle(
            edgeThickness = 18.dp, refractionStrength = 26.dp,
            chromaticAberration = 4.dp, backgroundBlur = 2.5.dp,
            tint = if (dark) tintDark.copy(alpha = 0.46f) else Color.White.copy(alpha = 0.30f),
            innerShadow = LiquidInnerShadow(0.dp, 3.dp, 8.dp, Color.Black.copy(alpha = if (dark) 0.35f else 0.10f)),
            strokeDimAlpha = strokeDim, reflectionWeight = 6.dp, reflectionBlur = 8.dp, reflectionAlpha = reflect,
            dropShadow = LiquidDropShadow(0.dp, 8.dp, 18.dp, Color.Black.copy(alpha = if (dark) 0.45f else 0.18f)),
            fallbackFill = if (dark) tintDark.copy(alpha = 0.92f) else GlassWhiteHigh
        )
        GlassKind.Card -> LiquidGlassStyle(
            edgeThickness = 14.dp, refractionStrength = 12.dp,
            chromaticAberration = 1.5.dp, backgroundBlur = 10.dp,
            tint = if (dark) tintDark.copy(alpha = 0.66f) else Color.White.copy(alpha = 0.58f),
            innerShadow = LiquidInnerShadow(0.dp, 2.dp, 8.dp, Color.Black.copy(alpha = if (dark) 0.30f else 0.06f)),
            strokeDimAlpha = strokeDim, reflectionWeight = 5.dp, reflectionBlur = 8.dp, reflectionAlpha = reflect * 0.8f,
            dropShadow = LiquidDropShadow(0.dp, 4.dp, 12.dp, Color.Black.copy(alpha = if (dark) 0.30f else 0.08f)),
            fallbackFill = if (dark) tintDark else GlassWhiteHigh
        )
        GlassKind.Panel -> LiquidGlassStyle(
            edgeThickness = 16.dp, refractionStrength = 16.dp,
            chromaticAberration = 2.dp, backgroundBlur = 8.dp,
            tint = if (dark) tintDark.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.45f),
            innerShadow = LiquidInnerShadow(0.dp, 2.dp, 8.dp, Color.Black.copy(alpha = if (dark) 0.30f else 0.07f)),
            strokeDimAlpha = strokeDim, reflectionWeight = 6.dp, reflectionBlur = 9.dp, reflectionAlpha = reflect * 0.85f,
            dropShadow = LiquidDropShadow(0.dp, 6.dp, 14.dp, Color.Black.copy(alpha = if (dark) 0.35f else 0.10f)),
            fallbackFill = if (dark) tintDark.copy(alpha = 0.9f) else GlassWhiteMedium
        )
        GlassKind.Button -> LiquidGlassStyle(
            edgeThickness = 10.dp, refractionStrength = 12.dp,
            chromaticAberration = 2.dp, backgroundBlur = 3.dp,
            tint = if (dark) tintDark.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.35f),
            innerShadow = LiquidInnerShadow(0.dp, 1.dp, 4.dp, Color.Black.copy(alpha = if (dark) 0.30f else 0.08f)),
            strokeDimAlpha = strokeDim, reflectionWeight = 4.dp, reflectionBlur = 5.dp, reflectionAlpha = reflect,
            dropShadow = LiquidDropShadow(0.dp, 3.dp, 8.dp, Color.Black.copy(alpha = if (dark) 0.30f else 0.10f)),
            fallbackFill = if (dark) tintDark.copy(alpha = 0.92f) else GlassWhiteHigh
        )
        GlassKind.Lens -> LiquidGlassStyle(
            edgeThickness = 14.dp, refractionStrength = 20.dp,
            chromaticAberration = 3.dp, backgroundBlur = 0.dp,
            tint = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.22f),
            innerShadow = LiquidInnerShadow(0.dp, 2.dp, 6.dp, Color.Black.copy(alpha = if (dark) 0.25f else 0.08f)),
            strokeDimAlpha = if (dark) 0.25f else 0.55f, reflectionWeight = 5.dp, reflectionBlur = 6.dp,
            reflectionAlpha = if (dark) 0.75f else 1f,
            dropShadow = null,
            fallbackFill = if (dark) Color.White.copy(alpha = 0.12f) else PrimarySkyBlue.copy(alpha = 0.18f)
        )
        GlassKind.Chip -> LiquidGlassStyle(
            edgeThickness = 8.dp, refractionStrength = 8.dp,
            chromaticAberration = 1.dp, backgroundBlur = 4.dp,
            tint = if (dark) tintDark.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.40f),
            innerShadow = null,
            strokeDimAlpha = strokeDim, reflectionWeight = 3.dp, reflectionBlur = 4.dp, reflectionAlpha = reflect * 0.7f,
            dropShadow = null,
            fallbackFill = if (dark) tintDark.copy(alpha = 0.9f) else GlassWhiteHigh
        )
    }

    var style = base
    if (tintOverride != Color.Unspecified) {
        // keep the override's colour, scale its alpha into the "glass" range so refraction stays visible
        val a = (tintOverride.alpha * if (dark) 0.72f else 0.66f).coerceIn(0f, 1f)
        style = style.copy(tint = tintOverride.copy(alpha = a), fallbackFill = tintOverride)
    }
    if (lite) {
        // LITE: no chromatic aberration, softer refraction, cheaper blur
        style = style.copy(
            chromaticAberration = 0.dp,
            refractionStrength = style.refractionStrength * 0.6f,
            backgroundBlur = style.backgroundBlur * 0.7f,
            reflectionBlur = style.reflectionBlur * 0.7f
        )
    }
    return style
}
