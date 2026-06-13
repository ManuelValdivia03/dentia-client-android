package com.dentia.patient.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dentia.patient.data.model.AvailabilitySlot
import com.dentia.patient.data.model.Dentist
import com.dentia.patient.ui.components.DentiaCard
import com.dentia.patient.ui.components.DentiaEmptyState
import com.dentia.patient.ui.components.DentiaErrorState
import com.dentia.patient.ui.components.DentiaLoadingState
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.ZoneOffset
import androidx.compose.material3.SelectableDates
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

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

    val dentists = state.dentists.filter { dentist ->
        val matchesSearch = search.isBlank() ||
                dentist.fullName.contains(search, ignoreCase = true) ||
                dentist.specialty.orEmpty().contains(search, ignoreCase = true)

        val matchesSpecialty = specialtyFilter == null ||
                dentist.specialty.equals(specialtyFilter, ignoreCase = true)

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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Especialidad",
                    style = MaterialTheme.typography.titleMedium,
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = specialtyFilter == null,
                            onClick = { specialtyFilter = null },
                            label = { Text("Todas") },
                        )
                    }

                    items(specialties) { specialty ->
                        FilterChip(
                            selected = specialtyFilter == specialty,
                            onClick = { specialtyFilter = specialty },
                            label = { Text(specialty) },
                        )
                    }
                }
            }
        }

        when {
            state.loadingDentists -> {
                DentiaLoadingState(
                    message = "Cargando dentistas disponibles...",
                )
            }

            state.errorMessage != null && state.dentists.isEmpty() -> {
                DentiaErrorState(
                    message = state.errorMessage,
                    onRetry = onRetry,
                )
            }

            state.dentists.isEmpty() -> {
                DentiaEmptyState(
                    title = "No hay dentistas disponibles",
                    message = "Por ahora no hay dentistas afiliados para mostrar.",
                    actionText = "Actualizar",
                    onAction = onRetry,
                )
            }

            dentists.isEmpty() -> {
                DentiaEmptyState(
                    title = "Sin resultados",
                    message = "No encontramos dentistas con ese filtro. Intenta buscar por otro nombre o especialidad.",
                    actionText = "Limpiar filtros",
                    onAction = {
                        search = ""
                        specialtyFilter = null
                    },
                )
            }

            else -> {
                dentists.forEach { dentist ->
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
                DentistAvatar(
                    initials = dentist.initials,
                    photoBytes = photoBytes,
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        dentist.fullName,
                        style = MaterialTheme.typography.titleMedium,
                    )

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
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DentiaMuted,
                )
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

                dentist.school?.let {
                    ProfileValue("Escuela", it)
                }

                dentist.email?.let {
                    ProfileValue("Correo", it)
                }

                dentist.description?.let {
                    ProfileValue("Acerca de", it)
                }

                Text(
                    "Opiniones recientes",
                    style = MaterialTheme.typography.titleMedium,
                )

                val ratings = state.ratingSummary?.latestRatings.orEmpty()

                if (!state.loadingRatings && ratings.isEmpty()) {
                    Text(
                        "Este dentista aún no tiene comentarios.",
                        color = DentiaMuted,
                    )
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
                                rating.createdAt.format(
                                    DateTimeFormatter.ofPattern("dd MMM yyyy"),
                                ),
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
            TextButton(onClick = onSchedule) {
                Text("Consultar horarios")
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
private fun ProfileValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            color = DentiaMuted,
            style = MaterialTheme.typography.labelLarge,
        )

        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDialog(
    dentist: Dentist,
    state: PatientUiState,
    onDismiss: () -> Unit,
    onLoadAvailability: (String) -> Unit,
    onSubmit: (AvailabilitySlot, String, String) -> Unit,
) {
    var selectedDate by remember {
        mutableStateOf(LocalDate.now().plusDays(1))
    }

    var loadedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showCalendar by remember { mutableStateOf(false) }
    var selectedSlot by remember { mutableStateOf<AvailabilitySlot?>(null) }
    var reason by remember { mutableStateOf("Consulta odontológica") }
    var notes by remember { mutableStateOf("") }

    val minSelectableDate = LocalDate.now().plusDays(1)
    val selectedDateText = selectedDate.format(DateTimeFormatter.ISO_DATE)
    val displayDate = selectedDate.format(
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM"),
    )

    if (showCalendar) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()

                    return !date.isBefore(minSelectableDate)
                }
            },
        )

        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedDate = datePickerState.selectedDateMillis
                            ?.let { millis ->
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                            }

                        if (pickedDate != null) {
                            selectedDate = pickedDate
                            selectedSlot = null
                            loadedDate = null
                        }

                        showCalendar = false
                    },
                ) {
                    Text("Seleccionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) {
                    Text("Cancelar")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agendar con ${dentist.fullName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "1. Elige el día de tu cita",
                    style = MaterialTheme.typography.titleMedium,
                )

                DentiaCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            displayDate.replaceFirstChar { it.uppercase() },
                            color = DentiaPrimary,
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Text(
                            "Selecciona el día exacto en el calendario.",
                            color = DentiaMuted,
                        )

                        OutlinedButton(
                            onClick = { showCalendar = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Elegir fecha en calendario")
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        selectedSlot = null
                        loadedDate = selectedDate
                        onLoadAvailability(selectedDateText)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedDate.isAfter(LocalDate.now()) &&
                            !state.loadingAvailability,
                ) {
                    Text(
                        if (state.loadingAvailability) {
                            "Consultando horarios..."
                        } else {
                            "Ver horarios disponibles"
                        },
                    )
                }

                Text(
                    "2. Selecciona un horario",
                    style = MaterialTheme.typography.titleMedium,
                )

                when {
                    state.loadingAvailability -> {
                        Text(
                            "Buscando horarios disponibles...",
                            color = DentiaMuted,
                        )
                    }

                    loadedDate != selectedDate -> {
                        Text(
                            "Presiona “Ver horarios disponibles” para consultar esta fecha.",
                            color = DentiaMuted,
                        )
                    }

                    state.availability.isEmpty() -> {
                        Text(
                            "No hay horarios disponibles para esta fecha.",
                            color = DentiaMuted,
                        )
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.availability.chunked(2).forEach { rowSlots ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    rowSlots.forEach { slot ->
                                        val start = slot.startAt.format(
                                            DateTimeFormatter.ofPattern("HH:mm"),
                                        )
                                        val end = slot.endAt.format(
                                            DateTimeFormatter.ofPattern("HH:mm"),
                                        )

                                        FilterChip(
                                            selected = selectedSlot == slot,
                                            onClick = { selectedSlot = slot },
                                            label = { Text("$start - $end") },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }

                                    if (rowSlots.size == 1) {
                                        Row(modifier = Modifier.weight(1f)) {}
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    "3. Motivo de la cita",
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(120) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Motivo de la cita") },
                    placeholder = { Text("Consulta odontológica") },
                    supportingText = {
                        Text("Puedes dejar el motivo predeterminado o ajustarlo si lo necesitas.")
                    },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(300) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notas opcionales") },
                    placeholder = { Text("Ej. Tengo sensibilidad en una muela") },
                    minLines = 2,
                )

                selectedSlot?.let { slot ->
                    val start = slot.startAt.format(DateTimeFormatter.ofPattern("HH:mm"))
                    val end = slot.endAt.format(DateTimeFormatter.ofPattern("HH:mm"))

                    DentiaCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Resumen",
                                style = MaterialTheme.typography.titleMedium,
                            )

                            Text(
                                "${displayDate.replaceFirstChar { it.uppercase() }} · $start - $end",
                                color = DentiaPrimary,
                            )

                            Text(
                                dentist.fullName,
                                color = DentiaMuted,
                            )

                            Text(
                                reason.trim().ifBlank { "Consulta odontológica" },
                                color = DentiaMuted,
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
            TextButton(
                onClick = {
                    selectedSlot?.let { slot ->
                        onSubmit(
                            slot,
                            reason.trim().ifBlank { "Consulta odontológica" },
                            notes,
                        )
                    }
                },
                enabled = selectedSlot != null &&
                        selectedDate.isAfter(LocalDate.now()) &&
                        loadedDate == selectedDate &&
                        !state.submitting,
            ) {
                Text(
                    if (state.submitting) {
                        "Agendando..."
                    } else {
                        "Solicitar cita"
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