package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SongJsonParserTest {

    @Test
    fun `parse valid JSON array with spanish keys`() {
        val json = """
            [
                {
                    "id": "track_1",
                    "titulo": "Cumbia de la Noche",
                    "artista": "Los Palmeras",
                    "genero": "Cumbia",
                    "duracionSeg": 180
                },
                {
                    "titulo": "Salsa Brava",
                    "artista": "Sonora",
                    "genero": "Salsa",
                    "duracionMs": 210000
                }
            ]
        """.trimIndent()

        val songs = SongJsonParser.parse(json, "file://default.wav")
        assertEquals(2, songs.size)

        assertEquals("track_1", songs[0].id)
        assertEquals("Cumbia de la Noche", songs[0].titulo)
        assertEquals("Los Palmeras", songs[0].artista)
        assertEquals("Cumbia", songs[0].genero)
        assertEquals(180000L, songs[0].duracionMs)
        assertEquals("file://default.wav", songs[0].uri)

        assertEquals("Salsa Brava", songs[1].titulo)
        assertEquals(210000L, songs[1].duracionMs)
        assertTrue(songs[1].id.startsWith("json_"))
    }

    @Test
    fun `parse valid JSON object with songs wrapper and english keys`() {
        val json = """
            {
                "songs": [
                    {
                        "title": "Electronic Beat",
                        "artist": "DJ Synth",
                        "genre": "Electrónica",
                        "duration": 190,
                        "url": "file://cyber.wav"
                    }
                ]
            }
        """.trimIndent()

        val songs = SongJsonParser.parse(json)
        assertEquals(1, songs.size)
        assertEquals("Electronic Beat", songs[0].titulo)
        assertEquals("DJ Synth", songs[0].artista)
        assertEquals("Electrónica", songs[0].genero)
        assertEquals(190000L, songs[0].duracionMs)
        assertEquals("file://cyber.wav", songs[0].uri)
    }

    @Test
    fun `parse sample json from template`() {
        val songs = SongJsonParser.parse(SongJsonParser.SAMPLE_JSON)
        assertTrue(songs.size >= 4)
        assertEquals("Noche de Cumbia", songs[0].titulo)
        assertEquals("Cumbia", songs[0].genero)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws on empty string`() {
        SongJsonParser.parse("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws on invalid root object without array key`() {
        SongJsonParser.parse("""{"name": "No array here"}""")
    }
}
