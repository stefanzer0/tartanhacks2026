package com.rightguard.app.ui.incident

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rightguard.app.data.repository.IncidentRepository
import com.rightguard.app.data.repository.RecordingRepository
import com.rightguard.app.domain.model.AiAdviceLog
import com.rightguard.app.domain.model.Incident
import com.rightguard.app.domain.model.Recording
import com.rightguard.app.domain.usecase.ExportPdfReportUseCase
import com.rightguard.app.domain.usecase.GenerateIncidentReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IncidentDetailUiState(
    val incident: Incident? = null,
    val recordings: List<Recording> = emptyList(),
    val aiLogs: List<AiAdviceLog> = emptyList(),
    val isExporting: Boolean = false,
    val exportSuccess: Boolean? = null,
    val error: String? = null
)

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val incidentRepository: IncidentRepository,
    private val recordingRepository: RecordingRepository,
    private val exportPdfReportUseCase: ExportPdfReportUseCase,
    private val generateIncidentReportUseCase: GenerateIncidentReportUseCase
) : ViewModel() {

    private val incidentId: Long = savedStateHandle["incidentId"] ?: 0L

    private val _state = MutableStateFlow(IncidentDetailUiState())
    val state: StateFlow<IncidentDetailUiState> = _state.asStateFlow()

    init {
        loadIncident()
    }

    fun loadIncident() {
        viewModelScope.launch {
            try {
                val incident = incidentRepository.getById(incidentId)
                val recordings = recordingRepository.getByIncidentId(incidentId)
                val aiLogs = incidentRepository.getAiAdviceLogsForIncident(incidentId)
                _state.update {
                    it.copy(
                        incident = incident,
                        recordings = recordings,
                        aiLogs = aiLogs,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun exportPdf(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, exportSuccess = null) }
            try {
                val success = exportPdfReportUseCase(incidentId, uri)
                _state.update { it.copy(isExporting = false, exportSuccess = success) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isExporting = false,
                        exportSuccess = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun clearExportResult() {
        _state.update { it.copy(exportSuccess = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
