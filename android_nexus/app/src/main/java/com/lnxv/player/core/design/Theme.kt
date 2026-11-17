package com.lnxv.player.core.design

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalIsDarkTheme = compositionLocalOf { false }
val LocalIsObsidianTheme = compositionLocalOf { true }
val LocalIsAmoledTheme = compositionLocalOf { false }

object AppColors {
    val textPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = when {
            LocalIsAmoledTheme.current -> Color.White
            LocalIsObsidianTheme.current -> ObsidianTextPrimary
            LocalIsDarkTheme.current -> DarkText
            else -> DeepSlateText
        }

    val textMuted: Color
        @Composable
        @ReadOnlyComposable
        get() = when {
            LocalIsAmoledTheme.current -> Color(0xFFA1A1AA)
            LocalIsObsidianTheme.current -> ObsidianTextMuted
            LocalIsDarkTheme.current -> DarkTextMuted
            else -> SlateMutedText
        }

    val cardSurface: Color
        @Composable
        @ReadOnlyComposable
        get() = when {
            LocalIsAmoledTheme.current -> Color(0xFF0C0C0C)
            LocalIsObsidianTheme.current -> ObsidianSurfaceElevated
            LocalIsDarkTheme.current -> DarkSurfaceContainer
            else -> GlassWhiteHigh
        }

    val cardBorder: Color
        @Composable
        @ReadOnlyComposable
        get() = when {
            LocalIsAmoledTheme.current -> Color(0xFF27272A)
            LocalIsObsidianTheme.current -> ObsidianBorder
            LocalIsDarkTheme.current -> DarkGlassBorder
            else -> GlassWhiteBorderCrisp
        }

    val background: Color
        @Composable
        @ReadOnlyComposable
        get() = when {
            LocalIsAmoledTheme.current -> AmoledBlack
            LocalIsObsidianTheme.current -> ObsidianBase
            LocalIsDarkTheme.current -> DarkSurface
            else -> LightSurface
        }
}

// Mandatory Light Liquid Glass Scheme
private val LightLiquidGlassColorScheme = lightColorScheme(
    primary = PrimarySkyBlue,
    onPrimary = Color.White,
    secondary = RadiantLavender,
    onSecondary = Color.White,
    tertiary = LuminousCyan,
    background = LightSurface,
    onBackground = DeepSlateText,
    surface = LightCardBg,
    onSurface = DeepSlateText,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = SlateMutedText,
    outline = SlateSubtleText,
    error = CrimsonDanger,
    onError = Color.White
)

// Obsidian Matte Dark Theme (From uploaded theme image)
private val ObsidianColorScheme = darkColorScheme(
    primary = AccentSkyBlue,
    onPrimary = Color.Black,
    secondary = RadiantLavender,
    onSecondary = Color.White,
    tertiary = LuminousCyan,
    background = ObsidianBase,             // #1A1A1B
    onBackground = ObsidianTextPrimary,     // #C8CACD
    surface = ObsidianSurfaceElevated,     // #242425
    onSurface = ObsidianTextPrimary,       // #C8CACD
    surfaceVariant = ObsidianPillActive,   // #2F2F31
    onSurfaceVariant = ObsidianTextMuted,  // #717072
    outline = ObsidianBorder,              // #38383A
    error = CrimsonDanger,
    onError = Color.White
)

// Optional Dark Theme
private val DarkGlassColorScheme = darkColorScheme(
    primary = AccentSkyBlue,
    onPrimary = DeepSlateText,
    secondary = RadiantLavender,
    onSecondary = Color.White,
    tertiary = LuminousCyan,
    background = DarkSurface,
    onBackground = DarkText,
    surface = DarkSurfaceContainer,
    onSurface = DarkText,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = DarkTextMuted,
    outline = DarkTextMuted,
    error = CrimsonDanger,
    onError = Color.White
)

// Optional AMOLED Black Scheme
private val AmoledColorScheme = darkColorScheme(
    primary = AccentSkyBlue,
    onPrimary = Color.Black,
    secondary = RadiantLavender,
    onSecondary = Color.White,
    background = AmoledBlack,                 // #000000
    onBackground = Color.White,
    surface = Color(0xFF0C0C0C),              // Pure dark surface card
    onSurface = Color.White,
    surfaceVariant = Color(0xFF18181B),       // #18181B border/pill
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF27272A),
    error = CrimsonDanger,
    onError = Color.White
)

@Composable
fun LNXVTheme(
    themeMode: String = "OBSIDIAN", // "LIGHT_GLASS", "OBSIDIAN", "AMOLED"
    darkTheme: Boolean = (themeMode != "LIGHT_GLASS"),
    amoled: Boolean = (themeMode == "AMOLED"),
    content: @Composable () -> Unit
) {
    val isAmoled = themeMode == "AMOLED" || amoled
    val isObsidian = (themeMode == "OBSIDIAN" || (darkTheme && !isAmoled)) && !isAmoled
    val colorScheme = when {
        isAmoled -> AmoledColorScheme
        isObsidian -> ObsidianColorScheme
        darkTheme -> DarkGlassColorScheme
        else -> LightLiquidGlassColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val isLight = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalIsObsidianTheme provides isObsidian,
        LocalIsAmoledTheme provides isAmoled
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
