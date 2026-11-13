"use client";

import React, { useEffect, useRef } from "react";
import * as THREE from "three";

interface HologramShieldProps {
  riskScore?: number;
  level?: "SAFE" | "WATCH" | "WARN" | "CRITICAL";
  isAudioActive?: boolean;
  className?: string;
}

export default function CyberHologramShield3D({
  riskScore = 0.05,
  level = "SAFE",
  isAudioActive = false,
  className = ""
}: HologramShieldProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  // Keep references to mutable state for 60 FPS animation loop without re-triggering React renders
  const stateRef = useRef({
    riskScore,
    level,
    isAudioActive,
    mouse: { x: 0, y: 0, targetX: 0, targetY: 0 }
  });

  useEffect(() => {
    stateRef.current.riskScore = riskScore;
    stateRef.current.level = level;
    stateRef.current.isAudioActive = isAudioActive;
  }, [riskScore, level, isAudioActive]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    let animId: number;
    let isContextLost = false;
    const width = container.clientWidth || 320;
    const height = container.clientHeight || 320;

    // Three.js Scene Setup
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(45, Math.max(0.1, width / (height || 1)), 0.1, 1000);
    camera.position.z = 5.2;

    const renderer = new THREE.WebGLRenderer({
      alpha: true,
      antialias: true,
      powerPreference: "high-performance"
    });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    container.appendChild(renderer.domElement);

    // WebGL Context Loss Handlers
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

    // Group for all shield elements
    const shieldGroup = new THREE.Group();
    scene.add(shieldGroup);

    // 1. Core Particle Orb (Sphere Point Cloud)
    const particleCount = 1400;
    const particlePositions = new Float32Array(particleCount * 3);
    const originalPositions = new Float32Array(particleCount * 3);
    const particleScales = new Float32Array(particleCount);

    const radius = 1.45;
    for (let i = 0; i < particleCount; i++) {
      // Golden spiral distribution on sphere
      const phi = Math.acos(-1 + (2 * i) / particleCount);
      const theta = Math.sqrt(particleCount * Math.PI) * phi;
      const r = radius * (0.94 + Math.random() * 0.12);

      const x = r * Math.sin(phi) * Math.cos(theta);
      const y = r * Math.sin(phi) * Math.sin(theta);
      const z = r * Math.cos(phi);

      particlePositions[i * 3] = x;
      particlePositions[i * 3 + 1] = y;
      particlePositions[i * 3 + 2] = z;

      originalPositions[i * 3] = x;
      originalPositions[i * 3 + 1] = y;
      originalPositions[i * 3 + 2] = z;

      particleScales[i] = Math.random() * 0.8 + 0.4;
    }

    const particleGeometry = new THREE.BufferGeometry();
    particleGeometry.setAttribute("position", new THREE.BufferAttribute(particlePositions, 3));
    particleGeometry.setAttribute("scale", new THREE.BufferAttribute(particleScales, 1));

    const pointMaterial = new THREE.PointsMaterial({
      color: 0x06b6d4,
      size: 0.055,
      transparent: true,
      opacity: 0.85,
      blending: THREE.AdditiveBlending
    });

    const particles = new THREE.Points(particleGeometry, pointMaterial);
    shieldGroup.add(particles);

    // 2. Outer Geodesic Icosahedron Wireframe
    const icoGeometry = new THREE.IcosahedronGeometry(1.68, 2);
    const icoWireframe = new THREE.WireframeGeometry(icoGeometry);
    const wireframeMaterial = new THREE.LineBasicMaterial({
      color: 0x06b6d4,
      transparent: true,
      opacity: 0.35,
      blending: THREE.AdditiveBlending
    });
    const shieldHull = new THREE.LineSegments(icoWireframe, wireframeMaterial);
    shieldGroup.add(shieldHull);

    // 3. Orbital Defense Reticle Rings
    const ringGeo1 = new THREE.RingGeometry(1.95, 1.97, 64);
    const ringMat1 = new THREE.MeshBasicMaterial({
      color: 0x06b6d4,
      side: THREE.DoubleSide,
      transparent: true,
      opacity: 0.3,
      blending: THREE.AdditiveBlending
    });
    const ring1 = new THREE.Mesh(ringGeo1, ringMat1);
    ring1.rotation.x = Math.PI / 3;
    shieldGroup.add(ring1);

    const ringGeo2 = new THREE.RingGeometry(2.15, 2.17, 64);
    const ringMat2 = new THREE.MeshBasicMaterial({
      color: 0x38bdf8,
      side: THREE.DoubleSide,
      transparent: true,
      opacity: 0.2,
      blending: THREE.AdditiveBlending
    });
    const ring2 = new THREE.Mesh(ringGeo2, ringMat2);
    ring2.rotation.y = Math.PI / 4;
    shieldGroup.add(ring2);

    // 4. Electric Arcs for CRITICAL states (Dynamic lightning lines)
    const arcCount = 12;
    const arcPositions = new Float32Array(arcCount * 6);
    const arcGeometry = new THREE.BufferGeometry();
    arcGeometry.setAttribute("position", new THREE.BufferAttribute(arcPositions, 3));
    const arcMaterial = new THREE.LineBasicMaterial({
      color: 0xff0055,
      transparent: true,
      opacity: 0.0,
      blending: THREE.AdditiveBlending,
      linewidth: 2
    });
    const electricArcs = new THREE.LineSegments(arcGeometry, arcMaterial);
    shieldGroup.add(electricArcs);

    // Color Targets
    const safeColor = new THREE.Color(0x06b6d4); // Neon Cyan
    const watchColor = new THREE.Color(0x10b981); // Emerald
    const warnColor = new THREE.Color(0xf59e0b); // Amber
    const critColor = new THREE.Color(0xef4444); // Electric Crimson
    const currentColor = new THREE.Color(0x06b6d4);

    // Unified Pointer Interaction (Touch + Mouse)
    const handlePointerMove = (e: PointerEvent) => {
      const rect = container.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        stateRef.current.mouse.targetX = ((e.clientX - rect.left) / rect.width - 0.5) * 1.4;
        stateRef.current.mouse.targetY = ((e.clientY - rect.top) / rect.height - 0.5) * 1.4;
      }
    };
    container.addEventListener("pointermove", handlePointerMove);

    // Resize handling
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

    // 60 FPS Render Loop
    const animate = () => {
      if (isContextLost) return;
      animId = requestAnimationFrame(animate);
      const elapsedTime = clock.getElapsedTime();
      const currentLevel = stateRef.current.level;
      const currentRisk = stateRef.current.riskScore;
      const isAudio = stateRef.current.isAudioActive;

      // Color Interpolation
      let targetColor = safeColor;
      if (currentLevel === "WATCH") targetColor = watchColor;
      else if (currentLevel === "WARN") targetColor = warnColor;
      else if (currentLevel === "CRITICAL") targetColor = critColor;

      currentColor.lerp(targetColor, 0.06);
      pointMaterial.color.copy(currentColor);
      wireframeMaterial.color.copy(currentColor);
      ringMat1.color.copy(currentColor);
      ringMat2.color.copy(currentColor);

      // Smooth Mouse/Touch Rotation Lerp
      stateRef.current.mouse.x += (stateRef.current.mouse.targetX - stateRef.current.mouse.x) * 0.05;
      stateRef.current.mouse.y += (stateRef.current.mouse.targetY - stateRef.current.mouse.y) * 0.05;

      const baseSpinSpeed = currentLevel === "CRITICAL" ? 1.8 : currentLevel === "WARN" ? 1.0 : 0.45;
      shieldGroup.rotation.y = elapsedTime * baseSpinSpeed + stateRef.current.mouse.x;
      shieldGroup.rotation.x = Math.sin(elapsedTime * 0.3) * 0.15 + stateRef.current.mouse.y;

      ring1.rotation.z += 0.012;
      ring2.rotation.z -= 0.018;

      // Vertex Noise & Pulse Modulation
      const posAttr = particleGeometry.attributes.position as THREE.BufferAttribute;
      const posArray = posAttr.array as Float32Array;

      const turbulence = currentLevel === "CRITICAL" ? 0.32 : currentLevel === "WARN" ? 0.16 : 0.05;
      const audioBoost = isAudio ? 0.12 : 0;
      const pulseSpeed = currentLevel === "CRITICAL" ? 8 : 3;

      for (let i = 0; i < particleCount; i++) {
        const i3 = i * 3;
        const ox = originalPositions[i3];
        const oy = originalPositions[i3 + 1];
        const oz = originalPositions[i3 + 2];

        // Harmonic displacement
        const displacement =
          1 +
          Math.sin(elapsedTime * pulseSpeed + ox * 2 + oy * 2) * (turbulence + audioBoost) +
          (currentLevel === "CRITICAL" ? (Math.random() - 0.5) * 0.14 : 0);

        posArray[i3] = ox * displacement;
        posArray[i3 + 1] = oy * displacement;
        posArray[i3 + 2] = oz * displacement;
      }
      posAttr.needsUpdate = true;

      // Outer Hull Breathing
      const hullScale = 1 + Math.sin(elapsedTime * 2.5) * (0.02 + currentRisk * 0.06);
      shieldHull.scale.set(hullScale, hullScale, hullScale);

      // Electric Arcs in CRITICAL Mode
      if (currentLevel === "CRITICAL") {
        arcMaterial.opacity = 0.85 + Math.sin(elapsedTime * 20) * 0.15;
        const arcAttr = arcGeometry.attributes.position as THREE.BufferAttribute;
        const arcArr = arcAttr.array as Float32Array;
        for (let i = 0; i < arcCount; i++) {
          const idx = i * 6;
          const p1 = Math.floor(Math.random() * particleCount) * 3;
          const p2 = Math.floor(Math.random() * particleCount) * 3;
          arcArr[idx] = posArray[p1];
          arcArr[idx + 1] = posArray[p1 + 1];
          arcArr[idx + 2] = posArray[p1 + 2];
          arcArr[idx + 3] = posArray[p2];
          arcArr[idx + 4] = posArray[p2 + 1];
          arcArr[idx + 5] = posArray[p2 + 2];
        }
        arcAttr.needsUpdate = true;
      } else {
        arcMaterial.opacity = 0.0;
      }

      renderer.render(scene, camera);
    };

    animate();

    return () => {
      cancelAnimationFrame(animId);
      canvas.removeEventListener("webglcontextlost", handleContextLost);
      canvas.removeEventListener("webglcontextrestored", handleContextRestored);
      container.removeEventListener("pointermove", handlePointerMove);
      resizeObserver.disconnect();

      // Clean Three.js resources
      particleGeometry.dispose();
      pointMaterial.dispose();
      icoGeometry.dispose();
      icoWireframe.dispose();
      wireframeMaterial.dispose();
      ringGeo1.dispose();
      ringMat1.dispose();
      ringGeo2.dispose();
      ringMat2.dispose();
      arcGeometry.dispose();
      arcMaterial.dispose();

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
    <div className={`relative w-full h-full min-h-[260px] flex items-center justify-center select-none ${className}`}>
      {/* 3D WebGL Canvas Mount with touch-action: none */}
      <div 
        ref={containerRef} 
        className="w-full h-full absolute inset-0 cursor-crosshair" 
        style={{ touchAction: "none" }}
      />

      {/* Cyber Reticle HUD Overlay */}
      <div className="absolute inset-2 pointer-events-none flex flex-col justify-between text-[9px] font-mono tracking-widest text-slate-500">
        <div className="flex justify-between items-center">
          <span className="flex items-center gap-1.5 text-cyan-400 font-bold">
            <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 animate-ping" />
            HOLOSHIELD_V2
          </span>
          <span className="text-slate-400">P_COUNT: 1,400</span>
        </div>
        <div className="flex justify-between items-center">
          <span>FPS: 60 [SYNC]</span>
          <span className={level === "CRITICAL" ? "text-red-400 font-bold animate-pulse" : "text-emerald-400 font-bold"}>
            DEFENSE: {level}
          </span>
        </div>
      </div>
    </div>
  );
}
