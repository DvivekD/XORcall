import numpy as np
from detector.vision_forensics import VideoForensicsEngine

def test_vision_forensics_synthetic_frame():
    engine = VideoForensicsEngine()
    
    # 1. Generate clean optical frame (natural sensor noise distribution)
    np.random.seed(42)
    clean_frame = np.clip(np.random.normal(120, 25, (300, 300, 3)), 0, 255).astype(np.uint8)
        
    res_clean = engine.analyze_frame(clean_frame)
    assert "deepfake_confidence" in res_clean
    assert res_clean["is_synthetic"] is False
    assert res_clean["verdict"] == "AUTHENTIC_OPTICAL_FEED"

    # 2. Generate frame with artificial high-frequency checkerboard pattern (simulating upscaler grid)
    checkerboard_frame = np.zeros((300, 300, 3), dtype=np.uint8)
    checkerboard_frame[::4, ::4, :] = 255
    res_synth = engine.analyze_frame(checkerboard_frame)
    assert res_synth["fft_lattice_score"] > 0.40
