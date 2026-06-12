package com.dentia.patient.ui.patient

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dentia.patient.DentiaApplication
import com.dentia.patient.data.model.Appointment
import com.dentia.patient.data.model.AvailabilitySlot
import com.dentia.patient.data.model.ClinicalFile
import com.dentia.patient.data.model.ChatAttachment
import com.dentia.patient.data.model.ChatMessage
import com.dentia.patient.data.model.Conversation
import com.dentia.patient.data.model.Dentist
import com.dentia.patient.data.model.DentistRatingSummary
import com.dentia.patient.data.model.Prescription
import com.dentia.patient.data.network.ApiException
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime

data class PatientUiState(
    val dentists: List<Dentist> = emptyList(),
    val dentistPhotos: Map<String, ByteArray> = emptyMap(),
    val appointments: List<Appointment> = emptyList(),
    val availability: List<AvailabilitySlot> = emptyList(),
    val ratingSummary: DentistRatingSummary? = null,
    val prescriptionsByAppointment: Map<String, List<Prescription>> = emptyMap(),
    val clinicalFiles: List<ClinicalFile> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val selectedConversationId: String? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val chatAttachmentPreviews: Map<String, ByteArray> = emptyMap(),
    val loadingDentists: Boolean = false,
    val loadingAppointments: Boolean = false,
    val loadingAvailability: Boolean = false,
    val loadingRatings: Boolean = false,
    val loadingPrescriptionsFor: String? = null,
    val loadingFiles: Boolean = false,
    val fileOperationId: String? = null,
    val loadingConversations: Boolean = false,
    val loadingMessages: Boolean = false,
    val sendingMessage: Boolean = false,
    val submitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class PatientViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DentiaApplication).patientRepository

    var uiState by mutableStateOf(PatientUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        loadDentists()
        loadAppointments()
    }

    fun loadDentists() {
        launchRequest(
            onStart = { uiState = uiState.copy(loadingDentists = true, errorMessage = null) },
            onFinish = { uiState = uiState.copy(loadingDentists = false) },
        ) {
            val dentists = withContext(Dispatchers.IO) {
                repository.getDentists()
            }
            uiState = uiState.copy(dentists = dentists)
            loadDentistPhotos(dentists)
        }
    }

    fun loadAppointments() {
        launchRequest(
            onStart = { uiState = uiState.copy(loadingAppointments = true, errorMessage = null) },
            onFinish = { uiState = uiState.copy(loadingAppointments = false) },
        ) {
            val appointments = withContext(Dispatchers.IO) {
                repository.getAppointments()
            }
            uiState = uiState.copy(appointments = appointments)
        }
    }

    private fun loadDentistPhotos(dentists: List<Dentist>) {
        viewModelScope.launch {
            val photos = withContext(Dispatchers.IO) {
                dentists.mapNotNull { dentist ->
                    val photoUrl = dentist.photoUrl ?: return@mapNotNull null
                    runCatching {
                        dentist.domainId to repository.downloadDentistPhoto(photoUrl)
                    }.getOrNull()
                }.toMap()
            }
            uiState = uiState.copy(dentistPhotos = photos)
        }
    }

    fun loadAvailability(dentistId: String, date: String) {
        launchRequest(
            onStart = {
                uiState = uiState.copy(
                    loadingAvailability = true,
                    availability = emptyList(),
                    errorMessage = null,
                )
            },
            onFinish = { uiState = uiState.copy(loadingAvailability = false) },
        ) {
            val slots = withContext(Dispatchers.IO) {
                repository.getAvailability(dentistId, date)
            }
            uiState = uiState.copy(availability = slots)
        }
    }

    fun createAppointment(
        dentistId: String,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
        reason: String,
        notes: String,
        onSuccess: () -> Unit,
    ) {
        launchRequest(
            onStart = {
                uiState = uiState.copy(
                    submitting = true,
                    errorMessage = null,
                    successMessage = null,
                )
            },
            onFinish = { uiState = uiState.copy(submitting = false) },
        ) {
            val appointments = withContext(Dispatchers.IO) {
                repository.createAppointment(dentistId, startAt, endAt, reason, notes)
                repository.getAppointments()
            }
            uiState = uiState.copy(
                appointments = appointments,
                successMessage = "Solicitud de cita enviada.",
            )
            onSuccess()
        }
    }

    fun cancelAppointment(id: String) {
        launchRequest(
            onStart = { uiState = uiState.copy(submitting = true, errorMessage = null) },
            onFinish = { uiState = uiState.copy(submitting = false) },
        ) {
            val appointments = withContext(Dispatchers.IO) {
                repository.cancelAppointment(id)
                repository.getAppointments()
            }
            uiState = uiState.copy(
                appointments = appointments,
                successMessage = "Cita cancelada.",
            )
        }
    }

    fun rescheduleAppointment(
        id: String,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
        onSuccess: () -> Unit,
    ) {
        val appointment = uiState.appointments.firstOrNull { it.id == id }
        if (appointment?.status != "PENDING") {
            uiState = uiState.copy(
                errorMessage = "Solo las citas pendientes pueden reprogramarse. Una cita confirmada ya tiene el horario reservado por el dentista.",
            )
            return
        }

        launchRequest(
            onStart = {
                uiState = uiState.copy(
                    submitting = true,
                    errorMessage = null,
                    successMessage = null,
                )
            },
            onFinish = { uiState = uiState.copy(submitting = false) },
        ) {
            val appointments = withContext(Dispatchers.IO) {
                val currentAppointments = repository.getAppointments()
                val currentAppointment = currentAppointments.firstOrNull { it.id == id }

                if (currentAppointment?.status != "PENDING") {
                    throw UserMessageException(
                        "La cita ya fue confirmada o cambió de estado y no puede reprogramarse.",
                    )
                }

                repository.rescheduleAppointment(id, startAt, endAt)
                repository.getAppointments()
            }
            uiState = uiState.copy(
                appointments = appointments,
                successMessage = "Cita reprogramada.",
            )
            onSuccess()
        }
    }

    fun rateAppointment(
        id: String,
        score: Int,
        comment: String,
        onSuccess: () -> Unit,
    ) {
        launchRequest(
            onStart = {
                uiState = uiState.copy(
                    submitting = true,
                    errorMessage = null,
                    successMessage = null,
                )
            },
            onFinish = { uiState = uiState.copy(submitting = false) },
        ) {
            val appointments = withContext(Dispatchers.IO) {
                repository.rateAppointment(id, score, comment)
                repository.getAppointments()
            }
            uiState = uiState.copy(
                appointments = appointments,
                successMessage = "Valoración enviada.",
            )
            onSuccess()
        }
    }

    fun loadDentistRatings(dentistId: String) {
        launchRequest(
            onStart = {
                uiState = uiState.copy(
                    loadingRatings = true,
                    ratingSummary = null,
                    errorMessage = null,
                )
            },
            onFinish = { uiState = uiState.copy(loadingRatings = false) },
        ) {
            val summary = withContext(Dispatchers.IO) {
                repository.getDentistRatingSummary(dentistId)
            }
            uiState = uiState.copy(ratingSummary = summary)
        }
    }

    fun clearMessages() {
        uiState = uiState.copy(errorMessage = null, successMessage = null)
    }

    fun clearAvailability() {
        uiState = uiState.copy(availability = emptyList(), errorMessage = null)
    }

    fun clearDentistRatings() {
        uiState = uiState.copy(ratingSummary = null, errorMessage = null)
    }

    fun loadPrescriptions(appointmentId: String) {
        launchRequest(
            onStart = {
                uiState = uiState.copy(
                    loadingPrescriptionsFor = appointmentId,
                    errorMessage = null,
                )
            },
            onFinish = { uiState = uiState.copy(loadingPrescriptionsFor = null) },
        ) {
            val prescriptions = withContext(Dispatchers.IO) {
                repository.getPrescriptions(appointmentId)
            }
            uiState = uiState.copy(
                prescriptionsByAppointment =
                    uiState.prescriptionsByAppointment + (appointmentId to prescriptions),
            )
        }
    }

    fun downloadPrescription(
        prescription: Prescription,
        onReady: (File) -> Unit,
    ) {
        launchRequest(
            onStart = { uiState = uiState.copy(submitting = true, errorMessage = null) },
            onFinish = { uiState = uiState.copy(submitting = false) },
        ) {
            val file = withContext(Dispatchers.IO) {
                val bytes = repository.downloadPrescriptionPdf(prescription.id)
                File(
                    getApplication<Application>().cacheDir,
                    "receta-${prescription.id}.pdf",
                ).apply { writeBytes(bytes) }
            }
            onReady(file)
        }
    }

    fun loadClinicalFiles() {
        launchRequest(
            onStart = { uiState = uiState.copy(loadingFiles = true, errorMessage = null) },
            onFinish = { uiState = uiState.copy(loadingFiles = false) },
        ) {
            val files = withContext(Dispatchers.IO) {
                repository.getClinicalFiles()
            }
            uiState = uiState.copy(clinicalFiles = files)
        }
    }

    fun uploadClinicalFile(uri: Uri) {
        launchRequest(
            onStart = {
                uiState = uiState.copy(
                    fileOperationId = "upload",
                    errorMessage = null,
                    successMessage = null,
                )
            },
            onFinish = { uiState = uiState.copy(fileOperationId = null) },
        ) {
            val application = getApplication<Application>()
            val resolver = application.contentResolver
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
            val fileName = metadata?.first ?: "archivo-clinico"
            if ((metadata?.second ?: 0L) > maxClinicalFileBytes) {
                throw UserMessageException("El archivo supera el límite de 10 MB.")
            }
            val mimeType = resolver.getType(uri) ?: mimeTypeFromName(fileName)

            if (mimeType !in allowedClinicalFileTypes) {
                throw UserMessageException(
                    "Formato no permitido. Usa PDF, JPG, PNG, WEBP, MP4 o WEBM.",
                )
            }

            val bytes = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw UserMessageException("No se pudo leer el archivo seleccionado.")
            }
            if (bytes.size > maxClinicalFileBytes) {
                throw UserMessageException("El archivo supera el límite de 10 MB.")
            }

            val files = withContext(Dispatchers.IO) {
                repository.uploadClinicalFile(fileName, mimeType, bytes)
                repository.getClinicalFiles()
            }
            uiState = uiState.copy(
                clinicalFiles = files,
                successMessage = "Archivo clínico subido.",
            )
        }
    }

    fun downloadClinicalFile(
        clinicalFile: ClinicalFile,
        onReady: (File) -> Unit,
    ) {
        launchRequest(
            onStart = {
                uiState = uiState.copy(
                    fileOperationId = clinicalFile.id,
                    errorMessage = null,
                )
            },
            onFinish = { uiState = uiState.copy(fileOperationId = null) },
        ) {
            val file = withContext(Dispatchers.IO) {
                val bytes = repository.downloadClinicalFile(clinicalFile.id)
                val safeName = clinicalFile.originalName
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .ifBlank { "archivo-${clinicalFile.id}" }
                File(getApplication<Application>().cacheDir, safeName).apply {
                    writeBytes(bytes)
                }
            }
            onReady(file)
        }
    }

    fun deleteClinicalFile(id: String) {
        launchRequest(
            onStart = {
                uiState = uiState.copy(
                    fileOperationId = id,
                    errorMessage = null,
                    successMessage = null,
                )
            },
            onFinish = { uiState = uiState.copy(fileOperationId = null) },
        ) {
            val files = withContext(Dispatchers.IO) {
                repository.deleteClinicalFile(id)
                repository.getClinicalFiles()
            }
            uiState = uiState.copy(
                clinicalFiles = files,
                successMessage = "Archivo eliminado.",
            )
        }
    }

    fun loadConversations() {
        launchRequest(
            onStart = {
                uiState = uiState.copy(
                    loadingConversations = true,
                    errorMessage = null,
                )
            },
            onFinish = { uiState = uiState.copy(loadingConversations = false) },
        ) {
            val conversations = withContext(Dispatchers.IO) {
                repository.getConversations()
            }
            uiState = uiState.copy(conversations = conversations)
        }
    }

    fun createConversation(
        patientId: String,
        dentistId: String,
        onSuccess: () -> Unit,
    ) {
        val hasClinicalRelationship = uiState.appointments.any {
            it.dentistId == dentistId && it.status in setOf("CONFIRMED", "COMPLETED")
        }
        if (patientId.isBlank() || !hasClinicalRelationship) {
            uiState = uiState.copy(
                errorMessage = "Solo puedes iniciar chat con dentistas de citas confirmadas o completadas.",
            )
            return
        }

        launchRequest(
            onStart = { uiState = uiState.copy(submitting = true, errorMessage = null) },
            onFinish = { uiState = uiState.copy(submitting = false) },
        ) {
            val result = withContext(Dispatchers.IO) {
                val conversation = repository.createConversation(patientId, dentistId)
                conversation to repository.getConversations()
            }
            uiState = uiState.copy(conversations = result.second)
            openConversation(result.first.id)
            onSuccess()
        }
    }

    fun openConversation(conversationId: String) {
        uiState = uiState.copy(
            selectedConversationId = conversationId,
            chatMessages = emptyList(),
        )
        loadMessages(conversationId)
    }

    fun closeConversation() {
        uiState = uiState.copy(
            selectedConversationId = null,
            chatMessages = emptyList(),
            chatAttachmentPreviews = emptyMap(),
            errorMessage = null,
        )
    }

    fun loadMessages(conversationId: String) {
        launchRequest(
            onStart = { uiState = uiState.copy(loadingMessages = true, errorMessage = null) },
            onFinish = { uiState = uiState.copy(loadingMessages = false) },
        ) {
            val messages = withContext(Dispatchers.IO) {
                runCatching { repository.markConversationAsRead(conversationId) }
                repository.getMessages(conversationId)
            }
            if (uiState.selectedConversationId == conversationId) {
                uiState = uiState.copy(chatMessages = messages)
                loadChatImagePreviews(messages)
            }
        }
    }

    fun sendChatMessage(
        conversationId: String,
        body: String,
        attachmentUri: Uri?,
        onSuccess: () -> Unit,
    ) {
        if (body.isBlank() && attachmentUri == null) return

        launchRequest(
            onStart = {
                uiState = uiState.copy(
                    sendingMessage = true,
                    errorMessage = null,
                )
            },
            onFinish = { uiState = uiState.copy(sendingMessage = false) },
        ) {
            val attachment = withContext(Dispatchers.IO) {
                attachmentUri?.let(::readChatAttachment)
            }
            val messages = withContext(Dispatchers.IO) {
                repository.sendMessage(
                    conversationId = conversationId,
                    body = body,
                    fileName = attachment?.name,
                    mimeType = attachment?.mimeType,
                    bytes = attachment?.bytes,
                )
                repository.getMessages(conversationId)
            }
            uiState = uiState.copy(chatMessages = messages)
            loadChatImagePreviews(messages)
            loadConversations()
            onSuccess()
        }
    }

    fun downloadChatAttachment(
        attachment: ChatAttachment,
        onReady: (File) -> Unit,
    ) {
        launchRequest(
            onStart = {
                uiState = uiState.copy(
                    fileOperationId = attachment.fileId,
                    errorMessage = null,
                )
            },
            onFinish = { uiState = uiState.copy(fileOperationId = null) },
        ) {
            val file = withContext(Dispatchers.IO) {
                val bytes = repository.downloadClinicalFile(attachment.fileId)
                val safeName = attachment.originalName
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .ifBlank { "adjunto-${attachment.fileId}" }
                File(getApplication<Application>().cacheDir, safeName).apply {
                    writeBytes(bytes)
                }
            }
            onReady(file)
        }
    }

    private fun loadChatImagePreviews(messages: List<ChatMessage>) {
        viewModelScope.launch {
            val previews = withContext(Dispatchers.IO) {
                messages.mapNotNull { message ->
                    val attachment = message.attachment ?: return@mapNotNull null
                    if (!attachment.contentType.startsWith("image/")) return@mapNotNull null
                    runCatching {
                        attachment.fileId to repository.downloadClinicalFile(attachment.fileId)
                    }.getOrNull()
                }.toMap()
            }
            uiState = uiState.copy(chatAttachmentPreviews = previews)
        }
    }

    private fun launchRequest(
        onStart: () -> Unit,
        onFinish: () -> Unit,
        block: suspend () -> Unit,
    ) {
        onStart()
        viewModelScope.launch {
            try {
                block()
            } catch (error: Exception) {
                uiState = uiState.copy(
                    errorMessage = when (error) {
                        is ApiException, is UserMessageException ->
                            error.message ?: "No se pudo completar la solicitud."
                        else ->
                            "No se pudo completar la solicitud en este momento. Inténtalo nuevamente."
                    },
                )
            } finally {
                onFinish()
            }
        }
    }

    private fun mimeTypeFromName(fileName: String): String = when {
        fileName.endsWith(".pdf", true) -> "application/pdf"
        fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
        fileName.endsWith(".png", true) -> "image/png"
        fileName.endsWith(".webp", true) -> "image/webp"
        fileName.endsWith(".mp4", true) -> "video/mp4"
        fileName.endsWith(".webm", true) -> "video/webm"
        else -> "application/octet-stream"
    }

    private fun readChatAttachment(uri: Uri): PendingAttachment {
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
        val name = metadata?.first ?: "adjunto"
        val mimeType = resolver.getType(uri) ?: mimeTypeFromName(name)

        if (mimeType !in allowedClinicalFileTypes) {
            throw UserMessageException(
                "Solo puedes enviar imágenes, videos o archivos PDF.",
            )
        }
        if ((metadata?.second ?: 0L) > maxChatAttachmentBytes) {
            throw UserMessageException("El archivo supera el límite de 50 MB.")
        }

        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw UserMessageException("No se pudo leer el archivo seleccionado.")
        if (bytes.size > maxChatAttachmentBytes) {
            throw UserMessageException("El archivo supera el límite de 50 MB.")
        }
        return PendingAttachment(name, mimeType, bytes)
    }

    companion object {
        private const val maxClinicalFileBytes = 10 * 1024 * 1024
        private const val maxChatAttachmentBytes = 50 * 1024 * 1024
        private val allowedClinicalFileTypes = setOf(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp",
            "video/mp4",
            "video/webm",
        )
    }
}

private data class PendingAttachment(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray,
)

private class UserMessageException(message: String) : Exception(message)
