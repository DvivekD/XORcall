package com.xorcall.engine

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

/**
 * GuardianSettingsManager
 * Thread-safe persistent configuration manager backed by SharedPreferences.
 * Synchronizes alert modalities (Haptics, Floating HUD, Guardian Ping, Audio Whisper)
 * and active demographic persona scope across MainActivity, CallMonitorService, and PhoneCallReceiver.
 */
class GuardianSettingsManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("xorcall_guardian_prefs", Context.MODE_PRIVATE)

    companion object {
        const val ACTION_SETTINGS_CHANGED = "com.xorcall.SETTINGS_CHANGED"
        
        private const val KEY_HAPTICS = "pref_haptics_enabled"
        private const val KEY_BANNER = "pref_banner_enabled"
        private const val KEY_FAMILY_PING = "pref_family_ping_enabled"
        private const val KEY_AUDIO_WHISPER = "pref_audio_whisper_enabled"
        private const val KEY_WHITELIST_CONTACTS = "pref_whitelist_contacts_enabled"
        private const val KEY_AUTO_ALLOW_BANKS = "pref_auto_allow_banks_enabled"
        private const val KEY_AI_VOICE_DETECTION = "pref_ai_voice_detection_enabled"
        private const val KEY_ACTIVE_VECTOR = "pref_active_vector"
        private const val KEY_GROQ_API_KEY = "pref_groq_api_key"
        private const val KEY_BACKEND_URL = "pref_backend_url"
        private const val KEY_AUTO_SPEAKERPHONE = "pref_auto_speakerphone_enabled"
        private const val KEY_CLOUD_STT = "pref_cloud_stt_enabled"

        val DEFAULT_GROQ_KEY: String by lazy {
            try {
                val mask = 0x5C
                val rawBytes = byteArrayOf(
                    59, 47, 55, 3, 107, 11, 111, 37, 51, 40, 29, 105, 26, 29, 18, 36, 38, 56, 50, 21,
                    8, 36, 14, 40, 11, 27, 56, 37, 62, 111, 26, 5, 11, 52, 25, 101, 49, 52, 104, 21,
                    54, 100, 43, 12, 37, 27, 46, 54, 62, 27, 23, 8, 48, 13, 20, 31
                )
                String(ByteArray(rawBytes.size) { (rawBytes[it].toInt() xor mask).toByte() }, Charsets.UTF_8).trim()
            } catch (_: Exception) {
                ""
            }
        }
    }

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HAPTICS, value).apply()
            notifyChange()
        }

    var isBannerEnabled: Boolean
        get() = prefs.getBoolean(KEY_BANNER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_BANNER, value).apply()
            notifyChange()
        }

    var isFamilyPingEnabled: Boolean
        get() = prefs.getBoolean(KEY_FAMILY_PING, true)
        set(value) {
            prefs.edit().putBoolean(KEY_FAMILY_PING, value).apply()
            notifyChange()
        }

    var isAudioWhisperEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUDIO_WHISPER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUDIO_WHISPER, value).apply()
            notifyChange()
        }

    var isWhitelistContactsEnabled: Boolean
        get() = prefs.getBoolean(KEY_WHITELIST_CONTACTS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_WHITELIST_CONTACTS, value).apply()
            notifyChange()
        }

    var isAutoAllowBanksEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_ALLOW_BANKS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_ALLOW_BANKS, value).apply()
            notifyChange()
        }

    var isAiVoiceDetectionArmed: Boolean
        get() = prefs.getBoolean(KEY_AI_VOICE_DETECTION, false)
        set(value) {
            prefs.edit().putBoolean(KEY_AI_VOICE_DETECTION, value).apply()
            notifyChange()
        }

    var activeVector: DemographicVector
        get() {
            val name = prefs.getString(KEY_ACTIVE_VECTOR, DemographicVector.ALL.name) ?: DemographicVector.ALL.name
            return try {
                DemographicVector.valueOf(name)
            } catch (_: Exception) {
                DemographicVector.ALL
            }
        }
        set(value) {
            prefs.edit().putString(KEY_ACTIVE_VECTOR, value.name).apply()
            notifyChange()
        }

    var groqApiKey: String
        get() {
            val stored = prefs.getString(KEY_GROQ_API_KEY, null)
            return if (!stored.isNullOrBlank()) stored else DEFAULT_GROQ_KEY
        }
        set(value) {
            prefs.edit().putString(KEY_GROQ_API_KEY, value.trim()).apply()
            notifyChange()
        }

    var backendUrl: String
        get() = prefs.getString(KEY_BACKEND_URL, "https://api.xorcall.kanyarasi.live") ?: "https://api.xorcall.kanyarasi.live"
        set(value) {
            prefs.edit().putString(KEY_BACKEND_URL, value.trim()).apply()
            notifyChange()
        }

    var isAutoSpeakerphoneEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPEAKERPHONE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_SPEAKERPHONE, value).apply()
            notifyChange()
        }

    var isCloudSttEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_STT, true)
        set(value) {
            prefs.edit().putBoolean(KEY_CLOUD_STT, value).apply()
            notifyChange()
        }

    private fun notifyChange() {
        val intent = Intent(ACTION_SETTINGS_CHANGED).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }
}
