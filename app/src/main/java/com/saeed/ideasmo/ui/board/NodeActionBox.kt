package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.IdeaNode
import com.saeed.ideasmo.core.model.LayoutDirection
import com.saeed.ideasmo.core.model.LinkColor
import com.saeed.ideasmo.core.model.NodeAction
import com.saeed.ideasmo.core.model.NodeActionCatalog
import com.saeed.ideasmo.core.model.NodeStyle
import com.saeed.ideasmo.core.model.StructureLayout
import com.saeed.ideasmo.ui.components.ChoiceChip
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Spacing
import kotlin.math.roundToInt

/** Which tool the action box is currently showing. */
internal enum class NodeTool { None, Resize, Colour, Shape, Arrange }

/**
 * The node action box: **one rectangle above the node** that holds every option in one place.
 *
 * Saeed's order after testing v0.10.0: *"the left bar came back but it is still not relative — forget
 * this, this way is more torture. We make the whole graphic rectangle-based: instead of these bars,
 * make a rectangular box that opens above the node, with all its options right there."*
 *
 * So the four bars that used to surround the card are gone. One rectangle, always the same shape,
 * always in the same relationship to its node:
 *
 * - it opens **above the node**, and flips **below** it when there is no room above;
 * - it **never scales with the zoom** (the v0.10.0 attempt at a proportional square is what he called
 *   torture) — the buttons are the same size at 5 % and at 200 %, which is what makes them hittable
 *   at every zoom;
 * - it holds **all sixteen actions** from [NodeActionCatalog] in a 6-column grid, so nothing is
 *   hidden behind a menu, and the tools (Resize, Colour, Shape) open their controls in an extra row
 *   inside the same box.
 */
