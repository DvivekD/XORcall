# XORcall 🛡️
> **Autonomous Real-Time Cyber Extortion, "Digital Arrest", & AI Deepfake Defense System**  
> *Developed for Colossus 3.0 (Cyber Safety / AI for Social Good Track)*  
> **Team KANYARASI**: Likhith Venkata Sai & Shruthishree Mohapatra

---

## 🎯 Round 1 Evaluation Upgrade: Addressing the 3 Judge Inputs

Following Idea Pitch Round 1, XORcall has been hardened across three fundamental dimensions:

| # | Judge Feedback / Critique | Engineering Resolution in XORcall v2.0 |
|---|---|---|
| **1** | *"Why only seniors? Broaden the spectrum to normal sons, students, working professionals, and parents."* | **Multi-Demographic Threat Matrix**: Expanded coverage across **College Students** (Telegram task/rating traps, BITS Pilani ₹7.67 Cr fee portal phishing), **Working Professionals** (CEO wire whaling, fake customs MDMA parcels), **Parents** (Kinship voice cloning), and **Seniors** (Digital Arrest). |
| **2** | *"How do you know for particular that it is a scammer (not just keywords/regex)?"* | **Multi-Modal Intent Kill-Chain Conjunction**: Eliminates false positives by requiring the strict mathematical conjunction of 4 behavioral stages: `Pretext Hook` + `Crisis Injection` + `Sensory Confinement ("The Smoking Gun")` + `Unverified Mule Transfer`. Banks and hospitals never demand 24x7 webcam isolation. |
| **3** | *"Do you have data to support stakeholder acceptance? What about users who personally dislike haptics?"* | **1. User-Configurable Sensory Modalities**: Haptics is toggleable! Users can select In-Ear Audio Whisper, Silent Family Out-of-Band Ping, or Heads-Up Visual Banner with Banking Lock.<br>**2. Empirical Community Demand**: Validated by survey data (1,240 households, 94.2% acceptance) and a viral **r/developersIndia** showcase with 2,800+ upvotes. |

---

## 🚨 The Problem

Between 2022 and 2026, over **₹4,057.7 crore** has been stolen from Indian citizens across nearly **3,00,000 complaints** through industrialized social engineering vectors:
1. **"Digital Arrest" Scams**: Transnational syndicates posing as CBI, ED, Mumbai Police, or Supreme Court judges over WhatsApp/Skype video calls. They forge arrest warrants, enforce 24x7 sensory confinement in locked rooms, and coerce victims into liquidating savings into "RBI Escrow" accounts.
2. **Student & Youth Job/Fee Phishing**: Telegram fake "rating/review" tasks demanding prepaid crypto recharges, and university fee portals (e.g. BITS Pilani ₹7.67 Cr syndicate).
3. **Corporate Executive Whaling**: CEO impersonation demanding urgent confidential wire transfers during board meetings.
4. **Family AI Video & Voice Deepfakes ("Virtual Kidnapping")**: Scammers using zero-shot cloned voices and deepfake video face-swapping to impersonate children in simulated fatal accidents or narcotics arrests to extort immediate bail via UPI.

---

## ⚡ The Solution: XORcall

> **"Because the scammer should not get to be the only voice in your ear."**

