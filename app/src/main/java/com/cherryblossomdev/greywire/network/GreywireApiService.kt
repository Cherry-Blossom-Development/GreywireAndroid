package com.cherryblossomdev.greywire.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// Greywire-specific endpoints (Breakroom/backend/routes/greywire.js).
// New Greywire backend routes belong in the Breakroom backend, under /api/greywire.

data class GreywirePedal(
    val id: Int,
    val model: String,
    val serial: String,
    val buildDate: String? = null,
    val claimedAt: String? = null
)

data class MembershipResponse(
    val member: Boolean,
    val grantedAt: String? = null,
    val pedals: List<GreywirePedal> = emptyList()
)

data class ClaimPedalRequest(val serial: String)

// 201 for a new claim, 200 when the pedal is already yours. Errors carry only `message`.
data class ClaimPedalResponse(
    val message: String? = null,
    val membership: MembershipResponse? = null
)

interface GreywireApiService {

    @GET("api/greywire/membership")
    suspend fun getMembership(@Header("Authorization") token: String): Response<MembershipResponse>

    @POST("api/greywire/claim")
    suspend fun claimPedal(
        @Header("Authorization") token: String,
        @Body request: ClaimPedalRequest
    ): Response<ClaimPedalResponse>
}
