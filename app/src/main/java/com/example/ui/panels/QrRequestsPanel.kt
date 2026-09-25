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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.DeckId
import com.example.DjConsoleState
import com.example.QrRequestItem
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
fun QrRequestsPanel(
    state: DjConsoleState,
    onShowQrDialog: () -> Unit,
    onToggleAcceptRequests: (Boolean) -> Unit,
    onLimitPerUserChange: (Int) -> Unit,
    onToggleSmartAutoApprove: (Boolean) -> Unit,
    onApproveToAutoMix: (QrRequestItem) -> Unit,
    onLoadToDeck: (QrRequestItem, DeckId) -> Unit,
    onRejectRequest: (QrRequestItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("qr_requests_panel"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DjPanelDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, DjBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Title + Global controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "QR Requests",
                        tint = DjCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SISTEMA DE PETICIONES POR CÓDIGO QR EN VIVO",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Switch [AUTO-APROBACIÓN INTELIGENTE]
                    StatusSwitch(
                        checked = state.qrSmartAutoApprove,
                        onCheckedChange = onToggleSmartAutoApprove,
                        label = "AUTO-APROBAR",
                        activeColor = DjCyan,
                        inactiveColor = Color.Gray,
                        testTag = "smart_auto_approve_switch"
                    )

                    // Switch Interruptor [ACEPTAR PETICIONES QR]
                    StatusSwitch(
                        checked = state.qrAcceptRequests,
                        onCheckedChange = onToggleAcceptRequests,
                        label = if (state.qrAcceptRequests) "RECEPCIÓN ACTIVA" else "BLOQUEADO",
                        activeColor = DjGreen,
                        inactiveColor = DjRed,
                        testTag = "accept_requests_switch"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-bar: Botón [MOSTRAR QR EN PANTALLA] + Indicador [URL DE PETICIONES] + Selector [LÍMITE POR USUARIO]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DjCardDark)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón [MOSTRAR QR EN PANTALLA]
                    Button(
                        onClick = onShowQrDialog,
                        modifier = Modifier.height(30.dp).testTag("show_qr_screen_button"),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DjCyan),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Mostrar QR",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MOSTRAR QR EN PANTALLA",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Indicador Texto [URL DE PETICIONES]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsEthernet,
                            contentDescription = "Server URL",
                            tint = DjTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "URL: ${state.qrServerUrl}",
                            color = DjCyan,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Selector Numérico [LÍMITE POR USUARIO] (1 a 5)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Límite por mesa: ",
                        color = DjTextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    (1..5).forEach { limit ->
                        val isSelected = limit == state.qrLimitPerUser
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) DjCyan else Color(0xFF161922))
                                .clickable { onLimitPerUserChange(limit) }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$limit",
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tabla [COLA DE PETICIONES QR]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = DjCardDark)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF141720))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MESA / CLIENTE",
                            color = DjTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "CANCIÓN / ARTISTA",
                            color = DjTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.weight(2f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "HORA",
                            color = DjTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.weight(0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ESTADO",
                            color = DjTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.weight(0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ACCIONES RÁPIDAS",
                            color = DjTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.weight(2.4f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (state.qrRequestsList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay peticiones en cola. Clientes pueden escanear el QR para pedir música.",
                                color = DjTextMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(state.qrRequestsList, key = { it.id }) { req ->
                                QrRequestRow(
                                    item = req,
                                    onApproveAutoMix = { onApproveToAutoMix(req) },
                                    onLoadDeckA = { onLoadToDeck(req, DeckId.DECK_A) },
                                    onLoadDeckB = { onLoadToDeck(req, DeckId.DECK_B) },
                                    onReject = { onRejectRequest(req) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QrRequestRow(
    item: QrRequestItem,
    onApproveAutoMix: () -> Unit,
    onLoadDeckA: () -> Unit,
    onLoadDeckB: () -> Unit,
    onReject: () -> Unit
) {
    var deckMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF171A24))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mesa
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(DjCyan.copy(alpha = 0.2f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.mesa,
                    color = DjCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Canción y Artista
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = item.cancion,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.artista + if (item.dedicatoria.isNotBlank()) " • \"${item.dedicatoria}\"" else "",
                color = DjTextMuted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Hora
        Text(
            text = item.hora,
            color = Color.LightGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            modifier = Modifier.weight(0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Estado
        val statusColor = when (item.estado) {
            "APROBADO", "EN_COLA" -> DjGreen
            "RECHAZADO" -> DjRed
            else -> DjAmber
        }
        Box(
            modifier = Modifier
                .weight(0.9f)
                .clip(RoundedCornerShape(4.dp))
                .background(statusColor.copy(alpha = 0.2f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = item.estado,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Acciones: Aprobar/Automix, Cargar en Deck, Rechazar
        Row(
            modifier = Modifier.weight(2.4f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón [APROBAR / ENVIAR A AUTOMIX]
            Button(
                onClick = onApproveAutoMix,
                modifier = Modifier.height(26.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DjGreen),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = "AutoMix",
                    tint = Color.Black,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "A AUTOMIX",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Botón [CARGAR EN DECK]
            Box {
                Button(
                    onClick = { deckMenuOpen = true },
                    modifier = Modifier.height(26.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DjCyan),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                ) {
                    Text(
                        text = "DECK ▼",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(
                    expanded = deckMenuOpen,
                    onDismissRequest = { deckMenuOpen = false },
                    modifier = Modifier.background(DjCardDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("Cargar a Plato A", color = DjCyan, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            onLoadDeckA()
                            deckMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cargar a Plato B", color = DjOrange, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            onLoadDeckB()
                            deckMenuOpen = false
                        }
                    )
                }
            }

            // Botón [RECHAZAR]
            IconButton(
                onClick = onReject,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Rechazar",
                    tint = DjRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
