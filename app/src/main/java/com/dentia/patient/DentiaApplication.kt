package com.dentia.patient

import android.app.Application
import com.dentia.patient.data.AuthRepository
import com.dentia.patient.data.ClinicalRecordRepository
import com.dentia.patient.data.PatientRepository
import com.dentia.patient.data.network.DentiaHttpClient
import com.dentia.patient.data.session.SecureSessionStore

class DentiaApplication : Application() {
    lateinit var authRepository: AuthRepository
        private set

    lateinit var patientRepository: PatientRepository
        private set

    lateinit var clinicalRecordRepository: ClinicalRecordRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val sessionStore = SecureSessionStore(this)
        val httpClient = DentiaHttpClient(sessionStore)

        authRepository = AuthRepository(
            httpClient = httpClient,
            sessionStore = sessionStore,
        )

        patientRepository = PatientRepository(httpClient)

        clinicalRecordRepository = ClinicalRecordRepository(httpClient)
    }
}
