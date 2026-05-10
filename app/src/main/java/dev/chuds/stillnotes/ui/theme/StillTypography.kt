package dev.chuds.stillnotes.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.chuds.stillnotes.data.FontPreset

/**
 * Typography roles for Still Notes. Adds a few markdown-specific roles (Body, H1, H2, H3,
 * BlockQuote, Code) on top of the launcher's role set.
 */
data class StillTypographyValues(
    val Kicker: TextStyle,
    val Title: TextStyle,
    val Menu: TextStyle,
    val SecondaryMenu: TextStyle,
    val Caption: TextStyle,
    val Small: TextStyle,
    val Body: TextStyle,
    val H1: TextStyle,
    val H2: TextStyle,
    val H3: TextStyle,
    val BlockQuote: TextStyle,
    val Code: TextStyle,
    val Editor: TextStyle,
)

fun stillTypographyValues(
    titleFont: FontFamily,
    menuFont: FontFamily,
    monoFont: FontFamily,
): StillTypographyValues = StillTypographyValues(
    Kicker = TextStyle(
        fontFamily = monoFont,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 1.6.sp,
        fontWeight = FontWeight.Normal,
    ),
    Title = TextStyle(
        fontFamily = titleFont,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.6).sp,
        fontWeight = FontWeight.Light,
    ),
    Menu = TextStyle(
        fontFamily = menuFont,
        fontSize = 22.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.2.sp,
        fontWeight = FontWeight.Light,
    ),
    SecondaryMenu = TextStyle(
        fontFamily = menuFont,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.2.sp,
        fontWeight = FontWeight.Light,
    ),
    Caption = TextStyle(
        fontFamily = monoFont,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp,
        fontWeight = FontWeight.Normal,
    ),
    Small = TextStyle(
        fontFamily = menuFont,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.2.sp,
        fontWeight = FontWeight.Light,
    ),
    Body = TextStyle(
        fontFamily = menuFont,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.1.sp,
        fontWeight = FontWeight.Normal,
    ),
    H1 = TextStyle(
        fontFamily = titleFont,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.4).sp,
        fontWeight = FontWeight.Light,
    ),
    H2 = TextStyle(
        fontFamily = titleFont,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
        fontWeight = FontWeight.Light,
    ),
    H3 = TextStyle(
        fontFamily = menuFont,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.1.sp,
        fontWeight = FontWeight.Medium,
    ),
    BlockQuote = TextStyle(
        fontFamily = titleFont,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.1.sp,
        fontWeight = FontWeight.Light,
    ),
    Code = TextStyle(
        fontFamily = monoFont,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        fontWeight = FontWeight.Normal,
    ),
    Editor = TextStyle(
        fontFamily = monoFont,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
        fontWeight = FontWeight.Normal,
    ),
)

fun stillTypographyFor(preset: FontPreset): StillTypographyValues = when (preset) {
    FontPreset.System -> stillTypographyValues(
        titleFont = FontFamily.Serif,
        menuFont = FontFamily.SansSerif,
        monoFont = FontFamily.Monospace,
    )
    FontPreset.Editorial -> stillTypographyValues(
        titleFont = StillFontFamilies.CormorantGaramond,
        menuFont = StillFontFamilies.Inter,
        monoFont = StillFontFamilies.IbmPlexMono,
    )
    FontPreset.Terminal -> stillTypographyValues(
        titleFont = StillFontFamilies.IbmPlexMono,
        menuFont = StillFontFamilies.IbmPlexMono,
        monoFont = StillFontFamilies.IbmPlexMono,
    )
    FontPreset.Grotesk -> stillTypographyValues(
        titleFont = StillFontFamilies.InstrumentSerif,
        menuFont = StillFontFamilies.SpaceGrotesk,
        monoFont = StillFontFamilies.IbmPlexMono,
    )
}

val LocalStillTypography = staticCompositionLocalOf {
    stillTypographyFor(FontPreset.System)
}

object StillTypography {
    val Kicker: TextStyle
        @Composable @ReadOnlyComposable
        get() = LocalStillTypography.current.Kicker

    val Title: TextStyle
        @Composable @ReadOnlyComposable
        get() = LocalStillTypography.current.Title

    val Menu: TextStyle
        @Composable @ReadOnlyComposable
        get() = LocalStillTypography.current.Menu

    val SecondaryMenu: TextStyle
        @Composable @ReadOnlyComposable
        get() = LocalStillTypography.current.SecondaryMenu

    val Caption: TextStyle
        @Composable @ReadOnlyComposable
        get() = LocalStillTypography.current.Caption

    val Small: TextStyle
        @Composable @ReadOnlyComposable
        get() = LocalStillTypography.current.Small

    val Body: TextStyle
        @Composable @ReadOnlyComposable
        get() = LocalStillTypography.current.Body

    val H1: TextStyle
        @Composable @ReadOnlyComposable
        get() = LocalStillTypography.current.H1

    val H2: TextStyle
        @Composable @ReadOnlyComposable
        get() = LocalStillTypography.current.H2

    val H3: TextStyle
        @Composable @ReadOnlyComposable
        get() = LocalStillTypography.current.H3

    val BlockQuote: TextStyle
        @Composable @ReadOnlyComposable
        get() = LocalStillTypography.current.BlockQuote

    val Code: TextStyle
        @Composable @ReadOnlyComposable
        get() = LocalStillTypography.current.Code

    val Editor: TextStyle
        @Composable @ReadOnlyComposable
        get() = LocalStillTypography.current.Editor
}
