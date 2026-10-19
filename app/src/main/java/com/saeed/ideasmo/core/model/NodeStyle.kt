package com.saeed.ideasmo.core.model

/**
 * The vocabulary a node's look is described in: shape, size and colour role.
 *
 * Kept in `core` as plain data with stable **keys** and human-readable **descriptions**, for the
 * same reason [LinkCatalog] is: the UI, Help, the stored JSON and (later) the language-model layer
 * all read from one list, so a style can be named, stored, exported and explained without any of
 * them drifting apart.
 *
 * Colours are Material **roles** by default — so day/night and dynamic colour keep working — plus a
 * fixed palette of preset colours and, since v0.17.0, a **custom RGB colour** the user builds by hand.
 * A custom colour is stored as `#RRGGBB`, so it is still just a key: no model change, and an unknown
 * key from a future build degrades to the plain card instead of crashing.
 */
object NodeStyle {

    // ---- shape ---------------------------------------------------------------
    const val SHAPE_ROUNDED = "rounded"
    const val SHAPE_SQUARE = "square"
    const val SHAPE_PILL = "pill"
    const val SHAPE_CUT = "cut"

    // v0.17.0 — Saeed asked for more shapes, so five more, each a shape a card can genuinely be.
    const val SHAPE_SOFT = "soft"
    const val SHAPE_CIRCLE = "circle"
    const val SHAPE_OCTAGON = "octagon"
    const val SHAPE_TICKET = "ticket"
    const val SHAPE_LEAF = "leaf"

    // ---- colour roles (null = the plain card) --------------------------------
    const val COLOR_PRIMARY = "primary"
    const val COLOR_SECONDARY = "secondary"
    const val COLOR_TERTIARY = "tertiary"
    const val COLOR_ERROR = "error"
    const val COLOR_OUTLINE = "outline"

    // v0.17.0 — more colours, and any colour at all: a fixed palette of presets plus a custom RGB
    // value. A preset is a plain hex string, which is what makes a custom colour need no new model.
    const val COLOR_BLUE = "#1E88E5"
    const val COLOR_TEAL = "#00897B"
    const val COLOR_GREEN = "#43A047"
    const val COLOR_LIME = "#7CB342"
    const val COLOR_YELLOW = "#F9A825"
    const val COLOR_ORANGE = "#FB8C00"
    const val COLOR_RED = "#E53935"
    const val COLOR_PINK = "#D81B60"
    const val COLOR_PURPLE = "#8E24AA"
    const val COLOR_INDIGO = "#3949AB"
    const val COLOR_BROWN = "#6D4C41"
    const val COLOR_SLATE = "#546E7A"

    data class Option(val key: String, val label: String, val detail: String)

    val SHAPES: List<Option> = listOf(
        Option(SHAPE_ROUNDED, "Rounded", "The default card, 12-unit corners."),
        Option(SHAPE_SQUARE, "Square", "A sharp rectangle."),
        Option(SHAPE_PILL, "Pill", "Fully rounded ends."),
        Option(SHAPE_CUT, "Cut corner", "One corner cut, like a sticky note."),
        Option(SHAPE_SOFT, "Soft", "Generously rounded corners."),
        Option(SHAPE_CIRCLE, "Circle", "A round card — for a short name, like a hub."),
        Option(SHAPE_OCTAGON, "Octagon", "Every corner cut, like a plaque."),
        Option(SHAPE_TICKET, "Ticket", "The two ends notched, like a ticket."),
        Option(SHAPE_LEAF, "Leaf", "Two opposite corners rounded, the other two sharp."),
    )

    /** The five theme roles: they follow day/night and the wallpaper colour. */
    val COLORS: List<Option> = listOf(
        Option(COLOR_PRIMARY, "Primary", "The app's main accent — the thing that matters most."),
        Option(COLOR_SECONDARY, "Secondary", "A quieter accent for supporting ideas."),
        Option(COLOR_TERTIARY, "Tertiary", "A contrasting accent — the central-node colour."),
        Option(COLOR_ERROR, "Error", "A warning or a problem."),
        Option(COLOR_OUTLINE, "Outline", "Plain and neutral, with a visible edge."),
    )

