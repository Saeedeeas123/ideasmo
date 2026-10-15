package com.saeed.ideasmo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * JVM tests for the structure engine. They run on the plain JVM (no device, no emulator), which is
 * why [StructureLayout] deliberately depends on nothing but Kotlin.
 *
 * What they protect: the branch order a user sets is the order the map is drawn in; a collapsed
 * branch is not laid out; a cycle cannot hang the arranger; and nodes that belong to no tree are
 * left exactly where the user put them.
 */
class StructureLayoutTest {

    private val step = StructureLayout.NODE_W + LayoutSpacing.Normal.levelGap
    private val gap = LayoutSpacing.Normal.siblingGap

    private fun node(
        id: String,
        x: Float = 0f,
        y: Float = 0f,
        collapsed: Boolean = false,
        isRoot: Boolean = false,
    ) = IdeaNode(
        id = id,
        kind = "idea",
        name = id,
        priority = 3,
        description = "",
        picUri = null,
        createdAt = 0L,
        x = x,
        y = y,
        isRoot = isRoot,
        collapsed = collapsed,
    )

    private fun link(id: String, from: String, to: String, order: Int = 0) =
        NodeLink(id = id, fromId = from, toId = to, type = "relates", order = order)

    @Test
    fun rootIsPlacedAtTheOriginAndChildrenFollowTheOrder() {
        val nodes = listOf(node("root"), node("b"), node("a"), node("c"))
        val links = listOf(
            link("l1", "root", "c", 0),
            link("l2", "root", "a", 1),
            link("l3", "root", "b", 2),
        )

        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            direction = LayoutDirection.Right,
            spacing = LayoutSpacing.Normal,
            originX = 0f,
            originY = 0f,
        )

        assertEquals(4, positions.size)
        val root = positions.getValue("root")
        assertEquals(0f, root.x, 0.01f)

        // Branch order 0..2 decides the vertical order, not the node id and not the input order.
        val c = positions.getValue("c")
        val a = positions.getValue("a")
        val b = positions.getValue("b")
        assertTrue("first branch must sit above the second", c.y < a.y)
        assertTrue("second branch must sit above the third", a.y < b.y)
        assertEquals(StructureLayout.NODE_H + gap, a.y - c.y, 0.01f)

