package com.dentia.patient.data.network

class ApiException(
    val statusCode: Int,
    override val message: String,
    val requiresEmailVerification: Boolean = false,
    val email: String? = null,
) : Exception(message)

