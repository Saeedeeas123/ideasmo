package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.IdeaNode
import com.saeed.ideasmo.core.model.LinkCatalog
import com.saeed.ideasmo.core.model.LinkFamily
import com.saeed.ideasmo.core.model.LinkType
import com.saeed.ideasmo.core.model.NodeLink
import com.saeed.ideasmo.core.model.NodeStore
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * Choosing a NEW relation for one node. Deliberately organised, not a flat wall of 42 entries:
 * the node's existing links first (each one opens the link inspector, so this screen and the board
 * lead to the same editor), then the catalog grouped by its seven families, then a custom phrase.
 * Every row shows how the link is drawn and how it reads as a sentence.
 */
@Composable
internal fun LinkPickerPanel(
    fromNode: IdeaNode,
    modifier: Modifier = Modifier,
    onPick: (LinkType) -> Unit,
    onCustom: (String) -> Unit,
    onOpenLink: (NodeLink) -> Unit,
    onClose: () -> Unit,
    /** How much of the viewport the sheet covers, and how to change it (v0.17.0). */
    fraction: Float = SHEET_DEFAULT_FRACTION,
    viewportHeightPx: Float = 0f,
    onFraction: (Float) -> Unit = {},
) {
    var customPhrase by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var family by remember { mutableStateOf<LinkFamily?>(null) }
    val existing = NodeStore.linksOf(fromNode.id)

    BoardSheet(
        // The shared board sheet (v0.17.0): Connect opens the SAME rectangle as More — the board stays
        // visible and movable behind it, and the handle resizes it. That is exactly what Saeed asked
        // for ("look at how More works; this must work the same way").
        title = stringResource(R.string.link_picker_title, fromNode.name),
        subtitle = stringResource(R.string.link_picker_detail),
        fraction = fraction,
        viewportHeightPx = viewportHeightPx,
        onFraction = onFraction,
        onClose = onClose,
        modifier = modifier.imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = Spacing.screenH,
                    end = Spacing.screenH,
                    bottom = Spacing.xxxl,
                ),
            ) {
                if (existing.isNotEmpty()) {
                    item(key = "existing_header") {
                        FamilyHeader(
                            title = stringResource(R.string.link_existing),
                            question = stringResource(R.string.link_existing_hint),
                        )
                    }
                    items(existing, key = { "ex_" + it.id }) { link ->
                        ExistingLinkRow(
                            link = link,
                            fromName = fromNode.name,
                            onClick = { onOpenLink(link) },
                        )
                    }
                    item(key = "existing_divider") {
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.m))
                    }
                }

                // ---- choosing a new relation (v0.15.0) ------------------------
                // Search + family chips first, then only the relations that are left. 42 rows in one
                // scroll was correct and slow; this is the "easier to choose, easier to group" half of
                // Saeed's order.
                item(key = "browse_header") {
                    FamilyHeader(
                        title = stringResource(R.string.link_pick_family),
                        question = stringResource(R.string.link_pick_family_hint),
                    )
                }
                item(key = "search") {
                    LinkSearchField(query = query, onQuery = { query = it })
                }
                item(key = "families") {
                    LinkFamilyChips(selected = family, onSelect = { family = it })
                }
                linkResults(
                    query = query,
                    family = family,
                    selectedKey = null,
                    onPick = onPick,
                )

                item(key = "custom") {
                    Spacer(Modifier.height(Spacing.xl))
                    Text(
                        text = stringResource(R.string.link_custom_hint),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Spacing.s))
                    OutlinedTextField(
                        value = customPhrase,
                        onValueChange = { customPhrase = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(Radius.medium),
                        label = { Text(stringResource(R.string.link_custom_hint)) },
                    )
                    Spacer(Modifier.height(Spacing.s))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = {
                                val phrase = customPhrase.trim()
                                if (phrase.isNotEmpty()) onCustom(phrase)
                            },
                            enabled = customPhrase.isNotBlank(),
                            modifier = Modifier.heightIn(min = Sizes.touchMin),
                        ) {
                            Text(stringResource(R.string.link_custom_use))
                        }
                    }
                }
            }
        }
    }
}

/** One existing relation of the node. Tapping it opens the link inspector for that link. */
@Composable
private fun ExistingLinkRow(link: NodeLink, fromName: String, onClick: () -> Unit) {
    val type = LinkCatalog.byKey(link.type)
    val other = NodeStore.nameOf(link.otherEnd(link.fromId))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.medium))
            .clickable(onClick = onClick)
            .heightIn(min = Sizes.listRowMin)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            LinkSwatch(type = type)
        }
        Spacer(Modifier.width(Spacing.s))
        Text(
            text = LinkCatalog.sentence(type, fromName, other, link.label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}