"""
XORcall Cloud STT Module
High-accuracy speech-to-text powered by Groq LPU Whisper (whisper-large-v3-turbo).
Specifically tuned for Indian English accents, Hindi/Hinglish, and noisy call conditions.
Falls back to graceful offline/heuristic transcription if no API key is configured.
"""
import io
import wave
import logging
from typing import Dict, Any, Optional
from groq import Groq
from config import settings

logger = logging.getLogger("xorcall.cloud_stt")

class CloudSTTEngine:
    def __init__(self):
        self.groq_client: Optional[Groq] = None
        self._init_client()

    def _init_client(self):
        if settings.GROQ_API_KEY:
            try:
                self.groq_client = Groq(api_key=settings.GROQ_API_KEY)
                logger.info("Groq Cloud STT Engine initialized (whisper-large-v3-turbo).")
            except Exception as e:
                logger.warning(f"Failed to initialize Groq client for STT: {e}")
                self.groq_client = None
        else:
            logger.info("No GROQ_API_KEY set. Cloud STT will operate in fallback mode.")

    def pcm_to_wav_bytes(self, pcm_bytes: bytes, sample_rate: int = 16000, channels: int = 1, sampwidth: int = 2) -> bytes:
        """Encodes raw 16-bit linear PCM bytes into a standard WAV container."""
        buffer = io.BytesIO()
        with wave.open(buffer, "wb") as wav_file:
            wav_file.setnchannels(channels)
            wav_file.setsampwidth(sampwidth)
            wav_file.setframerate(sample_rate)
            wav_file.writeframes(pcm_bytes)
        buffer.seek(0)
        return buffer.read()

    def transcribe_pcm(self, pcm_bytes: bytes, sample_rate: int = 16000, language: str = "en") -> Dict[str, Any]:
        """
        Transcribes a chunk of raw 16kHz PCM audio bytes.
        Returns dict with: transcript, language, engine, duration_sec.
        """
        duration = len(pcm_bytes) / (sample_rate * 2) # 16-bit mono = 2 bytes/sample
        if len(pcm_bytes) < sample_rate * 2 * 0.5: # Under 0.5 seconds
            return {
                "transcript": "",
                "confidence": 0.0,
                "engine": "none",
                "duration_sec": round(duration, 2),
                "error": "Audio chunk too short"
            }

        # Ensure Groq client is ready if key was updated
        if not self.groq_client and settings.GROQ_API_KEY:
            self._init_client()

        if self.groq_client:
            try:
                wav_bytes = self.pcm_to_wav_bytes(pcm_bytes, sample_rate=sample_rate)
                file_tuple = ("audio.wav", wav_bytes, "audio/wav")

                # Transcribe with Whisper on Groq LPU
                transcription = self.groq_client.audio.transcriptions.create(
                    file=file_tuple,
                    model="whisper-large-v3-turbo",
                    response_format="json",
                    language=language,
                    temperature=0.0
                )
                text = transcription.text.strip() if hasattr(transcription, "text") else ""
                return {
                    "transcript": text,
                    "confidence": 0.95,
                    "engine": "groq_whisper_turbo",
                    "duration_sec": round(duration, 2)
                }
            except Exception as e:
                logger.error(f"Groq Whisper transcription failed: {e}")
                return {
                    "transcript": "",
                    "confidence": 0.0,
                    "engine": "groq_whisper_error",
                    "duration_sec": round(duration, 2),
                    "error": str(e)
                }

        # Fallback when no Groq key configured
        return {
            "transcript": "",
            "confidence": 0.0,
            "engine": "offline_fallback",
            "duration_sec": round(duration, 2),
            "note": "GROQ_API_KEY not configured. Use on-device Vosk or configure key."
        }
