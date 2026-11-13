/**
 * Low-Level Digital Signal Processing (DSP) Pipeline for XORcall.
 * Resamples browser audio to 16kHz Mono and quantizes to 16-bit signed Linear PCM.
 */

export function resampleTo16k(input: Float32Array, inputSampleRate: number): Float32Array {
  if (inputSampleRate === 16000) return input;
  const ratio = inputSampleRate / 16000;
  const outputLength = Math.round(input.length / ratio);
  const result = new Float32Array(outputLength);

  for (let i = 0; i < outputLength; i++) {
    const originalPos = i * ratio;
    const index = Math.floor(originalPos);
    const frac = originalPos - index;
    const nextIndex = Math.min(index + 1, input.length - 1);
    result[i] = input[index] * (1 - frac) + input[nextIndex] * frac;
  }
  return result;
}

export function floatTo16BitPCM(float32Array: Float32Array): ArrayBuffer {
  const buffer = new ArrayBuffer(float32Array.length * 2);
  const view = new DataView(buffer);
  let offset = 0;

  for (let i = 0; i < float32Array.length; i++, offset += 2) {
    const s = Math.max(-1, Math.min(1, float32Array[i]));
    const val = s < 0 ? s * 0x8000 : s * 0x7fff;
    view.setInt16(offset, val, true); // true = little-endian
  }
  return buffer;
}

export function calculateRMS(samples: Float32Array): number {
  let sum = 0;
  for (let i = 0; i < samples.length; i++) {
    sum += samples[i] * samples[i];
  }
  return Math.sqrt(sum / samples.length);
}

export class RMSGate {
  private thresholdConversational = 0.002;
  private thresholdBargeIn = 0.020;
  private hangoverMs = 220;
  private lastSpeechTimestamp = 0;

  isSpeech(samples: Float32Array, isModelSpeaking: boolean): boolean {
    const rms = calculateRMS(samples);
    const threshold = isModelSpeaking ? this.thresholdBargeIn : this.thresholdConversational;
    const now = Date.now();

    if (rms >= threshold) {
      this.lastSpeechTimestamp = now;
      return true;
    }

    // Hangover timer ensures trailing phonemes and consonants are not clipped
    if (now - this.lastSpeechTimestamp < this.hangoverMs) {
      return true;
    }

    return false;
  }
}
