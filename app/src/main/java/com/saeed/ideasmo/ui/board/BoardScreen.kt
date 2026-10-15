package com.saeed.ideasmo.ui.board

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DarkMode
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.log.AppLog
import com.saeed.ideasmo.core.model.IdeaNode
import com.saeed.ideasmo.core.model.NodeStore
import com.saeed.ideasmo.core.util.copyPictureIntoAppStorage
import com.saeed.ideasmo.core.util.decodeThumbnail
import com.saeed.ideasmo.ui.components.PrimaryAction
import com.saeed.ideasmo.ui.components.ToolButton
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt

private const val MIN_SCALE = 0.25f
private const val MAX_SCALE = 2.0f
private const val ZOOM_STEP = 0.25f
private const val NODE_W = 200f
private const val NODE_H_EST = 96f
private const val GRID_STEP = 100f
private const val PIC_BOX = 112

/**
 * The board: an infinite-feeling 2D surface you pan with one finger and pinch to zoom,
 * exactly like a mind-map canvas. Nodes live in board coordinates and can be dragged.
 *
 * This is the shared foundation — every module (Idea, Mindmap, Knowledge, Note) uses the
 * same board, separated only by [kind].
 */
private class BoardState {
    var scale by mutableStateOf(1f)
    var offsetX by mutableStateOf(0f)
    var offsetY by mutableStateOf(0f)
    var viewportW by mutableStateOf(0f)
    var viewportH by mutableStateOf(0f)

    /** True while a node is being dragged, so the board itself does not pan at the same time. */
    var draggingNode by mutableStateOf(false)

    fun setScaleAround(target: Float, cx: Float, cy: Float) {
        val next = target.coerceIn(MIN_SCALE, MAX_SCALE)
        if (next == scale) return
        val k = next / scale
        offsetX = cx - (cx - offsetX) * k
        offsetY = cy - (cy - offsetY) * k
        scale = next
    }

    fun panBy(dx: Float, dy: Float) {
        offsetX += dx
        offsetY += dy
    }

    fun screenToWorldX(sx: Float): Float = (sx - offsetX) / scale

    fun screenToWorldY(sy: Float): Float = (sy - offsetY) / scale

