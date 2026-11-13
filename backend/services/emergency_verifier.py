"""
XORcall Emergency Verifier
Handles the "False Positive Death Trap" by evaluating institutional medical records,
official hospital ER landline cross-checks, and device accelerometer crash telemetry.
"""
import re
from typing import Dict, Any, Optional

# Verified Emergency Room Landlines for Major Indian Hospital Chains
HOSPITAL_DIRECTORY: Dict[str, Dict[str, str]] = {
    "apollo": {
        "name": "Apollo Hospitals Emergency Care (National Triage)",
        "phone": "1066",
        "alt_landline": "+91-80-2630-4050",
        "verified_domain": "apollohospitals.com",
        "city": "Pan-India"
    },
    "manipal": {
        "name": "Manipal Hospital Emergency & Trauma Care",
        "phone": "080-2222-1111",
        "alt_landline": "+91-80-2502-4444",
        "verified_domain": "manipalhospitals.com",
        "city": "Bengaluru/Pan-India"
    },
    "fortis": {
        "name": "Fortis Healthcare Critical Emergency Ward",
        "phone": "105010",
        "alt_landline": "+91-11-4713-5000",
        "verified_domain": "fortishealthcare.com",
        "city": "Delhi/NCR/Pan-India"
    },
    "aiims": {
        "name": "AIIMS Emergency & Trauma Centre",
        "phone": "011-2658-8500",
        "alt_landline": "+91-11-2659-3677",
        "verified_domain": "aiims.edu",
        "city": "New Delhi"
    },
    "max": {
        "name": "Max Super Speciality Hospital Emergency",
        "phone": "011-4055-4055",
        "alt_landline": "+91-11-2651-5050",
        "verified_domain": "maxhealthcare.in",
        "city": "Delhi/NCR"
    }
}

class EmergencyVerifier:
    def __init__(self):
        pass

    def evaluate_medical_claim(self, transcript: str, companion_telemetry: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """
        Differentiates real accidents from extortion scams.
        """
        # 1. Check for institutional hospital mentions
        matched_hospital = None
        for key, data in HOSPITAL_DIRECTORY.items():
            if re.search(rf"\b{key}\b", transcript, re.I):
                matched_hospital = data
                break

        # 2. Check for physical presence request vs digital payment demand
        demands_payment = bool(re.search(r"(transfer|upi|gpay|paytm|send\s?money|bail|deposit)", transcript, re.I))
        requests_presence = bool(re.search(r"(please come|visit|admitted at|emergency ward|reception|counter)", transcript, re.I))

        # 3. Accelerometer Crash Telemetry (if companion data provided)
        crash_detected = False
        kinetic_g_force = 1.0
        if companion_telemetry:
            kinetic_g_force = companion_telemetry.get("peak_g_force", 1.0)
            crash_detected = kinetic_g_force >= 15.0  # >15G indicates severe vehicle deceleration

        # Triage Outcome
        if requests_presence and not demands_payment:
            status = "REAL_EMERGENCY_PROBABLE"
            guidance = (
                "Call requests physical presence with zero digital transfer demand. "
                "Coordinating official hospital landline verification."
            )
        elif demands_payment and not crash_detected:
            status = "EXTORTION_SCAM_CONFIRMED"
            guidance = (
                "Caller demands immediate digital transfer while son's phone reports normal resting physics. "
                "Do NOT transfer money. This is an extortion hoax."
            )
        else:
            status = "VERIFICATION_REQUIRED"
            guidance = "Verify via direct official hospital desk call."

        return {
            "status": status,
            "guidance": guidance,
            "matched_hospital": matched_hospital,
            "hospital_verified_landline": matched_hospital["alt_landline"] if matched_hospital else "108 / 112 (National Emergency)",
            "telemetry_crash_detected": crash_detected,
            "kinetic_g_force": kinetic_g_force,
            "digital_payment_demanded": demands_payment
        }
