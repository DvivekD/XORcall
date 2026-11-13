"use client";

import React, { useState } from "react";
import Link from "next/link";
import CyberHologramShield3D from "@/components/motion/CyberHologramShield3D";
import CyberWarfareGlobe3D from "@/components/motion/CyberWarfareGlobe3D";
import AudioFrequencyRibbon3D from "@/components/motion/AudioFrequencyRibbon3D";
import ThreatRadar2D from "@/components/motion/ThreatRadar2D";
import AudioWaveformSpectrum2D from "@/components/motion/AudioWaveformSpectrum2D";
import ShockwavePulseRings from "@/components/motion/ShockwavePulseRings";
import { LiveTelemetryTicker, AnimatedCounter } from "@/components/motion/LiveTelemetryTicker";
import { 
  ShieldAlert, Radio, Globe, Waves, Activity, ArrowLeft, 
  Maximize2, Cpu, Eye, Sparkles, Terminal, Sliders
} from "lucide-react";

export default function SandboxPage() {
  const [activeSandbox, setActiveSandbox] = useState<"shield" | "globe" | "ribbon" | "quad">("quad");
  const [threatLevel, setThreatLevel] = useState<"SAFE" | "WATCH" | "WARN" | "CRITICAL">("WARN");
  const [riskScore, setRiskScore] = useState<number>(0.68);
  const [isAudioActive, setIsAudioActive] = useState<boolean>(true);

  return (
    <main className="min-h-screen bg-[#04060c] text-slate-100 flex flex-col font-mono select-none overflow-x-hidden">
      
      {/* Sandbox Top Control Bar */}
      <header className="border-b border-slate-800/90 bg-slate-950/90 backdrop-blur-md px-4 py-2.5 flex items-center justify-between gap-3 sticky top-0 z-50">
        <div className="flex items-center gap-3">
          <Link
            href="/"
            className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-cyan-400 bg-slate-900 px-2.5 py-1.5 rounded-lg border border-slate-800 transition-all cursor-pointer"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>EXIT_TO_MAIN</span>
          </Link>
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping" />
            <h1 className="text-xs sm:text-sm font-black text-white tracking-widest uppercase">
              ISOLATED THREE.JS 3D MOTION SANDBOX
            </h1>
            <span className="text-[9px] bg-cyan-950 border border-cyan-500/40 text-cyan-300 px-1.5 py-0.5 rounded font-bold">
              60 FPS SYNC
            </span>
          </div>
        </div>

        {/* Sandbox Viewport Switcher */}
        <div className="flex items-center bg-slate-900 border border-slate-800 p-0.5 rounded-xl text-xs">
          <button
            onClick={() => setActiveSandbox("quad")}
            className={`px-3 py-1 rounded-lg font-bold transition-all cursor-pointer ${
              activeSandbox === "quad"
                ? "bg-gradient-to-r from-cyan-600 to-blue-600 text-white shadow-md shadow-cyan-600/30"
                : "text-slate-400 hover:text-white"
            }`}
          >
            QUAD_MATRIX
          </button>
          <button
            onClick={() => setActiveSandbox("shield")}
            className={`px-3 py-1 rounded-lg font-bold transition-all cursor-pointer ${
              activeSandbox === "shield"
                ? "bg-cyan-600 text-white"
                : "text-slate-400 hover:text-white"
            }`}
          >
            3D SHIELD
          </button>
          <button
            onClick={() => setActiveSandbox("globe")}
            className={`px-3 py-1 rounded-lg font-bold transition-all cursor-pointer ${
              activeSandbox === "globe"
                ? "bg-red-600 text-white"
                : "text-slate-400 hover:text-white"
            }`}
          >
            3D GLOBE
          </button>
          <button
            onClick={() => setActiveSandbox("ribbon")}
            className={`px-3 py-1 rounded-lg font-bold transition-all cursor-pointer ${
              activeSandbox === "ribbon"
                ? "bg-indigo-600 text-white"
                : "text-slate-400 hover:text-white"
            }`}
          >
            3D RIBBON
          </button>
        </div>

        {/* Live FPS & Engine Pill */}
        <div className="hidden md:flex items-center gap-2 text-[10px]">
          <span className="bg-emerald-950/60 border border-emerald-500/30 text-emerald-400 px-2 py-0.5 rounded">
            GPU_DRAWCALL: 14 | 60.0 FPS
          </span>
          <span className="bg-slate-900 px-2 py-0.5 rounded border border-slate-800 text-slate-400">
            THREE_REVISION: r187
          </span>
        </div>
      </header>

      {/* Real-time State Modulator Dock */}
      <div className="bg-slate-950/70 border-b border-slate-900 px-4 py-2 flex flex-wrap items-center justify-between gap-3 text-xs">
        <div className="flex items-center gap-2">
          <span className="text-[10px] text-slate-500 uppercase">THREAT_LEVEL:</span>
          {(["SAFE", "WATCH", "WARN", "CRITICAL"] as const).map((lvl) => (
            <button
              key={lvl}
              onClick={() => {
                setThreatLevel(lvl);
                setRiskScore(lvl === "SAFE" ? 0.05 : lvl === "WATCH" ? 0.35 : lvl === "WARN" ? 0.68 : 0.96);
              }}
              className={`text-[9px] font-bold px-2 py-0.5 rounded border transition-all cursor-pointer ${
                threatLevel === lvl
                  ? lvl === "CRITICAL"
                    ? "bg-red-900 border-red-500 text-white animate-pulse"
                    : lvl === "WARN"
                    ? "bg-amber-900 border-amber-500 text-white"
                    : lvl === "WATCH"
                    ? "bg-emerald-900 border-emerald-500 text-white"
                    : "bg-cyan-900 border-cyan-500 text-white"
                  : "bg-slate-900 border-slate-800 text-slate-500 hover:text-slate-300"
              }`}
            >
              {lvl}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-4 text-[10px]">
          <div className="flex items-center gap-2">
            <span className="text-slate-500">RISK_SCORE:</span>
            <span className="text-cyan-400 font-bold">{(riskScore * 100).toFixed(0)}%</span>
          </div>

          <button
            onClick={() => setIsAudioActive(!isAudioActive)}
            className={`px-2 py-0.5 rounded border text-[9px] font-bold cursor-pointer transition-all ${
              isAudioActive
                ? "bg-cyan-950 border-cyan-500 text-cyan-300"
                : "bg-slate-900 border-slate-800 text-slate-500"
            }`}
          >
            AUDIO_STIM: {isAudioActive ? "ACTIVE" : "MUTED"}
          </button>
        </div>
      </div>

      {/* Main Viewport */}
      <div className="flex-1 p-3 sm:p-4 w-full max-w-[1600px] mx-auto flex flex-col gap-4">
        
        {/* QUAD MATRIX MODE */}
        {activeSandbox === "quad" && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
            
            {/* Top Left: 3D Hologram Shield (6 cols) */}
            <div className="lg:col-span-6 glass-panel rounded-2xl p-4 border border-slate-800 flex flex-col gap-2">
              <div className="flex items-center justify-between text-[10px] text-slate-400 pb-1 border-b border-slate-800">
                <span className="text-cyan-400 font-bold flex items-center gap-1.5">
                  <ShieldAlert className="w-3.5 h-3.5" /> 3D CYBER DEFENSE HOLOGRAM SHIELD
                </span>
                <span>PARTICLES: 1,400 · HARMONIC NOISE</span>
              </div>
              <div className="h-[340px] w-full rounded-xl overflow-hidden bg-slate-950/70 border border-slate-800">
                <CyberHologramShield3D
                  riskScore={riskScore}
                  level={threatLevel}
                  isAudioActive={isAudioActive}
                />
              </div>
            </div>

            {/* Top Right: 3D Audio Frequency Ribbon (6 cols) */}
            <div className="lg:col-span-6 glass-panel rounded-2xl p-4 border border-slate-800 flex flex-col gap-2">
              <div className="flex items-center justify-between text-[10px] text-slate-400 pb-1 border-b border-slate-800">
                <span className="text-indigo-400 font-bold flex items-center gap-1.5">
                  <Waves className="w-3.5 h-3.5" /> 3D AUDIO FREQUENCY SPECTRAL RIBBON
                </span>
                <span>DISPLACEMENT: 2,000 VERTICES · 60 FPS</span>
              </div>
              <div className="h-[340px] w-full rounded-xl overflow-hidden bg-slate-950/70 border border-slate-800">
                <AudioFrequencyRibbon3D
                  isActive={isAudioActive}
                  amplitude={1.3}
                  threatLevel={threatLevel}
                />
              </div>
            </div>

            {/* Bottom: 3D Cyber Warfare Globe (12 cols) */}
            <div className="lg:col-span-12 glass-panel rounded-2xl p-4 border border-slate-800 flex flex-col gap-2">
              <div className="flex items-center justify-between text-[10px] text-slate-400 pb-1 border-b border-slate-800">
                <span className="text-red-400 font-bold flex items-center gap-1.5">
                  <Globe className="w-3.5 h-3.5 animate-spin" /> 3D CYBER EXTORTION WARFARE GLOBE &amp; MULE TRACER
                </span>
                <span>SE ASIA COMPOUNDS -&gt; INDIA METRO TARGETS</span>
              </div>
              <CyberWarfareGlobe3D />
            </div>

          </div>
        )}

        {/* FULLSCREEN SHIELD MODE */}
        {activeSandbox === "shield" && (
          <div className="glass-panel rounded-2xl p-4 border border-slate-800 flex-1 flex flex-col gap-2 min-h-[620px]">
            <div className="flex items-center justify-between text-xs text-slate-400 pb-2 border-b border-slate-800">
              <span className="text-cyan-400 font-bold flex items-center gap-2">
                <ShieldAlert className="w-4 h-4" /> FULLSCREEN 3D HOLOGRAM SHIELD
              </span>
              <span>POINTER TRACKING: ACTIVE · DYNAMIC ELECTRIC ARCS</span>
            </div>
            <div className="flex-1 w-full rounded-xl overflow-hidden bg-slate-950/80 border border-slate-800/80">
              <CyberHologramShield3D
                riskScore={riskScore}
                level={threatLevel}
                isAudioActive={isAudioActive}
              />
            </div>
          </div>
        )}

        {/* FULLSCREEN GLOBE MODE */}
        {activeSandbox === "globe" && (
          <div className="glass-panel rounded-2xl p-4 border border-slate-800 flex-1 flex flex-col gap-2">
            <div className="flex items-center justify-between text-xs text-slate-400 pb-2 border-b border-slate-800">
              <span className="text-red-400 font-bold flex items-center gap-2">
                <Globe className="w-4 h-4 animate-spin" /> FULLSCREEN 3D SCAM WARFARE GLOBE
              </span>
              <span>ORBIT: TOUCH/DRAG SUPPORTED · LIVE PARABOLIC BEZIERS</span>
            </div>
            <CyberWarfareGlobe3D className="min-h-[640px]" />
          </div>
        )}

        {/* FULLSCREEN AUDIO RIBBON MODE */}
        {activeSandbox === "ribbon" && (
          <div className="glass-panel rounded-2xl p-4 border border-slate-800 flex-1 flex flex-col gap-2 min-h-[620px]">
            <div className="flex items-center justify-between text-xs text-slate-400 pb-2 border-b border-slate-800">
              <span className="text-indigo-400 font-bold flex items-center gap-2">
                <Waves className="w-4 h-4" /> FULLSCREEN 3D AUDIO FREQUENCY RIBBON
              </span>
              <span>PROCEDURAL WIREFRAME HARMONICS · 16 kHz FFT EMULATION</span>
            </div>
            <div className="flex-1 w-full rounded-xl overflow-hidden bg-slate-950/80 border border-slate-800/80">
              <AudioFrequencyRibbon3D
                isActive={isAudioActive}
                amplitude={1.5}
                threatLevel={threatLevel}
                className="h-full min-h-[580px]"
              />
            </div>
          </div>
        )}

      </div>

      {/* Telemetry Footer Ticker */}
      <footer className="mt-auto border-t border-slate-900 bg-slate-950">
        <LiveTelemetryTicker />
      </footer>
    </main>
  );
}
