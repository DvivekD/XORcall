package com.xorcall.engine

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

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
 * In-Process Acoustic & Lexicon Threat Engine:
 * - 100% On-Device execution with zero external Google App IPC during active calls.
 * - Hardware PCM acoustic stream analysis for AI Voice Clones & Vocoder artifacts.
 * - High-speed Demographic Threat Lexicon evaluation (Universal, Student, Senior, Professional).
 * - Standalone microphone recognition for Actor Roleplay with graceful fallback.
 */
class SherpaSTTManager(
    private val assetManager: AssetManager,
    private val context: Context? = null
) {
    private var isInitialized: Boolean = false
    private var singleSessionRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val syntheticDetector = AcousticSyntheticDetector()
    private var latestAcousticResult: AcousticAnalysisResult? = null

    var activeScope: DemographicVector = DemographicVector.ALL
    var onThreatDetected: ((analysis: ThreatAnalysis, transcript: String) -> Unit)? = null
    var onAcousticResult: ((result: AcousticAnalysisResult) -> Unit)? = null
    var onLiveTranscriptUpdate: ((transcript: String, isEndpoint: Boolean) -> Unit)? = null

    fun initialize(modelPath: String = "sherpa-onnx-streaming-zipformer-en") {
        isInitialized = true
    }

    /**
     * Evaluates text against DemographicThreatLexicon and current AI synthetic voice status.
     */
    fun analyzeText(text: String, scope: DemographicVector = activeScope): ThreatAnalysis {
        val result = DemographicThreatLexicon.evaluate(text, scope)
        val isAiVoice = latestAcousticResult?.isSyntheticVoice ?: false
        val aiConfidence = latestAcousticResult?.syntheticConfidence ?: 0f

        val effectiveRisk = if (isAiVoice && result.riskLevel == RiskLevel.SAFE) {
            RiskLevel.WARN
        } else if (isAiVoice && result.riskLevel == RiskLevel.WARN) {
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

    /**
     * Processes amplified PCM float audio buffer from hardware for acoustic vocoder & voice clone detection.
     * Evaluates biological vocal cord micro-jitter and high-frequency spectral roll-off (>8kHz boundary).
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
                warningMessage = "🚨 CRITICAL: AI SYNTHETIC VOICE DETECTED ON CALL! Voice is artificially generated.",
                demographicVector = activeScope,
                isSyntheticVoice = true,
                syntheticConfidence = acousticResult.syntheticConfidence
            )
            onThreatDetected?.invoke(analysis, alertText)
        }
    }

    /**
     * Single session speech recognition for actor roleplay tests (when not in call).
     */
    fun startSingleRoleplaySession(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val ctx = context ?: return
        mainHandler.post {
            try {
                destroySingleRecognizer()
                if (SpeechRecognizer.isRecognitionAvailable(ctx)) {
                    singleSessionRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {}
                            override fun onBeginningOfSpeech() {}
                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {}
                            override fun onError(error: Int) {
                                val msg = when (error) {
                                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording busy or in call"
                                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                                    SpeechRecognizer.ERROR_NETWORK -> "Network offline"
                                    else -> "Speech recognition error code $error"
                                }
                                onError(msg)
                                destroySingleRecognizer()
                            }
                            override fun onResults(results: Bundle?) {
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                val text = matches?.firstOrNull() ?: ""
                                onResult(text)
                                destroySingleRecognizer()
                            }
                            override fun onPartialResults(partialResults: Bundle?) {}
                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    }
                    singleSessionRecognizer?.startListening(intent)
                } else {
                    onError("Speech recognition service not available on this device")
                }
            } catch (e: Exception) {
                onError("Failed to start microphone: ${e.message}")
            }
        }
    }

    private fun destroySingleRecognizer() {
        try {
            singleSessionRecognizer?.cancel()
            singleSessionRecognizer?.destroy()
        } catch (_: Exception) {}
        singleSessionRecognizer = null
    }

    fun release() {
        destroySingleRecognizer()
        syntheticDetector.reset()
        isInitialized = false
    }
}
