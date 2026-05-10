package dev.chuds.stillnotes.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chuds.stillnotes.data.Note
import dev.chuds.stillnotes.ui.components.StillDivider
import dev.chuds.stillnotes.ui.components.StillVerb
import dev.chuds.stillnotes.ui.theme.StillColors
import dev.chuds.stillnotes.ui.theme.StillTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reverse-chronological note list. Search reveals an inline field; tags filter via a chip row.
 * Long-press a note for pin / share / export / delete.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesListScreen(
    viewModel: NotesListViewModel,
    onOpenNote: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onCreateNote: (String) -> Unit,
    onExportNote: (String) -> Unit,
    onShareNote: (String) -> Unit,
) {
    val notes by viewModel.visibleNotes.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val tagFilter by viewModel.tagFilter.collectAsStateWithLifecycle()
    val tags by viewModel.knownTags.collectAsStateWithLifecycle()

    var searching by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StillColors.OledBlack),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ListHeader(
                count = notes.size,
                searching = searching,
                query = query,
                onQueryChange = viewModel::setQuery,
                tagFilter = tagFilter,
            )
            if (tags.isNotEmpty()) {
                TagRow(
                    tags = tags,
                    selected = tagFilter,
                    onTap = { viewModel.setTagFilter(it) },
                )
            }

            if (notes.isEmpty()) {
                EmptyState(
                    searching = searching || query.isNotBlank() || tagFilter != null,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    items(items = notes, key = { it.id }) { note ->
                        NoteRow(
                            note = note,
                            onClick = { onOpenNote(note.id) },
                            onLongClick = { actionTarget = note.id },
                        )
                        StillDivider()
                    }
                }
            }
        }

        FooterActions(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            searching = searching,
            onSettings = onOpenSettings,
            onToggleSearch = {
                searching = !searching
                if (!searching) viewModel.setQuery("")
            },
            onNew = { viewModel.create(onCreateNote) },
        )

        actionTarget?.let { id ->
            val note = notes.firstOrNull { it.id == id }
            if (note != null) {
                ActionSheet(
                    note = note,
                    onTogglePinned = {
                        viewModel.togglePinned(id)
                        actionTarget = null
                    },
                    onShare = {
                        actionTarget = null
                        onShareNote(id)
                    },
                    onExport = {
                        actionTarget = null
                        onExportNote(id)
                    },
                    onDelete = {
                        viewModel.delete(id)
                        actionTarget = null
                    },
                    onDismiss = { actionTarget = null },
                )
            } else {
                actionTarget = null
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListHeader(
    count: Int,
    searching: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    tagFilter: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 12.dp),
    ) {
        Text(
            text = "notes",
            style = StillTypography.Title,
            color = StillColors.SoftWhite,
        )
        Text(
            text = buildString {
                append(if (count == 0) "empty" else "$count " + if (count == 1) "entry" else "entries")
                if (tagFilter != null) append("  ·  #$tagFilter")
            },
            style = StillTypography.Caption,
            color = StillColors.DimGray,
            modifier = Modifier.padding(top = 6.dp),
        )

        if (searching) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = StillTypography.Editor.copy(color = StillColors.SoftWhite),
                cursorBrush = SolidColor(StillColors.SoftWhite),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "search…",
                                style = StillTypography.Editor,
                                color = StillColors.DimGray,
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagRow(
    tags: List<String>,
    selected: String?,
    onTap: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagChip(
            label = "all",
            active = selected == null,
            onTap = { onTap(null) },
        )
        tags.forEach { tag ->
            Spacer(Modifier.width(10.dp))
            TagChip(
                label = "#$tag",
                active = selected == tag,
                onTap = { onTap(if (selected == tag) null else tag) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagChip(
    label: String,
    active: Boolean,
    onTap: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = label,
        style = StillTypography.Caption,
        color = if (active) StillColors.SoftWhite else StillColors.DimGray,
        modifier = Modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap,
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
    )
}

@Composable
private fun EmptyState(searching: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (searching) "no matches" else "tap new to start a note",
            style = StillTypography.SecondaryMenu,
            color = StillColors.DimGray,
            modifier = Modifier.padding(bottom = 80.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(
    note: Note,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = note.title.ifBlank { "untitled" },
                style = StillTypography.SecondaryMenu,
                color = if (note.title.isBlank()) StillColors.DimGray else StillColors.SoftWhite,
                modifier = Modifier.weight(1f, fill = true).padding(end = 12.dp),
            )
            if (note.pinned) {
                Text(text = "pinned", style = StillTypography.Caption, color = StillColors.Gray)
            }
        }

        if (note.preview.isNotBlank()) {
            Text(
                text = note.preview,
                style = StillTypography.Small,
                color = StillColors.MutedWhite,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatRelativeTimestamp(note.updatedAt),
                style = StillTypography.Caption,
                color = StillColors.DimGray,
            )
            if (note.tags.isNotEmpty()) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = note.tags.take(3).joinToString("  ") { "#$it" } +
                        if (note.tags.size > 3) "  +${note.tags.size - 3}" else "",
                    style = StillTypography.Caption,
                    color = StillColors.Gray,
                )
            }
        }
    }
}

@Composable
private fun FooterActions(
    modifier: Modifier = Modifier,
    searching: Boolean,
    onSettings: () -> Unit,
    onToggleSearch: () -> Unit,
    onNew: () -> Unit,
) {
    Row(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StillVerb(
            label = "settings",
            onClick = onSettings,
            bordered = true,
        )
        StillVerb(
            label = if (searching) "close" else "search",
            onClick = onToggleSearch,
            bordered = true,
            color = if (searching) StillColors.SoftWhite else StillColors.MutedWhite,
        )
        StillVerb(
            label = "new",
            onClick = onNew,
            bordered = true,
            color = StillColors.SoftWhite,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionSheet(
    note: Note,
    onTogglePinned: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dismissInteraction = remember { MutableInteractionSource() }
    val pinInteraction = remember { MutableInteractionSource() }
    val shareInteraction = remember { MutableInteractionSource() }
    val exportInteraction = remember { MutableInteractionSource() }
    val deleteInteraction = remember { MutableInteractionSource() }
    val cancelInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StillColors.OledBlack.copy(alpha = 0.94f))
            .combinedClickable(
                interactionSource = dismissInteraction,
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Text(
                text = note.title.ifBlank { "untitled" },
                style = StillTypography.Caption,
                color = StillColors.DimGray,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            ActionRow(if (note.pinned) "unpin" else "pin", onTogglePinned, pinInteraction)
            ActionRow("share", onShare, shareInteraction)
            ActionRow("export", onExport, exportInteraction)
            ActionRow("delete", onDelete, deleteInteraction)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "cancel",
                style = StillTypography.SecondaryMenu,
                color = StillColors.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = cancelInteraction,
                        indication = null,
                        onClick = onDismiss,
                    )
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionRow(
    label: String,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
) {
    Text(
        text = label,
        style = StillTypography.Menu,
        color = StillColors.SoftWhite,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
    )
}

private fun formatRelativeTimestamp(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - epochMillis
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        diff < minute -> "just now"
        diff < hour -> "${diff / minute}m ago"
        diff < day -> "${diff / hour}h ago"
        diff < 7 * day -> "${diff / day}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMillis))
    }
}

