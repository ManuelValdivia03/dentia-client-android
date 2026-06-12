package com.dentia.patient.data

import com.dentia.patient.data.model.AuthUser
import com.dentia.patient.data.network.DentiaHttpClient
import com.dentia.patient.data.session.SecureSessionStore
import org.json.JSONObject

class AuthRepository(
    private val httpClient: DentiaHttpClient,
    private val sessionStore: SecureSessionStore,
) {
    val storedUser: AuthUser?
        get() = sessionStore.user

    fun restoreSession(): AuthUser? {
        if (sessionStore.accessToken.isNullOrBlank()) return null
        return runCatching { getProfile() }
            .onFailure { sessionStore.clear() }
            .getOrNull()
    }

    fun login(email: String, password: String): AuthUser {
        val response = httpClient.post(
            "/auth/login",
            JSONObject()
                .put("email", email.trim())
                .put("password", password),
        )
        val json = JSONObject(response.body)
        val token = json.optString("accessToken")
        val userJson = json.optJSONObject("user")
            ?: throw IllegalStateException("El servidor no devolvió el perfil.")
        val user = AuthUser.fromJson(userJson)

        if (user.role != "PATIENT") {
            runCatching {
                httpClient.post(
                    path = "/auth/logout",
                    authenticated = true,
                    retryOnUnauthorized = false,
                )
            }
            sessionStore.clear()
            throw IllegalStateException("Esta aplicación es exclusiva para pacientes.")
        }

        sessionStore.saveSession(token, user)
        return user
    }

    fun registerPatient(fullName: String, email: String, password: String) {
        httpClient.postMultipart(
            path = "/auth/register",
            fields = mapOf(
                "fullName" to fullName.trim(),
                "email" to email.trim(),
                "password" to password,
                "role" to "PATIENT",
            ),
        )
    }

    fun verifyEmail(email: String, code: String) {
        httpClient.post(
            "/auth/verify-email",
            JSONObject().put("email", email.trim()).put("code", code),
        )
    }

    fun resendVerificationCode(email: String) {
        httpClient.post(
            "/auth/resend-verification-code",
            JSONObject().put("email", email.trim()),
        )
    }

    fun requestPasswordReset(email: String) {
        httpClient.post(
            "/auth/forgot-password",
            JSONObject().put("email", email.trim()),
        )
    }

    fun resetPassword(email: String, code: String, password: String) {
        httpClient.post(
            "/auth/reset-password",
            JSONObject()
                .put("email", email.trim())
                .put("code", code)
                .put("password", password),
        )
    }

    fun getProfile(): AuthUser {
        val response = httpClient.get("/profile")
        val user = AuthUser.fromJson(JSONObject(response.body))
        sessionStore.user = user
        return user
    }

    fun updateProfile(
        fullName: String,
        photoName: String? = null,
        photoContentType: String? = null,
        photoBytes: ByteArray? = null,
    ): AuthUser {
        val response = httpClient.patchMultipart(
            path = "/profile",
            fields = mapOf("fullName" to fullName.trim()),
            fieldName = photoBytes?.let { "photo" },
            fileName = photoName,
            fileContentType = photoContentType,
            fileBytes = photoBytes,
        )
        val user = AuthUser.fromJson(JSONObject(response.body))
        sessionStore.user = user
        return user
    }

    fun downloadProfilePhoto(photoUrl: String): ByteArray {
        val path = if (photoUrl.startsWith("http")) {
            photoUrl.substringAfter("://").substringAfter("/")
                .let { "/$it" }
        } else {
            photoUrl
        }
        return httpClient.download(path).bytes
    }

    fun logout() {
        runCatching {
            httpClient.post(
                path = "/auth/logout",
                authenticated = true,
                retryOnUnauthorized = false,
            )
        }
        sessionStore.clear()
    }
}
