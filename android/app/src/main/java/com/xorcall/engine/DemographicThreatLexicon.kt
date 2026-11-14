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
 * DemographicThreatLexicon — 4-Stage Kill-Chain Psychological Conjunction Engine
 * Eliminates naive single-keyword false alarms (e.g. "daughter", "hospital", "lakhs").
 * 
 * A threat reaches CRITICAL only upon co-occurrence of:
 * [Stage 1: Pretext Hook] + [Stage 2: Crisis/Authority Intimidation] + [Stage 3: Isolation] + [Stage 4: Payment Demand]
 * 
 * Benign Mitigators (Hospital presence, Institutional ERP, Routine Workplace, Family Chat)
 * actively suppress alarms on legitimate daily calls.
 */
object DemographicThreatLexicon {

    // --- Intent Signal Categories ---

    // 1. Authority Impersonation
    private val AUTHORITY_SIGNALS = listOf(
        "cbi", "crime branch", "narcotics", "ncb", "cyber crime", "trai", "department of telecommunication",
        "telecom regulatory", "rbi", "customs", "supreme court", "high court", "magistrate", "police officer",
        "inspector", "dcp", "acp", "sho", "delhi police", "mumbai police", "interpol", "cyber cell",
        "enforcement directorate", "ed", "national crime records", "mha"
    )

    // 2. Legal Coercion & Arrest Threats
    private val LEGAL_THREAT_SIGNALS = listOf(
        "arrest warrant", "you will be arrested", "digital arrest", "money laundering", "fir registered",
        "fir filed", "non-bailable", "giraftaar", "giraftari", "section 144", "official secrets act",
        "pmla", "crpc", "warrant nikla", "disciplinary arrest", "bank account freeze", "account seizure",
        "blacklisted pan", "jail hogi", "jail bhejenge", "national security violation", "court order"
    )

    // 3. Contraband / Parcel Pretext
    private val PARCEL_SIGNALS = listOf(
        "parcel seized", "customs parcel", "fedex parcel", "dhl courier", "contraband", "mdma",
        "narcotics parcel", "illegal drugs", "taiwan parcel", "cambodia courier", "passports seized",
        "aadhaar compromised", "sim card linked to crime", "illegal consignment"
    )

    // 4. Psychological & Sensory Confinement / Secrecy Pressure (The Hallmark of Digital Arrest)
    private val ISOLATION_SIGNALS = listOf(
        "stay on the video", "stay on video call", "stay on camera", "do not disconnect", "don't disconnect",
        "do not hang up", "don't hang up", "do not tell anyone", "don't tell anyone", "do not inform your family",
        "kisi ko mat bata", "kisi se baat mat", "confidential investigation", "lock your room", "room lock karo",
        "camera on rakho", "door lock", "strictly confidential", "gag order", "do not tell your children"
    )

    // 5. Coercive Financial Demand / Extraction (Unverified Accounts / Immediate Transfers)
    private val PAYMENT_SIGNALS = listOf(
        "wire transfer", "transfer immediately", "send money now", "pay immediately", "pay right now",
        "transfer 5 lakhs", "transfer 5 lakh", "5 lakhs transfer", "send 5 lakhs", "pay 5 lakhs",
        "rbi escrow", "secret verification account", "safe custody account", "supervision account",
        "bail amount", "bail money", "security deposit", "verification amount", "penalty fee",
        "recharge wallet", "crypto recharge", "send on upi", "is upi pe bhejo", "paise transfer karo",
        "liquidate fixed deposit", "break fd", "transfer to rbi account", "advance wire"
    )

    // 6. Urgency Pressure & Ultimatums
    private val URGENCY_SIGNALS = listOf(
        "immediately", "right now", "within 15 minutes", "within 30 minutes", "within 1 hour",
        "last warning", "turant", "jaldi", "immediate arrest", "before 5 pm", "deadline expires",
        "or else", "we know where you live", "harm your family", "destroy your career"
    )

    // 7. Kinship Emergency Hook (Family Impersonation / Virtual Kidnapping)
    private val KINSHIP_EMERGENCY_SIGNALS = listOf(
        "accident ho gaya", "police lockup", "police custody", "bacha lo", "mar jayenge",
        "hospitalized in critical condition", "emergency surgery needed", "inspector wants bail",
        "in our custody", "kidnapped your son", "kidnapped your daughter", "hostage", "kill your son",
        "kill your daughter", "murder your child"
    )

    // 8. Student & Youth Task Traps
    private val STUDENT_TASK_SIGNALS = listOf(
        "telegram task", "part-time job", "review job", "like youtube videos", "rating task",
        "crypto recharge", "recharge wallet", "freeze balance", "commission payout", "usdt deposit",
        "prepaid task", "vip task level", "withdrawal fee", "hall ticket withheld", "semester fee due",
        "seat cancelled", "admission cancelled", "video call recording leak", "viral on instagram"
    )

