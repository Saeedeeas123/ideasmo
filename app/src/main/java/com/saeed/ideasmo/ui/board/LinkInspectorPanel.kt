package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.SwapHoriz
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
import com.saeed.ideasmo.core.model.LinkCatalog
import com.saeed.ideasmo.core.model.LinkColor
import com.saeed.ideasmo.core.model.LinkFamily
import com.saeed.ideasmo.core.model.LinkType
import com.saeed.ideasmo.core.model.NodeLink
import com.saeed.ideasmo.core.model.NodeStore
import com.saeed.ideasmo.ui.components.ChoiceChip
import com.saeed.ideasmo.ui.components.SectionTitle
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * The link inspector: everything a single relation can be.
 *
 * It is opened by tapping a link **on the board** (or its label), and by selecting one of a node's
 * links **inside the node panel** — both routes land here, so a relation is always edited the same
 * way. Every change is applied immediately and written to disk, and every edit that can lose
 * something (delete) offers Undo.
 */
@Composable
internal fun LinkInspectorPanel(
    link: NodeLink,
    onUpdate: (NodeLink) -> Unit,
    onOpenOther: (NodeLink) -> Unit,
    /** Jump to one end of the relation on the board — "show me this node". */
    onOpenNode: (String) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** How much of the viewport the sheet covers, and how to change it (v0.17.0). */
    fraction: Float = SHEET_DEFAULT_FRACTION,
    viewportHeightPx: Float = 0f,
    onFraction: (Float) -> Unit = {},
) {
    val type = LinkCatalog.byKey(link.type)
    val fromName = NodeStore.nameOf(link.fromId)
    val toName = NodeStore.nameOf(link.toId)
    val currentRole = link.colorRole()
    val siblings = remember(link.id, link.fromId, link.toId) {
        NodeStore.links.filter { it.id != link.id && it.connects(link.fromId, link.toId) }
    }
    var phrase by remember(link.id) { mutableStateOf(link.label ?: "") }
    // Editing this relation (v0.15.0): the type list is searchable and can be narrowed to one family,
    // and it opens on the family the relation already belongs to, so the current choice is in view.
    var query by remember(link.id) { mutableStateOf("") }
    var family by remember(link.id) { mutableStateOf<LinkFamily?>(type.family) }
    var note by remember(link.id) { mutableStateOf(link.note ?: "") }

    BoardSheet(
        // The same sheet as More and Connect (v0.17.0): tapping a relation on the board opens a
        // rectangle over the board, not a screen that replaces it.
        title = stringResource(R.string.link_inspector_title),
        subtitle = LinkCatalog.sentence(type, fromName, toName, link.label),
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
                // ---- what this relation reads as -----------------------------
                item(key = "sentence") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.medium),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(modifier = Modifier.padding(Spacing.l)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LinkSwatch(type = type)
                                Spacer(Modifier.width(Spacing.m))
                                Text(
                                    text = type.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.height(Spacing.s))
                            Text(
                                text = LinkCatalog.sentence(type, fromName, toName, link.label),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(Spacing.s))
                            Text(
                                text = type.meaning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = type.example,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }

                // ---- which two nodes it joins --------------------------------
                item(key = "ends_header") {
                    SectionTitle(stringResource(R.string.link_ends))
                }
                item(key = "ends") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        EndRow(
                            label = stringResource(R.string.link_from),
                            name = fromName,
                            emphasised = type.directed,
                            onOpen = { onOpenNode(link.fromId) },
                        )
                        Spacer(Modifier.height(Spacing.s))
                        EndRow(
                            label = stringResource(R.string.link_to),
                            name = toName,
                            emphasised = false,
                            onOpen = { onOpenNode(link.toId) },
                        )
                        Spacer(Modifier.height(Spacing.s))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = { onUpdate(link.copy(fromId = link.toId, toId = link.fromId)) },
                                modifier = Modifier.heightIn(min = Sizes.touchMin),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(Sizes.iconSm),
                                )
                                Spacer(Modifier.width(Spacing.s))
                                Text(stringResource(R.string.link_swap))
                            }
                            Spacer(Modifier.width(Spacing.s))
                            Text(
                                text = stringResource(
                                    if (type.directed) R.string.link_directed else R.string.link_undirected,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // ---- the other relations between the same pair ---------------
                if (siblings.isNotEmpty()) {
                    item(key = "siblings_header") {
                        SectionTitle(stringResource(R.string.link_other_between))
                    }
                    items(siblings, key = { "sib_" + it.id }) { other ->
                        val otherType = LinkCatalog.byKey(other.type)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.medium))
                                .clickable { onOpenOther(other) }
                                .heightIn(min = Sizes.listRowMin)
                                .padding(vertical = Spacing.s),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.width(52.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                LinkSwatch(type = otherType)
                            }
                            Spacer(Modifier.width(Spacing.s))
                            Text(
                                text = LinkCatalog.sentence(otherType, fromName, toName, other.label),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Sizes.iconMd),
                            )
                        }
                    }
                }

                // ---- colour ---------------------------------------------------
                item(key = "color_header") {
                    SectionTitle(stringResource(R.string.link_colour))
                }
                item(key = "color_chips") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.link_colour_detail),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.s))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ColorChip(
                                label = stringResource(R.string.link_colour_default),
                                role = null,
                                selected = link.color == null,
                                onClick = { onUpdate(link.copy(color = null)) },
                            )
                            LinkColor.values().forEach { role ->
                                ColorChip(
                                    label = role.name,
                                    role = role,
                                    selected = link.color == role.name,
                                    onClick = { onUpdate(link.copy(color = role.name)) },
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.s))
                        Text(
                            text = stringResource(
                                R.string.link_colour_current,
                                LinkCatalog.byKey(link.type).name,
                                currentRole.name,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                // ---- custom phrase -------------------------------------------
                item(key = "phrase_header") {
                    SectionTitle(stringResource(R.string.link_phrase))
                }
                item(key = "phrase") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = phrase,
                            onValueChange = { phrase = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(Radius.medium),
                            label = { Text(stringResource(R.string.link_custom_hint)) },
                        )
                        Spacer(Modifier.height(Spacing.s))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = {
                                    phrase = ""
                                    onUpdate(link.copy(label = null))
                                },
                                enabled = link.label != null,
                                modifier = Modifier.heightIn(min = Sizes.touchMin),
                            ) {
                                Text(stringResource(R.string.link_phrase_clear))
                            }
                            TextButton(
                                onClick = {
                                    val trimmed = phrase.trim()
                                    onUpdate(link.copy(label = trimmed.ifEmpty { null }))
                                },
                                enabled = phrase.isNotBlank() && phrase.trim() != link.label,
                                modifier = Modifier.heightIn(min = Sizes.touchMin),
                            ) {
                                Text(stringResource(R.string.link_phrase_apply))
                            }
                        }
                    }
                }

                // ---- the relation's own note (v0.15.0) ------------------------
                // NodeLink.note has existed in the model and in the saved file since relations got
                // an identity, but nothing ever showed it. A relation can now carry a sentence that
                // explains it ("agreed in the October review"), which is what makes the two ends
                // self-explanatory months later.
                item(key = "note_header") {
                    SectionTitle(stringResource(R.string.link_note))
                }
                item(key = "note") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Radius.medium),
                            minLines = 2,
                            label = { Text(stringResource(R.string.link_note_hint)) },
                        )
                        Spacer(Modifier.height(Spacing.s))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = {
                                    note = ""
                                    onUpdate(link.copy(note = null))
                                },
                                enabled = link.note != null,
                                modifier = Modifier.heightIn(min = Sizes.touchMin),
                            ) {
                                Text(stringResource(R.string.link_note_clear))
                            }
                            TextButton(
                                onClick = {
                                    val trimmed = note.trim()
                                    onUpdate(link.copy(note = trimmed.ifEmpty { null }))
                                },
                                enabled = note.trim() != (link.note ?: ""),
                                modifier = Modifier.heightIn(min = Sizes.touchMin),
                            ) {
                                Text(stringResource(R.string.link_note_apply))
                            }
                        }
                    }
                }

                // ---- change the relation -------------------------------------
                // LAST, because it is the long one: everything that describes the relation as it is
                // now — ends, colour, phrase, note — is reachable without scrolling past 42 types, and
                // the catalog to change it into sits at the bottom where a long list belongs.
                item(key = "type_header") {
                    SectionTitle(stringResource(R.string.link_change_type))
                }
                item(key = "type_search") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinkSearchField(query = query, onQuery = { query = it })
                        Spacer(Modifier.height(Spacing.s))
                        LinkFamilyChips(selected = family, onSelect = { family = it })
                    }
                }
                linkResults(
                    query = query,
                    family = family,
                    selectedKey = type.key,
                    onPick = { candidate -> onUpdate(link.copy(type = candidate.key)) },
                )

                // ---- delete ---------------------------------------------------
                item(key = "delete") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.l))
                        TextButton(
                            onClick = onDelete,
                            modifier = Modifier.heightIn(min = Sizes.touchMin),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(Sizes.iconSm),
                            )
                            Spacer(Modifier.width(Spacing.s))
                            Text(
                                text = stringResource(R.string.link_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EndRow(label: String, name: String, emphasised: Boolean, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.listRowMin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = if (emphasised) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        // "Show me this end" — one tap and the board is centred on that node with it selected, so a
        // relation can be followed in either direction without closing the panel by hand first.
        IconButton(onClick = onOpen, modifier = Modifier.size(Sizes.touchMin)) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = stringResource(R.string.link_open_node),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ColorChip(
    label: String,
    role: LinkColor?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val swatch = if (role == null) MaterialTheme.colorScheme.outline else linkColor(role)
    ChoiceChip(
        text = label,
        selected = selected,
        onClick = onClick,
        leading = {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(swatch),
            )
        },
    )
}

@Composable
internal fun FamilyHeader(title: String, question: String?) {
    Column(modifier = Modifier.padding(top = Spacing.xl, bottom = Spacing.s)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (question != null) {
            Text(
                text = question,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Shared by the picker and the inspector: one relation type, with its swatch and its meaning. */
@Composable
internal fun LinkTypeRow(type: LinkType, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.medium))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = Sizes.listRowMin)
            .padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            LinkSwatch(type = type)
        }
        Spacer(Modifier.width(Spacing.s))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = type.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = type.example,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = type.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}