package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * What Copy / Cut / Duplicate ask before they act.
 *
 * Saeed's order: *"when the user clicks Copy, Cut or Duplicate, an option must appear that says
 * whether to do it with just this node or with its links — and if it has sub-links, with the
 * sub-links — toggled with a tick."*
 *
 * The two ticks are **its links** (the direct children and the links that join them) and **its
 * sub-links** (the whole branch below it). Sub-links imply links, so ticking the second one ticks and
 * locks the first: the dialog never offers a combination that cannot exist.
 */
@Composable
internal fun FragmentOptionsDialog(
    actionLabel: String,
    nodeName: String,
    childCount: Int,
    deeperCount: Int,
    onConfirm: (withLinks: Boolean, withSubLinks: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var withLinks by remember { mutableStateOf(false) }
    var withSubLinks by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(actionLabel) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = nodeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.m))

                TickRow(
                    checked = withLinks || withSubLinks,
                    enabled = childCount > 0 && !withSubLinks,
                    label = stringResource(R.string.fragment_with_links, childCount),
                    detail = stringResource(R.string.fragment_with_links_detail),
                    onCheckedChange = { withLinks = it },
                )

                TickRow(
                    checked = withSubLinks,
                    enabled = deeperCount > 0,
                    label = stringResource(R.string.fragment_with_sublinks, deeperCount),
                    detail = stringResource(R.string.fragment_with_sublinks_detail),
                    onCheckedChange = {
                        withSubLinks = it
                        if (it) withLinks = true
                    },
                )

                if (childCount == 0) {
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = stringResource(R.string.fragment_no_links),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(withLinks || withSubLinks, withSubLinks) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(actionLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun TickRow(
    checked: Boolean,
    enabled: Boolean,
    label: String,
    detail: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
        )
        Column(modifier = Modifier.padding(start = Spacing.s)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}