package com.saeed.ideasmo.core.model

/** A position on the board, in board units. */
data class LayoutPoint(val x: Float, val y: Float)

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
    ): Map<String, LayoutPoint> {
        if (nodes.isEmpty()) return emptyMap()
        val byId = nodes.associateBy { it.id }
        val children = childrenMap(nodes, links)
        val roots = resolveRoots(nodes, links, preferredRoots).filter { byId.containsKey(it) }
        if (roots.isEmpty()) return emptyMap()
        val laidOut = if (direction == LayoutDirection.Radial) {
            radial(byId, children, roots, spacing, originX, originY)
        } else {
            linear(byId, children, roots, direction, spacing, originX, originY)
        }
        return if (anchorRootAtOrigin) anchorAt(laidOut, roots.first(), originX, originY) else laidOut
    }

    /**
     * Translates a finished layout so [anchorId]'s centre lands on the origin.
     * Done after the layout rather than during it, so it works for every direction — including
     * Radial, where a parent's position is an angle, not a cursor.
     */
    private fun anchorAt(
        positions: Map<String, LayoutPoint>,
        anchorId: String,
        originX: Float,
        originY: Float,
    ): Map<String, LayoutPoint> {
        val anchor = positions[anchorId] ?: return positions
        val dx = originX - (anchor.x + NODE_HALF_W)
        val dy = originY - (anchor.y + NODE_HALF_H)
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
    ): Map<String, LayoutPoint> {
        val horizontal = direction == LayoutDirection.Right || direction == LayoutDirection.Left
        val extent = if (horizontal) NODE_H else NODE_W
        val gap = spacing.siblingGap
        val raw = LinkedHashMap<String, Pair<Int, Float>>()
        val visited = HashSet<String>()

        fun place(id: String, depth: Int, start: Float): Float {
            if (!visited.add(id)) return 0f
            val node = byId[id] ?: return 0f
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

        val levelStep = if (horizontal) NODE_W + spacing.levelGap else NODE_H + spacing.levelGap
        return raw.mapValues { (_, value) ->
            val depth = value.first
            val along = value.second
            when (direction) {
                LayoutDirection.Right -> LayoutPoint(originX + depth * levelStep, originY + along)
                LayoutDirection.Left -> LayoutPoint(originX - depth * levelStep - NODE_W, originY + along)
                LayoutDirection.Down -> LayoutPoint(originX + along, originY + depth * levelStep)
                LayoutDirection.Up -> LayoutPoint(originX + along, originY - depth * levelStep - NODE_H)
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
    ): Map<String, LayoutPoint> {
        val radiusStep = 200f + spacing.levelGap
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

        return raw.mapValues { (_, value) ->
            val angle = value.first
            val radius = value.second
            LayoutPoint(
                originX + kotlin.math.cos(angle) * radius - NODE_HALF_W,
                originY + kotlin.math.sin(angle) * radius - NODE_HALF_H,
            )
        }
    }
}