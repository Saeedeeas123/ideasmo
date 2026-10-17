package com.saeed.ideasmo.ui.board

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Spacing

/** Renaming a node — the Text action. The field is focused on open and an empty name is refused. */
@Composable
internal fun NodeTextDialog(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.node_text_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
                singleLine = true,
                shape = RoundedCornerShape(Radius.medium),
                label = { Text(stringResource(R.string.node_name)) },
                supportingText = { Text(stringResource(R.string.node_name_hint)) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(value.trim()) },
                enabled = value.isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/** The node's notes — the free text behind the card preview. */
@Composable
internal fun NodeNotesDialog(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.node_notes_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .focusRequester(focus),
                minLines = 5,
                shape = RoundedCornerShape(Radius.medium),
                label = { Text(stringResource(R.string.description)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.trim()) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * What the Image action asks when the node already has a picture: replace it or take it away.
 * A node with a picture never silently overwrites it.
 */
@Composable
internal fun NodePictureDialog(
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.picture_title)) },
        text = { Text(stringResource(R.string.picture_question)) },
        confirmButton = {
            TextButton(onClick = onReplace, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.picture_replace))
            }
        },
        dismissButton = {
            TextButton(onClick = onRemove, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(
                    text = stringResource(R.string.picture_remove),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

/**
 * The machine-readable view of a node: every field, every link, each with its id.
 * The text is selectable-looking monospace and can be copied out in one tap — this is the shape the
 * language-model layer will read.
 */
@Composable
internal fun NodeDetailsDialog(
    title: String,
    body: String,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.s))
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.details_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.link_close))
            }
        },
    )
}