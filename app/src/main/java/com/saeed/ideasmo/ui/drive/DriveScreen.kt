package com.saeed.ideasmo.ui.drive

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.outlined.Delete
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.drive.DriveEntry
import com.saeed.ideasmo.core.drive.DriveKind
import com.saeed.ideasmo.core.drive.DriveStore
import com.saeed.ideasmo.core.log.AppLog
import com.saeed.ideasmo.core.util.decodeThumbnail
import com.saeed.ideasmo.ui.components.EmptyState
import com.saeed.ideasmo.ui.components.SectionTitle
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing
import java.io.File

/**
 * **The Drive: the app's own light file manager.**
 *
 * Saeed's order (v0.13.0): a personal drive where every file lives, reachable from the whole app, with
 * a simple, light file manager that can open the files it holds — and directly connected to what a node
 * carries. This screen is deliberately plain: one column of rows, a breadcrumb, two actions
 * (import a file, make a folder) and a viewer.
 *
 * Files are shown with their kind icon and their size; a folder opens in place; an image or a text file
 * opens in a viewer; anything else says honestly that it has no in-app viewer yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // Where we are, as a Drive-relative path ("" = the root).
    var here by remember { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }
    var viewing by remember { mutableStateOf<DriveEntry?>(null) }
    var deleting by remember { mutableStateOf<DriveEntry?>(null) }
    var newFolder by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    val entries = remember(here, refresh) { DriveStore.list(here) }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            AppLog.i("Drive", "import cancelled")
        } else {
            val path = DriveStore.importUri(context, uri, here)
            notice = if (path == null) {
                context.getString(R.string.drive_import_failed)
            } else {
                context.getString(R.string.drive_imported, File(path).name)
            }
            refresh++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (here.isEmpty()) {
                                stringResource(R.string.drive_title)
                            } else {
                                here.substringAfterLast('/')
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(
                                R.string.drive_usage,
                                DriveStore.formatSize(DriveStore.usedBytes()),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (here.isEmpty()) {
                                onBack()
                            } else {
                                here = DriveStore.parentOf(here)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.s, vertical = Spacing.s),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(
                        onClick = { importFile.launch(arrayOf("*/*")) },
                        modifier = Modifier.heightIn(min = Sizes.touchMin),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(Sizes.iconSm))
                        Spacer(Modifier.width(Spacing.s))
                        Text(stringResource(R.string.drive_import))
                    }
                    FilledTonalButton(
                        onClick = { newFolder = true },
                        modifier = Modifier.heightIn(min = Sizes.touchMin),
                    ) {
                        Icon(
                            Icons.Outlined.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(Sizes.iconSm),
                        )
                        Spacer(Modifier.width(Spacing.s))
                        Text(stringResource(R.string.drive_new_folder))
                    }
                }
            }
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            Breadcrumb(here) { target -> here = target }
            notice?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.screenH, vertical = Spacing.xs),
                )
            }
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Outlined.CreateNewFolder,
                        title = stringResource(R.string.drive_empty),
                        detail = stringResource(R.string.drive_empty_detail),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(entries, key = { it.relative }) { entry ->
                        DriveRow(
                            entry = entry,
                            onOpen = {
                                if (entry.isFolder) {
                                    here = entry.relative
                                } else {
                                    viewing = entry
                                }
                            },
                            onDelete = { deleting = entry },
                        )
                    }
                }
            }
        }
    }

    viewing?.let { entry ->
        DriveViewer(entry = entry, onDismiss = { viewing = null })
    }

    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.drive_delete_title)) },
            text = { Text(stringResource(R.string.drive_delete_detail, entry.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        DriveStore.delete(entry.relative)
                        deleting = null
                        refresh++
                    },
                ) {
                    Text(stringResource(R.string.delete_node), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (newFolder) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newFolder = false },
            title = { Text(stringResource(R.string.drive_new_folder)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(Radius.medium),
                    label = { Text(stringResource(R.string.drive_folder_name)) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        DriveStore.createFolder(here, name)
                        newFolder = false
                        refresh++
                    },
                ) {
                    Text(stringResource(R.string.create_node))
                }
            },
            dismissButton = {
                TextButton(onClick = { newFolder = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Where you are: "Drive / Attachments / First idea", each step tappable. */
@Composable
private fun Breadcrumb(here: String, onJump: (String) -> Unit) {
    val parts = if (here.isEmpty()) emptyList() else here.split('/')
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenH, vertical = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.drive_title),
            style = MaterialTheme.typography.labelLarge,
            color = if (here.isEmpty()) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .clickable { onJump("") }
                .padding(Spacing.xs),
        )
        parts.forEachIndexed { index, part ->
            Text(
                text = "/",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
            )
            val target = parts.take(index + 1).joinToString("/")
            Text(
                text = part,
                style = MaterialTheme.typography.labelLarge,
                color = if (index == parts.lastIndex) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .clickable { onJump(target) }
                    .padding(Spacing.xs),
            )
        }
    }
}

/** One row: kind icon, name, size and date, and a delete action. */
@Composable
private fun DriveRow(entry: DriveEntry, onOpen: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .heightIn(min = Sizes.listRowMin)
            .padding(horizontal = Spacing.screenH),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconFor(entry),
            contentDescription = null,
            tint = if (entry.isFolder) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(Sizes.iconMd),
        )
        Spacer(Modifier.width(Spacing.m))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(Sizes.touchMin),
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(R.string.att_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

/** The viewer: images fit the screen, text is shown as text, everything else says so honestly. */
@Composable
private fun DriveViewer(entry: DriveEntry, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val path = DriveStore.absolute(entry.relative)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(Radius.large),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(Spacing.screenH)) {
                SectionTitle(entry.name)
                Text(
                    text = entry.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.m))
                when {
                    path == null -> Text(
                        text = stringResource(R.string.drive_missing),
                        color = MaterialTheme.colorScheme.error,
                    )

                    entry.kind == DriveKind.Image -> {
                        val bitmap = remember(path) { decodeThumbnail(context, path, 1400) }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = entry.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 460.dp),
                            )
                        } else {
                            Text(stringResource(R.string.att_image_broken))
                        }
                    }

                    entry.kind == DriveKind.Text -> {
                        val text = remember(path) {
                            runCatching { File(path).readText() }.getOrDefault("")
                        }
                        Text(
                            text = text.take(20_000),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState()),
                        )
                    }

                    else -> Text(
                        text = stringResource(R.string.drive_no_viewer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Spacing.m))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.link_close)) }
            }
        }
    }
}

