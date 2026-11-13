"use client";

import React, { useState } from "react";
import { ShieldCheck, Sparkles, X, ChevronRight, PhoneCall } from "lucide-react";

interface CompanionProps {
  isLightMode?: boolean;
}

export default function GuardianCompanion({ isLightMode = false }: CompanionProps) {
  const [isOpen, setIsOpen] = useState(true);
  const [step, setStep] = useState(0);

  const DIALOGS = [
    {
      text: "Hey defender! 🛡️ Welcome to XORcall. On-device Sherpa-ONNX is active with sub-300ms speed.",
      actionLabel: "Next",
      target: null
    },
    {
      text: "No police or court in India arrests anyone over video call or demands RTGS to safe escrow.",
      actionLabel: "Try Live Interceptor",
      target: "#interceptor"
    },
    {
      text: "Need urgent police cyber intervention? Dial 1930 to dispatch directly to National I4C Portal.",
      actionLabel: "Dial 1930",
      target: "tel:1930"
    }
  ];

  if (!isOpen) {
    return (
      <button
        onClick={() => setIsOpen(true)}
        className="fixed bottom-6 right-6 z-50 w-12 h-12 rounded-full bg-red-600 text-white flex items-center justify-center shadow-2xl hover:scale-110 transition-transform cursor-pointer"
        title="Open XORcall Assistant"
      >
        <ShieldCheck className="w-6 h-6" />
      </button>
    );
  }

  const currentDialog = DIALOGS[step % DIALOGS.length];

  return (
    <div className="fixed bottom-6 right-6 z-50 max-w-xs sm:max-w-sm flex items-end gap-3 pointer-events-auto animate-in fade-in slide-in-from-bottom-4 duration-300">
      
      {/* Speech Bubble (Exact Style from Video frame_0000 & frame_0300) */}
      <div 
        className={`p-4 rounded-2xl border shadow-2xl backdrop-blur-xl relative transition-all duration-300 ${
          isLightMode 
            ? "bg-[#ede8e3]/95 text-[#14151a] border-black/15 shadow-black/10" 
            : "bg-[#181920]/95 text-white border-white/15 shadow-black/60"
        }`}
      >
        <button
          onClick={() => setIsOpen(false)}
          className="absolute top-2 right-2 p-1 text-slate-400 hover:text-white transition-colors cursor-pointer"
        >
          <X className="w-3.5 h-3.5" />
        </button>

        <p className="text-xs leading-relaxed pr-4 font-medium">
          {currentDialog.text}
        </p>

        <div className="mt-3 flex items-center justify-between pt-2 border-t border-black/5 dark:border-white/10">
          <span className="text-[10px] font-mono opacity-50">
            KAVACH GUARDIAN v2.0
          </span>

          {currentDialog.target ? (
            <a
              href={currentDialog.target}
              className="text-[11px] font-mono font-bold text-red-600 dark:text-red-400 hover:underline flex items-center gap-1 cursor-pointer"
            >
              <span>{currentDialog.actionLabel}</span>
              <ChevronRight className="w-3 h-3" />
            </a>
          ) : (
            <button
              onClick={() => setStep((s) => s + 1)}
              className="text-[11px] font-mono font-bold text-cyan-600 dark:text-cyan-400 hover:underline flex items-center gap-1 cursor-pointer"
            >
              <span>{currentDialog.actionLabel}</span>
              <ChevronRight className="w-3 h-3" />
            </button>
          )}
        </div>
      </div>

      {/* Avatar Icon (Video Accurate) */}
      <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-red-600 to-rose-500 flex items-center justify-center text-white shadow-xl shrink-0 hover:scale-105 transition-transform">
        <ShieldCheck className="w-6 h-6" />
      </div>

    </div>
  );
}
