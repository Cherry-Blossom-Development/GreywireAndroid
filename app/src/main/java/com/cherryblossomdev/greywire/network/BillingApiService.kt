package com.cherryblossomdev.greywire.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

// Billing endpoints (Breakroom/backend/routes/billing.js). One subscription
// covers both Prosaurus and Greywire Studio.

data class BillingPlanResponse(
    val subscribed: Boolean = false,
    val platform: String? = null,
    val status: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("fee_percent") val feePercent: Int? = null,
    /** Only sent on the Greywire site (X-Site: greywire). */
    val greywire: MembershipResponse? = null
)

interface BillingApiService {

    @GET("api/billing/plan")
    suspend fun getPlan(@Header("Authorization") token: String): Response<BillingPlanResponse>
}
