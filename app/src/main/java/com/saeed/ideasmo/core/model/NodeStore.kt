package com.saeed.ideasmo.core.model

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.saeed.ideasmo.core.log.AppLog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class IdeaNode(
    val id: String,
    val name: String,
    val priority: Int,
    val description: String,
    val picUri: String?,
    val createdAt: Long,
)

/**
 * Node store. Compose-observable in memory, persisted to a JSON file in the app's
 * private files dir (org.json is part of the platform — no dependency added).
 * This is the seed of the node manager the Idea screen is heading towards.
 */
object NodeStore {

    private const val FILE_NAME = "nodes.json"

    private val items = mutableStateListOf<IdeaNode>()

    val nodes: List<IdeaNode> get() = items

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
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                loaded.add(
                    IdeaNode(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        priority = o.optInt("priority", 3),
                        description = o.optString("description"),
                        picUri = if (o.isNull("picUri")) null else o.optString("picUri"),
                        createdAt = o.optLong("createdAt"),
                    ),
                )
            }
            items.clear()
            items.addAll(loaded)
            AppLog.i("NodeStore", "loaded " + loaded.size + " node(s) from " + file.absolutePath)
        } catch (t: Throwable) {
            AppLog.e("NodeStore", "load failed", t)
        }
    }

    fun add(context: Context, node: IdeaNode) {
        items.add(0, node)
        save(context)
        AppLog.i(
            "NodeStore",
            "node added: name='" + node.name + "' priority=" + node.priority +
                " hasPic=" + (node.picUri != null) + " total=" + items.size,
        )
    }

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    private fun save(context: Context) {
        try {
            val array = JSONArray()
            items.forEach { n ->
                val o = JSONObject()
                o.put("id", n.id)
                o.put("name", n.name)
                o.put("priority", n.priority)
                o.put("description", n.description)
                o.put("picUri", n.picUri ?: JSONObject.NULL)
                o.put("createdAt", n.createdAt)
                array.put(o)
            }
            file(context).writeText(array.toString())
            AppLog.i("NodeStore", "saved " + items.size + " node(s) to " + file(context).absolutePath)
        } catch (t: Throwable) {
            AppLog.e("NodeStore", "save failed", t)
        }
    }
}