@Composable
internal fun NodeActionBox(
    node: IdeaNode,
    linkCount: Int,
    childCount: Int,
    centreX: Float,
    nodeTopPx: Float,
    nodeBottomPx: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    clipboardHasContent: Boolean,
    tool: NodeTool,
    onAction: (String) -> Unit,
    onTool: (NodeTool) -> Unit,
    onColor: (String?) -> Unit,
    onShape: (String) -> Unit,
    onResizeDone: () -> Unit,
    /** Choosing how this node's branch is arranged — a direction key, or null for Free. */
    onArrange: (String?) -> Unit,
) {
    val density = LocalDensity.current
    val margin = with(density) { BOX_MARGIN.toPx() }
    val gap = with(density) { BOX_GAP.toPx() }
    val pad = with(density) { BOX_PAD.toPx() }
    val rowGap = with(density) { ROW_GAP.toPx() }
    val cellH = with(density) { CELL_H.toPx() }
    val toolRowH = with(density) { TOOL_ROW_H.toPx() }
    val boxW = minOf(viewportWidth - margin * 2f, with(density) { BOX_MAX_W.toPx() })
    val boxWDp = with(density) { boxW.toDp() }

    val rows = NodeActionCatalog.box.chunked(COLUMNS)
    val bodyH = rows.size * cellH + (rows.size - 1) * rowGap
    val toolH = if (tool == NodeTool.None) 0f else toolRowH + rowGap
    val boxH = pad * 2f + bodyH + toolH

    // Above the node, or below it when the top of the screen is in the way; then kept inside the
    // viewport on both axes so no button can ever be pushed off screen.
    val left = (centreX - boxW / 2f).coerceIn(margin, maxOf(margin, viewportWidth - margin - boxW))
    val above = nodeTopPx - gap - boxH
    val below = nodeBottomPx + gap
    val top = if (above >= margin) {
        above
    } else {
        below.coerceIn(margin, maxOf(margin, viewportHeight - margin - boxH))
    }

    Box(
        modifier = Modifier.size(
            width = with(density) { viewportWidth.toDp() },
            height = with(density) { viewportHeight.toDp() },
        ),
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .width(boxWDp),
            shape = RoundedCornerShape(Radius.medium),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(BOX_PAD),
                verticalArrangement = Arrangement.spacedBy(ROW_GAP),
            ) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.width(boxWDp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        row.forEach { action ->
                            ActionCell(
                                action = action,
                                enabled = action.key != NodeActionCatalog.PASTE || clipboardHasContent,
                                height = CELL_H,
                                // How many of that thing the node already has (Saeed: the Note and Image
                                // actions must show the number, and the same goes for the counts that
                                // used to live in the list view).
                                badge = when (action.key) {
                                    NodeActionCatalog.NOTES -> node.notes.size
                                    NodeActionCatalog.IMAGE -> node.pictures.size
                                    NodeActionCatalog.FILES -> node.files.size
                                    NodeActionCatalog.CONNECT -> linkCount
                                    NodeActionCatalog.CHILD -> childCount
                                    else -> 0
                                },
                                modifier = Modifier.weight(1f),
                                onClick = { onAction(action.key) },
                            )
                        }
                        // Keeps the last, shorter row aligned with the grid above it.
                        repeat(COLUMNS - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }

                if (tool != NodeTool.None) {
                    Box(
                        modifier = Modifier
                            .width(boxWDp)
                            .height(TOOL_ROW_H),
                    ) {
                        when (tool) {
                            NodeTool.Colour -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                SwatchButton(
                                    colour = MaterialTheme.colorScheme.surface,
                                    selected = node.color == null,
                                    onClick = { onColor(null) },
                                )
                                NodeStyle.COLORS.forEach { option ->
                                    val role = LinkColor.values()
                                        .firstOrNull { it.name.equals(option.key, true) }
                                    SwatchButton(
                                        colour = if (role != null) {
                                            themeRoleColor(MaterialTheme.colorScheme, role)
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        selected = node.color == option.key,
                                        onClick = { onColor(option.key) },
                                    )
                                }
                                ToolClose(onClick = { onTool(NodeTool.None) })
                            }

                            NodeTool.Shape -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                NodeStyle.SHAPES.forEach { option ->
                                    ShapeButton(
                                        shapeKey = option.key,
                                        label = option.label,
                                        selected = node.shape == option.key,
                                        onClick = { onShape(option.key) },
                                    )
                                }
                                ToolClose(onClick = { onTool(NodeTool.None) })
                            }

                            NodeTool.Resize -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.resize_readout,
                                        StructureLayout.widthOf(node).roundToInt(),
                                        StructureLayout.heightOf(node).roundToInt(),
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(R.string.resize_done),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(Radius.small))
                                        .clickable(onClick = onResizeDone)
                                        .padding(horizontal = Spacing.m, vertical = Spacing.s),
                                )
                            }

                            // Arrange (v0.15.0): the direction this node's branch grows in — and
                            // Free, which gives the position back to the user. It lives HERE, in the
                            // node's own menu, exactly as Saeed ordered ("the arrange part is in the
                            // board's general settings; bring it into the node menu"), and the chosen
                            // chip is the one the node is actually arranged by.
                            NodeTool.Arrange -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                ArrangeChip(
                                    key = null,
                                    label = stringResource(R.string.arrange_free),
                                    current = node.arrange,
                                    onArrange = onArrange,
                                )
                                LayoutDirection.chipOrder.forEach { option ->
                                    ArrangeChip(
                                        key = option.key,
                                        label = arrangeLabel(option),
                                        current = node.arrange,
                                        onArrange = onArrange,
                                    )
                                }
                                ToolClose(onClick = { onTool(NodeTool.None) })
                            }

                            NodeTool.None -> Unit
                        }
                    }
                }
            }
        }
    }
}

