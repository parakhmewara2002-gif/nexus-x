package com.lnxv.player.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a baseline profile on a real device:
 *   ./gradlew :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lnxv.player.benchmark.BaselineProfileGenerator
 * then copy the produced baseline-prof.txt into app/src/main/baseline-prof.txt (profileinstaller is already a dependency).
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        launchWithQuality("LITE")
        flingFirstScrollable()
        listOf("Files", "Device", "Tools", "Settings", "Home").forEach { tapTab(it) }
        tapTab("Files"); flingFirstScrollable()
    }
}
