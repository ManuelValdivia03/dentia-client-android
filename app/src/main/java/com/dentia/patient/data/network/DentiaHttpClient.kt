package com.dentia.patient.data.network

import com.dentia.patient.BuildConfig
import com.dentia.patient.data.session.SecureSessionStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import com.dentia.patient.data.network.ClinicalRecordApi

data class ApiResponse(
    val statusCode: Int,
    val body: String,
)

data class BinaryResponse(
    val statusCode: Int,
    val bytes: ByteArray,
    val contentType: String?,
)

class DentiaHttpClient(
    private val sessionStore: SecureSessionStore,
) {
    fun get(path: String, authenticated: Boolean = true): ApiResponse =
        execute("GET", path, authenticated = authenticated)

    fun post(
        path: String,
        json: JSONObject? = null,
        authenticated: Boolean = false,
        retryOnUnauthorized: Boolean = true,
    ): ApiResponse = execute(
        method = "POST",
        path = path,
        json = json,
        authenticated = authenticated,
        retryOnUnauthorized = retryOnUnauthorized,
    )

    fun patch(
        path: String,
        json: JSONObject? = null,
        authenticated: Boolean = true,
    ): ApiResponse = execute(
        method = "PATCH",
        path = path,
        json = json,
        authenticated = authenticated,
    )

    fun delete(path: String): ApiResponse = execute(
        method = "DELETE",
        path = path,
        authenticated = true,
    )

    fun download(path: String): BinaryResponse {
        var response = rawBinaryRequest(path)
        if (response.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            val refreshed = runCatching { refreshSession() }.getOrDefault(false)
            if (refreshed) {
                response = rawBinaryRequest(path)
            }
        }
        if (response.statusCode !in 200..299) {
            throw ApiException(
                statusCode = response.statusCode,
                message = "No se pudo descargar el documento.",
            )
        }
        return response
    }

    fun postMultipart(
        path: String,
        fields: Map<String, String>,
    ): ApiResponse {
        val boundary = "DentiaBoundary${System.currentTimeMillis()}"
        val body = buildString {
            fields.forEach { (name, value) ->
                append("--$boundary\r\n")
                append("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                append(value)
                append("\r\n")
            }
            append("--$boundary--\r\n")
        }.toByteArray(Charsets.UTF_8)

        return execute(
            method = "POST",
            path = path,
            authenticated = false,
            contentType = "multipart/form-data; boundary=$boundary",
            body = body,
        )
    }

    fun postMultipartFile(
        path: String,
        fieldName: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray,
        fields: Map<String, String> = emptyMap(),
    ): ApiResponse {
        val boundary = "DentiaBoundary${System.currentTimeMillis()}"
        val lineBreak = "\r\n"
        val output = ByteArrayOutputStream()

        fields.forEach { (name, value) ->
            output.write("--$boundary$lineBreak".toByteArray())
            output.write(
                "Content-Disposition: form-data; name=\"$name\"$lineBreak$lineBreak"
                    .toByteArray(),
            )
            output.write(value.toByteArray())
            output.write(lineBreak.toByteArray())
        }

        val safeFileName = fileName.replace("\"", "")
        output.write("--$boundary$lineBreak".toByteArray())
        output.write(
            "Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$safeFileName\"$lineBreak"
                .toByteArray(),
        )
        output.write("Content-Type: $contentType$lineBreak$lineBreak".toByteArray())
        output.write(bytes)
        output.write(lineBreak.toByteArray())
        output.write("--$boundary--$lineBreak".toByteArray())

        return execute(
            method = "POST",
            path = path,
            authenticated = true,
            contentType = "multipart/form-data; boundary=$boundary",
            body = output.toByteArray(),
        )
    }

    fun patchMultipart(
        path: String,
        fields: Map<String, String>,
        fieldName: String? = null,
        fileName: String? = null,
        fileContentType: String? = null,
        fileBytes: ByteArray? = null,
    ): ApiResponse {
        val boundary = "DentiaBoundary${System.currentTimeMillis()}"
        val lineBreak = "\r\n"
        val output = ByteArrayOutputStream()

        fields.forEach { (name, value) ->
            output.write("--$boundary$lineBreak".toByteArray())
            output.write(
                "Content-Disposition: form-data; name=\"$name\"$lineBreak$lineBreak"
                    .toByteArray(),
            )
            output.write(value.toByteArray())
            output.write(lineBreak.toByteArray())
        }

        if (
            fieldName != null &&
            fileName != null &&
            fileContentType != null &&
            fileBytes != null
        ) {
            val safeFileName = fileName.replace("\"", "")
            output.write("--$boundary$lineBreak".toByteArray())
            output.write(
                "Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$safeFileName\"$lineBreak"
                    .toByteArray(),
            )
            output.write("Content-Type: $fileContentType$lineBreak$lineBreak".toByteArray())
            output.write(fileBytes)
            output.write(lineBreak.toByteArray())
        }

        output.write("--$boundary--$lineBreak".toByteArray())
        return execute(
            method = "PATCH",
            path = path,
            authenticated = true,
            contentType = "multipart/form-data; boundary=$boundary",
            body = output.toByteArray(),
        )
    }

    fun postMultipartFields(
        path: String,
        fields: Map<String, String>,
    ): ApiResponse {
        val boundary = "DentiaBoundary${System.currentTimeMillis()}"
        val body = buildString {
            fields.forEach { (name, value) ->
                append("--$boundary\r\n")
                append("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                append(value)
                append("\r\n")
            }
            append("--$boundary--\r\n")
        }.toByteArray(Charsets.UTF_8)

        return execute(
            method = "POST",
            path = path,
            authenticated = true,
            contentType = "multipart/form-data; boundary=$boundary",
            body = body,
        )
    }

    private fun execute(
        method: String,
        path: String,
        json: JSONObject? = null,
        authenticated: Boolean,
        retryOnUnauthorized: Boolean = true,
        contentType: String = "application/json; charset=UTF-8",
        body: ByteArray? = json?.toString()?.toByteArray(Charsets.UTF_8),
    ): ApiResponse {
        val response = rawRequest(
            method = method,
            path = path,
            authenticated = authenticated,
            contentType = contentType,
            body = body,
        )

        if (
            response.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED &&
            authenticated &&
            retryOnUnauthorized &&
            path != "/auth/refresh"
        ) {
            val refreshed = runCatching { refreshSession() }.getOrDefault(false)
            if (refreshed) {
                return execute(
                    method = method,
                    path = path,
                    authenticated = authenticated,
                    retryOnUnauthorized = false,
                    contentType = contentType,
                    body = body,
                )
            }
        }

        if (response.statusCode !in 200..299) {
            throw parseApiException(response)
        }

        return response
    }

    private fun rawRequest(
        method: String,
        path: String,
        authenticated: Boolean,
        contentType: String,
        body: ByteArray?,
    ): ApiResponse {
        val connection = (URL("${BuildConfig.API_BASE_URL}$path").openConnection() as HttpURLConnection)
            .apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", contentType)

                if (authenticated) {
                    sessionStore.accessToken?.let {
                        setRequestProperty("Authorization", "Bearer $it")
                    }
                }

                if (path.startsWith("/auth/")) {
                    sessionStore.refreshCookie?.let {
                        setRequestProperty("Cookie", it)
                    }
                }

                if (body != null) {
                    doOutput = true
                    outputStream.use { it.write(body) }
                }
            }

        return try {
            val status = connection.responseCode
            saveRefreshCookie(connection.headerFields)
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            ApiResponse(status, responseBody)
        } catch (error: SocketTimeoutException) {
            throw ApiException(
                statusCode = 0,
                message = "La solicitud tardó demasiado. Verifica tu conexión e inténtalo nuevamente.",
            )
        } catch (error: IOException) {
            throw ApiException(
                statusCode = 0,
                message = "No fue posible conectar con el servicio. Verifica tu conexión e inténtalo nuevamente.",
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun rawBinaryRequest(path: String): BinaryResponse {
        val connection = (URL("${BuildConfig.API_BASE_URL}$path").openConnection() as HttpURLConnection)
            .apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 30_000
                setRequestProperty("Accept", "*/*")
                sessionStore.accessToken?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                }
            }

        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            BinaryResponse(
                statusCode = status,
                bytes = stream?.use { it.readBytes() } ?: byteArrayOf(),
                contentType = connection.contentType,
            )
        } catch (error: SocketTimeoutException) {
            throw ApiException(
                statusCode = 0,
                message = "La descarga tardó demasiado. Verifica tu conexión e inténtalo nuevamente.",
            )
        } catch (error: IOException) {
            throw ApiException(
                statusCode = 0,
                message = "No fue posible descargar el archivo. Verifica tu conexión e inténtalo nuevamente.",
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun refreshSession(): Boolean {
        if (sessionStore.refreshCookie.isNullOrBlank()) return false

        val response = rawRequest(
            method = "POST",
            path = "/auth/refresh",
            authenticated = false,
            contentType = "application/json; charset=UTF-8",
            body = null,
        )

        if (response.statusCode !in 200..299) {
            sessionStore.clear()
            return false
        }

        val json = JSONObject(response.body)
        val token = json.optString("accessToken")
        val user = json.optJSONObject("user")
        if (token.isBlank() || user == null) return false

        sessionStore.saveSession(
            token,
            com.dentia.patient.data.model.AuthUser.fromJson(user),
        )
        return true
    }

    private fun saveRefreshCookie(headers: Map<String?, List<String>>) {
        val setCookie = headers.entries
            .firstOrNull { it.key?.equals("Set-Cookie", ignoreCase = true) == true }
            ?.value
            ?.firstOrNull()
            ?: return

        val cookie = setCookie.substringBefore(";")
        if (cookie.substringAfter("=", "").isBlank()) {
            sessionStore.refreshCookie = null
        } else {
            sessionStore.refreshCookie = cookie
        }
    }

    private fun parseApiException(response: ApiResponse): ApiException {
        val json = runCatching { JSONObject(response.body) }.getOrNull()
        val rawMessage = json?.opt("message")
        val nested = rawMessage as? JSONObject
        val rawParsedMessage = when (rawMessage) {
            is String -> rawMessage
            is JSONArray -> (0 until rawMessage.length())
                .joinToString("\n") { rawMessage.optString(it) }
            is JSONObject -> rawMessage.optString("message")
            else -> json?.optString("error")
        }.takeUnless { it.isNullOrBlank() } ?: "No se pudo completar la solicitud."
        val message = translateApiMessage(response.statusCode, rawParsedMessage)

        return ApiException(
            statusCode = response.statusCode,
            message = message,
            requiresEmailVerification =
                json?.optBoolean("requiresEmailVerification") == true ||
                    nested?.optBoolean("requiresEmailVerification") == true,
            email = json?.optString("email")?.takeIf { it.isNotBlank() }
                ?: nested?.optString("email")?.takeIf { it.isNotBlank() },
        )
    }

    private fun translateApiMessage(statusCode: Int, message: String): String = when {
        message.equals("Authorization header is required", true) ->
            "Tu sesión no es válida. Inicia sesión nuevamente."
        message.equals("Conversation not found", true) ->
            "No se encontró la conversación."
        message.equals("Chat service unavailable", true) ->
            "El servicio de mensajes no está disponible temporalmente."
        message.equals("Files service unavailable", true) ->
            "El servicio de archivos no está disponible temporalmente."
        message.equals("startAt and endAt must be valid dates", true) ->
            "La fecha y la hora seleccionadas no son válidas."
        message.equals("startAt must be before endAt", true) ->
            "La hora de inicio debe ser anterior a la hora de finalización."
        message.contains("startAt must be in the future", true) ->
            "Elige una fecha y hora posteriores al momento actual."
        message.equals(
            "Patient already has a pending appointment request in this time range",
            true,
        ) -> "Ya tienes una solicitud pendiente con ese dentista en ese horario."
        message.equals("Dentist already has an appointment in this time range", true) ->
            "El dentista ya tiene una cita en ese horario. Selecciona otro."
        message.equals("Only pending appointments can be rescheduled", true) ->
            "La cita ya fue confirmada o cambió de estado y no puede reprogramarse."
        message.equals("The requested resource was not found", true) ->
            "No se encontró la información solicitada."
        message.equals("An unexpected error occurred", true) ->
            "No se pudo completar la solicitud en este momento. Inténtalo más tarde."
        message.equals("Unauthorized", true) -> "No tienes autorización para realizar esta acción."
        message.equals("Forbidden", true) -> "No tienes permiso para realizar esta acción."
        message.equals("Not Found", true) -> "No se encontró la información solicitada."
        statusCode == HttpURLConnection.HTTP_UNAUTHORIZED ->
            "Tu sesión terminó. Inicia sesión nuevamente para continuar."
        statusCode == HttpURLConnection.HTTP_FORBIDDEN ->
            "No tienes permiso para realizar esta acción."
        statusCode == HttpURLConnection.HTTP_NOT_FOUND ->
            "No se encontró la información solicitada."
        statusCode == HttpURLConnection.HTTP_CONFLICT ->
            "La información cambió o ya no está disponible. Actualiza e inténtalo nuevamente."
        statusCode in 400..499 ->
            "No se pudo procesar la solicitud. Revisa los datos e inténtalo nuevamente."
        else -> "No se pudo completar la solicitud en este momento. Inténtalo más tarde."
    }
}
