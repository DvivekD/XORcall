"use client";

import React from "react";

interface CyberOctagonMedallionProps {
  progress?: number; // 0.0 to 1.0
  activePhaseIdx?: number; // 0 to 4
  className?: string;
}

const PHASES = [
  { id: "01", name: "IDENTIFY", subtitle: "PRETEXT VECTOR", x: 200.5, y: 59.8, textAnchor: "end", dx: -12, dy: -6 },
  { id: "02", name: "PROTECT", subtitle: "ZERO-TRUST VAULT", x: 399.5, y: 59.8, textAnchor: "start", dx: 12, dy: -6 },
  { id: "03", name: "DETECT", subtitle: "ZIPFORMER ASR", x: 540.2, y: 200.5, textAnchor: "start", dx: 14, dy: 4 },
  { id: "04", name: "RESPOND", subtitle: "SEVER CARRIER", x: 540.2, y: 399.5, textAnchor: "start", dx: 14, dy: 6 },
  { id: "05", name: "RECOVER", subtitle: "1930 DOSSIER", x: 399.5, y: 540.2, textAnchor: "start", dx: 12, dy: 18 }
];

export default function CyberOctagonMedallion({
  progress = 0,
  activePhaseIdx = 0,
  className = ""
}: CyberOctagonMedallionProps) {
  // Compute active phase index if not explicitly provided
  const activeIdx = activePhaseIdx ?? Math.min(PHASES.length - 1, Math.floor(progress * PHASES.length));

  return (
    <div className={`relative flex items-center justify-center select-none pointer-events-none ${className}`}>
      
      {/* Background Ambient Radial Glow */}
      <div 
        className="absolute w-[500px] h-[500px] rounded-full blur-3xl opacity-25 transition-colors duration-700 pointer-events-none"
        style={{
          background: activeIdx === 4
            ? "radial-gradient(circle, rgba(16, 185, 129, 0.25) 0%, transparent 70%)"
            : activeIdx >= 2
            ? "radial-gradient(circle, rgba(225, 29, 72, 0.28) 0%, rgba(56, 189, 248, 0.15) 50%, transparent 70%)"
            : "radial-gradient(circle, rgba(56, 189, 248, 0.22) 0%, transparent 70%)"
        }}
      />

      {/* SVG Geometric Medallion */}
      <svg
        viewBox="0 0 600 600"
        className="w-[380px] h-[380px] sm:w-[480px] sm:h-[480px] md:w-[560px] md:h-[560px] text-white/20 transition-transform duration-700"
        style={{
          transform: `scale(${1 + progress * 0.05}) rotate(${progress * 12}deg)`
        }}
      >
        <defs>
          {/* Subtle Cyber Gradients */}
          <linearGradient id="medallionStroke" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#e11d48" stopOpacity="0.4" />
            <stop offset="50%" stopColor="#38bdf8" stopOpacity="0.25" />
            <stop offset="100%" stopColor="#e11d48" stopOpacity="0.4" />
          </linearGradient>

          <linearGradient id="activeGlow" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#e11d48" />
            <stop offset="100%" stopColor="#38bdf8" />
          </linearGradient>

          <filter id="cyberGlow" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="3" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>
        </defs>

        {/* Outer Circular Reference Ticks */}
        <circle
          cx="300"
          cy="300"
          r="285"
          fill="none"
          stroke="rgba(255, 255, 255, 0.04)"
          strokeWidth="1"
          strokeDasharray="2 8"
        />

        {/* 1. Outer Octagon - Dim Slate Fill & Hairline Border */}
        <polygon
          points="540.2,200.5 540.2,399.5 399.5,540.2 200.5,540.2 59.8,399.5 59.8,200.5 200.5,59.8 399.5,59.8"
          fill="#14151a"
          fillOpacity="0.65"
          stroke="url(#medallionStroke)"
          strokeWidth="1"
        />

        {/* 2. Middle Octagon */}
        <polygon
          points="484.8,223.5 484.8,376.5 376.5,484.8 223.5,484.8 115.2,376.5 115.2,223.5 223.5,115.2 376.5,115.2"
          fill="none"
          stroke="rgba(255, 255, 255, 0.06)"
          strokeWidth="0.8"
          strokeDasharray="4 6"
        />

        {/* 3. Inner Octagon */}
        <polygon
          points="429.3,246.4 429.3,353.6 353.6,429.3 246.4,429.3 170.7,353.6 170.7,246.4 246.4,170.7 353.6,170.7"
          fill="#0c0d11"
          fillOpacity="0.4"
          stroke="rgba(56, 189, 248, 0.15)"
          strokeWidth="0.8"
        />

        {/* 4. Diagonal Octet Spokes (Tree Graph Connectivity) */}
        <line x1="200.5" y1="59.8" x2="399.5" y2="540.2" stroke="rgba(255, 255, 255, 0.03)" strokeWidth="0.8" />
        <line x1="399.5" y1="59.8" x2="200.5" y2="540.2" stroke="rgba(255, 255, 255, 0.03)" strokeWidth="0.8" />
        <line x1="59.8" y1="200.5" x2="540.2" y2="399.5" stroke="rgba(255, 255, 255, 0.03)" strokeWidth="0.8" />
        <line x1="59.8" y1="399.5" x2="540.2" y2="200.5" stroke="rgba(255, 255, 255, 0.03)" strokeWidth="0.8" />

        {/* 5. Center Circle & NIST Cyber Shield Reticle */}
        <circle
          cx="300"
          cy="300"
          r="82"
          fill="none"
          stroke="rgba(255, 255, 255, 0.08)"
          strokeWidth="1"
        />
        <circle
          cx="300"
          cy="300"
          r="78"
          fill="none"
          stroke="rgba(225, 29, 72, 0.2)"
          strokeWidth="1"
          strokeDasharray="6 4"
        />

        {/* Center Typography (StringTune Style) */}
        <text
          x="300"
          y="288"
          textAnchor="middle"
          className="font-mono text-[9px] fill-slate-400 tracking-[0.25em] uppercase font-bold"
        >
          NIST CSF 2.0
        </text>
        <text
          x="300"
          y="304"
          textAnchor="middle"
          className="font-mono text-[11px] fill-white tracking-[0.3em] uppercase font-black"
        >
          ( 8 ) OCTET
        </text>
        <text
          x="300"
          y="318"
          textAnchor="middle"
          className="font-mono text-[8px] fill-cyan-400/80 tracking-[0.2em] uppercase"
        >
          AUTONOMOUS CYCLE
        </text>

        {/* 6. Active Progression Connecting Edge Trail */}
        {PHASES.map((p, idx) => {
          if (idx === PHASES.length - 1) return null;
          const next = PHASES[idx + 1];
          const isPassed = activeIdx > idx;
          const isCurrent = activeIdx === idx;
          return (
            <line
              key={`edge-${p.id}`}
              x1={p.x}
              y1={p.y}
              x2={next.x}
              y2={next.y}
              stroke={isCurrent ? "url(#activeGlow)" : isPassed ? "rgba(56, 189, 248, 0.4)" : "rgba(255, 255, 255, 0.05)"}
              strokeWidth={isCurrent ? 2 : 1}
              strokeDasharray={isCurrent ? "none" : "3 3"}
              filter={isCurrent ? "url(#cyberGlow)" : undefined}
            />
          );
        })}

        {/* 7. The 5 NIST Phases Nodes and Labels */}
        {PHASES.map((phase, idx) => {
          const isActive = activeIdx === idx;
          const isPassed = activeIdx > idx;

          return (
            <g key={phase.id} className="transition-all duration-300">
              
              {/* Outer Glow Ring for Active Node */}
              {isActive && (
                <circle
                  cx={phase.x}
                  cy={phase.y}
                  r="14"
                  fill="none"
                  stroke="#e11d48"
                  strokeWidth="1.5"
                  strokeOpacity="0.8"
                  filter="url(#cyberGlow)"
                  className="animate-pulse"
                />
              )}

              {/* Node Center Dot */}
              <circle
                cx={phase.x}
                cy={phase.y}
                r={isActive ? "6" : "3.5"}
                fill={isActive ? "#e11d48" : isPassed ? "#38bdf8" : "#2d313d"}
                stroke={isActive ? "#ffffff" : isPassed ? "#38bdf8" : "rgba(255,255,255,0.2)"}
                strokeWidth={isActive ? "2" : "1"}
              />

              {/* Phase Title Text */}
              <text
                x={phase.x + phase.dx}
                y={phase.y + phase.dy}
                textAnchor={phase.textAnchor as any}
                className={`font-mono text-[10px] tracking-[0.18em] uppercase transition-colors duration-300 ${
                  isActive
                    ? "fill-white font-black"
                    : isPassed
                    ? "fill-cyan-300/80 font-bold"
                    : "fill-slate-500 font-medium"
                }`}
              >
                {phase.id} // {phase.name}
              </text>

              {/* Subtitle / Cyber Capability */}
              <text
                x={phase.x + phase.dx}
                y={phase.y + phase.dy + 12}
                textAnchor={phase.textAnchor as any}
                className={`font-mono text-[7.5px] tracking-wider transition-colors duration-300 ${
                  isActive ? "fill-rose-400/90 font-bold" : "fill-slate-600"
                }`}
              >
                {phase.subtitle}
              </text>

            </g>
          );
        })}

        {/* Subtle Cyber Reticle Corner Accents */}
        <path d="M 60,60 L 75,60 M 60,60 L 60,75" stroke="rgba(255,255,255,0.1)" strokeWidth="1" fill="none" />
        <path d="M 540,60 L 525,60 M 540,60 L 540,75" stroke="rgba(255,255,255,0.1)" strokeWidth="1" fill="none" />
        <path d="M 60,540 L 75,540 M 60,540 L 60,525" stroke="rgba(255,255,255,0.1)" strokeWidth="1" fill="none" />
        <path d="M 540,540 L 525,540 M 540,540 L 540,525" stroke="rgba(255,255,255,0.1)" strokeWidth="1" fill="none" />

      </svg>
    </div>
  );
}
