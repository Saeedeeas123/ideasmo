package com.saeed.ideasmo.core.model

/** A position on the board, in board units. */
data class LayoutPoint(val x: Float, val y: Float)

/** A size on the board, in board units. */
data class LayoutSize(val w: Float, val h: Float)

/** Which way a structure grows out of its central node. */
enum class LayoutDirection(val key: String, val label: String) {
    Right("right", "Right"),
    Left("left", "Left"),
    Down("down", "Down"),
    Up("up", "Up"),
    Radial("radial", "Radial"),
    ;

    companion object {
        fun fromKey(key: String?): LayoutDirection =
            values().firstOrNull { it.key == key } ?: Right
    }
}

/** How much room the auto-layout leaves between a parent and its children, and between siblings. */
enum class LayoutSpacing(val key: String, val label: String, val levelGap: Float, val siblingGap: Float) {
    Compact("compact", "Compact", 60f, 20f),
    Normal("normal", "Normal", 120f, 40f),
    Wide("wide", "Wide", 200f, 80f),
    ;

    companion object {
        fun fromKey(key: String?): LayoutSpacing =
            values().firstOrNull { it.key == key } ?: Normal
    }
}

/**
 * The structure engine: turns "a central node plus its typed links" into positions on the board.
 *
 * Grounded in how established mind-map tools behave (Xmind documents *structures* — mind map,
 * logic chart, tree, timeline — plus *Advanced Layout* with free branch positioning, and a
 * *Reorganize* command that tidies a map in one click; Miro, Whimsical and FigJam offer the same
 * "arrange the tree" idea with a direction). Three rules are taken from that behaviour:
 *
 * 1. **A manual position is never a mistake.** When the user has designated central nodes, only the
 *    trees that start at them are moved — everything else keeps the position the user gave it. With
 *    no central node at all the engine falls back to "every node nothing points at is a root", so
 *    Arrange still does something useful on a map that has never been structured.
 * 2. **Sibling order is user data, not a side effect.** The order comes from the stored branch
 *    order of the outgoing links, so what the user ordered is what gets drawn.
 * 3. **Collapsed means hidden, not deleted.** A collapsed node's subtree is not laid out.
 *
 * This file is deliberately pure Kotlin (no Android, no Compose) so the engine can be tested on
 * the JVM without a device.
 */
object StructureLayout {

    /** Node card size in board units — must match the board's own card size. */
    const val NODE_W = 200f
    const val NODE_H = 96f

    /** Half the node size, for callers that draw from the centre. */
    const val NODE_HALF_W = NODE_W / 2f
    const val NODE_HALF_H = NODE_H / 2f

    /** The card's real size in board units — a node can be resized, and the layout must agree. */
    fun widthOf(node: IdeaNode): Float = node.width

    fun heightOf(node: IdeaNode): Float = node.height

    /**
     * The space a node really needs: its stored card size, or the size the card actually rendered at
     * when that is bigger.
     *
     * A card grows with its content — a picture, a long name — so its stored height can be smaller
     * than what is on screen. Laying siblings out by the stored height only is what let branches
     * overlap after Arrange (Saeed's screenshot). The measured map is memory-only data the board
     * reports while it draws (see NodeStore.noteRenderedSize); the engine stays pure, so the JVM
     * tests pass the map in explicitly.
     */
    fun spaceOf(node: IdeaNode, measured: Map<String, LayoutSize> = emptyMap()): LayoutSize {
        val m = measured[node.id]
        return LayoutSize(
            w = maxOf(widthOf(node), m?.w ?: 0f),
            h = maxOf(heightOf(node), m?.h ?: 0f),
        )
    }

    /** The smallest and largest card a user can make by dragging a resize handle. */
    const val MIN_NODE_W = 120f
    const val MAX_NODE_W = 720f
    const val MIN_NODE_H = 64f
    const val MAX_NODE_H = 480f

    /**
     * Arranges the trees that start at the central nodes.
     * Returns positions for every node it touched; nodes outside those trees are absent, so a
     * caller can apply the result as a partial move.
     *
     * When [anchorRootAtOrigin] is true the whole result is translated so that the **centre of the
     * first central node sits exactly on ([originX], [originY])** — the board's fixed origin. That is
     * what makes Arrange deterministic: the result depends on the origin and the branch order only,
     * never on where the nodes happened to be dragged, so running it twice cannot drift.
     */
    fun arrange(
        nodes: List<IdeaNode>,
        links: List<NodeLink>,
        preferredRoots: List<String> = emptyList(),
        direction: LayoutDirection = LayoutDirection.Right,
        spacing: LayoutSpacing = LayoutSpacing.Normal,
        originX: Float = 0f,
        originY: Float = 0f,
        anchorRootAtOrigin: Boolean = false,
        measured: Map<String, LayoutSize> = emptyMap(),
    ): Map<String, LayoutPoint> {
        if (nodes.isEmpty()) return emptyMap()
        val byId = nodes.associateBy { it.id }
        val children = childrenMap(nodes, links)
        val roots = resolveRoots(nodes, links, preferredRoots).filter { byId.containsKey(it) }
        if (roots.isEmpty()) return emptyMap()
        val laidOut = if (direction == LayoutDirection.Radial) {
            radial(byId, children, roots, spacing, originX, originY, measured)
        } else {
            linear(byId, children, roots, direction, spacing, originX, originY, measured)
        }
        return if (anchorRootAtOrigin) {
            anchorAt(laidOut, roots.first(), byId, originX, originY, measured)
        } else {
            laidOut
        }
    }

