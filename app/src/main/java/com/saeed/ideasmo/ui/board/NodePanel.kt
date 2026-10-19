package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.IdeaNode
import com.saeed.ideasmo.core.model.LayoutDirection
import com.saeed.ideasmo.core.model.LinkCatalog
import com.saeed.ideasmo.core.model.NodeLink
import com.saeed.ideasmo.core.model.NodeStore
import com.saeed.ideasmo.ui.components.ChoiceChip
import com.saeed.ideasmo.ui.components.SectionTitle
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/** How far one tap of a direction button moves a subtree, in board units. */
private const val NUDGE_STEP = 40f

/** The little preview swatch in front of a branch row, in dp. */
private val SWATCH_W = 44.dp

/**
 * Everything about one node in one place: the links that leave it, the links that arrive at it,
 * the order of its branches, and its place in the structure.
 *
 * Both directions are listed separately and every row opens the link inspector — the same editor
 * the board opens when a link is tapped on the canvas, so a relation can always be reached from
 * either end.
 */
@Composable
internal fun NodePanel(
    node: IdeaNode,
    onClose: () -> Unit,
    onOpenLink: (NodeLink) -> Unit,
    onAddLink: () -> Unit,
    onDuplicate: () -> Unit,
    onReorder: (NodeLink, Int) -> Unit,
    onAutoOrder: (String) -> Unit,
    onToggleRoot: () -> Unit,
    onToggleCollapse: () -> Unit,
    onNudge: (Float, Float) -> Unit,
    /** Choosing how this node's branch is arranged: a direction key, or null for Free. */
    onArrange: (String?) -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    /** How much of the viewport the sheet covers, and how to change it (v0.17.0). */
    fraction: Float = SHEET_DEFAULT_FRACTION,
    viewportHeightPx: Float = 0f,
    onFraction: (Float) -> Unit = {},
) {
    val outgoing = NodeStore.linksOut(node.id)
    val incoming = NodeStore.linksIn(node.id)
    val subtree = NodeStore.subtreeOf(node.id)
    val children = NodeStore.childrenOf(node.id)

    // A node can carry many relations. From v0.15.0 the two lists can be filtered by the relation's
    // own name, by the other node's name, or by any word of the sentence they read as — the same
    // "find it instead of scrolling it" that the catalog chooser got.
    var linkFilter by remember(node.id) { mutableStateOf("") }
    val needle = linkFilter.trim().lowercase()
    fun matches(link: NodeLink): Boolean {
        if (needle.isEmpty()) return true
        val type = LinkCatalog.byKey(link.type)
        val other = NodeStore.nameOf(link.otherEnd(node.id)).lowercase()
        val sentence = LinkCatalog.sentence(
            type,
            NodeStore.nameOf(link.fromId),
            NodeStore.nameOf(link.toId),
            link.label,
        ).lowercase()
        return type.name.lowercase().contains(needle) || other.contains(needle) || sentence.contains(needle)
    }
    val shownOut = outgoing.filter { matches(it) }
    val shownIn = incoming.filter { matches(it) }

    BoardSheet(
        // The shared board sheet (v0.17.0): the same rectangle Connect and the link inspector use, with
        // the resize handle, the board still movable behind it, and the node pinned in its header —
        // which was Saeed's earlier order ("a big rectangle with the node shown at its top, fixed").
        title = node.name,
        subtitle = stringResource(
            R.string.node_panel_counts,
            outgoing.size,
            incoming.size,
            subtree.size,
        ),
        fraction = fraction,
        viewportHeightPx = viewportHeightPx,
        onFraction = onFraction,
        onClose = onClose,
        modifier = modifier.imePadding(),
        header = {
            NodeCardBody(
                node = node,
                linkCount = outgoing.size + incoming.size,
                hiddenBelow = if (node.collapsed) subtree.size else 0,
                childCount = children.size,
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = Spacing.xs),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )
        },
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
                // ---- role in the structure -----------------------------------
                item(key = "role") {
                    Row(
                        // Scrolls, because the panel is 40% of the screen: two chips do not fit
                        // side by side in it and a clipped row reads as a broken panel.
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChoiceChip(
                            text = stringResource(R.string.node_central),
                            selected = node.isRoot,
                            onClick = onToggleRoot,
                            leading = {
                                Icon(
                                    imageVector = Icons.Outlined.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(Sizes.iconSm),
                                )
                            },
                        )
                        ChoiceChip(
                            text = stringResource(
                                if (node.collapsed) R.string.node_expand else R.string.node_collapse,
                                subtree.size,
                            ),
                            selected = node.collapsed,
                            onClick = onToggleCollapse,
                        )
                    }
                }

                // ---- find a relation (v0.15.0) --------------------------------
                // Only when there is something to find: a node with three relations does not need a
                // search box, one with fifteen does.
                if (outgoing.size + incoming.size > 6) {
                    item(key = "link_filter") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinkSearchField(
                                query = linkFilter,
                                onQuery = { linkFilter = it },
                                placeholder = stringResource(R.string.link_filter_hint),
                            )
                        }
                    }
                }

                // ---- outgoing branches, in order ------------------------------
                item(key = "out_header") {
                    SectionTitle(stringResource(R.string.node_outgoing, outgoing.size))
                }
                item(key = "out_hint") {
                    Text(
                        text = stringResource(R.string.node_order_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (shownOut.isEmpty()) {
                    item(key = "out_empty") {
                        Text(
                            text = stringResource(
                                if (outgoing.isEmpty()) {
                                    R.string.node_no_outgoing
                                } else {
                                    R.string.link_filter_none
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = Spacing.s),
                        )
                    }
                } else {
                    item(key = "out_modes") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.s)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ChoiceChip(
                                text = stringResource(R.string.order_manual),
                                selected = false,
                                onClick = { onAutoOrder(NodeStore.ORDER_MANUAL) },
                            )
                            ChoiceChip(
                                text = stringResource(R.string.order_name),
                                selected = false,
                                onClick = { onAutoOrder(NodeStore.ORDER_NAME) },
                            )
                            ChoiceChip(
                                text = stringResource(R.string.order_priority),
                                selected = false,
                                onClick = { onAutoOrder(NodeStore.ORDER_PRIORITY) },
                            )
                            ChoiceChip(
                                text = stringResource(R.string.order_created),
                                selected = false,
                                onClick = { onAutoOrder(NodeStore.ORDER_CREATED) },
                            )
                        }
                    }
                    items(shownOut, key = { "out_" + it.id }) { link ->
                        BranchRow(
                            link = link,
                            index = outgoing.indexOfFirst { it.id == link.id },
                            total = outgoing.size,
                            onOpen = { onOpenLink(link) },
                            onMove = { delta -> onReorder(link, delta) },
                        )
                    }
                }

                // ---- incoming links -------------------------------------------
                item(key = "in_header") {
                    SectionTitle(stringResource(R.string.node_incoming, incoming.size))
                }
                if (shownIn.isEmpty()) {
                    item(key = "in_empty") {
                        Text(
                            text = stringResource(
                                if (incoming.isEmpty()) {
                                    R.string.node_no_incoming
                                } else {
                                    R.string.link_filter_none
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = Spacing.s),
                        )
                    }
                } else {
                    items(shownIn, key = { "in_" + it.id }) { link ->
                        val type = LinkCatalog.byKey(link.type)
                        LinkRow(
                            title = LinkCatalog.displayLabel(type, link.label),
                            sentence = LinkCatalog.sentence(
                                type,
                                NodeStore.nameOf(link.fromId),
                                NodeStore.nameOf(link.toId),
                                link.label,
                            ),
                            note = link.note,
                            type = type,
                            onClick = { onOpenLink(link) },
                        )
                    }
                }

                // ---- arrange this branch --------------------------------------
                // Saeed's order: "the Arrange button has no place on the board — it belongs in the
                // node's own properties, and there it should be a choice of left, right, up or
                // down, applied to the node I selected." The node keeps its position; everything
                // below it is laid out in the direction chosen here.
                item(key = "arrange_header") {
                    SectionTitle(stringResource(R.string.node_arrange_branch))
                }
                item(key = "arrange") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.node_arrange_hint),
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
                            // Free first, then the five directions in the shared chip order. The selected chip is the
                            // arrangement the node actually carries (v0.15.0), so this sheet and the
                            // node's action box can never disagree about it.
                            DirectionChip(
                                label = stringResource(R.string.arrange_free),
                                icon = Icons.Outlined.Close,
                                selected = node.arrange == null,
                                onClick = { onArrange(null) },
                            )
                            LayoutDirection.chipOrder.forEach { direction ->
                                DirectionChip(
                                    label = stringResource(directionLabel(direction)),
                                    icon = directionIcon(direction),
                                    selected = node.arrange == direction.key,
                                    onClick = { onArrange(direction.key) },
                                )
                            }
                        }
                    }
                }

                // ---- move -----------------------------------------------
                item(key = "move_header") {
                    SectionTitle(stringResource(R.string.node_move))
                }
                item(key = "move") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.node_move_hint, children.size + 1),
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
                            NudgeButton(
                                icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                label = stringResource(R.string.move_left),
                                onClick = { onNudge(-NUDGE_STEP, 0f) },
                            )
                            NudgeButton(
                                icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                label = stringResource(R.string.move_right),
                                onClick = { onNudge(NUDGE_STEP, 0f) },
                            )
                            NudgeButton(
                                icon = Icons.Outlined.KeyboardArrowUp,
                                label = stringResource(R.string.move_up),
                                onClick = { onNudge(0f, -NUDGE_STEP) },
                            )
                            NudgeButton(
                                icon = Icons.Outlined.KeyboardArrowDown,
                                label = stringResource(R.string.move_down),
                                onClick = { onNudge(0f, NUDGE_STEP) },
                            )
                        }
                    }
                }

                // ---- add / delete ---------------------------------------------
                item(key = "actions") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(Spacing.l))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = onAddLink,
                                modifier = Modifier.heightIn(min = Sizes.touchMin),
                            ) {
                                Text(stringResource(R.string.node_add_link))
                            }
                            OutlinedButton(
                                onClick = onDuplicate,
                                modifier = Modifier.heightIn(min = Sizes.touchMin),
                            ) {
                                Text(stringResource(R.string.act_duplicate))
                            }
                            OutlinedButton(
                                onClick = onDetails,
                                modifier = Modifier.heightIn(min = Sizes.touchMin),
                            ) {
                                Text(stringResource(R.string.node_details))
                            }
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
                                    text = stringResource(R.string.delete_node),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = Spacing.xl))
                    }
                }
            }
        }
    }
}

