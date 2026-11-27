package com.cherryblossomdev.greywire.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Friends endpoints (Breakroom/backend/routes/friends.js). With X-Site: greywire
// the lists only include Greywire users (used Greywire, or a member).

/** A person in a friends list or a friend request: the user's id, not the request's. */
data class FriendUser(
    val id: Int,
    val handle: String,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("photo_path") val photoPath: String? = null,
    /** Friends list only. */
    @SerializedName("friends_since") val friendsSince: String? = null,
    /** Friends list only. */
    @SerializedName("is_online") val isOnline: Boolean = false,
    /** Requests only. */
    @SerializedName("requested_at") val requestedAt: String? = null
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").trim().ifEmpty { handle }
}

data class FriendsListResponse(val friends: List<FriendUser> = emptyList())

data class FriendRequestsResponse(val requests: List<FriendUser> = emptyList())

data class SentRequestsResponse(val sent: List<FriendUser> = emptyList())

data class FindUsersResponse(val users: List<FriendUser> = emptyList())

data class MessageResponse(val message: String? = null)

interface FriendsApiService {

    @GET("api/friends")
    suspend fun getFriends(@Header("Authorization") token: String): Response<FriendsListResponse>

    @GET("api/friends/requests")
    suspend fun getRequests(@Header("Authorization") token: String): Response<FriendRequestsResponse>

    @GET("api/friends/sent")
    suspend fun getSent(@Header("Authorization") token: String): Response<SentRequestsResponse>

    /**
     * People to add: matches handle or name, leaving out yourself and anyone
     * you're already connected to (friends, requests, blocks). At least 2 characters.
     */
    @GET("api/friends/find")
    suspend fun find(@Header("Authorization") token: String, @Query("q") query: String): Response<FindUsersResponse>

    @POST("api/friends/request/{userId}")
    suspend fun sendRequest(@Header("Authorization") token: String, @Path("userId") userId: Int): Response<MessageResponse>

    @POST("api/friends/accept/{userId}")
    suspend fun accept(@Header("Authorization") token: String, @Path("userId") userId: Int): Response<MessageResponse>

    @POST("api/friends/decline/{userId}")
    suspend fun decline(@Header("Authorization") token: String, @Path("userId") userId: Int): Response<MessageResponse>

    @DELETE("api/friends/request/{userId}")
    suspend fun cancelRequest(@Header("Authorization") token: String, @Path("userId") userId: Int): Response<MessageResponse>

    @DELETE("api/friends/{userId}")
    suspend fun remove(@Header("Authorization") token: String, @Path("userId") userId: Int): Response<MessageResponse>

    /** Clears the "new requests" badge on the web and in Prosaurus. */
    @POST("api/friends/mark-seen")
    suspend fun markSeen(@Header("Authorization") token: String): Response<MessageResponse>
}
