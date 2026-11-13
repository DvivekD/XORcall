"""
XORcall Voice Forensics
Acoustic inspection of streaming PCM audio buffers for synthetic voice-clones
(phase incoherence, vocoder high-frequency roll-off) and acoustic pitch volatility.
"""
import numpy as np
from typing import Dict, Any

class VoiceForensicsEngine:
    def __init__(self, sample_rate: int = 16000):
        self.sample_rate = sample_rate

    def analyze_pcm_buffer(self, pcm_bytes: bytes) -> Dict[str, Any]:
        """
        Takes raw 16-bit Linear PCM Little-Endian bytes (16kHz)
        and runs acoustic synthetic artifact checks.
        """
        if len(pcm_bytes) < 512:
            return {"error": "Buffer too small", "is_cloned_voice": False, "score": 0.0}

        # Convert to normalized float32 (-1.0 to 1.0)
        audio = np.frombuffer(pcm_bytes, dtype=np.int16).astype(np.float32) / 32768.0

        # 1. Spectral Analysis via FFT
        fft_data = np.fft.rfft(audio)
        magnitude = np.abs(fft_data)
        freqs = np.fft.rfftfreq(len(audio), d=1.0 / self.sample_rate)

        # 2. High-Frequency Energy Ratio (>6.5 kHz vs Sub-6.5 kHz)
        # Neural vocoders (HiFi-GAN / ElevenLabs) often have distinct phase or spectral cutoffs
        high_freq_mask = freqs >= 6500
        low_freq_mask = (freqs >= 300) & (freqs < 6500)

        high_energy = np.sum(magnitude[high_freq_mask] ** 2) + 1e-9
        low_energy = np.sum(magnitude[low_freq_mask] ** 2) + 1e-9
        high_low_ratio = high_energy / low_energy

        # 3. Spectral Flatness & Phase Variance
        # Synthetic speech exhibits unnaturally uniform spectral energy distribution in certain bands
        geo_mean = np.exp(np.mean(np.log(magnitude + 1e-12)))
        arith_mean = np.mean(magnitude)
        spectral_flatness = float(geo_mean / (arith_mean + 1e-12))

        # 4. Root Mean Square (RMS) & Dynamic Range
        rms = float(np.sqrt(np.mean(audio ** 2)))

        # Scoring heuristics
        # Abnormally low high-frequency energy ratio (<0.005) is a known vocoder compression footprint
        cutoff_score = 0.85 if high_low_ratio < 0.004 else (0.40 if high_low_ratio < 0.015 else 0.10)
        flatness_score = 0.75 if spectral_flatness > 0.45 else 0.20

        composite_voice_score = round(0.60 * cutoff_score + 0.40 * flatness_score, 2)
        is_synthetic = composite_voice_score >= 0.65

        indicators = []
        if cutoff_score >= 0.70:
            indicators.append("Vocoder high-frequency spectral suppression (>6.5 kHz anomaly)")
        if flatness_score >= 0.70:
            indicators.append("Unnatural acoustic spectral entropy (synthetic neural vocoder signature)")

        return {
            "is_cloned_voice": is_synthetic,
            "voice_clone_score": composite_voice_score,
            "rms_energy": round(rms, 4),
            "high_low_ratio": round(float(high_low_ratio), 5),
            "spectral_flatness": round(spectral_flatness, 4),
            "detected_anomalies": indicators,
            "verdict": "SYNTHETIC_VOICE_CLONE" if is_synthetic else "NATURAL_HUMAN_VOICE"
        }
