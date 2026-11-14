package com.xorcall.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageWatchdogTest {

    @Test
    fun testAnyDeskDetection() {
        val detectedPkg = "com.anydesk.anydeskandroid"
        assertTrue(PackageWatchdog.isRemoteDesktopPackage(detectedPkg))
    }

    @Test
    fun testRustDeskDetection() {
        val detectedPkg = "com.carriez.rustdesk"
        assertTrue(PackageWatchdog.isRemoteDesktopPackage(detectedPkg))
    }

    @Test
    fun testSafeAppNotFlagged() {
        val safePkg = "com.whatsapp"
        assertFalse(PackageWatchdog.isRemoteDesktopPackage(safePkg))
        assertFalse(PackageWatchdog.isSensitiveFinancialPackage(safePkg))
    }

    @Test
    fun testFinancialAppDetectionPhonePe() {
        assertTrue(PackageWatchdog.isSensitiveFinancialPackage("com.phonepe.app"))
        assertEquals("PhonePe UPI", PackageWatchdog.getFinancialAppName("com.phonepe.app"))
    }

    @Test
    fun testFinancialAppDetectionGPay() {
        assertTrue(PackageWatchdog.isSensitiveFinancialPackage("com.google.android.apps.nbu.paisa.user"))
        assertEquals("Google Pay (GPay)", PackageWatchdog.getFinancialAppName("com.google.android.apps.nbu.paisa.user"))
    }

    @Test
    fun testFinancialAppDetectionPaytm() {
        assertTrue(PackageWatchdog.isSensitiveFinancialPackage("net.one97.paytm"))
        assertEquals("Paytm Payments", PackageWatchdog.getFinancialAppName("net.one97.paytm"))
    }

    @Test
    fun testFinancialAppDetectionBhim() {
        assertTrue(PackageWatchdog.isSensitiveFinancialPackage("in.org.npci.upiapp"))
        assertEquals("BHIM UPI", PackageWatchdog.getFinancialAppName("in.org.npci.upiapp"))
    }

    @Test
    fun testFinancialAppDetectionYonoSbi() {
        assertTrue(PackageWatchdog.isSensitiveFinancialPackage("com.sbi.upi"))
        assertEquals("YONO SBI", PackageWatchdog.getFinancialAppName("com.sbi.upi"))
    }

    @Test
    fun testFinancialAppDetectionTelegramCoercionVector() {
        assertTrue(PackageWatchdog.isSensitiveFinancialPackage("org.telegram.messenger"))
        assertEquals("Telegram (Digital Arrest Coercion)", PackageWatchdog.getFinancialAppName("org.telegram.messenger"))
    }

    @Test
    fun testScamCallActiveStateToggling() {
        PackageWatchdog.setScamCallActive(true)
        assertTrue(PackageWatchdog.isScamCallActive)

        PackageWatchdog.setScamCallActive(false)
        assertFalse(PackageWatchdog.isScamCallActive)
    }
}
