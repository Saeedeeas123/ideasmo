package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.NoteText
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * **The note's text menu** (v0.17.0).
 *
 * Saeed's order: *"when Note is chosen, above the place where the note is written the user must have a
 * simple text menu — bold, italic, smaller and larger, a font choice, the normal things."*
 *
 * So this is that menu, and the rules behind it live in [NoteText]: bold and italic are inline markers
 * inside the text (so they survive copy, export and the model layer), font and size belong to the whole
 * note and are stored beside it. The menu is deliberately small — the four things he named, in one row.
 */
@Composable
internal fun NoteToolbar(
    fontKey: String,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onSizeDown: () -> Unit,
    onSizeUp: () -> Unit,
    onFont: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fontsOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NoteToolButton(Icons.Outlined.FormatBold, stringResource(R.string.note_bold), onBold)
        NoteToolButton(Icons.Outlined.FormatItalic, stringResource(R.string.note_italic), onItalic)
        NoteToolButton(Icons.Outlined.TextDecrease, stringResource(R.string.note_smaller), onSizeDown)
        NoteToolButton(Icons.Outlined.TextIncrease, stringResource(R.string.note_larger), onSizeUp)

        Box {
            Row(
                modifier = Modifier
                    .heightIn(min = Sizes.touchMin)
                    .clip(RoundedCornerShape(Radius.small))
                    .clickable { fontsOpen = true }
                    .padding(horizontal = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = NoteText.fontLabel(NoteText.styleKey(fontKey, "normal")),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            DropdownMenu(expanded = fontsOpen, onDismissRequest = { fontsOpen = false }) {
                NoteText.FONTS.forEach { font ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(font.label)
                                Text(
                                    text = font.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            fontsOpen = false
                            onFont(font.key)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = stringResource(R.string.note_marker_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun NoteToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Sizes.touchMin)
            .clip(RoundedCornerShape(Radius.small))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Sizes.iconSm),
        )
    }
}

/**
 * A note as it reads: the markers taken out, bold and italic applied, and the note's own font and size.
 *
 * One definition, used by the list, by the live preview under the editor and by anything that shows a
 * note later — so a note can never look like two different things in two places.
 */
@Composable
internal fun NoteBody(
    raw: String,
    style: String?,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(raw) { NoteText.parse(raw) }
    val base = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * NoteText.scaleOf(style))
    Text(
        text = annotatedNote(parsed),
        style = base,
        fontFamily = noteFontFamily(NoteText.fontOf(style)),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

/** Builds the styled text from the parsed note: markers out, ranges styled. */
internal fun annotatedNote(parsed: NoteText.Parsed): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    parsed.spans.forEach { span ->
        if (span.start > cursor) append(parsed.text.substring(cursor, span.start))
        withStyle(
            SpanStyle(
                fontWeight = if (span.bold) FontWeight.Bold else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
            ),
        ) {
            append(parsed.text.substring(span.start, span.end))
        }
        cursor = span.end
    }
    if (cursor < parsed.text.length) append(parsed.text.substring(cursor))
}

/** The note's font, as a Compose family. Unknown keys fall back to the app's own typeface. */
internal fun noteFontFamily(key: String): FontFamily = when (key) {
    "serif" -> FontFamily.Serif
    "monospace" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    else -> FontFamily.Default
}