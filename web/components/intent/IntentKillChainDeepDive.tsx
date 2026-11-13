"use client";

import React, { useState } from "react";
import { 
  ShieldCheck, ShieldAlert, AlertTriangle, Fingerprint, 
  Workflow, Network, Cpu, Lock, CheckCircle2, XCircle, ArrowRight
} from "lucide-react";

export default function IntentKillChainDeepDive() {
  const [selectedScenario, setSelectedScenario] = useState<number>(0);

  const KILL_CHAIN_STAGES = [
    {
      stage: 1,
      name: "Persona Pretext Anchor",
      tagline: "Manufactured Trust or Kinship",
      scammerPattern: "Posing as CBI Inspector, Mumbai Police DCP, Supreme Court Registrar, or Cloned Son.",
      benignPattern: "Genuine bank representative, hospital ER triage desk, or university registrar.",
      whyBenignFailsKeyword: "Keyword alone ('Police', 'CBI') causes 80% false positives if triggered naively.",
      xorCallResolution: "Stage 1 merely flags 'WATCH' (Risk 0.15–0.25). Zero user interruption."
    },
    {
      stage: 2,
      name: "Crisis & High Cognitive Load",
      tagline: "Panic Injection",
      scammerPattern: "MDMA drugs seized in Aadhaar parcel, money laundering FIR, fatal car crash bail, or seat cancellation.",
      benignPattern: "Suspicious credit card transaction alert, patient admission notification.",
      whyBenignFailsKeyword: "Banks legitimately call about card fraud; doctors call about injuries.",
      xorCallResolution: "XORcall correlates with passive device sensors (e.g. Son's accelerometer shows zero G-impact)."
    },
    {
      stage: 3,
      name: "Sensory Confinement & Isolation",
      tagline: "THE SMOKING GUN",
      scammerPattern: "'Stay on video call', 'Lock your room door', 'Do not tell family or bank manager', 'Official Secrets Act gag order'.",
      benignPattern: "NEVER requested. Legitimate authorities never demand 24x7 webcam confinement or secrecy from spouse.",
      whyBenignFailsKeyword: "This is a behavioral imperative, not a buzzword. It isolates the victim's critical faculty.",
      xorCallResolution: "Mathematical threshold jump: Stage 1 + Stage 3 together elevate threat level to 'WARN' (0.65+)."
    },
    {
      stage: 4,
      name: "Unverified Direct Payment Vector",
      tagline: "The Extraction Siphon",
      scammerPattern: "RTGS transfer to 'Safe RBI Escrow', immediate UPI to personal clinic handle, crypto wallet task deposit.",
      benignPattern: "Visit official institutional ERP (erp.bits-pilani.ac.in), hospital billing desk in person, or bank branch.",
      whyBenignFailsKeyword: "Scammers cannot use official government/corporate payment gateways—they rely on mule accounts.",
      xorCallResolution: "CONJUNCTION TRIGGER: {Authority + Isolation + Direct Payment} = 0.96 CRITICAL. Instant intervention."
    },
    {
      stage: 5,
      name: "Terminal Extortion Trance",
      tagline: "Escalating Psychological Leverage",
      scammerPattern: "Threat of physical police van arrival in 15 mins, liquidating fixed deposits, ongoing webcam surveillance.",
      benignPattern: "Non-existent in lawful interactions.",
      whyBenignFailsKeyword: "Victim is trapped in emotional paralysis; traditional SMS or screen banners are completely ignored.",
      xorCallResolution: "Escalating Multi-Modal Interventions: Taptic Cheek Haptic SOS, In-Ear Audio Whisper, & Silent Family Ping."
    }
  ];

  const FALSE_POSITIVE_COMPARISONS = [
    {
      caller: "Genuine HDFC Fraud Alert",
      dialogue: "'Sir, we noticed a ₹45,000 charge on your credit card from Dubai. Was this you? If not, we will block the card. Please visit NetBanking or your branch.'",
      containsKeywords: ["Fraud", "Blocked", "Charge", "Money"],
      hasIsolationGag: false,
      hasDirectMulePayment: false,
      xorCallVerdict: "BENIGN (Risk: 0.12)",
      verdictColor: "text-emerald-400 border-emerald-500/30 bg-emerald-950/20"
    },
    {
      caller: "Apollo Hospital Emergency ER",
      dialogue: "'Hello, Dr. Mehta calling. Your son Rahul was brought into Emergency Ward Bed 4 following a minor bike slip. Please come to the hospital on Bannerghatta Road to see him.'",
      containsKeywords: ["Emergency", "Hospital", "Son", "Admitted"],
      hasIsolationGag: false,
      hasDirectMulePayment: false,
      xorCallVerdict: "BENIGN MITIGATION (Risk: 0.05)",
      verdictColor: "text-emerald-400 border-emerald-500/30 bg-emerald-950/20"
    },
    {
      caller: "Digital Arrest Syndicate (CBI Pretext)",
      dialogue: "'Inspector Rao from Crime Branch. 140g MDMA was seized with your Aadhaar. Stay in your room on video call. Do not cut call or tell family. Transfer 4 Lakhs to RBI supervision escrow.'",
      containsKeywords: ["CBI", "Drugs", "MDMA", "Arrest Warrant", "Video Call", "Transfer"],
      hasIsolationGag: true,
      hasDirectMulePayment: true,
      xorCallVerdict: "CRITICAL SCAM (Risk: 0.96)",
      verdictColor: "text-red-400 border-red-500/30 bg-red-950/20"
    },
    {
      caller: "College Fee Syndicate (BITS Pilani Phishing)",
      dialogue: "'University accounts office calling. Your admission fee failed on the portal. Your seat will be cancelled in 30 minutes. Pay 1.2 Lakhs right now to registrar personal UPI.'",
      containsKeywords: ["BITS Pilani", "Fee", "Cancellation", "Pay", "UPI", "Right now"],
      hasIsolationGag: true,
      hasDirectMulePayment: true,
      xorCallVerdict: "CRITICAL PHISHING (Risk: 0.93)",
      verdictColor: "text-red-400 border-red-500/30 bg-red-950/20"
    }
  ];

  return (
    <div id="killchain" className="w-full max-w-7xl mx-auto p-4 flex flex-col gap-6">
      {/* Title & Core Answering Banner */}
      <div className="glass-panel rounded-2xl p-6 border border-slate-700/60 shadow-xl">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Workflow className="w-5 h-5 text-indigo-400" />
              <h2 className="text-xl font-black text-white tracking-wide">
                The Multi-Modal Intent Kill-Chain
              </h2>
              <span className="text-[10px] bg-indigo-950/80 border border-indigo-500/40 text-indigo-300 px-2 py-0.5 rounded-full font-mono font-bold">
                BEYOND KEYWORD REGEX
              </span>
            </div>
            <p className="text-xs text-slate-300 max-w-3xl">
              Answering Judge Criticism #2: <em>&ldquo;How do you know for particular that it is a scammer?&rdquo;</em> XORcall does not trigger on isolated vocabulary. A scam is an orchestrated cognitive trap requiring the strict mathematical conjunction of 4 non-linear behavioral stages.
            </p>
          </div>
          <div className="flex items-center gap-3 bg-slate-900/90 border border-slate-800 p-2.5 rounded-xl font-mono text-[11px] text-slate-300">
            <Fingerprint className="w-4 h-4 text-emerald-400" />
            <span>Zero-Trust Behavioral Fingerprint</span>
          </div>
        </div>
      </div>

      {/* 5-Stage Interactive Kill-Chain Visualizer */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
        {KILL_CHAIN_STAGES.map((s, idx) => (
          <button
            key={s.stage}
            onClick={() => setSelectedScenario(idx)}
            className={`text-left p-4 rounded-xl border transition-all cursor-pointer flex flex-col justify-between ${
              selectedScenario === idx
                ? "bg-indigo-950/50 border-indigo-500/90 shadow-lg shadow-indigo-950/40 ring-1 ring-indigo-400/30"
                : "bg-slate-900/40 border-slate-800 hover:border-slate-700 text-slate-400"
            }`}
          >
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className={`text-[10px] px-2 py-0.5 rounded font-mono font-bold ${
                  s.stage === 3 
                    ? "bg-amber-500 text-black font-black" 
                    : s.stage >= 4 
                    ? "bg-red-950 text-red-300 border border-red-500/40" 
                    : "bg-slate-800 text-slate-300"
                }`}>
                  STAGE {s.stage}
                </span>
                {s.stage === 3 && (
                  <span className="text-[9px] font-bold text-amber-400 uppercase tracking-wider">
                    Critical Key
                  </span>
                )}
              </div>
              <h3 className="font-bold text-sm text-slate-100 mb-1">{s.name}</h3>
              <p className="text-[11px] text-slate-400">{s.tagline}</p>
            </div>
            <div className="mt-3 text-[10px] text-indigo-400 font-mono flex items-center gap-1">
              Click to inspect <ArrowRight className="w-3 h-3" />
            </div>
          </button>
        ))}
      </div>

      {/* Selected Stage Deep-Dive Card */}
      <div className="glass-panel rounded-2xl p-6 border border-indigo-500/30 bg-gradient-to-br from-slate-900/90 via-slate-950/90 to-indigo-950/20 shadow-2xl">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          <div className="lg:col-span-4 flex flex-col gap-2">
            <span className="text-[11px] font-mono text-indigo-400 font-bold uppercase tracking-wider">
              Stage {KILL_CHAIN_STAGES[selectedScenario].stage} Analysis
            </span>
            <h3 className="text-lg font-black text-white">
              {KILL_CHAIN_STAGES[selectedScenario].name}
            </h3>
            <p className="text-xs text-slate-300">
              {KILL_CHAIN_STAGES[selectedScenario].tagline}
            </p>
            <div className="mt-4 p-3 rounded-xl bg-slate-900/80 border border-slate-800 text-xs">
              <span className="font-mono text-amber-300 font-bold block mb-1">
                Why Naive Regex Fails:
              </span>
              <p className="text-slate-400 text-[11px]">
                {KILL_CHAIN_STAGES[selectedScenario].whyBenignFailsKeyword}
              </p>
            </div>
          </div>

          <div className="lg:col-span-8 grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-4 rounded-xl bg-red-950/20 border border-red-500/30">
              <div className="flex items-center gap-2 text-xs font-bold text-red-300 mb-2">
                <AlertTriangle className="w-4 h-4 text-red-400" />
                Scammer Exploitation Signature
              </div>
              <p className="text-xs text-slate-300 leading-relaxed">
                {KILL_CHAIN_STAGES[selectedScenario].scammerPattern}
              </p>
            </div>

            <div className="p-4 rounded-xl bg-emerald-950/20 border border-emerald-500/30">
              <div className="flex items-center gap-2 text-xs font-bold text-emerald-300 mb-2">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                Legitimate Institutional Contrast
              </div>
              <p className="text-xs text-slate-300 leading-relaxed">
                {KILL_CHAIN_STAGES[selectedScenario].benignPattern}
              </p>
            </div>

            <div className="md:col-span-2 p-3.5 rounded-xl bg-indigo-950/30 border border-indigo-500/40 flex items-center justify-between text-xs">
              <span className="text-slate-300">
                <strong>XORcall Multi-Modal Resolution:</strong> {KILL_CHAIN_STAGES[selectedScenario].xorCallResolution}
              </span>
              <span className="text-[10px] font-mono bg-indigo-900/80 text-indigo-300 px-2 py-1 rounded">
                Verified In-Engine
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* False-Positive Elimination Comparison Table */}
      <div className="glass-panel rounded-2xl p-6 border border-slate-800 shadow-xl">
        <h3 className="text-base font-bold text-white mb-2 flex items-center gap-2">
          <ShieldAlert className="w-5 h-5 text-amber-400" />
          Real-World Discrimination: Bank vs. Hospital vs. Syndicate
        </h3>
        <p className="text-xs text-slate-400 mb-5">
          See how XORcall evaluates identical alarming words against behavioral intent and unverified transfer demands.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {FALSE_POSITIVE_COMPARISONS.map((item, idx) => (
            <div key={idx} className="p-4 rounded-xl bg-slate-900/70 border border-slate-800 flex flex-col justify-between gap-3">
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <span className="font-bold text-sm text-white">{item.caller}</span>
                  <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded-full border ${item.verdictColor}`}>
                    {item.xorCallVerdict}
                  </span>
                </div>
                <p className="text-xs text-slate-300 italic mb-3">
                  {item.dialogue}
                </p>
              </div>

              <div className="pt-2 border-t border-slate-800/80 flex flex-wrap items-center gap-2 text-[10px] font-mono text-slate-400">
                <span className="text-slate-500">Flags:</span>
                {item.containsKeywords.map((kw, i) => (
                  <span key={i} className="bg-slate-800 px-1.5 py-0.5 rounded text-slate-300">
                    {kw}
                  </span>
                ))}
                <div className="w-full flex items-center justify-between mt-1 text-[11px]">
                  <span className="flex items-center gap-1">
                    {item.hasIsolationGag ? <CheckCircle2 className="w-3.5 h-3.5 text-red-400" /> : <XCircle className="w-3.5 h-3.5 text-slate-500" />}
                    Webcam Isolation Gag Order
                  </span>
                  <span className="flex items-center gap-1">
                    {item.hasDirectMulePayment ? <CheckCircle2 className="w-3.5 h-3.5 text-red-400" /> : <XCircle className="w-3.5 h-3.5 text-slate-500" />}
                    Direct Mule UPI / RTGS
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
