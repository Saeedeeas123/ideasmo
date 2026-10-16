package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.saeed.ideasmo.core.model.IdeaNode
import com.saeed.ideasmo.core.model.StructureLayout
import kotlin.math.roundToInt

/**
 * Free-form resize: eight handles on the card's own screen rectangle.
 *
 * Saeed's order: *"when you press Resize, the square itself must give you the tool so you can change
 * the node free-form — its height, its width, or both."* So the handles appear **on the card** while
 * the Resize tool is open, and the bottom bar shows the live size.
 *
 * Corner handles change both dimensions; edge handles change one. Dragging a left or top handle moves
 * the node as well, so the opposite edge stays exactly where it was — the behaviour a resize is
 * expected to have. The size is written in **board units**, so it means the same thing at every zoom,
 * and the store is only written on drag end (never per frame).
 */
@Composable
internal fun NodeResizeOverlay(
    node: IdeaNode,
    centreX: Float,
    centreY: Float,
    widthPx: Float,
    heightPx: Float,
    scale: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    onPreview: (w: Float, h: Float, x: Float, y: Float) -> Unit,
    onCommit: () -> Unit,
) {
    val density = LocalDensity.current
    // The handles shrink with the card and are capped by it (Saeed's report: at 25% zoom eight 30 dp
    // circles collapsed into one blob lying beside a small card instead of sitting on its corners).
    // They also stop shrinking at a size a finger can still hold, and each one keeps an invisible
    // touch margin so the visible circle can be the small one he asked for.
    val basePx = with(density) { HANDLE.toPx() }
    val floorPx = with(density) { HANDLE_MIN.toPx() }
    val padPx = with(density) { HANDLE_TOUCH_PAD.toPx() }
    val handlePx = (basePx * scale)
        .coerceIn(floorPx, basePx)
        .coerceAtMost(minOf(widthPx, heightPx) / 3f)
    // A handle may never claim more than half the card, so on a tiny card the eight touch boxes
    // cannot pile up on top of each other and steal each other's drags.
    val touchPx = (handlePx + padPx * 2f).coerceAtMost(minOf(widthPx, heightPx) / 2f)
    val strokePx = (2f * scale).coerceIn(1.5f, 2f)
    val left = centreX - widthPx / 2f
    val top = centreY - heightPx / 2f
    val right = centreX + widthPx / 2f
    val bottom = centreY + heightPx / 2f
    val dash = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primary

    // Live drag state: the size and position the gesture started from, plus the movement so far.
    var dragFrom by remember(node.id) { mutableStateOf<FloatArray?>(null) }

    Box(
        modifier = Modifier.size(
            width = with(density) { viewportWidth.toDp() },
            height = with(density) { viewportHeight.toDp() },
        ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = dash,
                topLeft = Offset(left, top),
                size = Size(widthPx.coerceAtLeast(1f), heightPx.coerceAtLeast(1f)),
                style = Stroke(
                    width = strokePx,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f * scale, 8f * scale), 0f),
                ),
            )
        }

        Handle(
            x = left, y = top, size = handlePx, touch = touchPx, colour = fill,
            onStart = { dragFrom = floatArrayOf(node.width, node.height, node.x, node.y) },
            onDrag = { dxPx, dyPx ->
                val from = dragFrom ?: return@Handle
                val dx = dxPx / scale
                val dy = dyPx / scale
                val newW = (from[0] - dx).coerceIn(StructureLayout.MIN_NODE_W, StructureLayout.MAX_NODE_W)
                val newH = (from[1] - dy).coerceIn(StructureLayout.MIN_NODE_H, StructureLayout.MAX_NODE_H)
                onPreview(newW, newH, from[2] + (from[0] - newW), from[3] + (from[1] - newH))
            },
            onEnd = { dragFrom = null; onCommit() },
        )
        Handle(
            x = right, y = top, size = handlePx, touch = touchPx, colour = fill,
            onStart = { dragFrom = floatArrayOf(node.width, node.height, node.x, node.y) },
            onDrag = { dxPx, dyPx ->
                val from = dragFrom ?: return@Handle
                val dx = dxPx / scale
                val dy = dyPx / scale
                val newW = (from[0] + dx).coerceIn(StructureLayout.MIN_NODE_W, StructureLayout.MAX_NODE_W)
                val newH = (from[1] - dy).coerceIn(StructureLayout.MIN_NODE_H, StructureLayout.MAX_NODE_H)
                onPreview(newW, newH, from[2], from[3] + (from[1] - newH))
            },
            onEnd = { dragFrom = null; onCommit() },
        )
        Handle(
            x = left, y = bottom, size = handlePx, touch = touchPx, colour = fill,
            onStart = { dragFrom = floatArrayOf(node.width, node.height, node.x, node.y) },
            onDrag = { dxPx, dyPx ->
                val from = dragFrom ?: return@Handle
                val dx = dxPx / scale
                val dy = dyPx / scale
                val newW = (from[0] - dx).coerceIn(StructureLayout.MIN_NODE_W, StructureLayout.MAX_NODE_W)
                val newH = (from[1] + dy).coerceIn(StructureLayout.MIN_NODE_H, StructureLayout.MAX_NODE_H)
                onPreview(newW, newH, from[2] + (from[0] - newW), from[3])
            },
            onEnd = { dragFrom = null; onCommit() },
        )
        Handle(
            x = right, y = bottom, size = handlePx, touch = touchPx, colour = fill,
            onStart = { dragFrom = floatArrayOf(node.width, node.height, node.x, node.y) },
            onDrag = { dxPx, dyPx ->
                val from = dragFrom ?: return@Handle
                val dx = dxPx / scale
                val dy = dyPx / scale
                val newW = (from[0] + dx).coerceIn(StructureLayout.MIN_NODE_W, StructureLayout.MAX_NODE_W)
                val newH = (from[1] + dy).coerceIn(StructureLayout.MIN_NODE_H, StructureLayout.MAX_NODE_H)
                onPreview(newW, newH, from[2], from[3])
            },
            onEnd = { dragFrom = null; onCommit() },
        )
        Handle(
            x = centreX, y = top, size = handlePx, touch = touchPx, colour = fill,
            onStart = { dragFrom = floatArrayOf(node.width, node.height, node.x, node.y) },
            onDrag = { _, dyPx ->
                val from = dragFrom ?: return@Handle
                val dy = dyPx / scale
                val newH = (from[1] - dy).coerceIn(StructureLayout.MIN_NODE_H, StructureLayout.MAX_NODE_H)
                onPreview(from[0], newH, from[2], from[3] + (from[1] - newH))
            },
            onEnd = { dragFrom = null; onCommit() },
        )
        Handle(
            x = centreX, y = bottom, size = handlePx, touch = touchPx, colour = fill,
            onStart = { dragFrom = floatArrayOf(node.width, node.height, node.x, node.y) },
            onDrag = { _, dyPx ->
                val from = dragFrom ?: return@Handle
                val dy = dyPx / scale
                val newH = (from[1] + dy).coerceIn(StructureLayout.MIN_NODE_H, StructureLayout.MAX_NODE_H)
                onPreview(from[0], newH, from[2], from[3])
            },
            onEnd = { dragFrom = null; onCommit() },
        )
        Handle(
            x = left, y = centreY, size = handlePx, touch = touchPx, colour = fill,
            onStart = { dragFrom = floatArrayOf(node.width, node.height, node.x, node.y) },
            onDrag = { dxPx, _ ->
                val from = dragFrom ?: return@Handle
                val dx = dxPx / scale
                val newW = (from[0] - dx).coerceIn(StructureLayout.MIN_NODE_W, StructureLayout.MAX_NODE_W)
                onPreview(newW, from[1], from[2] + (from[0] - newW), from[3])
            },
            onEnd = { dragFrom = null; onCommit() },
        )
        Handle(
            x = right, y = centreY, size = handlePx, touch = touchPx, colour = fill,
            onStart = { dragFrom = floatArrayOf(node.width, node.height, node.x, node.y) },
            onDrag = { dxPx, _ ->
                val from = dragFrom ?: return@Handle
                val dx = dxPx / scale
                val newW = (from[0] + dx).coerceIn(StructureLayout.MIN_NODE_W, StructureLayout.MAX_NODE_W)
                onPreview(newW, from[1], from[2], from[3])
            },
            onEnd = { dragFrom = null; onCommit() },
        )
    }
}

