package com.saeed.ideasmo.ui.board

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Adjust
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import com.saeed.ideasmo.core.model.FragmentBuilder
import com.saeed.ideasmo.core.model.IdeaNode
import com.saeed.ideasmo.core.model.LayoutDirection
import com.saeed.ideasmo.core.model.LayoutPoint
import com.saeed.ideasmo.core.model.LayoutSpacing
import com.saeed.ideasmo.core.model.LinkCatalog
import com.saeed.ideasmo.core.model.LinkColor
import com.saeed.ideasmo.core.model.LinkType
import com.saeed.ideasmo.core.model.NodeActionCatalog
import com.saeed.ideasmo.core.model.NodeClipboard
import com.saeed.ideasmo.core.model.NodeFragment
import com.saeed.ideasmo.core.model.NodeLink
import com.saeed.ideasmo.core.model.NodeStore
import com.saeed.ideasmo.core.model.NodeStyle
import com.saeed.ideasmo.core.model.StructureLayout
import com.saeed.ideasmo.core.model.UndoStack
import com.saeed.ideasmo.core.prefs.BoardPrefs
import com.saeed.ideasmo.core.util.copyPictureIntoAppStorage
import com.saeed.ideasmo.ui.components.NoticeBar
import com.saeed.ideasmo.ui.components.ToolButton
import com.saeed.ideasmo.ui.theme.Radius
import com.saeed.ideasmo.ui.theme.Sizes
import com.saeed.ideasmo.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MIN_SCALE = 0.25f
private const val MAX_SCALE = 2.0f
private const val ZOOM_STEP = 0.25f
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

