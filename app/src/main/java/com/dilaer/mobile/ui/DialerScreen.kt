package com.dilaer.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dilaer.mobile.QueueState
import com.dilaer.mobile.UiState
import com.dilaer.mobile.data.CallStatus
import com.dilaer.mobile.data.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialerScreen(
    state: UiState,
    onImportClick: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    onCooldownChange: (Int) -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dilaer", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            StatusCard(state)
            ControlsCard(
                state = state,
                onImportClick = onImportClick,
                onStart = onStart,
                onPause = onPause,
                onStop = onStop,
                onSkip = onSkip,
                onReset = onReset,
                onCooldownChange = onCooldownChange,
            )
            HorizontalDivider()
            ContactsList(state)
        }
    }
}

@Composable
private fun StatusCard(state: UiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(stateColor(state.queueState), CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(stateLabel(state.queueState), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Metric("Total", state.contacts.size)
                Metric("Llamados", state.totalCalled)
                Metric("Pendientes", state.totalPending)
                Metric("Saltados", state.totalSkipped)
            }
            if (state.activeIndex in state.contacts.indices) {
                Spacer(Modifier.height(12.dp))
                val c = state.contacts[state.activeIndex]
                Text(
                    "Marcando: ${c.name.ifBlank { c.phone }}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (c.name.isNotBlank()) {
                    Text(c.phone, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ControlsCard(
    state: UiState,
    onImportClick: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    onCooldownChange: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Importar CSV")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val running = state.queueState == QueueState.RUNNING
                Button(
                    onClick = if (running) onPause else onStart,
                    modifier = Modifier.weight(1f),
                    enabled = state.contacts.isNotEmpty() && state.queueState != QueueState.FINISHED,
                ) {
                    Icon(
                        if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when (state.queueState) {
                            QueueState.RUNNING -> "Pausar"
                            QueueState.PAUSED -> "Reanudar"
                            QueueState.FINISHED -> "Terminado"
                            else -> "Iniciar"
                        }
                    )
                }
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    enabled = state.queueState == QueueState.RUNNING || state.queueState == QueueState.PAUSED,
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Saltar")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    enabled = state.queueState != QueueState.IDLE,
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Detener")
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    enabled = state.contacts.isNotEmpty(),
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Reiniciar lista")
                }
            }

            Column {
                Text(
                    "Espera entre llamadas: ${state.cooldownSeconds}s",
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = state.cooldownSeconds.toFloat(),
                    onValueChange = { onCooldownChange(it.toInt()) },
                    valueRange = 2f..30f,
                    steps = 27,
                )
                Text(
                    "Recomendado: 4–6 s para evitar bloqueos por la operadora.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun ContactsList(state: UiState) {
    if (state.contacts.isEmpty()) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Importa un CSV (nombre, número) para empezar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(state.activeIndex) {
        if (state.activeIndex >= 0) {
            listState.animateScrollToItem(state.activeIndex.coerceAtMost(state.contacts.lastIndex))
        }
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(state.contacts, key = { it.id }) { contact ->
            ContactRow(contact, isActive = contact.id == state.contacts.getOrNull(state.activeIndex)?.id)
        }
    }
}

@Composable
private fun ContactRow(contact: Contact, isActive: Boolean) {
    val container = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer
        contact.status == CallStatus.CALLED -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusBadge(contact.status, isActive)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    contact.name.ifBlank { contact.phone },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (contact.name.isNotBlank()) {
                    Text(
                        contact.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: CallStatus, isActive: Boolean) {
    val (icon, tint) = when {
        isActive -> Icons.Default.Call to MaterialTheme.colorScheme.primary
        status == CallStatus.CALLED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        status == CallStatus.SKIPPED -> Icons.Default.SkipNext to MaterialTheme.colorScheme.secondary
        else -> Icons.Default.Call to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
}

private fun stateLabel(state: QueueState): String = when (state) {
    QueueState.IDLE -> "Listo"
    QueueState.RUNNING -> "Marcando"
    QueueState.PAUSED -> "En pausa"
    QueueState.FINISHED -> "Campaña finalizada"
}

private fun stateColor(state: QueueState): Color = when (state) {
    QueueState.IDLE -> Color(0xFF9AA0A6)
    QueueState.RUNNING -> Color(0xFF1D7A46)
    QueueState.PAUSED -> Color(0xFFE8A317)
    QueueState.FINISHED -> Color(0xFF2D5A7B)
}
