package com.example

enum class RackTab(val label: String) {
    ANUNCIOS("ANUNCIOS"),
    PETICIONES_QR("PETICIONES QR"),
    MIC_FX("MIC FX"),
    TTS("TEXT TO SPEECH"),
    AUTOMIX("AUTOMIX"),
    SAMPLER("SAMPLER")
}

enum class AdsIntervalMode(val label: String) {
    POR_TIEMPO("Por Tiempo"),
    POR_CANCIONES("Por Cantidad de Canciones")
}

enum class TipoLector(val label: String, val pitch: Float, val rate: Float, val isFemale: Boolean = false) {
    LOCUTOR_RADIO("🎙️ Locutor Radio DJ (Cálido y Enérgico)", 1.02f, 1.05f, false),
    LOCUTORA_COMERCIAL("👩 Locutora Comercial (Suave y Natural)", 1.15f, 0.98f, true),
    VOZ_PROFUNDA_CLUB("🎧 Voz Profunda Club (Grave e Impacto)", 0.82f, 0.95f, false),
    ANIMADOR_FIESTA("🔥 Animador de Fiesta (Dinámico)", 1.10f, 1.15f, false),
    INSTITUCIONAL_HOTEL("👔 Institucional Rivera (Pausado y Fino)", 0.96f, 0.92f, false)
}

data class AnuncioItem(
    val id: String,
    val nombre: String,
    val duracionSeg: Int,
    val frecuencia: String,
    val activo: Boolean,
    val uri: String = "",
    val textoLocucion: String = "",
    val tipoLector: TipoLector = TipoLector.LOCUTOR_RADIO
)

enum class MicEffect(val label: String) {
    NONE("DIRECTO"),
    PITCH_SHIFT("PITCH SHIFT"),
    ROBOT("ROBOT"),
    MEGAFONO("MEGÁFONO"),
    REVERB("REVERB"),
    ECHO_DELAY("ECHO / DELAY")
}

enum class TransitionType(val label: String) {
    CROSSFADE("Crossfade"),
    BEATMATCH_SYNC("Beatmatch Sync"),
    FADE_OUT_IN("FadeOut / FadeIn"),
    CORTE_DIRECTO("Corte Directo")
}

enum class SamplerTriggerMode(val label: String) {
    TRIGGER("TRIGGER"),
    HOLD("HOLD"),
    LOOP("LOOP")
}

data class SamplerPadState(
    val id: Int,
    val name: String,
    val colorHex: Long,
    val customUri: String? = null,
    val isPlaying: Boolean = false
)

data class QrRequestItem(
    val id: String,
    val mesa: String,
    val cancion: String,
    val artista: String,
    val hora: String,
    val estado: String = "PENDIENTE", // PENDIENTE, APROBADO, RECHAZADO, EN_COLA
    val dedicatoria: String = "",
    val emocion: String = "NEUTRO"
)

data class DeckState(
    val deckId: DeckId,
    val song: Cancion? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 60000L,
    val bpm: Int = 128,
    val musicalKey: String = "8A / Am",
    val gain: Float = 0.8f,
    val eqLow: Float = 0.5f,
    val eqMid: Float = 0.5f,
    val eqHigh: Float = 0.5f,
    val faderVolume: Float = 0.9f,
    val cueActive: Boolean = false,
    val pitchPercent: Float = 0f,
    val vuLevel: Float = 0.2f
)

val defaultSamplerPads: List<SamplerPadState> = listOf(
    SamplerPadState(1, "AIRHORN", 0xFF00E5FF),
    SamplerPadState(2, "APLAUSOS", 0xFF00E676),
    SamplerPadState(3, "RISAS", 0xFFFFD600),
    SamplerPadState(4, "SCRATCH", 0xFFFF6D00),
    SamplerPadState(5, "SIRENA", 0xFFFF1744),
    SamplerPadState(6, "JINGLE LOCAL", 0xFFE040FB),
    SamplerPadState(7, "DROP BASS", 0xFF00E5FF, customUri = "built_in_drop"),
    SamplerPadState(8, "LASER FX", 0xFF00E676, customUri = "built_in_laser"),
    SamplerPadState(9, "VOCODER", 0xFFFF6D00, customUri = "built_in_vocoder"),
    SamplerPadState(10, "SLOT 10", 0xFF4A5568),
    SamplerPadState(11, "SLOT 11", 0xFF4A5568),
    SamplerPadState(12, "SLOT 12", 0xFF4A5568)
)

