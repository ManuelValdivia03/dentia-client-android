package com.dentia.patient.data.model

import org.json.JSONObject

data class AuthUser(
    val id: String? = null,
    val domainId: String? = null,
    val email: String,
    val fullName: String? = null,
    val role: String,
    val photoUrl: String? = null,
    val emailVerified: Boolean = false,
) {
    val displayName: String
        get() = fullName?.takeIf { it.isNotBlank() } ?: email

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("domainId", domainId)
        .put("email", email)
        .put("fullName", fullName)
        .put("role", role)
        .put("photoUrl", photoUrl)
        .put("emailVerified", emailVerified)

    companion object {
        fun fromJson(json: JSONObject): AuthUser = AuthUser(
            id = json.optionalString("id") ?: json.optionalString("sub"),
            domainId = json.optionalString("domainId"),
            email = json.optString("email"),
            fullName = json.optionalString("fullName") ?: json.optionalString("name"),
            role = json.optString("role"),
            photoUrl = json.optionalString("photoUrl"),
            emailVerified = json.optBoolean("emailVerified", false),
        )
    }
}

internal fun JSONObject.optionalString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}

