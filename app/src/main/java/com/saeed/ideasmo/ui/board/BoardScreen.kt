package com.saeed.ideasmo.ui.board

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.saeed.ideasmo.R
import com.saeed.ideasmo.core.log.AppLog
import com.saeed.ideasmo.core.model.BoardSpace
import com.saeed.ideasmo.core.model.IdeaNode
import com.saeed.ideasmo.core.model.LayoutDirection
import com.saeed.ideasmo.core.model.LayoutPoint
import com.saeed.ideasmo.core.model.LayoutSpacing
import com.saeed.ideasmo.core.model.LinkCatalog
import com.saeed.ideasmo.core.model.LinkColor
import com.saeed.ideasmo.core.model.LinkType
import com.saeed.ideasmo.core.model.NodeActionCatalog
import com.saeed.ideasmo.core.model.NodeClipboard
import com.saeed.ideasmo.core.model.NodeLink
import com.saeed.ideasmo.core.model.NodeStore
import com.saeed.ideasmo.core.model.NodeStyle
import com.saeed.ideasmo.core.model.StructureLayout
import com.saeed.ideasmo.core.prefs.BoardPrefs
import com.saeed.ideasmo.core.util.copyPictureIntoAppStorage
import com.saeed.ideasmo.ui.components.ToolButton
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val MIN_SCALE = 0.25f
private const val MAX_SCALE = 2.0f
private const val ZOOM_STEP = 0.25f

/** Card size in board units — shared with the structure engine so both agree on the geometry. */
private const val NODE_W = StructureLayout.NODE_W
private const val NODE_H_EST = StructureLayout.NODE_H

private const val MINIMAP_LINGER_MS = 900L
private const val MINIMAP_W = 200
private const val MINIMAP_H = 140

private enum class ViewMode { Board, List }

/** Which full-screen panel is open. Only one at a time — a panel always covers the board. */
private sealed interface Panel {
    data class NodeInfo(val id: String) : Panel
    data class Picker(val id: String) : Panel
    data class LinkInfo(val id: String) : Panel
    data class Style(val id: String) : Panel
    data object Settings : Panel
}

/** One link, resolved for drawing: screen endpoints, board-space midpoint and its label. */
private data class RenderEdge(
    val linkId: String,
    val type: LinkType,
    val colorRole: LinkColor,
    val start: Offset,
    val end: Offset,
    val midBoard: Offset,
    val label: String?,
)

