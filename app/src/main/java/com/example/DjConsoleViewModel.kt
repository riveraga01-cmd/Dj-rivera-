package com.example

import android.app.Application
import android.content.ContentUris
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.components.DjAmber
import com.example.ui.components.DjCyan
import com.example.ui.components.DjGreen
import com.example.ui.components.DjOrange
import com.example.ui.components.DjRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DjConsoleViewModel(application: Application) : AndroidViewModel(application) {

    val audioEngine = DualExoPlayerEngine(application)
    val firebaseRepo = FirebaseRepository()
    val ttsManager: EmotionalTtsManager
    val micDspEngine: RealtimeMicDspEngine

    private val _state = MutableStateFlow(DjConsoleState())
    val state: StateFlow<DjConsoleState> = _state.asStateFlow()

    private var countdownJob: Job? = null
    private var automixJob: Job? = null
    private var isAutomixTransitioning = false

    init {
        ttsManager = EmotionalTtsManager(application) { isDucked ->
            updateDuckingState(isDucked, if (isDucked) _state.value.adsDuckingLevel else 1.0f)
        }

        micDspEngine = RealtimeMicDspEngine(application) { vuLevel ->
            _state.update { it.copy(micVuLevel = vuLevel) }
        }

        initializeDefaultPads()
        initializeDefaultAds()
        loadBuiltInTracks()
        startConsoleMonitoringLoop()
        startFirestoreSync()
        startUsbMidiListener()

        // Sync initial faders, crossfader, volume, and EQ to the audio engine
        audioEngine.setCrossfader(_state.value.crossfaderPosition)
        audioEngine.setDeckFader(DeckId.DECK_A, _state.value.deckA.faderVolume)
        audioEngine.setDeckFader(DeckId.DECK_B, _state.value.deckB.faderVolume)
        audioEngine.setMasterVolume(_state.value.masterVolume)
        audioEngine.setDeckEq(DeckId.DECK_A, _state.value.deckA.eqLow, _state.value.deckA.eqMid, _state.value.deckA.eqHigh)
        audioEngine.setDeckEq(DeckId.DECK_B, _state.value.deckB.eqLow, _state.value.deckB.eqMid, _state.value.deckB.eqHigh)

        // Sync initial TTS parameters
        ttsManager.setSpeechRate(_state.value.ttsSpeechRate)
        ttsManager.setPitch(_state.value.ttsPitch)
        ttsManager.applyVoicePreset(_state.value.selectedVoice)
    }

    private fun startUsbMidiListener() {
        viewModelScope.launch {
            UsbMidiService.events.collect { event ->
                when (event) {
                    is MidiControlEvent.Crossfader -> setCrossfader(event.value)
                    is MidiControlEvent.FaderA -> setDeckFader(DeckId.DECK_A, event.value)
                    is MidiControlEvent.FaderB -> setDeckFader(DeckId.DECK_B, event.value)
                    is MidiControlEvent.PlayDeck -> {
                        val isPlaying = if (event.deckId == DeckId.DECK_A) _state.value.deckA.isPlaying else _state.value.deckB.isPlaying
                        if (isPlaying) pauseDeck(event.deckId) else playDeck(event.deckId)
                    }
                    is MidiControlEvent.CueDeck -> toggleDeckCue(event.deckId)
                }
            }
        }
    }

    private fun initializeDefaultPads() {
        val defaultPads = listOf(
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
        _state.update { it.copy(samplerPads = defaultPads) }
    }

    private fun initializeDefaultAds() {
        val initialAds = listOf(
            AnuncioItem(
                id = "ad_1",
                nombre = "Cuña Rivera Hotel & Lounge",
                duracionSeg = 15,
                frecuencia = "Cada 30m",
                activo = true,
                textoLocucion = "Bienvenidos a Rivera Hotel and Lounge. Disfruta de la mejor música y coctelería de autor.",
                tipoLector = TipoLector.INSTITUCIONAL_HOTEL
            ),
            AnuncioItem(
                id = "ad_2",
                nombre = "Promo Barra Libre 2x1 Cócteles",
                duracionSeg = 12,
                frecuencia = "Cada 45m",
                activo = true,
                textoLocucion = "Atención a todas las mesas: dos por uno en cócteles seleccionados en la barra principal.",
                tipoLector = TipoLector.LOCUTORA_COMERCIAL
            ),
            AnuncioItem(
                id = "ad_3",
                nombre = "Aviso Estacionamiento & Guardarropa",
                duracionSeg = 10,
                frecuencia = "Cada 60m",
                activo = false,
                textoLocucion = "El servicio de estacionamiento y guardarropa se encuentra a su entera disposición.",
                tipoLector = TipoLector.LOCUTOR_RADIO
            ),
            AnuncioItem(
                id = "ad_4",
                nombre = "Despedida y Cierre Seguro",
                duracionSeg = 20,
                frecuencia = "Al Cierre",
                activo = true,
                textoLocucion = "Agradecemos su grata presencia esta noche. Recuerden conducir con precaución. Hasta pronto.",
                tipoLector = TipoLector.VOZ_PROFUNDA_CLUB
            )
        )
        _state.update {
            it.copy(
                adsLibrary = initialAds,
                selectedAnuncioId = initialAds.first().id
            )
        }
    }

    private fun startFirestoreSync() {
        viewModelScope.launch {
            firebaseRepo.peticiones.collect { peticiones ->
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val mapped = peticiones.map { p ->
                    QrRequestItem(
                        id = p.id,
                        mesa = p.mesa.ifBlank { "Mesa" },
                        cancion = p.cancion,
                        artista = p.artista,
                        hora = timeFormat.format(Date()),
                        estado = p.estado,
                        dedicatoria = p.dedicatoria,
                        emocion = p.emocion
                    )
                }

                _state.update { current ->
                    // Merge or replace
                    val existingNonPending = current.qrRequestsList.filter { it.estado != "PENDIENTE" }
                    val newCombined = mapped + existingNonPending.filter { exp -> mapped.none { it.id == exp.id } }
                    current.copy(qrRequestsList = newCombined)
                }

                // Smart auto-approve check
                if (_state.value.qrSmartAutoApprove) {
                    mapped.filter { it.estado == "PENDIENTE" }.forEach { pending ->
                        approveToAutoMix(pending)
                    }
                }
            }
        }
    }

    private fun startConsoleMonitoringLoop() {
        viewModelScope.launch {
            var tick = 0
            while (isActive) {
                delay(100L)
                tick++

                // Update Deck A positions and waveforms from audioEngine
                val posA = audioEngine.deckAPosition.value
                val durA = audioEngine.deckADuration.value
                val isPlayingA = audioEngine.deckAIsPlaying.value

                // Update Deck B
                val posB = audioEngine.deckBPosition.value
                val durB = audioEngine.deckBDuration.value
                val isPlayingB = audioEngine.deckBIsPlaying.value

                // Update VU levels
                val leftVu = if (isPlayingA) (0.4f + (tick % 7) * 0.08f).coerceIn(0.1f, 0.95f) else 0.05f
                val rightVu = if (isPlayingB) (0.4f + ((tick + 3) % 7) * 0.08f).coerceIn(0.1f, 0.95f) else 0.05f

                val crossfader = audioEngine.crossfaderPosition.value
                val masterPlaying = audioEngine.masterPlaying.value

                _state.update { current ->
                    current.copy(
                        masterPlaying = masterPlaying,
                        crossfaderPosition = crossfader,
                        masterVuLeft = leftVu,
                        masterVuRight = rightVu,
                        deckA = current.deckA.copy(
                            song = audioEngine.deckASong.value,
                            isPlaying = isPlayingA,
                            positionMs = posA,
                            durationMs = durA,
                            vuLevel = if (isPlayingA) leftVu else 0f
                        ),
                        deckB = current.deckB.copy(
                            song = audioEngine.deckBSong.value,
                            isPlaying = isPlayingB,
                            positionMs = posB,
                            durationMs = durB,
                            vuLevel = if (isPlayingB) rightVu else 0f
                        )
                    )
                }

                // Ads countdown loop (every 1s = 10 ticks)
                if (tick % 10 == 0 && _state.value.adsActive && !_state.value.isPlayingAd) {
                    updateAdsCountdown()
                }

                // AutoMix transition check: monitors active Deck position and executes transitions
                if (_state.value.autoMixActive) {
                    checkAutomixEngine()
                }
            }
        }
    }

    private fun updateAdsCountdown() {
        _state.update { current ->
            if (current.adsIntervalMode == AdsIntervalMode.POR_TIEMPO) {
                val nextCountdown = current.adsCountdownSeconds - 1
                if (nextCountdown <= 0) {
                    playAdNow()
                    current.copy(adsCountdownSeconds = (current.adsIntervalValue * 60).toInt())
                } else {
                    current.copy(adsCountdownSeconds = nextCountdown)
                }
            } else {
                current
            }
        }
    }

    private fun checkAutomixEngine() {
        if (isAutomixTransitioning) return

        val activeDeckId = audioEngine.activeDeck.value
        val (activeDeckState, otherDeckId) = if (activeDeckId == DeckId.DECK_A) {
            Pair(_state.value.deckA, DeckId.DECK_B)
        } else {
            Pair(_state.value.deckB, DeckId.DECK_A)
        }

        // If neither deck is playing, start playback on active deck
        if (!activeDeckState.isPlaying && !_state.value.deckA.isPlaying && !_state.value.deckB.isPlaying) {
            if (activeDeckState.song != null) {
                audioEngine.playDeck(activeDeckId)
            } else if (_state.value.librarySongs.isNotEmpty()) {
                val song = _state.value.librarySongs.first()
                audioEngine.loadSong(activeDeckId, song)
                audioEngine.playDeck(activeDeckId)
            }
            return
        }

        val durMs = activeDeckState.durationMs
        val posMs = activeDeckState.positionMs

        if (durMs > 3000L && activeDeckState.isPlaying) {
            val remainingMs = durMs - posMs
            val mixDurationSec = _state.value.autoMixDurationSec
            val mixDurationMs = (mixDurationSec * 1000f).toLong()

            val timeUntilMixMs = remainingMs - mixDurationMs
            val countdownSec = (timeUntilMixMs / 1000f).toInt().coerceAtLeast(0)

            _state.update { it.copy(autoMixCountdownSec = countdownSec) }

            // When remaining time <= mixDuration, trigger transition!
            if (remainingMs in 1L..mixDurationMs && !isAutomixTransitioning) {
                executeAutomixRoutine(fromDeck = activeDeckId, toDeck = otherDeckId)
            }
        }
    }

    private fun loadBuiltInTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val cumbia = DjAudioSynthesizer.createSongWavFile(context, "cumbia_rivera.wav", 60, 105, 130f, true)
            val salsa = DjAudioSynthesizer.createSongWavFile(context, "salsa_brava.wav", 60, 100, 146f, true)
            val electro = DjAudioSynthesizer.createSongWavFile(context, "cyber_beat.wav", 60, 128, 110f, false)
            val bachata = DjAudioSynthesizer.createSongWavFile(context, "bachata_sol.wav", 60, 130, 164f, true)
            val reggaeton = DjAudioSynthesizer.createSongWavFile(context, "reggaeton_duro.wav", 60, 96, 98f, false)
            val merengue = DjAudioSynthesizer.createSongWavFile(context, "merengue_loco.wav", 60, 140, 174f, true)

            val songs = listOf(
                Cancion("c1", "Cumbia Rivera", "Orquesta Rivera", "Cumbia", 60000L, cumbia.toURI().toString()),
                Cancion("s1", "Noche de Fuego", "DJ Rivera ft. Sabor Latino", "Salsa", 60000L, salsa.toURI().toString()),
                Cancion("e1", "Cyber Beat Drop", "Rivera Sound System", "Electrónica", 60000L, electro.toURI().toString()),
                Cancion("b1", "Bachata del Sol", "Los Románticos", "Bachata", 60000L, bachata.toURI().toString()),
                Cancion("r1", "Reggaeton Flow", "Rivera Flow", "Urbano", 60000L, reggaeton.toURI().toString()),
                Cancion("m1", "Merengue Loco", "Son Rivera", "Merengue", 60000L, merengue.toURI().toString())
            )

            _state.update {
                it.copy(
                    librarySongs = songs,
                    nextSong = songs[2]
                )
            }

            launch(Dispatchers.Main) {
                if (audioEngine.deckASong.value == null && songs.isNotEmpty()) {
                    audioEngine.loadSong(DeckId.DECK_A, songs[0])
                }
                if (audioEngine.deckBSong.value == null && songs.size > 1) {
                    audioEngine.loadSong(DeckId.DECK_B, songs[1])
                }
            }
        }
    }

    // Rack System Tabs
    fun selectRackTab(tab: RackTab) {
        _state.update { current ->
            if (current.activeRackTab == tab) {
                current.copy(isRackExpanded = !current.isRackExpanded)
            } else {
                current.copy(activeRackTab = tab, isRackExpanded = true)
            }
        }
    }

    fun toggleRackExpanded() {
        _state.update { it.copy(isRackExpanded = !it.isRackExpanded) }
    }

    // Master Controls
    fun toggleMasterPlayPause() {
        audioEngine.toggleMasterPlayPause()
    }

    fun setMasterVolume(vol: Float) {
        _state.update { it.copy(masterVolume = vol.coerceIn(0f, 1f)) }
    }

    fun toggleRecording() {
        _state.update { it.copy(isRecording = !it.isRecording) }
    }

    // Deck & Mixer Controls
    fun playDeck(deckId: DeckId) {
        audioEngine.playDeck(deckId)
    }

    fun pauseDeck(deckId: DeckId) {
        audioEngine.pauseDeck(deckId)
    }

    fun seekDeck(deckId: DeckId, progress: Float) {
        audioEngine.seekDeck(deckId, progress)
    }

    fun setCrossfader(pos: Float) {
        audioEngine.setCrossfader(pos)
    }

    fun setDeckGain(deckId: DeckId, gain: Float) {
        _state.update { current ->
            if (deckId == DeckId.DECK_A) {
                current.copy(deckA = current.deckA.copy(gain = gain))
            } else {
                current.copy(deckB = current.deckB.copy(gain = gain))
            }
        }
    }

    fun setDeckEq(deckId: DeckId, low: Float, mid: Float, high: Float) {
        audioEngine.setDeckEq(deckId, low, mid, high)
        _state.update { current ->
            if (deckId == DeckId.DECK_A) {
                current.copy(deckA = current.deckA.copy(eqLow = low, eqMid = mid, eqHigh = high))
            } else {
                current.copy(deckB = current.deckB.copy(eqLow = low, eqMid = mid, eqHigh = high))
            }
        }
    }

    fun setDeckFader(deckId: DeckId, volume: Float) {
        _state.update { current ->
            if (deckId == DeckId.DECK_A) {
                current.copy(deckA = current.deckA.copy(faderVolume = volume))
            } else {
                current.copy(deckB = current.deckB.copy(faderVolume = volume))
            }
        }
    }

    fun toggleDeckCue(deckId: DeckId) {
        _state.update { current ->
            if (deckId == DeckId.DECK_A) {
                current.copy(deckA = current.deckA.copy(cueActive = !current.deckA.cueActive))
            } else {
                current.copy(deckB = current.deckB.copy(cueActive = !current.deckB.cueActive))
            }
        }
    }

    fun loadSongToDeck(song: Cancion, deckId: DeckId) {
        audioEngine.loadSong(deckId, song)
        _state.update { current ->
            if (deckId == DeckId.DECK_A) {
                current.copy(deckA = current.deckA.copy(song = song))
            } else {
                current.copy(deckB = current.deckB.copy(song = song))
            }
        }
    }

    // 1. Ads Panel Functions
    fun setAdsActive(active: Boolean) {
        _state.update { it.copy(adsActive = active) }
    }

    fun setAdsIntervalMode(mode: AdsIntervalMode) {
        _state.update {
            it.copy(
                adsIntervalMode = mode,
                adsIntervalValue = if (mode == AdsIntervalMode.POR_TIEMPO) 30f else 5f,
                adsCountdownSeconds = if (mode == AdsIntervalMode.POR_TIEMPO) 1800 else 0
            )
        }
    }

    fun setAdsIntervalValue(value: Float) {
        _state.update {
            it.copy(
                adsIntervalValue = value,
                adsCountdownSeconds = (value * 60).toInt()
            )
        }
    }

    fun selectAnuncio(id: String) {
        _state.update { it.copy(selectedAnuncioId = id) }
    }

    fun toggleAnuncioActivo(id: String) {
        _state.update { current ->
            val updated = current.adsLibrary.map {
                if (it.id == id) it.copy(activo = !it.activo) else it
            }
            current.copy(adsLibrary = updated)
        }
    }

    fun setAdsDuckingLevel(level: Float) {
        _state.update { it.copy(adsDuckingLevel = level.coerceIn(0.05f, 0.50f)) }
    }

    fun setClosingTime(time: String) {
        _state.update { it.copy(closingTimeText = time) }
    }

    fun setClosingFarewell(enabled: Boolean) {
        _state.update { it.copy(closingFarewellAnnouncement = enabled) }
    }

    fun setClosingBlockQr(enabled: Boolean) {
        _state.update { it.copy(closingBlockQr = enabled) }
    }

    fun playAdNow() {
        val selectedAd = _state.value.adsLibrary.find { it.id == _state.value.selectedAnuncioId }
            ?: _state.value.adsLibrary.firstOrNull() ?: return

        _state.update { it.copy(isPlayingAd = true) }
        updateDuckingState(true, _state.value.adsDuckingLevel)

        // SOLO lee el texto exacto escrito en la caja de texto (sin prefijos sintéticos)
        val textToSpeak = selectedAd.textoLocucion.ifBlank { selectedAd.nombre }
        ttsManager.speakWithVoiceType(textToSpeak, selectedAd.tipoLector)

        viewModelScope.launch {
            delay(selectedAd.duracionSeg * 1000L.coerceAtLeast(3500L))
            _state.update { it.copy(isPlayingAd = false) }
            updateDuckingState(false, 1.0f)
        }
    }

    fun addNewAd(nombre: String = "Nueva Cuña Patrocinador", duracionSeg: Int = 15) {
        saveOrUpdateAd(null, nombre, duracionSeg, "Cada 30m", true, "", TipoLector.LOCUTOR_RADIO)
    }

    fun saveOrUpdateAd(
        id: String?,
        nombre: String,
        duracionSeg: Int,
        frecuencia: String,
        activo: Boolean = true,
        textoLocucion: String = "",
        tipoLector: TipoLector = TipoLector.LOCUTOR_RADIO
    ) {
        _state.update { current ->
            if (id != null && current.adsLibrary.any { it.id == id }) {
                val updated = current.adsLibrary.map {
                    if (it.id == id) it.copy(
                        nombre = nombre,
                        duracionSeg = duracionSeg,
                        frecuencia = frecuencia,
                        activo = activo,
                        textoLocucion = textoLocucion,
                        tipoLector = tipoLector
                    )
                    else it
                }
                current.copy(adsLibrary = updated)
            } else {
                val newAd = AnuncioItem(
                    id = "ad_${System.currentTimeMillis()}",
                    nombre = nombre,
                    duracionSeg = duracionSeg,
                    frecuencia = frecuencia,
                    activo = activo,
                    textoLocucion = textoLocucion,
                    tipoLector = tipoLector
                )
                current.copy(adsLibrary = current.adsLibrary + newAd, selectedAnuncioId = newAd.id)
            }
        }
    }

    fun deleteAd(id: String) {
        _state.update { current ->
            val updated = current.adsLibrary.filterNot { it.id == id }
            val nextSelected = if (current.selectedAnuncioId == id) updated.firstOrNull()?.id else current.selectedAnuncioId
            current.copy(adsLibrary = updated, selectedAnuncioId = nextSelected)
        }
    }

    // 2. QR Requests Panel Functions
    fun setShowQrDialog(show: Boolean) {
        _state.update { it.copy(showQrDialog = show) }
    }

    fun setQrAcceptRequests(accept: Boolean) {
        _state.update { it.copy(qrAcceptRequests = accept) }
    }

    fun setQrLimitPerUser(limit: Int) {
        _state.update { it.copy(qrLimitPerUser = limit) }
    }

    fun setQrSmartAutoApprove(auto: Boolean) {
        _state.update { it.copy(qrSmartAutoApprove = auto) }
    }

    fun approveToAutoMix(req: QrRequestItem) {
        firebaseRepo.aceptarPeticion(req.id)
        _state.update { current ->
            val updated = current.qrRequestsList.map {
                if (it.id == req.id) it.copy(estado = "EN_COLA") else it
            }
            current.copy(qrRequestsList = updated)
        }
        val match = _state.value.librarySongs.find { it.titulo.contains(req.cancion, ignoreCase = true) }
        if (match != null) {
            _state.update { it.copy(nextSong = match) }
        }
    }

    fun loadRequestToDeck(req: QrRequestItem, deckId: DeckId) {
        firebaseRepo.aceptarPeticion(req.id)
        _state.update { current ->
            val updated = current.qrRequestsList.map {
                if (it.id == req.id) it.copy(estado = "APROBADO") else it
            }
            current.copy(qrRequestsList = updated)
        }
        val match = _state.value.librarySongs.find { it.titulo.contains(req.cancion, ignoreCase = true) }
            ?: Cancion(
                id = "req_${req.id}",
                titulo = req.cancion,
                artista = req.artista,
                genero = "Petición QR",
                duracionMs = 60000L,
                uri = _state.value.librarySongs.firstOrNull()?.uri ?: ""
            )
        loadSongToDeck(match, deckId)
    }

    fun rejectRequest(req: QrRequestItem) {
        firebaseRepo.rechazarPeticion(req.id)
        _state.update { current ->
            val updated = current.qrRequestsList.map {
                if (it.id == req.id) it.copy(estado = "RECHAZADO") else it
            }
            current.copy(qrRequestsList = updated)
        }
    }

    // 3. Mic FX Panel Functions (Real-time AudioRecord DSP)
    fun setMicOn(on: Boolean) {
        _state.update { it.copy(micOn = on) }
        updateMicDspEngineState()
    }

    fun setTalkOver(active: Boolean) {
        _state.update { it.copy(talkOverActive = active) }
        updateDuckingState(active, if (active) 0.20f else 1.0f)
        updateMicDspEngineState()
    }

    private fun updateMicDspEngineState() {
        val shouldCapture = _state.value.micOn || _state.value.talkOverActive
        if (shouldCapture && !micDspEngine.isEnabled) {
            micDspEngine.start()
        } else if (!shouldCapture && micDspEngine.isEnabled) {
            micDspEngine.stop()
        }
    }

    fun setMicGain(gain: Float) {
        val clamped = gain.coerceIn(0f, 1f)
        _state.update { it.copy(micGain = clamped) }
        micDspEngine.micGain = clamped
    }

    fun setMicEffect(fx: MicEffect) {
        _state.update { it.copy(selectedMicFx = fx) }
        micDspEngine.selectedEffect = fx
    }

    fun setPitchShiftValue(value: Float) {
        val clamped = value.coerceIn(-12f, 12f)
        _state.update { it.copy(pitchShiftValue = clamped) }
        micDspEngine.pitchShiftValue = clamped
    }

    fun setReverbIntensity(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _state.update { it.copy(reverbIntensity = clamped) }
        micDspEngine.reverbIntensity = clamped
    }

    fun setEchoFeedback(value: Float) {
        val clamped = value.coerceIn(0f, 0.95f)
        _state.update { it.copy(echoFeedback = clamped) }
        micDspEngine.echoFeedback = clamped
    }

    fun setEchoBpmSync(sync: String) {
        _state.update { it.copy(echoBpmSync = sync) }
        micDspEngine.echoBpmSync = sync
    }

    fun setDryWetMix(mix: Float) {
        val clamped = mix.coerceIn(0f, 1f)
        _state.update { it.copy(dryWetMix = clamped) }
        micDspEngine.dryWetMix = clamped
    }

    // 4. TTS Panel Functions
    fun setTtsMessage(msg: String) {
        _state.update { it.copy(ttsMessage = msg) }
    }

    fun setTtsVoice(voice: String) {
        _state.update { it.copy(selectedVoice = voice) }
        ttsManager.applyVoicePreset(voice)
    }

    fun setTtsRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        _state.update { it.copy(ttsSpeechRate = clamped) }
        ttsManager.setSpeechRate(clamped)
    }

    fun setTtsPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        _state.update { it.copy(ttsPitch = clamped) }
        ttsManager.setPitch(clamped)
    }

    fun previewTtsPfl() {
        val message = _state.value.ttsMessage.trim()
        if (message.isEmpty()) return

        // Toggle: If currently playing PFL preview, clicking again stops it
        if (_state.value.ttsIsSpeaking && _state.value.ttsPflActive) {
            ttsManager.stop()
            _state.update { it.copy(ttsIsSpeaking = false, ttsPflActive = false) }
            return
        }

        // If speaking live, cancel live first
        if (_state.value.ttsIsSpeaking) {
            ttsManager.stop()
            updateDuckingState(false, 1.0f)
        }

        _state.update { it.copy(ttsIsSpeaking = true, ttsPflActive = true) }

        ttsManager.speakLocutorTts(
            text = message,
            selectedVoiceName = _state.value.selectedVoice,
            sliderRate = _state.value.ttsSpeechRate,
            sliderPitch = _state.value.ttsPitch,
            isLive = false, // Audífonos / PFL: pre-escucha sin atenuar la música de los Decks
            onStart = {
                _state.update { it.copy(ttsIsSpeaking = true, ttsPflActive = true) }
            },
            onFinish = {
                _state.update { it.copy(ttsIsSpeaking = false, ttsPflActive = false) }
            }
        )
    }

    fun broadcastTtsLive() {
        val message = _state.value.ttsMessage.trim()
        if (message.isEmpty()) return

        // Toggle: If currently broadcasting live, clicking again cuts locution and restores music
        if (_state.value.ttsIsSpeaking && !_state.value.ttsPflActive) {
            ttsManager.stop()
            updateDuckingState(false, 1.0f)
            _state.update { it.copy(ttsIsSpeaking = false, ttsPflActive = false) }
            return
        }

        // If speaking in PFL, stop PFL first
        if (_state.value.ttsIsSpeaking) {
            ttsManager.stop()
        }

        _state.update { it.copy(ttsIsSpeaking = true, ttsPflActive = false) }
        updateDuckingState(true, _state.value.adsDuckingLevel)

        ttsManager.speakLocutorTts(
            text = message,
            selectedVoiceName = _state.value.selectedVoice,
            sliderRate = _state.value.ttsSpeechRate,
            sliderPitch = _state.value.ttsPitch,
            isLive = true, // Al aire: Audio Ducking automático sobre los Decks A y B
            onStart = {
                _state.update { it.copy(ttsIsSpeaking = true, ttsPflActive = false) }
                updateDuckingState(true, _state.value.adsDuckingLevel)
            },
            onFinish = {
                _state.update { it.copy(ttsIsSpeaking = false, ttsPflActive = false) }
                updateDuckingState(false, 1.0f)
            }
        )
    }

    fun saveTtsToAds() {
        val snippet = _state.value.ttsMessage.take(24)
        addNewAd("Locución: \"$snippet...\"", 12)
    }

    // 5. AutoMix Panel Functions
    fun setAutoMixActive(active: Boolean) {
        _state.update { it.copy(autoMixActive = active) }
        if (active) {
            // If neither deck is currently playing, start playing Deck A
            if (!_state.value.deckA.isPlaying && !_state.value.deckB.isPlaying) {
                if (_state.value.deckA.song != null) {
                    audioEngine.playDeck(DeckId.DECK_A)
                } else if (_state.value.librarySongs.isNotEmpty()) {
                    val firstSong = _state.value.librarySongs.first()
                    audioEngine.loadSong(DeckId.DECK_A, firstSong)
                    audioEngine.playDeck(DeckId.DECK_A)
                }
            }
        }
    }

    fun setAutoMixTransitionType(type: TransitionType) {
        _state.update { it.copy(autoMixTransitionType = type) }
    }

    fun setAutoMixDuration(duration: Float) {
        _state.update { it.copy(autoMixDurationSec = duration) }
    }

    fun setHarmonicKeyLock(enabled: Boolean) {
        _state.update { it.copy(autoMixHarmonicKeyLock = enabled) }
    }

    fun setAutoMixInsertAds(enabled: Boolean) {
        _state.update { it.copy(autoMixInsertAds = enabled) }
    }

    fun setAutoMixInsertSoundFx(enabled: Boolean) {
        _state.update { it.copy(autoMixInsertSoundFx = enabled) }
    }

    fun skipAutoMixTrack() {
        val currentActive = audioEngine.activeDeck.value
        val targetDeck = if (currentActive == DeckId.DECK_A) DeckId.DECK_B else DeckId.DECK_A
        executeAutomixRoutine(fromDeck = currentActive, toDeck = targetDeck)
    }

    private fun executeAutomixRoutine(fromDeck: DeckId, toDeck: DeckId) {
        if (isAutomixTransitioning) return
        isAutomixTransitioning = true

        val songToLoad = _state.value.nextSong ?: getNextAutomixSong()
        if (songToLoad != null) {
            audioEngine.loadSong(toDeck, songToLoad)
            audioEngine.seekDeck(toDeck, 0f)
        }

        if (_state.value.autoMixInsertSoundFx) {
            audioEngine.playDjPad(DjPadEffect.SWEEP)
            triggerSamplerPad(4) // Scratch effect pad
        }

        val transitionType = _state.value.autoMixTransitionType
        val mixDuration = _state.value.autoMixDurationSec
        val harmonicLock = _state.value.autoMixHarmonicKeyLock

        audioEngine.executeAutomixTransition(
            fromDeck = fromDeck,
            toDeck = toDeck,
            type = transitionType,
            durationSec = mixDuration,
            harmonicKeyLock = harmonicLock
        ) {
            isAutomixTransitioning = false

            // Rotate queue to next song
            val nextUpcoming = getNextAutomixSong(after = songToLoad)
            _state.update { current ->
                current.copy(
                    nextSong = nextUpcoming,
                    nextSongBpm = (100..132).random(),
                    nextSongKey = listOf("8A / Am", "9A / Em", "7B / F", "4A / Fm", "11B / A").random(),
                    autoMixCountdownSec = (current.autoMixDurationSec * 2).toInt().coerceAtLeast(10)
                )
            }

            if (_state.value.autoMixInsertAds && _state.value.adsActive) {
                playAdNow()
            }
        }
    }

    private fun getNextAutomixSong(after: Cancion? = null): Cancion? {
        val songs = _state.value.librarySongs
        if (songs.isEmpty()) return null
        if (after == null) return songs.firstOrNull()

        val idx = songs.indexOfFirst { it.id == after.id }
        return if (idx >= 0 && idx < songs.size - 1) {
            songs[idx + 1]
        } else {
            songs.firstOrNull()
        }
    }

    // 6. Sampler Panel Functions
    fun triggerSamplerPad(padId: Int) {
        _state.update { current ->
            val updated = current.samplerPads.map {
                if (it.id == padId) it.copy(isPlaying = true) else it
            }
            current.copy(samplerPads = updated)
        }

        if (_state.value.samplerDuckingEnabled) {
            updateDuckingState(true, 0.40f)
        }

        val fx = when (padId) {
            1 -> DjPadEffect.SWOOSH
            2 -> DjPadEffect.REVERB
            3 -> DjPadEffect.ECHO
            4 -> DjPadEffect.SWEEP
            5 -> DjPadEffect.SWOOSH
            else -> DjPadEffect.REVERB
        }
        audioEngine.playDjPad(fx)

        viewModelScope.launch {
            delay(700L)
            _state.update { current ->
                val updated = current.samplerPads.map {
                    if (it.id == padId) it.copy(isPlaying = false) else it
                }
                current.copy(samplerPads = updated)
            }
            if (_state.value.samplerDuckingEnabled && !_state.value.talkOverActive && !_state.value.isPlayingAd) {
                updateDuckingState(false, 1.0f)
            }
        }
    }

    fun assignCustomSample(padId: Int) {
        _state.update { current ->
            val updated = current.samplerPads.map {
                if (it.id == padId) it.copy(name = "SAMPLE #$padId", colorHex = 0xFF00E676, customUri = "custom_uri_$padId") else it
            }
            current.copy(samplerPads = updated)
        }
    }

    fun setSamplerVolume(vol: Float) {
        _state.update { it.copy(samplerVolume = vol) }
    }

    fun setSamplerTriggerMode(mode: SamplerTriggerMode) {
        _state.update { it.copy(samplerTriggerMode = mode) }
    }

    fun setSamplerDucking(enabled: Boolean) {
        _state.update { it.copy(samplerDuckingEnabled = enabled) }
    }

    // Library Filter & MediaStore Local Audio Scanning
    fun setLibraryGenre(genre: String) {
        _state.update { it.copy(librarySelectedGenre = genre) }
    }

    fun setLibrarySearch(query: String) {
        _state.update { it.copy(librarySearchQuery = query) }
    }

    fun importSelectedAudioUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val imported = mutableListOf<Cancion>()

            for (uri in uris) {
                try {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {}

                    var displayName = "Canción Local"
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIdx != -1) {
                            displayName = cursor.getString(nameIdx) ?: "Canción Local"
                        }
                    }

                    var title = displayName.substringBeforeLast(".")
                    var artist = "Archivo Local"
                    var durationMs = 180000L

                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        val metaDur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                        if (!metaTitle.isNullOrBlank()) title = metaTitle
                        if (!metaArtist.isNullOrBlank()) artist = metaArtist
                        metaDur?.toLongOrNull()?.let { if (it > 0) durationMs = it }
                    } catch (e: Exception) {
                        Log.w("DjConsoleViewModel", "Error extracting metadata from uri $uri: ${e.message}")
                    } finally {
                        try { retriever.release() } catch (_: Exception) {}
                    }

                    imported.add(
                        Cancion(
                            id = "saf_${UUID.randomUUID().toString().replace("-", "").take(8)}",
                            titulo = title,
                            artista = artist,
                            genero = "Local",
                            duracionMs = durationMs,
                            uri = uri.toString()
                        )
                    )
                } catch (e: Exception) {
                    Log.e("DjConsoleViewModel", "Error importing audio uri $uri: ${e.message}", e)
                }
            }

            if (imported.isNotEmpty()) {
                _state.update { current ->
                    val existingUris = current.librarySongs.map { it.uri }.toSet()
                    val uniqueNew = imported.filter { it.uri !in existingUris }
                    val updatedList = current.librarySongs + uniqueNew
                    val localCount = updatedList.count { it.genero == "Local" }
                    current.copy(
                        librarySongs = updatedList,
                        localSongsCount = localCount,
                        librarySelectedGenre = "Local"
                    )
                }
            }
        }
    }

    fun setLocalPermissionDenied(denied: Boolean) {
        _state.update { it.copy(localPermissionDenied = denied, isScanningLocalMedia = false) }
    }

    fun scanMediaStoreMusic() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isScanningLocalMedia = true, localPermissionDenied = false) }
            try {
                val context = getApplication<Application>()
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATA
                )

                // Query external audio media flagged as music
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

                val cursor = context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    sortOrder
                )

                val localSongs = mutableListOf<Cancion>()
                val validExtensions = listOf(".mp3", ".wav", ".aac")

                cursor?.use { c ->
                    val idCol = c.getColumnIndex(MediaStore.Audio.Media._ID)
                    val titleCol = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val durCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                    val nameCol = c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                    val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)

                    while (c.moveToNext()) {
                        val id = if (idCol >= 0) c.getLong(idCol) else continue
                        val displayName = if (nameCol >= 0) c.getString(nameCol) ?: "" else ""
                        val dataPath = if (dataCol >= 0) c.getString(dataCol) ?: "" else ""

                        // Filter valid audio files (.mp3, .wav, .aac)
                        val isValidAudio = validExtensions.any { ext ->
                            displayName.endsWith(ext, ignoreCase = true) || dataPath.endsWith(ext, ignoreCase = true)
                        }

                        if (!isValidAudio && displayName.isNotBlank()) {
                            continue
                        }

                        val rawTitle = if (titleCol >= 0) c.getString(titleCol) else null
                        val cleanTitle = if (!rawTitle.isNullOrBlank()) rawTitle
                            else if (displayName.isNotBlank()) displayName.substringBeforeLast(".")
                            else "Pista Local $id"

                        val rawArtist = if (artistCol >= 0) c.getString(artistCol) else null
                        val cleanArtist = if (!rawArtist.isNullOrBlank() && rawArtist != "<unknown>") rawArtist else "Archivo Local"

                        val duration = if (durCol >= 0) c.getLong(durCol) else 0L

                        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                        localSongs.add(
                            Cancion(
                                id = "local_$id",
                                titulo = cleanTitle,
                                artista = cleanArtist,
                                genero = "Local",
                                duracionMs = duration.coerceAtLeast(1000L),
                                uri = contentUri.toString()
                            )
                        )
                    }
                }

                _state.update { current ->
                    val nonLocal = current.librarySongs.filter { it.genero != "Local" }
                    current.copy(
                        librarySongs = nonLocal + localSongs,
                        librarySelectedGenre = "Local",
                        isScanningLocalMedia = false,
                        localPermissionDenied = false,
                        localSongsCount = localSongs.size
                    )
                }
                Log.d("DjConsoleViewModel", "MediaStore scan completed. Found ${localSongs.size} local audio files.")
            } catch (e: Exception) {
                Log.e("DjConsoleViewModel", "Error scanning MediaStore: ${e.message}", e)
                _state.update { it.copy(isScanningLocalMedia = false) }
            }
        }
    }

    private fun updateDuckingState(isDucked: Boolean, multiplier: Float) {
        _state.update { it.copy(activeDucking = isDucked, duckingMultiplier = multiplier) }
        audioEngine.setDucking(isDucked, multiplier)
    }

    override fun onCleared() {
        super.onCleared()
        micDspEngine.stop()
        audioEngine.release()
        ttsManager.release()
        firebaseRepo.release()
    }
}
