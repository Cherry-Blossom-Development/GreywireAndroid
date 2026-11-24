package com.cherryblossomdev.greywire.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.data.SessionsRepository
import com.cherryblossomdev.greywire.network.Session
import com.cherryblossomdev.greywire.network.formatDuration
import com.cherryblossomdev.greywire.ui.components.EmptyStateCard
import com.cherryblossomdev.greywire.ui.components.ErrorText
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireCard
import com.cherryblossomdev.greywire.ui.components.GreywireSectionPicker
import com.cherryblossomdev.greywire.ui.components.GreywireTabFrame
import com.cherryblossomdev.greywire.ui.components.GreywireTopBar
import com.cherryblossomdev.greywire.ui.components.InitialAvatar
import com.cherryblossomdev.greywire.ui.components.LoadingFill
import com.cherryblossomdev.greywire.ui.components.SectionOption
import com.cherryblossomdev.greywire.ui.components.StatusPill
import com.cherryblossomdev.greywire.ui.components.countLabel
import com.cherryblossomdev.greywire.ui.components.readableWidth
import com.cherryblossomdev.greywire.ui.components.formatCalendarDate
import com.cherryblossomdev.greywire.ui.components.formatShortDate
import com.cherryblossomdev.greywire.ui.theme.GreywireBg
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface2
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

// The Sessions tab (roadmap 2.2 and 2.4, in part): the user's recordings and
// their bandmates', playback, and 1-10 ratings. A session opens inside the
// tab, as on iOS. Uploading, recording, comments and sharing come later.

enum class SessionsSection { Mine, Band }

data class SessionsUiState(
    val mySessions: List<Session> = emptyList(),
    val bandSessions: List<Session> = emptyList(),
    val section: SessionsSection = SessionsSection.Mine,
    /** The session being shown, if any. */
    val openSession: Session? = null,
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class SessionsViewModel(private val repository: SessionsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState

    fun selectSection(section: SessionsSection) {
        _uiState.value = _uiState.value.copy(section = section)
    }

    fun open(session: Session) {
        _uiState.value = _uiState.value.copy(openSession = session)
    }

    /** Back from a session: reload, so a new rating shows in the list. */
    fun close() {
        _uiState.value = _uiState.value.copy(openSession = null)
        load()
    }

    fun onSessionExpired() {
        _uiState.value = _uiState.value.copy(sessionExpired = true)
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val mine = async { repository.getSessions() }
            val band = async { repository.getBandMemberSessions() }
            val results = listOf(mine.await(), band.await())

            if (results.any { it is AuthResult.Error && it.unauthorized }) {
                _uiState.value = _uiState.value.copy(isLoading = false, sessionExpired = true)
                return@launch
            }
            val state = _uiState.value
            _uiState.value = state.copy(
                mySessions = (mine.await() as? AuthResult.Success)?.data?.sessions ?: state.mySessions,
                bandSessions = (band.await() as? AuthResult.Success)?.data?.sessions ?: state.bandSessions,
                isLoading = false,
                hasLoaded = true,
                errorMessage = results.filterIsInstance<AuthResult.Error>().firstOrNull()?.message
            )
        }
    }
}

data class SessionDetailUiState(
    val myRating: Int? = null,
    val avgRating: Double? = null,
    val ratingCount: Int = 0,
    val isRating: Boolean = false,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class SessionDetailViewModel(
    private val repository: SessionsRepository,
    val session: Session
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SessionDetailUiState(myRating = session.myRating, avgRating = session.avgRating, ratingCount = session.ratingCount)
    )
    val uiState: StateFlow<SessionDetailUiState> = _uiState

    val streamUrl: String get() = repository.streamUrl(session.id)

    fun mediaHttpClient() = repository.mediaHttpClient()

    /** Tapping the current rating clears it, as on iOS and the web. */
    fun tapRating(rating: Int) {
        val state = _uiState.value
        if (state.isRating) return
        val newRating = if (state.myRating == rating) null else rating
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRating = true, errorMessage = null)
            when (val result = repository.rate(session.id, newRating)) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                    myRating = result.data.myRating,
                    avgRating = result.data.avgRating,
                    ratingCount = result.data.ratingCount,
                    isRating = false
                )
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(
                    isRating = false,
                    sessionExpired = result.unauthorized,
                    errorMessage = result.message
                )
            }
        }
    }
}

