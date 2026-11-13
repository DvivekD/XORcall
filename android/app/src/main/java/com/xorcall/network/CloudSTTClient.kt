package com.xorcall.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/**
 * CloudSTTClient
 * High-performance, non-blocking HTTP client for:
 * 1. Groq LPU Whisper Turbo (whisper-large-v3-turbo) transcription
 * 2. XORcall Central FastAPI Brain fallback (/api/stt/transcribe-pcm)
 * 3. Central Video/Voice Forensics Engine (/api/voice/inspect-pcm)
 */
class CloudSTTClient(
    private var groqApiKey: String = "",
    private var backendUrl: String = "https://api.xorcall.kanyarasi.live"
) {

    companion object {
        private const val TAG = "CloudSTTClient"
        private const val GROQ_WHISPER_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    fun updateConfig(apiKey: String, serverUrl: String) {
        this.groqApiKey = apiKey.trim()
        this.backendUrl = serverUrl.trim().trimEnd('/')
    }

    /**
     * Converts raw 16-bit Mono Linear PCM bytes into standard 44-byte WAV container.
     */
    fun pcmToWav(pcmBytes: ByteArray, sampleRate: Int = 16000, channels: Short = 1, bitsPerSample: Short = 16): ByteArray {
        val totalAudioLen = pcmBytes.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        buffer.putInt(4, totalDataLen)
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt subchunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        buffer.putInt(16, 16) // Subchunk1Size for PCM
        buffer.putShort(20, 1) // AudioFormat: 1 = PCM
        buffer.putShort(22, channels)
        buffer.putInt(24, sampleRate)
        buffer.putInt(28, byteRate)
        buffer.putShort(32, blockAlign)
        buffer.putShort(34, bitsPerSample)

        // data subchunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        buffer.putInt(40, totalAudioLen)

        val out = ByteArray(44 + totalAudioLen)
        System.arraycopy(header, 0, out, 0, 44)
        System.arraycopy(pcmBytes, 0, out, 44, totalAudioLen)
        return out
    }

    /**
     * Asynchronously transcribes a PCM audio buffer.
     * Tries Direct Groq Whisper Turbo first; falls back to Backend STT endpoint.
     */
    fun transcribePcmAsync(
        scope: CoroutineScope,
        pcmBytes: ByteArray,
        sampleRate: Int = 16000,
        onTranscriptReceived: (transcript: String, source: String) -> Unit
    ) {
        if (pcmBytes.size < 4000) return // Skip tiny clicks

        scope.launch(Dispatchers.IO) {
            try {
                if (groqApiKey.isNotBlank()) {
                    val wavData = pcmToWav(pcmBytes, sampleRate)
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "file",
                            "speech.wav",
                            wavData.toRequestBody("audio/wav".toMediaType())
                        )
                        .addFormDataPart("model", "whisper-large-v3-turbo")
                        .addFormDataPart("response_format", "json")
                        .addFormDataPart("language", "en")
                        .addFormDataPart("temperature", "0.0")
                        .addFormDataPart(
                            "prompt",
                            "extortion, 5 lakhs, digital arrest, police custody, CBI, narcotics, MDMA, FedEx parcel, RBI escrow, kidnapping, transfer money, bail amount, murder, ICU, hospital, court order, UPI"
                        )
                        .build()

                    val request = Request.Builder()
                        .url(GROQ_WHISPER_URL)
                        .addHeader("Authorization", "Bearer $groqApiKey")
                        .post(requestBody)
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string() ?: ""
                            val json = JSONObject(bodyStr)
                            val text = json.optString("text", "").trim()
                            if (text.isNotBlank()) {
                                onTranscriptReceived(text, "Groq Whisper Turbo")
                                return@launch
                            }
                        } else {
                            Log.w(TAG, "Groq Whisper STT HTTP ${response.code}: ${response.message}")
                        }
                    }
                }

                // Fallback to Backend FastAPI Brain if reachable
                if (backendUrl.isNotBlank()) {
                    val multipart = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "file",
                            "audio.pcm",
                            pcmBytes.toRequestBody("application/octet-stream".toMediaType())
                        )
                        .build()

                    val request = Request.Builder()
                        .url("$backendUrl/api/stt/transcribe-pcm")
                        .post(multipart)
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string() ?: ""
                            val json = JSONObject(bodyStr)
                            val text = json.optString("transcript", "").trim()
                            if (text.isNotBlank()) {
                                onTranscriptReceived(text, "Cloud Brain Whisper")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Cloud STT non-critical dispatch error: ${e.message}")
            }
        }
    }

    /**
     * Inspects a PCM audio buffer against the Backend Deepfake Voice Forensics Engine.
     */
    fun inspectVoiceAsync(
        scope: CoroutineScope,
        pcmBytes: ByteArray,
        onResult: (isSynthetic: Boolean, confidence: Float, anomalies: List<String>) -> Unit
    ) {
        if (backendUrl.isBlank() || pcmBytes.size < 4000) return

        scope.launch(Dispatchers.IO) {
            try {
                val multipart = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        "voice.pcm",
                        pcmBytes.toRequestBody("application/octet-stream".toMediaType())
                    )
                    .build()

                val request = Request.Builder()
                    .url("$backendUrl/api/voice/inspect-pcm")
                    .post(multipart)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val json = JSONObject(bodyStr)
                        val isSynthetic = json.optBoolean("is_cloned_voice", false)
                        val score = json.optDouble("voice_clone_score", 0.0).toFloat()
                        val anomaliesJson = json.optJSONArray("detected_anomalies")
                        val anomalies = mutableListOf<String>()
                        if (anomaliesJson != null) {
                            for (i in 0 until anomaliesJson.length()) {
                                anomalies.add(anomaliesJson.getString(i))
                            }
                        }
                        onResult(isSynthetic, score, anomalies)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Cloud Voice Forensics non-critical error: ${e.message}")
            }
        }
    }
}
