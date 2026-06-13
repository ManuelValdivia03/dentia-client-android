package com.dentia.patient.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dentia.patient.data.model.ClinicalEncounter
import com.dentia.patient.data.model.ClinicalRecord
import com.dentia.patient.ui.patient.ClinicalRecordViewModel

@Composable
fun ClinicalRecordScreen(
    contentPadding: PaddingValues,
    viewModel: ClinicalRecordViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.loadClinicalRecord()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        OutlinedButton(onClick = onBack) {
            Text("Volver")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Expediente clínico",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading -> {
                CircularProgressIndicator()
            }

            state.error != null -> {
                Text(
                    text = state.error ?: "Error desconocido",
                    color = MaterialTheme.colorScheme.error,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = viewModel::retry) {
                    Text("Reintentar")
                }
            }

            state.record != null -> {
                ClinicalRecordContent(record = state.record!!)
            }

            else -> {
                Text("No hay información clínica para mostrar.")
            }
        }
    }
}

@Composable
private fun ClinicalRecordContent(record: ClinicalRecord) {
    Text(
        text = "Antecedentes",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )

    Spacer(modifier = Modifier.height(8.dp))

    InfoCard("Tipo de sangre", record.bloodType)
    InfoCard("Alergias", record.allergies)
    InfoCard("Enfermedades crónicas", record.chronicDiseases)
    InfoCard("Medicamentos actuales", record.currentMedications)
    InfoCard("Antecedentes quirúrgicos", record.surgicalHistory)
    InfoCard("Antecedentes familiares", record.familyHistory)
    InfoCard("Antecedentes dentales", record.dentalHistory)
    InfoCard("Notas de riesgo", record.riskNotes)

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Consultas clínicas",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (record.encounters.isEmpty()) {
        Text("Aún no hay consultas clínicas registradas.")
    } else {
        record.encounters.forEach { encounter ->
            EncounterCard(encounter)
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = safeValue(value),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EncounterCard(encounter: ClinicalEncounter) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = encounter.reasonForVisit ?: "Consulta clínica",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text("Diagnóstico: ${safeValue(encounter.diagnosis)}")
            Text("Síntomas: ${safeValue(encounter.symptoms)}")
            Text("Tratamiento: ${safeValue(encounter.treatmentPerformed)}")
            Text("Plan: ${safeValue(encounter.treatmentPlan)}")
            Text("Observaciones: ${safeValue(encounter.observations)}")
        }
    }
}

private fun safeValue(value: String?): String {
    return value?.takeIf { it.isNotBlank() } ?: "Sin registrar"
}