data class DjConsoleState(
    // Master Section
    val masterVolume: Float = 0.85f,
    val masterVuLeft: Float = 0.65f,
    val masterVuRight: Float = 0.62f,
    val masterBpm: Int = 128,
    val masterPlaying: Boolean = false,
    val isRecording: Boolean = false,
    val activeDucking: Boolean = false,
    val duckingMultiplier: Float = 1.0f,

    // Decks & Mixer
    val deckA: DeckState = DeckState(deckId = DeckId.DECK_A),
    val deckB: DeckState = DeckState(deckId = DeckId.DECK_B),
    val crossfaderPosition: Float = 0.5f, // 0.0 = A, 1.0 = B
    val headphoneMix: Float = 0.5f,
    val headphoneVolume: Float = 0.75f,

    // Rack System
    val activeRackTab: RackTab? = RackTab.ANUNCIOS,
    val isRackExpanded: Boolean = true,

    // 1. Ads Panel State
    val adsActive: Boolean = true,
    val adsIntervalMode: AdsIntervalMode = AdsIntervalMode.POR_TIEMPO,
    val adsIntervalValue: Float = 30f, // 30 minutes or 5 songs
    val adsCountdownSeconds: Int = 845, // 14:05 mm:ss
    val adsSongsRemaining: Int = 3,
    val adsLibrary: List<AnuncioItem> = emptyList(),
    val selectedAnuncioId: String? = null,
    val adsDuckingLevel: Float = 0.15f,
    val closingTimeText: String = "02:00 AM",
    val closingFarewellAnnouncement: Boolean = true,
    val closingBlockQr: Boolean = true,
    val isPlayingAd: Boolean = false,

    // 2. QR Requests Panel State
    val showQrDialog: Boolean = false,
    val qrServerUrl: String = "http://192.168.1.50:8080/pedir",
    val qrAcceptRequests: Boolean = true,
    val qrLimitPerUser: Int = 3,
    val qrSmartAutoApprove: Boolean = false,
    val qrRequestsList: List<QrRequestItem> = emptyList(),

    // 3. Mic FX Panel State
    val micOn: Boolean = false,
    val talkOverActive: Boolean = false,
    val micGain: Float = 0.75f,
    val micVuLevel: Float = 0.0f,
    val selectedMicFx: MicEffect = MicEffect.NONE,
    val pitchShiftValue: Float = 0f, // -12 .. +12 semitones
    val reverbIntensity: Float = 0.4f,
    val echoFeedback: Float = 0.35f,
    val echoBpmSync: String = "1/2",
    val dryWetMix: Float = 0.5f,

    // 4. TTS Synthesizer Panel State
    val ttsMessage: String = "¡Bienvenidos a Rivera Club! En cabina DJ Rivera encendiendo la noche.",
    val selectedVoice: String = "Español Latino (Locutor Profesional)",
    val availableVoices: List<String> = listOf(
        "Español Latino (Locutor Profesional)",
        "Español Latino (Femenino Enérgico)",
        "Español España (DJ Nightclub)",
        "Voz Modulada Neón (Deep Bass)"
    ),
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsIsSpeaking: Boolean = false,
    val ttsPflActive: Boolean = false,

    // 5. AutoMix Panel State
    val autoMixActive: Boolean = false,
    val autoMixTransitionType: TransitionType = TransitionType.CROSSFADE,
    val autoMixDurationSec: Float = 8f,
    val autoMixHarmonicKeyLock: Boolean = true,
    val autoMixInsertAds: Boolean = true,
    val autoMixInsertSoundFx: Boolean = true,
    val nextSong: Cancion? = null,
    val nextSongBpm: Int = 126,
    val nextSongKey: String = "8A / Am",
    val autoMixCountdownSec: Int = 18,

    // 6. Sampler Panel State
    val samplerPads: List<SamplerPadState> = defaultSamplerPads,
    val samplerVolume: Float = 0.85f,
    val samplerTriggerMode: SamplerTriggerMode = SamplerTriggerMode.TRIGGER,
    val samplerDuckingEnabled: Boolean = true,

    // Music Library Section
    val librarySongs: List<Cancion> = emptyList(),
    val librarySelectedGenre: String = "TODOS",
    val librarySearchQuery: String = "",
    val isScanningLocalMedia: Boolean = false,
    val localPermissionDenied: Boolean = false,
    val localSongsCount: Int = 0
)
