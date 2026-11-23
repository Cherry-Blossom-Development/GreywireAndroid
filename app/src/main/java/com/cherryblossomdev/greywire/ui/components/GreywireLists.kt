package com.cherryblossomdev.greywire.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cherryblossomdev.greywire.ui.theme.GreywireBg
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSignal
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface2
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle

// Building blocks for the list tabs (Friends, Chat, Sessions). They match the
// iOS app's equivalents; the website has no direct counterparts.

/**
 * A tab's frame: the title bar (no back arrow), with [header] (e.g. a section
 * picker) under it, then [content] filling the rest.
 */
@Composable
fun GreywireTabFrame(
    title: String,
    testTag: String,
    header: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().testTag(testTag)) {
        GreywireTopBar(title = title)
        header()
        content()
    }
}

/** One choice in a [GreywireSectionPicker]. A [badge] above zero shows as a copper count. */
data class SectionOption<T>(val value: T, val label: String, val badge: Int = 0, val testTag: String? = null)

/**
 * A row of equal-width section buttons on the surface color, the selected one
 * in copper (iOS: the section pickers on Friends, Chat and Sessions).
 * [mutedBadges]: unselected sections show their badge in muted rather than copper.
 */
@Composable
fun <T> GreywireSectionPicker(
    options: List<SectionOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    mutedBadges: Boolean = false
) {
    Column(modifier = Modifier.background(GreywireSurface)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                val isSelected = option.value == selected
                val color = if (isSelected) GreywireCopper else GreywireMuted
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(role = Role.Tab) { onSelect(option.value) }
                        .semantics { this.selected = isSelected }
                        .then(if (option.testTag != null) Modifier.testTag(option.testTag) else Modifier)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(option.label, style = MaterialTheme.typography.bodyMedium, color = color)
                    if (option.badge > 0) {
                        Spacer(Modifier.width(6.dp))
                        CountBadge(
                            option.badge,
                            color = if (mutedBadges && !isSelected) GreywireMuted else GreywireCopper
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = GreywireLine, thickness = 1.dp)
    }
}

/** A small pill with a count, dark text on copper. */
@Composable
fun CountBadge(count: Int, color: Color = GreywireCopper, modifier: Modifier = Modifier) {
    Text(
        text = count.toString(),
        style = MonoSmallStyle.copy(fontSize = 11.sp),
        color = GreywireBg,
        modifier = modifier
            .background(color, CircleShape)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

/**
 * A round avatar: the first letter of [name] on surface-2, or [icon] if given,
 * with a green dot when [online].
 */
@Composable
fun InitialAvatar(
    name: String,
    size: Dp = 44.dp,
    online: Boolean = false,
    icon: ImageVector? = null
) {
    Box(modifier = Modifier.size(size)) {
        Box(
            modifier = Modifier.size(size).background(GreywireSurface2, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = GreywireMuted, modifier = Modifier.size(size * 0.4f))
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    style = if (size >= 40.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
                    color = GreywireMuted
                )
            }
        }
        if (online) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.BottomEnd)
                    .background(GreywireSignal, CircleShape)
                    .border(2.dp, GreywireSurface, CircleShape)
            )
        }
    }
}

/** A card with a title and a line of muted text, for empty lists. */
@Composable
fun EmptyStateCard(title: String, text: String, modifier: Modifier = Modifier) {
    GreywireCard(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = GreywireText)
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = GreywireMuted)
    }
}

/** A centered copper spinner filling the available space. */
@Composable
fun LoadingFill() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = GreywireCopper, strokeWidth = 2.dp)
    }
}

/** An error line in the theme's error color, for a failed load or action. */
@Composable
fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.padding(bottom = 12.dp)
    )
}

/** The eyebrow over a list, e.g. "3 friends". */
fun countLabel(count: Int, singular: String, plural: String = singular + "s"): String =
    "$count ${if (count == 1) singular else plural}"
