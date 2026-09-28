package com.example

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class MidiControlEvent {
    data class Crossfader(val value: Float) : MidiControlEvent()
    data class FaderA(val value: Float) : MidiControlEvent()
    data class FaderB(val value: Float) : MidiControlEvent()
    data class PlayDeck(val deckId: DeckId) : MidiControlEvent()
    data class CueDeck(val deckId: DeckId) : MidiControlEvent()
}

class UsbMidiService : Service() {

    private val tag = "UsbMidiService"
    private var midiManager: MidiManager? = null
    private val openDevices = mutableListOf<MidiDevice>()
    private val openPorts = mutableListOf<MidiOutputPort>()

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): UsbMidiService = this@UsbMidiService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            midiManager = getSystemService(Context.MIDI_SERVICE) as? MidiManager
            setupMidiDeviceListener()
            scanConnectedMidiDevices()
        }
    }

    private fun setupMidiDeviceListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            midiManager?.registerDeviceCallback(object : MidiManager.DeviceCallback() {
                override fun onDeviceAdded(device: MidiDeviceInfo?) {
                    Log.d(tag, "Dispositivo MIDI conectado: ${device?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME)}")
                    device?.let { openMidiDevice(it) }
                }

                override fun onDeviceRemoved(device: MidiDeviceInfo?) {
                    Log.d(tag, "Dispositivo MIDI desconectado: ${device?.properties?.getString(MidiDeviceInfo.PROPERTY_NAME)}")
                }
            }, Handler(Looper.getMainLooper()))
        }
    }

    private fun scanConnectedMidiDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = midiManager?.devices ?: emptyArray()
            for (info in devices) {
                openMidiDevice(info)
            }
        }
    }

    private fun openMidiDevice(info: MidiDeviceInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            midiManager?.openDevice(info, { device ->
                if (device != null) {
                    openDevices.add(device)
                    for (portInfo in info.ports) {
                        if (portInfo.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                            val outputPort = device.openOutputPort(portInfo.portNumber)
                            if (outputPort != null) {
                                openPorts.add(outputPort)
                                outputPort.connect(MidiEventReceiver())
                                Log.i(tag, "Puerto MIDI de entrada conectado exitosamente")
                            }
                        }
                    }
                }
            }, Handler(Looper.getMainLooper()))
        }
    }

    private inner class MidiEventReceiver : MidiReceiver() {
        override fun onSend(msg: ByteArray?, offset: Int, count: Int, timestamp: Long) {
            if (msg == null || count < 3) return
            val status = (msg[offset].toInt() and 0xFF)
            val command = status and 0xF0
            val data1 = (msg[offset + 1].toInt() and 0xFF)
            val data2 = (msg[offset + 2].toInt() and 0xFF)

            // Handle Control Change (0xB0..0xBF)
            if (command == 0xB0) {
                val ccNumber = data1
                val ccValue = data2 / 127f // Normalized 0.0f .. 1.0f

                when (ccNumber) {
                    // Standard DJ Controller Crossfader CCs (CC 8, 10 or 19)
                    8, 10, 19 -> {
                        _events.tryEmit(MidiControlEvent.Crossfader(ccValue))
                    }
                    // Channel A Fader (CC 14, 20 or 28)
                    14, 20, 28 -> {
                        _events.tryEmit(MidiControlEvent.FaderA(ccValue))
                    }
                    // Channel B Fader (CC 15, 21 or 29)
                    15, 21, 29 -> {
                        _events.tryEmit(MidiControlEvent.FaderB(ccValue))
                    }
                }
            } else if (command == 0x90 && data2 > 0) {
                // Note On (Buttons / Pads / Play / Cue)
                when (data1) {
                    0x30, 0x31 -> _events.tryEmit(MidiControlEvent.PlayDeck(DeckId.DECK_A))
                    0x32, 0x33 -> _events.tryEmit(MidiControlEvent.PlayDeck(DeckId.DECK_B))
                    0x34 -> _events.tryEmit(MidiControlEvent.CueDeck(DeckId.DECK_A))
                    0x35 -> _events.tryEmit(MidiControlEvent.CueDeck(DeckId.DECK_B))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        openPorts.forEach {
            try { it.close() } catch (_: Exception) {}
        }
        openDevices.forEach {
            try { it.close() } catch (_: Exception) {}
        }
        openPorts.clear()
        openDevices.clear()
    }

    companion object {
        private val _events = MutableSharedFlow<MidiControlEvent>(extraBufferCapacity = 64)
        val events: SharedFlow<MidiControlEvent> = _events.asSharedFlow()
    }
}
