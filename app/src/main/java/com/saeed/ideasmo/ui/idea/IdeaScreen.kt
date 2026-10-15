package com.saeed.ideasmo.ui.idea

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.log.AppLog
import com.saeed.ideasmo.core.model.IdeaNode
import com.saeed.ideasmo.core.model.NodeStore
import com.saeed.ideasmo.ui.components.PrimaryAction
import com.saeed.ideasmo.ui.components.ToolButton
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.io.File

private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 2.0f
private const val ZOOM_STEP = 0.25f
private const val PIC_BOX = 112

/**
 * Idea node editor (wireframe 2, 2026-10-15). This is the core screen and the seed
 * of the node manager: a canvas of nodes, a "new node" affordance in the middle that
 * opens the node form, and the graph / zoom / list toolbar at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaScreen(
    snackbarHostState: SnackbarHostState,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var formOpen by remember { mutableStateOf(false) }
    var nodeName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(3) }
    var picPath by remember { mutableStateOf<String?>(null) }
    var zoom by remember { mutableStateOf(1f) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) {
            AppLog.i("Idea", "picture pick cancelled")
        } else {
            picPath = copyPictureIntoAppStorage(context, uri)
            if (picPath == null) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.pic_pick_failed)) }
            }
        }
    }

    val createNode: () -> Unit = {
        val trimmed = nodeName.trim()
        if (trimmed.isEmpty()) {
            AppLog.w("Idea", "create blocked: empty node name")
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.node_name_required)) }
        } else {
            NodeStore.add(
                context,
                IdeaNode(
                    id = NodeStore.newId(),
                    name = trimmed,
                    priority = priority,
                    description = description.trim(),
                    picUri = picPath,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.node_created, trimmed))
            }
            nodeName = ""
            description = ""
            priority = 3
            picPath = null
            formOpen = false
        }
    }

    val notBuilt: (String) -> Unit = { label ->
        AppLog.i("Idea", "toolbar tapped: " + label + " (not built)")
        scope.launch { snackbarHostState.showSnackbar(label) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.idea_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                            contentDescription = stringResource(R.string.cd_theme_toggle),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            // Canvas viewport
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .graphicsLayer(
                            scaleX = zoom,
                            scaleY = zoom,
                            transformOrigin = TransformOrigin(0.5f, 0f),
                        )
                        .padding(horizontal = Spacing.screenH),
                ) {
                    Spacer(Modifier.height(Spacing.l))

                    NodeStore.nodes.forEach { node ->
                        NodeCard(node)
                        Spacer(Modifier.height(Spacing.s))
                    }

                    NewNodeButton(onClick = {
                        formOpen = !formOpen
                        AppLog.i("Idea", "new node tapped, form open=" + formOpen)
                    })

                    if (formOpen) {
                        Spacer(Modifier.height(Spacing.l))
                        NodeForm(
                            name = nodeName,
                            onNameChange = { nodeName = it },
                            priority = priority,
                            onPriorityChange = { priority = it },
                            description = description,
                            onDescriptionChange = { description = it },
                            picPath = picPath,
                            onPickImage = {
                                AppLog.i("Idea", "picture picker launched")
                                pickImage.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            onAi = {
                                AppLog.i("Idea", "Ai button tapped (not built)")
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.ai_not_built))
                                }
                            },
                            onCancel = {
                                AppLog.i("Idea", "node form cancelled")
                                formOpen = false
                            },
                            onCreate = createNode,
                        )
                    }

                    Spacer(Modifier.height(Spacing.xl))
                }
            }

            // Bottom toolbar: graph · zoom out / in · list
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = Spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ToolButton(
                        icon = Icons.Outlined.AccountTree,
                        contentDescription = stringResource(R.string.tool_graph),
                        onClick = { notBuilt(context.getString(R.string.graph_not_built)) },
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ToolButton(
                            icon = Icons.Outlined.Remove,
                            contentDescription = stringResource(R.string.cd_zoom_out),
                            enabled = zoom > MIN_ZOOM,
                            onClick = {
                                zoom = (zoom - ZOOM_STEP).coerceAtLeast(MIN_ZOOM)
                                AppLog.i("Idea", "zoom out -> " + (zoom * 100).toInt() + "%")
                            },
                        )
                        Text(
                            text = stringResource(R.string.zoom_level, (zoom * 100).toInt()),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ToolButton(
                            icon = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.cd_zoom_in),
                            enabled = zoom < MAX_ZOOM,
                            onClick = {
                                zoom = (zoom + ZOOM_STEP).coerceAtMost(MAX_ZOOM)
                                AppLog.i("Idea", "zoom in -> " + (zoom * 100).toInt() + "%")
                            },
                        )
                    }

                    ToolButton(
                        icon = Icons.Outlined.FormatListBulleted,
                        contentDescription = stringResource(R.string.tool_list),
                        onClick = { notBuilt(context.getString(R.string.list_not_built)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NewNodeButton(onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.new_node),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.m))
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.cd_new_node),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Sizes.iconTile),
            )
        }
    }
}

@Composable
private fun NodeForm(
    name: String,
    onNameChange: (String) -> Unit,
    priority: Int,
    onPriorityChange: (Int) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    picPath: String?,
    onPickImage: () -> Unit,
    onAi: () -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.large),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.l)) {

            // Priority
            Text(
                text = stringResource(R.string.priority),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xs))
            Row {
                (1..5).forEach { level ->
                    Box(
                        modifier = Modifier
                            .size(Sizes.touchMin)
                            .clip(CircleShape)
                            .clickable {
                                onPriorityChange(level)
                                AppLog.i("Idea", "priority set to " + level)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (level <= priority) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.m))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(Radius.medium),
                label = { Text(stringResource(R.string.node_name)) },
            )

            Spacer(Modifier.height(Spacing.l))

            // Description + Pic, side by side (wireframe row 2)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.description),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilledTonalButton(
                            onClick = onAi,
                            modifier = Modifier.heightIn(min = Sizes.touchMin),
                        ) {
                            Text(stringResource(R.string.ai_assist))
                        }
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 112.dp),
                        shape = RoundedCornerShape(Radius.medium),
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.pic),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    PicBox(picPath = picPath, onPick = onPickImage)
                }
            }

            Spacer(Modifier.height(Spacing.l))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(Spacing.s))
                PrimaryAction(text = stringResource(R.string.create_node), onClick = onCreate)
            }
        }
    }
}

@Composable
private fun PicBox(picPath: String?, onPick: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(picPath) { picPath?.let { decodeThumbnail(context, it) } }

    Box(
        modifier = Modifier
            .size(PIC_BOX.dp)
            .clip(RoundedCornerShape(Radius.medium))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.cd_pick_image),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.iconMd),
            )
        }
    }
}

@Composable
private fun NodeCard(node: IdeaNode) {
    val context = LocalContext.current
    val bitmap = remember(node.picUri) { node.picUri?.let { decodeThumbnail(context, it) } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.padding(Spacing.l)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.xs))
                Row {
                    (1..5).forEach { level ->
                        Box(
                            modifier = Modifier
                                .padding(end = Spacing.xs)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (level <= node.priority) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                ),
                        )
                    }
                }
                if (node.description.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = node.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (bitmap != null) {
                Spacer(Modifier.width(Spacing.m))
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(Radius.small)),
                )
            }
        }
    }
}

/** Copies the picked image into app storage so it survives restarts (no URI permission needed). */
private fun copyPictureIntoAppStorage(context: Context, uri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "pics")
        if (!dir.exists() && !dir.mkdirs()) {
            AppLog.w("Idea", "cannot create pics dir: " + dir.absolutePath)
            return null
        }
        val target = File(dir, "pic-" + System.currentTimeMillis() + ".jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        AppLog.i("Idea", "picture stored: " + target.absolutePath + " (" + target.length() + " bytes)")
        target.absolutePath
    } catch (t: Throwable) {
        AppLog.e("Idea", "picture copy failed", t)
        null
    }
}

/** Manual thumbnail decode — no image-loading dependency needed for one small preview. */
private fun decodeThumbnail(context: Context, pathOrUri: String, maxPx: Int = 256): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(context, pathOrUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxPx || bounds.outHeight / sample > maxPx) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        openStream(context, pathOrUri)?.use { BitmapFactory.decodeStream(it, null, options) }
    } catch (t: Throwable) {
        AppLog.e("Idea", "thumbnail decode failed for " + pathOrUri, t)
        null
    }
}

private fun openStream(context: Context, pathOrUri: String) =
    if (pathOrUri.startsWith("content://")) {
        context.contentResolver.openInputStream(Uri.parse(pathOrUri))
    } else {
        File(pathOrUri).inputStream()
    }