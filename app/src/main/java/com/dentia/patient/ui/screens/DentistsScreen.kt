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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dentia.patient.data.model.AvailabilitySlot
import com.dentia.patient.data.model.Dentist
import com.dentia.patient.ui.components.DentiaCard
import com.dentia.patient.ui.components.DentistAvatar
import com.dentia.patient.ui.components.PrimaryAction
import com.dentia.patient.ui.components.ScreenHeader
import com.dentia.patient.ui.components.StatusPill
import com.dentia.patient.ui.patient.PatientUiState
import com.dentia.patient.ui.theme.DentiaMuted
import com.dentia.patient.ui.theme.DentiaPrimary
import com.dentia.patient.ui.theme.DentiaSuccess
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DentistsScreen(
    contentPadding: PaddingValues,
    state: PatientUiState,
    onRetry: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onCloseProfile: () -> Unit,
    onLoadAvailability: (String, String) -> Unit,
    onCreateAppointment: (Dentist, AvailabilitySlot, String, String, () -> Unit) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var specialtyFilter by remember { mutableStateOf<String?>(null) }
    var scheduleDentist by remember { mutableStateOf<Dentist?>(null) }
    var profileDentist by remember { mutableStateOf<Dentist?>(null) }
    val specialties = state.dentists
        .mapNotNull { it.specialty?.trim()?.takeIf(String::isNotBlank) }
        .distinct()
        .sorted()
    val dentists = state.dentists.filter {
        val matchesSearch = search.isBlank() ||
            it.fullName.contains(search, ignoreCase = true) ||
            it.specialty.orEmpty().contains(search, ignoreCase = true)
        val matchesSpecialty = specialtyFilter == null ||
            it.specialty.equals(specialtyFilter, ignoreCase = true)
        matchesSearch && matchesSpecialty
    }

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
            eyebrow = "Profesionales certificados",
            title = "Encuentra tu dentista",
            subtitle = "Busca por nombre o especialidad y consulta horarios reales.",
        )
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar dentista") },
            singleLine = true,
        )
        if (specialties.isNotEmpty()) {
            Text("Filtrar por especialidad", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = specialtyFilter == null,
                    onClick = { specialtyFilter = null },
                    label = { Text("Todas las especialidades") },
                )
                specialties.forEach { specialty ->
                    FilterChip(
                        selected = specialtyFilter == specialty,
                        onClick = { specialtyFilter = specialty },
                        label = { Text(specialty) },
                    )
                }
            }
        }

        when {
            state.loadingDentists -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            state.errorMessage != null && state.dentists.isEmpty() -> {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRetry) { Text("Reintentar") }
            }
            dentists.isEmpty() -> Text("No hay dentistas con ese filtro.", color = DentiaMuted)
            else -> dentists.forEach { dentist ->
                DentistCard(
                    dentist = dentist,
                    photoBytes = state.dentistPhotos[dentist.domainId],
                    onProfile = {
                        profileDentist = dentist
                        onOpenProfile(dentist.domainId)
                    },
                    onSchedule = {
                        onOpenSchedule()
                        scheduleDentist = dentist
                    },
                )
            }
        }
    }

    scheduleDentist?.let { dentist ->
        ScheduleDialog(
            dentist = dentist,
            state = state,
            onDismiss = { scheduleDentist = null },
            onLoadAvailability = { date ->
                onLoadAvailability(dentist.domainId, date)
            },
            onSubmit = { slot, reason, notes ->
                onCreateAppointment(dentist, slot, reason, notes) {
                    scheduleDentist = null
                }
            },
        )
    }

    profileDentist?.let { dentist ->
        DentistProfileDialog(
            dentist = dentist,
            photoBytes = state.dentistPhotos[dentist.domainId],
            state = state,
            onDismiss = {
                profileDentist = null
                onCloseProfile()
            },
            onSchedule = {
                profileDentist = null
                onCloseProfile()
                onOpenSchedule()
                scheduleDentist = dentist
            },
        )
    }
}

