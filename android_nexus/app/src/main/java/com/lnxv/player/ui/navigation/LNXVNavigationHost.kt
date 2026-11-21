package com.lnxv.player.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.lnxv.player.core.design.liquidBackdropSource
import com.lnxv.player.core.design.liquidScrollAware
import com.lnxv.player.core.design.rememberLiquidBackdrop
import com.lnxv.player.ui.adb.AdbTerminalScreen
import com.lnxv.player.ui.apk.ApkEditorScreen
import com.lnxv.player.ui.apps.AppManagerScreen
import com.lnxv.player.ui.device.DeviceScreen
import com.lnxv.player.ui.files.FilesScreen
import com.lnxv.player.ui.home.HomeScreen
import com.lnxv.player.ui.player.PlayerScreen
import com.lnxv.player.ui.player.PlayerViewModel
import com.lnxv.player.ui.preview.FilePreviewScreen
import com.lnxv.player.ui.settings.SettingsScreen
import com.lnxv.player.ui.storage.StorageAnalyzerScreen
import com.lnxv.player.ui.tools.DeveloperToolkitScreen
import com.lnxv.player.ui.tools.ToolsScreen
import com.lnxv.player.ui.vault.VaultScreen
import java.net.URLDecoder

@Composable
fun LNXVNavigationHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Files.route,
        Screen.Device.route,
        Screen.Tools.route,
        Screen.Settings.route
    ) || currentRoute.startsWith("files")

    // Whatever the screens draw is recorded so the floating dock can refract it.
    val dockBackdrop = rememberLiquidBackdrop()

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .liquidScrollAware(dockBackdrop)
                .liquidBackdropSource(dockBackdrop),
            // iOS-style: tabs cross-fade, pushed screens slide in from the right while the one below
            // drifts left at 1/3 speed; back reverses it. Springs, so the motion is interruptible.
            enterTransition = {
                if (isTabToTab(initialState.destination.route, targetState.destination.route)) fadeIn(NavFadeSpring)
                else slideInHorizontally(NavPushSpring) { it }
            },
            exitTransition = {
                if (isTabToTab(initialState.destination.route, targetState.destination.route)) fadeOut(NavFadeSpring)
                else slideOutHorizontally(NavPushSpring) { -it / 3 } + fadeOut(NavFadeSpring)
            },
            popEnterTransition = {
                if (isTabToTab(initialState.destination.route, targetState.destination.route)) fadeIn(NavFadeSpring)
                else slideInHorizontally(NavPushSpring) { -it / 3 } + fadeIn(NavFadeSpring)
            },
            popExitTransition = {
                if (isTabToTab(initialState.destination.route, targetState.destination.route)) fadeOut(NavFadeSpring)
                else slideOutHorizontally(NavPushSpring) { it }
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToFiles = { path -> navController.navigate(Screen.Files.createRoute(path)) },
                    onNavigateToAnalyzer = { navController.navigate(Screen.StorageAnalyzer.route) },
                    onNavigateToVault = { navController.navigate(Screen.Vault.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Files.createRoute("")) }
                )
            }

            composable(
                route = Screen.Files.route,
                arguments = listOf(navArgument("path") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
                val decodedPath = if (encodedPath.isNotEmpty()) URLDecoder.decode(encodedPath, "UTF-8") else ""
                FilesScreen(
                    initialPath = decodedPath,
                    onNavigateToPreview = { path ->
                        if (path.endsWith(".apk", ignoreCase = true)) {
                            navController.navigate(Screen.ApkEditor.createRoute(path))
                        } else {
                            navController.navigate(Screen.Preview.createRoute(path))
                        }
                    }
                )
            }

            composable(Screen.StorageAnalyzer.route) {
                StorageAnalyzerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Device.route) {
                DeviceScreen(
                    onNavigateToApps = { navController.navigate(Screen.AppManager.route) }
                )
            }

            composable(Screen.Tools.route) {
                ToolsScreen(
                    onNavigateToAdb = { navController.navigate(Screen.AdbTerminal.route) },
                    onNavigateToVault = { navController.navigate(Screen.Vault.route) },
                    onNavigateToApps = { navController.navigate(Screen.AppManager.route) },
                    onNavigateToAnalyzer = { navController.navigate(Screen.StorageAnalyzer.route) },
                    onNavigateToToolkit = { navController.navigate(Screen.DeveloperToolkit.route) }
                )
            }

            composable(Screen.DeveloperToolkit.route) {
                DeveloperToolkitScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Preview.route,
                arguments = listOf(navArgument("path") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
                val decodedPath = URLDecoder.decode(encodedPath, "UTF-8")
                FilePreviewScreen(
                    filePath = decodedPath,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenInPlayer = { uri ->
                        navController.navigate(Screen.Player.createRoute(uri))
                    }
                )
            }

            composable(Screen.Vault.route) {
                VaultScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AdbTerminal.route) {
                AdbTerminalScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AppManager.route) {
                AppManagerScreen(
                    onOpenApkEditor = { path -> navController.navigate(Screen.ApkEditor.createRoute(path)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ApkEditor.route,
                arguments = listOf(navArgument("path") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
                val decodedPath = URLDecoder.decode(encodedPath, "UTF-8")
                ApkEditorScreen(
                    apkPath = decodedPath,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(navArgument("uri") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val playerViewModel: PlayerViewModel = viewModel()
                PlayerScreen(
                    videoId = 0L,
                    viewModel = playerViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        if (showBottomBar) {
            FloatingGlassNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
                backdrop = dockBackdrop
            )
        }
    }
}

private val NavPushSpring = spring(
    dampingRatio = 0.92f,
    stiffness = 420f,
    visibilityThreshold = IntOffset.VisibilityThreshold
)
private val NavFadeSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow)

private val TAB_BASE_ROUTES = setOf("home", "files", "device", "tools", "settings")

/** "files?path=x" / "player/12" -> "files" / "player". */
internal fun baseRoute(route: String?): String =
    route?.substringBefore('?')?.substringBefore('/') ?: ""

/** True when both ends are bottom-dock tabs (those cross-fade instead of sliding). */
internal fun isTabToTab(from: String?, to: String?): Boolean =
    baseRoute(from) in TAB_BASE_ROUTES && baseRoute(to) in TAB_BASE_ROUTES
