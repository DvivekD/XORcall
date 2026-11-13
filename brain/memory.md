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
- **Adaptive Soft-Knee AGC & Groq Whisper Domain Prompt Biasing (`v1.2.3`)**:
  1. *Adaptive Soft-Knee AGC*: Eliminated harsh `7.5x tanh` saturation on max-volume speakerphone audio. Replaced with dynamic peak-tracking automatic gain control that maintains pristine, distortion-free speech formants for Vosk and Whisper.
  2. *Groq Whisper Turbo Domain Prompt Biasing*: Injected cyber extortion, Digital Arrest, police custody, and Indian currency prompt biasing into Whisper Turbo multipart requests, drastically increasing recognition probability on accents and loud in-call audio.
  3. *Comprehensive Phonetic Extortion Normalizer*: Added coverage for split words, acoustic distortions, and misrecognitions ("police custom" -> "police custody", "digital address" -> "digital arrest", "five lock/lacks/block" -> "5 lakh", "egg stortion" -> "extortion", "why are transfer" -> "wire transfer").
  4. *One-Tap Quick Paste Groq API Key*: Added one-tap "📋 Paste Key" in the Cloud STT configuration dialog with direct guidance on free instant key generation at `console.groq.com`.
- **Permanent Keystore Signing & In-Place Update Compatibility (`v1.2.2`)**:
  1. *Committed Permanent Keystore (`debug.keystore`)*: Resolved `INSTALL_FAILED_UPDATE_INCOMPATIBLE` ("Check APK") by committing a permanent PKCS12 debug keystore and enabling both `enableV1Signing = true` (JAR signing) and `enableV2Signing = true` (APK Sig Block 42). All future CI builds use the exact same signature, allowing smooth updates without uninstalling.
- **In-Call Audio Stream (`VOICE_RECOGNITION`), Android 14 Speakerphone Routing & Crash-Proof Mic Test (`v1.2.1`)**:
  1. *Live Mic Test Thread Safety Fix*: Fixed `CalledFromWrongThreadException` and Compose background snapshot state concurrency by strictly executing `FloatingAlertOverlay` WindowManager calls and all Compose state updates on `Dispatchers.Main` / `Looper.getMainLooper()`.
  2. *Android 14 In-Call Audio Stream*: Bypassed Android 14 `AudioPolicy` silence suppression on standard `AudioSource.MIC` during cellular phone calls by prioritizing `MediaRecorder.AudioSource.VOICE_RECOGNITION`. `VOICE_RECOGNITION` grants third-party STT engines active audio input during telephony and disables hardware echo-cancellation from filtering out remote party speech.
  3. *Android 12+ (API 31+) Speakerphone Routing*: Switched deprecated `audioManager.isSpeakerphoneOn = true` to `audioManager.setCommunicationDevice(TYPE_BUILTIN_SPEAKER)`, ensuring reliable hardware loudspeaker audio routing on Android 12, 13, and 14 (tested on FuntouchOS / MediaTek Dimensity).
  4. *Calibrated AI Voice vs Biological Human Detection*: Redesigned acoustic radar scoring model: natural human vocal cord micro-tremor (jitter >= 0.50%) forces synthetic confidence to nominal (<0.10). Neural vocoder flag strictly requires rigid harmonic pitch contours (<0.28% jitter over >=6 voiced frames) and elevated spectral Wiener entropy (>0.42).
- **Auto-Speakerphone In-Call Interceptor & Groq Whisper Cloud Hybrid STT (`v1.2.0`)**: 
  1. *Auto-Speakerphone*: Automatically engages loudspeaker on `OFFHOOK` and binds raw `MIC` stream with +20dB digital AGC amplification. Completely bypasses Android 14 hardware echo cancellation (`VOICE_COMMUNICATION` AEC) that historically stripped the remote caller's audio from being captured by third-party apps. Captures both parties clearly.
  2. *Dual Hybrid Speech-to-Text*: High-accuracy Groq LPU Whisper Turbo (`whisper-large-v3-turbo`) with sub-200ms latency for Indian English and Hinglish transcription, backed by 100% offline bundled Vosk Kaldi model as local baseline.
  3. *Scientific AI Voice Clone Radar*: Pure on-device 512-point Cooley-Tukey FFT, Hann windowing, Wiener entropy spectral flatness, high-frequency vocoder shelf cutoff (>6.5 kHz), and Normalized Autocorrelation (NACF) biological pitch micro-jitter tracking (0.5%-2.5% human vocal cords vs <0.30% neural vocoder rigidity). Real-time visual radar and metrics on mobile dashboard.
- **Direct Telephony Audio Interceptor & Phonetic Extortion Normalizer (`v1.1.0`)**: Direct duplex hardware audio capture prioritizing `VOICE_CALL` (modem line) and `VOICE_COMMUNICATION` with dynamic +20dB AGC pre-amplification. Engages automatically on `EXTRA_STATE_OFFHOOK` without contact whitelist blocking. Features `PhoneticSpeechNormalizer` resolving Indian English speech phonetics from US Kaldi acoustic models (`"five lacks"`, `"five lock"`, `"50 lacks"` -> canonical `"5 lakh"`, `"kid nap"` -> `"kidnap"`), multi-listener thread-safe STT pipeline, and live dashboard audio energy / source meter.
- **Vosk Bundled On-Device Speech Recognition Engine (`v1.0.9`)**: Resolved Vivo FuntouchOS / iManager installation parse check by bundling offline model as a clean uncompressed asset zip (`model-en.zip`) and locking ABI filters to `arm64-v8a` + `armeabi-v7a` (66MB APK). Unpacks in 0.5s from flash storage. Raw 16kHz PCM from `AudioRecord(VOICE_COMMUNICATION)` and `AudioRecord(MIC)` feeds directly into native Vosk Kaldi recognizer for real-time speech-to-text during live phone calls and direct in-app mic verification.
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
