package com.dentia.patient.data

import com.dentia.patient.data.model.Appointment
import com.dentia.patient.data.model.AvailabilitySlot
import com.dentia.patient.data.model.ClinicalFile
import com.dentia.patient.data.model.ChatMessage
import com.dentia.patient.data.model.Conversation
import com.dentia.patient.data.model.Dentist
import com.dentia.patient.data.model.DentistRatingSummary
import com.dentia.patient.data.model.Prescription
import com.dentia.patient.data.network.DentiaHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime

class PatientRepository(
    private val httpClient: DentiaHttpClient,
) {
    fun getDentists(): List<Dentist> {
        val response = httpClient.get("/dentists/prioritized")
        return parseArray(response.body).map(Dentist::fromJson)
    }

    fun downloadDentistPhoto(photoUrl: String): ByteArray {
        val path = if (photoUrl.startsWith("http")) {
            photoUrl.substringAfter("://").substringAfter("/").let { "/$it" }
        } else {
            photoUrl
        }
        return httpClient.download(path).bytes
    }

    fun getAppointments(): List<Appointment> {
        val response = httpClient.get("/appointments")
        return parseArray(response.body)
            .map(Appointment::fromJson)
            .sortedBy { it.startAt }
    }

    fun getAvailability(dentistId: String, date: String): List<AvailabilitySlot> {
        val encodedDentistId = URLEncoder.encode(dentistId, StandardCharsets.UTF_8.name())
        val encodedDate = URLEncoder.encode(date, StandardCharsets.UTF_8.name())
        val response = httpClient.get(
            "/appointments/availability?dentistId=$encodedDentistId&date=$encodedDate",
        )
        val json = JSONObject(response.body)
        val slots = json.optJSONArray("slots") ?: JSONArray()
        return (0 until slots.length())
            .map { AvailabilitySlot.fromJson(slots.getJSONObject(it)) }
            .filter { it.available && it.startAt.isAfter(OffsetDateTime.now()) }
    }

    fun createAppointment(
        dentistId: String,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
        reason: String?,
        notes: String?,
    ): Appointment {
        val body = JSONObject()
            .put("dentistId", dentistId)
            .put("startAt", startAt.toString())
            .put("endAt", endAt.toString())

        reason?.takeIf(String::isNotBlank)?.let { body.put("reason", it.trim()) }
        notes?.takeIf(String::isNotBlank)?.let { body.put("notes", it.trim()) }

        return Appointment.fromJson(
            JSONObject(httpClient.post("/appointments", body, authenticated = true).body),
        )
    }

    fun cancelAppointment(id: String) {
        httpClient.patch("/appointments/$id/cancel")
    }

    fun rescheduleAppointment(
        id: String,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ) {
        httpClient.patch(
            "/appointments/$id/reschedule",
            JSONObject()
                .put("startAt", startAt.toString())
                .put("endAt", endAt.toString()),
        )
    }

    fun rateAppointment(id: String, score: Int, comment: String?) {
        val body = JSONObject().put("score", score)
        comment?.takeIf(String::isNotBlank)?.let { body.put("comment", it.trim()) }
        httpClient.post(
            path = "/appointments/$id/rating",
            json = body,
            authenticated = true,
        )
    }

    fun getDentistRatingSummary(dentistId: String): DentistRatingSummary {
        val encodedId = URLEncoder.encode(dentistId, StandardCharsets.UTF_8.name())
        val response = httpClient.get("/dentists/$encodedId/ratings/summary")
        return DentistRatingSummary.fromJson(JSONObject(response.body))
    }

    fun getPrescriptions(appointmentId: String): List<Prescription> {
        val encodedId = URLEncoder.encode(appointmentId, StandardCharsets.UTF_8.name())
        val response = httpClient.get("/appointments/$encodedId/prescriptions")
        return parseArray(response.body).map(Prescription::fromJson)
    }

    fun downloadPrescriptionPdf(id: String): ByteArray {
        val encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8.name())
        return httpClient.download("/prescriptions/$encodedId/pdf").bytes
    }

    fun getClinicalFiles(): List<ClinicalFile> {
        val response = httpClient.get("/files")
        return parseArray(response.body)
            .map(ClinicalFile::fromJson)
            .sortedByDescending { it.createdAt }
    }

    fun uploadClinicalFile(
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): ClinicalFile {
        val response = httpClient.postMultipartFile(
            path = "/files",
            fieldName = "file",
            fileName = fileName,
            contentType = mimeType,
            bytes = bytes,
        )
        return ClinicalFile.fromJson(JSONObject(response.body))
    }

    fun downloadClinicalFile(id: String): ByteArray {
        val encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8.name())
        return httpClient.download("/files/$encodedId/download").bytes
    }

    fun deleteClinicalFile(id: String) {
        val encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8.name())
        httpClient.delete("/files/$encodedId")
    }

    fun getConversations(): List<Conversation> {
        val response = httpClient.get("/chat/conversations")
        return parseArray(response.body)
            .map(Conversation::fromJson)
            .sortedByDescending { it.lastMessageAt }
    }

    fun createConversation(patientId: String, dentistId: String): Conversation {
        val response = httpClient.post(
            path = "/chat/conversations",
            json = JSONObject()
                .put("patientId", patientId)
                .put("dentistId", dentistId),
            authenticated = true,
        )
        return Conversation.fromJson(JSONObject(response.body))
    }

    fun getMessages(conversationId: String): List<ChatMessage> {
        val encodedId = URLEncoder.encode(conversationId, StandardCharsets.UTF_8.name())
        val response = httpClient.get("/chat/conversations/$encodedId/messages")
        return parseArray(response.body)
            .map(ChatMessage::fromJson)
            .sortedBy { it.createdAt }
    }

    fun sendMessage(
        conversationId: String,
        body: String?,
        fileName: String? = null,
        mimeType: String? = null,
        bytes: ByteArray? = null,
    ) {
        val encodedId = URLEncoder.encode(conversationId, StandardCharsets.UTF_8.name())
        val path = "/chat/conversations/$encodedId/messages"
        val fields = body?.takeIf(String::isNotBlank)
            ?.let { mapOf("body" to it.trim()) }
            .orEmpty()

        if (bytes != null && fileName != null && mimeType != null) {
            httpClient.postMultipartFile(
                path = path,
                fieldName = "file",
                fileName = fileName,
                contentType = mimeType,
                bytes = bytes,
                fields = fields,
            )
        } else {
            httpClient.postMultipartFields(path, fields)
        }
    }

    fun markConversationAsRead(conversationId: String) {
        val encodedId = URLEncoder.encode(conversationId, StandardCharsets.UTF_8.name())
        httpClient.patch("/chat/conversations/$encodedId/read")
    }

    private fun parseArray(body: String): List<JSONObject> {
        val array = JSONArray(body)
        return (0 until array.length()).map(array::getJSONObject)
    }
}
