import "./globals.css";
import React from "react";

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
    <html lang="en">
      <body className="bg-slate-950 text-slate-100 min-h-screen antialiased selection:bg-red-500 selection:text-white">
        {children}
      </body>
    </html>
  );
}
