package com.rightguard.app.domain.model

data class UserProfile(
    val id: Long = 0,
    val name: String,
    val state: String,
    val citizenshipStatus: CitizenshipStatus,
    val immigrationStatus: ImmigrationStatus,
    val pinHash: String,
    val onboardingCompleted: Boolean = false
)
