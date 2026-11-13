"use client";

import { useEffect, useRef } from "react";

export default function StringTuneScroll() {
  const initialized = useRef(false);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    let stringTuneInstance: any = null;

    // Dynamically load StringTune on client side
    import("@fiddle-digital/string-tune")
      .then((mod) => {
        try {
          const { StringTune, StringParallax, StringMagnetic, StringTilt, StringSpotlight } = mod;
          if (StringTune && typeof StringTune.getInstance === "function") {
            stringTuneInstance = StringTune.getInstance();
            
            if (StringParallax && typeof stringTuneInstance.use === "function") {
              stringTuneInstance.use(StringParallax);
            }
            if (StringMagnetic && typeof stringTuneInstance.use === "function") {
              stringTuneInstance.use(StringMagnetic);
            }
            if (StringTilt && typeof stringTuneInstance.use === "function") {
              stringTuneInstance.use(StringTilt);
            }
            if (StringSpotlight && typeof stringTuneInstance.use === "function") {
              stringTuneInstance.use(StringSpotlight);
            }

            if (typeof stringTuneInstance.start === "function") {
              stringTuneInstance.start(60);
            }
          }
        } catch (e) {
          console.warn("StringTune init notice:", e);
        }
      })
      .catch((e) => {
        console.warn("StringTune import notice:", e);
      });

    return () => {
      try {
        if (stringTuneInstance && typeof stringTuneInstance.destroy === "function") {
          stringTuneInstance.destroy();
        }
      } catch {
        // safe cleanup
      }
    };
  }, []);

  return null;
}
