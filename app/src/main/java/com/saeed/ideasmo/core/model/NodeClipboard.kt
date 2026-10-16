package com.saeed.ideasmo.core.model

import androidx.compose.runtime.mutableStateOf
import com.saeed.ideasmo.core.log.AppLog

/**
 * The app's own clipboard, used by Copy / Cut / Paste.
 *
 * It holds a **fragment** — a node plus, optionally, its links and their sub-links — because a
 * branch is usually what a person means by "copy this". Pasting rebuilds every node and every link
 * with fresh ids and rewires the links to the copies, so a paste can never collide with the original.
 *
 * Memory-only on purpose: a clipboard that survives a restart would paste something the user has
 * long forgotten about. The state is observable so the Paste button can be shown as unavailable
 * while the clipboard is empty instead of failing silently.
 */
object NodeClipboard {

    private val held = mutableStateOf<NodeFragment?>(null)
    private val cutSource = mutableStateOf(false)

    val content: NodeFragment? get() = held.value

    val hasContent: Boolean get() = held.value?.isEmpty == false

    /** True when the clipboard came from a Cut, so Paste can say where the nodes came from. */
    val fromCut: Boolean get() = cutSource.value

    fun copy(fragment: NodeFragment) {
        held.value = fragment
        cutSource.value = false
        AppLog.i("Clipboard", "copied " + describe(fragment))
    }

    fun cut(fragment: NodeFragment) {
        held.value = fragment
        cutSource.value = true
        AppLog.i("Clipboard", "cut " + describe(fragment))
    }

    fun clear() {
        held.value = null
        cutSource.value = false
        AppLog.i("Clipboard", "cleared")
    }

    private fun describe(fragment: NodeFragment): String =
        fragment.nodes.size.toString() + " node(s) / " + fragment.links.size + " link(s)"
}