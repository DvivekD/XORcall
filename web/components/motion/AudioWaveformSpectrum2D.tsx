"use client";

import React, { useEffect, useRef } from "react";

interface AudioWaveformProps {
  isPlaying?: boolean;
  threatLevel?: "SAFE" | "WATCH" | "WARN" | "CRITICAL";
  className?: string;
}

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  alpha: number;
  size: number;
}

export default function AudioWaveformSpectrum2D({
  isPlaying = false,
  threatLevel = "SAFE",
  className = ""
}: AudioWaveformProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef({ isPlaying, threatLevel });

  useEffect(() => {
    stateRef.current.isPlaying = isPlaying;
    stateRef.current.threatLevel = threatLevel;
  }, [isPlaying, threatLevel]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animId: number;
    let tick = 0;
    const numBars = 48;
    const barHeights = new Float32Array(numBars);
    const particles: Particle[] = [];

    const render = () => {
      animId = requestAnimationFrame(render);
      tick += 0.05;

      const dpr = window.devicePixelRatio || 1;
      const width = canvas.clientWidth;
      const height = canvas.clientHeight;
      if (canvas.width !== width * dpr || canvas.height !== height * dpr) {
        canvas.width = width * dpr;
        canvas.height = height * dpr;
      }

      ctx.save();
      ctx.scale(dpr, dpr);
      ctx.clearRect(0, 0, width, height);

      const isAudio = stateRef.current.isPlaying;
      const level = stateRef.current.threatLevel;

      // Color Palette
      const isCrit = level === "CRITICAL";
      const isWarn = level === "WARN";
      const gradient = ctx.createLinearGradient(0, height, 0, 0);
      if (isCrit) {
        gradient.addColorStop(0, "rgba(239, 68, 68, 0.2)");
        gradient.addColorStop(0.6, "rgba(244, 63, 94, 0.85)");
        gradient.addColorStop(1, "rgba(255, 255, 255, 0.95)");
      } else if (isWarn) {
        gradient.addColorStop(0, "rgba(245, 158, 11, 0.2)");
        gradient.addColorStop(0.6, "rgba(251, 146, 60, 0.85)");
        gradient.addColorStop(1, "rgba(254, 240, 138, 0.95)");
      } else {
        gradient.addColorStop(0, "rgba(6, 182, 212, 0.2)");
        gradient.addColorStop(0.6, "rgba(56, 189, 248, 0.85)");
        gradient.addColorStop(1, "rgba(207, 250, 254, 0.95)");
      }

      const barWidth = Math.max(2, (width / numBars) - 2.5);
      const centerY = height * 0.72;

      // Update Bar Heights with procedural audio simulation
      for (let i = 0; i < numBars; i++) {
        const norm = i / numBars;
        // Bell curve envelope
        const envelope = Math.sin(norm * Math.PI);

        let targetH = 4;
        if (isAudio) {
          const wave1 = Math.sin(tick * 3 + i * 0.35) * 0.5 + 0.5;
          const wave2 = Math.cos(tick * 5 - i * 0.6) * 0.3 + 0.3;
          const jitter = Math.random() * 0.25;
          targetH = (wave1 * 0.6 + wave2 * 0.4 + jitter) * (height * 0.7) * envelope + 6;
        } else {
          // Subtle standby idle pulse
          targetH = (Math.sin(tick * 1.5 + i * 0.2) * 0.3 + 0.35) * (height * 0.18) * envelope + 3;
        }

        // Smooth dampening
        barHeights[i] += (targetH - barHeights[i]) * 0.2;

        const x = i * (barWidth + 2.5);
        const h = barHeights[i];

        // Draw Spectrum Bar
        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.roundRect(x, centerY - h, barWidth, h, [2, 2, 0, 0]);
        ctx.fill();

        // Mirrored bottom reflection
        ctx.fillStyle = isCrit
          ? "rgba(239, 68, 68, 0.12)"
          : isWarn
          ? "rgba(245, 158, 11, 0.12)"
          : "rgba(6, 182, 212, 0.12)";
        ctx.beginPath();
        ctx.roundRect(x, centerY + 2, barWidth, h * 0.3, [0, 0, 2, 2]);
        ctx.fill();

        // Spawn glowing particles at high bar peaks
        if (isAudio && h > height * 0.45 && Math.random() < 0.25) {
          particles.push({
            x: x + barWidth / 2,
            y: centerY - h,
            vx: (Math.random() - 0.5) * 1.2,
            vy: -Math.random() * 2 - 1,
            alpha: 1.0,
            size: Math.random() * 2.2 + 1
          });
        }
      }

      // Base line
      ctx.strokeStyle = isCrit ? "rgba(239, 68, 68, 0.4)" : "rgba(6, 182, 212, 0.4)";
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(0, centerY);
      ctx.lineTo(width, centerY);
      ctx.stroke();

      // Render Floating Physics Particles
      for (let p = particles.length - 1; p >= 0; p--) {
        const pt = particles[p];
        pt.x += pt.vx;
        pt.y += pt.vy;
        pt.alpha -= 0.025;

        if (pt.alpha <= 0) {
          particles.splice(p, 1);
          continue;
        }

        ctx.fillStyle = isCrit
          ? `rgba(255, 120, 120, ${pt.alpha})`
          : `rgba(180, 240, 255, ${pt.alpha})`;
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, pt.size, 0, Math.PI * 2);
        ctx.fill();
      }

      ctx.restore();
    };

    render();

    return () => {
      cancelAnimationFrame(animId);
    };
  }, []);

  return (
    <div className={`relative w-full h-[110px] rounded-xl overflow-hidden glass-panel border border-slate-800 ${className}`}>
      <canvas ref={canvasRef} className="w-full h-full" />
      <div className="absolute top-2 left-3 right-3 flex items-center justify-between text-[9px] font-mono pointer-events-none text-slate-400">
        <span className="flex items-center gap-1.5">
          <span className={`w-1.5 h-1.5 rounded-full ${isPlaying ? "bg-cyan-400 animate-ping" : "bg-slate-600"}`} />
          SPECTRAL_FFT_STREAM
        </span>
        <span className={isPlaying ? "text-cyan-400 font-bold" : "text-slate-500"}>
          {isPlaying ? "SAMPLING AUDIO LIVE" : "AWAITING IN-CALL AUDIO"}
        </span>
      </div>
    </div>
  );
}
