package com.lnxv.player.core.design

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Light Liquid Glass - iOS 26 Design System Palette
val PrimarySkyBlue = Color(0xFF0284C7)
val AccentSkyBlue = Color(0xFF0EA5E9)
val RadiantLavender = Color(0xFF8B5CF6)
val LuminousCyan = Color(0xFF06B6D4)
val EmeraldSuccess = Color(0xFF10B981)
val AmberWarning = Color(0xFFF59E0B)
val CrimsonDanger = Color(0xFFEF4444)

// Main Text & Surface - Light Mode Default
val DeepSlateText = Color(0xFF0F172A)
val SlateMutedText = Color(0xFF64748B)
val SlateSubtleText = Color(0xFF94A3B8)
val LightSurface = Color(0xFFF0F4F9)
val LightCardBg = Color(0xFFFFFFFF)

// Milky-White Translucent Glass Palette
val GlassWhiteHigh = Color(0xE0FFFFFF)     // 88%
val GlassWhiteMedium = Color(0xB0FFFFFF)   // 69%
val GlassWhiteSurface = Color(0xA6FFFFFF)  // 65%
val GlassWhiteSubtle = Color(0x66FFFFFF)   // 40%
val GlassWhiteUltraSubtle = Color(0x26FFFFFF) // 15%
val GlassWhiteBorder = Color(0xBFFFFFFF)   // 75%
val GlassWhiteBorderCrisp = Color(0xE6FFFFFF) // 90%
val GlassShadowSoft = Color(0x140F172A)

// Dark / AMOLED Palette & Obsidian Matte Studio Theme (High Contrast Refined)
val ObsidianBase = Color(0xFF161617)          // Deep clean obsidian background
val ObsidianSurfaceElevated = Color(0xFF222224)// Clean elevated surface
val ObsidianPillActive = Color(0xFF333336)     // High-visibility pill & inputs
val ObsidianTextPrimary = Color(0xFFF8FAFC)    // Ultra-crisp bright typography (#F8FAFC)
val ObsidianTextMuted = Color(0xFFA1A1AA)      // High-contrast secondary typography (#A1A1AA)
val ObsidianBorder = Color(0xFF3F3F46)         // Clean distinct outline (#3F3F46)
val ObsidianAccent = Color(0xFF38BDF8)         // Luminous sky blue highlight

val AmoledBlack = Color(0xFF000000)
val DarkSurface = Color(0xFF1A1A1B)            // Aligned with Obsidian
val DarkSurfaceContainer = Color(0xFF242425)   // Aligned with Obsidian Elevated
val DarkText = Color(0xFFF8FAFC)
val DarkTextMuted = Color(0xFF94A3B8)
val DarkGlassSurface = Color(0xCC242425)
val DarkGlassBorder = Color(0x26FFFFFF)

// Background Ambient Gradients
val LightAmbientGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFE0F2FE), // Soft Sky
        Color(0xFFF0F4F9), // Milky surface
        Color(0xFFF5F3FF)  // Soft Lavender tint
    )
)

val SkyRadialGradient = Brush.radialGradient(
    colors = listOf(Color(0x4DBA7E9), Color.Transparent)
)

val LavenderRadialGradient = Brush.radialGradient(
    colors = listOf(Color(0x388B5CF6), Color.Transparent)
)

val GlassNavDockBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xE6FFFFFF),
        Color(0xCCFFFFFF)
    )
)

val ActivePillGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF0284C7),
        Color(0xFF0EA5E9)
    )
)

// Compatibility Aliases
val NeonCyan = PrimarySkyBlue
val ElectricViolet = RadiantLavender
val CrimsonAlert = CrimsonDanger
val SuccessEmerald = EmeraldSuccess
val AmberAlert = AmberWarning
val SurfaceDark = DarkSurface
val SurfaceDarkElevated = DarkSurfaceContainer
