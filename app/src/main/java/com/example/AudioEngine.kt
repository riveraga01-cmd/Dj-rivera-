package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

enum class DeckId {
    DECK_A,
    DECK_B
}

enum class DjPadEffect {
    SWOOSH,
    SWEEP,
    REVERB,
    ECHO
}

class EmotionalTtsManager(
    private val context: Context,
    private val onDuckingChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastSpoken = MutableStateFlow("")
    val lastSpoken: StateFlow<String> = _lastSpoken.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            isInitialized = true
            setupUtteranceListener()
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                onDuckingChanged(true)
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                onDuckingChanged(false)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                onDuckingChanged(false)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                onDuckingChanged(false)
            }
        })
    }

    fun speak(text: String, emocion: EmocionVoz) {
        if (!isInitialized || text.isBlank()) return
        val currentTts = tts ?: return

        when (emocion) {
            EmocionVoz.ROMANTICO -> {
                currentTts.setPitch(0.80f)
                currentTts.setSpeechRate(0.85f)
            }
            EmocionVoz.FELIZ -> {
                currentTts.setPitch(1.15f)
                currentTts.setSpeechRate(1.05f)
            }
            EmocionVoz.EMOCIONADO -> {
                currentTts.setPitch(1.30f)
                currentTts.setSpeechRate(1.15f)
            }
            EmocionVoz.NEUTRO -> {
                currentTts.setPitch(1.00f)
                currentTts.setSpeechRate(1.00f)
            }
        }

        _lastSpoken.value = text
        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle()
        currentTts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun speakWithVoiceType(text: String, tipoLector: TipoLector) {
        if (!isInitialized || text.isBlank()) return
        val currentTts = tts ?: return

        try {
            val voices = currentTts.voices
            if (voices != null && voices.isNotEmpty()) {
                val matchingVoice = voices.find { voice ->
                    val name = voice.name.lowercase()
                    val localeMatches = voice.locale.language == "es"
                    val genderMatches = if (tipoLector.isFemale) {
                        name.contains("female") || name.contains("fem") || name.contains("mujer")
                    } else {
                        name.contains("male") || name.contains("hombre")
                    }
                    localeMatches && genderMatches
                } ?: voices.find { it.locale.language == "es" }

                matchingVoice?.let { currentTts.voice = it }
            }
        } catch (_: Exception) {}

        currentTts.setPitch(tipoLector.pitch)
        currentTts.setSpeechRate(tipoLector.rate)

        _lastSpoken.value = text
        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle()
        currentTts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        onDuckingChanged(false)
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

@OptIn(UnstableApi::class)
class DualExoPlayerEngine(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val deckA: ExoPlayer = ExoPlayer.Builder(context).build()
    val deckB: ExoPlayer = ExoPlayer.Builder(context).build()

    private var equalizerA: Equalizer? = null
    private var equalizerB: Equalizer? = null

    // State flows
    private val _deckASong = MutableStateFlow<Cancion?>(null)
    val deckASong: StateFlow<Cancion?> = _deckASong.asStateFlow()

    private val _deckBSong = MutableStateFlow<Cancion?>(null)
    val deckBSong: StateFlow<Cancion?> = _deckBSong.asStateFlow()

    private val _deckAPosition = MutableStateFlow(0L)
    val deckAPosition: StateFlow<Long> = _deckAPosition.asStateFlow()

    private val _deckADuration = MutableStateFlow(1L)
    val deckADuration: StateFlow<Long> = _deckADuration.asStateFlow()

    private val _deckBPosition = MutableStateFlow(0L)
    val deckBPosition: StateFlow<Long> = _deckBPosition.asStateFlow()

    private val _deckBDuration = MutableStateFlow(1L)
    val deckBDuration: StateFlow<Long> = _deckBDuration.asStateFlow()

    private val _deckAIsPlaying = MutableStateFlow(false)
    val deckAIsPlaying: StateFlow<Boolean> = _deckAIsPlaying.asStateFlow()

    private val _deckBIsPlaying = MutableStateFlow(false)
    val deckBIsPlaying: StateFlow<Boolean> = _deckBIsPlaying.asStateFlow()

    private val _activeDeck = MutableStateFlow(DeckId.DECK_A)
    val activeDeck: StateFlow<DeckId> = _activeDeck.asStateFlow()

    // 0.0f = Deck A only, 1.0f = Deck B only
    private val _crossfaderPosition = MutableStateFlow(0.0f)
    val crossfaderPosition: StateFlow<Float> = _crossfaderPosition.asStateFlow()

    private val _isAutoDj = MutableStateFlow(true)
    val isAutoDj: StateFlow<Boolean> = _isAutoDj.asStateFlow()

    private val _isDucked = MutableStateFlow(false)
    val isDucked: StateFlow<Boolean> = _isDucked.asStateFlow()

    private val _masterPlaying = MutableStateFlow(false)
    val masterPlaying: StateFlow<Boolean> = _masterPlaying.asStateFlow()

    // EQ bands (-12dB to +12dB)
    private val _eqLow = MutableStateFlow(0f)
    val eqLow: StateFlow<Float> = _eqLow.asStateFlow()

    private val _eqMid = MutableStateFlow(0f)
    val eqMid: StateFlow<Float> = _eqMid.asStateFlow()

    private val _eqHigh = MutableStateFlow(0f)
    val eqHigh: StateFlow<Float> = _eqHigh.asStateFlow()

    // Animated waveform bars for Canvas
    private val _waveformA = MutableStateFlow(List(32) { 0.2f })
    val waveformA: StateFlow<List<Float>> = _waveformA.asStateFlow()

    private val _waveformB = MutableStateFlow(List(32) { 0.2f })
    val waveformB: StateFlow<List<Float>> = _waveformB.asStateFlow()

    private var crossfadeJob: Job? = null
    private var isCrossfading = false

    var onSongRequestNext: (() -> Cancion?)? = null

    init {
        initEqualizers()
        setupPlayerListeners()
        startMonitoringLoop()
        updateDeckVolumes()
    }

    private fun initEqualizers() {
        try {
            val sessionA = deckA.audioSessionId
            if (sessionA != 0) {
                equalizerA = Equalizer(0, sessionA).apply { enabled = true }
            }
            val sessionB = deckB.audioSessionId
            if (sessionB != 0) {
                equalizerB = Equalizer(0, sessionB).apply { enabled = true }
            }
        } catch (_: Exception) {
            // AudioFx may not be supported on all virtual emulators
        }
    }

    private fun setupPlayerListeners() {
        deckA.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _deckAIsPlaying.value = isPlaying
                checkMasterPlaying()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _deckADuration.value = deckA.duration.coerceAtLeast(1L)
                }
            }
        })

        deckB.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _deckBIsPlaying.value = isPlaying
                checkMasterPlaying()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _deckBDuration.value = deckB.duration.coerceAtLeast(1L)
                }
            }
        })
    }

    private fun checkMasterPlaying() {
        _masterPlaying.value = deckA.isPlaying || deckB.isPlaying
    }

    private fun startMonitoringLoop() {
        scope.launch {
            var tick = 0
            while (isActive) {
                delay(100L)
                tick++

                // Update Deck A positions
                val posA = deckA.currentPosition
                val durA = deckA.duration.coerceAtLeast(1L)
                _deckAPosition.value = posA
                _deckADuration.value = durA

                // Update Deck B positions
                val posB = deckB.currentPosition
                val durB = deckB.duration.coerceAtLeast(1L)
                _deckBPosition.value = posB
                _deckBDuration.value = durB

                // Generate visual waveform ripples
                if (deckA.isPlaying) {
                    _waveformA.value = List(32) { i ->
                        val base = 0.2f + 0.6f * abs(sin((tick * 0.15f) + (i * 0.4f)))
                        base.coerceIn(0.1f, 1.0f)
                    }
                } else {
                    _waveformA.value = List(32) { 0.15f }
                }

                if (deckB.isPlaying) {
                    _waveformB.value = List(32) { i ->
                        val base = 0.2f + 0.6f * abs(cos((tick * 0.18f) + (i * 0.35f)))
                        base.coerceIn(0.1f, 1.0f)
                    }
                } else {
                    _waveformB.value = List(32) { 0.15f }
                }

                // Auto-DJ 12s transition monitoring
                if (_isAutoDj.value && !isCrossfading && _masterPlaying.value) {
                    checkAutoCrossfade(posA, durA, posB, durB)
                }
            }
        }
    }

    private fun checkAutoCrossfade(posA: Long, durA: Long, posB: Long, durB: Long) {
        val active = _activeDeck.value
        if (active == DeckId.DECK_A && deckA.isPlaying && durA > 15000L) {
            val remaining = durA - posA
            if (remaining in 1L..12000L) {
                // Ensure deck B has a song
                if (_deckBSong.value == null) {
                    val nextSong = onSongRequestNext?.invoke()
                    if (nextSong != null) {
                        loadSong(DeckId.DECK_B, nextSong)
                    }
                }
                triggerAutoCrossfade(fromDeck = DeckId.DECK_A, toDeck = DeckId.DECK_B)
            }
        } else if (active == DeckId.DECK_B && deckB.isPlaying && durB > 15000L) {
            val remaining = durB - posB
            if (remaining in 1L..12000L) {
                if (_deckASong.value == null) {
                    val nextSong = onSongRequestNext?.invoke()
                    if (nextSong != null) {
                        loadSong(DeckId.DECK_A, nextSong)
                    }
                }
                triggerAutoCrossfade(fromDeck = DeckId.DECK_B, toDeck = DeckId.DECK_A)
            }
        }
    }

    fun triggerAutoCrossfade(fromDeck: DeckId, toDeck: DeckId) {
        if (isCrossfading) return
        crossfadeJob?.cancel()
        isCrossfading = true

        crossfadeJob = scope.launch {
            val incomingPlayer = if (toDeck == DeckId.DECK_A) deckA else deckB
            val outgoingPlayer = if (fromDeck == DeckId.DECK_A) deckA else deckB

            // Start incoming deck at 0.0f
            if (toDeck == DeckId.DECK_A) {
                _crossfaderPosition.value = 1.0f
            } else {
                _crossfaderPosition.value = 0.0f
            }
            updateDeckVolumes()
            incomingPlayer.play()

            // 10s linear crossfade (100 steps of 100ms)
            val steps = 100
            val delayPerStep = 100L
            val startX = _crossfaderPosition.value
            val targetX = if (toDeck == DeckId.DECK_B) 1.0f else 0.0f

            for (i in 1..steps) {
                delay(delayPerStep)
                val progress = i.toFloat() / steps
                _crossfaderPosition.value = startX + (targetX - startX) * progress
                updateDeckVolumes()
            }

            // Finish transition
            _crossfaderPosition.value = targetX
            updateDeckVolumes()
            outgoingPlayer.pause()
            _activeDeck.value = toDeck
            isCrossfading = false
        }
    }

    fun setAutoDj(enabled: Boolean) {
        _isAutoDj.value = enabled
    }

    fun setDucking(ducked: Boolean) {
        _isDucked.value = ducked
        updateDeckVolumes()
    }

    fun setCrossfader(position: Float) {
        if (isCrossfading) {
            crossfadeJob?.cancel()
            isCrossfading = false
        }
        _crossfaderPosition.value = position.coerceIn(0.0f, 1.0f)
        if (position < 0.5f) {
            _activeDeck.value = DeckId.DECK_A
        } else {
            _activeDeck.value = DeckId.DECK_B
        }
        updateDeckVolumes()
    }

    fun updateDeckVolumes() {
        val xf = _crossfaderPosition.value
        val duckMultiplier = if (_isDucked.value) 0.15f else 1.0f

        // Linear crossfade: 1.0 -> 0.0 for Deck A, 0.0 -> 1.0 for Deck B
        val volA = (1.0f - xf).coerceIn(0.0f, 1.0f) * duckMultiplier
        val volB = xf.coerceIn(0.0f, 1.0f) * duckMultiplier

        deckA.volume = volA
        deckB.volume = volB
    }

    fun setEq(low: Float, mid: Float, high: Float) {
        _eqLow.value = low.coerceIn(-12f, 12f)
        _eqMid.value = mid.coerceIn(-12f, 12f)
        _eqHigh.value = high.coerceIn(-12f, 12f)
        applyEqBands()
    }

    private fun applyEqBands() {
        val lowMb = (_eqLow.value * 100).toInt().toShort()
        val midMb = (_eqMid.value * 100).toInt().toShort()
        val highMb = (_eqHigh.value * 100).toInt().toShort()

        listOfNotNull(equalizerA, equalizerB).forEach { eq ->
            try {
                val numBands = eq.numberOfBands.toInt()
                if (numBands >= 3) {
                    eq.setBandLevel(0, lowMb)
                    eq.setBandLevel((numBands / 2).toShort(), midMb)
                    eq.setBandLevel((numBands - 1).toShort(), highMb)
                }
            } catch (_: Exception) {}
        }
    }

    fun loadSong(deckId: DeckId, cancion: Cancion) {
        val mediaItem = MediaItem.fromUri(Uri.parse(cancion.uri))
        if (deckId == DeckId.DECK_A) {
            _deckASong.value = cancion
            deckA.setMediaItem(mediaItem)
            deckA.prepare()
        } else {
            _deckBSong.value = cancion
            deckB.setMediaItem(mediaItem)
            deckB.prepare()
        }
    }

    fun playDeck(deckId: DeckId) {
        if (deckId == DeckId.DECK_A) {
            deckA.play()
            _activeDeck.value = DeckId.DECK_A
        } else {
            deckB.play()
            _activeDeck.value = DeckId.DECK_B
        }
        checkMasterPlaying()
    }

    fun pauseDeck(deckId: DeckId) {
        if (deckId == DeckId.DECK_A) {
            deckA.pause()
        } else {
            deckB.pause()
        }
        checkMasterPlaying()
    }

    fun toggleMasterPlayPause() {
        val anyPlaying = deckA.isPlaying || deckB.isPlaying
        if (anyPlaying) {
            deckA.pause()
            deckB.pause()
        } else {
            // Play active deck or both if crossfader is mixed
            if (_crossfaderPosition.value <= 0.05f) {
                deckA.play()
            } else if (_crossfaderPosition.value >= 0.95f) {
                deckB.play()
            } else {
                deckA.play()
                deckB.play()
            }
        }
        checkMasterPlaying()
    }

    fun seekDeck(deckId: DeckId, progress: Float) {
        val player = if (deckId == DeckId.DECK_A) deckA else deckB
        val duration = player.duration.coerceAtLeast(1L)
        val target = (progress * duration).toLong().coerceIn(0L, duration)
        player.seekTo(target)
    }

    fun playDjPad(effect: DjPadEffect) {
        scope.launch(Dispatchers.Default) {
            DjAudioSynthesizer.playSyntheticEffect(effect)
        }
    }

    fun release() {
        scope.launch {
            deckA.release()
            deckB.release()
            equalizerA?.release()
            equalizerB?.release()
        }
    }
}

