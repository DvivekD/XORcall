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
    fun testEmptyOrSilentChunkHandledSafely() {
        val detector = AcousticSyntheticDetector()
        val silentChunk = FloatArray(1024) { 0f }
        val result = detector.analyzeAudioChunk(silentChunk)
        assertNotNull(result)
        assertTrue("Silent chunk is not voiced", !result.isVoicedSpeech)
    }
}
