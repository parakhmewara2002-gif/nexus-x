package com.lnxv.player.core.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ambient background layer providing subtle pastel mesh glows underneath glass panels
 */
@Composable
fun AmbientGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isObsidian = LocalIsObsidianTheme.current
    val isDark = LocalIsDarkTheme.current

    val baseBgColor = when {
        isObsidian -> ObsidianBase
        isDark -> AmoledBlack
        else -> LightSurface
    }

    // Everything drawn inside the source box is what Liquid Glass panels refract.
    // `content` is a SIBLING of the source so glass never samples itself.
    val backdrop = rememberLiquidBackdrop()

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .liquidBackdropSource(backdrop)
                .background(baseBgColor)
        ) {
            if (!isObsidian && !isDark) {
                // Top-right sky ambient circle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 60.dp, y = (-50).dp)
                        .size(340.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x66BAE6FD), Color(0x1AA5F3FC), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                // Mid-left lavender ambient circle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-80).dp, y = (-40).dp)
                        .size(360.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x55DDD6FE), Color(0x18FBCFE8), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                // Bottom-right indigo/cyan glow
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 50.dp, y = 60.dp)
                        .size(320.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x50BAE6FD), Color(0x16C7D2FE), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
            } else if (isObsidian) {
                // Obsidian: faint sky + violet glows so the glass has something to bend
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 100.dp, y = (-80).dp)
                        .size(400.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x3338BDF8), Color(0x0F38BDF8), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-120).dp, y = 80.dp)
                        .size(380.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x298B5CF6), Color(0x0C8B5CF6), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 70.dp, y = 90.dp)
                        .size(340.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x2606B6D4), Color(0x0A06B6D4), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
            }
        }

        CompositionLocalProvider(LocalLiquidBackdrop provides backdrop) {
            content()
        }
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassWhiteMedium,
    borderColor: Color = GlassWhiteBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlassSurface(
            shape = shape,
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            borderWidth = borderWidth,
            kind = GlassKind.Panel
        ),
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = GlassWhiteHigh,
    borderColor: Color = GlassWhiteBorderCrisp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isObsidian = LocalIsObsidianTheme.current
    val isAmoled = LocalIsAmoledTheme.current

    val effectiveBg = when {
        backgroundColor == GlassWhiteHigh && isAmoled -> Color(0xFF0C0C0C)
        backgroundColor == GlassWhiteHigh && isObsidian -> ObsidianSurfaceElevated
        else -> backgroundColor
    }

    val effectiveBorder = when {
        borderColor == GlassWhiteBorderCrisp && isAmoled -> Color(0xFF27272A)
        borderColor == GlassWhiteBorderCrisp && isObsidian -> ObsidianBorder
        else -> borderColor
    }

    val clickModifier = if (onClick != null || onLongClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.97f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "glass_card_scale"
        )
        Modifier
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick ?: {},
                onLongClick = onLongClick
            )
    } else Modifier

    Box(
        modifier = modifier
            .then(clickModifier)
            .liquidGlassSurface(
                shape = shape,
                backgroundColor = effectiveBg,
                borderColor = effectiveBorder,
                borderWidth = 1.dp,
                kind = GlassKind.Card
            ),
        content = content
    )
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glass_btn_scale"
    )

    val bgColor = when {
        !enabled -> Color(0x33E2E8F0)
        isPrimary -> PrimarySkyBlue
        else -> GlassWhiteHigh
    }

    val textColor = when {
        !enabled -> SlateSubtleText
        isPrimary -> Color.White
        else -> DeepSlateText
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor, RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = if (isPrimary) Color(0x600284C7) else GlassWhiteBorderCrisp,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp).padding(end = 6.dp)
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                style = Typography.labelLarge
            )
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    backgroundColor: Color = GlassWhiteHigh,
    size: Dp = 40.dp
) {
    val isObsidian = LocalIsObsidianTheme.current
    val isAmoled = LocalIsAmoledTheme.current

    val effectiveTint = when {
        tint != Color.Unspecified -> tint
        isAmoled -> Color.White
        isObsidian -> ObsidianTextPrimary
        else -> DeepSlateText
    }
    val effectiveBg = when {
        backgroundColor == GlassWhiteHigh && isAmoled -> Color(0xFF18181B)
        backgroundColor == GlassWhiteHigh && isObsidian -> ObsidianPillActive
        else -> backgroundColor
    }
    val effectiveBorder = when {
        isAmoled -> Color(0xFF27272A)
        isObsidian -> ObsidianBorder
        else -> GlassWhiteBorderCrisp
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glass_icon_btn_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .liquidGlassSurface(
                shape = CircleShape,
                backgroundColor = effectiveBg,
                borderColor = effectiveBorder,
                borderWidth = 1.dp,
                kind = GlassKind.Button
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = effectiveTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PrimarySkyBlue
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            style = Typography.labelSmall,
            maxLines = 1,
            softWrap = false
        )
    }
}
