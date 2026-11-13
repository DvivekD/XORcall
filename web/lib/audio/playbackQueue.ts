/**
 * Sample-Accurate Audio Playback Queue with Instant Interruption Kill-Switch.
 */
export class AudioPlaybackQueue {
  private audioCtx: AudioContext | null = null;
  private gainNode: GainNode | null = null;
  private nextStartTime = 0;
  private activeSources: AudioBufferSourceNode[] = [];

  constructor() {
    if (typeof window !== "undefined") {
      const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (AudioCtx) {
        this.audioCtx = new AudioCtx();
        this.gainNode = this.audioCtx.createGain();
        this.gainNode.connect(this.audioCtx.destination);
      }
    }
  }

  async playChunk(pcmFloat32: Float32Array, sampleRate = 24000) {
    if (!this.audioCtx || !this.gainNode) return;
    if (this.audioCtx.state === "suspended") {
      await this.audioCtx.resume();
    }

    const audioBuffer = this.audioCtx.createBuffer(1, pcmFloat32.length, sampleRate);
    audioBuffer.getChannelData(0).set(pcmFloat32);

    const now = this.audioCtx.currentTime;
    // 20ms jitter lookahead buffer ensures smooth transition between consecutive streaming chunks
    const startTime = Math.max(now + 0.02, this.nextStartTime);

    const source = this.audioCtx.createBufferSource();
    source.buffer = audioBuffer;
    source.connect(this.gainNode);

    source.start(startTime);
    this.nextStartTime = startTime + audioBuffer.duration;
    this.activeSources.push(source);

    source.onended = () => {
      this.activeSources = this.activeSources.filter((s) => s !== source);
    };
  }

  stopAll() {
    this.activeSources.forEach((s) => {
      try {
        s.stop();
        s.disconnect();
      } catch {
        // already stopped
      }
    });
    this.activeSources = [];
    if (this.audioCtx) {
      this.nextStartTime = this.audioCtx.currentTime;
    }
  }
}
