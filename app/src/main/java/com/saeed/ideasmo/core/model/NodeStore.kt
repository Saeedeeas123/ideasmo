package com.saeed.ideasmo.core.model

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.saeed.ideasmo.core.log.AppLog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * A node placed on a board. [x] / [y] are board coordinates (board units = dp at 100% zoom),
 * not screen pixels, so the layout survives panning, zooming and screen changes.
 */
data class IdeaNode(
    val id: String,
    val kind: String,
    val name: String,
    val priority: Int,
    /**
     * The node's notes — **a list, no limit**. Saeed's order: a node may carry any number of notes,
     * images and files, and the card must not show their content, only how many there are.
     */
    val notes: List<String> = emptyList(),
    /** Pictures, as paths inside the app's own storage. No limit. */
    val pictures: List<String> = emptyList(),
    /** Attached files, as paths inside the app's own storage. No limit. */
    val files: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val x: Float = 0f,
    val y: Float = 0f,
    /**
     * A node the whole structure is organised around ("central node"). More than one is allowed:
     * every central node starts its own tree. Toggleable at any time.
     */
    val isRoot: Boolean = false,
    /**
     * Collapsed: the node itself stays on the board, everything below it (its whole subtree)
     * is hidden until it is expanded again. Nothing is deleted — the data stays intact.
     */
    val collapsed: Boolean = false,
    /**
     * Optional colour override, as a Material role name (see [NodeStyle]).
     * Null = the default card surface. Kept as a role, never a raw hex, so day/night and dynamic
     * colour keep working.
     */
    val color: String? = null,
    /** Card shape key from [NodeStyle]. */
    val shape: String = NodeStyle.SHAPE_ROUNDED,
    /**
     * Explicit card size in board units. 0 means "use the default card" ([StructureLayout.NODE_W] /
     * [StructureLayout.NODE_H]). Free-form resizing writes real numbers here, so a card can be any
     * size — the auto-layout measures these values.
     */
    val w: Float = 0f,
    val h: Float = 0f,
    /**
     * **How this node's branch is arranged** (v0.15.0), as a [LayoutDirection] key — or null, which
     * means **Free**.
     *
     * Saeed's order: the arrangement belongs to the NODE (in its own menu, not in the board's
     * settings), and it comes with a Free option — "if it is set to Free the user can move it freely;
     * otherwise it must obey the arrangement that was set, so if the arrange is Up it stays Up".
     *
     * What that means in practice: setting a direction arranges the branch straight away and keeps
     * the choice on the node, the whole-map Arrange lays every tree out in the direction of its own
     * central node, and dragging a node that has a direction moves it **with** its branch, so the
     * arrangement it was given is not silently broken. Free is the default, so nothing about an
     * existing board changes until the user asks for a direction.
     */
    val arrange: String? = null,
    /** Last time anything about the node changed — useful for sync and for the model layer. */
    val updatedAt: Long = 0L,
) {
    /** The card's width in board units, clamped to something a finger can still grab. */
    val width: Float get() = if (w > 0f) w.coerceIn(StructureLayout.MIN_NODE_W, StructureLayout.MAX_NODE_W) else StructureLayout.NODE_W
    /** The card's height in board units. */
    val height: Float get() = if (h > 0f) h.coerceIn(StructureLayout.MIN_NODE_H, StructureLayout.MAX_NODE_H) else StructureLayout.NODE_H

    /**
     * The first note — for the places that need one line of text (the form, Details, the list).
     * Everything else reads [notes] itself.
     */
    val description: String get() = notes.firstOrNull().orEmpty()

    /** The first picture — kept so older code that shows one thumbnail keeps working. */
    val picUri: String? get() = pictures.firstOrNull()

    /**
     * This node's arrangement as a direction, or null when it is **Free** — the state in which the
     * user is the one who decides where the node and its branch sit.
     */
    val arrangeDirection: LayoutDirection? get() = LayoutDirection.fromKeyOrNull(arrange)

    /** Everything the node carries, for the machine-readable view and the model layer. */
    val attachmentCount: Int get() = notes.size + pictures.size + files.size
}

/**
 * A typed, optionally directed relation between two nodes.
 * [type] is a key from [LinkCatalog]; [label] is the escape hatch for phrases the catalog
 * does not name. Several different links may exist between the same pair of nodes.
 *
 * [id] is what makes a link editable: a link is no longer identified by its content, so its
 * type, phrase, colour and direction can all be changed in place.
 */
data class NodeLink(
    val id: String,
    val fromId: String,
    val toId: String,
    val type: String = LinkCatalog.DEFAULT_KEY,
    val label: String? = null,
    /** Optional colour override. Null = use the colour role of [type]. */
    val color: String? = null,
    /**
     * Position among the outgoing links of [fromId]. This is what "order the branches" means:
     * sibling order is stored on the link, so it survives restarts and drives the auto-layout.
     */
    val order: Int = 0,
    /**
     * A free description of THIS relation ("agreed in the 2026-10 review"). Separate from [label],
     * which is the phrase the link reads as. Machine-readable on purpose: the model layer gets both.
     */
    val note: String? = null,
    /** When the link was created. */
    val createdAt: Long = 0L,
) {
    fun otherEnd(nodeId: String): String = if (fromId == nodeId) toId else fromId

    fun connects(a: String, b: String): Boolean =
        (fromId == a && toId == b) || (fromId == b && toId == a)

    /** Two links are the same when they join the same pair with the same phrase. */
    fun sameAs(other: NodeLink): Boolean =
        connects(other.fromId, other.toId) && type == other.type && label == other.label

    /** The colour role actually used for drawing: the override, or the type's own role. */
    fun colorRole(): LinkColor =
        color?.let { key -> LinkColor.values().firstOrNull { it.name == key } }
            ?: LinkCatalog.byKey(type).color
}

