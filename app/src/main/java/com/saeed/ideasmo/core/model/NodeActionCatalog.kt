package com.saeed.ideasmo.core.model

/**
 * The families the actions belong to. They still describe what an action is about, even though the
 * box is now one flat row of sixteen.
 */
enum class NodeActionGroup(val key: String, val title: String, val detail: String) {
    Content("content", "Content", "What the node says and carries"),
    Branch("branch", "Branch", "What hangs off the node"),
    Style("style", "Style", "How the card looks"),
    Clipboard("clipboard", "Clipboard & delete", "Move the node around"),
}

/**
 * One thing the user can do to a node.
 *
 * [key] is the stable id, [label] is what the button says, and [description] is the
 * machine-readable explanation of what the action does **and which parameters it takes**.
 *
 * Why this exists as data rather than as buttons in a layout file: the action box is generated from
 * this list, Help reads it, and the language-model layer that is planned for later gets a complete,
 * stable list of the app's node operations without anyone having to re-describe them. Adding an
 * action means adding one entry here.
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
    /**
     * True when the action has a place in the node's action box. Saeed fixed the box's exact order and
     * set; anything outside it stays reachable from the node sheet, so no capability is lost.
     */
    val inBox: Boolean = true,
)

object NodeActionCatalog {

    const val TEXT = "node.text"
    const val IMAGE = "node.image"
    const val FILES = "node.files"
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

    /**
     * **Arrange** (v0.15.0) — how this node's branch is laid out, and whether it is arranged at all.
     *
     * Saeed's order: "the arrange part is in the board's general settings; bring it into the node's
     * menu, so that by clicking on it, which way the arrange goes is set" — plus a **Free** option:
     * "if it is on Free the user can move it freely, otherwise it must obey the arrangement that was
     * set, so if the arrange is Up it stays Up".
     */
    const val ARRANGE = "node.arrange"

