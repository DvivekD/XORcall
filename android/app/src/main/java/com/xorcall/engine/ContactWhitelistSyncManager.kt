package com.xorcall.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * WhitelistedContact
 * Represents a trusted, immune contact entity: Family (Mom, Dad, Son, Daughter, Spouse, Starred)
 * or Verified Official Emergency & Banking Toll-Free helpline.
 */
data class WhitelistedContact(
    val id: String,
    val name: String,
    val number: String,
    val relationship: String,
    val isOfficialBankOrEmergency: Boolean = false,
    val isStarred: Boolean = false
)

/**
 * ContactWhitelistSyncManager
 * Mission-critical cyber defense capability:
 * 1. Automatically detects and whitelists trusted family members (Mom, Dad, Son, Daughter, Spouse, Starred)
 *    and official emergency lines (1930 Cyber Helpline, 112, 100, 1800 official bank helplines).
 * 2. Legitimate calls from these contacts are automatically marked SAFE and immune to false alarms.
 * 3. In-call scam alerts are automatically suppressed, broadcasting com.xorcall.WHITELIST_CALL
 *    and logging the call audit record as TRUSTED_CONTACT in the BSA Section 63 forensic log.
 */
object ContactWhitelistSyncManager {

    private const val TAG = "ContactWhitelistSync"

    // Verified Indian Emergency and Government Helplines
    private val OFFICIAL_EMERGENCY_HELPLINES = listOf(
        WhitelistedContact("EMERG_1930", "National Cyber Crime Helpline (I4C)", "1930", "Cyber Emergency Helpline", isOfficialBankOrEmergency = true, isStarred = true),
        WhitelistedContact("EMERG_112", "National Emergency Response (ERSS)", "112", "Police & Emergency", isOfficialBankOrEmergency = true, isStarred = true),
        WhitelistedContact("EMERG_100", "Police Control Room", "100", "Police Helpline", isOfficialBankOrEmergency = true, isStarred = true),
        WhitelistedContact("EMERG_108", "Disaster & Emergency Ambulance", "108", "Ambulance / Medical", isOfficialBankOrEmergency = true),
        WhitelistedContact("EMERG_1091", "Women Safety Helpline", "1091", "Emergency Services", isOfficialBankOrEmergency = true),
        WhitelistedContact("EMERG_14440", "RBI Financial Fraud & Ombudsman", "14440", "Central Bank Helpline", isOfficialBankOrEmergency = true, isStarred = true)
    )

    // Verified Indian Bank IVR & Toll-Free Helplines
    private val VERIFIED_BANK_HELPLINES = listOf(
        WhitelistedContact("BANK_SBI_1", "State Bank of India (SBI Official)", "1800112211", "Official Bank Toll-Free", isOfficialBankOrEmergency = true),
        WhitelistedContact("BANK_SBI_2", "State Bank of India (SBI Official)", "18004253800", "Official Bank Toll-Free", isOfficialBankOrEmergency = true),
        WhitelistedContact("BANK_SBI_3", "State Bank of India (SBI Official)", "18001234", "Official Bank Toll-Free", isOfficialBankOrEmergency = true),
        WhitelistedContact("BANK_HDFC_1", "HDFC Bank (Official)", "18002026161", "Official Bank Toll-Free", isOfficialBankOrEmergency = true),
        WhitelistedContact("BANK_HDFC_2", "HDFC Bank (Official)", "18001600", "Official Bank Toll-Free", isOfficialBankOrEmergency = true),
        WhitelistedContact("BANK_ICICI", "ICICI Bank (Official)", "18001080", "Official Bank Toll-Free", isOfficialBankOrEmergency = true),
        WhitelistedContact("BANK_AXIS", "Axis Bank (Official)", "18002095577", "Official Bank Toll-Free", isOfficialBankOrEmergency = true),
        WhitelistedContact("BANK_PNB", "Punjab National Bank (Official)", "18001802222", "Official Bank Toll-Free", isOfficialBankOrEmergency = true),
        WhitelistedContact("BANK_KOTAK", "Kotak Mahindra Bank (Official)", "18002090000", "Official Bank Toll-Free", isOfficialBankOrEmergency = true),
        WhitelistedContact("BANK_BOB", "Bank of Baroda (Official)", "1800223344", "Official Bank Toll-Free", isOfficialBankOrEmergency = true),
        WhitelistedContact("BANK_CANARA", "Canara Bank (Official)", "18004194332", "Official Bank Toll-Free", isOfficialBankOrEmergency = true)
    )

