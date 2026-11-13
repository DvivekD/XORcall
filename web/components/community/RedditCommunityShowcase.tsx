"use client";

import React from "react";

export default function RedditCommunityShowcase() {
  return (
    <div id="community" className="w-full max-w-7xl mx-auto p-2 sm:p-4">
      <div className="w-full rounded-2xl overflow-hidden border border-white/10 shadow-2xl bg-[#0e1117]">
        <img
          src="/images/reddit-community.jpg"
          alt="r/developersIndia XORcall Community Validation Thread"
          className="w-full h-auto object-contain select-none block"
          loading="eager"
        />
      </div>
    </div>
  );
}
