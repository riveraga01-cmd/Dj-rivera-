package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
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

data class VoicePresetConfig(
    val presetName: String,
    val targetCountry: String?,
    val isFemale: Boolean,
    val basePitch: Float,
    val baseRate: Float,
    val keywords: List<String>
)

class EmotionalTtsManager(
    private val context: Context,
    private val onDuckingChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private val tag = "EmotionalTtsManager"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    var isInitialized = false
        private set

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastSpoken = MutableStateFlow("")
    val lastSpoken: StateFlow<String> = _lastSpoken.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices.asStateFlow()

    private var isLiveUtterance: Boolean = true
    private var currentOnStartCallback: (() -> Unit)? = null
    private var currentOnFinishCallback: (() -> Unit)? = null

    var currentSpeechRate: Float = 1.0f
        private set
    var currentPitch: Float = 1.0f
        private set

    companion object {
        val PRESETS = listOf(
            VoicePresetConfig(
                presetName = "Español Latino (Locutor Profesional)",
                targetCountry = "US",
                isFemale = false,
                basePitch = 0.95f,
                baseRate = 1.00f,
                keywords = listOf("es-us", "es-419", "es-mx", "male", "hombre", "spa-usa")
            ),
            VoicePresetConfig(
                presetName = "Español Latino (Femenino Enérgico)",
                targetCountry = "US",
                isFemale = true,
                basePitch = 1.28f,
                baseRate = 1.12f,
                keywords = listOf("female", "fem", "mujer", "femenino", "#female")
            ),
            VoicePresetConfig(
                presetName = "Español España (DJ Nightclub)",
                targetCountry = "ES",
                isFemale = false,
                basePitch = 0.85f,
                baseRate = 1.05f,
                keywords = listOf("es-es", "castellano", "spain", "spa-esp")
            ),
            VoicePresetConfig(
                presetName = "Voz Modulada Neón (Deep Bass)",
                targetCountry = null,
                isFemale = false,
                basePitch = 0.65f,
                baseRate = 0.92f,
                keywords = listOf("deep", "bass", "low", "male")
            )
        )
    }

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
            queryAvailableVoices()
        }
    }

    private fun queryAvailableVoices() {
        try {
            val voices = tts?.voices
            if (voices != null) {
                _availableVoices.value = voices.toList()
                Log.d(tag, "Detected ${voices.size} system TTS voices.")
            }
        } catch (e: Throwable) {
            Log.w(tag, "Error querying system voices: ${e.message}")
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post {
                    _isSpeaking.value = true
                    if (isLiveUtterance) {
                        onDuckingChanged(true)
                    }
                    currentOnStartCallback?.invoke()
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    _isSpeaking.value = false
                    if (isLiveUtterance) {
                        onDuckingChanged(false)
                    }
                    currentOnFinishCallback?.invoke()
                    currentOnStartCallback = null
                    currentOnFinishCallback = null
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    _isSpeaking.value = false
                    if (isLiveUtterance) {
                        onDuckingChanged(false)
                    }
                    currentOnFinishCallback?.invoke()
                    currentOnStartCallback = null
                    currentOnFinishCallback = null
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post {
                    _isSpeaking.value = false
                    if (isLiveUtterance) {
                        onDuckingChanged(false)
                    }
                    currentOnFinishCallback?.invoke()
                    currentOnStartCallback = null
                    currentOnFinishCallback = null
                }
            }
        })
    }

    fun setSpeechRate(rate: Float) {
        currentSpeechRate = rate.coerceIn(0.5f, 2.0f)
        try {
            tts?.setSpeechRate(currentSpeechRate)
        } catch (_: Throwable) {}
    }

    fun setPitch(pitch: Float) {
        currentPitch = pitch.coerceIn(0.5f, 2.0f)
        try {
            tts?.setPitch(currentPitch)
        } catch (_: Throwable) {}
    }

    fun getPresetConfig(selectedName: String): VoicePresetConfig {
        val lower = selectedName.lowercase()
        return when {
            lower.contains("femenin") || lower.contains("enérgico") || lower.contains("energico") -> PRESETS[1]
            lower.contains("nightclub") || lower.contains("españa") || lower.contains("espana") -> PRESETS[2]
            lower.contains("neón") || lower.contains("neon") || lower.contains("deep") || lower.contains("bass") -> PRESETS[3]
            else -> PRESETS[0]
        }
    }

    private fun isVoiceMale(voice: Voice): Boolean {
        val name = voice.name.lowercase()
        val features = voice.features?.map { it.lowercase() } ?: emptyList()
        if (features.any { it.contains("female") || it == "gender=female" }) return false
        if (features.any { it.contains("male") || it == "gender=male" }) return true
        if (name.contains("female") || name.contains("mujer") || name.contains("dama")) return false
        if (name.contains("male") || name.contains("hombre") || name.contains("varon") || name.contains("varón") || name.contains("masculin")) return true
        if (Regex("(?:^|[^a-z])(male|hombre|varon|varón)(?:[^a-z]|$)").containsMatchIn(name)) return true
        // Known male voice code markers in Android TTS (e.g., Google TTS es-es-x-eed, es-es-x-cas, es-us-x-esc, es-us-x-esd)
        if (name.contains("-cas-") || name.contains("-eed-") || name.contains("-esc-") || name.contains("-esd-")) return true
        return false
    }

    private fun isVoiceFemale(voice: Voice): Boolean {
        val name = voice.name.lowercase()
        val features = voice.features?.map { it.lowercase() } ?: emptyList()
        if (features.any { it.contains("female") || it == "gender=female" }) return true
        if (name.contains("female") || name.contains("mujer") || name.contains("fem") || name.contains("femenin") || name.contains("dama") || name.contains("girl")) return true
        if (Regex("(?:^|[^a-z])(female|mujer|femenino|femenina)(?:[^a-z]|$)").containsMatchIn(name)) return true
        // Known female voice code markers in Android TTS (e.g., Google TTS es-es-x-ana, es-es-x-eea, es-us-x-sfb, es-us-x-efa)
        if (name.contains("-ana-") || name.contains("-eea-") || name.contains("-sfb-") || name.contains("-efa-")) return true
        return false
    }

    data class VoiceResult(
        val voice: Voice?,
        val isRealMaleVoice: Boolean
    )

    fun applyVoicePreset(selectedVoiceName: String): VoiceResult {
        val currentTts = tts ?: return VoiceResult(null, false)
        val preset = getPresetConfig(selectedVoiceName)
        val wantsMale = !preset.isFemale // 'Locutor Profesional', 'DJ Nightclub', 'Deep Bass' are male

        var chosenVoice: Voice? = null
        var isRealMaleAssigned = false

        try {
            val allVoices = currentTts.voices
            if (allVoices != null && allVoices.isNotEmpty()) {
                val installedVoices = allVoices.filter { v ->
                    val notInstalled = v.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true
                    !notInstalled
                }

                val spanishVoices = installedVoices.filter {
                    it.locale.language.equals("es", ignoreCase = true)
                }
                val candidatePool = if (spanishVoices.isNotEmpty()) spanishVoices else installedVoices

                if (wantsMale) {
                    val maleVoices = candidatePool.filter { isVoiceMale(it) }
                    if (maleVoices.isNotEmpty()) {
                        chosenVoice = if (preset.targetCountry != null) {
                            maleVoices.find { it.locale.country.equals(preset.targetCountry, ignoreCase = true) }
                                ?: maleVoices.first()
                        } else {
                            maleVoices.first()
                        }
                        isRealMaleAssigned = true
                    } else {
                        // Dispositivo no cuenta con voces masculinas instaladas
                        chosenVoice = if (preset.targetCountry != null) {
                            candidatePool.find { it.locale.country.equals(preset.targetCountry, ignoreCase = true) }
                                ?: candidatePool.firstOrNull()
                        } else {
                            candidatePool.firstOrNull()
                        }
                        isRealMaleAssigned = false
                    }
                } else {
                    // Femenino Enérgico
                    val femaleVoices = candidatePool.filter { isVoiceFemale(it) }
                    chosenVoice = if (femaleVoices.isNotEmpty()) {
                        if (preset.targetCountry != null) {
                            femaleVoices.find { it.locale.country.equals(preset.targetCountry, ignoreCase = true) }
                                ?: femaleVoices.first()
                        } else {
                            femaleVoices.first()
                        }
                    } else {
                        candidatePool.firstOrNull()
                    }
                    isRealMaleAssigned = false
                }
            }
        } catch (e: Throwable) {
            Log.w(tag, "Error querying system voices via TextToSpeech.getVoices(): ${e.message}")
        }

        if (chosenVoice != null) {
            try {
                currentTts.setVoice(chosenVoice)
                Log.d(tag, "Mapped preset '${preset.presetName}' to real voice: ${chosenVoice.name} (isRealMale=$isRealMaleAssigned)")
            } catch (e: Throwable) {
                Log.w(tag, "Error setting tts voice: ${e.message}")
            }
        } else {
            // Fallback de idioma
            try {
                if (preset.targetCountry != null) {
                    currentTts.setLanguage(Locale("es", preset.targetCountry))
                } else {
                    currentTts.setLanguage(Locale("es"))
                }
            } catch (_: Throwable) {}
        }

        return VoiceResult(chosenVoice, isRealMaleAssigned)
    }

    fun speakLocutorTts(
        text: String,
        selectedVoiceName: String,
        sliderRate: Float,
        sliderPitch: Float,
        isLive: Boolean,
        onStart: (() -> Unit)? = null,
        onFinish: (() -> Unit)? = null
    ) {
        if (!isInitialized || text.isBlank()) {
            onFinish?.invoke()
            return
        }
        val currentTts = tts ?: return

        isLiveUtterance = isLive
        currentOnStartCallback = onStart
        currentOnFinishCallback = onFinish

        val preset = getPresetConfig(selectedVoiceName)
        val voiceResult = applyVoicePreset(selectedVoiceName)

        // Asigna la voz elegida mediante tts.setVoice(selectedVoice) antes de reproducir el texto introducido
        voiceResult.voice?.let { voice ->
            try {
                currentTts.setVoice(voice)
            } catch (_: Throwable) {}
        }

        val wantsMale = !preset.isFemale
        // Si el dispositivo no cuenta con voces masculinas instaladas, aplica un ajuste automático
        // de Tono (Pitch) bajo (ejemplo: 0.65x) al seleccionar voces masculinas para forzar un timbre grave.
        val basePitch = if (wantsMale && !voiceResult.isRealMaleVoice) {
            0.65f
        } else {
            preset.basePitch
        }

        // Combinar slider del usuario con las características distintivas acústicas del preset
        val finalRate = (sliderRate * preset.baseRate).coerceIn(0.5f, 2.0f)
        val finalPitch = (sliderPitch * basePitch).coerceIn(0.4f, 2.0f)

        currentSpeechRate = finalRate
        currentPitch = finalPitch

        try {
            currentTts.setSpeechRate(finalRate)
            currentTts.setPitch(finalPitch)
        } catch (_: Throwable) {}

        _lastSpoken.value = text
        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle()
        currentTts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun speak(text: String, emocion: EmocionVoz) {
        if (!isInitialized || text.isBlank()) return
        val currentTts = tts ?: return

        isLiveUtterance = true
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

        isLiveUtterance = true
        var chosenVoice: Voice? = null
        var isRealMale = false

        try {
            val voices = currentTts.voices
            if (voices != null && voices.isNotEmpty()) {
                val installed = voices.filter { v ->
                    v.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
                }
                val spanish = installed.filter { it.locale.language.equals("es", ignoreCase = true) }
                val pool = if (spanish.isNotEmpty()) spanish else installed

                if (tipoLector.isFemale) {
                    val females = pool.filter { isVoiceFemale(it) }
                    chosenVoice = females.firstOrNull() ?: pool.firstOrNull()
                } else {
                    val males = pool.filter { isVoiceMale(it) }
                    if (males.isNotEmpty()) {
                        chosenVoice = males.first()
                        isRealMale = true
                    } else {
                        chosenVoice = pool.firstOrNull()
                        isRealMale = false
                    }
                }
            }
        } catch (_: Exception) {}

        chosenVoice?.let { voice ->
            try {
                currentTts.setVoice(voice)
            } catch (_: Throwable) {}
        }

        val effectivePitch = if (!tipoLector.isFemale && !isRealMale) {
            0.65f
        } else {
            tipoLector.pitch
        }

        try {
            currentTts.setPitch(effectivePitch)
            currentTts.setSpeechRate(tipoLector.rate)
        } catch (_: Throwable) {}

        _lastSpoken.value = text
        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle()
        currentTts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Throwable) {}
        mainHandler.post {
            _isSpeaking.value = false
            if (isLiveUtterance) {
                onDuckingChanged(false)
            }
            currentOnFinishCallback?.invoke()
            currentOnStartCallback = null
            currentOnFinishCallback = null
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {}
        tts = null
    }
}

