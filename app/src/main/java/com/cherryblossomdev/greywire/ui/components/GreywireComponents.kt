package com.cherryblossomdev.greywire.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cherryblossomdev.greywire.R
import com.cherryblossomdev.greywire.ui.theme.EyebrowStyle
import com.cherryblossomdev.greywire.ui.theme.GreywireCopper
import com.cherryblossomdev.greywire.ui.theme.GreywireLine
import com.cherryblossomdev.greywire.ui.theme.GreywireMuted
import com.cherryblossomdev.greywire.ui.theme.GreywireSurface
import com.cherryblossomdev.greywire.ui.theme.GreywireText
import com.cherryblossomdev.greywire.ui.theme.MonoSmallStyle
import com.cherryblossomdev.greywire.ui.theme.SpaceGrotesk

// Compose versions of the website's building blocks (GreywireStudio-Website
// css/site.css). The class each one mirrors is noted above it.

/** `.brand`: the logo plus "Greywire Studio", with "Studio" in muted. */
@Composable
fun GreywireBrand(modifier: Modifier = Modifier, logoSize: Int = 32, textSize: Int = 17) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_greywire_logo),
            contentDescription = null,
            modifier = Modifier.size(logoSize.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = GreywireText)) { append("Greywire ") }
                withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = GreywireMuted)) { append("Studio") }
            },
            fontFamily = SpaceGrotesk,
            fontSize = textSize.sp,
            letterSpacing = 0.3.sp
        )
    }
}

/** `.site-header`: brand on the left, an optional action on the right, a line underneath. */
@Composable
fun GreywireHeader(action: (@Composable () -> Unit)? = null) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GreywireBrand()
            action?.invoke()
        }
        HorizontalDivider(color = GreywireLine, thickness = 1.dp)
    }
}

/** `.eyebrow`: small copper mono caps above a heading. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text = text.uppercase(), style = EyebrowStyle, modifier = modifier.padding(bottom = 10.dp))
}

/** A section heading (`h2`), marked as a heading for TalkBack. */
@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = GreywireText,
        modifier = modifier.semantics { heading() }
    )
}

/** `.card`: surface background, 1px line border, 10px radius, 24px padding. */
@Composable
fun GreywireCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GreywireSurface, RoundedCornerShape(10.dp))
            .border(1.dp, GreywireLine, RoundedCornerShape(10.dp))
            .padding(20.dp),
        content = content
    )
}

/** `.status`: a pill with a colored dot and mono caps. */
@Composable
fun StatusPill(text: String, dotColor: Color = GreywireCopper, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .border(1.dp, GreywireLine, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).background(dotColor, CircleShape))
        Spacer(Modifier.width(7.dp))
        Text(text = text.uppercase(), style = MonoSmallStyle.copy(fontSize = 11.sp), color = GreywireMuted)
    }
}

/** `.button`: copper pill with dark text. */
@Composable
fun GreywireButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) { content() }
}

/** `.button.ghost`: transparent pill with a line border. */
@Composable
fun GreywireGhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, GreywireLine),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreywireText),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { content() }
}