    /**
     * Arranges ONE branch: the node the user picked **stays exactly where it is** and everything
     * below it is laid out in [direction], following the stored branch order.
     *
     * This is the per-node Arrange (Saeed's order: the direction belongs to the selected node's own
     * properties, not to a button on the board). Only that node's subtree moves, so arranging a
     * branch can never throw the rest of the map somewhere else, and — unlike the whole-map Arrange,
     * which anchors the map on the board origin — nothing jumps under the user's finger.
     *
     * A node with nothing below it produces a single unchanged position, which the caller is expected
     * to report as "nothing to arrange" rather than as a move.
     */
    fun arrangeBranch(
        nodes: List<IdeaNode>,
        links: List<NodeLink>,
        rootId: String,
        direction: LayoutDirection = LayoutDirection.Right,
        spacing: LayoutSpacing = LayoutSpacing.Normal,
        measured: Map<String, LayoutSize> = emptyMap(),
    ): Map<String, LayoutPoint> {
        val byId = nodes.associateBy { it.id }
        val root = byId[rootId] ?: return emptyMap()
        val children = childrenMap(nodes, links)
        val laidOut = if (direction == LayoutDirection.Radial) {
            radial(byId, children, listOf(rootId), spacing, 0f, 0f, measured)
        } else {
            linear(byId, children, listOf(rootId), direction, spacing, 0f, 0f, measured)
        }
        if (laidOut.size <= 1) return emptyMap()
        val space = spaceOf(root, measured)
        return anchorAt(
            positions = laidOut,
            anchorId = rootId,
            byId = byId,
            originX = root.x + space.w / 2f,
            originY = root.y + space.h / 2f,
            measured = measured,
        )
    }

    /**
     * Translates a finished layout so [anchorId]'s centre lands on the origin.
     * Done after the layout rather than during it, so it works for every direction — including
     * Radial, where a parent's position is an angle, not a cursor.
     */
    private fun anchorAt(
        positions: Map<String, LayoutPoint>,
        anchorId: String,
        byId: Map<String, IdeaNode>,
        originX: Float,
        originY: Float,
        measured: Map<String, LayoutSize> = emptyMap(),
    ): Map<String, LayoutPoint> {
        val anchor = positions[anchorId] ?: return positions
        val anchorNode = byId[anchorId]
        val space = if (anchorNode != null) spaceOf(anchorNode, measured) else LayoutSize(NODE_W, NODE_H)
        val halfW = space.w / 2f
        val halfH = space.h / 2f
        val dx = originX - (anchor.x + halfW)
        val dy = originY - (anchor.y + halfH)
        if (dx == 0f && dy == 0f) return positions
        return positions.mapValues { (_, point) -> LayoutPoint(point.x + dx, point.y + dy) }
    }

    /**
     * Which nodes the structure is built around: the ones the user marked as central; otherwise
     * every node nothing points at; otherwise every node (a board with no links at all).
     */
    fun resolveRoots(
        nodes: List<IdeaNode>,
        links: List<NodeLink>,
        preferredRoots: List<String> = emptyList(),
    ): List<String> {
        val ids = nodes.map { it.id }.toSet()
        val preferred = preferredRoots.filter { it in ids }
        if (preferred.isNotEmpty()) return preferred
        val real = links.filter { it.fromId in ids && it.toId in ids && it.fromId != it.toId }
        val targets = real.map { it.toId }.toSet()
        val natural = nodes.filter { it.id !in targets }.map { it.id }
        return natural.ifEmpty { nodes.map { it.id } }
    }

    /** parent id -> child ids, in branch order, duplicates and self-links removed. */
    private fun childrenMap(nodes: List<IdeaNode>, links: List<NodeLink>): Map<String, List<String>> {
        val byId = nodes.associateBy { it.id }
        val result = HashMap<String, MutableList<String>>()
        val orderSeen = HashMap<String, MutableMap<String, Int>>()
        links.sortedBy { it.order }.forEach { link ->
            if (link.fromId == link.toId) return@forEach
            if (byId[link.fromId] == null || byId[link.toId] == null) return@forEach
            val seen = orderSeen.getOrPut(link.fromId) { HashMap() }
            if (seen.containsKey(link.toId)) return@forEach
            seen[link.toId] = link.order
            result.getOrPut(link.fromId) { ArrayList() }.add(link.toId)
        }
        return result
    }

