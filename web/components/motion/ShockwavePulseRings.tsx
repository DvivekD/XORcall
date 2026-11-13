"use client";

import React from "react";

interface ShockwaveProps {
  threatLevel?: "SAFE" | "WATCH" | "WARN" | "CRITICAL";
  isVibrating?: boolean;
  className?: string;
  size?: number;
}

export default function ShockwavePulseRings({
  threatLevel = "SAFE",
  isVibrating = false,
  className = "",
  size = 180
}: ShockwaveProps) {
  const isCrit = threatLevel === "CRITICAL";
  const isWarn = threatLevel === "WARN";

  const borderColor = isCrit
    ? "border-red-500 shadow-red-500/50"
    : isWarn
    ? "border-amber-500 shadow-amber-500/50"
    : "border-cyan-500 shadow-cyan-500/50";

  return (
    <div 
      className={`relative flex items-center justify-center pointer-events-none select-none ${className}`}
      style={{ width: size, height: size }}
    >
      {/* Dynamic Concentric Shockwave Rings */}
      <div 
        className={`absolute inset-0 rounded-full border ${borderColor} animate-shockwave-1`} 
      />
      <div 
        className={`absolute inset-0 rounded-full border ${borderColor} animate-shockwave-2`} 
      />
      <div 
        className={`absolute inset-0 rounded-full border ${borderColor} animate-shockwave-3`} 
      />

      {/* Core Emitter Node */}
      <div className={`relative z-10 rounded-full flex items-center justify-center p-3 border transition-all ${
        isCrit
          ? "bg-red-950/80 border-red-500 text-red-300 shadow-lg shadow-red-500/40"
          : isWarn
          ? "bg-amber-950/80 border-amber-500 text-amber-300 shadow-lg shadow-amber-500/40"
          : "bg-cyan-950/80 border-cyan-500 text-cyan-300 shadow-lg shadow-cyan-500/40"
      }`}>
        <div className={`w-3.5 h-3.5 rounded-full ${
          isCrit ? "bg-red-400 animate-ping" : isWarn ? "bg-amber-400 animate-pulse" : "bg-cyan-400"
        }`} />
      </div>
    </div>
  );
}
