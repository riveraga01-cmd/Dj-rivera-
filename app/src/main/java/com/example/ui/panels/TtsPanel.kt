package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DjConsoleState
import com.example.ui.components.DjAmber
import com.example.ui.components.DjBorder
import com.example.ui.components.DjCardDark
import com.example.ui.components.DjCyan
import com.example.ui.components.DjGreen
import com.example.ui.components.DjOrange
import com.example.ui.components.DjPanelDark
import com.example.ui.components.DjRed
import com.example.ui.components.DjTextMuted

@Composable
fun TtsPanel(
    state: DjConsoleState,
    onMessageChange: (String) -> Unit,
    onVoiceChange: (String) -> Unit,
    onRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onPreviewPfl: () -> Unit,
    onBroadcastLive: () -> Unit,
    onSaveToAds: () -> Unit,
    onApplyTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var voiceMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tts_panel"),
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
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "TTS",
                        tint = DjCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SINTETIZADOR TEXT TO SPEECH (VOZ PROFESIONAL)",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }

                if (state.ttsIsSpeaking) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DjRed)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (state.ttsPflActive) "PFL PRE-ESCUCHA ACTIVA" else "EN VIVO AL AIRE (DUCKED 15%)",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Col 1: Mensaje de Texto & Plantillas Rápidas
                Card(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DjCardDark)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "MENSAJE PARA LOCUCIÓN",
                            color = DjCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Caja de Texto [MENSAJE DE VOZ]
                        OutlinedTextField(
                            value = state.ttsMessage,
                            onValueChange = onMessageChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(82.dp)
                                .testTag("tts_message_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = DjCyan,
                                unfocusedBorderColor = DjBorder
                            ),
                            placeholder = { Text("Escribe el anuncio o dedicatoria...") }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botones de Plantillas Rápidas: [SALUDO MESA], [PROMO BEBIDAS], [AVISO CIERRE]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    onApplyTemplate("¡Un saludo especial a todas las mesas disfrutando en Rivera Hotel! DJ Rivera prendiendo la pista.")
                                },
                                modifier = Modifier.weight(1f).height(28.dp).testTag("tpl_saludo_button"),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242C3E)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "SALUDO MESA",
                                    color = DjCyan,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }

                            Button(
                                onClick = {
                                    onApplyTemplate("¡Atención! 2x1 en cócteles y tragos premium en la barra principal durante los próximos 20 minutos.")
                                },
                                modifier = Modifier.weight(1f).height(28.dp).testTag("tpl_promo_button"),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242C3E)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "PROMO BEBIDAS",
                                    color = DjAmber,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }

                            Button(
                                onClick = {
                                    onApplyTemplate("Última ronda de bebidas. En breve iniciamos el cierre del local. Gracias por acompañarnos en DJ Rivera.")
                                },
                                modifier = Modifier.weight(1f).height(28.dp).testTag("tpl_cierre_button"),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242C3E)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "AVISO CIERRE",
                                    color = DjRed,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }
                }

                // Col 2: Selector Voz, Sliders Velocidad y Tono, y Disparadores
                Card(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DjCardDark)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "PARÁMETROS DE VOZ",
                            color = DjCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Selector Desplegable [VOZ / IDIOMA]
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF151821))
                                    .border(1.dp, DjBorder, RoundedCornerShape(6.dp))
                                    .clickable { voiceMenuExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.selectedVoice,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false).basicMarquee()
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("▼", color = DjCyan, fontSize = 9.sp)
                            }

                            DropdownMenu(
                                expanded = voiceMenuExpanded,
                                onDismissRequest = { voiceMenuExpanded = false },
                                modifier = Modifier.background(DjCardDark)
                            ) {
                                state.availableVoices.forEach { voice ->
                                    DropdownMenuItem(
                                        text = { Text(voice, color = Color.White, fontSize = 11.sp) },
                                        onClick = {
                                            onVoiceChange(voice)
                                            voiceMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Slider [VELOCIDAD] (0.5x a 2.0x)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Velocidad", color = DjTextMuted, fontSize = 9.sp, maxLines = 1, modifier = Modifier.basicMarquee())
                            Text("${String.format("%.2f", state.ttsSpeechRate)}x", color = DjCyan, fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1)
                        }
                        Slider(
                            value = state.ttsSpeechRate,
                            onValueChange = onRateChange,
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = DjCyan, activeTrackColor = DjCyan),
                            modifier = Modifier.fillMaxWidth().height(22.dp)
                        )

                        // Slider [TONO] (0.5x a 2.0x)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tono (Pitch)", color = DjTextMuted, fontSize = 9.sp, maxLines = 1, modifier = Modifier.basicMarquee())
                            Text("${String.format("%.2f", state.ttsPitch)}x", color = DjAmber, fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1)
                        }
                        Slider(
                            value = state.ttsPitch,
                            onValueChange = onPitchChange,
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = DjAmber, activeTrackColor = DjAmber),
                            modifier = Modifier.fillMaxWidth().height(22.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Botones de acción: [PREVISUALIZAR PFL], [EMITIR EN VIVO], [GUARDAR EN ANUNCIOS]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Botón [PREVISUALIZAR EN AUDÍFONOS (PFL)]
                            val isPflSpeaking = state.ttsIsSpeaking && state.ttsPflActive
                            Button(
                                onClick = onPreviewPfl,
                                modifier = Modifier.weight(1f).height(30.dp).testTag("tts_pfl_button"),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPflSpeaking) DjCyan else Color(0xFF263045)
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = "PFL",
                                    tint = if (isPflSpeaking) Color.Black else DjCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isPflSpeaking) "DETENER PFL" else "PFL CUE",
                                    color = if (isPflSpeaking) Color.Black else DjCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }

                            // Botón [EMITIR EN VIVO]
                            val isLiveSpeaking = state.ttsIsSpeaking && !state.ttsPflActive
                            Button(
                                onClick = onBroadcastLive,
                                modifier = Modifier.weight(1.3f).height(30.dp).testTag("tts_live_button"),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLiveSpeaking) DjRed else DjGreen
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiveSpeaking) Icons.Default.Campaign else Icons.Default.Send,
                                    contentDescription = "En Vivo",
                                    tint = if (isLiveSpeaking) Color.White else Color.Black,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isLiveSpeaking) "DETENER EMISIÓN" else "EMITIR EN VIVO",
                                    color = if (isLiveSpeaking) Color.White else Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.5.sp,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }

                            // Botón [GUARDAR EN ANUNCIOS]
                            Button(
                                onClick = onSaveToAds,
                                modifier = Modifier.weight(1f).height(30.dp).testTag("tts_save_ad_button"),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DjAmber),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Guardar",
                                    tint = Color.Black,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "A ANUNCIO",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
