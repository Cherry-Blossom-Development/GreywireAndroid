package com.cherryblossomdev.greywire.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.cherryblossomdev.greywire.ui.theme.GreywireBg
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface2
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults

/**
 * A bio being edited: rich text (HTML) from the web's Tiptap editor
 * (Breakroom frontend RichTextEditor.vue), loaded into a [RichTextState].
 * Bold and italic come back out as <b> and <i>, which the web reads the same
 * as <strong> and <em>.
 */
class BioDraft(private val original: String?) {

    val state = RichTextState().apply {
        setHtml(original.orEmpty())
        history.clear() // Undo shouldn't go back to an empty editor
    }

    // What the editor exports before any change, to tell whether the user changed anything
    private val untouched = state.toHtml()

    /**
     * The HTML to save: [original] exactly as loaded when nothing changed (so
     * opening and saving never rewrites the web's HTML), "" when emptied.
     */
    fun html(): String? {
        val html = state.toHtml()
        return when {
            html == untouched -> original
            state.annotatedString.text.isBlank() -> ""
            else -> html
        }
    }
}

/**
 * The bio editor: a toolbar matching the web's (bold, italic, strikethrough,
 * bullet and numbered lists, heading, undo, redo) over an outlined editor.
 * Test tags: `<testTag>` on the editor, `<testTag>-bold` and so on on the buttons.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RichBioEditor(draft: BioDraft, label: String, enabled: Boolean, testTag: String, modifier: Modifier = Modifier) {
    val state = draft.state
    val span = state.currentSpanStyle
    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ToolbarButton(
                description = "Bold",
                active = span.fontWeight == FontWeight.Bold,
                enabled = enabled,
                testTag = "$testTag-bold",
                onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
            ) { ToolbarIcon(Icons.Filled.FormatBold) }
            ToolbarButton(
                description = "Italic",
                active = span.fontStyle == FontStyle.Italic,
                enabled = enabled,
                testTag = "$testTag-italic",
                onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
            ) { ToolbarIcon(Icons.Filled.FormatItalic) }
            ToolbarButton(
                description = "Strikethrough",
                active = span.textDecoration?.contains(TextDecoration.LineThrough) == true,
                enabled = enabled,
                testTag = "$testTag-strike",
                onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) }
            ) { ToolbarIcon(Icons.Filled.FormatStrikethrough) }
            ToolbarButton(
                description = "Bullet list",
                active = state.isUnorderedList,
                enabled = enabled,
                testTag = "$testTag-bullets",
                onClick = state::toggleUnorderedList
            ) { ToolbarIcon(Icons.AutoMirrored.Filled.FormatListBulleted) }
            ToolbarButton(
                description = "Numbered list",
                active = state.isOrderedList,
                enabled = enabled,
                testTag = "$testTag-numbers",
                onClick = state::toggleOrderedList
            ) { ToolbarIcon(Icons.Filled.FormatListNumbered) }
            ToolbarButton(
                description = "Heading",
                active = state.currentHeadingStyle == HeadingStyle.H3,
                enabled = enabled,
                testTag = "$testTag-heading",
                onClick = {
                    state.setHeadingStyle(if (state.currentHeadingStyle == HeadingStyle.H3) HeadingStyle.Normal else HeadingStyle.H3)
                }
            ) { ToolbarIcon(Icons.Filled.Title) }
            ToolbarButton(
                description = "Undo",
                active = false,
                enabled = enabled && state.history.canUndo,
                testTag = "$testTag-undo",
                onClick = { state.history.undo() }
            ) { ToolbarIcon(Icons.AutoMirrored.Filled.Undo) }
            ToolbarButton(
                description = "Redo",
                active = false,
                enabled = enabled && state.history.canRedo,
                testTag = "$testTag-redo",
                onClick = { state.history.redo() }
            ) { ToolbarIcon(Icons.AutoMirrored.Filled.Redo) }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedRichTextEditor(
            state = state,
            label = { Text(label) },
            enabled = enabled,
            minLines = 5,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = GreywireText),
            shape = RoundedCornerShape(10.dp),
            colors = RichTextEditorDefaults.outlinedRichTextEditorColors(
                textColor = GreywireText,
                containerColor = GreywireSurface,
                cursorColor = GreywireCopper,
                focusedBorderColor = GreywireCopper,
                unfocusedBorderColor = GreywireLine,
                focusedLabelColor = GreywireCopper,
                unfocusedLabelColor = GreywireMuted
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp).testTag(testTag)
        )
    }
}

/** A toolbar toggle: copper when its style is on where the cursor is. */
@Composable
private fun ToolbarButton(
    description: String,
    active: Boolean,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) GreywireCopper else GreywireSurface2)
            .border(1.dp, if (active) GreywireCopper else GreywireLine, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
                selected = active
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        val tint = when {
            active -> GreywireBg
            enabled -> GreywireText
            else -> GreywireMuted.copy(alpha = 0.5f)
        }
        CompositionLocalProvider(LocalContentColor provides tint) { content() }
    }
}


@Composable
private fun ToolbarIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
}
