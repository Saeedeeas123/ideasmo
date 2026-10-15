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
 * Node store. Compose-observable in memory, persisted to a JSON file in the app's
 * private files dir (org.json is part of the platform — no dependency added).
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

    val nodes: List<IdeaNode> get() = items

    fun nodesOf(kind: String): List<IdeaNode> = items.filter { it.kind == kind }

    fun byId(id: String): IdeaNode? = items.firstOrNull { it.id == id }

    fun newId(): String = UUID.randomUUID().toString()

    fun init(context: Context) {
        if (items.isNotEmpty()) return
        val file = file(context)
        if (!file.exists()) {
            AppLog.i("NodeStore", "no node file yet: " + file.absolutePath)
            return
        }
        try {
            val array = JSONArray(file.readText())
            val loaded = ArrayList<IdeaNode>(array.length())
            var migrated = 0
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                // Nodes written before the board existed have no x / y — lay them out on a grid
                // so they are visible instead of stacked on the origin.
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
            items.clear()
            items.addAll(loaded)
            AppLog.i(
                "NodeStore",
                "loaded " + loaded.size + " node(s) from " + file.absolutePath +
                    " (migrated without position: " + migrated + ")",
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

    fun persist(context: Context) {
        try {
            val array = JSONArray()
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
                array.put(o)
            }
            file(context).writeText(array.toString())
            AppLog.d("NodeStore", "saved " + items.size + " node(s)")
        } catch (t: Throwable) {
            AppLog.e("NodeStore", "save failed", t)
        }
    }

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)
}