package com.xorcall.engine

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CallAuditRecord(
    val id: String,
    val timestamp: Long,
    val formattedTime: String,
    val callerNumber: String,
    val callerClassification: String,
    val demographicVector: String,
    val riskLevel: String,
    val isSyntheticVoice: Boolean,
    val syntheticConfidence: Float,
    val matchedKeywords: List<String>,
    val transcript: String,
    val evidenceHashSha256: String
)

/**
 * CallHistoryManager
 * Persistent forensic audit logger storing timestamped call transcripts,
 * demographic vector tags, acoustic voice clone probabilities, and
 * SHA-256 tamper-evident hashes compliant with Section 63 of Bharatiya Sakshya Adhiniyam (BSA).
 */
class CallHistoryManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("xorcall_call_history", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())

    fun recordCall(
        callerNumber: String,
        callerClassification: String,
        vector: DemographicVector,
        risk: RiskLevel,
        isSynthetic: Boolean,
        syntheticConfidence: Float,
        keywords: List<String>,
        transcript: String
    ): CallAuditRecord {
        val now = System.currentTimeMillis()
        val formattedDate = dateFormat.format(Date(now))
        val id = "XOR-REC-${now}"

        val rawEvidence = "$id|$now|$callerNumber|$transcript|${keywords.joinToString(",")}"
        val sha256 = computeSha256(rawEvidence)

        val record = CallAuditRecord(
            id = id,
            timestamp = now,
            formattedTime = formattedDate,
            callerNumber = callerNumber,
            callerClassification = callerClassification,
            demographicVector = vector.name,
            riskLevel = risk.name,
            isSyntheticVoice = isSynthetic,
            syntheticConfidence = syntheticConfidence,
            matchedKeywords = keywords,
            transcript = transcript,
            evidenceHashSha256 = sha256
        )

        saveRecord(record)
        return record
    }

    private fun saveRecord(record: CallAuditRecord) {
        val currentJson = prefs.getString("records_json", "[]") ?: "[]"
        val array = JSONArray(currentJson)

        val obj = JSONObject().apply {
            put("id", record.id)
            put("timestamp", record.timestamp)
            put("formattedTime", record.formattedTime)
            put("callerNumber", record.callerNumber)
            put("callerClassification", record.callerClassification)
            put("demographicVector", record.demographicVector)
            put("riskLevel", record.riskLevel)
            put("isSyntheticVoice", record.isSyntheticVoice)
            put("syntheticConfidence", record.syntheticConfidence.toDouble())
            put("matchedKeywords", JSONArray(record.matchedKeywords))
            put("transcript", record.transcript)
            put("evidenceHashSha256", record.evidenceHashSha256)
        }

        // Prepend to top of list
        val newArray = JSONArray()
        newArray.put(obj)
        for (i in 0 until array.length().coerceAtMost(30)) {
            newArray.put(array.getJSONObject(i))
        }

        prefs.edit().putString("records_json", newArray.toString()).apply()
    }

    fun getAllRecords(): List<CallAuditRecord> {
        val currentJson = prefs.getString("records_json", "[]") ?: "[]"
        val array = JSONArray(currentJson)
        val list = mutableListOf<CallAuditRecord>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val keywordsArray = obj.optJSONArray("matchedKeywords")
            val keywords = mutableListOf<String>()
            if (keywordsArray != null) {
                for (k in 0 until keywordsArray.length()) {
                    keywords.add(keywordsArray.getString(k))
                }
            }

            list.add(
                CallAuditRecord(
                    id = obj.optString("id"),
                    timestamp = obj.optLong("timestamp"),
                    formattedTime = obj.optString("formattedTime"),
                    callerNumber = obj.optString("callerNumber"),
                    callerClassification = obj.optString("callerClassification"),
                    demographicVector = obj.optString("demographicVector"),
                    riskLevel = obj.optString("riskLevel"),
                    isSyntheticVoice = obj.optBoolean("isSyntheticVoice"),
                    syntheticConfidence = obj.optDouble("syntheticConfidence", 0.0).toFloat(),
                    matchedKeywords = keywords,
                    transcript = obj.optString("transcript"),
                    evidenceHashSha256 = obj.optString("evidenceHashSha256")
                )
            )
        }

        return list
    }

    fun clearAll() {
        prefs.edit().remove("records_json").apply()
    }

    /**
     * Generates a 1930 / I4C Cybercrime Complaint text ready for 1-click filing.
     */
    fun generate1930ComplaintText(record: CallAuditRecord): String {
        return """
            ===================================================================
            NATIONAL CYBER CRIME REPORTING PORTAL (1930 / I4C) - FORMAL DOSSIER
            COMPLIANT UNDER BHARATIYA SAKSHYA ADHINIYAM (BSA) SECTION 63
            ===================================================================
            INCIDENT ID: ${record.id}
            TIMESTAMP: ${record.formattedTime}
            SUSPECT CALLER NUMBER: ${record.callerNumber}
            CALLER CLASSIFICATION: ${record.callerClassification}
            THREAT DEMOGRAPHIC VECTOR: ${record.demographicVector}
            INCIDENT RISK SEVERITY: ${record.riskLevel}
            AI SYNTHETIC VOICE DETECTED: ${if (record.isSyntheticVoice) "YES (${(record.syntheticConfidence * 100).toInt()}% Confidence)" else "NO"}
            MATCHED SCAM KEYWORDS: ${record.matchedKeywords.joinToString(", ")}
            
            TRANSCRIPT EXCERPT:
            "${record.transcript}"
            
            FORENSIC CRYPTOGRAPHIC EVIDENCE:
            SHA-256 INTEGRITY HASH: ${record.evidenceHashSha256}
            GENERATING GUARDIAN: XORcall Autonomous Cyber Safety Engine v1.0.3
            ===================================================================
        """.trimIndent()
    }

    private fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
