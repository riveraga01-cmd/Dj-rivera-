package com.example

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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// Neon DJ Color Palette
val NeonCyan = Color(0xFF00E5FF)
val NeonMagenta = Color(0xFFFF007F)
val NeonGreen = Color(0xFF00E676)
val NeonAmber = Color(0xFFFFD600)
val DarkBackground = Color(0xFF0B0D14)
val DarkSurface = Color(0xFF141724)
val DarkCard = Color(0xFF1A1F30)
val BorderGlow = Color(0xFF28314A)

@Composable
fun VirtualDjScreen(
    viewModel: VirtualDjViewModel,
    modifier: Modifier = Modifier
) {
    var showQrModal by remember { mutableStateOf(false) }

    val isAutoDj by viewModel.audioEngine.isAutoDj.collectAsState()
    val isMasterPlaying by viewModel.audioEngine.masterPlaying.collectAsState()
    val activeDeck by viewModel.audioEngine.activeDeck.collectAsState()
    val isDucked by viewModel.audioEngine.isDucked.collectAsState()
    val isTtsSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Global Header
            GlobalDjHeader(
                isAutoDj = isAutoDj,
                isMasterPlaying = isMasterPlaying,
                isDucked = isDucked || isTtsSpeaking,
                onAutoDjChanged = { viewModel.audioEngine.setAutoDj(it) },
                onTogglePlayPause = { viewModel.audioEngine.toggleMasterPlayPause() },
                onOpenQrModal = { showQrModal = true }
            )

            // Responsive Content: Desktop vs Mobile
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (maxWidth >= 600.dp) {
                    DesktopDjLayout(viewModel = viewModel)
                } else {
                    MobileDjLayout(viewModel = viewModel)
                }
            }
        }
    }

    if (showQrModal) {
        QrModalDialog(
            onDismissRequest = { showQrModal = false },
            onSendSampleRequest = { mesa, cancion, artista, dedicatoria, emocion ->
                viewModel.enviarPeticionDesdeMesa(mesa, cancion, artista, dedicatoria, emocion)
            }
        )
    }
}

