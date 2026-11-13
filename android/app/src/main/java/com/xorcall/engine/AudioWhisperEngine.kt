package com.xorcall.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * AudioWhisperEngine
 * Generates an out-of-band acoustic whisper warning directly in-ear during an extortion call.
 * Uses Android's TextToSpeech engine configured with STREAM_VOICE_CALL or STREAM_NOTIFICATION
 * so the victim hears the alert discreetly even when held to the ear.
 */
class AudioWhisperEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setPitch(0.95f)
                    engine.setSpeechRate(1.15f) // Crisp, urgent delivery
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val audioAttrs = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        engine.setAudioAttributes(audioAttrs)
                    }
                    isReady = true
                }
            }
        }
    }

    fun speakWarning(warningText: String) {
        if (!isReady || tts == null) return
        try {
            val message = "Warning. Extortion threat detected. Do not pay. Hang up immediately."
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "XORCALL_WHISPER_${System.currentTimeMillis()}")
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isReady = false
        } catch (_: Exception) {}
    }
}
