package com.example

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class VirtualDjViewModel(application: Application) : AndroidViewModel(application) {

    val audioEngine = DualExoPlayerEngine(application)
    val firebaseRepo = FirebaseRepository()
    val ttsManager: EmotionalTtsManager

    private val _canciones = MutableStateFlow<List<Cancion>>(emptyList())
    val canciones: StateFlow<List<Cancion>> = _canciones.asStateFlow()

    private val _generoSeleccionado = MutableStateFlow("TODOS")
    val generoSeleccionado: StateFlow<String> = _generoSeleccionado.asStateFlow()

    private val _ttsInputText = MutableStateFlow("¡Saludos a la mesa 4 que está de fiesta con DJ Rivera!")
    val ttsInputText: StateFlow<String> = _ttsInputText.asStateFlow()

    private val _ttsEmocion = MutableStateFlow(EmocionVoz.EMOCIONADO)
    val ttsEmocion: StateFlow<EmocionVoz> = _ttsEmocion.asStateFlow()

    val peticionesPendientes: StateFlow<List<PeticionMesa>> = firebaseRepo.peticiones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        ttsManager = EmotionalTtsManager(application) { isDucked ->
            audioEngine.setDucking(isDucked)
        }

        // Auto-DJ next song supplier
        audioEngine.onSongRequestNext = {
            val list = _canciones.value
            if (list.isNotEmpty()) {
                val currentA = audioEngine.deckASong.value
                val currentB = audioEngine.deckBSong.value
                list.firstOrNull { it.id != currentA?.id && it.id != currentB?.id } ?: list.first()
            } else null
        }

        loadBuiltInTracks()
        scanDeviceMusic()
    }

    private fun loadBuiltInTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            // Generate real 44.1kHz stereo audio files
            val cumbiaFile = DjAudioSynthesizer.createSongWavFile(
                context, "cumbia_rivera.wav", 60, 105, 130f, isLatinStyle = true
            )
            val salsaFile = DjAudioSynthesizer.createSongWavFile(
                context, "salsa_brava.wav", 60, 100, 146f, isLatinStyle = true
            )
            val electroFile = DjAudioSynthesizer.createSongWavFile(
                context, "cyber_beat.wav", 60, 128, 110f, isLatinStyle = false
            )
            val bachataFile = DjAudioSynthesizer.createSongWavFile(
                context, "bachata_sol.wav", 60, 130, 164f, isLatinStyle = true
            )
            val reggaetonFile = DjAudioSynthesizer.createSongWavFile(
                context, "reggaeton_duro.wav", 60, 96, 98f, isLatinStyle = false
            )
            val merengueFile = DjAudioSynthesizer.createSongWavFile(
                context, "merengue_loco.wav", 60, 140, 174f, isLatinStyle = true
            )

            val builtInList = listOf(
                Cancion(
                    id = "cumbia_1",
                    titulo = "Cumbia Rivera",
                    artista = "Orquesta Rivera",
                    genero = "Cumbia",
                    duracionMs = 60000L,
                    uri = cumbiaFile.toURI().toString()
                ),
                Cancion(
                    id = "salsa_1",
                    titulo = "Noche de Fuego",
                    artista = "DJ Rivera ft. Sabor Latino",
                    genero = "Salsa",
                    duracionMs = 60000L,
                    uri = salsaFile.toURI().toString()
                ),
                Cancion(
                    id = "electro_1",
                    titulo = "Cyber Beat Drop",
                    artista = "Rivera Sound System",
                    genero = "Electrónica",
                    duracionMs = 60000L,
                    uri = electroFile.toURI().toString()
                ),
                Cancion(
                    id = "bachata_1",
                    titulo = "Bachata del Sol",
                    artista = "Los Románticos de Rivera",
                    genero = "Bachata",
                    duracionMs = 60000L,
                    uri = bachataFile.toURI().toString()
                ),
                Cancion(
                    id = "reggaeton_1",
                    titulo = "Reggaeton Flow",
                    artista = "Rivera Flow",
                    genero = "Urbano",
                    duracionMs = 60000L,
                    uri = reggaetonFile.toURI().toString()
                ),
                Cancion(
                    id = "merengue_1",
                    titulo = "Merengue Loco",
                    artista = "Son Rivera",
                    genero = "Merengue",
                    duracionMs = 60000L,
                    uri = merengueFile.toURI().toString()
                )
            )

            _canciones.value = builtInList

            // Load initial tracks into Deck A and Deck B
            launch(Dispatchers.Main) {
                if (audioEngine.deckASong.value == null && builtInList.isNotEmpty()) {
                    audioEngine.loadSong(DeckId.DECK_A, builtInList[0])
                }
                if (audioEngine.deckBSong.value == null && builtInList.size > 1) {
                    audioEngine.loadSong(DeckId.DECK_B, builtInList[1])
                }
            }
        }
    }

    fun scanDeviceMusic() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION
                )
                val cursor = context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                    null,
                    null
                )
                cursor?.use {
                    val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                    val deviceTracks = mutableListOf<Cancion>()
                    while (it.moveToNext()) {
                        val id = it.getLong(idCol)
                        val title = it.getString(titleCol) ?: "Audio $id"
                        val artist = it.getString(artistCol) ?: "Artista Desconocido"
                        val duration = it.getLong(durCol)
                        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                        deviceTracks.add(
                            Cancion(
                                id = "device_$id",
                                titulo = title,
                                artista = artist,
                                genero = "Local",
                                duracionMs = duration.coerceAtLeast(10000L),
                                uri = contentUri.toString()
                            )
                        )
                    }
                    if (deviceTracks.isNotEmpty()) {
                        _canciones.value = _canciones.value + deviceTracks
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun setGenero(genero: String) {
        _generoSeleccionado.value = genero
    }

    fun setTtsInputText(text: String) {
        _ttsInputText.value = text
    }

    fun setTtsEmocion(emocion: EmocionVoz) {
        _ttsEmocion.value = emocion
    }

    fun speakCurrentTts() {
        val text = _ttsInputText.value
        val emotion = _ttsEmocion.value
        ttsManager.speak(text, emotion)
    }

    fun aceptarPeticion(peticion: PeticionMesa, anunciarConVoz: Boolean = true) {
        firebaseRepo.aceptarPeticion(peticion.id)

        if (anunciarConVoz) {
            val emocion = try {
                EmocionVoz.valueOf(peticion.emocion)
            } catch (_: Exception) {
                EmocionVoz.EMOCIONADO
            }
            val anuncio = if (peticion.dedicatoria.isNotBlank()) {
                "Atención ${peticion.mesa}. Complaciendo con ${peticion.cancion}. Dedicatoria: ${peticion.dedicatoria}."
            } else {
                "Petición aceptada para ${peticion.mesa}. En breve sonará ${peticion.cancion} de ${peticion.artista}."
            }
            ttsManager.speak(anuncio, emocion)
        }

        // Search matching song in library or add as requested
        val songInLibrary = _canciones.value.find {
            it.titulo.contains(peticion.cancion, ignoreCase = true)
        }
        if (songInLibrary != null) {
            // Queue into inactive deck if ready
            if (audioEngine.activeDeck.value == DeckId.DECK_A && !audioEngine.deckBIsPlaying.value) {
                audioEngine.loadSong(DeckId.DECK_B, songInLibrary)
            } else if (audioEngine.activeDeck.value == DeckId.DECK_B && !audioEngine.deckAIsPlaying.value) {
                audioEngine.loadSong(DeckId.DECK_A, songInLibrary)
            }
        }
    }

    fun rechazarPeticion(peticion: PeticionMesa) {
        firebaseRepo.rechazarPeticion(peticion.id)
    }

    fun enviarPeticionDesdeMesa(
        mesa: String,
        cancion: String,
        artista: String,
        dedicatoria: String,
        emocion: EmocionVoz
    ) {
        firebaseRepo.enviarPeticion(mesa, cancion, artista, dedicatoria, emocion)
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
        ttsManager.release()
        firebaseRepo.release()
    }
}