/** A Drive picker for the node dialogs: choose a file that is already in the Drive. */
@Composable
internal fun DrivePickerDialog(
    title: String,
    onlyKind: DriveKind?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var here by remember { mutableStateOf("") }
    val entries = remember(here) {
        DriveStore.list(here).filter { it.isFolder || onlyKind == null || it.kind == onlyKind }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp)) {
                Breadcrumb(here) { target -> here = target }
                if (entries.isEmpty()) {
                    Text(
                        text = stringResource(R.string.drive_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    LazyColumn {
                        items(entries, key = { it.relative }) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (entry.isFolder) {
                                            here = entry.relative
                                        } else {
                                            DriveStore.absolute(entry.relative)?.let(onPick)
                                        }
                                    }
                                    .heightIn(min = Sizes.listRowMin),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = iconFor(entry),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(Sizes.iconSm),
                                )
                                Spacer(Modifier.width(Spacing.s))
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun iconFor(entry: DriveEntry) = when (entry.kind) {
    DriveKind.Folder -> Icons.Outlined.Folder
    DriveKind.Image -> Icons.Outlined.Image
    DriveKind.Text -> Icons.Outlined.Description
    DriveKind.Audio -> Icons.Outlined.MusicNote
    DriveKind.Video -> Icons.Outlined.Movie
    DriveKind.Pdf -> Icons.Outlined.PictureAsPdf
    else -> Icons.AutoMirrored.Outlined.InsertDriveFile
}