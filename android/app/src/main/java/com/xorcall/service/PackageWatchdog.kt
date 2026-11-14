package com.xorcall.service

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * PackageWatchdog
 * Cyber safety app monitor and financial transaction guardian.
 * 1. Detects remote desktop trojans (AnyDesk, TeamViewer, RustDesk) opened during scam calls.
 * 2. Monitors sensitive financial, UPI, banking, and crypto apps (PhonePe, GPay, Paytm, BHIM, YONO, Telegram).
 * 3. Enforces automatic application freeze when isScamCallActive is true.
 */
class PackageWatchdog(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    companion object {
        @Volatile
        private var _isScamCallActive: Boolean = false

        val isScamCallActive: Boolean
            get() = _isScamCallActive

        fun setScamCallActive(active: Boolean) {
            _isScamCallActive = active
        }

        // Targeted set of financial, payment, banking, crypto, and coercion messaging packages
        val SENSITIVE_FINANCIAL_PACKAGES = setOf(
            // UPI & Mobile Payments
            "com.phonepe.app",                                 // PhonePe UPI
            "com.google.android.apps.nbu.paisa.user",          // Google Pay (GPay Tez)
            "net.one97.paytm",                                 // Paytm Payments
            "in.org.npci.upiapp",                              // BHIM National UPI
            "com.dreamplug.androidapp",                        // CRED UPI
            "com.amazon.mShop.android.shopping",               // Amazon Pay
            
            // Major Indian Banking Apps
            "com.sbi.upi",                                     // YONO SBI
            "com.icicibank.mobile",                            // ICICI iMobile Pay
            "com.snapwork.hdfc",                               // HDFC MobileBanking
            "com.axis.mobile",                                 // Axis Mobile
            "com.msf.kbank.mobile",                            // Kotak 811
            "com.bankofbaroda.mconnect",                       // bob World
            "com.pnb.pnbone",                                  // PNB ONE
            "com.canarabank.mobility",                         // Canara ai1
            "com.unionbank.ecommerce.mobile.android",          // Union Bank Vyom
            "com.finopaytech.bpay",                            // Fino BPay

            // Crypto Wallets & Exchanges (High-Frequency Extortion Mule Vector)
            "com.binance.dev",                                 // Binance
            "com.coindcx",                                     // CoinDCX
            "com.coinswitch.kuber",                            // CoinSwitch Kuber
            "io.metamask",                                     // MetaMask
            "com.wallet.crypto.trustapp",                      // Trust Wallet
            "com.wazirx.android",                              // WazirX

            // Coercion & Digital Arrest Video Channels
            "org.telegram.messenger",                          // Telegram Official
            "org.telegram.plus"                                // Telegram Plus
        )

        private val blacklistedRemotePackages = setOf(
            "com.anydesk.anydeskandroid",
            "com.teamviewer.quicksupport.market",
            "com.teamviewer.host.market",
            "com.carriez.rustdesk",
            "com.google.chromeremotedesktop",
            "com.splashtop.remote"
        )

        fun isSensitiveFinancialPackage(packageName: String): Boolean {
            return SENSITIVE_FINANCIAL_PACKAGES.contains(packageName)
        }

        fun isRemoteDesktopPackage(packageName: String): Boolean {
            return blacklistedRemotePackages.contains(packageName)
        }

        fun getFinancialAppName(packageName: String): String {
            return when (packageName) {
                "com.phonepe.app" -> "PhonePe UPI"
                "com.google.android.apps.nbu.paisa.user" -> "Google Pay (GPay)"
                "net.one97.paytm" -> "Paytm Payments"
                "in.org.npci.upiapp" -> "BHIM UPI"
                "com.dreamplug.androidapp" -> "CRED Payments"
                "com.amazon.mShop.android.shopping" -> "Amazon Pay"
                "com.sbi.upi" -> "YONO SBI"
                "com.icicibank.mobile" -> "ICICI iMobile Pay"
                "com.snapwork.hdfc" -> "HDFC MobileBanking"
                "com.axis.mobile" -> "Axis Mobile"
                "com.msf.kbank.mobile" -> "Kotak 811"
                "com.bankofbaroda.mconnect" -> "bob World (Bank of Baroda)"
                "com.pnb.pnbone" -> "PNB ONE"
                "com.canarabank.mobility" -> "Canara ai1"
                "org.telegram.messenger", "org.telegram.plus" -> "Telegram (Digital Arrest Coercion)"
                "com.binance.dev" -> "Binance Crypto"
                "com.coindcx" -> "CoinDCX Crypto"
                "com.coinswitch.kuber" -> "CoinSwitch Kuber"
                "io.metamask" -> "MetaMask Wallet"
                "com.wallet.crypto.trustapp" -> "Trust Wallet"
                else -> "Financial / UPI Payment App"
            }
        }
    }

    fun checkActiveForegroundPackage(): String? {
        val time = System.currentTimeMillis()
        val stats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - TimeUnit.SECONDS.toMillis(10),
            time
        ) ?: return null

        var currentApp: String? = null
        var lastTimeUsed: Long = 0

        for (pkg in stats) {
            if (pkg.lastTimeUsed > lastTimeUsed) {
                currentApp = pkg.packageName
                lastTimeUsed = pkg.lastTimeUsed
            }
        }
        return currentApp
    }

    fun isDangerousAppInForeground(): Boolean {
        val currentApp = checkActiveForegroundPackage() ?: return false
        return blacklistedRemotePackages.contains(currentApp)
    }

    /**
     * Checks if a sensitive financial app is currently running in the foreground.
     * Returns package name if detected, or null.
     */
    fun isFinancialAppInForeground(): String? {
        val currentApp = checkActiveForegroundPackage() ?: return null
        return if (SENSITIVE_FINANCIAL_PACKAGES.contains(currentApp)) currentApp else null
    }
}
