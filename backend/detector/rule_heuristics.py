"""
XORcall Rule-Based Deterministic Multi-Modal Intent Heuristics Engine
Evaluates speech transcripts across English, Hindi, and Hinglish.
Runs 100% offline with zero cloud dependency.

Features:
- Multi-Demographic Coverage: Seniors, College Students, Working Professionals, and Parents.
- Mathematical Kill-Chain Intent Conjunction (Stage 1 Hook -> Stage 2 Crisis -> Stage 3 Isolation -> Stage 4 Unverified Payment).
- Benign Mitigators (Hospital presence, Institutional ERP, Routine workplace tasks).
"""
import re
from typing import Dict, List, Tuple, Set

# Deterministic threat signals and weights
SIGNALS: Dict[str, Tuple[float, str]] = {
    # 1. Authority Impersonation (Targets: Seniors & General Public)
    "authority_claim": (
        0.25,
        r"\b(cbi|ed|enforcement directorate|narcotics|narcotics bureau|cyber\s?crime|"
        r"trai|rbi|customs|supreme court|police officer|inspector|crime branch|dcp|acp|"
        r"delhi police|mumbai police|interpol|telecom regulatory|cyber cell)\b"
    ),
    # 2. Fake Legal Threats
    "legal_threat": (
        0.25,
        r"(arrest warrant|you will be arrested|digital arrest|money laundering|"
        r"case (has been|is) (registered|filed)|\bfir\b|non[- ]bailable|giraftaar|"
        r"section 144|official secrets act|pmla|crpc|warrant nikla hai|disciplinary arrest|"
        r"bank account freeze|account seizure|blacklisted pan)"
    ),
    # 3. Contraband / Parcel Pretext
    "parcel_or_sim": (
        0.15,
        r"(parcel|courier|fedex|dhl|contraband|drugs|mdma|narcotics parcel|sim card|"
        r"aadhaar (is|was|has been) (misused|linked|compromised)|illegal (activity|transaction)|"
        r"passport seized|taiwan parcel|cambodia courier)"
    ),
    # 4. Psychological & Sensory Confinement (The Smoking Gun of Digital Arrest)
    "isolation": (
        0.30,
        r"(stay on (the )?(video|call|line)|do not (disconnect|cut)|don'?t (disconnect|cut)|"
        r"do not tell (anyone|your family|colleagues)|don'?t tell (anyone|your family|colleagues)|"
        r"kisi ko mat bata|confidential investigation|stay in (a )?(room|front of camera)|"
        r"room lock karo|camera on rakho|door lock|strictly confidential|nd clause)"
    ),
    # 5. Urgent Financial Extortion Demand
    "payment_demand": (
        0.35,
        r"((transfer|deposit|rtgs|neft|upi|send|pay).{0,40}(money|amount|funds|rupees|lakh|crore|rs\.?|₹)|"
        r"verification (amount|deposit)|safe (rbi )?account|security deposit|"
        r"paise (bhej|transfer|jama)|bail (amount|money)|clinic deposit|task deposit|"
        r"recharge amount|wallet recharge|crypto transfer|advance wire)"
    ),
    # 6. Pressure / Urgency
    "urgency": (
        0.15,
        r"(immediately|right now|within (one|1|two|2|\d+) (hour|hours|minutes)|"
        r"last warning|abhi|turant|jail bhejenge|immediate arrest|within 15 minutes|before 5 pm)"
    ),
    # 7. Kinship Emergency Hook (Family Impersonation / Virtual Kidnapping)
    "kinship_emergency": (
        0.35,
        r"(papa|mummy|dad|mom|uncle|grandpa|grandma|beta).{0,30}(accident|police|custody|arrest|"
        r"hospital|jail|bacha lo|help me|mar jayenge|trouble|emergency|phone chori|friend ke phone)"
    ),
    # 8. Student & Youth Scam Signals (Telegram Part-Time Job / Rating / Task Fraud)
    "task_job_scam": (
        0.30,
        r"(telegram task|part[- ]time job|youtube (like|subscribe)|review hotel|google maps review|"
        r"daily income \d+|earn (2000|3000|5000|10000)|prepaid task|recharge balance to withdraw|"
        r"crypto task|rating commission|task level \d+)"
    ),
    # 9. Corporate / Working Professional Extortion (CEO / Boss Impersonation & Whaling)
    "corporate_wire_scam": (
        0.30,
        r"(i am in a (board )?meeting|do not call my number|urgent wire transfer|ceo directive|"
        r"confidential acquisition|purchase apple gift cards|send vendor payment directly|"
        r"urgent neft from company account|confidential client settlement)"
    ),
    # 10. College / University Phishing (e.g., BITS Pilani ₹7.67 Cr Syndicate Pretext)
    "college_fee_phishing": (
        0.30,
        r"(college fee portal|hostel fee deposit|bits pilani fee|seat cancellation|hall ticket hold|"
        r"semester fee pending|pay directly to registrar upi|admission clearance deposit|"
        r"campus syndicate fine)"
    ),
    # 11. Genuine Physical Medical Guidance (Benign Mitigator)
    "physical_hospital_presence": (
        -0.40,
        r"(please come to|visit the hospital|admitted at|emergency ward|reception counter|"
        r"billing desk pe aao|hospital aa jaiye|meet doctor in person)"
    ),
    # 12. Institutional Official Portal Mitigator
    "institutional_portal_mitigator": (
        -0.35,
        r"(official college erp|login to student portal|pay through university erp|"
        r"official website only|accounts office at admin block|bursar counter)"
    ),
    # 13. Routine Workplace Communication Mitigator
    "routine_workplace_mitigator": (
        -0.35,
        r"(check jira ticket|review pull request|github pr|scheduled calendar invite|"
        r"daily standup|slack thread|confluence doc)"
    )
}

