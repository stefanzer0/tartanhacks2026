package com.rightguard.app.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rightguard.app.data.repository.TrustedContactRepository
import com.rightguard.app.data.repository.UserProfileRepository
import com.rightguard.app.domain.model.TrustedContact
import com.rightguard.app.domain.model.UserProfile
import com.rightguard.app.receiver.PanicModeDeviceAdminReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val profile: UserProfile? = null,
    val contacts: List<TrustedContact> = emptyList(),
    val isDeviceAdminActive: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val trustedContactRepository: TrustedContactRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileRepository.observeProfile().collect { profile ->
                _state.update { it.copy(profile = profile) }
            }
        }
        viewModelScope.launch {
            trustedContactRepository.observeAll().collect { contacts ->
                _state.update { it.copy(contacts = contacts) }
            }
        }
        checkDeviceAdmin()
    }

    fun checkDeviceAdmin() {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, PanicModeDeviceAdminReceiver::class.java)
        _state.update { it.copy(isDeviceAdminActive = dpm.isAdminActive(adminComponent)) }
    }

    fun addContact(contact: TrustedContact) {
        viewModelScope.launch {
            trustedContactRepository.addContact(contact)
        }
    }

    fun removeContact(contact: TrustedContact) {
        viewModelScope.launch {
            trustedContactRepository.removeContact(contact)
        }
    }
}
