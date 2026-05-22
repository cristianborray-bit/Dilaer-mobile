package com.dilaer.mobile

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dilaer.mobile.data.CallStateMonitor
import com.dilaer.mobile.data.CallStatus
import com.dilaer.mobile.data.Contact
import com.dilaer.mobile.data.CsvImporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QueueState { IDLE, RUNNING, PAUSED, FINISHED }

data class UiState(
    val contacts: List<Contact> = emptyList(),
    val activeIndex: Int = -1,
    val queueState: QueueState = QueueState.IDLE,
    val cooldownSeconds: Int = 5,
    val message: String? = null,
) {
    val totalCalled: Int get() = contacts.count { it.status == CallStatus.CALLED }
    val totalSkipped: Int get() = contacts.count { it.status == CallStatus.SKIPPED }
    val totalPending: Int get() = contacts.count { it.status == CallStatus.PENDING }
}

class DialerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val monitor = CallStateMonitor(application).also {
        it.register { onCallEnded() }
    }
    private var cooldownJob: Job? = null

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            CsvImporter.importFromUri(getApplication(), uri).onSuccess { list ->
                cooldownJob?.cancel()
                _uiState.update {
                    it.copy(
                        contacts = list,
                        activeIndex = -1,
                        queueState = if (list.isEmpty()) QueueState.IDLE else QueueState.IDLE,
                        message = if (list.isEmpty()) {
                            "El archivo no contiene números válidos."
                        } else {
                            "Importados ${list.size} contactos."
                        },
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(message = "Error al importar: ${e.message}") }
            }
        }
    }

    fun setCooldown(seconds: Int) {
        _uiState.update { it.copy(cooldownSeconds = seconds.coerceIn(2, 60)) }
    }

    fun start() {
        val state = _uiState.value
        if (state.contacts.isEmpty()) {
            _uiState.update { it.copy(message = "Primero importa una lista CSV.") }
            return
        }
        if (!hasCallPermission()) {
            _uiState.update { it.copy(message = "Falta el permiso para realizar llamadas.") }
            return
        }
        val nextIndex = state.contacts.indexOfFirst { it.status == CallStatus.PENDING }
        if (nextIndex < 0) {
            _uiState.update { it.copy(queueState = QueueState.FINISHED, message = "No quedan contactos pendientes.") }
            return
        }
        _uiState.update { it.copy(queueState = QueueState.RUNNING, message = null) }
        placeCall(nextIndex)
    }

    fun pause() {
        cooldownJob?.cancel()
        _uiState.update {
            if (it.queueState == QueueState.RUNNING) it.copy(queueState = QueueState.PAUSED) else it
        }
    }

    fun stop() {
        cooldownJob?.cancel()
        _uiState.update { it.copy(queueState = QueueState.IDLE, activeIndex = -1) }
    }

    fun skipCurrent() {
        val state = _uiState.value
        val idx = state.activeIndex
        if (idx in state.contacts.indices) {
            updateContactStatus(idx, CallStatus.SKIPPED)
        }
        cooldownJob?.cancel()
        scheduleNext(immediate = true)
    }

    fun resetAll() {
        cooldownJob?.cancel()
        _uiState.update {
            it.copy(
                contacts = it.contacts.map { c -> c.copy(status = CallStatus.PENDING) },
                activeIndex = -1,
                queueState = QueueState.IDLE,
                message = "Lista reiniciada.",
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun placeCall(index: Int) {
        val state = _uiState.value
        val contact = state.contacts.getOrNull(index) ?: return
        _uiState.update { it.copy(activeIndex = index) }

        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:${contact.phone}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            getApplication<Application>().startActivity(intent)
            updateContactStatus(index, CallStatus.CALLED)
        } catch (e: SecurityException) {
            _uiState.update {
                it.copy(
                    queueState = QueueState.PAUSED,
                    message = "Permiso revocado: ${e.message}",
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    queueState = QueueState.PAUSED,
                    message = "No se pudo iniciar la llamada: ${e.message}",
                )
            }
        }
    }

    private fun onCallEnded() {
        if (_uiState.value.queueState != QueueState.RUNNING) return
        scheduleNext(immediate = false)
    }

    private fun scheduleNext(immediate: Boolean) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            if (!immediate) {
                val seconds = _uiState.value.cooldownSeconds
                delay(seconds * 1000L)
            }
            if (_uiState.value.queueState != QueueState.RUNNING) return@launch
            val nextIndex = _uiState.value.contacts.indexOfFirst { it.status == CallStatus.PENDING }
            if (nextIndex < 0) {
                _uiState.update {
                    it.copy(queueState = QueueState.FINISHED, activeIndex = -1, message = "Campaña terminada.")
                }
            } else {
                placeCall(nextIndex)
            }
        }
    }

    private fun updateContactStatus(index: Int, status: CallStatus) {
        _uiState.update { state ->
            val updated = state.contacts.toMutableList().also {
                it[index] = it[index].copy(status = status)
            }
            state.copy(contacts = updated)
        }
    }

    private fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCleared() {
        cooldownJob?.cancel()
        monitor.unregister()
        super.onCleared()
    }
}