/** A link prepared for display: the sentence, the other end's name and its direction. */
data class LinkView(
    val link: NodeLink,
    val otherName: String,
    val sentence: String,
    val displayLabel: String,
    /** True when the link leaves [nodeId], false when it arrives at it. */
    val outgoing: Boolean,
)
/** A node together with part of its branch — what Copy / Cut / Duplicate / Paste move around. */
data class NodeFragment(
    val rootId: String,
    val nodes: List<IdeaNode>,
    val links: List<NodeLink>,
) {
    val isEmpty: Boolean get() = nodes.isEmpty()
}

/**
 * The whole board at one instant: every node and every link.
 *
 * Redo is built on this (see [UndoStack]): the state right now is exactly what a Redo has to bring
 * back, so no action has to hand-write a second closure for the redo direction.
 */
data class BoardSnapshot(val nodes: List<IdeaNode>, val links: List<NodeLink>)

/** Nodes and links removed together, kept so one Undo can put all of it back. */
data class RemovedBundle(
    val nodes: List<IdeaNode>,
    val links: List<NodeLink>,
)

/**
 * Node + link store. Compose-observable in memory, persisted as JSON in the app's private
 * files dir (org.json is part of the platform — no dependency added).
 *
 * Board dragging calls [move] on every frame (memory only) and [persist] once on drag end,
 * so a drag never hammers the disk.
 */
object NodeStore {

    const val KIND_IDEA = "idea"
    const val KIND_MINDMAP = "mindmap"
    const val KIND_KNOWLEDGE = "knowledge"
    const val KIND_NOTE = "note"

    const val ORDER_MANUAL = "manual"
    const val ORDER_NAME = "name"
    const val ORDER_PRIORITY = "priority"
    const val ORDER_CREATED = "created"

    private const val FILE_NAME = "nodes.json"
    private const val MIGRATION_COL_W = 240f
    private const val MIGRATION_ROW_H = 140f

    private val items = mutableStateListOf<IdeaNode>()
    private val linkItems = mutableStateListOf<NodeLink>()

    val nodes: List<IdeaNode> get() = items

    val links: List<NodeLink> get() = linkItems

    fun nodesOf(kind: String): List<IdeaNode> = items.filter { it.kind == kind }

    fun byId(id: String): IdeaNode? = items.firstOrNull { it.id == id }

    fun nameOf(id: String): String = byId(id)?.name ?: "?"

    fun linkById(id: String): NodeLink? = linkItems.firstOrNull { it.id == id }

    fun linksOf(nodeId: String): List<NodeLink> =
        linkItems.filter { it.fromId == nodeId || it.toId == nodeId }

    /** Links that leave a node — these are its "sub-links" / branches. */
    fun linksOut(nodeId: String): List<NodeLink> =
        linkItems.filter { it.fromId == nodeId }.sortedBy { it.order }

    /** Links that arrive at a node from somewhere else. */
    fun linksIn(nodeId: String): List<NodeLink> = linkItems.filter { it.toId == nodeId }

    /**
     * Links of a node, ready to render as sentences. Outgoing first (in their stored order),
     * then incoming — the same order the node panel and the list view show.
     */
    fun linkViews(nodeId: String): List<LinkView> {
        val node = byId(nodeId) ?: return emptyList()
        return linksOf(nodeId).mapNotNull { link ->
            val other = byId(link.otherEnd(nodeId)) ?: return@mapNotNull null
            val type = LinkCatalog.byKey(link.type)
            LinkView(
                link = link,
                otherName = other.name,
                sentence = LinkCatalog.sentence(type, node.name, other.name, link.label),
                displayLabel = LinkCatalog.displayLabel(type, link.label),
                outgoing = link.fromId == nodeId,
            )
        }.sortedWith(compareBy({ if (it.outgoing) 0 else 1 }, { it.link.order }))
    }

    fun connectedNames(nodeId: String): List<String> =
        linksOf(nodeId).mapNotNull { byId(it.otherEnd(nodeId))?.name }

    fun isLinked(a: String, b: String, type: String? = null): Boolean =
        linkItems.any { it.connects(a, b) && (type == null || it.type == type) }

    // ---- fragments: a node together with part of its branch -----------------

    /**
     * Everything that would travel with a Copy / Cut / Duplicate of [nodeId]:
     *
     * - neither option: just the node;
     * - [withLinks]: the node plus its direct children and the links that join them;
     * - [withSubLinks]: the node plus its whole subtree and every link inside it.
     *
     * Links are only included when **both** ends are in the fragment, so a copy can never point at a
     * node that was not copied.
     */
    fun fragmentOf(nodeId: String, withLinks: Boolean, withSubLinks: Boolean): NodeFragment? =
        FragmentBuilder.build(nodeId, items, linkItems, withLinks, withSubLinks)

