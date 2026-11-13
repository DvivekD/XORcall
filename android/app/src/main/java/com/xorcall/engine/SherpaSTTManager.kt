package com.xorcall.engine

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import kotlinx.coroutines.*

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
 * Coordinates On-Device Speech Transcription & Multi-Demographic Threat Interception:
 * - Powered by VoskSTTEngine (100% offline bundled model, works during live calls)
 * - Integrates PhoneticSpeechNormalizer for robust Indian English extortion detection ("5 lakhs", "kidnap", "cbi")
 * - Streams live spoken words directly into UI transcript box in real-time
 * - Evaluates words against Extortion, Kidnap, Death Threats, and Demographic Vectors
 * - Analyzes raw PCM audio for AI synthetic voice clone signatures
 */
class SherpaSTTManager(
    private val assetManager: AssetManager,
    private val context: Context? = null
) {
    private var isInitialized: Boolean = false
    private val syntheticDetector = AcousticSyntheticDetector()
    private var latestAcousticResult: AcousticAnalysisResult? = null

    private var voskEngine: VoskSTTEngine? = null

    val isVoskReady: Boolean get() = voskEngine?.isReady == true
    val isUnpacking: Boolean get() = voskEngine?.isUnpacking == true
    val errorMessage: String? get() = voskEngine?.errorMessage

    var activeScope: DemographicVector = DemographicVector.ALL
    var onThreatDetected: ((analysis: ThreatAnalysis, transcript: String) -> Unit)? = null
    var onAcousticResult: ((result: AcousticAnalysisResult) -> Unit)? = null
    var onLiveTranscriptUpdate: ((transcript: String, isEndpoint: Boolean) -> Unit)? = null
    var onModelReady: (() -> Unit)? = null

    private val transcriptListener: (String, Boolean) -> Unit = { rawText, isFinal ->
        handleIncomingTranscript(rawText, isFinal)
    }

    fun initialize(modelPath: String = "model-en") {
        if (isInitialized) return
        val ctx = context ?: return
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

    private fun handleIncomingTranscript(rawText: String, isFinal: Boolean) {
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
     * Evaluated concurrently by Vosk STT and AcousticSyntheticDetector.
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
            val alertText = "Synthetic neural vocoder clone speaking on line."
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

    /**
     * Backwards-compatible float audio acceptor.
     */
    fun acceptAudioChunk(samples: FloatArray, onTranscript: ((text: String, isEndpoint: Boolean) -> Unit)? = null) {
        if (samples.isEmpty()) return

        val acousticResult = syntheticDetector.analyzeAudioChunk(samples)
        latestAcousticResult = acousticResult
        onAcousticResult?.invoke(acousticResult)

        if (acousticResult.isSyntheticVoice) {
            val alertText = "Synthetic neural vocoder clone speaking on line."
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
        isInitialized = false
    }
}
