package com.dentia.patient.data

import com.dentia.patient.data.model.ClinicalRecord
import com.dentia.patient.data.network.DentiaHttpClient
import org.json.JSONObject

class ClinicalRecordRepository(
    private val httpClient: DentiaHttpClient,
) {
    fun getMyClinicalRecord(): ClinicalRecord {
        val response = httpClient.get("/clinical-records/me")
        return ClinicalRecord.fromJson(JSONObject(response.body))
    }
}