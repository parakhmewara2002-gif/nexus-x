package com.lnxv.player.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnxv.player.core.design.*

enum class NavTab(val title: String, val icon: ImageVector, val route: String) {
    HOME("Home", Icons.Default.Home, "home"),
    FILES("Files", Icons.Default.Folder, "files"),
    DEVICE("Device", Icons.Default.Smartphone, "device"),
    TOOLS("Tools", Icons.Default.Build, "tools"),
    SETTINGS("Settings", Icons.Default.Settings, "settings")
}

@Composable
fun FloatingGlassNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isObsidian = LocalIsObsidianTheme.current
    val isAmoled = LocalIsAmoledTheme.current

    val navBg = when {
        isAmoled -> Color(0xFF0C0C0C)
        isObsidian -> ObsidianSurfaceElevated
        else -> GlassWhiteHigh
    }
    val navBorder = when {
        isAmoled -> Color(0xFF27272A)
        isObsidian -> ObsidianBorder
        else -> GlassWhiteBorderCrisp
    }

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
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = if (isObsidian || isAmoled) Color(0x66000000) else GlassShadowSoft,
                    spotColor = if (isObsidian || isAmoled) Color(0x66000000) else GlassShadowSoft
                )
                .liquidGlassSurface(
                    shape = CircleShape,
                    backgroundColor = navBg,
                    borderColor = navBorder,
                    borderWidth = 1.dp
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.values().forEach { tab ->
                    val isSelected = currentRoute.startsWith(tab.route)
                    NavPillItem(
                        tab = tab,
                        isSelected = isSelected,
                        onClick = { onNavigate(tab.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavPillItem(
    tab: NavTab,
    isSelected: Boolean,
    onClick: () -> Unit
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
        modifier = Modifier
            .scale(pressScale)
            .clip(CircleShape)
            .background(if (isSelected) activeBgColor else Color.Transparent, CircleShape)
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