@Composable
fun SessionsScreen(viewModel: SessionsViewModel, repository: SessionsRepository, onSessionExpired: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }

    val openSession = uiState.openSession
    if (openSession != null) {
        BackHandler(onBack = viewModel::close)
        val detailViewModel = remember(openSession.id) { SessionDetailViewModel(repository, openSession) }
        SessionDetailScreen(detailViewModel, onBack = viewModel::close, onSessionExpired = viewModel::onSessionExpired)
    } else {
        SessionsListScreen(uiState, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionsListScreen(uiState: SessionsUiState, viewModel: SessionsViewModel) {
    GreywireTabFrame(
        title = "Sessions",
        testTag = "sessions-screen",
        header = {
            GreywireSectionPicker(
                options = listOf(
                    SectionOption(SessionsSection.Mine, "My Sessions", badge = uiState.mySessions.size, testTag = "sessions-section-mine"),
                    SectionOption(SessionsSection.Band, "Band", badge = uiState.bandSessions.size, testTag = "sessions-section-band")
                ),
                selected = uiState.section,
                onSelect = viewModel::selectSection,
                mutedBadges = true
            )
        }
    ) {
        if (!uiState.hasLoaded && uiState.isLoading) {
            LoadingFill()
            return@GreywireTabFrame
        }
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::load,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .readableWidth()
                    .padding(16.dp)
            ) {
                uiState.errorMessage?.let { ErrorText(it, Modifier.testTag("sessions-error")) }
                when (uiState.section) {
                    SessionsSection.Mine -> if (uiState.mySessions.isEmpty()) {
                        EmptyStateCard(
                            "No recordings yet",
                            "Your band practices and solo takes will appear here. Upload from the web to get started."
                        )
                    } else {
                        Eyebrow(countLabel(uiState.mySessions.size, "recording"))
                        Spacer(Modifier.height(2.dp))
                        uiState.mySessions.forEach { session ->
                            SessionRow(session, showUploader = false) { viewModel.open(session) }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    SessionsSection.Band -> if (uiState.bandSessions.isEmpty()) {
                        EmptyStateCard("No band recordings", "Join a band to see recordings from your bandmates here.")
                    } else {
                        Eyebrow(countLabel(uiState.bandSessions.size, "recording") + " from bandmates")
                        Spacer(Modifier.height(2.dp))
                        uiState.bandSessions.forEach { session ->
                            SessionRow(session, showUploader = true) { viewModel.open(session) }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: Session, showUploader: Boolean, onClick: () -> Unit) {
    GreywireCard(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("session-${session.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(session.displayName, icon = Icons.Filled.GraphicEq)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.displayName, style = MaterialTheme.typography.titleMedium, color = GreywireText, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(session.formattedDuration, style = MonoSmallStyle, color = GreywireMuted)
                    Spacer(Modifier.width(8.dp))
                    Text(session.typeLabel, style = MaterialTheme.typography.bodySmall, color = GreywireMuted)
                    if (showUploader && session.uploaderHandle != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "by @${session.uploaderHandle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GreywireMuted,
                            maxLines = 1
                        )
                    }
                }
            }
            session.avgRating?.let { avg ->
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Star, contentDescription = "Rating", tint = GreywireCopper, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(formatRating(avg), style = MonoSmallStyle, color = GreywireText)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = GreywireMuted)
        }
    }
}

@Composable
private fun SessionDetailScreen(viewModel: SessionDetailViewModel, onBack: () -> Unit, onSessionExpired: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val session = viewModel.session

    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().testTag("session-detail-screen")) {
        GreywireTopBar(title = "Session", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .readableWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                session.displayName,
                style = MaterialTheme.typography.headlineMedium,
                color = GreywireText,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(session.typeLabel)
                session.instrumentName?.let { StatusPill(it) }
                session.bandName?.let { StatusPill(it) }
            }
            Spacer(Modifier.height(24.dp))

            PlayerCard(viewModel)
            Spacer(Modifier.height(24.dp))

            SectionDivider()
            Eyebrow("Rating")
            Spacer(Modifier.height(2.dp))
            RatingCard(uiState, onTap = viewModel::tapRating)
            uiState.errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                ErrorText(it, Modifier.testTag("rating-error"))
            }
            Spacer(Modifier.height(24.dp))

            SectionDivider()
            Eyebrow("Details")
            Spacer(Modifier.height(2.dp))
            GreywireCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    session.recordedAt?.let { DetailRow("Recorded", formatCalendarDate(it)) }
                    session.uploadedAt?.let { DetailRow("Uploaded", formatShortDate(it)) }
                    session.uploaderHandle?.let { DetailRow("By", "@$it") }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

/**
 * Plays the session's stream with ExoPlayer, sending the user's token. The
 * player is created on the first tap of Play and released when the screen goes.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlayerCard(viewModel: SessionDetailViewModel) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(viewModel.session.durationMs ?: 0L) }

    DisposableEffect(Unit) {
        onDispose { player?.release() }
    }
    // Move the elapsed time along while playing
    LaunchedEffect(player, isPlaying) {
        val p = player ?: return@LaunchedEffect
        while (isPlaying) {
            position = p.currentPosition
            delay(500)
        }
    }

    fun togglePlayback() {
        val existing = player
        if (existing != null) {
            if (existing.isPlaying) existing.pause() else {
                if (existing.playbackState == Player.STATE_ENDED) existing.seekTo(0)
                existing.play()
            }
            return
        }
        hasError = false
        isBuffering = true
        val exo = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(viewModel.mediaHttpClient())))
            .build()
        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY && exo.duration > 0) duration = exo.duration
                if (state == Player.STATE_ENDED) position = duration
            }
            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                isBuffering = false
                isPlaying = false
            }
        })
        exo.setMediaItem(MediaItem.fromUri(viewModel.streamUrl))
        exo.prepare()
        exo.playWhenReady = true
        player = exo
    }

    GreywireCard(modifier = Modifier.testTag("session-player")) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(GreywireSurface2, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = GreywireMuted.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (player == null) viewModel.session.formattedDuration
            else "${formatDuration(position)} / ${if (duration > 0) formatDuration(duration) else "--:--"}",
            style = MonoSmallStyle,
            color = GreywireMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(GreywireCopper)
                .clickable { togglePlayback() }
                .semantics { contentDescription = if (isPlaying) "Pause" else "Play" }
                .testTag("play-button"),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering && !isPlaying) {
                CircularProgressIndicator(Modifier.size(24.dp), color = GreywireBg, strokeWidth = 2.dp)
            } else {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = GreywireBg,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        if (hasError) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Couldn't play this recording.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally).testTag("player-error")
            )
        }
    }
}

@Composable
private fun RatingCard(uiState: SessionDetailUiState, onTap: (Int) -> Unit) {
    GreywireCard(modifier = Modifier.testTag("rating-card")) {
        val avg = uiState.avgRating
        if (avg != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = GreywireCopper)
                Spacer(Modifier.width(8.dp))
                Text(formatRating(avg), style = MaterialTheme.typography.headlineMedium, color = GreywireText)
                Spacer(Modifier.width(8.dp))
                Text(
                    "(${countLabel(uiState.ratingCount, "rating")})",
                    style = MaterialTheme.typography.bodySmall,
                    color = GreywireMuted
                )
            }
        } else {
            Text(
                "No ratings yet",
                style = MaterialTheme.typography.bodyMedium,
                color = GreywireMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Your rating",
            style = MaterialTheme.typography.bodySmall,
            color = GreywireMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(8.dp))
        // Ten equal buttons across the card, so they fit narrow phones
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            (1..10).forEach { rating ->
                val selected = uiState.myRating == rating
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selected) GreywireCopper else GreywireSurface2)
                        .clickable(enabled = !uiState.isRating) { onTap(rating) }
                        .semantics {
                            this.selected = selected
                            contentDescription = "Rate $rating"
                        }
                        .testTag("rate-$rating"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(rating.toString(), style = MonoSmallStyle, color = if (selected) GreywireBg else GreywireMuted)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
        Text(value, style = MonoSmallStyle, color = GreywireText)
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = GreywireLine, thickness = 1.dp)
    Spacer(Modifier.height(32.dp))
}

private fun formatRating(value: Double): String = String.format(Locale.US, "%.1f", value)