/**
 * The board: a 2D surface you pan with one finger and pinch to zoom, with nodes you can drag and
 * typed relations between them. Shared by every module (Idea, Mindmap, Knowledge, Note).
 *
 * v0.6.0 adds the three things the board was missing: a **link you can tap** (it opens the link
 * inspector), a **node panel** that lists the links leaving and arriving at a node in order, and a
 * **structure** layer — central nodes, ordered branches, collapse and an auto-layout in five
 * directions — plus the project settings behind the top-left button.
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
            val maxX = nodes.maxOf { it.x + StructureLayout.widthOf(it) }
            val minY = nodes.minOf { it.y }
            val maxY = nodes.maxOf { it.y + StructureLayout.heightOf(it) }
            offsetX = cx - (minX + maxX) / 2f
            offsetY = cy - (minY + maxY) / 2f
        }
    }

    /** Brings a point of board space to the middle of the screen — used for the origin. */
    fun centreOnWorld(wx: Float, wy: Float) {
        offsetX = viewportW / 2f - wx * scale
        offsetY = viewportH / 2f - wy * scale
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

    // Board settings are Compose state, so changing one re-draws the board immediately.
    val pattern = BoardPattern.fromKey(BoardPrefs.patternKey)
    val palette = BoardPalette.fromKey(BoardPrefs.paletteKey)
    val direction = LayoutDirection.fromKey(BoardPrefs.directionKey)
    val spacing = LayoutSpacing.fromKey(BoardPrefs.spacingKey)
    val showLabels = BoardPrefs.showLinkLabels
    val showMinimapPref = BoardPrefs.showMinimap
    val showOrigin = BoardPrefs.showOrigin

    val allNodes = NodeStore.nodesOf(kind)
    val hidden = NodeStore.hiddenIds()
    val nodes = allNodes.filter { it.id !in hidden }
    val nodeIds = nodes.map { it.id }.toSet()
    val links = NodeStore.links.filter { it.fromId in nodeIds && it.toId in nodeIds }

    var viewMode by remember { mutableStateOf(ViewMode.Board) }
    var formOpen by remember { mutableStateOf(false) }
    var nodeName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(3) }
    var picPath by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedLinkId by remember { mutableStateOf<String?>(null) }
    var panel by remember { mutableStateOf<Panel?>(null) }
    var connectFrom by remember { mutableStateOf<String?>(null) }
    var pendingType by remember { mutableStateOf(LinkCatalog.default) }
    var pendingLabel by remember { mutableStateOf<String?>(null) }
    var minimapVisible by remember { mutableStateOf(false) }
    var centeredOnce by remember { mutableStateOf(false) }

    // The node action square drives these: one dialog at a time, and the two tap-a-target modes.
    var textDialogFor by remember { mutableStateOf<String?>(null) }
    var notesDialogFor by remember { mutableStateOf<String?>(null) }
    var detailsFor by remember { mutableStateOf<String?>(null) }
    var pictureDialogFor by remember { mutableStateOf<String?>(null) }
    var imageTarget by remember { mutableStateOf<String?>(null) }
    var reattachFrom by remember { mutableStateOf<String?>(null) }

    val clipboard = LocalClipboardManager.current

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
        val target = imageTarget
        if (uri == null) {
            AppLog.i("Board", "picture pick cancelled")
            imageTarget = null
        } else {
            val path = copyPictureIntoAppStorage(context, uri)
            if (path == null) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.pic_pick_failed)) }
            } else if (target != null) {
                // Image action on an existing node: replace its picture in place.
                NodeStore.byId(target)?.let { node ->
                    NodeStore.updateNode(context, node.copy(picUri = path))
                }
                imageTarget = null
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.picture_updated)) }
            } else {
                picPath = path
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

    // Tap on a node: complete a pending reattach or link, otherwise select it.
    val onNodeTap: (IdeaNode) -> Unit = { node ->
        val reattach = reattachFrom
        val from = connectFrom
        if (reattach != null) {
            reattachFrom = null
            val moving = NodeStore.byId(reattach)
            when {
                reattach == node.id -> {
                    AppLog.w("Node", "reattach refused: same node")
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.link_self)) }
                }

                // A branch may never be folded into itself.
                NodeStore.subtreeOf(reattach).contains(node.id) -> {
                    AppLog.w("Node", "reattach refused: target is inside the moving subtree")
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.reattach_cycle)) }
                }

                else -> {
                    val oldLinks = NodeStore.linksIn(reattach)
                    val type = oldLinks.firstOrNull()?.type ?: LinkCatalog.DEFAULT_KEY
                    oldLinks.forEach { NodeStore.removeLink(context, it) }
                    val outcome = NodeStore.addLink(context, node.id, reattach, type, null)
                    if (outcome == "ok") {
                        val added = NodeStore.linksOut(node.id).lastOrNull()
                        selectedId = reattach
                        AppLog.i(
                            "Node",
                            "reattached '" + (moving?.name ?: reattach) + "' under '" + node.name +
                                "' (replaced " + oldLinks.size + " incoming link(s))",
                        )
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(
                                    R.string.reattached,
                                    moving?.name ?: "?",
                                    node.name,
                                ),
                                actionLabel = context.getString(R.string.undo),
                                withDismissAction = true,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                if (added != null) NodeStore.removeLink(context, added)
                                oldLinks.forEach { NodeStore.restoreLink(context, it) }
                                AppLog.i("Node", "reattach undone")
                            }
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.link_exists)) }
                    }
                }
            }
        } else if (from != null) {
            connectFrom = null
            val label = pendingLabel
            when (NodeStore.addLink(context, from, node.id, pendingType.key, label)) {
                "ok" -> {
                    val fromName = NodeStore.nameOf(from)
                    val phrase = LinkCatalog.displayLabel(pendingType, label)
                    AppLog.i("Board", "linked: '$fromName' $phrase '${node.name}'")
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.connected_ok, fromName, node.name),
                        )
                    }
                }

                "exists" -> {
                    AppLog.i("Board", "link skipped: that phrase already exists between these nodes")
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.link_exists)) }
                }

                else -> {
                    AppLog.w("Board", "link rejected: " + node.id)
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.link_self)) }
                }
            }
            pendingLabel = null
            pendingType = LinkCatalog.default
        } else {
            selectedId = node.id
            selectedLinkId = null
            AppLog.i("Board", "node selected: " + node.name)
        }
    }

    // The auto-layout, measured from the board's origin.
    //
    // Arrange puts the centre of the first central node exactly on board (0, 0) and grows the map
    // from there, so the result depends on the origin and the branch order only — never on where the
    // nodes happened to be dragged. That is what makes it repeatable: pressing it twice cannot drift.
    // Nodes that belong to no tree are not touched, and the previous positions are kept for Undo.
    val arrangeNow: () -> Unit = {
        val ids = nodes.map { it.id }
        val roots = NodeStore.rootIds(kind).filter { it in ids }
        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = roots,
            direction = direction,
            spacing = spacing,
            originX = BoardSpace.ORIGIN_X,
            originY = BoardSpace.ORIGIN_Y,
            anchorRootAtOrigin = true,
        )
        if (positions.isEmpty()) {
            AppLog.i("Board", "arrange: nothing to arrange")
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.arrange_nothing)) }
        } else {
            val before = nodes.associate { it.id to LayoutPoint(it.x, it.y) }
            NodeStore.applyPositions(context, positions)
            // Follow the result: the map now sits at the origin, so the view goes there too.
            board.centreOnWorld(BoardSpace.ORIGIN_X, BoardSpace.ORIGIN_Y)
            val anchorName = StructureLayout
                .resolveRoots(nodes, links, roots)
                .firstOrNull()
                ?.let { NodeStore.nameOf(it) }
                ?: "?"
            AppLog.i(
                "Board",
                "arranged " + positions.size + " node(s) as " + direction.key + " / " + spacing.key +
                    " anchored on the origin, central node: '" + anchorName + "'",
            )
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.arrange_done, anchorName, positions.size),
                    actionLabel = context.getString(R.string.undo),
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    NodeStore.applyPositions(context, before)
                    AppLog.i("Board", "arrange undone")
                }
            }
        }
    }

    // ---- the node action square ---------------------------------------------
    //
    // Every branch below is one entry of NodeActionCatalog, so the square, the Help screen and the
    // planned model layer all describe the same set of operations, with the same ids.

    fun snack(text: String) {
        scope.launch { snackbarHostState.showSnackbar(text) }
    }

    fun removeWithUndo(node: IdeaNode, message: String) {
        val removed = NodeStore.remove(context, node.id)
        selectedId = null
        panel = null
        if (removed != null) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = context.getString(R.string.undo),
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    NodeStore.restore(context, removed.first, removed.second)
                    AppLog.i("Node", "removal undone: '" + removed.first.name + "'")
                }
            }
        }
    }

    /** A new node lands just below-right of its reference, so it is visible without an Arrange. */
    fun createLinkedNode(reference: IdeaNode, asSibling: Boolean) {
        val parent = if (asSibling) NodeStore.parentOf(reference.id) else reference
        if (parent == null) {
            AppLog.w("Node", "sibling refused: '" + reference.name + "' has no parent")
            snack(context.getString(R.string.no_parent_for_sibling))
            return
        }
        val id = NodeStore.newId()
        val now = System.currentTimeMillis()
        NodeStore.add(
            context,
            IdeaNode(
                id = id,
                kind = kind,
                name = context.getString(R.string.node_new_default),
                priority = 3,
                description = "",
                picUri = null,
                createdAt = now,
                x = reference.x + StructureLayout.widthOf(reference) + 60f,
                y = reference.y + StructureLayout.heightOf(reference) + 24f,
                updatedAt = now,
            ),
        )
        NodeStore.addLink(context, parent.id, id, LinkCatalog.DEFAULT_KEY, null)
        selectedId = id
        // Name it straight away: a node called "new node" is not a finished thought.
        textDialogFor = id
        AppLog.i(
            "Node",
            (if (asSibling) "sibling" else "child") + " created: id=" + id + " under '" + parent.name + "'",
        )
    }

    val runNodeAction: (String) -> Unit = action@{ key ->
        val node = selectedId?.let { NodeStore.byId(it) } ?: return@action
        AppLog.i("Node", "action " + key + " on '" + node.name + "' (" + node.id + ")")
        when (key) {
            NodeActionCatalog.TEXT -> textDialogFor = node.id
            NodeActionCatalog.NOTES -> notesDialogFor = node.id
            NodeActionCatalog.IMAGE -> {
                if (node.picUri == null) {
                    imageTarget = node.id
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                } else {
                    pictureDialogFor = node.id
                }
            }

            NodeActionCatalog.CONNECT -> panel = Panel.Picker(node.id)
            NodeActionCatalog.MORE -> panel = Panel.NodeInfo(node.id)

            NodeActionCatalog.COPY -> {
                NodeClipboard.copy(node)
                snack(context.getString(R.string.copied_node, node.name))
            }

            NodeActionCatalog.CUT -> {
                NodeClipboard.cut(node)
                removeWithUndo(node, context.getString(R.string.cut_node, node.name))
            }

            NodeActionCatalog.PASTE -> {
                val source = NodeClipboard.content
                if (source == null) {
                    snack(context.getString(R.string.clipboard_empty))
                } else {
                    val now = System.currentTimeMillis()
                    val copy = source.copy(
                        id = NodeStore.newId(),
                        kind = kind,
                        x = board.screenToWorldX(board.viewportW / 2f) - StructureLayout.widthOf(source) / 2f,
                        y = board.screenToWorldY(board.viewportH / 2f) - StructureLayout.heightOf(source) / 2f,
                        isRoot = false,
                        collapsed = false,
                        createdAt = now,
                        updatedAt = now,
                    )
                    NodeStore.add(context, copy)
                    selectedId = copy.id
                    snack(context.getString(R.string.pasted_node, copy.name))
                }
            }

            NodeActionCatalog.DELETE -> removeWithUndo(node, context.getString(R.string.node_deleted))

            NodeActionCatalog.CHILD -> createLinkedNode(node, asSibling = false)
            NodeActionCatalog.SIBLING -> createLinkedNode(node, asSibling = true)

            NodeActionCatalog.DUPLICATE -> {
                val now = System.currentTimeMillis()
                val copy = node.copy(
                    id = NodeStore.newId(),
                    x = node.x + 32f,
                    y = node.y + 32f,
                    isRoot = false,
                    collapsed = false,
                    createdAt = now,
                    updatedAt = now,
                )
                NodeStore.add(context, copy)
                selectedId = copy.id
                snack(context.getString(R.string.duplicated_node, copy.name))
            }

            NodeActionCatalog.REATTACH -> {
                reattachFrom = node.id
                selectedId = null
                AppLog.i("Node", "reattach mode started from '" + node.name + "'")
                snack(context.getString(R.string.reattach_hint))
            }

            NodeActionCatalog.RESIZE,
            NodeActionCatalog.COLOR,
            NodeActionCatalog.SHAPE,
            -> panel = Panel.Style(node.id)

            else -> AppLog.w("Node", "no handler for action " + key)
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
                    // Back, then the project settings — the settings button sits at the top-left
                    // of the board, as ordered.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                            )
                        }
                        IconButton(
                            onClick = {
                                panel = Panel.Settings
                                AppLog.i("Board", "project settings opened")
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.cd_project_settings),
                            )
                        }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (viewMode == ViewMode.List) {
                    BoardListView(
                        nodes = nodes,
                        selectedId = selectedId,
                        onSelect = onNodeTap,
                        onOpenLink = { link ->
                            panel = Panel.LinkInfo(link.id)
                            AppLog.i("Board", "link opened from list: " + link.id)
                        },
                        onGoToBoard = { viewMode = ViewMode.Board },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    val scheme = MaterialTheme.colorScheme
                    val boardColors = boardColorsOf(palette)

                    // Resolve every link once per frame: screen endpoints for drawing,
                    // board-space midpoint for the label chip.
                    val renderEdges = links.mapNotNull { link ->
                        val a = NodeStore.byId(link.fromId)
                        val b = NodeStore.byId(link.toId)
                        if (a == null || b == null) {
                            null
                        } else {
                            val type = LinkCatalog.byKey(link.type)
                            val wA = StructureLayout.widthOf(a)
                            val hA = StructureLayout.heightOf(a)
                            val wB = StructureLayout.widthOf(b)
                            val hB = StructureLayout.heightOf(b)
                            val aCenter = Offset(
                                board.worldToScreenX(a.x + wA / 2f),
                                board.worldToScreenY(a.y + hA / 2f),
                            )
                            val bCenter = Offset(
                                board.worldToScreenX(b.x + wB / 2f),
                                board.worldToScreenY(b.y + hB / 2f),
                            )
                            RenderEdge(
                                linkId = link.id,
                                type = type,
                                colorRole = link.colorRole(),
                                // Each end is trimmed against the size of ITS OWN card, so a resized
                                // node still gets its lines stopped at the edge.
                                start = trimToNodeEdge(
                                    from = bCenter,
                                    to = aCenter,
                                    halfW = wA / 2f * board.scale,
                                    halfH = hA / 2f * board.scale,
                                ),
                                end = trimToNodeEdge(
                                    from = aCenter,
                                    to = bCenter,
                                    halfW = wB / 2f * board.scale,
                                    halfH = hB / 2f * board.scale,
                                ),
                                midBoard = Offset(
                                    (a.x + wA / 2f + b.x + wB / 2f) / 2f,
                                    (a.y + hA / 2f + b.y + hB / 2f) / 2f,
                                ),
                                label = if (type.key == LinkCatalog.DEFAULT_KEY && link.label == null) {
                                    null
                                } else {
                                    LinkCatalog.displayLabel(type, link.label)
                                },
                            )
                        }
                    }
                    val edgeDraws = renderEdges.map { edge ->
                        edge to linkColorOf(scheme, edge.colorRole)
                    }
                    val edgesState = rememberUpdatedState(renderEdges)
                    // A finger is ~40dp wide, so the hit band is a dp value, not a pixel count —
                    // 26 raw pixels would be under 10dp on a 3x display and nearly untappable.
                    val tapSlopPx = with(LocalDensity.current) { LINK_TAP_SLOP_DP.dp.toPx() }

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
                                drawBoardBackground(
                                    pattern = pattern,
                                    scale = board.scale,
                                    offsetX = board.offsetX,
                                    offsetY = board.offsetY,
                                    colors = boardColors,
                                )
                                // The floor marker: board (0, 0). Drawn under the links and the
                                // cards, so it reads as part of the surface, not as an object.
                                if (showOrigin) {
                                    drawBoardOrigin(
                                        scale = board.scale,
                                        offsetX = board.offsetX,
                                        offsetY = board.offsetY,
                                        colors = boardColors,
                                    )
                                }
                                // The selected link gets a halo underneath, so a tapped line is
                                // obvious even when several relations cross the same area.
                                edgeDraws.firstOrNull { it.first.linkId == selectedLinkId }?.let { (edge, color) ->
                                    drawLine(
                                        color = color.copy(alpha = 0.35f),
                                        start = edge.start,
                                        end = edge.end,
                                        strokeWidth = 10f * board.scale,
                                    )
                                }
                                edgeDraws.forEach { (edge, color) ->
                                    drawTypedLink(edge.start, edge.end, edge.type, color, board.scale)
                                }
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
                                // A tap either hits a link (and opens the inspector) or clears the
                                // selection. Links are drawn behind the cards, so a tap on a card
                                // never reaches this handler. The edges are read through
                                // rememberUpdatedState so panning and zooming never restart this
                                // gesture detector mid-tap.
                                detectTapGestures { position ->
                                    val hit = edgesState.value
                                        .map { edge ->
                                            edge to distanceToSegment(position, edge.start, edge.end)
                                        }
                                        .filter { it.second <= tapSlopPx }
                                        .minByOrNull { it.second }
                                    if (hit != null) {
                                        selectedLinkId = hit.first.linkId
                                        selectedId = null
                                        panel = Panel.LinkInfo(hit.first.linkId)
                                        AppLog.i(
                                            "Board",
                                            "link tapped on board: " + hit.first.linkId +
                                                " (distance " + hit.second.toInt() + "px)",
                                        )
                                    } else {
                                        selectedLinkId = null
                                        selectedId = null
                                    }
                                }
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
                            if (showLabels) {
                                renderEdges.forEach { edge ->
                                    if (edge.label != null) {
                                        LinkLabelChip(
                                            text = edge.label,
                                            boardX = edge.midBoard.x,
                                            boardY = edge.midBoard.y,
                                            selected = edge.linkId == selectedLinkId,
                                            onClick = {
                                                selectedLinkId = edge.linkId
                                                panel = Panel.LinkInfo(edge.linkId)
                                                AppLog.i("Board", "link opened from its label: " + edge.linkId)
                                            },
                                        )
                                    }
                                }
                            }
                            nodes.forEach { node ->
                                BoardNode(
                                    node = node,
                                    selected = node.id == selectedId,
                                    board = board,
                                    hiddenBelow = if (node.collapsed) NodeStore.subtreeOf(node.id).size else 0,
                                    onTap = { onNodeTap(node) },
                                    onOpenLinks = {
                                        selectedId = node.id
                                        panel = Panel.NodeInfo(node.id)
                                        AppLog.i("Board", "node panel opened for '" + node.name + "'")
                                    },
                                    onDragEnd = { NodeStore.persist(context) },
                                )
                            }
                        }

                        // Minimap — only while travelling, never interactive
                        if (minimapVisible && showMinimapPref) {
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

                // Pending modes replace the action square with one instruction, so the board never asks
                // for two things at once.
                if (!formOpen) {
                    if (reattachFrom != null) {
                        HintBar(
                            text = stringResource(R.string.reattach_hint),
                            onCancel = {
                                reattachFrom = null
                                AppLog.i("Board", "reattach cancelled")
                                snack(context.getString(R.string.reattach_cancelled))
                            },
                        )
                    } else if (connectFrom != null) {
                        HintBar(
                            text = stringResource(
                                R.string.link_choose_hint,
                                LinkCatalog.displayLabel(pendingType, pendingLabel),
                            ),
                            onCancel = {
                                connectFrom = null
                                pendingLabel = null
                                pendingType = LinkCatalog.default
                                AppLog.i("Board", "link cancelled")
                                snack(context.getString(R.string.connect_cancelled))
                            },
                        )
                    }
                }

                // Bottom toolbar: board / arrange · zoom · list
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
                                icon = Icons.Outlined.AutoFixHigh,
                                contentDescription = stringResource(R.string.tool_arrange),
                                enabled = onBoard,
                                onClick = arrangeNow,
                            )
                            ToolButton(
                                icon = Icons.Outlined.Adjust,
                                contentDescription = stringResource(R.string.tool_origin),
                                enabled = onBoard,
                                onClick = {
                                    board.centreOnWorld(BoardSpace.ORIGIN_X, BoardSpace.ORIGIN_Y)
                                    AppLog.i("Board", "view centred on the origin")
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.origin_here),
                                        )
                                    }
                                },
                            )
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

            // ---- the node action square: four bars around the selected node ----
            //
            // It is drawn in screen space, so the buttons keep a real touch target at every zoom,
            // and it steps aside while a panel, a dialog or a pending mode owns the screen.
            val barsNode = selectedId?.let { NodeStore.byId(it) }
            if (barsNode != null &&
                !formOpen &&
                panel == null &&
                connectFrom == null &&
                reattachFrom == null &&
                textDialogFor == null &&
                notesDialogFor == null &&
                detailsFor == null &&
                pictureDialogFor == null &&
                viewMode == ViewMode.Board &&
                !hidden.contains(barsNode.id)
            ) {
                val nodeW = StructureLayout.widthOf(barsNode)
                val nodeH = StructureLayout.heightOf(barsNode)
                NodeActionBars(
                    centreX = board.worldToScreenX(barsNode.x + nodeW / 2f),
                    centreY = board.worldToScreenY(barsNode.y + nodeH / 2f),
                    nodeWidthPx = nodeW * board.scale,
                    nodeHeightPx = nodeH * board.scale,
                    viewportWidth = board.viewportW,
                    viewportHeight = board.viewportH,
                    clipboardHasContent = NodeClipboard.hasContent,
                    onAction = runNodeAction,
                )
            }

            // ---- panels: exactly one can be open, and each one covers the board ----
            when (val open = panel) {
                is Panel.NodeInfo -> NodeStore.byId(open.id)?.let { node ->
                    NodePanel(
                        node = node,
                        onClose = { panel = null },
                        onOpenLink = { link -> panel = Panel.LinkInfo(link.id) },
                        onAddLink = { panel = Panel.Picker(node.id) },
                        onReorder = { link, delta ->
                            NodeStore.reorderLink(context, link.id, delta)
                        },
                        onAutoOrder = { mode ->
                            NodeStore.autoOrder(context, node.id, mode)
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.order_applied))
                            }
                        },
                        onToggleRoot = {
                            val next = !node.isRoot
                            NodeStore.setRoot(context, node.id, next)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(
                                        if (next) R.string.central_set else R.string.central_cleared,
                                        node.name,
                                    ),
                                )
                            }
                        },
                        onToggleCollapse = {
                            val next = !node.collapsed
                            val count = NodeStore.subtreeOf(node.id).size
                            NodeStore.setCollapsed(context, node.id, next)
                            if (next && count > 0) {
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.collapsed_hidden, count),
                                        actionLabel = context.getString(R.string.undo),
                                        withDismissAction = true,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        NodeStore.setCollapsed(context, node.id, false)
                                        AppLog.i("Board", "collapse undone for '" + node.name + "'")
                                    }
                                }
                            }
                        },
                        onNudge = { dx, dy -> NodeStore.moveSubtree(context, node.id, dx, dy) },
                        onArrange = arrangeNow,
                        onDetails = { detailsFor = node.id },
                        onDelete = {
                            val removed = NodeStore.remove(context, node.id)
                            panel = null
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
                                    }
                                }
                            }
                        },
                    )
                }

                is Panel.Picker -> NodeStore.byId(open.id)?.let { node ->
                    LinkPickerPanel(
                        fromNode = node,
                        onPick = { type ->
                            pendingType = type
                            pendingLabel = null
                            connectFrom = node.id
                            panel = null
                            selectedId = null
                            AppLog.i("Board", "link type chosen: " + type.key)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.link_choose_hint, type.name),
                                )
                            }
                        },
                        onCustom = { phrase ->
                            pendingType = LinkCatalog.default
                            pendingLabel = phrase
                            connectFrom = node.id
                            panel = null
                            selectedId = null
                            AppLog.i("Board", "custom link phrase: " + phrase)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.link_choose_hint, phrase),
                                )
                            }
                        },
                        onOpenLink = { link -> panel = Panel.LinkInfo(link.id) },
                        onClose = { panel = null },
                    )
                }

                is Panel.LinkInfo -> NodeStore.linkById(open.id)?.let { link ->
                    LinkInspectorPanel(
                        link = link,
                        onUpdate = { updated ->
                            when (NodeStore.updateLink(context, updated)) {
                                "ok" -> AppLog.i("Board", "link updated: " + updated.id)
                                "exists" -> scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.link_exists))
                                }

                                else -> AppLog.w("Board", "link update failed: link no longer exists")
                            }
                        },
                        onOpenOther = { other -> panel = Panel.LinkInfo(other.id) },
                        onDelete = {
                            val removed = NodeStore.removeLink(context, link)
                            panel = null
                            selectedLinkId = null
                            if (removed != null) {
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.link_removed),
                                        actionLabel = context.getString(R.string.undo),
                                        withDismissAction = true,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        NodeStore.restoreLink(context, removed)
                                        AppLog.i("Board", "link removal undone")
                                    }
                                }
                            }
                        },
                        onClose = { panel = null },
                    )
                }

                is Panel.Style -> NodeStore.byId(open.id)?.let { node ->
                    NodeStylePanel(
                        node = node,
                        onSize = { value ->
                            NodeStore.updateNode(context, node.copy(size = value))
                            AppLog.i("Style", "size -> " + value)
                        },
                        onColor = { key ->
                            NodeStore.updateNode(context, node.copy(color = key))
                            AppLog.i("Style", "colour -> " + (key ?: "default"))
                        },
                        onShape = { key ->
                            NodeStore.updateNode(context, node.copy(shape = key))
                            AppLog.i("Style", "shape -> " + key)
                        },
                        onClose = { panel = null },
                    )
                }

                Panel.Settings -> ProjectSettingsPanel(
                    pattern = pattern,
                    palette = palette,
                    direction = direction,
                    spacing = spacing,
                    showLabels = showLabels,
                    showMinimap = showMinimapPref,
                    showOrigin = showOrigin,
                    rootCount = NodeStore.rootIds(kind).size,
                    hiddenCount = hidden.size,
                    onPattern = { BoardPrefs.setPattern(context, it.key) },
                    onPalette = { BoardPrefs.setPalette(context, it.key) },
                    onDirection = { BoardPrefs.setDirection(context, it.key) },
                    onSpacing = { BoardPrefs.setSpacing(context, it.key) },
                    onShowLabels = { BoardPrefs.setShowLinkLabels(context, it) },
                    onShowMinimap = { BoardPrefs.setShowMinimap(context, it) },
                    onShowOrigin = { BoardPrefs.setShowOrigin(context, it) },
                    onArrange = arrangeNow,
                    onCollapseAll = {
                        NodeStore.setCollapsedAll(context, true)
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.collapse_all_done)) }
                    },
                    onExpandAll = {
                        NodeStore.setCollapsedAll(context, false)
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.expand_all_done)) }
                    },
                    onClose = { panel = null },
                )

                null -> Unit
            }

            // ---- node dialogs: at most one at a time, driven by the action square ----
            val textNode = textDialogFor?.let { NodeStore.byId(it) }
            if (textNode != null) {
                NodeTextDialog(
                    initial = textNode.name,
                    onSave = { name ->
                        if (name.isNotEmpty()) {
                            NodeStore.updateNode(context, textNode.copy(name = name))
                            snack(context.getString(R.string.node_renamed, name))
                        }
                        textDialogFor = null
                    },
                    onDismiss = { textDialogFor = null },
                )
            }

            val notesNode = notesDialogFor?.let { NodeStore.byId(it) }
            if (notesNode != null) {
                NodeNotesDialog(
                    initial = notesNode.description,
                    onSave = { text ->
                        NodeStore.updateNode(context, notesNode.copy(description = text))
                        snack(context.getString(R.string.notes_saved))
                        notesDialogFor = null
                    },
                    onDismiss = { notesDialogFor = null },
                )
            }

            val detailsNode = detailsFor?.let { NodeStore.byId(it) }
            if (detailsNode != null) {
                NodeDetailsDialog(
                    title = detailsNode.name,
                    body = NodeStore.describe(detailsNode.id),
                    onCopy = {
                        clipboard.setText(AnnotatedString(NodeStore.describe(detailsNode.id)))
                        AppLog.i("Node", "details copied for '" + detailsNode.name + "'")
                        snack(context.getString(R.string.details_copied))
                    },
                    onDismiss = { detailsFor = null },
                )
            }

            val pictureNode = pictureDialogFor?.let { NodeStore.byId(it) }
            if (pictureNode != null) {
                NodePictureDialog(
                    onReplace = {
                        pictureDialogFor = null
                        imageTarget = pictureNode.id
                        pickImage.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onRemove = {
                        NodeStore.updateNode(context, pictureNode.copy(picUri = null))
                        pictureDialogFor = null
                        snack(context.getString(R.string.picture_removed))
                    },
                    onDismiss = { pictureDialogFor = null },
                )
            }
        }
    }
}

