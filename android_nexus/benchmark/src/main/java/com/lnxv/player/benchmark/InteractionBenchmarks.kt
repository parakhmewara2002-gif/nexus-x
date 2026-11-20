package com.lnxv.player.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.MemoryUsageMetric.Mode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame timing (jank) for: Home rendering, navigation transitions, File Manager scrolling,
 * Storage / APK / Tools screens, plus memory and background<->foreground. All per quality mode.
 * Tab labels come from FloatingGlassNavBar (Home / Files / Device / Tools / Settings).
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class InteractionBenchmarks {
    @get:Rule val rule = MacrobenchmarkRule()

    private fun frames(quality: String, block: androidx.benchmark.macro.MacrobenchmarkScope.() -> Unit) =
        rule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = 6,
            setupBlock = { launchWithQuality(quality) },
            measureBlock = block
        )

    // 3. Home rendering
    private fun home(q: String) = frames(q) { flingFirstScrollable() }
    @Test fun homeFull() = home("FULL")
    @Test fun homeLite() = home("LITE")
    @Test fun homeAdaptive() = home("ADAPTIVE")

    // 4. Navigation transitions (dock + glass lens)
    private fun navigation(q: String) = frames(q) {
        listOf("Files", "Device", "Tools", "Settings", "Home").forEach { tapTab(it) }
    }
    @Test fun navigationFull() = navigation("FULL")
    @Test fun navigationLite() = navigation("LITE")
    @Test fun navigationAdaptive() = navigation("ADAPTIVE")

    // 5. File Manager scrolling (also the Liquid Glass stress case: many glass rows)
    private fun files(q: String) = frames(q) { tapTab("Files"); flingFirstScrollable(4) }
    @Test fun filesScrollFull() = files("FULL")
    @Test fun filesScrollLite() = files("LITE")
    @Test fun filesScrollAdaptive() = files("ADAPTIVE")

    // 7. Liquid Glass rendering: settings page is a column of glass cards
    private fun glass(q: String) = frames(q) { tapTab("Settings"); flingFirstScrollable(3) }
    @Test fun glassFull() = glass("FULL")
    @Test fun glassLite() = glass("LITE")
    @Test fun glassAdaptive() = glass("ADAPTIVE")

    // 9. Storage Analyzer / APK Manager entry points live under Tools
    private fun tools(q: String) = frames(q) { tapTab("Tools"); flingFirstScrollable(2) }
    @Test fun toolsFull() = tools("FULL")
    @Test fun toolsLite() = tools("LITE")
    @Test fun toolsAdaptive() = tools("ADAPTIVE")

    // 11. Memory while navigating
    @Test fun memoryNavigationFull() = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(MemoryUsageMetric(Mode.Last)),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = { launchWithQuality("FULL") }
    ) { listOf("Files", "Device", "Tools", "Home").forEach { tapTab(it) } }

    // 12. Background <-> foreground
    @Test fun backgroundForegroundLite() = frames("LITE") {
        device.pressHome()
        launchWithQuality("LITE")
    }
}
