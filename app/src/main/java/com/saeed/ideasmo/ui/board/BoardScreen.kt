package com.saeed.ideasmo.ui.board

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.log.AppLog
import com.saeed.ideasmo.core.model.IdeaNode
import com.saeed.ideasmo.core.model.NodeLink
import com.saeed.ideasmo.core.model.NodeStore
import com.saeed.ideasmo.core.util.copyPictureIntoAppStorage
import com.saeed.ideasmo.ui.components.ToolButton
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

private const val MIN_SCALE = 0.25f
private const val MAX_SCALE = 2.0f
private const val ZOOM_STEP = 0.25f
private const val NODE_W = 200f
private const val NODE_H_EST = 96f
private const val GRID_STEP = 100f
private const val MINIMAP_LINGER_MS = 900L
private const val MINIMAP_W = 200
private const val MINIMAP_H = 140

private enum class ViewMode { Board, List }

/**
 * The board: a 2D surface you pan with one finger and pinch to zoom, with nodes you can drag
 * and simple connections between them. Shared by every module (Idea, Mindmap, Knowledge, Note)
 * — separated only by [kind].
 *
 * A minimap appears while you travel (touch / drag) and shows where you are on the board,
 * the surrounding nodes, and your finger as a red dot. It is deliberately not always visible.
 */
private class BoardState {
    var scale by mutableStateOf(1f)
    var offsetX by mutableStateOf(0f)
    var offsetY by mutableStateOf(0f)
    var viewportW by mutableStateOf(0f)
    var viewportH by mutableStateOf(0f)

    /** True while a node is being dragged, so the board itself does not pan at the same time. */
    var draggingNode by mutableStateOf(false)

    /** True while a finger is down on the board — this is what summons the minimap. */
    var gestureActive by mutableStateOf(false)
    var gestureEndedAt by mutableStateOf(0L)

    /** Last touch position, in viewport pixels. */
    var touchX by mutableStateOf(0f)
    var touchY by mutableStateOf(0f)

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

    fun worldToScreenX(wx: Float): Float = wx * scale + offsetX

    fun worldToScreenY(wy: Float): Float = wy * scale + offsetY

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
    val links = NodeStore.links

