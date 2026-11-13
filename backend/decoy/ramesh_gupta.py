"""
XORcall Decoy Agent: "Ramesh Gupta"
Autonomous reverse-scam honeypot designed to stall scammers,
waste their call-center capital, and extract mule financial handles (UPI/Bank/IFSC).
"""
import re
import time
from typing import Dict, List, Any

RAMESH_PROMPT = """
You are Ramesh Gupta, a 68-year-old retired Indian Railways senior clerk living in Pune.
You are currently on a call with a high-pressure scammer claiming to be from the CBI, Police, or an emergency hospital.
Your goal is to completely waste their time while acting entirely innocent, slightly hard of hearing, polite, and technically inept.

Key Behaviors:
- Speak in authentic Indian English with gentle Hindi interjections ("Arre beta", "Accha accha", "One second ji").
- Pretend to be searching for your reading spectacles, your SBI passbook, or your hearing aid.
- When they demand a UPI or bank transfer, act like you're trying to do it, but struggle with the app:
  "Beta, my screen is showing 'Google Pay update'. Where is the RTGS button? Can you read the account number again slowly?"
- Ask them to repeat their full official name, badge number, and their exact branch address because "My nephew Inspector Deepak is also posted in Mumbai Police".
- Keep every reply strictly under 3 sentences so the scammer stays engaged.
- NEVER break character.
"""

class RameshGuptaDecoy:
    def __init__(self, session_id: str):
        self.session_id = session_id
        self.start_time = time.time()
        self.turns: List[Dict[str, str]] = []
        self.harvested_iocs: Dict[str, List[str]] = {
            "upi_ids": [],
            "bank_accounts": [],
            "ifsc_codes": [],
            "phone_numbers": []
        }

    def generate_reply(self, scammer_utterance: str) -> Dict[str, Any]:
        """Processes scammer speech, harvests IoCs, and returns Ramesh's in-character response."""
        self.turns.append({"speaker": "scammer", "text": scammer_utterance})
        self._harvest_iocs(scammer_utterance)

        elapsed_seconds = int(time.time() - self.start_time)
        turn_count = len(self.turns)

        # Dynamic heuristic responses if LLM is offline or fast turnaround is needed
        if turn_count <= 2:
            reply = (
                "Arre beta, one minute please... my daughter keeps my reading glasses on the fridge. "
                "Can you speak a little louder? Which police station did you say you are calling from?"
            )
        elif "upi" in scammer_utterance.lower() or "transfer" in scammer_utterance.lower() or "send" in scammer_utterance.lower():
            reply = (
                "Yes yes, I opened my phone, but it is asking for a 6-digit MPIN. "
                "Beta, please give me the exact spelling of the account name. Is it State Bank or Reserve Bank? My spectacles are dirty."
            )
        elif "video" in scammer_utterance.lower() or "camera" in scammer_utterance.lower():
            reply = (
                "Camera is on, but there is no light in my veranda. "
                "Officer sahab, are you sitting with Commissioner Vivek? My son says you never arrest people on WhatsApp call."
            )
        else:
            reply = (
                "Haan haan, I am writing it down on my diary with a ball pen. "
                "Can you repeat the account number once more from the beginning? My hearing aid battery is giving a beeping sound."
            )

        self.turns.append({"speaker": "ramesh", "text": reply})

        return {
            "reply": reply,
            "session_id": self.session_id,
            "seconds_wasted": elapsed_seconds,
            "harvested_iocs": self.harvested_iocs,
            "turn_count": turn_count
        }

    def _harvest_iocs(self, text: str):
        """Scans utterance for mule financial identifiers."""
        # 1. UPI ID Regex
        upi_matches = re.findall(r"[\w\.\-_]+@(?:okhdfcbank|okaxis|oksbi|paytm|ybl|apl|ibl|sbi|axis|icici)", text, re.I)
        for u in upi_matches:
            if u not in self.harvested_iocs["upi_ids"]:
                self.harvested_iocs["upi_ids"].append(u)

        # 2. Bank Account Numbers (9 to 18 contiguous digits)
        acct_matches = re.findall(r"\b\d{9,18}\b", text)
        for a in acct_matches:
            if a not in self.harvested_iocs["bank_accounts"]:
                self.harvested_iocs["bank_accounts"].append(a)

        # 3. IFSC Code Regex
        ifsc_matches = re.findall(r"\b[A-Z]{4}0[A-Z0-9]{6}\b", text, re.I)
        for i in ifsc_matches:
            if i.upper() not in self.harvested_iocs["ifsc_codes"]:
                self.harvested_iocs["ifsc_codes"].append(i.upper())

        # 4. Indian Phone Numbers
        phone_matches = re.findall(r"(?:\+91|91|0)?[6-9]\d{9}", text)
        for p in phone_matches:
            if p not in self.harvested_iocs["phone_numbers"]:
                self.harvested_iocs["phone_numbers"].append(p)
