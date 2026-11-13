"use client";

import React, { useState } from "react";
import { 
  ShieldCheck, ShieldAlert, AlertTriangle, Fingerprint, 
  Workflow, Network, Cpu, Lock, CheckCircle2, XCircle, ArrowRight,
  Zap, Eye, Activity
} from "lucide-react";

export default function IntentKillChainDeepDive() {
  const [selectedScenario, setSelectedScenario] = useState<number>(0);

  const KILL_CHAIN_STAGES = [
    {
      stage: 1,
      name: "Persona Pretext Anchor",
      tagline: "Manufactured Trust / Authority",
      scammerPattern: "CBI Inspector, Cyber Crime DCP, Supreme Court Registrar, Cloned Kinship voice.",
      benignPattern: "Genuine bank fraud desk, Apollo ER triage, university registrar.",
      whyBenignFailsKeyword: "Naive regex matches 'Police' or 'CBI' -> 80% false positive trap.",
      xorCallResolution: "Flagged 'WATCH' (Risk 0.15–0.25). Zero user interruption.",
      statusColor: "border-cyan-500 text-cyan-300 bg-cyan-950/40"
    },
    {
      stage: 2,
      name: "Crisis & High Cognitive Load",
      tagline: "Panic Injection",
      scammerPattern: "Aadhaar narcotics seizure (140g MDMA), PMLA warrant, fatal accident custody, seat drop.",
      benignPattern: "Suspicious ₹45K charge notification, patient emergency admission.",
      whyBenignFailsKeyword: "Legitimate institutions also discuss fraud, accidents, or fees.",
      xorCallResolution: "Correlates passive device sanity (e.g. child accelerometer shows 0.0G crash).",
      statusColor: "border-amber-500 text-amber-300 bg-amber-950/40"
    },
    {
      stage: 3,
      name: "Sensory Confinement & Isolation",
      tagline: "THE SMOKING GUN",
      scammerPattern: "'Stay on video call', 'Lock room door', 'Do not tell spouse', 'Official Secrets Act gag'.",
      benignPattern: "NEVER requested. Legitimate authorities never demand 24x7 video isolation.",
      whyBenignFailsKeyword: "Behavioral imperative that blinds victim critical faculties.",
      xorCallResolution: "Threshold jump: Stage 1 + Stage 3 = instant 'WARN' (0.65+).",
      statusColor: "border-orange-500 text-orange-300 bg-orange-950/40"
    },
    {
      stage: 4,
      name: "Direct Mule Payment Siphon",
      tagline: "Extraction Siphon",
      scammerPattern: "RTGS to 'Safe RBI Escrow', immediate clinic UPI, prepaid crypto recharge.",
      benignPattern: "Official institutional ERP portal, hospital cash desk in-person, bank branch.",
      whyBenignFailsKeyword: "Scammers cannot use institutional gateways—they rely on mule accounts.",
      xorCallResolution: "CONJUNCTION: {Authority ∧ Isolation ∧ Direct_Mule} = 0.96 CRITICAL. Instant freeze.",
      statusColor: "border-red-500 text-red-300 bg-red-950/40"
    },
    {
      stage: 5,
      name: "Terminal Extortion Trance",
      tagline: "Emotional Paralysis",
      scammerPattern: "Threat of police raid van in 15 mins, FD liquidation, ongoing webcam surveillance.",
      benignPattern: "Non-existent in lawful civil interactions.",
      whyBenignFailsKeyword: "Victim screen is dark (pressed to ear); SMS alerts are ignored.",
      xorCallResolution: "Multi-Modal Escalation: Cheek Haptics SOS + Audio Whisper + Silent Family Ping.",
      statusColor: "border-rose-500 text-rose-300 bg-rose-950/40"
    }
  ];

  const FALSE_POSITIVE_COMPARISONS = [
    {
      caller: "Genuine HDFC Bank Alert",
      dialogue: "'Sir, ₹45,000 charge from Dubai. Block card? Visit NetBanking or branch.'",
      flags: ["Charge", "Blocked", "Money"],
      hasIsolationGag: false,
      hasDirectMulePayment: false,
      verdict: "BENIGN (0.12)",
      color: "border-emerald-500/40 text-emerald-400 bg-emerald-950/30"
    },
    {
      caller: "Apollo Hospital ER Desk",
      dialogue: "'Dr. Mehta here. Rahul brought to Bed 4 after bike slip. Please visit hospital.'",
      flags: ["Emergency", "Hospital", "Admitted"],
      hasIsolationGag: false,
      hasDirectMulePayment: false,
      verdict: "BENIGN MITIGATION (0.05)",
      color: "border-emerald-500/40 text-emerald-400 bg-emerald-950/30"
    },
    {
      caller: "CBI Narcotics Extortion",
      dialogue: "'MDMA parcel seized with your Aadhaar. Stay on video call in room. Transfer ₹4L to safe RBI account.'",
      flags: ["CBI", "Drugs", "Gag", "RTGS"],
      hasIsolationGag: true,
      hasDirectMulePayment: true,
      verdict: "CRITICAL SCAM (0.96)",
      color: "border-red-500/60 text-red-300 bg-red-950/40"
    },
    {
      caller: "Campus Fee Phishing (BITS)",
      dialogue: "'Registrar office. Admission fee failed. Seat cancellation in 30 mins. Transfer ₹1.2L to registrar personal UPI.'",
      flags: ["Admission", "Hold", "Direct UPI"],
      hasIsolationGag: true,
      hasDirectMulePayment: true,
      verdict: "CRITICAL SCAM (0.93)",
      color: "border-red-500/60 text-red-300 bg-red-950/40"
    }
  ];

  return (
    <div id="killchain" className="w-full max-w-7xl mx-auto p-4 flex flex-col gap-5 select-none font-mono">
      
      {/* Title Bar */}
      <div className="glass-panel rounded-2xl p-4 border border-slate-800 shadow-xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-indigo-900/60 border border-indigo-500/50 flex items-center justify-center text-indigo-300">
            <Workflow className="w-5 h-5 animate-pulse" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-black text-white tracking-widest uppercase">
                Multi-Modal Intent Kill-Chain
              </h2>
              <span className="text-[9px] bg-indigo-950 border border-indigo-500/50 text-indigo-300 px-2 py-0.5 rounded font-bold">
                NON-LINEAR CONJUNCTION
              </span>
            </div>
            <p className="text-[10px] text-slate-400">
              Mathematically disproves false positives by enforcing 4-stage behavioral conjunction.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 text-[10px] bg-slate-900 px-3 py-1.5 rounded-xl border border-slate-800 text-slate-300">
          <Fingerprint className="w-4 h-4 text-emerald-400" />
          <span>ZERO-TRUST INTENT GRAPH</span>
        </div>
      </div>

      {/* 5-Stage Interactive Kinetic Node Deck */}
      <div className="grid grid-cols-1 sm:grid-cols-5 gap-2.5">
        {KILL_CHAIN_STAGES.map((s, idx) => (
          <button
            key={s.stage}
            onClick={() => setSelectedScenario(idx)}
            className={`text-left p-3 rounded-xl border transition-all cursor-pointer flex flex-col justify-between ${
              selectedScenario === idx
                ? "bg-indigo-950/70 border-indigo-500 shadow-lg shadow-indigo-950/50 ring-1 ring-indigo-400/40"
                : "bg-slate-900/60 border-slate-800 hover:border-slate-700 text-slate-400"
            }`}
          >
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <span className={`text-[9px] px-1.5 py-0.5 rounded font-bold border ${s.statusColor}`}>
                  STAGE {s.stage}
                </span>
                {s.stage === 3 && (
                  <span className="text-[8px] font-bold text-amber-400 animate-pulse">
                    KEY VECTOR
                  </span>
                )}
              </div>
              <div className="font-bold text-xs text-white truncate">{s.name}</div>
              <div className="text-[10px] text-slate-400 truncate">{s.tagline}</div>
            </div>

            <div className="mt-2 text-[9px] text-cyan-400 flex items-center gap-1">
              INSPECT <ArrowRight className="w-2.5 h-2.5" />
            </div>
          </button>
        ))}
      </div>

      {/* Selected Stage Detail Terminal */}
      <div className="glass-panel-glow rounded-2xl p-5 border border-indigo-500/40 grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
        <div className="lg:col-span-4 flex flex-col gap-2">
          <div className="text-[10px] text-cyan-400 font-bold uppercase tracking-wider flex items-center gap-1.5">
            <Activity className="w-3.5 h-3.5" />
            STAGE_{KILL_CHAIN_STAGES[selectedScenario].stage}_SIGNATURE
          </div>
          <h3 className="text-base font-black text-white">
            {KILL_CHAIN_STAGES[selectedScenario].name}
          </h3>
          <p className="text-xs text-indigo-200">
            {KILL_CHAIN_STAGES[selectedScenario].tagline}
          </p>

          <div className="mt-2 p-3 rounded-xl bg-slate-900/90 border border-slate-800 text-[11px]">
            <span className="text-amber-400 font-bold block mb-1">REGEX PITFALL:</span>
            <p className="text-slate-400">
              {KILL_CHAIN_STAGES[selectedScenario].whyBenignFailsKeyword}
            </p>
          </div>
        </div>

        <div className="lg:col-span-8 grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
          <div className="p-3.5 rounded-xl bg-red-950/20 border border-red-500/30">
            <div className="flex items-center gap-2 font-bold text-red-300 mb-1.5 text-[11px]">
              <AlertTriangle className="w-3.5 h-3.5 text-red-400" /> EXPLOITATION SIGNATURE
            </div>
            <p className="text-slate-300 text-[11px] leading-relaxed">
              {KILL_CHAIN_STAGES[selectedScenario].scammerPattern}
            </p>
          </div>

          <div className="p-3.5 rounded-xl bg-emerald-950/20 border border-emerald-500/30">
            <div className="flex items-center gap-2 font-bold text-emerald-300 mb-1.5 text-[11px]">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" /> LAWFUL BENIGN CONTRAST
            </div>
            <p className="text-slate-300 text-[11px] leading-relaxed">
              {KILL_CHAIN_STAGES[selectedScenario].benignPattern}
            </p>
          </div>

          <div className="sm:col-span-2 p-3 rounded-xl bg-indigo-950/40 border border-indigo-500/50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 text-[11px]">
            <span className="text-slate-200">
              <strong className="text-cyan-300">XORCALL ENGINE RESOLUTION:</strong> {KILL_CHAIN_STAGES[selectedScenario].xorCallResolution}
            </span>
            <span className="text-[9px] bg-indigo-900 px-2 py-0.5 rounded text-indigo-300 border border-indigo-500/40 shrink-0">
              VERIFIED
            </span>
          </div>
        </div>
      </div>

      {/* Discrimination Matrix */}
      <div className="glass-panel rounded-2xl p-4 border border-slate-800">
        <div className="flex items-center justify-between mb-3 text-xs">
          <span className="font-bold text-white uppercase flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 text-amber-400" /> Behavioral Discrimination Matrix
          </span>
          <span className="text-[10px] text-slate-500">BENIGN VS SYNDICATE</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
          {FALSE_POSITIVE_COMPARISONS.map((item, idx) => (
            <div key={idx} className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 flex flex-col justify-between gap-2">
              <div>
                <div className="flex items-center justify-between mb-1">
                  <span className="font-bold text-white text-xs">{item.caller}</span>
                  <span className={`text-[9px] font-bold px-2 py-0.5 rounded border ${item.color}`}>
                    {item.verdict}
                  </span>
                </div>
                <p className="text-[11px] text-slate-300 italic mb-2">
                  {item.dialogue}
                </p>
              </div>

              <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-[10px] text-slate-400">
                <span className="flex items-center gap-1">
                  {item.hasIsolationGag ? <CheckCircle2 className="w-3.5 h-3.5 text-red-400" /> : <XCircle className="w-3.5 h-3.5 text-slate-600" />}
                  Isolation Gag
                </span>
                <span className="flex items-center gap-1">
                  {item.hasDirectMulePayment ? <CheckCircle2 className="w-3.5 h-3.5 text-red-400" /> : <XCircle className="w-3.5 h-3.5 text-slate-600" />}
                  Direct Mule Siphon
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

    </div>
  );
}
