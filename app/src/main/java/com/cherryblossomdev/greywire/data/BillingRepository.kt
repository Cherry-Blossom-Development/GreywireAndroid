package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.BillingApiService
import com.cherryblossomdev.greywire.network.BillingPlanResponse

class BillingRepository(
    private val api: BillingApiService,
    private val tokenManager: TokenManager
) {
    /** The user's subscription status, shown on the paywall. */
    suspend fun getPlan(): AuthResult<BillingPlanResponse> =
        authedCall(tokenManager, "Couldn't load your plan") { api.getPlan(it) }
}
