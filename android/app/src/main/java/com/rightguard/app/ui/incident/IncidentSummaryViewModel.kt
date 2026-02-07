package com.rightguard.app.ui.incident

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rightguard.app.data.repository.IncidentRepository
import com.rightguard.app.data.repository.RecordingRepository
import com.rightguard.app.domain.model.EncounterType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IncidentSummaryUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val encounterType: EncounterType = EncounterType.GENERAL,
    val durationMs: Long = 0,
    val aiQueryCount: Int = 0,
    val recordingCount: Int = 0,
    val notes: String = ""
)

@HiltViewModel
class IncidentSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val incidentRepository: IncidentRepository,
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    private val incidentId: Long = savedStateHandle["incidentId"] ?: 0L

    private val _state = MutableStateFlow(IncidentSummaryUiState())
    val state: StateFlow<IncidentSummaryUiState> = _state.asStateFlow()

    init {
        loadSummary()
    }

    private fun loadSummary() {
        viewModelScope.launch {
            try {
                val incident = incidentRepository.getById(incidentId)
                val recordings = recordingRepository.getByIncidentId(incidentId)
                val aiLogs = incidentRepository.getAiAdviceLogsForIncident(incidentId)

                if (incident != null) {
                    val endTime = incident.endTime ?: System.currentTimeMillis()
                    val durationMs = endTime - incident.startTime

                    _state.update {
                        it.copy(
                            isLoading = false,
                            encounterType = incident.encounterType,
                            durationMs = durationMs,
                            aiQueryCount = aiLogs.size,
                            recordingCount = recordings.size,
                            notes = incident.notes
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateNotes(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    fun saveNotes() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val incident = incidentRepository.getById(incidentId)
                if (incident != null) {
                    incidentRepository.updateIncident(
                        incident.copy(notes = _state.value.notes)
                    )
                }
            } catch (_: Exception) {
                // Silently handle -- notes are best-effort
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }
}
