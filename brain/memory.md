# Memory Log

## Project: XORcall (Colossus 3.0 Hackathon)
- **Repo**: `https://github.com/DvivekD/XORcall.git`
- **Team**: KANYARASI (Likhith Venkata Sai, Shruthishree Mohapatra)
- **Premise**: Real-time on-device/hybrid AI cyber defense system against "Digital Arrest" scams and "Family AI Video/Voice Clones" in India.
- **Architectural Decisions (Locked via /grill-me)**:
  1. *Mobile*: Kotlin + Jetpack Compose Native Android client (`v1.0.2` Live on GitHub Releases: `XORcall-v1.0.2-debug.apk`). Features Proximity Haptics, Sherpa-ONNX streaming STT, Shizuku shell capture, Package Watchdog, and Live Microphone Voice Spotter.
  2. *Web*: Next.js 16 + React 19 + Three.js with StringTune 3D Katana (`katana.glb`), NIST 5-Phase Octet Medallion (`IDENTIFY` -> `RECOVER`), Space Grotesk typography, and "Defend With Clarity" light bone theme.
  3. *Backend*: FastAPI + Groq LPU (Llama 3.3 70B @ <200ms) + OpenCV 2D-FFT Deepfake Inspector + ReportLab 1930 Dossier Generator (All 15 unit tests passing).
  4. *Counter-Measure*: "Kavach AI Decoy" (Ramesh Gupta persona) with time-wasted tracker and mule account extractor.
  5. *Structure*: Full Monorepo (`backend/`, `web/`, `android/`, `fixtures/`).

## Key Defensive Innovations & Milestones
- **Vosk Bundled On-Device Speech Recognition Engine (`v1.0.8`)**: 100% offline Kaldi acoustic speech model bundled directly into the APK assets (88MB APK). Zero runtime downloads. Unpacks in 0.5s from flash storage. Raw 16kHz PCM from `AudioRecord(VOICE_COMMUNICATION)` and `AudioRecord(MIC)` feeds directly into native Vosk Kaldi recognizer for real-time speech-to-text during live phone calls and direct in-app mic verification.
- **Taptic Haptic Sensor Shield**: Escalating vibration shockwave patterns against ear/hand via Proximity Sensor with `hasAmplitudeControl()` defensive fallbacks for Vivo V25 Pro / FuntouchOS.
- **On-Device Real-Time Speech Lexicon (`SherpaSTTManager`)**: 50+ extortion keywords across kinship accident, hospital, police custody, bail, CBI, narcotics, digital arrest, and RBI escrow.
- **Incoming Phone State Receiver (`PhoneCallReceiver`)**: Automatically invokes `CallMonitorService` upon `EXTRA_STATE_RINGING` or `EXTRA_STATE_OFFHOOK`.
- **Heads-Up Floating Alert Banner & Urgent Notification (`FloatingAlertOverlay`)**: Dual-layer alert system with `PRIORITY_MAX` alarm notification + floating window over dialer with 1-click 1930 speed dial.
- **StringTune 3D Katana (`CyberKatana3D.tsx`)**: Exact video alignment with handle top-right (+22°), scabbard bottom-left, gold Habaki `( べ )` centered, and smooth unsheathing kinematics.
- **NIST 5-Phase Octet Medallion (`CyberOctagonMedallion.tsx`)**: High-DPI SVG regular octagon cycling through the 5 defense stages.
- **Light Theme Transition (`#e2dde3`)**: Replicates "Defend With Clarity", centered `( べ )`, and 4-column numbered breakdown.

## 3 Judge Critiques Tackled
1. *False Positives (Legitimate vs Scam)*: Mathematical Intent Conjunction Rule + Benign Mitigators (physical hospital presence `-0.40`, institutional ERP `-0.35`). Real emergencies demand physical presence; scams enforce isolation and unverified digital wire transfers.
2. *Multi-Demographic Scope*: Expanded beyond seniors to college students (Telegram task/fee fraud), working professionals (CEO whaling), and parents (voice clones).
3. *Modular Sensory Defense*: Customizable alerts (Taptic Cheek Haptics, In-Ear Audio Whisper TTS, Heads-Up Screen Overlay, Silent Family Safe-Ping) for vibration-averse users.

## Hackathon Submission & Pitch Assets
- **Full-Screen Interactive Pitch Deck (`/pitch`)**: 6 presentation slides with full-screen mode (`F`), arrow key navigation, and high-impact jury defense data.
- **Turnkey Live Demo CLI (`scripts/run_live_demo.py`)**: 3-second live terminal demonstration of real-time speech intent conjunction (142ms latency), threat escalation, legal dossier creation, and hospital mitigator validation.
- **Sample BSA Section 63 Forensic Dossier (`fixtures/dossiers/live_demo_dossier.pdf`)**: Real generated tamper-evident evidentiary PDF report with SHA-256 hash.
- **Devpost Submission Document (`DEVPOST_SUBMISSION.md`)**: Comprehensive copy-pasteable submission text with problem statement, architecture diagrams, challenges, and future roadmap.
- **Dual-Mode Community Validation (`/community`)**: Interactive upvote / sentiment canvas + high-resolution static Reddit capture.
- **All 15 Unit Tests Passing**: Verified with `pytest backend/tests -v`.
- **Git State**: Clean branch `master` pushed to GitHub.
