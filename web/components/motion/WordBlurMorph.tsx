"use client";

import React, { useState, useEffect, useRef } from "react";

const STAGES = [
  { word: "Impersonation", subtitle: "Spoofed police, CBI, or telecom authority credentials" },
  { word: "Coercion", subtitle: "Immediate arrest warrant & fabricated criminal charges" },
  { word: "Isolation", subtitle: "Strict demand to stay on call and lock physical room" },
  { word: "Liquidation", subtitle: "Urgent transfer demand to unverified 'Safe Escrow Vault'" },
  { word: "Neutralized", subtitle: "XORcall on-device kill chain intercepts before fund transfer" }
];

export default function WordBlurMorph() {
  const containerRef = useRef<HTMLDivElement>(null);
  const [activeStage, setActiveStage] = useState(0);

  useEffect(() => {
    const handleScroll = () => {
      if (!containerRef.current) return;
      const rect = containerRef.current.getBoundingClientRect();
      const windowH = window.innerHeight;
      
      // Calculate how far through this section we've scrolled (0 to 1)
      const totalH = rect.height;
      const topOffset = windowH * 0.5 - rect.top;
      const progress = Math.min(1, Math.max(0, topOffset / totalH));

      const stageIndex = Math.min(STAGES.length - 1, Math.floor(progress * STAGES.length));
      setActiveStage(stageIndex);
    };

    window.addEventListener("scroll", handleScroll, { passive: true });
    handleScroll();

    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <div ref={containerRef} className="w-full min-h-[70vh] flex flex-col items-center justify-center relative py-20">
      
      {/* Background Ghost Watermark */}
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none select-none overflow-hidden">
        <span className="text-[14vw] font-black text-black/[0.03] tracking-tighter uppercase whitespace-nowrap">
          LADDER OF INTENT
        </span>
      </div>

      {/* Center Giant Kinetic Word Blur-Morph (Accurate to Video frame_0481 & frame_0500) */}
      <div className="relative h-28 flex items-center justify-center z-10 w-full px-4">
        {STAGES.map((s, idx) => {
          const isActive = idx === activeStage;
          const dist = Math.abs(idx - activeStage);

          return (
            <div
              key={s.word}
              className="absolute text-center transition-all duration-500 will-change-transform will-change-filter"
              style={{
                opacity: isActive ? 1 : Math.max(0, 0.2 - dist * 0.1),
                filter: isActive ? "blur(0px)" : `blur(${dist * 12}px)`,
                transform: `scale(${isActive ? 1 : 0.9 - dist * 0.05}) translateY(${isActive ? 0 : (idx < activeStage ? -30 : 30)}px)`,
                pointerEvents: isActive ? "auto" : "none"
              }}
            >
              <h2 className={`text-6xl sm:text-8xl md:text-9xl font-black tracking-tight ${
                s.word === "Neutralized" ? "text-emerald-600" : "text-[#14151a]"
              }`}>
                {s.word}
              </h2>
            </div>
          );
        })}
      </div>

      {/* Subtitle explaining the currently active stage */}
      <div className="mt-8 text-center max-w-md px-4 z-10 transition-all duration-300">
        <span className="text-xs font-mono font-bold uppercase tracking-widest text-red-600">
          STAGE 0{activeStage + 1} OF 05
        </span>
        <p className="text-sm font-medium text-[#70717b] mt-1 leading-relaxed">
          {STAGES[activeStage].subtitle}
        </p>
      </div>

    </div>
  );
}
