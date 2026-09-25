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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.VolumeUp
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
import com.example.SamplerPadState
import com.example.SamplerTriggerMode
import com.example.ui.components.DjAmber
import com.example.ui.components.DjBorder
import com.example.ui.components.DjCardDark
import com.example.ui.components.DjCyan
import com.example.ui.components.DjGreen
import com.example.ui.components.DjOrange
import com.example.ui.components.DjPanelDark
import com.example.ui.components.DjRed
import com.example.ui.components.DjTextMuted
import com.example.ui.components.PadButton
import com.example.ui.components.RotaryKnob
import com.example.ui.components.StatusSwitch

@Composable
fun SamplerPanel(
    state: DjConsoleState,
    onTriggerPad: (Int) -> Unit,
    onAssignCustomSample: (Int) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTriggerModeChange: (SamplerTriggerMode) -> Unit,
    onToggleDucking: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sampler_panel"),
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
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Sampler",
                        tint = DjOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SAMPLER DE EFECTOS & JINGLES (12 PADS ILUMINADOS)",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Global Info
                Text(
                    text = "MODO: ${state.samplerTriggerMode.label} • DUCKING: ${if (state.samplerDuckingEnabled) "ACTIVO" else "OFF"}",
                    color = DjCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left Col: Controls (Volumen Sampler, Modo de Disparo, Ducking switch)
                Card(
                    modifier = Modifier
                        .width(170.dp)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DjCardDark)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CONTROL MASTER PADS",
                            color = DjCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Perilla [VOLUMEN SAMPLER]
                        RotaryKnob(
                            value = state.samplerVolume,
                            onValueChange = onVolumeChange,
                            range = 0f..1f,
                            label = "VOLUMEN PADS",
                            displayValue = "${(state.samplerVolume * 100).toInt()}%",
                            accentColor = DjOrange,
                            size = 48.dp,
                            testTag = "sampler_volume_knob"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Selector de Modo de Disparo [HOLD / TRIGGER / LOOP]
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "MODO DE DISPARO",
                                color = DjTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                SamplerTriggerMode.values().forEach { mode ->
                                    val isSelected = mode == state.samplerTriggerMode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) DjOrange else Color(0xFF161922))
                                            .clickable { onTriggerModeChange(mode) }
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.name.take(4),
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Switch [DUCKING EN SAMPLER]
                        StatusSwitch(
                            checked = state.samplerDuckingEnabled,
                            onCheckedChange = onToggleDucking,
                            label = "DUCKING AL DISPARAR",
                            activeColor = DjGreen,
                            inactiveColor = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "sampler_ducking_switch"
                        )
                    }
                }

                // Right: Matriz de 12 Pads Neón (2 filas de 6)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DjCardDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Fila 1: Pads 1 al 6
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.samplerPads.take(6).forEach { pad ->
                                PadButton(
                                    title = pad.name,
                                    subtitle = if (pad.id <= 6) "PRESET" else (if (pad.customUri != null) "CUSTOM" else "+ CARGAR"),
                                    accentColor = Color(pad.colorHex),
                                    isActive = pad.isPlaying,
                                    onClick = {
                                        if (pad.id > 6 && pad.customUri == null) {
                                            onAssignCustomSample(pad.id)
                                        } else {
                                            onTriggerPad(pad.id)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    testTag = "sampler_pad_${pad.id}"
                                )
                            }
                        }

                        // Fila 2: Pads 7 al 12
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.samplerPads.drop(6).take(6).forEach { pad ->
                                PadButton(
                                    title = pad.name,
                                    subtitle = if (pad.customUri != null) "CUSTOM WAV" else "+ CARGAR",
                                    accentColor = Color(pad.colorHex),
                                    isActive = pad.isPlaying,
                                    onClick = {
                                        if (pad.customUri == null) {
                                            onAssignCustomSample(pad.id)
                                        } else {
                                            onTriggerPad(pad.id)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    testTag = "sampler_pad_${pad.id}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