/** A Copy / Cut / Duplicate that is waiting for the user to choose what travels with the node. */
private data class FragmentRequest(val action: String, val ids: List<String>)

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
 * The board: a 2D surface you pan with one finger and pinch to zoom, with nodes you can drag, typed
 * relations between them, an origin the whole structure is measured from, and a node action square
 * around the selected card.
 *
 * Behaviour that is worth knowing before reading the code:
 * - **Selection is a list.** One node selected shows the action square; two or more show the group
 *   bar. Long-pressing the empty board starts a rubber band; long-pressing a node toggles it in or
 *   out of the selection.
 * - **Back is a step, not an exit.** Back closes the deepest thing first — a dialog, a panel, a
 *   pending mode, a tool, the form, the selection — and only then leaves the board.
 * - **No floating messages.** Refusals and pending modes are shown in a strip that sits in the
 *   layout; anything destructive is recoverable through the always-visible Undo button.
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

    fun screenRectOf(node: IdeaNode): Rect {
        val left = worldToScreenX(node.x)
        val top = worldToScreenY(node.y)
        return Rect(
            left,
            top,
            left + StructureLayout.widthOf(node) * scale,
            top + StructureLayout.heightOf(node) * scale,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
    kind: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val board = remember { BoardState() }
    val clipboard = LocalClipboardManager.current

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

    // Selection is a list: one node shows the action square, several show the group bar.
    var selection by remember { mutableStateOf(listOf<String>()) }
    val selectedId: String? = selection.singleOrNull()

    var selectedLinkId by remember { mutableStateOf<String?>(null) }
    var panel by remember { mutableStateOf<Panel?>(null) }
    var connectFrom by remember { mutableStateOf<String?>(null) }
    var pendingType by remember { mutableStateOf(LinkCatalog.default) }
    var pendingLabel by remember { mutableStateOf<String?>(null) }
    var reattachFrom by remember { mutableStateOf<String?>(null) }
    var tool by remember { mutableStateOf(NodeTool.None) }
    var notice by remember { mutableStateOf<String?>(null) }
    var minimapVisible by remember { mutableStateOf(false) }
    var centeredOnce by remember { mutableStateOf(false) }

    // Rubber band, in screen pixels, while a long-press drag is running.
    var bandStart by remember { mutableStateOf<Offset?>(null) }
    var bandNow by remember { mutableStateOf<Offset?>(null) }

    // Dialogs: one at a time.
    var textDialogFor by remember { mutableStateOf<String?>(null) }
    var notesDialogFor by remember { mutableStateOf<String?>(null) }
    var detailsFor by remember { mutableStateOf<String?>(null) }
    var pictureDialogFor by remember { mutableStateOf<String?>(null) }
    var imageTarget by remember { mutableStateOf<String?>(null) }
    var fragmentRequest by remember { mutableStateOf<FragmentRequest?>(null) }

    fun clearModes() {
        tool = NodeTool.None
        notice = null
    }

    // ---- back is a step, not an exit ---------------------------------------
    BackHandler(
        enabled = fragmentRequest != null || textDialogFor != null || notesDialogFor != null ||
            detailsFor != null || pictureDialogFor != null || panel != null || connectFrom != null ||
            reattachFrom != null || tool != NodeTool.None || formOpen || selection.isNotEmpty(),
    ) {
        when {
            fragmentRequest != null -> fragmentRequest = null
            textDialogFor != null -> textDialogFor = null
            notesDialogFor != null -> notesDialogFor = null
            detailsFor != null -> detailsFor = null
            pictureDialogFor != null -> pictureDialogFor = null
            panel != null -> panel = null
            connectFrom != null -> {
                connectFrom = null
                pendingLabel = null
                pendingType = LinkCatalog.default
                notice = null
                AppLog.i("Board", "link cancelled by back")
            }

            reattachFrom != null -> {
                reattachFrom = null
                notice = null
                AppLog.i("Board", "reattach cancelled by back")
            }

            tool != NodeTool.None -> {
                tool = NodeTool.None
                AppLog.i("Board", "tool closed by back")
            }

            formOpen -> formOpen = false
            selection.isNotEmpty() -> {
                selection = emptyList()
                notice = null
                AppLog.i("Board", "selection cleared by back")
            }
        }
    }

    LaunchedEffect(board.gestureActive, board.gestureEndedAt) {
        if (board.gestureActive) {
            minimapVisible = true
        } else if (board.gestureEndedAt > 0L) {
            delay(MINIMAP_LINGER_MS)
            minimapVisible = false
        }
    }

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
                notice = context.getString(R.string.pic_pick_failed)
            } else if (target != null) {
                NodeStore.byId(target)?.let { node -> NodeStore.updateNode(context, node.copy(picUri = path)) }
                imageTarget = null
                notice = context.getString(R.string.picture_updated)
            } else {
                picPath = path
            }
        }
    }

    // ---- helpers -----------------------------------------------------------

    fun removeIdsWithUndo(ids: List<String>, label: String) {
        val bundle = NodeStore.removeNodes(context, ids) ?: return
        selection = emptyList()
        panel = null
        UndoStack.push(label) { NodeStore.restoreBundle(context, bundle) }
        AppLog.i("Board", label + ": removed " + bundle.nodes.size + " node(s)")
    }

    fun insertFragmentAt(fragment: NodeFragment, atX: Float, atY: Float, label: String): String? {
        val before = NodeStore.nodes.map { it.id }.toSet()
        val rootId = NodeStore.insertFragment(context, fragment, atX, atY, kind) ?: return null
        val inserted = NodeStore.nodes.map { it.id }.filter { it !in before }
        UndoStack.push(label) { NodeStore.removeNodes(context, inserted) }
        return rootId
    }

    /** The fragment for a set of nodes: the nodes plus the links that join them. */
    fun fragmentOfSelection(ids: List<String>): NodeFragment? =
        FragmentBuilder.buildFromSelection(ids, NodeStore.nodes, NodeStore.links)

    val createNode: () -> Unit = {
        val trimmed = nodeName.trim()
        if (trimmed.isEmpty()) {
            AppLog.w("Board", "create blocked: empty node name")
            notice = context.getString(R.string.node_name_required)
        } else {
            val wx = board.screenToWorldX(board.viewportW / 2f) - StructureLayout.NODE_W / 2f
            val wy = board.screenToWorldY(board.viewportH / 2f) - StructureLayout.NODE_H / 2f
            val id = NodeStore.newId()
            val now = System.currentTimeMillis()
            NodeStore.add(
                context,
                IdeaNode(
                    id = id,
                    kind = kind,
                    name = trimmed,
                    priority = priority,
                    description = description.trim(),
                    picUri = picPath,
                    createdAt = now,
                    x = wx,
                    y = wy,
                    updatedAt = now,
                ),
            )
            UndoStack.push(context.getString(R.string.undo_create, trimmed)) {
                NodeStore.removeNodes(context, listOf(id))
            }
            selection = listOf(id)
            nodeName = ""
            description = ""
            priority = 3
            picPath = null
            formOpen = false
        }
    }

    // ---- tapping a node ----------------------------------------------------

    val onNodeTap: (IdeaNode) -> Unit = { node ->
        val reattach = reattachFrom
        val from = connectFrom
        if (reattach != null) {
            reattachFrom = null
            notice = null
            val moving = NodeStore.byId(reattach)
            when {
                reattach == node.id -> {
                    AppLog.w("Node", "reattach refused: same node")
                    notice = context.getString(R.string.link_self)
                }

                NodeStore.subtreeOf(reattach).contains(node.id) -> {
                    AppLog.w("Node", "reattach refused: target is inside the moving subtree")
                    notice = context.getString(R.string.reattach_cycle)
                }

                else -> {
                    val oldLinks = NodeStore.linksIn(reattach)
                    val type = oldLinks.firstOrNull()?.type ?: LinkCatalog.DEFAULT_KEY
                    oldLinks.forEach { NodeStore.removeLink(context, it) }
                    val outcome = NodeStore.addLink(context, node.id, reattach, type, null)
                    if (outcome == "ok") {
                        val added = NodeStore.linksOut(node.id).lastOrNull()
                        selection = listOf(reattach)
                        UndoStack.push(context.getString(R.string.undo_reattach, moving?.name ?: "?")) {
                            if (added != null) NodeStore.removeLink(context, added)
                            oldLinks.forEach { NodeStore.restoreLink(context, it) }
                        }
                        AppLog.i("Node", "reattached '" + (moving?.name ?: reattach) + "' under '" + node.name + "'")
                    } else {
                        notice = context.getString(R.string.link_exists)
                    }
                }
            }
        } else if (from != null) {
            connectFrom = null
            val label = pendingLabel
            when (NodeStore.addLink(context, from, node.id, pendingType.key, label)) {
                "ok" -> {
                    val fromName = NodeStore.nameOf(from)
                    val added = NodeStore.linksOut(from).lastOrNull()
                    if (added != null) {
                        UndoStack.push(context.getString(R.string.undo_link)) {
                            NodeStore.removeLink(context, added)
                        }
                    }
                    AppLog.i("Board", "linked: '$fromName' -> '${node.name}'")
                }

                "exists" -> notice = context.getString(R.string.link_exists)
                else -> notice = context.getString(R.string.link_self)
            }
            pendingLabel = null
            pendingType = LinkCatalog.default
        } else {
            // Selection rules: tap the only selected node to drop it, tap inside a group to toggle,
            // otherwise start a new single selection.
            selection = when {
                selection.size == 1 && selection.first() == node.id -> emptyList()
                selection.size >= 2 && selection.contains(node.id) -> selection - node.id
                selection.size >= 2 -> selection + node.id
                else -> listOf(node.id)
            }
            selectedLinkId = null
            clearModes()
            AppLog.i("Board", "selection: " + selection.size + " node(s)")
        }
    }

    val onNodeLongPress: (IdeaNode) -> Unit = { node ->
        selection = if (selection.contains(node.id)) selection - node.id else selection + node.id
        tool = NodeTool.None
        AppLog.i("Board", "long-press toggled '" + node.name + "', selection: " + selection.size)
    }

    // ---- arranging ---------------------------------------------------------

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
            notice = context.getString(R.string.arrange_nothing)
        } else {
            val before = nodes.associate { it.id to LayoutPoint(it.x, it.y) }
            NodeStore.applyPositions(context, positions)
            board.centreOnWorld(BoardSpace.ORIGIN_X, BoardSpace.ORIGIN_Y)
            val anchorName = StructureLayout.resolveRoots(nodes, links, roots)
                .firstOrNull()?.let { NodeStore.nameOf(it) } ?: "?"
            UndoStack.push(context.getString(R.string.undo_arrange)) {
                NodeStore.applyPositions(context, before)
            }
            AppLog.i("Board", "arranged " + positions.size + " node(s), central: '" + anchorName + "'")
        }
    }

    // ---- the node action square -------------------------------------------
