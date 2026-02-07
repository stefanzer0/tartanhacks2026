package com.rightguard.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rightguard.app.data.remote.api.RightGuardApiService
import com.rightguard.app.data.remote.api.dto.AdviceRequest
import com.rightguard.app.data.remote.api.dto.AdviceResponse
import com.rightguard.app.data.remote.api.dto.LegalRightsResponse
import com.rightguard.app.data.remote.api.dto.StatesResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegalAdviceRepository @Inject constructor(
    private val apiService: RightGuardApiService,
    private val context: Context
) {
    private val gson = Gson()

    suspend fun getAdvice(request: AdviceRequest): Result<AdviceResponse> {
        return try {
            val response = apiService.getAdvice(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "No error details"
                Result.failure(Exception("API error ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            // Fallback to offline knowledge
            getOfflineFallback(request)
        }
    }

    suspend fun getLegalRights(
        state: String,
        encounterType: String,
        status: String
    ): Result<LegalRightsResponse> {
        return try {
            val response = apiService.getLegalRights(state, encounterType, status)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            getOfflineRights(state, encounterType)
        }
    }

    suspend fun getStates(): Result<StatesResponse> {
        return try {
            val response = apiService.getStates()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getOfflineFallback(request: AdviceRequest): Result<AdviceResponse> {
        return try {
            val json = context.assets.open("legal_knowledge_fallback.json")
                .bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(json, type)

            @Suppress("UNCHECKED_CAST")
            val encounters = data["encounters"] as? Map<String, Any> ?: emptyMap()
            val encounter = encounters[request.encounterType] as? Map<String, Any>

            val rights = (encounter?.get("rights") as? List<String>) ?: listOf(
                "You have the right to remain silent (5th Amendment)",
                "You have the right to refuse consent to a search (4th Amendment)",
                "You have the right to record police in public"
            )
            val tips = (encounter?.get("de_escalation_tips") as? List<String>) ?: listOf(
                "Stay calm and keep your hands visible",
                "Be polite but firm about your rights",
                "Do not physically resist"
            )

            Result.success(
                AdviceResponse(
                    advice = "Offline mode: General rights guidance loaded from local database.",
                    rights = rights,
                    deEscalationTips = tips,
                    legalReferences = listOf("U.S. Constitution, 4th & 5th Amendments")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getOfflineRights(state: String, encounterType: String): Result<LegalRightsResponse> {
        return Result.failure(Exception("Offline rights lookup not available for $state/$encounterType"))
    }
}
