"use client";

import React, { useState, useEffect } from "react";

interface TelemetryHUDProps {
  isLightMode?: boolean;
}

export default function TelemetryHUD({ isLightMode = false }: TelemetryHUDProps) {
  const [scrollPercent, setScrollPercent] = useState(0);
  const [scrollTop, setScrollTop] = useState(0);
  const [fps, setFps] = useState(60);

  useEffect(() => {
    let frameCount = 0;
    let lastTime = performance.now();
    let animId: number;

    const measureFps = (now: number) => {
      frameCount++;
      if (now - lastTime >= 1000) {
        setFps(Math.min(60, Math.round((frameCount * 1000) / (now - lastTime))));
        frameCount = 0;
        lastTime = now;
      }
      animId = requestAnimationFrame(measureFps);
    };
    animId = requestAnimationFrame(measureFps);

    const handleScroll = () => {
      const top = window.scrollY || document.documentElement.scrollTop;
      const height = document.documentElement.scrollHeight - window.innerHeight;
      const pct = height > 0 ? Math.min(100, Math.max(0, Math.round((top / height) * 100))) : 0;
      setScrollPercent(pct);
      setScrollTop(Math.round(top));
    };

    window.addEventListener("scroll", handleScroll, { passive: true });
    handleScroll();

    return () => {
      cancelAnimationFrame(animId);
      window.removeEventListener("scroll", handleScroll);
    };
  }, []);

  return (
    <>
      {/* Top Right Scroll Percentage Indicator (Video Accurate) */}
      <div 
        className={`fixed top-5 right-6 z-50 text-xs font-mono font-bold tracking-wider transition-colors duration-300 ${
          isLightMode ? "text-[#14151a]" : "text-white/80"
        }`}
      >
        {scrollPercent}%
      </div>

      {/* Left Edge Sticky Telemetry Pill (Video Accurate: FPS & TOP PX) */}
      <div 
        className={`fixed left-4 top-1/2 -translate-y-1/2 z-40 hidden lg:flex flex-col items-center gap-1.5 text-[9px] font-mono tracking-widest px-2 py-3 rounded-lg border backdrop-blur-md transition-all duration-300 ${
          isLightMode 
            ? "bg-black/5 text-[#14151a]/60 border-black/10" 
            : "bg-white/5 text-white/50 border-white/10"
        }`}
        style={{ writingMode: "vertical-rl", transform: "translateY(-50%) rotate(180deg)" }}
      >
        <span>FPS : {fps}</span>
        <span className="opacity-40">·</span>
        <span>TOP : {scrollTop} PX</span>
      </div>
    </>
  );
}
