"use client";

import { useEffect, useRef } from "react";

export default function StringTuneScroll() {
  const initialized = useRef(false);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    let stringTuneInstance: any = null;

    import("@fiddle-digital/string-tune")
      .then((mod) => {
        try {
          const StringTune = mod.default || mod.StringTune;
          const StringParallax = mod.StringParallax;
          if (StringTune && typeof StringTune.getInstance === "function") {
            stringTuneInstance = StringTune.getInstance();
            if (StringParallax && typeof stringTuneInstance.use === "function") {
              stringTuneInstance.use(StringParallax);
            }
            if (typeof stringTuneInstance.start === "function") {
              stringTuneInstance.start(60);
            }
          }
        } catch (e) {
          console.warn("StringTune dynamic initialization note:", e);
        }
      })
      .catch((e) => {
        console.warn("StringTune dynamic import note:", e);
      });

    return () => {
      try {
        if (stringTuneInstance && typeof stringTuneInstance.destroy === "function") {
          stringTuneInstance.destroy();
        }
      } catch {
        // cleanup safe
      }
    };
  }, []);

  return null;
}
