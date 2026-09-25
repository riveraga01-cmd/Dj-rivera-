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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.MicEffect
import com.example.ui.components.DjAmber
import com.example.ui.components.DjBorder
import com.example.ui.components.DjCardDark
import com.example.ui.components.DjCyan
import com.example.ui.components.DjGreen
import com.example.ui.components.DjOrange
import com.example.ui.components.DjPanelDark
import com.example.ui.components.DjRed
import com.example.ui.components.DjTextMuted
import com.example.ui.components.RotaryKnob
import com.example.ui.components.StatusSwitch
import com.example.ui.components.VuMeterLed

@Composable
fun MicFxPanel(
    state: DjConsoleState,
    onToggleMic: (Boolean) -> Unit,
    onToggleTalkOver: (Boolean) -> Unit,
    onMicGainChange: (Float) -> Unit,
    onSelectEffect: (MicEffect) -> Unit,
    onPitchChange: (Float) -> Unit,
    onReverbChange: (Float) -> Unit,
    onEchoFeedbackChange: (Float) -> Unit,
    onEchoBpmSyncChange: (String) -> Unit,
    onDryWetChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mic_fx_panel"),
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
                        imageVector = if (state.micOn) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Mic",
                        tint = if (state.micOn) DjGreen else DjRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MODULADOR DE VOZ & CONTROL DE MICRÓFONO",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Global Status
                Text(
                    text = if (state.talkOverActive) "TALK OVER ACTIVO (DUCKING 20%)" else if (state.micOn) "MIC EN LÍNEA" else "MICRÓFONO MUTED",
                    color = if (state.talkOverActive) DjOrange else if (state.micOn) DjGreen else DjTextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section 1: Mic On/Off, Talk Over & Gain + VU
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DjCardDark)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ENTRADA & CONTROL PRINCIPAL",
                            color = DjCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botón Interruptor [MIC ON / OFF]
                        StatusSwitch(
                            checked = state.micOn,
                            onCheckedChange = onToggleMic,
                            label = if (state.micOn) "MIC ON (EN VIVO)" else "MIC OFF (MUTED)",
                            activeColor = DjGreen,
                            inactiveColor = DjRed,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "mic_on_off_switch"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Botón [TALK OVER] (con audio ducking al 20%)
                        Button(
                            onClick = { onToggleTalkOver(!state.talkOverActive) },
                            modifier = Modifier.fillMaxWidth().height(34.dp).testTag("talk_over_button"),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.talkOverActive) DjOrange else Color(0xFF262C3E)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (state.talkOverActive) DjOrange else Color(0xFF38435C)
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Talk Over",
                                tint = if (state.talkOverActive) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.talkOverActive) "TALK OVER ACTIVO" else "ACTIVAR TALK OVER",
                                color = if (state.talkOverActive) Color.Black else Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Perilla [GANANCIA MIC] con Vúmetro LED
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            RotaryKnob(
                                value = state.micGain,
                                onValueChange = onMicGainChange,
                                range = 0f..1f,
                                label = "GANANCIA",
                                displayValue = "${(state.micGain * 100).toInt()}%",
                                accentColor = DjCyan,
                                size = 50.dp,
                                testTag = "mic_gain_knob"
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "VÚMETRO",
                                    color = DjTextMuted,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                VuMeterLed(
                                    level = if (state.micOn || state.talkOverActive) state.micVuLevel else 0f,
                                    height = 64.dp,
                                    width = 12.dp,
                                    testTag = "mic_vu_meter"
                                )
                            }
                        }
                    }
                }

                // Section 2: Selector de Efectos de Voz
                Card(
                    modifier = Modifier
                        .weight(1.8f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DjCardDark)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "PROCESADOR DIGITAL DE VOZ (DSP)",
                            color = DjCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Chips / Botones de efecto
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MicEffect.values().forEach { fx ->
                                val isSelected = fx == state.selectedMicFx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) DjCyan else Color(0xFF171A24))
                                        .border(
                                            1.dp,
                                            if (isSelected) DjCyan else Color(0xFF282F42),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onSelectEffect(fx) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = fx.label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Parámetros específicos según efecto seleccionado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Knob 1: Depende del efecto
                            when (state.selectedMicFx) {
                                MicEffect.PITCH_SHIFT -> {
                                    RotaryKnob(
                                        value = state.pitchShiftValue,
                                        onValueChange = onPitchChange,
                                        range = -12f..12f,
                                        label = "PITCH (TONO)",
                                        displayValue = "${if (state.pitchShiftValue > 0) "+" else ""}${state.pitchShiftValue.toInt()} semi",
                                        accentColor = DjOrange,
                                        bipolar = true,
                                        size = 46.dp
                                    )
                                }
                                MicEffect.REVERB -> {
                                    RotaryKnob(
                                        value = state.reverbIntensity,
                                        onValueChange = onReverbChange,
                                        range = 0f..1f,
                                        label = "REVERB TIME",
                                        displayValue = "${(state.reverbIntensity * 100).toInt()}%",
                                        accentColor = DjAmber,
                                        size = 46.dp
                                    )
                                }
                                MicEffect.ECHO_DELAY -> {
                                    RotaryKnob(
                                        value = state.echoFeedback,
                                        onValueChange = onEchoFeedbackChange,
                                        range = 0f..0.95f,
                                        label = "FEEDBACK",
                                        displayValue = "${(state.echoFeedback * 100).toInt()}%",
                                        accentColor = DjCyan,
                                        size = 46.dp
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "BPM SYNC",
                                            color = DjTextMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            listOf("1/4", "1/2", "3/4", "1/1").forEach { sync ->
                                                val isSync = sync == state.echoBpmSync
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(if (isSync) DjCyan else Color(0xFF161922))
                                                        .clickable { onEchoBpmSyncChange(sync) }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = sync,
                                                        color = if (isSync) Color.Black else Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                MicEffect.ROBOT -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("MODULACIÓN METÁLICA", color = DjCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Frec: 140 Hz Ring Mod", color = DjTextMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                MicEffect.MEGAFONO -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("FILTRO PASA-BANDA", color = DjOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Corte: 400Hz - 3.2kHz", color = DjTextMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                MicEffect.NONE -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("VOZ LIMPIA (BYPASS)", color = DjGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Sin procesamiento", color = DjTextMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }

                            // Perilla [MEZCLA DRY / WET]
                            RotaryKnob(
                                value = state.dryWetMix,
                                onValueChange = onDryWetChange,
                                range = 0f..1f,
                                label = "DRY / WET",
                                displayValue = "${(state.dryWetMix * 100).toInt()}% WET",
                                accentColor = DjGreen,
                                size = 46.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
