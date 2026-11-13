"use client";

import React, { useState } from "react";
import CallSimulator from "@/components/simulator/CallSimulator";
import GuardianDashboard from "@/components/guardian/GuardianDashboard";
import IntentKillChainDeepDive from "@/components/intent/IntentKillChainDeepDive";
import RedditCommunityShowcase from "@/components/community/RedditCommunityShowcase";
import CyberWarfareGlobe3D from "@/components/motion/CyberWarfareGlobe3D";
import AudioFrequencyRibbon3D from "@/components/motion/AudioFrequencyRibbon3D";
import { LiveTelemetryTicker, AnimatedCounter } from "@/components/motion/LiveTelemetryTicker";
import StringTuneScroll from "@/components/effects/StringTuneScroll";
import { 
  ShieldAlert, Radio, UserCheck, Cpu, Workflow, 
  TrendingUp, Activity, Globe, Waves, Zap, Terminal
} from "lucide-react";

export default function Home() {
  const [activeTab, setActiveTab] = useState<
    "simulator" | "warfare" | "audio3d" | "killchain" | "community" | "guardian"
  >("simulator");

  return (
    <main className="min-h-screen bg-[#060911] text-slate-100 flex flex-col cyber-grid">
      {/* StringTune Smooth Scroll and Animation Injector */}
      <StringTuneScroll />

      {/* Top Cyber Defense Command Header */}
      <header className="border-b border-slate-800/80 bg-slate-950/90 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between gap-3">
          
          {/* Logo & Operational Badge */}
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-cyan-600 via-blue-600 to-indigo-600 flex items-center justify-center shadow-lg shadow-cyan-500/20">
              <ShieldAlert className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-black text-lg tracking-wider text-white">XORcall</span>
                <span className="text-[10px] bg-cyan-950 border border-cyan-500/50 text-cyan-300 px-1.5 py-0.5 rounded font-mono font-bold">
                  v2.0-60FPS
                </span>
              </div>
              <p className="text-[10px] text-slate-400 font-mono hidden sm:block">
                Autonomous Cyber Extortion &amp; Deepfake Interceptor
              </p>
            </div>
          </div>

          {/* Tactical View Switcher (6 Views) */}
          <div className="flex items-center bg-slate-900/90 border border-slate-800 p-1 rounded-xl text-xs font-mono overflow-x-auto max-w-full">
            <button
              onClick={() => setActiveTab("simulator")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer whitespace-nowrap ${
                activeTab === "simulator"
                  ? "bg-cyan-600 text-white shadow-md shadow-cyan-600/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <Radio className="w-3.5 h-3.5" /> 
              <span>SIMULATOR</span>
            </button>

            <button
              onClick={() => setActiveTab("warfare")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer whitespace-nowrap ${
                activeTab === "warfare"
                  ? "bg-red-600 text-white shadow-md shadow-red-600/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <Globe className="w-3.5 h-3.5" /> 
              <span>3D GLOBE</span>
            </button>

            <button
              onClick={() => setActiveTab("audio3d")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer whitespace-nowrap ${
                activeTab === "audio3d"
                  ? "bg-indigo-600 text-white shadow-md shadow-indigo-600/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <Waves className="w-3.5 h-3.5" /> 
              <span>3D AUDIO</span>
            </button>

            <button
              onClick={() => setActiveTab("killchain")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer whitespace-nowrap ${
                activeTab === "killchain"
                  ? "bg-amber-600 text-white shadow-md shadow-amber-600/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <Workflow className="w-3.5 h-3.5" /> 
              <span className="hidden sm:inline">KILL-CHAIN</span>
              <span className="sm:hidden">INTEL</span>
            </button>

            <button
              onClick={() => setActiveTab("community")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer whitespace-nowrap ${
                activeTab === "community"
                  ? "bg-orange-600 text-white shadow-md shadow-orange-600/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <TrendingUp className="w-3.5 h-3.5" /> 
              <span className="hidden sm:inline">COMMUNITY</span>
              <span className="sm:hidden">PROOFS</span>
            </button>

            <button
              onClick={() => setActiveTab("guardian")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer whitespace-nowrap ${
                activeTab === "guardian"
                  ? "bg-emerald-600 text-white shadow-md shadow-emerald-600/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              <UserCheck className="w-3.5 h-3.5" /> 
              <span>GUARDIAN</span>
            </button>
          </div>

          {/* Fast Telemetry Pills */}
          <div className="hidden xl:flex items-center gap-2.5 text-[10px] font-mono">
            <div className="flex items-center gap-1.5 text-emerald-400 bg-emerald-950/40 border border-emerald-500/30 px-2 py-0.5 rounded-full">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              LPU: 184ms
            </div>
            <div className="flex items-center gap-1.5 text-cyan-400 bg-cyan-950/40 border border-cyan-500/30 px-2 py-0.5 rounded-full">
              <Cpu className="w-3 h-3" />
              SHERPA-ONNX LOCAL
            </div>
          </div>
        </div>

        {/* Live Rolling Cyber Telemetry Ticker */}
        <LiveTelemetryTicker />
      </header>

      {/* Main Dynamic Viewport */}
      <div className="flex-1 py-5 px-2 sm:px-4 max-w-7xl mx-auto w-full">
        {activeTab === "simulator" && <CallSimulator />}

        {activeTab === "warfare" && (
          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between font-mono text-xs text-slate-400 px-1">
              <span className="text-white font-bold flex items-center gap-2">
                <Globe className="w-4 h-4 text-cyan-400 animate-spin" /> Cross-Border Cyber Warfare Radar Matrix
              </span>
              <span>INTERACTIVE WEBGL 3D · DRAG TO ORBIT</span>
            </div>
            <CyberWarfareGlobe3D />
          </div>
        )}

        {activeTab === "audio3d" && (
          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between font-mono text-xs text-slate-400 px-1">
              <span className="text-white font-bold flex items-center gap-2">
                <Waves className="w-4 h-4 text-cyan-400 animate-pulse" /> Real-Time 3D Audio Frequency Ribbon
              </span>
              <span>SPECTRAL WAVE DISPLACEMENT · 60 FPS</span>
            </div>
            <AudioFrequencyRibbon3D isActive={true} amplitude={1.2} />
          </div>
        )}

        {activeTab === "killchain" && <IntentKillChainDeepDive />}
        {activeTab === "community" && <RedditCommunityShowcase />}
        {activeTab === "guardian" && <GuardianDashboard />}
      </div>

      {/* Futuristic Telemetry Footer */}
      <footer className="border-t border-slate-900 bg-slate-950 py-3.5 px-4 text-xs font-mono text-slate-500">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-2.5">
          <div className="flex items-center gap-3 text-[11px]">
            <span>TEAM: <strong className="text-slate-300">KANYARASI</strong></span>
            <span>LEAD: Likhith Venkata Sai · Shruthishree Mohapatra</span>
            <span className="text-slate-700 hidden sm:inline">|</span>
            <span className="text-cyan-400 font-bold">COLOSSUS 3.0</span>
          </div>
          <div className="flex items-center gap-3 text-[11px] text-slate-400">
            <span>NATIONAL EXTORTION SIPHON: <strong className="text-amber-400">₹4,057.7 CR</strong></span>
            <span className="text-slate-700">|</span>
            <span className="text-emerald-400">1930 CFCFRMS AUTO-DISPATCH</span>
          </div>
        </div>
      </footer>
    </main>
  );
}
