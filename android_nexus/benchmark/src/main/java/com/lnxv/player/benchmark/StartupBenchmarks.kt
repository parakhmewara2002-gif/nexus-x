package com.lnxv.player.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 1. Cold start  2. Warm start.  Each is run for every quality mode. */
@RunWith(AndroidJUnit4::class)
class StartupBenchmarks {
    @get:Rule val rule = MacrobenchmarkRule()

    private fun start(mode: StartupMode, quality: String) = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = mode,
        iterations = 8
    ) {
        pressHome()
        launchWithQuality(quality)
    }

    @Test fun coldFull() = start(StartupMode.COLD, "FULL")
    @Test fun coldLite() = start(StartupMode.COLD, "LITE")
    @Test fun coldAdaptive() = start(StartupMode.COLD, "ADAPTIVE")
    @Test fun warmFull() = start(StartupMode.WARM, "FULL")
    @Test fun warmLite() = start(StartupMode.WARM, "LITE")
    @Test fun warmAdaptive() = start(StartupMode.WARM, "ADAPTIVE")
}
