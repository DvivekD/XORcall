"use client";

import React, { useState } from "react";
import { 
  ShieldCheck, AlertOctagon, Smartphone, Wifi, Battery, MapPin, 
  PhoneCall, Volume2, Lock, FileText, CheckCircle2, Zap, Radio, Activity
} from "lucide-react";
import { AnimatedCounter } from "@/components/motion/LiveTelemetryTicker";

export default function GuardianDashboard() {
  const [sirenActive, setSirenActive] = useState<boolean>(false);
  const [bankingLocked, setBankingLocked] = useState<boolean>(true);
  const [safeConfirmed, setSafeConfirmed] = useState<boolean>(false);

  const handleTriggerSiren = () => {
    setSirenActive(true);
    setTimeout(() => setSirenActive(false), 5000);
  };

  return (
    <div className="w-full max-w-7xl mx-auto p-4 flex flex-col gap-5 select-none font-mono">
      
      {/* Protected Profile Top Bar */}
      <div className="glass-panel rounded-2xl p-4 border border-slate-800 shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-emerald-600 to-teal-500 flex items-center justify-center text-white shadow-lg shadow-emerald-600/30">
            <Smartphone className="w-5 h-5 animate-pulse" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-bold text-white uppercase tracking-wider">
                Protected Device: Dad (Samsung Galaxy S23)
              </h2>
              <span className="text-[9px] bg-emerald-950 border border-emerald-500/40 text-emerald-400 px-2 py-0.5 rounded font-bold">
                GUARDIAN_LINK_ACTIVE
              </span>
            </div>
            <p className="text-[10px] text-slate-400 mt-0.5">
              PAIR: Likhith (Son) · SENSORY_SHIELD: 60 FPS NOMINAL
            </p>
          </div>
        </div>

        {/* Real-time Hardware Telemetry Badges */}
        <div className="flex items-center gap-2 text-[10px] text-slate-300">
          <div className="flex items-center gap-1.5 bg-slate-900/80 px-2.5 py-1 rounded-lg border border-slate-800">
            <Battery className="w-3.5 h-3.5 text-emerald-400" />
            <AnimatedCounter target={84} decimals={0} suffix="%" />
          </div>
          <div className="flex items-center gap-1.5 bg-slate-900/80 px-2.5 py-1 rounded-lg border border-slate-800">
            <Wifi className="w-3.5 h-3.5 text-cyan-400" /> Home_Fiber_5G
          </div>
          <div className="flex items-center gap-1.5 bg-slate-900/80 px-2.5 py-1 rounded-lg border border-slate-800">
            <MapPin className="w-3.5 h-3.5 text-rose-400" /> Bengaluru_IN
          </div>
        </div>
      </div>

      {/* Grid of Passive Sanity & Overrides */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
        
        {/* Left Column: Passive Telemetry & Proof-of-Life (5 cols) */}
        <div className="lg:col-span-5 flex flex-col gap-4">
          <div className="glass-panel rounded-2xl p-4 border border-slate-800">
            <div className="flex items-center justify-between mb-3 text-xs">
              <span className="font-bold text-white uppercase flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-emerald-400" /> Passive Proof-of-Life Metrics
              </span>
              <span className="text-[9px] text-emerald-400 font-bold bg-emerald-950 px-2 py-0.5 rounded border border-emerald-500/30">
                LIVE_SENSOR_FEED
              </span>
            </div>

            <div className="space-y-2 text-[11px]">
              <div className="p-2.5 bg-slate-900/70 rounded-xl border border-slate-800 flex items-center justify-between">
                <span className="text-slate-400">CHILD_DEVICE_STATE:</span>
                <span className="text-emerald-400 font-bold">Normal Resting (1.02G)</span>
              </div>

              <div className="p-2.5 bg-slate-900/70 rounded-xl border border-slate-800 flex items-center justify-between">
                <span className="text-slate-400">GEO_LOCATION_BEACON:</span>
                <span className="text-cyan-300 font-bold">Campus Wi-Fi (RVCE)</span>
              </div>

              <div className="p-2.5 bg-slate-900/70 rounded-xl border border-slate-800 flex items-center justify-between">
                <span className="text-slate-400">PEDOMETER_10M_VELOCITY:</span>
                <span className="text-slate-200 font-bold">84 Steps (Active Walk)</span>
              </div>

              <div className="p-2.5 bg-slate-900/70 rounded-xl border border-slate-800 flex items-center justify-between">
                <span className="text-slate-400">IMPACT_DECELERATION:</span>
                <span className="text-emerald-400 font-bold">0.0G (Zero Crash Detected)</span>
              </div>
            </div>
          </div>

          {/* 1-Tap Out-of-band Verification Button */}
          <div className="glass-panel-glow rounded-2xl p-4 border border-slate-800">
            <div className="text-xs font-bold text-white mb-1 uppercase tracking-wide">
              Simulate Reverse Safe-Ping Response
            </div>
            <p className="text-[10px] text-slate-400 mb-2.5">
              Simulates child tapping the out-of-band mobile verification notification.
            </p>
            <button
              onClick={() => setSafeConfirmed(true)}
              className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-bold py-2 px-3 rounded-xl text-xs flex items-center justify-center gap-2 cursor-pointer transition-all shadow-lg shadow-emerald-600/30"
            >
              <CheckCircle2 className="w-4 h-4" /> 
              {safeConfirmed ? "VERIFIED: CHILD TAPPED 'I AM SAFE'" : "TEST CHILD SAFE-PING TAP"}
            </button>
          </div>
        </div>

        {/* Right Column: Remote Interventions & Official ER Triage (7 cols) */}
        <div className="lg:col-span-7 flex flex-col gap-4">
          
          {/* Remote Overrides */}
          <div className="glass-panel rounded-2xl p-4 border border-slate-800">
            <div className="flex items-center justify-between mb-3 text-xs">
              <span className="font-bold text-white uppercase flex items-center gap-2">
                <AlertOctagon className="w-4 h-4 text-rose-400" /> Immediate Remote Controls
              </span>
              <span className="text-[9px] text-slate-500">OOB_SMS_CHANNEL</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {/* Trigger Siren */}
              <button
                onClick={handleTriggerSiren}
                className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                  sirenActive 
                    ? "bg-red-600 border-red-400 text-white animate-pulse shadow-lg shadow-red-600/50" 
                    : "bg-slate-900/80 border-slate-800 hover:border-red-500/50 text-slate-200"
                }`}
              >
                <div className="flex items-center gap-2 font-bold text-xs mb-1 text-red-300">
                  <Volume2 className="w-4 h-4" /> TRIGGER EMERGENCY SIREN
                </div>
                <div className="text-[10px] text-slate-400">
                  Overrides silent/DND and blasts loud police alert through phone speaker.
                </div>
              </button>

              {/* Lock Banking Apps */}
              <button
                onClick={() => setBankingLocked(!bankingLocked)}
                className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                  bankingLocked 
                    ? "bg-amber-950/40 border-amber-500/70 text-amber-200 shadow-lg shadow-amber-950/40" 
                    : "bg-slate-900/80 border-slate-800 text-slate-400"
                }`}
              >
                <div className="flex items-center gap-2 font-bold text-xs mb-1 text-amber-300">
                  <Lock className="w-4 h-4" /> BANKING APP LOCKDOWN
                </div>
                <div className="text-[10px] text-slate-400">
                  {bankingLocked 
                    ? "ACTIVE: UPI, YONO, HDFC locked until call disconnects." 
                    : "DISABLED: Banking access open."}
                </div>
              </button>
            </div>
          </div>

          {/* Verified ER Hospital Directory */}
          <div className="glass-panel rounded-2xl p-4 border border-slate-800">
            <div className="flex items-center justify-between mb-2 text-xs">
              <span className="font-bold text-white uppercase flex items-center gap-2">
                <PhoneCall className="w-4 h-4 text-cyan-400" /> Verified Hospital ER Landlines
              </span>
              <span className="text-[9px] text-slate-500">1-TAP TRIAGE</span>
            </div>

            <div className="space-y-1.5 text-xs">
              <div className="p-2.5 bg-slate-900/70 rounded-xl border border-slate-800 flex items-center justify-between">
                <div>
                  <div className="font-bold text-slate-200 text-xs">Apollo Hospitals Trauma ER</div>
                  <div className="text-[10px] text-slate-400">Helpline: 1066 / +91-80-2630-4050</div>
                </div>
                <a 
                  href="tel:1066" 
                  className="px-2.5 py-1 bg-cyan-950 hover:bg-cyan-900 border border-cyan-500/50 text-cyan-300 rounded-lg text-[10px] font-bold"
                >
                  CALL ER DESK
                </a>
              </div>

              <div className="p-2.5 bg-slate-900/70 rounded-xl border border-slate-800 flex items-center justify-between">
                <div>
                  <div className="font-bold text-slate-200 text-xs">Manipal Hospital Emergency</div>
                  <div className="text-[10px] text-slate-400">Desk: 080-2222-1111 / +91-80-2502-4444</div>
                </div>
                <a 
                  href="tel:08022221111" 
                  className="px-2.5 py-1 bg-cyan-950 hover:bg-cyan-900 border border-cyan-500/50 text-cyan-300 rounded-lg text-[10px] font-bold"
                >
                  CALL ER DESK
                </a>
              </div>
            </div>
          </div>

        </div>

      </div>

    </div>
  );
}
