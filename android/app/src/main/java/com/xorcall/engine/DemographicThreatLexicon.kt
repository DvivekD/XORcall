package com.xorcall.engine

import java.util.regex.Pattern

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
 * Eliminates naive single-keyword false alarms (e.g. casual "daughter", "hospital", "lakhs").
 * 
 * Uses deterministic regular expressions modeled after federal cybercrime intelligence
 * (CBI, Delhi Cyber Cell, I4C, CERT-In, US FTC).
 * 
 * Conjunction Rules:
 * 1. "Digital arrest" is 100% a criminal fraud construct -> Immediate CRITICAL.
 * 2. Law enforcement never asks for money transfers/UPI over phone -> Authority/Legal Threat + Payment Demand -> Immediate CRITICAL.
 * 3. Kinship Emergency Hook + Payment Demand -> Immediate CRITICAL.
 * 4. Task Trap Hook + Recharge/Deposit -> Immediate CRITICAL.
 * 5. CEO / Boss Pretext + Wire Transfer -> Immediate CRITICAL.
 * 
 * Benign Mitigators (Hospital presence, Institutional ERP, Routine Workplace, Family Chat)
 * actively suppress alarms on legitimate daily calls.
 */
object DemographicThreatLexicon {

    // --- Intent Signal Regular Expressions ---

    // 1. Authority Impersonation
    private val AUTHORITY_REGEX = Pattern.compile(
        "\\b(cbi|crime branch|narcotics|ncb|cyber\\s?crime|trai|dot|telecom department|rbi|" +
        "customs|supreme court|high court|magistrate|police officer|inspector|dcp|acp|sho|" +
        "delhi police|mumbai police|interpol|cyber cell|enforcement directorate|ed|mha|i4c|national crime records)\\b",
        Pattern.CASE_INSENSITIVE
    )

    // 2. Legal Coercion & Arrest Threats
    private val LEGAL_THREAT_REGEX = Pattern.compile(
        "\\b(arrest warrant|digital arrest|you will be arrested|money laundering|" +
        "fir (registered|filed)|non[- ]bailable|giraftaar|giraftari|section 144|official secrets act|" +
        "pmla|crpc|warrant nikla|disciplinary arrest|bank account freeze|account seizure|" +
        "blacklisted pan|jail hogi|jail bhejenge|national security violation|court order)\\b",
        Pattern.CASE_INSENSITIVE
    )

    // 3. Contraband / Parcel Pretext
    private val PARCEL_REGEX = Pattern.compile(
        "\\b(parcel|courier|fedex|dhl|blue dart|contraband|drugs|mdma|narcotics parcel|sim card|" +
        "aadhaar (compromised|linked|misused)|illegal (consignment|drugs|activity)|taiwan parcel|" +
        "cambodia courier|passports? seized|customs hold)\\b",
        Pattern.CASE_INSENSITIVE
    )

    // 4. Psychological & Sensory Confinement / Secrecy Pressure (Hallmark of Digital Arrest)
    private val ISOLATION_REGEX = Pattern.compile(
        "\\b(stay on (the )?(video|camera|call|line)|do not (disconnect|cut)|don'?t (disconnect|cut)|" +
        "do not hang up|don'?t hang up|do not tell (anyone|your family|colleagues|children)|" +
        "don'?t tell (anyone|your family|colleagues|children)|kisi ko mat bata|kisi se baat mat|" +
        "confidential investigation|lock your room|room lock karo|camera on rakho|door lock|" +
        "strictly confidential|gag order|do not inform your family)\\b",
        Pattern.CASE_INSENSITIVE
    )

    // 5. Coercive Financial Demand / Extraction (Unverified Accounts / Immediate Transfers / Lakhs)
    private val PAYMENT_REGEX = Pattern.compile(
        "(?i)(?:(?:transfer|deposit|send|wire|pay|bhej[ao]?|jama|daal)\\s+(?:.*?)(?:lakh|lakhs|lac|lacs|crore|crores|rupees|rs|inr|usd|dollars?|funds?|money|amount|\\d{3,9}))|" +
        "(?:(?:\\d{3,9}|lakh|lakhs|lac|lacs|crore|crores|rupees|rs)\\s+(?:.*?)(?:transfer|deposit|bhej[ao]?|pay|send|jama))|" +
        "(?:(?:transfer|deposit|send|pay|wire)\\s+(?:.*?)(?:to|into|pe)?\\s*(?:rbi|escrow|verification|custody|supervision|safe|secret|police|cbi|court)\\s*(?:account|fund|treasury|portal|id)?)|" +
        "(?:rbi\\s*(?:escrow|account|safe\\s*account))|" +
        "(?:safe\\s*custody\\s*account)|" +
        "(?:secret\\s*verification\\s*account)|" +
        "(?:supervision\\s*account)|" +
        "(?:bail\\s*(?:amount|money))|" +
        "(?:security\\s*deposit)|" +
        "(?:verification\\s*amount)|" +
        "(?:penalty\\s*fee)|" +
        "(?:recharge\\s*(?:wallet|balance))|" +
        "(?:crypto\\s*(?:recharge|deposit))|" +
        "(?:send\\s+on\\s+upi)|" +
        "(?:is\\s+upi\\s+pe\\s+bhejo)|" +
        "(?:paise\\s*(?:transfer|bhej[ao]?|jama))|" +
        "(?:liquidate\\s*fixed\\s*deposit)|" +
        "(?:break\\s*fd)|" +
        "(?:advance\\s*wire)"
    )

