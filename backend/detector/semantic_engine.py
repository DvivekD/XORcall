"""
XORcall Semantic Engine
Integrates Groq LPU (Llama 3.3 70B Versatile) for sub-200ms reasoning over code-mixed Indian transcripts.
Gracefully falls back to CallHeuristicSession if offline or if GROQ_API_KEY is not set.
"""
import json
import logging
from typing import Dict, List, Optional
from groq import Groq
from config import settings
from detector.rule_heuristics import CallHeuristicSession

logger = logging.getLogger("xorcall.semantic")

GROQ_SYSTEM_PROMPT = """
You are XORcall's Real-Time Cyber Extortion & Social Engineering Intelligence Engine.
You receive rolling live call transcripts in English, Hindi, Kannada, Tamil, Telugu, or code-mixed Hinglish/Kanglish.

Protect victims across 4 primary demographic profiles:
1. "SENIOR_CITIZEN": Digital Arrest (impersonating CBI/Police/Customs/ED, fake MDMA parcel, webcam sensory confinement, safe RBI account RTGS transfer).
2. "STUDENT_YOUTH": Telegram task/part-time job fraud (YouTube likes, rating reviews, prepay recharge to withdraw balance), and College Fee Phishing (fake registrar UPI, BITS Pilani ₹7.67 Cr syndicate pretexts).
3. "WORKING_PROFESSIONAL": Corporate Executive Whaling (urgent CEO wire transfer during board meetings, confidential client settlement, gift cards).
4. "PARENT": Kinship AI voice/video deepfake (child arrested in car accident, narcotics charge, immediate unverified bail UPI).
5. "GENUINE_EMERGENCY" / "BENIGN": Genuine hospital triage (visit ER in person, zero digital transfer), official university ERP, or routine workplace Jira/Slack discussion.

You MUST respond strictly with a valid JSON object with these keys:
{
  "threat_type": "DIGITAL_ARREST_SCAM" | "KINSHIP_DEEPFAKE_EXTORTION" | "STUDENT_TASK_SCAM" | "CEO_IMPERSONATION_FRAUD" | "COLLEGE_FEE_PHISHING" | "GENUINE_MEDICAL_TRIAGE" | "BENIGN",
  "demographic_target": "SENIOR_CITIZEN" | "STUDENT_YOUTH" | "WORKING_PROFESSIONAL" | "PARENT" | "GENERAL",
  "risk_score": float (0.0 to 1.0),
  "stage": int (1 to 5: 1=Pretext Hook, 2=Crisis Injection, 3=Isolation/Confinement, 4=Financial Extortion, 5=Terminal Coercion),
  "primary_coercion_vectors": list of strings (e.g. ["authority_impersonation", "sensory_isolation", "urgent_payment", "gag_order"]),
  "recommended_action": "SAFE" | "WATCH" | "WARN" | "CRITICAL_INTERVENE" | "COORDINATE_HOSPITAL",
  "vernacular_summary": "Brief 1-sentence explanation of what is happening in English/Hinglish",
  "conjunction_explanation": "Explanation of multi-modal intent kill-chain conjunction"
}
"""

class SemanticEngine:
    def __init__(self):
        self.groq_client: Optional[Groq] = None
        if settings.GROQ_API_KEY:
            try:
                self.groq_client = Groq(api_key=settings.GROQ_API_KEY)
                logger.info("Groq LPU Client initialized successfully.")
            except Exception as e:
                logger.warning(f"Failed to initialize Groq client: {e}. Defaulting to rule heuristics.")
        else:
            logger.info("No GROQ_API_KEY provided. Operating in deterministic offline heuristic mode.")

        self.sessions: Dict[str, CallHeuristicSession] = {}

    def get_or_create_session(self, session_id: str) -> CallHeuristicSession:
        if session_id not in self.sessions:
            self.sessions[session_id] = CallHeuristicSession(session_id)
        return self.sessions[session_id]

    async def analyze_turn(self, session_id: str, new_utterance: str, call_history: Optional[List[str]] = None) -> dict:
        heuristic_session = self.get_or_create_session(session_id)
        local_result = heuristic_session.feed(new_utterance)

        # If Groq is not available, return the hardened local heuristic response directly
        if not self.groq_client:
            return {
                **local_result,
                "engine": "offline_heuristic",
                "stage": local_result.get("kill_chain_stage", 4 if local_result["level"] == "CRITICAL" else (3 if local_result["level"] == "WARN" else 1)),
                "recommended_action": "CRITICAL_INTERVENE" if local_result["level"] == "CRITICAL" else local_result["level"]
            }

        # Format multi-turn transcript for Groq LPU
        history = call_history or [entry["utterance"] for entry in heuristic_session.log]
        conversation_context = "\n".join([f"Speaker: {line}" for line in history[-6:]])

        try:
            chat_completion = self.groq_client.chat.completions.create(
                messages=[
                    {"role": "system", "content": GROQ_SYSTEM_PROMPT},
                    {
                        "role": "user",
                        "content": f"Full Conversation So Far:\n{conversation_context}\n\nLatest Utterance: {new_utterance}\n\nAnalyze threat:"
                    }
                ],
                model=settings.GROQ_MODEL,
                temperature=0.1,
                response_format={"type": "json_object"}
            )
            response_json = json.loads(chat_completion.choices[0].message.content)
            
            # Combine cloud reasoning with local deterministic hard-bounds
            blended_risk = max(local_result["risk_score"], float(response_json.get("risk_score", 0.0)))
            level = "CRITICAL" if blended_risk >= 0.80 else ("WARN" if blended_risk >= 0.55 else ("WATCH" if blended_risk >= 0.30 else "SAFE"))
            
            return {
                "session_id": session_id,
                "risk_score": round(blended_risk, 2),
                "level": level,
                "threat_type": response_json.get("threat_type", local_result["threat_type"]),
                "demographic_target": response_json.get("demographic_target", local_result["demographic_target"]),
                "stage": response_json.get("stage", local_result.get("kill_chain_stage", 1)),
                "primary_coercion_vectors": response_json.get("primary_coercion_vectors", local_result["flags"]),
                "recommended_action": response_json.get("recommended_action", "CRITICAL_INTERVENE" if level == "CRITICAL" else level),
                "vernacular_summary": response_json.get("vernacular_summary", local_result.get("conjunction_explanation", "Monitoring ongoing conversation.")),
                "conjunction_explanation": response_json.get("conjunction_explanation", local_result.get("conjunction_explanation")),
                "flags": local_result["flags"],
                "trigger_flags": local_result["trigger_flags"],
                "engine": "groq_hybrid",
                "utterance": new_utterance
            }
        except Exception as e:
            logger.error(f"Groq API call error: {e}. Falling back to offline heuristic.")
            return {
                **local_result,
                "engine": "offline_fallback",
                "stage": local_result.get("kill_chain_stage", 4 if local_result["level"] == "CRITICAL" else 2),
                "recommended_action": "CRITICAL_INTERVENE" if local_result["level"] == "CRITICAL" else local_result["level"]
            }
