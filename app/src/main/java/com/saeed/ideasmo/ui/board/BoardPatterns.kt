package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.core.model.BoardSpace
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The eight board surfaces. The set is grounded in what infinite-canvas tools expose as their
 * background options — a plain sheet, a square grid, a dot grid, fine graph paper and ruled
 * notebook lines are the common five (Miro, FigJam, Apple Freeform and Obsidian Canvas all offer
 * a subset of these), and isometric / hex / blueprint are the classic engineering-paper patterns
 * that make a board read as a technical drawing rather than a note pad.
 *
 * Every pattern is drawn in WORLD space and transformed to the screen, so it pans and zooms with
 * the board instead of sliding under it.
 */
enum class BoardPattern(val key: String, val label: String, val detail: String) {
    Plain("plain", "Plain", "No pattern — a clean sheet"),
    Grid("grid", "Grid", "Square grid, a heavier line every five"),
    Dots("dots", "Dots", "A dot at every step, heavier every five"),
    Graph("graph", "Graph paper", "Fine squares with a bold line every four"),
    Lines("lines", "Ruled lines", "Notebook lines, heavier every five"),
    Isometric("isometric", "Isometric", "30° triangles — engineering paper"),
    Hex("hex", "Hex grid", "A honeycomb lattice"),
    Blueprint("blueprint", "Blueprint", "Drafting sheet with crosshair ticks"),
    ;

    companion object {
        fun fromKey(key: String?): BoardPattern =
            values().firstOrNull { it.key == key } ?: Grid
    }
}

/**
 * The board's graphic theme. `Default` follows the app's Material theme (so it respects day/night
 * and dynamic colour); the others are fixed workspaces — a paper desk, a warm desk, a mint desk,
 * charcoal and blueprint — chosen because different kinds of work want a different surface.
 */
enum class BoardPalette(val key: String, val label: String, val detail: String) {
    Default("default", "App theme", "Follows day / night and your wallpaper colour"),
    Paper("paper", "Paper", "Warm cream, like a sketch book"),
    Warm("warm", "Warm", "Soft amber desk"),
    Mint("mint", "Mint", "Pale green, the app's own seed colour"),
    Charcoal("charcoal", "Charcoal", "Dark grey desk"),
    Blueprint("blueprint", "Blueprint", "Drafting blue with light lines"),
    ;

    companion object {
        fun fromKey(key: String?): BoardPalette =
            values().firstOrNull { it.key == key } ?: Default
    }
}

/**
 * The resolved colours of a board surface.
 * [background] is null for the theme-following palette, so the app's own surface shows through.
 * These are theme-level colours (like `ui/theme/Color.kt`) — screens still never use a raw hex.
 */
data class BoardColors(
    val background: Color?,
    val minor: Color,
    val major: Color,
    val accent: Color,
)

@Composable
internal fun boardColorsOf(palette: BoardPalette): BoardColors {
    val scheme = MaterialTheme.colorScheme
    return when (palette) {
        BoardPalette.Default -> BoardColors(
            background = null,
            minor = scheme.outline.copy(alpha = 0.20f),
            major = scheme.outline.copy(alpha = 0.45f),
            accent = scheme.primary,
        )

        BoardPalette.Paper -> BoardColors(
            background = Color(0xFFF4EFE4),
            minor = Color(0xFFD9CFBB),
            major = Color(0xFFB9AA8E),
            accent = Color(0xFF8A6B3F),
        )

        BoardPalette.Warm -> BoardColors(
            background = Color(0xFFFDF3E7),
            minor = Color(0xFFF0DCC4),
            major = Color(0xFFD9B98F),
            accent = Color(0xFFB4611F),
        )

        BoardPalette.Mint -> BoardColors(
            background = Color(0xFFEDF6F1),
            minor = Color(0xFFCFE4D9),
            major = Color(0xFFA7CBBA),
            accent = Color(0xFF2E5E4E),
        )

        BoardPalette.Charcoal -> BoardColors(
            background = Color(0xFF1C1C1E),
            minor = Color(0xFF2E2E31),
            major = Color(0xFF454549),
            accent = Color(0xFFA8A8AD),
        )

        BoardPalette.Blueprint -> BoardColors(
            background = Color(0xFF0F2440),
            minor = Color(0xFF1E4A78),
            major = Color(0xFF3C7FB8),
            accent = Color(0xFF7FD3FF),
        )
    }
}

/** A hex cell costs six lines; above this many cells the pattern is skipped rather than stuttered. */
private const val MAX_HEX_CELLS = 700

