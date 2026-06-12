package com.dentia.patient.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dentia.patient.data.model.AuthUser
import com.dentia.patient.ui.components.DentiaCard
import com.dentia.patient.ui.components.PrimaryAction
import com.dentia.patient.ui.components.ScreenHeader
import com.dentia.patient.ui.theme.DentiaMuted
import com.dentia.patient.ui.theme.DentiaPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val context = LocalContext.current
    var fullName by remember(user.fullName) { mutableStateOf(user.displayName) }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }
    var previewBytes by remember { mutableStateOf<ByteArray?>(profilePhotoBytes) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        selectedPhoto = uri
        onClearMessages()
    }

    LaunchedEffect(profilePhotoBytes, selectedPhoto) {
        previewBytes = if (selectedPhoto != null) {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(selectedPhoto!!)?.use { it.readBytes() }
            }
        } else {
            profilePhotoBytes
        }
    }

    val initials = fullName
        .split(" ")
        .filter(String::isNotBlank)
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "P" }
    val hasChanges = fullName.trim() != user.displayName.trim() || selectedPhoto != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        TextButton(onClick = onBack) { Text("< Volver") }
        ScreenHeader(
            eyebrow = "Mi cuenta",
            title = "Perfil",
            subtitle = "Actualiza tu nombre y fotografía de paciente.",
        )

        DentiaCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfilePhoto(
                    bytes = previewBytes,
                    initials = initials,
                    loading = isLoadingPhoto && previewBytes == null,
                )
                Text(fullName.ifBlank { user.email }, style = MaterialTheme.typography.titleLarge)
                Text(user.email, color = DentiaMuted)
                Text("Paciente", color = DentiaPrimary, fontWeight = FontWeight.Bold)
            }
        }

        DentiaCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        onClearMessages()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre completo") },
                    singleLine = true,
                    supportingText = { Text("Entre 3 y 120 caracteres.") },
                )
                OutlinedTextField(
                    value = user.email,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correo electrónico") },
                    readOnly = true,
                    supportingText = {
                        Text("El correo y la contraseña no se editan desde esta pantalla.")
                    },
                )
                OutlinedButton(
                    onClick = {
                        picker.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                ) {
                    Text(if (selectedPhoto == null) "Seleccionar fotografía" else "Cambiar fotografía")
                }
                Text(
                    "JPG, PNG o WEBP. Tamaño máximo: 5 MB.",
                    color = DentiaMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                selectedPhoto?.let {
                    TextButton(
                        onClick = {
                            selectedPhoto = null
                            onClearMessages()
                        },
                    ) {
                        Text("Quitar selección")
                    }
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                successMessage?.let { Text(it, color = DentiaPrimary) }
                PrimaryAction(
                    text = when {
                        isSubmitting -> "Guardando..."
                        hasChanges -> "Guardar perfil"
                        else -> "Sin cambios"
                    },
                    onClick = {
                        onSave(fullName, selectedPhoto) {
                            selectedPhoto = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasChanges && !isSubmitting && fullName.trim().length in 3..120,
                )
            }
        }
    }
}

@Composable
private fun ProfilePhoto(
    bytes: ByteArray?,
    initials: String,
    loading: Boolean,
) {
    val bitmap = remember(bytes) {
        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }
    Box(
        modifier = Modifier
            .size(112.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        when {
            loading -> CircularProgressIndicator()
            bitmap != null -> Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto de perfil",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            else -> Text(
                initials,
                color = DentiaPrimary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
