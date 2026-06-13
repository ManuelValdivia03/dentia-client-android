package com.dentia.patient.data.model

import org.json.JSONObject

data class ClinicalRecord(
    val id: String,
    val patientId: String,
    val bloodType: String?,
    val allergies: String?,
    val chronicDiseases: String?,
    val currentMedications: String?,
    val surgicalHistory: String?,
    val familyHistory: String?,
    val dentalHistory: String?,
    val riskNotes: String?,
    val encounters: List<ClinicalEncounter> = emptyList(),
) {
    companion object {
        fun fromJson(json: JSONObject): ClinicalRecord {
            val encountersJson = json.optJSONArray("encounters")
            val encounters = if (encountersJson != null) {
                (0 until encountersJson.length()).mapNotNull { index ->
                    encountersJson.optJSONObject(index)?.let(ClinicalEncounter::fromJson)
                }
            } else {
                emptyList()
            }

            return ClinicalRecord(
                id = json.optString("id"),
                patientId = json.optString("patientId"),
                bloodType = json.optNullableString("bloodType"),
                allergies = json.optNullableString("allergies"),
                chronicDiseases = json.optNullableString("chronicDiseases"),
                currentMedications = json.optNullableString("currentMedications"),
                surgicalHistory = json.optNullableString("surgicalHistory"),
                familyHistory = json.optNullableString("familyHistory"),
                dentalHistory = json.optNullableString("dentalHistory"),
                riskNotes = json.optNullableString("riskNotes"),
                encounters = encounters,
            )
        }
    }
}

data class ClinicalEncounter(
    val id: String,
    val patientId: String,
    val dentistId: String,
    val appointmentId: String?,
    val reasonForVisit: String?,
    val arrivalDescription: String?,
    val symptoms: String?,
    val diagnosis: String?,
    val treatmentPerformed: String?,
    val treatmentPlan: String?,
    val observations: String?,
    val createdAt: String?,
) {
    companion object {
        fun fromJson(json: JSONObject): ClinicalEncounter {
            return ClinicalEncounter(
                id = json.optString("id"),
                patientId = json.optString("patientId"),
                dentistId = json.optString("dentistId"),
                appointmentId = json.optNullableString("appointmentId"),
                reasonForVisit = json.optNullableString("reasonForVisit"),
                arrivalDescription = json.optNullableString("arrivalDescription"),
                symptoms = json.optNullableString("symptoms"),
                diagnosis = json.optNullableString("diagnosis"),
                treatmentPerformed = json.optNullableString("treatmentPerformed"),
                treatmentPlan = json.optNullableString("treatmentPlan"),
                observations = json.optNullableString("observations"),
                createdAt = json.optNullableString("createdAt"),
            )
        }
    }
}

private fun JSONObject.optNullableString(name: String): String? {
    if (isNull(name)) return null
    return optString(name).takeIf { it.isNotBlank() && it != "null" }
}