package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.core.model.LinkArrow
import com.saeed.ideasmo.core.model.LinkColor
import com.saeed.ideasmo.core.model.LinkLine
import com.saeed.ideasmo.core.model.LinkType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Maps a link's colour role onto the theme. Roles only — never a raw hex. */
@Composable
internal fun linkColor(role: LinkColor): Color = linkColorOf(MaterialTheme.colorScheme, role)

/**
 * Non-composable variant: the board resolves colours once per frame and passes them into
 * the draw lambda, which cannot call a @Composable function.
 */
internal fun linkColorOf(scheme: ColorScheme, role: LinkColor): Color = when (role) {
    LinkColor.Primary -> scheme.primary
    LinkColor.Secondary -> scheme.secondary
    LinkColor.Tertiary -> scheme.tertiary
    LinkColor.Error -> scheme.error
    LinkColor.Outline -> scheme.outline
}

/** Small preview of how a link type is drawn — used in the picker and in Help. */
@Composable
internal fun LinkSwatch(type: LinkType, modifier: Modifier = Modifier) {
    val color = linkColor(type.color)
    Canvas(modifier = modifier.size(width = 44.dp, height = 20.dp)) {
        val y = size.height / 2f
        drawTypedLink(
            start = Offset(3f, y),
            end = Offset(size.width - 5f, y),
            type = type,
            color = color,
            scale = 1f,
        )
    }
}

/**
 * Draws a link according to its type: line style, colour role and arrowheads.
 * Called from the board's viewport layer, so coordinates are already in screen space.
 */
internal fun DrawScope.drawTypedLink(
    start: Offset,
    end: Offset,
    type: LinkType,
    color: Color,
    scale: Float,
) {
    val width = max(1.5f, 2f * scale)

    if (type.line == LinkLine.Double) {
        // Two parallel lines read as "strong/owned" without needing another colour.
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = max(0.001f, kotlin.math.sqrt(dx * dx + dy * dy))
        val nx = -dy / len * (width * 1.6f)
        val ny = dx / len * (width * 1.6f)
        drawLine(color, Offset(start.x + nx, start.y + ny), Offset(end.x + nx, end.y + ny), width * 0.6f, StrokeCap.Round)
        drawLine(color, Offset(start.x - nx, start.y - ny), Offset(end.x - nx, end.y - ny), width * 0.6f, StrokeCap.Round)
    } else {
        val effect = when (type.line) {
            LinkLine.Dashed -> PathEffect.dashPathEffect(floatArrayOf(9f, 7f), 0f)
            LinkLine.Dotted -> PathEffect.dashPathEffect(floatArrayOf(1.5f, 6f), 0f)
            else -> null
        }
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = width,
            cap = StrokeCap.Round,
            pathEffect = effect,
        )
    }

    when (type.arrow) {
        LinkArrow.Single -> drawArrowHead(tip = end, from = start, color = color, width = width)
        LinkArrow.Double -> {
            drawArrowHead(tip = end, from = start, color = color, width = width)
            drawArrowHead(tip = start, from = end, color = color, width = width)
        }
        LinkArrow.None -> Unit
    }
}

private fun DrawScope.drawArrowHead(tip: Offset, from: Offset, color: Color, width: Float) {
    val dx = (tip.x - from.x).toDouble()
    val dy = (tip.y - from.y).toDouble()
    if (dx == 0.0 && dy == 0.0) return
    val angle = atan2(dy, dx)
    val length = max(9f, width * 5f)
    val spread = 0.42
    val p1 = Offset(
        (tip.x - length * cos(angle - spread)).toFloat(),
        (tip.y - length * sin(angle - spread)).toFloat(),
    )
    val p2 = Offset(
        (tip.x - length * cos(angle + spread)).toFloat(),
        (tip.y - length * sin(angle + spread)).toFloat(),
    )
    drawLine(color, tip, p1, strokeWidth = width, cap = StrokeCap.Round)
    drawLine(color, tip, p2, strokeWidth = width, cap = StrokeCap.Round)
}

/**
 * Pulls a line endpoint out to the edge of a node card, so the line never runs underneath a card
 * and arrowheads stay visible. [halfW] / [halfH] are the card's half extents in screen pixels.
 */
internal fun trimToNodeEdge(from: Offset, to: Offset, halfW: Float, halfH: Float): Offset {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val adx = abs(dx)
    val ady = abs(dy)
    val tx = if (adx > 0.001f) halfW / adx else Float.MAX_VALUE
    val ty = if (ady > 0.001f) halfH / ady else Float.MAX_VALUE
    val t = min(1f, min(tx, ty))
    return Offset(to.x - dx * t, to.y - dy * t)
}