/**
 * The travelling minimap: a window that appears while you move around the board.
 * Shows every node, the connections, the current viewport, and your finger as a red dot.
 * Deliberately not interactive.
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
        val w = StructureLayout.widthOf(n)
        val h = StructureLayout.heightOf(n)
        if (n.x < minX) minX = n.x
        if (n.x + w > maxX) maxX = n.x + w
        if (n.y < minY) minY = n.y
        if (n.y + h > maxY) maxY = n.y + h
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
        CanvasBox {
            val sx = size.width / worldW
            val sy = size.height / worldH

            links.forEach { link ->
                val a = NodeStore.byId(link.fromId) ?: return@forEach
                val b = NodeStore.byId(link.toId) ?: return@forEach
                drawLine(
                    color = linkColor,
                    start = Offset(
                        (a.x + StructureLayout.widthOf(a) / 2f - minX) * sx,
                        (a.y + StructureLayout.heightOf(a) / 2f - minY) * sy,
                    ),
                    end = Offset(
                        (b.x + StructureLayout.widthOf(b) / 2f - minX) * sx,
                        (b.y + StructureLayout.heightOf(b) / 2f - minY) * sy,
                    ),
                    strokeWidth = 1.5f,
                )
            }

            nodes.forEach { n ->
                drawRect(
                    color = nodeColor,
                    topLeft = Offset((n.x - minX) * sx, (n.y - minY) * sy),
                    size = Size(StructureLayout.widthOf(n) * sx, StructureLayout.heightOf(n) * sy),
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

/** The relation name, pinned to the middle of its line. Moves and scales with the board. */
@Composable
private fun LinkLabelChip(
    text: String,
    boardX: Float,
    boardY: Float,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var chipSize by remember { mutableStateOf(IntSize.Zero) }
    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    boardX.roundToInt() - chipSize.width / 2,
                    boardY.roundToInt() - chipSize.height / 2,
                )
            }
            .onSizeChanged { chipSize = it }
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Radius.small),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs),
        )
    }
}

