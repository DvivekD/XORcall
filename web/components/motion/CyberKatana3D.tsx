"use client";

import React, { useEffect, useRef } from "react";
import * as THREE from "three";

interface CyberKatanaProps {
  progress?: number; // 0.0 to 1.0
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
    const width = container.clientWidth || window.innerWidth;
    const height = container.clientHeight || window.innerHeight;

    const scene = new THREE.Scene();
    
    // Close camera perspective so katana fills the screen width (video-accurate)
    const camera = new THREE.PerspectiveCamera(40, width / height, 0.1, 100);
    camera.position.set(0, 0, 4.4);

    const renderer = new THREE.WebGLRenderer({
      alpha: true,
      antialias: true,
      powerPreference: "high-performance"
    });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.35;
    container.appendChild(renderer.domElement);

    // Studio Key Lights (Video Accurate Highlights on Steel and Urushi Lacquer)
    const ambientLight = new THREE.AmbientLight(0xffffff, 1.0);
    scene.add(ambientLight);

    const keyLight = new THREE.DirectionalLight(0xffffff, 3.2);
    keyLight.position.set(4, 6, 8);
    scene.add(keyLight);

    const vermilionRim = new THREE.DirectionalLight(0xe11d48, 4.5);
    vermilionRim.position.set(-6, -3, 4);
    scene.add(vermilionRim);

    const cyanRim = new THREE.DirectionalLight(0x38bdf8, 3.0);
    cyanRim.position.set(6, 3, 5);
    scene.add(cyanRim);

    // Master Katana Pivot
    const katanaMaster = new THREE.Group();
    scene.add(katanaMaster);

    // -------------------------------------------------------------------------
    // 1. Blade Assembly (Upper Tier: Blade + Habaki + Tsuba + Tsuka Handle)
    // -------------------------------------------------------------------------
    const bladeAssembly = new THREE.Group();
    katanaMaster.add(bladeAssembly);

    // Realistic curved blade curve (length ~4.8 units spanning screen)
    const bladeCurve = new THREE.QuadraticBezierCurve3(
      new THREE.Vector3(-0.1, 0, 0),
      new THREE.Vector3(2.2, 0.09, 0),
      new THREE.Vector3(4.5, 0.28, 0)
    );

    // Steel Blade Body
    const bladeGeo = new THREE.TubeGeometry(bladeCurve, 64, 0.052, 8, false);
    const bladeMat = new THREE.MeshStandardMaterial({
      color: 0xebedf2,
      metalness: 0.98,
      roughness: 0.12,
      envMapIntensity: 2.5
    });
    const bladeMesh = new THREE.Mesh(bladeGeo, bladeMat);
    bladeAssembly.add(bladeMesh);

    // Wavy Hamon Temper Line (Laser Cyber Glow)
    const hamonGeo = new THREE.TubeGeometry(bladeCurve, 64, 0.022, 6, false);
    const hamonMat = new THREE.MeshBasicMaterial({
      color: 0x38bdf8,
      transparent: true,
      opacity: 0.85
    });
    const hamonMesh = new THREE.Mesh(hamonGeo, hamonMat);
    hamonMesh.position.y -= 0.026;
    bladeAssembly.add(hamonMesh);

    // Habaki (Brass Blade Collar with Japanese character texture)
    const habakiGeo = new THREE.BoxGeometry(0.18, 0.14, 0.08);
    const habakiMat = new THREE.MeshStandardMaterial({
      color: 0xd4af37,
      metalness: 0.9,
      roughness: 0.25
    });
    const habakiMesh = new THREE.Mesh(habakiGeo, habakiMat);
    habakiMesh.position.set(-0.1, 0, 0);
    bladeAssembly.add(habakiMesh);

    // Tsuba (Circular Handguard)
    const tsubaGeo = new THREE.CylinderGeometry(0.34, 0.34, 0.035, 32);
    const tsubaMat = new THREE.MeshStandardMaterial({
      color: 0x14151a,
      metalness: 0.85,
      roughness: 0.3
    });
    const tsubaMesh = new THREE.Mesh(tsubaGeo, tsubaMat);
    tsubaMesh.rotation.z = Math.PI / 2;
    tsubaMesh.position.set(-0.2, 0, 0);
    bladeAssembly.add(tsubaMesh);

    // Tsuka (Hilt Handle Wrapped in Diamond White Cord)
    const tsukaCurve = new THREE.LineCurve3(
      new THREE.Vector3(-0.22, 0, 0),
      new THREE.Vector3(-1.85, -0.06, 0)
    );
    const tsukaGeo = new THREE.TubeGeometry(tsukaCurve, 24, 0.075, 8, false);
    const tsukaMat = new THREE.MeshStandardMaterial({
      color: 0xf8fafc,
      roughness: 0.8,
      metalness: 0.1
    });
    const tsukaMesh = new THREE.Mesh(tsukaGeo, tsukaMat);
    bladeAssembly.add(tsukaMesh);

