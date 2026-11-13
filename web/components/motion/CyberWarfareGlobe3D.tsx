"use client";

import React, { useEffect, useRef, useState } from "react";
import * as THREE from "three";

interface GeoNode {
  name: string;
  lat: number;
  lon: number;
  type: "SYNDICATE" | "TARGET";
  country: string;
  details: string;
}

const GEO_NODES: GeoNode[] = [
  // Scam Syndicate Compounds
  { name: "KK Park Compound", lat: 16.5, lon: 98.5, type: "SYNDICATE", country: "Myanmar (Myawaddy)", details: "Forced labor boiler room, digital arrest call centers" },
  { name: "Sihanoukville Hub", lat: 10.6, lon: 103.5, type: "SYNDICATE", country: "Cambodia", details: "Telegram part-time job & crypto recharge syndicate" },
  { name: "Golden Triangle SEZ", lat: 20.3, lon: 100.1, type: "SYNDICATE", country: "Laos Border", details: "Deepfake voice-cloning farm & mule bank accounts" },
  { name: "Dubai Mule Cell", lat: 25.2, lon: 55.3, type: "SYNDICATE", country: "UAE / Middle East", details: "Hawala siphon & international crypto cashout node" },

  // Indian Target Metros
  { name: "New Delhi NCR", lat: 28.61, lon: 77.2, type: "TARGET", country: "India", details: "CBI parcel & Supreme Court digital arrest targeting" },
  { name: "Mumbai Cyber Cell", lat: 19.07, lon: 72.87, type: "TARGET", country: "India", details: "Corporate CEO whaling & customs drug seizure hoaxes" },
  { name: "Bengaluru Tech Hub", lat: 12.97, lon: 77.59, type: "TARGET", country: "India", details: "Tech professional crypto scams & elder extortion" },
  { name: "Hyderabad Cyberabad", lat: 17.38, lon: 78.48, type: "TARGET", country: "India", details: "BITS campus fee phishing & student task scams" },
  { name: "Kolkata Salt Lake", lat: 22.57, lon: 88.36, type: "TARGET", country: "India", details: "Electricity bill & Aadhaar cancellation phishing" }
];

const SYNDICATE_ARCS = [
  { from: 0, to: 4, label: "KK Park -> New Delhi [CBI Extortion]" },
  { from: 0, to: 5, label: "KK Park -> Mumbai [Narcotics Threat]" },
  { from: 1, to: 7, label: "Sihanoukville -> Hyderabad [Telegram Task]" },
  { from: 1, to: 6, label: "Sihanoukville -> Bengaluru [Crypto Deposit]" },
  { from: 2, to: 5, label: "Golden Triangle -> Mumbai [AI Deepfake Voice]" },
  { from: 3, to: 4, label: "Dubai Mule -> New Delhi [RTGS Siphon]" }
];

// Helper: Convert Lat/Lon to 3D Cartesian Vector
function latLonToVector3(lat: number, lon: number, radius: number): THREE.Vector3 {
  const phi = (90 - lat) * (Math.PI / 180);
  const theta = (lon + 180) * (Math.PI / 180);
  const x = -(radius * Math.sin(phi) * Math.cos(theta));
  const z = radius * Math.sin(phi) * Math.sin(theta);
  const y = radius * Math.cos(phi);
  return new THREE.Vector3(x, y, z);
}

