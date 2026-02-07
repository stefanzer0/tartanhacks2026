package com.rightguard.app.data.remote.api.dto

import com.google.gson.annotations.SerializedName

data class AdviceResponse(
    val advice: String,
    val rights: List<String>,
    @SerializedName("de_escalation_tips") val deEscalationTips: List<String>,
    @SerializedName("legal_references") val legalReferences: List<String>
)
