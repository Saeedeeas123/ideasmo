package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * **The board sheet** (v0.17.0): the one container every panel on the board opens in.
 *
 * Saeed's order: *"when Connect is tapped something like More must come up — a rectangle with the board
 * still visible behind it. Look at exactly how More works; this must work exactly the same way. And the
 * thing you are going to build: the board behind it must be movable. I know I said before that it should
 * be static, but I have changed my mind — the rectangle comes with a handle that can make it smaller and
 * larger lengthwise, and behind it the board can be moved."*
 *
 * So this is that rectangle, and it is the same one for the node sheet (More), the relation picker
 * (Connect) and the link inspector:
 *
 * - **No scrim.** The sheet is a child of the board's own box with nothing behind it to swallow
 *   gestures, so panning, zooming and tapping the board still work while a panel is open. That is what
 *   "the board behind it must be movable" means in practice.
 * - **A resize handle.** Dragging the pill at the top changes the height between [MIN_FRACTION] and
 *   [MAX_FRACTION] of the viewport, so the user decides how much of the board stays visible.
 * - **A pinned header.** The title row never scrolls away, so the panel can always be closed and always
 *   says what it is.
 */
internal const val SHEET_MIN_FRACTION = 0.30f
internal const val SHEET_MAX_FRACTION = 0.92f
internal const val SHEET_DEFAULT_FRACTION = 0.70f

@Composable
internal fun BoardSheet(
    title: String,
    subtitle: String?,
    fraction: Float,
    viewportHeightPx: Float,
    onFraction: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Pinned above the scrolling content — the node's own card in the node sheet, for example. */
    header: (@Composable () -> Unit)? = null,
    /** The panel's own content; it is a ColumnScope, so a panel can give its list `weight(1f)`. */
    content: @Composable ColumnScope.() -> Unit,
) {
    val heightDp = with(androidx.compose.ui.platform.LocalDensity.current) {
        (viewportHeightPx * fraction).toDp()
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = Radius.large, topEnd = Radius.large),
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // The handle: drag it up for more room, down to see more board.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .pointerInput(viewportHeightPx) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (viewportHeightPx > 0f) {
                                onFraction(fraction - dragAmount / viewportHeightPx)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.l, end = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(Sizes.touchMin)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.link_close),
                    )
                }
            }
            header?.let {
                it()
                Spacer(Modifier.height(Spacing.s))
            }
            content()
        }
    }
}

/** Keeps a requested sheet height inside the range the sheet allows. */
internal fun clampSheetFraction(value: Float): Float =
    value.coerceIn(SHEET_MIN_FRACTION, SHEET_MAX_FRACTION)