/**
 * One outgoing branch: its sentence, then — on its own line — where it sits in the order and the two
 * reorder arrows.
 *
 * Two lines, not one: the panel is 40% of the screen, and a swatch + a sentence + two 48 dp buttons
 * cannot share one row in that width without clipping the sentence away.
 */
@Composable
private fun BranchRow(
    link: NodeLink,
    index: Int,
    total: Int,
    onOpen: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val type = LinkCatalog.byKey(link.type)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.medium))
                .clickable(onClick = onOpen)
                .padding(vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LinkSwatch(type = type, modifier = Modifier.width(SWATCH_W))
            Spacer(Modifier.width(Spacing.s))
            // Two lines: WHAT the relation is (its catalog name, which is the word the user chose)
            // and then how it reads as a sentence. Scanning a branch's relations is the thing Saeed
            // asked to make easier, and one sentence per row made that hard.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = LinkCatalog.displayLabel(type, link.label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = LinkCatalog.sentence(
                        type,
                        NodeStore.nameOf(link.fromId),
                        NodeStore.nameOf(link.toId),
                        link.label,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                link.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.branch_position, index + 1, total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(
                onClick = { onMove(-1) },
                enabled = index > 0,
                modifier = Modifier.size(Sizes.touchMin),
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.branch_up),
                    tint = if (index > 0) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }
            IconButton(
                onClick = { onMove(1) },
                enabled = index < total - 1,
                modifier = Modifier.size(Sizes.touchMin),
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.branch_down),
                    tint = if (index < total - 1) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }
        }
    }
}

