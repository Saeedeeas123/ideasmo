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
    val description: String,
    val picUri: String?,
    val createdAt: Long,
    val x: Float,
    val y: Float,
)

/**
 * A typed, optionally directed relation between two nodes.
 * [type] is a key from [LinkCatalog]; [label] is the escape hatch for phrases the catalog
 * does not name. Several different links may exist between the same pair of nodes.
 */
data class NodeLink(
    val fromId: String,
    val toId: String,
    val type: String = LinkCatalog.DEFAULT_KEY,
    val label: String? = null,
) {
    fun otherEnd(nodeId: String): String = if (fromId == nodeId) toId else fromId

    fun connects(a: String, b: String): Boolean =
        (fromId == a && toId == b) || (fromId == b && toId == a)

    /** Two links are the same when they join the same pair with the same phrase. */
    fun sameAs(other: NodeLink): Boolean =
        connects(other.fromId, other.toId) && type == other.type && label == other.label
}

/** A link prepared for display: the sentence plus the name of the node at the other end. */
data class LinkView(
    val link: NodeLink,
    val otherName: String,
    val sentence: String,
    val displayLabel: String,
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

    private const val FILE_NAME = "nodes.json"
    private const val MIGRATION_COL_W = 240f
    private const val MIGRATION_ROW_H = 140f

    private val items = mutableStateListOf<IdeaNode>()
    private val linkItems = mutableStateListOf<NodeLink>()

    val nodes: List<IdeaNode> get() = items

    val links: List<NodeLink> get() = linkItems

    fun nodesOf(kind: String): List<IdeaNode> = items.filter { it.kind == kind }

    fun byId(id: String): IdeaNode? = items.firstOrNull { it.id == id }

    fun linksOf(nodeId: String): List<NodeLink> =
        linkItems.filter { it.fromId == nodeId || it.toId == nodeId }

    /** Links of a node, ready to render as sentences. */
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
            )
        }
    }

    fun connectedNames(nodeId: String): List<String> =
        linksOf(nodeId).mapNotNull { byId(it.otherEnd(nodeId))?.name }

    fun isLinked(a: String, b: String, type: String? = null): Boolean =
        linkItems.any { it.connects(a, b) && (type == null || it.type == type) }

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
                        description = o.optString("description"),
                        picUri = if (o.isNull("picUri")) null else o.optString("picUri"),
                        createdAt = o.optLong("createdAt"),
                        x = if (hasPosition) o.optDouble("x", 0.0).toFloat() else (i % 2) * MIGRATION_COL_W,
                        y = if (hasPosition) o.optDouble("y", 0.0).toFloat() else (i / 2) * MIGRATION_ROW_H,
                    ),
                )
            }

            val loadedLinks = ArrayList<NodeLink>()
            if (linksArray != null) {
                for (i in 0 until linksArray.length()) {
                    val o = linksArray.getJSONObject(i)
                    loadedLinks.add(
                        NodeLink(
                            fromId = o.optString("from"),
                            toId = o.optString("to"),
                            // Links written before types existed become the neutral default.
                            type = if (o.has("type")) o.optString("type") else LinkCatalog.DEFAULT_KEY,
                            label = if (o.isNull("label")) null else o.optString("label"),
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
            AppLog.i(
                "NodeStore",
                "loaded " + loaded.size + " node(s) / " + linkItems.size + " link(s) from " + file.absolutePath +
                    " (migrated without position: " + migrated + ", legacyFormat: " + (linksArray == null) + ")",
            )
        } catch (t: Throwable) {
            AppLog.e("NodeStore", "load failed", t)
        }
    }

    fun add(context: Context, node: IdeaNode) {
        items.add(node)
        persist(context)
        AppLog.i(
            "NodeStore",
            "node added: kind=" + node.kind + " name='" + node.name + "' priority=" + node.priority +
                " pos=(" + node.x.toInt() + "," + node.y.toInt() + ") hasPic=" + (node.picUri != null) +
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
        val candidate = NodeLink(fromId, toId, typeKey, label)
        if (linkItems.any { it.sameAs(candidate) }) return "exists"
        linkItems.add(candidate)
        persist(context)
        AppLog.i(
            "NodeStore",
            "link added: '" + (byId(fromId)?.name ?: fromId) + "' " +
                LinkCatalog.displayLabel(LinkCatalog.byKey(typeKey), label) +
                " '" + (byId(toId)?.name ?: toId) + "' total=" + linkItems.size,
        )
        return "ok"
    }

    /** Removes one link. Returns it, so the UI can offer Undo. */
    fun removeLink(context: Context, link: NodeLink): NodeLink? {
        val index = linkItems.indexOfFirst { it.sameAs(link) }
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
        if (linkItems.none { it.sameAs(link) }) {
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
            if (byId(l.fromId) != null && byId(l.toId) != null && linkItems.none { it.sameAs(l) }) {
                linkItems.add(l)
            }
        }
        persist(context)
        AppLog.i("NodeStore", "node restored: '" + node.name + "' links=" + links.size)
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
                o.put("description", n.description)
                o.put("picUri", n.picUri ?: JSONObject.NULL)
                o.put("createdAt", n.createdAt)
                o.put("x", n.x.toDouble())
                o.put("y", n.y.toDouble())
                nodesArray.put(o)
            }
            val linksArray = JSONArray()
            linkItems.forEach { l ->
                val o = JSONObject()
                o.put("from", l.fromId)
                o.put("to", l.toId)
                o.put("type", l.type)
                o.put("label", l.label ?: JSONObject.NULL)
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