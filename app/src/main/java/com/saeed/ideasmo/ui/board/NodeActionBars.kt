package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.NodeAction
import com.saeed.ideasmo.core.model.NodeActionCatalog
import com.saeed.ideasmo.core.model.NodeActionGroup
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Spacing
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The node action square: four bars — top, left, right, bottom — that form a square around the
 * selected node, exactly the arrangement in the reference app Saeed sent.
 *
 * The bars are built **from [NodeActionCatalog]**, not from hand-placed buttons, so every action has
 * one id, one label and one machine-readable description, and adding an action means adding one
 * catalogue entry. The square lives in **screen space**, not in the board's transformed layer, so
 * the buttons keep a real 48 dp touch target at every zoom level, and it is centred on the node with
 * a minimum size so it never collapses into an unreadable cluster.
 */
@Composable
internal fun NodeActionBars(
    centreX: Float,
    centreY: Float,
    nodeWidthPx: Float,
    nodeHeightPx: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    clipboardHasContent: Boolean,
    onAction: (String) -> Unit,
) {
    val density = LocalDensity.current
    val itemW = with(density) { ITEM_W.toPx() }
    val itemH = with(density) { ITEM_H.toPx() }
    val barPad = with(density) { BAR_PAD.toPx() }
    val gap = with(density) { BAR_GAP.toPx() }

    val top = NodeActionCatalog.byGroup(NodeActionGroup.Content)
    val left = NodeActionCatalog.byGroup(NodeActionGroup.Clipboard)
    val right = NodeActionCatalog.byGroup(NodeActionGroup.Branch)
    val bottom = NodeActionCatalog.byGroup(NodeActionGroup.Style)

    val topW = itemW * top.size + barPad * 2
    val bottomW = itemW * bottom.size + barPad * 2
    val barH = itemH + barPad * 2
    val sideW = itemW + barPad * 2
    val leftH = itemH * left.size + barPad * 2
    val rightH = itemH * right.size + barPad * 2

    val halfW = max(nodeWidthPx / 2f, max(topW, bottomW) / 2f) + gap
    val halfH = max(nodeHeightPx / 2f, max(leftH, rightH) / 2f) + gap

    val edge = gap / 2f
    fun clampX(value: Float, width: Float) =
        value.coerceIn(edge, max(edge, viewportWidth - width - edge))

    fun clampY(value: Float, height: Float) =
        value.coerceIn(edge, max(edge, viewportHeight - height - edge))

    Box(
        modifier = Modifier.size(
            width = with(density) { viewportWidth.toDp() },
            height = with(density) { viewportHeight.toDp() },
        ),
    ) {
        NodeBar(
            modifier = Modifier.offset {
                IntOffset(
                    clampX(centreX - topW / 2f, topW).roundToInt(),
                    clampY(centreY - halfH - barH, barH).roundToInt(),
                )
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                top.forEach { action ->
                    BarButton(
                        action = action,
                        enabled = true,
                        onClick = { onAction(action.key) },
                    )
                }
            }
        }

        NodeBar(
            modifier = Modifier.offset {
                IntOffset(
                    clampX(centreX - topW / 2f, bottomW).roundToInt(),
                    clampY(centreY + halfH, barH).roundToInt(),
                )
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                bottom.forEach { action ->
                    BarButton(
                        action = action,
                        enabled = true,
                        onClick = { onAction(action.key) },
                    )
                }
            }
        }

        NodeBar(
            modifier = Modifier.offset {
                IntOffset(
                    clampX(centreX - halfW - sideW, sideW).roundToInt(),
                    clampY(centreY - leftH / 2f, leftH).roundToInt(),
                )
            },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                left.forEach { action ->
                    BarButton(
                        action = action,
                        // Paste is honest about the empty clipboard instead of failing silently.
                        enabled = action.key != NodeActionCatalog.PASTE || clipboardHasContent,
                        onClick = { onAction(action.key) },
                    )
                }
            }
        }

        NodeBar(
            modifier = Modifier.offset {
                IntOffset(
                    clampX(centreX + halfW, sideW).roundToInt(),
                    clampY(centreY - rightH / 2f, rightH).roundToInt(),
                )
            },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                right.forEach { action ->
                    BarButton(
                        action = action,
                        enabled = true,
                        onClick = { onAction(action.key) },
                    )
                }
            }
        }
    }
}

