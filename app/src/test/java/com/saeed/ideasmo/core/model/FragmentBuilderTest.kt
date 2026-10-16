package com.saeed.ideasmo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for what travels with a Copy / Cut / Duplicate.
 *
 * Saeed's order was that the user chooses: just the node, the node with its links, or the node with
 * its sub-links. Those three cases and the safety rules around them (never copy a link whose other
 * end stayed behind, never loop forever on a cycle) are what this pins down.
 */
class FragmentBuilderTest {

    private fun node(id: String) = IdeaNode(
        id = id,
        kind = "idea",
        name = id,
        priority = 3,
        description = "",
        picUri = null,
        createdAt = 0L,
        x = 0f,
        y = 0f,
    )

    private fun link(id: String, from: String, to: String, order: Int = 0) =
        NodeLink(id = id, fromId = from, toId = to, type = "relates", order = order)

    // a → b → c, and a → d, plus an outside node z that a points at.
    private val nodes = listOf(node("a"), node("b"), node("c"), node("d"), node("z"))
    private val links = listOf(
        link("l1", "a", "b"),
        link("l2", "b", "c"),
        link("l3", "a", "d"),
        link("l4", "a", "z"),
    )

    @Test
    fun onlyTheNodeTravelsWhenNothingIsTicked() {
        val ids = FragmentBuilder.idsInFragment("a", nodes, links, withLinks = false, withSubLinks = false)
        assertEquals(setOf("a"), ids)
    }

    @Test
    fun withLinksTakesTheDirectChildrenAndTheirLinks() {
        val fragment = FragmentBuilder.build("a", nodes, links, withLinks = true, withSubLinks = false)
        assertNotNull(fragment)
        assertEquals(setOf("a", "b", "d", "z"), fragment!!.nodes.map { it.id }.toSet())
        // b→c stays behind because c is not in the fragment, so its link must not be copied either.
        assertEquals(setOf("l1", "l3", "l4"), fragment.links.map { it.id }.toSet())
        assertTrue("a link to a node outside the fragment must never travel", fragment.links.none { it.toId == "c" })
    }

    @Test
    fun withSubLinksTakesTheWholeBranch() {
        val fragment = FragmentBuilder.build("a", nodes, links, withLinks = true, withSubLinks = true)
        assertNotNull(fragment)
        assertEquals(setOf("a", "b", "c", "d", "z"), fragment!!.nodes.map { it.id }.toSet())
        assertEquals(setOf("l1", "l2", "l3", "l4"), fragment.links.map { it.id }.toSet())
    }

    @Test
    fun aCycleTerminatesAndIsCopiedOnce() {
        val cyc = listOf(node("a"), node("b"))
        val cycLinks = listOf(link("l1", "a", "b"), link("l2", "b", "a"))
        val fragment = FragmentBuilder.build("a", cyc, cycLinks, withLinks = true, withSubLinks = true)
        assertNotNull(fragment)
        assertEquals(setOf("a", "b"), fragment!!.nodes.map { it.id }.toSet())
        assertEquals(2, fragment.links.size)
    }

    @Test
    fun anUnknownRootProducesNothingInsteadOfCrashing() {
        assertEquals(null, FragmentBuilder.build("nope", nodes, links, withLinks = true, withSubLinks = true))
    }

    @Test
    fun aFreeSelectionTakesOnlyTheLinksBetweenItsOwnNodes() {
        val fragment = FragmentBuilder.buildFromSelection(listOf("a", "b"), nodes, links)
        assertNotNull(fragment)
        assertEquals(setOf("a", "b"), fragment!!.nodes.map { it.id }.toSet())
        // a→d and a→z point outside the selection, so they are dropped.
        assertEquals(setOf("l1"), fragment.links.map { it.id }.toSet())
        assertEquals("a", fragment.rootId)
    }
}