@Composable
fun GlobalDjHeader(
    isAutoDj: Boolean,
    isMasterPlaying: Boolean,
    isDucked: Boolean,
    onAutoDjChanged: (Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onOpenQrModal: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("global_dj_header"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand & Ducking Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NeonCyan, NeonMagenta))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "DJ Logo",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "VIRTUAL DJ RIVERA",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isDucked) "AUDIO DUCKING ACTIVO (15%)" else "SISTEMA DUAL DECK ONLINE",
                        color = if (isDucked) NeonAmber else NeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Controls: Auto-DJ Switch, QR Button, Master Play/Pause
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Auto-DJ Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2336))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "AUTO-DJ",
                        color = if (isAutoDj) NeonCyan else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isAutoDj,
                        onCheckedChange = onAutoDjChanged,
                        modifier = Modifier.size(40.dp, 24.dp).testTag("auto_dj_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color(0xFF2C324B)
                        )
                    )
                }

                // QR Modal Button
                OutlinedButton(
                    onClick = onOpenQrModal,
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("open_qr_modal_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = NeonCyan
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "QR Mesas",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "QR MESAS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play / Pause Master
                Button(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("master_play_pause_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMasterPlaying) NeonMagenta else NeonGreen
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = if (isMasterPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isMasterPlaying) "Pausar" else "Reproducir",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isMasterPlaying) "PAUSA" else "PLAY MASTER",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DESKTOP LAYOUT (maxWidth >= 600.dp)
// -------------------------------------------------------------
@Composable
fun DesktopDjLayout(viewModel: VirtualDjViewModel) {
    val waveformA by viewModel.audioEngine.waveformA.collectAsState()
    val waveformB by viewModel.audioEngine.waveformB.collectAsState()
    val songA by viewModel.audioEngine.deckASong.collectAsState()
    val songB by viewModel.audioEngine.deckBSong.collectAsState()
    val posA by viewModel.audioEngine.deckAPosition.collectAsState()
    val durA by viewModel.audioEngine.deckADuration.collectAsState()
    val posB by viewModel.audioEngine.deckBPosition.collectAsState()
    val durB by viewModel.audioEngine.deckBDuration.collectAsState()
    val isPlayingA by viewModel.audioEngine.deckAIsPlaying.collectAsState()
    val isPlayingB by viewModel.audioEngine.deckBIsPlaying.collectAsState()
    val crossfaderPos by viewModel.audioEngine.crossfaderPosition.collectAsState()
    val eqLow by viewModel.audioEngine.eqLow.collectAsState()
    val eqMid by viewModel.audioEngine.eqMid.collectAsState()
    val eqHigh by viewModel.audioEngine.eqHigh.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // SUPERIOR: Canvas con Waveforms de ambos Decks
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .testTag("superior_waveforms_canvas"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlow)
        ) {
            DualWaveformCanvas(
                waveformA = waveformA,
                waveformB = waveformB,
                progressA = (posA.toFloat() / durA.coerceAtLeast(1L)).coerceIn(0f, 1f),
                progressB = (posB.toFloat() / durB.coerceAtLeast(1L)).coerceIn(0f, 1f),
                titleA = songA?.titulo ?: "DECK A - VACÍO",
                titleB = songB?.titulo ?: "DECK B - VACÍO",
                crossfaderPos = crossfaderPos
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // CENTRAL: 3 columnas (Deck A + Pads, Mixer central EQ + Crossfader, Deck B + Pads)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Columna 1: Deck A
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("desktop_deck_a"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                DeckPanel(
                    deckId = DeckId.DECK_A,
                    deckName = "DECK A",
                    accentColor = NeonCyan,
                    song = songA,
                    isPlaying = isPlayingA,
                    positionMs = posA,
                    durationMs = durA,
                    onPlay = { viewModel.audioEngine.playDeck(DeckId.DECK_A) },
                    onPause = { viewModel.audioEngine.pauseDeck(DeckId.DECK_A) },
                    onSeek = { viewModel.audioEngine.seekDeck(DeckId.DECK_A, it) },
                    onPadClick = { viewModel.audioEngine.playDjPad(it) }
                )
            }

            // Columna 2: Mixer Central con EQ 3 bandas + Crossfader
            Card(
                modifier = Modifier
                    .width(170.dp)
                    .fillMaxHeight()
                    .testTag("desktop_central_mixer"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlow)
            ) {
                CentralMixerPanel(
                    crossfaderPos = crossfaderPos,
                    eqLow = eqLow,
                    eqMid = eqMid,
                    eqHigh = eqHigh,
                    onCrossfaderChange = { viewModel.audioEngine.setCrossfader(it) },
                    onEqChange = { l, m, h -> viewModel.audioEngine.setEq(l, m, h) }
                )
            }

            // Columna 3: Deck B
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("desktop_deck_b"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.5f))
            ) {
                DeckPanel(
                    deckId = DeckId.DECK_B,
                    deckName = "DECK B",
                    accentColor = NeonMagenta,
                    song = songB,
                    isPlaying = isPlayingB,
                    positionMs = posB,
                    durationMs = durB,
                    onPlay = { viewModel.audioEngine.playDeck(DeckId.DECK_B) },
                    onPause = { viewModel.audioEngine.pauseDeck(DeckId.DECK_B) },
                    onSeek = { viewModel.audioEngine.seekDeck(DeckId.DECK_B, it) },
                    onPadClick = { viewModel.audioEngine.playDjPad(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // INFERIOR: 3 paneles en paralelo (Librería por Género, Peticiones en Vivo, Locutor TTS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Panel 1: Librería por Género
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("desktop_library_panel"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlow)
            ) {
                LibraryPanel(
                    viewModel = viewModel,
                    onLoadToDeckA = { viewModel.audioEngine.loadSong(DeckId.DECK_A, it) },
                    onLoadToDeckB = { viewModel.audioEngine.loadSong(DeckId.DECK_B, it) }
                )
            }

            // Panel 2: Peticiones por Mesa en Vivo
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("desktop_requests_panel"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlow)
            ) {
                LiveRequestsPanel(viewModel = viewModel)
            }

            // Panel 3: Locutor TTS
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("desktop_tts_panel"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlow)
            ) {
                TtsLocutorPanel(viewModel = viewModel)
            }
        }
    }
}

// -------------------------------------------------------------
// MOBILE LAYOUT (maxWidth < 600.dp)
// -------------------------------------------------------------
@Composable
fun MobileDjLayout(viewModel: VirtualDjViewModel) {
    val activeDeck by viewModel.audioEngine.activeDeck.collectAsState()
    val isPlayingA by viewModel.audioEngine.deckAIsPlaying.collectAsState()
    val isPlayingB by viewModel.audioEngine.deckBIsPlaying.collectAsState()
    val songA by viewModel.audioEngine.deckASong.collectAsState()
    val songB by viewModel.audioEngine.deckBSong.collectAsState()
    val posA by viewModel.audioEngine.deckAPosition.collectAsState()
    val durA by viewModel.audioEngine.deckADuration.collectAsState()
    val posB by viewModel.audioEngine.deckBPosition.collectAsState()
    val durB by viewModel.audioEngine.deckBDuration.collectAsState()
    val waveformA by viewModel.audioEngine.waveformA.collectAsState()
    val waveformB by viewModel.audioEngine.waveformB.collectAsState()
    val crossfaderPos by viewModel.audioEngine.crossfaderPosition.collectAsState()

    var activeMobileTab by remember { mutableStateOf(0) } // 0: Peticiones, 1: Librería, 2: TTS, 3: EQ

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Waveform Activa
        val currentSong = if (activeDeck == DeckId.DECK_A) songA else songB
        val currentWaveform = if (activeDeck == DeckId.DECK_A) waveformA else waveformB
        val currentProgress = if (activeDeck == DeckId.DECK_A) {
            (posA.toFloat() / durA.coerceAtLeast(1L)).coerceIn(0f, 1f)
        } else {
            (posB.toFloat() / durB.coerceAtLeast(1L)).coerceIn(0f, 1f)
        }
        val currentAccent = if (activeDeck == DeckId.DECK_A) NeonCyan else NeonMagenta

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .testTag("mobile_active_waveform"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, currentAccent.copy(alpha = 0.5f))
        ) {
            SingleWaveformCanvas(
                waveform = currentWaveform,
                progress = currentProgress,
                accentColor = currentAccent,
                label = "${if (activeDeck == DeckId.DECK_A) "DECK A" else "DECK B"}: ${currentSong?.titulo ?: "Sin pista"}"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Turntables & Quick Deck Player
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Deck A Compact
            DeckMiniControl(
                modifier = Modifier.weight(1f),
                deckName = "DECK A",
                accentColor = NeonCyan,
                songTitle = songA?.titulo ?: "Vacío",
                isPlaying = isPlayingA,
                onPlay = { viewModel.audioEngine.playDeck(DeckId.DECK_A) },
                onPause = { viewModel.audioEngine.pauseDeck(DeckId.DECK_A) }
            )

            // Deck B Compact
            DeckMiniControl(
                modifier = Modifier.weight(1f),
                deckName = "DECK B",
                accentColor = NeonMagenta,
                songTitle = songB?.titulo ?: "Vacío",
                isPlaying = isPlayingB,
                onPlay = { viewModel.audioEngine.playDeck(DeckId.DECK_B) },
                onPause = { viewModel.audioEngine.pauseDeck(DeckId.DECK_B) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Crossfader Horizontal Amplio
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mobile_crossfader_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlow)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DECK A (${((1f - crossfaderPos) * 100).toInt()}%)",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "CROSSFADER RIVERA",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "DECK B (${(crossfaderPos * 100).toInt()}%)",
                        color = NeonMagenta,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Slider(
                    value = crossfaderPos,
                    onValueChange = { viewModel.audioEngine.setCrossfader(it) },
                    valueRange = 0.0f..1.0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mobile_crossfader_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = NeonMagenta,
                        inactiveTrackColor = NeonCyan
                    )
                )

                // Quick Cut buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { viewModel.audioEngine.setCrossfader(0.0f) },
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "CORTE A",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { viewModel.audioEngine.setCrossfader(0.5f) },
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C324B)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "CENTRO 50/50",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { viewModel.audioEngine.setCrossfader(1.0f) },
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "CORTE B",
                            color = NeonMagenta,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Pads DJ rápidos
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlow)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "PADS DE EFECTOS DJ EN VIVO",
                    color = NeonAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                DjPadsRow(onPadClick = { viewModel.audioEngine.playDjPad(it) })
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Mobile Tab Switcher: Peticiones Mesas | Entrada TTS rápida | Librería
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf("Peticiones Mesas", "Locutor TTS", "Librería")
            tabs.forEachIndexed { idx, label ->
                val selected = activeMobileTab == idx
                Button(
                    onClick = { activeMobileTab = idx },
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) NeonCyan else Color(0xFF1E2336)
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content of selected tab
        when (activeMobileTab) {
            0 -> {
                Card(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    LiveRequestsPanel(viewModel = viewModel)
                }
            }
            1 -> {
                Card(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    TtsLocutorPanel(viewModel = viewModel)
                }
            }
            else -> {
                Card(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    LibraryPanel(
                        viewModel = viewModel,
                        onLoadToDeckA = { viewModel.audioEngine.loadSong(DeckId.DECK_A, it) },
                        onLoadToDeckB = { viewModel.audioEngine.loadSong(DeckId.DECK_B, it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// -------------------------------------------------------------
// DECK PANEL (Vinyl Turntable + Track Info + 4 Pads + CUE/PLAY)
// -------------------------------------------------------------
@Composable
fun DeckPanel(
    deckId: DeckId,
    deckName: String,
    accentColor: Color,
    song: Cancion?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onPadClick: (DjPadEffect) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Deck Title & Time readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) NeonGreen else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = deckName,
                    color = accentColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Song Title & Artist
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = song?.titulo ?: "Ninguna pista cargada",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song?.artista ?: "Elige una canción"} • ${song?.genero ?: "-"}",
                color = Color.Gray,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Vinyl Turntable
        Box(
            modifier = Modifier
                .size(105.dp)
                .rotate(if (isPlaying) rotation else 0f),
            contentAlignment = Alignment.Center
        ) {
            VinylCanvas(accentColor = accentColor)
        }

        // Progress Slider
        val currentProgress = (positionMs.toFloat() / durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
        Slider(
            value = currentProgress,
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth().height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF23283E)
            )
        )

        // 4 Pads DJ: Swoosh, Sweep, Reverb, Echo
        DjPadsRow(onPadClick = onPadClick)

        // Cue and Play / Pause Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onSeek(0.0f) },
                modifier = Modifier.weight(1f).height(36.dp).testTag("${deckName.lowercase()}_cue_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262C40)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "CUE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = { if (isPlaying) onPause() else onPlay() },
                modifier = Modifier.weight(1.5f).height(36.dp).testTag("${deckName.lowercase()}_play_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlaying) NeonMagenta else NeonGreen
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isPlaying) "PAUSA" else "PLAY",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VinylCanvas(accentColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Vinyl disc black base
        drawCircle(color = Color(0xFF101116), radius = radius, center = center)

        // Grooves
        for (i in 1..4) {
            val r = radius * (0.35f + i * 0.14f)
            drawCircle(
                color = Color(0xFF222533),
                radius = r,
                center = center,
                style = Stroke(width = 1.2f)
            )
        }

        // Center colored label
        drawCircle(color = accentColor, radius = radius * 0.32f, center = center)

        // Center spindle hole
        drawCircle(color = Color(0xFF0B0D14), radius = radius * 0.09f, center = center)

        // Outer neon border ring
        drawCircle(
            color = accentColor.copy(alpha = 0.8f),
            radius = radius - 1f,
            center = center,
            style = Stroke(width = 2.5f)
        )
    }
}

// -------------------------------------------------------------
// CENTRAL MIXER PANEL (EQ 3 bandas + Crossfader)
// -------------------------------------------------------------
@Composable
fun CentralMixerPanel(
    crossfaderPos: Float,
    eqLow: Float,
    eqMid: Float,
    eqHigh: Float,
    onCrossfaderChange: (Float) -> Unit,
    onEqChange: (Float, Float, Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "MIXER & EQ",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // EQ 3 Band Controls: HIGH, MID, LOW
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EqBandSlider(
                label = "HIGH",
                value = eqHigh,
                color = NeonMagenta,
                onValueChange = { onEqChange(eqLow, eqMid, it) }
            )
            EqBandSlider(
                label = "MID",
                value = eqMid,
                color = NeonAmber,
                onValueChange = { onEqChange(eqLow, it, eqHigh) }
            )
            EqBandSlider(
                label = "LOW",
                value = eqLow,
                color = NeonCyan,
                onValueChange = { onEqChange(it, eqMid, eqHigh) }
            )
        }

        // Reset EQ button
        Button(
            onClick = { onEqChange(0f, 0f, 0f) },
            modifier = Modifier.height(24.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2436)),
            contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
            Text(
                text = "RESET EQ",
                color = Color.LightGray,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Crossfader Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "A",
                    color = NeonCyan,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "CROSSFADER",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "B",
                    color = NeonMagenta,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Slider(
                value = crossfaderPos,
                onValueChange = onCrossfaderChange,
                valueRange = 0.0f..1.0f,
                modifier = Modifier.fillMaxWidth().height(26.dp).testTag("desktop_crossfader_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = NeonMagenta,
                    inactiveTrackColor = NeonCyan
                )
            )

            // Cut buttons: A, C, B
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "[CUT A]",
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onCrossfaderChange(0f) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "[50/50]",
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onCrossfaderChange(0.5f) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "[CUT B]",
                    color = NeonMagenta,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onCrossfaderChange(1f) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EqBandSlider(label: String, value: Float, color: Color, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${if (value > 0) "+" else ""}${value.toInt()}dB",
                color = Color.Gray,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -12f..12f,
            modifier = Modifier.fillMaxWidth().height(20.dp),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color(0xFF252A3F)
            )
        )
    }
}

// -------------------------------------------------------------
// 4 DJ PADS ROW (Swoosh, Sweep, Reverb, Echo)
// -------------------------------------------------------------
@Composable
fun DjPadsRow(onPadClick: (DjPadEffect) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val pads = listOf(
            Triple(DjPadEffect.SWOOSH, "SWOOSH", NeonCyan),
            Triple(DjPadEffect.SWEEP, "SWEEP", NeonMagenta),
            Triple(DjPadEffect.REVERB, "REVERB", NeonGreen),
            Triple(DjPadEffect.ECHO, "ECHO", NeonAmber)
        )

        pads.forEach { (effect, label, color) ->
            var isPressed by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    onPadClick(effect)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .testTag("dj_pad_${label.lowercase()}"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = color.copy(alpha = 0.22f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.8f)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = label,
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -------------------------------------------------------------
// LOWER PANELS: Library, Live Requests, TTS Announcer
// -------------------------------------------------------------
@Composable
fun LibraryPanel(
    viewModel: VirtualDjViewModel,
    onLoadToDeckA: (Cancion) -> Unit,
    onLoadToDeckB: (Cancion) -> Unit
) {
    val canciones by viewModel.canciones.collectAsState()
    val selectedGenre by viewModel.generoSeleccionado.collectAsState()

    val genres = listOf("TODOS", "Cumbia", "Salsa", "Electrónica", "Bachata", "Urbano", "Merengue", "Local")

    val filtered = if (selectedGenre == "TODOS") canciones else canciones.filter {
        it.genero.equals(selectedGenre, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = "Librería",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIBRERÍA POR GÉNERO",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "${filtered.size} pistas",
                color = Color.Gray,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal genre pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            genres.forEach { genre ->
                val isSelected = genre == selectedGenre
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) NeonCyan else Color(0xFF1E2336))
                        .clickable { viewModel.setGenero(genre) }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = genre,
                        color = if (isSelected) Color.Black else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Songs list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filtered, key = { it.id }) { cancion ->
                SongRowItem(
                    cancion = cancion,
                    onLoadA = { onLoadToDeckA(cancion) },
                    onLoadB = { onLoadToDeckB(cancion) }
                )
            }
        }
    }
}

@Composable
fun SongRowItem(
    cancion: Cancion,
    onLoadA: () -> Unit,
    onLoadB: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171A29))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cancion.titulo,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${cancion.artista} • ${cancion.genero} • ${formatTime(cancion.duracionMs)}",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onLoadA,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.25f)),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "DECK A",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onLoadB,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta.copy(alpha = 0.25f)),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "DECK B",
                        color = NeonMagenta,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// LIVE REQUESTS PANEL (Firestore /peticiones_rivera)
// -------------------------------------------------------------
@Composable
fun LiveRequestsPanel(viewModel: VirtualDjViewModel) {
    val peticiones by viewModel.peticionesPendientes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (peticiones.isNotEmpty()) NeonMagenta else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PETICIONES EN VIVO",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NeonMagenta.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${peticiones.size} PENDIENTES",
                    color = NeonMagenta,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (peticiones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF121420)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Sin peticiones",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sin peticiones de mesa pendientes",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Abre el QR para que las mesas envíen pedidos",
                        color = Color(0xFF6B7280),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(peticiones, key = { it.id }) { peticion ->
                    RequestCardItem(
                        peticion = peticion,
                        onAceptar = { viewModel.aceptarPeticion(peticion, anunciarConVoz = true) },
                        onRechazar = { viewModel.rechazarPeticion(peticion) }
                    )
                }
            }
        }
    }
}

@Composable
fun RequestCardItem(
    peticion: PeticionMesa,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1E30)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C324E))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = peticion.mesa.ifBlank { "Mesa" },
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = peticion.emocion,
                            color = NeonAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Action buttons: Rechazar & Aceptar
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onRechazar,
                        modifier = Modifier.size(28.dp).testTag("reject_request_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Rechazar",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Button(
                        onClick = onAceptar,
                        modifier = Modifier.height(28.dp).testTag("accept_request_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Aceptar",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "ACEPTAR",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${peticion.cancion} - ${peticion.artista}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (peticion.dedicatoria.isNotBlank()) {
                Text(
                    text = "“${peticion.dedicatoria}”",
                    color = Color(0xFFB0B9D0),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -------------------------------------------------------------
// LOCUTOR TTS PANEL (Text to Speech + Ducking Indicator)
// -------------------------------------------------------------
@Composable
fun TtsLocutorPanel(viewModel: VirtualDjViewModel) {
    val ttsInput by viewModel.ttsInputText.collectAsState()
    val ttsEmocion by viewModel.ttsEmocion.collectAsState()
    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Locutor",
                        tint = NeonAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LOCUTOR TTS EMOCIONAL",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isSpeaking) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF1744))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AL AIRE (15%)",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Emotion selector buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EmocionVoz.values().forEach { emo ->
                    val isSelected = emo == ttsEmocion
                    val emoColor = when (emo) {
                        EmocionVoz.ROMANTICO -> Color(0xFFFF4081)
                        EmocionVoz.FELIZ -> NeonAmber
                        EmocionVoz.EMOCIONADO -> NeonMagenta
                        EmocionVoz.NEUTRO -> NeonCyan
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) emoColor else Color(0xFF1E2336))
                            .clickable { viewModel.setTtsEmocion(emo) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emo.name.take(4),
                            color = if (isSelected) Color.Black else Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = ttsInput,
                onValueChange = { viewModel.setTtsInputText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("tts_text_field"),
                placeholder = { Text("Escribe mensaje para el público...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonAmber,
                    unfocusedBorderColor = BorderGlow
                )
            )
        }

        // Action: Speak / Stop
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { viewModel.speakCurrentTts() },
                modifier = Modifier.weight(1.4f).height(36.dp).testTag("speak_tts_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Hablar",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "HABLAR (CON DUCKING)",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSpeaking) {
                Button(
                    onClick = { viewModel.ttsManager.stop() },
                    modifier = Modifier.weight(0.6f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "CALLAR",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// WAVEFORM CANVASES
// -------------------------------------------------------------
@Composable
fun DualWaveformCanvas(
    waveformA: List<Float>,
    waveformB: List<Float>,
    progressA: Float,
    progressB: Float,
    titleA: String,
    titleB: String,
    crossfaderPos: Float
) {
    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
        val w = size.width
        val h = size.height
        val halfW = w / 2

        // Deck A Half (Left)
        drawWaveformBars(
            waveform = waveformA,
            progress = progressA,
            startX = 0f,
            totalW = halfW - 8f,
            height = h,
            barColor = NeonCyan,
            progressHeadColor = Color.White
        )

        // Center line
        drawLine(
            color = BorderGlow,
            start = Offset(halfW, 0f),
            end = Offset(halfW, h),
            strokeWidth = 2f
        )

        // Deck B Half (Right)
        drawWaveformBars(
            waveform = waveformB,
            progress = progressB,
            startX = halfW + 8f,
            totalW = halfW - 8f,
            height = h,
            barColor = NeonMagenta,
            progressHeadColor = Color.White
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveformBars(
    waveform: List<Float>,
    progress: Float,
    startX: Float,
    totalW: Float,
    height: Float,
    barColor: Color,
    progressHeadColor: Color
) {
    val barCount = waveform.size
    val barWidth = totalW / (barCount * 1.5f)
    val centerY = height / 2

    waveform.forEachIndexed { i, amp ->
        val x = startX + (i * barWidth * 1.5f)
        val barH = (amp * (height * 0.75f)).coerceAtLeast(4f)
        val isPlayed = (x - startX) <= (progress * totalW)
        val color = if (isPlayed) barColor else barColor.copy(alpha = 0.3f)

        drawRoundRect(
            color = color,
            topLeft = Offset(x, centerY - barH / 2),
            size = Size(barWidth, barH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
        )
    }

    // Playhead indicator line
    val playheadX = startX + (progress * totalW)
    drawLine(
        color = progressHeadColor,
        start = Offset(playheadX, 0f),
        end = Offset(playheadX, height),
        strokeWidth = 2f
    )
}

@Composable
fun SingleWaveformCanvas(
    waveform: List<Float>,
    progress: Float,
    accentColor: Color,
    label: String
) {
    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
        val w = size.width
        val h = size.height

        drawWaveformBars(
            waveform = waveform,
            progress = progress,
            startX = 0f,
            totalW = w,
            height = h,
            barColor = accentColor,
            progressHeadColor = Color.White
        )
    }
}

@Composable
fun DeckMiniControl(
    modifier: Modifier = Modifier,
    deckName: String,
    accentColor: Color,
    songTitle: String,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deckName,
                    color = accentColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = songTitle,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { if (isPlaying) onPause() else onPlay() },
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) NeonMagenta else NeonGreen)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausa" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
}
