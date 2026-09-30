package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real-time DSP Microphone Engine.
 * Captures live microphone input via AudioRecord, calculates dynamic VU level,
 * applies real-time digital voice effects (ROBOT, MEGAFONO, REVERB, ECHO, PITCH),
 * blends DRY / WET signals, and streams directly into AudioTrack mixing with master audio.
 */
class RealtimeMicDspEngine(
    private val context: Context,
    private val onVuLevelChanged: (Float) -> Unit
) {
    private val tag = "MicDspEngine"
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var processingJob: Job? = null

    @Volatile
    var isEnabled: Boolean = false
        private set

    // Real-time controllable parameters
    @Volatile var micGain: Float = 0.80f
    @Volatile var selectedEffect: MicEffect = MicEffect.NONE
    @Volatile var dryWetMix: Float = 0.50f
    @Volatile var pitchShiftValue: Float = 0.0f
    @Volatile var reverbIntensity: Float = 0.40f
    @Volatile var echoFeedback: Float = 0.50f
    @Volatile var echoBpmSync: String = "1/2"

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val CHUNK_SIZE = 1024
    }

    fun start() {
        if (isEnabled) return

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(tag, "RECORD_AUDIO permission not granted; cannot start AudioRecord.")
            return
        }

        try {
            val minInSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
            val inBufferSize = maxOf(minInSize * 2, CHUNK_SIZE * 4)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_IN,
                ENCODING,
                inBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(tag, "AudioRecord failed to initialize.")
                audioRecord?.release()
                audioRecord = null
                return
            }

            val minOutSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, ENCODING)
            val outBufferSize = maxOf(minOutSize * 2, CHUNK_SIZE * 4)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(ENCODING)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_OUT)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(outBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioRecord?.startRecording()
            audioTrack?.play()
            isEnabled = true

            startProcessingLoop()
            Log.d(tag, "Real-time DSP Mic started successfully.")
        } catch (e: Throwable) {
            Log.e(tag, "Error starting MicDspEngine: ${e.message}", e)
            stop()
        }
    }

    fun stop() {
        isEnabled = false
        processingJob?.cancel()
        processingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Throwable) {}
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Throwable) {}
        audioTrack = null

        onVuLevelChanged(0f)
        Log.d(tag, "Real-time DSP Mic stopped.")
    }

    private fun startProcessingLoop() {
        processingJob = scope.launch {
            val inBuffer = ShortArray(CHUNK_SIZE)
            val wetBuffer = FloatArray(CHUNK_SIZE)
            val outBuffer = ShortArray(CHUNK_SIZE)

            // Effect state variables
            var robotPhase = 0.0
            val robotCarrierFreq = 140.0 // 140 Hz metallic ring-mod carrier
            val twoPi = 2.0 * PI

            // Megáfono 2-stage bandpass filter state (400 Hz HPF + 3200 Hz LPF)
            var hpOut = 0.0f
            var hpPrevX = 0.0f
            var lpOut = 0.0f
            val dt = 1.0f / SAMPLE_RATE
            val rcHp = 1.0f / (twoPi.toFloat() * 400.0f)
            val alphaHp = rcHp / (rcHp + dt)
            val rcLp = 1.0f / (twoPi.toFloat() * 3200.0f)
            val alphaLp = dt / (rcLp + dt)

            // Reverb state: 4 comb filters + 1 all-pass diffuser
            val combLengths = intArrayOf(1116, 1188, 1277, 1356) // ~25 to 31ms delay lines
            val combBuffers = Array(4) { FloatArray(combLengths[it]) }
            val combIndices = IntArray(4)

            val apLength = 441 // ~10ms allpass
            val apBuffer = FloatArray(apLength)
            var apIndex = 0
            val apGain = 0.5f

            // Echo / Delay circular buffer (supports up to 1.5 seconds at 44.1kHz = 66150 samples)
            val maxDelaySamples = (SAMPLE_RATE * 1.5).toInt()
            val echoBuffer = FloatArray(maxDelaySamples)
            var echoWritePtr = 0

            // Pitch shift granular state
            val pitchDelayMax = 2048
            val pitchBuffer = FloatArray(pitchDelayMax)
            var pitchWritePtr = 0
            var pitchReadPhase1 = 0.0
            var pitchReadPhase2 = (pitchDelayMax / 2).toDouble()

            var smoothedVu = 0.0f
            var frameCount = 0

            while (isActive && isEnabled) {
                val record = audioRecord ?: break
                val track = audioTrack ?: break

                val readCount = record.read(inBuffer, 0, CHUNK_SIZE)
                if (readCount <= 0) continue

                // 1. Calculate VU meter (RMS)
                var sumSq = 0.0
                for (i in 0 until readCount) {
                    val s = inBuffer[i].toDouble()
                    sumSq += s * s
                }
                val rms = sqrt(sumSq / readCount)
                val instantVu = (rms / 6500.0).toFloat().coerceIn(0f, 1f)
                smoothedVu = smoothedVu * 0.65f + instantVu * 0.35f

                frameCount++
                if (frameCount % 3 == 0) {
                    onVuLevelChanged(smoothedVu)
                }

                // 2. DSP Processing per effect
                val currentFx = selectedEffect
                when (currentFx) {
                    MicEffect.NONE -> {
                        for (i in 0 until readCount) {
                            wetBuffer[i] = inBuffer[i].toFloat()
                        }
                    }
                    MicEffect.ROBOT -> {
                        for (i in 0 until readCount) {
                            val carrier = sin(robotPhase).toFloat()
                            robotPhase += twoPi * robotCarrierFreq / SAMPLE_RATE
                            if (robotPhase > twoPi) robotPhase -= twoPi
                            wetBuffer[i] = inBuffer[i] * carrier
                        }
                    }
                    MicEffect.MEGAFONO -> {
                        for (i in 0 until readCount) {
                            val x = inBuffer[i].toFloat()
                            // High-pass stage (~400 Hz cut)
                            hpOut = alphaHp * (hpOut + x - hpPrevX)
                            hpPrevX = x
                            // Low-pass stage (~3.2 kHz cut)
                            lpOut += alphaLp * (hpOut - lpOut)

                            // Soft saturation / megaphone horn overdrive
                            val drive = lpOut * 2.2f / 16000.0f
                            val saturated = when {
                                drive > 1.0f -> 1.0f
                                drive < -1.0f -> -1.0f
                                else -> 1.5f * drive - 0.5f * drive * drive * drive
                            }
                            wetBuffer[i] = saturated * 16000.0f
                        }
                    }
                    MicEffect.REVERB -> {
                        val fb = (0.60f + 0.28f * reverbIntensity.coerceIn(0f, 1f))
                        for (i in 0 until readCount) {
                            val inSample = inBuffer[i].toFloat()
                            var combSum = 0.0f
                            for (c in 0 until 4) {
                                val cBuf = combBuffers[c]
                                val idx = combIndices[c]
                                val delayed = cBuf[idx]
                                cBuf[idx] = inSample + delayed * fb
                                combIndices[c] = (idx + 1) % cBuf.size
                                combSum += delayed
                            }
                            // Allpass diffusion
                            val apIn = combSum * 0.25f
                            val apDelayed = apBuffer[apIndex]
                            val apOut = -apGain * apIn + apDelayed
                            apBuffer[apIndex] = apIn + apGain * apOut
                            apIndex = (apIndex + 1) % apLength
                            wetBuffer[i] = (inSample * 0.4f + apOut * 1.2f)
                        }
                    }
                    MicEffect.ECHO_DELAY -> {
                        // Calculate delay time according to BPM Sync (128 BPM base = 468.75ms quarter note)
                        val delayTimeSec = when (echoBpmSync) {
                            "1/4" -> 0.117f
                            "1/2" -> 0.234f
                            "3/4" -> 0.351f
                            "1/1" -> 0.468f
                            else -> 0.234f
                        }
                        val delaySamples = (SAMPLE_RATE * delayTimeSec).toInt().coerceIn(100, maxDelaySamples - 1)
                        val fb = echoFeedback.coerceIn(0.0f, 0.90f)

                        for (i in 0 until readCount) {
                            val inSample = inBuffer[i].toFloat()
                            var readPtr = echoWritePtr - delaySamples
                            if (readPtr < 0) readPtr += maxDelaySamples

                            val delayed = echoBuffer[readPtr]
                            echoBuffer[echoWritePtr] = inSample + delayed * fb
                            echoWritePtr = (echoWritePtr + 1) % maxDelaySamples
                            wetBuffer[i] = delayed
                        }
                    }
                    MicEffect.PITCH_SHIFT -> {
                        // Pitch shift by semitones (-12 to +12)
                        val semitones = pitchShiftValue.coerceIn(-12f, 12f)
                        val pitchRatio = java.lang.Math.pow(2.0, semitones.toDouble() / 12.0)
                        val rateChange = pitchRatio - 1.0

                        for (i in 0 until readCount) {
                            val inSample = inBuffer[i].toFloat()
                            pitchBuffer[pitchWritePtr] = inSample

                            pitchReadPhase1 = (pitchReadPhase1 + rateChange + pitchDelayMax) % pitchDelayMax
                            pitchReadPhase2 = (pitchReadPhase2 + rateChange + pitchDelayMax) % pitchDelayMax

                            val idx1 = pitchReadPhase1.toInt()
                            val idx2 = pitchReadPhase2.toInt()

                            val w1 = 0.5f * (1f - kotlin.math.cos(twoPi * pitchReadPhase1 / pitchDelayMax).toFloat())
                            val w2 = 0.5f * (1f - kotlin.math.cos(twoPi * pitchReadPhase2 / pitchDelayMax).toFloat())

                            val s1 = pitchBuffer[idx1]
                            val s2 = pitchBuffer[idx2]

                            wetBuffer[i] = s1 * w1 + s2 * w2
                            pitchWritePtr = (pitchWritePtr + 1) % pitchDelayMax
                        }
                    }
                }

                // 3. Dry / Wet blending & Mic Gain
                val dryAmount = 1.0f - dryWetMix.coerceIn(0.0f, 1.0f)
                val wetAmount = dryWetMix.coerceIn(0.0f, 1.0f)
                val gain = micGain.coerceIn(0.0f, 1.5f) * 1.8f

                for (i in 0 until readCount) {
                    val dry = inBuffer[i].toFloat()
                    val wet = wetBuffer[i]
                    val mixed = (dry * dryAmount + wet * wetAmount) * gain
                    outBuffer[i] = mixed.coerceIn(-32767f, 32767f).toInt().toShort()
                }

                // 4. Stream to master audio output
                track.write(outBuffer, 0, readCount)
            }
        }
    }
}