fun runFragmentAction(actionKey: String, ids: List<String>, withLinks: Boolean, withSubLinks: Boolean) {
        val fragment = if (ids.size > 1) {
            fragmentOfSelection(ids)
        } else {
            NodeStore.fragmentOf(ids.first(), withLinks, withSubLinks)
        } ?: return
        when (actionKey) {
            NodeActionCatalog.COPY -> {
                NodeClipboard.copy(fragment)
                notice = context.getString(R.string.copied_node, fragment.nodes.size)
            }

            NodeActionCatalog.CUT -> {
                NodeClipboard.cut(fragment)
                removeIdsWithUndo(
                    fragment.nodes.map { it.id },
                    context.getString(R.string.undo_cut, fragment.nodes.size),
                )
            }

            NodeActionCatalog.DUPLICATE -> {
                val root = NodeStore.byId(ids.first())
                if (root != null) {
                    insertFragmentAt(
                        fragment,
                        root.x + 32f,
                        root.y + 32f,
                        context.getString(R.string.undo_duplicate),
                    )?.let { selection = listOf(it) }
                }
            }
        }
    }

    fun fragmentOrRun(actionKey: String, ids: List<String>) {
        val node = NodeStore.byId(ids.first()) ?: return
        val children = NodeStore.childrenOf(node.id).size
        val deeper = (NodeStore.subtreeOf(node.id).size - children).coerceAtLeast(0)
        val label = when (actionKey) {
            NodeActionCatalog.COPY -> context.getString(R.string.act_copy)
            NodeActionCatalog.CUT -> context.getString(R.string.act_cut)
            else -> context.getString(R.string.act_duplicate)
        }
        if (children == 0 || ids.size > 1) {
            // Nothing to choose: a leaf node, or a group where the selection is the fragment.
            runFragmentAction(actionKey, ids, false, false)
        } else {
            fragmentRequest = FragmentRequest(actionKey, ids)
            AppLog.i("Node", "fragment options for " + actionKey + " on '" + node.name +
                "' (" + deeper + " deeper node(s))")
        }
        if (label.isEmpty()) AppLog.w("Node", "empty action label")
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
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                } else {
                    pictureDialogFor = node.id
                }
            }

            NodeActionCatalog.CONNECT -> panel = Panel.Picker(node.id)
            NodeActionCatalog.MORE -> panel = Panel.NodeInfo(node.id)

            NodeActionCatalog.COPY,
            NodeActionCatalog.CUT,
            NodeActionCatalog.DUPLICATE,
            -> fragmentOrRun(key, listOf(node.id))

            NodeActionCatalog.PASTE -> {
                val source = NodeClipboard.content
                if (source == null) {
                    notice = context.getString(R.string.clipboard_empty)
                } else {
                    val atX = board.screenToWorldX(board.viewportW / 2f) - StructureLayout.widthOf(source.nodes.first()) / 2f
                    val atY = board.screenToWorldY(board.viewportH / 2f) - StructureLayout.heightOf(source.nodes.first()) / 2f
                    insertFragmentAt(source, atX, atY, context.getString(R.string.undo_paste))?.let {
                        selection = listOf(it)
                        notice = context.getString(R.string.pasted_node, source.nodes.size)
                    }
                }
            }

            NodeActionCatalog.DELETE -> removeIdsWithUndo(
                listOf(node.id),
                context.getString(R.string.undo_delete, node.name),
            )

            NodeActionCatalog.CHILD -> createLinkedNodeIn(
                context = context,
                kind = kind,
                reference = node,
                asSibling = false,
                onCreated = { id ->
                    selection = listOf(id)
                    textDialogFor = id
                },
                onNotice = { text -> notice = text },
            )

            NodeActionCatalog.SIBLING -> createLinkedNodeIn(
                context = context,
                kind = kind,
                reference = node,
                asSibling = true,
                onCreated = { id ->
                    selection = listOf(id)
                    textDialogFor = id
                },
                onNotice = { text -> notice = text },
            )

            NodeActionCatalog.REATTACH -> {
                reattachFrom = node.id
                selection = emptyList()
                tool = NodeTool.None
                notice = context.getString(R.string.reattach_hint)
            }

            NodeActionCatalog.RESIZE -> tool = NodeTool.Resize
            NodeActionCatalog.COLOR -> tool = NodeTool.Colour
            NodeActionCatalog.SHAPE -> tool = NodeTool.Shape

            else -> AppLog.w("Node", "no handler for action " + key)
        }
    }

    val panelWidth = (board.viewportW * 0.82f).coerceAtMost(340f)

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
                        onOpenLink = { link -> panel = Panel.LinkInfo(link.id) },
                        onGoToBoard = { viewMode = ViewMode.Board },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    val scheme = MaterialTheme.colorScheme
                    val boardColors = boardColorsOf(palette)

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
                    val edgeDraws = renderEdges.map { edge -> edge to linkColorOf(scheme, edge.colorRole) }
                    val edgesState = rememberUpdatedState(renderEdges)
                    val tapSlopPx = with(LocalDensity.current) { LINK_TAP_SLOP_DP.dp.toPx() }
                    val nodesState = rememberUpdatedState(nodes)
                    val bandState = rememberUpdatedState(Pair(bandStart, bandNow))

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
                                if (showOrigin) {
                                    drawBoardOrigin(
                                        scale = board.scale,
                                        offsetX = board.offsetX,
                                        offsetY = board.offsetY,
                                        colors = boardColors,
                                    )
                                }
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
                                    if (!board.draggingNode && bandState.value.first == null) {
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
                                detectTapGestures { position ->
                                    val hit = edgesState.value
                                        .map { edge -> edge to distanceToSegment(position, edge.start, edge.end) }
                                        .filter { it.second <= tapSlopPx }
                                        .minByOrNull { it.second }
                                    if (hit != null) {
                                        selectedLinkId = hit.first.linkId
                                        selection = emptyList()
                                        panel = Panel.LinkInfo(hit.first.linkId)
                                        AppLog.i("Board", "link tapped: " + hit.first.linkId)
                                    } else {
                                        selectedLinkId = null
                                        selection = emptyList()
                                        clearModes()
                                    }
                                }
                            }
                            .pointerInput(kind) {
                                // Long-press on the empty board starts a rubber band. It consumes the
                                // drag, so the board does not pan while the band is being drawn.
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val onNode = nodesState.value.any { board.screenRectOf(it).contains(down.position) }
                                    if (onNode) return@awaitEachGesture
                                    val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                                    bandStart = longPress.position
                                    bandNow = longPress.position
                                    drag(down.id) { change ->
                                        bandNow = change.position
                                        change.consume()
                                    }
                                    val a = bandStart
                                    val b = bandNow
                                    bandStart = null
                                    bandNow = null
                                    if (a != null && b != null) {
                                        val band = Rect(
                                            min(a.x, b.x),
                                            min(a.y, b.y),
                                            max(a.x, b.x),
                                            max(a.y, b.y),
                                        )
                                        val hit = nodesState.value
                                            .filter { band.overlaps(board.screenRectOf(it)) }
                                            .map { it.id }
                                        selection = hit
                                        tool = NodeTool.None
                                        notice = if (hit.isEmpty()) {
                                            context.getString(R.string.rubber_none)
                                        } else {
                                            null
                                        }
                                        AppLog.i("Board", "rubber band selected " + hit.size + " node(s)")
                                    }
                                }
                            }
                            .pointerInput(kind) {
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
                                                selection = emptyList()
                                                panel = Panel.LinkInfo(edge.linkId)
                                            },
                                        )
                                    }
                                }
                            }
                            nodes.forEach { node ->
                                val group = selection.size > 1 && selection.contains(node.id)
                                BoardNode(
                                    node = node,
                                    selected = selection.contains(node.id),
                                    board = board,
                                    hiddenBelow = if (node.collapsed) NodeStore.subtreeOf(node.id).size else 0,
                                    childCount = NodeStore.childrenOf(node.id).size,
                                    moveIds = if (group) selection else listOf(node.id),
                                    onTap = { onNodeTap(node) },
                                    onLongPress = { onNodeLongPress(node) },
                                    onToggleCollapse = {
                                        val next = !node.collapsed
                                        val count = NodeStore.subtreeOf(node.id).size
                                        NodeStore.setCollapsed(context, node.id, next)
                                        if (next && count > 0) {
                                            UndoStack.push(context.getString(R.string.undo_collapse, count)) {
                                                NodeStore.setCollapsed(context, node.id, false)
                                            }
                                        }
                                    },
                                    onDragEnd = { NodeStore.persist(context) },
                                )
                            }
                        }

                        if (bandStart != null && bandNow != null) {
                            RubberBand(start = bandStart!!, end = bandNow!!)
                        }

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
                                    pickImage.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                onAi = {
                                    AppLog.i("Board", "Ai button tapped (not built)")
                                    notice = context.getString(R.string.ai_not_built)
                                },
                                onCancel = { formOpen = false },
                                onCreate = createNode,
                            )
                        } else if (nodes.isEmpty()) {
                            NewNodeCentre(
                                modifier = Modifier.align(Alignment.Center),
                                onClick = { formOpen = true },
                            )
                        } else {
                            NewNodePill(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = Spacing.l),
                                onClick = { formOpen = true },
                            )
                        }
                    }
                }

                // A pending mode or a refusal is shown in the layout, never as a floating toast.
                if (notice != null && panel == null) {
                    NoticeBar(
                        text = notice!!,
                        tonal = connectFrom != null || reattachFrom != null,
                        actionLabel = if (connectFrom != null || reattachFrom != null) {
                            stringResource(R.string.cancel)
                        } else {
                            null
                        },
                        onAction = {
                            connectFrom = null
                            reattachFrom = null
                            pendingLabel = null
                            pendingType = LinkCatalog.default
                            notice = null
                        },
                        onDismiss = { notice = null },
                    )
                }

                // The group bar, when more than one node is selected.
                if (selection.size >= 2 && !formOpen && panel == null) {
                    BoardMultiBar(
                        count = selection.size,
                        onCopy = {
                            runFragmentAction(NodeActionCatalog.COPY, selection, true, false)
                        },
                        onCut = {
                            runFragmentAction(NodeActionCatalog.CUT, selection, true, false)
                        },
                        onDuplicate = {
                            runFragmentAction(NodeActionCatalog.DUPLICATE, selection, true, false)
                        },
                        onDelete = {
                            removeIdsWithUndo(
                                selection,
                                context.getString(R.string.undo_delete_many, selection.size),
                            )
                        },
                        onNudge = { dx, dy ->
                            selection.forEach { id -> NodeStore.moveSubtree(context, id, dx, dy) }
                        },
                        onClear = { selection = emptyList() },
                    )
                }

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
                            onClick = { viewMode = ViewMode.Board },
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
                                icon = Icons.AutoMirrored.Outlined.Undo,
                                contentDescription = stringResource(R.string.tool_undo),
                                enabled = UndoStack.canUndo,
                                onClick = {
                                    val label = UndoStack.undo()
                                    notice = if (label == null) {
                                        context.getString(R.string.undo_nothing)
                                    } else {
                                        context.getString(R.string.undone, label)
                                    }
                                },
                            )
                            ToolButton(
                                icon = Icons.Outlined.Adjust,
                                contentDescription = stringResource(R.string.tool_origin),
                                enabled = onBoard,
                                onClick = {
                                    board.centreOnWorld(BoardSpace.ORIGIN_X, BoardSpace.ORIGIN_Y)
                                    notice = context.getString(R.string.origin_here)
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
                                        notice = context.getString(R.string.view_reset)
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
                                },
                            )
                        }

                        ToolButton(
                            icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                            contentDescription = stringResource(R.string.tool_list),
                            active = viewMode == ViewMode.List,
                            onClick = { viewMode = ViewMode.List },
                        )
                    }
                }
            }

            // ---- the node action square: four bars around the single selected node ----
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
                fragmentRequest == null &&
                viewMode == ViewMode.Board &&
                !hidden.contains(barsNode.id)
            ) {
                val nodeW = StructureLayout.widthOf(barsNode)
                val nodeH = StructureLayout.heightOf(barsNode)
                val centreX = board.worldToScreenX(barsNode.x + nodeW / 2f)
                val centreY = board.worldToScreenY(barsNode.y + nodeH / 2f)
                NodeActionBars(
                    node = barsNode,
                    centreX = centreX,
                    centreY = centreY,
                    nodeWidthPx = nodeW * board.scale,
                    nodeHeightPx = nodeH * board.scale,
                    viewportWidth = board.viewportW,
                    viewportHeight = board.viewportH,
                    clipboardHasContent = NodeClipboard.hasContent,
                    tool = tool,
                    onAction = runNodeAction,
                    onTool = { tool = it },
                    onColor = { key ->
                        NodeStore.updateNode(context, barsNode.copy(color = key))
                        AppLog.i("Style", "colour -> " + (key ?: "default"))
                    },
                    onShape = { key ->
                        NodeStore.updateNode(context, barsNode.copy(shape = key))
                        AppLog.i("Style", "shape -> " + key)
                    },
                    onResizeDone = { tool = NodeTool.None },
                )

                if (tool == NodeTool.Resize) {
                    NodeResizeOverlay(
                        node = barsNode,
                        centreX = centreX,
                        centreY = centreY,
                        widthPx = nodeW * board.scale,
                        heightPx = nodeH * board.scale,
                        scale = board.scale,
                        viewportWidth = board.viewportW,
                        viewportHeight = board.viewportH,
                        onPreview = { w, h, x, y -> NodeStore.resize(barsNode.id, w, h, x, y) },
                        onCommit = {
                            val current = NodeStore.byId(barsNode.id)
                            if (current != null) {
                                val previous = barsNode
                                NodeStore.persist(context)
                                UndoStack.push(context.getString(R.string.undo_resize, current.name)) {
                                    NodeStore.updateNode(context, previous)
                                }
                                AppLog.i(
                                    "Style",
                                    "resized '" + current.name + "' to " + current.width.toInt() +
                                        " x " + current.height.toInt(),
                                )
                            }
                        },
                    )
                }
            }

            // ---- panels ----
            when (val open = panel) {
                is Panel.NodeInfo -> NodeStore.byId(open.id)?.let { node ->
                    NodePanel(
                        node = node,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(with(LocalDensity.current) { panelWidth.toDp() }),
                        onClose = { panel = null },
                        onOpenLink = { link -> panel = Panel.LinkInfo(link.id) },
                        onAddLink = { panel = Panel.Picker(node.id) },
                        onReorder = { link, delta -> NodeStore.reorderLink(context, link.id, delta) },
                        onAutoOrder = { mode ->
                            NodeStore.autoOrder(context, node.id, mode)
                            notice = context.getString(R.string.order_applied)
                        },
                        onToggleRoot = {
                            val next = !node.isRoot
                            NodeStore.setRoot(context, node.id, next)
                            notice = context.getString(
                                if (next) R.string.central_set else R.string.central_cleared,
                                node.name,
                            )
                        },
                        onToggleCollapse = {
                            val next = !node.collapsed
                            val count = NodeStore.subtreeOf(node.id).size
                            NodeStore.setCollapsed(context, node.id, next)
                            if (next && count > 0) {
                                UndoStack.push(context.getString(R.string.undo_collapse, count)) {
                                    NodeStore.setCollapsed(context, node.id, false)
                                }
                            }
                        },
                        onNudge = { dx, dy -> NodeStore.moveSubtree(context, node.id, dx, dy) },
                        onArrange = arrangeNow,
                        onDetails = { detailsFor = node.id },
                        onDelete = {
                            removeIdsWithUndo(
                                listOf(node.id),
                                context.getString(R.string.undo_delete, node.name),
                            )
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
                            selection = emptyList()
                            notice = context.getString(R.string.link_choose_hint, type.name)
                        },
                        onCustom = { phrase ->
                            pendingType = LinkCatalog.default
                            pendingLabel = phrase
                            connectFrom = node.id
                            panel = null
                            selection = emptyList()
                            notice = context.getString(R.string.link_choose_hint, phrase)
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
                                "exists" -> notice = context.getString(R.string.link_exists)
                                else -> AppLog.w("Board", "link update failed: link no longer exists")
                            }
                        },
                        onOpenOther = { other -> panel = Panel.LinkInfo(other.id) },
                        onDelete = {
                            val removed = NodeStore.removeLink(context, link)
                            panel = null
                            selectedLinkId = null
                            if (removed != null) {
                                UndoStack.push(context.getString(R.string.undo_link)) {
                                    NodeStore.restoreLink(context, removed)
                                }
                            }
                        },
                        onClose = { panel = null },
                    )
                }

                is Panel.Style -> NodeStore.byId(open.id)?.let { node ->
                    NodeStylePanel(
                        node = node,
                        onSize = { w, h -> NodeStore.updateNode(context, node.copy(w = w, h = h)) },
                        onColor = { key -> NodeStore.updateNode(context, node.copy(color = key)) },
                        onShape = { key -> NodeStore.updateNode(context, node.copy(shape = key)) },
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
                        notice = context.getString(R.string.collapse_all_done)
                    },
                    onExpandAll = {
                        NodeStore.setCollapsedAll(context, false)
                        notice = context.getString(R.string.expand_all_done)
                    },
                    onClose = { panel = null },
                )

                null -> Unit
            }

            // ---- dialogs ----
            val textNode = textDialogFor?.let { NodeStore.byId(it) }
            if (textNode != null) {
                NodeTextDialog(
                    initial = textNode.name,
                    onSave = { name ->
                        if (name.isNotEmpty()) {
                            NodeStore.updateNode(context, textNode.copy(name = name))
                            notice = context.getString(R.string.node_renamed, name)
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
                        notice = context.getString(R.string.notes_saved)
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
                        notice = context.getString(R.string.details_copied)
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
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemove = {
                        NodeStore.updateNode(context, pictureNode.copy(picUri = null))
                        pictureDialogFor = null
                        notice = context.getString(R.string.picture_removed)
                    },
                    onDismiss = { pictureDialogFor = null },
                )
            }

            val request = fragmentRequest
            if (request != null) {
                val node = NodeStore.byId(request.ids.first())
                if (node == null) {
                    fragmentRequest = null
                } else {
                    val children = NodeStore.childrenOf(node.id).size
                    FragmentOptionsDialog(
                        actionLabel = stringResource(
                            when (request.action) {
                                NodeActionCatalog.COPY -> R.string.act_copy
                                NodeActionCatalog.CUT -> R.string.act_cut
                                else -> R.string.act_duplicate
                            },
                        ),
                        nodeName = node.name,
                        childCount = children,
                        deeperCount = (NodeStore.subtreeOf(node.id).size - children).coerceAtLeast(0),
                        onConfirm = { withLinks, withSubLinks ->
                            val action = request.action
                            val ids = request.ids
                            fragmentRequest = null
                            runFragmentAction(action, ids, withLinks, withSubLinks)
                        },
                        onDismiss = { fragmentRequest = null },
                    )
                }
            }
        }
    }
}
/**
 * Creates a node and links it under [reference] (as a child) or under [reference]'s parent (as a
 * sibling), then hands the new id back so the caller can select it and open the rename dialog.
 *
 * Top-level rather than local to the composable so the rule "a sibling needs a parent" lives in one
 * place and the board composable stays readable.
 */
