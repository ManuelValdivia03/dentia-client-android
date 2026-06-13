package com.dentia.patient.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.dentia.patient.data.model.ChatAttachment
import com.dentia.patient.data.model.ChatMessage
import com.dentia.patient.data.model.Conversation
import com.dentia.patient.data.model.Dentist
import com.dentia.patient.ui.components.DentiaCard
import com.dentia.patient.ui.components.DentiaEmptyState
import com.dentia.patient.ui.components.DentiaErrorState
import com.dentia.patient.ui.components.DentiaLoadingState
import com.dentia.patient.ui.components.PrimaryAction
import com.dentia.patient.ui.components.ScreenHeader
import com.dentia.patient.ui.patient.PatientUiState
import com.dentia.patient.ui.theme.DentiaMuted
import com.dentia.patient.ui.theme.DentiaPrimary
import java.io.File
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun ChatScreen(
    contentPadding: PaddingValues,
    patientId: String,
    state: PatientUiState,
    onBack: () -> Unit,
    onLoadConversations: () -> Unit,
    onCreateConversation: (String, String, () -> Unit) -> Unit,
    onOpenConversation: (String) -> Unit,
    onCloseConversation: () -> Unit,
    onRefreshMessages: (String) -> Unit,
    onSendMessage: (String, String, Uri?, () -> Unit) -> Unit,
    onDownloadAttachment: (ChatAttachment, (File) -> Unit) -> Unit,
) {
    LaunchedEffect(state.selectedConversationId) {
        while (true) {
            val conversationId = state.selectedConversationId

            if (conversationId == null) {
                onLoadConversations()
                delay(15_000)
            } else {
                onRefreshMessages(conversationId)
                delay(8_000)
            }
        }
    }

    val selected = state.conversations.firstOrNull {
        it.id == state.selectedConversationId
    }

    if (selected == null) {
        ConversationInbox(
            contentPadding = contentPadding,
            patientId = patientId,
            state = state,
            onBack = onBack,
            onRefresh = onLoadConversations,
            onCreateConversation = onCreateConversation,
            onOpenConversation = onOpenConversation,
        )
    } else {
        ConversationPanel(
            contentPadding = contentPadding,
            patientId = patientId,
            conversation = selected,
            state = state,
            onBack = onCloseConversation,
            onRefresh = { onRefreshMessages(selected.id) },
            onSendMessage = { body, uri, onSuccess ->
                onSendMessage(selected.id, body, uri, onSuccess)
            },
            onDownloadAttachment = onDownloadAttachment,
        )
    }
}

