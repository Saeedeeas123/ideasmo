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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.drive.DriveEntry
import com.saeed.ideasmo.core.drive.DriveKind
import com.saeed.ideasmo.core.drive.DriveOpen
import com.saeed.ideasmo.core.drive.DrivePaths
import com.saeed.ideasmo.core.drive.DriveStore
import com.saeed.ideasmo.core.log.AppLog
import com.saeed.ideasmo.core.util.decodeThumbnail
import com.saeed.ideasmo.ui.components.ChoiceChip
import com.saeed.ideasmo.ui.components.EmptyState
import com.saeed.ideasmo.ui.components.SectionTitle
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing
import java.io.File

/**
 * **The Drive: the app's own light file manager.**
 *
 * Saeed's orders: a personal drive where every file lives, reachable from the whole app, with a simple,
 * light file manager that can open the files it holds, directly connected to what a node carries
 * (v0.13.0); the **basic operations** — rename, move, copy, sort, search, open-with (v0.14.0); and
 * from v0.15.0 the Drive is read **file-first**:
 *
 * - The top of the screen is a row of **shelves** — All files, Images, Texts, Documents, Audio, Video,
 *   Archives, Other — and Folders for walking the tree. Choosing a shelf shows **every file of that
 *   kind wherever it sits**, so a picture attached to a node and a picture imported here are both
 *   simply pictures (Saeed: "whatever type of file is chosen at the top, every existing file of that
 *   type is listed").
 * - **Import follows the shelf**: importing while Images is open files the file in `Images/`, and
 *   importing from All or Folders files it by its own kind. The user says *what* they are importing,
 *   never *where* it goes — the Drive files it (Saeed: "you tidy the file manager yourself and it ends
 *   up in the right place").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // Which view the Drive is showing. Folders is the only view with a "where we are" path; the
    // shelf views are flat by design.
    var view by remember { mutableStateOf<DriveView>(DriveView.Folders) }
    var here by remember { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }
    var sort by remember { mutableStateOf(DriveStore.Sort.Name) }
    var query by remember { mutableStateOf("") }
    var viewing by remember { mutableStateOf<DriveEntry?>(null) }
    var deleting by remember { mutableStateOf<DriveEntry?>(null) }
    var renaming by remember { mutableStateOf<DriveEntry?>(null) }
    var moving by remember { mutableStateOf<DriveEntry?>(null) }
    var newFolder by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    val searching = query.isNotBlank()
    val counts = remember(refresh) { DriveStore.countsByShelf() }
    val entries = remember(view, here, refresh, sort, query) {
        val scope = if (view == DriveView.Folders) here else ""
        if (searching) {
            DriveStore.search(query, scope)
        } else {
            when (val current = view) {
                DriveView.All -> DriveStore.listAllFiles(sort)
                DriveView.Folders -> DriveStore.list(here, sort)
                is DriveView.Shelf -> DriveStore.listShelf(current.shelf, sort)
            }
        }
    }

    // Where an import lands: the shelf that is open, or — from All / Folders — the file's own kind.
    val importTarget: String? = (view as? DriveView.Shelf)?.let { DriveStore.shelfFolder(it.shelf) }

    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            AppLog.i("Drive", "import cancelled")
        } else {
            val path = DriveStore.importUri(context, uri, importTarget)
            notice = if (path == null) {
                context.getString(R.string.drive_import_failed)
            } else {
                context.getString(
                    R.string.drive_imported_to,
                    File(path).name,
                    DrivePaths.parentOf(DriveStore.relativeOf(path).orEmpty()),
                )
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
                            text = when (val current = view) {
                                is DriveView.Shelf -> current.shelf.label
                                else -> if (here.isEmpty()) {
                                    stringResource(R.string.drive_title)
                                } else {
                                    here.substringAfterLast('/')
                                }
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
                            when {
                                searching -> query = ""
                                // Folders walks up one level; a shelf goes back to the shelves.
                                view == DriveView.Folders && here.isNotEmpty() ->
                                    here = DriveStore.parentOf(here)

                                else -> onBack()
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
            // navigationBarsPadding: a custom bottom bar does NOT get the system-bar inset from Scaffold,
            // which is exactly how the buttons ended up under the home indicator (Saeed's report).
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
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
                        // The button says where the file will be filed, so the routing is never a
                        // surprise: "Import to Images" while Images is open, plain "Add a file"
                        // elsewhere (the file's own kind decides there).
                        Text(
                            text = (view as? DriveView.Shelf)?.let { shelf ->
                                stringResource(R.string.drive_import_to, shelf.shelf.label)
                            } ?: stringResource(R.string.drive_import),
                        )
                    }
                    if (view == DriveView.Folders) {
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
            }
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            // The shelves: the Drive's index, file-first. Every chip carries how many files are on it.
            ShelfBar(
                selected = view,
                counts = counts,
                allCount = DriveStore.listAllFiles().size,
                onSelect = { chosen ->
                    view = chosen
                    query = ""
                    AppLog.i("Drive", "view changed to " + chosen)
                },
            )

            if (view == DriveView.Folders) {
                Breadcrumb(here) { target ->
                    here = target
                    query = ""
                }
            }

            // Search + sort: the two operations that belong to the whole list, not to one row.
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenH, vertical = Spacing.xs),
                singleLine = true,
                shape = RoundedCornerShape(Radius.medium),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.iconSm),
                    )
                },
                placeholder = { Text(stringResource(R.string.drive_search)) },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenH, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SortChip(R.string.drive_sort_name, sort == DriveStore.Sort.Name) { sort = DriveStore.Sort.Name }
                SortChip(R.string.drive_sort_size, sort == DriveStore.Sort.Size) { sort = DriveStore.Sort.Size }
                SortChip(R.string.drive_sort_new, sort == DriveStore.Sort.Newest) {
                    sort = DriveStore.Sort.Newest
                }
            }

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
                        title = stringResource(
                            when {
                                searching -> R.string.drive_no_results
                                view is DriveView.Shelf -> R.string.drive_shelf_empty
                                else -> R.string.drive_empty
                            },
                        ),
                        detail = stringResource(
                            when {
                                searching -> R.string.drive_no_results_detail
                                view is DriveView.Shelf ->
                                    R.string.drive_shelf_empty_detail

                                else -> R.string.drive_empty_detail
                            },
                        ),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(entries, key = { it.relative }) { entry ->
                        DriveRow(
                            entry = entry,
                            // In a flat view a row says where the file lives, because the list
                            // deliberately does not follow the folders.
                            location = if (view is DriveView.Shelf || view == DriveView.All) {
                                DrivePaths.parentOf(entry.relative)
                            } else {
                                null
                            },
                            onOpen = {
                                if (entry.isFolder) {
                                    // Tapping a folder always means "show me inside it", so a folder
                                    // that turns up in a flat view (a search result, or All files)
                                    // switches to the folder view at that path. Otherwise the tap
                                    // would look like it did nothing.
                                    view = DriveView.Folders
                                    here = entry.relative
                                    query = ""
                                } else {
                                    viewing = entry
                                }
                            },
                            onRename = { renaming = entry },
                            onMove = { moving = entry },
                            onCopy = {
                                val copy = DriveStore.copy(entry.relative)
                                notice = if (copy == null) {
                                    context.getString(R.string.drive_copy_failed)
                                } else {
                                    context.getString(R.string.drive_copied, copy.substringAfterLast('/'))
                                }
                                refresh++
                            },
                            onOpenWith = {
                                val path = DriveStore.absolute(entry.relative)
                                if (path == null || !DriveOpen.openWith(context, path)) {
                                    notice = context.getString(R.string.drive_no_app)
                                }
                            },
                            onDelete = { deleting = entry },
                        )
                    }
                }
            }
        }
    }

    viewing?.let { entry -> DriveViewer(entry = entry, onDismiss = { viewing = null }) }

    renaming?.let { entry ->
        var name by remember(entry.relative) { mutableStateOf(entry.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.drive_rename_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(Radius.medium),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        DriveStore.rename(entry.relative, name)
                        renaming = null
                        refresh++
                    },
                ) {
                    Text(stringResource(R.string.drive_rename))
                }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    moving?.let { entry ->
        val targets = remember(entry.relative) { DriveStore.folders() }
        AlertDialog(
            onDismissRequest = { moving = null },
            title = { Text(stringResource(R.string.drive_move_title, entry.name)) },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    targets.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val ok = DriveStore.move(entry.relative, folder)
                                    notice = if (ok) {
                                        context.getString(R.string.drive_moved)
                                    } else {
                                        context.getString(R.string.drive_move_failed)
                                    }
                                    moving = null
                                    refresh++
                                }
                                .heightIn(min = Sizes.listRowMin),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(Sizes.iconSm),
                            )
                            Spacer(Modifier.width(Spacing.s))
                            Text(
                                text = if (folder.isEmpty()) {
                                    stringResource(R.string.drive_title)
                                } else {
                                    folder
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { moving = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
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

@Composable
private fun SortChip(label: Int, selected: Boolean, onClick: () -> Unit) {
    ChoiceChip(
        text = stringResource(label),
        selected = selected,
        onClick = onClick,
    )
}

/** Which view the Drive is showing — the file-first index, or the folder tree. */
private sealed interface DriveView {
    /** Every file in the Drive, whatever kind, folders ignored. */
    data object All : DriveView

