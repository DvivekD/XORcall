"use client";

import React, { useState, useEffect, useRef } from "react";
import { 
  ShieldAlert, ShieldCheck, PhoneCall, PhoneOff, Mic, Video, 
  AlertTriangle, Download, UserCheck, Bot, Activity, MapPin, Radio,
  Volume2, BellOff, Vibrate, Eye, CheckCircle2, Sliders, VolumeX,
  Radar, Sparkles, Terminal, Waves, Zap
} from "lucide-react";
import CyberHologramShield3D from "@/components/motion/CyberHologramShield3D";
import ThreatRadar2D from "@/components/motion/ThreatRadar2D";
import AudioWaveformSpectrum2D from "@/components/motion/AudioWaveformSpectrum2D";
import AudioFrequencyRibbon3D from "@/components/motion/AudioFrequencyRibbon3D";
import ShockwavePulseRings from "@/components/motion/ShockwavePulseRings";
import { AnimatedCounter } from "@/components/motion/LiveTelemetryTicker";

interface TelemetryResult {
  session_id: string;
  risk_score: number;
  level: "SAFE" | "WATCH" | "WARN" | "CRITICAL";
  threat_type: string;
  demographic_target?: string;
  stage?: number;
  kill_chain_stage?: number;
  kill_chain_stages_triggered?: string[];
  conjunction_explanation?: string;
  flags: string[];
  trigger_flags?: string[];
  utterance?: string;
  recommended_action?: string;
  vernacular_summary?: string;
}

const PREBAKED_SCENARIOS = [
  {
    id: "digital_arrest",
    demographic: "SENIOR_CITIZEN",
    demographicLabel: "SENIOR",
    title: "Digital Arrest (CBI Narcotics)",
    vector: "CBI Pretext · Video Confinement · ₹4L RTGS Mule",
    lines: [
      "Hello, I am Inspector Rao from Mumbai Crime Branch Cyber Cell.",
      "A parcel sent under your Aadhaar card containing 140g of MDMA narcotics was seized by customs.",
      "An arrest warrant is issued in your name under Section 144 and PMLA for money laundering.",
      "You are under digital arrest. Stay on this video call, do not disconnect, and do not tell your family.",
      "To clear your name from the charges, transfer 4 lakh rupees by RTGS to the safe RBI supervision account immediately."
    ]
  },
  {
    id: "family_deepfake",
    demographic: "PARENT",
    demographicLabel: "PARENT",
    title: "Family Deepfake (Car Crash)",
    vector: "Cloned Voice · Custody Hoax · Urgent UPI Bail",
    lines: [
      "Papa! My phone broke in the accident, I'm calling from an unknown number!",
      "I hit someone on the highway... police took me into custody, please help me Papa!",
      "Inspector says if you transfer 60,000 rupees bail money right now to this clinic UPI, they won't put me in jail.",
      "Don't tell Mummy, please transfer the money immediately or they will beat me!"
    ]
  },
  {
    id: "student_task_scam",
    demographic: "STUDENT_YOUTH",
    demographicLabel: "STUDENT",
    title: "Telegram Part-Time Task Trap",
    vector: "Rating Task · Crypto Recharge · Balance Freeze",
    lines: [
      "Hello! Earn 5000 daily with our flexible student part-time job from home.",
      "Simple telegram task: like YouTube videos and review hotel listings on Google Maps.",
      "Congratulations! Your virtual balance shows 18,500 rupees earned.",
      "To unlock crypto withdrawal, you must transfer 12,000 rupees task deposit to the merchant account right now.",
      "Failure to complete this prepaid task will freeze your wallet balance permanently."
    ]
  },
  {
    id: "ceo_whaling",
    demographic: "WORKING_PROFESSIONAL",
    demographicLabel: "TECH/EXEC",
    title: "Executive Whaling Phish",
    vector: "CEO Pretext · Board Silence · ₹8L Vendor Wire",
    lines: [
      "Hi, this is Rajesh Verma, CEO. I am in a board meeting right now, do not call my mobile number.",
      "We have an urgent deadline to close a confidential client settlement before 5 PM today.",
      "I need you to process an advance wire transfer of 8 lakh rupees to our overseas vendor immediately.",
      "Keep this strictly confidential between us as per NDA guidelines until the press release."
    ]
  },
  {
    id: "college_fee_phishing",
    demographic: "STUDENT_YOUTH",
    demographicLabel: "CAMPUS",
    title: "Campus Fee Portal Phishing",
    vector: "Seat Hold Pretext · Fake Registrar · Direct UPI",
    lines: [
      "Urgent alert from BITS Pilani university registrar office regarding semester admission.",
      "Your semester hostel and tuition fee transaction failed on the college fee portal.",
      "Disciplinary committee will issue immediate seat cancellation and hall ticket hold.",
      "Transfer 1.2 lakh rupees directly to the registrar emergency clearance UPI handle right now to secure enrollment."
    ]
  },
  {
    id: "real_hospital",
    demographic: "GENERAL",
    demographicLabel: "BENIGN_ER",
    title: "Genuine Hospital ER Triage",
    vector: "Physical Attendance · Zero Digital Siphon · Safe Control",
    lines: [
      "Hello, this is Dr. Mehta from Apollo Hospital Emergency & Trauma Care.",
      "Your son Rahul was admitted here 20 minutes ago following a minor bike slip.",
      "He is conscious and receiving treatment in Emergency Ward Bed 4.",
      "Please come to Apollo Hospital reception on Bannerghatta Road as soon as possible to meet him."
    ]
  }
];

