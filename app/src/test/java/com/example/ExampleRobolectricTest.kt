package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Virtual DJ Rivera", appName)
  }

  @Test
  fun `verify data models structure`() {
    val cancion = Cancion("c1", "Cumbia Rivera", "Orquesta Rivera", "Cumbia", 60000L, "file://test.wav")
    assertEquals("c1", cancion.id)
    assertEquals("Cumbia Rivera", cancion.titulo)

    val peticion = PeticionMesa(id = "p1", mesa = "Mesa 3", cancion = "La Bilirrubina", artista = "Juan Luis Guerra")
    assertEquals("PENDIENTE", peticion.estado)
    assertEquals("NEUTRO", peticion.emocion)
  }

  @Test
  fun `verify dj console state initialization`() {
    val state = DjConsoleState()
    assertEquals(RackTab.ANUNCIOS, state.activeRackTab)
    assertEquals(12, state.samplerPads.size.coerceAtLeast(0))
    assertEquals(true, state.adsActive)
    assertEquals("http://192.168.1.50:8080/pedir", state.qrServerUrl)
    assertEquals(MicEffect.NONE, state.selectedMicFx)
    assertEquals(TransitionType.CROSSFADE, state.autoMixTransitionType)
  }
}
