package com.rightguard.app.data.remote.api

import com.rightguard.app.data.remote.api.dto.AdviceRequest
import com.rightguard.app.data.remote.api.dto.AdviceResponse
import com.rightguard.app.data.remote.api.dto.LegalRightsResponse
import com.rightguard.app.data.remote.api.dto.StatesResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RightGuardApiService {

    @POST("api/v1/gemini/advice")
    suspend fun getAdvice(@Body request: AdviceRequest): Response<AdviceResponse>

    @GET("api/v1/legal/rights")
    suspend fun getLegalRights(
        @Query("state") state: String,
        @Query("encounter_type") encounterType: String,
        @Query("status") status: String
    ): Response<LegalRightsResponse>

    @GET("api/v1/legal/states")
    suspend fun getStates(): Response<StatesResponse>
}