    var viewMode by remember { mutableStateOf(ViewMode.Board) }
    var formOpen by remember { mutableStateOf(false) }
    var nodeName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(3) }
    var picPath by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var connectFrom by remember { mutableStateOf<String?>(null) }
    var minimapVisible by remember { mutableStateOf(false) }
    var centeredOnce by remember { mutableStateOf(false) }

    // The minimap is summoned by touching the board and lingers briefly after release.
    LaunchedEffect(board.gestureActive, board.gestureEndedAt) {
        if (board.gestureActive) {
            minimapVisible = true
        } else if (board.gestureEndedAt > 0L) {
            delay(MINIMAP_LINGER_MS)
            minimapVisible = false
        }
    }

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

    // Tap on a node: complete a pending connection, otherwise select it.
    val onNodeTap: (IdeaNode) -> Unit = { node ->
        val from = connectFrom
        if (from != null) {
            connectFrom = null
            when (NodeStore.addLink(context, from, node.id)) {
                "ok" -> {
                    val fromName = NodeStore.byId(from)?.name ?: "?"
                    AppLog.i("Board", "linked '" + fromName + "' to '" + node.name + "'")
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.connected_ok, fromName, node.name),
                        )
                    }
                }

                "exists" -> {
                    AppLog.i("Board", "link skipped: already connected")
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.link_exists)) }
                }

                else -> {
                    AppLog.w("Board", "link rejected: " + node.id)
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.link_self)) }
                }
            }
        } else {
            selectedId = node.id
            AppLog.i("Board", "node selected: " + node.name)
        }
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
            if (viewMode == ViewMode.List) {
                BoardListView(
                    nodes = nodes,
                    selectedId = selectedId,
                    onSelect = onNodeTap,
                    onGoToBoard = { viewMode = ViewMode.Board },
                    modifier = Modifier.weight(1f),
                )
            } else {
                val gridMinor = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                val gridMajor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                val linkColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)

                // Edge endpoints in screen space, computed once per frame for the current transform.
                val edgeEnds = links.mapNotNull { link ->
                    val a = NodeStore.byId(link.fromId)
                    val b = NodeStore.byId(link.toId)
                    if (a == null || b == null) {
                        null
                    } else {
                        Offset(
                            board.worldToScreenX(a.x + NODE_W / 2f),
                            board.worldToScreenY(a.y + NODE_H_EST / 2f),
                        ) to Offset(
                            board.worldToScreenX(b.x + NODE_W / 2f),
                            board.worldToScreenY(b.y + NODE_H_EST / 2f),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .onSizeChanged {
                            board.viewportW = it.width.toFloat()
                            board.viewportH = it.height.toFloat()
                        }
                        .drawBehind {
                            drawBoardGrid(board, gridMinor, gridMajor)
                            drawLinks(edgeEnds, linkColor, board.scale)
                        }
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
                        }
                        .pointerInput(kind) {
                            // Tracks any touch on the board purely to drive the minimap.
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                board.gestureActive = true
                                board.touchX = down.position.x
                                board.touchY = down.position.y
                                var pressed = true
                                while (pressed) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull()
                                    if (change != null) {
                                        board.touchX = change.position.x
                                        board.touchY = change.position.y
                                    }
                                    pressed = event.changes.any { it.pressed }
                                }
                                board.gestureActive = false
                                board.gestureEndedAt = System.currentTimeMillis()
                            }
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
                                onTap = { onNodeTap(node) },
                                onDragEnd = { NodeStore.persist(context) },
                            )
                        }
                    }

                    // Minimap — only while travelling
                    if (minimapVisible) {
                        MiniMap(
                            nodes = nodes,
                            links = links,
                            board = board,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(Spacing.l)
                                .size(width = MINIMAP_W.dp, height = MINIMAP_H.dp),
                        )
                    }

                    // Fixed overlays
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
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.ai_not_built))
                                }
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
            }

            // Node actions (selected node) or the connect hint
            val selectedNode = selectedId?.let { NodeStore.byId(it) }
            if (!formOpen) {
                if (connectFrom != null) {
                    ConnectHintBar(onCancel = {
                        connectFrom = null
                        AppLog.i("Board", "connection cancelled")
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.connect_cancelled)) }
                    })
                } else if (selectedNode != null) {
                    NodeActionBar(
                        nodeName = selectedNode.name,
                        onConnect = {
                            connectFrom = selectedNode.id
                            selectedId = null
                            AppLog.i("Board", "connect started from '" + selectedNode.name + "'")
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.connect_hint)) }
                        },
                        onDelete = {
                            val removed = NodeStore.remove(context, selectedNode.id)
                            selectedId = null
                            if (removed != null) {
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.node_deleted),
                                        actionLabel = context.getString(R.string.undo),
                                        withDismissAction = true,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        NodeStore.restore(context, removed.first, removed.second)
                                        AppLog.i("Board", "delete undone: " + removed.first.name)
                                    }
                                }
                            }
                        },
                    )
                }
            }

            // Bottom toolbar: board / list · zoom
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
                        active = viewMode == ViewMode.Board,
                        onClick = {
                            viewMode = ViewMode.Board
                            AppLog.i("Board", "view: board")
                        },
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val onBoard = viewMode == ViewMode.Board
                        ToolButton(
                            icon = Icons.Outlined.Remove,
                            contentDescription = stringResource(R.string.cd_zoom_out),
                            enabled = onBoard && board.scale > MIN_SCALE,
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
                                .clickable(enabled = onBoard) {
                                    board.resetView(NodeStore.nodesOf(kind))
                                    AppLog.i("Board", "view reset by tapping the zoom label")
                                    scope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.view_reset))
                                    }
                                }
                                .padding(horizontal = Spacing.m, vertical = Spacing.s),
                        )
                        ToolButton(
                            icon = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.cd_zoom_in),
                            enabled = onBoard && board.scale < MAX_SCALE,
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
                        active = viewMode == ViewMode.List,
                        onClick = {
                            viewMode = ViewMode.List
                            AppLog.i("Board", "view: list")
                        },
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

