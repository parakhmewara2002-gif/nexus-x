# Liquid Glass (ported from figma-liquid-glass)

Real backdrop refraction for NEXUS X, built from the Figma plugin's shader + layer spec.

| Plugin | Android |
|---|---|
| WebGL fragment shader (`src/ui/webgl/shaders.ts`) | AGSL `RuntimeShader` in `LiquidGlassShader.kt` (API 33+) |
| ET / RS / CA / BB sliders | `LiquidGlassStyle.edgeThickness / refractionStrength / chromaticAberration / backgroundBlur` |
| Inner shadow, angular stroke, reflection, tint | same fields, drawn in `LiquidGlassModifier.kt` in the plugin's layer order |
| Layer order (refraction → tint → content → highlight) | `Modifier.liquidGlass()` |

Tiers: API 33+ full refraction, API 31-32 frosted blur only, API 26-30 classic translucent glass.

## How it works
- `AmbientGlassBackground` and the nav host record their background into a `LiquidBackdrop`.
- Glass elements drawn after it sample that recording (`LocalLiquidBackdrop`).
- Existing `liquidGlassSurface / GlassCard / GlassPanel / GlassIconButton` call sites upgrade automatically, no screen changes.
- Dock = glass pill + sliding glass "lens" for the active tab.
- Settings → Appearance → Liquid Glass Effect: Full / Lite / Off.

## Notes
- Compose BOM bumped 2024.06.00 → 2024.12.01 (needs `rememberGraphicsLayer`, Compose UI 1.7+).
- Refraction direction uses the SDF gradient (true edge normal) instead of "direction from centre", so long edges of pills/cards bend correctly.
- Frost blur is a native `RenderEffect` chained before the shader (much cheaper than the plugin's in-shader kernel).
- Glass inside Popup/Dialog windows automatically falls back to flat glass.
