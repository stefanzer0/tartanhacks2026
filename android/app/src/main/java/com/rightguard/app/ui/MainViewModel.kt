package com.rightguard.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rightguard.app.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userProfileRepository: UserProfileRepository
) : ViewModel() {

    val isOnboarded: StateFlow<Boolean> = userProfileRepository.observeProfile()
        .map { it?.onboardingCompleted == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}
