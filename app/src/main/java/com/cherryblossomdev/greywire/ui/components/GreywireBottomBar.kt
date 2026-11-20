package com.cherryblossomdev.greywire.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface2
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle

/**
 * The app's main sections, in bottom bar order. They match the web
 * community's menu (Chat, Sessions, Friends, Profile), plus Home.
 * [route] is the navigation route; [tag] is the tab's test tag.
 */
enum class GreywireTab(val route: String, val label: String, val icon: ImageVector, val tag: String) {
    Home("home", "Home", Icons.Outlined.Home, "tab-home"),
    Sessions("sessions", "Sessions", Icons.Outlined.LibraryMusic, "tab-sessions"),
    Chat("chat", "Chat", Icons.AutoMirrored.Outlined.Chat, "tab-chat"),
    Friends("friends", "Friends", Icons.Outlined.Group, "tab-friends"),
    Profile("profile", "Profile", Icons.Outlined.Person, "tab-profile");

    companion object {
        fun fromRoute(route: String?): GreywireTab? = entries.firstOrNull { it.route == route }
    }
}

/**
 * The bottom tab bar: surface background with a line on top (like the site's
 * header border), copper for the selected tab, mono labels.
 * (The site has no equivalent; on the web these are sidebar links.)
 */
@Composable
fun GreywireBottomBar(selected: GreywireTab?, onSelect: (GreywireTab) -> Unit) {
    Column {
        HorizontalDivider(color = GreywireLine, thickness = 1.dp)
        NavigationBar(containerColor = GreywireSurface, tonalElevation = 0.dp) {
            GreywireTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label, style = MonoSmallStyle.copy(fontSize = 11.sp)) },
                    modifier = Modifier.testTag(tab.tag),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GreywireCopper,
                        selectedTextColor = GreywireCopper,
                        indicatorColor = GreywireSurface2,
                        unselectedIconColor = GreywireMuted,
                        unselectedTextColor = GreywireMuted
                    )
                )
            }
        }
    }
}
