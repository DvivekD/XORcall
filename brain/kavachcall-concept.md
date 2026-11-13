# KavachCall: Master System Specification & Defense Architecture

## 1. Problem Space & Threat Taxonomy
KavachCall defends against the two most lethal social engineering vectors threatening families and seniors today:
1. **The Authority Coercion Crisis ("Digital Arrest")**:
   - Transnational syndicates impersonating CBI, ED, Mumbai Police, and Supreme Court judges over WhatsApp/Skype video calls.
   - Victims subjected to fake arrest warrants under OSA/Section 144, 24x7 sensory isolation, and forced to liquidate life savings into "RBI Escrow" mule accounts.
2. **The Kinship Manipulation Crisis ("Family AI Clone & Virtual Kidnap")**:
   - Scammers cloning a family member's voice and live video (using deepfake face-swapping tools like DeepFaceLive).
   - Simulating catastrophic emergencies (car accident, narcotics arrest, kidnapping) to extort immediate ransom/bail via UPI.

## 2. The 5-Layer Defense Architecture
- **Layer 1: Hardware & Sensory Shield**: Android `Sensor.TYPE_PROXIMITY` + `VibratorManager` waveform haptics (escalating from heartbeat to violent pulse when phone is against the ear).
- **Layer 2: Acoustic & Voice Forensics**: On-device `sherpa-onnx` streaming Zipformer (<150ms) + Neural Vocoder Phase/Spectral Incoherence (>8kHz cutoff) + Vocal Agitation/Prosody.
- **Layer 3: Visual Forensics ("KavachVision")**: 2D-FFT frequency domain grid detection (upscaling artifacts) + facial boundary/collar seam warping + official police emblem/badge verification + blink/liveness analysis.
- **Layer 4: Cognitive & Semantic Kill-Chain**: 5-stage social engineering ladder (Authority/Kinship -> Crisis -> Isolation -> Extortion) evaluated by Groq Llama-3.3-70B in <250ms with native Indic code-mixing (Hindi, Kannada, Tamil, Telugu, Hinglish).
- **Layer 5: Active Intervention & "Breaking Isolation"**:
  - Out-of-Band Family Safe-Ping with Passive Heartbeat (GPS/Wi-Fi sanity + Device Activity check when family member is unresponsive).
  - Secondary Family Escalation Tree (pings roommate, spouse, daughter if son doesn't reply in 45s).
  - Zero-Trust Family Safe-Word prompt.
  - In-Ear Synthesized Whisper alert.
  - Banking App & Remote Desktop (AnyDesk/TeamViewer) Collateral Lock during suspicious video calls.
  - 1-Click 1930 / I4C CFCFRMS Golden Hour Cybercrime Dossier.

## 3. Hardened Loophole Matrix & Mitigations
1. **Unresponsive Relative**: Passive device heartbeat (screen-on, home/office Wi-Fi match, location sanity) + multi-hop family tree escalation.
2. **Multilingual & Dialect Evasion**: AI4Bharat / Bhashini IndicConformer on edge + Groq Llama 3.3 70B multilingual code-switch understanding.
3. **Legitimate Emergency vs Scam (False Positives)**: Conjunction rule - real emergencies invite physical presence; scams enforce isolation + immediate unverified digital transfers.
4. **Offline / Poor Network**: Local on-device deterministic kill-chain + Sherpa-ONNX runs 100% offline.
5. **Remote Desktop Hijack (AnyDesk/RustDesk)**: Foreground package sniffer instantly blocks screen-sharing tools during unknown video calls.

