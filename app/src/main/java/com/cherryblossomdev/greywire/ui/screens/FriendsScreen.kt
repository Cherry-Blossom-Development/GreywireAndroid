package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.cherryblossomdev.greywire.ui.components.GreywireGhostButton
import com.cherryblossomdev.greywire.ui.components.greywireTextFieldColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.data.FriendsRepository
import com.cherryblossomdev.greywire.network.FriendUser
import com.cherryblossomdev.greywire.ui.components.EmptyStateCard
import com.cherryblossomdev.greywire.ui.components.popupTestTags
import com.cherryblossomdev.greywire.ui.components.ErrorText
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireCard
import com.cherryblossomdev.greywire.ui.components.GreywireSectionPicker
import com.cherryblossomdev.greywire.ui.components.GreywireTabFrame
import com.cherryblossomdev.greywire.ui.components.InitialAvatar
import com.cherryblossomdev.greywire.ui.components.LoadingFill
import com.cherryblossomdev.greywire.ui.components.SectionOption
import com.cherryblossomdev.greywire.ui.components.StatusPill
import com.cherryblossomdev.greywire.ui.components.countLabel
import com.cherryblossomdev.greywire.ui.components.readableWidth
import com.cherryblossomdev.greywire.ui.theme.GreywireBg
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface2
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// The Friends tab (roadmap 2.8): friends with online status, incoming
// requests (accept or decline), sent requests (cancel), and Find, which
// searches for people to add (GET /api/friends/find) and sends requests.

enum class FriendsSection(val label: String) { Friends("Friends"), Requests("Requests"), Find("Find") }

/** The shortest search the backend accepts. */
private const val MIN_FIND_LENGTH = 2

data class FriendsUiState(
    val friends: List<FriendUser> = emptyList(),
    val requests: List<FriendUser> = emptyList(),
    val sent: List<FriendUser> = emptyList(),
    val section: FriendsSection = FriendsSection.Friends,
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false,
    /** Find: what's typed, and the results for [foundQuery] (null before any search). */
    val findQuery: String = "",
    val foundQuery: String? = null,
    val found: List<FriendUser> = emptyList(),
    val isSearching: Boolean = false,
    /** Find: people whose request is being sent. */
    val sending: Set<Int> = emptySet()
)

