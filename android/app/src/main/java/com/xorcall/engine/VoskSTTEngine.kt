package com.xorcall.engine

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.*
import java.util.zip.ZipInputStream

/**
 * VoskSTTEngine — True On-Device Speech-to-Text Engine (Singleton)
 * 100% offline speech recognition using the bundled model-en.zip from APK assets.
 * No internet required. Works DURING active phone calls on Android 14.
 */
class VoskSTTEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "VoskSTT"
        private const val ASSET_ZIP_NAME = "model-en.zip"
        private const val MODEL_FOLDER_NAME = "model-en"
        private const val SAMPLE_RATE = 16000f

        @Volatile
        private var instance: VoskSTTEngine? = null

        fun getInstance(context: Context): VoskSTTEngine {
            return instance ?: synchronized(this) {
                instance ?: VoskSTTEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private var model: Model? = null
    private var recognizer: Recognizer? = null

    @Volatile
    var isReady: Boolean = false
        private set

    @Volatile
    var isUnpacking: Boolean = false
        private set

    var onTranscript: ((text: String, isFinal: Boolean) -> Unit)? = null
    var onModelReady: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val targetModelDir: File
        get() = File(context.filesDir, MODEL_FOLDER_NAME)

    fun isModelExtracted(): Boolean {
        return findModelDir(targetModelDir) != null
    }

    /**
     * Initializes Vosk model by extracting model-en.zip from APK assets (if not already done)
     * and loading the native Model + Recognizer into memory.
     */
    fun initialize(scope: CoroutineScope? = null) {
        if (isReady) return

        val launchScope = scope ?: CoroutineScope(Dispatchers.IO + SupervisorJob())
        launchScope.launch(Dispatchers.IO) {
            try {
                if (!isModelExtracted()) {
                    isUnpacking = true
                    Log.i(TAG, "Extracting bundled offline Vosk model zip from assets...")
                    extractModelZipFromAssets(context.assets, ASSET_ZIP_NAME, targetModelDir)
                    isUnpacking = false
                    Log.i(TAG, "Vosk model successfully extracted to ${targetModelDir.absolutePath}")
                }

                // Locate actual model directory (handles nested folders inside zip)
                val modelPath = findModelDir(targetModelDir)
                if (modelPath != null) {
                    model = Model(modelPath.absolutePath)
                    recognizer = Recognizer(model, SAMPLE_RATE).apply {
                        setPartialWords(true)
                    }
                    isReady = true
                    Log.i(TAG, "Vosk STT engine initialized successfully at ${modelPath.absolutePath}")
                    withContext(Dispatchers.Main) {
                        onModelReady?.invoke()
                    }
                } else {
                    Log.e(TAG, "Model directory not found after extraction")
                    onError?.invoke("Model files missing in assets")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Vosk model", e)
                isUnpacking = false
                onError?.invoke("Speech engine init error: ${e.message}")
            }
        }
    }

    private fun findModelDir(baseDir: File): File? {
        if (!baseDir.exists()) return null
        if (File(baseDir, "am").exists() || File(baseDir, "conf").exists()) return baseDir
        baseDir.listFiles()?.forEach { sub ->
            if (sub.isDirectory && (File(sub, "am").exists() || File(sub, "conf").exists())) {
                return sub
            }
        }
        if (baseDir.isDirectory && (baseDir.listFiles()?.isNotEmpty() == true)) {
            return baseDir
        }
        return null
    }

    private fun extractModelZipFromAssets(assetManager: AssetManager, zipAssetName: String, destDir: File) {
        destDir.mkdirs()
        assetManager.open(zipAssetName).use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
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
    }

    /**
     * Feeds raw PCM 16-bit mono 16kHz audio byte buffers into Vosk.
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
     * Feeds raw PCM 16-bit ShortArray from AudioRecord.
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
