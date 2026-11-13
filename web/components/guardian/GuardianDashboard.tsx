"use client";

import React, { useState } from "react";
import { 
  ShieldCheck, AlertOctagon, Smartphone, Wifi, Battery, MapPin, 
  PhoneCall, Volume2, Lock, FileText, CheckCircle2 
} from "lucide-react";

export default function GuardianDashboard() {
  const [sirenActive, setSirenActive] = useState<boolean>(false);
  const [bankingLocked, setBankingLocked] = useState<boolean>(true);
  const [safeConfirmed, setSafeConfirmed] = useState<boolean>(false);

  const handleTriggerSiren = () => {
    setSirenActive(true);
    setTimeout(() => setSirenActive(false), 5000);
  };

  return (
    <div className="w-full max-w-7xl mx-auto p-4 flex flex-col gap-6">
      {/* Top Banner: Protected Elder Profile */}
      <div className="glass-panel rounded-2xl p-6 border border-slate-700/60 shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-500 flex items-center justify-center text-white shadow-lg shadow-emerald-600/30">
            <Smartphone className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-bold text-white">Dad&apos;s Phone (Samsung Galaxy S23)</h2>
              <span className="text-[10px] bg-emerald-950 border border-emerald-500/40 text-emerald-400 px-2 py-0.5 rounded-full font-mono font-bold">
                KAVACH GUARD ACTIVE
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-0.5">
              Paired Guardian Profile: Likhith (Son) · Real-Time Sensory Shield Enabled
            </p>
          </div>
        </div>

        {/* Telemetry Pills */}
        <div className="flex items-center gap-3 text-xs font-mono text-slate-300">
          <div className="flex items-center gap-1.5 bg-slate-900/80 px-3 py-1.5 rounded-xl border border-slate-800">
            <Battery className="w-4 h-4 text-emerald-400" /> 84%
          </div>
          <div className="flex items-center gap-1.5 bg-slate-900/80 px-3 py-1.5 rounded-xl border border-slate-800">
            <Wifi className="w-4 h-4 text-indigo-400" /> Home_Fiber_5G
          </div>
          <div className="flex items-center gap-1.5 bg-slate-900/80 px-3 py-1.5 rounded-xl border border-slate-800">
            <MapPin className="w-4 h-4 text-rose-400" /> Bengaluru
          </div>
        </div>
      </div>

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left: Passive Proof-of-Life & Physical Sanity (5 cols) */}
        <div className="lg:col-span-5 flex flex-col gap-5">
          <div className="glass-panel rounded-2xl p-5 border border-slate-800">
            <h3 className="text-sm font-bold text-white mb-3 flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-emerald-400" /> Passive Proof-of-Life Telemetry
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Continuous physical sanity metrics. When scammers claim a son is in custody or dead, his live device telemetry disproves the claim automatically.
            </p>

            <div className="space-y-3">
              <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 flex items-center justify-between text-xs">
                <span className="text-slate-400">Son&apos;s Device Physical State:</span>
                <span className="font-mono text-emerald-400 font-bold">Normal Resting (1.02G)</span>
              </div>

              <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 flex items-center justify-between text-xs">
                <span className="text-slate-400">Son&apos;s Active Location:</span>
                <span className="font-mono text-indigo-300 font-bold">Campus Wi-Fi (RVCE)</span>
              </div>

              <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 flex items-center justify-between text-xs">
                <span className="text-slate-400">Step Activity (Last 10m):</span>
                <span className="font-mono text-slate-200">84 Steps (Active Walk)</span>
              </div>

              <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 flex items-center justify-between text-xs">
                <span className="text-slate-400">Collision Impact Deceleration:</span>
                <span className="font-mono text-emerald-400 font-bold">0.0G (No Crash Detected)</span>
              </div>
            </div>
          </div>

          {/* Quick Out-of-band Verification Button */}
          <div className="glass-panel rounded-2xl p-5 border border-slate-800">
            <h3 className="text-sm font-bold text-white mb-2">Simulate Son&apos;s Response</h3>
            <p className="text-xs text-slate-400 mb-3">
              When Dad receives a virtual kidnapping call, you receive an instant push notification.
            </p>
            <button
              onClick={() => setSafeConfirmed(true)}
              className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-semibold py-2.5 px-4 rounded-xl text-xs flex items-center justify-center gap-2 cursor-pointer transition-all shadow-lg shadow-emerald-600/20"
            >
              <CheckCircle2 className="w-4 h-4" /> 
              {safeConfirmed ? "Verified: Son Tapped 'I AM 100% SAFE'" : "Simulate Son Tapping 'I AM SAFE'"}
            </button>
          </div>
        </div>

        {/* Right: Remote Emergency Overrides & 1930 Integration (7 cols) */}
        <div className="lg:col-span-7 flex flex-col gap-5">
          {/* Remote Overrides */}
          <div className="glass-panel rounded-2xl p-5 border border-slate-800">
            <h3 className="text-sm font-bold text-white mb-3 flex items-center gap-2">
              <AlertOctagon className="w-4 h-4 text-rose-400" /> Immediate Remote Interventions
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Break the psychological isolation trance remotely from your phone if Dad is trapped on a digital arrest call.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              {/* Trigger Siren */}
              <button
                onClick={handleTriggerSiren}
                className={`p-4 rounded-xl border text-left transition-all cursor-pointer ${
                  sirenActive 
                    ? "bg-red-600 border-red-400 text-white animate-pulse" 
                    : "bg-slate-900/70 border-slate-800 hover:border-red-500/50 text-slate-200"
                }`}
              >
                <div className="flex items-center gap-2 font-bold text-sm mb-1 text-red-300">
                  <Volume2 className="w-4 h-4" /> Trigger Emergency Siren
                </div>
                <div className="text-xs text-slate-400">
                  Overrides silent/DND mode and blasts a loud police warning siren through his speakers to interrupt the scammer.
                </div>
              </button>

              {/* Lock Banking Apps */}
              <button
                onClick={() => setBankingLocked(!bankingLocked)}
                className={`p-4 rounded-xl border text-left transition-all cursor-pointer ${
                  bankingLocked 
                    ? "bg-amber-950/40 border-amber-500/60 text-amber-200" 
                    : "bg-slate-900/70 border-slate-800 text-slate-200"
                }`}
              >
                <div className="flex items-center gap-2 font-bold text-sm mb-1 text-amber-300">
                  <Lock className="w-4 h-4" /> Banking Apps Lockout
                </div>
                <div className="text-xs text-slate-400">
                  {bankingLocked 
                    ? "ACTIVE: SBI YONO, HDFC, and Google Pay are blocked until the call ends." 
                    : "DISABLED: Banking access is open."}
                </div>
              </button>
            </div>
          </div>

          {/* Official Hospital Emergency Triage Directory */}
          <div className="glass-panel rounded-2xl p-5 border border-slate-800">
            <h3 className="text-sm font-bold text-white mb-2 flex items-center gap-2">
              <PhoneCall className="w-4 h-4 text-indigo-400" /> Verified Hospital ER Landlines (Direct Triage)
            </h3>
            <p className="text-xs text-slate-400 mb-3">
              If an unknown caller claims a family member is in a hospital emergency, verify with the official ER desk in 1-tap:
            </p>

            <div className="space-y-2">
              <div className="p-2.5 bg-slate-900/60 rounded-xl border border-slate-800 flex items-center justify-between text-xs">
                <div>
                  <div className="font-semibold text-slate-200">Apollo Hospitals Emergency Care</div>
                  <div className="text-[11px] text-slate-400">National Triage Helpline: 1066 / +91-80-2630-4050</div>
                </div>
                <a 
                  href="tel:1066" 
                  className="px-3 py-1 bg-indigo-600/30 hover:bg-indigo-600/50 border border-indigo-500/50 text-indigo-300 rounded-lg text-xs font-semibold"
                >
                  Call ER Desk
                </a>
              </div>

              <div className="p-2.5 bg-slate-900/60 rounded-xl border border-slate-800 flex items-center justify-between text-xs">
                <div>
                  <div className="font-semibold text-slate-200">Manipal Hospital Emergency &amp; Trauma</div>
                  <div className="text-[11px] text-slate-400">Landline Desk: 080-2222-1111 / +91-80-2502-4444</div>
                </div>
                <a 
                  href="tel:08022221111" 
                  className="px-3 py-1 bg-indigo-600/30 hover:bg-indigo-600/50 border border-indigo-500/50 text-indigo-300 rounded-lg text-xs font-semibold"
                >
                  Call ER Desk
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
