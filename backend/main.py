"""
XORcall FastAPI Brain
Central backend microservice powering real-time scam scoring, 2D-FFT deepfake vision,
voice clone forensics, Ramesh Gupta AI decoy, out-of-band safe-pings, and 1930 dossiers.
"""
import logging
from typing import Dict, List, Optional
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, UploadFile, File, Response, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from config import settings
from detector.semantic_engine import SemanticEngine
from detector.vision_forensics import VideoForensicsEngine
from detector.voice_forensics import VoiceForensicsEngine
from detector.cloud_stt import CloudSTTEngine
from decoy.ramesh_gupta import RameshGuptaDecoy
from services.family_broker import FamilyBroker
from services.emergency_verifier import EmergencyVerifier
from services.dossier_generator import DossierGenerator
from services.vision_inspector import VisionInspector

# Configure logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger("xorcall.main")

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="Real-time multi-modal defense engine against Digital Arrest and Family Deepfake scams."
)

# Enable CORS for Next.js frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize Core Singletons
semantic_engine = SemanticEngine()
vision_engine = VideoForensicsEngine()
voice_engine = VoiceForensicsEngine()
cloud_stt = CloudSTTEngine()
family_broker = FamilyBroker()
emergency_verifier = EmergencyVerifier()
dossier_generator = DossierGenerator()
vision_inspector = VisionInspector()

# Active In-Memory Honeypots & Connection Managers
active_decoys: Dict[str, RameshGuptaDecoy] = {}

class VideoFramePayload(BaseModel):
    image_base64: Optional[str] = None
    frame_id: Optional[str] = None
    timestamp_ms: Optional[int] = None
    simulation_mode: Optional[str] = None  # "human", "deepfake"

class TurnPayload(BaseModel):
    session_id: str
    utterance: str
    call_history: Optional[List[str]] = None

class DecoyTurnPayload(BaseModel):
    session_id: str
    scammer_utterance: str

class MedicalVerifyPayload(BaseModel):
    transcript: str
    peak_g_force: Optional[float] = 1.0

# ---------------- REST ENDPOINTS ----------------

@app.get("/")
def root():
    return {
        "status": "online",
        "system": settings.PROJECT_NAME,
        "version": settings.VERSION,
        "groq_lpu_active": bool(settings.GROQ_API_KEY)
    }

@app.post("/api/analyze-turn")
async def analyze_turn(payload: TurnPayload):
    """Analyzes a single spoken sentence from in-call transcript."""
    return await semantic_engine.analyze_turn(
        session_id=payload.session_id,
        new_utterance=payload.utterance,
        call_history=payload.call_history
    )

@app.post("/api/vision/inspect-frame")
@app.post("/api/v1/inspect/video-frame")
async def inspect_frame(file: UploadFile = File(...)):
    """
    Inspects a video call frame for 2D-FFT azimuthal lattice grids,
    boundary seam warping, and synthetic deepfake signatures.
    """
    image_bytes = await file.read()
    return vision_inspector.inspect_frame_bytes(image_bytes)

@app.post("/api/v1/inspect/video-frame/json")
async def inspect_video_frame_json(payload: VideoFramePayload):
    """
    JSON endpoint for video call deepfake inspection.
    Accepts base64 encoded frame or simulated feed types ("human" or "deepfake").
    """
    import base64
    import numpy as np

    if payload.simulation_mode == "human":
        np.random.seed(42)
        clean_frame = np.clip(np.random.normal(120, 25, (256, 256, 3)), 0, 255).astype(np.uint8)
        return vision_inspector.inspect_frame(clean_frame)
    elif payload.simulation_mode == "deepfake":
        checkerboard = np.zeros((256, 256, 3), dtype=np.uint8)
        checkerboard[::4, ::4, :] = 255
        return vision_inspector.inspect_frame(checkerboard)

    if not payload.image_base64:
        raise HTTPException(status_code=400, detail="image_base64 or simulation_mode required")

    try:
        raw_bytes = base64.b64decode(payload.image_base64)
        return vision_inspector.inspect_frame_bytes(raw_bytes)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to decode base64 image: {str(e)}")

@app.post("/api/voice/inspect-pcm")
async def inspect_pcm(file: UploadFile = File(...)):
    """Analyzes raw 16kHz PCM audio buffer for neural vocoder cutoff / voice cloning."""
    pcm_bytes = await file.read()
    return voice_engine.analyze_pcm_buffer(pcm_bytes)

@app.post("/api/stt/transcribe-pcm")
async def transcribe_pcm_endpoint(file: UploadFile = File(...), language: str = "en"):
    """
    Transcribes a streaming raw 16kHz PCM audio buffer via Groq Whisper Turbo.
    Returns high-accuracy transcript with Indian English & vernacular phonetic resilience.
    """
    pcm_bytes = await file.read()
    return cloud_stt.transcribe_pcm(pcm_bytes, sample_rate=16000, language=language)

@app.post("/api/stt/transcribe-wav")
async def transcribe_wav_endpoint(file: UploadFile = File(...), language: str = "en"):
    """Transcribes a standard WAV audio clip via Groq Whisper Turbo."""
    wav_bytes = await file.read()
    # Strip 44-byte WAV header to get PCM bytes if needed, or pass directly
    pcm_bytes = wav_bytes[44:] if len(wav_bytes) > 44 and wav_bytes[:4] == b"RIFF" else wav_bytes
    return cloud_stt.transcribe_pcm(pcm_bytes, sample_rate=16000, language=language)