/** Draws the connections between nodes, behind the node cards. */
private fun DrawScope.drawLinks(edges: List<Pair<Offset, Offset>>, color: Color, scale: Float) {
    val width = max(1.5f, 2f * scale)
    edges.forEach { (a, b) ->
        drawLine(
            color = color,
            start = a,
            end = b,
            strokeWidth = width,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * The travelling minimap: a window that appears while you move around the board.
 * Shows every node, the connections, the current viewport, and your finger as a red dot.
 */
@Composable
private fun MiniMap(
    nodes: List<IdeaNode>,
    links: List<NodeLink>,
    board: BoardState,
    modifier: Modifier = Modifier,
) {
    val viewLeft = board.screenToWorldX(0f)
    val viewTop = board.screenToWorldY(0f)
    val viewRight = board.screenToWorldX(board.viewportW)
    val viewBottom = board.screenToWorldY(board.viewportH)
    val touchWorldX = board.screenToWorldX(board.touchX)
    val touchWorldY = board.screenToWorldY(board.touchY)

    var minX = viewLeft
    var maxX = viewRight
    var minY = viewTop
    var maxY = viewBottom
    nodes.forEach { n ->
        if (n.x < minX) minX = n.x
        if (n.x + NODE_W > maxX) maxX = n.x + NODE_W
        if (n.y < minY) minY = n.y
        if (n.y + NODE_H_EST > maxY) maxY = n.y + NODE_H_EST
    }
    val padX = (maxX - minX) * 0.12f + 40f
    val padY = (maxY - minY) * 0.12f + 40f
    minX -= padX
    maxX += padX
    minY -= padY
    maxY += padY
    val worldW = (maxX - minX).coerceAtLeast(1f)
    val worldH = (maxY - minY).coerceAtLeast(1f)

    val nodeColor = MaterialTheme.colorScheme.onSurfaceVariant
    val linkColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val viewColor = MaterialTheme.colorScheme.primary
    val touchColor = MaterialTheme.colorScheme.error
    val borderColor = MaterialTheme.colorScheme.outline

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Radius.medium),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 3.dp,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(Spacing.s)) {
            val sx = size.width / worldW
            val sy = size.height / worldH

            links.forEach { link ->
                val a = NodeStore.byId(link.fromId) ?: return@forEach
                val b = NodeStore.byId(link.toId) ?: return@forEach
                drawLine(
                    color = linkColor,
                    start = Offset((a.x + NODE_W / 2f - minX) * sx, (a.y + NODE_H_EST / 2f - minY) * sy),
                    end = Offset((b.x + NODE_W / 2f - minX) * sx, (b.y + NODE_H_EST / 2f - minY) * sy),
                    strokeWidth = 1.5f,
                )
            }

            nodes.forEach { n ->
                drawRect(
                    color = nodeColor,
                    topLeft = Offset((n.x - minX) * sx, (n.y - minY) * sy),
                    size = Size(NODE_W * sx, NODE_H_EST * sy),
                )
            }

            drawRect(
                color = viewColor,
                topLeft = Offset((viewLeft - minX) * sx, (viewTop - minY) * sy),
                size = Size((viewRight - viewLeft) * sx, (viewBottom - viewTop) * sy),
                style = Stroke(width = 2f),
            )

            drawCircle(
                color = touchColor,
                radius = 5f,
                center = Offset((touchWorldX - minX) * sx, (touchWorldY - minY) * sy),
            )
        }
    }
}

@Composable
private fun NodeActionBar(
    nodeName: String,
    onConnect: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.l, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = nodeName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onConnect,
                modifier = Modifier.heightIn(min = Sizes.touchMin),
            ) {
                Text(stringResource(R.string.cd_connect))
            }
            TextButton(
                onClick = onDelete,
                modifier = Modifier.heightIn(min = Sizes.touchMin),
            ) {
                Text(
                    text = stringResource(R.string.delete_node),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ConnectHintBar(onCancel: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.l, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.connect_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onCancel,
                modifier = Modifier.heightIn(min = Sizes.touchMin),
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
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
    val linkedCount = NodeStore.linksOf(node.id).size

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
                Spacer(Modifier.width(Spacing.s))
                if (linkedCount > 0) {
                    Text(
                        text = linkedCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
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