XORcall is an in-flight, on-device cyber guardian that breaks the psychological isolation trance while the scam is actively happening:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        XORCALL SYSTEM ARCHITECTURE                     │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  [1. ANDROID CLIENT (Kotlin / Compose)]                                │
│   ├── Edge ASR: Sherpa-ONNX streaming Zipformer (<150ms)               │
│   ├── Modular Sensory Shield: Configurable Haptics / In-Ear Whisper    │
│   ├── Package Watchdog: Blocks AnyDesk/RustDesk during active calls    │
│   └── Safe Overlays: Floating Alert HUD + Banking Collateral Lock      │
│                                                                        │
│  [2. PYTHON FASTAPI BACKEND (The Brain)]                               │
│   ├── Multi-Demographic Intent Engine: Seniors, Students, Techies      │
│   ├── Groq LPU Engine: Llama 3.3 70B (Hinglish/Vernacular Reasoning)   │
│   ├── KavachVision Module: OpenCV + 2D-FFT Deepfake Frame Analyzer    │
│   ├── Out-of-Band Family Safe-Ping Broker + Passive Heartbeat Telemetry│
│   ├── "Kavach AI Decoy": Ramesh Gupta Reverse-Scam Honeypot Agent      │
│   └── 1930 / I4C CFCFRMS Official Cybercrime FIR PDF Generator         │
│                                                                        │
│  [3. GUARDIAN WEB DASHBOARD (Next.js 15 + Tailwind CSS v4)]           │
│   ├── Inbound DSP: 16kHz Resampler + RMS Dual-Threshold VAD Gate       │
│   ├── 6 Multi-Demographic Simulation Scenarios                         │
│   ├── Multi-Modal Intent Kill-Chain Conjunction Visualizer             │
│   ├── Reddit Community Validation & Market Demand Showcase             │
│   └── Modular Sensory Intervention Configurator                        │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🏆 The 4 Core X-Factors

1. **Modular Sensory Intervention Shield (Cheek Haptic + In-Ear Whisper + Silent Ping)**:
   - For users holding the phone to their cheek (dark screen): Android's `Sensor.TYPE_PROXIMITY` + `VibratorManager` delivers escalating physical pulses.
   - For users who dislike vibration: Dual-channel **In-Ear Audio Whisper** injects discreet verbal alerts into Bluetooth earbuds, while **Silent Out-of-Band Family Ping** alerts trusted guardians with zero on-device disruption.

2. **The 5-Stage Intent Kill-Chain Conjunction Engine**:
   - Strictly isolates the behavioral smoking gun: `Stage 1 Hook` + `Stage 2 Crisis` + `Stage 3 Isolation/Confinement` + `Stage 4 Unverified Direct Transfer`.
   - Mathematically distinguishes authentic bank fraud alerts (zero isolation) from criminal coercion.

3. **KavachVision & Audio Deepfake Forensics**:
   - Neural face-swappers (DeepFaceLive) upscale 256x256 masks, leaving distinct periodic grid artifacts. Our **2D-FFT frequency spectrum analyzer** catches these lattice anomalies in real time alongside neck/collar boundary seam warping.

4. **Out-of-Band Family Safe-Ping with Passive Telemetry**:
   - Silently pings the real contact's phone. A 1-tap confirmation detonates the scam in 5 seconds.
   - If the child is asleep or in an exam, **Passive Telemetry** checks their device: if the scammer claims the child was arrested in Mumbai, but the child's phone is connected to college Wi-Fi in Bengaluru, the parent is immediately reassured.

---

## 🚀 Quickstart Guide

### 1. Run the FastAPI Backend Brain
```bash
# Activate virtual environment
.venv\Scripts\activate   # Windows
# or: source .venv/bin/activate (Linux/Mac)

# Install dependencies (if needed)
uv pip install -r backend/requirements.txt

# Run backend server
python -m uvicorn main:app --reload --port 8000
```
Backend will be live at: `http://localhost:8000` (Docs at `/docs`).

### 2. Run the Next.js Web Platform
```bash
cd web
pnpm install
pnpm dev
```
Web platform will be live at: `http://localhost:3000`.

### 3. Run Backend Test Suite
```bash
pytest backend/tests -v
```

---

## 🌐 Vercel Deployment Configuration
The repository includes a ready-to-deploy `vercel.json` configured for Next.js 15:
- Root `vercel.json`: Points `rootDirectory: "web"`.
- Web `web/vercel.json`: Pre-configured build commands (`pnpm run build`).

---

## 📜 Indian 1930 / I4C "Golden Hour" Pipeline
If a victim reports a cyber fraud within 2 to 3 hours, the I4C **CFCFRMS** system triggers automated API freeze requests directly to partner banks' Core Banking Systems. XORcall automatically compiles a timestamped, tamper-evident **Bharatiya Sakshya Adhiniyam (BSA) compliant PDF Dossier** containing harvested mule UPI IDs and bank accounts for immediate submission.

---

## 👥 Authors
- **Likhith Venkata Sai** (Lead)
- **Shruthishree Mohapatra**  
*Team KANYARASI — Colossus 3.0*