    /**
     * Inserts a fragment as fresh data: new ids for every node and every link, links rewired to the
     * new ids, and the fragment's root placed at ([atX], [atY]). Returns the new root's id.
     */
    fun insertFragment(context: Context, fragment: NodeFragment, atX: Float, atY: Float, kind: String): String? {
        val root = fragment.nodes.firstOrNull { it.id == fragment.rootId } ?: return null
        val now = System.currentTimeMillis()
        val idMap = HashMap<String, String>()
        fragment.nodes.forEach { idMap[it.id] = newId() }
        fragment.nodes.forEach { node ->
            val dx = node.x - root.x
            val dy = node.y - root.y
            items.add(
                node.copy(
                    id = idMap.getValue(node.id),
                    kind = kind,
                    x = atX + dx,
                    y = atY + dy,
                    isRoot = false,
                    collapsed = false,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        fragment.links.forEach { link ->
            val from = idMap[link.fromId] ?: return@forEach
            val to = idMap[link.toId] ?: return@forEach
            linkItems.add(
                link.copy(
                    id = newId(),
                    fromId = from,
                    toId = to,
                    order = linksOut(from).size,
                    createdAt = now,
                ),
            )
        }
        persist(context)
        AppLog.i(
            "Fragment",
            "inserted " + fragment.nodes.size + " node(s) / " + fragment.links.size + " link(s) at (" +
                atX.toInt() + "," + atY.toInt() + ")",
        )
        return idMap[root.id]
    }

    /** Removes several nodes (and every link touching them) as one bundle, for Undo. */
    fun removeNodes(context: Context, ids: Collection<String>): RemovedBundle? {
        val wanted = ids.toSet()
        val removedNodes = items.filter { it.id in wanted }
        if (removedNodes.isEmpty()) return null
        val removedLinks = linkItems.filter { it.fromId in wanted || it.toId in wanted }
        items.removeAll(removedNodes)
        linkItems.removeAll(removedLinks)
        persist(context)
        AppLog.i(
            "Node",
            "removed " + removedNodes.size + " node(s) and " + removedLinks.size + " link(s)",
        )
        return RemovedBundle(removedNodes, removedLinks)
    }

    /** Puts a bundle removed by [removeNodes] back. */
    fun restoreBundle(context: Context, bundle: RemovedBundle) {
        bundle.nodes.forEach { node -> if (byId(node.id) == null) items.add(node) }
        bundle.links.forEach { link ->
            if (byId(link.fromId) != null && byId(link.toId) != null && linkItems.none { it.id == link.id }) {
                linkItems.add(link)
            }
        }
        persist(context)
        AppLog.i("Node", "restored " + bundle.nodes.size + " node(s)")
    }

    // ---- editing one node ---------------------------------------------------

    /** Replaces a node with an edited copy, stamps `updatedAt` and persists. */
    fun updateNode(context: Context, updated: IdeaNode): Boolean {
        val index = items.indexOfFirst { it.id == updated.id }
        if (index < 0) {
            AppLog.w("Node", "update refused: no node " + updated.id)
            return false
        }
        val stamped = updated.copy(updatedAt = System.currentTimeMillis())
        items[index] = stamped
        persist(context)
        AppLog.i(
            "Node",
            "node updated: '" + stamped.name + "' (" + stamped.id + ") color=" + (stamped.color ?: "default") +
                " size=" + stamped.width.toInt() + "x" + stamped.height.toInt() +
                " notes=" + stamped.notes.size + " images=" + stamped.pictures.size +
                " files=" + stamped.files.size,
        )
        return true
    }

    // ---- what a node carries: notes, images, files (no limit on any of them) --

    /** Adds a note to a node. Empty text is refused. Returns true when it was added. */
    fun addNote(context: Context, nodeId: String, text: String): Boolean {
        val clean = text.trim()
        val node = byId(nodeId) ?: return false
        if (clean.isEmpty()) return false
        updateNode(context, node.copy(notes = node.notes + clean))
        AppLog.i("Node", "note added to '" + node.name + "' — now " + (node.notes.size + 1))
        return true
    }

    /** Replaces one note in place. An empty text removes it. */
    fun setNote(context: Context, nodeId: String, index: Int, text: String) {
        val node = byId(nodeId) ?: return
        if (index !in node.notes.indices) return
        val next = node.notes.toMutableList()
        val clean = text.trim()
        if (clean.isEmpty()) next.removeAt(index) else next[index] = clean
        updateNode(context, node.copy(notes = next))
    }

    fun removeNote(context: Context, nodeId: String, index: Int) {
        val node = byId(nodeId) ?: return
        if (index !in node.notes.indices) return
        val next = node.notes.toMutableList()
        next.removeAt(index)
        updateNode(context, node.copy(notes = next))
    }

    /** Adds a picture the caller has already copied into the app's own storage. */
    fun addPicture(context: Context, nodeId: String, path: String) {
        val node = byId(nodeId) ?: return
        if (path.isBlank()) return
        updateNode(context, node.copy(pictures = node.pictures + path))
        AppLog.i("Node", "image added to '" + node.name + "' — now " + (node.pictures.size + 1))
    }

    fun removePicture(context: Context, nodeId: String, index: Int) {
        val node = byId(nodeId) ?: return
        if (index !in node.pictures.indices) return
        val next = node.pictures.toMutableList()
        next.removeAt(index)
        updateNode(context, node.copy(pictures = next))
    }

    fun addFile(context: Context, nodeId: String, path: String) {
        val node = byId(nodeId) ?: return
        if (path.isBlank()) return
        updateNode(context, node.copy(files = node.files + path))
        AppLog.i("Node", "file added to '" + node.name + "' — now " + (node.files.size + 1))
    }

    fun removeFile(context: Context, nodeId: String, index: Int) {
        val node = byId(nodeId) ?: return
        if (index !in node.files.indices) return
        val next = node.files.toMutableList()
        next.removeAt(index)
        updateNode(context, node.copy(files = next))
    }

    /**
     * Reads a string array out of a stored node, tolerating a missing key, a JSON null, and a single
     * string written by an older build.
     */
    private fun readList(o: JSONObject, key: String): List<String> {
        val array = o.optJSONArray(key) ?: return emptyList()
        val out = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            val value = array.optString(i)
            if (value.isNotBlank()) out.add(value)
        }
        return out
    }

    /**
     * The parent of a node: the source of the first incoming link.
     * Used by Sibling and by Reattach. A node nothing points at has no parent — it is a root.
     */
    fun parentOf(nodeId: String): IdeaNode? = linksIn(nodeId).firstOrNull()?.let { byId(it.fromId) }

    /**
     * A node written as data: every field, every link, each with its id.
     *
     * This is the shape the language-model layer will read (and the user can copy it out of the
     * details dialog). It is deliberately plain text with stable field names, not prose.
     */
    fun describe(nodeId: String): String {
        val node = byId(nodeId) ?: return "node " + nodeId + ": not found"
        val out = StringBuilder()
        out.appendLine("node: " + node.id)
        out.appendLine("name: " + node.name)
        out.appendLine("kind: " + node.kind)
        out.appendLine("priority: " + node.priority)
        out.appendLine("color: " + (node.color ?: "default"))
        out.appendLine("shape: " + node.shape)
        out.appendLine("width: " + node.width.toInt() + " board units")
        out.appendLine("height: " + node.height.toInt() + " board units")
        out.appendLine("central: " + node.isRoot)
        out.appendLine("collapsed: " + node.collapsed)
        out.appendLine("images: " + node.pictures.size)
        out.appendLine("files: " + node.files.size)
        out.appendLine("position: (" + node.x.toInt() + ", " + node.y.toInt() + ") board units, origin = (0, 0)")
        out.appendLine("created: " + node.createdAt)
        out.appendLine("updated: " + node.updatedAt)
        out.appendLine("notes: " + node.notes.size)
        node.notes.forEachIndexed { i, note -> out.appendLine("note[" + i + "]: " + note) }
        node.pictures.forEachIndexed { i, path -> out.appendLine("image[" + i + "]: " + path) }
        node.files.forEachIndexed { i, path -> out.appendLine("file[" + i + "]: " + path) }

        val outs = linksOut(nodeId)
        out.appendLine("links out (" + outs.size + "):")
        if (outs.isEmpty()) out.appendLine("  - none")
        outs.forEachIndexed { i, l ->
            val t = LinkCatalog.byKey(l.type)
            out.appendLine(
                "  " + (i + 1) + ". " + LinkCatalog.sentence(t, node.name, nameOf(l.toId), l.label) +
                    " [id=" + l.id + ", type=" + l.type + ", order=" + l.order +
                    ", color=" + (l.color ?: "type") + ", note=" + (l.note ?: "-") + "]",
            )
        }

        val ins = linksIn(nodeId)
        out.appendLine("links in (" + ins.size + "):")
        if (ins.isEmpty()) out.appendLine("  - none")
        ins.forEach { l ->
            val t = LinkCatalog.byKey(l.type)
            out.appendLine(
                "  - " + LinkCatalog.sentence(t, nameOf(l.fromId), node.name, l.label) +
                    " [id=" + l.id + ", type=" + l.type + ", note=" + (l.note ?: "-") + "]",
            )
        }
        return out.toString()
    }

    // ---- structure ----------------------------------------------------------

    /** The children of a node, in branch order: the targets of its outgoing links. */
    fun childrenOf(nodeId: String): List<IdeaNode> = linksOut(nodeId)
        .mapNotNull { byId(it.toId) }
        .filter { it.id != nodeId }

    /** Nodes the user marked as central. More than one is allowed. */
    fun rootIds(kind: String? = null): List<String> =
        items.filter { it.isRoot && (kind == null || it.kind == kind) }.map { it.id }

    /** Every node under [nodeId] (not including it), following outgoing links, cycle-safe. */
    fun subtreeOf(nodeId: String): List<String> {
        val seen = LinkedHashSet<String>()
        val queue = ArrayDeque<String>()
        linksOut(nodeId).forEach { queue.add(it.toId) }
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (next == nodeId || !seen.add(next)) continue
            linksOut(next).forEach { queue.add(it.toId) }
        }
        return seen.toList()
    }

    /**
     * Everything hidden right now: the subtrees of every collapsed node.
     * Hidden means "not drawn", never "deleted" — expanding brings it straight back.
     */
    fun hiddenIds(): Set<String> {
        val hidden = HashSet<String>()
        items.filter { it.collapsed }.forEach { node -> hidden.addAll(subtreeOf(node.id)) }
        return hidden
    }
    /**
     * Stores how this node's branch is arranged, or clears it back to **Free** ([key] null).
     *
     * Saeed's order (v0.15.0): the arrangement is a property of the NODE, chosen in the node's own
     * menu — not a board-wide setting — and it has a Free option beside the directions. Storing it
     * (instead of only acting on it) is what makes the choice visible and repeatable: the node's menu
     * shows which direction it is on, the whole-map Arrange honours it, and Free means the position is
     * the user's own.
     */
    fun setArrange(context: Context, nodeId: String, key: String?) {
        val index = items.indexOfFirst { it.id == nodeId }
        if (index < 0) return
        if (items[index].arrange == key) return
        items[index] = items[index].copy(arrange = key)
        persist(context)
        AppLog.i(
            "Structure",
            "arrange of '" + items[index].name + "' set to " + (key ?: "free"),
        )
    }

    fun setRoot(context: Context, nodeId: String, isRoot: Boolean) {
        val index = items.indexOfFirst { it.id == nodeId }
        if (index < 0) return
        items[index] = items[index].copy(isRoot = isRoot)
        persist(context)
        AppLog.i("Structure", "central node ${if (isRoot) "set" else "cleared"}: '${items[index].name}'")
    }

    fun setCollapsed(context: Context, nodeId: String, collapsed: Boolean) {
        val index = items.indexOfFirst { it.id == nodeId }
        if (index < 0) return
        items[index] = items[index].copy(collapsed = collapsed)
        persist(context)
        AppLog.i(
            "Structure",
            "node '${items[index].name}' ${if (collapsed) "collapsed" else "expanded"} " +
                "(subtree ${subtreeOf(nodeId).size} node(s))",
        )
    }

    fun setCollapsedAll(context: Context, collapsed: Boolean) {
        var changed = 0
        for (i in items.indices) {
            if (items[i].collapsed != collapsed) {
                items[i] = items[i].copy(collapsed = collapsed)
                changed++
            }
        }
        persist(context)
        AppLog.i("Structure", "${if (collapsed) "collapse" else "expand"} all: $changed node(s) changed")
    }

    /** Moves a node and everything under it by a board-space delta. */
    fun moveSubtree(context: Context, nodeId: String, dx: Float, dy: Float) {
        val ids = subtreeOf(nodeId) + nodeId
        ids.forEach { id ->
            val index = items.indexOfFirst { it.id == id }
            if (index >= 0) {
                items[index] = items[index].copy(x = items[index].x + dx, y = items[index].y + dy)
            }
        }
        persist(context)
        AppLog.i("Structure", "moved subtree of '$nodeId' by ($dx,$dy) — ${ids.size} node(s)")
    }

    /** Writes a whole set of computed positions at once (used by the auto-layout). */
    fun applyPositions(context: Context, positions: Map<String, LayoutPoint>) {
        var moved = 0
        positions.forEach { (id, point) ->
            val index = items.indexOfFirst { it.id == id }
            if (index >= 0) {
                items[index] = items[index].copy(x = point.x, y = point.y)
                moved++
            }
        }
        persist(context)
        AppLog.i("Structure", "layout applied to $moved node(s)")
    }

    // ---- the size each card really renders at (memory only) ------------------
    //
    // A card grows with its content — a picture, a long name — so the height stored on the node can
    // be smaller than what is on screen. The board reports the size it actually drew (BoardNode), and
    // the auto-layout reserves that, so siblings can never be laid out closer than the cards are
    // tall. This is deliberately NOT part of the saved model: it is a measurement of the current
    // screen and font scale, and it is re-measured on every run.
    private val rendered = HashMap<String, LayoutSize>()

    /** Called by the board once a card has been laid out. Sizes are in board units. */
    fun noteRenderedSize(id: String, w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        val known = rendered[id]
        if (known != null &&
            kotlin.math.abs(known.w - w) < 0.5f &&
            kotlin.math.abs(known.h - h) < 0.5f
        ) {
            return
        }
        rendered[id] = LayoutSize(w, h)
    }

    /** What the cards measured the last time the board drew them. */
    fun measuredSizes(): Map<String, LayoutSize> = rendered

    // ---- link ordering ------------------------------------------------------

    /** Moves a link up or down among its siblings (the branches of the same parent). */
    fun reorderLink(context: Context, linkId: String, delta: Int) {
        val link = linkById(linkId) ?: return
        val siblings = linksOut(link.fromId).toMutableList()
        val index = siblings.indexOfFirst { it.id == linkId }
        val target = index + delta
        if (index < 0 || target < 0 || target >= siblings.size) {
            AppLog.i("Structure", "reorder refused: already at the edge")
            return
        }
        val moved = siblings.removeAt(index)
        siblings.add(target, moved)
        siblings.forEachIndexed { i, l -> replaceOrder(l.id, i) }
        persist(context)
        AppLog.i("Structure", "branch '${moved.id}' moved to position ${target + 1}/${siblings.size}")
    }

    /** Auto-order modes for a node's branches. */
    fun autoOrder(context: Context, nodeId: String, mode: String) {
        val siblings = linksOut(nodeId).toMutableList()
        if (siblings.isEmpty()) return
        val sorted = when (mode) {
            ORDER_NAME -> siblings.sortedBy { nameOf(it.toId).lowercase() }
            ORDER_PRIORITY -> siblings.sortedByDescending { byId(it.toId)?.priority ?: 0 }
            ORDER_CREATED -> siblings.sortedBy { byId(it.toId)?.createdAt ?: 0L }
            else -> siblings.sortedBy { it.order }
        }
        sorted.forEachIndexed { i, l -> replaceOrder(l.id, i) }
        persist(context)
        AppLog.i("Structure", "branch order of '$nodeId' set to $mode (${sorted.size} branch(es))")
    }

    private fun replaceOrder(linkId: String, order: Int) {
        val index = linkItems.indexOfFirst { it.id == linkId }
        if (index >= 0) linkItems[index] = linkItems[index].copy(order = order)
    }

    // ---- mutations ----------------------------------------------------------

    fun newId(): String = UUID.randomUUID().toString()

    fun init(context: Context) {
        if (items.isNotEmpty()) return
        val file = file(context)
        if (!file.exists()) {
            AppLog.i("NodeStore", "no node file yet: " + file.absolutePath)
            return
        }
        try {
            val text = file.readText()
            val nodesArray: JSONArray
            val linksArray: JSONArray?
            if (text.trimStart().startsWith("[")) {
                // Legacy format: a bare array of nodes, written before links existed.
                nodesArray = JSONArray(text)
                linksArray = null
            } else {
                val root = JSONObject(text)
                nodesArray = root.optJSONArray("nodes") ?: JSONArray()
                linksArray = root.optJSONArray("links")
            }

            val loaded = ArrayList<IdeaNode>(nodesArray.length())
            var migrated = 0
            for (i in 0 until nodesArray.length()) {
                val o = nodesArray.getJSONObject(i)
                val hasPosition = o.has("x") && o.has("y")
                if (!hasPosition) migrated++
                loaded.add(
                    IdeaNode(
                        id = o.optString("id"),
                        kind = if (o.has("kind")) o.optString("kind") else KIND_IDEA,
                        name = o.optString("name"),
                        priority = o.optInt("priority", 3),
                        // v0.12.0 replaced the single description / picUri with lists. An older node
                        // loads with its description as its first note and its picture as its first
                        // image, so nothing is lost and nothing has to be re-entered.
                        notes = readList(o, "notes").ifEmpty {
                            val single = o.optString("description")
                            if (single.isNotBlank()) listOf(single) else emptyList()
                        },
                        pictures = readList(o, "pictures").ifEmpty {
                            val single = if (o.isNull("picUri")) null else o.optString("picUri")
                            if (single.isNullOrBlank()) emptyList() else listOf(single)
                        },
                        files = readList(o, "files"),
                        createdAt = o.optLong("createdAt"),
                        x = if (hasPosition) o.optDouble("x", 0.0).toFloat() else (i % 2) * MIGRATION_COL_W,
                        y = if (hasPosition) o.optDouble("y", 0.0).toFloat() else (i / 2) * MIGRATION_ROW_H,
                        isRoot = o.optBoolean("isRoot", false),
                        collapsed = o.optBoolean("collapsed", false),
                        color = if (o.has("color") && !o.isNull("color")) o.optString("color") else null,
                        shape = if (o.has("shape")) o.optString("shape") else NodeStyle.SHAPE_ROUNDED,
                        // v0.15.0: the node's own arrangement key; absent (or null) means Free.
                        arrange = if (o.has("arrange") && !o.isNull("arrange")) {
                            o.optString("arrange")
                        } else {
                            null
                        },
                        // v0.8.0 stored a size multiplier; v0.9.0 stores real board units. Migrate.
                        w = if (o.has("w") && o.optDouble("w", 0.0) > 0.0) {
                            o.optDouble("w").toFloat()
                        } else if (o.has("size")) {
                            StructureLayout.NODE_W * NodeStyle.sizeOf(o.optDouble("size", 1.0).toFloat())
                        } else {
                            0f
                        },
                        h = if (o.has("h") && o.optDouble("h", 0.0) > 0.0) {
                            o.optDouble("h").toFloat()
                        } else if (o.has("size")) {
                            StructureLayout.NODE_H * NodeStyle.sizeOf(o.optDouble("size", 1.0).toFloat())
                        } else {
                            0f
                        },
                        updatedAt = o.optLong("updatedAt", 0L),
                    ),
                )
            }

            val loadedLinks = ArrayList<NodeLink>()
            if (linksArray != null) {
                for (i in 0 until linksArray.length()) {
                    val o = linksArray.getJSONObject(i)
                    val existingId = if (o.has("id")) o.optString("id") else ""
                    loadedLinks.add(
                        NodeLink(
                            // Links written before ids existed get one on load, so they become
                            // editable without any other change.
                            id = existingId.ifEmpty { UUID.randomUUID().toString() },
                            fromId = o.optString("from"),
                            toId = o.optString("to"),
                            // Links written before types existed become the neutral default.
                            type = if (o.has("type")) o.optString("type") else LinkCatalog.DEFAULT_KEY,
                            label = if (o.isNull("label")) null else o.optString("label"),
                            color = if (o.has("color") && !o.isNull("color")) o.optString("color") else null,
                            order = o.optInt("order", 0),
                            note = if (o.has("note") && !o.isNull("note")) o.optString("note") else null,
                            createdAt = o.optLong("createdAt", 0L),
                        ),
                    )
                }
            }

            items.clear()
            items.addAll(loaded)
            linkItems.clear()
            linkItems.addAll(
                loadedLinks.filter { l -> loaded.any { it.id == l.fromId } && loaded.any { it.id == l.toId } },
            )
            normalizeOrders()
            AppLog.i(
                "NodeStore",
                "loaded " + loaded.size + " node(s) / " + linkItems.size + " link(s) from " + file.absolutePath +
                    " (migrated without position: " + migrated + ", legacyFormat: " + (linksArray == null) +
                    ", central: " + loaded.count { it.isRoot } + ", collapsed: " + loaded.count { it.collapsed } + ")",
            )
        } catch (t: Throwable) {
            AppLog.e("NodeStore", "load failed", t)
        }
    }

    /** Guarantees one clean 0..n-1 branch order per parent, for files written by older versions. */
    private fun normalizeOrders() {
        linkItems.map { it.fromId }.distinct().forEach { parent ->
            val siblings = linkItems.filter { it.fromId == parent }.sortedBy { it.order }
            siblings.forEachIndexed { i, l ->
                val index = linkItems.indexOfFirst { it.id == l.id }
                if (index >= 0 && linkItems[index].order != i) {
                    linkItems[index] = linkItems[index].copy(order = i)
                }
            }
        }
    }

    fun add(context: Context, node: IdeaNode) {
        items.add(node)
        persist(context)
        AppLog.i(
            "NodeStore",
            "node added: kind=" + node.kind + " name='" + node.name + "' priority=" + node.priority +
                " pos=(" + node.x.toInt() + "," + node.y.toInt() + ") notes=" + node.notes.size +
                " images=" + node.pictures.size + " files=" + node.files.size +
                " total=" + items.size,
        )
    }

    /** Memory-only update, used on every drag frame. */
    fun move(id: String, x: Float, y: Float) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            items[index] = items[index].copy(x = x, y = y)
        }
    }

    /**
     * Memory-only move of a node **and everything under it**, used while a finger drags an arranged
     * node so its branch travels with it (v0.15.0). Like [move], it writes nothing to disk — the board
     * calls [persist] once when the finger lifts, so a drag never hammers the file.
     */
    fun moveSubtreeLive(nodeId: String, dx: Float, dy: Float) {
        val ids = listOf(nodeId) + subtreeOf(nodeId)
        ids.forEach { id ->
            val index = items.indexOfFirst { it.id == id }
            if (index >= 0) {
                items[index] = items[index].copy(x = items[index].x + dx, y = items[index].y + dy)
            }
        }
    }

    /**
     * Memory-only resize, used on every frame of a resize drag.
     * [persist] is called once when the finger lifts, so resizing never hammers the disk.
     */
    fun resize(id: String, w: Float, h: Float, x: Float, y: Float) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            items[index] = items[index].copy(w = w, h = h, x = x, y = y)
        }
    }

    /**
     * Creates a typed connection. Several different types may join the same pair, but the same
     * type between the same pair is refused. Returns "ok", "self", "exists" or "missing".
     */
    fun addLink(
        context: Context,
        fromId: String,
        toId: String,
        typeKey: String = LinkCatalog.DEFAULT_KEY,
        label: String? = null,
    ): String {
        if (fromId == toId) return "self"
        if (byId(fromId) == null || byId(toId) == null) return "missing"
        val candidate = NodeLink(
            id = newId(),
            fromId = fromId,
            toId = toId,
            type = typeKey,
            label = label,
            color = null,
            order = linksOut(fromId).size,
            note = null,
            createdAt = System.currentTimeMillis(),
        )
        if (linkItems.any { it.sameAs(candidate) }) return "exists"
        linkItems.add(candidate)
        persist(context)
        AppLog.i(
            "NodeStore",
            "link added: '" + nameOf(fromId) + "' " +
                LinkCatalog.displayLabel(LinkCatalog.byKey(typeKey), label) +
                " '" + nameOf(toId) + "' total=" + linkItems.size,
        )
        return "ok"
    }

    /**
     * Edits an existing link in place: type, phrase, colour override or direction.
     * Returns "ok", "missing" or "exists" (the edit would duplicate another link).
     */
    fun updateLink(context: Context, updated: NodeLink): String {
        val index = linkItems.indexOfFirst { it.id == updated.id }
        if (index < 0) return "missing"
        val previous = linkItems[index]
        if (linkItems.any { it.id != updated.id && it.sameAs(updated) }) return "exists"
        linkItems[index] = updated
        persist(context)
        AppLog.i(
            "Link",
            "link edited: '" + nameOf(updated.fromId) + "' " +
                LinkCatalog.displayLabel(LinkCatalog.byKey(updated.type), updated.label) +
                " '" + nameOf(updated.toId) + "' (type " + previous.type + " -> " + updated.type +
                ", color " + previous.color + " -> " + updated.color + ")",
        )
        return "ok"
    }

    /** Removes one link. Returns it, so the UI can offer Undo. */
    fun removeLink(context: Context, link: NodeLink): NodeLink? {
        val index = linkItems.indexOfFirst { it.id == link.id }
        if (index < 0) return null
        val removed = linkItems.removeAt(index)
        persist(context)
        AppLog.i(
            "NodeStore",
            "link removed: " + removed.type + " " + removed.fromId + " -> " + removed.toId +
                " remaining=" + linkItems.size,
        )
        return removed
    }

    fun restoreLink(context: Context, link: NodeLink) {
        if (linkItems.none { it.id == link.id }) {
            linkItems.add(link)
            persist(context)
            AppLog.i("NodeStore", "link restored: " + link.type)
        }
    }

    /** Removes a node and every link that touched it. Returns what was removed, for undo. */
    fun remove(context: Context, nodeId: String): Pair<IdeaNode, List<NodeLink>>? {
        val index = items.indexOfFirst { it.id == nodeId }
        if (index < 0) return null
        val removedNode = items.removeAt(index)
        val removedLinks = linkItems.filter { it.fromId == nodeId || it.toId == nodeId }
        linkItems.removeAll(removedLinks)
        persist(context)
        AppLog.i(
            "NodeStore",
            "node removed: '" + removedNode.name + "' (links removed: " + removedLinks.size +
                ") remaining=" + items.size,
        )
        return removedNode to removedLinks
    }

    /** Puts back a node removed by [remove], together with its links (used by Undo). */
    fun restore(context: Context, node: IdeaNode, links: List<NodeLink>) {
        if (byId(node.id) == null) items.add(node)
        links.forEach { l ->
            if (byId(l.fromId) != null && byId(l.toId) != null && linkItems.none { it.id == l.id }) {
                linkItems.add(l)
            }
        }
        persist(context)
        AppLog.i("NodeStore", "node restored: '" + node.name + "' links=" + links.size)
    }

    /**
     * The whole board right now. Used by Redo (see [UndoStack]).
     */
    fun snapshot(): BoardSnapshot = BoardSnapshot(items.toList(), linkItems.toList())

    /** Puts a snapshot back — every node and every link — and persists it. */
    fun restoreSnapshot(context: Context, snapshot: BoardSnapshot) {
        items.clear()
        items.addAll(snapshot.nodes)
        linkItems.clear()
        linkItems.addAll(snapshot.links)
        persist(context)
        AppLog.i("NodeStore", "snapshot restored: " + snapshot.nodes.size + " node(s), " + snapshot.links.size + " link(s)")
    }

    /**
     * Rewrites stored attachment paths after the Drive migration moved the files (v0.13.0).
     * Only the paths change — nothing is copied and no node is otherwise touched.
     */
    fun remapAttachments(context: Context, mapping: Map<String, String>) {
        if (mapping.isEmpty()) return
        var changed = 0
        items.forEachIndexed { index, node ->
            val pictures = node.pictures.map { mapping[it] ?: it }
            val files = node.files.map { mapping[it] ?: it }
            if (pictures != node.pictures || files != node.files) {
                items[index] = node.copy(pictures = pictures, files = files)
                changed++
            }
        }
        if (changed > 0) {
            persist(context)
            AppLog.i("NodeStore", "attachment paths remapped on " + changed + " node(s)")
        }
    }

    fun persist(context: Context) {
        try {
            val nodesArray = JSONArray()
            items.forEach { n ->
                val o = JSONObject()
                o.put("id", n.id)
                o.put("kind", n.kind)
                o.put("name", n.name)
                o.put("priority", n.priority)
                // v0.12.0: three lists, no limit on any of them.
                o.put("notes", JSONArray(n.notes))
                o.put("pictures", JSONArray(n.pictures))
                o.put("files", JSONArray(n.files))
                o.put("createdAt", n.createdAt)
                o.put("x", n.x.toDouble())
                o.put("y", n.y.toDouble())
                o.put("isRoot", n.isRoot)
                o.put("collapsed", n.collapsed)
                o.put("color", n.color ?: JSONObject.NULL)
                o.put("shape", n.shape)
                o.put("w", n.w.toDouble())
                o.put("h", n.h.toDouble())
                o.put("arrange", n.arrange ?: JSONObject.NULL)
                o.put("updatedAt", n.updatedAt)
                nodesArray.put(o)
            }
            val linksArray = JSONArray()
            linkItems.forEach { l ->
                val o = JSONObject()
                o.put("id", l.id)
                o.put("from", l.fromId)
                o.put("to", l.toId)
                o.put("type", l.type)
                o.put("label", l.label ?: JSONObject.NULL)
                o.put("color", l.color ?: JSONObject.NULL)
                o.put("order", l.order)
                o.put("note", l.note ?: JSONObject.NULL)
                o.put("createdAt", l.createdAt)
                linksArray.put(o)
            }
            val root = JSONObject()
            root.put("nodes", nodesArray)
            root.put("links", linksArray)
            file(context).writeText(root.toString())
            AppLog.d("NodeStore", "saved " + items.size + " node(s) / " + linkItems.size + " link(s)")
        } catch (t: Throwable) {
            AppLog.e("NodeStore", "save failed", t)
        }
    }

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)
}