package com.xorcall.engine

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

enum class NumberCategory {
    TRUSTED_CONTACT,
    VERIFIED_OFFICIAL_BANK,
    VERIFIED_EMERGENCY_POLICE,
    HIGH_RISK_SPAM_INTERNATIONAL,
    UNKNOWN_NUMBER
}

data class NumberClassification(
    val rawNumber: String,
    val category: NumberCategory,
    val entityName: String?,
    val isAutoWhitelisted: Boolean,
    val requiresActiveMonitoring: Boolean
)

/**
 * NumberClassificationEngine
 * Differentiates between normal trusted contacts, verified official banking/toll-free/police lines,
 * and unknown/international spoofed numbers before audio monitoring engages.
 */
object NumberClassificationEngine {

    // Verified Indian Bank Toll-Free & Customer Service Lines
    private val VERIFIED_BANK_NUMBERS = mapOf(
        "1800112211" to "State Bank of India (Official)",
        "18004253800" to "State Bank of India (Official)",
        "18001234" to "State Bank of India (Official)",
        "18002026161" to "HDFC Bank (Official)",
        "18001600" to "HDFC Bank (Official)",
        "18001080" to "ICICI Bank (Official)",
        "18002095577" to "Axis Bank (Official)",
        "18001802222" to "Punjab National Bank (Official)",
        "18002090000" to "Kotak Mahindra Bank (Official)",
        "1800223344" to "Bank of Baroda (Official)",
        "18004194332" to "Canara Bank (Official)"
    )

    // Official Emergency and Government Helplines
    private val OFFICIAL_EMERGENCY_NUMBERS = mapOf(
        "112" to "National Emergency Response Support System (ERSS)",
        "100" to "Police Control Room (Official)",
        "108" to "Disaster & Emergency Ambulance Service",
        "1930" to "National Cyber Crime Reporting Helpline (I4C)",
        "1909" to "TRAI Telecom Commercial Communications / DND"
    )

    // Pre-flagged international syndicate and VoIP spoof prefixes
    private val HIGH_RISK_ISD_PREFIXES = listOf(
        "+92",   // Pakistan
        "+880",  // Bangladesh
        "+234",  // Nigeria
        "+855",  // Cambodia (Southeast Asian compound syndicates)
        "+95",   // Myanmar (Laukkai / Myawaddy scam parks)
        "+856",  // Laos (Golden Triangle SEZ)
        "+4470"  // UK Personal Numbering VoIP spoofing
    )

    /**
     * Classifies an incoming caller number.
     */
    fun classifyNumber(context: Context, number: String?): NumberClassification {
        val cleanNumber = number?.replace("[\\s\\-\\(\\)]".toRegex(), "") ?: ""

        if (cleanNumber.isBlank()) {
            return NumberClassification(
                rawNumber = "Unknown",
                category = NumberCategory.UNKNOWN_NUMBER,
                entityName = "Private / Hidden Caller",
                isAutoWhitelisted = false,
                requiresActiveMonitoring = true
            )
        }

        // 1. Check ContactWhitelistSyncManager (Automatic Starred, Kinship Family & Verified Helplines)
        val whitelisted = ContactWhitelistSyncManager.isNumberWhitelisted(context, cleanNumber)
        if (whitelisted != null) {
            val category = if (whitelisted.isOfficialBankOrEmergency) {
                if (whitelisted.relationship.contains("Emergency", ignoreCase = true) ||
                    whitelisted.relationship.contains("Police", ignoreCase = true) ||
                    whitelisted.relationship.contains("Cyber", ignoreCase = true)
                ) {
                    NumberCategory.VERIFIED_EMERGENCY_POLICE
                } else {
                    NumberCategory.VERIFIED_OFFICIAL_BANK
                }
            } else {
                NumberCategory.TRUSTED_CONTACT
            }
            return NumberClassification(
                rawNumber = cleanNumber,
                category = category,
                entityName = "${whitelisted.name} (${whitelisted.relationship})",
                isAutoWhitelisted = true,
                requiresActiveMonitoring = false
            )
        }

        // 2. Check Official Emergency Lines
        OFFICIAL_EMERGENCY_NUMBERS[cleanNumber]?.let { name ->
            return NumberClassification(
                rawNumber = cleanNumber,
                category = NumberCategory.VERIFIED_EMERGENCY_POLICE,
                entityName = name,
                isAutoWhitelisted = true,
                requiresActiveMonitoring = false
            )
        }

        // 2. Check Verified Bank Toll-Free Numbers
        VERIFIED_BANK_NUMBERS[cleanNumber]?.let { name ->
            return NumberClassification(
                rawNumber = cleanNumber,
                category = NumberCategory.VERIFIED_OFFICIAL_BANK,
                entityName = name,
                isAutoWhitelisted = true,
                requiresActiveMonitoring = false
            )
        }

        // Check generic 1800 / 1860 toll-free enterprise lines
        if (cleanNumber.startsWith("1800") || cleanNumber.startsWith("1860")) {
            return NumberClassification(
                rawNumber = cleanNumber,
                category = NumberCategory.VERIFIED_OFFICIAL_BANK,
                entityName = "Verified Enterprise Toll-Free",
                isAutoWhitelisted = true,
                requiresActiveMonitoring = false
            )
        }

        // 3. Check Phone Contacts Whitelist
        val contactName = lookupContactName(context, cleanNumber)
        if (contactName != null) {
            return NumberClassification(
                rawNumber = cleanNumber,
                category = NumberCategory.TRUSTED_CONTACT,
                entityName = contactName,
                isAutoWhitelisted = true,
                requiresActiveMonitoring = false
            )
        }

        // 4. Check High-Risk International / Virtual Spoof Prefixes
        if (HIGH_RISK_ISD_PREFIXES.any { cleanNumber.startsWith(it) }) {
            return NumberClassification(
                rawNumber = cleanNumber,
                category = NumberCategory.HIGH_RISK_SPAM_INTERNATIONAL,
                entityName = "High-Risk International / VoIP Spoof",
                isAutoWhitelisted = false,
                requiresActiveMonitoring = true
            )
        }

        // 5. Default: Unknown Number (Mandatory Monitoring)
        return NumberClassification(
            rawNumber = cleanNumber,
            category = NumberCategory.UNKNOWN_NUMBER,
            entityName = "Unknown Caller",
            isAutoWhitelisted = false,
            requiresActiveMonitoring = true
        )
    }

    /**
     * Looks up contact name in the user's phone contacts provider.
     */
    private fun lookupContactName(context: Context, number: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIdx != -1) cursor.getString(nameIdx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
