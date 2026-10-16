package com.saeed.ideasmo.core.model

/**
 * The vocabulary a node's look is described in: shape, size and colour role.
 *
 * Kept in `core` as plain data with stable **keys** and human-readable **descriptions**, for the
 * same reason [LinkCatalog] is: the UI, Help, the stored JSON and (later) the language-model layer
 * all read from one list, so a style can be named, stored, exported and explained without any of
 * them drifting apart.
 *
 * Colours are Material **roles**, never raw hex — so day/night and dynamic colour keep working.
 */
object NodeStyle {

    // ---- shape ---------------------------------------------------------------
    const val SHAPE_ROUNDED = "rounded"
    const val SHAPE_SQUARE = "square"
    const val SHAPE_PILL = "pill"
    const val SHAPE_CUT = "cut"

    // ---- colour roles (null = the plain card) --------------------------------
    const val COLOR_PRIMARY = "primary"
    const val COLOR_SECONDARY = "secondary"
    const val COLOR_TERTIARY = "tertiary"
    const val COLOR_ERROR = "error"
    const val COLOR_OUTLINE = "outline"

    data class Option(val key: String, val label: String, val detail: String)

    val SHAPES: List<Option> = listOf(
        Option(SHAPE_ROUNDED, "Rounded", "The default card, 12-unit corners."),
        Option(SHAPE_SQUARE, "Square", "A sharp rectangle."),
        Option(SHAPE_PILL, "Pill", "Fully rounded ends."),
        Option(SHAPE_CUT, "Cut corner", "One corner cut, like a sticky note."),
    )

    val COLORS: List<Option> = listOf(
        Option(COLOR_PRIMARY, "Primary", "The app's main accent — the thing that matters most."),
        Option(COLOR_SECONDARY, "Secondary", "A quieter accent for supporting ideas."),
        Option(COLOR_TERTIARY, "Tertiary", "A contrasting accent — the central-node colour."),
        Option(COLOR_ERROR, "Error", "A warning or a problem."),
        Option(COLOR_OUTLINE, "Outline", "Plain and neutral, with a visible edge."),
    )

    /** Size multipliers. 1.0 is the card the board shipped with. */
    data class SizeOption(val value: Float, val label: String, val detail: String)

    val SIZES: List<SizeOption> = listOf(
        SizeOption(0.8f, "Small", "Compact card, for detail nodes."),
        SizeOption(1f, "Normal", "The default card."),
        SizeOption(1.25f, "Large", "Roomier card, for important nodes."),
        SizeOption(1.5f, "Extra large", "The biggest card, for a headline node."),
    )

    /** Clamps any stored or typed size onto the supported set, so bad data cannot break the board. */
    fun sizeOf(value: Float): Float = SIZES
        .minByOrNull { kotlin.math.abs(it.value - value) }
        ?.value
        ?: 1f

    fun shapeLabel(key: String): String =
        SHAPES.firstOrNull { it.key == key }?.label ?: SHAPES.first().label

    fun colorLabel(key: String?): String =
        key?.let { k -> COLORS.firstOrNull { it.key == k }?.label } ?: "Default"
}