package com.cherryblossomdev.greywire.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

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

interface GreywireApiService {

    @GET("api/greywire/membership")
    suspend fun getMembership(@Header("Authorization") token: String): Response<MembershipResponse>
}
