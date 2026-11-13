"use client";

import React from "react";
import Link from "next/link";
import { ArrowLeft, MessageSquare, TrendingUp, ShieldCheck, PhoneCall } from "lucide-react";
import RedditCommunityShowcase from "@/components/community/RedditCommunityShowcase";

export default function CommunityPage() {
  return (
    <div className="min-h-screen bg-[#07090e] text-white flex flex-col justify-between selection:bg-orange-500 selection:text-white">
      {/* Top Navigation Bar */}
      <header className="sticky top-0 z-50 backdrop-blur-xl bg-[#07090e]/85 border-b border-white/10 px-6 py-4">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link
              href="/"
              className="flex items-center gap-2 text-xs font-mono text-slate-400 hover:text-white transition-colors px-3 py-1.5 rounded-full border border-white/10 hover:border-white/30 bg-white/5"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Back to Landing</span>
            </Link>

            <div className="hidden sm:flex items-center gap-2 text-xs font-mono">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              <span className="text-slate-400">Endpoint:</span>
              <span className="text-orange-400 font-bold">/community</span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <span className="text-[10px] font-mono uppercase text-slate-500 hidden md:inline">
              r/developersIndia · Thread Simulation
            </span>
            <a
              href="tel:1930"
              className="text-xs font-mono font-bold px-4 py-1.5 rounded-full bg-red-600 hover:bg-red-500 text-white shadow-lg flex items-center gap-1.5 transition-all"
            >
              <PhoneCall className="w-3.5 h-3.5" />
              <span>1930 Helpline</span>
            </a>
          </div>
        </div>
      </header>

      {/* Main Community Showcase Container */}
      <main className="flex-1 py-8 px-4 sm:px-6">
        <RedditCommunityShowcase />
      </main>

      {/* Footer Bar */}
      <footer className="border-t border-white/10 py-6 px-6 text-center text-xs font-mono text-slate-500 bg-[#05070a]">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-3">
          <span>XORcall Neural Cyber Guardian · Team KANYARASI</span>
          <span>BSA Section 63 Compliant · Freezes Mule Accounts via 1930</span>
        </div>
      </footer>
    </div>
  );
}
