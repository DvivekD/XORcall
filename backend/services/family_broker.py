"""
XORcall Family Broker
Manages Out-of-Band Family Safe-Pings, passive proof-of-life heartbeats,
and multi-hop family escalation trees.
"""
import time
from typing import Dict, List, Optional, Any

class FamilyBroker:
    def __init__(self):
        # In-memory storage for active safe-ping alerts
        self.active_pings: Dict[str, Dict[str, Any]] = {}
        # Registered device registry: user_id -> profile
        self.registered_profiles: Dict[str, Dict[str, Any]] = {
            "son_rahul": {
                "name": "Rahul (Son)",
                "phone": "+91-98765-43210",
                "registered_location": "Bengaluru, Karnataka",
                "last_active_time": time.time() - 120, # 2 mins ago
                "current_wifi_ssid": "RVCE_Campus_5G",
                "screen_unlocked": True,
                "step_count_last_10m": 84,
                "peak_g_force": 1.02, # Normal resting gravity
                "escalation_contact": "sister_priya"
            },
            "sister_priya": {
                "name": "Priya (Daughter)",
                "phone": "+91-98765-88888",
                "registered_location": "Pune, Maharashtra",
                "last_active_time": time.time() - 60,
                "current_wifi_ssid": "Home_Fiber",
                "screen_unlocked": False
            }
        }

    def dispatch_safe_ping(self, session_id: str, caller_claim: str, target_family_id: str = "son_rahul") -> Dict[str, Any]:
        """Sends immediate high-priority out-of-band check to child's phone."""
        profile = self.registered_profiles.get(target_family_id, {})
        
        ping_record = {
            "session_id": session_id,
            "target_id": target_family_id,
            "target_name": profile.get("name", "Family Member"),
            "timestamp": time.time(),
            "status": "PENDING_CONFIRMATION", # PENDING -> CONFIRMED_SAFE -> TIMED_OUT
            "caller_claim": caller_claim,
            # Passive Telemetry Proof-of-Life Snapshot
            "passive_telemetry": {
                "screen_active": profile.get("screen_unlocked", True),
                "last_active_minutes_ago": round((time.time() - profile.get("last_active_time", time.time())) / 60, 1),
                "connected_wifi": profile.get("current_wifi_ssid", "Mobile Network"),
                "location_sanity": profile.get("registered_location", "Unknown"),
                "accelerometer_status": "NORMAL_RESTING (1G)" if profile.get("peak_g_force", 1.0) < 5.0 else "IMPACT_DETECTED",
                "pedometer_steps": profile.get("step_count_last_10m", 0)
            }
        }
        self.active_pings[session_id] = ping_record
        return ping_record

    def confirm_safe(self, session_id: str) -> Dict[str, Any]:
        """Called when real child taps 'I AM 100% SAFE' on their phone."""
        if session_id in self.active_pings:
            self.active_pings[session_id]["status"] = "CONFIRMED_SAFE"
            self.active_pings[session_id]["resolved_at"] = time.time()
            return {"success": True, "status": "CONFIRMED_SAFE", "message": "Child confirmed safety."}
        return {"success": False, "message": "No active ping session found."}

    def get_ping_status(self, session_id: str) -> Dict[str, Any]:
        record = self.active_pings.get(session_id)
        if not record:
            return {"status": "NO_ALERT"}
        
        # Check timeout (45s escalation)
        elapsed = time.time() - record["timestamp"]
        if record["status"] == "PENDING_CONFIRMATION" and elapsed > 45.0:
            record["status"] = "ESCALATED_TO_SECONDARY"
            record["escalated_to"] = "sister_priya"

        return record
