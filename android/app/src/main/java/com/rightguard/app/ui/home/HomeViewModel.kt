package com.rightguard.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rightguard.app.data.repository.IncidentRepository
import com.rightguard.app.data.repository.UserProfileRepository
import com.rightguard.app.domain.model.AppMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * UI state for the Home screen.
 *
 * @property currentMode  The active app mode (IDLE / SAFEGUARD / PANIC).
 * @property userName     Display name from the user profile, or "User" as a
 *                        fallback.
 * @property incidentCount Total number of recorded incidents.
 */
data class HomeUiState(
    val currentMode: AppMode = AppMode.IDLE,
    val userName: String = "",
    val incidentCount: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val incidentRepository: IncidentRepository,
) : ViewModel() {

    private val _currentMode = MutableStateFlow(AppMode.IDLE)

    /**
     * Combined UI state derived from the user profile, incident list, and the
     * locally-held current mode.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        _currentMode,
        userProfileRepository.observeProfile(),
        incidentRepository.observeAll(),
    ) { mode, profile, incidents ->
        HomeUiState(
            currentMode = mode,
            userName = profile?.name.orEmpty(),
            incidentCount = incidents.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    /**
     * Transition the app into the given [mode].
     *
     * Typically called when the user taps the safeguard or panic buttons.
     */
    fun setMode(mode: AppMode) {
        _currentMode.value = mode
    }
}
