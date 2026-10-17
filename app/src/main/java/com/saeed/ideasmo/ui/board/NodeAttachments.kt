package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.model.IdeaNode
import com.saeed.ideasmo.core.util.decodeThumbnail
import com.saeed.ideasmo.core.util.storedFileName
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing

/**
 * The node's **notes** — a list, with no limit.
 *
 * Saeed's order (v0.12.0): a node may carry any number of notes, and the Note action must show how
 * many there are; opening it has to give access to the ones that are already there and to creating
 * one or several new ones. The card itself no longer shows note text — only the count.
 */
@Composable
internal fun NodeNotesDialog(
    node: IdeaNode,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit,
    onSaveToDrive: (index: Int, text: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(node.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.att_notes_title, node.notes.size)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (node.notes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.att_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = Spacing.s),
                    )
                }
                node.notes.forEachIndexed { index, note ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = Spacing.s),
                        )
                        TextButton(
                            onClick = { onSaveToDrive(index, note) },
                            modifier = Modifier.heightIn(min = Sizes.touchMin),
                        ) {
                            Text(stringResource(R.string.note_to_drive))
                        }
                        IconButton(
                            onClick = { onRemove(index) },
                            modifier = Modifier.size(Sizes.touchMin),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.att_remove),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
                Spacer(Modifier.height(Spacing.m))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(Radius.medium),
                    label = { Text(stringResource(R.string.att_new_note)) },
                )
                Spacer(Modifier.height(Spacing.s))
                TextButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onAdd(draft)
                            draft = ""
                        }
                    },
                    modifier = Modifier.heightIn(min = Sizes.touchMin),
                ) {
                    Text(stringResource(R.string.att_add_note))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.link_close)) }
        },
    )
}

/** The node's **pictures** — a list, with no limit. Shows the ones already there and adds new ones. */
@Composable
internal fun NodeImagesDialog(
    node: IdeaNode,
    onAdd: () -> Unit,
    onFromDrive: () -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.att_images_title, node.pictures.size)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (node.pictures.isEmpty()) {
                    Text(
                        text = stringResource(R.string.att_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = Spacing.s),
                    )
                }
                node.pictures.forEachIndexed { index, path ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val bitmap = remember(path) { decodeThumbnail(context, path, 320) }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 96.dp, height = 72.dp)
                                    .padding(vertical = Spacing.xs),
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.att_image_broken),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.width(Spacing.s))
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { onRemove(index) },
                            modifier = Modifier.size(Sizes.touchMin),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.att_remove),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
                Spacer(Modifier.height(Spacing.s))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    TextButton(
                        onClick = onAdd,
                        modifier = Modifier.heightIn(min = Sizes.touchMin),
                    ) {
                        Text(stringResource(R.string.att_add_image))
                    }
                    TextButton(
                        onClick = onFromDrive,
                        modifier = Modifier.heightIn(min = Sizes.touchMin),
                    ) {
                        Text(stringResource(R.string.att_from_drive))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.link_close)) }
        },
    )
}

/** The node's **files** — a list, with no limit, added through the system document picker. */
@Composable
internal fun NodeFilesDialog(
    node: IdeaNode,
    onAdd: () -> Unit,
    onFromDrive: () -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.att_files_title, node.files.size)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (node.files.isEmpty()) {
                    Text(
                        text = stringResource(R.string.att_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = Spacing.s),
                    )
                }
                node.files.forEachIndexed { index, path ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Sizes.iconSm),
                        )
                        Spacer(Modifier.width(Spacing.s))
                        Text(
                            text = storedFileName(path),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = Spacing.s),
                        )
                        IconButton(
                            onClick = { onRemove(index) },
                            modifier = Modifier.size(Sizes.touchMin),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.att_remove),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
                Spacer(Modifier.height(Spacing.s))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    TextButton(
                        onClick = onAdd,
                        modifier = Modifier.heightIn(min = Sizes.touchMin),
                    ) {
                        Text(stringResource(R.string.att_add_file))
                    }
                    TextButton(
                        onClick = onFromDrive,
                        modifier = Modifier.heightIn(min = Sizes.touchMin),
                    ) {
                        Text(stringResource(R.string.att_from_drive))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.link_close)) }
        },
    )
}