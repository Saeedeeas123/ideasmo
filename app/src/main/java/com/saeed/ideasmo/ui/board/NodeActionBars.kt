package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.IdeaNode
import com.saeed.ideasmo.core.model.LinkColor
import com.saeed.ideasmo.core.model.NodeAction
import com.saeed.ideasmo.core.model.NodeActionCatalog
import com.saeed.ideasmo.core.model.NodeActionGroup
import com.saeed.ideasmo.core.model.NodeStyle
import com.saeed.ideasmo.core.model.StructureLayout
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Spacing
import kotlin.math.max
import kotlin.math.roundToInt

/** Which tool the bottom bar is currently showing. */
internal enum class NodeTool { None, Resize, Colour, Shape }

/**
 * The node action square: four bars — top, left, right, bottom — around the selected node.
 *
 * Two rules make it behave:
 *
 * 1. **Everything is relative to the selected node.** The bars are placed from the node's centre and
 *    the whole square is shifted **as one unit** when it would leave the screen, so it keeps its
 *    shape and stays attached to its node while the board is panned, zoomed or the node is dragged.
 *    (v0.8.0 clamped each bar on its own, which let them fly apart — Saeed's report.)
 * 2. **The tools live inside the square.** The bottom bar is not a menu that navigates away: tapping
 *    Resize, Colour or Shape turns that same bar into the tool — resize readout with handles on the
 *    card, a colour palette, or the shape picker.
 *
 * The bars are built from [NodeActionCatalog], so every action has one id, one label and one
 * machine-readable description, and adding an action means adding one catalogue entry.
 */
