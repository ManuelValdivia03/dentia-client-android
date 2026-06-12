package com.dentia.patient.data.model

import org.json.JSONObject
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

data class Dentist(
    val domainId: String,
    val fullName: String,
    val email: String?,
    val specialty: String?,
    val professionalLicense: String?,
    val school: String?,
    val description: String?,
    val photoUrl: String?,
    val previouslyVisited: Boolean,
) {
    val initials: String
        get() = fullName.split(" ")
            .filter(String::isNotBlank)
            .take(2)
            .joinToString("") { it.first().uppercase() }

    companion object {
        fun fromJson(json: JSONObject): Dentist = Dentist(
            domainId = json.optString("domainId"),
            fullName = json.optionalString("fullName")
                ?: json.optionalString("name")
                ?: json.optString("email", "Dentista"),
            email = json.optionalString("email"),
            specialty = json.optionalString("specialty"),
            professionalLicense = json.optionalString("cedulaProfesional"),
            school = json.optionalString("escuela"),
            description = json.optionalString("descripcion"),
            photoUrl = json.optionalString("photoUrl"),
            previouslyVisited = json.optBoolean("previouslyVisited", false),
        )
    }
}

data class Appointment(
    val id: String,
    val patientId: String,
    val dentistId: String,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
    val status: String,
    val reason: String?,
    val notes: String?,
    val hasRating: Boolean,
) {
    companion object {
        fun fromJson(json: JSONObject): Appointment = Appointment(
            id = json.optString("id"),
            patientId = json.optString("patientId"),
            dentistId = json.optString("dentistId"),
            startAt = parseApiDateTime(json.optString("startAt")),
            endAt = parseApiDateTime(json.optString("endAt")),
            status = when (json.optString("status").uppercase()) {
                "SCHEDULED" -> "PENDING"
                "CANCELED" -> "CANCELLED"
                else -> json.optString("status").uppercase()
            },
            reason = json.optionalString("reason"),
            notes = json.optionalString("notes"),
            hasRating = json.optBoolean("hasRating", false),
        )
    }
}

data class AvailabilitySlot(
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
    val available: Boolean,
) {
    companion object {
        fun fromJson(json: JSONObject): AvailabilitySlot = AvailabilitySlot(
            startAt = parseApiDateTime(json.optString("startAt")),
            endAt = parseApiDateTime(json.optString("endAt")),
            available = json.optBoolean("available", true),
        )
    }
}

data class RatingItem(
    val id: String,
    val score: Int,
    val comment: String?,
    val createdAt: OffsetDateTime,
) {
    companion object {
        fun fromJson(json: JSONObject): RatingItem = RatingItem(
            id = json.optString("id"),
            score = json.optInt("score"),
            comment = json.optionalString("comment"),
            createdAt = parseApiDateTime(json.optString("createdAt")),
        )
    }
}

data class DentistRatingSummary(
    val dentistId: String,
    val totalRatings: Int,
    val averageScore: Double,
    val latestRatings: List<RatingItem>,
) {
    companion object {
        fun fromJson(json: JSONObject): DentistRatingSummary {
            val ratings = json.optJSONArray("latestRatings")
            return DentistRatingSummary(
                dentistId = json.optString("dentistId"),
                totalRatings = json.optInt("totalRatings"),
                averageScore = json.optDouble("averageScore"),
                latestRatings = if (ratings == null) {
                    emptyList()
                } else {
                    (0 until ratings.length()).map {
                        RatingItem.fromJson(ratings.getJSONObject(it))
                    }
                },
            )
        }
    }
}

data class Prescription(
    val id: String,
    val appointmentId: String,
    val dentistId: String,
    val diagnosis: String,
    val indications: String,
    val notes: String?,
    val status: String?,
    val createdAt: OffsetDateTime?,
) {
    companion object {
        fun fromJson(json: JSONObject): Prescription = Prescription(
            id = json.optString("id"),
            appointmentId = json.optString("appointmentId"),
            dentistId = json.optString("dentistId"),
            diagnosis = json.optString("diagnosis"),
            indications = json.optString("indications"),
            notes = json.optionalString("notes"),
            status = json.optionalString("status"),
            createdAt = json.optionalString("createdAt")?.let(::parseApiDateTime),
        )
    }
}

data class ClinicalFile(
    val id: String,
    val originalName: String,
    val mimeType: String,
    val size: Long,
    val createdAt: OffsetDateTime?,
) {
    val isPdf: Boolean
        get() = mimeType == "application/pdf" ||
            originalName.endsWith(".pdf", ignoreCase = true)

    companion object {
        fun fromJson(json: JSONObject): ClinicalFile = ClinicalFile(
            id = json.optionalString("id") ?: json.optString("_id"),
            originalName = json.optionalString("originalName") ?: "Archivo clínico",
            mimeType = json.optionalString("mimeType")
                ?: json.optionalString("contentType")
                ?: "application/octet-stream",
            size = json.optLong("size"),
            createdAt = json.optionalString("createdAt")?.let(::parseApiDateTime),
        )
    }
}

data class Conversation(
    val id: String,
    val patientId: String,
    val dentistId: String,
    val lastMessagePreview: String?,
    val lastMessageAt: OffsetDateTime?,
) {
    companion object {
        fun fromJson(json: JSONObject): Conversation = Conversation(
            id = json.optionalString("id") ?: json.optString("_id"),
            patientId = json.optString("patientId"),
            dentistId = json.optString("dentistId"),
            lastMessagePreview = json.optionalString("lastMessagePreview"),
            lastMessageAt = json.optionalString("lastMessageAt")?.let(::parseApiDateTime),
        )
    }
}

data class ChatAttachment(
    val fileId: String,
    val contentType: String,
    val originalName: String,
    val size: Long,
) {
    companion object {
        fun fromJson(json: JSONObject): ChatAttachment = ChatAttachment(
            fileId = json.optString("fileId"),
            contentType = json.optionalString("contentType") ?: "application/octet-stream",
            originalName = json.optionalString("originalName") ?: "Archivo adjunto",
            size = json.optLong("size"),
        )
    }
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderRole: String,
    val body: String?,
    val attachment: ChatAttachment?,
    val createdAt: OffsetDateTime?,
) {
    companion object {
        fun fromJson(json: JSONObject): ChatMessage = ChatMessage(
            id = json.optionalString("id") ?: json.optString("_id"),
            conversationId = json.optString("conversationId"),
            senderId = json.optString("senderId"),
            senderRole = json.optString("senderRole").uppercase(),
            body = json.optionalString("body"),
            attachment = json.optJSONObject("attachment")?.let(ChatAttachment::fromJson),
            createdAt = json.optionalString("createdAt")?.let(::parseApiDateTime),
        )
    }
}

private fun parseApiDateTime(value: String): OffsetDateTime =
    runCatching { OffsetDateTime.parse(value) }
        .getOrElse {
            LocalDateTime.parse(value)
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime()
        }
