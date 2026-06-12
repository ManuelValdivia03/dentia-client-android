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
import com.dentia.patient.ui.components.ScreenHeader
import com.dentia.patient.ui.patient.PatientUiState
import com.dentia.patient.ui.theme.DentiaMuted
import com.dentia.patient.ui.theme.DentiaPrimary
import java.io.File
import java.time.format.DateTimeFormatter

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
        TextButton(onClick = onBack) { Text("‹ Volver") }
        ScreenHeader(
            eyebrow = "Expediente del paciente",
            title = "Historial clínico",
            subtitle = "Consulta tus citas atendidas y las recetas asociadas.",
        )

        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        openError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (state.loadingAppointments) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (completedAppointments.isEmpty()) {
            DentiaCard {
                Text("Todavía no tienes citas completadas.", color = DentiaMuted)
            }
        } else {
            completedAppointments.forEach { appointment ->
                HistoryAppointmentCard(
                    appointment = appointment,
                    dentistName = dentistNames[appointment.dentistId] ?: "Dentista",
                    prescriptions = state.prescriptionsByAppointment[appointment.id],
                    isLoading = state.loadingPrescriptionsFor == appointment.id,
                    isDownloading = state.submitting,
                    onLoadPrescriptions = { onLoadPrescriptions(appointment.id) },
                    onDownloadPrescription = { prescription ->
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
            Text(dentistName, style = MaterialTheme.typography.titleMedium)

            when {
                isLoading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator()
                    Text("Cargando recetas...")
                }
                prescriptions == null -> OutlinedButton(onClick = onLoadPrescriptions) {
                    Text("Ver recetas")
                }
                prescriptions.isEmpty() -> Text(
                    "Esta cita no tiene recetas registradas.",
                    color = DentiaMuted,
                )
                else -> prescriptions.forEach { prescription ->
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Diagnóstico", color = DentiaMuted, style = MaterialTheme.typography.labelLarge)
        Text(prescription.diagnosis)
        Text("Indicaciones", color = DentiaMuted, style = MaterialTheme.typography.labelLarge)
        Text(prescription.indications)
        prescription.notes?.let {
            Text("Notas", color = DentiaMuted, style = MaterialTheme.typography.labelLarge)
            Text(it)
        }
        OutlinedButton(
            onClick = onDownload,
            enabled = !isDownloading,
        ) {
            Text(if (isDownloading) "Descargando..." else "Abrir PDF")
        }
    }
}
