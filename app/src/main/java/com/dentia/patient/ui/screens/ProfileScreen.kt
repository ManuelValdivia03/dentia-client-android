package com.dentia.patient.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dentia.patient.data.model.AuthUser
import com.dentia.patient.ui.components.DentiaCard
import com.dentia.patient.ui.components.DentiaErrorState
import com.dentia.patient.ui.components.PrimaryAction
import com.dentia.patient.ui.components.ScreenHeader
import com.dentia.patient.ui.theme.DentiaMuted
import com.dentia.patient.ui.theme.DentiaPrimary
import java.io.InputStream
import android.graphics.BitmapFactory

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    user: AuthUser,
    profilePhotoBytes: ByteArray?,
    isLoadingPhoto: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onBack: () -> Unit,
    onClearMessages: () -> Unit,
    onSave: (String, Uri?, () -> Unit) -> Unit,
) {
    var displayName by remember(user.displayName) {
        mutableStateOf(user.displayName)
    }

    var selectedPhotoUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var selectedPhotoBytes by remember {
        mutableStateOf<ByteArray?>(null)
    }

    var localError by remember {
        mutableStateOf<String?>(null)
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        selectedPhotoUri = uri
        selectedPhotoBytes = null
        localError = null
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(selectedPhotoUri) {
        val uri = selectedPhotoUri ?: return@LaunchedEffect

        try {
            val bytes = context.contentResolver.openInputStream(uri)
                ?.use(InputStream::readBytes)

            if (bytes == null) {
                localError = "No se pudo leer la imagen seleccionada."
                selectedPhotoUri = null
                return@LaunchedEffect
            }

            if (bytes.size > 5 * 1024 * 1024) {
                localError = "La foto no debe pesar más de 5 MB."
                selectedPhotoUri = null
                return@LaunchedEffect
            }

            selectedPhotoBytes = bytes
        } catch (_: Exception) {
            localError = "No se pudo cargar la imagen seleccionada."
            selectedPhotoUri = null
        }
    }

    val photoBytes = selectedPhotoBytes ?: profilePhotoBytes

    val bitmap = remember(photoBytes) {
        photoBytes?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    }

    val hasChanges = displayName.trim() != user.displayName.trim() ||
            selectedPhotoUri != null

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
            eyebrow = "Cuenta del paciente",
            title = "Mi perfil",
            subtitle = "Actualiza tu nombre visible y fotografía de perfil.",
        )

        DentiaCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        isLoadingPhoto -> {
                            CircularProgressIndicator()
                        }

                        bitmap != null -> {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }

                        else -> {
                            Text(
                                user.displayName
                                    .split(" ")
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .take(2)
                                    .joinToString(""),
                                color = DentiaPrimary,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                    }
                }

                Text(
                    user.email,
                    color = DentiaMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )

                OutlinedButton(
                    onClick = {
                        localError = null
                        onClearMessages()
                        photoPicker.launch("image/*")
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cambiar foto")
                }

                selectedPhotoUri?.let {
                    Text(
                        "Nueva foto seleccionada. Guarda cambios para aplicarla.",
                        color = DentiaPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        DentiaCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Datos personales",
                    style = MaterialTheme.typography.titleLarge,
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it.take(80)
                        localError = null
                        onClearMessages()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre completo") },
                    singleLine = true,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            displayName = user.displayName
                            selectedPhotoUri = null
                            selectedPhotoBytes = null
                            localError = null
                            onClearMessages()
                        },
                        enabled = hasChanges && !isSubmitting,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Descartar")
                    }

                    PrimaryAction(
                        text = if (isSubmitting) {
                            "Guardando..."
                        } else {
                            "Guardar"
                        },
                        onClick = {
                            val cleanName = displayName.trim()

                            if (cleanName.length < 3) {
                                localError = "El nombre debe tener al menos 3 caracteres."
                                return@PrimaryAction
                            }

                            onSave(cleanName, selectedPhotoUri) {
                                selectedPhotoUri = null
                                selectedPhotoBytes = null
                            }
                        },
                        enabled = hasChanges &&
                                displayName.trim().length >= 3 &&
                                !isSubmitting,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        successMessage?.let {
            DentiaCard {
                Text(
                    text = it,
                    color = DentiaPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        localError?.let {
            DentiaErrorState(message = it)
        }

        errorMessage?.let {
            DentiaErrorState(message = it)
        }

        DentiaCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Paciente: solo puedes consultar y modificar información asociada a tu propia cuenta.",
                    color = DentiaMuted,
                )
            }
        }
    }
}