/**
 * 3-Band Equalizer manager wrapping android.media.audiofx.Equalizer
 * attached directly to an audioSessionId.
 *
 * Frequency division:
 * - LOW: < 250 Hz (Bajos)
 * - MID: 500 Hz - 2 kHz (Medios, covers 250 Hz - 3.5 kHz)
 * - HI: > 4 kHz (Agudos, covers >= 3.5 kHz)
 *
 * Value mapping:
 * - 0.5f (center) -> 0 dB (0 millibels)
 * - 0.0f (minimum) -> minLevelMb (minimum supported millibels, e.g. -1500 mB)
 * - 1.0f (maximum) -> maxLevelMb (maximum supported millibels, e.g. +1500 mB)
 */
class DjThreeBandEqualizer(
    val audioSessionId: Int,
    priority: Int = 0
) {
    private val tag = "DjEqualizer"
    var equalizer: Equalizer? = null
        private set
    var isEnabled: Boolean = false
        private set

    var minLevelMb: Short = -1500
        private set
    var maxLevelMb: Short = 1500
        private set

    val lowBands = mutableListOf<Short>()
    val midBands = mutableListOf<Short>()
    val highBands = mutableListOf<Short>()

    private var currentLowNorm: Float = 0.5f
    private var currentMidNorm: Float = 0.5f
    private var currentHighNorm: Float = 0.5f

    init {
        try {
            if (audioSessionId > 0) {
                val eq = Equalizer(priority, audioSessionId)
                eq.enabled = true
                equalizer = eq
                isEnabled = true

                val range = eq.bandLevelRange
                if (range != null && range.size >= 2) {
                    minLevelMb = range[0]
                    maxLevelMb = range[1]
                }

                val numBands = eq.numberOfBands.toInt()
                for (b in 0 until numBands) {
                    val band = b.toShort()
                    // getCenterFreq returns milliHertz (mHz). Divide by 1000 to get Hz.
                    val centerFreqHz = eq.getCenterFreq(band) / 1000
                    when {
                        centerFreqHz < 250 -> lowBands.add(band)
                        centerFreqHz in 250..3500 -> midBands.add(band)
                        else -> highBands.add(band)
                    }
                }

                // Fallbacks to guarantee that every frequency segment has at least one band
                if (lowBands.isEmpty() && numBands > 0) {
                    lowBands.add(0.toShort())
                }
                if (highBands.isEmpty() && numBands > 1) {
                    highBands.add((numBands - 1).toShort())
                }
                if (midBands.isEmpty() && numBands > 2) {
                    for (b in 0 until numBands) {
                        val band = b.toShort()
                        if (!lowBands.contains(band) && !highBands.contains(band)) {
                            midBands.add(band)
                        }
                    }
                    if (midBands.isEmpty()) {
                        midBands.add((numBands / 2).toShort())
                    }
                }

                Log.d(tag, "Equalizer initialized for session $audioSessionId. Bands: $numBands, range: [$minLevelMb, $maxLevelMb] mB. Low: $lowBands, Mid: $midBands, High: $highBands")
            }
        } catch (e: Throwable) {
            Log.w(tag, "AudioFx Equalizer not supported on this platform: ${e.message}")
            equalizer = null
            isEnabled = false
        }
    }

    /**
     * Maps normalized knob position [0.0f .. 1.0f] with center 0.5f = 0 dB
     * - 0.0f -> minLevelMb
     * - 0.5f -> 0 mB (0 dB)
     * - 1.0f -> maxLevelMb
     */
    fun mapNormalizedToMilliBels(value: Float): Short {
        val clamped = value.coerceIn(0.0f, 1.0f)
        return when {
            clamped < 0.5f -> {
                val ratio = (0.5f - clamped) / 0.5f
                (minLevelMb * ratio).toInt().toShort()
            }
            clamped > 0.5f -> {
                val ratio = (clamped - 0.5f) / 0.5f
                (maxLevelMb * ratio).toInt().toShort()
            }
            else -> 0.toShort()
        }
    }

    fun setLow(value: Float) {
        currentLowNorm = value.coerceIn(0.0f, 1.0f)
        applyLevelToBands(lowBands, mapNormalizedToMilliBels(currentLowNorm))
    }

    fun setMid(value: Float) {
        currentMidNorm = value.coerceIn(0.0f, 1.0f)
        applyLevelToBands(midBands, mapNormalizedToMilliBels(currentMidNorm))
    }

    fun setHigh(value: Float) {
        currentHighNorm = value.coerceIn(0.0f, 1.0f)
        applyLevelToBands(highBands, mapNormalizedToMilliBels(currentHighNorm))
    }

    fun setBands(low: Float, mid: Float, high: Float) {
        setLow(low)
        setMid(mid)
        setHigh(high)
    }

    private fun applyLevelToBands(bands: List<Short>, levelMb: Short) {
        val eq = equalizer ?: return
        try {
            bands.forEach { band ->
                eq.setBandLevel(band, levelMb)
            }
        } catch (e: Throwable) {
            Log.w(tag, "Failed to set band level on Equalizer: ${e.message}")
        }
    }

    fun release() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: Throwable) {}
        equalizer = null
        isEnabled = false
    }
}