        listOf(c, a, b).forEach { child ->
            assertEquals(step, child.x, 0.01f)
        }
        // The parent is centred on the block of its children.
        assertEquals((c.y + b.y) / 2f, root.y, 0.01f)
    }

    @Test
    fun changingTheStoredBranchOrderChangesTheLayout() {
        val nodes = listOf(node("root"), node("a"), node("b"))
        val first = StructureLayout.arrange(
            nodes = nodes,
            links = listOf(link("l1", "root", "a", 0), link("l2", "root", "b", 1)),
        )
        val swapped = StructureLayout.arrange(
            nodes = nodes,
            links = listOf(link("l1", "root", "a", 1), link("l2", "root", "b", 0)),
        )

        assertTrue(first.getValue("a").y < first.getValue("b").y)
        assertTrue(swapped.getValue("b").y < swapped.getValue("a").y)
    }

    @Test
    fun aCollapsedBranchIsNotLaidOut() {
        val nodes = listOf(
            node("root"),
            node("child", collapsed = true),
            node("grandchild"),
        )
        val links = listOf(
            link("l1", "root", "child"),
            link("l2", "child", "grandchild"),
        )

        val positions = StructureLayout.arrange(nodes = nodes, links = links)

        assertNotNull(positions["root"])
        assertNotNull(positions["child"])
        assertFalse("a collapsed node must not pull its subtree into the layout", positions.containsKey("grandchild"))
    }

    @Test
    fun aCycleDoesNotHangTheArranger() {
        val nodes = listOf(node("a"), node("b"))
        val links = listOf(link("l1", "a", "b"), link("l2", "b", "a"))

        val positions = StructureLayout.arrange(nodes = nodes, links = links)

        assertEquals(2, positions.size)
        assertNotNull(positions["a"])
        assertNotNull(positions["b"])
    }

    @Test
    fun whenCentralNodesAreSetOnlyTheirTreesAreMoved() {
        val nodes = listOf(node("root"), node("child"), node("free", x = 900f, y = 900f))
        val links = listOf(link("l1", "root", "child"))

        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = listOf("root"),
        )

        assertTrue(positions.containsKey("root"))
        assertTrue(positions.containsKey("child"))
        assertFalse(
            "a node outside the designated tree keeps the position the user gave it",
            positions.containsKey("free"),
        )
    }

    @Test
    fun withNoCentralNodeEveryNodeNothingPointsAtStartsItsOwnTree() {
        // This is the fallback that makes Arrange useful before a central node is ever set:
        // nodes with no incoming link each become a root, and everything else hangs off them.
        val nodes = listOf(node("root"), node("child"), node("free", x = 900f, y = 900f))
        val links = listOf(link("l1", "root", "child"))

        val positions = StructureLayout.arrange(nodes = nodes, links = links)

        assertTrue(positions.containsKey("root"))
        assertTrue(positions.containsKey("child"))
        assertTrue(positions.containsKey("free"))
    }

    @Test
    fun downDirectionGrowsAlongTheVerticalAxis() {
        val nodes = listOf(node("root"), node("a"), node("b"))
        val links = listOf(link("l1", "root", "a", 0), link("l2", "root", "b", 1))

        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            direction = LayoutDirection.Down,
            originX = 0f,
            originY = 0f,
        )

        val root = positions.getValue("root")
        val a = positions.getValue("a")
        val b = positions.getValue("b")
        assertEquals(StructureLayout.NODE_H + LayoutSpacing.Normal.levelGap, a.y, 0.01f)
        assertEquals(StructureLayout.NODE_W + gap, b.x - a.x, 0.01f)
        assertEquals((a.x + b.x) / 2f, root.x, 0.01f)
    }

    @Test
    fun radialPushesEachLevelFurtherOutAndKeepsNodesApart() {
        val nodes = listOf(node("root"), node("a"), node("b"), node("c"))
        val links = listOf(
            link("l1", "root", "a", 0),
            link("l2", "root", "b", 1),
            link("l3", "root", "c", 2),
        )

        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            direction = LayoutDirection.Radial,
            originX = 0f,
            originY = 0f,
        )

        assertEquals(4, positions.size)
        val root = positions.getValue("root")
        val centreX = root.x + StructureLayout.NODE_HALF_W
        val centreY = root.y + StructureLayout.NODE_HALF_H

        val radii = listOf("a", "b", "c").map { id ->
            val p = positions.getValue(id)
            hypot(
                (p.x + StructureLayout.NODE_HALF_W - centreX).toDouble(),
                (p.y + StructureLayout.NODE_HALF_H - centreY).toDouble(),
            ).toFloat()
        }
        radii.forEach { radius ->
            assertTrue("a radial child must sit away from the centre", radius > 100f)
        }
        assertTrue("every child sits on the same ring", radii.max() - radii.min() < 1f)

        // Distinct angles, so the three children cannot land on top of each other.
        val angles = listOf("a", "b", "c").map { id ->
            val p = positions.getValue(id)
            Math.toDegrees(
                kotlin.math.atan2(
                    (p.y + StructureLayout.NODE_HALF_H - centreY).toDouble(),
                    (p.x + StructureLayout.NODE_HALF_W - centreX).toDouble(),
                ),
            ).toInt()
        }
        assertEquals(3, angles.distinct().size)
    }

    @Test
    fun centralNodesWinAndOtherwiseNodesNothingPointsAtAreRoots() {
        val nodes = listOf(node("a"), node("b"), node("c"))
        val links = listOf(link("l1", "a", "b"))

        assertEquals(
            listOf("c"),
            StructureLayout.resolveRoots(nodes, links, preferredRoots = listOf("c")),
        )
        assertEquals(
            listOf("a", "c"),
            StructureLayout.resolveRoots(nodes, links, preferredRoots = emptyList()),
        )
        // A board with no links at all has no natural root, so every node starts its own tree.
        assertEquals(
            listOf("a", "b", "c"),
            StructureLayout.resolveRoots(nodes, emptyList(), preferredRoots = emptyList()),
        )
    }

    @Test
    fun severalCentralNodesEachStartTheirOwnTree() {
        val nodes = listOf(node("r1"), node("r2"), node("c1"), node("c2"))
        val links = listOf(link("l1", "r1", "c1"), link("l2", "r2", "c2"))

        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = listOf("r1", "r2"),
            direction = LayoutDirection.Right,
            originX = 0f,
            originY = 0f,
        )

        assertEquals(4, positions.size)
        // Both roots sit at depth 0 but must not overlap.
        assertEquals(0f, positions.getValue("r1").x, 0.01f)
        assertEquals(0f, positions.getValue("r2").x, 0.01f)
        assertTrue(positions.getValue("r1").y != positions.getValue("r2").y)
    }
}