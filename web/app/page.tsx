"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import TelemetryHUD from "@/components/ui/TelemetryHUD";
import KatanaScrollJourney from "@/components/motion/KatanaScrollJourney";
import GuardianCompanion from "@/components/ui/GuardianCompanion";
import CyberWarfareGlobe3D from "@/components/motion/CyberWarfareGlobe3D";
import AudioFrequencyRibbon3D from "@/components/motion/AudioFrequencyRibbon3D";
import CyberKatana3D from "@/components/motion/CyberKatana3D";
import CyberOctagonMedallion from "@/components/motion/CyberOctagonMedallion";
import { 
  Zap, Radio, PhoneCall, ChevronRight, AlertOctagon, 
  Vibrate, Volume2, Eye, UserCheck
} from "lucide-react";

const SCENARIOS = [
  {
    id: "digital_arrest",
    title: "Digital Arrest",
    tag: "CBI PRETEXT",
    lines: [
      "Hello, I am Inspector Rao from Mumbai Crime Branch Cyber Cell.",
      "A parcel sent under your Aadhaar containing 140g of MDMA narcotics was seized by customs.",
      "You are under digital arrest. Lock your room, stay on video call, do not disconnect.",
      "Transfer 4 lakh rupees immediately to the RBI supervision escrow account to avoid warrant."
    ]
  },
  {
    id: "family_deepfake",
    title: "Voice Clone",
    tag: "KINSHIP EXTORTION",
    lines: [
      "Papa! My phone broke in the accident, I'm calling from an unknown hospital number!",
      "I hit someone on the highway... police took me into custody, please help me Papa!",
      "Inspector says transfer 60,000 rupees bail money right now to this clinic UPI handle.",
      "Don't tell Mummy, transfer immediately or they will put me in lockup!"
    ]
  },
  {
    id: "student_task",
    title: "Telegram Task Trap",
    tag: "STUDENT FRAUD",
    lines: [
      "Earn ₹5,000 daily with our flexible part-time review job from home.",
      "Simple task: like YouTube videos and review hotel listings on Google Maps.",
      "To unlock crypto withdrawal of ₹18,500, deposit ₹12,000 task security immediately.",
      "Failure to complete this task will freeze your wallet balance permanently."
    ]
  },
  {
    id: "ceo_whaling",
    title: "CEO Whaling",
    tag: "EXEC WIRE PHISH",
    lines: [
      "Hi, this is Rajesh Verma, CEO. I am in a board meeting, do not call my mobile.",
      "We have an urgent deadline to close a confidential client settlement before 5 PM.",
      "Process an advance wire transfer of 8 lakh rupees to our overseas vendor immediately.",
      "Keep this strictly confidential as per NDA guidelines until the press release."
    ]
  }
];

