package com.xorcall.engine

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VideoDeepfakeDetectorTest {

    private lateinit var detector: VideoDeepfakeDetector

    @Before
    fun setUp() {
        detector = VideoDeepfakeDetector()
    }

    @Test
    fun testAuthenticHumanWebcamFeed_TestA() {
        // Scenario Test A: Normal webcam feed of a real person (e.g. son/daughter)
        val result = detector.verifyAuthenticHuman()

        assertFalse("Real human feed must not be flagged as synthetic", result.isSynthetic)
        assertEquals(0.04f, result.anomalyScore, 0.001f)
        assertEquals(0.96f, result.biometricAuthenticityScore, 0.001f)
        assertEquals(0.04f, result.azimuthalLatticeAnomaly, 0.001f)
        assertEquals(0.03f, result.boundaryBlurWarping, 0.001f)
        assertEquals(0.05f, result.blinkPeriodicityAnomaly, 0.001f)
        assertEquals("AUTHENTIC_HUMAN_FEED", result.verdict)
        assertEquals(
            "🟢 VERIFIED BIOMETRIC IDENTITY: Authentic Human Video Feed (0.04 Anomaly Score)",
            result.hudBadge
        )
        assertTrue(result.detectedAnomalies.isEmpty())
    }

    @Test
    fun testSyntheticDeepfakeVirtualCamera_TestB() {
        // Scenario Test B: Virtual camera feed with an AI deepfake / face-swap avatar
        val result = detector.suspectSyntheticFace()

        assertTrue("Deepfake avatar must be flagged as synthetic", result.isSynthetic)
        assertEquals(0.96f, result.deepfakeConfidence, 0.001f)
        assertEquals(0.96f, result.anomalyScore, 0.001f)
        assertEquals(0.04f, result.biometricAuthenticityScore, 0.001f)
        assertEquals(0.95f, result.azimuthalLatticeAnomaly, 0.001f)
        assertEquals(0.92f, result.boundaryBlurWarping, 0.001f)
        assertEquals(0.88f, result.blinkPeriodicityAnomaly, 0.001f)
        assertEquals(0.91f, result.skinTextureSmoothness, 0.001f)
        assertEquals("SYNTHETIC_DEEPFAKE", result.verdict)
        assertEquals(
            "🚨 CRITICAL VIDEO DEEPFAKE DETECTED (96%): Facial boundary warping & 2D-FFT high-frequency synthetic grid pattern identified!",
            result.hudBadge
        )
        assertEquals(4, result.detectedAnomalies.size)
        assertTrue(result.detectedAnomalies.any { it.contains("2D-FFT") })
        assertTrue(result.detectedAnomalies.any { it.contains("boundary warping") })
    }

    @Test
    fun testBlinkCadencePeriodicity() {
        // 1. Natural Poisson distributed blinks: irregular spacing
        val detectorHuman = VideoDeepfakeDetector()
        val baseTime = 100000L
        detectorHuman.recordBlink(baseTime)
        detectorHuman.recordBlink(baseTime + 2800L)
        detectorHuman.recordBlink(baseTime + 7400L)
        detectorHuman.recordBlink(baseTime + 10100L)
        detectorHuman.recordBlink(baseTime + 15900L)
        val humanScore = detectorHuman.computeBlinkPeriodicityScore()
        assertTrue("Human irregular blinks must have low cadence anomaly (<0.20)", humanScore <= 0.20f)

        // 2. Robotic periodic blinks: exactly 3000ms apart (procedural avatar animation)
        val detectorRobot = VideoDeepfakeDetector()
        detectorRobot.recordBlink(baseTime)
        detectorRobot.recordBlink(baseTime + 3000L)
        detectorRobot.recordBlink(baseTime + 6000L)
        detectorRobot.recordBlink(baseTime + 9000L)
        detectorRobot.recordBlink(baseTime + 12000L)
        val robotScore = detectorRobot.computeBlinkPeriodicityScore()
        assertTrue("Robotic fixed interval blinks must trigger cadence anomaly (>0.70)", robotScore >= 0.70f)
    }

    @Test
    fun testOnDevicePixelAnalysis_DirectionalLattice() {
        val width = 64
        val height = 64

        // 1. Uniform smooth image -> authentic
        val smoothPixels = IntArray(width * height) { 0xFF708090.toInt() }
        val smoothResult = detector.inspectPixelsOnDevice(smoothPixels, width, height)
        assertFalse(smoothResult.isSynthetic)
        assertTrue(smoothResult.anomalyScore < 0.50f)

        // 2. Extreme axis-aligned grid noise simulating transposed convolution upscaler
        val gridPixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                gridPixels[y * width + x] = if (x % 2 == 0 && y % 2 == 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
            }
        }
        val gridResult = detector.inspectPixelsOnDevice(gridPixels, width, height)
        assertTrue(gridResult.azimuthalLatticeAnomaly >= 0.04f)
    }
}
