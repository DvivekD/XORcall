import numpy as np
from detector.voice_forensics import VoiceForensicsEngine
from detector.cloud_stt import CloudSTTEngine

def test_voice_forensics_natural_vs_synthetic():
    engine = VoiceForensicsEngine(sample_rate=16000)
    
    # 1. Synthetic-like audio: pure single sine wave with zero HF energy and perfectly uniform tone
    t = np.linspace(0, 1.0, 16000, endpoint=False)
    synthetic_wave = (0.5 * np.sin(2 * np.pi * 300 * t) * 32767).astype(np.int16)
    synthetic_pcm = synthetic_wave.tobytes()
    res_synthetic = engine.analyze_pcm_buffer(synthetic_pcm)
    assert "voice_clone_score" in res_synthetic
    assert res_synthetic["high_low_ratio"] < 0.01

    # 2. Natural-like audio: complex multi-harmonic signal with noise/breath turbulence
    np.random.seed(42)
    natural_wave = (
        0.3 * np.sin(2 * np.pi * 150 * t) +
        0.2 * np.sin(2 * np.pi * 300 * t) +
        0.15 * np.sin(2 * np.pi * 1200 * t) +
        0.1 * np.sin(2 * np.pi * 3200 * t) +
        0.05 * np.sin(2 * np.pi * 7000 * t) +
        0.02 * np.random.normal(0, 1, 16000) # natural breath noise
    )
    natural_wave = (natural_wave / np.max(np.abs(natural_wave)) * 30000).astype(np.int16)
    natural_pcm = natural_wave.tobytes()
    res_natural = engine.analyze_pcm_buffer(natural_pcm)
    assert res_natural["high_low_ratio"] > 0.01

def test_cloud_stt_pcm_to_wav():
    stt = CloudSTTEngine()
    # 1 second of silence
    pcm = np.zeros(16000, dtype=np.int16).tobytes()
    wav_bytes = stt.pcm_to_wav_bytes(pcm, sample_rate=16000)
    assert len(wav_bytes) == len(pcm) + 44
    assert wav_bytes[:4] == b"RIFF"
    assert wav_bytes[8:12] == b"WAVE"

def test_cloud_stt_short_audio_rejection():
    stt = CloudSTTEngine()
    pcm = np.zeros(200, dtype=np.int16).tobytes() # very short
    res = stt.transcribe_pcm(pcm)
    assert res["transcript"] == ""
    assert "Audio chunk too short" in res.get("error", "")