export default function Home() {
  const [isLightMode, setIsLightMode] = useState(false);
  const [scrollProgress, setScrollProgress] = useState(0);

  // Interceptor State
  const [selectedScenario, setSelectedScenario] = useState(SCENARIOS[0]);
  const [isRunning, setIsRunning] = useState(false);
  const [currentLineIndex, setCurrentLineIndex] = useState(0);
  const [threatLevel, setThreatLevel] = useState<"SAFE" | "WATCH" | "WARN" | "CRITICAL">("SAFE");
  const [hapticsOn, setHapticsOn] = useState(false); // Default OFF
  const [whisperOn, setWhisperOn] = useState(true);
  const [bannerOn, setBannerOn] = useState(true);
  const [familyPingOn, setFamilyPingOn] = useState(true);
  const [safePingSent, setSafePingSent] = useState(false);
  const [dossierDispatched, setDossierDispatched] = useState(false);

  // Mathematically accurate light mode detection based on #clarity section offset
  useEffect(() => {
    const handleScroll = () => {
      const top = window.scrollY || document.documentElement.scrollTop;
      const height = document.documentElement.scrollHeight - window.innerHeight;
      const pct = height > 0 ? top / height : 0;
      setScrollProgress(pct);

      const clarityEl = document.getElementById("clarity");
      if (clarityEl) {
        const rect = clarityEl.getBoundingClientRect();
        // Switches to light mode ONLY when #clarity is scrolled into the viewport
        setIsLightMode(rect.top <= 100);
      }
    };

    window.addEventListener("scroll", handleScroll, { passive: true });
    handleScroll();

    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const handleStartSimulation = () => {
    setIsRunning(true);
    setCurrentLineIndex(0);
    setThreatLevel("WATCH");
    setDossierDispatched(false);

    let idx = 0;
    const interval = setInterval(() => {
      idx += 1;
      if (idx < selectedScenario.lines.length) {
        setCurrentLineIndex(idx);
        if (idx === 1) setThreatLevel("WARN");
        if (idx >= 2) setThreatLevel("CRITICAL");
      } else {
        clearInterval(interval);
        setThreatLevel("CRITICAL");
      }
    }, 1800);
  };

  const handleResetSimulation = () => {
    setIsRunning(false);
    setCurrentLineIndex(0);
    setThreatLevel("SAFE");
    setDossierDispatched(false);
  };

  const watermarkX = (scrollProgress * 220).toFixed(1);

  return (
    <div className={`min-h-screen transition-colors duration-700 ${isLightMode ? "bg-[#e2dde3] text-[#14151a]" : "bg-[#0f1014] text-white"}`}>
      
      {/* Real-time Telemetry HUD (0-100% Scroll & Left Sticky Pill) */}
      <TelemetryHUD isLightMode={isLightMode} />

      {/* Floating AI Companion (AIKA / OJI widget) */}
      <GuardianCompanion isLightMode={isLightMode} />

      {/* Fixed Minimalist Header */}
      <header className="fixed top-0 left-0 right-0 z-50 px-6 py-4 flex items-center justify-between">
        
        {/* Brand & Red Vermilion Square Icon + Badges */}
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-[#e11d48] flex items-center justify-center text-white shadow-md">
            <Zap className="w-4 h-4 fill-white" />
          </div>
          <div className="flex items-center gap-2">
            <span className={`font-display text-lg font-black tracking-tight ${isLightMode ? "text-[#14151a]" : "text-white"}`}>
              XORcall<sup className="text-[10px] font-normal">©</sup>
            </span>

            {/* ( 8 ) Octet Symbol */}
            <span className="font-mono text-xs font-bold text-slate-400 bg-white/5 border border-white/10 px-2.5 py-0.5 rounded-full hidden sm:inline-block">
              ( 8 )
            </span>

            {/* Orange / Coral Pill ( 🗡️ べ ) */}
            <span className="font-mono text-xs font-bold text-[#fb923c] bg-[#f97316]/20 border border-[#f97316]/40 px-2.5 py-0.5 rounded-full flex items-center gap-1 shadow-sm">
              <span>( 🗡️ べ )</span>
            </span>

            {/* Version Badge */}
            <span className="font-mono text-[10px] font-bold text-cyan-400 bg-cyan-400/10 border border-cyan-400/20 px-2 py-0.5 rounded-full hidden md:inline-block">
              V_2.0.4
            </span>
          </div>
        </div>

        {/* Center Pill Navigation */}
        <nav className="hidden md:flex items-center gap-2">
          <a
            href="#hero"
            className={`text-xs font-mono px-3.5 py-1.5 rounded-full transition-all cursor-pointer ${
              isLightMode ? "pill-button-light" : "pill-button"
            }`}
          >
            Overview
          </a>
          <a
            href="#journey"
            className={`text-xs font-mono px-3.5 py-1.5 rounded-full transition-all cursor-pointer ${
              isLightMode ? "pill-button-light" : "pill-button"
            }`}
          >
            5-Phase Blade
          </a>
          <a
            href="#clarity"
            className={`text-xs font-mono px-3.5 py-1.5 rounded-full transition-all cursor-pointer ${
              isLightMode ? "pill-button-light" : "pill-button"
            }`}
          >
            Principles
          </a>
          <a
            href="#interceptor"
            className={`text-xs font-mono px-3.5 py-1.5 rounded-full transition-all cursor-pointer ${
              isLightMode ? "pill-button-light" : "pill-button"
            }`}
          >
            Interceptor
          </a>
          <Link
            href="/community"
            className={`text-xs font-mono px-3.5 py-1.5 rounded-full transition-all cursor-pointer flex items-center gap-1 ${
              isLightMode ? "pill-button-light" : "pill-button text-orange-400 border-orange-500/30"
            }`}
          >
            <span className="text-orange-500 font-bold">r/</span>Community
          </Link>
          <a
            href="tel:1930"
            className="shimmer-pill-red text-xs font-mono font-bold px-4 py-1.5 rounded-full shadow-lg flex items-center gap-1.5 cursor-pointer"
          >
            <PhoneCall className="w-3.5 h-3.5" />
            <span>1930 Helpline</span>
          </a>
        </nav>

        {/* Right side live status indicator */}
        <div className="flex items-center gap-2 font-mono text-xs">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <span className="text-slate-400 hidden lg:inline">ON-DEVICE ZIPFORMER</span>
        </div>
      </header>

      {/* =========================================================================
          ACT 1: THE DARK OBSIDIAN REALM (#0f1014) - PURE NEGATIVE SPACE
          ========================================================================= */}
      <section id="hero" className="min-h-screen pt-32 pb-16 px-6 sm:px-12 relative flex flex-col justify-between overflow-hidden bg-[#0f1014] text-white">
        
        {/* Giant Hollow Outline Parallax Watermark Text (Space Grotesk) */}
        <div 
          className="absolute -bottom-4 sm:-bottom-8 left-0 right-0 overflow-hidden pointer-events-none select-none z-0"
          style={{ transform: `translate3d(-${watermarkX}px, 0, 0)` }}
        >
          <span className={`${isLightMode ? "ghost-hollow-watermark-light" : "ghost-hollow-watermark"} text-[20vw] sm:text-[22vw] leading-none block font-display`}>
            X O R c a l l
          </span>
        </div>

        {/* Centerpiece: Pop-up 3D Katana & NIST Medallion Entrance (Video Frame 0000) */}
        <div className="absolute inset-0 w-full h-full z-0 pointer-events-none flex items-center justify-center">
          <CyberOctagonMedallion progress={0} activePhaseIdx={0} />
          <CyberKatana3D progress={0} className="w-full h-full" />
        </div>

        {/* Minimalist Hero Title (Top-Left: Master Your Safety - Space Grotesk) */}
        <div className="relative z-10 max-w-4xl mt-8 sm:mt-12">
          <div className="text-[11px] font-mono tracking-widest text-[#e11d48] font-bold uppercase mb-4 flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#e11d48] animate-pulse" />
            V_2.0.4 · AUTONOMOUS COGNITIVE DEFENSE
          </div>
          
          <h1 className="font-display text-5xl sm:text-7xl md:text-8xl lg:text-9xl font-black tracking-tight leading-[0.94] text-white">
            Master <br />
            Your Safety
          </h1>

          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 mt-6">
            <span className="text-xs font-mono font-bold text-slate-300">
              Built by Team KANYARASI
            </span>
            <span className="text-slate-600 hidden sm:inline">|</span>
            <span className="text-xs font-mono text-cyan-400 font-bold uppercase tracking-wider">
              CSS-FIRST · JS-LIGHT · 60 FPS
            </span>
          </div>
        </div>

        {/* Bottom Hero Micro-Telemetry */}
        <div className="relative z-10 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-t border-white/10 pt-4 text-[11px] font-mono text-slate-400 mt-20">
          <div>FOR IN-CALL EXTORTION AND VOICE-CLONE NEUTRALIZATION</div>
          <div className="flex items-center gap-3">
            <span className="text-white font-bold">&lt;300MS COGNITIVE SPEED</span>
            <span>·</span>
            <span className="text-emerald-400">0% CLOUD VOICE RETENTION</span>
          </div>
        </div>
      </section>

      {/* =========================================================================
          THE 3D CYBER KATANA 5-PHASE SCROLL JOURNEY (ALWAYS DARK, PURE NEGATIVE SPACE)
          ========================================================================= */}
      <section id="journey" className="w-full relative bg-[#0f1014]">
        <KatanaScrollJourney />
      </section>

      {/* =========================================================================
          ACT 2: TRANSITION TO WARM ARCHITECTURAL BONE / CHALK THEME (#e2dde3)
          Exact match to user reference image (media_1794570774157_1235ccf2.png)
          ========================================================================= */}
      <div id="clarity-container" className="w-full transition-colors duration-700 bg-[#e2dde3] text-[#14151a]">
        
        {/* Section: "Defend With Clarity" (Exact replication of user screenshot) */}
        <section id="clarity" className="min-h-screen pt-32 pb-24 px-8 sm:px-16 max-w-7xl mx-auto flex flex-col justify-between relative border-t border-black/10">
          
          {/* Top Row: Large Display Title + Small Right Label */}
          <div className="flex flex-col md:flex-row items-start md:items-start justify-between gap-8">
            <div>
              <h2 className="font-display text-6xl sm:text-7xl md:text-8xl font-black tracking-tight text-[#14151a] leading-[0.95]">
                Defend <br />
                With <br />
                Clarity
              </h2>
            </div>

            <div className="text-[11px] font-mono tracking-widest text-[#70717b] uppercase max-w-xs mt-3 leading-relaxed">
              BUILT TO TUNE YOUR SAFETY, <br />
              NOT FIGHT YOUR PHONE LINE
            </div>
          </div>

          {/* Centerpiece: Clean Negative Space Symbol (Replicating user image "( べ )") */}
          <div className="my-24 flex items-center justify-center">
            <span className="font-display text-5xl sm:text-7xl font-bold text-[#14151a]/85 tracking-widest">
              ( べ )
            </span>
          </div>

          {/* 4-Column Numbered Layout (1), (2), (3), (4) */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8 border-t border-black/10 pt-12">
            <div>
              <div className="text-xs font-mono text-[#70717b] mb-3">(1)</div>
              <p className="text-xs text-[#555660] leading-relaxed font-sans font-medium">
                Uses native on-device Sherpa-ONNX streaming Zipformer, refined by a precision mathematical formula.
              </p>
            </div>

            <div>
              <div className="text-xs font-mono text-[#70717b] mb-3">(2)</div>
              <p className="text-xs text-[#555660] leading-relaxed font-sans font-medium">
                Designed with a lightweight offline architecture that lets zero audio stream to remote servers.
              </p>
            </div>

            <div>
              <div className="text-xs font-mono text-[#70717b] mb-3">(3)</div>
              <p className="text-xs text-[#555660] leading-relaxed font-sans font-medium">
                Configure behavior directly in markup with modular controls — no vibration required until you really want it.
              </p>
            </div>

            <div>
              <div className="text-xs font-mono text-[#70717b] mb-3">(4)</div>
              <p className="text-xs text-[#555660] leading-relaxed font-sans font-medium">
                Built for BSA-compliant digital forensics packaging, open to instant 1-click dispatch to 1930 / I4C.
              </p>
            </div>
          </div>
        </section>

        {/* Section: Frameless Interceptor Console (Light & Clean) */}
        <section id="interceptor" className="py-20 px-8 max-w-5xl mx-auto border-t border-black/10">
          <div className="text-center mb-10">
            <span className="text-xs font-mono font-bold uppercase tracking-widest text-[#e11d48]">
              [ REAL-TIME COGNITIVE DEFENSE ]
            </span>
            <h2 className="font-display text-4xl sm:text-5xl font-black text-[#14151a] mt-2">
              Autonomous Interceptor
            </h2>
            <p className="text-xs sm:text-sm text-[#70717b] max-w-md mx-auto mt-2">
              Select a scam vector below and run real-time speech interception.
            </p>
          </div>

          {/* Scenario Selector Pills */}
          <div className="flex flex-wrap items-center justify-center gap-2 mb-8">
            {SCENARIOS.map((s) => (
              <button
                key={s.id}
                onClick={() => {
                  setSelectedScenario(s);
                  handleResetSimulation();
                }}
                className={`px-4 py-2 rounded-full font-mono text-xs font-bold transition-all cursor-pointer ${
                  selectedScenario.id === s.id
                    ? "bg-[#14151a] text-white shadow-lg scale-105"
                    : "bg-black/5 text-[#555660] hover:text-[#14151a] hover:bg-black/10"
                }`}
              >
                <span>{s.title}</span>
                <span className="ml-1 text-[9px] opacity-60">[{s.tag}]</span>
              </button>
            ))}
          </div>

          {/* Minimalist Interceptor Console Box */}
          <div className="bg-white/95 rounded-3xl p-6 sm:p-8 border border-black/10 shadow-xl backdrop-blur-md">
            
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pb-6 border-b border-black/10">
              <div className="flex items-center gap-3">
                <div className={`w-3 h-3 rounded-full ${threatLevel === "CRITICAL" ? "bg-red-600 animate-ping" : threatLevel === "WARN" ? "bg-amber-500 animate-pulse" : "bg-emerald-600"}`} />
                <div>
                  <div className="text-xs font-mono font-bold text-[#14151a]">
                    STATUS: {threatLevel === "CRITICAL" ? "CRITICAL THREAT INTERCEPTED" : threatLevel === "WARN" ? "CONJUNCTION IN FLIGHT" : isRunning ? "LINE MONITORING" : "STANDBY"}
                  </div>
                  <div className="text-[10px] font-mono text-[#70717b]">
                    SHERPA-ONNX ZIPFORMER · 184MS GROQ LPU
                  </div>
                </div>
              </div>

              <div>
                {!isRunning ? (
                  <button
                    onClick={handleStartSimulation}
                    className="shimmer-pill-red font-mono font-bold text-xs px-6 py-2.5 rounded-full shadow-lg hover:scale-105 transition-all flex items-center gap-2 cursor-pointer"
                  >
                    <Zap className="w-3.5 h-3.5 fill-white" />
                    <span>START IN-CALL STREAM</span>
                  </button>
                ) : (
                  <button
                    onClick={handleResetSimulation}
                    className="pill-button-light font-mono text-xs px-5 py-2.5 rounded-full transition-all cursor-pointer"
                  >
                    RESET STREAM
                  </button>
                )}
              </div>
            </div>

            {/* Dynamic 3D Audio Frequency Ribbon */}
            <div className="my-6 h-36 w-full rounded-2xl overflow-hidden bg-[#0f1014] border border-black/10 relative flex items-center justify-center">
              <AudioFrequencyRibbon3D isActive={isRunning} amplitude={threatLevel === "CRITICAL" ? 1.8 : 0.8} />
            </div>

            {/* In-Call Transcript Chunk Box */}
            <div className="bg-[#f2eee9] rounded-2xl p-5 border border-black/5 font-mono text-xs min-h-[100px] flex flex-col justify-center">
              {isRunning ? (
                <div>
                  <span className="text-[10px] text-red-600 font-bold uppercase tracking-wider block mb-1">
                    [ INCOMING AUDIO PACKET #{currentLineIndex + 1} ]
                  </span>
                  <p className="text-[#14151a] text-sm leading-relaxed font-sans font-medium">
                    &ldquo;{selectedScenario.lines[currentLineIndex]}&rdquo;
                  </p>
                </div>
              ) : (
                <p className="text-[#70717b] text-center font-mono">
                  Click &ldquo;START IN-CALL STREAM&rdquo; above to run real-time speech interception.
                </p>
              )}
            </div>

            {/* Verdict & 1930 Auto-Dispatch Banner */}
            {threatLevel === "CRITICAL" && (
              <div className="mt-6 p-5 rounded-2xl bg-red-50 border border-red-300 flex flex-col sm:flex-row items-center justify-between gap-4 animate-in fade-in duration-300">
                <div className="flex items-center gap-3">
                  <AlertOctagon className="w-8 h-8 text-red-600 shrink-0" />
                  <div>
                    <div className="font-black text-sm text-red-900 font-mono tracking-wide">
                      BSA DIGITAL FORENSICS DOSSIER GENERATED
                    </div>
                    <div className="text-[11px] text-red-700 font-mono">
                      SHA-256: 3e8b...9f41 · Extortion Confidence: 99.4%
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => setDossierDispatched(true)}
                  disabled={dossierDispatched}
                  className={`px-5 py-2.5 rounded-full font-mono font-bold text-xs shadow-lg transition-all cursor-pointer whitespace-nowrap ${
                    dossierDispatched
                      ? "bg-emerald-600 text-white"
                      : "bg-red-600 hover:bg-red-500 text-white shadow-red-600/30 hover:scale-105"
                  }`}
                >
                  {dossierDispatched ? "✓ DISPATCHED TO 1930" : "1-CLICK DISPATCH TO 1930"}
                </button>
              </div>
            )}
          </div>
        </section>

        {/* Section: Modular Sensory Interventions (Judge Critique #3) */}
        <section className="py-16 px-8 max-w-5xl mx-auto border-t border-black/10">
          <div className="mb-8">
            <span className="text-xs font-mono font-bold uppercase tracking-widest text-[#70717b]">
              [ JUDGE CRITIQUE #3 RESOLUTION ]
            </span>
            <h3 className="font-display text-2xl font-black text-[#14151a] mt-1">
              Modular Sensory Interventions
            </h3>
            <p className="text-xs text-[#70717b] mt-1">
              Taptic Cheek Haptics default to OFF for vibration-averse users. Choose from In-Ear Whisper or Screen Overlay.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white/90 rounded-2xl p-5 border border-black/10 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <Vibrate className="w-5 h-5 text-amber-600" />
                  <input 
                    type="checkbox" 
                    checked={hapticsOn} 
                    onChange={(e) => setHapticsOn(e.target.checked)}
                    className="w-4 h-4 accent-amber-600 cursor-pointer"
                  />
                </div>
                <h4 className="font-bold text-sm text-[#14151a]">Taptic Haptics</h4>
                <p className="text-[11px] text-[#70717b] mt-1">Cheek bone pulses. (Default: OFF).</p>
              </div>
              <div className="text-[10px] font-mono text-[#70717b] mt-4">
                STATUS: {hapticsOn ? "ENABLED" : "OFF (MODULAR)"}
              </div>
            </div>

            <div className="bg-white/90 rounded-2xl p-5 border border-black/10 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <Volume2 className="w-5 h-5 text-cyan-600" />
                  <input 
                    type="checkbox" 
                    checked={whisperOn} 
                    onChange={(e) => setWhisperOn(e.target.checked)}
                    className="w-4 h-4 accent-cyan-600 cursor-pointer"
                  />
                </div>
                <h4 className="font-bold text-sm text-[#14151a]">Audio Whisper</h4>
                <p className="text-[11px] text-[#70717b] mt-1">Synthetic TTS voice advising hang up.</p>
              </div>
              <div className="text-[10px] font-mono text-[#70717b] mt-4">
                STATUS: {whisperOn ? "ACTIVE" : "MUTED"}
              </div>
            </div>

            <div className="bg-white/90 rounded-2xl p-5 border border-black/10 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <Eye className="w-5 h-5 text-emerald-600" />
                  <input 
                    type="checkbox" 
                    checked={bannerOn} 
                    onChange={(e) => setBannerOn(e.target.checked)}
                    className="w-4 h-4 accent-emerald-600 cursor-pointer"
                  />
                </div>
                <h4 className="font-bold text-sm text-[#14151a]">Heads-Up Banner</h4>
                <p className="text-[11px] text-[#70717b] mt-1">Floating overlay over dialer screen.</p>
              </div>
              <div className="text-[10px] font-mono text-[#70717b] mt-4">
                STATUS: {bannerOn ? "ARMED" : "DISABLED"}
              </div>
            </div>

            <div className="bg-white/90 rounded-2xl p-5 border border-black/10 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <UserCheck className="w-5 h-5 text-blue-600" />
                  <input 
                    type="checkbox" 
                    checked={familyPingOn} 
                    onChange={(e) => setFamilyPingOn(e.target.checked)}
                    className="w-4 h-4 accent-blue-600 cursor-pointer"
                  />
                </div>
                <h4 className="font-bold text-sm text-[#14151a]">Family Safe-Ping</h4>
                <p className="text-[11px] text-[#70717b] mt-1">1-tap &ldquo;I am safe&rdquo; heartbeat to parents.</p>
              </div>
              <button
                onClick={() => setSafePingSent(true)}
                className="mt-4 text-[10px] font-mono bg-blue-100 text-blue-700 rounded px-2 py-1 hover:bg-blue-200 transition-colors cursor-pointer"
              >
                {safePingSent ? "✓ PING SENT" : "SIMULATE SAFE PING"}
              </button>
            </div>
          </div>
        </section>

        {/* Section: 3D Cyber Warfare Matrix */}
        <section id="warfare" className="py-16 px-8 max-w-5xl mx-auto border-t border-black/10">
          <div className="bg-[#0f1014] text-white rounded-3xl p-6 sm:p-8 border border-black/20 overflow-hidden shadow-2xl">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
              <div>
                <div className="text-xs font-mono text-red-400 font-bold tracking-wider mb-1">
                  [ CROSS-BORDER SYNDICATE TELEMETRY ]
                </div>
                <h3 className="font-display text-2xl sm:text-3xl font-black text-white">
                  Live Cyber Warfare Matrix
                </h3>
              </div>
              <div className="text-xs font-mono text-slate-400 bg-white/5 border border-white/10 px-3 py-1.5 rounded-full">
                DRAG ORBIT · CAMBODIA / MYANMAR TO INDIA ARCS
              </div>
            </div>

            <div className="w-full h-[400px] rounded-2xl overflow-hidden border border-white/5 bg-slate-950">
              <CyberWarfareGlobe3D />
            </div>
          </div>
        </section>

        {/* Minimalist Light Footer */}
        <footer className="border-t border-black/10 py-10 px-8 text-xs font-mono text-[#70717b] max-w-5xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <span className="font-bold text-[#14151a]">XORcall · Kavach Neural Guardian</span>
            <span>·</span>
            <span>Team KANYARASI</span>
          </div>

          <div>
            <a href="tel:1930" className="text-[#e11d48] font-bold hover:underline flex items-center gap-1.5">
              <PhoneCall className="w-3.5 h-3.5" />
              <span>National Cybercrime Helpline: 1930</span>
            </a>
          </div>
        </footer>

      </div>

    </div>
  );
}