    /**
     * Every node action, **in the order Saeed fixed for the box** (v0.12.0):
     * Name · Child · Sibling · Connect · Note · Image · Files · Shape · Colour · Resize ·
     * Copy · Cut · Paste · Delete · Reattach · Arrange · More.
     *
     * (Arrange joined the end of the branch group in v0.15.0: it is a branch operation, and putting
     * it there leaves every position above unchanged and keeps More as the last door.)
     *
     * `Duplicate` is deliberately NOT in the box (his list does not include it) but it stays in the
     * catalogue, so it still exists — the node sheet offers it. Keys are stable ids: `node.text` is
     * what he renamed to **Name**, and its id is unchanged so stored data and the model layer keep
     * working.
     */
    val all: List<NodeAction> = listOf(
        NodeAction(
            TEXT, "Name", NodeActionGroup.Content,
            "Rename the node — its name is what the card shows. Parameter: name (string). Writes " +
                "IdeaNode.name and stamps updatedAt. (This action used to be called Text.)",
        ),
        NodeAction(
            CHILD, "SubNode", NodeActionGroup.Branch,
            "Create a new node UNDER this one (a SubNode) and link it, then name it. Parameters: name " +
                "(string), link type (defaults to LinkCatalog's neutral type). The box shows how many " +
                "children the node already has.",
            mutatesStructure = true,
        ),
        NodeAction(
            SIBLING, "CoNode", NodeActionGroup.Branch,
            "Create a new node BESIDE this one, under the same parent (a CoNode), then name it. Refused " +
                "when this node has no parent (nothing points at it) — use SubNode instead.",
            mutatesStructure = true,
        ),
        NodeAction(
            CONNECT, "Connect", NodeActionGroup.Content,
            "Start a new typed relation from this node: choose a type from LinkCatalog, then tap the " +
                "target. Creates a NodeLink with a fresh id and appends it to the branch order. The box " +
                "shows how many links the node already has.",
            needsTargetNode = true, mutatesStructure = true,
        ),
        NodeAction(
            NOTES, "Note", NodeActionGroup.Content,
            "The node's notes — a LIST with no limit. The action shows how many there are; opening it " +
                "lists them (read, edit, delete) and adds new ones. Writes IdeaNode.notes (a list of " +
                "strings).",
        ),
        NodeAction(
            IMAGE, "Image", NodeActionGroup.Content,
            "The node's pictures — a LIST with no limit. The action shows how many there are; opening " +
                "it shows the thumbnails, deletes them and adds new ones from the system picker. Each " +
                "file is copied into the app so it survives restarts. Writes IdeaNode.pictures.",
        ),
        NodeAction(
            FILES, "Files", NodeActionGroup.Content,
            "Attach any kind of file to the node — a LIST with no limit (v0.12.0). The action shows how " +
                "many are attached; opening it lists them, opens them, deletes them and adds new ones " +
                "through the system document picker. Writes IdeaNode.files.",
        ),
        NodeAction(
            SHAPE, "Shape", NodeActionGroup.Style,
            "Change the card shape. Parameter: a shape key from NodeStyle.SHAPES (rounded, square, " +
                "pill, cut corner). Writes IdeaNode.shape.",
        ),
        NodeAction(
            COLOR, "Colour", NodeActionGroup.Style,
            "Change the card colour. Parameter: a colour role from NodeStyle.COLORS, or none for the " +
                "plain card. Roles only, so day/night and dynamic colour keep working.",
        ),
        NodeAction(
            RESIZE, "Resize", NodeActionGroup.Style,
            "Change the card size by dragging the eight handles on the card. Writes IdeaNode.w / h in " +
                "board units; the board and the auto-layout both use them.",
        ),
        NodeAction(
            COPY, "Copy", NodeActionGroup.Clipboard,
            "Copy this node — and, if you choose, its links and its sub-links — onto the app clipboard.",
        ),
        NodeAction(
            CUT, "Cut", NodeActionGroup.Clipboard,
            "Copy this node onto the app clipboard and remove it from the board. Undo brings the node " +
                "and all of its links back.",
            mutatesStructure = true,
        ),
        NodeAction(
            PASTE, "Paste", NodeActionGroup.Clipboard,
            "Create a copy of the clipboard content at the centre of the current view, with new ids. " +
                "Shown unavailable when the clipboard is empty.",
            mutatesStructure = true,
        ),
        NodeAction(
            DELETE, "Delete", NodeActionGroup.Clipboard,
            "Remove the node and every link that touches it. Undo restores the node and its links.",
            mutatesStructure = true,
        ),
        NodeAction(
            REATTACH, "Reattach", NodeActionGroup.Branch,
            "Move this node — and everything under it — under a different parent: tap the new " +
                "parent. Replaces this node's incoming links and refuses a parent that sits inside " +
                "its own subtree, so a branch can never be folded into itself.",
            needsTargetNode = true, mutatesStructure = true,
        ),
        NodeAction(
            ARRANGE, "Arrange", NodeActionGroup.Branch,
            "How this node's branch is laid out: Right, Left, Up, Down or Radial — or Free, which " +
                "hands the position back to you. Choosing a direction lays the branch out at once AND " +
                "keeps the choice on the node: the whole-map Arrange follows it, and dragging the node " +
                "moves it together with its branch so the arrangement is not broken. Free is the " +
                "default. Parameter: a LayoutDirection key (right / left / up / down / radial) or null " +
                "for Free. Writes IdeaNode.arrange.",
            mutatesStructure = true,
        ),
        NodeAction(
            MORE, "More", NodeActionGroup.Content,
            "Open the node sheet: the node pinned at the top, its links out and in, the branch order, " +
                "arrange, its machine-readable details and Duplicate.",
        ),
        // ---- kept, but not in the box: his order for the box does not include it ----
        NodeAction(
            DUPLICATE, "Duplicate", NodeActionGroup.Branch,
            "Create a copy of this node — and, if you choose, its links and its sub-links — offset " +
                "slightly. Offered from the node sheet.",
            mutatesStructure = true, inBox = false,
        ),
    )

    /** The actions the node's action box shows, in his order. */
    val box: List<NodeAction> = all.filter { it.inBox }

    private val byKey: Map<String, NodeAction> = all.associateBy { it.key }

    fun byKey(key: String): NodeAction? = byKey[key]

    fun byGroup(group: NodeActionGroup): List<NodeAction> = all.filter { it.group == group }

    /** One line describing every action, for Help and for the model layer. */
    fun describeAll(): String = all.joinToString("\n") { action ->
        action.key + " | " + action.label + " | " + action.description
    }
}