"use client";

import React, { useEffect, useRef } from "react";

interface ThreatRadarProps {
  threatLevel?: "SAFE" | "WATCH" | "WARN" | "CRITICAL";
  riskScore?: number;
  className?: string;
}

interface Blip {
  angle: number; // in radians
  dist: number;  // 0 to 1
  label: string;
  type: "CRIT" | "WARN" | "INFO";
  lastHit: number; // timestamp
}

export default function ThreatRadar2D({
  threatLevel = "SAFE",
  riskScore = 0.05,
  className = ""
}: ThreatRadarProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef({ threatLevel, riskScore });

  useEffect(() => {
    stateRef.current.threatLevel = threatLevel;
    stateRef.current.riskScore = riskScore;
  }, [threatLevel, riskScore]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animId: number;
    let angle = 0;

    const blips: Blip[] = [
      { angle: 0.8, dist: 0.65, label: "MULE_ACC_SBI", type: "CRIT", lastHit: 0 },
      { angle: 2.1, dist: 0.82, label: "PBX_VOIP_SPOOF", type: "WARN", lastHit: 0 },
      { angle: 3.7, dist: 0.42, label: "DEEPFAKE_VEC", type: "CRIT", lastHit: 0 },
      { angle: 5.2, dist: 0.55, label: "SIM_CARRIER_IN", type: "INFO", lastHit: 0 }
    ];

    const render = () => {
      animId = requestAnimationFrame(render);

      const dpr = window.devicePixelRatio || 1;
      const width = canvas.clientWidth;
      const height = canvas.clientHeight;
      if (canvas.width !== width * dpr || canvas.height !== height * dpr) {
        canvas.width = width * dpr;
        canvas.height = height * dpr;
      }

      ctx.save();
      ctx.scale(dpr, dpr);

      const cx = width / 2;
      const cy = height / 2;
      const radius = Math.min(cx, cy) - 16;

      ctx.clearRect(0, 0, width, height);

      // Radar Background Dark Tint
      ctx.fillStyle = "rgba(6, 12, 24, 0.45)";
      ctx.beginPath();
      ctx.arc(cx, cy, radius, 0, Math.PI * 2);
      ctx.fill();

      // Theme Colors
      const isCrit = stateRef.current.threatLevel === "CRITICAL";
      const isWarn = stateRef.current.threatLevel === "WARN";
      const mainColor = isCrit ? "#ef4444" : isWarn ? "#f59e0b" : "#06b6d4";
      const mainRgb = isCrit ? "239, 68, 68" : isWarn ? "245, 158, 11" : "6, 182, 212";

      // Concentric Distance Circles
      ctx.strokeStyle = `rgba(${mainRgb}, 0.25)`;
      ctx.lineWidth = 1;
      for (let i = 1; i <= 4; i++) {
        ctx.beginPath();
        ctx.arc(cx, cy, (radius * i) / 4, 0, Math.PI * 2);
        ctx.stroke();
      }

      // Crosshair Axes
      ctx.beginPath();
      ctx.moveTo(cx - radius, cy);
      ctx.lineTo(cx + radius, cy);
      ctx.moveTo(cx, cy - radius);
      ctx.lineTo(cx, cy + radius);
      ctx.strokeStyle = `rgba(${mainRgb}, 0.2)`;
      ctx.stroke();

      // Cardinal Degree Labels
      ctx.fillStyle = `rgba(${mainRgb}, 0.6)`;
      ctx.font = "9px monospace";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText("000°", cx, cy - radius + 8);
      ctx.fillText("090°", cx + radius - 14, cy);
      ctx.fillText("180°", cx, cy + radius - 8);
      ctx.fillText("270°", cx - radius + 14, cy);

      // Rotating Sweep Beam with Fading Phosphor Trail
      const sweepSpeed = isCrit ? 0.055 : 0.035;
      angle = (angle + sweepSpeed) % (Math.PI * 2);

      const trailSegments = 24;
      for (let i = 0; i < trailSegments; i++) {
        const segAngle = angle - (i * 0.025);
        const alpha = (1 - i / trailSegments) * 0.25;
        ctx.fillStyle = `rgba(${mainRgb}, ${alpha})`;
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.arc(cx, cy, radius, segAngle - 0.025, segAngle);
        ctx.closePath();
        ctx.fill();
      }

      // Sharp Leading Edge Line
      ctx.strokeStyle = `rgba(${mainRgb}, 0.9)`;
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.lineTo(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
      ctx.stroke();

      // Check blip hits & render targets
      const now = performance.now();
      blips.forEach((blip) => {
        // Difference between sweep angle and blip angle
        let diff = (angle - blip.angle + Math.PI * 2) % (Math.PI * 2);
        if (diff < 0.08) {
          blip.lastHit = now;
        }

        const elapsedSinceHit = now - blip.lastHit;
        const decay = Math.max(0, 1 - elapsedSinceHit / 2400);

        const bx = cx + Math.cos(blip.angle) * (blip.dist * radius);
        const by = cy + Math.sin(blip.angle) * (blip.dist * radius);

        // Render Blip
        const blipColor = blip.type === "CRIT" ? "#ef4444" : blip.type === "WARN" ? "#f59e0b" : "#38bdf8";
        ctx.fillStyle = blipColor;
        ctx.beginPath();
        ctx.arc(bx, by, 3 + decay * 2, 0, Math.PI * 2);
        ctx.fill();

        if (decay > 0.05) {
          // Blip Expanding Ripple
          ctx.strokeStyle = blipColor;
          ctx.lineWidth = 1;
          ctx.globalAlpha = decay;
          ctx.beginPath();
          ctx.arc(bx, by, 6 + (1 - decay) * 14, 0, Math.PI * 2);
          ctx.stroke();

          // Blip Micro-label
          ctx.font = "8px monospace";
          ctx.fillStyle = "#ffffff";
          ctx.fillText(blip.label, bx + 12, by - 4);
          ctx.globalAlpha = 1.0;
        }
      });

      // Center Core Dot
      ctx.fillStyle = mainColor;
      ctx.beginPath();
      ctx.arc(cx, cy, 3, 0, Math.PI * 2);
      ctx.fill();

      ctx.restore();
    };

    render();

    return () => {
      cancelAnimationFrame(animId);
    };
  }, []);

  return (
    <div className={`relative w-full h-full min-h-[220px] flex items-center justify-center ${className}`}>
      <canvas ref={canvasRef} className="w-full h-full" />
      <div className="absolute bottom-2 left-3 text-[9px] font-mono text-slate-500 pointer-events-none">
        SWEEP: 360° AUTO · RF TRACE ACTIVE
      </div>
    </div>
  );
}
