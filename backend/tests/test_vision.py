import io
import cv2
import numpy as np
import pytest
from fastapi.testclient import TestClient

from detector.vision_forensics import VideoForensicsEngine
from services.vision_inspector import VisionInspector
from main import app

client = TestClient(app)

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

def test_vision_inspector_human_vs_deepfake_hud_badges():
    inspector = VisionInspector()

    # Natural optical frame: Real human simulation
    np.random.seed(42)
    human_frame = np.clip(np.random.normal(128, 20, (256, 256, 3)), 0, 255).astype(np.uint8)
    res_human = inspector.inspect_frame(human_frame)

    assert res_human["is_synthetic"] is False
    assert res_human["biometric_authenticity_score"] >= 0.70
    assert "VERIFIED BIOMETRIC IDENTITY" in res_human["hud_badge"]
    assert res_human["azimuthal_lattice_anomaly"] <= 0.40

    # Synthetic deepfake frame: High-frequency azimuthal grid lattice
    deepfake_frame = np.zeros((256, 256, 3), dtype=np.uint8)
    deepfake_frame[::3, ::3, :] = 255
    deepfake_frame[::4, :, :] = 180
    res_deepfake = inspector.inspect_frame(deepfake_frame)

    assert res_deepfake["is_synthetic"] is True
    assert res_deepfake["deepfake_confidence"] >= 0.60
    assert "CRITICAL VIDEO DEEPFAKE DETECTED" in res_deepfake["hud_badge"]
    assert len(res_deepfake["detected_anomalies"]) > 0

def test_api_v1_inspect_video_frame_multipart():
    # Create test JPEG bytes
    np.random.seed(42)
    frame = np.zeros((120, 120, 3), dtype=np.uint8)
    frame[::4, ::4, :] = 255
    success, buffer = cv2.imencode(".jpg", frame)
    assert success

    response = client.post(
        "/api/v1/inspect/video-frame",
        files={"file": ("frame.jpg", io.BytesIO(buffer.tobytes()), "image/jpeg")}
    )
    assert response.status_code == 200
    data = response.json()
    assert "is_synthetic" in data
    assert "deepfake_confidence" in data
    assert "hud_badge" in data
    assert "azimuthal_lattice_anomaly" in data

def test_api_v1_inspect_video_frame_json_simulation():
    # Human test
    resp_human = client.post(
        "/api/v1/inspect/video-frame/json",
        json={"simulation_mode": "human"}
    )
    assert resp_human.status_code == 200
    data_human = resp_human.json()
    assert data_human["is_synthetic"] is False
    assert "VERIFIED BIOMETRIC IDENTITY" in data_human["hud_badge"]

    # Deepfake test
    resp_deepfake = client.post(
        "/api/v1/inspect/video-frame/json",
        json={"simulation_mode": "deepfake"}
    )
    assert resp_deepfake.status_code == 200
    data_deepfake = resp_deepfake.json()
    assert data_deepfake["is_synthetic"] is True
    assert "CRITICAL VIDEO DEEPFAKE DETECTED" in data_deepfake["hud_badge"]