@Composable
private fun LinkRow(
    title: String,
    sentence: String,
    note: String?,
    type: com.saeed.ideasmo.core.model.LinkType,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.medium))
            .clickable(onClick = onClick)
            .heightIn(min = Sizes.listRowMin)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinkSwatch(type = type, modifier = Modifier.width(SWATCH_W))
        Spacer(Modifier.width(Spacing.s))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = sentence,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            note?.takeIf { it.isNotBlank() }?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The label of one direction, in the words the node sheet and the action box both use. */
@Composable
private fun directionLabel(direction: LayoutDirection): Int = when (direction) {
    LayoutDirection.Right -> R.string.arrange_right
    LayoutDirection.Left -> R.string.arrange_left
    LayoutDirection.Up -> R.string.arrange_up
    LayoutDirection.Down -> R.string.arrange_down
    LayoutDirection.Radial -> R.string.arrange_radial
}

/** The arrow a direction is drawn with. Radial points back at its own centre, hence MyLocation. */
private fun directionIcon(direction: LayoutDirection): ImageVector = when (direction) {
    LayoutDirection.Right -> Icons.AutoMirrored.Outlined.KeyboardArrowRight
    LayoutDirection.Left -> Icons.AutoMirrored.Outlined.KeyboardArrowLeft
    LayoutDirection.Up -> Icons.Outlined.KeyboardArrowUp
    LayoutDirection.Down -> Icons.Outlined.KeyboardArrowDown
    LayoutDirection.Radial -> Icons.Outlined.MyLocation
}

/**
 * One of the five directions a branch can be arranged in (plus Free, which the sheet adds itself).
 * A chip, not a menu: Saeed explicitly dropped the "small list pops out of the button" idea in favour
 * of a choice inside the node's own properties.
 */
@Composable
private fun DirectionChip(
    label: String,
    icon: ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    ChoiceChip(
        text = label,
        selected = selected,
        onClick = onClick,
        leading = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconSm),
            )
        },
    )
}

@Composable
private fun NudgeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(Sizes.touchMin),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}