/** Draws the chosen surface: the palette fill first, then the pattern. */
internal fun DrawScope.drawBoardBackground(
    pattern: BoardPattern,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    colors: BoardColors,
) {
    colors.background?.let { drawRect(color = it) }
    if (scale <= 0f) return
    when (pattern) {
        BoardPattern.Plain -> Unit
        BoardPattern.Grid -> drawGrid(scale, offsetX, offsetY, colors, 100f, 500f, true)
        BoardPattern.Graph -> drawGrid(scale, offsetX, offsetY, colors, 25f, 100f, true)
        BoardPattern.Blueprint -> {
            drawGrid(scale, offsetX, offsetY, colors, 25f, 100f, true)
            drawCrossTicks(scale, offsetX, offsetY, colors, 100f)
        }

        BoardPattern.Dots -> drawDots(scale, offsetX, offsetY, colors, 50f, 250f)
        BoardPattern.Lines -> drawRuledLines(scale, offsetY, colors, 60f, 300f)
        BoardPattern.Isometric -> drawIsometric(scale, offsetX, offsetY, colors, 60f)
        BoardPattern.Hex -> drawHexGrid(scale, offsetX, offsetY, colors, 60f)
    }
}

private fun isMultipleOf(value: Float, step: Float): Boolean {
    if (step <= 0f) return false
    val ratio = value / step
    return abs(ratio - round(ratio)) < 0.01f
}

private fun DrawScope.drawGrid(
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    colors: BoardColors,
    minorStep: Float,
    majorStep: Float,
    vertical: Boolean,
) {
    val minorPx = minorStep * scale
    val majorPx = majorStep * scale
    val drawMinor = minorPx >= 6f
    if (!drawMinor && majorPx < 6f) return

    val left = (0f - offsetX) / scale
    val right = (size.width - offsetX) / scale
    val top = (0f - offsetY) / scale
    val bottom = (size.height - offsetY) / scale

    if (vertical) {
        if (drawMinor) {
            var x = floor(left / minorStep) * minorStep
            while (x <= right) {
                if (!isMultipleOf(x, majorStep)) {
                    val sx = x * scale + offsetX
                    drawLine(colors.minor, Offset(sx, 0f), Offset(sx, size.height), strokeWidth = 1f)
                }
                x += minorStep
            }
        }
        var mx = floor(left / majorStep) * majorStep
        while (mx <= right) {
            val sx = mx * scale + offsetX
            drawLine(colors.major, Offset(sx, 0f), Offset(sx, size.height), strokeWidth = 1.5f)
            mx += majorStep
        }
    } else {
        if (drawMinor) {
            var y = floor(top / minorStep) * minorStep
            while (y <= bottom) {
                if (!isMultipleOf(y, majorStep)) {
                    val sy = y * scale + offsetY
                    drawLine(colors.minor, Offset(0f, sy), Offset(size.width, sy), strokeWidth = 1f)
                }
                y += minorStep
            }
        }
        var my = floor(top / majorStep) * majorStep
        while (my <= bottom) {
            val sy = my * scale + offsetY
            drawLine(colors.major, Offset(0f, sy), Offset(size.width, sy), strokeWidth = 1.5f)
            my += majorStep
        }
    }
}

private fun DrawScope.drawDots(
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    colors: BoardColors,
    step: Float,
    majorStep: Float,
) {
    val stepPx = step * scale
    if (stepPx < 8f) return
    val radius = max(1.2f, 1.5f * scale)
    val left = (0f - offsetX) / scale
    val right = (size.width - offsetX) / scale
    val top = (0f - offsetY) / scale
    val bottom = (size.height - offsetY) / scale

    var x = floor(left / step) * step
    while (x <= right) {
        var y = floor(top / step) * step
        val majorColumn = isMultipleOf(x, majorStep)
        while (y <= bottom) {
            val major = majorColumn && isMultipleOf(y, majorStep)
            drawCircle(
                color = if (major) colors.major else colors.minor,
                radius = if (major) radius * 1.7f else radius,
                center = Offset(x * scale + offsetX, y * scale + offsetY),
            )
            y += step
        }
        x += step
    }
}

private fun DrawScope.drawRuledLines(
    scale: Float,
    offsetY: Float,
    colors: BoardColors,
    step: Float,
    majorStep: Float,
) {
    val stepPx = step * scale
    if (stepPx < 7f) return
    val top = (0f - offsetY) / scale
    val bottom = (size.height - offsetY) / scale
    var y = floor(top / step) * step
    while (y <= bottom) {
        val sy = y * scale + offsetY
        val major = isMultipleOf(y, majorStep)
        drawLine(
            color = if (major) colors.major else colors.minor,
            start = Offset(0f, sy),
            end = Offset(size.width, sy),
            strokeWidth = if (major) 1.5f else 1f,
        )
        y += step
    }
}

/**
 * Two families of parallel lines at ±30° — the classic isometric sheet. The lines are computed in
 * world space and transformed, so the sheet travels with the board.
 */