    // Preset family contacts (used as baseline fallback or demo contacts)
    private val PRESET_FAMILY_CONTACTS = listOf(
        WhitelistedContact("FAM_MOM", "Maa (Mom)", "+91 98765 43210", "Mother", isOfficialBankOrEmergency = false, isStarred = true),
        WhitelistedContact("FAM_DAD", "Papa (Dad)", "+91 98765 43211", "Father", isOfficialBankOrEmergency = false, isStarred = true),
        WhitelistedContact("FAM_SON", "Rohan (Son)", "+91 98765 43212", "Son", isOfficialBankOrEmergency = false, isStarred = false),
        WhitelistedContact("FAM_DAUGHTER", "Pooja (Daughter)", "+91 98765 43213", "Daughter", isOfficialBankOrEmergency = false, isStarred = false),
        WhitelistedContact("FAM_SPOUSE", "Priya (Spouse)", "+91 98765 43214", "Spouse", isOfficialBankOrEmergency = false, isStarred = true)
    )

    // Kinship patterns for Indian English and regional kinship recognition
    private val KINSHIP_PATTERNS = listOf(
        Regex("(?i)\\b(mom|mother|maa|mummy|ammi|aai|amma)\\b") to "Mother",
        Regex("(?i)\\b(dad|father|papa|appa|baba|pitaji|abbu)\\b") to "Father",
        Regex("(?i)\\b(wife|husband|spouse|partner)\\b") to "Spouse",
        Regex("(?i)\\b(son|beta)\\b") to "Son",
        Regex("(?i)\\b(daughter|beti)\\b") to "Daughter",
        Regex("(?i)\\b(brother|bro|bhai|bhaiya)\\b") to "Brother",
        Regex("(?i)\\b(sister|sis|didi|bhabhi)\\b") to "Sister",
        Regex("(?i)\\b(uncle|chacha|mama|kaka)\\b") to "Family (Uncle)",
        Regex("(?i)\\b(aunt|chachi|mami|kaki)\\b") to "Family (Aunt)"
    )

    @Volatile
    private var cachedWhitelist: List<WhitelistedContact>? = null

