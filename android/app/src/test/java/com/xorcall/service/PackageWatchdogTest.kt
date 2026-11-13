package com.xorcall.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageWatchdogTest {

    private val dangerousPackages = setOf(
        "com.anydesk.anydeskandroid",
        "com.rustdesk.rustdesk",
        "com.teamviewer.teamviewer.market.mobile",
        "com.teamviewer.host.market.mobile",
        "com.google.chromeremotedesktop"
    )

    @Test
    fun testAnyDeskDetection() {
        val detectedPkg = "com.anydesk.anydeskandroid"
        assertTrue(dangerousPackages.contains(detectedPkg))
    }

    @Test
    fun testRustDeskDetection() {
        val detectedPkg = "com.rustdesk.rustdesk"
        assertTrue(dangerousPackages.contains(detectedPkg))
    }

    @Test
    fun testSafeAppNotFlagged() {
        val safePkg = "com.whatsapp"
        assertFalse(dangerousPackages.contains(safePkg))
    }
}
