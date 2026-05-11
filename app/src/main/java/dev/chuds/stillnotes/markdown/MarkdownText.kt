package dev.chuds.stillnotes.markdown

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import dev.chuds.stillnotes.ui.theme.LocalStillTypography
import dev.chuds.stillnotes.ui.theme.StillColors
import dev.chuds.stillnotes.ui.theme.StillTypography
import java.net.URI
import java.util.Locale

/**
 * Render markdown source as a Compose column. Block-level dispatch with inline parsing
 * applied per text-bearing block. No third-party deps.
 *
 * Two callers:
 *   - [MarkdownText]              renders into a plain Column (good for short snippets)
 *   - [MarkdownBlockContent]      renders a single block; pair with parseBlocks + LazyColumn
 *                                 so each block can host its own scrollable region without
 *                                 fighting a parent verticalScroll.
 */
@Composable
fun MarkdownText(
    source: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val blocks = remember(source) { parseBlocks(source) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
    ) {
        blocks.forEachIndexed { index, block ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            MarkdownBlockContent(block)
        }
    }
}

/**
 * Render one parsed block. Useful inside `LazyColumn { items(blocks) { MarkdownBlockContent(it) } }`
 * so that horizontalScroll inside a code block doesn't fight a parent verticalScroll.
 */
@Composable
fun MarkdownBlockContent(block: MdBlock, modifier: Modifier = Modifier) {
    when (block) {
        is MdBlock.Heading -> HeadingBlock(block, modifier)
        is MdBlock.Paragraph -> ParagraphBlock(block, modifier)
        is MdBlock.BulletList -> BulletListBlock(block, modifier)
        is MdBlock.OrderedList -> OrderedListBlock(block, modifier)
        is MdBlock.BlockQuote -> BlockQuoteBlock(block, modifier)
        is MdBlock.CodeBlock -> CodeBlockView(block, modifier)
        MdBlock.Rule -> RuleBlock(modifier)
    }
}

@Composable
private fun HeadingBlock(block: MdBlock.Heading, modifier: Modifier = Modifier) {
    val style = when (block.level) {
        1 -> StillTypography.H1
        2 -> StillTypography.H2
        else -> StillTypography.H3
    }
    val annotated = inline(block.text)
    LinkAwareText(
        text = annotated,
        style = style,
        color = StillColors.SoftWhite,
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

@Composable
private fun ParagraphBlock(block: MdBlock.Paragraph, modifier: Modifier = Modifier) {
    LinkAwareText(
        text = inline(block.text),
        style = StillTypography.Body,
        color = StillColors.SoftWhite,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun BulletListBlock(block: MdBlock.BulletList, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        block.items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(text = "•  ", style = StillTypography.Body, color = StillColors.MutedWhite)
                LinkAwareText(
                    text = inline(item),
                    style = StillTypography.Body,
                    color = StillColors.SoftWhite,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OrderedListBlock(block: MdBlock.OrderedList, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        block.items.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(
                    text = "${index + 1}.  ",
                    style = StillTypography.Body,
                    color = StillColors.MutedWhite,
                )
                LinkAwareText(
                    text = inline(item),
                    style = StillTypography.Body,
                    color = StillColors.SoftWhite,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BlockQuoteBlock(block: MdBlock.BlockQuote, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .background(StillColors.Hairline),
        )
        LinkAwareText(
            text = inline(block.text),
            style = StillTypography.BlockQuote,
            color = StillColors.MutedWhite,
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 2.dp, bottom = 2.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CodeBlockView(block: MdBlock.CodeBlock, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StillColors.CodeSurface)
            .border(1.dp, StillColors.Hairline, RoundedCornerShape(10.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
                onLongClick = { copyToClipboard(context, block.code, label = "code") },
            ),
    ) {
        if (!block.language.isNullOrBlank()) {
            Text(
                text = block.language.lowercase(),
                style = StillTypography.Caption,
                color = StillColors.DimGray,
                modifier = Modifier.padding(start = 14.dp, top = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = block.code,
                style = StillTypography.Code,
                color = StillColors.MutedWhite,
            )
        }
    }
}

@Composable
private fun RuleBlock(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .background(StillColors.Hairline),
    )
}

@Composable
private fun inline(text: String) = parseInline(
    source = text,
    monoFont = LocalStillTypography.current.Code.fontFamily,
    codeColor = StillColors.MutedWhite,
    codeBackground = StillColors.CodeSurface,
    linkColor = StillColors.MutedWhite,
    inlineCodeFontSize = LocalStillTypography.current.Code.fontSize,
)

/**
 * Renders an [AnnotatedString] and dispatches taps on URL-tagged spans to the system browser.
 * No INTERNET permission required — ACTION_VIEW lets the user's chosen browser do the work.
 */
@Composable
private fun LinkAwareText(
    text: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier.pointerInput(text) {
            detectTapGestures { offset ->
                val layout = layoutResult.value ?: return@detectTapGestures
                val pos = layout.getOffsetForPosition(offset)
                val annotation = text.getStringAnnotations(tag = "URL", start = pos, end = pos)
                    .firstOrNull()
                if (annotation != null) {
                    openUrl(context, annotation.item)
                }
            }
        },
        onTextLayout = { layoutResult.value = it },
    )
}

private fun openUrl(context: Context, url: String) {
    if (!isBrowserUrl(url)) return
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

internal fun isBrowserUrl(url: String): Boolean {
    val scheme = runCatching { URI(url).scheme?.lowercase(Locale.US) }.getOrNull()
        ?: return false
    return scheme == "http" || scheme == "https"
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    // Android 13+ shows a system clipboard chip; older versions get a quiet toast.
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "copied", Toast.LENGTH_SHORT).show()
    }
}
