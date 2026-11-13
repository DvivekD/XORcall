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
        get() = prefs.getBoolean(KEY_AI_VOICE_DETECTION, true)
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

    private fun notifyChange() {
        val intent = Intent(ACTION_SETTINGS_CHANGED).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }
}
