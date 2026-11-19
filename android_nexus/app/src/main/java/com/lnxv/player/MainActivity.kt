package com.lnxv.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.CompositionLocalProvider
import com.lnxv.player.core.design.LNXVTheme
import com.lnxv.player.core.design.LiquidQuality
import com.lnxv.player.core.design.LiquidQualityController
import com.lnxv.player.core.design.LocalLiquidQuality
import com.lnxv.player.playback.pip.PipManager
import com.lnxv.player.ui.navigation.LNXVNavigationHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val prefScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val qualityController by lazy { LiquidQualityController(this) }
    @Volatile private var backgroundPlayEnabled = false
    @Volatile private var autoPipEnabled = true

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.any { it }
        if (isGranted) {
            val repo = (application as LNXVApplication).videoRepository
            CoroutineScope(Dispatchers.IO).launch {
                repo.refreshLibrary()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        // JankStats-backed adaptive renderer. Reads (never forces) the display's refresh rate.
        @Suppress("DEPRECATION")
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else windowManager.defaultDisplay
        qualityController.attach(window, display)
        // Optional per-session override (benchmarks / QA): adb ... --es lnxv.liquid_quality FULL|LITE|ADAPTIVE|OFF
        // It is never persisted and never touches the user's saved setting.
        val sessionQuality = intent?.getStringExtra("lnxv.liquid_quality")?.uppercase()

        val app = application as LNXVApplication
        val initialPrefs = runBlocking { app.dataStore.userPreferencesFlow.first() }
        backgroundPlayEnabled = initialPrefs.isBackgroundPlayEnabled
        autoPipEnabled = initialPrefs.isAutoPiPEnabled
        prefScope.launch {
            app.dataStore.userPreferencesFlow.collect {
                backgroundPlayEnabled = it.isBackgroundPlayEnabled
                autoPipEnabled = it.isAutoPiPEnabled
            }
        }

        setContent {
            val prefs by app.dataStore.userPreferencesFlow.collectAsStateWithLifecycle(initialValue = initialPrefs)
            val autoQuality by qualityController.effective.collectAsStateWithLifecycle()
            val setting = sessionQuality ?: prefs.liquidGlassQuality
            val fixedQuality = remember(setting) { runCatching { LiquidQuality.valueOf(setting) }.getOrNull() }
            // "AUTO" (or anything unrecognised) lets the controller decide; frame tracking only runs then
            LaunchedEffect(fixedQuality) { qualityController.setActive(fixedQuality == null) }
            val liquidQuality = fixedQuality ?: autoQuality

            LNXVTheme(themeMode = prefs.themeMode, darkTheme = prefs.isDarkMode, amoled = prefs.isAmoled) {
                val navController = rememberNavController()
                CompositionLocalProvider(LocalLiquidQuality provides liquidQuality) {
                    LNXVNavigationHost(navController = navController)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If playing video, enter PiP mode automatically
        val app = application as LNXVApplication
        if (!app.isPlayerInitialized) return
        // Only auto-enter PiP from the video screen, and only if the user enabled it
        if (!app.playerManager.isVideoScreenActive || !autoPipEnabled) return
        val player = app.playerManager.exoPlayer
        if (player.isPlaying && PipManager.isPipSupported(this)) {
            val format = player.videoFormat
            val width = format?.width ?: 16
            val height = format?.height ?: 9
            PipManager.enterPipMode(this, width, height)
        }
    }

    override fun onStop() {
        super.onStop()
        val app = application as LNXVApplication
        if (!app.isPlayerInitialized) return
        val manager = app.playerManager
        if (isInPictureInPictureMode) {
            // PiP window was dismissed by the user: stop playback completely
            manager.stopPlayback()
        } else if (!backgroundPlayEnabled) {
            manager.pause()
        }
    }

    override fun onDestroy() {
        qualityController.release()
        val app = application as LNXVApplication
        if (isFinishing && app.isPlayerInitialized) {
            app.playerManager.stopPlayback()
        }
        prefScope.cancel()
        super.onDestroy()
    }
}
