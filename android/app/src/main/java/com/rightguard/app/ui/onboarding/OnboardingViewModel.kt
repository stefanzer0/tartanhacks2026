package com.rightguard.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.favre.lib.crypto.bcrypt.BCrypt
import com.rightguard.app.data.repository.TrustedContactRepository
import com.rightguard.app.data.repository.UserProfileRepository
import com.rightguard.app.domain.model.CitizenshipStatus
import com.rightguard.app.domain.model.ImmigrationStatus
import com.rightguard.app.domain.model.TrustedContact
import com.rightguard.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val name: String = "",
    val state: String = "",
    val citizenshipStatus: CitizenshipStatus = CitizenshipStatus.PREFER_NOT_TO_SAY,
    val immigrationStatus: ImmigrationStatus = ImmigrationStatus.NOT_APPLICABLE,
    val trustedContacts: List<TrustedContact> = emptyList(),
    val pin: String = "",
    val confirmPin: String = "",
    val isNameValid: Boolean = true,
    val isStateValid: Boolean = true,
    val isPinValid: Boolean = true,
    val pinError: String = "",
    val isSaving: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val trustedContactRepository: TrustedContactRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun updateName(name: String) {
        _state.update { it.copy(name = name, isNameValid = name.isNotBlank()) }
    }

    fun updateState(state: String) {
        _state.update { it.copy(state = state, isStateValid = state.isNotBlank()) }
    }

    fun updateCitizenshipStatus(status: CitizenshipStatus) {
        _state.update { it.copy(citizenshipStatus = status) }
    }

    fun updateImmigrationStatus(status: ImmigrationStatus) {
        _state.update { it.copy(immigrationStatus = status) }
    }

    fun addTrustedContact(contact: TrustedContact) {
        _state.update { it.copy(trustedContacts = it.trustedContacts + contact) }
    }

    fun removeTrustedContact(contact: TrustedContact) {
        _state.update { it.copy(trustedContacts = it.trustedContacts - contact) }
    }

    fun updatePin(pin: String) {
        _state.update { it.copy(pin = pin, pinError = "") }
    }

    fun updateConfirmPin(confirmPin: String) {
        _state.update { it.copy(confirmPin = confirmPin, pinError = "") }
    }

    fun validateProfile(): Boolean {
        val current = _state.value
        val nameValid = current.name.isNotBlank()
        val stateValid = current.state.isNotBlank()
        _state.update { it.copy(isNameValid = nameValid, isStateValid = stateValid) }
        return nameValid && stateValid
    }

    fun validatePin(): Boolean {
        val current = _state.value
        if (current.pin.length < 4) {
            _state.update { it.copy(isPinValid = false, pinError = "PIN must be at least 4 digits") }
            return false
        }
        if (current.pin != current.confirmPin) {
            _state.update { it.copy(isPinValid = false, pinError = "PINs do not match") }
            return false
        }
        _state.update { it.copy(isPinValid = true, pinError = "") }
        return true
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val current = _state.value
            val pinHash = BCrypt.withDefaults().hashToString(12, current.pin.toCharArray())

            val profile = UserProfile(
                name = current.name,
                state = current.state,
                citizenshipStatus = current.citizenshipStatus,
                immigrationStatus = current.immigrationStatus,
                pinHash = pinHash,
                onboardingCompleted = true
            )

            userProfileRepository.saveProfile(profile)

            for (contact in current.trustedContacts) {
                trustedContactRepository.addContact(contact)
            }

            _state.update { it.copy(isSaving = false) }
            onComplete()
        }
    }

    companion object {
        val US_STATES = listOf(
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
            "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
            "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
            "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
            "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY", "DC"
        )
    }
}
