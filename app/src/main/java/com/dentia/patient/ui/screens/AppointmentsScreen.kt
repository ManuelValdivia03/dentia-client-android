package com.dentia.patient.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dentia.patient.data.model.Appointment
import com.dentia.patient.data.model.AvailabilitySlot
import com.dentia.patient.ui.components.DentiaCard
import com.dentia.patient.ui.components.DentiaEmptyState
import com.dentia.patient.ui.components.DentiaErrorState
import com.dentia.patient.ui.components.DentiaLoadingState
import com.dentia.patient.ui.components.ScreenHeader
import com.dentia.patient.ui.components.StatusPill
import com.dentia.patient.ui.patient.PatientUiState
import com.dentia.patient.ui.theme.DentiaMuted
import com.dentia.patient.ui.theme.DentiaPrimary
import com.dentia.patient.ui.theme.DentiaSuccess
import com.dentia.patient.ui.theme.DentiaWarning
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun AppointmentsScreen(
    contentPadding: PaddingValues,
    state: PatientUiState,
    onRetry: () -> Unit,
    onCancel: (String) -> Unit,
    onOpenReschedule: () -> Unit,
    onLoadAvailability: (String, String) -> Unit,
    onReschedule: (Appointment, AvailabilitySlot, () -> Unit) -> Unit,
    onRate: (Appointment, Int, String, () -> Unit) -> Unit,
) {
    val filters = listOf(
        "Próximas",
        "Pendientes",
        "Confirmadas",
        "Completadas",
        "Canceladas",
        "Todas",
    )

    var selectedFilter by remember { mutableStateOf(filters.first()) }
    var cancelTarget by remember { mutableStateOf<Appointment?>(null) }
    var rescheduleTarget by remember { mutableStateOf<Appointment?>(null) }
    var ratingTarget by remember { mutableStateOf<Appointment?>(null) }

    val now = OffsetDateTime.now()

    val filtered = state.appointments.filter { appointment ->
        when (selectedFilter) {
            "Próximas" -> appointment.startAt.isAfter(now) &&
                    appointment.status in setOf("PENDING", "CONFIRMED")

            "Pendientes" -> appointment.status == "PENDING"
            "Confirmadas" -> appointment.status == "CONFIRMED"
            "Completadas" -> appointment.status == "COMPLETED"
            "Canceladas" -> appointment.status == "CANCELLED"
            else -> true
        }
    }

    val dentistNames = state.dentists.associate { it.domainId to it.fullName }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader(
            eyebrow = "Paciente",
            title = "Mis citas",
            subtitle = "Consulta, cancela o reprograma tus visitas odontológicas.",
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            filters.chunked(2).forEach { rowFilters ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowFilters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        state.successMessage?.let {
            DentiaCard {
                Text(
                    text = it,
                    color = DentiaSuccess,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        when {
            state.loadingAppointments -> {
                DentiaLoadingState(
                    message = "Cargando tus citas...",
                )
            }

            state.errorMessage != null && state.appointments.isEmpty() -> {
                DentiaErrorState(
                    message = state.errorMessage,
                    onRetry = onRetry,
                )
            }

            state.appointments.isEmpty() -> {
                DentiaEmptyState(
                    title = "Aún no tienes citas",
                    message = "Cuando solicites una cita con un dentista, aparecerá aquí.",
                )
            }

            filtered.isEmpty() -> {
                DentiaEmptyState(
                    title = "No hay citas en esta categoría",
                    message = "Prueba con otro filtro para ver el resto de tus citas.",
                    actionText = "Ver todas",
                    onAction = { selectedFilter = "Todas" },
                )
            }

            else -> {
                filtered.forEach { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        dentistName = dentistNames[appointment.dentistId] ?: "Dentista",
                        onCancel = { cancelTarget = appointment },
                        onReschedule = {
                            if (appointment.status == "PENDING") {
                                onOpenReschedule()
                                rescheduleTarget = appointment
                            }
                        },
                        onRate = { ratingTarget = appointment },
                    )
                }
            }
        }
    }

    cancelTarget?.let { appointment ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Cancelar cita") },
            text = {
                Text(
                    "¿Deseas cancelar esta cita? Esta acción no se puede revertir.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        cancelTarget = null
                        onCancel(appointment.id)
                    },
                    enabled = !state.submitting,
                ) {
                    Text(
                        "Cancelar cita",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) {
                    Text("Volver")
                }
            },
        )
    }

    rescheduleTarget?.let { appointment ->
        RescheduleDialog(
            appointment = appointment,
            state = state,
            onDismiss = { rescheduleTarget = null },
            onLoadAvailability = { date ->
                onLoadAvailability(appointment.dentistId, date)
            },
            onSubmit = { slot ->
                onReschedule(appointment, slot) {
                    rescheduleTarget = null
                }
            },
        )
    }

    ratingTarget?.let { appointment ->
        RatingDialog(
            appointment = appointment,
            state = state,
            onDismiss = { ratingTarget = null },
            onSubmit = { score, comment ->
                onRate(appointment, score, comment) {
                    ratingTarget = null
                }
            },
        )
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    dentistName: String,
    onCancel: () -> Unit,
    onReschedule: () -> Unit,
    onRate: () -> Unit,
) {
    val statusColor = when (appointment.status) {
        "CONFIRMED", "COMPLETED" -> DentiaSuccess
        "PENDING" -> DentiaWarning
        "CANCELLED" -> MaterialTheme.colorScheme.error
        else -> Color.Gray
    }

    val statusLabel = when (appointment.status) {
        "CONFIRMED" -> "Confirmada"
        "COMPLETED" -> "Completada"
        "PENDING" -> "Pendiente"
        "CANCELLED" -> "Cancelada"
        else -> "Estado no disponible"
    }

    val canCancel = appointment.status in setOf("PENDING", "CONFIRMED") &&
            appointment.startAt.isAfter(OffsetDateTime.now())

    val canReschedule = appointment.status == "PENDING" &&
            appointment.startAt.isAfter(OffsetDateTime.now())

    val isConfirmedFuture = appointment.status == "CONFIRMED" &&
            appointment.startAt.isAfter(OffsetDateTime.now())

    val canRate = appointment.status == "COMPLETED" && !appointment.hasRating

    DentiaCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        appointment.startAt.format(
                            DateTimeFormatter.ofPattern("dd MMM"),
                        ),
                        color = DentiaPrimary,
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Text(
                        appointment.startAt.format(
                            DateTimeFormatter.ofPattern("HH:mm"),
                        ),
                        color = DentiaMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                StatusPill(statusLabel, statusColor)
            }

            Text(
                appointment.reason ?: "Cita odontológica",
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                appointment.startAt.format(
                    DateTimeFormatter.ofPattern("EEEE d 'de' MMMM · HH:mm"),
                ),
                color = DentiaMuted,
            )

            Text(
                "Dentista: $dentistName",
                style = MaterialTheme.typography.labelLarge,
            )

            appointment.notes
                ?.takeIf(String::isNotBlank)
                ?.let {
                    Text(
                        it,
                        color = DentiaMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

            if (isConfirmedFuture) {
                DentiaCard {
                    Text(
                        "Horario confirmado. Para cambiarlo, cancela y solicita una nueva cita.",
                        color = DentiaMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (canCancel || canRate || canReschedule) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canReschedule) {
                        OutlinedButton(onClick = onReschedule) {
                            Text("Reprogramar")
                        }
                    }

                    if (canRate) {
                        OutlinedButton(onClick = onRate) {
                            Text("Valorar")
                        }
                    }

                    if (canCancel) {
                        TextButton(onClick = onCancel) {
                            Text(
                                "Cancelar",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RescheduleDialog(
    appointment: Appointment,
    state: PatientUiState,
    onDismiss: () -> Unit,
    onLoadAvailability: (String) -> Unit,
    onSubmit: (AvailabilitySlot) -> Unit,
) {
    var date by remember {
        mutableStateOf(LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE))
    }

    var selectedSlot by remember { mutableStateOf<AvailabilitySlot?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reprogramar cita") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    appointment.reason ?: "Cita odontológica",
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = {
                        date = it
                        selectedSlot = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nueva fecha (AAAA-MM-DD)") },
                    singleLine = true,
                )

                OutlinedButton(
                    onClick = { onLoadAvailability(date) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = date.length == 10 && !state.loadingAvailability,
                ) {
                    Text(
                        if (state.loadingAvailability) {
                            "Consultando..."
                        } else {
                            "Ver horarios"
                        },
                    )
                }

                state.availability.forEach { slot ->
                    FilterChip(
                        selected = selectedSlot == slot,
                        onClick = { selectedSlot = slot },
                        label = {
                            Text(
                                slot.startAt.format(
                                    DateTimeFormatter.ofPattern("HH:mm"),
                                ),
                            )
                        },
                    )
                }

                if (state.availability.isEmpty() && !state.loadingAvailability) {
                    Text(
                        "Consulta una fecha para elegir horario.",
                        color = DentiaMuted,
                    )
                }

                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedSlot?.let(onSubmit) },
                enabled = selectedSlot != null && !state.submitting,
            ) {
                Text(
                    if (state.submitting) {
                        "Guardando..."
                    } else {
                        "Guardar cambio"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
    )
}

@Composable
private fun RatingDialog(
    appointment: Appointment,
    state: PatientUiState,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit,
) {
    var score by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Valora tu atención") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    appointment.reason ?: "Cita odontológica",
                    style = MaterialTheme.typography.titleMedium,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { value ->
                        FilterChip(
                            selected = score == value,
                            onClick = { score = value },
                            label = { Text("$value") },
                        )
                    }
                }

                Text("$score de 5", color = DentiaPrimary)

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Comentario opcional") },
                    minLines = 3,
                )

                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(score, comment) },
                enabled = !state.submitting,
            ) {
                Text(
                    if (state.submitting) {
                        "Enviando..."
                    } else {
                        "Enviar valoración"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}