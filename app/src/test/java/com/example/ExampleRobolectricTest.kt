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
    assertEquals(8f, state.autoMixDurationSec)
    assertEquals(false, state.autoMixActive)
  }

  @Test
  fun `verify mic fx and automix transitions enum definitions`() {
    val effects = MicEffect.values()
    assertEquals(true, effects.contains(MicEffect.ROBOT))
    assertEquals(true, effects.contains(MicEffect.MEGAFONO))
    assertEquals(true, effects.contains(MicEffect.REVERB))
    assertEquals(true, effects.contains(MicEffect.ECHO_DELAY))

    val transitions = TransitionType.values()
    assertEquals(true, transitions.contains(TransitionType.CROSSFADE))
    assertEquals(true, transitions.contains(TransitionType.BEATMATCH_SYNC))
    assertEquals(true, transitions.contains(TransitionType.FADE_OUT_IN))
    assertEquals(true, transitions.contains(TransitionType.CORTE_DIRECTO))
  }

  @Test
  fun `verify automix remaining time threshold logic`() {
    val durationMs = 60000L
    val positionMs = 52000L
    val mixDurationSec = 8f
    val mixDurationMs = (mixDurationSec * 1000f).toLong()

    val remainingMs = durationMs - positionMs
    assertEquals(8000L, remainingMs)
    assertEquals(mixDurationMs, remainingMs)

    // Trigger condition is met when remainingMs in 1L..mixDurationMs
    val shouldTrigger = remainingMs in 1L..mixDurationMs
    assertEquals(true, shouldTrigger)
  }

  @Test
  fun `verify mic dsp engine initial parameters and effect mapping`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var receivedVu = 0f
    val engine = RealtimeMicDspEngine(context) { vu -> receivedVu = vu }

    assertEquals(false, engine.isEnabled)
    assertEquals(0.80f, engine.micGain, 0.01f)
    assertEquals(0.50f, engine.dryWetMix, 0.01f)
    assertEquals(MicEffect.NONE, engine.selectedEffect)

    engine.selectedEffect = MicEffect.ROBOT
    assertEquals(MicEffect.ROBOT, engine.selectedEffect)

    engine.dryWetMix = 0.75f
    assertEquals(0.75f, engine.dryWetMix, 0.01f)

    engine.micGain = 0.90f
    assertEquals(0.90f, engine.micGain, 0.01f)
  }

  @Test
  fun `verify tts voice preset configuration and male female detection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val ttsManager = EmotionalTtsManager(context) { /* no-op ducking */ }

    val locutorPreset = ttsManager.getPresetConfig("Español Latino (Locutor Profesional)")
    assertEquals(false, locutorPreset.isFemale)
    assertEquals(0.95f, locutorPreset.basePitch, 0.01f)

    val femeninaPreset = ttsManager.getPresetConfig("Español Latino (Femenino Enérgico)")
    assertEquals(true, femeninaPreset.isFemale)
    assertEquals(1.28f, femeninaPreset.basePitch, 0.01f)

    val nightclubPreset = ttsManager.getPresetConfig("Español España (DJ Nightclub)")
    assertEquals(false, nightclubPreset.isFemale)

    val neonPreset = ttsManager.getPresetConfig("Voz Modulada Neón (Deep Bass)")
    assertEquals(false, neonPreset.isFemale)
    assertEquals(0.65f, neonPreset.basePitch, 0.01f)
  }
}
