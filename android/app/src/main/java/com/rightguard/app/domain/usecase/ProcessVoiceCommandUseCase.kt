package com.rightguard.app.domain.usecase

import com.rightguard.app.service.VoiceCommand
import javax.inject.Inject

sealed class VoiceAction {
    data object ActivateSafeguard : VoiceAction()
    data object ActivatePanic : VoiceAction()
    data object EndEncounter : VoiceAction()
    data object SendAlert : VoiceAction()
    data class QueryAi(val text: String) : VoiceAction()
}

class ProcessVoiceCommandUseCase @Inject constructor() {

    operator fun invoke(command: VoiceCommand): VoiceAction {
        return when (command) {
            VoiceCommand.ACTIVATE_SAFEGUARD -> VoiceAction.ActivateSafeguard
            VoiceCommand.ACTIVATE_PANIC -> VoiceAction.ActivatePanic
            VoiceCommand.END_ENCOUNTER -> VoiceAction.EndEncounter
            VoiceCommand.SEND_ALERT -> VoiceAction.SendAlert
            VoiceCommand.QUERY_RIGHTS -> VoiceAction.QueryAi("What are my rights in this situation?")
            VoiceCommand.WAKE -> VoiceAction.QueryAi("") // Wake word, ready for next input
        }
    }

    fun processTranscription(text: String): VoiceAction {
        return VoiceAction.QueryAi(text)
    }
}
