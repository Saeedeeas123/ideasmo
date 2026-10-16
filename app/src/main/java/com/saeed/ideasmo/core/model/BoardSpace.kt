package com.saeed.ideasmo.core.model

/**
 * The board's coordinate system.
 *
 * - **X** — board units to the right. One unit is 1 dp at 100 % zoom.
 * - **Y** — board units downwards.
 * - **Z** — the layer a node lives on (0 = the board plane).
 *
 * The **origin** (0, 0) is the ONE fixed point in board space. The viewport can travel anywhere, a
 * node can be dragged anywhere, but the origin never moves. Everything that has to be *ordered*
 * is measured from it: Arrange puts the centre of the central node exactly on the origin and grows
 * the map from there.
 *
 * What that buys, concretely:
 * - Arrange becomes **deterministic and idempotent** — pressing it twice cannot drift, because the
 *   result is not derived from wherever the nodes happened to be, it is derived from the origin.
 * - A map has a **home**: the view can always be brought back to the origin, and the origin is where
 *   a new map starts (an empty board shows the origin in the middle of the screen).
 * - Two maps can later be **composed** deliberately: each one is arranged from the origin, then one
 *   is offset by a known amount — no guessing, no overlap.
 * - Node coordinates stay **absolute**, so they survive panning, zooming, restarts and screen
 *   changes, and they can be written down or compared between sessions.
 *
 * Z is reserved, not built: every node currently sits on layer [LAYER_BOARD] and Z only decides the
 * drawing order (older nodes are drawn first, so newer ones overlap them). A real Z control — raise
 * or lower a node, layer separation, per-layer dimming — lands on this axis without a rewrite.
 */
object BoardSpace {

    /** The origin, in board units. Everything else is measured from here. */
    const val ORIGIN_X = 0f
    const val ORIGIN_Y = 0f

    /** The layer every node sits on today. */
    const val LAYER_BOARD = 0
}