@Composable
private fun DentistCard(
    dentist: Dentist,
    photoBytes: ByteArray?,
    onProfile: () -> Unit,
    onSchedule: () -> Unit,
) {
    DentiaCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DentistAvatar(dentist.initials, photoBytes = photoBytes)
                Column(modifier = Modifier.weight(1f)) {
                    Text(dentist.fullName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        dentist.specialty ?: "Odontología general",
                        color = DentiaMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (dentist.previouslyVisited) {
                        StatusPill("Ya te atendió", DentiaSuccess)
                    }
                }
            }
            dentist.description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = DentiaMuted)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onProfile,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Ver perfil")
                }
                PrimaryAction(
                    text = "Horarios",
                    onClick = onSchedule,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DentistProfileDialog(
    dentist: Dentist,
    photoBytes: ByteArray?,
    state: PatientUiState,
    onDismiss: () -> Unit,
    onSchedule: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dentist.fullName) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    DentistAvatar(
                        initials = dentist.initials,
                        modifier = Modifier.size(72.dp),
                        photoBytes = photoBytes,
                    )
                    Column {
                        Text(
                            dentist.specialty ?: "Odontología general",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (state.loadingRatings) {
                            Text("Cargando opiniones...", color = DentiaMuted)
                        } else {
                            state.ratingSummary?.let {
                                Text(
                                    "${"%.1f".format(it.averageScore)} / 5 · ${it.totalRatings} valoraciones",
                                    color = DentiaPrimary,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
                dentist.professionalLicense?.let {
                    ProfileValue("Cédula profesional", it)
                }
                dentist.school?.let { ProfileValue("Escuela", it) }
                dentist.email?.let { ProfileValue("Correo", it) }
                dentist.description?.let { ProfileValue("Acerca de", it) }

                Text("Opiniones recientes", style = MaterialTheme.typography.titleMedium)
                val ratings = state.ratingSummary?.latestRatings.orEmpty()
                if (!state.loadingRatings && ratings.isEmpty()) {
                    Text("Este dentista aún no tiene comentarios.", color = DentiaMuted)
                }
                ratings.forEach { rating ->
                    DentiaCard {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "${rating.score} / 5",
                                color = DentiaPrimary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(rating.comment ?: "Sin comentario.")
                            Text(
                                rating.createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                                color = DentiaMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSchedule) { Text("Consultar horarios") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

@Composable
private fun ProfileValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = DentiaMuted, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ScheduleDialog(
    dentist: Dentist,
    state: PatientUiState,
    onDismiss: () -> Unit,
    onLoadAvailability: (String) -> Unit,
    onSubmit: (AvailabilitySlot, String, String) -> Unit,
) {
    var date by remember {
        mutableStateOf(LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE))
    }
    var selectedSlot by remember { mutableStateOf<AvailabilitySlot?>(null) }
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agendar con ${dentist.fullName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = {
                        date = it
                        selectedSlot = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Fecha (AAAA-MM-DD)") },
                    singleLine = true,
                )
                OutlinedButton(
                    onClick = { onLoadAvailability(date) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loadingAvailability && date.length == 10,
                ) {
                    Text(if (state.loadingAvailability) "Consultando..." else "Ver horarios")
                }
                if (state.availability.isEmpty() && !state.loadingAvailability) {
                    Text("Selecciona una fecha para consultar horarios.", color = DentiaMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.availability.forEach { slot ->
                            val label = slot.startAt.format(DateTimeFormatter.ofPattern("HH:mm"))
                            FilterChip(
                                selected = selectedSlot == slot,
                                onClick = { selectedSlot = slot },
                                label = { Text(label) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Motivo") },
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notas opcionales") },
                    minLines = 2,
                )
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedSlot?.let { onSubmit(it, reason, notes) } },
                enabled = selectedSlot != null && !state.submitting,
            ) {
                Text(if (state.submitting) "Agendando..." else "Solicitar cita")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
