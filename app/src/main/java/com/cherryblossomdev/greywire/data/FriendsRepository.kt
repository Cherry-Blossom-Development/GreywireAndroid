package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.FindUsersResponse
import com.cherryblossomdev.greywire.network.FriendRequestsResponse
import com.cherryblossomdev.greywire.network.FriendsApiService
import com.cherryblossomdev.greywire.network.FriendsListResponse
import com.cherryblossomdev.greywire.network.MessageResponse
import com.cherryblossomdev.greywire.network.SentRequestsResponse

class FriendsRepository(
    private val api: FriendsApiService,
    private val tokenManager: TokenManager
) {
    suspend fun getFriends(): AuthResult<FriendsListResponse> =
        authedCall(tokenManager, "Couldn't load your friends") { api.getFriends(it) }

    suspend fun getRequests(): AuthResult<FriendRequestsResponse> =
        authedCall(tokenManager, "Couldn't load friend requests") { api.getRequests(it) }

    suspend fun getSent(): AuthResult<SentRequestsResponse> =
        authedCall(tokenManager, "Couldn't load sent requests") { api.getSent(it) }

    suspend fun find(query: String): AuthResult<FindUsersResponse> =
        authedCall(tokenManager, "Couldn't search for people") { api.find(it, query) }

    suspend fun sendRequest(userId: Int): AuthResult<MessageResponse> =
        authedCall(tokenManager, "Couldn't send the request") { api.sendRequest(it, userId) }

    suspend fun accept(userId: Int): AuthResult<MessageResponse> =
        authedCall(tokenManager, "Couldn't accept the request") { api.accept(it, userId) }

    suspend fun decline(userId: Int): AuthResult<MessageResponse> =
        authedCall(tokenManager, "Couldn't decline the request") { api.decline(it, userId) }

    suspend fun cancelRequest(userId: Int): AuthResult<MessageResponse> =
        authedCall(tokenManager, "Couldn't cancel the request") { api.cancelRequest(it, userId) }

    suspend fun remove(userId: Int): AuthResult<MessageResponse> =
        authedCall(tokenManager, "Couldn't remove the friend") { api.remove(it, userId) }

    suspend fun markSeen(): AuthResult<MessageResponse> =
        authedCall(tokenManager, "Couldn't mark requests seen") { api.markSeen(it) }
}