    /** Every file of one kind, wherever it sits. */
    data class Shelf(val shelf: DrivePaths.DriveShelf) : DriveView

    /** The folder tree, for the times a folder is what the user is after. */
    data object Folders : DriveView
}

/**
 * The shelf bar: the Drive's index, and the thing that makes it file-first.
 *
 * Saeed's order (v0.15.0): "whatever type of file is chosen at the top, every existing file of that
 * type must be listed — and the same is true for importing: to import an image we go into Images and
 * import it there." So the bar is the first thing on the screen, it carries the number of files on
 * every shelf, and it costs one tap to switch.
 */
@Composable
private fun ShelfBar(
    selected: DriveView,
    counts: Map<DrivePaths.DriveShelf, Int>,
    allCount: Int,
    onSelect: (DriveView) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenH, vertical = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChoiceChip(
            text = stringResource(R.string.drive_shelf_all, allCount),
            selected = selected == DriveView.All,
            onClick = { onSelect(DriveView.All) },
        )
        DrivePaths.DriveShelf.order.forEach { shelf ->
            ChoiceChip(
                text = shelf.label + " " + (counts[shelf] ?: 0),
                selected = selected == DriveView.Shelf(shelf),
                onClick = { onSelect(DriveView.Shelf(shelf)) },
            )
        }
        ChoiceChip(
            text = stringResource(R.string.drive_shelf_folders),
            selected = selected == DriveView.Folders,
            onClick = { onSelect(DriveView.Folders) },
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
            .padding(horizontal = Spacing.screenH, vertical = Spacing.xs),
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

/** One row: kind icon, name, size and date, and the operations menu. */
@Composable
private fun DriveRow(
    entry: DriveEntry,
    location: String?,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onOpenWith: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember(entry.relative) { mutableStateOf(false) }
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
            // A file-first list does not follow folders, so a row says where its file actually lives.
            if (location != null) {
                Text(
                    text = if (location.isEmpty()) {
                        stringResource(R.string.drive_title)
                    } else {
                        location
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(Sizes.touchMin)) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.drive_operations),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (!entry.isFolder && !DriveOpen.readableInside(entry.name)) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.drive_open_with)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
                        onClick = { menu = false; onOpenWith() },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.drive_rename)) },
                    leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                    onClick = { menu = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.drive_move)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.DriveFileMove, contentDescription = null) },
                    onClick = { menu = false; onMove() },
                )
                if (!entry.isFolder) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.drive_copy)) },
                        leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        onClick = { menu = false; onCopy() },
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.delete_node),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

