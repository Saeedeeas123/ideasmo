package com.saeed.ideasmo.core.model

/**
 * The note editor's pure rules (v0.17.0).
 *
 * Saeed's order: *"when Note is chosen, above the place where the note is written the user must have a
 * simple text menu — bold, italic, smaller and larger, a font choice, the normal things."*
 *
 * Two kinds of formatting, split by where each one can honestly live:
 *
 * - **Bold and italic are inline.** They belong to a piece of the text, so they are stored *inside* it
 *   as markers (`**bold**`, `*italic*`). That keeps them portable: the note stays one plain string in
 *   `IdeaNode.notes`, it survives copy/paste and the export to a Drive file, and the language-model
 *   layer that will read these notes gets readable text rather than a style table.
 * - **Font and size belong to the whole note.** They are stored beside it (`IdeaNode.noteStyles`), so a
 *   note can be serif at 18 without any of that leaking into the text itself.
 *
 * The editor shows the markers while typing (like every simple Markdown field) and renders a live
 * preview underneath, so what the markers do is never a guess. This file is pure Kotlin — no Android,
 * no Compose — so the rules are unit-tested on the JVM.
 */
object NoteText {

    /** The inline markers. Two stars for bold, one for italic. */
    const val BOLD = "**"
    const val ITALIC = "*"

    /** What a marker pair looks like once it is rendered: a range of the clean text. */
    data class Span(val start: Int, val end: Int, val bold: Boolean, val italic: Boolean)

    /** A note with its markers taken out, plus the ranges that carry a style. */
    data class Parsed(val text: String, val spans: List<Span>)

    /** The text after an edit, and where the cursor/selection should land. */
    data class Edit(val text: String, val selection: IntRange)

    // ---- fonts and sizes -----------------------------------------------------

    data class Font(val key: String, val label: String, val detail: String)

    val FONTS: List<Font> = listOf(
        Font("default", "Default", "The app's own typeface."),
        Font("serif", "Serif", "A reading face, for longer notes."),
        Font("monospace", "Monospace", "Fixed width — code, lists, alignment."),
        Font("cursive", "Cursive", "A handwriting-like face, for a quick aside."),
    )

    data class Size(val key: String, val label: String, val scale: Float, val detail: String)

    val SIZES: List<Size> = listOf(
        Size("small", "Small", 0.85f, "A little smaller than the body text."),
        Size("normal", "Normal", 1.0f, "The body size."),
        Size("large", "Large", 1.25f, "Easier to read at arm's length."),
        Size("huge", "Huge", 1.5f, "A heading inside the note."),
    )

    /** Font + size for one note, as the single key that is stored in `noteStyles`. */
    const val DEFAULT_STYLE = "default:normal"

    fun styleKey(font: String, size: String): String = font + ":" + size

    fun fontOf(style: String?): String =
        style?.substringBefore(':')?.takeIf { key -> FONTS.any { it.key == key } } ?: "default"

    fun sizeOf(style: String?): String =
        style?.substringAfter(':', "normal")?.takeIf { key -> SIZES.any { it.key == key } } ?: "normal"

    fun fontLabel(style: String?): String =
        FONTS.firstOrNull { it.key == fontOf(style) }?.label ?: FONTS.first().label

    fun sizeLabel(style: String?): String =
        SIZES.firstOrNull { it.key == sizeOf(style) }?.label ?: SIZES[1].label

    fun scaleOf(style: String?): Float =
        SIZES.firstOrNull { it.key == sizeOf(style) }?.scale ?: 1f

    // ---- inline markers ------------------------------------------------------

    /**
     * Renders a note: the markers come out, and the ranges they covered become spans of the clean text.
     *
     * The rule for a marker that is never matched — a half-typed `**`, or a lone `*` inside a word like
     * `2*3` — is that it is **literal text**: it only becomes formatting when it is closed (or when it
     * closes something already open). Nothing is ever dropped, so no sentence can be eaten by a marker
     * that was still being typed.
     */
    fun parse(raw: String): Parsed {
        val out = StringBuilder()
        val spans = ArrayList<Span>()
        var bold = false
        var italic = false
        var openStart = 0
        var i = 0

        fun closeIfOpen(at: Int) {
            if (bold || italic) spans.add(Span(openStart, at, bold, italic))
            openStart = at
        }

        while (i < raw.length) {
            if (raw.startsWith(BOLD, i)) {
                // A run of two: formatting if it closes an open bold, or if another run follows.
                if (bold || raw.indexOf(BOLD, i + BOLD.length) >= 0) {
                    closeIfOpen(out.length)
                    bold = !bold
                } else {
                    out.append(BOLD)
                }
                i += BOLD.length
            } else if (raw[i] == '*') {
                if (italic || raw.indexOf('*', i + 1) >= 0) {
                    closeIfOpen(out.length)
                    italic = !italic
                } else {
                    out.append('*')
                }
                i += 1
            } else {
                out.append(raw[i])
                i++
            }
        }
        closeIfOpen(out.length)
        return Parsed(out.toString(), spans)
    }

    /** The note as it reads, with no markers — what a plain-text export should contain. */
    fun plain(raw: String): String = parse(raw).text

    /**
     * Wraps the selection in a marker pair, or takes the pair off again when it is already there.
     *
     * A selection of nothing inserts the pair and puts the cursor **between** them, so the next thing
     * typed is the formatted thing — which is what a person expects from a Bold button.
     */
    fun toggle(raw: String, selection: IntRange, marker: String): Edit {
        val start = selection.first.coerceIn(0, raw.length)
        val end = (selection.last + 1).coerceIn(start, raw.length)
        if (start == end) {
            val text = raw.substring(0, start) + marker + marker + raw.substring(end)
            val caret = start + marker.length
            return Edit(text, caret..caret)
        }
        val before = raw.substring(0, start)
        val selected = raw.substring(start, end)
        val after = raw.substring(end)
        // Already wrapped from the inside out: unwrap.
        if (before.endsWith(marker) && after.startsWith(marker)) {
            val text = before.dropLast(marker.length) + selected + after.drop(marker.length)
            val from = start - marker.length
            return Edit(text, from..(from + selected.length - 1).coerceAtLeast(from))
        }
        val text = before + marker + selected + marker + after
        val from = start + marker.length
        return Edit(text, from..(from + selected.length - 1).coerceAtLeast(from))
    }

    fun toggleBold(raw: String, selection: IntRange): Edit = toggle(raw, selection, BOLD)

    fun toggleItalic(raw: String, selection: IntRange): Edit = toggle(raw, selection, ITALIC)
}