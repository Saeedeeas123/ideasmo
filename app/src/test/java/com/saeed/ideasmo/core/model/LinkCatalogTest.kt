package com.saeed.ideasmo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the relation catalog's finding rules (v0.15.0).
 *
 * What they protect: a relation can be found by whatever the user remembers about it — its name, how
 * it reads, its example, its meaning, its key or its family — filtering by family never hides a
 * relation that belongs to it, and the counts the family chips show are the real counts.
 */
class LinkCatalogTest {

    @Test
    fun aRelationIsFoundByWhateverTheUserRemembers() {
        // By name.
        assertTrue(LinkCatalog.browse("part").flatMap { it.second }.any { it.key == "part_of" })
        // By how it reads as a sentence.
        assertTrue(LinkCatalog.browse("is part of").flatMap { it.second }.any { it.key == "part_of" })
        // By its example.
        assertTrue(LinkCatalog.browse("Wheel").flatMap { it.second }.any { it.key == "part_of" })
        // By its meaning.
        assertTrue(LinkCatalog.browse("component").flatMap { it.second }.any { it.key == "part_of" })
        // By its key.
        assertTrue(LinkCatalog.browse("part_of").flatMap { it.second }.any { it.key == "part_of" })
        // By its family.
        assertTrue(
            LinkCatalog.browse("Composition").flatMap { it.second }.any { it.key == "part_of" },
        )
        // Case does not matter, and surrounding spaces are ignored.
        assertTrue(LinkCatalog.browse("  WHEEL ").flatMap { it.second }.any { it.key == "part_of" })
    }

    @Test
    fun filteringByFamilyKeepsThatFamilyAndOnlyThatFamily() {
        LinkFamily.values().forEach { family ->
            val groups = LinkCatalog.browse(family = family)
            assertEquals("one group per family", 1, groups.size)
            assertEquals(family, groups.first().first)
            assertTrue("the family is not empty", groups.first().second.isNotEmpty())
            assertTrue(
                "every returned type belongs to it",
                groups.first().second.all { it.family == family },
            )
        }
        // No filter means every family, and every relation, is still reachable.
        val all = LinkCatalog.browse()
        assertEquals(LinkFamily.values().size, all.size)
        assertEquals(LinkCatalog.all.size, all.sumOf { it.second.size })
    }

    @Test
    fun searchAndFamilyNarrowTogether() {
        // "causes" exists in Causality; asking for it inside Logic must not return it.
        assertTrue(LinkCatalog.browse("causes").flatMap { it.second }.any { it.key == "causes" })
        assertFalse(
            LinkCatalog.browse("causes", LinkFamily.Logic).flatMap { it.second }.any { it.key == "causes" },
        )
    }

    @Test
    fun anEmptySearchIsHonestAboutIt() {
        assertFalse(LinkCatalog.searchIsEmpty(""))
        assertFalse(LinkCatalog.searchIsEmpty("   "))
        assertTrue(LinkCatalog.searchIsEmpty("nothing-is-called-this"))
        // An empty query finds everything; a query that finds nothing finds nothing.
        assertEquals(LinkCatalog.all.size, LinkCatalog.browse("").sumOf { it.second.size })
        assertTrue(LinkCatalog.browse("nothing-is-called-this").isEmpty())
    }

    @Test
    fun theFamilyChipsCountWhatIsActuallyThere() {
        val counts = LinkCatalog.countByFamily()
        assertEquals(LinkCatalog.all.size, counts.values.sum())
        LinkFamily.values().forEach { family ->
            assertEquals(
                LinkCatalog.all.count { it.family == family },
                counts[family],
            )
        }
    }

    @Test
    fun theDefaultRelationOfANewLinkSurvivesAnythingStored() {
        // v0.16.0 — the relation a new link starts as is a stored preference, so it has to be safe:
        // whatever ends up in the preference (a key from an older build, a renamed type, nothing at
        // all), creating a link must still work.
        assertEquals("relates", LinkCatalog.defaultFor(null).key)
        assertEquals("relates", LinkCatalog.defaultFor("").key)
        assertEquals("relates", LinkCatalog.defaultFor("a-type-that-never-existed").key)
        // A real preference is honoured, and every type in the catalog can be one.
        assertEquals("depends_on", LinkCatalog.defaultFor("depends_on").key)
        LinkCatalog.all.forEach { type ->
            assertEquals(type.key, LinkCatalog.defaultFor(type.key).key)
        }
    }
}
