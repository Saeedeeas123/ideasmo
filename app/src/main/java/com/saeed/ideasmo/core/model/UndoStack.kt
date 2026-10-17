package com.saeed.ideasmo.core.model

import androidx.compose.runtime.mutableStateListOf
import com.saeed.ideasmo.core.log.AppLog

/** One undoable step: a name for the user and the work that puts things back. */
data class UndoEntry(val label: String, val undo: () -> Unit)

/**
 * The app's undo stack.
 *
 * It exists because the floating message bar was removed (Saeed's order: the toast was more
 * annoying than useful). The bar was also the only way back from a delete, so undo had to become a
 * **visible, always-available control** instead of something that appeared for four seconds and
 * then vanished — which is a better guarantee anyway: you can still undo after the moment has
 * passed.
 *
 * Memory-only, bounded, and observable so the toolbar's Undo button can enable itself.
 */
object UndoStack {

    private const val MAX_ENTRIES = 12

    private val entries = mutableStateListOf<UndoEntry>()
    private val redoEntries = mutableStateListOf<UndoEntry>()

    val canUndo: Boolean get() = entries.isNotEmpty()

    /** True when there is something on the redo side (Saeed's v0.12.0 toolbar has a Redo button). */
    val canRedo: Boolean get() = redoEntries.isNotEmpty()

    val size: Int get() = entries.size

    /** What the next Undo would put back — shown on the button so it is never a mystery. */
    val nextLabel: String? get() = entries.lastOrNull()?.label

    /** What the next Redo would put back. */
    val nextRedoLabel: String? get() = redoEntries.lastOrNull()?.label

    fun push(label: String, undo: () -> Unit) {
        entries.add(UndoEntry(label, undo))
        while (entries.size > MAX_ENTRIES) {
            entries.removeAt(0)
        }
        // A new action starts a new branch of history: whatever could have been redone is gone.
        redoEntries.clear()
        AppLog.i("Undo", "stacked: " + label + " (depth " + entries.size + ")")
    }

    /** Runs the most recent entry. Returns its label, or null when there was nothing to undo. */
    fun undo(context: android.content.Context): String? {
        val entry = entries.removeLastOrNull() ?: run {
            AppLog.i("Undo", "nothing to undo")
            return null
        }
        // Redo is built from whole-board snapshots rather than from a second closure per action: the
        // state as it is RIGHT NOW is exactly what a Redo has to bring back.
        val after = NodeStore.snapshot()
        entry.undo()
        redoEntries.add(UndoEntry(entry.label) { NodeStore.restoreSnapshot(context, after) })
        AppLog.i("Undo", "undone: " + entry.label + " (depth " + entries.size + ")")
        return entry.label
    }

    /** Puts back what the last Undo removed. Returns its label, or null when there was nothing. */
    fun redo(context: android.content.Context): String? {
        val entry = redoEntries.removeLastOrNull() ?: run {
            AppLog.i("Undo", "nothing to redo")
            return null
        }
        val before = NodeStore.snapshot()
        entry.undo()
        entries.add(UndoEntry(entry.label) { NodeStore.restoreSnapshot(context, before) })
        AppLog.i("Undo", "redone: " + entry.label + " (undo depth " + entries.size + ")")
        return entry.label
    }

    fun clear() {
        entries.clear()
        redoEntries.clear()
        AppLog.i("Undo", "stack cleared")
    }
}