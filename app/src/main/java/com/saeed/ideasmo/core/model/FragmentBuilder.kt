package com.saeed.ideasmo.core.model

/**
 * Works out what travels with a Copy / Cut / Duplicate / Paste.
 *
 * Pure Kotlin on purpose — no store, no Android — so the rule is testable on the JVM and is stated
 * in exactly one place:
 *
 * - neither option: the node alone;
 * - [withLinks]: the node plus the nodes it points at directly, and the links that join them;
 * - [withSubLinks]: the node plus its whole subtree, and every link inside it.
 *
 * A link is only included when **both** of its ends are inside the fragment, so a copy can never
 * point at a node that was not copied. The walk is cycle-safe: a branch that loops back on itself
 * still terminates.
 */
object FragmentBuilder {

    fun idsInFragment(
        rootId: String,
        nodes: List<IdeaNode>,
        links: List<NodeLink>,
        withLinks: Boolean,
        withSubLinks: Boolean,
    ): Set<String> {
        val known = nodes.map { it.id }.toSet()
        if (rootId !in known) return emptySet()
        val ids = LinkedHashSet<String>()
        ids.add(rootId)

        val outgoing: Map<String, List<String>> = links
            .filter { it.fromId in known && it.toId in known && it.fromId != it.toId }
            .groupBy({ it.fromId }, { it.toId })

        if (withSubLinks) {
            val queue = ArrayDeque<String>()
            outgoing[rootId].orEmpty().forEach { queue.add(it) }
            while (queue.isNotEmpty()) {
                val next = queue.removeFirst()
                if (next == rootId || !ids.add(next)) continue
                outgoing[next].orEmpty().forEach { queue.add(it) }
            }
        } else if (withLinks) {
            outgoing[rootId].orEmpty().forEach { ids.add(it) }
        }
        return ids
    }

    /** The fragment itself: the nodes, plus every link whose two ends are both inside it. */
    fun build(
        rootId: String,
        nodes: List<IdeaNode>,
        links: List<NodeLink>,
        withLinks: Boolean,
        withSubLinks: Boolean,
    ): NodeFragment? {
        val ids = idsInFragment(rootId, nodes, links, withLinks, withSubLinks)
        if (ids.isEmpty()) return null
        val fragNodes = nodes.filter { it.id in ids }
        val fragLinks = if (ids.size > 1) {
            links.filter { it.fromId in ids && it.toId in ids }
        } else {
            emptyList()
        }
        return NodeFragment(rootId = rootId, nodes = fragNodes, links = fragLinks)
    }

    /** The fragment for a free selection: the chosen nodes and the links among them. */
    fun buildFromSelection(
        ids: List<String>,
        nodes: List<IdeaNode>,
        links: List<NodeLink>,
    ): NodeFragment? {
        val wanted = ids.toSet()
        val fragNodes = nodes.filter { it.id in wanted }
        if (fragNodes.isEmpty()) return null
        return NodeFragment(
            rootId = ids.first(),
            nodes = fragNodes,
            links = links.filter { it.fromId in wanted && it.toId in wanted },
        )
    }
}