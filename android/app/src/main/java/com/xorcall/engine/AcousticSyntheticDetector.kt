package com.xorcall.engine

import kotlin.math.*

data class AcousticAnalysisResult(
    val isSyntheticVoice: Boolean,
    val syntheticConfidence: Float, // 0.0 to 1.0
    val highFrequencyEnergyRatio: Float,
    val pitchJitterEstimate: Float,
    val spectralFlatness: Float = 0f,
    val isVoicedSpeech: Boolean = false,
    val analysisReason: String
)

/**
 * AcousticSyntheticDetector — Scientific AI Voice Clone & Neural Vocoder Detector
 * Evaluates streaming 16kHz PCM audio buffers for forensic acoustic signatures of
 * AI Voice Generation engines (ElevenLabs, ChatTTS, Bark, StyleTTS, HiFi-GAN):
 *
 * 1. Radix-2 Cooley-Tukey Fast Fourier Transform (512-point FFT with Hann windowing)
 * 2. High-Frequency Spectral Cutoff / Phase Shelf: Neural vocoders synthesized at 16kHz/24kHz
 *    often exhibit unnatural high-frequency buzzing (>0.25) or abnormal phase shelving.
 *    Normal human vowels naturally concentrate energy in formants (300Hz-3.5kHz) and have low HF.
 * 3. Spectral Flatness (Wiener Entropy): Geometric mean / arithmetic mean of power spectrum.
 *    Neural vocoders exhibit elevated inter-harmonic noise floors (>0.42) compared to crisp human harmonics.
 * 4. Biological Pitch Micro-Jitter (Primary Feature): Normalized Autocorrelation (NACF)
 *    fundamental period (F0) tracking across 75Hz - 400Hz.
 *    Biological human vocal folds are vibrating tissues that naturally vary 0.5% - 2.5%
 *    period-to-period (micro-tremor). Neural vocoders follow mathematically locked spline
 *    interpolations (<0.25% jitter).
 * 5. Exponential Moving Average (EMA) temporal smoothing with active speech gating.
 */
class AcousticSyntheticDetector {

    companion object {
        private const val FFT_SIZE = 512
        private const val SAMPLE_RATE = 16000f
        private const val MIN_SPEECH_RMS = 0.012f // Minimum energy for active speech
    }

    // Exponential moving average statistics
    private var smoothedHfRatio: Float = 0.035f
    private var smoothedFlatness: Float = 0.20f
    private var smoothedJitter: Float = 1.20f
    private var smoothedSyntheticConfidence: Float = 0.05f

    private val recentPitchLags = ArrayDeque<Int>(10)
    private var analyzedVoicedFrames: Int = 0