    /** 100% zoom, with the content (or the origin, when empty) centred in the viewport. */
    fun resetView(nodes: List<IdeaNode>) {
        val cx = viewportW / 2f
        val cy = viewportH / 2f
        scale = 1f
        if (nodes.isEmpty()) {
            offsetX = cx
            offsetY = cy
        } else {
            val minX = nodes.minOf { it.x }
            val maxX = nodes.maxOf { it.x + NODE_W }
            val minY = nodes.minOf { it.y }
            val maxY = nodes.maxOf { it.y + NODE_H_EST }
            offsetX = cx - (minX + maxX) / 2f
            offsetY = cy - (minY + maxY) / 2f
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
    kind: String,
    snackbarHostState: SnackbarHostState,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val board = remember { BoardState() }

    val nodes = NodeStore.nodesOf(kind)

    var formOpen by remember { mutableStateOf(false) }
    var nodeName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(3) }
    var picPath by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var centeredOnce by remember { mutableStateOf(false) }

    // Put the origin in the middle of the viewport on first layout.
    LaunchedEffect(board.viewportW, board.viewportH) {
        if (!centeredOnce && board.viewportW > 0f && board.viewportH > 0f) {
            board.resetView(NodeStore.nodesOf(kind))
            centeredOnce = true
            AppLog.i("Board", "board centred: viewport=" + board.viewportW.toInt() + "x" + board.viewportH.toInt())
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) {
            AppLog.i("Board", "picture pick cancelled")
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
            AppLog.w("Board", "create blocked: empty node name")
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.node_name_required)) }
        } else {
            // Place the new node where the user is currently looking.
            val wx = board.screenToWorldX(board.viewportW / 2f) - NODE_W / 2f
            val wy = board.screenToWorldY(board.viewportH / 2f) - NODE_H_EST / 2f
            NodeStore.add(
                context,
                IdeaNode(
                    id = NodeStore.newId(),
                    kind = kind,
                    name = trimmed,
                    priority = priority,
                    description = description.trim(),
                    picUri = picPath,
                    createdAt = System.currentTimeMillis(),
                    x = wx,
                    y = wy,
                ),
            )
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.node_created, trimmed)) }
            nodeName = ""
            description = ""
            priority = 3
            picPath = null
            formOpen = false
        }
    }

    val notBuilt: (String) -> Unit = { label ->
        AppLog.i("Board", "toolbar tapped: " + label + " (not built)")
        scope.launch { snackbarHostState.showSnackbar(label) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.board_title),
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
            val gridMinor = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
            val gridMajor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .onSizeChanged {
                        board.viewportW = it.width.toFloat()
                        board.viewportH = it.height.toFloat()
                    }
                    .drawBehind { drawBoardGrid(board, gridMinor, gridMajor) }
                    .pointerInput(kind) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            if (!board.draggingNode) {
                                if (zoom != 1f) {
                                    board.setScaleAround(board.scale * zoom, centroid.x, centroid.y)
                                }
                                if (pan != Offset.Zero) {
                                    board.panBy(pan.x, pan.y)
                                }
                            }
                        }
                    }
                    .pointerInput(kind) {
                        detectTapGestures { selectedId = null }
                    },
            ) {
                // Transformed board content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = board.scale,
                            scaleY = board.scale,
                            translationX = board.offsetX,
                            translationY = board.offsetY,
                            transformOrigin = TransformOrigin(0f, 0f),
                            clip = false,
                        ),
                ) {
                    nodes.forEach { node ->
                        BoardNode(
                            node = node,
                            selected = node.id == selectedId,
                            board = board,
                            onTap = {
                                selectedId = node.id
                                AppLog.i("Board", "node selected: " + node.name)
                            },
                            onDragEnd = { NodeStore.persist(context) },
                        )
                    }
                }

                // Fixed overlays (they must not move with the board)
                if (formOpen) {
                    NodeFormPanel(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        name = nodeName,
                        onNameChange = { nodeName = it },
                        priority = priority,
                        onPriorityChange = { priority = it },
                        description = description,
                        onDescriptionChange = { description = it },
                        picPath = picPath,
                        onPickImage = {
                            AppLog.i("Board", "picture picker launched")
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onAi = {
                            AppLog.i("Board", "Ai button tapped (not built)")
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.ai_not_built)) }
                        },
                        onCancel = {
                            AppLog.i("Board", "node form cancelled")
                            formOpen = false
                        },
                        onCreate = createNode,
                    )
                } else if (nodes.isEmpty()) {
                    NewNodeCentre(
                        modifier = Modifier.align(Alignment.Center),
                        onClick = {
                            formOpen = true
                            AppLog.i("Board", "new node tapped (empty board)")
                        },
                    )
                } else {
                    NewNodePill(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = Spacing.l),
                        onClick = {
                            formOpen = true
                            AppLog.i("Board", "new node tapped (pill)")
                        },
                    )
                }
            }

            // Bottom toolbar: graph (current view) · zoom · list
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
                        active = true,
                        onClick = { notBuilt(context.getString(R.string.graph_not_built)) },
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ToolButton(
                            icon = Icons.Outlined.Remove,
                            contentDescription = stringResource(R.string.cd_zoom_out),
                            enabled = board.scale > MIN_SCALE,
                            onClick = {
                                board.setScaleAround(
                                    board.scale - ZOOM_STEP,
                                    board.viewportW / 2f,
                                    board.viewportH / 2f,
                                )
                                AppLog.i("Board", "zoom out -> " + (board.scale * 100).toInt() + "%")
                            },
                        )
                        Text(
                            text = stringResource(R.string.zoom_level, (board.scale * 100).toInt()),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(Radius.small))
                                .clickable {
                                    board.resetView(NodeStore.nodesOf(kind))
                                    AppLog.i("Board", "view reset by tapping the zoom label")
                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.view_reset)) }
                                }
                                .padding(horizontal = Spacing.m, vertical = Spacing.s),
                        )
                        ToolButton(
                            icon = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.cd_zoom_in),
                            enabled = board.scale < MAX_SCALE,
                            onClick = {
                                board.setScaleAround(
                                    board.scale + ZOOM_STEP,
                                    board.viewportW / 2f,
                                    board.viewportH / 2f,
                                )
                                AppLog.i("Board", "zoom in -> " + (board.scale * 100).toInt() + "%")
                            },
                        )
                    }

                    ToolButton(
                        icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                        contentDescription = stringResource(R.string.tool_list),
                        onClick = { notBuilt(context.getString(R.string.list_not_built)) },
                    )
                }
            }
        }
    }
}