class FriendsViewModel(private val repository: FriendsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState

    private var searchJob: Job? = null

    fun selectSection(section: FriendsSection) {
        _uiState.value = _uiState.value.copy(section = section)
    }

    /** Searches as the user types, after a short pause. */
    fun updateFindQuery(query: String) {
        _uiState.value = _uiState.value.copy(findQuery = query)
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < MIN_FIND_LENGTH) {
            _uiState.value = _uiState.value.copy(found = emptyList(), foundQuery = null, isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            search(trimmed)
        }
    }

    private suspend fun search(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true, errorMessage = null)
        when (val result = repository.find(query)) {
            is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                found = result.data.users,
                foundQuery = query,
                isSearching = false
            )
            is AuthResult.Error -> _uiState.value = if (result.unauthorized) {
                _uiState.value.copy(isSearching = false, sessionExpired = true)
            } else {
                _uiState.value.copy(isSearching = false, errorMessage = result.message)
            }
        }
    }

    fun sendRequest(person: FriendUser) {
        if (person.id in _uiState.value.sending) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(errorMessage = null, sending = _uiState.value.sending + person.id)
            val result = repository.sendRequest(person.id)
            val state = _uiState.value.copy(sending = _uiState.value.sending - person.id)
            _uiState.value = when (result) {
                is AuthResult.Success -> state.copy(
                    found = state.found.filterNot { it.id == person.id },
                    sent = listOf(person) + state.sent.filterNot { it.id == person.id }
                )
                is AuthResult.Error -> if (result.unauthorized) {
                    state.copy(sessionExpired = true)
                } else {
                    state.copy(errorMessage = result.message)
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val friends = async { repository.getFriends() }
            val requests = async { repository.getRequests() }
            val sent = async { repository.getSent() }
            val results = listOf(friends.await(), requests.await(), sent.await())

            if (results.any { it is AuthResult.Error && it.unauthorized }) {
                _uiState.value = _uiState.value.copy(isLoading = false, sessionExpired = true)
                return@launch
            }
            val state = _uiState.value
            _uiState.value = state.copy(
                friends = (friends.await() as? AuthResult.Success)?.data?.friends ?: state.friends,
                requests = (requests.await() as? AuthResult.Success)?.data?.requests ?: state.requests,
                sent = (sent.await() as? AuthResult.Success)?.data?.sent ?: state.sent,
                isLoading = false,
                hasLoaded = true,
                errorMessage = results.filterIsInstance<AuthResult.Error>().firstOrNull()?.message
            )

            // The user has now seen their requests (clears the badge on the web)
            repository.markSeen()
        }
    }

    fun accept(request: FriendUser) = act({ repository.accept(request.id) }) { state ->
        state.copy(
            requests = state.requests.filterNot { it.id == request.id },
            friends = listOf(request.copy(requestedAt = null)) + state.friends
        )
    }

    fun decline(request: FriendUser) = act({ repository.decline(request.id) }) { state ->
        state.copy(requests = state.requests.filterNot { it.id == request.id })
    }

    fun cancel(request: FriendUser) = act({ repository.cancelRequest(request.id) }) { state ->
        state.copy(sent = state.sent.filterNot { it.id == request.id })
    }

    fun remove(friend: FriendUser) = act({ repository.remove(friend.id) }) { state ->
        state.copy(friends = state.friends.filterNot { it.id == friend.id })
    }

    /** Runs [call]; on success applies [onSuccess] to the state, otherwise shows the error. */
    private fun act(call: suspend () -> AuthResult<*>, onSuccess: (FriendsUiState) -> FriendsUiState) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(errorMessage = null)
            when (val result = call()) {
                is AuthResult.Success -> _uiState.value = onSuccess(_uiState.value)
                is AuthResult.Error -> _uiState.value = if (result.unauthorized) {
                    _uiState.value.copy(sessionExpired = true)
                } else {
                    _uiState.value.copy(errorMessage = result.message)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    /** Opens a person's profile, by handle. */
    onOpenProfile: (String) -> Unit,
    onSessionExpired: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }

    GreywireTabFrame(
        title = "Friends",
        testTag = "friends-screen",
        header = {
            GreywireSectionPicker(
                options = listOf(
                    SectionOption(FriendsSection.Friends, "Friends", testTag = "friends-section-friends"),
                    SectionOption(FriendsSection.Requests, "Requests", badge = uiState.requests.size, testTag = "friends-section-requests"),
                    SectionOption(FriendsSection.Find, "Find", testTag = "friends-section-find")
                ),
                selected = uiState.section,
                onSelect = viewModel::selectSection
            )
        }
    ) {
        if (!uiState.hasLoaded && uiState.isLoading) {
            LoadingFill()
        } else {
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
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    uiState.errorMessage?.let { ErrorText(it, Modifier.testTag("friends-error")) }
                    when (uiState.section) {
                        FriendsSection.Friends -> FriendsList(uiState.friends, onOpenProfile, viewModel::remove)
                        FriendsSection.Requests -> RequestsList(
                            uiState.requests, uiState.sent,
                            onOpenProfile = onOpenProfile,
                            onAccept = viewModel::accept,
                            onDecline = viewModel::decline,
                            onCancel = viewModel::cancel
                        )
                        FriendsSection.Find -> FindPeople(
                            uiState,
                            onQueryChange = viewModel::updateFindQuery,
                            onOpenProfile = onOpenProfile,
                            onAdd = viewModel::sendRequest
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun FriendsList(friends: List<FriendUser>, onOpenProfile: (String) -> Unit, onRemove: (FriendUser) -> Unit) {
    if (friends.isEmpty()) {
        EmptyStateCard(
            "No friends yet",
            "Other Greywire musicians will show up here once you connect. Check the Requests tab to see who wants to be friends."
        )
        return
    }
    Eyebrow(countLabel(friends.size, "friend"))
    Spacer(Modifier.height(2.dp))
    friends.forEach { friend ->
        var confirmRemove by remember { mutableStateOf(false) }
        PersonCard(friend, online = friend.isOnline, testTag = "friend-${friend.id}", onClick = { onOpenProfile(friend.handle) }) {
            IconButton(onClick = { confirmRemove = true }, modifier = Modifier.testTag("remove-friend-${friend.id}")) {
                Icon(Icons.Filled.Close, contentDescription = "Remove ${friend.handle}", tint = GreywireMuted, modifier = Modifier.size(16.dp))
            }
        }
        if (confirmRemove) {
            ConfirmDialog(
                title = "Remove friend?",
                text = "You can send them a friend request again later.",
                confirmLabel = "Remove ${friend.handle}",
                dismissLabel = "Cancel",
                onConfirm = { confirmRemove = false; onRemove(friend) },
                onDismiss = { confirmRemove = false }
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RequestsList(
    requests: List<FriendUser>,
    sent: List<FriendUser>,
    onOpenProfile: (String) -> Unit,
    onAccept: (FriendUser) -> Unit,
    onDecline: (FriendUser) -> Unit,
    onCancel: (FriendUser) -> Unit
) {
    if (requests.isEmpty() && sent.isEmpty()) {
        EmptyStateCard("No pending requests", "When someone sends you a friend request, it will appear here.")
        return
    }
    if (requests.isNotEmpty()) {
        Eyebrow("Incoming")
        Spacer(Modifier.height(2.dp))
        requests.forEach { request ->
            PersonCard(request, testTag = "friend-request-${request.id}", onClick = { onOpenProfile(request.handle) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundIconButton(
                        icon = Icons.Filled.Close,
                        description = "Decline",
                        background = GreywireSurface2,
                        tint = GreywireMuted,
                        testTag = "decline-request-${request.id}",
                        onClick = { onDecline(request) }
                    )
                    RoundIconButton(
                        icon = Icons.Filled.Check,
                        description = "Accept",
                        background = GreywireCopper,
                        tint = GreywireBg,
                        testTag = "accept-request-${request.id}",
                        onClick = { onAccept(request) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
    if (sent.isNotEmpty()) {
        Eyebrow("Sent")
        Spacer(Modifier.height(2.dp))
        sent.forEach { request ->
            var confirmCancel by remember { mutableStateOf(false) }
            PersonCard(request, testTag = "sent-request-${request.id}", onClick = { onOpenProfile(request.handle) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill("Pending")
                    IconButton(onClick = { confirmCancel = true }, modifier = Modifier.testTag("cancel-request-${request.id}")) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel request", tint = GreywireMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (confirmCancel) {
                ConfirmDialog(
                    title = "Cancel request?",
                    text = null,
                    confirmLabel = "Cancel request",
                    dismissLabel = "Keep",
                    onConfirm = { confirmCancel = false; onCancel(request) },
                    onDismiss = { confirmCancel = false }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FindPeople(
    state: FriendsUiState,
    onQueryChange: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onAdd: (FriendUser) -> Unit
) {
    OutlinedTextField(
        value = state.findQuery,
        onValueChange = onQueryChange,
        placeholder = { Text("Search by username or name") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = GreywireMuted) },
        trailingIcon = if (state.findQuery.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.testTag("find-clear-button")) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = GreywireMuted)
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = greywireTextFieldColors(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth().testTag("find-input")
    )
    Spacer(Modifier.height(16.dp))

    val query = state.findQuery.trim()
    when {
        query.length < MIN_FIND_LENGTH -> EmptyStateCard(
            "Find musicians",
            "Search for other Greywire musicians by username or name, then send them a friend request."
        )
        state.isSearching && state.foundQuery != query -> Box(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = GreywireCopper, strokeWidth = 2.dp)
        }
        state.foundQuery == null -> Unit
        state.found.isEmpty() -> EmptyStateCard(
            "No one found",
            "No Greywire musicians you aren't already connected to match \"${state.foundQuery}\".",
            modifier = Modifier.testTag("find-empty")
        )
        else -> {
            Eyebrow(countLabel(state.found.size, "result"))
            Spacer(Modifier.height(2.dp))
            state.found.forEach { person ->
                val sending = person.id in state.sending
                PersonCard(person, testTag = "find-result-${person.id}", onClick = { onOpenProfile(person.handle) }) {
                    GreywireGhostButton(
                        onClick = { onAdd(person) },
                        enabled = !sending,
                        modifier = Modifier.testTag("add-friend-${person.id}")
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (sending) "Sending" else "Add")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/**
 * A card with the person's avatar, name and @handle, and [actions] on the
 * right. Tapping the card opens their profile.
 */
@Composable
private fun PersonCard(
    person: FriendUser,
    online: Boolean = false,
    testTag: String,
    onClick: () -> Unit,
    actions: @Composable () -> Unit
) {
    GreywireCard(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "View profile", onClick = onClick)
            .testTag(testTag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(person.handle, online = online)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(person.displayName, style = MaterialTheme.typography.titleMedium, color = GreywireText, maxLines = 1)
                Text("@${person.handle}", style = MonoSmallStyle, color = GreywireMuted, maxLines = 1)
            }
            actions()
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    background: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    testTag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp).background(background, CircleShape).testTag(testTag)
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(16.dp))
    }
}

/** A confirmation dialog in the app's colors (iOS uses an action sheet). */
@Composable
internal fun ConfirmDialog(
    title: String,
    text: String?,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.popupTestTags(),
        containerColor = GreywireSurface,
        titleContentColor = GreywireText,
        textContentColor = GreywireMuted,
        title = { Text(title) },
        text = text?.let { { Text(it) } },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("confirm-button")) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel, color = GreywireText) }
        }
    )
}
