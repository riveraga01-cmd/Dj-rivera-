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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AdsIntervalMode
import com.example.AnuncioItem
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
import com.example.ui.components.RotaryKnob
import com.example.ui.components.StatusSwitch

@Composable
fun AdsPanel(
    state: DjConsoleState,
    onToggleAdsActive: (Boolean) -> Unit,
    onIntervalModeChange: (AdsIntervalMode) -> Unit,
    onIntervalValueChange: (Float) -> Unit,
    onSelectAnuncio: (String) -> Unit,
    onToggleAnuncioActivo: (String) -> Unit,
    onPlayAdNow: () -> Unit,
    onAddAd: () -> Unit,
    onDuckingLevelChange: (Float) -> Unit,
    onClosingTimeChange: (String) -> Unit,
    onClosingFarewellChange: (Boolean) -> Unit,
    onClosingBlockQrChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var modeDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ads_panel"),
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
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Anuncios",
                        tint = DjAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PANEL DE PUBLICIDAD Y ANUNCIOS AUTOMÁTICOS",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Switch Interruptor: [ANUNCIOS ACTIVOS / INACTIVOS]
                StatusSwitch(
                    checked = state.adsActive,
                    onCheckedChange = onToggleAdsActive,
                    label = if (state.adsActive) "SISTEMA ACTIVO" else "SISTEMA EN PAUSA",
                    activeColor = DjGreen,
                    inactiveColor = DjRed,
                    testTag = "ads_active_switch"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main 3-column / Section layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Col 1: Configuración de Intervalos & Contador
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DjCardDark)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "CONFIGURACIÓN DE INTERVALO",
                            color = DjCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Selector Desplegable (Dropdown) [MODO DE INTERVALO]
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF151821))
                                    .border(1.dp, DjBorder, RoundedCornerShape(6.dp))
                                    .clickable { modeDropdownExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.adsIntervalMode.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "▼",
                                    color = DjCyan,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            DropdownMenu(
                                expanded = modeDropdownExpanded,
                                onDismissRequest = { modeDropdownExpanded = false },
                                modifier = Modifier.background(DjCardDark)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Por Tiempo (Minutos)", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        onIntervalModeChange(AdsIntervalMode.POR_TIEMPO)
                                        modeDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Por Cantidad de Canciones", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        onIntervalModeChange(AdsIntervalMode.POR_CANCIONES)
                                        modeDropdownExpanded = false
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Deslizador (Slider) [VALOR DE INTERVALO]
                        val isPorTiempo = state.adsIntervalMode == AdsIntervalMode.POR_TIEMPO
                        val minVal = if (isPorTiempo) 5f else 1f
                        val maxVal = if (isPorTiempo) 60f else 15f
                        val displayUnit = if (isPorTiempo) "${state.adsIntervalValue.toInt()} min" else "${state.adsIntervalValue.toInt()} canciones"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Intervalo",
                                color = DjTextMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = displayUnit,
                                color = DjAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Slider(
                            value = state.adsIntervalValue,
                            onValueChange = onIntervalValueChange,
                            valueRange = minVal..maxVal,
                            colors = SliderDefaults.colors(
                                thumbColor = DjAmber,
                                activeTrackColor = DjAmber,
                                inactiveTrackColor = Color(0xFF2E3547)
                            ),
                            modifier = Modifier.fillMaxWidth().height(26.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Pantalla Contador en Vivo
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1118)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DjAmber.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isPorTiempo) "PRÓXIMO ANUNCIO EN:" else "CANCIONES RESTANTES:",
                                    color = DjTextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val countdownText = if (isPorTiempo) {
                                    val m = state.adsCountdownSeconds / 60
                                    val s = state.adsCountdownSeconds % 60
                                    String.format("%02d:%02d", m, s)
                                } else {
                                    "${state.adsSongsRemaining} TEMAS"
                                }
                                Text(
                                    text = countdownText,
                                    color = if (state.isPlayingAd) DjRed else DjAmber,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (state.isPlayingAd) "EMITIENDO AHORA (AUDIO DUCKED)" else "AUTO-TRIGGER LISTO",
                                    color = if (state.isPlayingAd) DjRed else DjGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Slider / Perilla [NIVEL AUDIO DUCKING]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            RotaryKnob(
                                value = state.adsDuckingLevel,
                                onValueChange = onDuckingLevelChange,
                                range = 0.05f..0.50f,
                                label = "DUCKING",
                                displayValue = "${(state.adsDuckingLevel * 100).toInt()}%",
                                accentColor = DjOrange,
                                size = 44.dp
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(
                                    text = "Atenuación Música",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Música baja al ${(state.adsDuckingLevel * 100).toInt()}% de volumen",
                                    color = DjTextMuted,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Col 2: Tabla / Lista [BIBLIOTECA DE ANUNCIOS] + Botones
                Card(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DjCardDark)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BIBLIOTECA DE ANUNCIOS",
                                color = DjCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Botón [+ AGREGAR ANUNCIO]
                            Button(
                                onClick = onAddAd,
                                modifier = Modifier.height(26.dp).testTag("add_ad_button"),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DjCyan),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Agregar",
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "+ AGREGAR ANUNCIO",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Lista de anuncios
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(state.adsLibrary, key = { it.id }) { item ->
                                val isSelected = item.id == state.selectedAnuncioId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF283147) else Color(0xFF151822))
                                        .border(
                                            1.dp,
                                            if (isSelected) DjCyan else Color(0xFF242B3C),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onSelectAnuncio(item.id) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.nombre,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.duracionSeg}s • Frec: ${item.frecuencia}",
                                            color = DjTextMuted,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (item.activo) DjGreen.copy(alpha = 0.2f) else DjRed.copy(alpha = 0.2f))
                                                .clickable { onToggleAnuncioActivo(item.id) }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (item.activo) "ACTIVO" else "PAUSA",
                                                color = if (item.activo) DjGreen else DjRed,
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botón [REPRODUCIR AHORA]
                        Button(
                            onClick = onPlayAdNow,
                            modifier = Modifier.fillMaxWidth().height(32.dp).testTag("play_ad_now_button"),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isPlayingAd) DjRed else DjOrange
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.isPlayingAd) "REPRODUCIENDO PUBLICIDAD..." else "REPRODUCIR AHORA (FORZAR)",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Col 3: Módulo Hora de Cierre
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DjCardDark)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Hora de Cierre",
                                tint = DjRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "HORA DE CIERRE LOCAL",
                                color = DjRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Campo [HORA DE CIERRE]
                        OutlinedTextField(
                            value = state.closingTimeText,
                            onValueChange = onClosingTimeChange,
                            label = { Text("Hora de Cierre", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("closing_time_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = DjRed,
                                unfocusedBorderColor = DjBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Checkbox [ANUNCIO DE DESPEDIDA]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClosingFarewellChange(!state.closingFarewellAnnouncement) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.closingFarewellAnnouncement,
                                onCheckedChange = onClosingFarewellChange,
                                colors = CheckboxDefaults.colors(checkedColor = DjRed),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Anuncio de despedida al llegar hora",
                                color = Color.White,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Checkbox [BLOQUEAR QR AL CERRAR]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClosingBlockQrChange(!state.closingBlockQr) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.closingBlockQr,
                                onCheckedChange = onClosingBlockQrChange,
                                colors = CheckboxDefaults.colors(checkedColor = DjOrange),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bloquear peticiones QR al cumplir horario",
                                color = Color.White,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF151821))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Seguridad de Cierre: Programada",
                                color = DjTextMuted,
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
