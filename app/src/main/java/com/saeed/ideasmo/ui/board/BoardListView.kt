package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.saeed.ideasmo.core.model.LinkType
import com.saeed.ideasmo.core.model.LinkView
import com.saeed.ideasmo.core.model.NodeLink
import com.saeed.ideasmo.core.model.NodeStore
import com.saeed.ideasmo.ui.components.EmptyState
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * List view of the same nodes the board shows. Relations are split into the ones that leave a node
 * and the ones that arrive at it, in branch order, and every row opens the link inspector — so the
 * list and the canvas cannot disagree, and a relation is editable from either view.
 */
@Composable
internal fun BoardListView(
    nodes: List<IdeaNode>,
    selectedId: String?,
    onSelect: (IdeaNode) -> Unit,
    onOpenLink: (NodeLink) -> Unit,
    onGoToBoard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (nodes.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                icon = Icons.Outlined.Inbox,
                title = stringResource(R.string.list_empty_title),
                detail = stringResource(R.string.list_empty_body),
                actionLabel = stringResource(R.string.list_empty_action),
                onAction = onGoToBoard,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = Spacing.screenH,
            vertical = Spacing.l,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        items(nodes, key = { it.id }) { node ->
            ListNodeRow(
                node = node,
                selected = node.id == selectedId,
                views = NodeStore.linkViews(node.id),
                hiddenBelow = if (node.collapsed) NodeStore.subtreeOf(node.id).size else 0,
                onClick = { onSelect(node) },
                onOpenLink = onOpenLink,
            )
        }
    }
}

@Composable
private fun ListNodeRow(
    node: IdeaNode,
    selected: Boolean,
    views: List<LinkView>,
    hiddenBelow: Int,
    onClick: () -> Unit,
    onOpenLink: (NodeLink) -> Unit,
) {
    val outgoing = views.filter { it.outgoing }
    val incoming = views.filter { !it.outgoing }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.listRowMin),
        shape = RoundedCornerShape(Radius.medium),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.l)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (node.isRoot) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = stringResource(R.string.node_central),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(Sizes.iconSm),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                }
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (hiddenBelow > 0) {
                    Text(
                        text = stringResource(R.string.node_hidden_badge, hiddenBelow),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xs))
            Row {
                (1..5).forEach { level ->
                    Box(
                        modifier = Modifier
                            .padding(end = Spacing.xs)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (level <= node.priority) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            ),
                    )
                }
            }
            if (node.description.isNotBlank()) {
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = node.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (views.isEmpty()) {
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = stringResource(R.string.not_linked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LinkGroup(
                    title = stringResource(R.string.node_outgoing, outgoing.size),
                    views = outgoing,
                    onOpenLink = onOpenLink,
                )
                LinkGroup(
                    title = stringResource(R.string.node_incoming, incoming.size),
                    views = incoming,
                    onOpenLink = onOpenLink,
                )
            }
        }
    }
}

@Composable
private fun LinkGroup(
    title: String,
    views: List<LinkView>,
    onOpenLink: (NodeLink) -> Unit,
) {
    Spacer(Modifier.height(Spacing.s))
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (views.isEmpty()) {
        Text(
            text = stringResource(R.string.link_none_in_group),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        return
    }
    views.forEach { view ->
        val type: LinkType = LinkCatalog.byKey(view.link.type)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.small))
                .clickable { onOpenLink(view.link) }
                .heightIn(min = Sizes.touchMin)
                .padding(top = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LinkSwatch(type = type)
            Spacer(Modifier.width(Spacing.s))
            Text(
                text = view.sentence,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}