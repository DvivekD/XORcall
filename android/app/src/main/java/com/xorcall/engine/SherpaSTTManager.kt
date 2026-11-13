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
    val syntheticConfidence: Float = 0f
)

/**
 * SherpaSTTManager
 * On-Device Speech Transcription & Threat Interception Engine:
 * - Powered by VoskSTTEngine (100% on-device, offline, works DURING phone calls on Android 14)
 * - Streams live partial and final words directly into UI & background alert system
 * - Instantly evaluates spoken words against Kidnap, Death Threats, Extortion & Demographic Lexicons
 * - Analyzes raw PCM audio for AI synthetic voice clone signatures via AcousticSyntheticDetector
 */
class SherpaSTTManager(
    private val assetManager: AssetManager,
    private val context: Context? = null
) {
    private var isInitialized: Boolean = false
    private val syntheticDetector = AcousticSyntheticDetector()
    private var latestAcousticResult: AcousticAnalysisResult? = null

    var voskEngine: VoskSTTEngine? = null
        private set

    val isVoskReady: Boolean get() = voskEngine?.isReady == true
    val isDownloading: Boolean get() = voskEngine?.isDownloading == true
    val downloadProgress: Float get() = voskEngine?.downloadProgress ?: 0f

    var activeScope: DemographicVector = DemographicVector.ALL
    var onThreatDetected: ((analysis: ThreatAnalysis, transcript: String) -> Unit)? = null
    var onAcousticResult: ((result: AcousticAnalysisResult) -> Unit)? = null
    var onLiveTranscriptUpdate: ((transcript: String, isEndpoint: Boolean) -> Unit)? = null
    var onModelDownloadProgress: ((Float) -> Unit)? = null
    var onModelReady: (() -> Unit)? = null

    fun initialize(modelPath: String = "vosk-model-small-en-us-0.15") {
        if (isInitialized) return
        val ctx = context ?: return
        voskEngine = VoskSTTEngine(ctx).apply {
            onTranscript = { text, isFinal ->
                handleIncomingTranscript(text, isFinal)
            }
            onDownloadProgress = { progress ->
                this@SherpaSTTManager.onModelDownloadProgress?.invoke(progress)
            }
            onModelReady = {
                this@SherpaSTTManager.onModelReady?.invoke()
            }
        }
        isInitialized = true
    }

    /**
     * Initializes Vosk on-device model. If not yet downloaded, downloads it asynchronously.
     */
    fun initializeVosk(scope: CoroutineScope) {
        if (voskEngine == null) {
            initialize()
        }
        val engine = voskEngine ?: return
        if (engine.isModelDownloaded()) {
            engine.initialize()
        } else {
            engine.downloadModel(scope)
        }
    }

    private fun handleIncomingTranscript(text: String, isFinal: Boolean) {
        if (text.isBlank()) return

        // 1. Notify UI
        onLiveTranscriptUpdate?.invoke(text, isFinal)

        // 2. Broadcast for UI receivers
        context?.let { ctx ->
            val transcriptIntent = Intent("com.xorcall.TRANSCRIPT_UPDATE").apply {
                putExtra("text", text)
                putExtra("isFinal", isFinal)
                setPackage(ctx.packageName)
            }
            ctx.sendBroadcast(transcriptIntent)
        }

        // 3. Evaluate threat against active lexicon
        val analysis = analyzeText(text, activeScope)
        if (analysis.isScamDetected) {
            onThreatDetected?.invoke(analysis, text)
        }
    }

    /**
     * Feed raw PCM 16-bit ShortArray from AudioRecord.
     * Feeds directly to Vosk for STT and AcousticSyntheticDetector for AI voice clone detection.
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
                syntheticConfidence = acousticResult.syntheticConfidence
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
                syntheticConfidence = acousticResult.syntheticConfidence
            )
            onThreatDetected?.invoke(analysis, alertText)
        }
    }

    /**
     * Starts live listening listener.
     */
    fun startAlwaysOnLiveListening(onTranscript: (text: String, isEndpoint: Boolean) -> Unit) {
        this.onLiveTranscriptUpdate = onTranscript
        if (voskEngine == null) {
            initialize()
        }
        if (voskEngine?.isModelDownloaded() == true && voskEngine?.isReady == false) {
            voskEngine?.initialize()
        }
    }

    fun stopAlwaysOnLiveListening() {
        // Vosk operates continuously with audio feeds
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
            syntheticConfidence = aiConfidence
        )
    }

    fun release() {
        voskEngine?.release()
        voskEngine = null
        syntheticDetector.reset()
        isInitialized = false
    }
}