@Composable
internal fun NodeActionBars(
    node: IdeaNode,
    zoom: Float,
    centreX: Float,
    centreY: Float,
    nodeWidthPx: Float,
    nodeHeightPx: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    clipboardHasContent: Boolean,
    tool: NodeTool,
    onAction: (String) -> Unit,
    onTool: (NodeTool) -> Unit,
    onColor: (String?) -> Unit,
    onShape: (String) -> Unit,
    onResizeDone: () -> Unit,
) {
    val density = LocalDensity.current
    // THE SQUARE IS RELATIVE TO ITS NODE (Saeed's report: "when I zoom out they don't get smaller…
    // they are not relative to the node yet"). The bars used to keep their full 100% size at every
    // zoom, so at 25% the square was ~478 dp wide on a 412 dp screen — wider than the screen — and
    // the right-edge clamp pushed it left until the LEFT BAR was gone. Two things fix that:
    //   * the whole square scales with the board, floored at [SQUARE_MIN_SCALE] so the buttons stay
    //     usable at the lowest zoom, and
    //   * the viewport clamp below is a real clamp, so the four bars keep their shape.
    // Below [LABEL_MIN_SCALE] the labels are dropped: at that size they would be unreadable, and an
    // icon-only bar is both cleaner and smaller.
    val square = zoom.coerceIn(SQUARE_MIN_SCALE, 1f)
    val labels = square >= LABEL_MIN_SCALE
    val naturalH = if (labels) ITEM_H else ITEM_H_ICON
    val itemW = with(density) { ITEM_W.toPx() } * square
    val itemH = with(density) { naturalH.toPx() } * square
    val smallW = with(density) { SMALL_W.toPx() } * square
    val barPad = with(density) { BAR_PAD.toPx() } * square
    val gap = with(density) { BAR_GAP.toPx() } * square
    val margin = with(density) { SCREEN_MARGIN.toPx() }

    val top = NodeActionCatalog.byGroup(NodeActionGroup.Content)
    val left = NodeActionCatalog.byGroup(NodeActionGroup.Clipboard)
    val right = NodeActionCatalog.byGroup(NodeActionGroup.Branch)
    val bottom = NodeActionCatalog.byGroup(NodeActionGroup.Style)

    val topW = itemW * top.size + barPad * 2
    val barH = itemH + barPad * 2
    val sideW = itemW + barPad * 2
    val leftH = itemH * left.size + barPad * 2
    val rightH = itemH * right.size + barPad * 2
    val bottomW = when (tool) {
        NodeTool.Colour -> smallW * (NodeStyle.COLORS.size + 2) + barPad * 2
        NodeTool.Shape -> itemW * (NodeStyle.SHAPES.size + 1) + barPad * 2
        NodeTool.Resize -> smallW * 3 + barPad * 2
        NodeTool.None -> itemW * bottom.size + barPad * 2
    }

    val halfW = max(nodeWidthPx / 2f, max(topW, bottomW) / 2f) + gap
    val halfH = max(nodeHeightPx / 2f, max(leftH, rightH) / 2f) + gap

    // Ideal positions, all measured from the node's centre.
    val topX = centreX - topW / 2f
    val topY = centreY - halfH - barH
    val bottomX = centreX - bottomW / 2f
    val bottomY = centreY + halfH
    val leftX = centreX - halfW - sideW
    val leftY = centreY - leftH / 2f
    val rightX = centreX + halfW
    val rightY = centreY - rightH / 2f

    // ONE shift for the whole square — and it is a CLAMP, not two independent pushes. v0.9.0 satisfied
    // the left margin and then broke it again while clamping the right edge, which is exactly how the
    // left bar was lost. When the square cannot fit at all it is centred on the viewport instead, so
    // the four bars keep their shape around the node at any zoom.
    val squareLeft = minOf(leftX, topX, bottomX)
    val squareRight = maxOf(rightX + sideW, topX + topW, bottomX + bottomW)
    val squareTop = minOf(topY, leftY, rightY)
    val squareBottom = maxOf(bottomY + barH, leftY + leftH, rightY + rightH)
    val minDx = margin - squareLeft
    val maxDx = (viewportWidth - margin) - squareRight
    val minDy = margin - squareTop
    val maxDy = (viewportHeight - margin) - squareBottom
    val dx = if (minDx <= maxDx) 0f.coerceIn(minDx, maxDx) else viewportWidth / 2f - (squareLeft + squareRight) / 2f
    val dy = if (minDy <= maxDy) 0f.coerceIn(minDy, maxDy) else viewportHeight / 2f - (squareTop + squareBottom) / 2f
    val topAt = IntOffset((topX + dx).roundToInt(), (topY + dy).roundToInt())
    val leftAt = IntOffset((leftX + dx).roundToInt(), (leftY + dy).roundToInt())
    val rightAt = IntOffset((rightX + dx).roundToInt(), (rightY + dy).roundToInt())
    val bottomAt = IntOffset((bottomX + dx).roundToInt(), (bottomY + dy).roundToInt())

    Box(
        modifier = Modifier.size(
            width = with(density) { viewportWidth.toDp() },
            height = with(density) { viewportHeight.toDp() },
        ),
    ) {
        NodeBar(modifier = Modifier.offset { topAt }.squareScale(square)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                top.forEach { action ->
                    BarButton(
                        action = action,
                        enabled = true,
                        selected = false,
                        labels = labels,
                        naturalH = naturalH,
                        onClick = { onAction(action.key) },
                    )
                }
            }
        }

        NodeBar(modifier = Modifier.offset { leftAt }.squareScale(square)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                left.forEach { action ->
                    BarButton(
                        action = action,
                        // Paste is honest about the empty clipboard instead of failing silently.
                        enabled = action.key != NodeActionCatalog.PASTE || clipboardHasContent,
                        selected = false,
                        labels = labels,
                        naturalH = naturalH,
                        onClick = { onAction(action.key) },
                    )
                }
            }
        }

        NodeBar(modifier = Modifier.offset { rightAt }.squareScale(square)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                right.forEach { action ->
                    BarButton(
                        action = action,
                        enabled = true,
                        selected = false,
                        labels = labels,
                        naturalH = naturalH,
                        onClick = { onAction(action.key) },
                    )
                }
            }
        }

        // The bottom bar: either the three style actions, or the tool they opened.
        NodeBar(modifier = Modifier.offset { bottomAt }.squareScale(square)) {
            when (tool) {
                NodeTool.None -> Row(verticalAlignment = Alignment.CenterVertically) {
                    bottom.forEach { action ->
                        BarButton(
                            action = action,
                            enabled = true,
                            selected = false,
                            labels = labels,
                            naturalH = naturalH,
                            onClick = { onAction(action.key) },
                        )
                    }
                }

                NodeTool.Colour -> Row(verticalAlignment = Alignment.CenterVertically) {
                    SwatchButton(
                        colour = MaterialTheme.colorScheme.surfaceVariant,
                        selected = node.color == null,
                        naturalH = naturalH,
                        onClick = { onColor(null) },
                    )
                    NodeStyle.COLORS.forEach { option ->
                        val role = LinkColor.values().firstOrNull { it.name.equals(option.key, true) }
                        SwatchButton(
                            colour = if (role != null) {
                                themeRoleColor(MaterialTheme.colorScheme, role)
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            selected = node.color == option.key,
                            naturalH = naturalH,
                            onClick = { onColor(option.key) },
                        )
                    }
                    ToolClose(naturalH = naturalH, onClick = { onTool(NodeTool.None) })
                }

                NodeTool.Shape -> Row(verticalAlignment = Alignment.CenterVertically) {
                    NodeStyle.SHAPES.forEach { option ->
                        ShapeButton(
                            shapeKey = option.key,
                            label = option.label,
                            selected = node.shape == option.key,
                            labels = labels,
                            naturalH = naturalH,
                            onClick = { onShape(option.key) },
                        )
                    }
                    ToolClose(naturalH = naturalH, onClick = { onTool(NodeTool.None) })
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
            }
        }
    }
}

