package com.rightguard.app.ui.panic

import androidx.lifecycle.ViewModel
import at.favre.lib.crypto.bcrypt.BCrypt
import com.rightguard.app.data.repository.UserProfileRepository
import com.rightguard.app.domain.usecase.ActivatePanicModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PanicUiState(
    val pinInput: String = "",
    val pinError: String = "",
    val isLocked: Boolean = true
)

@HiltViewModel
class PanicViewModel @Inject constructor(
    private val activatePanic: ActivatePanicModeUseCase,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PanicUiState())
    val state: StateFlow<PanicUiState> = _state.asStateFlow()

    init {
        activatePanic()
    }

    fun updatePin(pin: String) {
        _state.update { it.copy(pinInput = pin, pinError = "") }
    }

    suspend fun attemptUnlock(): Boolean {
        val profile = userProfileRepository.getProfile() ?: return false
        val pin = _state.value.pinInput

        val result = BCrypt.verifyer().verify(pin.toCharArray(), profile.pinHash)
        return if (result.verified) {
            activatePanic.deactivate()
            _state.update { it.copy(isLocked = false) }
            true
        } else {
            _state.update { it.copy(pinError = "Incorrect PIN", pinInput = "") }
            false
        }
    }
}
