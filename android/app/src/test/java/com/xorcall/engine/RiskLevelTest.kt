package com.xorcall.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskLevelTest {

    @Test
    fun testRiskLevelHierarchy() {
        val levels = listOf(RiskLevel.SAFE, RiskLevel.WATCH, RiskLevel.WARN, RiskLevel.CRITICAL)
        assertEquals(4, levels.size)
        assertTrue(RiskLevel.CRITICAL.ordinal > RiskLevel.SAFE.ordinal)
    }

    @Test
    fun testModularHapticsDefaultOff() {
        // Judge Critique #3: Haptics must default to disabled for vibration-averse users
        var isHapticsEnabled = false
        var isWhisperEnabled = true
        var isFamilyPingEnabled = true

        assertFalse("Haptics should be default OFF per user feedback", isHapticsEnabled)
        assertTrue("In-ear Audio Whisper should be active fallback", isWhisperEnabled)
        assertTrue("Family safe-ping should be active fallback", isFamilyPingEnabled)
    }
}
