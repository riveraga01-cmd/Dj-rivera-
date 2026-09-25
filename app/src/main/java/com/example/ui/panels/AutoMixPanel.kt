package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DjConsoleState
import com.example.TransitionType
import com.example.ui.components.DjAmber
import com.example.ui.components.DjBorder
import com.example.ui.components.DjCardDark
import com.example.ui.components.DjCyan
import com.example.ui.components.DjGreen
import com.example.ui.components.DjOrange
import com.example.ui.components.DjPanelDark
import com.example.ui.components.DjRed
import com.example.ui.components.DjTextMuted
import com.example.ui.components.StatusSwitch

@Composable
fun AutoMixPanel(
    state: DjConsoleState,
    onToggleAutoMix: (Boolean) -> Unit,
    onTransitionTypeChange: (TransitionType) -> Unit,
    onDurationChange: (Float) -> Unit,
    onToggleHarmonicKeyLock: (Boolean) -> Unit,
    onToggleInsertAds: (Boolean) -> Unit,
    onToggleInsertSoundFx: (Boolean) -> Unit,
    onSkipTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("automix_panel"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DjPanelDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, DjBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AutoMix",
                        tint = DjGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DJ VIRTUAL AUTOMÁTICO (AUTOMIX INTELIGENTE)",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Botón [INICIAR / DETENER AUTOMIX]
                Button(
                    onClick = { onToggleAutoMix(!state.autoMixActive) },
                    modifier = Modifier.height(32.dp).testTag("start_stop_automix_button"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.autoMixActive) DjRed else DjGreen
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = if (state.autoMixActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Stop",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.autoMixActive) "DETENER AUTOMIX" else "INICIAR AUTOMIX",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Column Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Col 1: Tipo de Transición & Duración
                Card(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DjCardDark)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "TIPO DE TRANSICIÓN",
                            color = DjCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Selector [TIPO DE TRANSICIÓN]
                        TransitionType.values().forEach { trans ->
                            val isSelected = trans == state.autoMixTransitionType
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) DjCyan.copy(alpha = 0.2f) else Color(0xFF161922))
                                    .border(
                                        1.dp,
                                        if (isSelected) DjCyan else Color(0xFF262D3C),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onTransitionTypeChange(trans) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = trans.label,
                                    color = if (isSelected) Color.White else DjTextMuted,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(DjCyan)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Control de Tiempo [DURACIÓN DE MEZCLA] (2s a 16s)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Duración Mezcla",
                                color = DjTextMuted,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${state.autoMixDurationSec.toInt()} seg",
                                color = DjGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Slider(
                            value = state.autoMixDurationSec,
                            onValueChange = onDurationChange,
                            valueRange = 2f..16f,
                            colors = SliderDefaults.colors(thumbColor = DjGreen, activeTrackColor = DjGreen),
                            modifier = Modifier.fillMaxWidth().height(22.dp)
                        )
                    }
                }

                // Col 2: Switches Inteligentes: Key Lock, Anuncios, Sound FX
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DjCardDark)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "REGLAS DE MEZCLA INTELIGENTE",
                            color = DjCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Switch [MEZCLA ARMÓNICA - KEY LOCK]
                        StatusSwitch(
                            checked = state.autoMixHarmonicKeyLock,
                            onCheckedChange = onToggleHarmonicKeyLock,
                            label = "KEY LOCK ARMÓNICO",
                            activeColor = DjCyan,
                            inactiveColor = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "key_lock_switch"
                        )

                        // Switch [INTERCALAR ANUNCIOS]
                        StatusSwitch(
                            checked = state.autoMixInsertAds,
                            onCheckedChange = onToggleInsertAds,
                            label = "INTERCALAR ANUNCIOS",
                            activeColor = DjAmber,
                            inactiveColor = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "insert_ads_switch"
                        )

                        // Switch [INTERCALAR SOUND FX]
                        StatusSwitch(
                            checked = state.autoMixInsertSoundFx,
                            onCheckedChange = onToggleInsertSoundFx,
                            label = "INTERCALAR SOUND FX",
                            activeColor = DjOrange,
                            inactiveColor = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "insert_sound_fx_switch"
                        )
                    }
                }

                // Col 3: Pantalla [PRÓXIMA CANCIÓN] & Botón [SALTAR CANCIÓN (SKIP)]
                Card(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DjCardDark)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PRÓXIMA CANCIÓN EN COLA",
                                    color = DjCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "EN ${state.autoMixCountdownSec}s",
                                    color = if (state.autoMixCountdownSec <= 10) DjRed else DjGreen,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Tarjeta Próxima pista
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF131620))
                                    .border(1.dp, DjBorder, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = state.nextSong?.titulo ?: "Cumbia Rivera (En cola)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${state.nextSong?.artista ?: "Orquesta Rivera"} • ${state.nextSong?.genero ?: "Cumbia"}",
                                        color = DjTextMuted,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "BPM: ${state.nextSongBpm}",
                                            color = DjCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "KEY: ${state.nextSongKey}",
                                            color = DjAmber,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "SYNC: LISTO",
                                            color = DjGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botón [SALTAR CANCIÓN (SKIP)]
                        Button(
                            onClick = onSkipTrack,
                            modifier = Modifier.fillMaxWidth().height(32.dp).testTag("skip_track_button"),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DjOrange),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Saltar",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SALTAR CANCIÓN (FORZAR MEZCLA YA)",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.5.sp,
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
