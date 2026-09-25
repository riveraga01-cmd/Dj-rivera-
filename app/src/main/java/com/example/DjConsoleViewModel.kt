package com.example

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
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

    private val _state = MutableStateFlow(DjConsoleState())
    val state: StateFlow<DjConsoleState> = _state.asStateFlow()

    private var countdownJob: Job? = null
    private var automixJob: Job? = null

    init {
        ttsManager = EmotionalTtsManager(application) { isDucked ->
            updateDuckingState(isDucked, if (isDucked) _state.value.adsDuckingLevel else 1.0f)
        }

        initializeDefaultPads()
        initializeDefaultAds()
        loadBuiltInTracks()
        startConsoleMonitoringLoop()
        startFirestoreSync()
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
            AnuncioItem("ad_1", "Cuña Rivera Hotel & Lounge", 15, "Cada 30m", activo = true),
            AnuncioItem("ad_2", "Promo Barra Libre 2x1 Cócteles", 12, "Cada 45m", activo = true),
            AnuncioItem("ad_3", "Aviso Estacionamiento & Guardarropa", 10, "Cada 60m", activo = false),
            AnuncioItem("ad_4", "Despedida y Cierre Seguro", 20, "Al Cierre", activo = true)
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

                // AutoMix transition check
                if (tick % 10 == 0 && _state.value.autoMixActive) {
                    updateAutoMixCountdown()
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

    private fun updateAutoMixCountdown() {
        _state.update { current ->
            val next = current.autoMixCountdownSec - 1
            if (next <= 0) {
                skipAutoMixTrack()
                current.copy(autoMixCountdownSec = (current.autoMixDurationSec * 2).toInt().coerceAtLeast(10))
            } else {
                current.copy(autoMixCountdownSec = next)
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
        audioEngine.setEq(low, mid, high)
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

        val announcement = "Mensaje publicitario en DJ Rivera: ${selectedAd.nombre}."
        ttsManager.speak(announcement, EmocionVoz.EMOCIONADO)

        viewModelScope.launch {
            delay(selectedAd.duracionSeg * 1000L.coerceAtLeast(4000L))
            _state.update { it.copy(isPlayingAd = false) }
            updateDuckingState(false, 1.0f)
        }
    }

    fun addNewAd(nombre: String = "Nueva Cuña Patrocinador", duracionSeg: Int = 15) {
        val newAd = AnuncioItem(
            id = "ad_${System.currentTimeMillis()}",
            nombre = nombre,
            duracionSeg = duracionSeg,
            frecuencia = "Cada 30m",
            activo = true
        )
        _state.update { it.copy(adsLibrary = it.adsLibrary + newAd, selectedAnuncioId = newAd.id) }
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

    // 3. Mic FX Panel Functions
    fun setMicOn(on: Boolean) {
        _state.update { it.copy(micOn = on) }
    }

    fun setTalkOver(active: Boolean) {
        _state.update { it.copy(talkOverActive = active) }
        updateDuckingState(active, if (active) 0.20f else 1.0f)
    }

    fun setMicGain(gain: Float) {
        _state.update { it.copy(micGain = gain) }
    }

    fun setMicEffect(fx: MicEffect) {
        _state.update { it.copy(selectedMicFx = fx) }
    }

    fun setPitchShiftValue(value: Float) {
        _state.update { it.copy(pitchShiftValue = value) }
    }

    fun setReverbIntensity(value: Float) {
        _state.update { it.copy(reverbIntensity = value) }
    }

    fun setEchoFeedback(value: Float) {
        _state.update { it.copy(echoFeedback = value) }
    }

    fun setEchoBpmSync(sync: String) {
        _state.update { it.copy(echoBpmSync = sync) }
    }

    fun setDryWetMix(mix: Float) {
        _state.update { it.copy(dryWetMix = mix) }
    }

    // 4. TTS Panel Functions
    fun setTtsMessage(msg: String) {
        _state.update { it.copy(ttsMessage = msg) }
    }

    fun setTtsVoice(voice: String) {
        _state.update { it.copy(selectedVoice = voice) }
    }

    fun setTtsRate(rate: Float) {
        _state.update { it.copy(ttsSpeechRate = rate) }
    }

    fun setTtsPitch(pitch: Float) {
        _state.update { it.copy(ttsPitch = pitch) }
    }

    fun previewTtsPfl() {
        _state.update { it.copy(ttsIsSpeaking = true, ttsPflActive = true) }
        ttsManager.speak(_state.value.ttsMessage, EmocionVoz.EMOCIONADO)
        viewModelScope.launch {
            delay(3500L)
            _state.update { it.copy(ttsIsSpeaking = false, ttsPflActive = false) }
        }
    }

    fun broadcastTtsLive() {
        _state.update { it.copy(ttsIsSpeaking = true, ttsPflActive = false) }
        updateDuckingState(true, _state.value.adsDuckingLevel)
        ttsManager.speak(_state.value.ttsMessage, EmocionVoz.EMOCIONADO)
        viewModelScope.launch {
            delay(4000L)
            _state.update { it.copy(ttsIsSpeaking = false) }
            updateDuckingState(false, 1.0f)
        }
    }

    fun saveTtsToAds() {
        val snippet = _state.value.ttsMessage.take(24)
        addNewAd("Locución: \"$snippet...\"", 12)
    }

    // 5. AutoMix Panel Functions
    fun setAutoMixActive(active: Boolean) {
        _state.update { it.copy(autoMixActive = active) }
        audioEngine.setAutoDj(active)
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
        val targetDeck = if (audioEngine.activeDeck.value == DeckId.DECK_A) DeckId.DECK_B else DeckId.DECK_A
        audioEngine.triggerAutoCrossfade(fromDeck = audioEngine.activeDeck.value, toDeck = targetDeck)

        if (_state.value.autoMixInsertSoundFx) {
            triggerSamplerPad(4) // Scratch
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

    // Library Filter
    fun setLibraryGenre(genre: String) {
        _state.update { it.copy(librarySelectedGenre = genre) }
    }

    fun setLibrarySearch(query: String) {
        _state.update { it.copy(librarySearchQuery = query) }
    }

    private fun updateDuckingState(isDucked: Boolean, multiplier: Float) {
        _state.update { it.copy(activeDucking = isDucked, duckingMultiplier = multiplier) }
        audioEngine.setDucking(isDucked)
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
        ttsManager.release()
        firebaseRepo.release()
    }
}