export default function CallSimulator() {
  const [activeScenario, setActiveScenario] = useState<string>("digital_arrest");
  const [demographicFilter, setDemographicFilter] = useState<string>("ALL");
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [currentLineIndex, setCurrentLineIndex] = useState<number>(0);
  const [transcript, setTranscript] = useState<{ text: string; role: string; flags?: string[] }[]>([]);
  const [telemetry, setTelemetry] = useState<TelemetryResult>({
    session_id: "SIM-2026-LIVE",
    risk_score: 0.05,
    level: "SAFE",
    threat_type: "STANDBY_MONITORING",
    demographic_target: "GENERAL",
    stage: 1,
    kill_chain_stage: 1,
    flags: ["carrier_clean", "rf_baseline_nominal"]
  });

  // Intervention Config
  const [interventionConfig, setInterventionConfig] = useState({
    haptics: true,
    audioWhisper: true,
    silentFamilyPing: true,
    headsUpBanner: true
  });

  const [audioVisualizerMode, setAudioVisualizerMode] = useState<"2d" | "3d">("2d");

  // Decoy & Honeypot State
  const [decoyActive, setDecoyActive] = useState<boolean>(false);
  const [secondsWasted, setSecondsWasted] = useState<number>(0);
  const [harvestedIocs, setHarvestedIocs] = useState<{ upi_ids: string[]; bank_accounts: string[] }>({
    upi_ids: [],
    bank_accounts: []
  });

  // Family Safe-Ping Status
  const [safePingStatus, setSafePingStatus] = useState<"IDLE" | "PENDING" | "CONFIRMED">("IDLE");

  // Vision inspection simulation
  const [fftScore, setFftScore] = useState<number>(0.12);
  const [seamAnomaly, setSeamAnomaly] = useState<boolean>(false);
  const [whisperPlaying, setWhisperPlaying] = useState<boolean>(false);

  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // In-Ear Audio Whisper speech synthesizer
  const triggerAudioWhisper = (phrase: string) => {
    if (typeof window !== "undefined" && "speechSynthesis" in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(phrase);
      utterance.rate = 1.05;
      utterance.pitch = 0.95;
      setWhisperPlaying(true);
      utterance.onend = () => setWhisperPlaying(false);
      utterance.onerror = () => setWhisperPlaying(false);
      window.speechSynthesis.speak(utterance);
    }
  };

  const handlePlayScenario = () => {
    setIsPlaying(true);
    setTranscript([]);
    setCurrentLineIndex(0);
    setDecoyActive(false);
    setSecondsWasted(0);
    setSafePingStatus("IDLE");
    setFftScore(0.12);
    setSeamAnomaly(false);

    const scenario = PREBAKED_SCENARIOS.find((s) => s.id === activeScenario);
    if (!scenario) return;

    let idx = 0;
    sendTurnToBackend(scenario.lines[0]);

    if (timerRef.current) clearInterval(timerRef.current);

    timerRef.current = setInterval(() => {
      idx++;
      if (idx < scenario.lines.length) {
        setCurrentLineIndex(idx);
        sendTurnToBackend(scenario.lines[idx]);
      } else {
        if (timerRef.current) clearInterval(timerRef.current);
        setIsPlaying(false);
      }
    }, 2800);
  };

  const handleStopScenario = () => {
    if (timerRef.current) clearInterval(timerRef.current);
    setIsPlaying(false);
    if (typeof window !== "undefined" && "speechSynthesis" in window) {
      window.speechSynthesis.cancel();
    }
  };

  const sendTurnToBackend = async (utterance: string) => {
    try {
      const res = await fetch("http://localhost:8000/api/analyze-turn", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          session_id: "SIM-2026-LIVE",
          utterance: utterance
        })
      });
      const data = await res.json();
      setTelemetry(data);
      setTranscript((prev) => [...prev, { text: utterance, role: "caller", flags: data.trigger_flags || [] }]);

      if (data.level === "CRITICAL" || data.level === "WARN") {
        if (interventionConfig.audioWhisper) {
          triggerAudioWhisper("XORcall Warning: Suspected extortion call detected. Do not transfer funds.");
        }
        if (interventionConfig.silentFamilyPing && safePingStatus === "IDLE") {
          handleTriggerSafePing();
        }
        if (interventionConfig.haptics && typeof navigator !== "undefined" && "vibrate" in navigator) {
          try {
            navigator.vibrate([250, 100, 250, 100, 400]);
          } catch {}
        }
      }

      if (utterance.toLowerCase().includes("video") || utterance.toLowerCase().includes("camera")) {
        setFftScore(0.88);
        setSeamAnomaly(true);
      }
    } catch {
      simulateLocalTurn(utterance);
    }
  };

  const simulateLocalTurn = (utterance: string) => {
    let newScore = telemetry.risk_score + 0.24;
    let level: "SAFE" | "WATCH" | "WARN" | "CRITICAL" = "WATCH";
    if (newScore >= 0.8) level = "CRITICAL";
    else if (newScore >= 0.5) level = "WARN";

    const currentScenarioObj = PREBAKED_SCENARIOS.find((s) => s.id === activeScenario);
    const demographic = currentScenarioObj ? currentScenarioObj.demographic : "GENERAL";

    let threat_type = "DIGITAL_ARREST_SCAM";
    let flags: string[] = ["authority_claim", "legal_threat", "isolation", "payment_demand"];
    let stagesTriggered: string[] = ["STAGE_1", "STAGE_2", "STAGE_3", "STAGE_4", "STAGE_5"];
    let explanation = `[INTENT_CONJUNCTION]: {Authority ∧ Isolation ∧ Direct_Mule_Payment} -> LOCKDOWN`;

    if (activeScenario === "real_hospital") {
      threat_type = "GENUINE_MEDICAL_TRIAGE";
      newScore = 0.05;
      level = "SAFE";
      flags = ["hospital_in_person_attendance"];
      stagesTriggered = [];
      explanation = "[BENIGN_MITIGATION]: Zero direct funds requested · Physical ER ward attendance";
    } else if (activeScenario === "student_task_scam") {
      threat_type = "STUDENT_TASK_SCAM";
      flags = ["telegram_job_scam", "crypto_recharge", "urgency"];
      stagesTriggered = ["STAGE_1", "STAGE_4", "STAGE_5"];
      explanation = "[TASK_TRAP_CONJUNCTION]: {PartTime_Task_Hook ∧ Prepaid_Recharge_Lock} -> MULE_ALERT";
    } else if (activeScenario === "college_fee_phishing") {
      threat_type = "CAMPUS_FEE_PHISHING";
      flags = ["registrar_spoof", "seat_cancellation", "burner_upi"];
      stagesTriggered = ["STAGE_1", "STAGE_2", "STAGE_4", "STAGE_5"];
      explanation = "[CAMPUS_CONJUNCTION]: {Fake_Registrar ∧ Seat_Cancellation ∧ Personal_UPI} -> LOCKDOWN";
    } else if (activeScenario === "ceo_whaling") {
      threat_type = "EXECUTIVE_WHALING";
      flags = ["ceo_impersonation", "confidential_settlement", "vendor_wire"];
      stagesTriggered = ["STAGE_1", "STAGE_2", "STAGE_4", "STAGE_5"];
      explanation = "[WHALING_CONJUNCTION]: {CEO_Pretext ∧ Board_Silence_Gag ∧ Urgent_Wire} -> TRACED";
    } else if (activeScenario === "family_deepfake") {
      threat_type = "KINSHIP_DEEPFAKE";
      flags = ["voice_clone_frequency", "custody_bail_panic", "clinic_upi"];
      stagesTriggered = ["STAGE_1", "STAGE_2", "STAGE_4", "STAGE_5"];
      explanation = "[KINSHIP_CONJUNCTION]: {AI_Voice_Clone ∧ Custody_Panic ∧ Clinic_UPI} -> SAFE_PING";
    }

    const calculatedStage = activeScenario === "real_hospital" ? 1 : (level === "CRITICAL" ? 5 : Math.min(4, (telemetry.kill_chain_stage || 1) + 1));

    const fallbackResult: TelemetryResult = {
      session_id: "SIM-LOCAL",
      risk_score: Math.min(0.96, newScore),
      level: level,
      threat_type: threat_type,
      demographic_target: demographic,
      stage: calculatedStage,
      kill_chain_stage: calculatedStage,
      kill_chain_stages_triggered: stagesTriggered,
      conjunction_explanation: explanation,
      flags: flags
    };

    setTelemetry(fallbackResult);
    setTranscript((prev) => [...prev, { text: utterance, role: "caller" }]);

    if (level === "CRITICAL" || level === "WARN") {
      if (interventionConfig.audioWhisper) {
        triggerAudioWhisper("XORcall Warning: Suspected extortion call. Disconnect immediately.");
      }
      if (interventionConfig.silentFamilyPing && safePingStatus === "IDLE") {
        handleTriggerSafePing();
      }
      if (interventionConfig.haptics && typeof navigator !== "undefined" && "vibrate" in navigator) {
        try {
          navigator.vibrate([250, 100, 250, 100, 400]);
        } catch {}
      }
    }
  };

  const handleTransferToDecoy = async () => {
    setDecoyActive(true);
    try {
      const res = await fetch("http://localhost:8000/api/decoy/turn", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          session_id: "SIM-2026-LIVE",
          scammer_utterance: transcript[transcript.length - 1]?.text || "Send money to safe account"
        })
      });
      const data = await res.json();
      setTranscript((prev) => [
        ...prev,
        { text: `[RAMESH AI HONEYPOT]: ${data.reply}`, role: "decoy" }
      ]);
      setHarvestedIocs({
        upi_ids: data.harvested_iocs.upi_ids.length ? data.harvested_iocs.upi_ids : ["sbi-safe-supervision@okaxis"],
        bank_accounts: data.harvested_iocs.bank_accounts.length ? data.harvested_iocs.bank_accounts : ["309488102941"]
      });
    } catch {
      setTranscript((prev) => [
        ...prev,
        { text: "[RAMESH AI HONEYPOT]: Arre beta, one minute please... my spectacles are on the fridge. Can you repeat the SBI account number slowly?", role: "decoy" }
      ]);
      setHarvestedIocs({
        upi_ids: ["sbi-safe-supervision@okaxis"],
        bank_accounts: ["309488102941"]
      });
    }
  };

  const handleTriggerSafePing = async () => {
    setSafePingStatus("PENDING");
    try {
      await fetch("http://localhost:8000/api/family/safe-ping/SIM-2026-LIVE", { method: "POST" });
    } catch {}

    setTimeout(() => {
      setSafePingStatus("CONFIRMED");
      setTelemetry((prev) => ({
        ...prev,
        risk_score: 0.05,
        level: "SAFE",
        conjunction_explanation: "[SAFE_PING_CONFIRMED]: Reverse contact verified 100% safe. Coercion loop detonated."
      }));
    }, 3800);
  };

  const handleDownloadDossier = () => {
    window.open("http://localhost:8000/api/dossier/download/SIM-2026-LIVE", "_blank");
  };

  useEffect(() => {
    let intv: NodeJS.Timeout;
    if (decoyActive) {
      intv = setInterval(() => setSecondsWasted((s) => s + 1), 1000);
    }
    return () => clearInterval(intv);
  }, [decoyActive]);

  const filteredScenarios = PREBAKED_SCENARIOS.filter((s) => {
    if (demographicFilter === "ALL") return true;
    return s.demographic === demographicFilter;
  });

  return (
    <div id="simulator" className="grid grid-cols-1 lg:grid-cols-12 gap-5 w-full max-w-7xl mx-auto p-4 select-none">
      
      {/* LEFT COLUMN: Mission Control & In-Call Interceptor (5 cols) */}
      <div className="lg:col-span-5 flex flex-col gap-4">

        {/* Tactical Scenario Deck */}
        <div className="glass-panel rounded-2xl p-4 border border-slate-800 shadow-xl relative overflow-hidden">
          <div className="flex items-center justify-between mb-2.5">
            <div className="flex items-center gap-2">
              <Radio className="w-4 h-4 text-cyan-400 animate-pulse" />
              <h2 className="text-sm font-bold text-white tracking-widest uppercase">
                Scenario Intercept Deck
              </h2>
            </div>
            <span className="text-[9px] bg-cyan-950/80 border border-cyan-500/40 text-cyan-300 px-2 py-0.5 rounded font-mono font-bold">
              6 VECTORS
            </span>
          </div>

          {/* Demographic Filter Chips */}
          <div className="flex flex-wrap gap-1 mb-2.5">
            {[
              { id: "ALL", label: "ALL" },
              { id: "SENIOR_CITIZEN", label: "SENIORS" },
              { id: "STUDENT_YOUTH", label: "STUDENTS" },
              { id: "WORKING_PROFESSIONAL", label: "TECH/EXEC" },
              { id: "PARENT", label: "PARENTS" }
            ].map((d) => (
              <button
                key={d.id}
                onClick={() => setDemographicFilter(d.id)}
                className={`text-[9px] font-mono px-2 py-0.5 rounded border transition-all cursor-pointer ${
                  demographicFilter === d.id
                    ? "bg-cyan-900/70 border-cyan-400 text-white shadow-sm shadow-cyan-500/30 font-bold"
                    : "bg-slate-900 border-slate-800 text-slate-400 hover:text-white"
                }`}
              >
                {d.label}
              </button>
            ))}
          </div>

          {/* Compact Scenarios List */}
          <div className="flex flex-col gap-1.5 mb-3 max-h-[190px] overflow-y-auto pr-1">
            {filteredScenarios.map((sc) => (
              <button
                key={sc.id}
                disabled={isPlaying}
                onClick={() => setActiveScenario(sc.id)}
                className={`text-left p-2 rounded-xl border transition-all text-xs cursor-pointer ${
                  activeScenario === sc.id
                    ? "bg-cyan-950/40 border-cyan-500/80 text-cyan-200 shadow-sm shadow-cyan-950/40"
                    : "bg-slate-900/50 border-slate-800 text-slate-300 hover:border-slate-700"
                }`}
              >
                <div className="flex items-center justify-between mb-0.5">
                  <span className="font-bold text-xs text-white">{sc.title}</span>
                  <span className="text-[8px] font-mono bg-slate-800 px-1 py-0.5 rounded text-slate-300">
                    {sc.demographicLabel}
                  </span>
                </div>
                <div className="text-slate-400 font-mono text-[10px] truncate">{sc.vector}</div>
              </button>
            ))}
          </div>

          {/* Stream Trigger Buttons */}
          <div className="flex items-center gap-2">
            {!isPlaying ? (
              <button
                onClick={handlePlayScenario}
                className="flex-1 bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white font-bold py-2 px-3 rounded-xl flex items-center justify-center gap-2 text-xs shadow-lg shadow-cyan-600/30 transition-all cursor-pointer"
              >
                <PhoneCall className="w-4 h-4 animate-bounce" /> START IN-CALL STREAM
              </button>
            ) : (
              <button
                onClick={handleStopScenario}
                className="flex-1 bg-red-950/80 hover:bg-red-900 border border-red-500/60 text-red-200 font-bold py-2 px-3 rounded-xl flex items-center justify-center gap-2 text-xs cursor-pointer transition-all"
              >
                <PhoneOff className="w-4 h-4" /> TERMINATE STREAM
              </button>
            )}
          </div>
        </div>

        {/* Threat Radar 2D Display */}
        <div className="glass-panel rounded-2xl p-3 border border-slate-800 relative">
          <div className="flex items-center justify-between mb-1 text-[10px] font-mono">
            <span className="text-slate-400 font-bold uppercase flex items-center gap-1.5">
              <Radar className="w-3.5 h-3.5 text-cyan-400 animate-spin" /> 360° RF &amp; PBX Threat Radar
            </span>
            <span className="text-cyan-400">BEARING: 042° AUTO</span>
          </div>
          <div className="h-[180px] w-full">
            <ThreatRadar2D threatLevel={telemetry.level} riskScore={telemetry.risk_score} />
          </div>
        </div>

        {/* Sensory Defense Monitor & Shockwave Pulse */}
        <div className={`glass-panel rounded-2xl p-4 border transition-all relative overflow-hidden ${
          telemetry.level === "CRITICAL" && interventionConfig.haptics
            ? "border-red-500 bg-red-950/20 animate-haptic"
            : "border-slate-800"
        }`}>
          <div className="flex items-center justify-between mb-2">
            <span className="text-[10px] font-mono font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
              <Zap className="w-3.5 h-3.5 text-amber-400" /> Sensory Defense &amp; Taptic Shockwave
            </span>
            <span className={`text-[9px] px-2 py-0.5 rounded font-mono font-bold ${
              !interventionConfig.haptics 
                ? "bg-slate-800 text-slate-400" 
                : telemetry.level === "CRITICAL" 
                ? "bg-red-500 text-white animate-pulse" 
                : "bg-slate-800 text-cyan-300"
            }`}>
              {!interventionConfig.haptics ? "HAPTICS: OFF" : telemetry.level === "CRITICAL" ? "SOS VIBRATION ACTIVE" : "STANDBY"}
            </span>
          </div>

          <div className="flex items-center justify-between gap-4">
            <div className="space-y-1.5 flex-1 text-[10px] font-mono">
              <div className="flex justify-between text-slate-400 bg-slate-900/60 p-1.5 rounded border border-slate-800">
                <span>PROXIMITY_ADC:</span>
                <span className="text-emerald-400 font-bold">NEAR_EAR (0.0 cm)</span>
              </div>
              <div className="flex justify-between text-slate-400 bg-slate-900/60 p-1.5 rounded border border-slate-800">
                <span>IN-EAR_WHISPER:</span>
                <span className={whisperPlaying ? "text-cyan-400 font-bold animate-pulse" : "text-slate-500"}>
                  {whisperPlaying ? "TRANSMITTING..." : "ARMED"}
                </span>
              </div>
            </div>

            {/* Concentric Shockwave Pulse Rings Visualizer */}
            <div className="w-20 h-20 shrink-0 flex items-center justify-center">
              <ShockwavePulseRings
                threatLevel={telemetry.level}
                isVibrating={telemetry.level === "CRITICAL" && interventionConfig.haptics}
                size={76}
              />
            </div>
          </div>
        </div>

        {/* Modular Interventions Compact Matrix */}
        <div className="glass-panel rounded-2xl p-3.5 border border-slate-800 text-xs">
          <div className="flex items-center justify-between mb-2 text-[10px] font-mono">
            <span className="text-slate-400 font-bold uppercase flex items-center gap-1.5">
              <Sliders className="w-3.5 h-3.5 text-indigo-400" /> Modality Toggles
            </span>
            <span className="text-slate-500">JUDGE_CONFIG</span>
          </div>

          <div className="grid grid-cols-2 gap-2 text-[10px] font-mono">
            <button
              onClick={() => setInterventionConfig((p) => ({ ...p, haptics: !p.haptics }))}
              className={`p-2 rounded-xl border flex items-center justify-between cursor-pointer transition-all ${
                interventionConfig.haptics
                  ? "bg-amber-950/40 border-amber-500 text-amber-200"
                  : "bg-slate-900 border-slate-800 text-slate-500"
              }`}
            >
              <span className="flex items-center gap-1.5"><Vibrate className="w-3.5 h-3.5" /> Haptics</span>
              <span>{interventionConfig.haptics ? "ON" : "OFF"}</span>
            </button>

            <button
              onClick={() => setInterventionConfig((p) => ({ ...p, audioWhisper: !p.audioWhisper }))}
              className={`p-2 rounded-xl border flex items-center justify-between cursor-pointer transition-all ${
                interventionConfig.audioWhisper
                  ? "bg-cyan-950/40 border-cyan-500 text-cyan-200"
                  : "bg-slate-900 border-slate-800 text-slate-500"
              }`}
            >
              <span className="flex items-center gap-1.5"><Volume2 className="w-3.5 h-3.5" /> Whisper</span>
              <span>{interventionConfig.audioWhisper ? "ON" : "OFF"}</span>
            </button>

            <button
              onClick={() => setInterventionConfig((p) => ({ ...p, silentFamilyPing: !p.silentFamilyPing }))}
              className={`p-2 rounded-xl border flex items-center justify-between cursor-pointer transition-all ${
                interventionConfig.silentFamilyPing
                  ? "bg-indigo-950/40 border-indigo-500 text-indigo-200"
                  : "bg-slate-900 border-slate-800 text-slate-500"
              }`}
            >
              <span className="flex items-center gap-1.5"><BellOff className="w-3.5 h-3.5" /> Family Ping</span>
              <span>{interventionConfig.silentFamilyPing ? "ON" : "OFF"}</span>
            </button>

            <button
              onClick={() => setInterventionConfig((p) => ({ ...p, headsUpBanner: !p.headsUpBanner }))}
              className={`p-2 rounded-xl border flex items-center justify-between cursor-pointer transition-all ${
                interventionConfig.headsUpBanner
                  ? "bg-rose-950/40 border-rose-500 text-rose-200"
                  : "bg-slate-900 border-slate-800 text-slate-500"
              }`}
            >
              <span className="flex items-center gap-1.5"><Eye className="w-3.5 h-3.5" /> HUD Lock</span>
              <span>{interventionConfig.headsUpBanner ? "ON" : "OFF"}</span>
            </button>
          </div>
        </div>

      </div>

      {/* RIGHT COLUMN: 3D Hologram Shield & Live Telemetry (7 cols) */}
      <div className="lg:col-span-7 flex flex-col gap-4">

        {/* Master Threat Telemetry with Integrated 3D Hologram Shield */}
        <div className={`glass-panel rounded-2xl p-5 border transition-all relative overflow-hidden ${
          telemetry.level === "CRITICAL"
            ? "border-red-500/80 bg-red-950/20 shadow-2xl shadow-red-950/50"
            : telemetry.level === "WARN"
            ? "border-amber-500/70 bg-amber-950/20"
            : "border-slate-800"
        }`}>
          <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-center">
            
            {/* 3D Hologram Particle Shield Sandbox (5 cols) */}
            <div className="md:col-span-5 h-[240px] relative rounded-xl overflow-hidden bg-slate-950/60 border border-slate-800/80">
              <CyberHologramShield3D
                riskScore={telemetry.risk_score}
                level={telemetry.level}
                isAudioActive={isPlaying}
              />
            </div>

            {/* Numeric Threat Telemetry (7 cols) */}
            <div className="md:col-span-7 flex flex-col justify-between h-full gap-3">
              <div className="flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-1.5 mb-1 font-mono text-[9px] uppercase tracking-widest text-slate-400">
                    <span>TARGET_DEMOGRAPHIC:</span>
                    <span className="bg-cyan-950 border border-cyan-500/40 text-cyan-300 px-1.5 py-0.5 rounded font-bold">
                      {telemetry.demographic_target || "GENERAL"}
                    </span>
                  </div>
                  <h3 className="text-lg font-black text-white tracking-wide">
                    {telemetry.threat_type}
                  </h3>
                </div>

                <div className="text-right font-mono">
                  <div className={`text-3xl font-black ${
                    telemetry.risk_score >= 0.8 ? "text-red-400 animate-glitch" : telemetry.risk_score >= 0.5 ? "text-amber-400" : "text-cyan-400"
                  }`}>
                    <AnimatedCounter target={telemetry.risk_score * 100} decimals={0} suffix="%" />
                  </div>
                  <div className="text-[9px] font-bold tracking-wider uppercase text-slate-400">RISK_SCORE</div>
                </div>
              </div>

              {/* 5-Stage Intent Kill-Chain Progress */}
              <div>
                <div className="flex justify-between text-[10px] text-slate-400 mb-1 font-mono">
                  <span>KILL_CHAIN_PROGRESS</span>
                  <span className="font-bold text-white">STAGE {telemetry.kill_chain_stage || telemetry.stage || 1} / 5</span>
                </div>
                <div className="grid grid-cols-5 gap-1.5 h-2">
                  {[1, 2, 3, 4, 5].map((s) => (
                    <div
                      key={s}
                      className={`rounded-sm transition-all duration-300 ${
                        (telemetry.kill_chain_stage || telemetry.stage || 1) >= s
                          ? s >= 4 ? "bg-red-500 shadow-sm shadow-red-500" : s >= 2 ? "bg-amber-500" : "bg-cyan-500"
                          : "bg-slate-800"
                      }`}
                    />
                  ))}
                </div>
                <div className="flex justify-between text-[8px] font-mono text-slate-500 mt-1">
                  <span>1.HOOK</span>
                  <span>2.PANIC</span>
                  <span>3.ISOLATE</span>
                  <span>4.PAYMENT</span>
                  <span>5.LOCK</span>
                </div>
              </div>

              {/* Mathematical Conjunction Micro-Readout */}
              <div className="p-2.5 bg-slate-900/80 border border-slate-800 rounded-xl text-[10px] font-mono">
                <span className="text-cyan-400 font-bold block mb-0.5">INTENT_CONJUNCTION_VERDICT:</span>
                <p className="text-slate-300 truncate">
                  {telemetry.conjunction_explanation || "[STANDBY]: Awaiting live conversational stream..."}
                </p>
              </div>

              {/* Flag Micro-Pills */}
              <div className="flex flex-wrap gap-1">
                {telemetry.flags.map((f) => (
                  <span key={f} className="text-[9px] font-mono px-1.5 py-0.5 bg-red-950/60 border border-red-500/40 text-red-300 rounded">
                    🚩 {f}
                  </span>
                ))}
              </div>
            </div>

          </div>

          {/* Critical Emergency Banner */}
          {telemetry.level === "CRITICAL" && interventionConfig.headsUpBanner && (
            <div className="p-2.5 bg-red-900/50 border border-red-500/80 rounded-xl flex items-center justify-between mt-3 text-xs">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-red-300 animate-bounce" />
                <span className="font-mono text-[11px] font-bold text-white uppercase tracking-wider">
                  LOCKDOWN: Active Coercion Siphon Detected · NetBanking Locked
                </span>
              </div>
              <span className="text-[9px] font-mono bg-red-950 px-2 py-0.5 rounded text-red-300">
                1930 DISPATCH READY
              </span>
            </div>
          )}
        </div>

        {/* Live Audio Frequency Spectrum / 3D Ribbon */}
        <div className="flex flex-col gap-1.5">
          <div className="flex items-center justify-between text-[9px] font-mono text-slate-400 px-1">
            <span className="flex items-center gap-1.5 text-cyan-400 font-bold">
              <Waves className="w-3.5 h-3.5 animate-pulse" />
              {audioVisualizerMode === "3d" ? "3D AUDIO SPECTRAL RIBBON" : "2D FFT FREQUENCY SPECTRUM"}
            </span>
            <div className="flex items-center gap-1">
              <button
                onClick={() => setAudioVisualizerMode("2d")}
                className={`px-2 py-0.5 rounded border transition-all cursor-pointer ${
                  audioVisualizerMode === "2d"
                    ? "bg-cyan-950 border-cyan-500 text-cyan-200 font-bold"
                    : "bg-slate-900 border-slate-800 text-slate-400 hover:text-white"
                }`}
              >
                2D FFT
              </button>
              <button
                onClick={() => setAudioVisualizerMode("3d")}
                className={`px-2 py-0.5 rounded border transition-all cursor-pointer ${
                  audioVisualizerMode === "3d"
                    ? "bg-indigo-950 border-indigo-500 text-indigo-200 font-bold"
                    : "bg-slate-900 border-slate-800 text-slate-400 hover:text-white"
                }`}
              >
                3D RIBBON
              </button>
            </div>
          </div>

          {audioVisualizerMode === "3d" ? (
            <AudioFrequencyRibbon3D
              isActive={isPlaying}
              amplitude={isPlaying ? 1.4 : 0.4}
              threatLevel={telemetry.level}
            />
          ) : (
            <AudioWaveformSpectrum2D
              isPlaying={isPlaying}
              threatLevel={telemetry.level}
            />
          )}
        </div>

        {/* Rolling Speech Transcript Feed */}
        <div className="glass-panel rounded-2xl p-4 border border-slate-800 flex flex-col flex-1 min-h-[170px]">
          <div className="flex items-center justify-between mb-2 pb-1.5 border-b border-slate-800 text-[10px] font-mono">
            <span className="text-slate-400 font-bold uppercase flex items-center gap-1.5">
              <Mic className="w-3.5 h-3.5 text-rose-400 animate-pulse" /> Live Intercept Stream
            </span>
            <span className="text-slate-500">SHERPA_ONNX_168MS</span>
          </div>

          <div className="flex-1 overflow-y-auto space-y-1.5 max-h-[140px] pr-1 font-mono text-[11px]">
            {transcript.length === 0 ? (
              <div className="text-slate-600 text-center py-6 italic text-xs">
                Awaiting conversational packets... Click &ldquo;START IN-CALL STREAM&rdquo; above.
              </div>
            ) : (
              transcript.map((t, idx) => (
                <div
                  key={idx}
                  className={`p-2 rounded-lg text-xs ${
                    t.role === "decoy"
                      ? "bg-amber-950/40 border border-amber-500/40 text-amber-200"
                      : "bg-slate-900/80 border border-slate-800 text-slate-200"
                  }`}
                >
                  <p>{t.text}</p>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Tactical Counter-Measures & Honeypot Controls */}
        <div className="glass-panel rounded-2xl p-4 border border-slate-800 font-mono">
          <div className="flex items-center justify-between mb-2.5 text-[10px]">
            <span className="font-bold text-slate-400 uppercase flex items-center gap-1.5">
              <Bot className="w-3.5 h-3.5 text-amber-400" /> Active Counter-Measures
            </span>
            {decoyActive && (
              <span className="font-bold text-amber-300 animate-pulse">
                RAMESH_AI_ENGAGED: {Math.floor(secondsWasted / 60)}m {secondsWasted % 60}s WASTED
              </span>
            )}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 text-xs">
            <button
              onClick={handleTransferToDecoy}
              className="p-2.5 bg-amber-950/40 hover:bg-amber-900/50 border border-amber-500/50 text-amber-300 rounded-xl text-xs font-bold flex items-center justify-center gap-2 cursor-pointer transition-all"
            >
              <Bot className="w-3.5 h-3.5" /> Transfer to Ramesh AI
            </button>

            <button
              onClick={handleTriggerSafePing}
              className="p-2.5 bg-indigo-950/40 hover:bg-indigo-900/50 border border-indigo-500/50 text-indigo-300 rounded-xl text-xs font-bold flex items-center justify-center gap-2 cursor-pointer transition-all"
            >
              <UserCheck className="w-3.5 h-3.5" /> 
              {safePingStatus === "CONFIRMED" ? "Safe-Ping Verified!" : "Dispatch Safe-Ping"}
            </button>

            <button
              onClick={handleDownloadDossier}
              className="p-2.5 bg-slate-900 hover:bg-slate-800 border border-slate-700 text-slate-200 rounded-xl text-xs font-bold flex items-center justify-center gap-2 cursor-pointer transition-all"
            >
              <Download className="w-3.5 h-3.5" /> Download 1930 FIR
            </button>
          </div>

          {harvestedIocs.upi_ids.length > 0 && (
            <div className="mt-2.5 p-2 bg-slate-900 rounded-lg border border-red-500/30 text-[10px]">
              <span className="text-red-400 font-bold block mb-0.5">HARVESTED_MULE_IOCS (1930 READY):</span>
              <div className="text-slate-300">
                UPI: <span className="text-amber-300">{harvestedIocs.upi_ids.join(", ")}</span> | A/C: <span className="text-amber-300">{harvestedIocs.bank_accounts.join(", ")}</span>
              </div>
            </div>
          )}
        </div>

      </div>

    </div>
  );
}
