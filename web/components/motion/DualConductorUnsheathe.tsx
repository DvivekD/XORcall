"use client";

import React, { useEffect, useState, useRef } from "react";
import CyberHologramShield3D from "@/components/motion/CyberHologramShield3D";
import { ShieldAlert, Zap, Lock, Radio } from "lucide-react";

export default function DualConductorUnsheathe() {
  const containerRef = useRef<HTMLDivElement>(null);
  const [splitProgress, setSplitProgress] = useState(0);

  useEffect(() => {
    const handleScroll = () => {
      if (!containerRef.current) return;
      const rect = containerRef.current.getBoundingClientRect();
      const windowH = window.innerHeight;
      
      // Compute split progress based on viewport position
      // When top reaches 0, progress starts moving from 0 to 1
      const totalDist = windowH * 0.9;
      const current = windowH - rect.top;
      const rawProgress = current / totalDist;
      const clamped = Math.min(1, Math.max(0, rawProgress));
      setSplitProgress(clamped);
    };

    window.addEventListener("scroll", handleScroll, { passive: true });
    handleScroll();

    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Compute smooth horizontal offsets
  // Left scabbard glides up to -140px, right blade glides up to +140px
  const leftX = -(splitProgress * 130);
  const rightX = splitProgress * 130;
  const coreOpacity = Math.min(1, Math.max(0, (splitProgress - 0.15) * 1.4));
  const coreScale = 0.8 + splitProgress * 0.3;

  return (
    <div ref={containerRef} className="w-full max-w-5xl mx-auto py-12 px-4 relative flex flex-col items-center">
      {/* Background Panoramic Graphic / Mountain Aurora (Accurate to Video frame_0300) */}
      <div 
        className="w-full h-44 sm:h-64 rounded-3xl overflow-hidden relative border border-white/10 bg-gradient-to-b from-[#181920] to-[#0f1014] flex items-center justify-center shadow-2xl"
        style={{
          boxShadow: `0 20px 50px rgba(0,0,0,0.8), 0 0 ${splitProgress * 40}px rgba(6, 182, 212, 0.2)`
        }}
      >
        {/* Subtle Cyber Grid & Particle Field inside letterbox */}
        <div className="absolute inset-0 opacity-20 bg-[radial-gradient(#38bdf8_1px,transparent_1px)] [background-size:16px_16px]" />

        {/* Center Revealed Core: 3D Holographic Particle Shield (Appears as conductor unsheathes) */}
        <div 
          className="absolute z-10 flex flex-col items-center justify-center transition-opacity duration-300 pointer-events-none"
          style={{
            opacity: coreOpacity,
            transform: `scale(${coreScale})`
          }}
        >
          <div className="w-40 h-40 relative flex items-center justify-center">
            <CyberHologramShield3D 
              riskScore={0.85}
              level={splitProgress > 0.6 ? "CRITICAL" : "WARN"}
              isAudioActive={true}
              className="w-full h-full"
            />
          </div>
          <div className="text-[10px] font-mono tracking-widest text-cyan-400 font-bold uppercase mt-1 bg-cyan-950/80 px-3 py-0.5 rounded-full border border-cyan-500/40">
            [ BSA FORENSIC CORE EXPOSED ]
          </div>
        </div>

        {/* Dual Conductor Beams (Horizontal Unsheathe Physics) */}
        <div className="relative w-full max-w-3xl flex items-center justify-between z-20 px-4">
          
          {/* Left Conductor (The Scam Pretext & Caller Vector) */}
          <div 
            className="flex items-center gap-3 bg-[#13141a]/95 border border-red-500/30 rounded-2xl p-4 shadow-xl backdrop-blur-md transition-transform duration-75 will-change-transform"
            style={{
              transform: `translate3d(${leftX}px, 0, 0)`
            }}
          >
            <div className="w-8 h-8 rounded-xl bg-red-950 border border-red-500/50 flex items-center justify-center text-red-400">
              <Radio className="w-4 h-4 animate-pulse" />
            </div>
            <div>
              <div className="text-[10px] font-mono text-red-400 font-bold uppercase tracking-wider">
                THREAT VECTOR
              </div>
              <div className="text-xs font-mono text-white font-bold">
                +92 VOIP SPOOF // DIGITAL ARREST
              </div>
            </div>
          </div>

          {/* Right Conductor (XORcall Neural Shield & Zipformer Receptors) */}
          <div 
            className="flex items-center gap-3 bg-[#13141a]/95 border border-cyan-500/30 rounded-2xl p-4 shadow-xl backdrop-blur-md transition-transform duration-75 will-change-transform"
            style={{
              transform: `translate3d(${rightX}px, 0, 0)`
            }}
          >
            <div>
              <div className="text-[10px] font-mono text-cyan-400 font-bold uppercase tracking-wider text-right">
                AUTONOMOUS DEFENSE
              </div>
              <div className="text-xs font-mono text-white font-bold text-right">
                SHERPA-ONNX // ZERO-TOUCH INTERCEPT
              </div>
            </div>
            <div className="w-8 h-8 rounded-xl bg-cyan-950 border border-cyan-500/50 flex items-center justify-center text-cyan-400">
              <Zap className="w-4 h-4 fill-cyan-400" />
            </div>
          </div>

        </div>

      </div>

      {/* Micro instructions under the unsheathe showcase */}
      <div className="mt-3 text-[10px] font-mono tracking-widest text-slate-500 flex items-center gap-3">
        <span>SCROLL TO UNSHEATHE DEFENSE</span>
        <span>·</span>
        <span className="text-cyan-400 font-bold">{Math.round(splitProgress * 100)}% DISRUPTED</span>
      </div>
    </div>
  );
}