    // 6. Urgency Pressure & Ultimatums
    private val URGENCY_REGEX = Pattern.compile(
        "\\b(immediately|right now|within \\d+ (minutes|hours)|last warning|turant|jaldi|" +
        "immediate arrest|before \\d+ pm|deadline expires|or else|we know where you live|harm your family|destroy your career)\\b",
        Pattern.CASE_INSENSITIVE
    )

    // 7. Kinship Emergency Pretext (Family Impersonation / Virtual Kidnapping)
    private val KINSHIP_EMERGENCY_REGEX = Pattern.compile(
        "(?i)(?:(?:papa|mummy|dad|mom|uncle|grandpa|grandma|beta).{0,35}(?:accident|police|custody|arrest|hospital|jail|bacha lo|help me|mar jayenge|trouble|emergency|phone chori|pakad liya|giraftaar))|" +
        "(?:(?:kidnap|kidnapped|kill|hostage|murder).{0,35}(?:son|daughter|child|brother|sister|father|mother))",
        Pattern.CASE_INSENSITIVE
    )

    // 8. Student & Youth Task Traps
    private val STUDENT_TASK_REGEX = Pattern.compile(
        "(?i)(?:(?:telegram|whatsapp)\\s*(?:task|job|group))|(?:part[- ]time\\s*job)|(?:(?:youtube|instagram)\\s*(?:like|review|subscribe))|" +
        "(?:recharge\\s*(?:crypto|wallet|balance|usdt))|(?:prepaid\\s*task)|(?:freeze\\s*balance)|(?:commission\\s*payout)|" +
        "(?:hall\\s*ticket\\s*withheld)|(?:admission\\s*cancelled)|(?:video\\s*call\\s*recording\\s*leak)",
        Pattern.CASE_INSENSITIVE
    )

    // 9. Corporate Whaling (CEO / Boss Impersonation)
    private val CORPORATE_REGEX = Pattern.compile(
        "(?i)(?:in a\\s*(?:board|investor|client)?\\s*meeting)|(?:do not call my (?:number|phone))|" +
        "(?:urgent\\s*(?:vendor|wire|bank)\\s*(?:transfer|payment))|(?:ceo\\s*(?:directive|instruction))|(?:confidential\\s*acquisition)|" +
        "(?:buy\\s*apple\\s*gift\\s*cards)",
        Pattern.CASE_INSENSITIVE
    )

    // --- Benign Mitigators (Protects Legitimate Daily Calls) ---

    // A. Physical Hospital Presence (Legitimate Doctor / Emergency Triage with zero payment demand)
    private val HOSPITAL_MITIGATORS_REGEX = Pattern.compile(
        "(?i)(?:please come to the hospital|visit the hospital|admitted at (?:apollo|hospital)|emergency ward|reception counter|billing desk pe aao|hospital aa jaiye|meet doctor in person|come to the clinic|casualty ward|visiting hours)"
    )

    // B. Official Institutional Portal (University ERP / Official accounts)
    private val INSTITUTIONAL_MITIGATORS_REGEX = Pattern.compile(
        "(?i)(?:official college erp|login to student portal|pay through university erp|official website only|accounts office at admin block|bursar counter|dean office)"
    )