/** One bar of the square: a rounded tonal surface, no shadow, 1 dp outline. */
@Composable
private fun NodeBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Radius.medium),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        tonalElevation = 4.dp,
    ) {
        Box(modifier = Modifier.padding(horizontal = BAR_PAD, vertical = BAR_PAD)) {
            content()
        }
    }
}

/** One action: icon above its label, 62×60 dp — always a full touch target. */
@Composable
private fun BarButton(
    action: NodeAction,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(ITEM_W, ITEM_H)
            .clip(RoundedCornerShape(Radius.small))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = actionIcon(action.key),
            contentDescription = stringResource(actionLabelRes(action.key)),
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(actionLabelRes(action.key)),
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Item size, in dp. Fixed so the square has a predictable footprint on any screen. */
private val ITEM_W = 62.dp
private val ITEM_H = 60.dp
private val BAR_PAD = 4.dp
private val BAR_GAP = 10.dp

/** The label shown on the button — the catalogue's own labels live in strings.xml by key. */
private fun actionLabelRes(key: String): Int = when (key) {
    NodeActionCatalog.TEXT -> R.string.act_text
    NodeActionCatalog.IMAGE -> R.string.act_image
    NodeActionCatalog.CONNECT -> R.string.act_connect
    NodeActionCatalog.NOTES -> R.string.act_notes
    NodeActionCatalog.MORE -> R.string.act_more
    NodeActionCatalog.COPY -> R.string.act_copy
    NodeActionCatalog.CUT -> R.string.act_cut
    NodeActionCatalog.PASTE -> R.string.act_paste
    NodeActionCatalog.DELETE -> R.string.act_delete
    NodeActionCatalog.CHILD -> R.string.act_child
    NodeActionCatalog.SIBLING -> R.string.act_sibling
    NodeActionCatalog.DUPLICATE -> R.string.act_duplicate
    NodeActionCatalog.REATTACH -> R.string.act_reattach
    NodeActionCatalog.RESIZE -> R.string.act_resize
    NodeActionCatalog.COLOR -> R.string.act_color
    NodeActionCatalog.SHAPE -> R.string.act_shape
    else -> R.string.act_more
}

/** Icon per action. Outlined chrome, drawn from the icon font — never an emoji. */
private fun actionIcon(key: String): ImageVector = when (key) {
    NodeActionCatalog.TEXT -> Icons.Outlined.Edit
    NodeActionCatalog.IMAGE -> Icons.Outlined.Image
    NodeActionCatalog.CONNECT -> Icons.Outlined.Link
    NodeActionCatalog.NOTES -> Icons.Outlined.Description
    NodeActionCatalog.MORE -> Icons.Outlined.MoreVert
    NodeActionCatalog.COPY -> Icons.Outlined.ContentCopy
    NodeActionCatalog.CUT -> Icons.Outlined.ContentCut
    NodeActionCatalog.PASTE -> Icons.Outlined.ContentPaste
    NodeActionCatalog.DELETE -> Icons.Outlined.DeleteOutline
    NodeActionCatalog.CHILD -> Icons.Outlined.AccountTree
    NodeActionCatalog.SIBLING -> Icons.Outlined.Add
    NodeActionCatalog.DUPLICATE -> Icons.Outlined.FileCopy
    NodeActionCatalog.REATTACH -> Icons.AutoMirrored.Outlined.Undo
    NodeActionCatalog.RESIZE -> Icons.Outlined.OpenInFull
    NodeActionCatalog.COLOR -> Icons.Outlined.Palette
    NodeActionCatalog.SHAPE -> Icons.Outlined.CropSquare
    else -> Icons.Outlined.MoreVert
}