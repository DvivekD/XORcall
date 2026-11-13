import "./globals.css";
import React from "react";
import { Space_Grotesk, Space_Mono } from "next/font/google";

const spaceGrotesk = Space_Grotesk({
  subsets: ["latin"],
  variable: "--font-space-grotesk",
  display: "swap",
});

const spaceMono = Space_Mono({
  weight: ["400", "700"],
  subsets: ["latin"],
  variable: "--font-space-mono",
  display: "swap",
});

export const metadata = {
  title: "XORcall — Real-Time Anti-Digital Arrest & Family Deepfake Defense",
  description: "Autonomous real-time on-device cyber guardian stopping digital arrest and family AI voice/video deepfakes in-flight.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className={`${spaceGrotesk.variable} ${spaceMono.variable}`}>
      <body className="bg-[#0f1014] text-white min-h-screen antialiased selection:bg-[#e11d48] selection:text-white font-sans">
        {children}
      </body>
    </html>
  );
}
