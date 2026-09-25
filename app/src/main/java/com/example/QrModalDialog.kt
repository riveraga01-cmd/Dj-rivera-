package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun QrModalDialog(
    onDismissRequest: () -> Unit,
    onSendSampleRequest: (mesa: String, cancion: String, artista: String, dedicatoria: String, emocion: EmocionVoz) -> Unit
) {
    var mesaSeleccionada by remember { mutableStateOf("Mesa 4") }
    var cancionNombre by remember { mutableStateOf("La Bilirrubina") }
    var artistaNombre by remember { mutableStateOf("Juan Luis Guerra") }
    var dedicatoriaTexto by remember { mutableStateOf("Para María con todo mi amor") }
    var emocionSeleccionada by remember { mutableStateOf(EmocionVoz.ROMANTICO) }
    var mostrarSimuladorEnvio by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("qr_modal_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF141724),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "QR Code Icon",
                            tint = Color(0xFF00E5FF)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CÓDIGO QR PARA MESAS",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp).testTag("close_qr_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar modal",
                            tint = Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Escanea para pedir canciones desde tu mesa en vivo",
                    color = Color(0xFFB0B8C8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // QR Graphic Box
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .background(Color.White, shape = RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeCanvas(url = "https://virtualdj-rivera.app/mesa?id=${mesaSeleccionada.replace(" ", "")}")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "URL: virtualdj-rivera.app/mesa",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Quick Table Request Simulator
                Button(
                    onClick = { mostrarSimuladorEnvio = !mostrarSimuladorEnvio },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mostrarSimuladorEnvio) Color(0xFF242B42) else Color(0xFF00E5FF)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("toggle_simulator_button")
                ) {
                    Text(
                        text = if (mostrarSimuladorEnvio) "Ocultar Simulador de Mesa" else "Probar Petición de Mesa en Vivo",
                        color = if (mostrarSimuladorEnvio) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (mostrarSimuladorEnvio) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Enviar Petición de Mesa a Firebase",
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = mesaSeleccionada,
                                    onValueChange = { mesaSeleccionada = it },
                                    label = { Text("Mesa", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.weight(1f).testTag("input_mesa"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                OutlinedTextField(
                                    value = cancionNombre,
                                    onValueChange = { cancionNombre = it },
                                    label = { Text("Canción", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.weight(1.5f).testTag("input_cancion"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = artistaNombre,
                                    onValueChange = { artistaNombre = it },
                                    label = { Text("Artista", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.weight(1f).testTag("input_artista"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                OutlinedTextField(
                                    value = dedicatoriaTexto,
                                    onValueChange = { dedicatoriaTexto = it },
                                    label = { Text("Dedicatoria", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.weight(1.5f).testTag("input_dedicatoria"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Emotion selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EmocionVoz.values().forEach { emo ->
                                    val isSelected = emo == emocionSeleccionada
                                    Button(
                                        onClick = { emocionSeleccionada = emo },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) Color(0xFFFF007F) else Color(0xFF2C324B)
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = emo.name.take(4),
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    onSendSampleRequest(
                                        mesaSeleccionada,
                                        cancionNombre,
                                        artistaNombre,
                                        dedicatoriaTexto,
                                        emocionSeleccionada
                                    )
                                    mostrarSimuladorEnvio = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                modifier = Modifier.fillMaxWidth().testTag("send_table_request_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Enviar",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Enviar a /peticiones_rivera",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
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

/**
 * Procedural standard QR code pattern rendering on Compose Canvas
 */
@Composable
fun QrCodeCanvas(url: String, modifier: Modifier = Modifier.size(150.dp)) {
    Canvas(modifier = modifier) {
        val gridSize = 21 // Version 1 QR code size
        val cellSize = size.width / gridSize
        val hash = url.hashCode()

        // Background white
        drawRect(Color.White, size = size)

        // Draw Finder Patterns (top-left, top-right, bottom-left)
        fun drawFinderPattern(startX: Int, startY: Int) {
            // 7x7 outer black
            drawRect(
                color = Color.Black,
                topLeft = Offset(startX * cellSize, startY * cellSize),
                size = Size(7 * cellSize, 7 * cellSize)
            )
            // 5x5 inner white
            drawRect(
                color = Color.White,
                topLeft = Offset((startX + 1) * cellSize, (startY + 1) * cellSize),
                size = Size(5 * cellSize, 5 * cellSize)
            )
            // 3x3 inner black
            drawRect(
                color = Color.Black,
                topLeft = Offset((startX + 2) * cellSize, (startY + 2) * cellSize),
                size = Size(3 * cellSize, 3 * cellSize)
            )
        }

        drawFinderPattern(0, 0)
        drawFinderPattern(14, 0)
        drawFinderPattern(0, 14)

        // Draw timing patterns
        for (i in 8 until 13) {
            if (i % 2 == 0) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(6 * cellSize, i * cellSize),
                    size = Size(cellSize, cellSize)
                )
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(i * cellSize, 6 * cellSize),
                    size = Size(cellSize, cellSize)
                )
            }
        }

        // Procedural data bits based on url hash
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                // Skip finder areas
                val inTopLeft = (r in 0..7 && c in 0..7)
                val inTopRight = (r in 0..7 && c in 13..20)
                val inBottomLeft = (r in 13..20 && c in 0..7)
                val inTiming = (r == 6 || c == 6)

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inTiming) {
                    val bit = ((hash xor (r * 31 + c * 17) xor (r * c)) and (1 shl ((r + c) % 16))) != 0
                    if (bit) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    }
}
