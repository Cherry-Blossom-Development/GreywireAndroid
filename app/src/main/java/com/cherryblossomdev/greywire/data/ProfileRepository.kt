package com.cherryblossomdev.greywire.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

import com.cherryblossomdev.greywire.network.BandUpdate
import com.cherryblossomdev.greywire.network.InstrumentUpdate
import com.cherryblossomdev.greywire.network.InstrumentsResponse
import com.cherryblossomdev.greywire.network.MessageOnlyResponse
import com.cherryblossomdev.greywire.network.MusicianResponse
import com.cherryblossomdev.greywire.network.MusicianUpdate
import com.cherryblossomdev.greywire.network.PhotoResponse
import com.cherryblossomdev.greywire.network.Profile
import com.cherryblossomdev.greywire.network.ProfileApiService
import com.cherryblossomdev.greywire.network.ProfileResponse
import com.cherryblossomdev.greywire.network.ProfileUpdate

class ProfileRepository(
    private val api: ProfileApiService,
    /** The same API without body logging, for the photo upload. */
    private val uploadApi: ProfileApiService,
    private val tokenManager: TokenManager
) {
    /**
     * The last profile loaded, so the Profile tab can show it straight away
     * (and keep its scroll position) when it comes back from an edit screen.
     */
    var lastProfile: Profile? = null
        private set

    suspend fun getProfile(): AuthResult<ProfileResponse> =
        authedCall(tokenManager, "Couldn't load your profile") { api.getProfile(it) }
            .also { if (it is AuthResult.Success) lastProfile = it.data.user }

    /** On sign out, so the next account doesn't briefly see this one's profile. */
    fun clearLastProfile() {
        lastProfile = null
    }

    suspend fun updateProfile(update: ProfileUpdate): AuthResult<MessageOnlyResponse> =
        authedCall(tokenManager, "Couldn't save your profile") { api.updateProfile(it, update) }

    /** Uploads [jpeg] as the new profile photo. */
    suspend fun uploadPhoto(jpeg: ByteArray): AuthResult<PhotoResponse> {
        val part = MultipartBody.Part.createFormData(
            "photo",
            "profile.jpg",
            jpeg.toRequestBody("image/jpeg".toMediaType())
        )
        return authedCall(tokenManager, "Couldn't upload the photo") { uploadApi.uploadPhoto(it, part) }
    }

    suspend fun removePhoto(): AuthResult<MessageOnlyResponse> =
        authedCall(tokenManager, "Couldn't remove the photo") { api.removePhoto(it) }

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
