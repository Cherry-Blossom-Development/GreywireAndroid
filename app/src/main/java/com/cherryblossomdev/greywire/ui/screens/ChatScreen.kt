package com.cherryblossomdev.greywire.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherryblossomdev.greywire.data.AuthResult
import com.cherryblossomdev.greywire.data.ChatRepository
import com.cherryblossomdev.greywire.network.ChatMessage
import com.cherryblossomdev.greywire.network.ChatRoom
import com.cherryblossomdev.greywire.ui.components.CountBadge
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
import com.cherryblossomdev.greywire.ui.components.countLabel
import com.cherryblossomdev.greywire.ui.components.formatTime
import com.cherryblossomdev.greywire.ui.components.greywireTextFieldColors
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface2
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// The Chat tab (roadmap 2.7): the Greywire community's rooms and the user's
// direct messages. A room opens inside the tab (the bottom bar stays), as on
// iOS. Messages load over REST; live updates over Socket.IO come later.

enum class ChatSection { Rooms, Messages }

data class ChatUiState(
    val rooms: List<ChatRoom> = emptyList(),
    val dms: List<ChatRoom> = emptyList(),
    val section: ChatSection = ChatSection.Rooms,
    /** The room or conversation being shown, if any. */
    val openRoom: ChatRoom? = null,
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    fun selectSection(section: ChatSection) {
        _uiState.value = _uiState.value.copy(section = section)
    }

    fun open(room: ChatRoom) {
        _uiState.value = _uiState.value.copy(openRoom = room)
    }

    /** Back from a room: reload, so unread counts and last messages are current. */
    fun close() {
        _uiState.value = _uiState.value.copy(openRoom = null)
        load()
    }

    fun onSessionExpired() {
        _uiState.value = _uiState.value.copy(sessionExpired = true)
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val rooms = async { repository.getRooms() }
            val dms = async { repository.getDms() }
            val results = listOf(rooms.await(), dms.await())

            if (results.any { it is AuthResult.Error && it.unauthorized }) {
                _uiState.value = _uiState.value.copy(isLoading = false, sessionExpired = true)
                return@launch
            }
            val state = _uiState.value
            _uiState.value = state.copy(
                rooms = (rooms.await() as? AuthResult.Success)?.data?.rooms ?: state.rooms,
                dms = (dms.await() as? AuthResult.Success)?.data?.dms ?: state.dms,
                isLoading = false,
                hasLoaded = true,
                errorMessage = results.filterIsInstance<AuthResult.Error>().firstOrNull()?.message
            )
        }
    }
}