    // 9. Corporate Whaling (CEO / Boss Impersonation)
    private val CORPORATE_SIGNALS = listOf(
        "in a board meeting", "in an investor meeting", "do not call my number", "confidential acquisition",
        "urgent vendor wire", "advance wire transfer", "ceo directive", "buy apple gift cards"
    )

    // --- Benign Mitigators (Protects Legitimate Daily Calls) ---

    // A. Physical Hospital Presence (Legitimate Doctor / Emergency Triage with zero payment demand)
    private val HOSPITAL_MITIGATORS = listOf(
        "please come to the hospital", "visit the hospital", "admitted at apollo", "admitted at hospital",
        "emergency ward", "reception counter", "billing desk pe aao", "hospital aa jaiye",
        "meet doctor in person", "come to the clinic", "casualty ward", "visiting hours"
    )

    // B. Official Institutional Portal (University ERP / Official accounts)
    private val INSTITUTIONAL_MITIGATORS = listOf(
        "official college erp", "login to student portal", "pay through university erp",
        "official website only", "accounts office at admin block", "bursar counter", "dean office"
    )

    // C. Routine Workplace Communication
    private val WORKPLACE_MITIGATORS = listOf(
        "check jira ticket", "review pull request", "github pr", "scheduled calendar invite",
        "daily standup", "slack thread", "confluence doc", "sprint planning"
    )

    // D. Benign Kinship Words (When spoken casually without threats or financial demands)
    private val BENIGN_KINSHIP_TOKENS = listOf("daughter", "son", "kids", "children", "hospital", "doctor", "lakhs", "lakh", "rupees", "fee", "college")

