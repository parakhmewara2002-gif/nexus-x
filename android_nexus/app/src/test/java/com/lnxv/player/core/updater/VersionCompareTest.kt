package com.lnxv.player.core.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {
    @Test fun newerPatch() = assertTrue(isNewerVersion("1.0.3", "1.0.2"))
    @Test fun numericNotLexical() = assertTrue(isNewerVersion("1.0.10", "1.0.9"))
    @Test fun sameIsNotNewer() = assertFalse(isNewerVersion("1.0.2", "1.0.2"))
    @Test fun olderIsNotNewer() = assertFalse(isNewerVersion("1.0.1", "1.0.2"))
    @Test fun vPrefixIgnored() = assertTrue(isNewerVersion("v1.1.0", "1.0.9"))
    @Test fun missingPartsAreZero() = assertFalse(isNewerVersion("1.0", "1.0.0"))
}
