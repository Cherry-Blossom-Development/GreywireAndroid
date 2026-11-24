package com.cherryblossomdev.greywire.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cherryblossomdev.greywire.network.RetrofitClient
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface2
import com.cherryblossomdev.greywire.ui.theme.GreywireText

/**
 * A round profile photo from `/api/uploads/<photoPath>`, or the first letter of
 * [name] when there's no photo or it doesn't load (the web's `.photo-placeholder`).
 */
@Composable
fun ProfileAvatar(photoPath: String?, name: String, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    var failed by remember(photoPath) { mutableStateOf(false) }
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(GreywireSurface2),
        contentAlignment = Alignment.Center
    ) {
        if (photoPath.isNullOrBlank() || failed) {
            Text(
                text = name.take(1).uppercase(),
                style = if (size >= 64.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                color = GreywireMuted
            )
        } else {
            AsyncImage(
                model = RetrofitClient.uploadUrl(photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.size(size)
            )
        }
    }
}

/** Rich text from the website's editor (HTML) as styled text, links in copper. */
fun htmlToAnnotatedString(html: String): AnnotatedString =
    AnnotatedString.fromHtml(
        html.trim(),
        linkStyles = TextLinkStyles(SpanStyle(color = GreywireCopper, textDecoration = TextDecoration.Underline))
    )

/** True when [html] has no visible text (null, empty, or an empty editor such as "<p></p>"). */
fun isBlankHtml(html: String?): Boolean = html.isNullOrBlank() || htmlToAnnotatedString(html).text.isBlank()

/** Shows rich text (HTML) from the website: bold, italics, lists and links. */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = GreywireText
) {
    // Drop the trailing newlines a closing </p> leaves
    val text = remember(html) { htmlToAnnotatedString(html).let { it.subSequence(0, it.text.trimEnd().length) } }
    Text(text = text, style = style, color = color, modifier = modifier)
}
