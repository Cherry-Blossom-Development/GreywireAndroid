package com.cherryblossomdev.greywire.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

// Profile endpoints (Breakroom/backend/routes/profile.js). The JSON is camelCase.
// With X-Site: greywire the profile includes the Musician Profile (`musician`),
// and the web hides the Prosaurus work history (work bio, skills, jobs), so
// the app leaves those out too.

/** A profile: your own (GET /api/profile) or someone else's (GET /api/profile/user/:handle). */
data class Profile(
    val id: Int,
    val handle: String,
    val firstName: String? = null,
    val lastName: String? = null,
    /** Your own profile only. */
    val email: String? = null,
    /** Rich text (HTML) from the website's editor. Null on someone else's musician-only profile. */
    val bio: String? = null,
    /**
     * Prosaurus's work bio (your own profile only). Greywire doesn't show it,
     * but `PUT /api/profile` saves it with the names, so edits send it back unchanged.
     */
    val workBio: String? = null,
    /** Served at /api/uploads/<photoPath>. */
    val photoPath: String? = null,
    val createdAt: String? = null,
    val friendCount: Int = 0,
    /** Greywire only (X-Site: greywire). */
    val musician: MusicianProfile? = null
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").trim().ifEmpty { handle }
}

/** The Greywire Musician Profile (migration 094). */
data class MusicianProfile(
    /** Rich text (HTML). */
    val bio: String? = null,
    /** Others on Greywire see only the Musician Profile, not the general bio. */
    val musicianOnly: Boolean = false,
    val instruments: List<ProfileInstrument> = emptyList(),
    val bands: List<ProfileBand> = emptyList()
)

data class ProfileInstrument(
    /** The user_instruments row, used to edit or remove it. */
    val id: Int,
    val instrumentId: Int,
    val name: String,
    /** "beginner", "intermediate", "advanced" or "professional". */
    val skillLevel: String? = null,
    val yearsPlaying: Int? = null,
    val isPrimary: Boolean = false
) {
    /** "Advanced · 12 years", as on the web; empty when neither is set. */
    val details: String
        get() = listOfNotNull(
            skillLevel?.replaceFirstChar { it.uppercase() },
            yearsPlaying?.let { "$it ${if (it == 1) "year" else "years"}" }
        ).joinToString(" · ")
}

/** A band in the user's history (free text, not a Greywire band). */
data class ProfileBand(
    val id: Int,
    val bandName: String,
    val role: String? = null,
    val startYear: Int? = null,
    val endYear: Int? = null,
    val isCurrent: Boolean = false
) {
    /** "2019 – Present", "2015 – 2018", "From 2015"..., as on the web. */
    val years: String
        get() = when {
            isCurrent -> startYear?.let { "$it – Present" } ?: "Current"
            startYear != null && endYear != null -> if (startYear == endYear) "$startYear" else "$startYear – $endYear"
            startYear != null -> "From $startYear"
            endYear != null -> "Until $endYear"
            else -> ""
        }
}

data class ProfileResponse(val user: Profile)

/**
 * `PUT /api/profile` replaces all four fields, so send the bios as they are
 * when only the names change (null stays null).
 */
data class ProfileUpdate(
    val firstName: String,
    val lastName: String,
    val bio: String?,
    val workBio: String?
)

data class MessageOnlyResponse(val message: String? = null)

/** `PUT /api/profile/musician` replaces both, so send the bio as loaded when only the switch changes. */
data class MusicianUpdate(val bio: String, val musicianOnly: Boolean)

/** The Musician Profile endpoints answer with the whole updated Musician Profile. */
data class MusicianResponse(val musician: MusicianProfile)

interface ProfileApiService {

    /** The signed-in user's own profile. */
    @GET("api/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<ProfileResponse>

    /** Names and bios. */
    @PUT("api/profile")
    suspend fun updateProfile(@Header("Authorization") token: String, @Body body: ProfileUpdate): Response<MessageOnlyResponse>

    /** Music bio and the musician-only setting (Greywire only). */
    @PUT("api/profile/musician")
    suspend fun updateMusician(@Header("Authorization") token: String, @Body body: MusicianUpdate): Response<MusicianResponse>
}
