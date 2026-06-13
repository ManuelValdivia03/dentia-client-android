package com.dentia.patient.ui.patient

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentia.patient.data.ClinicalRecordRepository
import com.dentia.patient.data.model.ClinicalRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ClinicalRecordUiState(
    val isLoading: Boolean = false,
    val record: ClinicalRecord? = null,
    val error: String? = null,
)

class ClinicalRecordViewModel(
    private val repository: ClinicalRecordRepository,
) : ViewModel() {

    var uiState = mutableStateOf(ClinicalRecordUiState())
        private set

    private var hasLoaded = false

    fun loadClinicalRecord(force: Boolean = false) {
        if (hasLoaded && !force) return

        hasLoaded = true
        uiState.value = ClinicalRecordUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val record = withContext(Dispatchers.IO) {
                    repository.getMyClinicalRecord()
                }

                uiState.value = ClinicalRecordUiState(record = record)
            } catch (error: Exception) {
                hasLoaded = false
                uiState.value = ClinicalRecordUiState(
                    error = error.message ?: "No se pudo cargar el expediente clínico.",
                )
            }
        }
    }

    fun retry() {
        loadClinicalRecord(force = true)
    }
}