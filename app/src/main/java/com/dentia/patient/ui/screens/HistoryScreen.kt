package com.dentia.patient.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.dentia.patient.data.model.Appointment
import com.dentia.patient.data.model.Prescription
import com.dentia.patient.ui.components.DentiaCard
import com.dentia.patient.ui.components.DentiaEmptyState
import com.dentia.patient.ui.components.DentiaErrorState
import com.dentia.patient.ui.components.DentiaLoadingState
import com.dentia.patient.ui.components.ScreenHeader
import com.dentia.patient.ui.theme.DentiaMuted
import com.dentia.patient.ui.theme.DentiaPrimary
import java.io.File
import java.time.format.DateTimeFormatter
import com.dentia.patient.ui.patient.PatientUiState

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues,
    state: PatientUiState,
    onBack: () -> Unit,
    onLoadPrescriptions: (String) -> Unit,
    onDownloadPrescription: (Prescription, (File) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var openError by remember { mutableStateOf<String?>(null) }

    val completedAppointments = state.appointments
        .filter { it.status == "COMPLETED" }
        .sortedByDescending(Appointment::startAt)

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
        TextButton(onClick = onBack) {
            Text("‹ Volver")
        }

        ScreenHeader(
            eyebrow = "Expediente del paciente",
            title = "Historial clínico",
            subtitle = "Consulta tus citas atendidas y las recetas emitidas por tus dentistas.",
        )

        openError?.let {
            DentiaErrorState(message = it)
        }

        when {
            state.loadingAppointments -> {
                DentiaLoadingState(
                    message = "Cargando historial clínico...",
                )
            }

            state.errorMessage != null && state.appointments.isEmpty() -> {
                DentiaErrorState(
                    message = state.errorMessage,
                )
            }

            completedAppointments.isEmpty() -> {
                DentiaEmptyState(
                    title = "Todavía no tienes historial clínico",
                    message = "Cuando un dentista marque una cita como completada, aparecerá aquí junto con sus recetas.",
                )
            }

            else -> {
                completedAppointments.forEach { appointment ->
                    HistoryAppointmentCard(
                        appointment = appointment,
                        dentistName = dentistNames[appointment.dentistId] ?: "Dentista",
                        prescriptions = state.prescriptionsByAppointment[appointment.id],
                        isLoading = state.loadingPrescriptionsFor == appointment.id,
                        isDownloading = state.submitting,
                        onLoadPrescriptions = { onLoadPrescriptions(appointment.id) },
                        onDownloadPrescription = { prescription ->
                            openError = null

                            onDownloadPrescription(prescription) { file ->
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.files",
                                    file,
                                )

                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                try {
                                    context.startActivity(
                                        Intent.createChooser(intent, "Abrir receta"),
                                    )
                                } catch (_: ActivityNotFoundException) {
                                    openError = "No hay una aplicación instalada para abrir archivos PDF."
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryAppointmentCard(
    appointment: Appointment,
    dentistName: String,
    prescriptions: List<Prescription>?,
    isLoading: Boolean,
    isDownloading: Boolean,
    onLoadPrescriptions: () -> Unit,
    onDownloadPrescription: (Prescription) -> Unit,
) {
    DentiaCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                appointment.reason ?: "Cita odontológica",
                style = MaterialTheme.typography.titleLarge,
            )

            Text(
                appointment.startAt.format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm"),
                ),
                color = DentiaPrimary,
                style = MaterialTheme.typography.labelLarge,
            )

            Text(
                "Dentista: $dentistName",
                style = MaterialTheme.typography.titleMedium,
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

            when {
                isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Cargando recetas...",
                            color = DentiaMuted,
                        )
                    }
                }

                prescriptions == null -> {
                    OutlinedButton(
                        onClick = onLoadPrescriptions,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ver recetas")
                    }
                }

                prescriptions.isEmpty() -> {
                    DentiaEmptyState(
                        title = "Sin recetas registradas",
                        message = "Esta cita fue completada, pero no tiene recetas asociadas.",
                    )
                }

                else -> {
                    prescriptions.forEach { prescription ->
                        PrescriptionCard(
                            prescription = prescription,
                            isDownloading = isDownloading,
                            onDownload = { onDownloadPrescription(prescription) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrescriptionCard(
    prescription: Prescription,
    isDownloading: Boolean,
    onDownload: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.large,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Receta médica",
            color = DentiaPrimary,
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            "Diagnóstico",
            color = DentiaMuted,
            style = MaterialTheme.typography.labelLarge,
        )

        Text(
            prescription.diagnosis,
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            "Indicaciones",
            color = DentiaMuted,
            style = MaterialTheme.typography.labelLarge,
        )

        Text(
            prescription.indications,
            style = MaterialTheme.typography.bodyLarge,
        )

        prescription.notes
            ?.takeIf(String::isNotBlank)
            ?.let {
                Text(
                    "Notas",
                    color = DentiaMuted,
                    style = MaterialTheme.typography.labelLarge,
                )

                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

        OutlinedButton(
            onClick = onDownload,
            enabled = !isDownloading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (isDownloading) {
                    "Descargando..."
                } else {
                    "Abrir PDF"
                },
            )
        }
    }
}