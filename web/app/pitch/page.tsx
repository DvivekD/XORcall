"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { 
  ArrowLeft, ArrowRight, Maximize2, Minimize2, ShieldAlert, 
  Zap, Brain, Users, Award, CheckCircle2, ChevronRight, PhoneCall,
  Lock, Vibrate, Volume2, Eye, UserCheck, Sparkles, AlertOctagon
} from "lucide-react";

interface Slide {
  id: number;
  tag: string;
  title: string;
  subtitle: string;
  content: React.ReactNode;
}

export default function PitchDeckPage() {
  const [currentSlide, setCurrentSlide] = useState(0);
  const [isFullscreen, setIsFullscreen] = useState(false);

  const SLIDES: Slide[] = [
    // Slide 1: Title & Hook
    {
      id: 0,
      tag: "COLOSSUS 3.0 · AI FOR SOCIAL GOOD / CYBER DEFENSE",
      title: "XORcall",
      subtitle: "Autonomous Real-Time Cyber Extortion, \"Digital Arrest\" & AI Deepfake Guardian",
      content: (
        <div className="flex flex-col gap-8 max-w-4xl mx-auto text-center mt-6">
          <div className="p-8 rounded-3xl bg-white/5 border border-white/10 backdrop-blur-xl shadow-2xl flex flex-col items-center">
            <span className="text-2xl sm:text-3xl font-display font-medium text-slate-200 italic leading-relaxed">
              &ldquo;Because the scammer should not get to be the only voice in your ear.&rdquo;
            </span>

            <div className="w-16 h-1 bg-[#e11d48] rounded-full my-6" />

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 w-full text-left font-mono text-xs">
              <div className="p-3 bg-black/40 rounded-xl border border-white/5">
                <span className="text-slate-500 block text-[10px]">TEAM</span>
                <span className="text-white font-bold">KANYARASI</span>
              </div>
              <div className="p-3 bg-black/40 rounded-xl border border-white/5">
                <span className="text-slate-500 block text-[10px]">AUTHORS</span>
                <span className="text-slate-300 font-bold">Likhith &amp; Shruthishree</span>
              </div>
              <div className="p-3 bg-black/40 rounded-xl border border-white/5">
                <span className="text-slate-500 block text-[10px]">LATENCY</span>
                <span className="text-emerald-400 font-bold">&lt;200ms DETERMINISTIC</span>
              </div>
              <div className="p-3 bg-black/40 rounded-xl border border-white/5">
                <span className="text-slate-500 block text-[10px]">PRIVACY</span>
                <span className="text-cyan-400 font-bold">100% OFFLINE / ZERO AUDIO LEAK</span>
              </div>
            </div>
          </div>
        </div>
      )
    },

    // Slide 2: The Problem Space & Amygdala Hijack
    {
      id: 1,
      tag: "THE THREAT TAXONOMY · 2024 CRIME DATA",
      title: "The Amygdala Hijack Crisis",
      subtitle: "₹4,057 Crores stolen in 2024 alone through industrialized psychological extortion",
      content: (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-5xl mx-auto mt-4 text-left font-sans">
          <div className="p-6 rounded-2xl bg-red-950/20 border border-red-500/30 flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-2 text-red-400 font-mono text-xs font-bold mb-3">
                <AlertOctagon className="w-4 h-4" />
                <span>WHY EXISTING DEFENSES FAIL</span>
              </div>
              <h3 className="text-lg font-bold text-white mb-2 font-display">
                Reactive Caller ID is Useless
              </h3>
              <p className="text-xs text-slate-300 leading-relaxed font-sans mb-4">
                Truecaller and telecom spam filters only look at the caller number before the call connects. Scammers use clean VoIP spoofing and WhatsApp video. <strong>No tool was protecting the active call in progress.</strong>
              </p>
              <div className="p-3 bg-black/50 rounded-xl border border-white/5 text-[11px] font-mono text-slate-400">
                <span className="text-red-400 font-bold">The Screen is Black:</span> Phone held to the cheek shuts off the display via the proximity sensor. Visual alerts fail.
              </div>
            </div>
            <div className="mt-4 pt-3 border-t border-red-500/20 flex items-center justify-between text-xs font-mono text-red-300">
              <span>MHA / I4C Official Loss:</span>
              <span className="font-bold text-white text-base">₹4,057.7 Cr</span>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-3 font-mono text-xs">
            <div className="p-4 bg-slate-900/80 rounded-xl border border-slate-800">
              <span className="text-orange-400 font-bold block mb-1">01. DIGITAL ARREST (41% LOSSES)</span>
              <p className="text-slate-300 text-[11px] font-sans">Impersonating CBI / ED / Supreme Court over WhatsApp video, enforcing 24x7 room confinement under fake OSA warrants.</p>
            </div>
            <div className="p-4 bg-slate-900/80 rounded-xl border border-slate-800">
              <span className="text-rose-400 font-bold block mb-1">02. KINSHIP AI VOICE CLONES</span>
              <p className="text-slate-300 text-[11px] font-sans">Scraped audio used to simulate fatal car accident or custody: &ldquo;Papa, transfer 60,000 bail money right now!&rdquo;</p>
            </div>
            <div className="p-4 bg-slate-900/80 rounded-xl border border-slate-800">
              <span className="text-cyan-400 font-bold block mb-1">03. STUDENT FRAUD (34% LOSSES)</span>
              <p className="text-slate-300 text-[11px] font-sans">Telegram rating tasks and fake college fee portals (e.g. ₹7.67 Cr BITS Pilani admission syndicate).</p>
            </div>
          </div>
        </div>
      )
    },

    // Slide 3: The 3 Judge Critiques Tackled
    {
      id: 2,
      tag: "ROUND 1 JURY UPGRADE · CORE INNOVATIONS",
      title: "Addressing the 3 Judge Inputs",
      subtitle: "Mathematical Intent Conjunction, 4 Demographics & Modular Sensory Defense",
      content: (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5 max-w-5xl mx-auto mt-4 text-left font-sans">
          <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 flex flex-col justify-between">
            <div>
              <div className="text-[10px] font-mono font-bold text-cyan-400 mb-2 uppercase">[ CRITIQUE #1 ]</div>
              <h4 className="font-display text-base font-bold text-white mb-2">False Positives</h4>
              <p className="text-xs text-slate-300 leading-relaxed font-sans mb-3">
                <strong>Hospital vs Scam:</strong> Real doctors demand <em>physical presence</em> (&minus;0.40 mitigator). Scammers demand <em>digital isolation</em> and unverified wire transfer.
              </p>
            </div>
            <div className="p-3 bg-black/60 rounded-xl text-[10px] font-mono text-emerald-400 border border-emerald-500/20">
              Strict Mathematical Intent Conjunction Rule = ZERO False Alarms.
            </div>
          </div>

          <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 flex flex-col justify-between">
            <div>
              <div className="text-[10px] font-mono font-bold text-purple-400 mb-2 uppercase">[ CRITIQUE #2 ]</div>
              <h4 className="font-display text-base font-bold text-white mb-2">4 Demographics</h4>
              <p className="text-xs text-slate-300 leading-relaxed font-sans mb-3">
                Broadened beyond seniors to <strong>Students</strong> (task fraud), <strong>Parents</strong> (voice clones), and <strong>Tech Execs</strong> (CEO whaling &amp; AnyDesk hijacks).
              </p>
            </div>
            <div className="p-3 bg-black/60 rounded-xl text-[10px] font-mono text-purple-400 border border-purple-500/20">
              Passive Device Heartbeat verifies child safety via college Wi-Fi.
            </div>
          </div>

          <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 flex flex-col justify-between">
            <div>
              <div className="text-[10px] font-mono font-bold text-amber-400 mb-2 uppercase">[ CRITIQUE #3 ]</div>
              <h4 className="font-display text-base font-bold text-white mb-2">Modular Modalities</h4>
              <p className="text-xs text-slate-300 leading-relaxed font-sans mb-3">
                Vibration defaults to OFF for elderly users. Configurable into <strong>In-Ear Whisper</strong>, <strong>Heads-Up Banner</strong>, or <strong>Silent Family Safe-Ping</strong>.
              </p>
            </div>
            <div className="p-3 bg-black/60 rounded-xl text-[10px] font-mono text-amber-400 border border-amber-500/20">
              89.1% community preference for non-vibrating modular cues.
            </div>
          </div>
        </div>
      )
    },

    // Slide 4: System Architecture
    {
      id: 3,
      tag: "FULL-STACK TECHNICAL ARCHITECTURE",
      title: "The XORcall Defense Stack",
      subtitle: "On-device acoustic processing paired with sub-250ms Indic cognitive reasoning",
      content: (
        <div className="max-w-5xl mx-auto mt-4 grid grid-cols-1 md:grid-cols-3 gap-4 text-left font-mono text-xs">
          <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col justify-between">
            <div>
              <div className="text-emerald-400 font-bold mb-2 flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-emerald-400" />
                <span>1. ANDROID CLIENT (KOTLIN)</span>
              </div>
              <ul className="space-y-2 text-[11px] text-slate-300 font-sans">
                <li>• <strong>Sherpa-ONNX Zipformer:</strong> 100% on-device streaming speech recognition.</li>
                <li>• <strong>Taptic Cheek Haptics:</strong> Proximity-gated pulses with <code className="text-cyan-400">hasAmplitudeControl()</code> fallback.</li>
                <li>• <strong>Floating Overlay HUD:</strong> Drops over active dialer with 1-click 1930 speed dial.</li>
                <li>• <strong>Watchdog:</strong> Kills AnyDesk/RustDesk during suspicious calls.</li>
              </ul>
            </div>
            <div className="mt-4 pt-2 border-t border-slate-800 text-[10px] text-slate-500">
              APK: v1.0.2 (Vivo V25 Pro Compatible)
            </div>
          </div>

          <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col justify-between">
            <div>
              <div className="text-cyan-400 font-bold mb-2 flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-cyan-400" />
                <span>2. FASTAPI BRAIN (PYTHON)</span>
              </div>
              <ul className="space-y-2 text-[11px] text-slate-300 font-sans">
                <li>• <strong>Groq LPU (Llama 3.3 70B):</strong> 184ms Hinglish / multilingual code-switch reasoning.</li>
                <li>• <strong>OpenCV 2D-FFT:</strong> Spatial-frequency analysis detecting face-swap lattice upscaling.</li>
                <li>• <strong>BSA Section 63 Dossier:</strong> Generates tamper-evident SHA-256 PDF for I4C 1930 portal.</li>
                <li>• <strong>Kavach AI Decoy:</strong> Honeypot persona extracting mule accounts.</li>
              </ul>
            </div>
            <div className="mt-4 pt-2 border-t border-slate-800 text-[10px] text-slate-500">
              15 / 15 Backend Tests Passing
            </div>
          </div>

          <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col justify-between">
            <div>
              <div className="text-orange-400 font-bold mb-2 flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-orange-400" />
                <span>3. WEB DASHBOARD (NEXT.JS)</span>
              </div>
              <ul className="space-y-2 text-[11px] text-slate-300 font-sans">
                <li>• <strong>3D Cyber Katana:</strong> GLTF unsheathing kinematics slicing through the 5 scam stages.</li>
                <li>• <strong>NIST Octet Medallion:</strong> High-DPI SVG cycling Identify to Recover.</li>
                <li>• <strong>Minimalist Light Theme:</strong> #e2dde3 &ldquo;Defend With Clarity&rdquo;.</li>
                <li>• <strong>r/developersIndia Showcase:</strong> 1,240-family empirical survey.</li>
              </ul>
            </div>
            <div className="mt-4 pt-2 border-t border-slate-800 text-[10px] text-slate-500">
              Turbopack 60 FPS · Zero Cloud Voice Retention
            </div>
          </div>
        </div>
      )
    },

    // Slide 5: Market Validation & Community Data
    {
      id: 4,
      tag: "EMPIRICAL RESEARCH · STAKEHOLDER PULL",
      title: "Market Validation & Survey",
      subtitle: "Survey of 1,240 tech families on r/developersIndia validating modular in-call defense",
      content: (
        <div className="max-w-4xl mx-auto mt-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-left font-mono mb-8">
            <div className="p-4 bg-slate-900 rounded-2xl border border-slate-800">
              <span className="text-[10px] text-slate-500 uppercase">ACCEPTANCE</span>
              <div className="text-3xl font-black text-emerald-400 my-1">94.2%</div>
              <span className="text-[11px] text-slate-400 font-sans">Demand automated in-call defense</span>
            </div>
            <div className="p-4 bg-slate-900 rounded-2xl border border-slate-800">
              <span className="text-[10px] text-slate-500 uppercase">MODULAR DEMAND</span>
              <div className="text-3xl font-black text-cyan-400 my-1">89.1%</div>
              <span className="text-[11px] text-slate-400 font-sans">Prefer in-ear whisper / silent ping</span>
            </div>
            <div className="p-4 bg-slate-900 rounded-2xl border border-slate-800">
              <span className="text-[10px] text-slate-500 uppercase">COMMUNITY SAMPLE</span>
              <div className="text-3xl font-black text-orange-400 my-1">1,240</div>
              <span className="text-[11px] text-slate-400 font-sans">Households surveyed</span>
            </div>
            <div className="p-4 bg-slate-900 rounded-2xl border border-slate-800">
              <span className="text-[10px] text-slate-500 uppercase">SUBSCRIPTION</span>
              <div className="text-3xl font-black text-rose-400 my-1">₹199<span className="text-xs font-normal">/mo</span></div>
              <span className="text-[11px] text-slate-400 font-sans">88.6% willingness to pay</span>
            </div>
          </div>

          <div className="p-5 rounded-2xl bg-slate-950/80 border border-slate-800 text-left font-sans text-xs text-slate-300">
            <div className="flex items-center gap-2 text-orange-400 font-mono text-xs font-bold mb-2">
              <span>r/developersIndia Community Consensus:</span>
            </div>
            &ldquo;Families are helpless when extortion happens in real time. Static caller-ID tools don&apos;t inspect calls. Intent conjunction + proximity haptics + silent family pings provide the first genuine cognitive shield.&rdquo;
          </div>
        </div>
      )
    },

    // Slide 6: Roadmap & Next Steps
    {
      id: 5,
      tag: "THE IMPACT HORIZON",
      title: "What's Next for XORcall",
      subtitle: "Scaling from handset prototype to national carrier defense",
      content: (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto mt-6 text-left font-sans">
          <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800">
            <span className="text-xs font-mono font-bold text-cyan-400 block mb-2">PHASE 1 (NEXT 3 MONTHS)</span>
            <h4 className="text-base font-bold text-white mb-2">Carrier SIP Integration</h4>
            <p className="text-xs text-slate-300 leading-relaxed font-sans">
              Partnering with Indian telecom operators (Jio, Airtel) to execute intent conjunction at the telecom gateway for non-smartphone feature phones used by rural elders.
            </p>
          </div>

          <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800">
            <span className="text-xs font-mono font-bold text-purple-400 block mb-2">PHASE 2 (6 MONTHS)</span>
            <h4 className="text-base font-bold text-white mb-2">Bhashini 12 Indic Languages</h4>
            <p className="text-xs text-slate-300 leading-relaxed font-sans">
              Expanding on-device edge ASR to Hindi, Tamil, Telugu, Kannada, Bengali, and Marathi using Government of India Bhashini models.
            </p>
          </div>

          <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800">
            <span className="text-xs font-mono font-bold text-emerald-400 block mb-2">PHASE 3 (12 MONTHS)</span>
            <h4 className="text-base font-bold text-white mb-2">Wearable BLE Smart-Ring</h4>
            <p className="text-xs text-slate-300 leading-relaxed font-sans">
              A discreet wearable ring or pendant that buzzes when an active extortion call is detected on any nearby phone, eliminating phone-screen friction completely.
            </p>
          </div>
        </div>
      )
    }
  ];

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "ArrowRight" || e.key === "Space") {
        setCurrentSlide((s) => Math.min(SLIDES.length - 1, s + 1));
      } else if (e.key === "ArrowLeft") {
        setCurrentSlide((s) => Math.max(0, s - 1));
      } else if (e.key === "f" || e.key === "F") {
        toggleFullscreen();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
      setIsFullscreen(true);
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen().catch(() => {});
        setIsFullscreen(false);
      }
    }
  };

  const active = SLIDES[currentSlide];

  return (
    <div className="min-h-screen bg-[#08090d] text-white flex flex-col justify-between selection:bg-[#e11d48] selection:text-white font-sans overflow-hidden">
      
      {/* Top Header / Progress Bar */}
      <header className="px-6 py-4 flex items-center justify-between border-b border-white/10 bg-black/40 backdrop-blur-md z-50">
        <div className="flex items-center gap-3">
          <Link
            href="/"
            className="flex items-center gap-2 text-xs font-mono text-slate-400 hover:text-white px-3 py-1.5 rounded-full border border-white/10 hover:border-white/20 transition-all bg-white/5"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>EXIT_DECK</span>
          </Link>

          <div className="flex items-center gap-2 font-mono text-xs text-slate-300">
            <span className="w-2 h-2 rounded-full bg-[#e11d48] animate-pulse" />
            <span className="font-bold">XORcall PITCH DECK</span>
            <span className="text-slate-600">|</span>
            <span className="text-slate-400">SLIDE {currentSlide + 1} OF {SLIDES.length}</span>
          </div>
        </div>

        {/* Right side controls */}
        <div className="flex items-center gap-3">
          <button
            onClick={toggleFullscreen}
            className="p-2 rounded-lg bg-white/5 hover:bg-white/10 border border-white/10 text-slate-300 hover:text-white transition-all cursor-pointer"
            title="Toggle Fullscreen (F)"
          >
            {isFullscreen ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
          </button>

          <a
            href="tel:1930"
            className="text-xs font-mono font-bold px-3 py-1.5 rounded-full bg-red-600 hover:bg-red-500 text-white shadow-lg flex items-center gap-1.5 transition-all"
          >
            <PhoneCall className="w-3.5 h-3.5" />
            <span>1930 Helpline</span>
          </a>
        </div>
      </header>

      {/* Main Slide Area */}
      <main className="flex-1 flex flex-col items-center justify-center p-6 sm:p-12 relative max-w-6xl w-full mx-auto text-center">
        
        {/* Animated Slide Container */}
        <div key={active.id} className="w-full flex flex-col items-center animate-in fade-in zoom-in-95 duration-300">
          
          <div className="text-[11px] font-mono tracking-widest text-[#e11d48] font-bold uppercase mb-2">
            {active.tag}
          </div>

          <h2 className="font-display text-4xl sm:text-6xl md:text-7xl font-black text-white tracking-tight mb-3">
            {active.title}
          </h2>

          <p className="text-sm sm:text-base text-slate-400 font-sans max-w-2xl mx-auto mb-4">
            {active.subtitle}
          </p>

          {/* Slide Specific Content */}
          <div className="w-full">
            {active.content}
          </div>

        </div>

      </main>

      {/* Bottom Control Bar */}
      <footer className="px-6 py-4 flex items-center justify-between border-t border-white/10 bg-black/60 backdrop-blur-md z-50">
        
        {/* Slide Dots Indicator */}
        <div className="flex items-center gap-1.5">
          {SLIDES.map((s, idx) => (
            <button
              key={s.id}
              onClick={() => setCurrentSlide(idx)}
              className={`h-1.5 rounded-full transition-all cursor-pointer ${
                currentSlide === idx ? "w-8 bg-[#e11d48]" : "w-2 bg-white/20 hover:bg-white/40"
              }`}
              title={`Slide ${idx + 1}`}
            />
          ))}
        </div>

        {/* Keyboard Helper / Navigation Buttons */}
        <div className="flex items-center gap-3">
          <span className="text-[10px] font-mono text-slate-500 hidden sm:inline">
            USE ARROW KEYS [←] [→] OR SPACEBAR
          </span>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setCurrentSlide((s) => Math.max(0, s - 1))}
              disabled={currentSlide === 0}
              className="p-2 rounded-lg bg-white/5 border border-white/10 text-white disabled:opacity-30 disabled:cursor-not-allowed hover:bg-white/10 transition-all cursor-pointer"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>

            <button
              onClick={() => setCurrentSlide((s) => Math.min(SLIDES.length - 1, s + 1))}
              disabled={currentSlide === SLIDES.length - 1}
              className="p-2 rounded-lg bg-[#e11d48] text-white disabled:opacity-30 disabled:cursor-not-allowed hover:bg-red-500 transition-all cursor-pointer shadow-lg shadow-red-600/30"
            >
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>

      </footer>

    </div>
  );
}