private fun createLinkedNodeIn(
    context: android.content.Context,
    kind: String,
    reference: IdeaNode,
    asSibling: Boolean,
    onCreated: (String) -> Unit,
    onNotice: (String) -> Unit,
) {
    val parent = if (asSibling) NodeStore.parentOf(reference.id) else reference
    if (parent == null) {
        AppLog.w("Node", "sibling refused: '" + reference.name + "' has no parent")
        onNotice(context.getString(R.string.no_parent_for_sibling))
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
    UndoStack.push(context.getString(R.string.undo_create, context.getString(R.string.node_new_default))) {
        NodeStore.removeNodes(context, listOf(id))
    }
    AppLog.i("Node", (if (asSibling) "sibling" else "child") + " created under '" + parent.name + "'")
    onCreated(id)
}

/** The rubber band: a translucent rectangle drawn while a long-press drag is running. */
@Composable
private fun RubberBand(start: Offset, end: Offset) {
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val stroke = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rect = Rect(min(start.x, end.x), min(start.y, end.y), max(start.x, end.x), max(start.y, end.y))
        drawRect(
            color = fill,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width.coerceAtLeast(1f), rect.height.coerceAtLeast(1f)),
        )
        drawRect(
            color = stroke,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width.coerceAtLeast(1f), rect.height.coerceAtLeast(1f)),
            style = Stroke(width = 2f),
        )
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
    childCount: Int,
    moveIds: List<String>,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onToggleCollapse: () -> Unit,
    onDragEnd: () -> Unit,
) {
    val linkCount = NodeStore.linksOf(node.id).size
    val scheme = MaterialTheme.colorScheme
    val role = node.color?.let { key -> LinkColor.values().firstOrNull { it.name.equals(key, true) } }
    val container = if (role == null) {
        scheme.surfaceVariant
    } else {
        lerp(scheme.surfaceVariant, themeRoleColor(scheme, role), 0.30f)
    }
    val pill = node.shape == NodeStyle.SHAPE_PILL
    val padH = if (pill) Spacing.xxl else Spacing.m
    val padV = if (pill) Spacing.l else Spacing.m
    val canCollapse = childCount > 0 || node.collapsed

    Card(
        modifier = Modifier
            .offset { IntOffset(node.x.roundToInt(), node.y.roundToInt()) }
            .width(StructureLayout.widthOf(node).dp)
            .heightIn(min = StructureLayout.heightOf(node).dp)
            .pointerInput(node.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() },
                )
            }
            .pointerInput(node.id, moveIds) {
                detectDragGestures(
                    onDragStart = { board.draggingNode = true },
                    onDragEnd = {
                        board.draggingNode = false
                        onDragEnd()
                        AppLog.i("Board", "node drag ended: " + node.name + " (" + moveIds.size + " node(s))")
                    },
                    onDragCancel = { board.draggingNode = false },
                ) { change, dragAmount ->
                    change.consume()
                    // Every selected node moves together, and the live position is read from the
                    // store because the gesture coroutine outlives recomposition.
                    moveIds.forEach { id ->
                        val current = NodeStore.byId(id) ?: return@forEach
                        NodeStore.move(
                            id,
                            current.x + dragAmount.x / board.scale,
                            current.y + dragAmount.y / board.scale,
                        )
                    }
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
                Spacer(Modifier.weight(1f))
                // The collapse control lives on the card: ⊖ gathers the branch, ⊕ opens it again.
                if (canCollapse) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                            .clickable(onClick = onToggleCollapse),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (node.collapsed) Icons.Outlined.Add else Icons.Outlined.Remove,
                            contentDescription = stringResource(
                                if (node.collapsed) R.string.node_expand else R.string.node_collapse,
                                childCount,
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
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
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.s),
    ) {
        content()
    }
}