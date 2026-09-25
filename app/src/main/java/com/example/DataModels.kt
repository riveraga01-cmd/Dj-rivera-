package com.example

data class Cancion(
    val id: String,
    val titulo: String,
    val artista: String,
    val genero: String,
    val duracionMs: Long,
    val uri: String
)

enum class EmocionVoz {
    ROMANTICO,
    FELIZ,
    EMOCIONADO,
    NEUTRO
}

data class PeticionMesa(
    val id: String = "",
    val mesa: String = "",
    val cancion: String = "",
    val artista: String = "",
    val dedicatoria: String = "",
    val emocion: String = "NEUTRO",
    val estado: String = "PENDIENTE"
)