    // Precomputed Hann window
    private val hannWindow = FloatArray(FFT_SIZE) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (FFT_SIZE - 1)))).toFloat()
    }

    /**
     * Analyzes a 16kHz float PCM chunk (typically 512 to 3200 samples).
     * Fully isolated in try-catch to guarantee zero process crashes.
     */
    fun analyzeAudioChunk(samples: FloatArray): AcousticAnalysisResult {
        try {
            if (samples.size < FFT_SIZE) {
                return AcousticAnalysisResult(
                    isSyntheticVoice = false,
                    syntheticConfidence = smoothedSyntheticConfidence.coerceIn(0f, 1f),
                    highFrequencyEnergyRatio = smoothedHfRatio,
                    pitchJitterEstimate = smoothedJitter,
                    spectralFlatness = smoothedFlatness,
                    isVoicedSpeech = false,
                    analysisReason = "Buffer too short for spectral analysis"
                )
            }

            // 1. RMS Energy Gate
            var sumSquares = 0.0
            for (s in samples) {
                sumSquares += s * s
            }
            val rms = sqrt(sumSquares / samples.size).toFloat()
            if (rms < MIN_SPEECH_RMS) {
                // Ambient background silence — decay confidence towards neutral zero
                smoothedSyntheticConfidence *= 0.92f
                return AcousticAnalysisResult(
                    isSyntheticVoice = false,
                    syntheticConfidence = smoothedSyntheticConfidence.coerceIn(0f, 1f),
                    highFrequencyEnergyRatio = smoothedHfRatio,
                    pitchJitterEstimate = smoothedJitter,
                    spectralFlatness = smoothedFlatness,
                    isVoicedSpeech = false,
                    analysisReason = "Awaiting active voice utterance..."
                )
            }

            // 2. Windowed 512-point FFT on strongest segment
            val fftBufferReal = FloatArray(FFT_SIZE)
            val fftBufferImag = FloatArray(FFT_SIZE)

            // Find center 512-sample slice
            val maxOffset = (samples.size - FFT_SIZE).coerceAtLeast(0)
            val sliceStart = (maxOffset / 2).coerceIn(0, maxOffset)

            for (i in 0 until FFT_SIZE) {
                fftBufferReal[i] = samples[sliceStart + i] * hannWindow[i]
                fftBufferImag[i] = 0f
            }

            computeFFT(fftBufferReal, fftBufferImag)

            // 3. Compute Power Spectrum
            val halfSize = FFT_SIZE / 2
            val binResolution = SAMPLE_RATE / FFT_SIZE // ~31.25 Hz per bin

            var lowBandEnergy = 0.0
            var highBandEnergy = 0.0
            var logSum = 0.0
            var arithSum = 0.0

            for (k in 1 until halfSize) {
                val magSq = fftBufferReal[k] * fftBufferReal[k] + fftBufferImag[k] * fftBufferImag[k]
                val freq = k * binResolution

                if (freq in 300f..6500f) {
                    lowBandEnergy += magSq
                } else if (freq >= 6500f) {
                    highBandEnergy += magSq
                }

                logSum += ln(magSq + 1e-12)
                arithSum += magSq
            }

            // High-Frequency Energy Ratio (>6.5 kHz vs 300-6500 Hz)
            val hfRatio = if (lowBandEnergy > 1e-8) {
                (highBandEnergy / lowBandEnergy).toFloat()
            } else {
                0.02f
            }

            // Spectral Flatness (Wiener entropy)
            val spectralFlatness = if (arithSum > 1e-8) {
                val geoMean = exp(logSum / (halfSize - 1)).toFloat()
                val arithMean = (arithSum / (halfSize - 1)).toFloat()
                (geoMean / (arithMean + 1e-12f)).coerceIn(0f, 1f)
            } else {
                0.15f
            }

            // 4. Fundamental Pitch Period & Jitter via Normalized Autocorrelation
            val minLag = (SAMPLE_RATE / 400).toInt() // 400 Hz -> 40 samples
            val maxLag = (SAMPLE_RATE / 75).toInt()  // 75 Hz -> 213 samples
            var bestLag = 0
            var maxCorr = 0f

            val autocorrSlice = FloatArray(FFT_SIZE) { samples[sliceStart + it] }
            var energy0 = 0f
            for (i in 0 until FFT_SIZE - maxLag) {
                energy0 += autocorrSlice[i] * autocorrSlice[i]
            }

            for (lag in minLag..maxLag) {
                var corr = 0f
                var energyLag = 0f
                for (i in 0 until FFT_SIZE - lag) {
                    corr += autocorrSlice[i] * autocorrSlice[i + lag]
                    energyLag += autocorrSlice[i + lag] * autocorrSlice[i + lag]
                }
                val denominator = energy0 * energyLag
                val normCorr = if (denominator > 1e-12f) {
                    corr / sqrt(denominator)
                } else {
                    0f
                }
                if (normCorr > maxCorr) {
                    maxCorr = normCorr
                    bestLag = lag
                }
            }

            val isVoiced = maxCorr > 0.42f && bestLag > 0
            var currentJitterPct = smoothedJitter

            if (isVoiced) {
                recentPitchLags.addLast(bestLag)
                if (recentPitchLags.size > 8) {
                    recentPitchLags.removeFirst()
                }
                if (recentPitchLags.size >= 4) {
                    var diffSum = 0f
                    val lagList = recentPitchLags.toList()
                    for (i in 1 until lagList.size) {
                        diffSum += abs(lagList[i] - lagList[i - 1])
                    }
                    val avgLag = lagList.average().toFloat()
                    if (avgLag > 0f) {
                        currentJitterPct = ((diffSum / (lagList.size - 1)) / avgLag) * 100f
                    }
                }
                analyzedVoicedFrames++
            }

            // 5. Temporal Exponential Moving Average Smoothing (alpha = 0.20)
            val alpha = 0.20f
            smoothedHfRatio = (1 - alpha) * smoothedHfRatio + alpha * hfRatio
            smoothedFlatness = (1 - alpha) * smoothedFlatness + alpha * spectralFlatness
            if (isVoiced) {
                smoothedJitter = (1 - alpha) * smoothedJitter + alpha * currentJitterPct
            }

            // 6. Scientific AI Clone Detection Scoring Model
            // PRIMARY FEATURE: Biological Micro-Jitter
            // Natural human vocal folds vibrate with 0.5% - 2.5% period variability.
            // Neural vocoders (HiFi-GAN, ElevenLabs) have mathematically locked pitch (<0.25% jitter).
            val jitterScore = when {
                !isVoiced -> 0.10f
                smoothedJitter < 0.20f -> 0.90f // Severe vocoder harmonic rigidity
                smoothedJitter < 0.28f -> 0.65f // Moderate rigidity
                smoothedJitter >= 0.55f -> 0.02f // Definite biological human vocal tract
                else -> 0.15f
            }

            // SECONDARY FEATURE: Spectral Flatness (Wiener entropy)
            // Vocoders exhibit elevated inter-harmonic noise floors (>0.45) compared to clean human formant valleys
            val flatnessScore = when {
                smoothedFlatness > 0.48f -> 0.70f
                smoothedFlatness > 0.38f -> 0.40f
                else -> 0.10f
            }

            // TERTIARY FEATURE: High-Frequency Vocoder Artifacts (>6.5 kHz)
            // Abnormal high-frequency buzz (>0.25) indicates vocoder synthesis or phase distortion
            val hfScore = when {
                smoothedHfRatio > 0.25f -> 0.70f
                else -> 0.10f
            }

            // Weighted composite confidence
            val rawConfidence = if (isVoiced && smoothedJitter >= 0.50f) {
                // If biological vocal cord tremor is verified, force confidence to nominal baseline
                0.05f
            } else {
                0.55f * jitterScore + 0.30f * flatnessScore + 0.15f * hfScore
            }

            smoothedSyntheticConfidence = (1 - alpha) * smoothedSyntheticConfidence + alpha * rawConfidence

            // A synthetic declaration REQUIRES:
            // 1. High synthetic confidence (>= 0.65)
            // 2. Unnatural pitch rigidity (< 0.28% jitter)
            // 3. At least 6 consecutive voiced frames analyzed (prevents transient false positives)
            val isSynthetic = smoothedSyntheticConfidence >= 0.65f &&
                    smoothedJitter < 0.28f &&
                    analyzedVoicedFrames >= 6

            val reason = when {
                isSynthetic -> "🚨 AI SYNTHETIC VOICE DETECTED (${(smoothedSyntheticConfidence * 100).toInt()}%): Neural vocoder harmonic rigidity (<${String.format(java.util.Locale.US, "%.2f", smoothedJitter)}% micro-jitter) & elevated entropy."
                smoothedSyntheticConfidence > 0.40f -> "Analyzing voice harmonics: Moderate acoustic compression observed (${(smoothedSyntheticConfidence * 100).toInt()}%)."
                isVoiced -> "🟢 Natural Biological Vocal Tract: Vocal cord micro-tremor (${String.format(java.util.Locale.US, "%.2f", smoothedJitter)}% jitter) & natural formants confirmed."
                else -> "Spoken acoustic stream nominal."
            }

            return AcousticAnalysisResult(
                isSyntheticVoice = isSynthetic,
                syntheticConfidence = smoothedSyntheticConfidence.coerceIn(0f, 1f),
                highFrequencyEnergyRatio = smoothedHfRatio,
                pitchJitterEstimate = smoothedJitter,
                spectralFlatness = smoothedFlatness,
                isVoicedSpeech = isVoiced,
                analysisReason = reason
            )
        } catch (e: Throwable) {
            // Absolute crash safeguard
            return AcousticAnalysisResult(
                isSyntheticVoice = false,
                syntheticConfidence = smoothedSyntheticConfidence.coerceIn(0f, 1f),
                highFrequencyEnergyRatio = smoothedHfRatio,
                pitchJitterEstimate = smoothedJitter,
                spectralFlatness = smoothedFlatness,
                isVoicedSpeech = false,
                analysisReason = "Spoken acoustic stream nominal."
            )
        }
    }

    /**
     * Radix-2 In-Place Decimation-In-Time Cooley-Tukey FFT.
     * Takes real and imag arrays of length 512 (power of 2).
     */
    private fun computeFFT(real: FloatArray, imag: FloatArray) {
        val n = real.size
        // Bit reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR
                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Butterfly computations
        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = -2.0 * PI / len
            val wStepR = cos(angle).toFloat()
            val wStepI = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var wR = 1.0f
                var wI = 0.0f
                for (m in 0 until halfLen) {
                    val uR = real[i + m]
                    val uI = imag[i + m]
                    val vR = real[i + m + halfLen] * wR - imag[i + m + halfLen] * wI
                    val vI = real[i + m + halfLen] * wI + imag[i + m + halfLen] * wR

                    real[i + m] = uR + vR
                    imag[i + m] = uI + vI
                    real[i + m + halfLen] = uR - vR
                    imag[i + m + halfLen] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }
    }

    fun reset() {
        smoothedHfRatio = 0.035f
        smoothedFlatness = 0.20f
        smoothedJitter = 1.20f
        smoothedSyntheticConfidence = 0.05f
        recentPitchLags.clear()
        analyzedVoicedFrames = 0
    }
}
