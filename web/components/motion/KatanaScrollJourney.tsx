"use client";

import React, { useState, useEffect, useRef } from "react";
import CyberKatana3D from "@/components/motion/CyberKatana3D";
import CyberOctagonMedallion from "@/components/motion/CyberOctagonMedallion";

const PHRASES = [
  {
    phase: "PHASE 01 // IMPERSONATION [IDENTIFY]",
    title: "The Hook Pretext",
    quote: "“A parcel containing 140g narcotics seized under your Aadhaar card.”",
    color: "text-red-400",
    stageNum: "01",
    nist: "IDENTIFY"
  },
  {
    phase: "PHASE 02 // COERCION [PROTECT]",
    title: "Digital Arrest",
    quote: "“You are under non-bailable arrest. Immediate video confinement active.”",
    color: "text-amber-400",
    stageNum: "02",
    nist: "PROTECT"
  },
  {
    phase: "PHASE 03 // ISOLATION [DETECT]",
    title: "Psychological Isolation",
    quote: "“Lock your door. Stay on camera. Do not inform any family member.”",
    color: "text-blue-400",
    stageNum: "03",
    nist: "DETECT"
  },
  {
    phase: "PHASE 04 // EXTORTION [RESPOND]",
    title: "Liquidation Vault",
    quote: "“Transfer ₹4,00,000 by RTGS to RBI verification escrow account now.”",
    color: "text-rose-400",
    stageNum: "04",
    nist: "RESPOND"
  },
  {
    phase: "PHASE 05 // NEUTRALIZED [RECOVER]",
    title: "XORcall Disruption",
    quote: "“Line severed by on-device Zipformer. Tamper-evident dossier locked to 1930.”",
    color: "text-emerald-400",
    stageNum: "05",
    nist: "RECOVER"
  }
];

export default function KatanaScrollJourney() {
  const containerRef = useRef<HTMLDivElement>(null);
  const [journeyProgress, setJourneyProgress] = useState(0);

  useEffect(() => {
    const handleScroll = () => {
      if (!containerRef.current) return;
      const rect = containerRef.current.getBoundingClientRect();
      const totalH = rect.height - window.innerHeight;
      if (totalH <= 0) return;

      const scrolled = -rect.top;
      const progress = Math.min(1, Math.max(0, scrolled / totalH));
      setJourneyProgress(progress);
    };

    window.addEventListener("scroll", handleScroll, { passive: true });
    handleScroll();

    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const activeIdx = Math.min(PHRASES.length - 1, Math.floor(journeyProgress * PHRASES.length));
  const currentPhrase = PHRASES[activeIdx];

  return (
    <div ref={containerRef} className="relative w-full h-[400vh] bg-[#0f1014] text-white">
      
      {/* Sticky Viewport Pinned in Center Screen (Always Dark, Pure Negative Space) */}
      <div className="sticky top-0 h-screen w-full flex flex-col justify-between items-center py-16 px-6 overflow-hidden bg-[#0f1014] z-20">
        
        {/* Top Phase Header */}
        <div key={`header-${currentPhrase.stageNum}`} className="z-30 text-center max-w-2xl pt-6 transition-all duration-300 animate-in fade-in slide-in-from-top-2">
          <div className="text-[11px] font-mono tracking-widest uppercase font-bold text-slate-400 mb-2 flex items-center justify-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-[#e11d48] animate-ping" />
            <span>{currentPhrase.phase}</span>
          </div>
          <h2 className="font-display text-4xl sm:text-6xl md:text-7xl font-black text-white tracking-tight">
            {currentPhrase.title}
          </h2>
        </div>

        {/* 5-Phase NIST Octagon Cyber Medallion (Behind 3D Katana) */}
        <div className="absolute inset-0 w-full h-full z-0 pointer-events-none flex items-center justify-center">
          <CyberOctagonMedallion progress={journeyProgress} activePhaseIdx={activeIdx} />
        </div>

        {/* 3D Katana Canvas (Fills Screen, Upper Blade & Lower Scabbard Split Around Center) */}
        <div className="absolute inset-0 w-full h-full z-10 pointer-events-none flex items-center justify-center">
          <CyberKatana3D progress={journeyProgress} className="w-full h-full" />
        </div>

        {/* Center Quote Overlay (Pure Space Grotesk Typography, Razor-Sharp Negative Space) */}
        <div key={`quote-${currentPhrase.stageNum}`} className="z-30 text-center max-w-3xl px-6 transition-all duration-300 pb-12 animate-in fade-in slide-in-from-bottom-2">
          <p className="font-display text-xl sm:text-3xl text-slate-200 font-normal leading-relaxed italic drop-shadow-lg">
            {currentPhrase.quote}
          </p>

          <div className="mt-6 flex items-center justify-center gap-4 text-[11px] font-mono tracking-widest text-slate-400">
            <span>KATANA UNSHEATHE : {Math.round(journeyProgress * 100)}%</span>
            <span className="text-slate-600">·</span>
            <span className={`font-bold ${currentPhrase.color}`}>
              STAGE {currentPhrase.stageNum} OF 05 // {currentPhrase.nist}
            </span>
          </div>
        </div>

      </div>

    </div>
  );
}