/** One action in the grid: its icon above its label, filling one cell of the box. */
@Composable
private fun ActionCell(
    action: NodeAction,
    enabled: Boolean,
    height: Dp,
    badge: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .height(height)
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.small))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = actionIcon(action.key),
                contentDescription = stringResource(actionLabelRes(action.key)),
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(actionLabelRes(action.key)),
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // The count sits on the action itself: "Note 3", "Image 5", "Files 2", "Connect 4", "Child 2".
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 2.dp)
                    .size(width = 18.dp, height = 14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A colour swatch inside the palette row. */
@Composable
private fun SwatchButton(
    colour: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SWATCH_W, TOOL_ROW_H)
            .clip(RoundedCornerShape(Radius.small))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 30.dp else 24.dp)
                .clip(CircleShape)
                .background(colour),
        )
    }
}

/** A shape option inside the shape row: a small preview of the shape. */
@Composable
private fun ShapeButton(
    shapeKey: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .height(TOOL_ROW_H)
            .clip(RoundedCornerShape(Radius.small))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 22.dp)
                .clip(nodeShapeOf(shapeKey))
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The label of one direction chip. The same words the node sheet uses, so both read the same. */
@Composable
private fun arrangeLabel(direction: LayoutDirection): String = stringResource(
    when (direction) {
        LayoutDirection.Right -> R.string.arrange_right
        LayoutDirection.Left -> R.string.arrange_left
        LayoutDirection.Up -> R.string.arrange_up
        LayoutDirection.Down -> R.string.arrange_down
        LayoutDirection.Radial -> R.string.arrange_radial
    },
)

/**
 * One chip in the Arrange row — Free, or one of the five directions.
 *
 * [key] is the value that gets stored on the node, so Free is simply `null`: the chip row is a
 * chooser, and the selected one is what the node is actually arranged by.
 */
@Composable
private fun ArrangeChip(
    key: String?,
    label: String,
    current: String?,
    onArrange: (String?) -> Unit,
) {
    ChoiceChip(
        text = label,
        selected = current == key,
        onClick = { onArrange(key) },
    )
}

/** Leaves a tool and puts the box back to its normal rows. */
@Composable
private fun ToolClose(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(SWATCH_W, TOOL_ROW_H)
            .clip(RoundedCornerShape(Radius.small))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = stringResource(R.string.link_close),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** How many actions share a row. Six keeps every label readable on a 360 dp screen. */
private const val COLUMNS = 6

/** The widest the box may get, so it still looks like a box on a tablet. */
private val BOX_MAX_W = 420.dp

/** The gap between the box and the node it belongs to. */
private val BOX_GAP = 10.dp

/** Space between the box and the edge of the screen. */
private val BOX_MARGIN = 6.dp

/** Padding inside the box, and between its rows. */
private val BOX_PAD = 6.dp
private val ROW_GAP = 4.dp

/** One action cell, and the extra row a tool adds. */
private val CELL_H = 58.dp
private val TOOL_ROW_H = 44.dp

/** The width of a swatch or a close button in a tool row. */
private val SWATCH_W = 44.dp

/** The label shown on the button — the catalogue's own labels live in strings.xml by key. */
private fun actionLabelRes(key: String): Int = when (key) {
    NodeActionCatalog.TEXT -> R.string.act_name
    NodeActionCatalog.IMAGE -> R.string.act_image
    NodeActionCatalog.FILES -> R.string.act_files
    NodeActionCatalog.CONNECT -> R.string.act_connect
    NodeActionCatalog.NOTES -> R.string.act_note
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
    NodeActionCatalog.ARRANGE -> R.string.act_arrange
    else -> R.string.act_more
}

/** Icon per action. Outlined chrome, drawn from the icon font — never an emoji. */
private fun actionIcon(key: String): ImageVector = when (key) {
    NodeActionCatalog.TEXT -> Icons.Outlined.Edit
    NodeActionCatalog.IMAGE -> Icons.Outlined.Image
    NodeActionCatalog.FILES -> Icons.AutoMirrored.Outlined.InsertDriveFile
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
    NodeActionCatalog.ARRANGE -> Icons.Outlined.AutoFixHigh
    else -> Icons.Outlined.MoreVert
}
