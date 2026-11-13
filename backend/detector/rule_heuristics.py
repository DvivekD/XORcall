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
from typing import Dict, List, Tuple, Set, Optional

# Deterministic threat signals and weights
SIGNALS: Dict[str, Tuple[float, str]] = {
    # 1. Authority Impersonation (Targets: Seniors & General Public)
    "authority_claim": (
        0.25,
        r"\b(cbi|ed|enforcement directorate|narcotics|ncb|narcotics bureau|cyber\s?crime|"
        r"trai|dot|telecom department|rbi|customs|supreme court|high court|magistrate|"
        r"police officer|inspector|crime branch|dcp|acp|sho|delhi police|mumbai police|"
        r"interpol|telecom regulatory|cyber cell|mha|i4c|national crime records)\b"
    ),
    # 2. Fake Legal Threats
    "legal_threat": (
        0.25,
        r"(arrest warrant|you will be arrested|digital arrest|money laundering|"
        r"case (has been|is) (registered|filed)|\bfir\b|non[- ]bailable|giraftaar|giraftari|"
        r"section 144|official secrets act|pmla|crpc|warrant nikla hai|disciplinary arrest|"
        r"bank account freeze|account seizure|blacklisted pan|jail hogi|jail bhejenge|"
        r"pan card blocked|national security violation)"
    ),
    # 3. Contraband / Parcel Pretext
    "parcel_or_sim": (
        0.15,
        r"(parcel|courier|fedex|dhl|blue\s?dart|contraband|drugs|mdma|narcotics parcel|sim card|"
        r"aadhaar (is|was|has been) (misused|linked|compromised)|illegal (activity|transaction)|"
        r"passport seized|taiwan parcel|cambodia courier|consignment seized|customs hold)"
    ),
    # 4. Psychological & Sensory Confinement (The Smoking Gun of Digital Arrest)
    "isolation": (
        0.30,
        r"(stay on (the )?(video|call|line)|do not (disconnect|cut)|don'?t (disconnect|cut)|"
        r"do not tell (anyone|your family|colleagues)|don'?t tell (anyone|your family|colleagues)|"
        r"kisi ko mat bata|kisi se baat mat|confidential investigation|stay in (a )?(room|front of camera)|"
        r"room lock karo|camera on rakho|door lock|strictly confidential|nd clause|"
        r"(call|phone) mat (kaatna|disconnect|rakhna)|camera mat band|skype video|zoom call pe raho)"
    ),
    # 5. Urgent Financial Extortion Demand
    "payment_demand": (
        0.35,
        r"((transfer|deposit|rtgs|neft|upi|send|pay|bhej[ao]?|jama|daal).{0,40}(money|amount|funds|rupees|lakh|crore|rs\.?|₹|\b\d{3,9}\b|upi|qr|gpay|phonepe|paytm)|"
        r"(upi\s?id|upi handle|qr\s?code|google\s?pay|gpay|phonepe|paytm|bhim).{0,30}(send|pay|bhej[ao]?|transfer|deposit|karo)|"
        r"(\b\d{3,9}\b|₹|rs\.?|lakh|crore|rupees).{0,30}(transfer|deposit|bhej[ao]?|pay|send|jama)|"
        r"verification (amount|deposit)|safe (rbi )?account|security deposit|"
        r"paise (bhej[ao]?|transfer|jama)|bail (amount|money)|clinic deposit|task deposit|"
        r"recharge (amount|balance)|wallet recharge|crypto (transfer|deposit)|advance wire|clearance (fee|deposit))"
    ),
    # 6. Pressure / Urgency
    "urgency": (
        0.15,
        r"(immediately|right now|within (one|1|two|2|\d+) (hour|hours|minutes)|"
        r"last warning|abhi|turant|jaldi|immediate arrest|within 15 minutes|before 5 pm|deadline)"
    ),
    # 7. Kinship Emergency Hook (Family Impersonation / Virtual Kidnapping)
    "kinship_emergency": (
        0.35,
        r"(papa|mummy|dad|mom|uncle|grandpa|grandma|beta).{0,35}(accident|police|custody|arrest|"
        r"hospital|jail|bacha lo|help me|mar jayenge|trouble|emergency|phone chori|friend ke phone|"
        r"pakad liya|giraftaar)"
    ),
    # 8. Student & Youth Scam Signals (Telegram Part-Time Job / Rating / Task Fraud)
    "task_job_scam": (
        0.30,
        r"((telegram|whatsapp)\s*(task|job|group)|part[- ]time job|work from home|"
        r"(youtube|instagram|tiktok).{0,20}(like|subscribe|follow)|"
        r"(review|rate).{0,20}(hotel|movie|google maps|listing|place)|"
        r"(daily|monthly)\s*(income|earning|salary)|earn.{0,20}(\d{3,6}|daily|per day)|"
        r"prepaid.{0,15}task|recharge.{0,25}withdraw|crypto task|rating commission|task level \d+|"
        r"merchant task|hotel review task)"
    ),
    # 9. Corporate / Working Professional Extortion (CEO / Boss Impersonation & Whaling)
    "corporate_wire_scam": (
        0.30,
        r"(i am in a\s*(board|client|investor)?\s*meeting|in a (conference|meeting|call)|"
        r"do not call (my\s*)?(number|mobile|phone)|urgent\s*(wire|neft|rtgs|vendor|bank).{0,25}(transfer|payment|account|funds)|"
        r"(ceo|cfo|coo|director|managing director|vp)\s*(directive|instruction|speaking|here)|"
        r"confidential\s*(acquisition|settlement|project|transaction)|"
        r"(purchase|buy)\s*(apple|google\s*play|amazon)\s*gift\s*cards|"
        r"advance\s*wire\s*transfer|company\s*account\s*wire)"
    ),
    # 10. College / University Phishing (e.g., BITS Pilani ₹7.67 Cr Syndicate Pretext)
    "college_fee_phishing": (
        0.30,
        r"(college\s*fee|hostel\s*fee|tuition\s*fee|semester\s*fee|admission\s*fee|exam\s*fee|"
        r"bits\s*pilani|seat\s*(cancel|cancelled|cancellation)|hall\s*ticket\s*hold|"
        r"admission\s*clearance|registrar\s*(office|upi)|campus\s*syndicate|student\s*portal\s*due)"
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

    def feed(self, utterance: Optional[str]) -> dict:
        utterance = str(utterance or "").strip()
        matched_flags = []
        sentence_score = 0.0

        if not utterance:
            return {
                "session_id": self.session_id,
                "risk_score": round(self.accumulated_risk, 2),
                "level": "SAFE" if self.accumulated_risk < 0.3 else ("WATCH" if self.accumulated_risk < 0.55 else ("WARN" if self.accumulated_risk < 0.8 else "CRITICAL")),
                "threat_type": "STANDBY",
                "demographic_target": "GENERAL",
                "kill_chain_stage": 1,
                "kill_chain_stages_triggered": [],
                "conjunction_explanation": "Standing by for speech input.",
                "flags": sorted(list(self.seen_flags)),
                "trigger_flags": [],
                "utterance": ""
            }

        for flag, (weight, rx) in COMPILED_SIGNALS.items():
            if rx.search(utterance):
                matched_flags.append(flag)
                sentence_score += weight

        self.seen_flags.update(matched_flags)

        # Non-linear risk accumulation model
        u = max(0.0, min(1.0, sentence_score))
        self.accumulated_risk = 1.0 - (1.0 - self.accumulated_risk) * (1.0 - 0.65 * u)

        # KILL-CHAIN RULE 1A: Authority Coercion with Isolation & Payment (Digital Arrest)
        if {"authority_claim", "isolation", "payment_demand"} <= self.seen_flags:
            self.accumulated_risk = max(self.accumulated_risk, 0.96)

        # KILL-CHAIN RULE 1B: Authority / Legal Threat + Direct Payment (Law enforcement never asks for UPI/wires)
        if ({"authority_claim", "payment_demand"} <= self.seen_flags or 
            {"legal_threat", "payment_demand"} <= self.seen_flags or
            {"parcel_or_sim", "payment_demand"} <= self.seen_flags):
            self.accumulated_risk = max(self.accumulated_risk, 0.91)

        # KILL-CHAIN RULE 2: Kinship Emergency Extortion (Family Deepfake / Parents)
        if {"kinship_emergency", "payment_demand"} <= self.seen_flags:
            self.accumulated_risk = max(self.accumulated_risk, 0.93)

        # KILL-CHAIN RULE 3: Student Part-Time Job / Task Scam
        if {"task_job_scam", "payment_demand"} <= self.seen_flags:
            self.accumulated_risk = max(self.accumulated_risk, 0.92)

        # KILL-CHAIN RULE 4: Corporate / CEO Impersonation (Working Professionals)
        if {"corporate_wire_scam", "payment_demand"} <= self.seen_flags:
            self.accumulated_risk = max(self.accumulated_risk, 0.92)

        # KILL-CHAIN RULE 5: College Fee Phishing / Youth Extortion
        if {"college_fee_phishing", "payment_demand"} <= self.seen_flags:
            self.accumulated_risk = max(self.accumulated_risk, 0.93)

        # BENIGN MITIGATION 1: Genuine Hospital Visit (Zero payment demanded)
        if "physical_hospital_presence" in self.seen_flags and "payment_demand" not in self.seen_flags:
            self.accumulated_risk = min(self.accumulated_risk, 0.20)

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

        # Demographic Profile Target Inference
        if any(f in self.seen_flags for f in ["task_job_scam", "college_fee_phishing"]):
            demographic = "STUDENT_YOUTH"
        elif "corporate_wire_scam" in self.seen_flags:
            demographic = "WORKING_PROFESSIONAL"
        elif "kinship_emergency" in self.seen_flags:
            demographic = "PARENT"
        elif "authority_claim" in self.seen_flags or "parcel_or_sim" in self.seen_flags:
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
        elif ("authority_claim" in self.seen_flags or "parcel_or_sim" in self.seen_flags) and self.accumulated_risk >= 0.55:
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

        stage_hierarchy = {
            "STAGE_1_PRETEXT_HOOK": 1,
            "STAGE_2_CRISIS_MANUFACTURE": 2,
            "STAGE_3_ISOLATION_CONFINEMENT": 3,
            "STAGE_4_PAYMENT_EXTORTION": 4,
            "STAGE_5_TERMINAL_COERCION": 5,
        }
        kill_chain_stage = max([stage_hierarchy[s] for s in active_stages], default=1)

        # Synthesize mathematical conjunction explanation
        if level == "CRITICAL":
            conjunction_explanation = (
                f"Multi-Stage Intent Kill-Chain Conjunction Triggered [{demographic}]: "
                f"{' + '.join(active_stages)}. Unambiguous fraud signature."
            )
        elif level == "WARN":
            if "payment_demand" in self.seen_flags:
                conjunction_explanation = (
                    f"Elevated Extortion Risk Detected [{demographic}]: "
                    f"Direct unverified payment demanded. High probability cyber trap."
                )
            elif "isolation" in self.seen_flags:
                conjunction_explanation = (
                    f"Sensory Confinement Detected [{demographic}]: "
                    f"Webcam / isolation gag order active. Monitoring for payment siphon."
                )
            else:
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
