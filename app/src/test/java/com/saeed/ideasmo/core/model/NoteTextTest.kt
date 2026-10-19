package com.saeed.ideasmo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the note editor's rules (v0.17.0).
 *
 * What they protect: formatting never loses text, the markers come out cleanly when a note is rendered
 * or exported, the same button both applies and removes a style, and a note's font and size survive
 * whatever is stored for them.
 */
class NoteTextTest {

    @Test
    fun markersBecomeSpansAndTheTextComesOutClean() {
        val parsed = NoteText.parse("a **bold** word and *soft* end")
        assertEquals("a bold word and soft end", parsed.text)
        assertEquals(2, parsed.spans.size)
        val bold = parsed.spans[0]
        assertTrue("the first span is bold", bold.bold)
        assertEquals("bold", parsed.text.substring(bold.start, bold.end))
        val italic = parsed.spans[1]
        assertTrue("the second span is italic", italic.italic)
        assertEquals("soft", parsed.text.substring(italic.start, italic.end))
    }

    @Test
    fun formattingNeverLosesText() {
        // A marker that is never matched stays literal text, so a half-typed ** cannot eat a sentence
        // and a lone star in "2*3" is not silently removed.
        assertEquals("**hello world", NoteText.parse("**hello world").text)
        assertEquals("hello world", NoteText.parse("hello **world**").text)
        assertEquals("plain", NoteText.parse("plain").text)
        assertEquals("", NoteText.parse("").text)
        // Only markers, and matched: nothing left.
        assertEquals("", NoteText.parse("****").text)
        // A lone star in the middle of a word is literal.
        assertEquals("2*3", NoteText.parse("2*3").text)
    }

    @Test
    fun boldIsAToggleAroundTheSelection() {
        val text = "one two three"
        val bolded = NoteText.toggleBold(text, 4..6)
        assertEquals("one **two** three", bolded.text)
        // The selection stays on the same word, so a second tap can undo it.
        assertEquals("two", bolded.text.substring(bolded.selection.first, bolded.selection.last + 1))
        val unbolded = NoteText.toggleBold(bolded.text, bolded.selection)
        assertEquals(text, unbolded.text)
    }

    @Test
    fun anEmptySelectionPutsTheCaretBetweenTheMarkers() {
        val edit = NoteText.toggleItalic("ab", 1..0)
        assertEquals("a**b", edit.text)
        assertEquals(2, edit.selection.first)
        assertEquals(2, edit.selection.last)
    }

    @Test
    fun theNoteStyleIsReadBackWhateverIsStored() {
        assertEquals("serif:large", NoteText.styleKey("serif", "large"))
        assertEquals("serif", NoteText.fontOf("serif:large"))
        assertEquals("large", NoteText.sizeOf("serif:large"))
        assertEquals(1.25f, NoteText.scaleOf("serif:large"), 0.001f)
        // Nothing stored, an unknown font, an unknown size: all fall back to the default reading.
        assertEquals("default", NoteText.fontOf(null))
        assertEquals("normal", NoteText.sizeOf(null))
        assertEquals("default", NoteText.fontOf("papyrus:huge"))
        assertEquals("normal", NoteText.sizeOf("serif:gigantic"))
        assertEquals(1f, NoteText.scaleOf("nonsense"), 0.001f)
        assertEquals("Default", NoteText.fontLabel(null))
        assertEquals("Normal", NoteText.sizeLabel(null))
    }

    @Test
    fun thePlainTextOfANoteHasNoMarkers() {
        assertEquals("bold and soft", NoteText.plain("**bold** and *soft*"))
    }
}