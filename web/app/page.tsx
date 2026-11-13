"use client";

import React, { useState } from "react";
import CallSimulator from "@/components/simulator/CallSimulator";
import GuardianDashboard from "@/components/guardian/GuardianDashboard";
import IntentKillChainDeepDive from "@/components/intent/IntentKillChainDeepDive";
import RedditCommunityShowcase from "@/components/community/RedditCommunityShowcase";
import StringTuneScroll from "@/components/effects/StringTuneScroll";
import { 
  ShieldAlert, Radio, UserCheck, Cpu, Workflow, 
  TrendingUp, Activity, Sparkles, Layers, Sliders
} from "lucide-react";

export default function Home() {
  const [activeTab, setActiveTab] = useState<"simulator" | "killchain" | "community" | "guardian">("simulator");

  return (
    <main className="min-h-screen bg-[#070b14] text-slate-100 flex flex-col">
      {/* StringTune Smooth Scroll and Animation Injector */}
      <StringTuneScroll />

      {/* Top Cyber Defense Header */}
      <header className="border-b border-slate-800/80 bg-slate-950/80 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between gap-2">
          {/* Logo & Tag */}
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-red-600 via-rose-500 to-amber-500 flex items-center justify-center shadow-lg shadow-red-500/20">
              <ShieldAlert className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-black text-lg tracking-wider text-white">XORcall</span>
                <span className="text-[10px] bg-red-950/80 border border-red-500/50 text-red-300 px-1.5 py-0.5 rounded font-mono font-bold">
                  v2.0-HACKATHON
                </span>
              </div>
              <p className="text-[10px] text-slate-400 font-medium hidden sm:block">
                Multi-Demographic Cyber Extortion &amp; AI Deepfake Defense
              </p>
            </div>
          </div>

          {/* Mode Switcher Tabs (4 Comprehensive Views) */}
          <div className="flex items-center bg-slate-900/90 border border-slate-800 p-1 rounded-xl">
            <button
              onClick={() => setActiveTab("simulator")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                activeTab === "simulator"
                  ? "bg-gradient-to-r from-red-600 to-rose-600 text-white shadow-md shadow-red-600/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <Radio className="w-3.5 h-3.5" /> 
              <span>Simulator</span>
            </button>
            <button
              onClick={() => setActiveTab("killchain")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                activeTab === "killchain"
                  ? "bg-gradient-to-r from-indigo-600 to-blue-600 text-white shadow-md shadow-indigo-600/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <Workflow className="w-3.5 h-3.5" /> 
              <span className="hidden sm:inline">Intent Kill-Chain</span>
              <span className="sm:hidden">Engine</span>
            </button>
            <button
              onClick={() => setActiveTab("community")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                activeTab === "community"
                  ? "bg-gradient-to-r from-orange-600 to-amber-600 text-white shadow-md shadow-orange-600/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <TrendingUp className="w-3.5 h-3.5" /> 
              <span className="hidden sm:inline">Reddit Validation</span>
              <span className="sm:hidden">Demand</span>
            </button>
            <button
              onClick={() => setActiveTab("guardian")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                activeTab === "guardian"
                  ? "bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-md shadow-emerald-600/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <UserCheck className="w-3.5 h-3.5" /> 
              <span>Guardian</span>
            </button>
          </div>

          {/* Status Pills */}
          <div className="hidden lg:flex items-center gap-3 text-[11px] font-mono">
            <div className="flex items-center gap-1.5 text-emerald-400 bg-emerald-950/40 border border-emerald-500/30 px-2.5 py-1 rounded-full">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              Groq LPU Sub-200ms
            </div>
            <div className="flex items-center gap-1.5 text-indigo-400 bg-indigo-950/40 border border-indigo-500/30 px-2.5 py-1 rounded-full">
              <Cpu className="w-3.5 h-3.5" />
              Sherpa-ONNX Local
            </div>
          </div>
        </div>
      </header>

      {/* Hero Strip with Parallax Attributes for StringTune */}
      <section 
        className="border-b border-slate-900 bg-gradient-to-b from-slate-950 to-[#070b14] py-3 px-4"
        data-string="parallax"
        data-string-parallax="0.15"
        data-string-parallax-bias="0"
        {...({ string: "parallax", "string-parallax": "0.15", "string-parallax-bias": "0" } as any)}
      >
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
            <span className="text-slate-300 font-medium">
              Colossus 3.0 Round 2 Defense Engine · Upgraded with Judge Feedback
            </span>
          </div>
          <div className="flex items-center gap-4 text-slate-400 font-mono text-[11px]">
            <span className="text-slate-300">
              <strong>Demographics:</strong> Seniors · Students · Techies · Parents
            </span>
            <span className="text-slate-600">|</span>
            <span className="text-slate-300">
              <strong>Sensory Modes:</strong> Cheek Haptic · In-Ear Whisper · Silent Ping
            </span>
          </div>
        </div>
      </section>

      {/* Main Container */}
      <div className="flex-1 py-6">
        {activeTab === "simulator" && <CallSimulator />}
        {activeTab === "killchain" && <IntentKillChainDeepDive />}
        {activeTab === "community" && <RedditCommunityShowcase />}
        {activeTab === "guardian" && <GuardianDashboard />}
      </div>

      {/* Footer & Telemetry Stats */}
      <footer className="border-t border-slate-900 bg-slate-950 py-4 px-4 text-xs text-slate-500">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-4 text-[11px]">
            <span>Team: <strong className="text-slate-300">KANYARASI</strong></span>
            <span>Lead: Likhith Venkata Sai · Shruthishree Mohapatra</span>
            <span className="text-slate-600">|</span>
            <span>Colossus 3.0 Hackathon (Cyber Safety Track)</span>
          </div>
          <div className="flex items-center gap-4 text-[11px] font-mono text-slate-400">
            <span>National Extortion Loss: ₹4,057.7 Cr (2022–2026)</span>
            <span>1930 / I4C CFCFRMS Integrated</span>
          </div>
        </div>
      </footer>
    </main>
  );
}