data class ChatRoomUiState(
    /** Oldest first, as the backend sends them. */
    val messages: List<ChatMessage> = emptyList(),
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val draft: String = "",
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class ChatRoomViewModel(
    private val repository: ChatRepository,
    val room: ChatRoom
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatRoomUiState())
    val uiState: StateFlow<ChatRoomUiState> = _uiState

    fun updateDraft(text: String) {
        _uiState.value = _uiState.value.copy(draft = text, errorMessage = null)
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getMessages(room.id)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        messages = result.data.messages,
                        hasMore = result.data.hasMore,
                        isLoading = false
                    )
                    repository.markRead(room.id)
                }
                is AuthResult.Error -> fail(result)
            }
        }
    }

    fun loadEarlier() {
        val state = _uiState.value
        val oldest = state.messages.firstOrNull()?.createdAt ?: return
        if (state.isLoadingMore || !state.hasMore) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true, errorMessage = null)
            when (val result = repository.getMessages(room.id, before = oldest)) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                    messages = result.data.messages + _uiState.value.messages,
                    hasMore = result.data.hasMore,
                    isLoadingMore = false
                )
                is AuthResult.Error -> fail(result)
            }
        }
    }

    fun send() {
        val state = _uiState.value
        val text = state.draft.trim()
        if (text.isEmpty() || state.isSending) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, errorMessage = null)
            when (val result = repository.sendMessage(room.id, text)) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + result.data,
                    draft = "",
                    isSending = false
                )
                is AuthResult.Error -> fail(result)
            }
        }
    }

    private fun fail(error: AuthResult.Error) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isLoadingMore = false,
            isSending = false,
            sessionExpired = error.unauthorized,
            errorMessage = error.message
        )
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel, repository: ChatRepository, onSessionExpired: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }

    val openRoom = uiState.openRoom
    if (openRoom != null) {
        BackHandler(onBack = viewModel::close)
        val roomViewModel = remember(openRoom.id) { ChatRoomViewModel(repository, openRoom) }
        ChatRoomScreen(roomViewModel, onBack = viewModel::close, onSessionExpired = viewModel::onSessionExpired)
    } else {
        ChatListScreen(uiState, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatListScreen(uiState: ChatUiState, viewModel: ChatViewModel) {
    GreywireTabFrame(
        title = "Chat",
        testTag = "chat-screen",
        header = {
            GreywireSectionPicker(
                options = listOf(
                    SectionOption(ChatSection.Rooms, "Rooms", testTag = "chat-section-rooms"),
                    SectionOption(ChatSection.Messages, "Messages", badge = uiState.dms.size, testTag = "chat-section-messages")
                ),
                selected = uiState.section,
                onSelect = viewModel::selectSection
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
                    .padding(16.dp)
            ) {
                uiState.errorMessage?.let { ErrorText(it, Modifier.testTag("chat-error")) }
                when (uiState.section) {
                    ChatSection.Rooms -> if (uiState.rooms.isEmpty()) {
                        EmptyStateCard(
                            "No chat rooms yet",
                            "Join the Greywire community to participate in chat rooms with other musicians."
                        )
                    } else {
                        Eyebrow(countLabel(uiState.rooms.size, "room"))
                        Spacer(Modifier.height(2.dp))
                        uiState.rooms.forEach { room ->
                            ChatRow(
                                room = room,
                                subtitle = room.description,
                                avatar = { InitialAvatar(room.displayName, icon = Icons.Outlined.Forum) },
                                testTag = "chat-room-${room.id}",
                                onClick = { viewModel.open(room) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    ChatSection.Messages -> if (uiState.dms.isEmpty()) {
                        EmptyStateCard("No messages yet", "Start a conversation with a friend from the Friends tab.")
                    } else {
                        Eyebrow(countLabel(uiState.dms.size, "conversation"))
                        Spacer(Modifier.height(2.dp))
                        uiState.dms.forEach { dm ->
                            ChatRow(
                                room = dm,
                                subtitle = dm.lastMessage,
                                avatar = { InitialAvatar(dm.displayName) },
                                testTag = "chat-dm-${dm.id}",
                                onClick = { viewModel.open(dm) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRow(
    room: ChatRoom,
    subtitle: String?,
    avatar: @Composable () -> Unit,
    testTag: String,
    onClick: () -> Unit
) {
    GreywireCard(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            avatar()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(room.displayName, style = MaterialTheme.typography.titleMedium, color = GreywireText, maxLines = 1)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GreywireMuted, maxLines = 1)
                }
            }
            val unread = room.unreadCount ?: 0
            if (unread > 0) {
                Spacer(Modifier.width(8.dp))
                CountBadge(unread)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = GreywireMuted)
        }
    }
}

@Composable
private fun ChatRoomScreen(viewModel: ChatRoomViewModel, onBack: () -> Unit, onSessionExpired: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) onSessionExpired()
    }
    // Follow the newest message (on load and after sending), not when earlier ones are prepended
    val newestId = uiState.messages.lastOrNull()?.id
    LaunchedEffect(newestId) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size)
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding().testTag("chat-room-screen")) {
        GreywireTopBar(title = viewModel.room.displayName, onBack = onBack)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (uiState.isLoading && uiState.messages.isEmpty()) {
                LoadingFill()
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "earlier") {
                        if (uiState.hasMore) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                if (uiState.isLoadingMore) {
                                    CircularProgressIndicator(Modifier.size(20.dp), color = GreywireCopper, strokeWidth = 2.dp)
                                } else {
                                    TextButton(onClick = viewModel::loadEarlier, modifier = Modifier.testTag("load-earlier-button")) {
                                        Text("Load earlier messages", style = MaterialTheme.typography.bodySmall, color = GreywireCopper)
                                    }
                                }
                            }
                        }
                    }
                    if (uiState.messages.isEmpty()) {
                        item(key = "empty") {
                            Text("No messages yet. Say hello!", style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
                        }
                    }
                    items(uiState.messages, key = { it.id }) { message -> MessageRow(message) }
                }
            }
        }

        uiState.errorMessage?.let {
            ErrorText(it, Modifier.padding(horizontal = 16.dp).testTag("chat-error"))
        }
        MessageInputBar(
            text = uiState.draft,
            isSending = uiState.isSending,
            onTextChange = viewModel::updateDraft,
            onSend = viewModel::send
        )
    }
}

@Composable
private fun MessageRow(message: ChatMessage) {
    val handle = message.handle ?: "Unknown"
    Row(modifier = Modifier.fillMaxWidth().padding(end = 40.dp)) {
        InitialAvatar(handle, size = 32.dp)
        Spacer(Modifier.width(8.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(handle, style = MonoSmallStyle, color = GreywireCopper)
                message.createdAt?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(formatTime(it), style = MaterialTheme.typography.bodySmall, color = GreywireMuted)
                }
            }
            Spacer(Modifier.height(2.dp))
            if (!message.message.isNullOrBlank()) {
                Text(message.message, style = MaterialTheme.typography.bodyMedium, color = GreywireText)
            }
            if (message.hasMedia) {
                // Images and videos aren't shown in the app yet, as on iOS
                val isImage = message.imagePath != null
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(GreywireSurface2, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isImage) Icons.Outlined.Image else Icons.Outlined.Videocam,
                        contentDescription = null,
                        tint = GreywireMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isImage) "Image" else "Video", style = MaterialTheme.typography.bodySmall, color = GreywireMuted)
                }
            }
        }
    }
}

@Composable
private fun MessageInputBar(
    text: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = text.isNotBlank() && !isSending
    Column(modifier = Modifier.background(GreywireSurface)) {
        HorizontalDivider(color = GreywireLine, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Message") },
                maxLines = 5,
                shape = RoundedCornerShape(20.dp),
                colors = greywireTextFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(1f).testTag("message-input")
            )
            Spacer(Modifier.width(8.dp))
            if (isSending) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = GreywireCopper, strokeWidth = 2.dp)
                }
            } else {
                IconButton(onClick = onSend, enabled = canSend, modifier = Modifier.testTag("send-message-button")) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) GreywireCopper else GreywireMuted
                    )
                }
            }
        }
    }
}