    /**
     * Evaluates text using the 4-Stage Psychological Kill-Chain Conjunction Model.
     * Incorporates PhoneticSpeechNormalizer for robust Indian English telephony transcription.
     */
    fun evaluate(text: String, scope: DemographicVector = DemographicVector.ALL): VectorThreatResult {
        val lower = text.lowercase().replace("\n", " ").trim()
        val norm = PhoneticSpeechNormalizer.normalize(text)

        val matchesSignal = { signals: List<String> ->
            signals.any { sig -> lower.contains(sig) || norm.contains(sig) }
        }

        // Match categorical intent signals
        val hasAuthority = matchesSignal(AUTHORITY_SIGNALS)
        val hasLegalThreat = matchesSignal(LEGAL_THREAT_SIGNALS)
        val hasParcel = matchesSignal(PARCEL_SIGNALS)
        val hasIsolation = matchesSignal(ISOLATION_SIGNALS)
        val hasPaymentDemand = matchesSignal(PAYMENT_SIGNALS)
        val hasUrgency = matchesSignal(URGENCY_SIGNALS)
        val hasKinshipEmergency = matchesSignal(KINSHIP_EMERGENCY_SIGNALS)
        val hasStudentTask = matchesSignal(STUDENT_TASK_SIGNALS)
        val hasCorporate = matchesSignal(CORPORATE_SIGNALS)

        // Match benign mitigators
        val hasHospitalMitigator = matchesSignal(HOSPITAL_MITIGATORS)
        val hasInstitutionalMitigator = matchesSignal(INSTITUTIONAL_MITIGATORS)
        val hasWorkplaceMitigator = matchesSignal(WORKPLACE_MITIGATORS)

        // 1. BENIGN MITIGATION FIRST: Physical Hospital Visit, Institutional Portal, or Workplace
        if ((hasHospitalMitigator || hasInstitutionalMitigator || hasWorkplaceMitigator) && !hasPaymentDemand) {
            return VectorThreatResult(
                matchedVector = scope,
                matchedKeywords = listOf("Benign Mitigator Verified"),
                threatSummary = "🟢 Legitimate call: Medical triage / institutional / routine workplace discussion verified.",
                riskLevel = RiskLevel.SAFE
            )
        }

        // 2. CASUAL CONVERSATION CHECK:
        // Everyday words like "daughter", "hospital", "lakhs" with NO authority threats or payment demands are 100% SAFE.
        val hasBenignWordsOnly = BENIGN_KINSHIP_TOKENS.any { lower.contains(it) || norm.contains(it) }
        val hasExtortionIntent = hasPaymentDemand || hasLegalThreat || hasIsolation || hasKinshipEmergency || hasStudentTask || hasCorporate

        if (hasBenignWordsOnly && !hasExtortionIntent) {
            return VectorThreatResult(
                matchedVector = scope,
                matchedKeywords = emptyList(),
                threatSummary = "🟢 Normal conversation: Everyday family, medical, or financial discussion nominal.",
                riskLevel = RiskLevel.SAFE
            )
        }

        // 3. MULTI-STAGE KILL-CHAIN CONJUNCTION RULES:

        // Rule 1: Full Digital Arrest (Authority/Legal + Isolation/Secrecy + Coercive Payment)
        val isDigitalArrestKillChain = (hasAuthority || hasLegalThreat || hasParcel) && hasIsolation && hasPaymentDemand

        // Rule 2: Impersonation Extortion (Authority / Legal Threat + Direct Payment Demand)
        // Law enforcement/CBI/courts NEVER demand money via UPI or immediate transfers over the phone
        val isAuthorityPaymentExtortion = (hasAuthority || hasLegalThreat) && hasPaymentDemand

        // Rule 3: Kinship Emergency Extortion (Accident / Police Custody / Kidnap + Immediate Payment)
        val isKinshipExtortion = hasKinshipEmergency && hasPaymentDemand

        // Rule 4: Violent Kidnap / Ransom Threat
        val isViolentKidnap = (lower.contains("kidnap") || lower.contains("kill") || lower.contains("murder") || lower.contains("hostage")) &&
                (lower.contains("son") || lower.contains("daughter") || lower.contains("child")) &&
                (hasPaymentDemand || lower.contains("ransom") || hasUrgency)

        // Rule 5: Student Task Scam (Task Hook + Recharge/Deposit Demand)
        val isStudentTaskScam = hasStudentTask && hasPaymentDemand

        // Rule 6: Corporate CEO Whaling (Meeting Hook + Urgent Wire Transfer)
        val isCorporateScam = hasCorporate && hasPaymentDemand

        // Evaluate Conjunction
        val isCritical = isDigitalArrestKillChain || isAuthorityPaymentExtortion || isKinshipExtortion ||
                isViolentKidnap || isStudentTaskScam || isCorporateScam

        if (isCritical) {
            val matchedCategories = mutableListOf<String>()
            if (hasAuthority) matchedCategories.add("Authority Impersonation")
            if (hasLegalThreat) matchedCategories.add("Legal/Arrest Threat")
            if (hasIsolation) matchedCategories.add("Psychological Isolation")
            if (hasPaymentDemand) matchedCategories.add("Coercive Payment Demand")
            if (hasKinshipEmergency) matchedCategories.add("Kinship Emergency Hook")
            if (hasStudentTask) matchedCategories.add("Task Scam Pretext")
            if (hasCorporate) matchedCategories.add("Corporate Whaling")

            val vector = when {
                isKinshipExtortion || isViolentKidnap -> DemographicVector.ALL
                isStudentTaskScam -> DemographicVector.STUDENT
                isCorporateScam -> DemographicVector.PROFESSIONAL
                isDigitalArrestKillChain -> DemographicVector.SENIOR
                else -> scope
            }

            val summary = when {
                isDigitalArrestKillChain ->
                    "🚨 DIGITAL ARREST SCAM INTERCEPTED: Fake authority coercion, video isolation & asset transfer demanded (${matchedCategories.joinToString(" + ")}). HANG UP IMMEDIATELY!"
                isKinshipExtortion || isViolentKidnap ->
                    "🚨 FAMILY KIDNAP / EXTORTION INTERCEPTED: Fabricated kinship crisis & ransom demand spotted (${matchedCategories.joinToString(" + ")}). DO NOT TRANSFER FUNDS!"
                isStudentTaskScam ->
                    "🚨 STUDENT TELEGRAM / TASK SCAM INTERCEPTED: Prepaid task trap & balance recharge extortion spotted. DO NOT DEPOSIT MONEY!"
                isCorporateScam ->
                    "🚨 CORPORATE WHALING / WIRE SCAM INTERCEPTED: CEO impersonation & urgent transfer demand spotted. Verify with manager directly!"
                else ->
                    "🚨 CRITICAL CYBER EXTORTION INTERCEPTED: Multi-stage coercion & unverified payment demand spotted (${matchedCategories.joinToString(" + ")}). HANG UP!"
            }

            return VectorThreatResult(
                matchedVector = vector,
                matchedKeywords = matchedCategories,
                threatSummary = summary,
                riskLevel = RiskLevel.CRITICAL
            )
        }

        // Suspicious / Partial Conjunction (Pretext or Legal threat without payment demand yet)
        if (hasAuthority && hasLegalThreat) {
            return VectorThreatResult(
                matchedVector = scope,
                matchedKeywords = listOf("Authority Claim", "Legal Threat"),
                threatSummary = "⚠️ SUSPICIOUS CALL: High-pressure authority & legal threats detected. Do not isolate yourself or transfer funds.",
                riskLevel = RiskLevel.WARN
            )
        }

        // Standalone signals with zero extortion conjunction remain SAFE
        return VectorThreatResult(
            matchedVector = scope,
            matchedKeywords = emptyList(),
            threatSummary = "🟢 Normal speech: Spoken dialogue nominal.",
            riskLevel = RiskLevel.SAFE
        )
    }
}