/** Draws only the grid lines that are actually visible, in viewport space. */
private fun DrawScope.drawBoardGrid(board: BoardState, minor: Color, major: Color) {
    val scale = board.scale
    if (scale <= 0f) return
    val stepPx = GRID_STEP * scale
    if (stepPx < 6f) return

    val worldLeft = board.screenToWorldX(0f)
    val worldTop = board.screenToWorldY(0f)
    val worldRight = board.screenToWorldX(size.width)
    val worldBottom = board.screenToWorldY(size.height)

    var wx = floor(worldLeft / GRID_STEP) * GRID_STEP
    while (wx <= worldRight) {
        val sx = wx * scale + board.offsetX
        val isMajor = wx % (GRID_STEP * 5f) == 0f
        drawLine(
            color = if (isMajor) major else minor,
            start = Offset(sx, 0f),
            end = Offset(sx, size.height),
            strokeWidth = if (isMajor) 1.5f else 1f,
        )
        wx += GRID_STEP
    }

    var wy = floor(worldTop / GRID_STEP) * GRID_STEP
    while (wy <= worldBottom) {
        val sy = wy * scale + board.offsetY
        val isMajor = wy % (GRID_STEP * 5f) == 0f
        drawLine(
            color = if (isMajor) major else minor,
            start = Offset(0f, sy),
            end = Offset(size.width, sy),
            strokeWidth = if (isMajor) 1.5f else 1f,
        )
        wy += GRID_STEP
    }
}

@Composable
private fun NewNodeCentre(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier,
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
        Spacer(Modifier.height(Spacing.l))
        Text(
            text = stringResource(R.string.board_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NewNodePill(modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Sizes.touchMin),
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(Sizes.iconSm),
        )
        Spacer(Modifier.width(Spacing.s))
        Text(stringResource(R.string.new_node))
    }
}

@Composable
private fun BoardNode(
    node: IdeaNode,
    selected: Boolean,
    board: BoardState,
    onTap: () -> Unit,
    onDragEnd: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap = remember(node.picUri) { node.picUri?.let { decodeThumbnail(context, it) } }

    Card(
        modifier = Modifier
            .offset { IntOffset(node.x.roundToInt(), node.y.roundToInt()) }
            .width(NODE_W.dp)
            .pointerInput(node.id) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(node.id) {
                detectDragGestures(
                    onDragStart = { board.draggingNode = true },
                    onDragEnd = {
                        board.draggingNode = false
                        onDragEnd()
                        AppLog.i("Board", "node drag ended: " + node.name)
                    },
                    onDragCancel = { board.draggingNode = false },
                ) { change, dragAmount ->
                    change.consume()
                    // Read the live position from the store: the gesture coroutine outlives
                    // recomposition, so the captured node object would go stale mid-drag.
                    val current = NodeStore.byId(node.id) ?: node
                    NodeStore.move(
                        node.id,
                        current.x + dragAmount.x / board.scale,
                        current.y + dragAmount.y / board.scale,
                    )
                }
            },
        shape = RoundedCornerShape(Radius.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 0.dp),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Column(modifier = Modifier.padding(Spacing.m)) {
            Text(
                text = node.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.s))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f)) {
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
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(Radius.small)),
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
    }
}

@Composable
private fun NodeFormPanel(
    modifier: Modifier = Modifier,
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = Radius.large, topEnd = Radius.large),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.l),
        ) {
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
                                AppLog.i("Board", "priority set to " + level)
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
                            .heightIn(min = 96.dp),
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