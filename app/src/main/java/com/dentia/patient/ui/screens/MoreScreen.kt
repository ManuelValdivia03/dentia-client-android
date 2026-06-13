package com.dentia.patient.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dentia.patient.R
import com.dentia.patient.ui.components.DentiaCard
import com.dentia.patient.ui.components.MenuRow
import com.dentia.patient.ui.components.ScreenHeader

@Composable
fun MoreScreen(
    contentPadding: PaddingValues,
    patientName: String,
    patientEmail: String,
    onHistory: () -> Unit,
    onClinicalFiles: () -> Unit,
    onClinicalRecord: () -> Unit,
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
            subtitle = "Expediente, perfil y configuración.",
        )

        DentiaCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    patientName,
                    style = MaterialTheme.typography.titleLarge,
                )

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
                    symbol = "E",
                    icon = Icons.Rounded.Assignment,
                    title = "Expediente clínico",
                    description = "Antecedentes y consultas registradas",
                    onClick = onClinicalRecord,
                )

                MenuRow(
                    symbol = "H",
                    icon = Icons.Rounded.History,
                    title = "Historial clínico",
                    description = "Citas atendidas y recetas",
                    onClick = onHistory,
                )

                MenuRow(
                    symbol = "A",
                    icon = Icons.Rounded.FolderOpen,
                    title = "Archivos clínicos",
                    description = "Estudios, imágenes y documentos",
                    onClick = onClinicalFiles,
                )

                MenuRow(
                    symbol = "DR",
                    iconResId = R.drawable.dentistry,
                    title = "Dentistas",
                    description = "Directorio, perfiles y horarios",
                    onClick = onDentists,
                )

                MenuRow(
                    symbol = "P",
                    icon = Icons.Rounded.AccountCircle,
                    title = "Mi perfil",
                    description = "Datos personales y fotografía",
                    onClick = onProfile,
                )

                MenuRow(
                    symbol = "SALIR",
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    title = "Cerrar sesión",
                    description = "Salir de Dentia en este dispositivo",
                    onClick = onLogout,
                )
            }
        }
    }
}