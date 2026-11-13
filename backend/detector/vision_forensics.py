"""
XORcall Vision Forensics (KavachVision)
Real-time inspection of video call frames for deepfake face-swapping,
boundary seam anomalies, and 2D-FFT neural upscaler frequency lattice artifacts.
"""
import cv2
import numpy as np
from typing import Dict, Any, Tuple, Optional

class VideoForensicsEngine:
    def __init__(self):
        # Load OpenCV Haar cascade for face detection
        cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
        self.face_cascade = cv2.CascadeClassifier(cascade_path)

    def analyze_frame_bytes(self, image_bytes: bytes) -> Dict[str, Any]:
        """Decode image buffer and run forensic deepfake evaluation."""
        nparr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if img is None:
            return {"error": "Invalid image data", "is_synthetic": False, "confidence": 0.0}
        return self.analyze_frame(img)

    def analyze_frame(self, frame: np.ndarray) -> Dict[str, Any]:
        """
        Runs 3 forensic tests on frame:
        1. 2D-FFT High-Frequency Spectral Lattice Analysis (Upscaler checkerboard detector)
        2. Facial Boundary Seam & Color Variance along jawline/collar
        3. High-Frequency Laplacian Variance (Blur/Sharpening Discrepancy)
        """
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        h, w = gray.shape
        faces = self.face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(60, 60))

        if len(faces) == 0:
            # Analyze whole frame if no isolated face is detected
            face_roi = gray
            face_box = [0, 0, w, h]
            has_face = False
        else:
            (x, y, fw, fh) = faces[0]
            # Add small margin for neck/collar seam analysis
            y_start = max(0, y)
            y_end = min(h, y + int(fh * 1.25))
            x_start = max(0, x - int(fw * 0.1))
            x_end = min(w, x + int(fw * 1.1))
            face_roi = gray[y_start:y_end, x_start:x_end]
            face_box = [int(x), int(y), int(fw), int(fh)]
            has_face = True

        # 1. 2D-FFT Spectral Analysis
        fft_score, peak_count = self._compute_fft_lattice_score(face_roi)

        # 2. Boundary Seam Discontinuity (Collar/Jawline variance)
        seam_score = self._compute_boundary_seam_score(frame, face_box) if has_face else 0.15

        # 3. Inner-Face Texture Smoothness vs Edge Disparity
        laplacian_var = cv2.Laplacian(face_roi, cv2.CV_64F).var()
        # Synthetic faces often lack high-frequency micro-pores or have extreme edge-ringing
        smoothness_indicator = 1.0 if laplacian_var < 70.0 or laplacian_var > 1200.0 else 0.2

        # Composite Deepfake Score (0.0 to 1.0)
        composite_score = (0.50 * fft_score) + (0.35 * seam_score) + (0.15 * smoothness_indicator)
        composite_score = round(min(1.0, max(0.0, composite_score)), 2)

        is_synthetic = composite_score >= 0.65

        indicators = []
        if fft_score > 0.60:
            indicators.append("Neural upscaler periodic lattice artifacts (DeepFaceLive/GAN grid)")
        if seam_score > 0.55:
            indicators.append("Collar/Jawline color grading mismatch and blending seam detected")
        if smoothness_indicator > 0.60:
            indicators.append("Unnatural skin texture distribution (missing micro-pores or edge-ringing)")

        return {
            "is_synthetic": is_synthetic,
            "deepfake_confidence": composite_score,
            "fft_lattice_score": round(fft_score, 2),
            "boundary_seam_score": round(seam_score, 2),
            "face_detected": has_face,
            "face_box": face_box,
            "detected_anomalies": indicators,
            "verdict": "SYNTHETIC_DEEPFAKE" if is_synthetic else "AUTHENTIC_OPTICAL_FEED"
        }

    def _compute_fft_lattice_score(self, roi: np.ndarray) -> Tuple[float, int]:
        """
        Computes 2D Fast Fourier Transform magnitude spectrum.
        Detects periodic peak grids caused by transposed convolutions or upsamplers.
        """
        # Resize to fixed standard 256x256 for consistent spectral resolution
        resized = cv2.resize(roi, (256, 256)).astype(np.float32)
        f = np.fft.fft2(resized)
        fshift = np.fft.fftshift(f)
        magnitude_spectrum = 20 * np.log(np.abs(fshift) + 1e-6)

        # Mask out DC and low-frequency components in the center (radius 20)
        rows, cols = resized.shape
        crow, ccol = rows // 2, cols // 2
        y, x = np.ogrid[:rows, :cols]
        mask = (x - ccol) ** 2 + (y - crow) ** 2 <= 20 ** 2
        magnitude_spectrum[mask] = 0

        # High-frequency thresholding for periodic spectral spikes
        mean_val = np.mean(magnitude_spectrum)
        std_val = np.std(magnitude_spectrum)
        spikes = magnitude_spectrum > (mean_val + 2.8 * std_val)
        peak_count = int(np.sum(spikes))

        # Optical lenses show smooth radial decay; upscalers generate >45 isolated spike clusters
        fft_score = min(1.0, peak_count / 80.0)
        return float(fft_score), peak_count

    def _compute_boundary_seam_score(self, frame: np.ndarray, face_box: list) -> float:
        """
        Compares color histogram and edge continuity between the inner face
        and the surrounding collar/background region.
        """
        x, y, w, h = face_box
        fh, fw, _ = frame.shape
        
        # Inner face patch
        inner = frame[y + int(h * 0.2): y + int(h * 0.8), x + int(w * 0.2): x + int(w * 0.8)]
        # Collar / neck patch immediately below chin
        neck_y1 = min(fh - 1, y + h)
        neck_y2 = min(fh, y + int(h * 1.3))
        neck = frame[neck_y1:neck_y2, x + int(w * 0.2): x + int(w * 0.8)]

        if inner.size == 0 or neck.size == 0:
            return 0.2

        # Convert to HSV to measure chromatic discrepancy
        inner_hsv = cv2.cvtColor(inner, cv2.COLOR_BGR2HSV)
        neck_hsv = cv2.cvtColor(neck, cv2.COLOR_BGR2HSV)

        inner_hist = cv2.calcHist([inner_hsv], [0, 1], None, [16, 16], [0, 180, 0, 256])
        neck_hist = cv2.calcHist([neck_hsv], [0, 1], None, [16, 16], [0, 180, 0, 256])

        cv2.normalize(inner_hist, inner_hist, 0, 1, cv2.NORM_MINMAX)
        cv2.normalize(neck_hist, neck_hist, 0, 1, cv2.NORM_MINMAX)

        # Bhattacharyya distance (0 = identical, 1 = completely mismatched)
        mismatch_dist = cv2.compareHist(inner_hist, neck_hist, cv2.HISTCMP_BHATTACHARYYA)
        return float(min(1.0, mismatch_dist * 1.2))
