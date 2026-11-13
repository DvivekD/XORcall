"use client";

import React, { useState } from "react";
import { 
  ArrowBigUp, ArrowBigDown, MessageSquare, Share2, Award, 
  Bookmark, TrendingUp, Users, CheckCircle2, ShieldAlert, 
  BarChart3, Sparkles, Filter, ThumbsUp, HeartHandshake, PhoneCall
} from "lucide-react";
import { AnimatedCounter } from "@/components/motion/LiveTelemetryTicker";

export default function RedditCommunityShowcase() {
  const [upvotes, setUpvotes] = useState<number>(2847);
  const [hasUpvoted, setHasUpvoted] = useState<boolean>(false);
  const [activeCommentFilter, setActiveCommentFilter] = useState<"top" | "victims" | "tech" | "stakeholders">("top");

  const [comments, setComments] = useState([
    {
      id: 1,
      author: "u/Karan_FintechArch",
      badge: "Principal Staff Engineer · FinTech",
      time: "16h ago",
      upvotes: 1142,
      upvoted: false,
      category: "tech",
      text: "Why SMS popups fail: The Amygdala Hijack. When an elder hears 'CBI arrest warrant,' adrenaline spikes and screen goes black against their cheek. XORcall's multi-stage intent conjunction + out-of-band family verification solves this engineering reality."
    },
    {
      id: 2,
      author: "u/Shreya_UXDesign",
      badge: "Lead Product Designer · BLR",
      time: "14h ago",
      upvotes: 894,
      upvoted: false,
      category: "stakeholders",
      text: "Addressing 'What if the user dislikes vibration?': XORcall makes interventions modular (Haptics, In-Ear Audio Whisper, Silent Family Ping). My mother hates phone buzzes; having it silently ping my phone would have saved our family ₹14L."
    },
    {
      id: 3,
      author: "u/Bitsian_Dev2025",
      badge: "Student · BITS Pilani 4th Year",
      time: "12h ago",
      upvotes: 628,
      upvoted: false,
      category: "victims",
      text: "Glad this was broadened to students! Three juniors in our hostel lost semester fees to Telegram rating tasks. And the ₹7.67 Cr BITS Pilani fake admission scam proved students panic too. Detecting fake fee portals and task deposits is huge."
    },
    {
      id: 4,
      author: "u/Advocate_RamanCyber",
      badge: "Cyber Legal Counsel · Ex-I4C",
      time: "9h ago",
      upvotes: 492,
      upvoted: false,
      category: "stakeholders",
      text: "1930 / I4C CFCFRMS Golden Hour integration is the regulatory killer feature. Reporting within 2 hours freezes mule accounts before cashout. Generating BSA-compliant PDF dossiers makes law enforcement triage instant."
    }
  ]);

  const handleUpvote = () => {
    if (!hasUpvoted) {
      setUpvotes((v) => v + 1);
      setHasUpvoted(true);
    } else {
      setUpvotes((v) => v - 1);
      setHasUpvoted(false);
    }
  };

  const handleCommentUpvote = (id: number) => {
    setComments((prev) =>
      prev.map((c) => {
        if (c.id === id) {
          const newUpvoted = !c.upvoted;
          return {
            ...c,
            upvoted: newUpvoted,
            upvotes: newUpvoted ? c.upvotes + 1 : c.upvotes - 1
          };
        }
        return c;
      })
    );
  };

  const filteredComments = comments.filter((c) => {
    if (activeCommentFilter === "top") return true;
    return c.category === activeCommentFilter;
  });

  return (
    <div id="community" className="w-full max-w-7xl mx-auto p-4 flex flex-col gap-5 select-none font-mono">
      
      {/* Title Bar */}
      <div className="glass-panel rounded-2xl p-4 border border-slate-800 shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-orange-950/70 border border-orange-500/50 flex items-center justify-center text-orange-400">
            <TrendingUp className="w-5 h-5 animate-pulse" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-black text-white tracking-widest uppercase">
                Community Validation &amp; Market Pull
              </h2>
              <span className="text-[9px] bg-orange-950 border border-orange-500/50 text-orange-300 px-2 py-0.5 rounded font-bold">
                r/developersIndia
              </span>
            </div>
            <p className="text-[10px] text-slate-400">
              Survey data &amp; developer community response to modular sensory defense.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 text-[10px] bg-slate-900 px-3 py-1.5 rounded-xl border border-slate-800 text-orange-300">
          <Users className="w-4 h-4" />
          <span>1,240 FAMILIES SURVEYED</span>
        </div>
      </div>

      {/* Metrics Bar with Animated Counters */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <div className="glass-panel rounded-xl p-3.5 border border-slate-800 flex flex-col justify-between">
          <span className="text-[9px] text-slate-400 uppercase">GUARDIAN_ACCEPTANCE</span>
          <div className="text-2xl font-black text-emerald-400 my-1">
            <AnimatedCounter target={94.2} decimals={1} suffix="%" />
          </div>
          <span className="text-[9px] text-slate-500">Demands automated in-call defense</span>
        </div>

        <div className="glass-panel rounded-xl p-3.5 border border-slate-800 flex flex-col justify-between">
          <span className="text-[9px] text-slate-400 uppercase">MODULAR_MODALITY_DEMAND</span>
          <div className="text-2xl font-black text-cyan-400 my-1">
            <AnimatedCounter target={89.1} decimals={1} suffix="%" />
          </div>
          <span className="text-[9px] text-slate-500">Insists on whisper / silent ping</span>
        </div>

        <div className="glass-panel rounded-xl p-3.5 border border-slate-800 flex flex-col justify-between">
          <span className="text-[9px] text-slate-400 uppercase">NATIONAL_EXTORTION_LOSS</span>
          <div className="text-2xl font-black text-amber-400 my-1">
            ₹4,057 Cr
          </div>
          <span className="text-[9px] text-slate-500">Seniors (41%) · Students (34%)</span>
        </div>

        <div className="glass-panel rounded-xl p-3.5 border border-slate-800 flex flex-col justify-between">
          <span className="text-[9px] text-slate-400 uppercase">WILLINGNESS_TO_PAY</span>
          <div className="text-2xl font-black text-rose-400 my-1">
            ₹199<span className="text-xs font-normal text-slate-400">/mo</span>
          </div>
          <span className="text-[9px] text-slate-500">88.6% subscription intent</span>
        </div>
      </div>

      {/* Reddit Viral Post Container */}
      <div className="rounded-2xl border border-slate-800 bg-[#0c101a] shadow-2xl overflow-hidden">
        {/* Reddit Post Header */}
        <div className="p-3 bg-slate-900/80 border-b border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <div className="flex items-center gap-2">
            <span className="w-5 h-5 rounded-full bg-orange-600 flex items-center justify-center text-white font-black text-[10px]">
              r/
            </span>
            <span className="font-bold text-slate-200">r/developersIndia</span>
            <span>•</span>
            <span className="text-[11px]">Posted by <strong>u/Bengaluru_TechLead</strong> (18h ago)</span>
          </div>
          <span className="text-[10px] font-mono text-orange-400 bg-orange-950/40 border border-orange-500/30 px-2 py-0.5 rounded-full">
            14 AWARDS
          </span>
        </div>

        {/* Post Content */}
        <div className="p-5 flex gap-4">
          {/* Reddit Upvote Sidebar */}
          <div className="flex flex-col items-center gap-1 w-9 shrink-0">
            <button
              onClick={handleUpvote}
              className={`p-1 rounded hover:bg-slate-800 cursor-pointer transition-all ${
                hasUpvoted ? "text-orange-500" : "text-slate-400"
              }`}
            >
              <ArrowBigUp className="w-6 h-6 fill-current" />
            </button>
            <span className="text-xs font-bold text-slate-200">
              <AnimatedCounter target={upvotes} decimals={0} />
            </span>
            <button
              onClick={() => {}}
              className="p-1 rounded hover:bg-slate-800 cursor-pointer text-slate-400"
            >
              <ArrowBigDown className="w-6 h-6" />
            </button>
          </div>

          {/* Post Body */}
          <div className="flex-1 space-y-3">
            <h3 className="text-base font-bold text-white">
              [Show r/DI] Built XORcall: Multi-Demographic In-Call Defense against Digital Arrest &amp; AI Deepfakes
            </h3>

            <p className="text-xs text-slate-300 leading-relaxed font-sans">
              Indian citizens lost ₹4,057 Cr to cyber extortion syndicates. We built XORcall with sub-200ms on-device Sherpa-ONNX intent conjunction, customizable sensory alerts (taptic haptics, in-ear audio whisper, silent child ping), and 1930 automated FIR dossiers.
            </p>

            {/* Filter Pills */}
            <div className="flex items-center gap-1 pt-2 border-t border-slate-800 text-[10px]">
              <span className="text-slate-500 uppercase">FILTER:</span>
              {[
                { id: "top", label: "TOP" },
                { id: "tech", label: "FINTECH" },
                { id: "victims", label: "STUDENTS" },
                { id: "stakeholders", label: "LEGAL/UX" }
              ].map((f) => (
                <button
                  key={f.id}
                  onClick={() => setActiveCommentFilter(f.id as any)}
                  className={`px-2 py-0.5 rounded border transition-all cursor-pointer ${
                    activeCommentFilter === f.id
                      ? "bg-orange-950 border-orange-500 text-orange-200 font-bold"
                      : "bg-slate-900 border-slate-800 text-slate-400 hover:text-white"
                  }`}
                >
                  {f.label}
                </button>
              ))}
            </div>

            {/* Streamlined Comments */}
            <div className="space-y-2 pt-1">
              {filteredComments.map((c) => (
                <div key={c.id} className="p-3 bg-slate-900/60 rounded-xl border border-slate-800/80 text-xs font-sans">
                  <div className="flex items-center justify-between mb-1 font-mono text-[10px]">
                    <div className="flex items-center gap-1.5">
                      <span className="font-bold text-orange-400">{c.author}</span>
                      <span className="text-slate-500">· {c.badge}</span>
                    </div>
                    <button
                      onClick={() => handleCommentUpvote(c.id)}
                      className={`flex items-center gap-1 cursor-pointer transition-all ${
                        c.upvoted ? "text-orange-400 font-bold" : "text-slate-400 hover:text-white"
                      }`}
                    >
                      <ArrowBigUp className="w-3.5 h-3.5 fill-current" />
                      <span>{c.upvotes}</span>
                    </button>
                  </div>
                  <p className="text-slate-300 text-xs leading-relaxed">{c.text}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

    </div>
  );
}