// The single-row node action bar (Links · Link · Delete) that shipped in v0.6.0/v0.7.0 was
// replaced in v0.8.0 by the four-bar action square — NodeActionBars — which is generated from
// NodeActionCatalog and carries every node operation, not just three.

@Composable
private fun HintBar(text: String, onCancel: () -> Unit) {
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
                text = text,
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
    hiddenBelow: Int,
    onTap: () -> Unit,
    onOpenLinks: () -> Unit,
    onDragEnd: () -> Unit,
) {
    val linkCount = NodeStore.linksOf(node.id).size
    val scheme = MaterialTheme.colorScheme
    // A colour override is a theme role blended into the card tone, so the card stays opaque and
    // the text keeps its contrast in both day and night mode.
    val role = node.color?.let { key -> LinkColor.values().firstOrNull { it.name.equals(key, true) } }
    val container = if (role == null) {
        scheme.surfaceVariant
    } else {
        lerp(scheme.surfaceVariant, themeRoleColor(scheme, role), 0.30f)
    }
    // A pill clips its own corners, so its text has to start further in.
    val pill = node.shape == NodeStyle.SHAPE_PILL
    val padH = if (pill) Spacing.xxl else Spacing.m
    val padV = if (pill) Spacing.l else Spacing.m

    Card(
        modifier = Modifier
            .offset { IntOffset(node.x.roundToInt(), node.y.roundToInt()) }
            .width(StructureLayout.widthOf(node).dp)
            .heightIn(min = StructureLayout.heightOf(node).dp)
            .pointerInput(node.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onOpenLinks() },
                )
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
        shape = nodeShapeOf(node.shape),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 0.dp),
        border = when {
            selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            node.isRoot -> BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
            else -> null
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = padH, vertical = padV)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (node.isRoot) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = stringResource(R.string.node_central),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(Sizes.iconSm),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                }
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
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
                if (linkCount > 0) {
                    Text(
                        text = linkCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (hiddenBelow > 0) {
                    Spacer(Modifier.width(Spacing.s))
                    Text(
                        text = stringResource(R.string.node_hidden_badge, hiddenBelow),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
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

/** Small wrapper so the minimap's canvas body reads as a plain DrawScope block. */
@Composable
private fun CanvasBox(content: DrawScope.() -> Unit) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.s),
    ) {
        content()
    }
}