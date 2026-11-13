package com.xorcall.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * VoskSTTEngine — True On-Device Speech-to-Text Engine
 * Runs entirely offline using the Vosk library + vosk-model-small-en-us-0.15.
 * Processes raw PCM 16-bit mono 16kHz audio byte buffers → text.
 * No Google dependency. No IPC. No cloud. Works DURING active phone calls.
 */
class VoskSTTEngine(private val context: Context) {

    companion object {
        private const val TAG = "VoskSTT"
        private const val MODEL_NAME = "vosk-model-small-en-us-0.15"
        private const val MODEL_URL = "https://alphacephei.com/vosk/models/$MODEL_NAME.zip"
        private const val SAMPLE_RATE = 16000f
    }

    private var model: Model? = null
    private var recognizer: Recognizer? = null

    @Volatile
    var isReady: Boolean = false
        private set

    @Volatile
    var isDownloading: Boolean = false
        private set

    var downloadProgress: Float = 0f
        private set

    var onTranscript: ((text: String, isFinal: Boolean) -> Unit)? = null
    var onModelReady: (() -> Unit)? = null
    var onDownloadProgress: ((Float) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val modelDir: File
        get() = File(context.filesDir, MODEL_NAME)

    private fun resolveActualModelDir(): File? {
        val direct = modelDir
        if (!direct.exists()) return null
        if (File(direct, "am").exists() || File(direct, "conf").exists()) return direct
        // Check subdirectories in case zip had nested folder
        direct.listFiles()?.forEach { sub ->
            if (sub.isDirectory && (File(sub, "am").exists() || File(sub, "conf").exists())) {
                return sub
            }
        }
        if (direct.isDirectory && (direct.listFiles()?.isNotEmpty() == true)) {
            return direct
        }
        return null
    }

    fun isModelDownloaded(): Boolean {
        return resolveActualModelDir() != null
    }

    /**
     * Initialize Vosk model from local storage.
     * Call downloadModel() first if not yet downloaded.
     */
    fun initialize(): Boolean {
        if (isReady) return true

        return try {
            val actualDir = resolveActualModelDir()
            if (actualDir == null) {
                Log.w(TAG, "Model not downloaded or resolved yet")
                return false
            }

            model = Model(actualDir.absolutePath)
            recognizer = Recognizer(model, SAMPLE_RATE).apply {
                setPartialWords(true)
            }
            isReady = true
            Log.i(TAG, "Vosk STT engine initialized successfully at ${actualDir.absolutePath}")
            onModelReady?.invoke()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Vosk model", e)
            onError?.invoke("Failed to load speech model: ${e.message}")
            false
        }
    }

    /**
     * Downloads the Vosk model zip from the official server, extracts it,
     * and stores it in app-internal storage (~40MB download, ~67MB extracted).
     */
    fun downloadModel(scope: CoroutineScope) {
        if (isDownloading || isModelDownloaded()) {
            if (isModelDownloaded()) initialize()
            return
        }

        isDownloading = true
        downloadProgress = 0f

        scope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "Downloading Vosk model from $MODEL_URL")
                val zipFile = File(context.cacheDir, "$MODEL_NAME.zip")

                // Download with progress
                val url = URL(MODEL_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30_000
                connection.readTimeout = 60_000
                connection.connect()

                val totalBytes = connection.contentLength.toLong()
                var downloadedBytes = 0L

                connection.inputStream.buffered().use { input ->
                    FileOutputStream(zipFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            downloadProgress = if (totalBytes > 0) {
                                (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 0.9f)
                            } else {
                                0.5f
                            }
                            onDownloadProgress?.invoke(downloadProgress)
                        }
                    }
                }

                Log.i(TAG, "Download complete. Extracting...")
                downloadProgress = 0.9f
                onDownloadProgress?.invoke(0.9f)

                // Extract zip
                extractZip(zipFile, context.filesDir)
                zipFile.delete()

                downloadProgress = 1.0f
                onDownloadProgress?.invoke(1.0f)
                Log.i(TAG, "Model extracted successfully to ${modelDir.absolutePath}")

                // Initialize the model
                withContext(Dispatchers.Main) {
                    initialize()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Model download failed", e)
                onError?.invoke("Model download failed: ${e.message}")
            } finally {
                isDownloading = false
            }
        }
    }

    /**
     * Feed raw PCM 16-bit mono 16kHz audio bytes to Vosk for recognition.
     * This is called from the AudioRecord loop in CallMonitorService.
     * Returns immediately — callbacks fire on transcript detection.
     */
    fun acceptWaveform(pcmBytes: ByteArray, length: Int) {
        if (!isReady || recognizer == null) return

        try {
            val isFinal = recognizer!!.acceptWaveForm(pcmBytes, length)

            if (isFinal) {
                val resultJson = recognizer!!.result
                val text = parseVoskText(resultJson)
                if (text.isNotBlank()) {
                    onTranscript?.invoke(text, true)
                }
            } else {
                val partialJson = recognizer!!.partialResult
                val text = parseVoskPartial(partialJson)
                if (text.isNotBlank()) {
                    onTranscript?.invoke(text, false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vosk acceptWaveform error", e)
        }
    }

    /**
     * Feed raw PCM 16-bit ShortArray (what AudioRecord.read() returns).
     * Converts to byte array for Vosk.
     */
    fun acceptWaveformShorts(pcmShorts: ShortArray, count: Int) {
        if (!isReady) return

        val bytes = ByteArray(count * 2)
        for (i in 0 until count) {
            val s = pcmShorts[i]
            bytes[i * 2] = (s.toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (s.toInt() shr 8 and 0xFF).toByte()
        }
        acceptWaveform(bytes, bytes.size)
    }

    private fun parseVoskText(json: String): String {
        return try {
            JSONObject(json).optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseVoskPartial(json: String): String {
        return try {
            JSONObject(json).optString("partial", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractZip(zipFile: File, destDir: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    fun release() {
        try {
            recognizer?.close()
            model?.close()
        } catch (_: Exception) {}
        recognizer = null
        model = null
        isReady = false
    }
}