/**
 * One drag handle. The visible circle is [size]; the box that receives the touch is [touch], so the
 * circle can be the small one Saeed asked for while the finger still has something to hold on to.
 */
@Composable
private fun Handle(
    x: Float,
    y: Float,
    size: Float,
    touch: Float,
    colour: androidx.compose.ui.graphics.Color,
    onStart: () -> Unit,
    onDrag: (dxPx: Float, dyPx: Float) -> Unit,
    onEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val half = touch / 2f
    val ringPx = (size * 0.09f).coerceIn(1.5f, 4f)
    Box(
        modifier = Modifier
            .offset { IntOffset((x - half).roundToInt(), (y - half).roundToInt()) }
            .size(with(density) { touch.toDp() })
            .pointerInput(Unit) {
                var totalX = 0f
                var totalY = 0f
                detectDragGestures(
                    onDragStart = {
                        totalX = 0f
                        totalY = 0f
                        onStart()
                    },
                    onDragEnd = { onEnd() },
                    onDragCancel = { onEnd() },
                ) { change, drag ->
                    change.consume()
                    totalX += drag.x
                    totalY += drag.y
                    onDrag(totalX, totalY)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(with(density) { size.toDp() })
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = with(density) { ringPx.toDp() },
                    color = colour,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(with(density) { (size / 3f).toDp() })
                    .clip(CircleShape)
                    .background(colour),
            )
        }
    }
}

/** Handle diameter at 100% zoom, in dp — smaller than v0.9.0's 30 dp, as Saeed asked. */
private val HANDLE = 22.dp

/** The smallest a handle may shrink to, in dp, before it is too small to grab. */
private val HANDLE_MIN = 12.dp

/** Invisible margin around a handle, in dp, so the visible circle can stay small. */
private val HANDLE_TOUCH_PAD = 10.dp