export default function CyberWarfareGlobe3D({ className = "" }: { className?: string }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [selectedNode, setSelectedNode] = useState<GeoNode | null>(GEO_NODES[0]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    let animId: number;
    let isContextLost = false;
    const width = container.clientWidth || 600;
    const height = container.clientHeight || 500;

    // Track all allocated Three.js disposables for complete memory cleanup
    const disposables: { dispose: () => void }[] = [];

    // Scene & Camera
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(45, Math.max(0.1, width / (height || 1)), 0.1, 1000);
    camera.position.set(0, 1.2, 5.6);

    const renderer = new THREE.WebGLRenderer({
      alpha: true,
      antialias: true,
      powerPreference: "high-performance"
    });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    container.appendChild(renderer.domElement);

    // WebGL Context Loss Handling
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

    const globeGroup = new THREE.Group();
    // Rotate to focus primarily on South Asia / Indian subcontinent
    globeGroup.rotation.y = -Math.PI * 0.48;
    globeGroup.rotation.x = 0.22;
    scene.add(globeGroup);

    const radius = 2.0;

    // 1. Base Wireframe Globe Sphere
    const sphereGeo = new THREE.SphereGeometry(radius, 36, 36);
    const wireframe = new THREE.WireframeGeometry(sphereGeo);
    const globeMat = new THREE.LineBasicMaterial({
      color: 0x1e293b,
      transparent: true,
      opacity: 0.4
    });
    disposables.push(sphereGeo, wireframe, globeMat);

    const globeMesh = new THREE.LineSegments(wireframe, globeMat);
    globeGroup.add(globeMesh);

    // 2. Glowing Concentric Lat/Lon Grid Lines
    const ringsGroup = new THREE.Group();
    for (let i = -60; i <= 60; i += 30) {
      const ringR = radius * Math.cos((i * Math.PI) / 180);
      const ringY = radius * Math.sin((i * Math.PI) / 180);
      const ringGeo = new THREE.RingGeometry(Math.max(0.01, ringR - 0.005), ringR + 0.005, 64);
      const ringMat = new THREE.MeshBasicMaterial({
        color: 0x334155,
        side: THREE.DoubleSide,
        transparent: true,
        opacity: 0.35
      });
      disposables.push(ringGeo, ringMat);

      const ring = new THREE.Mesh(ringGeo, ringMat);
      ring.position.y = ringY;
      ring.rotation.x = Math.PI / 2;
      ringsGroup.add(ring);
    }
    globeGroup.add(ringsGroup);

    // 3. Dense Matrix Point Cloud on the surface (Landmass simulation dots)
    const pointCount = 3000;
    const dotPositions = new Float32Array(pointCount * 3);
    for (let i = 0; i < pointCount; i++) {
      const lat = (Math.random() - 0.5) * 160;
      const lon = (Math.random() - 0.5) * 360;
      const v = latLonToVector3(lat, lon, radius * 1.002);
      dotPositions[i * 3] = v.x;
      dotPositions[i * 3 + 1] = v.y;
      dotPositions[i * 3 + 2] = v.z;
    }
    const dotGeo = new THREE.BufferGeometry();
    dotGeo.setAttribute("position", new THREE.BufferAttribute(dotPositions, 3));
    const dotMat = new THREE.PointsMaterial({
      color: 0x38bdf8,
      size: 0.02,
      transparent: true,
      opacity: 0.4,
      blending: THREE.AdditiveBlending
    });
    disposables.push(dotGeo, dotMat);

    const dots = new THREE.Points(dotGeo, dotMat);
    globeGroup.add(dots);

    // 4. Pin Nodes for Syndicates and Targets
    const nodePositions: THREE.Vector3[] = [];
    GEO_NODES.forEach((node) => {
      const pos = latLonToVector3(node.lat, node.lon, radius * 1.015);
      nodePositions.push(pos);

      const isSyndicate = node.type === "SYNDICATE";
      const markerGeo = new THREE.SphereGeometry(isSyndicate ? 0.045 : 0.035, 12, 12);
      const markerMat = new THREE.MeshBasicMaterial({
        color: isSyndicate ? 0xef4444 : 0x06b6d4
      });
      disposables.push(markerGeo, markerMat);

      const marker = new THREE.Mesh(markerGeo, markerMat);
      marker.position.copy(pos);
      globeGroup.add(marker);

      const ringGeo = new THREE.RingGeometry(0.06, 0.075, 24);
      const ringMat = new THREE.MeshBasicMaterial({
        color: isSyndicate ? 0xf43f5e : 0x38bdf8,
        side: THREE.DoubleSide,
        transparent: true,
        opacity: 0.75
      });
      disposables.push(ringGeo, ringMat);

      const beaconRing = new THREE.Mesh(ringGeo, ringMat);
      beaconRing.position.copy(pos);
      beaconRing.lookAt(pos.clone().multiplyScalar(2));
      globeGroup.add(beaconRing);
    });

    // 5. 3D Arc Curves & Flying Telemetry Packets
    const curves: THREE.QuadraticBezierCurve3[] = [];
    const arcLinesGroup = new THREE.Group();
    const packetSprites: THREE.Mesh[] = [];

    SYNDICATE_ARCS.forEach((arc) => {
      const p1 = nodePositions[arc.from];
      const p2 = nodePositions[arc.to];

      const mid = p1.clone().add(p2).multiplyScalar(0.5);
      const distance = p1.distanceTo(p2);
      mid.normalize().multiplyScalar(radius + distance * 0.38);

      const curve = new THREE.QuadraticBezierCurve3(p1, mid, p2);
      curves.push(curve);

      const points = curve.getPoints(40);
      const curveGeo = new THREE.BufferGeometry().setFromPoints(points);
      const curveMat = new THREE.LineBasicMaterial({
        color: 0xf43f5e,
        transparent: true,
        opacity: 0.45,
        blending: THREE.AdditiveBlending
      });
      disposables.push(curveGeo, curveMat);

      const curveLine = new THREE.Line(curveGeo, curveMat);
      arcLinesGroup.add(curveLine);

      const packetGeo = new THREE.SphereGeometry(0.03, 8, 8);
      const packetMat = new THREE.MeshBasicMaterial({
        color: 0xffffff,
        blending: THREE.AdditiveBlending
      });
      disposables.push(packetGeo, packetMat);

      const packet = new THREE.Mesh(packetGeo, packetMat);
      globeGroup.add(packet);
      packetSprites.push(packet);
    });
    globeGroup.add(arcLinesGroup);

    // Unified Pointer Drag Interaction (Works on touchscreens, iPads, mobile & desktop)
    let isDragging = false;
    let previousPointerPos = { x: 0, y: 0 };

    const onPointerDown = (e: PointerEvent) => {
      isDragging = true;
      previousPointerPos = { x: e.clientX, y: e.clientY };
      try {
        container.setPointerCapture(e.pointerId);
      } catch {}
    };

    const onPointerMove = (e: PointerEvent) => {
      if (!isDragging) return;
      const deltaX = e.clientX - previousPointerPos.x;
      const deltaY = e.clientY - previousPointerPos.y;

      globeGroup.rotation.y += deltaX * 0.005;
      globeGroup.rotation.x = Math.max(-0.8, Math.min(0.8, globeGroup.rotation.x + deltaY * 0.005));

      previousPointerPos = { x: e.clientX, y: e.clientY };
    };

    const onPointerUp = (e: PointerEvent) => {
      isDragging = false;
      try {
        container.releasePointerCapture(e.pointerId);
      } catch {}
    };

    container.addEventListener("pointerdown", onPointerDown);
    container.addEventListener("pointermove", onPointerMove);
    container.addEventListener("pointerup", onPointerUp);
    container.addEventListener("pointercancel", onPointerUp);

    // Resize Observer
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

    const animate = () => {
      if (isContextLost) return;
      animId = requestAnimationFrame(animate);
      const elapsed = clock.getElapsedTime();

      // Gentle auto-rotation when user is not actively dragging
      if (!isDragging) {
        globeGroup.rotation.y += 0.0018;
      }

      // Update traveling photon packets along arcs
      curves.forEach((curve, index) => {
        const speed = 0.35 + index * 0.04;
        const t = (elapsed * speed) % 1.0;
        const pos = curve.getPoint(t);
        const packet = packetSprites[index];
        if (packet) {
          packet.position.copy(pos);
          const scale = 1 + Math.sin(t * Math.PI) * 1.5;
          packet.scale.set(scale, scale, scale);
        }
      });

      renderer.render(scene, camera);
    };

    animate();

    return () => {
      cancelAnimationFrame(animId);
      canvas.removeEventListener("webglcontextlost", handleContextLost);
      canvas.removeEventListener("webglcontextrestored", handleContextRestored);
      container.removeEventListener("pointerdown", onPointerDown);
      container.removeEventListener("pointermove", onPointerMove);
      container.removeEventListener("pointerup", onPointerUp);
      container.removeEventListener("pointercancel", onPointerUp);
      resizeObserver.disconnect();

      // Thorough GPU memory deallocation
      disposables.forEach((item) => {
        try {
          item.dispose();
        } catch {}
      });

      // Clear all scene children
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
    <div className={`relative w-full h-[520px] rounded-2xl overflow-hidden glass-panel border border-slate-800 select-none ${className}`}>
      {/* 3D WebGL Canvas Mount with touch-action: none */}
      <div 
        ref={containerRef} 
        className="w-full h-full cursor-grab active:cursor-grabbing" 
        style={{ touchAction: "none" }}
      />

      {/* Top Left Telemetry HUD */}
      <div className="absolute top-4 left-4 z-10 pointer-events-none flex flex-col gap-1.5 font-mono">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-red-500 animate-ping" />
          <span className="text-xs font-black text-white tracking-widest uppercase">
            Cross-Border Scam Warfare Radar
          </span>
        </div>
        <div className="flex items-center gap-3 text-[10px] text-slate-400">
          <span>ORIGIN: SOUTHEAST ASIA MULE CLUSTERS</span>
          <span className="text-slate-600">|</span>
          <span className="text-red-400 font-bold">SIPHON FLOW: ₹4,057.7 CR</span>
        </div>
        <div className="flex items-center gap-2 text-[10px]">
          <span className="bg-red-950/80 border border-red-500/50 text-red-300 px-2 py-0.5 rounded font-bold">
            6 ACTIVE TRACE VECTORS
          </span>
          <span className="bg-cyan-950/80 border border-cyan-500/50 text-cyan-300 px-2 py-0.5 rounded font-bold">
            9 INTERCEPT NODES
          </span>
        </div>
      </div>

      {/* Top Right Monitored Node Selector */}
      <div className="absolute top-4 right-4 z-10 flex flex-col gap-1 max-w-[220px]">
        <span className="text-[9px] font-mono uppercase text-slate-400 text-right tracking-wider">
          Inspect Monitored Node:
        </span>
        <div className="flex flex-wrap justify-end gap-1">
          {GEO_NODES.slice(0, 6).map((node) => (
            <button
              key={node.name}
              onClick={() => setSelectedNode(node)}
              className={`text-[9px] font-mono px-2 py-0.5 rounded border transition-all cursor-pointer ${
                selectedNode?.name === node.name
                  ? node.type === "SYNDICATE"
                    ? "bg-red-900 border-red-500 text-white font-bold"
                    : "bg-cyan-900 border-cyan-500 text-white font-bold"
                  : "bg-slate-900/80 border-slate-800 text-slate-400 hover:text-white"
              }`}
            >
              {node.name.split(" ")[0]}
            </button>
          ))}
        </div>
      </div>

      {/* Bottom Node Inspector Drawer */}
      {selectedNode && (
        <div className="absolute bottom-4 left-4 right-4 z-10 glass-panel-glow p-3.5 rounded-xl border border-slate-700/80 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-3">
            <div className={`w-8 h-8 rounded-lg flex items-center justify-center font-bold text-white text-xs shrink-0 ${
              selectedNode.type === "SYNDICATE" ? "bg-red-600" : "bg-cyan-600"
            }`}>
              {selectedNode.type === "SYNDICATE" ? "SYN" : "TGT"}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-white text-sm">{selectedNode.name}</span>
                <span className="text-[10px] font-mono text-slate-400">({selectedNode.country})</span>
              </div>
              <p className="text-[11px] text-slate-300 mt-0.5">{selectedNode.details}</p>
            </div>
          </div>

          <div className="flex items-center gap-2 font-mono text-[10px] shrink-0">
            <span className="px-2 py-1 bg-slate-900 rounded border border-slate-800 text-slate-400">
              LAT: {selectedNode.lat.toFixed(2)}°
            </span>
            <span className="px-2 py-1 bg-slate-900 rounded border border-slate-800 text-slate-400">
              LON: {selectedNode.lon.toFixed(2)}°
            </span>
            <span className={`px-2 py-1 rounded border font-bold ${
              selectedNode.type === "SYNDICATE"
                ? "bg-red-950 border-red-500/50 text-red-300"
                : "bg-cyan-950 border-cyan-500/50 text-cyan-300"
            }`}>
              {selectedNode.type === "SYNDICATE" ? "COERCION SOURCE" : "VICTIM TARGET ZONE"}
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
