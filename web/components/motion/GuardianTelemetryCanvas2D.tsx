"use client";

import React, { useEffect, useRef } from "react";

interface GuardianTelemetryProps {
  isSafeConfirmed?: boolean;
  isSirenActive?: boolean;
  className?: string;
}

export default function GuardianTelemetryCanvas2D({
  isSafeConfirmed = false,
  isSirenActive = false,
  className = ""
}: GuardianTelemetryProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef({ isSafeConfirmed, isSirenActive });

  useEffect(() => {
    stateRef.current.isSafeConfirmed = isSafeConfirmed;
    stateRef.current.isSirenActive = isSirenActive;
  }, [isSafeConfirmed, isSirenActive]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animId: number;
    let tick = 0;

    const render = () => {
      animId = requestAnimationFrame(render);
      tick += 0.05;

      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const width = canvas.clientWidth;
      const height = canvas.clientHeight;

      if (width < 30 || height < 30) return;

      if (canvas.width !== Math.floor(width * dpr) || canvas.height !== Math.floor(height * dpr)) {
        canvas.width = Math.floor(width * dpr);
        canvas.height = Math.floor(height * dpr);
      }

      ctx.save();
      ctx.scale(dpr, dpr);
      ctx.clearRect(0, 0, width, height);

      const isSiren = stateRef.current.isSirenActive;
      const isConfirmed = stateRef.current.isSafeConfirmed;

      const p1 = { x: width * 0.18, y: height * 0.5 };
      const p2 = { x: width * 0.82, y: height * 0.5 };

      // Link Tether Wave
      ctx.lineWidth = 1.5;
      ctx.strokeStyle = isSiren
        ? "rgba(239, 68, 68, 0.8)"
        : isConfirmed
        ? "rgba(16, 185, 129, 0.8)"
        : "rgba(6, 182, 212, 0.6)";

      ctx.beginPath();
      const points = 60;
      for (let i = 0; i <= points; i++) {
        const t = i / points;
        const x = p1.x + (p2.x - p1.x) * t;
        // Harmonic carrier wave modulated by safe status
        const amp = isSiren ? 22 : 8;
        const freq = isSiren ? 12 : 6;
        const y = height * 0.5 + Math.sin(t * Math.PI * freq - tick * 4) * amp * Math.sin(t * Math.PI);

        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.stroke();

      // Traveling Ping Particles along the tether
      for (let k = 0; k < 3; k++) {
        const progress = ((tick * 0.3 + k * 0.33) % 1.0);
        const px = p1.x + (p2.x - p1.x) * progress;
        const py = height * 0.5 + Math.sin(progress * Math.PI * 6 - tick * 4) * 8 * Math.sin(progress * Math.PI);

        ctx.fillStyle = isSiren ? "#ef4444" : "#ffffff";
        ctx.beginPath();
        ctx.arc(px, py, 3, 0, Math.PI * 2);
        ctx.fill();
      }

      // Parent Node Orbit
      const drawNode = (
        pos: { x: number; y: number },
        label: string,
        sub: string,
        color: string,
        isTarget: boolean
      ) => {
        // Orbit rings
        const orbitR = 24 + Math.sin(tick * 2) * 2;
        ctx.strokeStyle = `rgba(${color}, 0.3)`;
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.arc(pos.x, pos.y, orbitR, 0, Math.PI * 2);
        ctx.stroke();

        // Pulsing radar ping ripple
        const pingR = ((tick * 20) % 45);
        const pingAlpha = Math.max(0, 1 - pingR / 45);
        ctx.strokeStyle = `rgba(${color}, ${pingAlpha * 0.6})`;
        ctx.beginPath();
        ctx.arc(pos.x, pos.y, pingR, 0, Math.PI * 2);
        ctx.stroke();

        // Core circle
        ctx.fillStyle = `rgb(${color})`;
        ctx.beginPath();
        ctx.arc(pos.x, pos.y, 8, 0, Math.PI * 2);
        ctx.fill();

        // Inner highlight
        ctx.fillStyle = "#ffffff";
        ctx.beginPath();
        ctx.arc(pos.x, pos.y, 3, 0, Math.PI * 2);
        ctx.fill();

        // Label
        ctx.fillStyle = "#ffffff";
        ctx.font = "bold 9px monospace";
        ctx.textAlign = "center";
        ctx.fillText(label, pos.x, pos.y - 30);

        ctx.fillStyle = `rgb(${color})`;
        ctx.font = "8px monospace";
        ctx.fillText(sub, pos.x, pos.y + 36);
      };

      drawNode(p1, "GUARDIAN (PARENT)", "LIKHITH · S24 ULTRA", "6, 182, 212", false);
      drawNode(
        p2,
        "PROTECTED (DAD)",
        isConfirmed ? "VERIFIED SAFE (100%)" : "MONITORED (S23)",
        isSiren ? "239, 68, 68" : isConfirmed ? "16, 185, 129" : "56, 189, 248",
        true
      );

      // Accelerometer Seismic Readout in Center
      ctx.fillStyle = "#94a3b8";
      ctx.font = "9px monospace";
      ctx.textAlign = "center";
      ctx.fillText("ACCELEROMETER: 0.0G IMPACT (NORMAL_RESTING)", width * 0.5, height * 0.86);

      ctx.restore();
    };

    render();

    return () => {
      cancelAnimationFrame(animId);
    };
  }, []);

  return (
    <div className={`relative w-full h-[180px] rounded-2xl overflow-hidden glass-panel border border-slate-800 select-none ${className}`}>
      <canvas ref={canvasRef} className="w-full h-full" />
      <div className="absolute top-2 left-3 right-3 flex items-center justify-between text-[9px] font-mono pointer-events-none text-slate-400">
        <span className="flex items-center gap-1.5 text-emerald-400 font-bold">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping" />
          SATELLITE_GUARDIAN_ORBIT
        </span>
        <span className="text-slate-500">60 FPS PASSIVE SENSORY TETHER</span>
      </div>
    </div>
  );
}
