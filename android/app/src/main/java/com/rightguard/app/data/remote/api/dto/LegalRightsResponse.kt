package com.rightguard.app.data.remote.api.dto

import com.google.gson.annotations.SerializedName

data class LegalRightsResponse(
    val state: String,
    @SerializedName("encounter_type") val encounterType: String,
    val rights: List<LegalRight>,
    @SerializedName("state_specific_notes") val stateSpecificNotes: String
)

data class LegalRight(
    val right: String,
    val description: String,
    @SerializedName("applicable_to") val applicableTo: List<String>,
    val source: String
)

data class StatesResponse(
    val states: List<StateInfo>
)

data class StateInfo(
    val code: String,
    val name: String,
    @SerializedName("recording_consent") val recordingConsent: String
)
