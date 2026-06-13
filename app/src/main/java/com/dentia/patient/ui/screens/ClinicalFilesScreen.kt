package com.dentia.patient.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.dentia.patient.data.model.ClinicalFile
import com.dentia.patient.ui.components.DentiaCard
import com.dentia.patient.ui.components.DentiaEmptyState
import com.dentia.patient.ui.components.DentiaErrorState
import com.dentia.patient.ui.components.DentiaLoadingState
import com.dentia.patient.ui.components.PrimaryAction
import com.dentia.patient.ui.components.ScreenHeader
import com.dentia.patient.ui.theme.DentiaMuted
import com.dentia.patient.ui.theme.DentiaPrimary
import com.dentia.patient.ui.patient.PatientUiState
import java.io.File
import java.time.format.DateTimeFormatter

@Composable
fun ClinicalFilesScreen(
    contentPadding: PaddingValues,
    state: PatientUiState,
    onBack: () -> Unit,
    onLoad: () -> Unit,
    onUpload: (Uri) -> Unit,
    onDownload: (ClinicalFile, (File) -> Unit) -> Unit,
    onDelete: (String) -> Unit,
) {
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<ClinicalFile?>(null) }
    var openError by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            openError = null
            onUpload(it)
        }
    }

    LaunchedEffect(Unit) {
        onLoad()
    }

    pendingDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar archivo") },
            text = {
                Text(
                    "Se eliminará \"${file.originalName}\" de tu expediente. Esta acción no se puede revertir.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(file.id)
                        pendingDelete = null
                    },
                    enabled = state.fileOperationId != file.id,
                ) {
                    Text(
                        "Eliminar",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar")
                }
            },
        )
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
        TextButton(onClick = onBack) {
            Text("‹ Volver")
        }

        ScreenHeader(
            eyebrow = "Expediente del paciente",
            title = "Archivos clínicos",
            subtitle = "Guarda estudios, imágenes, documentos y videos relacionados con tu atención.",
        )

        DentiaCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Subir nuevo archivo",
                    style = MaterialTheme.typography.titleLarge,
                )

                Text(
                    "Formatos permitidos: PDF, JPG, PNG, WEBP, MP4 o WEBM. Máximo 10 MB.",
                    color = DentiaMuted,
                )

                PrimaryAction(
                    text = if (state.fileOperationId == "upload") {
                        "Subiendo..."
                    } else {
                        "Seleccionar archivo"
                    },
                    onClick = {
                        openError = null
                        filePicker.launch(
                            arrayOf(
                                "application/pdf",
                                "image/jpeg",
                                "image/png",
                                "image/webp",
                                "video/mp4",
                                "video/webm",
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.fileOperationId != "upload",
                )
            }
        }

        state.successMessage?.let {
            DentiaCard {
                Text(
                    text = it,
                    color = DentiaPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        openError?.let {
            DentiaErrorState(message = it)
        }

        when {
            state.loadingFiles -> {
                DentiaLoadingState(
                    message = "Cargando archivos clínicos...",
                )
            }

            state.errorMessage != null && state.clinicalFiles.isEmpty() -> {
                DentiaErrorState(
                    message = state.errorMessage,
                    onRetry = onLoad,
                )
            }

            state.clinicalFiles.isEmpty() -> {
                DentiaEmptyState(
                    title = "Aún no hay archivos",
                    message = "Puedes subir radiografías, estudios, documentos o imágenes relacionadas con tu atención dental.",
                    actionText = "Seleccionar archivo",
                    onAction = {
                        openError = null
                        filePicker.launch(
                            arrayOf(
                                "application/pdf",
                                "image/jpeg",
                                "image/png",
                                "image/webp",
                                "video/mp4",
                                "video/webm",
                            ),
                        )
                    },
                )
            }

            else -> {
                state.clinicalFiles.forEach { clinicalFile ->
                    ClinicalFileCard(
                        clinicalFile = clinicalFile,
                        processing = state.fileOperationId == clinicalFile.id,
                        onOpen = {
                            openError = null

                            onDownload(clinicalFile) { localFile ->
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.files",
                                    localFile,
                                )

                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, clinicalFile.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                try {
                                    context.startActivity(
                                        Intent.createChooser(intent, "Abrir archivo"),
                                    )
                                } catch (_: ActivityNotFoundException) {
                                    openError = "No hay una aplicación instalada para abrir este archivo."
                                }
                            }
                        },
                        onDelete = { pendingDelete = clinicalFile },
                    )
                }
            }
        }
    }
}

@Composable
private fun ClinicalFileCard(
    clinicalFile: ClinicalFile,
    processing: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    DentiaCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                clinicalFile.originalName,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                fileTypeLabel(clinicalFile.mimeType),
                color = DentiaPrimary,
                style = MaterialTheme.typography.labelLarge,
            )

            Text(
                listOfNotNull(
                    formatFileSize(clinicalFile.size),
                    clinicalFile.createdAt?.format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy"),
                    ),
                ).joinToString(" · "),
                color = DentiaMuted,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onOpen,
                    enabled = !processing,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (processing) {
                            "Procesando..."
                        } else {
                            "Abrir"
                        },
                    )
                }

                TextButton(
                    onClick = onDelete,
                    enabled = !processing,
                ) {
                    Text(
                        "Eliminar",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun fileTypeLabel(mimeType: String): String = when {
    mimeType == "application/pdf" -> "Documento PDF"
    mimeType.startsWith("image/") -> "Imagen clínica"
    mimeType.startsWith("video/") -> "Video clínico"
    else -> "Archivo clínico"
}