package com.dentia.patient.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dentia.patient.ui.components.DentiaCard
import com.dentia.patient.ui.components.MenuRow
import com.dentia.patient.ui.components.ScreenHeader

@Composable
fun MoreScreen(
    contentPadding: PaddingValues,
    patientName: String,
    patientEmail: String,
    onHistory: () -> Unit,
    onDentists: () -> Unit,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
) {
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
            eyebrow = "Tu cuenta",
            title = "Más",
            subtitle = "Historial, comunicación y configuración.",
        )
        DentiaCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(patientName, style = MaterialTheme.typography.titleLarge)
                Text(
                    patientEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Paciente",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        DentiaCard {
            Column {
                MenuRow(
                    "H",
                    "Historial clínico",
                    "Citas atendidas y recetas",
                    onClick = onHistory,
                )
                MenuRow(
                    "DR",
                    "Dentistas",
                    "Directorio, perfiles y horarios",
                    onClick = onDentists,
                )
                MenuRow(
                    "P",
                    "Mi perfil",
                    "Datos personales y fotografía",
                    onClick = onProfile,
                )
                MenuRow(
                    "SALIR",
                    "Cerrar sesión",
                    "Salir de Dentia en este dispositivo",
                    onClick = onLogout,
                )
            }
        }
    }
}
