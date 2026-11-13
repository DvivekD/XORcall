package com.xorcall.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.xorcall.engine.NumberClassificationEngine

/**
 * PhoneCallReceiver
 * Listens for incoming telephony calls.
 * Implements Smart Whitelisting:
 * - Verified Official Banks (1800 SBI, HDFC, ICICI) and Police/Emergency (112, 100, 1930) are automatically bypassed.
 * - Phone contacts are trusted.
 * - Unknown numbers and high-risk international prefixes (+92, +880, +234, +855) automatically trigger active audio monitoring.
 */
class PhoneCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            if (state == TelephonyManager.EXTRA_STATE_OFFHOOK || state == TelephonyManager.EXTRA_STATE_RINGING) {
                val classification = NumberClassificationEngine.classifyNumber(context, incomingNumber)

                // If number is a verified bank or trusted contact and whitelisting is active
                if (classification.isAutoWhitelisted) {
                    val whiteListIntent = Intent("com.xorcall.WHITELIST_CALL").apply {
                        putExtra("number", classification.rawNumber)
                        putExtra("entity", classification.entityName)
                        putExtra("category", classification.category.name)
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(whiteListIntent)
                    return
                }

                // If unknown number or international spoof -> Send Caller Info to Monitor Service
                val hasAudio = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasAudio) {
                    val serviceIntent = Intent(context, CallMonitorService::class.java).apply {
                        putExtra("EXTRA_INCOMING_NUMBER", classification.rawNumber)
                        putExtra("EXTRA_CLASSIFICATION", classification.category.name)
                    }
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // Call ended: Notify service of call completion without tearing down the protective shield
                val idleIntent = Intent(context, CallMonitorService::class.java).apply {
                    putExtra("EXTRA_CALL_STATE", "IDLE")
                }
                try {
                    context.startService(idleIntent)
                } catch (_: Exception) {}
            }
        }
    }
}
