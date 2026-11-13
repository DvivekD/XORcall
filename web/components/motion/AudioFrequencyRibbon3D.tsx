"use client";

import React, { useEffect, useRef } from "react";
import * as THREE from "three";

interface AudioFrequencyRibbonProps {
  isActive?: boolean;
  amplitude?: number;
  threatLevel?: "SAFE" | "WATCH" | "WARN" | "CRITICAL";
  className?: string;
}

export default function AudioFrequencyRibbon3D({
  isActive = true,
  amplitude = 1.0,
  threatLevel = "SAFE",
  className = ""
}: AudioFrequencyRibbonProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const stateRef = useRef({ isActive, amplitude, threatLevel, mouseX: 0, mouseY: 0 });

  useEffect(() => {
    stateRef.current.isActive = isActive;
    stateRef.current.amplitude = amplitude;
    stateRef.current.threatLevel = threatLevel;
  }, [isActive, amplitude, threatLevel]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    let animId: number;
    let isContextLost = false;
    const width = container.clientWidth || 500;
    const height = container.clientHeight || 280;

    // Three.js Scene
    const scene = new THREE.Scene();
    scene.fog = new THREE.FogExp2(0x060911, 0.12);

    const camera = new THREE.PerspectiveCamera(55, Math.max(0.1, width / (height || 1)), 0.1, 100);
    camera.position.set(0, 2.2, 5.5);
    camera.lookAt(0, -0.2, 0);

    const renderer = new THREE.WebGLRenderer({
      alpha: true,
      antialias: true,
      powerPreference: "high-performance"
    });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    container.appendChild(renderer.domElement);

    // Context Loss Handlers
    const handleContextLost = (e: Event) => {
      e.preventDefault();
      isContextLost = true;
      if (animId) cancelAnimationFrame(animId);
    };

    const handleContextRestored = () => {
      isContextLost = false;
      animate();
    };

    const canvas = renderer.domElement;
    canvas.addEventListener("webglcontextlost", handleContextLost, false);
    canvas.addEventListener("webglcontextrestored", handleContextRestored, false);

    // 1. Procedural 3D Plane Mesh for Frequency Landscape
    const cols = 50;
    const rows = 40;
    const planeGeo = new THREE.PlaneGeometry(8, 12, cols - 1, rows - 1);
    planeGeo.rotateX(-Math.PI / 2);

    const posAttr = planeGeo.attributes.position as THREE.BufferAttribute;
    const vertexCount = posAttr.count;

    const planeMat = new THREE.MeshBasicMaterial({
      color: 0x06b6d4,
      wireframe: true,
      transparent: true,
      opacity: 0.65,
      blending: THREE.AdditiveBlending
    });
    const landscape = new THREE.Mesh(planeGeo, planeMat);
    landscape.position.z = -2;
    scene.add(landscape);

    // 2. Floating Audio Sparks (Particle Field above the wave crests)
    const sparkCount = 200;
    const sparkGeo = new THREE.BufferGeometry();
    const sparkPos = new Float32Array(sparkCount * 3);
    for (let i = 0; i < sparkCount; i++) {
      sparkPos[i * 3] = (Math.random() - 0.5) * 7;
      sparkPos[i * 3 + 1] = Math.random() * 2 + 0.2;
      sparkPos[i * 3 + 2] = (Math.random() - 0.5) * 10 - 2;
    }
    sparkGeo.setAttribute("position", new THREE.BufferAttribute(sparkPos, 3));
    const sparkMat = new THREE.PointsMaterial({
      color: 0x38bdf8,
      size: 0.04,
      transparent: true,
      opacity: 0.8,
      blending: THREE.AdditiveBlending
    });
    const sparks = new THREE.Points(sparkGeo, sparkMat);
    scene.add(sparks);

    // Colors
    const safeColor = new THREE.Color(0x06b6d4); // Cyan
    const warnColor = new THREE.Color(0xf59e0b); // Amber
    const critColor = new THREE.Color(0xef4444); // Crimson
    const currentColor = new THREE.Color(0x06b6d4);

    // Unified Pointer Tracking (Touch + Mouse)
    const handlePointerMove = (e: PointerEvent) => {
      const rect = container.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        stateRef.current.mouseX = ((e.clientX - rect.left) / rect.width - 0.5) * 1.5;
        stateRef.current.mouseY = ((e.clientY - rect.top) / rect.height - 0.5) * 0.8;
      }
    };
    container.addEventListener("pointermove", handlePointerMove);

    // Resize Handler
    const resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const newW = entry.contentRect.width;
        const newH = entry.contentRect.height;
        if (newW > 0 && newH > 0) {
          camera.aspect = newW / newH;
          camera.updateProjectionMatrix();
          renderer.setSize(newW, newH);
        }
      }
    });
    resizeObserver.observe(container);

    const clock = new THREE.Clock();

    // 60 FPS Render loop
    const animate = () => {
      if (isContextLost) return;
      animId = requestAnimationFrame(animate);
      const elapsed = clock.getElapsedTime();
      const { isActive, amplitude, threatLevel, mouseX, mouseY } = stateRef.current;

      // Color lerp
      let targetColor = safeColor;
      if (threatLevel === "WARN") targetColor = warnColor;
      else if (threatLevel === "CRITICAL") targetColor = critColor;
      currentColor.lerp(targetColor, 0.05);
      planeMat.color.copy(currentColor);
      sparkMat.color.copy(currentColor);

      // Camera tilt lerp
      camera.position.x += (mouseX - camera.position.x) * 0.05;
      camera.position.y += (2.2 - mouseY - camera.position.y) * 0.05;
      camera.lookAt(0, -0.2, -2);

      // Deform Landscape Vertices (Traveling audio wave spectrum)
      const posArray = posAttr.array as Float32Array;
      const energyMultiplier = isActive ? (threatLevel === "CRITICAL" ? 1.8 : 1.1) * amplitude : 0.25;
      const waveSpeed = isActive ? 4.5 : 1.5;

      for (let i = 0; i < vertexCount; i++) {
        const i3 = i * 3;
        const x = posArray[i3];
        const z = posArray[i3 + 2];

        // Complex harmonic multi-frequency synthesis
        const wave1 = Math.sin(x * 1.8 + z * 0.8 + elapsed * waveSpeed) * 0.45;
        const wave2 = Math.cos(x * 3.2 - z * 1.2 + elapsed * (waveSpeed * 1.3)) * 0.25;
        const wave3 = Math.sin(x * 0.9 - elapsed * 2) * 0.3;

        // Attenuate sides so ribbon gracefully tapers off
        const lateralFalloff = Math.exp(-Math.pow(x / 3.4, 2));

        posArray[i3 + 1] = (wave1 + wave2 + wave3) * energyMultiplier * lateralFalloff;
      }
      posAttr.needsUpdate = true;

      // Drift sparks forward along z-axis
      const sparkArr = sparkGeo.attributes.position.array as Float32Array;
      for (let i = 0; i < sparkCount; i++) {
        const idx = i * 3 + 2;
        sparkArr[idx] += 0.04;
        if (sparkArr[idx] > 3) {
          sparkArr[idx] = -7;
        }
      }
      sparkGeo.attributes.position.needsUpdate = true;

      renderer.render(scene, camera);
    };

    animate();

    return () => {
      cancelAnimationFrame(animId);
      canvas.removeEventListener("webglcontextlost", handleContextLost);
      canvas.removeEventListener("webglcontextrestored", handleContextRestored);
      container.removeEventListener("pointermove", handlePointerMove);
      resizeObserver.disconnect();

      planeGeo.dispose();
      planeMat.dispose();
      sparkGeo.dispose();
      sparkMat.dispose();

      while (scene.children.length > 0) {
        scene.remove(scene.children[0]);
      }

      renderer.dispose();
      if (renderer.domElement && container.contains(renderer.domElement)) {
        container.removeChild(renderer.domElement);
      }
    };
  }, []);

  return (
    <div className={`relative w-full h-[260px] rounded-2xl overflow-hidden glass-panel border border-slate-800 select-none ${className}`}>
      {/* Three.js Canvas Mount */}
      <div 
        ref={containerRef} 
        className="w-full h-full cursor-pointer" 
        style={{ touchAction: "none" }}
      />

      {/* Futuristic Audio Spectrogram HUD */}
      <div className="absolute top-3 left-4 right-4 pointer-events-none flex items-center justify-between text-[10px] font-mono">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
          <span className="font-bold text-white tracking-widest uppercase">
            3D Audio Spectral Ribbon · Sherpa Local
          </span>
        </div>
        <div className="flex items-center gap-3 text-slate-400">
          <span>SAMPLING: 16 kHz PCM</span>
          <span className="text-cyan-400 font-bold">FFT CHANNELS: 128</span>
          <span className="text-slate-600">|</span>
          <span className={threatLevel === "CRITICAL" ? "text-red-400 font-bold" : "text-emerald-400 font-bold"}>
            STATUS: {isActive ? "SYNCHRONIZED" : "STANDBY"}
          </span>
        </div>
      </div>
    </div>
  );
}
