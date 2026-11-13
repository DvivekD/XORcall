"use client";

import React, { useEffect, useRef } from "react";

interface KillChainCanvasProps {
  activeStage?: number;
  onSelectStage?: (stage: number) => void;
  className?: string;
}

interface NodePoint {
  stage: number;
  label: string;
  sub: string;
  xNorm: number;
  yNorm: number;
  color: string;
}

export default function KillChainCanvas2D({
  activeStage = 1,
  onSelectStage,
  className = ""
}: KillChainCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef({ activeStage, onSelectStage });

  useEffect(() => {
    stateRef.current.activeStage = activeStage;
    stateRef.current.onSelectStage = onSelectStage;
  }, [activeStage, onSelectStage]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animId: number;
    let tick = 0;

    const nodes: NodePoint[] = [
      { stage: 1, label: "1.PRETEXT", sub: "Authority", xNorm: 0.1, yNorm: 0.5, color: "#06b6d4" },
      { stage: 2, label: "2.CRISIS", sub: "Adrenaline", xNorm: 0.3, yNorm: 0.28, color: "#f59e0b" },
      { stage: 3, label: "3.ISOLATION", sub: "Smoking Gun", xNorm: 0.5, yNorm: 0.72, color: "#f97316" },
      { stage: 4, label: "4.MULE_SIPHON", sub: "Direct Transfer", xNorm: 0.7, yNorm: 0.28, color: "#ef4444" },
      { stage: 5, label: "5.LOCKDOWN", sub: "Extortion Trance", xNorm: 0.9, yNorm: 0.5, color: "#f43f5e" }
    ];

    // Flying conduit photon packets
    const packetCount = 18;
    const packets = Array.from({ length: packetCount }, (_, i) => ({
      segment: i % 4, // 0: 1->2, 1: 2->3, 2: 3->4, 3: 4->5
      progress: (i / packetCount),
      speed: 0.008 + (i % 3) * 0.003
    }));

    const render = () => {
      animId = requestAnimationFrame(render);
      tick += 0.04;

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

      const currentStage = stateRef.current.activeStage;

      // Draw Conduit Laser Pathways between nodes
      ctx.lineWidth = 1.5;
      for (let i = 0; i < nodes.length - 1; i++) {
        const n1 = nodes[i];
        const n2 = nodes[i + 1];
        const x1 = n1.xNorm * width;
        const y1 = n1.yNorm * height;
        const x2 = n2.xNorm * width;
        const y2 = n2.yNorm * height;

        const isTraversed = n2.stage <= currentStage;

        // Base glow path
        ctx.strokeStyle = isTraversed ? "rgba(239, 68, 68, 0.45)" : "rgba(51, 65, 85, 0.35)";
        ctx.beginPath();
        ctx.moveTo(x1, y1);
        ctx.lineTo(x2, y2);
        ctx.stroke();
      }

      // Update & Draw Traveling Photon Packets
      packets.forEach((p) => {
        p.progress += p.speed;
        if (p.progress >= 1) p.progress = 0;

        const n1 = nodes[p.segment];
        const n2 = nodes[p.segment + 1];
        const px = (n1.xNorm + (n2.xNorm - n1.xNorm) * p.progress) * width;
        const py = (n1.yNorm + (n2.yNorm - n1.yNorm) * p.progress) * height;

        const isHot = n2.stage <= currentStage;
        ctx.fillStyle = isHot ? "#ffffff" : "rgba(56, 189, 248, 0.7)";
        ctx.beginPath();
        ctx.arc(px, py, isHot ? 2.5 : 1.8, 0, Math.PI * 2);
        ctx.fill();

        // Trail glow
        if (isHot) {
          ctx.fillStyle = "rgba(239, 68, 68, 0.35)";
          ctx.beginPath();
          ctx.arc(px, py, 6, 0, Math.PI * 2);
          ctx.fill();
        }
      });

      // Draw Interactive Stage Nodes
      nodes.forEach((n) => {
        const nx = n.xNorm * width;
        const ny = n.yNorm * height;
        const isSelected = n.stage === currentStage;
        const isPassed = n.stage <= currentStage;

        // Outer pulsing ring for selected node
        if (isSelected) {
          const pulseR = 14 + Math.sin(tick * 3) * 4;
          ctx.strokeStyle = n.color;
          ctx.lineWidth = 1.5;
          ctx.beginPath();
          ctx.arc(nx, ny, pulseR, 0, Math.PI * 2);
          ctx.stroke();
        }

        // Concentric node halo
        ctx.fillStyle = isPassed ? n.color : "#1e293b";
        ctx.beginPath();
        ctx.arc(nx, ny, isSelected ? 9 : 7, 0, Math.PI * 2);
        ctx.fill();

        // Inner core
        ctx.fillStyle = "#ffffff";
        ctx.beginPath();
        ctx.arc(nx, ny, 2.5, 0, Math.PI * 2);
        ctx.fill();

        // Node Label
        ctx.fillStyle = isSelected ? "#ffffff" : isPassed ? "#cbd5e1" : "#64748b";
        ctx.font = isSelected ? "bold 10px monospace" : "9px monospace";
        ctx.textAlign = "center";
        ctx.fillText(n.label, nx, ny + (n.yNorm > 0.5 ? 20 : -14));

        // Subtitle
        ctx.fillStyle = isSelected ? n.color : "#475569";
        ctx.font = "8px monospace";
        ctx.fillText(n.sub, nx, ny + (n.yNorm > 0.5 ? 31 : -24));
      });

      ctx.restore();
    };

    render();

    // Canvas click to select stage
    const handleCanvasClick = (e: MouseEvent) => {
      const rect = canvas.getBoundingClientRect();
      const clickX = e.clientX - rect.left;
      const clickY = e.clientY - rect.top;

      nodes.forEach((n) => {
        const nx = n.xNorm * rect.width;
        const ny = n.yNorm * rect.height;
        const dist = Math.hypot(clickX - nx, clickY - ny);
        if (dist < 26) {
          stateRef.current.onSelectStage?.(n.stage);
        }
      });
    };

    canvas.addEventListener("click", handleCanvasClick);

    return () => {
      cancelAnimationFrame(animId);
      canvas.removeEventListener("click", handleCanvasClick);
    };
  }, []);

  return (
    <div className={`relative w-full h-[160px] rounded-2xl overflow-hidden glass-panel border border-slate-800 select-none ${className}`}>
      <canvas ref={canvasRef} className="w-full h-full cursor-pointer" />
      <div className="absolute top-2 left-3 right-3 flex items-center justify-between text-[9px] font-mono pointer-events-none text-slate-400">
        <span className="flex items-center gap-1 text-indigo-400 font-bold">
          <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 animate-ping" />
          KINETIC_KILL_CHAIN_GRAPH
        </span>
        <span className="text-slate-500">CLICK ANY NODE TO INSPECT CONJUNCTION</span>
      </div>
    </div>
  );
}
