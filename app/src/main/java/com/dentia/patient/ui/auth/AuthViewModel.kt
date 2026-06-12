package com.dentia.patient.ui.auth

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dentia.patient.DentiaApplication
import com.dentia.patient.data.model.AuthUser
import com.dentia.patient.data.network.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AuthPage {
    Login,
    Register,
    VerifyEmail,
    ForgotPassword,
    ResetPassword,
}

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: AuthUser) : SessionState
}

data class AuthUiState(
    val session: SessionState = SessionState.Loading,
    val page: AuthPage = AuthPage.Login,
    val email: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val profilePhotoBytes: ByteArray? = null,
    val isLoadingProfilePhoto: Boolean = false,
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DentiaApplication).authRepository

    var uiState by mutableStateOf(AuthUiState())
        private set

    init {
        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) { repository.restoreSession() }
            uiState = uiState.copy(
                session = user?.let(SessionState::SignedIn) ?: SessionState.SignedOut,
            )
            user?.let(::loadProfilePhoto)
        }
    }

    fun showPage(page: AuthPage, email: String = uiState.email) {
        uiState = uiState.copy(
            page = page,
            email = email,
            errorMessage = null,
            successMessage = null,
        )
    }

    fun login(email: String, password: String) {
        submit {
            try {
                val user = withContext(Dispatchers.IO) {
                    repository.login(email, password)
                }
                uiState = uiState.copy(session = SessionState.SignedIn(user))
                loadProfilePhoto(user)
            } catch (error: ApiException) {
                if (error.requiresEmailVerification) {
                    uiState = uiState.copy(
                        page = AuthPage.VerifyEmail,
                        email = error.email ?: email,
                        errorMessage = null,
                    )
                } else {
                    throw error
                }
            }
        }
    }

    fun register(fullName: String, email: String, password: String) {
        submit {
            withContext(Dispatchers.IO) {
                repository.registerPatient(fullName, email, password)
            }
            uiState = uiState.copy(
                page = AuthPage.VerifyEmail,
                email = email.trim(),
                successMessage = "Cuenta creada. Revisa tu correo.",
            )
        }
    }

    fun verifyEmail(email: String, code: String) {
        submit {
            withContext(Dispatchers.IO) {
                repository.verifyEmail(email, code)
            }
            uiState = uiState.copy(
                page = AuthPage.Login,
                email = email.trim(),
                successMessage = "Correo verificado. Ya puedes iniciar sesión.",
            )
        }
    }

    fun resendVerificationCode(email: String) {
        submit {
            withContext(Dispatchers.IO) {
                repository.resendVerificationCode(email)
            }
            uiState = uiState.copy(
                email = email.trim(),
                successMessage = "Código reenviado. Revisa tu correo.",
            )
        }
    }

    fun requestPasswordReset(email: String) {
        submit {
            withContext(Dispatchers.IO) {
                repository.requestPasswordReset(email)
            }
            uiState = uiState.copy(
                page = AuthPage.ResetPassword,
                email = email.trim(),
                successMessage = "Código enviado. Revisa tu correo.",
            )
        }
    }

    fun resetPassword(email: String, code: String, password: String) {
        submit {
            withContext(Dispatchers.IO) {
                repository.resetPassword(email, code, password)
            }
            uiState = uiState.copy(
                page = AuthPage.Login,
                email = email.trim(),
                successMessage = "Contraseña actualizada. Inicia sesión.",
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.logout() }
            uiState = AuthUiState(session = SessionState.SignedOut)
        }
    }

    fun updateProfile(fullName: String, photoUri: Uri?, onSuccess: () -> Unit) {
        val trimmedName = fullName.trim()
        if (trimmedName.length !in 3..120) {
            uiState = uiState.copy(
                errorMessage = "El nombre debe tener entre 3 y 120 caracteres.",
            )
            return
        }

        submit {
            val photo = withContext(Dispatchers.IO) {
                photoUri?.let(::readProfilePhoto)
            }
            val user = withContext(Dispatchers.IO) {
                repository.updateProfile(
                    fullName = trimmedName,
                    photoName = photo?.name,
                    photoContentType = photo?.contentType,
                    photoBytes = photo?.bytes,
                )
            }
            uiState = uiState.copy(
                session = SessionState.SignedIn(user),
                profilePhotoBytes = photo?.bytes ?: uiState.profilePhotoBytes,
                successMessage = "Perfil actualizado correctamente.",
            )
            if (photo == null && user.photoUrl != null && uiState.profilePhotoBytes == null) {
                loadProfilePhoto(user)
            }
            onSuccess()
        }
    }

    fun clearProfileMessages() {
        uiState = uiState.copy(errorMessage = null, successMessage = null)
    }

    private fun loadProfilePhoto(user: AuthUser) {
        val photoUrl = user.photoUrl ?: run {
            uiState = uiState.copy(profilePhotoBytes = null, isLoadingProfilePhoto = false)
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingProfilePhoto = true)
            val bytes = withContext(Dispatchers.IO) {
                runCatching { repository.downloadProfilePhoto(photoUrl) }.getOrNull()
            }
            uiState = uiState.copy(
                profilePhotoBytes = bytes,
                isLoadingProfilePhoto = false,
            )
        }
    }

    private fun readProfilePhoto(uri: Uri): PendingProfilePhoto {
        val resolver = getApplication<Application>().contentResolver
        val metadata = resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
            val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                cursor.getLong(sizeIndex)
            } else {
                null
            }
            name to size
        }
        val name = metadata?.first ?: "foto-perfil"
        val contentType = resolver.getType(uri) ?: when {
            name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
            name.endsWith(".png", true) -> "image/png"
            name.endsWith(".webp", true) -> "image/webp"
            else -> "application/octet-stream"
        }

        if (contentType !in profilePhotoTypes) {
            throw IllegalArgumentException("La foto debe ser JPG, PNG o WEBP.")
        }
        if ((metadata?.second ?: 0L) > maxProfilePhotoBytes) {
            throw IllegalArgumentException("La foto supera el límite de 5 MB.")
        }
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("No se pudo leer la foto seleccionada.")
        if (bytes.size > maxProfilePhotoBytes) {
            throw IllegalArgumentException("La foto supera el límite de 5 MB.")
        }
        return PendingProfilePhoto(name, contentType, bytes)
    }

    private fun submit(action: suspend () -> Unit) {
        if (uiState.isSubmitting) return

        uiState = uiState.copy(
            isSubmitting = true,
            errorMessage = null,
            successMessage = null,
        )

        viewModelScope.launch {
            try {
                action()
            } catch (error: Exception) {
                uiState = uiState.copy(
                    errorMessage = error.message ?: "No se pudo completar la solicitud.",
                )
            } finally {
                uiState = uiState.copy(isSubmitting = false)
            }
        }
    }

    private companion object {
        const val maxProfilePhotoBytes = 5 * 1024 * 1024
        val profilePhotoTypes = setOf("image/jpeg", "image/png", "image/webp")
    }
}

private data class PendingProfilePhoto(
    val name: String,
    val contentType: String,
    val bytes: ByteArray,
)