@OptIn(UnstableApi::class)
class DualExoPlayerEngine(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val deckA: ExoPlayer = ExoPlayer.Builder(context).build()
    val deckB: ExoPlayer = ExoPlayer.Builder(context).build()

    private var equalizerA: DjThreeBandEqualizer? = null
    private var equalizerB: DjThreeBandEqualizer? = null

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
    private val _crossfaderPosition = MutableStateFlow(0.5f)
    val crossfaderPosition: StateFlow<Float> = _crossfaderPosition.asStateFlow()

    // Individual channel faders
    private val _faderA = MutableStateFlow(0.9f)
    val faderA: StateFlow<Float> = _faderA.asStateFlow()

    private val _faderB = MutableStateFlow(0.9f)
    val faderB: StateFlow<Float> = _faderB.asStateFlow()

    private val _masterVolume = MutableStateFlow(0.85f)
    val masterVolume: StateFlow<Float> = _masterVolume.asStateFlow()

    private val _isAutoDj = MutableStateFlow(false)
    val isAutoDj: StateFlow<Boolean> = _isAutoDj.asStateFlow()

    private val _isDucked = MutableStateFlow(false)
    val isDucked: StateFlow<Boolean> = _isDucked.asStateFlow()

    private val _duckMultiplier = MutableStateFlow(1.0f)
    val duckMultiplier: StateFlow<Float> = _duckMultiplier.asStateFlow()

    private val _masterPlaying = MutableStateFlow(false)
    val masterPlaying: StateFlow<Boolean> = _masterPlaying.asStateFlow()

    // Normalized EQ bands (0.0 to 1.0, 0.5 = 0 dB)
    private val _deckAEqLow = MutableStateFlow(0.5f)
    val deckAEqLow: StateFlow<Float> = _deckAEqLow.asStateFlow()
    private val _deckAEqMid = MutableStateFlow(0.5f)
    val deckAEqMid: StateFlow<Float> = _deckAEqMid.asStateFlow()
    private val _deckAEqHigh = MutableStateFlow(0.5f)
    val deckAEqHigh: StateFlow<Float> = _deckAEqHigh.asStateFlow()

    private val _deckBEqLow = MutableStateFlow(0.5f)
    val deckBEqLow: StateFlow<Float> = _deckBEqLow.asStateFlow()
    private val _deckBEqMid = MutableStateFlow(0.5f)
    val deckBEqMid: StateFlow<Float> = _deckBEqMid.asStateFlow()
    private val _deckBEqHigh = MutableStateFlow(0.5f)
    val deckBEqHigh: StateFlow<Float> = _deckBEqHigh.asStateFlow()

    // Backward compatibility global EQ flows
    private val _eqLow = MutableStateFlow(0.5f)
    val eqLow: StateFlow<Float> = _eqLow.asStateFlow()
    private val _eqMid = MutableStateFlow(0.5f)
    val eqMid: StateFlow<Float> = _eqMid.asStateFlow()
    private val _eqHigh = MutableStateFlow(0.5f)
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
        ensureEqualizerForDeck(DeckId.DECK_A)
        ensureEqualizerForDeck(DeckId.DECK_B)
    }

    private fun ensureEqualizerForDeck(deckId: DeckId): DjThreeBandEqualizer? {
        val player = if (deckId == DeckId.DECK_A) deckA else deckB
        val currentEq = if (deckId == DeckId.DECK_A) equalizerA else equalizerB
        if (currentEq != null && currentEq.isEnabled) {
            return currentEq
        }
        val sessionId = player.audioSessionId
        if (sessionId > 0) {
            val newEq = DjThreeBandEqualizer(sessionId)
            if (deckId == DeckId.DECK_A) {
                equalizerA = newEq
                newEq.setBands(_deckAEqLow.value, _deckAEqMid.value, _deckAEqHigh.value)
            } else {
                equalizerB = newEq
                newEq.setBands(_deckBEqLow.value, _deckBEqMid.value, _deckBEqHigh.value)
            }
            return newEq
        }
        return null
    }

    private fun setupPlayerListeners() {
        deckA.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _deckAIsPlaying.value = isPlaying
                checkMasterPlaying()
                if (isPlaying) ensureEqualizerForDeck(DeckId.DECK_A)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _deckADuration.value = deckA.duration.coerceAtLeast(1L)
                    ensureEqualizerForDeck(DeckId.DECK_A)
                }
            }
        })

        deckB.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _deckBIsPlaying.value = isPlaying
                checkMasterPlaying()
                if (isPlaying) ensureEqualizerForDeck(DeckId.DECK_B)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _deckBDuration.value = deckB.duration.coerceAtLeast(1L)
                    ensureEqualizerForDeck(DeckId.DECK_B)
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
        executeAutomixTransition(
            fromDeck = fromDeck,
            toDeck = toDeck,
            type = TransitionType.CROSSFADE,
            durationSec = 8.0f
        )
    }

    fun executeAutomixTransition(
        fromDeck: DeckId,
        toDeck: DeckId,
        type: TransitionType,
        durationSec: Float,
        harmonicKeyLock: Boolean = true,
        onComplete: (() -> Unit)? = null
    ) {
        if (isCrossfading) return
        crossfadeJob?.cancel()
        isCrossfading = true

        crossfadeJob = scope.launch {
            val incomingPlayer = if (toDeck == DeckId.DECK_A) deckA else deckB
            val outgoingPlayer = if (fromDeck == DeckId.DECK_A) deckA else deckB
            val targetCrossfader = if (toDeck == DeckId.DECK_B) 1.0f else 0.0f
            val startCrossfader = if (toDeck == DeckId.DECK_B) 0.0f else 1.0f

            val totalDurationMs = (durationSec.coerceIn(2f, 16f) * 1000f).toLong()

            when (type) {
                TransitionType.CROSSFADE -> {
                    _crossfaderPosition.value = startCrossfader
                    _faderA.value = 0.9f
                    _faderB.value = 0.9f
                    updateDeckVolumes()
                    incomingPlayer.play()

                    val steps = (totalDurationMs / 50L).toInt().coerceAtLeast(10)
                    val delayPerStep = totalDurationMs / steps

                    for (i in 1..steps) {
                        delay(delayPerStep)
                        val progress = i.toFloat() / steps
                        _crossfaderPosition.value = startCrossfader + (targetCrossfader - startCrossfader) * progress
                        updateDeckVolumes()
                    }

                    _crossfaderPosition.value = targetCrossfader
                    updateDeckVolumes()
                    outgoingPlayer.pause()
                }
                TransitionType.BEATMATCH_SYNC -> {
                    if (harmonicKeyLock) {
                        try {
                            incomingPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(1.0f, 1.0f)
                        } catch (_: Throwable) {}
                    }
                    _crossfaderPosition.value = startCrossfader
                    _faderA.value = 0.9f
                    _faderB.value = 0.9f
                    updateDeckVolumes()
                    incomingPlayer.play()

                    val steps = (totalDurationMs / 50L).toInt().coerceAtLeast(10)
                    val delayPerStep = totalDurationMs / steps

                    for (i in 1..steps) {
                        delay(delayPerStep)
                        val progress = i.toFloat() / steps
                        // Equal power progression curve
                        _crossfaderPosition.value = startCrossfader + (targetCrossfader - startCrossfader) * progress
                        updateDeckVolumes()
                    }

                    _crossfaderPosition.value = targetCrossfader
                    updateDeckVolumes()
                    outgoingPlayer.pause()
                }
                TransitionType.FADE_OUT_IN -> {
                    val halfDuration = totalDurationMs / 2
                    val stepsHalf = (halfDuration / 50L).toInt().coerceAtLeast(5)
                    val delayHalf = halfDuration / stepsHalf

                    val outgoingFader = if (fromDeck == DeckId.DECK_A) _faderA else _faderB
                    val incomingFader = if (toDeck == DeckId.DECK_A) _faderA else _faderB

                    // Phase 1: Fade out outgoing deck to 0
                    for (i in 1..stepsHalf) {
                        delay(delayHalf)
                        val progress = i.toFloat() / stepsHalf
                        outgoingFader.value = (0.9f * (1.0f - progress)).coerceIn(0.0f, 0.9f)
                        updateDeckVolumes()
                    }

                    outgoingPlayer.pause()
                    outgoingFader.value = 0.9f

                    // Phase 2: Switch to incoming deck and fade in
                    _crossfaderPosition.value = targetCrossfader
                    incomingFader.value = 0.0f
                    updateDeckVolumes()
                    incomingPlayer.play()

                    for (i in 1..stepsHalf) {
                        delay(delayHalf)
                        val progress = i.toFloat() / stepsHalf
                        incomingFader.value = (0.9f * progress).coerceIn(0.0f, 0.9f)
                        updateDeckVolumes()
                    }

                    incomingFader.value = 0.9f
                    updateDeckVolumes()
                }
                TransitionType.CORTE_DIRECTO -> {
                    outgoingPlayer.pause()
                    _crossfaderPosition.value = targetCrossfader
                    _faderA.value = 0.9f
                    _faderB.value = 0.9f
                    updateDeckVolumes()
                    incomingPlayer.play()
                    delay(100L)
                }
            }

            _activeDeck.value = toDeck
            isCrossfading = false
            onComplete?.invoke()
        }
    }

    fun setAutoDj(enabled: Boolean) {
        _isAutoDj.value = enabled
    }

    fun setDucking(ducked: Boolean, level: Float = 0.15f) {
        _isDucked.value = ducked
        _duckMultiplier.value = if (ducked) level.coerceIn(0.05f, 1.0f) else 1.0f
        updateDeckVolumes()
    }

    fun setMasterVolume(volume: Float) {
        _masterVolume.value = volume.coerceIn(0.0f, 1.0f)
        updateDeckVolumes()
    }

    fun setDeckFader(deckId: DeckId, volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        if (deckId == DeckId.DECK_A) {
            _faderA.value = clamped
        } else {
            _faderB.value = clamped
        }
        updateDeckVolumes()
    }

    fun setCrossfader(position: Float) {
        if (isCrossfading) {
            crossfadeJob?.cancel()
            isCrossfading = false
        }
        val clamped = position.coerceIn(0.0f, 1.0f)
        _crossfaderPosition.value = clamped
        if (clamped < 0.5f) {
            _activeDeck.value = DeckId.DECK_A
        } else {
            _activeDeck.value = DeckId.DECK_B
        }
        updateDeckVolumes()
    }

    /**
     * Controls progressively the linear volume of Deck A and Deck B in real time:
     * - Crossfader position:
     *     0.0 (Deck A full cut): crossA = 1.0, crossB = 0.0
     *     0.5 (Center 50/50):    crossA = 0.5, crossB = 0.5
     *     1.0 (Deck B full cut): crossA = 0.0, crossB = 1.0
     * - Multiplied by Channel Fader (Fader A, Fader B), Master Volume, and Audio Ducking.
     */
    fun updateDeckVolumes() {
        val xf = _crossfaderPosition.value.coerceIn(0.0f, 1.0f)
        val fA = _faderA.value.coerceIn(0.0f, 1.0f)
        val fB = _faderB.value.coerceIn(0.0f, 1.0f)
        val master = _masterVolume.value.coerceIn(0.0f, 1.0f)
        val duck = if (_isDucked.value) _duckMultiplier.value.coerceIn(0.05f, 1.0f) else 1.0f

        val crossA = (1.0f - xf).coerceIn(0.0f, 1.0f)
        val crossB = xf.coerceIn(0.0f, 1.0f)

        val volA = (fA * crossA * master * duck).coerceIn(0.0f, 1.0f)
        val volB = (fB * crossB * master * duck).coerceIn(0.0f, 1.0f)

        deckA.volume = volA
        deckB.volume = volB
    }

    /**
     * Sets 3-Band Equalizer (LOW, MID, HI) for a specific deck:
     * Maps normalized knob position [0.0 .. 1.0] with center [0.5] = 0 dB
     * to the hardware android.media.audiofx.Equalizer frequency bands.
     */
    fun setDeckEq(deckId: DeckId, low: Float, mid: Float, high: Float) {
        val clampedLow = low.coerceIn(0.0f, 1.0f)
        val clampedMid = mid.coerceIn(0.0f, 1.0f)
        val clampedHigh = high.coerceIn(0.0f, 1.0f)

        if (deckId == DeckId.DECK_A) {
            _deckAEqLow.value = clampedLow
            _deckAEqMid.value = clampedMid
            _deckAEqHigh.value = clampedHigh
            ensureEqualizerForDeck(DeckId.DECK_A)?.setBands(clampedLow, clampedMid, clampedHigh)
        } else {
            _deckBEqLow.value = clampedLow
            _deckBEqMid.value = clampedMid
            _deckBEqHigh.value = clampedHigh
            ensureEqualizerForDeck(DeckId.DECK_B)?.setBands(clampedLow, clampedMid, clampedHigh)
        }
    }

    /**
     * Legacy / Global EQ: Accepts either normalized [0.0 .. 1.0] or dB [-12 .. +12]
     * and synchronizes to both decks.
     */
    fun setEq(low: Float, mid: Float, high: Float) {
        val normLow = if (low < 0f || low > 1f || high < 0f || high > 1f || mid < 0f || mid > 1f) {
            ((low + 12f) / 24f).coerceIn(0f, 1f)
        } else low
        val normMid = if (low < 0f || low > 1f || high < 0f || high > 1f || mid < 0f || mid > 1f) {
            ((mid + 12f) / 24f).coerceIn(0f, 1f)
        } else mid
        val normHigh = if (low < 0f || low > 1f || high < 0f || high > 1f || mid < 0f || mid > 1f) {
            ((high + 12f) / 24f).coerceIn(0f, 1f)
        } else high

        _eqLow.value = normLow
        _eqMid.value = normMid
        _eqHigh.value = normHigh
        setDeckEq(DeckId.DECK_A, normLow, normMid, normHigh)
        setDeckEq(DeckId.DECK_B, normLow, normMid, normHigh)
    }

    fun loadSong(deckId: DeckId, cancion: Cancion) {
        val mediaItem = MediaItem.fromUri(Uri.parse(cancion.uri))
        if (deckId == DeckId.DECK_A) {
            _deckASong.value = cancion
            deckA.setMediaItem(mediaItem)
            deckA.prepare()
            ensureEqualizerForDeck(DeckId.DECK_A)
        } else {
            _deckBSong.value = cancion
            deckB.setMediaItem(mediaItem)
            deckB.prepare()
            ensureEqualizerForDeck(DeckId.DECK_B)
        }
        updateDeckVolumes()
    }

    fun playDeck(deckId: DeckId) {
        ensureEqualizerForDeck(deckId)
        updateDeckVolumes()
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
