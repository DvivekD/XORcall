"use client";

import React, { useEffect, useRef } from "react";

interface CommunitySentimentCanvasProps {
  upvotes?: number;
  className?: string;
}

interface Spark {
  x: number;
  y: number;
  vy: number;
  vx: number;
  alpha: number;
  color: string;
}

export default function CommunitySentimentCanvas2D({
  upvotes = 2847,
  className = ""
}: CommunitySentimentCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef({ upvotes });

  useEffect(() => {
    stateRef.current.upvotes = upvotes;
  }, [upvotes]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animId: number;
    let tick = 0;
    const sparks: Spark[] = [];
    const MAX_SPARKS = 50;

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

      // Background subtle gradient
      const bgGrad = ctx.createLinearGradient(0, height, 0, 0);
      bgGrad.addColorStop(0, "rgba(234, 88, 12, 0.08)");
      bgGrad.addColorStop(1, "rgba(6, 9, 17, 0)");
      ctx.fillStyle = bgGrad;
      ctx.fillRect(0, 0, width, height);

      // Sentiment Harmonic Waves
      ctx.lineWidth = 1.5;
      const waveCount = 2;
      for (let w = 0; w < waveCount; w++) {
        ctx.strokeStyle = w === 0 ? "rgba(249, 115, 22, 0.7)" : "rgba(6, 182, 212, 0.5)";
        ctx.beginPath();
        const steps = 60;
        for (let i = 0; i <= steps; i++) {
          const x = (i / steps) * width;
          const y = height * 0.55 +
            Math.sin(i * 0.15 + tick * (w === 0 ? 2 : 1.5)) * 14 +
            Math.cos(i * 0.3 - tick) * 6;
          if (i === 0) ctx.moveTo(x, y);
          else ctx.lineTo(x, y);
        }
        ctx.stroke();
      }

      // Spawn Upvote Sparks
      if (sparks.length < MAX_SPARKS && Math.random() < 0.3) {
        sparks.push({
          x: Math.random() * width,
          y: height * 0.85,
          vx: (Math.random() - 0.5) * 0.8,
          vy: -Math.random() * 1.5 - 0.8,
          alpha: 1.0,
          color: Math.random() > 0.3 ? "#f97316" : "#06b6d4"
        });
      }

      // Update & Render Sparks
      for (let s = sparks.length - 1; s >= 0; s--) {
        const sp = sparks[s];
        sp.x += sp.vx;
        sp.y += sp.vy;
        sp.alpha -= 0.02;

        if (sp.alpha <= 0 || sp.y < 0) {
          sparks.splice(s, 1);
          continue;
        }

        ctx.fillStyle = sp.color;
        ctx.globalAlpha = sp.alpha;
        ctx.beginPath();
        ctx.arc(sp.x, sp.y, 2, 0, Math.PI * 2);
        ctx.fill();
        ctx.globalAlpha = 1.0;
      }

      ctx.restore();
    };

    render();

    return () => {
      cancelAnimationFrame(animId);
    };
  }, []);

  return (
    <div className={`relative w-full h-[120px] rounded-2xl overflow-hidden glass-panel border border-slate-800 select-none ${className}`}>
      <canvas ref={canvasRef} className="w-full h-full" />
      <div className="absolute top-2 left-3 right-3 flex items-center justify-between text-[9px] font-mono pointer-events-none text-slate-400">
        <span className="flex items-center gap-1.5 text-orange-400 font-bold">
          <span className="w-1.5 h-1.5 rounded-full bg-orange-400 animate-ping" />
          COMMUNITY_SENTIMENT_HARMONICS
        </span>
        <span className="text-slate-400 font-bold">
          UPVOTE INTENSITY: {(stateRef.current.upvotes / 1000).toFixed(1)}k
        </span>
      </div>
    </div>
  );
}
