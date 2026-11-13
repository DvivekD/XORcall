"use client";

import React, { useEffect, useRef } from "react";
import * as THREE from "three";

interface CyberKatanaProps {
  progress?: number; // 0.0 (fully sheathed) to 1.0 (fully unsheathed)
  className?: string;
}

export default function CyberKatana3D({
  progress = 0,
  className = ""
}: CyberKatanaProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const stateRef = useRef({ progress, mouse: { x: 0, y: 0 } });

  useEffect(() => {
    stateRef.current.progress = progress;
  }, [progress]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    let animId: number;
    const width = container.clientWidth || 800;
    const height = container.clientHeight || 300;

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(36, width / height, 0.1, 100);
    camera.position.set(0, 0, 7.5);

    const renderer = new THREE.WebGLRenderer({
      alpha: true,
      antialias: true,
      powerPreference: "high-performance"
    });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.2;
    container.appendChild(renderer.domElement);

    // Lighting
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.8);
    scene.add(ambientLight);

    const dirLight1 = new THREE.DirectionalLight(0xffffff, 2.5);
    dirLight1.position.set(5, 8, 10);
    scene.add(dirLight1);

    const redRimLight = new THREE.DirectionalLight(0xe11d48, 3.0);
    redRimLight.position.set(-8, -4, 5);
    scene.add(redRimLight);

    const cyanRimLight = new THREE.DirectionalLight(0x06b6d4, 2.0);
    cyanRimLight.position.set(8, 4, 3);
    scene.add(cyanRimLight);

    // Master Katana Pivot
    const katanaRoot = new THREE.Group();
    scene.add(katanaRoot);

    // ----------------------------------------------------
    // 1. Blade Group (Slides to the RIGHT)
    // ----------------------------------------------------
    const bladeGroup = new THREE.Group();
    katanaRoot.add(bladeGroup);

    // Katana Blade Geometry with Curve (Sori)
    const bladeCurve = new THREE.QuadraticBezierCurve3(
      new THREE.Vector3(0, 0, 0),
      new THREE.Vector3(1.5, 0.08, 0),
      new THREE.Vector3(3.2, 0.22, 0)
    );
    const bladePoints = bladeCurve.getPoints(32);
    
    // Extrude blade profile along curve
    const bladeShape = new THREE.Shape();
    bladeShape.moveTo(0, 0.05);
    bladeShape.lineTo(0.02, 0); // Spine (Mune)
    bladeShape.lineTo(0, -0.06); // Cutting edge (Ha)
    bladeShape.lineTo(-0.02, 0);
    bladeShape.closePath();

    // Steel Blade Mesh
    const bladeGeo = new THREE.TubeGeometry(bladeCurve, 64, 0.035, 8, false);
    const bladeMat = new THREE.MeshStandardMaterial({
      color: 0xd8dde6,
      metalness: 0.95,
      roughness: 0.15,
      envMapIntensity: 2.0
    });
    const bladeMesh = new THREE.Mesh(bladeGeo, bladeMat);
    bladeGroup.add(bladeMesh);

    // Glowing Laser Hamon Edge
    const hamonGeo = new THREE.TubeGeometry(bladeCurve, 64, 0.015, 6, false);
    const hamonMat = new THREE.MeshBasicMaterial({
      color: 0x38bdf8,
      transparent: true,
      opacity: 0.8
    });
    const hamonMesh = new THREE.Mesh(hamonGeo, hamonMat);
    hamonMesh.position.y -= 0.02;
    bladeGroup.add(hamonMesh);

    // Habaki (Blade Collar)
    const habakiGeo = new THREE.BoxGeometry(0.12, 0.1, 0.06);
    const habakiMat = new THREE.MeshStandardMaterial({
      color: 0xd4af37, // Gold brass
      metalness: 0.9,
      roughness: 0.2
    });
    const habakiMesh = new THREE.Mesh(habakiGeo, habakiMat);
    habakiMesh.position.set(0, 0, 0);
    bladeGroup.add(habakiMesh);

    // Tsuba (Guard)
    const tsubaGeo = new THREE.CylinderGeometry(0.24, 0.24, 0.025, 32);
    const tsubaMat = new THREE.MeshStandardMaterial({
      color: 0x18191d,
      metalness: 0.8,
      roughness: 0.3
    });
    const tsubaMesh = new THREE.Mesh(tsubaGeo, tsubaMat);
    tsubaMesh.rotation.z = Math.PI / 2;
    tsubaMesh.position.set(-0.06, 0, 0);
    bladeGroup.add(tsubaMesh);

    // Tsuka (Handle / Hilt)
    const tsukaCurve = new THREE.LineCurve3(
      new THREE.Vector3(-0.06, 0, 0),
      new THREE.Vector3(-1.1, -0.04, 0)
    );
    const tsukaGeo = new THREE.TubeGeometry(tsukaCurve, 16, 0.05, 8, false);
    const tsukaMat = new THREE.MeshStandardMaterial({
      color: 0xf1f5f9, // White ito wrap
      roughness: 0.7,
      metalness: 0.1
    });
    const tsukaMesh = new THREE.Mesh(tsukaGeo, tsukaMat);
    bladeGroup.add(tsukaMesh);

    // Kashira (Pommel Cap)
    const kashiraGeo = new THREE.SphereGeometry(0.06, 16, 16);
    const kashiraMat = new THREE.MeshStandardMaterial({
      color: 0x18191d,
      metalness: 0.9,
      roughness: 0.2
    });
    const kashiraMesh = new THREE.Mesh(kashiraGeo, kashiraMat);
    kashiraMesh.position.set(-1.1, -0.04, 0);
    bladeGroup.add(kashiraMesh);

    // ----------------------------------------------------
    // 2. Saya Group (Scabbard - Slides to the LEFT)
    // ----------------------------------------------------
    const sayaGroup = new THREE.Group();
    katanaRoot.add(sayaGroup);

    const sayaCurve = new THREE.QuadraticBezierCurve3(
      new THREE.Vector3(0, 0, 0),
      new THREE.Vector3(1.5, 0.08, 0),
      new THREE.Vector3(3.3, 0.23, 0)
    );
    const sayaGeo = new THREE.TubeGeometry(sayaCurve, 64, 0.06, 12, false);
    const sayaMat = new THREE.MeshStandardMaterial({
      color: 0x111317, // Black Urushi lacquer
      roughness: 0.25,
      metalness: 0.4
    });
    const sayaMesh = new THREE.Mesh(sayaGeo, sayaMat);
    sayaGroup.add(sayaMesh);

    // Vermilion Lacquer Accent on Saya (Replicating red splash in video)
    const accentGeo = new THREE.CylinderGeometry(0.065, 0.065, 0.4, 16);
    const accentMat = new THREE.MeshStandardMaterial({
      color: 0xe11d48, // Vermilion red
      roughness: 0.3,
      metalness: 0.2
    });
    const accentMesh = new THREE.Mesh(accentGeo, accentMat);
    accentMesh.rotation.z = Math.PI / 2;
    accentMesh.position.set(0.35, 0.02, 0);
    sayaGroup.add(accentMesh);

    // ----------------------------------------------------
    // Animation Loop
    // ----------------------------------------------------
    const handleMouseMove = (e: MouseEvent) => {
      const rect = container.getBoundingClientRect();
      const x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
      const y = -(((e.clientY - rect.top) / rect.height) * 2 - 1);
      stateRef.current.mouse.x = x;
      stateRef.current.mouse.y = y;
    };
    window.addEventListener("mousemove", handleMouseMove);

    const animate = () => {
      animId = requestAnimationFrame(animate);

      const curProgress = stateRef.current.progress;

      // Horizontal Unsheathe Translation
      // Scabbard moves left by up to 2.4 units
      // Blade moves right by up to 2.4 units
      sayaGroup.position.x = THREE.MathUtils.lerp(sayaGroup.position.x, -curProgress * 2.4, 0.1);
      bladeGroup.position.x = THREE.MathUtils.lerp(bladeGroup.position.x, curProgress * 2.4, 0.1);

      // Subtle 3D tilt responding to mouse and scroll progress
      const targetRotY = stateRef.current.mouse.x * 0.15;
      const targetRotX = -stateRef.current.mouse.y * 0.12 + curProgress * 0.1;
      katanaRoot.rotation.y = THREE.MathUtils.lerp(katanaRoot.rotation.y, targetRotY, 0.08);
      katanaRoot.rotation.x = THREE.MathUtils.lerp(katanaRoot.rotation.x, targetRotX, 0.08);

      // Subtle levitation bobbing
      katanaRoot.position.y = Math.sin(Date.now() * 0.0015) * 0.05;

      renderer.render(scene, camera);
    };
    animate();

    // Resize handler
    const handleResize = () => {
      if (!container) return;
      const w = container.clientWidth;
      const h = container.clientHeight;
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h);
    };
    window.addEventListener("resize", handleResize);

    return () => {
      cancelAnimationFrame(animId);
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("resize", handleResize);
      renderer.dispose();
      if (container && renderer.domElement) {
        container.removeChild(renderer.domElement);
      }
    };
  }, []);

  return <div ref={containerRef} className={`w-full h-full relative ${className}`} />;
}
