package com.saeed.ideasmo.ui.board

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
import androidx.compose.material.icons.outlined.AutoFixHigh
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.IdeaNode
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
    onReorder: (NodeLink, Int) -> Unit,
    onAutoOrder: (String) -> Unit,
    onToggleRoot: () -> Unit,
    onToggleCollapse: () -> Unit,
    onNudge: (Float, Float) -> Unit,
    onArrange: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outgoing = NodeStore.linksOut(node.id)
    val incoming = NodeStore.linksIn(node.id)
    val subtree = NodeStore.subtreeOf(node.id)
    val children = NodeStore.childrenOf(node.id)

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.node_panel_counts,
                            outgoing.size,
                            incoming.size,
                            subtree.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                // ---- role in the structure -----------------------------------
                item(key = "role") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                if (outgoing.isEmpty()) {
                    item(key = "out_empty") {
                        Text(
                            text = stringResource(R.string.node_no_outgoing),
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
                    items(outgoing, key = { "out_" + it.id }) { link ->
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
                if (incoming.isEmpty()) {
                    item(key = "in_empty") {
                        Text(
                            text = stringResource(R.string.node_no_incoming),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = Spacing.s),
                        )
                    }
                } else {
                    items(incoming, key = { "in_" + it.id }) { link ->
                        val type = LinkCatalog.byKey(link.type)
                        LinkRow(
                            sentence = LinkCatalog.sentence(
                                type,
                                NodeStore.nameOf(link.fromId),
                                NodeStore.nameOf(link.toId),
                                link.label,
                            ),
                            type = type,
                            onClick = { onOpenLink(link) },
                        )
                    }
                }

                // ---- move / arrange -------------------------------------------
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
                            modifier = Modifier.fillMaxWidth(),
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
                        Spacer(Modifier.height(Spacing.m))
                        OutlinedButton(
                            onClick = onArrange,
                            modifier = Modifier.heightIn(min = Sizes.touchMin),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(Sizes.iconSm),
                            )
                            Spacer(Modifier.width(Spacing.s))
                            Text(stringResource(R.string.node_arrange))
                        }
                    }
                }

                // ---- add / delete ---------------------------------------------
                item(key = "actions") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(Spacing.l))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = onAddLink,
                                modifier = Modifier.heightIn(min = Sizes.touchMin),
                            ) {
                                Text(stringResource(R.string.node_add_link))
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

/** One outgoing branch: its position in the order, its sentence, and the two reorder arrows. */
@Composable
private fun BranchRow(
    link: NodeLink,
    index: Int,
    total: Int,
    onOpen: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val type = LinkCatalog.byKey(link.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.listRowMin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (index + 1).toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(Radius.medium))
                .clickable(onClick = onOpen)
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
                text = LinkCatalog.sentence(
                    type,
                    NodeStore.nameOf(link.fromId),
                    NodeStore.nameOf(link.toId),
                    link.label,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
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

@Composable
private fun LinkRow(sentence: String, type: com.saeed.ideasmo.core.model.LinkType, onClick: () -> Unit) {
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
            text = sentence,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
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