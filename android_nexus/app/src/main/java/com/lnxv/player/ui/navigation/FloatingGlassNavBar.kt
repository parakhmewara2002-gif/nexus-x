package com.lnxv.player.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnxv.player.core.design.GlassKind
import com.lnxv.player.core.design.GlassShadowSoft
import com.lnxv.player.core.design.LiquidBackdrop
import com.lnxv.player.core.design.LiquidQuality
import com.lnxv.player.core.design.LocalIsAmoledTheme
import com.lnxv.player.core.design.LocalIsObsidianTheme
import com.lnxv.player.core.design.LocalLiquidQuality
import com.lnxv.player.core.design.ObsidianPillActive
import com.lnxv.player.core.design.ObsidianTextMuted
import com.lnxv.player.core.design.PrimarySkyBlue
import com.lnxv.player.core.design.SlateMutedText
import com.lnxv.player.core.design.isLiquidGlassAvailable
import com.lnxv.player.core.design.liquidGlass
import com.lnxv.player.core.design.rememberLiquidStyle
import kotlin.math.roundToInt

enum class NavTab(val title: String, val icon: ImageVector, val route: String) {
    HOME("Home", Icons.Default.Home, "home"),
    FILES("Files", Icons.Default.Folder, "files"),
    DEVICE("Device", Icons.Default.Smartphone, "device"),
    TOOLS("Tools", Icons.Default.Build, "tools"),
    SETTINGS("Settings", Icons.Default.Settings, "settings")
}

/**
 * Floating pill dock rendered as real Liquid Glass: the screen scrolling underneath is
 * refracted at the dock's edges (see [liquidGlass]); the selected tab is a second, smaller
 * glass "lens" that springs between tabs like the iOS 26 tab bar.
 *
 * @param backdrop the recorded screen content to refract (null => classic translucent dock)
 */
@Composable
fun FloatingGlassNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LiquidBackdrop? = null
) {
    val isObsidian = LocalIsObsidianTheme.current
    val isAmoled = LocalIsAmoledTheme.current
    val quality = LocalLiquidQuality.current
    val liquid = backdrop != null && isLiquidGlassAvailable() &&
        (quality == LiquidQuality.FULL || quality == LiquidQuality.LITE)

    val dockStyle = rememberLiquidStyle(GlassKind.Dock)
    val lensStyle = rememberLiquidStyle(GlassKind.Lens)

    val tabs = NavTab.values()
    val selectedIndex = tabs.indexOfFirst { currentRoute.startsWith(it.route) }

    val density = LocalDensity.current
    val itemX = remember { mutableStateMapOf<Int, Float>() }
    val itemW = remember { mutableStateMapOf<Int, Int>() }
    var itemH by remember { mutableStateOf(0) }

    val lensSpec = spring<Float>(dampingRatio = 0.72f, stiffness = 380f)
    val lensX by animateFloatAsState(
        targetValue = itemX[selectedIndex] ?: 0f,
        animationSpec = lensSpec,
        label = "nav_lens_x"
    )
    val lensW by animateFloatAsState(
        targetValue = (itemW[selectedIndex] ?: 0).toFloat(),
        animationSpec = lensSpec,
        label = "nav_lens_w"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Signature Floating Pill Navigation Dock
        Box(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .then(
                    if (!isLiquidGlassAvailable()) {
                        // API < 31: no custom shadow layer, keep the platform elevation shadow
                        Modifier.shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            ambientColor = if (isObsidian || isAmoled) Color(0x66000000) else GlassShadowSoft,
                            spotColor = if (isObsidian || isAmoled) Color(0x66000000) else GlassShadowSoft
                        )
                    } else Modifier
                )
                .liquidGlass(backdrop, CircleShape, dockStyle)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Box {
                // Sliding glass lens behind the selected tab
                if (liquid && selectedIndex >= 0 && itemW[selectedIndex] != null && itemH > 0) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(lensX.roundToInt(), 0) }
                            .size(
                                width = with(density) { lensW.toDp() },
                                height = with(density) { itemH.toDp() }
                            )
                            .liquidGlass(backdrop, CircleShape, lensStyle)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = index == selectedIndex
                        NavPillItem(
                            tab = tab,
                            isSelected = isSelected,
                            showFlatPill = !liquid,
                            onClick = { onNavigate(tab.route) },
                            modifier = Modifier.onGloballyPositioned { coords ->
                                itemX[index] = coords.positionInParent().x
                                itemW[index] = coords.size.width
                                itemH = coords.size.height
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavPillItem(
    tab: NavTab,
    isSelected: Boolean,
    showFlatPill: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isObsidian = LocalIsObsidianTheme.current
    val isAmoled = LocalIsAmoledTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Bouncy Spring Press Scale
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_pill_press_scale"
    )

    // Animated Active Pop Scale for the Icon
    val iconActiveScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "nav_icon_active_scale"
    )

    // Animated Icon Rotation Wobble on Selection
    val iconRotation by animateFloatAsState(
        targetValue = if (isSelected) 0f else -6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "nav_icon_rotation"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected && (isObsidian || isAmoled) -> Color.White
            isSelected -> PrimarySkyBlue
            isAmoled -> Color(0xFFA1A1AA)
            isObsidian -> ObsidianTextMuted
            else -> SlateMutedText
        },
        label = "nav_pill_color"
    )

    val pillBackgroundAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.22f else 0.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "nav_pill_bg"
    )

    val activeBgColor = when {
        isAmoled -> Color(0xFF18181B)
        isObsidian -> ObsidianPillActive
        else -> PrimarySkyBlue.copy(alpha = pillBackgroundAlpha)
    }

    Box(
        modifier = modifier
            .scale(pressScale)
            .clip(CircleShape)
            .background(
                if (isSelected && showFlatPill) activeBgColor else Color.Transparent,
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = contentColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconActiveScale
                        scaleY = iconActiveScale
                        rotationZ = iconRotation
                    }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tab.title,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
