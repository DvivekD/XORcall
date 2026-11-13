"use client";

import React, { useState } from "react";
import { 
  ShieldCheck, ShieldAlert, AlertTriangle, Fingerprint, 
  Workflow, Network, Cpu, Lock, CheckCircle2, XCircle, ArrowRight,
  Zap, Eye, Activity
} from "lucide-react";
import KillChainCanvas2D from "@/components/motion/KillChainCanvas2D";

export default function IntentKillChainDeepDive() {
  const [selectedStage, setSelectedStage] = useState<number>(3); // Default on Key Smoking Gun (Stage 3)

  const KILL_CHAIN_STAGES = [
    {
      stage: 1,
      name: "Pretext Anchor",
      tagline: "Manufactured Trust",
      scammerPattern: "CBI DCP, Supreme Court, Cloned Kinship voice.",
      benignPattern: "Bank fraud desk, Apollo ER triage, registrar.",
      whyBenignFailsKeyword: "Naive regex matches 'Police' -> 80% false positives.",
      xorCallResolution: "Flagged 'WATCH' (0.15–0.25). Zero user interruption.",
      statusColor: "border-cyan-500 text-cyan-300 bg-cyan-950/40"
    },
    {
      stage: 2,
      name: "Crisis Injection",
      tagline: "Cognitive Paralysis",
      scammerPattern: "MDMA parcel seized, PMLA warrant, car custody.",
      benignPattern: "Suspicious ₹45K charge, bike slip admission.",
      whyBenignFailsKeyword: "Banks also discuss fraud and hospital emergencies.",
      xorCallResolution: "Correlates passive device sanity (0.0G crash).",
      statusColor: "border-amber-500 text-amber-300 bg-amber-950/40"
    },
    {
      stage: 3,
      name: "Sensory Confinement",
      tagline: "THE SMOKING GUN",
      scammerPattern: "'Stay on video', 'Lock room', 'Do not tell spouse'.",
      benignPattern: "NEVER requested by legitimate authorities.",
      whyBenignFailsKeyword: "Gag orders blind victim critical faculties.",
      xorCallResolution: "Stage 1 + Stage 3 = instant 'WARN' (0.65+).",
      statusColor: "border-orange-500 text-orange-300 bg-orange-950/40"
    },
    {
      stage: 4,
      name: "Direct Mule Siphon",
      tagline: "Extraction Siphon",
      scammerPattern: "RTGS to 'Safe RBI Escrow', clinic UPI, crypto.",
      benignPattern: "ERP portal, hospital cash desk in-person, branch.",
      whyBenignFailsKeyword: "Scammers rely on private mule accounts.",
      xorCallResolution: "{Authority ∧ Isolation ∧ Direct_Mule} = 0.96 CRITICAL.",
      statusColor: "border-red-500 text-red-300 bg-red-950/40"
    },
    {
      stage: 5,
      name: "Extortion Trance",
      tagline: "Terminal Coercion",
      scammerPattern: "Raid van threat in 15 mins, webcam surveillance.",
      benignPattern: "Non-existent in lawful interactions.",
      whyBenignFailsKeyword: "Victim screen is dark; SMS alerts are ignored.",
      xorCallResolution: "Cheek Haptics + In-Ear Whisper + Family Ping.",
      statusColor: "border-rose-500 text-rose-300 bg-rose-950/40"
    }
  ];

  const currentStageData = KILL_CHAIN_STAGES.find((s) => s.stage === selectedStage) || KILL_CHAIN_STAGES[2];

  const FALSE_POSITIVE_COMPARISONS = [
    {
      caller: "Genuine HDFC Bank Alert",
      dialogue: "'₹45K charge from Dubai. Block card? Visit NetBanking.'",
      hasIsolationGag: false,
      hasDirectMulePayment: false,
      verdict: "BENIGN (0.12)",
      color: "border-emerald-500/40 text-emerald-400 bg-emerald-950/30"
    },
    {
      caller: "Apollo Hospital ER Desk",
      dialogue: "'Rahul brought to Bed 4 after bike slip. Please visit ER.'",
      hasIsolationGag: false,
      hasDirectMulePayment: false,
      verdict: "BENIGN MITIGATION (0.05)",
      color: "border-emerald-500/40 text-emerald-400 bg-emerald-950/30"
    },
    {
      caller: "CBI Narcotics Extortion",
      dialogue: "'MDMA parcel seized. Stay on video call. RTGS ₹4L to safe RBI.'",
      hasIsolationGag: true,
      hasDirectMulePayment: true,
      verdict: "CRITICAL SCAM (0.96)",
      color: "border-red-500/60 text-red-300 bg-red-950/40"
    },
    {
      caller: "Campus Fee Phishing (BITS)",
      dialogue: "'Admission hold. Pay ₹1.2L to registrar personal UPI in 30m.'",
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
              Eliminates false positives via 4-stage behavioral conjunction logic.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 text-[10px] bg-slate-900 px-3 py-1.5 rounded-xl border border-slate-800 text-slate-300">
          <Fingerprint className="w-4 h-4 text-emerald-400" />
          <span>ZERO-TRUST INTENT GRAPH</span>
        </div>
      </div>

      {/* Kinetic Kill-Chain State Machine Canvas */}
      <KillChainCanvas2D
        activeStage={selectedStage}
        onSelectStage={(stg) => setSelectedStage(stg)}
      />

      {/* Selected Stage Detail Terminal */}
      <div className="glass-panel-glow rounded-2xl p-4 border border-indigo-500/40 grid grid-cols-1 lg:grid-cols-12 gap-4 items-start">
        <div className="lg:col-span-4 flex flex-col gap-1.5">
          <div className="text-[10px] text-cyan-400 font-bold uppercase tracking-wider flex items-center gap-1.5">
            <Activity className="w-3.5 h-3.5" />
            STAGE_{currentStageData.stage}_TELEMETRY
          </div>
          <h3 className="text-sm font-black text-white">
            {currentStageData.name}
          </h3>
          <p className="text-xs text-indigo-200">
            {currentStageData.tagline}
          </p>

          <div className="mt-1 p-2 rounded-xl bg-slate-900/90 border border-slate-800 text-[10px]">
            <span className="text-amber-400 font-bold block mb-0.5">REGEX TRAP:</span>
            <p className="text-slate-400 leading-tight">
              {currentStageData.whyBenignFailsKeyword}
            </p>
          </div>
        </div>

        <div className="lg:col-span-8 grid grid-cols-1 sm:grid-cols-2 gap-2.5 text-xs">
          <div className="p-3 rounded-xl bg-red-950/20 border border-red-500/30">
            <div className="flex items-center gap-1.5 font-bold text-red-300 mb-1 text-[10px]">
              <AlertTriangle className="w-3.5 h-3.5 text-red-400" /> SYNDICATE SIGNATURE
            </div>
            <p className="text-slate-300 text-[11px] leading-tight">
              {currentStageData.scammerPattern}
            </p>
          </div>

          <div className="p-3 rounded-xl bg-emerald-950/20 border border-emerald-500/30">
            <div className="flex items-center gap-1.5 font-bold text-emerald-300 mb-1 text-[10px]">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" /> LAWFUL BENIGN CONTRAST
            </div>
            <p className="text-slate-300 text-[11px] leading-tight">
              {currentStageData.benignPattern}
            </p>
          </div>

          <div className="sm:col-span-2 p-2.5 rounded-xl bg-indigo-950/40 border border-indigo-500/50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 text-[10px]">
            <span className="text-slate-200">
              <strong className="text-cyan-300">XORCALL VERDICT:</strong> {currentStageData.xorCallResolution}
            </span>
            <span className="text-[9px] bg-indigo-900 px-2 py-0.5 rounded text-indigo-300 border border-indigo-500/40 shrink-0 font-bold">
              VERIFIED
            </span>
          </div>
        </div>
      </div>

      {/* Discrimination Matrix */}
      <div className="glass-panel rounded-2xl p-4 border border-slate-800">
        <div className="flex items-center justify-between mb-2.5 text-xs">
          <span className="font-bold text-white uppercase flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 text-amber-400" /> Behavioral Discrimination Matrix
          </span>
          <span className="text-[10px] text-slate-500">BENIGN VS SYNDICATE</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5 text-xs">
          {FALSE_POSITIVE_COMPARISONS.map((item, idx) => (
            <div key={idx} className="p-3 rounded-xl bg-slate-900/80 border border-slate-800 flex flex-col justify-between gap-2">
              <div>
                <div className="flex items-center justify-between mb-1">
                  <span className="font-bold text-white text-xs">{item.caller}</span>
                  <span className={`text-[9px] font-bold px-2 py-0.5 rounded border ${item.color}`}>
                    {item.verdict}
                  </span>
                </div>
                <p className="text-[11px] text-slate-300 italic">
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
