package com.saeed.ideasmo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the two catalogues that the node UI is generated from: [NodeActionCatalog] (the four
 * bars) and [NodeStyle] (size, colour, shape).
 *
 * They exist because these lists are now the **single source of truth** — the buttons, the Help
 * screen and (later) the language-model layer all read them. A missing id, a duplicate key or a
 * missing description would silently break one of those consumers, and this is where that is caught.
 */
class NodeCatalogTest {

    @Test
    fun everyActionCarriesAnIdLabelAndMachineReadableDescription() {
        assertTrue("the catalogue must not be empty", NodeActionCatalog.all.isNotEmpty())
        NodeActionCatalog.all.forEach { action ->
            assertTrue("blank key in " + action.label, action.key.isNotBlank())
            assertTrue("blank label for " + action.key, action.label.isNotBlank())
            assertTrue(
                "description too thin for " + action.key + " — the model layer needs what it does",
                action.description.length > 30,
            )
            // Keys are namespaced so they can never collide with a link type key.
            assertTrue("un-namespaced key: " + action.key, action.key.startsWith("node."))
        }
    }

    @Test
    fun actionKeysAreUniqueAndResolvable() {
        val keys = NodeActionCatalog.all.map { it.key }
        assertEquals("duplicate action key", keys.size, keys.distinct().size)
        keys.forEach { key ->
            assertNotNull("byKey failed for " + key, NodeActionCatalog.byKey(key))
        }
    }

    @Test
    fun theSquareHasFourBarsWithTheExpectedActions() {
        // Top bar: what the node says. Left: clipboard and delete. Right: the branch.
        // Bottom: style. The counts are part of the layout contract of the square.
        assertEquals(5, NodeActionCatalog.byGroup(NodeActionGroup.Content).size)
        assertEquals(4, NodeActionCatalog.byGroup(NodeActionGroup.Clipboard).size)
        assertEquals(4, NodeActionCatalog.byGroup(NodeActionGroup.Branch).size)
        assertEquals(3, NodeActionCatalog.byGroup(NodeActionGroup.Style).size)

        // Every action belongs to exactly one bar.
        val grouped = NodeActionGroup.values().sumOf { NodeActionCatalog.byGroup(it).size }
        assertEquals(NodeActionCatalog.all.size, grouped)
    }

    @Test
    fun theTwoTargetPickingActionsAreMarkedAsSuch() {
        // Connect and Reattach ask the user to tap a second node; the board relies on this flag.
        assertTrue(NodeActionCatalog.byKey(NodeActionCatalog.CONNECT)!!.needsTargetNode)
        assertTrue(NodeActionCatalog.byKey(NodeActionCatalog.REATTACH)!!.needsTargetNode)
        assertTrue(NodeActionCatalog.byKey(NodeActionCatalog.COPY)!!.needsTargetNode.not())
    }

    @Test
    fun structureChangingActionsAreMarkedForUndo() {
        // Anything that removes or re-parents something must offer a way back.
        listOf(
            NodeActionCatalog.DELETE,
            NodeActionCatalog.CUT,
            NodeActionCatalog.REATTACH,
            NodeActionCatalog.CHILD,
            NodeActionCatalog.SIBLING,
            NodeActionCatalog.DUPLICATE,
            NodeActionCatalog.PASTE,
            NodeActionCatalog.CONNECT,
        ).forEach { key ->
            assertTrue(key + " must be marked as structure-changing", NodeActionCatalog.byKey(key)!!.mutatesStructure)
        }
    }

    @Test
    fun describeAllIsOneReadableLinePerAction() {
        val lines = NodeActionCatalog.describeAll().lines()
        assertEquals(NodeActionCatalog.all.size, lines.size)
        lines.forEach { line ->
            val parts = line.split(" | ")
            assertEquals(3, parts.size)
            assertNotNull(NodeActionCatalog.byKey(parts[0]))
        }
    }

    @Test
    fun styleVocabularyIsSoundAndClampsBadInput() {
        val shapeKeys = NodeStyle.SHAPES.map { it.key }
        val colourKeys = NodeStyle.COLORS.map { it.key }
        assertEquals(shapeKeys.size, shapeKeys.distinct().size)
        assertEquals(colourKeys.size, colourKeys.distinct().size)
        assertTrue(NodeStyle.SHAPES.all { it.detail.isNotBlank() })
        assertTrue(NodeStyle.COLORS.all { it.detail.isNotBlank() })
        assertTrue(NodeStyle.SIZES.all { it.detail.isNotBlank() })

        // Every stored key must resolve to a label, and an unknown one must not crash anything.
        shapeKeys.forEach { assertEquals(true, NodeStyle.shapeLabel(it).isNotBlank()) }
        assertEquals("Default", NodeStyle.colorLabel(null))
        assertEquals("Default", NodeStyle.colorLabel("not-a-role"))

        // A size read from an old or hand-edited file snaps onto the supported set.
        assertEquals(1f, NodeStyle.sizeOf(1f), 0.001f)
        assertEquals(0.8f, NodeStyle.sizeOf(0.31f), 0.001f)
        assertEquals(1.5f, NodeStyle.sizeOf(99f), 0.001f)
    }

    @Test
    fun theDefaultStyleIsTheCardTheBoardShippedWith() {
        val node = IdeaNode(
            id = "n1",
            kind = "idea",
            name = "x",
            priority = 3,
            description = "",
            picUri = null,
            createdAt = 0L,
            x = 0f,
            y = 0f,
        )
        assertEquals(NodeStyle.SHAPE_ROUNDED, node.shape)
        assertEquals(1f, node.size, 0.001f)
        assertEquals(null, node.color)
        // And a default node keeps the original card geometry.
        assertEquals(StructureLayout.NODE_W, StructureLayout.widthOf(node), 0.001f)
        assertEquals(StructureLayout.NODE_H, StructureLayout.heightOf(node), 0.001f)
    }
}