    // Kashira (Pommel Cap)
    const kashiraGeo = new THREE.CylinderGeometry(0.08, 0.07, 0.1, 16);
    const kashiraMat = new THREE.MeshStandardMaterial({
      color: 0x14151a,
      metalness: 0.9,
      roughness: 0.2
    });
    const kashiraMesh = new THREE.Mesh(kashiraGeo, kashiraMat);
    kashiraMesh.rotation.z = Math.PI / 2;
    kashiraMesh.position.set(-1.85, -0.06, 0);
    bladeAssembly.add(kashiraMesh);

    // -------------------------------------------------------------------------
    // 2. Saya Assembly (Lower Tier: Lacquered Scabbard)
    // -------------------------------------------------------------------------
    const sayaAssembly = new THREE.Group();
    katanaMaster.add(sayaAssembly);

    const sayaCurve = new THREE.QuadraticBezierCurve3(
      new THREE.Vector3(-0.1, 0, 0),
      new THREE.Vector3(2.2, 0.09, 0),
      new THREE.Vector3(4.6, 0.29, 0)
    );
    const sayaGeo = new THREE.TubeGeometry(sayaCurve, 64, 0.088, 12, false);
    const sayaMat = new THREE.MeshStandardMaterial({
      color: 0x0c0d11, // Deep black Urushi lacquer
      roughness: 0.2,
      metalness: 0.5
    });
    const sayaMesh = new THREE.Mesh(sayaGeo, sayaMat);
    sayaAssembly.add(sayaMesh);

    // Splattered Crimson Lacquer Accent on Saya (Matches Red splash in Video)
    const redAccentCurve = new THREE.QuadraticBezierCurve3(
      new THREE.Vector3(0.2, 0.01, 0),
      new THREE.Vector3(0.9, 0.04, 0),
      new THREE.Vector3(1.6, 0.07, 0)
    );
    const redAccentGeo = new THREE.TubeGeometry(redAccentCurve, 32, 0.092, 12, false);
    const redAccentMat = new THREE.MeshStandardMaterial({
      color: 0xe11d48,
      roughness: 0.3,
      metalness: 0.3
    });
    const redAccentMesh = new THREE.Mesh(redAccentGeo, redAccentMat);
    sayaAssembly.add(redAccentMesh);

    // Kurikata Ring & Brass Fittings
    const ringGeo = new THREE.TorusGeometry(0.12, 0.02, 16, 32);
    const ringMat = new THREE.MeshStandardMaterial({
      color: 0xd4af37,
      metalness: 0.9,
      roughness: 0.2
    });
    const ringMesh = new THREE.Mesh(ringGeo, ringMat);
    ringMesh.position.set(0.6, 0.04, 0);
    sayaAssembly.add(ringMesh);

    // -------------------------------------------------------------------------
    // Kinematic Animation Loop (Exact Video-Accurate Transformations)
    // -------------------------------------------------------------------------
    const handleMouseMove = (e: MouseEvent) => {
      const rect = container.getBoundingClientRect();
      const x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
      const y = -(((e.clientY - rect.top) / rect.height) * 2 - 1);
      stateRef.current.mouse.x = x;
      stateRef.current.mouse.y = y;
    };
    window.addEventListener("mousemove", handleMouseMove, { passive: true });

    const animate = () => {
      animId = requestAnimationFrame(animate);

      const p = stateRef.current.progress;

      // 1. Initial Angle: Starts diagonal at -22deg (frame_0000) and levels to 0deg (frame_0100)
      const levelProgress = Math.min(1, p / 0.15);
      const startAngle = -0.38; // ~ -22 degrees
      const targetAngle = 0.0;
      katanaMaster.rotation.z = THREE.MathUtils.lerp(startAngle, targetAngle, levelProgress);

      // 2. Vertical Split: As scroll enters the phrases (p > 0.15), blade rises and scabbard drops
      const splitProgress = Math.min(1, Math.max(0, (p - 0.15) / 0.7));
      const targetBladeY = splitProgress * 0.95; // Moves up to upper tier
      const targetSayaY = -splitProgress * 0.95; // Moves down to lower tier
      bladeAssembly.position.y = THREE.MathUtils.lerp(bladeAssembly.position.y, targetBladeY, 0.1);
      sayaAssembly.position.y = THREE.MathUtils.lerp(sayaAssembly.position.y, targetSayaY, 0.1);

      // 3. Horizontal Unsheathe Translation:
      // Scabbard glides to the left by up to 2.8 units
      // Blade glides to the right by up to 3.2 units across the screen
      const unsheatheX = splitProgress * 3.0;
      sayaAssembly.position.x = THREE.MathUtils.lerp(sayaAssembly.position.x, -unsheatheX, 0.1);
      bladeAssembly.position.x = THREE.MathUtils.lerp(bladeAssembly.position.x, unsheatheX * 0.6, 0.1);

      // 4. Subtle interactive 3D mouse parallax
      const rotY = stateRef.current.mouse.x * 0.12;
      const rotX = -stateRef.current.mouse.y * 0.08;
      katanaMaster.rotation.y = THREE.MathUtils.lerp(katanaMaster.rotation.y, rotY, 0.06);
      katanaMaster.rotation.x = THREE.MathUtils.lerp(katanaMaster.rotation.x, rotX, 0.06);

      // 5. Breathing levitation
      katanaMaster.position.y = Math.sin(Date.now() * 0.0018) * 0.04;

      renderer.render(scene, camera);
    };
    animate();

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
