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
 * Always-On Live Continuous Speech Transcription & Threat Interception Engine:
 * - Automatically activates as soon as the Shield is toggled ON.
 * - Streams live partial and final spoken words directly into the UI transcript box in real-time.
 * - Instantly evaluates spoken words against Kidnap, Death Threats, Extortion & Demographic Lexicons.
 * - Evaluates hardware PCM audio buffers for AI synthetic voice clone signatures.
 * - Self-healing continuous recognition loop that auto-recovers silently.
 */
class SherpaSTTManager(
    private val assetManager: AssetManager,
    private val context: Context? = null
) {
    private var isInitialized: Boolean = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening: Boolean = false
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
     * Starts Always-On Continuous Live Speech Recognition.
     * Continuously listens to the microphone and streams words to onLiveTranscriptUpdate.
     */
    fun startAlwaysOnLiveListening(onTranscript: (text: String, isEndpoint: Boolean) -> Unit) {
        val ctx = context ?: return
        this.onLiveTranscriptUpdate = onTranscript
        isListening = true

        mainHandler.post {
            createAndStartContinuousRecognizer(ctx)
        }
    }

    private fun createAndStartContinuousRecognizer(ctx: Context) {
        if (!isListening) return

        try {
            cleanupRecognizer()

            if (SpeechRecognizer.isRecognitionAvailable(ctx)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            // Seamless silent recovery: re-arm without popup or freeze
                            if (isListening) {
                                val delayMs = when (error) {
                                    SpeechRecognizer.ERROR_NO_MATCH,
                                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 150L
                                    SpeechRecognizer.ERROR_AUDIO -> 500L
                                    else -> 300L
                                }
                                mainHandler.postDelayed({
                                    if (isListening) {
                                        createAndStartContinuousRecognizer(ctx)
                                    }
                                }, delayMs)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotBlank()) {
                                onLiveTranscriptUpdate?.invoke(text, true)
                                val analysis = analyzeText(text, activeScope)
                                if (analysis.isScamDetected) {
                                    onThreatDetected?.invoke(analysis, text)
                                }
                            }
                            // Loop immediately for continuous conversation listening
                            if (isListening) {
                                mainHandler.postDelayed({
                                    if (isListening) {
                                        startListeningIntent()
                                    }
                                }, 100L)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotBlank()) {
                                onLiveTranscriptUpdate?.invoke(text, false)
                                val analysis = analyzeText(text, activeScope)
                                if (analysis.isScamDetected) {
                                    onThreatDetected?.invoke(analysis, text)
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
                startListeningIntent()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (isListening) {
                mainHandler.postDelayed({
                    if (isListening) createAndStartContinuousRecognizer(ctx)
                }, 800L)
            }
        }
    }

    private fun startListeningIntent() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            val ctx = context ?: return
            if (isListening) {
                mainHandler.postDelayed({
                    if (isListening) createAndStartContinuousRecognizer(ctx)
                }, 400L)
            }
        }
    }

    fun stopAlwaysOnLiveListening() {
        isListening = false
        mainHandler.post {
            cleanupRecognizer()
        }
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
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

    /**
     * Processes amplified PCM float audio buffer from hardware for acoustic vocoder & voice clone detection.
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

    fun release() {
        stopAlwaysOnLiveListening()
        syntheticDetector.reset()
        isInitialized = false
    }
}