    // C. Routine Workplace Communication
    private val WORKPLACE_MITIGATORS_REGEX = Pattern.compile(
        "(?i)(?:check jira ticket|review pull request|github pr|scheduled calendar invite|daily standup|slack thread|confluence doc|sprint planning)"
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

        val checkMatch = { pattern: Pattern ->
            pattern.matcher(lower).find() || pattern.matcher(norm).find()
        }

        // Match categorical intent signals
        val hasAuthority = checkMatch(AUTHORITY_REGEX)
        val hasLegalThreat = checkMatch(LEGAL_THREAT_REGEX)
        val hasParcel = checkMatch(PARCEL_REGEX)
        val hasIsolation = checkMatch(ISOLATION_REGEX)
        val hasPaymentDemand = checkMatch(PAYMENT_REGEX)
        val hasUrgency = checkMatch(URGENCY_REGEX)
        val hasKinshipEmergency = checkMatch(KINSHIP_EMERGENCY_REGEX)
        val hasStudentTask = checkMatch(STUDENT_TASK_REGEX)
        val hasCorporate = checkMatch(CORPORATE_REGEX)

        // Match benign mitigators
        val hasHospitalMitigator = checkMatch(HOSPITAL_MITIGATORS_REGEX)
        val hasInstitutionalMitigator = checkMatch(INSTITUTIONAL_MITIGATORS_REGEX)
        val hasWorkplaceMitigator = checkMatch(WORKPLACE_MITIGATORS_REGEX)

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
        val hasExtortionIntent = hasPaymentDemand || hasLegalThreat || hasIsolation || hasKinshipEmergency || hasStudentTask || hasCorporate || hasAuthority

        if (hasBenignWordsOnly && !hasExtortionIntent) {
            return VectorThreatResult(
                matchedVector = scope,
                matchedKeywords = emptyList(),
                threatSummary = "🟢 Normal conversation: Everyday family, medical, or financial discussion nominal.",
                riskLevel = RiskLevel.SAFE
            )
        }

        // 3. MULTI-STAGE KILL-CHAIN CONJUNCTION RULES:

        // Smoking Gun: Any mention of "digital arrest" (a 100% fake construct used only by cyber syndicates)
        val isExplicitDigitalArrest = lower.contains("digital arrest") || norm.contains("digital arrest")

        // Rule 1A: Digital Arrest Conjunction (Authority / Legal / Parcel + Isolation + Coercive Payment)
        val isDigitalArrestKillChain = (hasAuthority || hasLegalThreat || hasParcel) && hasIsolation && hasPaymentDemand

        // Rule 1B: High-Pressure Authority / Legal Extortion (Authority / Legal Threat + Direct Payment Demand)
        // Law enforcement/CBI/courts NEVER demand money via UPI, RTGS, or transfers over the phone
        val isAuthorityPaymentExtortion = (hasAuthority || hasLegalThreat) && hasPaymentDemand

        // Rule 1C: Parcel / Customs Extortion
        val isParcelPaymentExtortion = hasParcel && hasPaymentDemand

        // Rule 1D: Authority Coercion with Isolation (Digital Arrest Pretext before money demand)
        val isAuthorityIsolationCoercion = hasAuthority && hasLegalThreat && hasIsolation

        // Rule 2: Kinship Emergency Extortion (Accident / Police Custody / Kidnap + Immediate Payment)
        val isKinshipExtortion = hasKinshipEmergency && hasPaymentDemand

        // Rule 3: Violent Kidnap / Ransom Threat
        val isViolentKidnap = (lower.contains("kidnap") || lower.contains("kill") || lower.contains("murder") || lower.contains("hostage")) &&
                (lower.contains("son") || lower.contains("daughter") || lower.contains("child")) &&
                (hasPaymentDemand || lower.contains("ransom") || hasUrgency)

        // Rule 4: Student Task Scam (Task Hook + Recharge/Deposit Demand)
        val isStudentTaskScam = hasStudentTask && hasPaymentDemand

        // Rule 5: Corporate CEO Whaling (Meeting Hook + Urgent Wire Transfer)
        val isCorporateScam = hasCorporate && hasPaymentDemand

        // Evaluate Conjunction for CRITICAL
        val isCritical = isExplicitDigitalArrest || isDigitalArrestKillChain || isAuthorityPaymentExtortion ||
                isParcelPaymentExtortion || isAuthorityIsolationCoercion || isKinshipExtortion ||
                isViolentKidnap || isStudentTaskScam || isCorporateScam

        if (isCritical) {
            val matchedCategories = mutableListOf<String>()
            if (isExplicitDigitalArrest) matchedCategories.add("Digital Arrest Keyword")
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
                isDigitalArrestKillChain || isExplicitDigitalArrest || isAuthorityPaymentExtortion -> DemographicVector.SENIOR
                else -> scope
            }

            val summary = when {
                isDigitalArrestKillChain || isExplicitDigitalArrest || isAuthorityPaymentExtortion ->
                    "🚨 DIGITAL ARREST SCAM INTERCEPTED: Fake authority coercion & illegal asset transfer demanded (${matchedCategories.joinToString(" + ")}). HANG UP IMMEDIATELY!"
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

        // Suspicious / Partial Conjunction (Authority or Legal threat without payment demand yet)
        if (hasAuthority && hasLegalThreat) {
            return VectorThreatResult(
                matchedVector = scope,
                matchedKeywords = listOf("Authority Claim", "Legal Threat"),
                threatSummary = "⚠️ SUSPICIOUS CALL: High-pressure authority & arrest warrant claim detected. Do not isolate yourself or transfer funds.",
                riskLevel = RiskLevel.WARN
            )
        }

        if (hasAuthority || hasLegalThreat || hasParcel || hasKinshipEmergency) {
            val signalName = when {
                hasAuthority -> "Authority Impersonation Pretext"
                hasLegalThreat -> "Legal Intimidation Pretext"
                hasParcel -> "Customs Parcel Pretext"
                else -> "Kinship Emergency Pretext"
            }
            return VectorThreatResult(
                matchedVector = scope,
                matchedKeywords = listOf(signalName),
                threatSummary = "👀 WATCH: $signalName observed. Verifying caller authenticity.",
                riskLevel = RiskLevel.WATCH
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
