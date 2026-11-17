package com.lnxv.player.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Files : Screen("files?path={path}") {
        fun createRoute(path: String = ""): String =
            if (path.isEmpty()) "files" else "files?path=${java.net.URLEncoder.encode(path, "UTF-8")}"
    }
    object StorageAnalyzer : Screen("storage_analyzer")
    object Device : Screen("device")
    object Tools : Screen("tools")
    object Settings : Screen("settings")
    object Preview : Screen("preview?path={path}") {
        fun createRoute(path: String): String =
            "preview?path=${java.net.URLEncoder.encode(path, "UTF-8")}"
    }
    object Vault : Screen("vault")
    object AdbTerminal : Screen("adb_terminal")
    object DeveloperToolkit : Screen("developer_toolkit")
    object AppManager : Screen("app_manager")
    object ApkEditor : Screen("apk_editor?path={path}") {
        fun createRoute(path: String): String =
            "apk_editor?path=${java.net.URLEncoder.encode(path, "UTF-8")}"
    }
    object Player : Screen("player?uri={uri}") {
        fun createRoute(uri: String): String =
            "player?uri=${java.net.URLEncoder.encode(uri, "UTF-8")}"
    }
}