/**
 * High-performance synthesizer that creates real, punchy DJ sound effects and real audio tracks
 * without relying on external file downloads or hardcoded strings.
 */
object DjAudioSynthesizer {

    fun playSyntheticEffect(effect: DjPadEffect) {
        try {
            val sampleRate = 44100
            val durationSec = when (effect) {
                DjPadEffect.SWOOSH -> 0.7f
                DjPadEffect.SWEEP -> 0.9f
                DjPadEffect.REVERB -> 0.8f
                DjPadEffect.ECHO -> 1.0f
            }
            val numSamples = (sampleRate * durationSec).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val progress = i.toFloat() / numSamples
                val sample: Float = when (effect) {
                    DjPadEffect.SWOOSH -> {
                        // Rising noise filter with frequency sweep
                        val noise = (Math.random().toFloat() * 2f - 1f)
                        val cutoff = 300f + 2500f * progress
                        val env = sin(progress * PI.toFloat())
                        noise * env * sin(2f * PI.toFloat() * cutoff * t)
                    }
                    DjPadEffect.SWEEP -> {
                        // Resonant electronic riser synth
                        val freq = 120f + 1400f * (progress * progress)
                        val env = sin(progress * PI.toFloat())
                        sin(2f * PI.toFloat() * freq * t) * env
                    }
                    DjPadEffect.REVERB -> {
                        // Impulse slap with exponential decaying room tail
                        val impulse = if (progress < 0.05f) sin(2f * PI.toFloat() * 880f * t) else 0f
                        val tail = (Math.random().toFloat() * 2f - 1f) * exp(-progress * 4.5f)
                        (impulse * 0.7f + tail * 0.5f)
                    }
                    DjPadEffect.ECHO -> {
                        // Rhythmic delay impulse repeats
                        val tapProgress = (progress * 4f) % 1f
                        val tapIndex = (progress * 4f).toInt()
                        val tapDecay = exp(-tapIndex * 0.7f)
                        val freq = 600f
                        sin(2f * PI.toFloat() * freq * t) * exp(-tapProgress * 6f) * tapDecay
                    }
                }
                buffer[i] = (sample.coerceIn(-1.0f, 1.0f) * 30000).toInt().toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            // Clean up track after play
            CoroutineScope(Dispatchers.Default).launch {
                delay((durationSec * 1000).toLong() + 300L)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    /**
     * Synthesizes a real playable 44.1kHz stereo WAV song into local files cache.
     */
    fun createSongWavFile(
        context: Context,
        fileName: String,
        durationSeconds: Int,
        bpm: Int,
        baseFreq: Float,
        isLatinStyle: Boolean
    ): File {
        val musicDir = File(context.filesDir, "dj_tracks")
        if (!musicDir.exists()) musicDir.mkdirs()
        val file = File(musicDir, fileName)
        if (file.exists() && file.length() > 44) {
            return file
        }

        val sampleRate = 44100
        val numSamples = sampleRate * durationSeconds
        val bytesPerSample = 2 // 16-bit
        val channels = 2 // stereo
        val dataSize = numSamples * channels * bytesPerSample

        val fos = FileOutputStream(file)
        // Write standard 44-byte WAV header
        writeWavHeader(fos, sampleRate, channels, dataSize)

        val beatDuration = 60.0f / bpm
        val samplesPerBeat = (sampleRate * beatDuration).toInt()
        val buffer = ByteArray(4096)
        var bufferIndex = 0

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val beatPhase = (i % samplesPerBeat).toFloat() / samplesPerBeat

            // Kick drum: on every beat
            val kickDecay = exp(-beatPhase * 14.0f)
            val kickFreq = 140.0f * exp(-beatPhase * 18.0f) + 45.0f
            val kick = sin(2.0f * PI.toFloat() * kickFreq * t) * kickDecay

            // Snare / Guiro rhythm: on beats 2 & 4 or offbeats
            val isSnareBeat = ((i / samplesPerBeat) % 2 == 1)
            val snareDecay = if (isSnareBeat) exp(-beatPhase * 18.0f) else 0.0f
            val noise = (Math.random().toFloat() * 2f - 1f)
            val snare = noise * snareDecay * 0.4f

            // Bass melody
            val barNumber = (i / (samplesPerBeat * 4)) % 4
            val bassPitchMultiplier = when (barNumber) {
                0 -> 1.0f
                1 -> 1.334f // 4th
                2 -> 1.5f   // 5th
                else -> 1.122f // 2nd
            }
            val bassFreq = baseFreq * bassPitchMultiplier
            val bassWave = sin(2.0f * PI.toFloat() * bassFreq * t) * 0.35f

            // Latin percussion or synth chords
            val syncopation = if (isLatinStyle) {
                // Montuno / Cumbia 16th note syncopation
                val subBeat = (i % (samplesPerBeat / 4)).toFloat() / (samplesPerBeat / 4)
                sin(2f * PI.toFloat() * (baseFreq * 2.5f) * t) * exp(-subBeat * 8f) * 0.25f
            } else {
                // Electronic arpeggio
                val arpStep = ((i / (samplesPerBeat / 4)) % 8)
                val arpFreq = baseFreq * (2.0f + arpStep * 0.25f)
                sin(2f * PI.toFloat() * arpFreq * t) * 0.20f
            }

            val mix = (kick * 0.6f + snare * 0.35f + bassWave + syncopation).coerceIn(-1.0f, 1.0f)
            val sampleShort = (mix * 28000).toInt().toShort()

            // Stereo Left & Right
            buffer[bufferIndex++] = (sampleShort.toInt() and 0xFF).toByte()
            buffer[bufferIndex++] = ((sampleShort.toInt() shr 8) and 0xFF).toByte()
            buffer[bufferIndex++] = (sampleShort.toInt() and 0xFF).toByte()
            buffer[bufferIndex++] = ((sampleShort.toInt() shr 8) and 0xFF).toByte()

            if (bufferIndex >= buffer.size) {
                fos.write(buffer, 0, bufferIndex)
                bufferIndex = 0
            }
        }
        if (bufferIndex > 0) {
            fos.write(buffer, 0, bufferIndex)
        }
        fos.flush()
        fos.close()

        return file
    }

    private fun writeWavHeader(fos: FileOutputStream, sampleRate: Int, channels: Int, dataSize: Int) {
        val totalSize = dataSize + 36
        val byteRate = sampleRate * channels * 2

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(totalSize)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // Subchunk1Size (16 for PCM)
        header.putShort(1.toShort()) // AudioFormat (1 for PCM)
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((channels * 2).toShort()) // BlockAlign
        header.putShort(16.toShort()) // BitsPerSample
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize)

        fos.write(header.array())
    }
}
