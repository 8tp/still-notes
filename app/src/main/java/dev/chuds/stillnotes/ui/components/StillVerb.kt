package dev.chuds.stillnotes.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chuds.stillnotes.ui.theme.StillColors
import dev.chuds.stillnotes.ui.theme.StillTypography

/**
 * A tappable lowercase verb. Persistent bottom-bar verbs pass bordered = true so they
 * read as buttons; transient verbs (headers, sheets) leave it false. No ripple.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StillVerb(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bordered: Boolean = false,
    color: Color = StillColors.MutedWhite,
    style: TextStyle = StillTypography.SecondaryMenu,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHaptics.current
    Text(
        text = label,
        style = style,
        color = color,
        modifier = modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics()
                    onClick()
                },
            )
            .then(
                if (bordered) Modifier.border(1.dp, StillColors.Hairline, RectangleShape)
                else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}
