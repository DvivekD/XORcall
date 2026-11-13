package com.xorcall.engine

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.*

/**
 * SherpaSTTManager
 * Manages on-device neural transducer (Zipformer) speech recognition.
 * Feeds live 16kHz PCM audio chunks locally on CPU/NPU with <150ms latency.
 */
class SherpaSTTManager(private val assetManager: AssetManager) {
    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null

    fun initialize(modelPath: String = "sherpa-onnx-streaming-zipformer-en") {
        try {
            val config = OnlineRecognizerConfig(
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = "$modelPath/encoder-epoch-99-avg-1.onnx",
                        decoder = "$modelPath/decoder-epoch-99-avg-1.onnx",
                        joiner = "$modelPath/joiner-epoch-99-avg-1.onnx"
                    ),
                    tokens = "$modelPath/tokens.txt",
                    numThreads = 2,
                    provider = "cpu",
                    modelType = "zipformer"
                ),
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                enableEndpoint = true // Voice activity detection endpointing
            )
            recognizer = OnlineRecognizer(assetManager = assetManager, config = config)
            stream = recognizer?.createStream()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun acceptAudioChunk(samples: FloatArray, onTranscript: (text: String, isEndpoint: Boolean) -> Unit) {
        val currentRecognizer = recognizer ?: return
        val currentStream = stream ?: return

        currentStream.acceptWaveform(samples, sampleRate = 16000)

        while (currentRecognizer.isReady(currentStream)) {
            currentRecognizer.decode(currentStream)
        }

        val result = currentRecognizer.getResult(currentStream)
        if (result.text.isNotBlank()) {
            val isEndpoint = currentRecognizer.isEndpoint(currentStream)
            onTranscript(result.text, isEndpoint)
            if (isEndpoint) {
                currentRecognizer.reset(currentStream)
            }
        }
    }

    fun release() {
        stream?.release()
        recognizer?.release()
        stream = null
        recognizer = null
    }
}
