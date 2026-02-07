package com.rightguard.app.ui.safeguard

import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rightguard.app.data.remote.api.dto.AdviceResponse
import com.rightguard.app.data.remote.api.dto.ConversationEntry
import com.rightguard.app.data.repository.IncidentRepository
import com.rightguard.app.data.repository.UserProfileRepository
import com.rightguard.app.domain.model.AiAdviceLog
import com.rightguard.app.domain.model.EncounterType
import com.rightguard.app.domain.usecase.ActivateSafeguardModeUseCase
import com.rightguard.app.domain.usecase.DeactivateModeUseCase
import com.rightguard.app.domain.usecase.GetLegalAdviceUseCase
import com.rightguard.app.domain.usecase.ProcessVoiceCommandUseCase
import com.rightguard.app.domain.usecase.VoiceAction
import com.rightguard.app.service.VoiceCommand
import com.rightguard.app.service.VoiceCommandService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SafeguardUiState(
    val isActive: Boolean = false,
    val incidentId: Long = 0,
    val encounterType: EncounterType = EncounterType.GENERAL,
    val currentTranscription: String = "",
    val isListening: Boolean = false,
    val aiAdvice: AdviceResponse? = null,
    val isLoadingAdvice: Boolean = false,
    val conversationHistory: List<ConversationEntry> = emptyList(),
    val elapsedTimeMs: Long = 0,
    val error: String? = null
)

@HiltViewModel
class SafeguardViewModel @Inject constructor(
    private val activateSafeguard: ActivateSafeguardModeUseCase,
    private val deactivateMode: DeactivateModeUseCase,
    private val getLegalAdvice: GetLegalAdviceUseCase,
    private val processVoiceCommand: ProcessVoiceCommandUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val incidentRepository: IncidentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SafeguardUiState())
    val state: StateFlow<SafeguardUiState> = _state.asStateFlow()

    var tts: TextToSpeech? = null

    init {
        // Listen for voice commands
        viewModelScope.launch {
            VoiceCommandService.voiceCommands.collect { command ->
                handleVoiceCommand(command)
            }
        }
        // Listen for transcriptions
        viewModelScope.launch {
            VoiceCommandService.transcriptions.collect { text ->
                _state.update { it.copy(currentTranscription = text) }
                if (text.isNotBlank()) {
                    queryAiAdvice(text)
                }
            }
        }
    }

    fun activate(encounterType: EncounterType = EncounterType.GENERAL) {
        viewModelScope.launch {
            try {
                val incidentId = activateSafeguard(encounterType)
                _state.update {
                    it.copy(
                        isActive = true,
                        incidentId = incidentId,
                        encounterType = encounterType
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun endEncounter(): Long {
        val incidentId = _state.value.incidentId
        viewModelScope.launch {
            deactivateMode(incidentId)
            _state.update { it.copy(isActive = false) }
        }
        return incidentId
    }

    fun queryAiAdvice(situationDescription: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingAdvice = true) }

            val profile = userProfileRepository.getProfile()
            val result = getLegalAdvice(
                encounterType = _state.value.encounterType.name,
                state = profile?.state?.takeIf { it.length == 2 } ?: "PA",
                citizenshipStatus = profile?.citizenshipStatus?.name,
                immigrationStatus = profile?.immigrationStatus?.let {
                    if (it.name == "NOT_APPLICABLE") null else it.name
                },
                situationDescription = situationDescription,
                conversationHistory = _state.value.conversationHistory
            )

            result.onSuccess { response ->
                _state.update { current ->
                    val updatedHistory = current.conversationHistory +
                        ConversationEntry("user", situationDescription) +
                        ConversationEntry("assistant", response.advice)
                    current.copy(
                        aiAdvice = response,
                        isLoadingAdvice = false,
                        conversationHistory = updatedHistory
                    )
                }

                // Log AI advice to incident
                val incidentId = _state.value.incidentId
                if (incidentId > 0) {
                    incidentRepository.addAiAdviceLog(
                        AiAdviceLog(
                            incidentId = incidentId,
                            query = situationDescription,
                            response = response.advice,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                // Speak advice via TTS
                tts?.speak(response.advice, TextToSpeech.QUEUE_ADD, null, "advice")
            }

            result.onFailure { error ->
                _state.update { it.copy(isLoadingAdvice = false, error = error.message) }
            }
        }
    }

    fun setEncounterType(type: EncounterType) {
        _state.update { it.copy(encounterType = type) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun handleVoiceCommand(command: VoiceCommand) {
        when (val action = processVoiceCommand(command)) {
            is VoiceAction.ActivateSafeguard -> {
                if (!_state.value.isActive) activate()
            }
            is VoiceAction.ActivatePanic -> {
                // Handled by the screen
            }
            is VoiceAction.EndEncounter -> {
                if (_state.value.isActive) endEncounter()
            }
            is VoiceAction.SendAlert -> {
                // Handled by AlertService
            }
            is VoiceAction.QueryAi -> {
                if (action.text.isNotBlank()) queryAiAdvice(action.text)
            }
        }
    }
}
