package com.example

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val tag = "FirebaseRepository"
    private var firestore: FirebaseFirestore? = null
    private var listenerRegistration: ListenerRegistration? = null

    private val _peticiones = MutableStateFlow<List<PeticionMesa>>(emptyList())
    val peticiones: StateFlow<List<PeticionMesa>> = _peticiones.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val localBackup = mutableListOf<PeticionMesa>()

    init {
        initFirestore()
    }

    private fun initFirestore() {
        try {
            if (FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()) {
                firestore = FirebaseFirestore.getInstance()
                listenPeticionesPendientes()
                _isConnected.value = true
            } else {
                setupLocalFallback()
            }
        } catch (e: Exception) {
            Log.w(tag, "Firebase not initialized or missing config, falling back to local sync: ${e.message}")
            setupLocalFallback()
        }
    }

    private fun listenPeticionesPendientes() {
        val db = firestore ?: return
        listenerRegistration?.remove()

        listenerRegistration = db.collection("peticiones_rivera")
            .whereEqualTo("estado", "PENDIENTE")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Firestore snapshot listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val item = doc.toObject(PeticionMesa::class.java)
                        item?.copy(id = doc.id)
                    }
                    _peticiones.value = list
                }
            }
    }

    private fun setupLocalFallback() {
        // Keep in-memory live list if Firebase config is not yet supplied by user
        _peticiones.value = localBackup.filter { it.estado == "PENDIENTE" }
    }

    fun aceptarPeticion(id: String) {
        val db = firestore
        if (db != null) {
            db.collection("peticiones_rivera").document(id)
                .update("estado", "EN_COLA")
                .addOnFailureListener { e ->
                    Log.e(tag, "Error aceptando petición en Firestore", e)
                    updateLocalState(id, "EN_COLA")
                }
        } else {
            updateLocalState(id, "EN_COLA")
        }
    }

    fun rechazarPeticion(id: String) {
        val db = firestore
        if (db != null) {
            db.collection("peticiones_rivera").document(id)
                .update("estado", "RECHAZADA")
                .addOnFailureListener { e ->
                    Log.e(tag, "Error rechazando petición en Firestore", e)
                    updateLocalState(id, "RECHAZADA")
                }
        } else {
            updateLocalState(id, "RECHAZADA")
        }
    }

    fun enviarPeticion(
        mesa: String,
        cancion: String,
        artista: String,
        dedicatoria: String,
        emocion: EmocionVoz
    ) {
        val nuevaPeticion = PeticionMesa(
            id = System.currentTimeMillis().toString(),
            mesa = mesa,
            cancion = cancion,
            artista = artista,
            dedicatoria = dedicatoria,
            emocion = emocion.name,
            estado = "PENDIENTE"
        )

        val db = firestore
        if (db != null) {
            val docData = hashMapOf(
                "mesa" to nuevaPeticion.mesa,
                "cancion" to nuevaPeticion.cancion,
                "artista" to nuevaPeticion.artista,
                "dedicatoria" to nuevaPeticion.dedicatoria,
                "emocion" to nuevaPeticion.emocion,
                "estado" to "PENDIENTE",
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("peticiones_rivera").add(docData)
                .addOnSuccessListener { docRef ->
                    Log.d(tag, "Petición agregada con ID: ${docRef.id}")
                }
                .addOnFailureListener {
                    localBackup.add(nuevaPeticion)
                    _peticiones.value = localBackup.filter { it.estado == "PENDIENTE" }
                }
        } else {
            localBackup.add(nuevaPeticion)
            _peticiones.value = localBackup.filter { it.estado == "PENDIENTE" }
        }
    }

    private fun updateLocalState(id: String, nuevoEstado: String) {
        val index = localBackup.indexOfFirst { it.id == id }
        if (index != -1) {
            localBackup[index] = localBackup[index].copy(estado = nuevoEstado)
        }
        val current = _peticiones.value.toMutableList()
        val pos = current.indexOfFirst { it.id == id }
        if (pos != -1) {
            current.removeAt(pos)
            _peticiones.value = current
        }
    }

    fun release() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
