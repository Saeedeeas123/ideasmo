package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * The bar that appears when more than one node is selected.
 *
 * It is deliberately not the node square: a square around one card makes no sense for a set, so a
 * multi-selection gets a plain bar with the actions that make sense on a group — copy, cut,
 * duplicate, delete, nudge the whole set, and clear. Every destructive action goes through
 * [com.saeed.ideasmo.core.model.UndoStack], so nothing needs a temporary message to be recoverable.
 */
@Composable
internal fun BoardMultiBar(
    count: Int,
    modifier: Modifier = Modifier,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onNudge: (Float, Float) -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.l, end = Spacing.s, top = Spacing.xs, bottom = Spacing.xs)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.multi_selected, count),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(96.dp),
            )

            MultiAction(Icons.Outlined.ContentCopy, stringResource(R.string.act_copy), onCopy)
            MultiAction(Icons.Outlined.ContentCut, stringResource(R.string.act_cut), onCut)
            MultiAction(Icons.Outlined.FileCopy, stringResource(R.string.act_duplicate), onDuplicate)
            MultiAction(Icons.Outlined.DeleteOutline, stringResource(R.string.act_delete), onDelete, error = true)

            Spacer(Modifier.width(Spacing.s))
            Text(
                text = stringResource(R.string.multi_move),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Nudge(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.move_left)) { onNudge(-MULTI_STEP, 0f) }
            Nudge(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.move_right)) { onNudge(MULTI_STEP, 0f) }
            Nudge(Icons.Outlined.KeyboardArrowUp, stringResource(R.string.move_up)) { onNudge(0f, -MULTI_STEP) }
            Nudge(Icons.Outlined.KeyboardArrowDown, stringResource(R.string.move_down)) { onNudge(0f, MULTI_STEP) }

            Spacer(Modifier.width(Spacing.s))
            TextButton(onClick = onClear, modifier = Modifier.heightIn(min = Sizes.touchMin)) {
                Text(stringResource(R.string.multi_clear))
            }
        }
    }
}

@Composable
private fun MultiAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    error: Boolean = false,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.small))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.s, vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (error) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            modifier = Modifier.size(Sizes.iconSm),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (error) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun Nudge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(Sizes.touchMin)) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** How far one nudge moves the whole selection, in board units. */
private const val MULTI_STEP = 40f