/** The viewer: images fit the screen, text is shown as text, everything else can go to its own app. */
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
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    if (path != null) {
                        TextButton(
                            onClick = { DriveOpen.openWith(context, path) },
                        ) {
                            Text(stringResource(R.string.drive_open_with))
                        }
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.link_close)) }
                }
            }
        }
    }
}

/**
 * A Drive picker for the node dialogs: choose a file that is already in the Drive.
 *
 * **File-first (v0.15.0):** when a kind is being asked for — a picture for a node's Image list, say —
 * the picker shows EVERY file of that kind wherever it sits, exactly like the Drive's own shelves. The
 * folder walk is still there for the "any file" case, where a folder is genuinely what the user is
 * looking for.
 */
@Composable
internal fun DrivePickerDialog(
    title: String,
    onlyKind: DriveKind?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var here by remember { mutableStateOf("") }
    val shelf = remember(onlyKind) { onlyKind?.let { DrivePaths.shelfOf(it) } }
    val entries = remember(here, onlyKind) {
        if (shelf != null) {
            DriveStore.listShelf(shelf)
        } else {
            DriveStore.list(here).filter { it.isFolder || onlyKind == null || it.kind == onlyKind }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp)) {
                // No breadcrumb on a shelf: the list is flat by design, so "where we are" would be a
                // lie. It stays for the folder walk, which is the only view that has a place.
                if (shelf == null) {
                    Breadcrumb(here) { target -> here = target }
                }
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    // On a shelf the folder is the only thing the row does not already
                                    // say, so it is the only thing the second line says.
                                    if (shelf != null) {
                                        Text(
                                            text = DrivePaths.parentOf(entry.relative).ifEmpty {
                                                stringResource(R.string.drive_title)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
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
    DriveKind.Document -> Icons.Outlined.Description
    DriveKind.Archive -> Icons.Outlined.Folder
    else -> Icons.AutoMirrored.Outlined.InsertDriveFile
}