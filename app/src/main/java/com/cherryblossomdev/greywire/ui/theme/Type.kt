package com.cherryblossomdev.greywire.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cherryblossomdev.greywire.R

// Same fonts as the website: Space Grotesk for text, IBM Plex Mono for labels.
// Both are bundled in res/font (SIL Open Font License, see /licenses).
val SpaceGrotesk = FontFamily(
    Font(R.font.spacegrotesk_regular, FontWeight.Normal),
    Font(R.font.spacegrotesk_medium, FontWeight.Medium),
    // No static SemiBold exists upstream; the site's 600 weight maps to Medium/Bold
    Font(R.font.spacegrotesk_medium, FontWeight.SemiBold),
    Font(R.font.spacegrotesk_bold, FontWeight.Bold)
)

val PlexMono = FontFamily(
    Font(R.font.plexmono_regular, FontWeight.Normal),
    Font(R.font.plexmono_medium, FontWeight.Medium)
)

// The site's ".eyebrow": small mono caps in copper, above a heading.
val EyebrowStyle = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    letterSpacing = 1.7.sp,
    color = GreywireCopper
)

// The site's ".status" pill text and ".type" line
val MonoSmallStyle = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    letterSpacing = 0.8.sp
)

private val base = Typography()

val Typography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.3).sp),
    headlineLarge = base.headlineLarge.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineMedium = base.headlineMedium.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = base.titleMedium.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = SpaceGrotesk, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = SpaceGrotesk, fontSize = 15.sp, lineHeight = 23.sp),
    bodySmall = base.bodySmall.copy(fontFamily = SpaceGrotesk),
    labelLarge = base.labelLarge.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    labelMedium = base.labelMedium.copy(fontFamily = SpaceGrotesk),
    labelSmall = base.labelSmall.copy(fontFamily = PlexMono)
)
