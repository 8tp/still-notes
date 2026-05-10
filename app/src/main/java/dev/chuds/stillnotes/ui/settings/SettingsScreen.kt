package dev.chuds.stillnotes.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chuds.stillnotes.data.FontPreset
import dev.chuds.stillnotes.data.NotesSettings
import dev.chuds.stillnotes.ui.components.StillMenuItem
import dev.chuds.stillnotes.ui.components.StillSectionCard
import dev.chuds.stillnotes.ui.theme.StillColors
import dev.chuds.stillnotes.ui.theme.StillTypography

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    settings: NotesSettings,
    notesCount: Int,
    onCycleFontPreset: () -> Unit,
    onTogglePreviewByDefault: () -> Unit,
    onExportAll: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StillColors.OledBlack),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 96.dp),
        ) {
            Text(
                text = "settings",
                style = StillTypography.Title,
                color = StillColors.SoftWhite,
            )
            Text(
                text = "still notes · v0.1.0",
                style = StillTypography.Caption,
                color = StillColors.DimGray,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
            )

            StillSectionCard {
                StillMenuItem(
                    title = "fonts",
                    subtitle = settings.fontPreset.label(),
                    onClick = onCycleFontPreset,
                )
                StillMenuItem(
                    title = "open as preview",
                    subtitle = if (settings.previewByDefault) "on — render markdown first" else "off — open in edit",
                    onClick = onTogglePreviewByDefault,
                )
            }

            Spacer(Modifier.height(20.dp))

            StillSectionCard {
                StillMenuItem(
                    title = "import notes",
                    subtitle = "pick .md files via the system picker",
                    onClick = onImport,
                )
                StillMenuItem(
                    title = "export all",
                    subtitle = "save every note as a .md inside a single zip",
                    onClick = onExportAll,
                )
            }

            Spacer(Modifier.height(20.dp))

            StillSectionCard {
                StillMenuItem(
                    title = "stored locally",
                    subtitle = "$notesCount ${if (notesCount == 1) "note" else "notes"} in app-private storage",
                    enabled = false,
                    onClick = {},
                )
                StillMenuItem(
                    title = "no internet",
                    subtitle = "the app declares no network permission",
                    enabled = false,
                    onClick = {},
                )
                StillMenuItem(
                    title = "no analytics",
                    subtitle = "no telemetry, no firebase, no play services",
                    enabled = false,
                    onClick = {},
                )
            }
        }

        FooterBar(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .navigationBarsPadding(),
            onBack = onBack,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FooterBar(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 22.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "back",
            style = StillTypography.SecondaryMenu,
            color = StillColors.MutedWhite,
            modifier = Modifier.combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onBack,
            ),
        )
    }
}

private fun FontPreset.label(): String = when (this) {
    FontPreset.System -> "system — serif + sans + mono"
    FontPreset.Editorial -> "editorial — cormorant + inter + plex"
    FontPreset.Terminal -> "terminal — plex mono throughout"
    FontPreset.Grotesk -> "grotesk — instrument serif + space"
}
