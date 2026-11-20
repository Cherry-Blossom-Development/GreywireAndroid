package com.cherryblossomdev.greywire.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cherryblossomdev.greywire.ui.components.Eyebrow
import com.cherryblossomdev.greywire.ui.components.GreywireCard
import com.cherryblossomdev.greywire.ui.components.GreywireGhostButton
import com.cherryblossomdev.greywire.ui.components.GreywireTopBar
import com.cherryblossomdev.greywire.ui.components.SectionHeading
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted

// Placeholder tab screens for the navigation shell (roadmap step 2.1).
// Each is replaced by the real feature: Sessions (2.2), Chat (2.7),
// Friends (2.8), Profile (2.9).

@Composable
fun SessionsTabScreen() = PlaceholderTab(
    title = "Sessions",
    heading = "Your recordings",
    text = "Band practices and your own takes, ready to play back, rate and share.",
    testTag = "sessions-screen"
)

@Composable
fun ChatTabScreen() = PlaceholderTab(
    title = "Chat",
    heading = "Rooms and messages",
    text = "Talk with the Greywire community and message your bandmates.",
    testTag = "chat-screen"
)

@Composable
fun FriendsTabScreen() = PlaceholderTab(
    title = "Friends",
    heading = "Your people",
    text = "Find other Greywire musicians, send requests and see who's around.",
    testTag = "friends-screen"
)

/** Profile, for now just who's signed in and Sign out. */
@Composable
fun ProfileTabScreen(username: String?, onLogout: () -> Unit) {
    TabColumn(title = "Profile", testTag = "profile-screen") {
        Eyebrow("Signed in")
        SectionHeading(username ?: "Your account")
        Spacer(Modifier.height(16.dp))
        ComingSoonCard("Your profile, settings and other members' profiles are coming soon.")
        Spacer(Modifier.height(24.dp))
        GreywireGhostButton(onClick = onLogout, modifier = Modifier.testTag("logout-button")) {
            Text("Sign out", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PlaceholderTab(title: String, heading: String, text: String, testTag: String) {
    TabColumn(title = title, testTag = testTag) {
        Eyebrow("Coming soon")
        SectionHeading(heading)
        Spacer(Modifier.height(16.dp))
        ComingSoonCard(text)
    }
}

/** A tab's frame: title bar (no back arrow) and scrolling content. */
@Composable
private fun TabColumn(title: String, testTag: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().testTag(testTag)) {
        GreywireTopBar(title = title)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) { content() }
    }
}

@Composable
private fun ComingSoonCard(text: String) {
    GreywireCard {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
    }
}
