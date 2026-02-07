package com.rightguard.app.domain.usecase

import com.rightguard.app.data.remote.api.dto.AdviceRequest
import com.rightguard.app.data.remote.api.dto.AdviceResponse
import com.rightguard.app.data.remote.api.dto.ConversationEntry
import com.rightguard.app.data.repository.LegalAdviceRepository
import javax.inject.Inject

class GetLegalAdviceUseCase @Inject constructor(
    private val legalAdviceRepository: LegalAdviceRepository
) {
    suspend operator fun invoke(
        encounterType: String,
        state: String,
        citizenshipStatus: String? = null,
        immigrationStatus: String? = null,
        situationDescription: String,
        conversationHistory: List<ConversationEntry> = emptyList()
    ): Result<AdviceResponse> {
        val request = AdviceRequest(
            encounterType = encounterType,
            state = state,
            citizenshipStatus = citizenshipStatus,
            immigrationStatus = immigrationStatus,
            situationDescription = situationDescription,
            conversationHistory = conversationHistory
        )
        return legalAdviceRepository.getAdvice(request)
    }
}
