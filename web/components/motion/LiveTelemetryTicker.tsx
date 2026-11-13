"use client";

import React, { useEffect, useState } from "react";
import { Activity, Radio, Cpu, ShieldCheck, Zap } from "lucide-react";

export function AnimatedCounter({
  target,
  duration = 1200,
  prefix = "",
  suffix = "",
  decimals = 0
}: {
  target: number;
  duration?: number;
  prefix?: string;
  suffix?: string;
  decimals?: number;
}) {
  const [count, setCount] = useState<number>(target);

  useEffect(() => {
    let start = performance.now();
    let initial = count;
    let animId: number;

    const tick = (now: number) => {
      const progress = Math.min((now - start) / duration, 1);
      // easeOutExpo
      const ease = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);
      const current = initial + (target - initial) * ease;
      setCount(current);

      if (progress < 1) {
        animId = requestAnimationFrame(tick);
      }
    };

    animId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(animId);
  }, [target, duration]);

  return (
    <span>
      {prefix}
      {count.toFixed(decimals)}
      {suffix}
    </span>
  );
}

const TELEMETRY_STREAM = [
  "LPU_INFERENCE: 184ms",
  "AUDIO_FRAME: 16kHz_PCM",
  "PROXIMITY_ADC: 0.0cm (EAR_TOUCH)",
  "CARRIER: PBX_SIP_INSPECT",
  "MULE_GATEWAY: SBI_ESCROW_TRACED",
  "DEEPFAKE_LAT_SCORE: 0.14_NOMINAL",
  "BSA_SECTION_63: COMPLIANT_DOSSIER",
  "I4C_1930_API: READY_T_ZERO",
  "FFT_HARMONICS: 44.1kHz_VERIFIED",
  "GROQ_LPU_STREAM: ACTIVE"
];

export function LiveTelemetryTicker() {
  const [index, setIndex] = useState(0);

  useEffect(() => {
    const intv = setInterval(() => {
      setIndex((i) => (i + 1) % TELEMETRY_STREAM.length);
    }, 2200);
    return () => clearInterval(intv);
  }, []);

  return (
    <div className="flex items-center gap-2 overflow-hidden py-1 px-3 bg-slate-950/70 border-y border-slate-900/90 text-[10px] font-mono select-none">
      <div className="flex items-center gap-1.5 text-cyan-400 font-bold shrink-0">
        <Activity className="w-3.5 h-3.5 animate-pulse" />
        <span>LIVE_TELEMETRY:</span>
      </div>

      <div className="flex items-center gap-4 text-slate-400 whitespace-nowrap overflow-x-hidden">
        <span className="text-white font-bold bg-cyan-950/60 border border-cyan-500/30 px-1.5 py-0.5 rounded transition-all duration-300">
          {TELEMETRY_STREAM[index]}
        </span>
        <span className="text-slate-600">|</span>
        <span>{TELEMETRY_STREAM[(index + 1) % TELEMETRY_STREAM.length]}</span>
        <span className="text-slate-600">|</span>
        <span className="hidden sm:inline">{TELEMETRY_STREAM[(index + 2) % TELEMETRY_STREAM.length]}</span>
        <span className="text-slate-600 hidden sm:inline">|</span>
        <span className="hidden md:inline text-emerald-400">STATUS: INTERCEPTOR_60FPS_ONLINE</span>
      </div>
    </div>
  );
}
