package dev.chuds.stillnotes.ui.note

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chuds.stillnotes.markdown.MarkdownBlockContent
import dev.chuds.stillnotes.markdown.parseBlocks
import dev.chuds.stillnotes.ui.components.StillVerb
import dev.chuds.stillnotes.ui.theme.StillColors
import dev.chuds.stillnotes.ui.theme.StillTypography
import kotlinx.coroutines.launch

/**
 * Single-note screen. Two modes: edit and preview.
 *
 * Preview uses a LazyColumn rather than verticalScroll so each block (notably code blocks)
 * can host its own horizontalScroll without losing gestures to the parent scroller.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteScreen(
    viewModel: NoteViewModel,
    startInEdit: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
) {
    val body by viewModel.body.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(startInEdit) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    DisposableEffect(viewModel) {
        onDispose { scope.launch { viewModel.flush() } }
    }

    LaunchedEffect(loaded, editing) {
        if (loaded && editing && body.isEmpty()) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StillColors.OledBlack),
    ) {
        if (!loaded) {
            Text(
                text = "loading…",
                style = StillTypography.Caption,
                color = StillColors.DimGray,
                modifier = Modifier.statusBarsPadding().padding(start = 24.dp, top = 36.dp),
            )
        } else if (editing) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 96.dp),
            ) {
                BasicTextField(
                    value = body,
                    onValueChange = viewModel::update,
                    textStyle = StillTypography.Editor.copy(color = StillColors.SoftWhite),
                    cursorBrush = SolidColor(StillColors.SoftWhite),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (body.isEmpty()) {
                                Text(
                                    text = "start typing. # heading, **bold**, `code`, ```block```, #tag",
                                    style = StillTypography.Editor,
                                    color = StillColors.DimGray,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        } else {
            val blocks = remember(body) { parseBlocks(body.ifBlank { "*empty note*" }) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 36.dp,
                    bottom = 96.dp,
                ),
            ) {
                itemsIndexed(blocks) { index, block ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    MarkdownBlockContent(block)
                }
            }
        }

        FooterBar(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            editing = editing,
            counts = counts,
            onBack = {
                scope.launch {
                    viewModel.flush()
                    onBack()
                }
            },
            onShare = onShare,
            onExport = onExport,
            onToggleMode = {
                if (editing) {
                    scope.launch {
                        viewModel.flush()
                        editing = false
                    }
                } else {
                    editing = true
                }
            },
        )
    }
}

@Composable
private fun FooterBar(
    modifier: Modifier = Modifier,
    editing: Boolean,
    counts: NoteViewModel.Counts,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onToggleMode: () -> Unit,
) {
    Column(
        modifier = modifier.background(StillColors.OledBlack),
    ) {
        if (editing) {
            Text(
                text = "${counts.words} ${if (counts.words == 1) "word" else "words"} · ${counts.chars} ${if (counts.chars == 1) "char" else "chars"}",
                style = StillTypography.Caption,
                color = StillColors.DimGray,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 2.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StillVerb(
                label = "back",
                onClick = onBack,
                bordered = true,
            )
            StillVerb(
                label = "share",
                onClick = onShare,
                bordered = true,
            )
            StillVerb(
                label = "export",
                onClick = onExport,
                bordered = true,
            )
            StillVerb(
                label = if (editing) "preview" else "edit",
                onClick = onToggleMode,
                bordered = true,
                color = StillColors.SoftWhite,
            )
        }
    }
}
