package com.dentia.patient.data.network

import com.dentia.patient.data.model.ClinicalRecord
import retrofit2.http.GET
import retrofit2.http.Header

interface ClinicalRecordApi {
    @GET("clinical-records/me")
    suspend fun getMyClinicalRecord(
        @Header("Authorization") authorization: String
    ): ClinicalRecord
}