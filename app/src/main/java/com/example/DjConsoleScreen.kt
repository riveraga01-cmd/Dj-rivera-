package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DjAmber
import com.example.ui.components.DjBgDark
import com.example.ui.components.DjBorder
import com.example.ui.components.DjCardDark
import com.example.ui.components.DjCyan
import com.example.ui.components.DjGreen
import com.example.ui.components.DjOrange
import com.example.ui.components.DjPanelDark
import com.example.ui.components.DjRed
import com.example.ui.components.DjTextMuted
import com.example.ui.components.RotaryKnob
import com.example.ui.components.VerticalFader
import com.example.ui.components.VuMeterLed
import com.example.ui.panels.AdsPanel
import com.example.ui.panels.AutoMixPanel
import com.example.ui.panels.MicFxPanel
import com.example.ui.panels.QrRequestsPanel
import com.example.ui.panels.SamplerPanel
import com.example.ui.panels.TtsPanel

@Composable
fun DjConsoleScreen(
    viewModel: DjConsoleViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("dj_console_screen"),
        color = DjBgDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ==================================================================
            // 1. BARRA SUPERIOR FIJA CON CONTROLES MÁSTER
            // ==================================================================
            MasterHeaderBar(
                state = state,
                onTogglePlayPause = { viewModel.toggleMasterPlayPause() },
                onVolumeChange = { viewModel.setMasterVolume(it) },
                onToggleRecord = { viewModel.toggleRecording() }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ==================================================================
            // 2. MÓDULO CENTRAL PERSISTENTE: DECK A, MIXER CENTRAL, DECK B
            // ==================================================================
            PersistentDecksAndMixer(
                state = state,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ==================================================================
            // 3. SELECTOR DE PESTAÑAS HORIZONTALES PARA EL RACK INTERMEDIO
            // ==================================================================
            RackTabBar(
                activeTab = state.activeRackTab,
                isExpanded = state.isRackExpanded,
                onSelectTab = { viewModel.selectRackTab(it) },
                onToggleExpand = { viewModel.toggleRackExpanded() }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ==================================================================
            // 4. ÁREA DESPLEGABLE DEL RACK (AnimatedVisibility)
            // ==================================================================
            AnimatedVisibility(
                visible = state.isRackExpanded && state.activeRackTab != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    when (state.activeRackTab) {
                        RackTab.ANUNCIOS -> {
                            AdsPanel(
                                state = state,
                                onToggleAdsActive = { viewModel.setAdsActive(it) },
                                onIntervalModeChange = { viewModel.setAdsIntervalMode(it) },
                                onIntervalValueChange = { viewModel.setAdsIntervalValue(it) },
                                onSelectAnuncio = { viewModel.selectAnuncio(it) },
                                onToggleAnuncioActivo = { viewModel.toggleAnuncioActivo(it) },
                                onPlayAdNow = { viewModel.playAdNow() },
                                onAddAd = { viewModel.addNewAd() },
                                onDuckingLevelChange = { viewModel.setAdsDuckingLevel(it) },
                                onClosingTimeChange = { viewModel.setClosingTime(it) },
                                onClosingFarewellChange = { viewModel.setClosingFarewell(it) },
                                onClosingBlockQrChange = { viewModel.setClosingBlockQr(it) },
                                onSaveAd = { id, nombre, duracion, frecuencia, textoLocucion, tipoLector ->
                                    viewModel.saveOrUpdateAd(id, nombre, duracion, frecuencia, true, textoLocucion, tipoLector)
                                },
                                onDeleteAd = { viewModel.deleteAd(it) }
                            )
                        }
                        RackTab.PETICIONES_QR -> {
                            QrRequestsPanel(
                                state = state,
                                onShowQrDialog = { viewModel.setShowQrDialog(true) },
                                onToggleAcceptRequests = { viewModel.setQrAcceptRequests(it) },
                                onLimitPerUserChange = { viewModel.setQrLimitPerUser(it) },
                                onToggleSmartAutoApprove = { viewModel.setQrSmartAutoApprove(it) },
                                onApproveToAutoMix = { viewModel.approveToAutoMix(it) },
                                onLoadToDeck = { req, deck -> viewModel.loadRequestToDeck(req, deck) },
                                onRejectRequest = { viewModel.rejectRequest(it) }
                            )
                        }
                        RackTab.MIC_FX -> {
                            MicFxPanel(
                                state = state,
                                onToggleMic = { viewModel.setMicOn(it) },
                                onToggleTalkOver = { viewModel.setTalkOver(it) },
                                onMicGainChange = { viewModel.setMicGain(it) },
                                onSelectEffect = { viewModel.setMicEffect(it) },
                                onPitchChange = { viewModel.setPitchShiftValue(it) },
                                onReverbChange = { viewModel.setReverbIntensity(it) },
                                onEchoFeedbackChange = { viewModel.setEchoFeedback(it) },
                                onEchoBpmSyncChange = { viewModel.setEchoBpmSync(it) },
                                onDryWetChange = { viewModel.setDryWetMix(it) }
                            )
                        }
                        RackTab.TTS -> {
                            TtsPanel(
                                state = state,
                                onMessageChange = { viewModel.setTtsMessage(it) },
                                onVoiceChange = { viewModel.setTtsVoice(it) },
                                onRateChange = { viewModel.setTtsRate(it) },
                                onPitchChange = { viewModel.setTtsPitch(it) },
                                onPreviewPfl = { viewModel.previewTtsPfl() },
                                onBroadcastLive = { viewModel.broadcastTtsLive() },
                                onSaveToAds = { viewModel.saveTtsToAds() },
                                onApplyTemplate = { viewModel.setTtsMessage(it) }
                            )
                        }
                        RackTab.AUTOMIX -> {
                            AutoMixPanel(
                                state = state,
                                onToggleAutoMix = { viewModel.setAutoMixActive(it) },
                                onTransitionTypeChange = { viewModel.setAutoMixTransitionType(it) },
                                onDurationChange = { viewModel.setAutoMixDuration(it) },
                                onToggleHarmonicKeyLock = { viewModel.setHarmonicKeyLock(it) },
                                onToggleInsertAds = { viewModel.setAutoMixInsertAds(it) },
                                onToggleInsertSoundFx = { viewModel.setAutoMixInsertSoundFx(it) },
                                onSkipTrack = { viewModel.skipAutoMixTrack() }
                            )
                        }
                        RackTab.SAMPLER -> {
                            SamplerPanel(
                                state = state,
                                onTriggerPad = { viewModel.triggerSamplerPad(it) },
                                onAssignCustomSample = { viewModel.assignCustomSample(it) },
                                onVolumeChange = { viewModel.setSamplerVolume(it) },
                                onTriggerModeChange = { viewModel.setSamplerTriggerMode(it) },
                                onToggleDucking = { viewModel.setSamplerDucking(it) }
                            )
                        }
                        null -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ==================================================================
            // 5. NAVEGADOR / BIBLIOTECA DE ARCHIVOS MP3 (SECCIÓN INFERIOR)
            // ==================================================================
            LowerLibrarySection(
                state = state,
                onSearchChange = { viewModel.setLibrarySearch(it) },
                onSelectGenre = { viewModel.setLibraryGenre(it) },
                onLoadToDeckA = { viewModel.loadSongToDeck(it, DeckId.DECK_A) },
                onLoadToDeckB = { viewModel.loadSongToDeck(it, DeckId.DECK_B) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Modal QR Dialog if requested
    if (state.showQrDialog) {
        QrModalDialog(
            onDismissRequest = { viewModel.setShowQrDialog(false) },
            onSendSampleRequest = { mesa, cancion, artista, dedicatoria, emocion ->
                viewModel.firebaseRepo.enviarPeticion(mesa, cancion, artista, dedicatoria, emocion)
            }
        )
    }
}

// ------------------------------------------------------------------
// 1. MASTER HEADER BAR
// ------------------------------------------------------------------
@Composable
fun MasterHeaderBar(
    state: DjConsoleState,
    onTogglePlayPause: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleRecord: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("master_header_bar"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DjPanelDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, DjBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand & Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(DjCyan, DjOrange))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "DJ Console",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "VIRTUAL DJ RIVERA PRO CONSOLE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "MASTER BPM: ${state.masterBpm}",
                            color = DjCyan,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "CIERRE: ${state.closingTimeText}",
                            color = DjRed,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.activeDucking) {
                            Text(
                                text = "AUDIO DUCKING ACTIVO",
                                color = DjAmber,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Master VU Meter, Volume Knob & Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dual Master VU meters (L & R)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VuMeterLed(level = state.masterVuLeft, height = 38.dp, width = 8.dp)
                    VuMeterLed(level = state.masterVuRight, height = 38.dp, width = 8.dp)
                }

                // Master Volume Knob
                RotaryKnob(
                    value = state.masterVolume,
                    onValueChange = onVolumeChange,
                    range = 0f..1f,
                    label = "MASTER",
                    displayValue = "${(state.masterVolume * 100).toInt()}%",
                    accentColor = DjCyan,
                    size = 40.dp
                )

                // Recording Button
                Button(
                    onClick = onToggleRecord,
                    modifier = Modifier.height(34.dp).testTag("rec_button"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isRecording) DjRed else Color(0xFF202533)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = "REC",
                        tint = if (state.isRecording) Color.White else DjRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (state.isRecording) "REC EN VIVO" else "REC",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Master Play / Pause
                Button(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.height(34.dp).testTag("master_play_button"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.masterPlaying) DjOrange else DjGreen
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = if (state.masterPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.masterPlaying) "PAUSA" else "PLAY MASTER",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 2. PERSISTENT DECKS & CENTRAL MIXER
// ------------------------------------------------------------------
@Composable
fun PersistentDecksAndMixer(
    state: DjConsoleState,
    viewModel: DjConsoleViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("persistent_decks_mixer"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DjPanelDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, DjBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Deck A (Plato Izquierdo)
            DeckHardwareView(
                modifier = Modifier.weight(1.2f),
                deck = state.deckA,
                accentColor = DjCyan,
                onPlay = { viewModel.playDeck(DeckId.DECK_A) },
                onPause = { viewModel.pauseDeck(DeckId.DECK_A) },
                onSeek = { viewModel.seekDeck(DeckId.DECK_A, it) },
                onCue = { viewModel.toggleDeckCue(DeckId.DECK_A) }
            )

            // Central Mixer (EQ Knobs, Faders, Crossfader)
            CentralMixerHardwareView(
                modifier = Modifier.weight(1.5f),
                state = state,
                viewModel = viewModel
            )

            // Deck B (Plato Derecho)
            DeckHardwareView(
                modifier = Modifier.weight(1.2f),
                deck = state.deckB,
                accentColor = DjOrange,
                onPlay = { viewModel.playDeck(DeckId.DECK_B) },
                onPause = { viewModel.pauseDeck(DeckId.DECK_B) },
                onSeek = { viewModel.seekDeck(DeckId.DECK_B, it) },
                onCue = { viewModel.toggleDeckCue(DeckId.DECK_B) }
            )
        }
    }
}

@Composable
fun DeckHardwareView(
    deck: DeckState,
    accentColor: Color,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onCue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DjCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Deck Label & BPM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (deck.deckId == DeckId.DECK_A) "DECK A" else "DECK B",
                    color = accentColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${deck.bpm} BPM • ${deck.musicalKey}",
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Track Title
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = deck.song?.titulo ?: "Sin pista cargada",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = deck.song?.artista ?: "Arrastra desde la librería",
                    color = DjTextMuted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Vinyl Turntable
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .rotate(if (deck.isPlaying) rotation else 0f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(Color(0xFF0F1116), radius = radius, center = center)
                    for (i in 1..3) {
                        drawCircle(
                            Color(0xFF25293A),
                            radius = radius * (0.35f + i * 0.18f),
                            center = center,
                            style = Stroke(width = 1.2f)
                        )
                    }
                    drawCircle(accentColor, radius = radius * 0.32f, center = center)
                    drawCircle(Color(0xFF0A0B0E), radius = radius * 0.08f, center = center)
                    drawCircle(accentColor.copy(alpha = 0.8f), radius = radius - 1f, center = center, style = Stroke(width = 2f))
                }
            }

            // Time & Progress Slider
            val progress = (deck.positionMs.toFloat() / deck.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(deck.positionMs), color = Color.White, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatTime(deck.durationMs), color = DjTextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Slider(
                value = progress,
                onValueChange = onSeek,
                modifier = Modifier.fillMaxWidth().height(16.dp),
                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
            )

            // CUE and PLAY/PAUSE Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onCue,
                    modifier = Modifier.weight(1f).height(30.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (deck.cueActive) DjAmber else Color(0xFF222838)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "CUE",
                        color = if (deck.cueActive) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = { if (deck.isPlaying) onPause() else onPlay() },
                    modifier = Modifier.weight(1.3f).height(30.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (deck.isPlaying) DjOrange else DjGreen
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (deck.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (deck.isPlaying) "PAUSA" else "PLAY",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CentralMixerHardwareView(
    state: DjConsoleState,
    viewModel: DjConsoleViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DjCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, DjBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CONSOLA MEZCLADORA CENTRAL",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // EQ 3-Band Knobs for Channel A and Channel B
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Channel A EQ
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CH-A", color = DjCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    RotaryKnob(
                        value = state.deckA.eqHigh,
                        onValueChange = { viewModel.setDeckEq(DeckId.DECK_A, state.deckA.eqLow, state.deckA.eqMid, it) },
                        range = -12f..12f,
                        label = "HI",
                        displayValue = "${state.deckA.eqHigh.toInt()}dB",
                        accentColor = DjCyan,
                        size = 36.dp,
                        bipolar = true
                    )
                    RotaryKnob(
                        value = state.deckA.eqMid,
                        onValueChange = { viewModel.setDeckEq(DeckId.DECK_A, state.deckA.eqLow, it, state.deckA.eqHigh) },
                        range = -12f..12f,
                        label = "MID",
                        displayValue = "${state.deckA.eqMid.toInt()}dB",
                        accentColor = DjCyan,
                        size = 36.dp,
                        bipolar = true
                    )
                    RotaryKnob(
                        value = state.deckA.eqLow,
                        onValueChange = { viewModel.setDeckEq(DeckId.DECK_A, it, state.deckA.eqMid, state.deckA.eqHigh) },
                        range = -12f..12f,
                        label = "LOW",
                        displayValue = "${state.deckA.eqLow.toInt()}dB",
                        accentColor = DjCyan,
                        size = 36.dp,
                        bipolar = true
                    )
                }

                // Vertical Faders Channel A & Channel B with VU meters
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VerticalFader(
                        value = state.deckA.faderVolume,
                        onValueChange = { viewModel.setDeckFader(DeckId.DECK_A, it) },
                        label = "FADER A",
                        accentColor = DjCyan,
                        height = 110.dp
                    )
                    VuMeterLed(level = state.deckA.vuLevel, height = 110.dp, width = 7.dp)
                    VuMeterLed(level = state.deckB.vuLevel, height = 110.dp, width = 7.dp)
                    VerticalFader(
                        value = state.deckB.faderVolume,
                        onValueChange = { viewModel.setDeckFader(DeckId.DECK_B, it) },
                        label = "FADER B",
                        accentColor = DjOrange,
                        height = 110.dp
                    )
                }

                // Channel B EQ
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CH-B", color = DjOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    RotaryKnob(
                        value = state.deckB.eqHigh,
                        onValueChange = { viewModel.setDeckEq(DeckId.DECK_B, state.deckB.eqLow, state.deckB.eqMid, it) },
                        range = -12f..12f,
                        label = "HI",
                        displayValue = "${state.deckB.eqHigh.toInt()}dB",
                        accentColor = DjOrange,
                        size = 36.dp,
                        bipolar = true
                    )
                    RotaryKnob(
                        value = state.deckB.eqMid,
                        onValueChange = { viewModel.setDeckEq(DeckId.DECK_B, state.deckB.eqLow, it, state.deckB.eqHigh) },
                        range = -12f..12f,
                        label = "MID",
                        displayValue = "${state.deckB.eqMid.toInt()}dB",
                        accentColor = DjOrange,
                        size = 36.dp,
                        bipolar = true
                    )
                    RotaryKnob(
                        value = state.deckB.eqLow,
                        onValueChange = { viewModel.setDeckEq(DeckId.DECK_B, it, state.deckB.eqMid, state.deckB.eqHigh) },
                        range = -12f..12f,
                        label = "LOW",
                        displayValue = "${state.deckB.eqLow.toInt()}dB",
                        accentColor = DjOrange,
                        size = 36.dp,
                        bipolar = true
                    )
                }
            }

            // Crossfader Section
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("A (${((1f - state.crossfaderPosition) * 100).toInt()}%)", color = DjCyan, fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("CROSSFADER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("B (${(state.crossfaderPosition * 100).toInt()}%)", color = DjOrange, fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Slider(
                    value = state.crossfaderPosition,
                    onValueChange = { viewModel.setCrossfader(it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = DjOrange,
                        inactiveTrackColor = DjCyan
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "[CUT A]",
                        color = DjCyan,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.setCrossfader(0f) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "[50/50]",
                        color = Color.LightGray,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.setCrossfader(0.5f) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "[CUT B]",
                        color = DjOrange,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.setCrossfader(1f) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// 3. RACK TAB BAR
// ------------------------------------------------------------------
@Composable
fun RackTabBar(
    activeTab: RackTab?,
    isExpanded: Boolean,
    onSelectTab: (RackTab) -> Unit,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rack_tab_bar"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DjPanelDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, DjBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RackTab.values().forEach { tab ->
                    val isSelected = tab == activeTab && isExpanded
                    val tabColor = when (tab) {
                        RackTab.ANUNCIOS -> DjAmber
                        RackTab.PETICIONES_QR -> DjCyan
                        RackTab.MIC_FX -> DjGreen
                        RackTab.TTS -> DjOrange
                        RackTab.AUTOMIX -> DjGreen
                        RackTab.SAMPLER -> DjRed
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) tabColor else Color(0xFF151821))
                            .border(
                                1.dp,
                                if (isSelected) tabColor else Color(0xFF282F40),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelectTab(tab) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("rack_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Expand/Collapse Toggle Button
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(30.dp).testTag("rack_toggle_expand_button")
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand/Collapse",
                    tint = DjCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// 5. LOWER MUSIC LIBRARY SECTION
// ------------------------------------------------------------------
@Composable
fun LowerLibrarySection(
    state: DjConsoleState,
    onSearchChange: (String) -> Unit,
    onSelectGenre: (String) -> Unit,
    onLoadToDeckA: (Cancion) -> Unit,
    onLoadToDeckB: (Cancion) -> Unit
) {
    val genres = listOf("TODOS", "Cumbia", "Salsa", "Electrónica", "Bachata", "Urbano", "Merengue", "Local")

    val filteredSongs = state.librarySongs.filter { song ->
        val matchesGenre = state.librarySelectedGenre == "TODOS" || song.genero.equals(state.librarySelectedGenre, ignoreCase = true)
        val matchesSearch = state.librarySearchQuery.isBlank() ||
                song.titulo.contains(state.librarySearchQuery, ignoreCase = true) ||
                song.artista.contains(state.librarySearchQuery, ignoreCase = true)
        matchesGenre && matchesSearch
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lower_library_section"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DjPanelDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, DjBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header & Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = "Biblioteca",
                        tint = DjCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EXPLORADOR DE BIBLIOTECA MUSICAL Y MP3",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Search field
                OutlinedTextField(
                    value = state.librarySearchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Buscar canción o artista...", fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = DjTextMuted, modifier = Modifier.size(14.dp))
                    },
                    modifier = Modifier.width(220.dp).height(42.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = DjCyan,
                        unfocusedBorderColor = DjBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Genre filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                genres.forEach { g ->
                    val isSel = g == state.librarySelectedGenre
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSel) DjCyan else Color(0xFF161922))
                            .border(1.dp, if (isSel) DjCyan else Color(0xFF262D3E), RoundedCornerShape(4.dp))
                            .clickable { onSelectGenre(g) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = g,
                            color = if (isSel) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Song list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(filteredSongs, key = { it.id }) { cancion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF141722))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cancion.titulo,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${cancion.artista} • ${cancion.genero} • ${formatTime(cancion.duracionMs)}",
                                color = DjTextMuted,
                                fontSize = 8.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { onLoadToDeckA(cancion) },
                                modifier = Modifier.height(24.dp),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DjCyan.copy(alpha = 0.3f)),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = "A DECK A",
                                    color = DjCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = { onLoadToDeckB(cancion) },
                                modifier = Modifier.height(24.dp),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DjOrange.copy(alpha = 0.3f)),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = "A DECK B",
                                    color = DjOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
