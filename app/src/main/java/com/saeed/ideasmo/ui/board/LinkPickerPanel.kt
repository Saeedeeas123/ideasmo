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
) {
    var customPhrase by remember { mutableStateOf("") }
    val existing = NodeStore.linksOf(fromNode.id)
    val families = remember { LinkCatalog.byFamily() }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.l, end = Spacing.s, top = Spacing.s, bottom = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.link_picker_title, fromNode.name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.link_close),
                    )
                }
            }

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

                families.forEach { (family, types) ->
                    item(key = "family_${family.name}") {
                        FamilyHeader(title = family.title, question = family.question)
                    }
                    items(types, key = { "pick_" + it.key }) { type ->
                        LinkTypeRow(type = type, onClick = { onPick(type) })
                    }
                }

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