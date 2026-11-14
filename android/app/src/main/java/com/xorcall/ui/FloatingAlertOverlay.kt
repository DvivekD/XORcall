package com.xorcall.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xorcall.MainActivity

/**
 * FloatingAlertOverlay
 * Injects a full-screen or heads-up system alert window directly over
 * incoming or ongoing WhatsApp/Skype/Phone calls when a Digital Arrest or extortion script is confirmed.
 * Also broadcasts a high-priority Heads-Up Notification so the alert is 100% visible even if overlay permission is pending.
 */
class FloatingAlertOverlay(private val context: Context) {

    private val windowManager = (context.applicationContext ?: context).getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    private val ALERT_CHANNEL_ID = "XORcall_Critical_Alerts"
    private val ALERT_NOTIF_ID = 2002

    init {
        createAlertNotificationChannel()
    }

    private fun createAlertNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "🚨 XORcall Critical Scam Intercepts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority heads-up alerts during active scam calls"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 500)
                setShowBadge(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun showCriticalAlert(warningText: String, onHangupClick: () -> Unit = {}) {
        // 1. Always fire an intrusive, high-priority Heads-Up Notification (100% visible over any dialer/call screen)
        sendHeadsUpNotification(warningText)

        // 2. If System Overlay permission is granted, inject floating banner overlay safely on Main Looper
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        mainHandler.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return@post
            }

            if (overlayView != null) return@post

            try {
                val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutParamsType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = 60
                }

                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(0xF0180404.toInt()) // Deep Crimson Dark Slate
                    setPadding(36, 28, 36, 28)
                }

                val header = TextView(context).apply {
                    text = "🚨 KAVACH CYBER GUARDIAN: THREAT INTERCEPTED"
                    textSize = 14f
                    setTextColor(0xFFFF3333.toInt())
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }

                val body = TextView(context).apply {
                    text = warningText
                    textSize = 13f
                    setTextColor(0xFFF1F5F9.toInt())
                    setPadding(0, 12, 0, 16)
                }

                val buttonRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                }

                val dial1930Btn = TextView(context).apply {
                    text = "📞 DIAL 1930 HELPLINE"
                    textSize = 12f
                    setTextColor(0xFFFFFFFF.toInt())
                    setBackgroundColor(0xFFDC2626.toInt())
                    setPadding(28, 16, 28, 16)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setOnClickListener {
                        try {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(dialIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        dismiss()
                    }
                }

                val dismissBtn = TextView(context).apply {
                    text = "DISMISS"
                    textSize = 12f
                    setTextColor(0xFF94A3B8.toInt())
                    setPadding(28, 16, 28, 16)
                    setOnClickListener {
                        dismiss()
                    }
                }

                buttonRow.addView(dismissBtn)
                buttonRow.addView(dial1930Btn)

                container.addView(header)
                container.addView(body)
                container.addView(buttonRow)

                overlayView = container
                windowManager.addView(container, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Injects a specialized video call deepfake detection HUD banner directly over
     * WhatsApp / video call screens.
     * Displays green verification badge for authentic video feeds or red alert for synthetic avatars.
     */
    fun showVideoDeepfakeHUD(hudBadge: String, isSynthetic: Boolean, onDismiss: () -> Unit = {}) {
        if (isSynthetic) {
            sendHeadsUpNotification(hudBadge)
        }

        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        mainHandler.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return@post
            }

            overlayView?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                overlayView = null
            }

            try {
                val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutParamsType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = 50
                }

                val bgColor = if (isSynthetic) 0xF22A0404.toInt() else 0xF2042A14.toInt()
                val borderColor = if (isSynthetic) 0xFFEF4444.toInt() else 0xFF10B981.toInt()

                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(bgColor)
                    setPadding(36, 24, 36, 24)
                }

                val header = TextView(context).apply {
                    text = if (isSynthetic) "🚨 VIDEO CALL DEEPFAKE INTERCEPTED" else "🟢 VERIFIED BIOMETRIC IDENTITY"
                    textSize = 13f
                    setTextColor(borderColor)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }

                val body = TextView(context).apply {
                    text = hudBadge
                    textSize = 12f
                    setTextColor(0xFFF1F5F9.toInt())
                    setPadding(0, 10, 0, 14)
                }

                val buttonRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                }

                if (isSynthetic) {
                    val dial1930Btn = TextView(context).apply {
                        text = "📞 DIAL 1930 HELPLINE"
                        textSize = 11f
                        setTextColor(0xFFFFFFFF.toInt())
                        setBackgroundColor(0xFFDC2626.toInt())
                        setPadding(24, 14, 24, 14)
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setOnClickListener {
                            try {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930")).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(dialIntent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            dismiss()
                            onDismiss()
                        }
                    }
                    buttonRow.addView(dial1930Btn)
                }

                val dismissBtn = TextView(context).apply {
                    text = if (isSynthetic) "DISMISS" else "CLOSE HUD"
                    textSize = 11f
                    setTextColor(0xFF94A3B8.toInt())
                    setPadding(24, 14, 24, 14)
                    setOnClickListener {
                        dismiss()
                        onDismiss()
                    }
                }
                buttonRow.addView(dismissBtn)

                container.addView(header)
                container.addView(body)
                container.addView(buttonRow)

                overlayView = container
                windowManager.addView(container, params)

                // For authentic human HUD, auto-dismiss after 4 seconds
                if (!isSynthetic) {
                    mainHandler.postDelayed({
                        if (overlayView == container) {
                            dismiss()
                        }
                    }, 4000L)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendHeadsUpNotification(warningText: String) {
        try {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val dialPendingIntent = PendingIntent.getActivity(
                context,
                1,
                dialIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🚨 SCAM CALL DETECTED: HANG UP!")
                .setContentText(warningText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(warningText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(openAppPendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 300, 150, 300, 150, 500))
                .addAction(android.R.drawable.ic_menu_call, "Dial 1930 Helpline", dialPendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(ALERT_NOTIF_ID, notif)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismiss() {
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        mainHandler.post {
            overlayView?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                overlayView = null
            }
        }
    }
}