    /** The preset colours: fixed hues for people who want a colour, not a role. */
    val PRESETS: List<Option> = listOf(
        Option(COLOR_BLUE, "Blue", "A calm, neutral blue."),
        Option(COLOR_TEAL, "Teal", "Green-blue, quiet and clear."),
        Option(COLOR_GREEN, "Green", "Something that is going well."),
        Option(COLOR_LIME, "Lime", "A fresh, light green."),
        Option(COLOR_YELLOW, "Yellow", "Attention without alarm."),
        Option(COLOR_ORANGE, "Orange", "In progress, warm."),
        Option(COLOR_RED, "Red", "Urgent."),
        Option(COLOR_PINK, "Pink", "Soft emphasis."),
        Option(COLOR_PURPLE, "Purple", "Something set apart."),
        Option(COLOR_INDIGO, "Indigo", "Deep and serious."),
        Option(COLOR_BROWN, "Brown", "Earthy and settled."),
        Option(COLOR_SLATE, "Slate", "Grey-blue, deliberately plain."),
    )

    /** Every colour a card can be given without opening the custom picker. */
    val ALL_COLORS: List<Option> = COLORS + PRESETS

    /** True when the stored colour is a custom RGB value rather than a role or a preset. */
    fun isCustomColor(key: String?): Boolean = key != null && key.startsWith("#")

    /**
     * Reads `#RRGGBB` (or `#RGB`) into an ARGB long, or null when the text is not a colour.
     * Pure, so the picker's rule can be tested without a device.
     */
    fun parseHex(key: String?): Long? {
        val clean = key?.trim()?.removePrefix("#") ?: return null
        val expanded = when (clean.length) {
            3 -> clean.map { "" + it + it }.joinToString("")
            6 -> clean
            else -> return null
        }
        val value = expanded.toLongOrNull(16) ?: return null
        return 0xFF000000L or value
    }

    /** A colour as the key the model stores: `#RRGGBB`, upper case, always with the hash. */
    fun hexOf(red: Int, green: Int, blue: Int): String =
        "#" + listOf(red, green, blue).joinToString("") { channel ->
            channel.coerceIn(0, 255).toString(16).uppercase().padStart(2, '0')
        }

    /** The three channels of a stored colour, or null when it is not a colour. */
    fun rgbOf(key: String?): Triple<Int, Int, Int>? {
        val argb = parseHex(key) ?: return null
        return Triple(
            ((argb shr 16) and 0xFF).toInt(),
            ((argb shr 8) and 0xFF).toInt(),
            (argb and 0xFF).toInt(),
        )
    }

    /** A readable label for any stored colour — a role's name, a preset's name, or the hex itself. */
    fun colorLabel(key: String?): String = when {
        key == null -> "Default"
        else -> ALL_COLORS.firstOrNull { it.key == key }?.label ?: key
    }

    /**
     * Size presets, now in real board units rather than a multiplier, because a card can be resized
     * freely: a preset is just a convenient pair of numbers, and a free-form size matches none.
     */
    data class SizeOption(val w: Float, val h: Float, val label: String, val detail: String)

    val SIZES: List<SizeOption> = listOf(
        SizeOption(160f, 78f, "Small", "A compact card, for detail nodes."),
        SizeOption(StructureLayout.NODE_W, StructureLayout.NODE_H, "Normal", "The default card."),
        SizeOption(250f, 120f, "Large", "A roomier card, for important nodes."),
        SizeOption(300f, 144f, "Extra large", "The biggest card, for a headline node."),
    )

    /** The preset a card matches, or null when the card has been resized by hand. */
    fun presetFor(w: Float, h: Float): SizeOption? = SIZES.firstOrNull {
        kotlin.math.abs(it.w - w) < 4f && kotlin.math.abs(it.h - h) < 4f
    }

    /** The legacy multiplier used by files written before v0.9.0 — read once, on migration. */
    fun sizeOf(value: Float): Float = SIZES
        .minByOrNull { kotlin.math.abs(it.w / StructureLayout.NODE_W - value) }
        ?.let { it.w / StructureLayout.NODE_W }
        ?: 1f

    fun shapeLabel(key: String): String =
        SHAPES.firstOrNull { it.key == key }?.label ?: SHAPES.first().label

}