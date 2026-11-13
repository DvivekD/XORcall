"use client";

import React, { useState } from "react";
import { X, ChevronRight, PhoneCall, ShieldAlert, Sparkles } from "lucide-react";

interface CompanionProps {
  isLightMode?: boolean;
}

export default function GuardianCompanion({ isLightMode = false }: CompanionProps) {
  const [isOpen, setIsOpen] = useState(true);
  const [step, setStep] = useState(0);

  const DIALOGS = [
    {
      badge: "ON-DEVICE SENTINEL",
      text: "Zipformer acoustic model active. Zero milliseconds audio sent to cloud. All inference stays local on your handset.",
      actionLabel: "Try Live Interceptor",
      target: "#interceptor"
    },
    {
      badge: "LEGAL GROUNDING",
      text: "CBI, Customs, or Mumbai Police NEVER arrest citizens over WhatsApp / Skype video calls or ask for RTGS supervision funds.",
      actionLabel: "View Kill Chain",
      target: "#journey"
    },
    {
      badge: "EMERGENCY DISPATCH",
      text: "Under active extortion? Dial 1930 immediately to freeze fraudulent mule accounts across Indian banks.",
      actionLabel: "Dial 1930",
      target: "tel:1930"
    }
  ];

  const currentDialog = DIALOGS[step % DIALOGS.length];

  // Pixel Art Sentinel Icon (SVG Pixel Matrix 16x16)
  const PixelSentinel = () => (
    <div className="relative w-12 h-12 rounded-2xl bg-[#14151a] border border-white/20 flex items-center justify-center shadow-xl group cursor-pointer overflow-hidden">
      {/* Background Pixel Glow */}
      <div className="absolute inset-0 bg-gradient-to-tr from-[#e11d48]/40 to-[#38bdf8]/40 opacity-70 group-hover:opacity-100 transition-opacity" />
      
      {/* 8-bit Pixel Robot/Sentinel Glyph */}
      <svg
        viewBox="0 0 16 16"
        className="w-7 h-7 relative z-10 fill-white drop-shadow-[0_0_4px_rgba(56,189,248,0.8)]"
        shapeRendering="crispEdges"
      >
        {/* Helmet / Head outline */}
        <rect x="5" y="1" width="6" height="1" fill="#38bdf8" />
        <rect x="4" y="2" width="8" height="1" fill="#38bdf8" />
        <rect x="3" y="3" width="10" height="2" fill="#ebedf2" />
        {/* Glowing Visor (Red / Cyan scanner) */}
        <rect x="4" y="5" width="8" height="2" fill="#e11d48" />
        <rect x="7" y="5" width="2" height="2" fill="#ffffff" />
        {/* Jaw / Filter */}
        <rect x="3" y="7" width="10" height="2" fill="#ebedf2" />
        <rect x="5" y="9" width="6" height="1" fill="#38bdf8" />
        {/* Shoulders / Torso */}
        <rect x="2" y="10" width="12" height="2" fill="#0f1014" />
        <rect x="4" y="11" width="8" height="3" fill="#e11d48" />
        <rect x="6" y="12" width="4" height="2" fill="#ffffff" />
        {/* Base */}
        <rect x="3" y="14" width="10" height="1" fill="#38bdf8" />
      </svg>

      {/* Micro Status Ping Dot */}
      <span className="absolute top-1 right-1 w-2 h-2 rounded-full bg-emerald-400 ring-2 ring-[#14151a] animate-pulse" />
    </div>
  );

  if (!isOpen) {
    return (
      <button
        onClick={() => setIsOpen(true)}
        className="fixed bottom-6 right-6 z-50 transition-transform duration-300 hover:scale-110 cursor-pointer"
        title="Open XORcall Pixel Sentinel"
      >
        <PixelSentinel />
      </button>
    );
  }

  return (
    <div className="fixed bottom-6 right-6 z-50 max-w-xs sm:max-w-sm flex items-end gap-3 pointer-events-auto animate-in fade-in slide-in-from-bottom-4 duration-300">
      
      {/* Speech Bubble (Pixel Sentinel Companion) */}
      <div 
        className={`p-4 rounded-2xl border shadow-2xl backdrop-blur-xl relative transition-all duration-300 ${
          isLightMode 
            ? "bg-[#ede8e3]/95 text-[#14151a] border-black/15 shadow-black/10" 
            : "bg-[#14151a]/95 text-white border-white/15 shadow-black/80"
        }`}
      >
        {/* Close Button */}
        <button
          onClick={() => setIsOpen(false)}
          className="absolute top-2.5 right-2.5 p-1 text-slate-400 hover:text-white transition-colors cursor-pointer"
        >
          <X className="w-3.5 h-3.5" />
        </button>

        {/* Sentinel Title Header */}
        <div className="flex items-center gap-2 mb-2 pr-6">
          <span className="text-[9px] font-mono font-bold tracking-widest px-2 py-0.5 rounded bg-[#e11d48]/20 text-[#fb7185] border border-[#e11d48]/40">
            {currentDialog.badge}
          </span>
          <span className="text-[10px] font-mono text-cyan-400 font-bold">
            [ SENTINEL AI ]
          </span>
        </div>

        {/* Dialog Content */}
        <p className="text-xs leading-relaxed font-sans font-medium text-slate-200">
          {currentDialog.text}
        </p>

        {/* Action Controls */}
        <div className="mt-3 flex items-center justify-between pt-2 border-t border-white/10">
          <span className="text-[10px] font-mono text-slate-400">
            0{step % DIALOGS.length + 1} / 0{DIALOGS.length}
          </span>

          <div className="flex items-center gap-2">
            {currentDialog.target ? (
              <a
                href={currentDialog.target}
                className="text-[11px] font-mono font-bold text-[#e11d48] hover:text-[#fb7185] hover:underline flex items-center gap-0.5 cursor-pointer"
              >
                <span>{currentDialog.actionLabel}</span>
                <ChevronRight className="w-3 h-3" />
              </a>
            ) : null}

            <button
              onClick={() => setStep((s) => s + 1)}
              className="text-[11px] font-mono font-bold text-cyan-400 hover:text-cyan-300 hover:underline flex items-center gap-0.5 cursor-pointer ml-2"
            >
              <span>Next</span>
              <ChevronRight className="w-3 h-3" />
            </button>
          </div>
        </div>
      </div>

      {/* Pixel Sentinel Avatar */}
      <button
        onClick={() => setStep((s) => s + 1)}
        className="transition-transform duration-300 hover:scale-105 cursor-pointer shrink-0"
        title="Cycle Companion Advisory"
      >
        <PixelSentinel />
      </button>

    </div>
  );
}
