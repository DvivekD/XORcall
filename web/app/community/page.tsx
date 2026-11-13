"use client";

import React, { useState } from "react";
import Link from "next/link";
import { ArrowLeft, PhoneCall, Sparkles, Image as ImageIcon } from "lucide-react";
import RedditCommunityShowcase from "@/components/community/RedditCommunityShowcase";

export default function CommunityPage() {
  const [viewMode, setViewMode] = useState<"interactive" | "snapshot">("interactive");

  return (
    <div className="min-h-screen bg-[#07090e] text-white flex flex-col justify-between selection:bg-[#ff4500] selection:text-white">
      {/* Sticky Top Header */}
      <header className="sticky top-0 z-50 backdrop-blur-xl bg-[#07090e]/90 border-b border-white/10 px-4 sm:px-6 py-3.5">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-3 sm:gap-4">
            <Link
              href="/"
              className="flex items-center gap-2 text-xs font-mono text-slate-300 hover:text-white transition-colors px-3 py-1.5 rounded-full border border-white/15 hover:border-white/30 bg-white/5"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Back to Landing</span>
            </Link>

            <div className="flex items-center gap-2 text-xs font-mono">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              <span className="text-slate-400">Endpoint:</span>
              <span className="text-orange-400 font-bold">/community</span>
            </div>
          </div>

          {/* Mode Switcher Pills */}
          <div className="flex items-center gap-2">
            <div className="flex items-center bg-white/5 border border-white/10 rounded-full p-0.5 text-xs font-mono">
              <button
                onClick={() => setViewMode("interactive")}
                className={`px-3 py-1 rounded-full transition-all cursor-pointer flex items-center gap-1.5 ${
                  viewMode === "interactive"
                    ? "bg-orange-600 text-white font-bold shadow-md"
                    : "text-slate-400 hover:text-white"
                }`}
              >
                <Sparkles className="w-3.5 h-3.5" />
                <span>Interactive</span>
              </button>
              <button
                onClick={() => setViewMode("snapshot")}
                className={`px-3 py-1 rounded-full transition-all cursor-pointer flex items-center gap-1.5 ${
                  viewMode === "snapshot"
                    ? "bg-orange-600 text-white font-bold shadow-md"
                    : "text-slate-400 hover:text-white"
                }`}
              >
                <ImageIcon className="w-3.5 h-3.5" />
                <span>Screenshot</span>
              </button>
            </div>

            <a
              href="tel:1930"
              className="text-xs font-mono font-bold px-3.5 py-1.5 rounded-full bg-red-600 hover:bg-red-500 text-white shadow-lg flex items-center gap-1.5 transition-all hidden sm:flex"
            >
              <PhoneCall className="w-3.5 h-3.5" />
              <span>1930 Helpline</span>
            </a>
          </div>
        </div>
      </header>

      {/* Main Content: Interactive or High-Resolution Static View */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-3 sm:p-6 flex flex-col items-center">
        {viewMode === "interactive" ? (
          <div className="w-full animate-in fade-in duration-300">
            <RedditCommunityShowcase />
          </div>
        ) : (
          <div className="w-full rounded-2xl overflow-hidden border border-white/10 shadow-2xl bg-[#0e1117] transition-all animate-in fade-in duration-300">
            <img
              src="/images/reddit-community.jpg"
              alt="r/developersIndia XORcall Community Validation Thread"
              className="w-full h-auto object-contain select-none block"
              loading="eager"
            />
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-white/10 py-5 px-6 text-center text-xs font-mono text-slate-500 bg-[#05070a]">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>XORcall Neural Cyber Guardian · Team KANYARASI</span>
          <span>r/developersIndia Community Feedback · BSA Section 63 Compliant</span>
        </div>
      </footer>
    </div>
  );
}
