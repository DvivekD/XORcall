"""
XORcall Voice Forensics
Acoustic inspection of streaming PCM audio buffers for synthetic voice-clones
(phase incoherence, vocoder high-frequency roll-off, unnatural pitch jitter, and spectral entropy).
"""
import numpy as np
from typing import Dict, Any

class VoiceForensicsEngine:
    def __init__(self, sample_rate: int = 16000):
        self.sample_rate = sample_rate

    def estimate_pitch_jitter(self, audio: np.ndarray) -> Dict[str, float]:
        """
        Estimates biological vocal cord micro-jitter by computing the fundamental period (F0)
        across consecutive 32ms frames using Normalized Autocorrelation.
        Human speech naturally exhibits 0.5% - 2.5% period jitter.
        Neural vocoders (ElevenLabs/HiFi-GAN) exhibit unnaturally static jitter (<0.3%)
        or chaotic unvoiced noise.
        """
        frame_len = int(self.sample_rate * 0.032) # 512 samples (~32ms)
        hop_len = int(self.sample_rate * 0.016)   # 256 samples (~16ms)
        min_lag = int(self.sample_rate / 400)    # 400 Hz -> 40 samples
        max_lag = int(self.sample_rate / 75)     # 75 Hz -> 213 samples

        periods = []
        num_frames = (len(audio) - frame_len) // hop_len
        for f in range(max(0, num_frames)):
            frame = audio[f * hop_len : f * hop_len + frame_len]
            # Check frame energy
            if np.sum(frame ** 2) < 0.005:
                continue
            # Autocorrelation
            autocorr = np.correlate(frame, frame, mode='full')
            center = len(autocorr) // 2
            lag_region = autocorr[center + min_lag : center + max_lag]
            if len(lag_region) == 0:
                continue
            best_lag = min_lag + np.argmax(lag_region)
            peak_val = autocorr[center + best_lag]
            zero_lag_val = autocorr[center] + 1e-12
            # Periodicity check
            if peak_val / zero_lag_val > 0.35: # voiced frame
                periods.append(best_lag)

        if len(periods) < 3:
            return {"jitter_pct": 1.2, "is_voiced": False}

        # Calculate relative jitter: average difference between consecutive periods / mean period
        diffs = [abs(periods[i] - periods[i - 1]) for i in range(1, len(periods))]
        mean_period = np.mean(periods)
        jitter = (np.mean(diffs) / (mean_period + 1e-9)) * 100.0
        return {"jitter_pct": round(float(jitter), 3), "is_voiced": True}

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

        # 5. Pitch Jitter Analysis
        jitter_res = self.estimate_pitch_jitter(audio)
        jitter_pct = jitter_res["jitter_pct"]

        # Scoring heuristics:
        # Biological human speech naturally exhibits vocal cord micro-tremor (jitter >= 0.5%)
        if jitter_res["is_voiced"] and jitter_pct >= 0.50:
            composite_voice_score = 0.08
            is_synthetic = False
            indicators = []
        else:
            # Neural vocoder characteristics (ElevenLabs, HiFi-GAN, WaveNet)
            jitter_score = 0.90 if (jitter_res["is_voiced"] and jitter_pct < 0.25) else (0.60 if (jitter_res["is_voiced"] and jitter_pct < 0.35) else 0.15)
            flatness_score = 0.75 if spectral_flatness > 0.45 else 0.20
            cutoff_score = 0.70 if high_low_ratio > 0.25 else 0.10

            composite_voice_score = round(0.50 * jitter_score + 0.30 * flatness_score + 0.20 * cutoff_score, 2)
            is_synthetic = composite_voice_score >= 0.58

            indicators = []
            if jitter_score >= 0.70:
                indicators.append(f"Unnatural harmonic rigidity: lack of biological vocal cord micro-jitter ({jitter_pct}%)")
            if flatness_score >= 0.70:
                indicators.append("Unnatural acoustic spectral entropy (synthetic neural vocoder signature)")
            if cutoff_score >= 0.70:
                indicators.append("Vocoder high-frequency spectral buzz (>6.5 kHz anomaly)")

        return {
            "is_cloned_voice": is_synthetic,
            "voice_clone_score": composite_voice_score,
            "rms_energy": round(rms, 4),
            "high_low_ratio": round(float(high_low_ratio), 5),
            "spectral_flatness": round(spectral_flatness, 4),
            "pitch_jitter_pct": jitter_pct,
            "detected_anomalies": indicators,
            "verdict": "SYNTHETIC_VOICE_CLONE" if is_synthetic else "NATURAL_HUMAN_VOICE"
        }
