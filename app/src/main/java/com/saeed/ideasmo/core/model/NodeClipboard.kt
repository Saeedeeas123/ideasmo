package com.saeed.ideasmo.core.model

import androidx.compose.runtime.mutableStateOf
import com.saeed.ideasmo.core.log.AppLog

/**
 * The app's own node clipboard, used by Copy / Cut / Paste.
 *
 * It holds a **node**, not text: pasting rebuilds the node with a fresh id, so a copy can never
 * collide with the original or with a link. It is memory-only on purpose — a clipboard that
 * survives a restart would paste something the user has long forgotten about.
 *
 * The state is observable, so the Paste button can be shown as unavailable while the clipboard is
 * empty instead of failing silently.
 */
object NodeClipboard {

    private val held = mutableStateOf<IdeaNode?>(null)
    private val cutSource = mutableStateOf(false)

    val content: IdeaNode? get() = held.value

    val hasContent: Boolean get() = held.value != null

    /** True when the clipboard came from a Cut, so Paste can say where the node came from. */
    val fromCut: Boolean get() = cutSource.value

    fun copy(node: IdeaNode) {
        held.value = node
        cutSource.value = false
        AppLog.i("Clipboard", "copied node '" + node.name + "' (" + node.id + ")")
    }

    fun cut(node: IdeaNode) {
        held.value = node
        cutSource.value = true
        AppLog.i("Clipboard", "cut node '" + node.name + "' (" + node.id + ")")
    }

    fun clear() {
        held.value = null
        cutSource.value = false
        AppLog.i("Clipboard", "cleared")
    }
}