package com.saeed.ideasmo.core.model

/** The four bars of the node action square, in the order they are drawn. */
enum class NodeActionGroup(val key: String, val title: String, val detail: String) {
    Content("content", "Content", "What the node says"),
    Clipboard("clipboard", "Clipboard & delete", "Move the node around"),
    Branch("branch", "Branch", "What hangs off the node"),
    Style("style", "Style", "How the card looks"),
}

/**
 * One thing the user can do to a node.
 *
 * [key] is the stable id, [label] is what the button says, and [description] is the
 * machine-readable explanation of what the action does **and which parameters it takes**.
 *
 * Why this exists as data rather than as buttons in a layout file: the four bars are generated
 * from this list, Help reads it, and the language-model layer that is planned for later gets a
 * complete, stable list of the app's node operations without anyone having to re-describe them.
 * Adding an action means adding one entry here.
 */
data class NodeAction(
    val key: String,
    val label: String,
    val group: NodeActionGroup,
    val description: String,
    /** True when the action asks the user to tap another node before it can complete. */
    val needsTargetNode: Boolean = false,
    /** True when the action changes the node/link structure (so it always offers Undo). */
    val mutatesStructure: Boolean = false,
)

object NodeActionCatalog {

    const val TEXT = "node.text"
    const val IMAGE = "node.image"
    const val CONNECT = "node.connect"
    const val NOTES = "node.notes"
    const val MORE = "node.more"

    const val COPY = "node.copy"
    const val CUT = "node.cut"
    const val PASTE = "node.paste"
    const val DELETE = "node.delete"

    const val CHILD = "node.child"
    const val SIBLING = "node.sibling"
    const val DUPLICATE = "node.duplicate"
    const val REATTACH = "node.reattach"

    const val RESIZE = "node.resize"
    const val COLOR = "node.color"
    const val SHAPE = "node.shape"

    val all: List<NodeAction> = listOf(
        // ---- top bar: what the node says ------------------------------------
        NodeAction(
            TEXT, "Text", NodeActionGroup.Content,
            "Rename the node. Parameter: name (string). Writes IdeaNode.name and stamps updatedAt.",
        ),
        NodeAction(
            IMAGE, "Image", NodeActionGroup.Content,
            "Attach, replace or remove the node's picture. Parameter: picture (an image from the " +
                "system picker) or none. The file is copied into the app so it survives restarts; " +
                "writes IdeaNode.picUri.",
        ),
        NodeAction(
            CONNECT, "Connect", NodeActionGroup.Content,
            "Start a new typed relation from this node: choose a type from LinkCatalog, then tap the " +
                "target. Creates a NodeLink with a fresh id and appends it to the branch order.",
            needsTargetNode = true, mutatesStructure = true,
        ),
        NodeAction(
            NOTES, "Notes", NodeActionGroup.Content,
            "Edit the node's notes — the free text behind the card's preview. Parameter: notes " +
                "(string). Writes IdeaNode.description.",
        ),
        NodeAction(
            MORE, "More", NodeActionGroup.Content,
            "Open everything else about this node: the links that leave it and arrive at it, the " +
                "branch order, the structure controls, and its machine-readable details (id, kind, " +
                "coordinates, timestamps).",
        ),

        // ---- left bar: clipboard and delete ---------------------------------
        NodeAction(
            COPY, "Copy", NodeActionGroup.Clipboard,
            "Copy this node onto the app clipboard, without its links. Paste puts it back wherever " +
                "the view is at that moment.",
        ),
        NodeAction(
            CUT, "Cut", NodeActionGroup.Clipboard,
            "Copy this node onto the app clipboard and remove it from the board. Undo brings the " +
                "node and all of its links back.",
            mutatesStructure = true,
        ),
        NodeAction(
            PASTE, "Paste", NodeActionGroup.Clipboard,
            "Create a copy of the clipboard node at the centre of the current view, with a new id. " +
                "Does nothing when the clipboard is empty.",
            mutatesStructure = true,
        ),
        NodeAction(
            DELETE, "Delete", NodeActionGroup.Clipboard,
            "Remove the node and every link that touches it. Undo restores the node and its links.",
            mutatesStructure = true,
        ),

        // ---- right bar: the branch ------------------------------------------
        NodeAction(
            CHILD, "Child", NodeActionGroup.Branch,
            "Create a new node and link it as a child of this one, then name it. Parameters: name " +
                "(string), link type (defaults to LinkCatalog's neutral type).",
            mutatesStructure = true,
        ),
        NodeAction(
            SIBLING, "Sibling", NodeActionGroup.Branch,
            "Create a new node under the same parent as this one, then name it. Refused when this " +
                "node has no parent (nothing points at it) — use Child instead.",
            mutatesStructure = true,
        ),
        NodeAction(
            DUPLICATE, "Duplicate", NodeActionGroup.Branch,
            "Create an unlinked copy of this node, offset slightly, keeping its name, notes, " +
                "priority, colour, shape and size.",
            mutatesStructure = true,
        ),
        NodeAction(
            REATTACH, "Reattach", NodeActionGroup.Branch,
            "Move this node — and everything under it — under a different parent: tap the new " +
                "parent. Replaces this node's incoming links and refuses a parent that sits inside " +
                "its own subtree, so a branch can never be folded into itself.",
            needsTargetNode = true, mutatesStructure = true,
        ),

        // ---- bottom bar: style ----------------------------------------------
        NodeAction(
            RESIZE, "Resize", NodeActionGroup.Style,
            "Change the card size. Parameter: size (one of NodeStyle.SIZES: small, normal, large, " +
                "extra large). Writes IdeaNode.size; the board and the auto-layout both use it.",
        ),
        NodeAction(
            COLOR, "Colour", NodeActionGroup.Style,
            "Change the card colour. Parameter: a colour role from NodeStyle.COLORS, or none for the " +
                "plain card. Roles only, so day/night and dynamic colour keep working.",
        ),
        NodeAction(
            SHAPE, "Shape", NodeActionGroup.Style,
            "Change the card shape. Parameter: a shape key from NodeStyle.SHAPES (rounded, square, " +
                "pill, cut corner). Writes IdeaNode.shape.",
        ),
    )

    private val byKey: Map<String, NodeAction> = all.associateBy { it.key }

    fun byKey(key: String): NodeAction? = byKey[key]

    fun byGroup(group: NodeActionGroup): List<NodeAction> = all.filter { it.group == group }

    /** One line describing every action, for Help and for the model layer. */
    fun describeAll(): String = all.joinToString("\n") { action ->
        action.key + " | " + action.label + " | " + action.description
    }
}