COMPILED_SIGNALS = {k: (w, re.compile(p, re.I)) for k, (w, p) in SIGNALS.items()}

class CallHeuristicSession:
    def __init__(self, session_id: str):
        self.session_id = session_id
        self.seen_flags: Set[str] = set()
        self.accumulated_risk: float = 0.0
        self.log: List[dict] = []

    def feed(self, utterance: str) -> dict:
        matched_flags = []
        sentence_score = 0.0

        for flag, (weight, rx) in COMPILED_SIGNALS.items():
            if rx.search(utterance):
                matched_flags.append(flag)
                sentence_score += weight

        self.seen_flags.update(matched_flags)

        # Non-linear risk accumulation model
        u = max(0.0, min(1.0, sentence_score))
        self.accumulated_risk = 1.0 - (1.0 - self.accumulated_risk) * (1.0 - 0.65 * u)

        # KILL-CHAIN RULE 1: Authority Coercion (Digital Arrest / Seniors & Public)
        # Authority + Isolation + Payment demand = Immediate Critical Alarm
        if {"authority_claim", "isolation", "payment_demand"} <= self.seen_flags:
            self.accumulated_risk = max(self.accumulated_risk, 0.94)

        # KILL-CHAIN RULE 2: Kinship Emergency Extortion (Family Deepfake / Parents)
        # Kinship claim + Payment demand = Immediate Critical Alarm
        if {"kinship_emergency", "payment_demand"} <= self.seen_flags:
            self.accumulated_risk = max(self.accumulated_risk, 0.92)

        # KILL-CHAIN RULE 3: Student Part-Time Job / Task Scam
        # Job/Task offer + Payment demand (recharge to withdraw) = Critical Alarm
        if {"task_job_scam", "payment_demand"} <= self.seen_flags:
            self.accumulated_risk = max(self.accumulated_risk, 0.92)

        # KILL-CHAIN RULE 4: Corporate / CEO Impersonation (Working Professionals)
        # Corporate impersonation + Payment demand + Urgency = Critical Alarm
        if {"corporate_wire_scam", "payment_demand"} <= self.seen_flags:
            self.accumulated_risk = max(self.accumulated_risk, 0.91)

        # KILL-CHAIN RULE 5: College Fee Phishing / Youth Extortion
        # College fee pretext + Payment demand = Critical Alarm
        if {"college_fee_phishing", "payment_demand"} <= self.seen_flags:
            self.accumulated_risk = max(self.accumulated_risk, 0.93)

        # BENIGN MITIGATION 1: Genuine Hospital Visit (Zero payment demanded)
        if "physical_hospital_presence" in self.seen_flags and "payment_demand" not in self.seen_flags:
            self.accumulated_risk = min(self.accumulated_risk, 0.25)

        # BENIGN MITIGATION 2: Institutional Official ERP (Zero private UPI)
        if "institutional_portal_mitigator" in self.seen_flags and "payment_demand" not in self.seen_flags:
            self.accumulated_risk = min(self.accumulated_risk, 0.20)

        # BENIGN MITIGATION 3: Routine Workplace Collaboration
        if "routine_workplace_mitigator" in self.seen_flags and "payment_demand" not in self.seen_flags:
            self.accumulated_risk = min(self.accumulated_risk, 0.15)

        # Determine level
        if self.accumulated_risk >= 0.80:
            level = "CRITICAL"
        elif self.accumulated_risk >= 0.55:
            level = "WARN"
        elif self.accumulated_risk >= 0.30:
            level = "WATCH"
        else:
            level = "SAFE"

        # Demographic Profile Target Inference (inferred from active pretexts)
        if any(f in self.seen_flags for f in ["task_job_scam", "college_fee_phishing"]):
            demographic = "STUDENT_YOUTH"
        elif "corporate_wire_scam" in self.seen_flags:
            demographic = "WORKING_PROFESSIONAL"
        elif "kinship_emergency" in self.seen_flags:
            demographic = "PARENT"
        elif "authority_claim" in self.seen_flags:
            demographic = "SENIOR_CITIZEN"
        else:
            demographic = "GENERAL"

        # Multi-Demographic Threat Classification
        if "kinship_emergency" in self.seen_flags and self.accumulated_risk >= 0.55:
            threat_type = "KINSHIP_DEEPFAKE_EXTORTION"
        elif "task_job_scam" in self.seen_flags and self.accumulated_risk >= 0.55:
            threat_type = "STUDENT_TASK_SCAM"
        elif "college_fee_phishing" in self.seen_flags and self.accumulated_risk >= 0.55:
            threat_type = "COLLEGE_FEE_PHISHING"
        elif "corporate_wire_scam" in self.seen_flags and self.accumulated_risk >= 0.55:
            threat_type = "CEO_IMPERSONATION_FRAUD"
        elif "authority_claim" in self.seen_flags and self.accumulated_risk >= 0.55:
            threat_type = "DIGITAL_ARREST_SCAM"
        elif "physical_hospital_presence" in self.seen_flags:
            threat_type = "GENUINE_MEDICAL_TRIAGE"
        elif "institutional_portal_mitigator" in self.seen_flags or "routine_workplace_mitigator" in self.seen_flags:
            threat_type = "BENIGN_WORK_OR_CAMPUS"
        else:
            threat_type = "UNKNOWN_OR_BENIGN"

        # Calculate Kill-Chain Stage Progression (1 to 5)
        # Stage 1: Pretext Hook
        # Stage 2: Crisis Injection
        # Stage 3: Isolation / Sensory Confinement
        # Stage 4: Financial Extortion Demand
        # Stage 5: Terminal Escalation
        active_stages = []
        if any(f in self.seen_flags for f in ["authority_claim", "kinship_emergency", "task_job_scam", "corporate_wire_scam", "college_fee_phishing"]):
            active_stages.append("STAGE_1_PRETEXT_HOOK")
        if any(f in self.seen_flags for f in ["legal_threat", "parcel_or_sim", "urgency"]):
            active_stages.append("STAGE_2_CRISIS_MANUFACTURE")
        if "isolation" in self.seen_flags:
            active_stages.append("STAGE_3_ISOLATION_CONFINEMENT")
        if "payment_demand" in self.seen_flags:
            active_stages.append("STAGE_4_PAYMENT_EXTORTION")
        if level == "CRITICAL":
            active_stages.append("STAGE_5_TERMINAL_COERCION")

        kill_chain_stage = len(active_stages) if active_stages else 1

        # Synthesize mathematical conjunction explanation
        if level == "CRITICAL":
            conjunction_explanation = (
                f"Multi-Stage Intent Kill-Chain Conjunction Triggered [{demographic}]: "
                f"{' + '.join(active_stages)}. Unambiguous fraud signature."
            )
        elif level == "WARN":
            conjunction_explanation = (
                f"Elevated Coercion Pretext Detected [{demographic}]: "
                f"Monitoring for Stage 3 Isolation or Stage 4 Siphon."
            )
        else:
            conjunction_explanation = "Low Risk / Routine Dialogue. No active intent conjunction."

        result = {
            "session_id": self.session_id,
            "risk_score": round(self.accumulated_risk, 2),
            "level": level,
            "threat_type": threat_type,
            "demographic_target": demographic,
            "kill_chain_stage": kill_chain_stage,
            "kill_chain_stages_triggered": active_stages,
            "conjunction_explanation": conjunction_explanation,
            "flags": sorted(list(self.seen_flags)),
            "trigger_flags": matched_flags,
            "utterance": utterance
        }
        self.log.append(result)
        return result
