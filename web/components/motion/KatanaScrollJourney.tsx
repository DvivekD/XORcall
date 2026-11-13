"use client";

import React, { useState, useEffect, useRef } from "react";
import CyberKatana3D from "@/components/motion/CyberKatana3D";

const PHRASES = [
  {
    phase: "PHASE 01 // IMPERSONATION",
    title: "The Hook Pretext",
    quote: "“A parcel containing 140g narcotics seized under your Aadhaar card.”",
    color: "text-red-400"
  },
  {
    phase: "PHASE 02 // COERCION",
    title: "Digital Arrest",
    quote: "“You are under non-bailable arrest. Immediate video confinement active.”",
    color: "text-amber-400"
  },
  {
    phase: "PHASE 03 // ISOLATION",
    title: "Psychological Isolation",
    quote: "“Lock your door. Stay on camera. Do not inform any family member.”",
    color: "text-blue-400"
  },
  {
    phase: "PHASE 04 // EXTORTION",
    title: "Liquidation Vault",
    quote: "“Transfer ₹4,00,000 by RTGS to RBI verification escrow account now.”",
    color: "text-rose-400"
  },
  {
    phase: "PHASE 05 // NEUTRALIZED",
    title: "XORcall Disruption",
    quote: "“Line severed by on-device Zipformer. Tamper-evident dossier locked to 1930.”",
    color: "text-emerald-400"
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

  // Compute active phrase index (0 to 4)
  const activeIdx = Math.min(PHRASES.length - 1, Math.floor(journeyProgress * PHRASES.length));
  const currentPhrase = PHRASES[activeIdx];

  return (
    <div ref={containerRef} className="relative w-full h-[320vh]">
      
      {/* Sticky Viewport Pinned in Center Screen (Pure Negative Space, Zero Clutter) */}
      <div className="sticky top-0 h-screen w-full flex flex-col justify-between items-center py-20 px-6 overflow-hidden">
        
        {/* Dynamic Phase Badge & Title */}
        <div className="z-10 text-center max-w-xl transition-all duration-300">
          <div className="text-[11px] font-mono tracking-widest uppercase font-bold text-slate-400 mb-2">
            {currentPhrase.phase}
          </div>
          <h2 className="font-display text-4xl sm:text-6xl font-black text-white tracking-tight">
            {currentPhrase.title}
          </h2>
        </div>

        {/* Central 3D Cyber Katana Canvas (Unsheathes Horizontally Across the Phrases) */}
        <div className="w-full max-w-4xl h-64 sm:h-80 z-20 flex items-center justify-center">
          <CyberKatana3D progress={journeyProgress} className="w-full h-full" />
        </div>

        {/* Dynamic Scam Quote Subtitle (Pure Typography, Zero Boxes) */}
        <div className="z-10 text-center max-w-2xl px-4 transition-all duration-300">
          <p className="font-display text-lg sm:text-2xl text-slate-300 font-normal leading-relaxed italic">
            {currentPhrase.quote}
          </p>

          <div className="mt-4 flex items-center justify-center gap-3 text-[10px] font-mono tracking-widest text-slate-500">
            <span>SCROLL PROGRESS: {Math.round(journeyProgress * 100)}%</span>
            <span>·</span>
            <span className={currentPhrase.color}>
              STAGE {activeIdx + 1} OF 5
            </span>
          </div>
        </div>

      </div>

    </div>
  );
}
