package com.metrolist.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test
    fun comparesVersionsNumerically() {
        assertEquals(0, Updater.compareVersions("6.1.0", "6.1.0"))
        assertEquals(1, Updater.compareVersions("6.10.0", "6.9.0"))
        assertEquals(-1, Updater.compareVersions("6.1", "6.1.1"))
        assertEquals(0, Updater.compareVersions("v6.1", "6.1.0"))
    }

    @Test
    fun offersOnlyNewerVersions() {
        assertTrue(Updater.isUpdateAvailable(currentVersion = "6.1.0", latestVersion = "6.2.0"))
        assertFalse(Updater.isUpdateAvailable(currentVersion = "6.1.0", latestVersion = "6.1.0"))
        assertFalse(Updater.isUpdateAvailable(currentVersion = "6.1.0", latestVersion = "6.0.9"))
    }
}
