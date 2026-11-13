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
 * 1. Kidnap, Death Threats & Violent Extortion (e.g. "son", "killing", "5 lakhs", "kidnapped", "murder")
 * 2. Universal Financial Extortion & Ransom (e.g. "5 million dollar extraction", "wire transfer")
 * 3. Student Vector (Telegram Task Traps, Fake Semester Fees, Hall Ticket Withheld, Sextortion)
 * 4. Senior Citizen Vector (Digital Arrest, CBI/Police, Pension/KYC Suspension, FD Liquidation, RBI Escrow)
 * 5. Working Professional Vector (FedEx/Customs MDMA Parcel, TRAI Number Severing, GST/IT Raids)
 */
object DemographicThreatLexicon {

    // 0. Universal Financial Extortion, Kidnap & Death Threats (Active across ALL vectors)
    val UNIVERSAL_EXTORTION_KEYWORDS = listOf(
        // Kidnapping & Violent Death Threats (User Test Vector)
        "kill", "killing", "killed", "murder", "murdered", "shoot", "dead", "die", "death",
        "son", "daughter", "child", "children", "kid", "kids", "brother", "sister", "mother", "father", "family",
        "kidnap", "kidnapped", "kidnapping", "hostage", "abduct", "abducted", "in our custody", "in custody",
        
        // Currency & High-Denomination Demands
        "5 lakhs", "lakh", "lakhs", "500000", "crore", "crores", "rupees", "cash", "ransom",
        "million dollar", "million dollars", "5 million", "million", "millions", "dollar", "dollars",
        "extraction", "extract money", "extortion", "extort",

        // Accident & Urgent Hospital Coercion
        "accident", "critical condition", "hospitalized", "icu", "emergency surgery",
        "police custody", "bail money", "bail amount", "custody release",

        // Forceful Transfer & Ultimatums
        "wire transfer", "transfer immediately", "pay immediately", "send money now",
        "pay right now", "urgent payment", "demand money", "demanding money",
        "clear the penalty", "settlement fee", "or else", "consequences",
        "we know where you live", "harm your family", "destroy your career"
    )

    // 1. Student & Youth Threat Keywords
    val STUDENT_KEYWORDS = listOf(
        // Telegram & Task Fraud
        "telegram", "part-time job", "review job", "like youtube videos", "rating task",
        "crypto recharge", "recharge wallet", "freeze balance", "commission", "usdt", "deposit money",
        "task completion", "prepaid task", "vip level", "withdrawal fee",
        // Campus & Semester Fee Traps
        "semester fee", "exam fee", "hall ticket", "hall ticket withheld", "cheating in exam",
        "police complaint from dean", "hostel raid", "university penalty", "college administration",
        "admission cancelled", "rusticate", "disciplinary committee", "backlog clearance fee", "internship deposit",
        // Sextortion & Video Blackmail
        "video call recording", "leaked video", "nude video", "viral on instagram", "youtube upload",
        "send money on upi", "phonepe", "gpay", "share to all contacts", "contacts backup", "blackmail"
    )

    // 2. Senior Citizen & Retiree Threat Keywords
    val SENIOR_KEYWORDS = listOf(
        // Digital Arrest & High-Pressure Coercion
        "digital arrest", "supreme court", "cbi", "narcotics", "mumbai police", "customs parcel",
        "mdma", "contraband", "money laundering", "naresh goyal", "arrest warrant", "section 144",
        "official secrets act", "gag order", "do not disconnect", "stay on camera", "lock your room",
        "stay on video call", "do not inform your children", "do not tell anyone", "national security investigation",
        // Financial Liquidation
        "rbi escrow", "secret verification account", "liquidate fixed deposit", "break fd",
        "transfer to rbi account", "supervision account", "clearing account", "mutual fund liquidation",
        "safe custody account", "refunded in 2 hours",
        // Pension & Utility Fraud
        "pension block", "jeevan pramaan", "life certificate", "electricity disconnection tonight",
        "bank account suspended", "aadhaar biometric lock", "sim verification"
    )

    // 3. Working Professional & Corporate Threat Keywords
    val PROFESSIONAL_KEYWORDS = listOf(
        // FedEx / Customs Parcel Pretext
        "fedex", "dhl", "customs", "parcel intercepted", "taiwan", "thailand", "cambodia",
        "5 expired passports", "illegal parcel", "mdma", "narcotics drugs", "trai",
        "department of telecommunications", "sim cards in your name", "9 sim cards",
        "mobile disconnection in 2 hours", "crime branch andheri", "cyber police station",
        // Corporate & Tax Intimidation
        "income tax raid", "gst notice", "director kyc", "corporate bank account freeze",
        "statutory non-compliance", "pmla investigation", "audit clearance", "enforcement directorate"
    )

