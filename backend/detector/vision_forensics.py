"""
XORcall Vision Forensics (KavachVision)
Real-time inspection of video call frames for deepfake face-swapping,
boundary seam anomalies, and 2D-FFT neural upscaler frequency lattice artifacts.
"""
import cv2
import numpy as np
from typing import Dict, Any, Tuple, Optional
from services.vision_inspector import VisionInspector

class VideoForensicsEngine:
    def __init__(self):
        self.inspector = VisionInspector()
        # Keep direct cascade references for backward compatibility
        self.face_cascade = self.inspector.face_cascade

    def analyze_frame_bytes(self, image_bytes: bytes) -> Dict[str, Any]:
        """Decode image buffer and run forensic deepfake evaluation."""
        result = self.inspector.inspect_frame_bytes(image_bytes)
        # Add legacy keys for complete backwards compatibility
        result["fft_lattice_score"] = result.get("azimuthal_lattice_anomaly", 0.0)
        result["boundary_seam_score"] = result.get("boundary_blur_warping", 0.0)
        return result

    def analyze_frame(self, frame: np.ndarray) -> Dict[str, Any]:
        """Runs forensic tests on frame."""
        result = self.inspector.inspect_frame(frame)
        result["fft_lattice_score"] = result.get("azimuthal_lattice_anomaly", 0.0)
        result["boundary_seam_score"] = result.get("boundary_blur_warping", 0.0)
        # Support legacy verdict naming if needed
        if not result["is_synthetic"] and result["verdict"] == "AUTHENTIC_HUMAN_FEED":
            result["verdict"] = "AUTHENTIC_OPTICAL_FEED"
        return result
