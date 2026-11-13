"use client";

import React, { useEffect, useRef } from "react";
import * as THREE from "three";
import { GLTFLoader } from "three/examples/jsm/loaders/GLTFLoader.js";

interface CyberKatanaProps {
  progress?: number; // 0.0 to 1.0
  className?: string;
}

export default function CyberKatana3D({
  progress = 0,
  className = ""
}: CyberKatanaProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const stateRef = useRef({
    progress,
    mouse: { x: 0, y: 0 },
    isLoaded: false
  });

  useEffect(() => {
    stateRef.current.progress = progress;
  }, [progress]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    let animId: number;
    const width = container.clientWidth || window.innerWidth;
    const height = container.clientHeight || window.innerHeight;

    // 1. Scene & Close Perspective Camera (katana spans screen width)
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(40, width / height, 0.1, 100);
    camera.position.set(0, 0, 4.4);

    // 2. High-Performance WebGL Renderer with ACES Tone Mapping
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

    // 3. Studio Lighting (Video-Accurate Highlights on Steel & Urushi Lacquer)
    const ambientLight = new THREE.AmbientLight(0xffffff, 1.2);
    scene.add(ambientLight);

    const keyLight = new THREE.DirectionalLight(0xffffff, 3.8);
    keyLight.position.set(4, 7, 8);
    scene.add(keyLight);

    const topFill = new THREE.DirectionalLight(0xffffff, 1.4);
    topFill.position.set(0, 9, 3);
    scene.add(topFill);

    // Vermilion Red Rim Light (#e11d48) grazing Urushi scabbard
    const vermilionRim = new THREE.DirectionalLight(0xe11d48, 5.2);
    vermilionRim.position.set(-8, -4, 4);
    scene.add(vermilionRim);

    // Vivid Cyan Rim Light (#38bdf8) grazing polished steel & gold habaki
    const cyanRim = new THREE.DirectionalLight(0x38bdf8, 4.2);
    cyanRim.position.set(8, 4, 5);
    scene.add(cyanRim);

    // 4. Master Hierarchy Setup
    const katanaMaster = new THREE.Group();
    scene.add(katanaMaster);

    // Alignment Pivot: Shifts sword center so the entire length is framed symmetrically
    const katanaPivot = new THREE.Group();
    katanaPivot.position.set(-1.5, 0, 0);
    katanaPivot.rotation.y = -Math.PI / 2;
    katanaMaster.add(katanaPivot);

    // Independent assemblies for kinematic separation
    const bladeAssembly = new THREE.Group();
    const sayaAssembly = new THREE.Group();
    katanaPivot.add(bladeAssembly);
    katanaPivot.add(sayaAssembly);

    // -------------------------------------------------------------------------
    // Fallback Procedural Katana (Ensures instant 0ms visual until GLB parses)
    // -------------------------------------------------------------------------
    const proxyGroup = new THREE.Group();
    katanaMaster.add(proxyGroup);

    const pBladeCurve = new THREE.QuadraticBezierCurve3(
      new THREE.Vector3(-0.1, 0, 0),
      new THREE.Vector3(2.2, 0.09, 0),
      new THREE.Vector3(4.5, 0.28, 0)
    );
    const pBladeMesh = new THREE.Mesh(
      new THREE.TubeGeometry(pBladeCurve, 64, 0.05, 8, false),
      new THREE.MeshStandardMaterial({ color: 0xebedf2, metalness: 0.98, roughness: 0.12 })
    );
    const pHabakiMesh = new THREE.Mesh(
      new THREE.BoxGeometry(0.18, 0.14, 0.08),
      new THREE.MeshStandardMaterial({ color: 0xd4af37, metalness: 0.95, roughness: 0.2 })
    );
    pHabakiMesh.position.set(-0.1, 0, 0);
    const pSayaCurve = new THREE.QuadraticBezierCurve3(
      new THREE.Vector3(-0.1, 0, 0),
      new THREE.Vector3(2.2, 0.09, 0),
      new THREE.Vector3(4.6, 0.29, 0)
    );
    const pSayaMesh = new THREE.Mesh(
      new THREE.TubeGeometry(pSayaCurve, 64, 0.085, 12, false),
      new THREE.MeshStandardMaterial({ color: 0x0c0d11, roughness: 0.2, metalness: 0.5 })
    );
    proxyGroup.add(pBladeMesh);
    proxyGroup.add(pHabakiMesh);
    proxyGroup.add(pSayaMesh);

    // -------------------------------------------------------------------------
    // 5. Load Staged 10.3 MB GLB Model (`/models/katana.glb`)
    // -------------------------------------------------------------------------
    const loader = new GLTFLoader();
    loader.load(
      "/models/katana.glb",
      (gltf) => {
        const rootScene = gltf.scene;

        // Traverse & tune materials for studio lighting and lacquer depth
        rootScene.traverse((child) => {
          if ((child as THREE.Mesh).isMesh) {
            const mesh = child as THREE.Mesh;
            mesh.castShadow = true;
            mesh.receiveShadow = true;

            if (mesh.material) {
              const mat = (
                Array.isArray(mesh.material) ? mesh.material[0] : mesh.material
              ) as THREE.MeshStandardMaterial;
              mat.needsUpdate = true;
              mat.side = THREE.DoubleSide;

              // Polished Damascus Razor Steel (Node Cube / Material.002)
              if (mesh.name.includes("Cube") || mat.name.includes("Material.002")) {
                mat.metalness = 0.98;
                mat.roughness = 0.08;
              }

              // Gold Habaki Collar with 'べ' character (Node Plane.006 / MY GOLD COLOR)
              if (
                mesh.name.includes("Plane.006") ||
                mesh.name.includes("MY GOLD COLOR") ||
                mat.name.includes("GOLD")
              ) {
                mat.color.setHex(0xd4af37);
                mat.metalness = 0.95;
                mat.roughness = 0.18;
                mat.emissive = new THREE.Color(0xd4af37);
                mat.emissiveIntensity = 0.25;
              }

              // Red-marbled Urushi Scabbard (Node Cube.002 / Material.004)
              if (mesh.name.includes("Cube.002") || mat.name.includes("Material.004")) {
                mat.roughness = 0.14;
                mat.metalness = 0.4;
              }
            }
          }
        });

        // Hide duplicate instances: Node 44 (Circle.008) and Node 65 (Cube.006)
        const dupCircle008 = rootScene.getObjectByName("Circle.008");
        if (dupCircle008) {
          dupCircle008.visible = false;
          dupCircle008.traverse((c) => (c.visible = false));
        }

        const dupCube006 = rootScene.getObjectByName("Cube.006");
        if (dupCube006) {
          dupCube006.visible = false;
          dupCube006.traverse((c) => (c.visible = false));
        }

        // Identify primary Blade & Saya assemblies
        // Node 3: Circle.004 (Blade, Habaki with gold 'べ', Tsuba, Tsuka handle)
        // Node 28: Cube.002 (Red-marbled Saya with sageo cord)
        const bladeObj = rootScene.getObjectByName("Circle.004");
        const sayaObj = rootScene.getObjectByName("Cube.002");

        if (bladeObj && sayaObj) {
          // Add rootScene temporarily to compute clean world transforms
          scene.add(rootScene);
          rootScene.updateMatrixWorld(true);

          // Reparent blade & saya into independent kinematic groups
          bladeAssembly.attach(bladeObj);
          sayaAssembly.attach(sayaObj);

          // Clean up unused nodes from scene
          scene.remove(rootScene);

          // Remove procedural proxy
          katanaMaster.remove(proxyGroup);
          stateRef.current.isLoaded = true;
        }
      },
      undefined,
      (err) => {
        console.warn("[CyberKatana3D] GLTF load fallback to procedural:", err);
      }
    );

    // -------------------------------------------------------------------------
    // 6. Kinematic Animation Loop (Pop-up Entrance + 5-Phase Scroll Unsheathe)
    // -------------------------------------------------------------------------
    const handleMouseMove = (e: MouseEvent) => {
      const rect = container.getBoundingClientRect();
      const x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
      const y = -(((e.clientY - rect.top) / rect.height) * 2 - 1);
      stateRef.current.mouse.x = x;
      stateRef.current.mouse.y = y;
    };
    window.addEventListener("mousemove", handleMouseMove, { passive: true });

    const mountTime = performance.now();
    const ENTRANCE_DURATION = 1600; // 1.6s smooth entrance

    const animate = () => {
      animId = requestAnimationFrame(animate);

      const now = performance.now();
      const p = stateRef.current.progress;

      // Calculate initial pop-up entrance animation (eased 0 to 1)
      const entranceTime = Math.min(1, Math.max(0, (now - mountTime) / ENTRANCE_DURATION));
      const entranceEase = 1 - Math.pow(1 - entranceTime, 3); // Cubic ease-out

      // 1. Angle:
      // At mount, slides into diagonal (-22 deg = -0.384 rad).
      // As scroll proceeds, levels smoothly from -22 deg to 0 deg.
      const diagonalAngle = -0.384;
      const levelProgress = Math.min(1, p / 0.18);
      const targetZ = THREE.MathUtils.lerp(diagonalAngle * entranceEase, 0.0, levelProgress);
      katanaMaster.rotation.z = THREE.MathUtils.lerp(katanaMaster.rotation.z, targetZ, 0.1);

      // 2. Unsheathe Kinematics:
      // On initial mount: reveals gold habaki by ~15% (offset ~0.45 units along sword axis)
      const initialRevealOffset = 0.45 * entranceEase;

      // In scroll mode: splitProgress drives separation across the 5 phases
      const splitProgress = Math.min(1, Math.max(0, (p - 0.12) / 0.75));

      // Vertical separation: blade rises into upper tier, saya drops to lower tier
      const targetBladeY = splitProgress * 0.95;
      const targetSayaY = -splitProgress * 0.95;
      bladeAssembly.position.y = THREE.MathUtils.lerp(bladeAssembly.position.y, targetBladeY, 0.1);
      sayaAssembly.position.y = THREE.MathUtils.lerp(sayaAssembly.position.y, targetSayaY, 0.1);

      // Horizontal separation:
      // Saya glides left by up to 2.8 units; blade glides right by up to 2.0 units
      const scrollSayaX = -splitProgress * 2.8;
      const scrollBladeX = splitProgress * 2.0;

      // Combine entrance reveal with scroll glide
      // In katanaPivot coordinates (pointing along X):
      // Saya unsheathes by initialRevealOffset + scrollSayaX
      const targetSayaX = (1 - splitProgress) * initialRevealOffset + scrollSayaX;
      const targetBladeX = scrollBladeX;

      sayaAssembly.position.x = THREE.MathUtils.lerp(sayaAssembly.position.x, targetSayaX, 0.1);
      bladeAssembly.position.x = THREE.MathUtils.lerp(bladeAssembly.position.x, targetBladeX, 0.1);

      // Also animate proxy group if GLTF is still streaming
      if (!stateRef.current.isLoaded) {
        pSayaMesh.position.x = sayaAssembly.position.x;
        pBladeMesh.position.x = bladeAssembly.position.x;
        pHabakiMesh.position.x = bladeAssembly.position.x - 0.1;
      }

      // 3. Subtle interactive 3D mouse parallax
      const rotY = stateRef.current.mouse.x * 0.12;
      const rotX = -stateRef.current.mouse.y * 0.08;
      katanaMaster.rotation.y = THREE.MathUtils.lerp(katanaMaster.rotation.y, rotY, 0.06);
      katanaMaster.rotation.x = THREE.MathUtils.lerp(katanaMaster.rotation.x, rotX, 0.06);

      // 4. Subtle ambient breathing levitation
      katanaMaster.position.y = Math.sin(now * 0.0018) * 0.04;

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
