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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dentia.patient.data.model.Appointment
import com.dentia.patient.data.model.Dentist
import com.dentia.patient.ui.components.DentiaCard
import com.dentia.patient.ui.components.DentistAvatar
import com.dentia.patient.ui.components.PrimaryAction
import com.dentia.patient.ui.components.ScreenHeader
import com.dentia.patient.ui.components.StatusPill
import com.dentia.patient.ui.theme.DentiaMuted
import com.dentia.patient.ui.theme.DentiaPrimary
import com.dentia.patient.ui.theme.DentiaSuccess
import com.dentia.patient.ui.theme.DentiaWarning
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    patientName: String,
    appointments: List<Appointment>,
    dentists: List<Dentist>,
    dentistPhotos: Map<String, ByteArray>,
    onOpenDentists: () -> Unit,
    onOpenAppointments: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenClinicalFiles: () -> Unit,
) {
    val now = OffsetDateTime.now()

    val nextAppointment = appointments
        .filter {
            it.startAt.isAfter(now) &&
                    it.status in setOf("PENDING", "CONFIRMED")
        }
        .minByOrNull(Appointment::startAt)

    val completedAppointmentsCount = appointments.count { it.status == "COMPLETED" }
    val pendingAppointmentsCount = appointments.count { it.status == "PENDING" }
    val confirmedAppointmentsCount = appointments.count { it.status == "CONFIRMED" }

    val dentistById = dentists.associateBy(Dentist::domainId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFEAF8F8), MaterialTheme.colorScheme.background),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenHeader(
            eyebrow = "Portal de pacientes",
            title = "Hola, ${patientName.substringBefore(' ')}",
            subtitle = "Agenda, consulta tus citas y mantén tu expediente a la mano.",
        )

        if (nextAppointment == null) {
            DentiaCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Sin próximas citas",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Text(
                        "Busca un dentista disponible y solicita un horario.",
                        color = DentiaMuted,
                    )

                    PrimaryAction(
                        text = "Buscar dentista",
                        onClick = onOpenDentists,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            val dentist = dentistById[nextAppointment.dentistId]

            DentiaCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Próxima cita",
                            style = MaterialTheme.typography.titleLarge,
                        )

                        StatusPill(
                            text = if (nextAppointment.status == "CONFIRMED") {
                                "Confirmada"
                            } else {
                                "Pendiente"
                            },
                            color = if (nextAppointment.status == "CONFIRMED") {
                                DentiaSuccess
                            } else {
                                DentiaWarning
                            },
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        DentistAvatar(
                            initials = dentist?.initials ?: "D",
                            photoBytes = dentist?.domainId?.let(dentistPhotos::get),
                        )

                        Column {
                            Text(
                                dentist?.fullName ?: "Dentista",
                                style = MaterialTheme.typography.titleMedium,
                            )

                            Text(
                                dentist?.specialty ?: "Odontología general",
                                color = DentiaMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    Text(
                        nextAppointment.startAt.format(
                            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM · HH:mm"),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = DentiaPrimary,
                    )

                    Text(
                        nextAppointment.reason ?: "Cita odontológica",
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    OutlinedButton(
                        onClick = onOpenAppointments,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ver mis citas")
                    }
                }
            }
        }

        DentiaCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Resumen de atención",
                    style = MaterialTheme.typography.titleLarge,
                )

                SummaryRow(
                    label = "Pendientes",
                    value = pendingAppointmentsCount.toString(),
                )

                SummaryRow(
                    label = "Confirmadas",
                    value = confirmedAppointmentsCount.toString(),
                )

                SummaryRow(
                    label = "Citas completadas",
                    value = completedAppointmentsCount.toString(),
                )

                SummaryRow(
                    label = "Dentistas disponibles",
                    value = dentists.size.toString(),
                )
            }
        }

        DentiaCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Tu red de atención",
                    style = MaterialTheme.typography.titleLarge,
                )

                Text(
                    "${dentists.size} dentistas disponibles",
                    color = DentiaPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(
                    "Consulta perfiles, valoraciones y horarios reales desde la pestaña Dentistas.",
                    color = DentiaMuted,
                )

                PrimaryAction(
                    text = "Ver dentistas",
                    onClick = onOpenDentists,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = DentiaMuted,
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            value,
            color = DentiaPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}