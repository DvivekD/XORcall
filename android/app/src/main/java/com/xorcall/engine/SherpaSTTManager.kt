package com.xorcall.engine

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import com.xorcall.network.CloudSTTClient
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

data class ThreatAnalysis(
    val isScamDetected: Boolean,
    val matchedKeywords: List<String>,
    val riskLevel: RiskLevel,
    val warningMessage: String,
    val demographicVector: DemographicVector = DemographicVector.ALL,
    val isSyntheticVoice: Boolean = false,
    val syntheticConfidence: Float = 0f,
    val normalizedTranscript: String = ""
)

/**
 * SherpaSTTManager
 * Coordinates Speech Transcription & Multi-Demographic Threat Interception:
 * - Dual Hybrid STT: High-Accuracy Cloud Whisper Turbo (Groq LPU) + Instant On-Device Vosk Fallback
 * - Integrates PhoneticSpeechNormalizer for robust Indian English extortion detection ("5 lakhs", "kidnap", "cbi")
 * - Streams live spoken words directly into UI transcript box in real-time
 * - Evaluates words against Extortion, Kidnap, Death Threats, and Demographic Vectors
 * - Scientific Acoustic Synthetic Voice Detector (FFT + Jitter + Wiener Entropy)
 */
class SherpaSTTManager(
    private val assetManager: AssetManager,
    private val context: Context? = null
) {
    private var isInitialized: Boolean = false
    private val syntheticDetector = AcousticSyntheticDetector()
    private var latestAcousticResult: AcousticAnalysisResult? = null

    private var voskEngine: VoskSTTEngine? = null
    val cloudSttClient = CloudSTTClient()
    private val cloudScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val isVoskReady: Boolean get() = voskEngine?.isReady == true
    val isUnpacking: Boolean get() = voskEngine?.isUnpacking == true
    val errorMessage: String? get() = voskEngine?.errorMessage

    var activeScope: DemographicVector = DemographicVector.ALL
    var onThreatDetected: ((analysis: ThreatAnalysis, transcript: String) -> Unit)? = null
    var onAcousticResult: ((result: AcousticAnalysisResult) -> Unit)? = null
    var onLiveTranscriptUpdate: ((transcript: String, isEndpoint: Boolean) -> Unit)? = null
    var onModelReady: (() -> Unit)? = null

    // Cloud STT Accumulator Buffer (~2 seconds of speech @ 16kHz 16-bit mono = 64,000 bytes)
    private val cloudAudioBuffer = ByteArrayOutputStream(64000)
    private var voicedChunksInCurrentBuffer = 0
    private var lastSpeechTimestamp = 0L

    private val transcriptListener: (String, Boolean) -> Unit = { rawText, isFinal ->
        handleIncomingTranscript(rawText, isFinal)
    }

    fun updateCloudConfig(apiKey: String, serverUrl: String) {
        cloudSttClient.updateConfig(apiKey, serverUrl)
    }

    fun initialize(modelPath: String = "model-en") {
        if (isInitialized) return
        val ctx = context ?: return

        // Read stored configuration from settings
        try {
            val settings = GuardianSettingsManager(ctx)
            cloudSttClient.updateConfig(settings.groqApiKey, settings.backendUrl)
        } catch (_: Exception) {}

        voskEngine = VoskSTTEngine.getInstance(ctx).apply {
            addTranscriptListener(transcriptListener)
            onModelReady = {
                this@SherpaSTTManager.onModelReady?.invoke()
            }
        }
        voskEngine?.initialize()
        isInitialized = true
    }

    fun initializeVosk(scope: CoroutineScope) {
        if (voskEngine == null) {
            initialize()
        }
        voskEngine?.initialize(scope)
    }

    fun handleIncomingTranscript(rawText: String, isFinal: Boolean) {
        if (rawText.isBlank()) return

        // Normalize raw speech (e.g. "five lacks", "five lock" -> "5 lakh")
        val normalized = PhoneticSpeechNormalizer.normalize(rawText)
        val displayText = if (normalized.isNotBlank()) normalized else rawText

        // 1. Notify UI listener
        onLiveTranscriptUpdate?.invoke(displayText, isFinal)

        // 2. Broadcast for UI receivers
        context?.let { ctx ->
            val transcriptIntent = Intent("com.xorcall.TRANSCRIPT_UPDATE").apply {
                putExtra("text", displayText)
                putExtra("rawText", rawText)
                putExtra("isFinal", isFinal)
                setPackage(ctx.packageName)
            }
            ctx.sendBroadcast(transcriptIntent)
        }

        // 3. Evaluate threat against active lexicon (using both raw and normalized)
        val analysis = analyzeText(displayText, activeScope)
        if (analysis.isScamDetected) {
            onThreatDetected?.invoke(analysis, displayText)
        }
    }

    /**
     * Feeds raw PCM 16-bit ShortArray from AudioRecord.
     * Evaluated concurrently by:
     * 1. On-Device Vosk STT (Instant offline local baseline)
     * 2. Cloud Whisper Turbo (High-accuracy vernacular transcription dispatch)
     * 3. Scientific AcousticSyntheticDetector (FFT spectral roll-off + micro-jitter)
     */
    fun acceptAudioChunkRaw(pcmShorts: ShortArray, count: Int) {
        if (count <= 0) return

        // 1. On-Device Speech Recognition via Vosk
        voskEngine?.acceptWaveformShorts(pcmShorts, count)

        // 2. Acoustic Voice Harmonics & Clone Detection
        val floatSamples = FloatArray(count) { pcmShorts[it] / 32768.0f }
        val acousticResult = syntheticDetector.analyzeAudioChunk(floatSamples)
        latestAcousticResult = acousticResult
        onAcousticResult?.invoke(acousticResult)

        if (acousticResult.isSyntheticVoice) {
            val alertText = acousticResult.analysisReason
            val analysis = ThreatAnalysis(
                isScamDetected = true,
                matchedKeywords = listOf("AI_VOICE_CLONE"),
                riskLevel = RiskLevel.CRITICAL,
                warningMessage = "🚨 CRITICAL: AI SYNTHETIC VOICE CLONE DETECTED! Voice is artificially generated.",
                demographicVector = activeScope,
                isSyntheticVoice = true,
                syntheticConfidence = acousticResult.syntheticConfidence,
                normalizedTranscript = alertText
            )
            onThreatDetected?.invoke(analysis, alertText)
        }

        // 3. Cloud STT Audio Chunk Accumulation
        // Convert shorts to bytes
        val byteChunk = ByteArray(count * 2)
        var sumSquares = 0.0
        for (i in 0 until count) {
            val s = pcmShorts[i]
            byteChunk[i * 2] = (s.toInt() and 0xFF).toByte()
            byteChunk[i * 2 + 1] = (s.toInt() shr 8 and 0xFF).toByte()
            sumSquares += s.toDouble() * s.toDouble()
        }
        val rms = sqrt(sumSquares / count) / 32768.0

        val now = System.currentTimeMillis()
        if (rms > 0.012) {
            voicedChunksInCurrentBuffer++
            lastSpeechTimestamp = now
        }

        synchronized(cloudAudioBuffer) {
            cloudAudioBuffer.write(byteChunk)
            val currentBytes = cloudAudioBuffer.size()

            // Sentence-Aligned Adaptive VAD for Groq Whisper Turbo LPU:
            // Dispatches on natural 250ms speech cadence pauses when buffer has >= 1.2s of speech,
            // or when buffer accumulates full conversational sentence (~2.2s / 70,000 bytes).
            val hasEnoughSpeech = voicedChunksInCurrentBuffer >= 3
            val isBufferFull = currentBytes >= 70000
            val isSpeechPaused = (now - lastSpeechTimestamp > 250) && currentBytes >= 38000

            if (hasEnoughSpeech && (isBufferFull || isSpeechPaused)) {
                val pcmToDispatch = cloudAudioBuffer.toByteArray()
                cloudAudioBuffer.reset()
                voicedChunksInCurrentBuffer = 0

                cloudSttClient.transcribePcmAsync(cloudScope, pcmToDispatch) { transcript, source ->
                    if (transcript.isNotBlank()) {
                        handleIncomingTranscript(transcript, true)
                    }
                }
            } else if (currentBytes > 128000) {
                // Safeguard against unbound buffer growth during continuous loud noise
                cloudAudioBuffer.reset()
                voicedChunksInCurrentBuffer = 0
            }
        }
    }

    /**
     * Backwards-compatible float audio acceptor.
     */
    fun acceptAudioChunk(samples: FloatArray, onTranscript: ((text: String, isEndpoint: Boolean) -> Unit)? = null) {
        if (samples.isEmpty()) return

        val acousticResult = syntheticDetector.analyzeAudioChunk(samples)
        latestAcousticResult = acousticResult
        onAcousticResult?.invoke(acousticResult)

        if (acousticResult.isSyntheticVoice) {
            val alertText = acousticResult.analysisReason
            val analysis = ThreatAnalysis(
                isScamDetected = true,
                matchedKeywords = listOf("AI_VOICE_CLONE"),
                riskLevel = RiskLevel.CRITICAL,
                warningMessage = "🚨 CRITICAL: AI SYNTHETIC VOICE DETECTED! Voice is artificially generated.",
                demographicVector = activeScope,
                isSyntheticVoice = true,
                syntheticConfidence = acousticResult.syntheticConfidence,
                normalizedTranscript = alertText
            )
            onThreatDetected?.invoke(analysis, alertText)
        }
    }

    fun startAlwaysOnLiveListening(onTranscript: (text: String, isEndpoint: Boolean) -> Unit) {
        this.onLiveTranscriptUpdate = onTranscript
        if (voskEngine == null) {
            initialize()
        }
    }

    fun stopAlwaysOnLiveListening() {
        // Continuous Vosk audio stream processing
    }

    /**
     * Evaluates text against DemographicThreatLexicon and current AI synthetic voice status.
     */
    fun analyzeText(text: String, scope: DemographicVector = activeScope): ThreatAnalysis {
        val result = DemographicThreatLexicon.evaluate(text, scope)
        val isAiVoice = latestAcousticResult?.isSyntheticVoice ?: false
        val aiConfidence = latestAcousticResult?.syntheticConfidence ?: 0f

        val effectiveRisk = if (isAiVoice && result.riskLevel == RiskLevel.SAFE) {
            RiskLevel.CRITICAL
        } else if (result.riskLevel != RiskLevel.SAFE) {
            RiskLevel.CRITICAL
        } else {
            result.riskLevel
        }

        val enrichedMessage = if (isAiVoice) {
            "${result.threatSummary} [AI SYNTHETIC VOICE DETECTED (${(aiConfidence * 100).toInt()}%)]"
        } else {
            result.threatSummary
        }

        return ThreatAnalysis(
            isScamDetected = effectiveRisk != RiskLevel.SAFE,
            matchedKeywords = result.matchedKeywords,
            riskLevel = effectiveRisk,
            warningMessage = enrichedMessage,
            demographicVector = result.matchedVector,
            isSyntheticVoice = isAiVoice,
            syntheticConfidence = aiConfidence,
            normalizedTranscript = text
        )
    }

    fun release() {
        voskEngine?.removeTranscriptListener(transcriptListener)
        syntheticDetector.reset()
        cloudScope.cancel()
        isInitialized = false
    }
}