@Composable
private fun ConversationInbox(
    contentPadding: PaddingValues,
    patientId: String,
    state: PatientUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCreateConversation: (String, String, () -> Unit) -> Unit,
    onOpenConversation: (String) -> Unit,
) {
    var showNewConversation by remember { mutableStateOf(false) }

    val dentistNames = state.dentists.associate { it.domainId to it.fullName }

    val eligibleDentists = state.dentists.filter { dentist ->
        state.appointments.any {
            it.dentistId == dentist.domainId &&
                    it.status in setOf("CONFIRMED", "COMPLETED")
        }
    }

    if (showNewConversation) {
        NewConversationDialog(
            dentists = eligibleDentists,
            submitting = state.submitting,
            onDismiss = { showNewConversation = false },
            onCreate = { dentistId ->
                onCreateConversation(patientId, dentistId) {
                    showNewConversation = false
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
            eyebrow = "Mensajería clínica",
            title = "Chat",
            subtitle = "Comunícate con dentistas que ya confirmaron o atendieron una cita.",
        )

        PrimaryAction(
            text = "Nueva conversación",
            onClick = { showNewConversation = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = patientId.isNotBlank() && eligibleDentists.isNotEmpty(),
        )

        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loadingConversations,
        ) {
            Text(
                if (state.loadingConversations) {
                    "Actualizando..."
                } else {
                    "Actualizar conversaciones"
                },
            )
        }

        when {
            state.loadingConversations && state.conversations.isEmpty() -> {
                DentiaLoadingState(
                    message = "Cargando conversaciones...",
                )
            }

            state.errorMessage != null && state.conversations.isEmpty() -> {
                DentiaErrorState(
                    message = state.errorMessage,
                    onRetry = onRefresh,
                )
            }

            state.conversations.isEmpty() -> {
                DentiaEmptyState(
                    title = "No hay conversaciones todavía",
                    message = if (eligibleDentists.isEmpty()) {
                        "Podrás iniciar chat cuando tengas una cita confirmada o completada."
                    } else {
                        "Inicia una conversación con uno de tus dentistas."
                    },
                    actionText = if (eligibleDentists.isEmpty()) null else "Nueva conversación",
                    onAction = if (eligibleDentists.isEmpty()) null else {
                        { showNewConversation = true }
                    },
                )
            }

            else -> {
                state.conversations.forEach { conversation ->
                    ConversationCard(
                        conversation = conversation,
                        dentistName = dentistNames[conversation.dentistId] ?: "Dentista",
                        onOpen = { onOpenConversation(conversation.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    dentistName: String,
    onOpen: () -> Unit,
) {
    DentiaCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                dentistName,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                conversation.lastMessagePreview ?: "Sin mensajes",
                color = DentiaMuted,
            )

            conversation.lastMessageAt?.let {
                Text(
                    it.format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm")),
                    color = DentiaMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            OutlinedButton(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Abrir conversación")
            }
        }
    }
}

@Composable
private fun NewConversationDialog(
    dentists: List<Dentist>,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var selectedId by remember(dentists) {
        mutableStateOf(dentists.firstOrNull()?.domainId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva conversación") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (dentists.isEmpty()) {
                    Text(
                        "Podrás iniciar chat cuando tengas una cita confirmada o completada.",
                        color = DentiaMuted,
                    )
                }

                dentists.forEach { dentist ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(
                            selected = selectedId == dentist.domainId,
                            onClick = { selectedId = dentist.domainId },
                        )

                        Column {
                            Text(dentist.fullName)
                            Text(
                                dentist.specialty ?: "Odontología",
                                color = DentiaMuted,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedId?.let(onCreate) },
                enabled = selectedId != null && !submitting,
            ) {
                Text(
                    if (submitting) {
                        "Creando..."
                    } else {
                        "Crear"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun ConversationPanel(
    contentPadding: PaddingValues,
    patientId: String,
    conversation: Conversation,
    state: PatientUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSendMessage: (String, Uri?, () -> Unit) -> Unit,
    onDownloadAttachment: (ChatAttachment, (File) -> Unit) -> Unit,
) {
    val context = LocalContext.current

    val dentistName = state.dentists.firstOrNull {
        it.domainId == conversation.dentistId
    }?.fullName ?: "Dentista"

    var body by remember(conversation.id) { mutableStateOf("") }
    var attachmentUri by remember(conversation.id) { mutableStateOf<Uri?>(null) }
    var openError by remember { mutableStateOf<String?>(null) }

    val messagesListState = rememberLazyListState()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        attachmentUri = uri
        openError = null
    }

    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            messagesListState.animateScrollToItem(state.chatMessages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("‹ Conversaciones")
        }

        ScreenHeader(
            eyebrow = "Conversación",
            title = dentistName,
            subtitle = "Mensajes clínicos con historial.",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.weight(1f),
                enabled = !state.loadingMessages,
            ) {
                Text(
                    if (state.loadingMessages) {
                        "Actualizando..."
                    } else {
                        "Actualizar"
                    },
                )
            }
        }

        state.errorMessage?.let {
            DentiaErrorState(message = it)
        }

        openError?.let {
            DentiaErrorState(message = it)
        }

        when {
            state.loadingMessages && state.chatMessages.isEmpty() -> {
                DentiaLoadingState(
                    message = "Cargando mensajes...",
                    modifier = Modifier.weight(1f),
                )
            }

            state.chatMessages.isEmpty() -> {
                DentiaEmptyState(
                    title = "Aún no hay mensajes",
                    message = "Puedes iniciar la conversación escribiendo abajo.",
                    modifier = Modifier.weight(1f),
                )
            }

            else -> {
                LazyColumn(
                    state = messagesListState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = state.chatMessages,
                        key = { message -> message.id },
                    ) { message ->
                        MessageBubble(
                            message = message,
                            own = message.senderRole == "PATIENT" ||
                                    message.senderId == patientId,
                            processingAttachment = state.fileOperationId ==
                                    message.attachment?.fileId,
                            attachmentPreview = message.attachment?.fileId
                                ?.let(state.chatAttachmentPreviews::get),
                            onOpenAttachment = { attachment ->
                                openError = null

                                onDownloadAttachment(attachment) { file ->
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.files",
                                        file,
                                    )

                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, attachment.contentType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }

                                    try {
                                        context.startActivity(
                                            Intent.createChooser(intent, "Abrir adjunto"),
                                        )
                                    } catch (_: ActivityNotFoundException) {
                                        openError = "No hay una aplicación instalada para abrir este archivo."
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

        MessageComposer(
            body = body,
            attachmentUri = attachmentUri,
            sending = state.sendingMessage,
            onBodyChange = { body = it },
            onPickFile = {
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
            onClearAttachment = { attachmentUri = null },
            onSend = {
                onSendMessage(body, attachmentUri) {
                    body = ""
                    attachmentUri = null
                }
            },
        )
    }
}

@Composable
private fun MessageComposer(
    body: String,
    attachmentUri: Uri?,
    sending: Boolean,
    onBodyChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onClearAttachment: () -> Unit,
    onSend: () -> Unit,
) {
    DentiaCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = body,
                onValueChange = { onBodyChange(it.take(1000)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Escribe un mensaje") },
                minLines = 2,
                maxLines = 4,
            )

            Text(
                "Adjuntos: JPG, PNG, WEBP, PDF, MP4 o WEBM. Máximo 50 MB.",
                color = DentiaMuted,
                style = MaterialTheme.typography.bodySmall,
            )

            OutlinedButton(
                onClick = onPickFile,
                modifier = Modifier.fillMaxWidth(),
                enabled = !sending,
            ) {
                Text(
                    if (attachmentUri == null) {
                        "Adjuntar archivo"
                    } else {
                        "Cambiar adjunto"
                    },
                )
            }

            attachmentUri?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Archivo listo", color = DentiaPrimary)

                    TextButton(
                        onClick = onClearAttachment,
                        enabled = !sending,
                    ) {
                        Text("Quitar")
                    }
                }
            }

            PrimaryAction(
                text = if (sending) "Enviando..." else "Enviar",
                onClick = onSend,
                modifier = Modifier.fillMaxWidth(),
                enabled = !sending && (body.isNotBlank() || attachmentUri != null),
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    own: Boolean,
    processingAttachment: Boolean,
    attachmentPreview: ByteArray?,
    onOpenAttachment: (ChatAttachment) -> Unit,
) {
    val previewBitmap = remember(attachmentPreview) {
        attachmentPreview?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (own) {
            Arrangement.End
        } else {
            Arrangement.Start
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .background(
                    if (own) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    MaterialTheme.shapes.large,
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            message.body
                ?.takeIf(String::isNotBlank)
                ?.let {
                    Text(it)
                }

            message.attachment?.let { attachment ->
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Imagen adjunta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentScale = ContentScale.Crop,
                    )
                }

                OutlinedButton(
                    onClick = { onOpenAttachment(attachment) },
                    enabled = !processingAttachment,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (processingAttachment) {
                            "Descargando..."
                        } else {
                            attachment.originalName
                        },
                    )
                }
            }

            Text(
                listOfNotNull(
                    if (own) "Tú" else "Dentista",
                    message.createdAt?.format(DateTimeFormatter.ofPattern("dd MMM · HH:mm")),
                ).joinToString(" · "),
                color = DentiaMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}