    /**
     * Evaluates text against a specific demographic scope or ALL scopes.
     * Universal extortion markers are always evaluated regardless of chosen persona.
     */
    fun evaluate(text: String, scope: DemographicVector = DemographicVector.ALL): VectorThreatResult {
        val lower = text.lowercase().replace("\n", " ").trim()

        val universalMatches = UNIVERSAL_EXTORTION_KEYWORDS.filter { lower.contains(it) }
        val studentMatches = STUDENT_KEYWORDS.filter { lower.contains(it) }
        val seniorMatches = SENIOR_KEYWORDS.filter { lower.contains(it) }
        val profMatches = PROFESSIONAL_KEYWORDS.filter { lower.contains(it) }

        // Assemble active matches based on scope
        val activeMatches = when (scope) {
            DemographicVector.STUDENT -> (studentMatches + universalMatches).distinct()
            DemographicVector.SENIOR -> (seniorMatches + universalMatches).distinct()
            DemographicVector.PROFESSIONAL -> (profMatches + universalMatches).distinct()
            DemographicVector.ALL -> (universalMatches + studentMatches + seniorMatches + profMatches).distinct()
        }

        // Determine dominant vector
        val dominantVector = when {
            scope != DemographicVector.ALL -> scope
            studentMatches.size > seniorMatches.size && studentMatches.size > profMatches.size -> DemographicVector.STUDENT
            profMatches.size > seniorMatches.size && profMatches.size > studentMatches.size -> DemographicVector.PROFESSIONAL
            seniorMatches.isNotEmpty() -> DemographicVector.SENIOR
            studentMatches.isNotEmpty() -> DemographicVector.STUDENT
            profMatches.isNotEmpty() -> DemographicVector.PROFESSIONAL
            universalMatches.isNotEmpty() -> DemographicVector.ALL
            else -> DemographicVector.ALL
        }

        // High-severity markers trigger CRITICAL immediately on the first phrase
        val criticalTriggers = listOf(
            "kill", "killing", "murder", "son", "daughter", "kidnap", "kidnapped", "hostage", "5 lakhs", "lakh",
            "million dollar", "million dollars", "extraction", "extort", "ransom", "accident",
            "digital arrest", "rbi escrow", "liquidate fixed deposit", "break fd", "arrest warrant",
            "semester fee", "crypto recharge", "recharge wallet", "viral on instagram", "video call recording",
            "parcel intercepted", "fedex", "trai", "mobile disconnection", "income tax raid"
        )

        val hasCriticalKeyword = activeMatches.any { match ->
            criticalTriggers.any { trigger -> match.contains(trigger) || trigger.contains(match) }
        }

        val risk = when {
            hasCriticalKeyword || activeMatches.isNotEmpty() -> RiskLevel.CRITICAL
            else -> RiskLevel.SAFE
        }

        val summary = when (risk) {
            RiskLevel.CRITICAL -> {
                val sample = activeMatches.take(3).joinToString(", ")
                when (dominantVector) {
                    DemographicVector.STUDENT ->
                        "🚨 STUDENT EXTORTION INTERCEPTED: Academic/task trap or extortion demand spotted ('$sample'). Do NOT pay or recharge!"
                    DemographicVector.SENIOR ->
                        "🚨 SENIOR DIGITAL ARREST INTERCEPTED: Impersonation & asset liquidation demand spotted ('$sample'). HANG UP!"
                    DemographicVector.PROFESSIONAL ->
                        "🚨 CORPORATE / PARCEL EXTORTION INTERCEPTED: FedEx/TRAI coercion script spotted ('$sample'). Do NOT share funds or OTP!"
                    DemographicVector.ALL ->
                        "🚨 CRITICAL EXTORTION / KIDNAP THREAT INTERCEPTED: Coercion & ransom demand spotted ('$sample'). HANG UP IMMEDIATELY!"
                }
            }
            RiskLevel.WARN -> "⚠️ SUSPICIOUS CALL: Coercion keywords detected ('${activeMatches.joinToString(", ")}'). Stay alert."
            RiskLevel.SAFE -> "Normal conversation."
        }

        return VectorThreatResult(
            matchedVector = dominantVector,
            matchedKeywords = activeMatches,
            threatSummary = summary,
            riskLevel = risk
        )
    }
}