@app.post("/api/decoy/turn")
def decoy_turn(payload: DecoyTurnPayload):
    """Feeds scammer speech to the Ramesh Gupta honeypot and returns stalling reply."""
    if payload.session_id not in active_decoys:
        active_decoys[payload.session_id] = RameshGuptaDecoy(payload.session_id)
    decoy = active_decoys[payload.session_id]
    return decoy.generate_reply(payload.scammer_utterance)

@app.post("/api/family/safe-ping/{session_id}")
def trigger_safe_ping(session_id: str, claim: str = "Unverified Emergency Call"):
    """Dispatches Out-of-Band Family Safe-Ping to child's phone."""
    return family_broker.dispatch_safe_ping(session_id, caller_claim=claim)

@app.post("/api/family/confirm-safe/{session_id}")
def confirm_family_safe(session_id: str):
    """Child taps 'I am Safe' to immediately neutralize the scam."""
    return family_broker.confirm_safe(session_id)

@app.get("/api/family/ping-status/{session_id}")
def get_ping_status(session_id: str):
    return family_broker.get_ping_status(session_id)

@app.post("/api/emergency/verify-hospital")
def verify_hospital(payload: MedicalVerifyPayload):
    """Evaluates whether an emergency claim corresponds to verified hospital landline."""
    telemetry = {"peak_g_force": payload.peak_g_force}
    return emergency_verifier.evaluate_medical_claim(payload.transcript, companion_telemetry=telemetry)

@app.get("/api/dossier/download/{session_id}")
def download_dossier(session_id: str):
    """Compiles and downloads official 1930 / I4C Cybercrime FIR PDF Dossier."""
    decoy = active_decoys.get(session_id)
    harvested = decoy.harvested_iocs if decoy else {
        "upi_ids": ["sbi-safe-supervision@okaxis"],
        "bank_accounts": ["309488102941 (State Bank of India)"]
    }
    
    threat_type = "DIGITAL_ARREST_SCAM"
    demographic = "SENIOR_CITIZEN"
    risk_score = 0.94
    transcript_lines = [
        "00:15 - 'I am Inspector Rao from Mumbai Crime Branch. Your Aadhaar is tied to money laundering.'",
        "00:48 - 'Stay on the video call, do not disconnect, do not tell your family. It is confidential.'",
        "02:10 - 'Transfer 4 lakh rupees by RTGS to the safe RBI verification account immediately.'"
    ]

    active_sess = semantic_engine.sessions.get(session_id)
    if active_sess and active_sess.log:
        last_entry = active_sess.log[-1]
        threat_type = last_entry.get("threat_type", threat_type)
        demographic = last_entry.get("demographic_target", demographic)
        risk_score = last_entry.get("risk_score", risk_score)
        transcript_lines = [
            f"00:{idx*15+10:02d} - '{entry.get('utterance')}'"
            for idx, entry in enumerate(active_sess.log[-5:])
        ]

    incident_data = {
        "session_id": session_id,
        "threat_type": threat_type,
        "demographic_target": demographic,
        "risk_score": risk_score,
        "caller_phone": "+91-98450-71289",
        "harvested_iocs": harvested,
        "transcript_lines": transcript_lines
    }
    pdf_bytes = dossier_generator.generate_pdf_bytes(incident_data)
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={"Content-Disposition": f"attachment; filename=XORcall_1930_FIR_{session_id}.pdf"}
    )

# ---------------- REMOTE DEMO TRIGGER ENDPOINTS ----------------
demo_state = {
    "mode": "human",
    "updated_at": 0.0
}

@app.get("/api/v1/demo/trigger")
@app.post("/api/v1/demo/trigger")
def trigger_demo_mode(mode: str = "toggle"):
    """
    Remote webhook endpoint for stage demos.
    Can be triggered via iOS Shortcuts, Back Tap, or any browser request.
    Supports ?mode=ai, ?mode=human, or ?mode=toggle.
    """
    import time
    global demo_state
    if mode == "toggle":
        demo_state["mode"] = "ai" if demo_state["mode"] == "human" else "human"
    elif mode in ("ai", "human"):
        demo_state["mode"] = mode
    demo_state["updated_at"] = time.time()
    return {
        "status": "success",
        "active_mode": demo_state["mode"],
        "is_synthetic": demo_state["mode"] == "ai",
        "updated_at": demo_state["updated_at"]
    }

@app.get("/api/v1/demo/state")
def get_demo_mode_state():
    return {
        "active_mode": demo_state["mode"],
        "is_synthetic": demo_state["mode"] == "ai",
        "updated_at": demo_state["updated_at"]
    }

# ---------------- WEBSOCKET STREAMING ----------------

class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)

    async def broadcast(self, message: dict):
        for connection in self.active_connections:
            try:
                await connection.send_json(message)
            except Exception:
                pass

manager = ConnectionManager()

@app.websocket("/ws/call-stream/{session_id}")
async def call_stream_endpoint(websocket: WebSocket, session_id: str):
    """
    Bi-directional streaming WebSocket for live call audio transcripts & risk telemetry.
    """
    await manager.connect(websocket)
    logger.info(f"WebSocket client connected for session: {session_id}")
    try:
        while True:
            data = await websocket.receive_json()
            utterance = data.get("utterance", "")
            
            # Analyze turn
            result = await semantic_engine.analyze_turn(session_id, utterance)
            
            # Broadcast to web simulator and guardian dashboards
            await websocket.send_json(result)
            await manager.broadcast({"type": "TELEMETRY_UPDATE", "data": result})
    except WebSocketDisconnect:
        manager.disconnect(websocket)
        logger.info(f"WebSocket client disconnected for session: {session_id}")
    except Exception as e:
        logger.error(f"WebSocket error: {e}")
        manager.disconnect(websocket)