/**
 * Shrinks a whole bar with the board so the square stays in proportion to its node.
 *
 * The bar keeps its natural layout size and is scaled around its own top-left corner, which is the
 * corner the geometry above reserved for it — so the rendered bar lands exactly in its slot and the
 * touch area follows the pixels (Compose maps pointer input through the layer, the same way the node
 * cards stay tappable inside the board's own scaled layer).
 */
private fun Modifier.squareScale(scale: Float): Modifier =
    if (scale >= 1f) {
        this
    } else {
        graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }

/** One bar of the square: a rounded tonal surface, no shadow, a 1 dp outline. */
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

/** One action: icon above its label, 62×60 dp — always a full touch target at 100% zoom. */
@Composable
private fun BarButton(
    action: NodeAction,
    enabled: Boolean,
    selected: Boolean,
    labels: Boolean,
    naturalH: Dp,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(ITEM_W, naturalH)
            .clip(RoundedCornerShape(Radius.small))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = actionIcon(action.key),
            contentDescription = stringResource(actionLabelRes(action.key)),
            tint = when {
                !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(22.dp),
        )
        if (labels) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(actionLabelRes(action.key)),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    selected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A colour swatch inside the palette strip. */
@Composable
private fun SwatchButton(
    colour: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    naturalH: Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SMALL_W, naturalH)
            .clip(RoundedCornerShape(Radius.small))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 30.dp else 24.dp)
                .clip(CircleShape)
                .background(colour)
                .then(
                    if (selected) {
                        Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape)
                    } else {
                        Modifier
                    },
                ),
        )
    }
}

/** A shape option inside the shape strip: a small preview of the shape. */
@Composable
private fun ShapeButton(
    shapeKey: String,
    label: String,
    selected: Boolean,
    labels: Boolean,
    naturalH: Dp,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(ITEM_W, naturalH)
            .clip(RoundedCornerShape(Radius.small))
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.xs),
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
        if (labels) {
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
}

/** Leaves a tool and puts the style actions back in the bottom bar. */
@Composable
private fun ToolClose(naturalH: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(SMALL_W, naturalH)
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

/** Item size, in dp. Fixed so the square has a predictable footprint on any screen. */
private val ITEM_W = 62.dp
private val ITEM_H = 60.dp
private val ITEM_H_ICON = 44.dp
private val SMALL_W = 44.dp
private val BAR_PAD = 4.dp
private val BAR_GAP = 10.dp
private val SCREEN_MARGIN = 6.dp

/**
 * How small the square may get. It scales with the board so it stays in proportion to its node, but
 * it stops here: below roughly 45% the buttons would be too small to hit, and the alternative — the
 * square swallowing the node it belongs to — is what Saeed reported.
 */
private const val SQUARE_MIN_SCALE = 0.45f

/** Below this the labels are dropped and the bars become icon-only, which keeps them legible. */
private const val LABEL_MIN_SCALE = 0.7f

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