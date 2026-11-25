package com.cherryblossomdev.greywire.data

import com.cherryblossomdev.greywire.network.BandUpdate
import com.cherryblossomdev.greywire.network.InstrumentUpdate
import com.cherryblossomdev.greywire.network.InstrumentsResponse
import com.cherryblossomdev.greywire.network.MessageOnlyResponse
import com.cherryblossomdev.greywire.network.MusicianResponse
import com.cherryblossomdev.greywire.network.MusicianUpdate
import com.cherryblossomdev.greywire.network.ProfileApiService
import com.cherryblossomdev.greywire.network.ProfileResponse
import com.cherryblossomdev.greywire.network.ProfileUpdate

class ProfileRepository(
    private val api: ProfileApiService,
    private val tokenManager: TokenManager
) {
    suspend fun getProfile(): AuthResult<ProfileResponse> =
        authedCall(tokenManager, "Couldn't load your profile") { api.getProfile(it) }

    suspend fun updateProfile(update: ProfileUpdate): AuthResult<MessageOnlyResponse> =
        authedCall(tokenManager, "Couldn't save your profile") { api.updateProfile(it, update) }

    suspend fun updateMusician(update: MusicianUpdate): AuthResult<MusicianResponse> =
        authedCall(tokenManager, "Couldn't save your Musician Profile") { api.updateMusician(it, update) }

    suspend fun getInstruments(): AuthResult<InstrumentsResponse> =
        authedCall(tokenManager, "Couldn't load the instruments") { api.getInstruments(it) }

    suspend fun addInstrument(update: InstrumentUpdate): AuthResult<MusicianResponse> =
        authedCall(tokenManager, "Couldn't add the instrument") { api.addInstrument(it, update) }

    suspend fun updateInstrument(id: Int, update: InstrumentUpdate): AuthResult<MusicianResponse> =
        authedCall(tokenManager, "Couldn't save the instrument") { api.updateInstrument(it, id, update) }

    suspend fun removeInstrument(id: Int): AuthResult<MusicianResponse> =
        authedCall(tokenManager, "Couldn't remove the instrument") { api.removeInstrument(it, id) }

    suspend fun addBand(update: BandUpdate): AuthResult<MusicianResponse> =
        authedCall(tokenManager, "Couldn't add the band") { api.addBand(it, update) }

    suspend fun updateBand(id: Int, update: BandUpdate): AuthResult<MusicianResponse> =
        authedCall(tokenManager, "Couldn't save the band") { api.updateBand(it, id, update) }

    suspend fun removeBand(id: Int): AuthResult<MusicianResponse> =
        authedCall(tokenManager, "Couldn't remove the band") { api.removeBand(it, id) }
}
