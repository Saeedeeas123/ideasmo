package com.saeed.ideasmo.core.prefs

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.saeed.ideasmo.core.log.AppLog
import com.saeed.ideasmo.core.model.NodeStyle
import com.saeed.ideasmo.core.model.StructureLayout

/**
 * **Node settings** — the defaults every NEW node starts with (v0.17.0).
 *
 * Saeed's order for the settings round was that each kind of setting lives in its own place: the
 * board's in one, the links' in one, the nodes' in one, the app's in one. This is the nodes' place.
 *
 * What belongs here is only what a *new* node is born with: its priority, its card shape, its colour
 * and its size. Everything else about a node (its name, its notes, its pictures, its arrangement) is
 * the node's own data and is edited on the node — a default for those would be meaningless.
 *
 * The per-node panels still override any of it: this only decides where a fresh card starts.
 */
object NodePrefs {

    private const val FILE = "ideasmo_nodes"
    private const val KEY_PRIORITY = "node_priority"
    private const val KEY_SHAPE = "node_shape"
    private const val KEY_COLOR = "node_color"
    private const val KEY_WIDTH = "node_width"
    private const val KEY_HEIGHT = "node_height"

    const val DEFAULT_PRIORITY = 3

    private val priorityState = mutableStateOf(DEFAULT_PRIORITY)
    private val shapeState = mutableStateOf(NodeStyle.SHAPE_ROUNDED)
    private val colorState = mutableStateOf<String?>(null)
    private val widthState = mutableStateOf(StructureLayout.NODE_W)
    private val heightState = mutableStateOf(StructureLayout.NODE_H)

    private var loaded = false

    val priority: Int get() = priorityState.value
    val shapeKey: String get() = shapeState.value

    /** Null = the plain card, exactly like a node's own colour. */
    val colorKey: String? get() = colorState.value

    val width: Float get() = widthState.value
    val height: Float get() = heightState.value

    fun init(context: Context) {
        if (loaded) return
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        priorityState.value = prefs.getInt(KEY_PRIORITY, DEFAULT_PRIORITY).coerceIn(1, 5)
        shapeState.value = prefs.getString(KEY_SHAPE, NodeStyle.SHAPE_ROUNDED) ?: NodeStyle.SHAPE_ROUNDED
        colorState.value = prefs.getString(KEY_COLOR, null)
        widthState.value = prefs.getFloat(KEY_WIDTH, StructureLayout.NODE_W)
        heightState.value = prefs.getFloat(KEY_HEIGHT, StructureLayout.NODE_H)
        loaded = true
        AppLog.i(
            "NodePrefs",
            "node defaults loaded: priority=" + priorityState.value + " shape=" + shapeState.value +
                " color=" + (colorState.value ?: "default") +
                " size=" + widthState.value.toInt() + "x" + heightState.value.toInt(),
        )
    }

    fun setPriority(context: Context, value: Int) {
        priorityState.value = value.coerceIn(1, 5)
        edit(context).putInt(KEY_PRIORITY, priorityState.value).apply()
        AppLog.i("NodePrefs", "default node priority -> " + priorityState.value)
    }

    fun setShape(context: Context, key: String) {
        shapeState.value = key
        edit(context).putString(KEY_SHAPE, key).apply()
        AppLog.i("NodePrefs", "default node shape -> $key")
    }

    fun setColor(context: Context, key: String?) {
        colorState.value = key
        edit(context).apply {
            if (key == null) remove(KEY_COLOR) else putString(KEY_COLOR, key)
        }.apply()
        AppLog.i("NodePrefs", "default node colour -> " + (key ?: "default"))
    }

    /** The card size a new node starts at — a preset from the style sheet, or a resized pair. */
    fun setSize(context: Context, width: Float, height: Float) {
        widthState.value = width
        heightState.value = height
        edit(context).putFloat(KEY_WIDTH, width).putFloat(KEY_HEIGHT, height).apply()
        AppLog.i("NodePrefs", "default node size -> " + width.toInt() + "x" + height.toInt())
    }

    private fun edit(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
}