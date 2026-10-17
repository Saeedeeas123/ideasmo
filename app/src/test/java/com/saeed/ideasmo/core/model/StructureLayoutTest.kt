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
 * branch is not laid out; a cycle cannot hang the arranger; nodes that belong to no tree are left
 * exactly where the user put them; and Arrange anchored on the board origin cannot drift, so
 * arranging twice always produces the same map.
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
        notes = emptyList(),
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

    // ---- the board origin (v0.7.0) -----------------------------------------

    @Test
    fun arrangePutsTheCentreOfTheCentralNodeOnTheOrigin() {
        val nodes = listOf(
            node("root", x = 900f, y = -400f),
            node("a", x = 1234f, y = 77f),
            node("b", x = -800f, y = 2500f),
        )
        val links = listOf(link("l1", "root", "a"), link("l2", "root", "b"))

        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = listOf("root"),
            direction = LayoutDirection.Right,
            spacing = LayoutSpacing.Normal,
            anchorRootAtOrigin = true,
        )

        val root = positions.getValue("root")
        assertEquals(BoardSpace.ORIGIN_X, root.x + StructureLayout.NODE_HALF_W, 0.01f)
        assertEquals(BoardSpace.ORIGIN_Y, root.y + StructureLayout.NODE_HALF_H, 0.01f)
        // And the map grows out of the origin, not out of wherever the nodes happened to be.
        assertTrue(positions.getValue("a").x > root.x)
        assertTrue(positions.getValue("b").x > root.x)
    }

    @Test
    fun arrangingTwiceCannotDrift() {
        // This is the whole point of anchoring: the second Arrange reproduces the first exactly,
        // no matter where the nodes were dragged in between.
        val nodes = listOf(node("root"), node("a"), node("b"), node("c"))
        val links = listOf(
            link("l1", "root", "a", 0),
            link("l2", "root", "b", 1),
            link("l3", "a", "c", 0),
        )
        val first = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = listOf("root"),
            anchorRootAtOrigin = true,
        )
        val dragged = nodes.map { it.copy(x = it.x + 5000f, y = it.y - 3000f) }
        val second = StructureLayout.arrange(
            nodes = dragged,
            links = links,
            preferredRoots = listOf("root"),
            anchorRootAtOrigin = true,
        )

        assertEquals(first.size, second.size)
        first.forEach { (id, point) ->
            val again = second.getValue(id)
            assertEquals(point.x, again.x, 0.001f)
            assertEquals(point.y, again.y, 0.001f)
        }
    }

    @Test
    fun anchoringAlsoWorksForRadial() {
        val nodes = listOf(node("root", x = 4000f, y = 4000f), node("a"), node("b"))
        val links = listOf(link("l1", "root", "a"), link("l2", "root", "b"))

        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = listOf("root"),
            direction = LayoutDirection.Radial,
            anchorRootAtOrigin = true,
        )

        val root = positions.getValue("root")
        assertEquals(BoardSpace.ORIGIN_X, root.x + StructureLayout.NODE_HALF_W, 0.01f)
        assertEquals(BoardSpace.ORIGIN_Y, root.y + StructureLayout.NODE_HALF_H, 0.01f)
    }

    @Test
    fun withoutAnchoringTheMapGrowsFromTheGivenPoint() {
        // Regression test for a real v0.6.0 bug: the layout cursor used to be seeded with originY
        // and then originY was added again by the transform, so every Arrange displaced the whole
        // map by the origin. With the origin at 0 that was invisible; with a dragged board it was
        // the drift that made Arrange feel like it did not work.
        val nodes = listOf(node("root", x = 800f, y = 600f), node("a"))
        val links = listOf(link("l1", "root", "a"))

        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = listOf("root"),
            originX = 800f,
            originY = 600f,
        )

        assertEquals(800f, positions.getValue("root").x, 0.01f)
        assertEquals(600f, positions.getValue("root").y, 0.01f)
        // The child sits one level to the right, on the same row as its parent.
        assertEquals(800f + StructureLayout.NODE_W + LayoutSpacing.Normal.levelGap, positions.getValue("a").x, 0.01f)
        assertEquals(600f, positions.getValue("a").y, 0.01f)
    }

    @Test
    fun aResizedNodeGetsItsOwnSpaceInTheLayout() {
        // Sizes are per node, so both the sibling axis and the step between levels have to follow
        // the real card sizes — otherwise a large card would be laid out on top of its neighbour.
        val root = node("root").copy(w = 300f, h = 144f)
        val small = node("a").copy(w = 160f, h = 77f)
        val other = node("b")
        val nodes = listOf(root, small, other)
        val links = listOf(link("l1", "root", "a", 0), link("l2", "root", "b", 1))

        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = listOf("root"),
            direction = LayoutDirection.Right,
            spacing = LayoutSpacing.Normal,
        )

        // The step between levels clears the LARGEST card on the board (1.5x = 300 units).
        assertEquals(
            300f + LayoutSpacing.Normal.levelGap,
            positions.getValue("a").x - positions.getValue("root").x,
            0.01f,
        )
        // Siblings are separated by their own height plus the sibling gap.
        val gapBetween = positions.getValue("b").y - positions.getValue("a").y
        assertEquals(StructureLayout.heightOf(small) + LayoutSpacing.Normal.siblingGap, gapBetween, 0.01f)
        assertTrue("the second card must not overlap the first", gapBetween >= StructureLayout.heightOf(other))
    }

    @Test
    fun aResizedCentralNodeIsStillCentredOnTheOrigin() {
        val nodes = listOf(node("root").copy(w = 250f, h = 120f), node("a"), node("b"))
        val links = listOf(link("l1", "root", "a"), link("l2", "root", "b"))

        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = listOf("root"),
            anchorRootAtOrigin = true,
        )

        val root = positions.getValue("root")
        assertEquals(BoardSpace.ORIGIN_X, root.x + StructureLayout.widthOf(nodes[0]) / 2f, 0.01f)
        assertEquals(BoardSpace.ORIGIN_Y, root.y + StructureLayout.heightOf(nodes[0]) / 2f, 0.01f)
    }

    // ---- arranging ONE branch from the node's own properties (v0.10.0) -----

    @Test
    fun arrangeBranchKeepsTheSelectedNodeExactlyWhereItWas() {
        // Saeed's rule: the user selects a node, picks a direction, and the branch grows away from
        // that node. The node itself must not move — unlike the whole-map Arrange, which parks the
        // map on the board origin.
        val nodes = listOf(
            node("root", x = 1234f, y = -777f),
            node("a", x = 10f),
            node("b", x = 10f),
            node("far", x = 5000f, y = 5000f),
        )
        val links = listOf(link("l1", "root", "a", 0), link("l2", "root", "b", 1))

        val positions = StructureLayout.arrangeBranch(
            nodes = nodes,
            links = links,
            rootId = "root",
            direction = LayoutDirection.Down,
        )

        assertEquals(1234f, positions.getValue("root").x, 0.01f)
        assertEquals(-777f, positions.getValue("root").y, 0.01f)
        assertFalse(
            "a node that is not below the selected node must not move",
            positions.containsKey("far"),
        )
    }

    @Test
    fun arrangeBranchSendsTheSubtreeToTheChosenSide() {
        val nodes = listOf(node("root"), node("a"), node("b"), node("c"))
        val links = listOf(
            link("l1", "root", "a", 0),
            link("l2", "root", "b", 1),
            link("l3", "a", "c", 0),
        )

        val right = StructureLayout.arrangeBranch(nodes, links, "root", LayoutDirection.Right)
        val left = StructureLayout.arrangeBranch(nodes, links, "root", LayoutDirection.Left)
        val up = StructureLayout.arrangeBranch(nodes, links, "root", LayoutDirection.Up)
        val down = StructureLayout.arrangeBranch(nodes, links, "root", LayoutDirection.Down)

        assertTrue("Right sends the children right", right.getValue("a").x > right.getValue("root").x)
        assertTrue("Left sends the children left", left.getValue("a").x < left.getValue("root").x)
        assertTrue("Up sends the children up", up.getValue("a").y < up.getValue("root").y)
        assertTrue("Down sends the children down", down.getValue("a").y > down.getValue("root").y)

        // And the level below follows the same direction.
        assertTrue(right.getValue("c").x > right.getValue("a").x)
        assertTrue(left.getValue("c").x < left.getValue("a").x)
        assertTrue(up.getValue("c").y < up.getValue("a").y)
        assertTrue(down.getValue("c").y > down.getValue("a").y)
    }

    @Test
    fun arrangeBranchOfANodeWithNothingBelowItDoesNothing() {
        val nodes = listOf(node("root"), node("leaf"))
        val links = listOf(link("l1", "root", "leaf"))

        val positions = StructureLayout.arrangeBranch(nodes, links, "leaf", LayoutDirection.Right)

        assertTrue("a leaf cannot be arranged", positions.isEmpty())
    }

    @Test
    fun arrangeBranchSkipsACollapsedSubtree() {
        val nodes = listOf(node("root"), node("child", collapsed = true), node("grandchild"))
        val links = listOf(link("l1", "root", "child"), link("l2", "child", "grandchild"))

        val positions = StructureLayout.arrangeBranch(nodes, links, "root", LayoutDirection.Right)

        assertTrue(positions.containsKey("child"))
        assertFalse(positions.containsKey("grandchild"))
    }

    @Test
    fun theLayoutReservesTheSizeTheCardReallyRenders() {
        // A card grows with its content — a picture, a long name — so the height stored on the node
        // can be smaller than what is on screen. Laying siblings out by the stored height is what let
        // branches overlap on Saeed's board; the measured size has to win.
        val nodes = listOf(node("root"), node("a"), node("b"))
        val links = listOf(link("l1", "root", "a", 0), link("l2", "root", "b", 1))
        val measured = mapOf(
            "a" to LayoutSize(StructureLayout.NODE_W, 260f),
            "b" to LayoutSize(StructureLayout.NODE_W, 260f),
        )

        val positions = StructureLayout.arrangeBranch(
            nodes = nodes,
            links = links,
            rootId = "root",
            direction = LayoutDirection.Right,
            spacing = LayoutSpacing.Normal,
            measured = measured,
        )

        val distance = positions.getValue("b").y - positions.getValue("a").y
        assertTrue(
            "siblings must clear the card that really rendered, not the stored size",
            distance >= 260f,
        )
    }

    @Test
    fun leftAndUpUseTheCardsOwnSizeNotTheDefault() {
        // Regression test: the offset back towards the parent used the NODE_W / NODE_H constants, so
        // a resized card straddled the level line instead of starting on it.
        val big = node("root").copy(w = 400f, h = 200f)
        val nodes = listOf(big, node("a"))
        val links = listOf(link("l1", "root", "a"))

        val left = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = listOf("root"),
            direction = LayoutDirection.Left,
            spacing = LayoutSpacing.Normal,
            originX = 0f,
            originY = 0f,
        )
        val up = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = listOf("root"),
            direction = LayoutDirection.Up,
            spacing = LayoutSpacing.Normal,
            originX = 0f,
            originY = 0f,
        )

        assertEquals(0f, left.getValue("root").x + StructureLayout.widthOf(big), 0.01f)
        assertEquals(0f, up.getValue("root").y + StructureLayout.heightOf(big), 0.01f)
    }

    // ---- standard link length and no overlap after an Arrange (v0.11.0) -----

    @Test
    fun everyLinkIsTheSameLengthAfterArrange() {
        // Saeed's report: "it gets the direction right but it does not consider the links — there must
        // be a standard length for the links, with a logical gap". v0.10.0 spaced the levels by the
        // LARGEST card on the board, so the visible link after a big card was long and after a small
        // one short. Now every link is exactly one level gap, edge to edge.
        val root = node("root").copy(w = 400f, h = 150f)
        val mid = node("mid").copy(w = 160f, h = 80f)
        val leaf = node("leaf")
        val nodes = listOf(root, mid, leaf)
        val links = listOf(link("l1", "root", "mid", 0), link("l2", "mid", "leaf", 0))

        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = listOf("root"),
            direction = LayoutDirection.Right,
            spacing = LayoutSpacing.Normal,
        )

        val first = positions.getValue("mid").x - (positions.getValue("root").x + StructureLayout.widthOf(root))
        val second = positions.getValue("leaf").x - (positions.getValue("mid").x + StructureLayout.widthOf(mid))
        assertEquals("the link after the big card", LayoutSpacing.Normal.levelGap, first, 0.01f)
        assertEquals("the link after the small card", LayoutSpacing.Normal.levelGap, second, 0.01f)
    }

    @Test
    fun aBranchIsSlidClearOfNodesThatAreNotPartOfIt() {
        // The branch keeps its own node where it is, so it can land on a node that is not part of it.
        // Arrange has to move the branch out of the way instead of drawing one card on top of another.
        val root = node("root", x = 0f, y = 0f)
        val kid = node("kid", x = 900f, y = 900f)
        val blocker = node("blocker", x = StructureLayout.NODE_W + LayoutSpacing.Normal.levelGap, y = 0f)
        val nodes = listOf(root, kid, blocker)
        val links = listOf(link("l1", "root", "kid"))

        val positions = StructureLayout.arrangeBranch(nodes, links, "root", LayoutDirection.Right)

        assertEquals("the selected node must not move", 0f, positions.getValue("root").x, 0.01f)
        val kidPos = positions.getValue("kid")
        val bw = StructureLayout.widthOf(blocker)
        val bh = StructureLayout.heightOf(blocker)
        val overlaps = kidPos.x < blocker.x + bw &&
            kidPos.x + StructureLayout.widthOf(kid) > blocker.x &&
            kidPos.y < blocker.y + bh &&
            kidPos.y + StructureLayout.heightOf(kid) > blocker.y
        assertFalse("the arranged branch must not land on another node", overlaps)
        assertTrue("it is slid along its cross axis", kidPos.y > blocker.y)
    }

    @Test
    fun arrangingTheSameBranchTwiceGivesTheSameResult() {
        val nodes = listOf(
            node("root", x = 500f, y = 500f),
            node("a"),
            node("b"),
            node("blocker", x = 700f, y = 500f),
        )
        val links = listOf(link("l1", "root", "a", 0), link("l2", "root", "b", 1))

        val first = StructureLayout.arrangeBranch(nodes, links, "root", LayoutDirection.Right)
        val second = StructureLayout.arrangeBranch(nodes, links, "root", LayoutDirection.Right)

        assertEquals(first.size, second.size)
        first.forEach { (id, point) ->
            assertEquals(point.x, second.getValue(id).x, 0.001f)
            assertEquals(point.y, second.getValue(id).y, 0.001f)
        }
    }

    @Test
    fun theWholeMapArrangeParksLooseNodesClearOfTheMap() {
        // A node that belongs to no tree used to be left exactly where it was, which is how a loose
        // node ended up sitting on top of the arranged map in Saeed's screenshot.
        val root = node("root")
        val kid = node("kid")
        val loose = node("loose", x = 0f, y = 0f)
        val nodes = listOf(root, kid, loose)
        val links = listOf(link("l1", "root", "kid"))

        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = listOf("root"),
            direction = LayoutDirection.Right,
            anchorRootAtOrigin = true,
        )

        assertTrue("the loose node is placed too", positions.containsKey("loose"))
        val loosePoint = positions.getValue("loose")
        val mapLeft = positions.filterKeys { it != "loose" }.values.minOf { it.x }
        assertTrue(
            "the loose node is parked in a column beside the map",
            loosePoint.x + StructureLayout.widthOf(loose) <= mapLeft,
        )
        val overlapsAnything = positions.filterKeys { it != "loose" }.any { (id, point) ->
            val other = nodes.first { it.id == id }
            loosePoint.x < point.x + StructureLayout.widthOf(other) &&
                loosePoint.x + StructureLayout.widthOf(loose) > point.x &&
                loosePoint.y < point.y + StructureLayout.heightOf(other) &&
                loosePoint.y + StructureLayout.heightOf(loose) > point.y
        }
        assertFalse("and it does not overlap the map", overlapsAnything)
    }

    @Test
    fun eachCentralNodeArrangesItsOwnTreeInItsOwnDirection() {
        // v0.15.0 — the arrangement lives on the NODE. "If the arrange is Up, it stays Up": a board
        // can hold an Up tree and a Right tree at the same time, without a board-wide setting deciding
        // for both. This is the engine half of that promise.
        val up = node("up", isRoot = true).copy(arrange = LayoutDirection.Up.key)
        val upKid = node("upKid")
        val right = node("right", isRoot = true).copy(arrange = LayoutDirection.Right.key)
        val rightKid = node("rightKid")
        val nodes = listOf(up, upKid, right, rightKid)
        val links = listOf(link("l1", "up", "upKid"), link("l2", "right", "rightKid"))

        val positions = StructureLayout.arrange(
            nodes = nodes,
            links = links,
            preferredRoots = listOf("up", "right"),
            // The fallback direction is deliberately NOT what either tree uses.
            direction = LayoutDirection.Down,
            spacing = LayoutSpacing.Normal,
        )

        val upRoot = positions.getValue("up")
        val upChild = positions.getValue("upKid")
        val rightRoot = positions.getValue("right")
        val rightChild = positions.getValue("rightKid")

        // An Up tree: the child sits ABOVE its parent, on the parent's own axis.
        assertTrue("the Up child is above its parent", upChild.y < upRoot.y)
        assertEquals("and shares its column", upRoot.x, upChild.x, 0.001f)

        // A Right tree: the child sits to the RIGHT and on the same row.
        assertTrue("the Right child is to the right of its parent", rightChild.x > rightRoot.x)
        assertEquals("and shares its row", rightRoot.y, rightChild.y, 0.001f)

        // And the two trees do not overlap, because each group starts clear of the one before it.
        assertTrue("the Right tree is clear of the Up tree", rightRoot.y > upRoot.y)
    }

    @Test
    fun aFreeNodeFollowsTheBoardDirection() {
        // Free is the absence of an arrangement, so such a tree uses the direction it is given.
        val free = node("free", isRoot = true)
        val kid = node("kid")
        val positions = StructureLayout.arrange(
            nodes = listOf(free, kid),
            links = listOf(link("l1", "free", "kid")),
            preferredRoots = listOf("free"),
            direction = LayoutDirection.Left,
            spacing = LayoutSpacing.Normal,
        )
        val root = positions.getValue("free")
        val child = positions.getValue("kid")
        assertTrue("a Free tree grows left when the board says left", child.x < root.x)
    }

    @Test
    fun theArrangementKeyRoundTripsAndFreeIsTheDefault() {
        val plain = node("plain")
        assertEquals("a node with no arrangement is Free", null, plain.arrange)
        assertEquals(null, plain.arrangeDirection)
        assertEquals(LayoutDirection.Radial.key, plain.copy(arrange = "radial").arrangeDirection?.key)
        // An unknown key from an older or newer build is Free, not a crash.
        assertEquals(null, plain.copy(arrange = "sideways").arrangeDirection)
    }
}