    private fun linear(
        byId: Map<String, IdeaNode>,
        children: Map<String, List<String>>,
        roots: List<String>,
        direction: LayoutDirection,
        spacing: LayoutSpacing,
        originX: Float,
        originY: Float,
        measured: Map<String, LayoutSize>,
    ): Map<String, LayoutPoint> {
        val horizontal = direction == LayoutDirection.Right || direction == LayoutDirection.Left
        // A node can be resized — and a card can render taller than its stored height — so the extent
        // along the sibling axis belongs to the node itself, and the step between two levels must
        // clear the LARGEST card on the board.
        val maxW = byId.values.maxOf { spaceOf(it, measured).w }
        val maxH = byId.values.maxOf { spaceOf(it, measured).h }
        val gap = spacing.siblingGap
        val raw = LinkedHashMap<String, Pair<Int, Float>>()
        val visited = HashSet<String>()

        fun place(id: String, depth: Int, start: Float): Float {
            if (!visited.add(id)) return 0f
            val node = byId[id] ?: return 0f
            val space = spaceOf(node, measured)
            val extent = if (horizontal) space.h else space.w
            val kids = if (node.collapsed) {
                emptyList()
            } else {
                children[id].orEmpty().filter { it !in visited }
            }
            if (kids.isEmpty()) {
                raw[id] = depth to start
                return extent
            }
            var cursor = start
            var placed = 0
            kids.forEach { kid ->
                val used = place(kid, depth + 1, cursor)
                if (used > 0f) {
                    placed++
                    cursor += used + gap
                }
            }
            if (placed == 0) {
                raw[id] = depth to start
                return extent
            }
            val span = cursor - gap - start
            // The parent sits centred on the block of its children — the classic tree look.
            raw[id] = depth to (start + (span - extent) / 2f)
            return maxOf(span, extent)
        }

        // The cursor is measured from the origin, NOT started at it: the transform below already adds
        // originX / originY. Starting it at the origin too (as v0.6.0 did) shifted the whole map by
        // the origin on every Arrange — the drift that made Arrange feel unreliable.
        var cursor = 0f
        roots.forEach { root ->
            val used = place(root, 0, cursor)
            if (used > 0f) cursor += used + gap * 2f
        }

        val levelStep = if (horizontal) maxW + spacing.levelGap else maxH + spacing.levelGap
        return raw.mapValues { (id, value) ->
            val depth = value.first
            val along = value.second
            // The offset back towards the parent uses the NODE'S OWN size, not a constant: a 554-unit
            // card in Left/Up used to be pushed out by the default 200/96, so a resized card ended up
            // straddling the level line instead of starting on it.
            val space = byId[id]?.let { spaceOf(it, measured) } ?: LayoutSize(NODE_W, NODE_H)
            when (direction) {
                LayoutDirection.Right -> LayoutPoint(originX + depth * levelStep, originY + along)
                LayoutDirection.Left -> LayoutPoint(originX - depth * levelStep - space.w, originY + along)
                LayoutDirection.Down -> LayoutPoint(originX + along, originY + depth * levelStep)
                LayoutDirection.Up -> LayoutPoint(originX + along, originY - depth * levelStep - space.h)
                LayoutDirection.Radial -> LayoutPoint(originX + depth * levelStep, originY + along)
            }
        }
    }

    private fun radial(
        byId: Map<String, IdeaNode>,
        children: Map<String, List<String>>,
        roots: List<String>,
        spacing: LayoutSpacing,
        originX: Float,
        originY: Float,
        measured: Map<String, LayoutSize> = emptyMap(),
    ): Map<String, LayoutPoint> {
        val maxW = byId.values.maxOf { spaceOf(it, measured).w }
        // Rings must clear the biggest card, so a resized node can never sit on top of its parent.
        val radiusStep = maxOf(NODE_W, maxW) + spacing.levelGap
        val rootRadius = if (roots.size <= 1) 0f else radiusStep * 0.55f
        val raw = LinkedHashMap<String, Pair<Float, Float>>()
        val visited = HashSet<String>()

        fun place(id: String, depth: Int, startAngle: Float, endAngle: Float) {
            if (!visited.add(id)) return
            val angle = (startAngle + endAngle) / 2f
            val radius = if (depth == 0) rootRadius else radiusStep * depth
            raw[id] = angle to radius
            val node = byId[id] ?: return
            val kids = if (node.collapsed) {
                emptyList()
            } else {
                children[id].orEmpty().filter { it !in visited }
            }
            if (kids.isEmpty()) return
            val span = endAngle - startAngle
            kids.forEachIndexed { i, kid ->
                place(
                    kid,
                    depth + 1,
                    startAngle + span * i / kids.size,
                    startAngle + span * (i + 1) / kids.size,
                )
            }
        }

        val full = (2.0 * Math.PI).toFloat()
        roots.forEachIndexed { i, root ->
            val span = full / roots.size
            place(root, 0, i * span, (i + 1) * span)
        }

        return raw.mapValues { (id, value) ->
            val angle = value.first
            val radius = value.second
            val node = byId[id]
            val space = if (node != null) spaceOf(node, measured) else LayoutSize(NODE_W, NODE_H)
            val halfW = space.w / 2f
            val halfH = space.h / 2f
            LayoutPoint(
                originX + kotlin.math.cos(angle) * radius - halfW,
                originY + kotlin.math.sin(angle) * radius - halfH,
            )
        }
    }
}