private fun DrawScope.drawIsometric(
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    colors: BoardColors,
    worldStep: Float,
) {
    if (worldStep * scale < 10f) return
    val w = size.width
    val h = size.height
    val worldCorners = listOf(
        Offset((0f - offsetX) / scale, (0f - offsetY) / scale),
        Offset((w - offsetX) / scale, (0f - offsetY) / scale),
        Offset((0f - offsetX) / scale, (h - offsetY) / scale),
        Offset((w - offsetX) / scale, (h - offsetY) / scale),
    )

    listOf(30.0, -30.0).forEach { degrees ->
        val radians = Math.toRadians(degrees)
        val dx = cos(radians).toFloat()
        val dy = sin(radians).toFloat()
        val nx = -dy
        val ny = dx
        val minC = worldCorners.minOf { it.x * nx + it.y * ny }
        val maxC = worldCorners.maxOf { it.x * nx + it.y * ny }
        val minT = worldCorners.minOf { it.x * dx + it.y * dy }
        val maxT = worldCorners.maxOf { it.x * dx + it.y * dy }

        var c = floor(minC / worldStep) * worldStep
        while (c <= maxC) {
            val start = Offset(nx * c + dx * minT, ny * c + dy * minT)
            val end = Offset(nx * c + dx * maxT, ny * c + dy * maxT)
            drawLine(
                color = if (isMultipleOf(c, worldStep * 5f)) colors.major else colors.minor,
                start = Offset(start.x * scale + offsetX, start.y * scale + offsetY),
                end = Offset(end.x * scale + offsetX, end.y * scale + offsetY),
                strokeWidth = 1f,
            )
            c += worldStep
        }
    }
}

/** Pointy-top hexagons on the standard axial lattice. */
private fun DrawScope.drawHexGrid(
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    colors: BoardColors,
    radiusWorld: Float,
) {
    val radiusPx = radiusWorld * scale
    if (radiusPx < 14f) return
    val colStep = sqrt(3f) * radiusWorld
    val rowStep = 1.5f * radiusWorld
    val left = (0f - offsetX) / scale
    val right = (size.width - offsetX) / scale
    val top = (0f - offsetY) / scale
    val bottom = (size.height - offsetY) / scale

    val firstCol = floor(left / colStep).toInt() - 1
    val lastCol = ceil(right / colStep).toInt() + 1
    val firstRow = floor(top / rowStep).toInt() - 1
    val lastRow = ceil(bottom / rowStep).toInt() + 1
    val cells = (lastCol - firstCol + 1).toLong() * (lastRow - firstRow + 1).toLong()
    if (cells > MAX_HEX_CELLS) return

    for (row in firstRow..lastRow) {
        for (col in firstCol..lastCol) {
            val cx = colStep * (col + (row and 1) * 0.5f)
            val cy = rowStep * row
            val scx = cx * scale + offsetX
            val scy = cy * scale + offsetY
            val path = Path()
            for (corner in 0 until 6) {
                val angle = Math.toRadians(60.0 * corner - 90.0)
                val px = scx + cos(angle).toFloat() * radiusPx
                val py = scy + sin(angle).toFloat() * radiusPx
                if (corner == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path, colors.minor, style = Stroke(width = 1f))
        }
    }
}

/** Small crosshairs at the major intersections — what makes a blueprint read as a drafting sheet. */
private fun DrawScope.drawCrossTicks(
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    colors: BoardColors,
    step: Float,
) {
    val stepPx = step * scale
    if (stepPx < 24f) return
    val arm = max(3f, 5f * scale)
    val left = (0f - offsetX) / scale
    val right = (size.width - offsetX) / scale
    val top = (0f - offsetY) / scale
    val bottom = (size.height - offsetY) / scale

    var x = floor(left / step) * step
    while (x <= right) {
        var y = floor(top / step) * step
        while (y <= bottom) {
            val sx = x * scale + offsetX
            val sy = y * scale + offsetY
            drawLine(colors.accent, Offset(sx - arm, sy), Offset(sx + arm, sy), strokeWidth = 1f)
            drawLine(colors.accent, Offset(sx, sy - arm), Offset(sx, sy + arm), strokeWidth = 1f)
            y += step
        }
        x += step
    }
}

/**
 * A live miniature of a pattern, used by the project settings so a mode is chosen by looking at it
 * rather than by reading its name. The transform is fixed (35% zoom, origin at the centre) so the
 * preview is stable and cheap.
 */
@Composable
internal fun BoardPatternPreview(
    pattern: BoardPattern,
    colors: BoardColors,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = 64.dp, height = 44.dp)) {
        drawBoardBackground(
            pattern = pattern,
            scale = 0.35f,
            offsetX = size.width / 2f,
            offsetY = size.height / 2f,
            colors = colors,
        )
    }
}