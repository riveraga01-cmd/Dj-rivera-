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
import androidx.compose.ui.window.Dialog
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
import com.example.TipoLector
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
    onSaveAd: ((id: String?, nombre: String, duracion: Int, frecuencia: String, textoLocucion: String, tipoLector: TipoLector) -> Unit)? = null,
    onDeleteAd: ((id: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var modeDropdownExpanded by remember { mutableStateOf(false) }

    var isModalOpen by remember { mutableStateOf(false) }
    var editingAdId by remember { mutableStateOf<String?>(null) }
    var formNombre by remember { mutableStateOf("") }
    var formTextoLocucion by remember { mutableStateOf("") }
    var formTipoLector by remember { mutableStateOf(TipoLector.LOCUTOR_RADIO) }
    var formDuracion by remember { mutableStateOf("15") }
    var formFrecuencia by remember { mutableStateOf("Cada 30m") }
    var formTipoTts by remember { mutableStateOf(true) }

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
                                onClick = {
                                    editingAdId = null
                                    formNombre = ""
                                    formTextoLocucion = ""
                                    formTipoLector = TipoLector.LOCUTOR_RADIO
                                    formDuracion = "15"
                                    formFrecuencia = "Cada 30m"
                                    formTipoTts = true
                                    isModalOpen = true
                                },
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
                                        if (item.textoLocucion.isNotBlank()) {
                                            Text(
                                                text = "\"${item.textoLocucion}\"",
                                                color = DjCyan.copy(alpha = 0.85f),
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${item.duracionSeg}s • Frec: ${item.frecuencia} • ${item.tipoLector.label}",
                                            color = DjTextMuted,
                                            fontSize = 8.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Botón Editar (Lápiz)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF1E2433))
                                                .clickable {
                                                    editingAdId = item.id
                                                    formNombre = item.nombre
                                                    formTextoLocucion = item.textoLocucion
                                                    formTipoLector = item.tipoLector
                                                    formDuracion = item.duracionSeg.toString()
                                                    formFrecuencia = item.frecuencia
                                                    isModalOpen = true
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "✏️",
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Botón Eliminar (Basura)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2E1C1C))
                                                .clickable { onDeleteAd?.invoke(item.id) }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "🗑️",
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Switch Activo / Pausa
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

    // Ventana Modal de Agregar / Editar Anuncio
    if (isModalOpen) {
        Dialog(onDismissRequest = { isModalOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DjPanelDark),
                border = androidx.compose.foundation.BorderStroke(2.dp, DjCyan)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (editingAdId != null) "✏️ EDITAR ANUNCIO PUBLICITARIO" else "+ AGREGAR NUEVO ANUNCIO",
                        color = DjCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = formNombre,
                        onValueChange = { formNombre = it },
                        label = { Text("Título / Nombre del Anuncio", color = DjTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DjCyan,
                            unfocusedBorderColor = DjBorder
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = formTextoLocucion,
                        onValueChange = { formTextoLocucion = it },
                        label = { Text("Texto Exacto a Leer por el Locutor (Sin Prefijos)", color = DjAmber, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("Escribe aquí el texto exacto que el locutor pronunciará...", color = Color.Gray, fontSize = 9.sp) },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DjAmber,
                            unfocusedBorderColor = DjBorder
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "TIPO DE LECTOR / PERFIL DE VOZ (CERO ROBÓTICO):",
                        color = DjCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (lector in TipoLector.entries) {
                            val isSelected = formTipoLector == lector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) DjCyan.copy(alpha = 0.2f) else Color(0xFF151821))
                                    .border(1.dp, if (isSelected) DjCyan else DjBorder, RoundedCornerShape(6.dp))
                                    .clickable { formTipoLector = lector }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lector.label,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isSelected) {
                                    Text("✓", color = DjCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = formDuracion,
                            onValueChange = { formDuracion = it },
                            label = { Text("Duración (s)", color = DjTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = DjCyan,
                                unfocusedBorderColor = DjBorder
                            )
                        )
                        OutlinedTextField(
                            value = formFrecuencia,
                            onValueChange = { formFrecuencia = it },
                            label = { Text("Frecuencia", color = DjTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = DjCyan,
                                unfocusedBorderColor = DjBorder
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { formTipoTts = true },
                            colors = ButtonDefaults.buttonColors(containerColor = if (formTipoTts) DjCyan else Color(0xFF222632)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sintetizador TTS", color = if (formTipoTts) Color.Black else Color.White, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = { formTipoTts = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (!formTipoTts) DjAmber else Color(0xFF222632)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Audio Local (.mp3)", color = if (!formTipoTts) Color.Black else Color.White, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { isModalOpen = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2E3D))
                        ) {
                            Text("CANCELAR", color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (formNombre.isNotBlank()) {
                                    val dur = formDuracion.toIntOrNull() ?: 15
                                    val textLoc = if (formTextoLocucion.isNotBlank()) formTextoLocucion.trim() else formNombre.trim()
                                    if (onSaveAd != null) {
                                        onSaveAd.invoke(editingAdId, formNombre, dur, formFrecuencia, textLoc, formTipoLector)
                                    } else {
                                        onAddAd()
                                    }
                                    isModalOpen = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DjGreen)
                        ) {
                            Text("GUARDAR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
