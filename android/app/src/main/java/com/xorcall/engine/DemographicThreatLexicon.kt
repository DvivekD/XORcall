package com.xorcall.engine

enum class DemographicVector {
    ALL,
    STUDENT,
    SENIOR,
    PROFESSIONAL
}

data class VectorThreatResult(
    val matchedVector: DemographicVector,
    val matchedKeywords: List<String>,
    val threatSummary: String,
    val riskLevel: RiskLevel
)

/**
 * DemographicThreatLexicon
 * Comprehensive multi-demographic lexicon and intent conjunction rules covering:
 * 1. Student Vector (Telegram Task Traps, Fake Semester Fees, Sextortion)
 * 2. Senior Citizen Vector (Digital Arrest, CBI/Police, Pension/KYC Suspension, FD Liquidation)
 * 3. Working Professional Vector (FedEx/Customs MDMA Parcel, TRAI Number Severing, GST/IT Raids)
 */
object DemographicThreatLexicon {

    // 1. Student & Youth Threat Keywords
    val STUDENT_KEYWORDS = listOf(
        // Telegram & Task Fraud
        "telegram", "part-time job", "review job", "like youtube videos", "rating task",
        "crypto recharge", "recharge wallet", "freeze balance", "commission", "usdt", "deposit money",
        // Campus & Semester Fee Traps
        "semester fee", "exam fee", "hall ticket withheld", "cheating in exam", "police complaint from dean",
        "hostel raid", "university penalty", "college administration", "admission cancelled",
        // Sextortion
        "video call recording", "leaked video", "viral on instagram", "youtube upload", "send money on upi"
    )

    // 2. Senior Citizen & Retiree Threat Keywords
    val SENIOR_KEYWORDS = listOf(
        // Digital Arrest & High-Pressure Coercion
        "digital arrest", "supreme court", "cbi", "narcotics", "mumbai police", "customs parcel",
        "mdma", "contraband", "money laundering", "naresh goyal", "arrest warrant", "section 144",
        "official secrets act", "gag order", "do not disconnect", "stay on camera", "lock your room",
        // Financial Liquidation
        "rbi escrow", "secret verification account", "liquidate fixed deposit", "break fd",
        "transfer to rbi account", "supervision account", "do not tell your children",
        // Pension & Utility Fraud
        "pension block", "jeevan pramaan kyc", "electricity disconnection tonight", "bank account suspended",
        "aadhaar biometric lock"
    )

    // 3. Working Professional & Corporate Threat Keywords
    val PROFESSIONAL_KEYWORDS = listOf(
        // FedEx / Customs Parcel Pretext
        "fedex", "dhl", "customs", "parcel intercepted", "taiwan", "thailand", "5 expired passports",
        "illegal parcel", "trai", "department of telecommunications", "sim cards in your name",
        "mobile disconnection in 2 hours", "crime branch andheri",
        // Corporate & Tax Intimidation
        "income tax raid", "gst notice", "director kyc", "corporate bank account freeze",
        "statutory non-compliance", "pmla investigation", "audit clearance"
    )

    /**
     * Evaluates text against a specific demographic scope or ALL scopes.
     */
    fun evaluate(text: String, scope: DemographicVector = DemographicVector.ALL): VectorThreatResult {
        val lower = text.lowercase()

        val studentMatches = STUDENT_KEYWORDS.filter { lower.contains(it) }
        val seniorMatches = SENIOR_KEYWORDS.filter { lower.contains(it) }
        val profMatches = PROFESSIONAL_KEYWORDS.filter { lower.contains(it) }

        val activeMatches = when (scope) {
            DemographicVector.STUDENT -> studentMatches
            DemographicVector.SENIOR -> seniorMatches
            DemographicVector.PROFESSIONAL -> profMatches
            DemographicVector.ALL -> (studentMatches + seniorMatches + profMatches).distinct()
        }

        // Determine dominant vector
        val dominantVector = when {
            scope != DemographicVector.ALL -> scope
            studentMatches.size > seniorMatches.size && studentMatches.size > profMatches.size -> DemographicVector.STUDENT
            profMatches.size > seniorMatches.size && profMatches.size > studentMatches.size -> DemographicVector.PROFESSIONAL
            seniorMatches.isNotEmpty() -> DemographicVector.SENIOR
            studentMatches.isNotEmpty() -> DemographicVector.STUDENT
            profMatches.isNotEmpty() -> DemographicVector.PROFESSIONAL
            else -> DemographicVector.ALL
        }

        val risk = when {
            activeMatches.size >= 2 || activeMatches.any { 
                it in listOf("digital arrest", "rbi escrow", "liquidate fixed deposit", "semester fee", "crypto recharge", "recharge wallet", "viral on instagram", "arrest warrant") 
            } -> RiskLevel.CRITICAL
            activeMatches.isNotEmpty() -> RiskLevel.WARN
            else -> RiskLevel.SAFE
        }

        val summary = when (risk) {
            RiskLevel.CRITICAL -> when (dominantVector) {
                DemographicVector.STUDENT -> "🚨 STUDENT THREAT DETECTED: Fake task/fee extortion script spotted ('${activeMatches.take(3).joinToString(", ")}'). Do NOT pay or recharge!"
                DemographicVector.SENIOR -> "🚨 SENIOR DIGITAL ARREST INTERCEPTED: Impersonation & liquidation demand ('${activeMatches.take(3).joinToString(", ")}'). HANG UP!"
                DemographicVector.PROFESSIONAL -> "🚨 CORPORATE EXTORTION INTERCEPTED: FedEx/TRAI parcel pretext ('${activeMatches.take(3).joinToString(", ")}'). Do NOT share OTP or funds!"
                DemographicVector.ALL -> "🚨 CRITICAL COERCION DETECTED: High-pressure extortion markers ('${activeMatches.take(3).joinToString(", ")}')."
            }
            RiskLevel.WARN -> "⚠️ SUSPICIOUS CALL: Keywords detected ('${activeMatches.joinToString(", ")}'). Stay alert."
            RiskLevel.SAFE -> "Normal conversation."
            else -> "Normal conversation."
        }

        return VectorThreatResult(
            matchedVector = dominantVector,
            matchedKeywords = activeMatches,
            threatSummary = summary,
            riskLevel = risk
        )
    }
}
