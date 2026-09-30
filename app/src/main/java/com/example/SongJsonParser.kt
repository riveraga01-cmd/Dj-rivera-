package com.example

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Parses simplified JSON structures containing songs/tracks into [Cancion] models.
 * Supports:
 * - Direct array of song objects: [ { "titulo": "...", "artista": "..." }, ... ]
 * - Object with wrapper key: { "canciones": [ ... ] } or { "songs": [ ... ] } or { "tracks": [ ... ] }
 * - Both Spanish and English field names (titulo/title, artista/artist, genero/genre, duracionMs/duration)
 * - Flexible duration formats (milliseconds or seconds)
 */
object SongJsonParser {

    /**
     * Parses the given JSON string into a list of [Cancion] objects.
     *
     * @param jsonStr Raw JSON string.
     * @param fallbackUri Default playable URI to assign when a song does not provide a URI.
     * @return List of parsed [Cancion] items.
     * @throws IllegalArgumentException on malformed JSON or invalid schema.
     */
    fun parse(jsonStr: String, fallbackUri: String = ""): List<Cancion> {
        val trimmed = jsonStr.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("El contenido JSON no puede estar vacío.")
        }

        val jsonArray: JSONArray = when {
            trimmed.startsWith("[") -> {
                JSONArray(trimmed)
            }
            trimmed.startsWith("{") -> {
                val rootObj = JSONObject(trimmed)
                when {
                    rootObj.has("canciones") -> rootObj.getJSONArray("canciones")
                    rootObj.has("songs") -> rootObj.getJSONArray("songs")
                    rootObj.has("tracks") -> rootObj.getJSONArray("tracks")
                    rootObj.has("pistas") -> rootObj.getJSONArray("pistas")
                    rootObj.has("items") -> rootObj.getJSONArray("items")
                    rootObj.has("data") -> rootObj.getJSONArray("data")
                    else -> throw IllegalArgumentException(
                        "El objeto JSON debe contener una lista bajo la clave 'canciones', 'songs', 'tracks' o 'items'."
                    )
                }
            }
            else -> {
                throw IllegalArgumentException("Formato JSON inválido. Debe comenzar con '[' o '{'.")
            }
        }

        val parsedSongs = mutableListOf<Cancion>()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue

            val id = if (item.has("id") && item.getString("id").isNotBlank()) {
                item.getString("id")
            } else {
                "json_${UUID.randomUUID().toString().replace("-", "").take(8)}"
            }

            val titulo = when {
                item.has("titulo") && item.getString("titulo").isNotBlank() -> item.getString("titulo")
                item.has("title") && item.getString("title").isNotBlank() -> item.getString("title")
                item.has("cancion") && item.getString("cancion").isNotBlank() -> item.getString("cancion")
                item.has("name") && item.getString("name").isNotBlank() -> item.getString("name")
                else -> "Pista Importada ${i + 1}"
            }

            val artista = when {
                item.has("artista") && item.getString("artista").isNotBlank() -> item.getString("artista")
                item.has("artist") && item.getString("artist").isNotBlank() -> item.getString("artist")
                item.has("autor") && item.getString("autor").isNotBlank() -> item.getString("autor")
                item.has("author") && item.getString("author").isNotBlank() -> item.getString("author")
                else -> "Artista Desconocido"
            }

            val genero = when {
                item.has("genero") && item.getString("genero").isNotBlank() -> item.getString("genero")
                item.has("genre") && item.getString("genre").isNotBlank() -> item.getString("genre")
                item.has("categoria") && item.getString("categoria").isNotBlank() -> item.getString("categoria")
                item.has("category") && item.getString("category").isNotBlank() -> item.getString("category")
                else -> "Varios"
            }

            val duracionMs = when {
                item.has("duracionMs") -> item.getLong("duracionMs")
                item.has("durationMs") -> item.getLong("durationMs")
                item.has("duracionSeg") -> item.getLong("duracionSeg") * 1000L
                item.has("duration") -> item.getLong("duration") * 1000L
                item.has("durationSec") -> item.getLong("durationSec") * 1000L
                item.has("segundos") -> item.getLong("segundos") * 1000L
                else -> 180000L // 3 minutes default
            }.coerceAtLeast(1000L)

            val uri = when {
                item.has("uri") && item.getString("uri").isNotBlank() -> item.getString("uri")
                item.has("url") && item.getString("url").isNotBlank() -> item.getString("url")
                item.has("path") && item.getString("path").isNotBlank() -> item.getString("path")
                else -> fallbackUri
            }

            parsedSongs.add(
                Cancion(
                    id = id,
                    titulo = titulo,
                    artista = artista,
                    genero = genero,
                    duracionMs = duracionMs,
                    uri = uri
                )
            )
        }

        return parsedSongs
    }

    /**
     * Default sample JSON for template and demonstration purposes.
     */
    const val SAMPLE_JSON = """[
  {
    "titulo": "Noche de Cumbia",
    "artista": "Los Palmeras Rivera",
    "genero": "Cumbia",
    "duracionSeg": 195
  },
  {
    "titulo": "Salsa Brava Club",
    "artista": "Sonora Rivera",
    "genero": "Salsa",
    "duracionSeg": 210
  },
  {
    "titulo": "Cyber Electronic Drop",
    "artista": "DJ Synthwave",
    "genero": "Electrónica",
    "duracionSeg": 180
  },
  {
    "titulo": "Bachata Romántica",
    "artista": "Romeo Rivera",
    "genero": "Bachata",
    "duracionSeg": 225
  }
]"""
}
