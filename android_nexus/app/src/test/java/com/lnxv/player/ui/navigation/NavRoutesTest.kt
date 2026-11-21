package com.lnxv.player.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavRoutesTest {
    @Test fun baseRoute_stripsArgs() {
        assertEquals("files", baseRoute("files?path=/sdcard"))
        assertEquals("player", baseRoute("player/12"))
        assertEquals("home", baseRoute("home"))
        assertEquals("", baseRoute(null))
    }

    @Test fun tabToTab_fades() {
        assertTrue(isTabToTab("home", "files?path=/x"))
        assertTrue(isTabToTab("settings", "tools"))
    }

    @Test fun detailScreens_push() {
        assertFalse(isTabToTab("files", "player/3"))
        assertFalse(isTabToTab("player/3", "files"))
        assertFalse(isTabToTab(null, "home"))
    }
}
