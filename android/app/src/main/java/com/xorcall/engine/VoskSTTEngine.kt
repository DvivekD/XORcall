package com.xorcall.engine

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.*
import java.util.concurrent.CopyOnWriteArrayList
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
        private const val MARKER_FILE_NAME = ".extraction_complete"

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
    private val recognizerLock = Any()

    @Volatile
    var isReady: Boolean = false
        private set

    @Volatile
    var isUnpacking: Boolean = false
        private set

    @Volatile
    var errorMessage: String? = null
        private set

    private val transcriptListeners = CopyOnWriteArrayList<(text: String, isFinal: Boolean) -> Unit>()

    var onTranscript: ((text: String, isFinal: Boolean) -> Unit)?
        get() = null
        set(value) {
            transcriptListeners.clear()
            if (value != null) {
                transcriptListeners.add(value)
            }
        }

    fun addTranscriptListener(listener: (text: String, isFinal: Boolean) -> Unit) {
        if (!transcriptListeners.contains(listener)) {
            transcriptListeners.add(listener)
        }
    }

    fun removeTranscriptListener(listener: (text: String, isFinal: Boolean) -> Unit) {
        transcriptListeners.remove(listener)
    }

    var onModelReady: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val targetModelDir: File
        get() = File(context.filesDir, MODEL_FOLDER_NAME)

    fun isModelExtracted(): Boolean {
        val marker = File(targetModelDir, MARKER_FILE_NAME)
        if (!marker.exists()) return false
        val modelDir = findModelDir(targetModelDir) ?: return false
        val finalMdl = File(modelDir, "am/final.mdl")
        return finalMdl.exists() && finalMdl.length() > 5_000_000L
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
                    // Clean destination if partially corrupted
                    if (targetModelDir.exists()) {
                        targetModelDir.deleteRecursively()
                    }
                    targetModelDir.mkdirs()
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
                    errorMessage = null
                    Log.i(TAG, "Vosk STT engine initialized successfully at ${modelPath.absolutePath}")
                    withContext(Dispatchers.Main) {
                        onModelReady?.invoke()
                    }
                } else {
                    val err = "Model files missing after extraction"
                    Log.e(TAG, err)
                    errorMessage = err
                    onError?.invoke(err)
                }
            } catch (e: Exception) {
                val err = "Speech engine init error: ${e.message}"
                Log.e(TAG, "Failed to initialize Vosk model", e)
                isUnpacking = false
                errorMessage = err
                onError?.invoke(err)
            }
        }
    }

    private fun findModelDir(baseDir: File): File? {
        if (!baseDir.exists()) return null
        if (File(baseDir, "am").exists() && File(baseDir, "conf").exists()) return baseDir
        baseDir.listFiles()?.forEach { sub ->
            if (sub.isDirectory && File(sub, "am").exists() && File(sub, "conf").exists()) {
                return sub
            }
        }
        return null
    }

    private fun extractModelZipFromAssets(assetManager: AssetManager, zipAssetName: String, destDir: File) {
        destDir.mkdirs()
        assetManager.open(zipAssetName).use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream, 65536)).use { zis ->
                var entry = zis.nextEntry
                val buffer = ByteArray(65536)
                while (entry != null) {
                    val outFile = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
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
        // Write completion marker file atomically
        File(destDir, MARKER_FILE_NAME).writeText("ready")
    }

    /**
     * Feeds raw PCM 16-bit mono 16kHz audio byte buffers into Vosk.
     * Synchronized on recognizerLock to prevent concurrent multi-threaded JNI calls
     * which trigger native SIGSEGV crashes in libvosk.so.
     */
    fun acceptWaveform(pcmBytes: ByteArray, length: Int) {
        if (!isReady || recognizer == null) return

        synchronized(recognizerLock) {
            try {
                val rec = recognizer ?: return
                val isFinal = rec.acceptWaveForm(pcmBytes, length)

                if (isFinal) {
                    val resultJson = rec.result
                    val text = parseVoskText(resultJson)
                    if (text.isNotBlank()) {
                        notifyTranscript(text, true)
                    }
                } else {
                    val partialJson = rec.partialResult
                    val text = parseVoskPartial(partialJson)
                    if (text.isNotBlank()) {
                        notifyTranscript(text, false)
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Vosk acceptWaveform error", e)
            }
        }
    }

    private fun notifyTranscript(text: String, isFinal: Boolean) {
        for (listener in transcriptListeners) {
            try {
                listener.invoke(text, isFinal)
            } catch (e: Throwable) {
                Log.e(TAG, "Transcript listener error", e)
            }
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
        synchronized(recognizerLock) {
            try {
                recognizer?.close()
                model?.close()
            } catch (_: Throwable) {}
            recognizer = null
            model = null
            isReady = false
        }
    }
}