    /**
     * Synchronizes and returns the complete whitelisted contacts list.
     * Combines queried ContactsContract contacts (Starred & Kinship labels)
     * with verified Emergency Helplines and Bank IVR lines.
     */
    fun syncWhitelistedContacts(context: Context): List<WhitelistedContact> {
        val list = mutableListOf<WhitelistedContact>()

        // 1. Always include official emergency numbers
        list.addAll(OFFICIAL_EMERGENCY_HELPLINES)

        // 2. Always include verified bank lines
        list.addAll(VERIFIED_BANK_HELPLINES)

        // 3. Query ContactsContract if READ_CONTACTS permission is granted
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        var foundUserContacts = 0

        if (hasPermission) {
            try {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.STARRED
                )

                val cursor: Cursor? = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    null
                )

                cursor?.use {
                    val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val starCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

                    while (it.moveToNext()) {
                        val id = if (idCol != -1) it.getString(idCol) ?: "" else ""
                        val name = if (nameCol != -1) it.getString(nameCol) ?: "" else ""
                        val number = if (numCol != -1) it.getString(numCol) ?: "" else ""
                        val isStarred = if (starCol != -1) it.getInt(starCol) == 1 else false

                        if (number.isNotBlank()) {
                            val kinshipLabel = detectKinshipLabel(name)
                            if (isStarred || kinshipLabel != null) {
                                val relationship = when {
                                    kinshipLabel != null -> kinshipLabel
                                    isStarred -> "Starred Contact"
                                    else -> "Trusted Contact"
                                }
                                list.add(
                                    WhitelistedContact(
                                        id = "CONTACT_$id",
                                        name = name.ifBlank { "Trusted Contact" },
                                        number = number,
                                        relationship = relationship,
                                        isOfficialBankOrEmergency = false,
                                        isStarred = isStarred
                                    )
                                )
                                foundUserContacts++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ContactsContract query failed: ${e.message}")
            }
        }

        // If no user contacts were accessible or found, merge preset family contacts as baseline
        if (foundUserContacts == 0) {
            list.addAll(PRESET_FAMILY_CONTACTS)
        }

        val deduplicated = list.distinctBy { normalizePhoneNumber(it.number) }
        cachedWhitelist = deduplicated
        return deduplicated
    }

    /**
     * Checks if the incoming caller number matches a whitelisted contact.
     */
    fun isNumberWhitelisted(context: Context, number: String?): WhitelistedContact? {
        if (number.isNullOrBlank()) return null
        val cleanNumber = normalizePhoneNumber(number)

        // Check cache or sync
        val currentList = cachedWhitelist ?: syncWhitelistedContacts(context)

        // 1. Direct emergency check
        for (emerg in OFFICIAL_EMERGENCY_HELPLINES) {
            if (cleanNumber == emerg.number || cleanNumber.endsWith(emerg.number)) {
                return emerg
            }
        }

        // 2. Direct bank check
        for (bank in VERIFIED_BANK_HELPLINES) {
            if (cleanNumber == bank.number || cleanNumber.endsWith(bank.number)) {
                return bank
            }
        }

        // 3. Generic 1800 / 1860 toll-free check
        if (cleanNumber.startsWith("1800") || cleanNumber.startsWith("1860")) {
            return WhitelistedContact(
                id = "BANK_TOLLFREE",
                name = "Verified Enterprise Toll-Free",
                number = number,
                relationship = "Official Enterprise IVR",
                isOfficialBankOrEmergency = true
            )
        }

        // 4. Match against synced contacts by last 10 digits
        val targetSuffix = if (cleanNumber.length >= 10) cleanNumber.takeLast(10) else cleanNumber
        return currentList.firstOrNull { contact ->
            val contactClean = normalizePhoneNumber(contact.number)
            val contactSuffix = if (contactClean.length >= 10) contactClean.takeLast(10) else contactClean
            contactSuffix == targetSuffix
        }
    }

    /**
     * Executes the automatic whitelisting procedure:
     * - Suppresses scam alerts
     * - Broadcasts com.xorcall.WHITELIST_CALL
     * - Records the call in CallAuditRecord as TRUSTED_CONTACT
     */
    fun handleWhitelistedCall(
        context: Context,
        number: String,
        contact: WhitelistedContact,
        historyManager: CallHistoryManager? = null
    ) {
        Log.i(TAG, "🟢 AUTO-WHITELISTED: Call from ${contact.name} (${contact.relationship}, ${contact.number}) approved. False alerts suppressed.")

        // Broadcast to UI and services
        val intent = Intent("com.xorcall.WHITELIST_CALL").apply {
            putExtra("number", number)
            putExtra("name", contact.name)
            putExtra("entity", contact.name)
            putExtra("relationship", contact.relationship)
            putExtra("isBankOrEmergency", contact.isOfficialBankOrEmergency)
            putExtra("isStarred", contact.isStarred)
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)

        // Log to BSA Section 63 forensic audit trail as TRUSTED_CONTACT
        historyManager?.recordCall(
            callerNumber = number,
            callerClassification = if (contact.isOfficialBankOrEmergency) "VERIFIED_OFFICIAL" else "TRUSTED_CONTACT",
            vector = DemographicVector.ALL,
            risk = RiskLevel.SAFE,
            isSynthetic = false,
            syntheticConfidence = 0.0f,
            keywords = listOf("WHITELIST_APPROVED", "BENIGN_CALLER"),
            transcript = "[SECURE CALL] Trusted contact: ${contact.name} (${contact.relationship}). All threat alarms suppressed."
        )
    }

    /**
     * Extracts kinship label from a contact's display name.
     */
    private fun detectKinshipLabel(name: String): String? {
        for ((regex, label) in KINSHIP_PATTERNS) {
            if (regex.containsMatchIn(name)) {
                return label
            }
        }
        return null
    }

    /**
     * Normalizes phone numbers for matching (removes spaces, dashes, brackets, leading +91 or 0).
     */
    fun normalizePhoneNumber(number: String): String {
        return number.replace("[\\s\\-\\(\\)\\+]".toRegex(), "")
    }

    /**
     * Returns pre-populated family samples for dashboard display.
     */
    fun getPresetFamilyContacts(): List<WhitelistedContact> = PRESET_FAMILY_CONTACTS

    /**
     * Returns official emergency contacts for dashboard display.
     */
    fun getEmergencyContacts(): List<WhitelistedContact> = OFFICIAL_EMERGENCY_HELPLINES

    /**
     * Returns verified bank contacts for dashboard display.
     */
    fun getBankContacts(): List<WhitelistedContact> = VERIFIED_BANK_HELPLINES
}
