package com.rightguard.app.data.remote.api.dto

import com.google.gson.annotations.SerializedName

data class AdviceRequest(
    @SerializedName("encounter_type") val encounterType: String,
    val state: String,
    @SerializedName("citizenship_status") val citizenshipStatus: String? = null,
    @SerializedName("immigration_status") val immigrationStatus: String? = null,
    @SerializedName("situation_description") val situationDescription: String,
    @SerializedName("conversation_history") val conversationHistory: List<ConversationEntry> = emptyList()
)

data class ConversationEntry(
    val role: String,
    val content: String
)
