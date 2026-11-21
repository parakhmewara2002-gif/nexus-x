package com.lnxv.player.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

const val TARGET_PACKAGE = "com.nexus.fliem.benchmark"

/** The three rendering pipelines + OFF, selected per session through an intent extra (never persisted). */
val QUALITY_MODES = listOf("FULL", "LITE", "ADAPTIVE")

fun MacrobenchmarkScope.launchWithQuality(mode: String) {
    startActivityAndWait { intent -> intent.putExtra("lnxv.liquid_quality", mode) }
}

fun MacrobenchmarkScope.tapTab(title: String) {
    device.wait(Until.hasObject(By.text(title)), 5_000)
    device.findObject(By.text(title))?.click()
    device.waitForIdle()
}

fun MacrobenchmarkScope.flingFirstScrollable(times: Int = 3) {
    device.wait(Until.hasObject(By.scrollable(true)), 5_000)
    val list = device.findObject(By.scrollable(true)) ?: return
    list.setGestureMargin(device.displayWidth / 5)
    repeat(times) {
        list.fling(Direction.DOWN)
        device.waitForIdle()
        list.fling(Direction.UP)
        device.waitForIdle()
    }
}
