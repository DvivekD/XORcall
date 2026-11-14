package com.xorcall.engine

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AcousticSyntheticDetectorTest {

    @Test
    fun testSyntheticToneDetection() {
        val detector = AcousticSyntheticDetector()
        val sampleRate = 16000f
        val f0 = 160f // 160 Hz fundamental

        // Simulate 8 consecutive 1024-sample chunks of neural vocoder (locked spline, low jitter)
        var lastResult: AcousticAnalysisResult? = null
        for (chunkIdx in 0 until 8) {
            val chunk = FloatArray(1024) { i ->
                val t = (chunkIdx * 1024 + i) / sampleRate
                (sin(2 * PI * f0 * t) + 0.5 * sin(4 * PI * f0 * t)).toFloat() * 0.4f
            }
            lastResult = detector.analyzeAudioChunk(chunk)
        }

        assertNotNull(lastResult)
        assertTrue("Synthetic tone must have high harmonic purity (> 0.65)", lastResult!!.pitchJitterEstimate > 0.65f)
        assertTrue("Synthetic confidence must be elevated", lastResult.syntheticConfidence > 0.45f)
    }

    @Test
    fun testTargetAiCloneModeElevatesDetection() {
        val detector = AcousticSyntheticDetector().apply {
            isTargetAiClone = true
        }
        val sampleRate = 16000f
        val f0 = 160f
        var lastResult: AcousticAnalysisResult? = null
        for (chunkIdx in 0 until 5) {
            val chunk = FloatArray(1024) { i ->
                val t = (chunkIdx * 1024 + i) / sampleRate
                (sin(2 * PI * f0 * t) + 0.5 * sin(4 * PI * f0 * t)).toFloat() * 0.4f
            }
            lastResult = detector.analyzeAudioChunk(chunk)
        }
        assertNotNull(lastResult)
        assertTrue("Target AI clone mode must trigger synthetic detection", lastResult!!.isSyntheticVoice)
        assertTrue("Synthetic confidence must be >= 0.75", lastResult.syntheticConfidence >= 0.75f)
    }

    @Test
    fun testLoudspeakerAirGapVsProximityDetection() {
        val detector = AcousticSyntheticDetector()
        val sampleRate = 16000f

        // 1. Simulate loudspeaker replay (ChatGPT from laptop): 500 Hz + 1000 Hz with ZERO sub-180Hz energy
        var laptopResult: AcousticAnalysisResult? = null
        for (chunkIdx in 0 until 6) {
            val chunk = FloatArray(1024) { i ->
                val t = (chunkIdx * 1024 + i) / sampleRate
                (sin(2 * PI * 500.0 * t) + 0.6 * sin(2 * PI * 1000.0 * t)).toFloat() * 0.4f
            }
            laptopResult = detector.analyzeAudioChunk(chunk)
        }
        assertNotNull(laptopResult)
        assertTrue("Laptop speaker without sub-180Hz energy must have low sub-bass ratio (<0.15)", laptopResult!!.subBassProximityRatio < 0.15f)

        // 2. Simulate authentic human speech with 120 Hz near-field proximity resonance
        val detectorHuman = AcousticSyntheticDetector()
        var humanResult: AcousticAnalysisResult? = null
        for (chunkIdx in 0 until 6) {
            val chunk = FloatArray(1024) { i ->
                val t = (chunkIdx * 1024 + i) / sampleRate
                (1.2 * sin(2 * PI * 120.0 * t) + 0.4 * sin(2 * PI * 400.0 * t)).toFloat() * 0.4f
            }
            humanResult = detectorHuman.analyzeAudioChunk(chunk)
        }
        assertNotNull(humanResult)
        assertTrue("Direct human mouth proximity must yield elevated sub-bass ratio (>0.15)", humanResult!!.subBassProximityRatio > 0.15f)
    }

    @Test
    fun testEmptyOrSilentChunkHandledSafely() {
        val detector = AcousticSyntheticDetector()
        val silentChunk = FloatArray(1024) { 0f }
        val result = detector.analyzeAudioChunk(silentChunk)
        assertNotNull(result)
        assertTrue("Silent chunk is not voiced", !result.isVoicedSpeech)
    }
}
