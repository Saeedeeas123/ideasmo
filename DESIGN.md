# DESIGN.md — Ideasmo

Design contract for this project. Written before UI code, per the `ui-design-kit` skill.
Any deviation from this file is a defect.

## 1. Intent

- **Product name:** Ideasmo
- **One sentence:** the home base of an idea processor — capture an idea, give it structure, keep it in one place.
- **Who uses it:** Saeed (personal tool).
- **Platform:** native Android (Compose), on-device.
- **Mood in 3 words:** calm, structured, quiet.
- **Reference feel:** a well-made note/knowledge tool — restrained, no decoration for its own sake.
- **Explicitly NOT:** playful, gradient-heavy, "landing page" style, rounded-everything.

## 2. Color

- **Seed:** #2E5E4E (deep green) — surfaces and text derive from the Material 3 roles.
- **Dynamic color (Material You):** enabled on API 31+ (`dynamicLight/DarkColorScheme`); the seed palette is the fallback on older devices.
- **Dark theme:** mandatory and shipped (`values-night` window background + `darkColorScheme`).

Roles used: `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `secondary`,
`surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `outline`, `error`,
`errorContainer`, `onErrorContainer`. **No other colors exist.** No raw hex in any screen.

## 3. Type

- System font (Roboto) — Material 3 default scale.
- One correction: `labelSmall` raised to **12sp** so nothing in the app is below the 12sp floor.

## 4. Tokens (do not deviate)

- **Spacing (4dp grid):** 4 / 8 / 12 / 16 / 24 / 32 / 48. Screen padding 16. Items 8. Sections 24.
- **Radius:** 8 small · 12 medium · 16 large · 28 extraLarge · pill for buttons.
- **Touch target minimum:** 48dp (header tabs, icon buttons, tiles all honour this).
- **Motion:** 150–200 ms small, 250–300 ms screen. Nothing over 400 ms.
- **Direction:** start/end only — never left/right (Persian must be a drop-in later).

## 5. Components (from `ui/components/Defaults.kt` — reused, never re-invented)

`PrimaryAction` · `SecondaryAction` · `DestructiveAction` · `SectionTitle` · `EmptyState` ·
`ErrorState` · `NavRow` · plus the home-only `EntryTile` and `HeaderTabs`.

Decisions already made: one filled pill primary action per screen; tonal tiles rather than
shadows; divider between groups only; long text is `maxLines` + ellipsis.

## 6. Screen inventory — v0.2.0

| Screen | Default | Empty | Loading | Error | Long content |
|---|---|---|---|---|---|
| Home | ✅ | ✅ (recent list empty) | n/a — no async source | n/a — no async source | ✅ scrolls, labels ellipsised |
| Project (wireframe 1) | ✅ 8 tiles | n/a (fixed menu) | n/a | n/a | ✅ scrolls |
| Idea node editor (wireframe 2) | ✅ | ✅ ("new node" only, no nodes yet) | n/a | ✅ field-level validation message | ✅ scrolls, name/description ellipsised |
| Logs (long-press Home title) | ✅ | ✅ ("No log lines yet.") | n/a | ✅ banner if the log file could not be opened | ✅ scrolls |

Honest note: loading/error states for the list screens arrive with the first module that reads
data remotely. They are not faked now. Validation error on the node form is real
(creating a node with no name is refused with an inline explanation + a log entry).

### 6.1 Wireframe 2 (Idea) — fidelity notes

| Sketch element | Implementation | Note |
|---|---|---|
| "new node" + circle button, centre | `NewNodeButton` — label above a 72dp primary circle | initially the only content, as drawn |
| Panel that opens on tap | `NodeForm` card below the button | inline (not a modal), matching the sketch's spatial flow |
| Priority ○○○○○ | 5 tappable dots, 48dp targets, filled up to the chosen level | default 3 |
| Node name | `OutlinedTextField` with a persistent label | required — empty name is refused |
| Description (+ Ai box) | description field, `Ai` tonal button in the section header | **deviation:** the sketch puts `Ai` inside the box's top-right corner; it is placed in the header row instead so it can never overlap typed text. Behaviour identical, honesty preserved: `Ai` reports "not built yet" |
| Pic (+) | 112dp box → system photo picker → image copied into app storage | copy (not just a URI) so it survives restarts without URI permissions |
| graph / zoom − + / List (bottom bar) | fixed 56dp toolbar, full 48dp targets | zoom is **real** (50–200%, scales the canvas content, shows the %). Graph and List report "not built yet" and log the tap |
| Top settings: dark / light | day/night toggle in the Idea top bar, applied app-wide and persisted | follows the system until first use |

### 6.2 Mobile adaptation of the form

The sketch lays Priority and Node name side by side. On a 360dp phone that would squeeze both,
so the form stacks them (Priority full width → Node name full width) and keeps the sketch's
second row intact (Description | Pic). No information is lost; nothing is clipped.

### 6.3 The Board (v0.3.0) — the shared foundation

The node page is now a real **board**, as Saeed ordered: *"we need a full map you can travel
through with touch, not a static page"*. Every module uses the same board, separated only by
node kind (`idea` / `mindmap` / `knowledge` / `note`).

**Interaction model**
- **Pan:** one finger drag anywhere on the board.
- **Zoom:** two-finger pinch, or the − / + toolbar buttons. Range **25 %–200 %**, 25 % steps.
  Both zoom paths keep the point under the finger / viewport centre fixed.
- **Drag a node:** press and drag the node itself. Node drag and board pan never fight — while a
  node is being dragged the board ignores pan.
- **Select:** tap a node (primary border + elevation). Tap empty board to deselect.
- **Reset view:** tap the zoom percentage label → back to 100 % with the content centred.
- **Create:** "new node" — centred label + circle when the board is empty (exactly as drawn), and a
  compact pill above the toolbar once nodes exist, so it never covers the board. New nodes land in
  the centre of what you are currently looking at.

**Coordinates:** nodes store `x` / `y` in **board units** (= dp at 100 % zoom), never screen pixels,
so layout survives panning, zooming and screen changes. Dragging divides the gesture delta by the
current scale, so a node tracks the finger at every zoom level.

**Persistence:** positions are saved once per drag *end* (not per frame), so a drag never hammers
the disk. Nodes written before the board existed (no `x` / `y`) are migrated onto a grid on load.

**Grid:** drawn in viewport space — only the visible lines are generated, with a heavier line every
500 units. It is what makes panning legible on an empty board.

**Deviation from v0.2.0:** the node form moved from *inside* the board to a fixed bottom panel with
keyboard insets. Inline was correct when the page could not move; on a pannable board a form that
drifts away under your finger would be unusable.

### 6.4 Relations, the travelling minimap and the list view (v0.4.0)

**Simple relations.** Two nodes can be connected; the connection is drawn as a line between their
centres, behind the cards, with the width following the zoom.
- **Creating one:** tap a node → the node action bar appears → **Connect** → tap the target node.
  A hint bar replaces the action bar during the wait, with Cancel. Self-links and duplicates are
  refused with an explicit message, never silently.
- **Data:** `NodeLink(fromId, toId)` — undirected, deduplicated both ways. Stored in the same
  `nodes.json` as `{"nodes":[…],"links":[…]}`; files written by older versions (a bare node array)
  are migrated on load, so no data is lost.
- **Where it shows:** the board (as lines), the **list view** ("Linked to: X, Y"), and the node card
  shows a small count of its connections.

**Node actions** (appear when a node is selected, above the toolbar): the node's name, **Connect**,
and **Delete**. Delete removes the node *and* its links, and offers **Undo** in the snackbar
(a destructive action always gets a way back).

**List view.** The list button is now real: it switches the board area to a list of the same nodes,
each row showing name, priority, description and its connections. The zoom controls disable in list
view (they mean nothing there) and the empty state points back to the board.

**The travelling minimap.** As Saeed specified: it is **not always on screen**. It appears the moment
you touch the board and start travelling, lingers ~900 ms after you let go, then disappears.
- Shows: every node (as blocks), the connections, the **current viewport** as an outlined rectangle,
  and **your finger as a red dot** — drawn with the `error` role so it stays a semantic colour rather
  than a hardcoded hex.
- The visible world is the union of the viewport and all nodes, plus 12 % padding, so it always
  frames both "where I am" and "where my stuff is".
- Size 200 × 140 dp, top-end corner, out of the way of the toolbar and of the hand.
- Honest note: it is driven by *any* touch on the board, so it also appears briefly while dragging a
  node. That is harmless and arguably informative, and it keeps the gesture tracking simple.

### 6.5 Typed relations — the full taxonomy (v0.5.0)

Saeed's order: *support literally everything* — every family, every link, every "reads as" — but keep
it **clean and organised**, because an LLM layer is planned later and will need the full vocabulary.

**Single source of truth:** `core/model/LinkCatalog.kt` holds 7 families and **42 link types**, each
with a name, a sentence template (`%1$s is a %2$s`), a real example, a one-line meaning, direction,
line style, arrow mode and colour role. The picker, the board and the Help screen are **generated
from it**, so they cannot drift apart.

**The families** (each with the question it answers):
Hierarchy · Composition · Association · Equivalence · Causality · Logic & argument · Process & time.
The full table lives in the knowledge base: `Ideasmo — Node Relation Taxonomy (7 Families, 42 Link Types)`.

**Creating a relation:** tap a node → action bar → **Link** → the link manager opens (it covers the
screen, so 42 types stay readable) → pick a type → the hint bar says *"Now tap the target for 'depends
on'"* → tap the target. A **custom phrase** field at the bottom is the escape hatch for relations the
catalog does not name.

**Managing relations:** the link manager lists a node's **existing links first**, each read as a full
sentence with its swatch, and each removable (**Undo** in the snackbar).

**Visual grammar** — 4 colour roles × 4 line styles × 3 arrow modes, plus the relation name pinned to
the middle of the line (drawn for every type except the neutral default, so a plain board stays calm):
- Lines are **trimmed to the node edges**, so nothing runs under a card and arrowheads stay visible.
- Double lines mark strong relations (composition, equivalence) without inventing another colour.
- Every colour is a Material 3 **role** — no raw hex, so dark mode and dynamic colour work.

**Decisions Saeed confirmed (2026-10-15):** several different link types may join the same pair
(duplicate protection is per *type*); the default stays `relates to`; polarity is expressed by
choosing `increases`/`decreases`/`prevents` rather than a separate +/− field.

**Help + Settings.** Settings is now a real screen (theme, Help, app logs, About — nothing
placeholder). Help documents the board gestures and **the entire relation catalog**, generated from
`LinkCatalog` so the documentation can never be out of date.

**Honest limits:** no canvas edge selection (links are managed from the node, not the line); no
direction flag stored (the arrow shows the reading direction); loop detection over causal polarity is
possible but not implemented.

### 6.6 Clickable links, the node panel, the structure layer and board modes (v0.6.0)

Saeed's order (2026-10-15): *links must be clickable and fully editable; every node must show the
links that leave it and the links that arrive at it; the structure of the map must be orderable —
with central nodes, sub-nodes, sub-links, expand/collapse and movement in four directions; and the
top-left corner must hold the project settings, starting with the board mode (eight patterns) and
the board's graphic theme.*

**A link is an object, not a side effect.** `NodeLink` gained an `id`, a colour override and a
branch `order`. That is what makes a link editable in place: type, phrase, direction and colour can
all change without deleting and recreating it, and an old `nodes.json` is migrated on load (ids are
generated for links written by earlier versions).

**Tapping a link (the link inspector).** A tap on the canvas is matched against the line segments
(distance to segment ≤ 26 px) and against the relation name chips, both of which open the same
inspector. It shows what the relation reads as, which two nodes it joins (From / To), the type's
meaning and example, **the other relations between the same pair** (each one tappable), the full
42-type catalog to change the relation, a colour override (five theme roles), a custom phrase, a
**Swap** action for the direction, and **Delete with Undo**. A tapped link is also highlighted on
the board with a halo, so it is findable when several relations cross the same area.

**Inside a node (the node panel).** Selecting a node shows the action bar — *Links · Link · Delete*;
long-pressing the node opens the panel directly. The panel lists **Links out** (in stored branch
order, numbered, with up/down arrows and auto-order by name / priority / age) and **Links in**, plus
the central-node toggle, the collapse toggle, the four direction buttons and Arrange. Every row
opens the link inspector, so a relation is reachable from either end and always edited the same way.

**Structure.** `StructureLayout` is a pure-Kotlin engine (no Android, no Compose — so it is
unit-tested on the JVM). Grounded in how established mind-map tools behave (Xmind documents
*structures*, *Advanced Layout* with free branch positioning and a one-click *Reorganize*):
- A **central node** can be set on any node, more than one is allowed, and it is toggleable. Each
  central node starts its own tree.
- **Arrange** lays those trees out in one of five directions — **Right, Left, Down, Up, Radial** —
  at one of three spacings (Compact / Normal / Wide), always following the branch order the user
  set. With no central node designated it falls back to "every node nothing points at is a root",
  so Arrange is useful before any structure exists. The whole operation offers **Undo**.
- **Collapse** hides a node's subtree (the data is kept, the card shows `+N hidden`, and expanding
  or *Expand all* brings it straight back). **Collapse all / Expand all** live in the settings.
- **Movement**: four direction buttons move a node **and everything under it** by 40 board units.
- Nodes that belong to no tree are never moved by Arrange.

**Project settings (top-left button on the board).** The first section is the **board mode** — eight
surfaces, each shown as a live miniature in the current theme: **Plain**, **Grid** (the original
surface, still the default), **Dots**, **Graph paper**, **Ruled lines**, **Isometric**, **Hex** and
**Blueprint** (which adds crosshair ticks). Then the **board theme** — App theme (follows day/night
and dynamic colour), Paper, Warm, Mint, Charcoal, Blueprint. Then **Structure** (direction, spacing,
Arrange, collapse/expand all) and **Display** (relation names on the board, the travelling minimap).
Every pattern is drawn in world space and transformed, so the surface pans and zooms with the board.
Patterns whose geometry would cost too much at low zoom (hex) or become a grey mush (grids, dots)
skip themselves instead of stuttering.

**Honest limits of v0.6.0:** Arrange is a tree layout, so a dense graph with many cross-links is
tidied along its trees and the extra links are left as they are (no force-directed pass yet); the
minimap is still not interactive; there is no multi-select and no rubber-band selection; collapse
hides a subtree from the board and the list (a badge and *Expand all* are the way back); nothing has
been device-tested yet — Saeed's install is the first real check.

### 6.7 The board origin — one fixed point everything is measured from (v0.7.0)

Saeed's diagnosis after testing v0.6.0: *"the board has no starting point, no initial anchor point —
and I think that is exactly why Arrange does not work."* He was right, and the fix is a coordinate
system with a fixed origin.

**The coordinate system** (`core/model/BoardSpace.kt`) — X is board units to the right, Y is board
units downwards, Z is the layer a node sits on. The **origin (0, 0) is the only fixed point in board
space**: the view can travel anywhere and nodes can be dragged anywhere, but the origin never moves.
The user never has to think about it and never has to work with it — they see one dot in a ring, and
that is all.

**What the user sees.** A dot inside a ring with four short arms, drawn at board (0, 0), scaled with
the zoom so it behaves like a mark on the floor: it stays put while the view travels over it. Drawn in
the board's accent role, under the links and under the cards, deliberately quiet. It can be hidden
from the project settings (Display → *Show the origin*), and the toolbar's **Origin** button brings
the view back to it from wherever you have travelled.

**Arrange, measured from the origin.** Pressing Arrange now puts the **centre of the first central
node exactly on the origin** and grows the map from there. The result depends on the origin, the
branch order, the direction and the spacing — and on nothing else. Consequences:

- **It cannot drift.** Pressing Arrange twice reproduces the identical map, whatever happened to the
  nodes in between (there is a JVM test for exactly this).
- **It is comparable.** Two maps arranged from the origin sit in the same place in board space, so
  they can be offset deliberately later instead of by accident.
- **The view follows the result** — after arranging, the viewport is centred on the origin, so the
  map is never arranged "somewhere off screen".
- **Manual positions are still respected**: only the designated trees move, and the whole operation
  still offers Undo.

**The bug this uncovered (v0.6.0, found by the new regression test):** the layout cursor was seeded
with the origin and the origin was then added *again* by the coordinate transform, so every Arrange
displaced the whole map by the origin it was given. With the origin at 0 that was invisible; with a
dragged board it was exactly the drift that made Arrange feel broken. Fixed by measuring the cursor
from zero and adding the origin once, in one place.

**Z is reserved, not built.** Every node currently sits on layer 0 and Z only decides the drawing
order (older nodes are drawn first, so newer ones overlap them). A real Z control — raise or lower a
node, layer separation, per-layer dimming — lands on this axis without a rewrite.

### 6.8 The node action square — every node feature in one place (v0.8.0)

Saeed sent a screenshot of a node menu from another mind-mapping app: **four bars forming a square
around the selected node** (top: Text · Image · Connect · Notes · More — left: Copy · Cut · Paste ·
Delete — right: Child · Sibling · Duplicate · Reattach — bottom: Resize · Color · Shape · Schema ·
Group) and asked for those node features in Ideasmo, with that square graphic. He also set a standing
rule for the whole project: **give everything an id and a description from now on**, because the
language-model layer that comes later needs a machine-readable surface.

**The catalogue comes first.** `core/model/NodeActionCatalog.kt` is the single source of truth: every
node operation has a stable **key** (`node.text`, `node.child`, …), a label, the **group** that is the
bar it belongs to, and a **description that states what it does and which parameters it takes**. The
four bars are generated from it, so a button can never exist without a description — and the model
layer gets the app's whole node vocabulary for free. `NodeStyle.kt` does the same for the look
(shape keys, colour roles, size steps), and a JVM test fails the build if an id, label or description
goes missing or a key is duplicated.

**The square.** `ui/board/NodeActionBars.kt` draws the four bars around the selected node. It lives in
**screen space**, not in the board's transformed layer, so the buttons keep a real 48 dp touch target
at every zoom; it is centred on the node with a minimum square size, and clamped to the viewport so a
bar can never be pushed off screen. It steps aside whenever a panel, a dialog or a pending
"tap a target" mode owns the screen, and Paste is shown as unavailable while the clipboard is empty
instead of failing silently. The single-row action bar of v0.6.0/v0.7.0 is gone.

**What each action does**

| Bar | Action | Behaviour |
|---|---|---|
| Top | **Text** | Rename, in a dialog, focused on open; an empty name is refused. |
| Top | **Image** | Pick a picture (copied into the app); if one exists, a dialog offers replace or remove. |
| Top | **Connect** | Pick a link type, then tap the target — the existing typed-relation flow. |
| Top | **Notes** | Edit the node's notes (the free text behind the card preview). |
| Top | **More** | The deep panel: links out and in, branch order, structure controls — and **Details**. |
| Left | **Copy / Cut / Paste** | The app's own node clipboard (`NodeClipboard`): Copy holds the node, Cut also removes it with Undo, Paste creates it at the centre of the view with a new id. Memory-only on purpose. |
| Left | **Delete** | Removes the node and every link that touches it, with Undo. |
| Right | **Child** | Creates a node **and** links it as a child, then names it straight away. |
| Right | **Sibling** | Same, but under this node's parent; refused with an explanation when the node has no parent. |
| Right | **Duplicate** | An unlinked copy, offset slightly, keeping name, notes, priority and style. |
| Right | **Reattach** | Re-parent: tap the new parent; the node's incoming links are replaced and Undo restores them. **Refuses a parent inside the node's own subtree**, so a branch can never be folded into itself. |
| Bottom | **Resize / Colour / Shape** | One style sheet, three sections. Size (small/normal/large/extra large), colour (a Material role blended into the card tone, or default), shape (rounded, square, pill, cut corner). |

**Per-node style, and the layout follows it.** `IdeaNode` gained `color`, `shape`, `size` and
`updatedAt`; `NodeLink` gained `note` and `createdAt` — every new field is optional and old files load
unchanged. The structure engine now measures **each node's own card** (`widthOf`/`heightOf`) instead of
one global constant: siblings are separated by their own height, the step between levels clears the
largest card on the board, and the origin anchor uses the central node's real half-size. Link trimming
is done per end, so a resized card still gets its lines stopped at its own edge.

**Everything has an id, and a node can be read as data.** `NodeStore.describe(nodeId)` writes the node
as plain text with stable field names — id, kind, priority, style, central/collapsed flags, position in
board units with the origin at (0, 0), timestamps, notes, and every link with its id, type, order,
colour and note. **More → Details** shows it and copies it to the clipboard in one tap: this is the
shape the language-model layer will read.

**Deliberately NOT built, and why** (the rule Saeed set: implement what is clear, ask about the rest):

- **Group** — needs multi-select, which the board does not have yet. Grouping one node is meaningless,
  so this waits for selection (rubber-band or long-press multi-select) rather than being guessed at.
- **Schema** — ambiguous in the reference app. Two readings: (a) the structure type of a branch (mind
  map / org chart / timeline), which in Ideasmo is already the global direction + spacing; or (b) a
  per-branch override of that. Both are useful and both are guesses, so neither was built.
- **The ⊖ button in the middle of the card** in the screenshot (next to the edit pencil) — ambiguous
  between "collapse the branch" and "remove the node". Collapse already exists in the node panel;
  guessing wrong would put a destructive button under the user's thumb.
- The **pencil** shortcut was left out because it only duplicates the top bar's Text.

### 6.9 Fixes and the second round of node work (v0.9.0)

Saeed's report after using v0.8.0 on the device, and what was done about each item:

**1. Back did not go back one step.** It left the board entirely. The board now installs its own
`BackHandler` and unwinds in order: dialog → panel → pending mode (connect / reattach) → tool →
node form → selection → and only then leaves the screen. So back inside the node stage returns to
the board, as he asked.

**2. The square flew apart when the board moved.** Each bar had been clamped to the viewport on its
own, so panning let them detach and stick to different edges. Now every bar is placed from the
**node's centre** and, when the square would leave the screen, **the whole square is shifted as one
unit** — the shape is preserved and the bars stay with their node while panning, zooming or dragging.

**3. The tools must live inside the square.** Resize, Colour and Shape no longer navigate to a full
screen. Tapping one turns the **bottom bar into the tool**:
- **Resize** — eight handles appear on the card (corners change both dimensions, edges change one;
  dragging a left/top handle moves the node so the opposite edge stays put), and the bar shows the
  live size in board units with **Done**. Free-form, as ordered.
- **Colour** — a palette of the five theme roles plus the default, right there.
- **Shape** — the four shapes, right there, each with a preview.
The Node style panel still exists (reachable from More) for the presets and the written descriptions.

**4. Free-form size is real data.** `IdeaNode.size` (a multiplier) was replaced by `w`/`h` in **board
units**; files written by v0.8.0 are migrated on load. The layout engine, link trimming, the minimap
and fit-to-view all measure the real card, and resize writes the store once per gesture (never per
frame).

**5. The floating message bar is gone.** Saeed's order: the toast was more annoying than useful. It
was also the only way back from a delete, so it was replaced by two better things: an inline
**NoticeBar** that sits in the layout (pending modes, refusals — never covering the board) and a
bounded **UndoStack** behind an always-visible **Undo** button in the toolbar, which names what it
will put back. A message is no longer the only guarantee that work can be recovered.

**6. Copy / Cut / Duplicate ask what travels.** A dialog with two ticks — **its links** (the nodes it
points at) and **its sub-links** (the whole branch below it) — as ordered. Sub-links imply links, so
the dialog can never offer an impossible combination, and a leaf node skips the dialog because there
is nothing to choose. A link only travels when **both** its ends travel, so a copy can never point at
a node that stayed behind. The clipboard now holds a fragment (nodes + links) instead of one node.

**7. More opens a rectangular panel from the right.** The node panel is docked to the right edge, full
height, rounded on its left side, leaving the board visible — as drawn in his sketch. It still holds
links out/in, branch order, structure controls and Details.

**8. Multi-select, rubber band and the group bar.** Selection is a list now: one node shows the
square, two or more show a **group bar** (count · Copy · Cut · Duplicate · Delete · nudge the whole
set · Clear). **Long-press the empty board** to draw a rubber band; **long-press a node** to toggle it
in or out of the selection; tapping inside an existing group also toggles. Dragging one selected node
moves **all** of them. Group operations respect the same fragment rules.

**9. The ⊖ / ⊕ control is on the card.** Saeed clarified that the minus button in the screenshot is
**collapse**, not delete: the card now carries a small circular control — ⊖ gathers the branch, ⊕
opens it again, and the card shows how many nodes are hidden.

**10. Schema — deferred on purpose.** He said to leave the direction/structure question for later, so
nothing was guessed at. **Group** is still open: multi-select now exists, so the next question is
whether a group is a frame that holds the selected nodes or a merge into one node.

## 7. Assets

- **Icons:** `material-icons-extended` 1.7.2 (local, no download). Outlined for chrome.
- **App icon:** custom adaptive icon (`ic_launcher_foreground` = idea-node mark, green background,
  plus a monochrome layer for themed icons). No default launcher icon.
- **Illustrations / Lottie:** none in v0.1.0.
- **Fonts:** system only.

## 8. Standing requirements

- ✅ Live log + in-app viewer (hard rule §30): durable per-run file, flushed per line, 10 sessions
  retained, written to the app external files dir **and** mirrored to `Download/Ideasmo/logs`,
  uncaught-exception handler, memory logged, viewer with filter / copy / share / sessions /
  auto-scroll toggle, visible banner when the file cannot be opened.
- ✅ Dark theme verified.
- ✅ Window insets: `enableEdgeToEdge()` + `Scaffold` insets, so nothing hides behind the status
  or navigation bar (the failure mode reported on 2026-10-14).
- ⏳ `@Preview` + screenshot tests: not yet (no screenshot module in v0.1.0).

## 9. Forbidden list

No gradient hero · no emoji icons · no three-identical-cards · nothing "everything centered" ·
no hardcoded hex in screens · no off-grid spacing · no shadows everywhere · no text under 12sp ·
no light-only · no sub-48dp targets · no competing filled buttons · no raw exception text ·
no placeholder/lorem/TODO text · no layout that only works at one font scale ·
no dependency where a Compose `Canvas` would do.

## 10. Wireframe fidelity (Saeed's whiteboard sketch, 2026-10-15)

| Sketch element | Implementation | Note |
|---|---|---|
| Top-left box | Calendar icon button (app-bar navigation icon) | **Assumption:** the sketch box is the calendar entry, since a calendar was requested and no other slot fits |
| "Ideasmo" title (centre) | `CenterAlignedTopAppBar` title | long-press opens the log viewer (hidden affordance, documented) |
| Gear + "Setting" | Settings icon button (app-bar action) | |
| Three words under the title | `HeaderTabs`: خانه / منو / تاریخ | **Assumption:** read as Home / Menu / History tabs; labels live in `strings.xml` for one-line changes |
| Template + / Project + / Drive / Knowledge base | 2×2 `EntryTile` grid, `+` badge on the two create actions | |
| "search ..." bar | `OutlinedTextField` with search icon, IME action = Search | |
| Three recent rows | `NavRow` list, currently the **empty state** | Real rows appear with the Project module — no fake data is shipped |

## 11. Sign-off

- Implemented: 2026-10-15 — **v0.1.0** (home screen) and **v0.2.0** (Project + Idea core)
- Verified how: build + install by Saeed (device loop). Paparazzi screenshot module still not set up.
- Rounds of self-critique: 1 per version (structure/token pass against the checklist above).
- Saeed's verdict on v0.1.0 (2026-10-15): everything works, the design improved a lot, accepted.
- Open visual issues: none known; first judgement of the new screens belongs to Saeed on the device.

### v0.6.0 sign-off (2026-10-15)

- Implemented: clickable/editable links (link inspector), the node panel with ordered Links out and
  Links in, the structure layer (central nodes, branch order, collapse, five layout directions,
  Arrange with Undo), the project settings behind the top-left button with eight board modes and six
  board themes, and a list view that mirrors the board.
- Verified how: `:app:testDebugUnitTest` **10/10 green** (the structure engine, on the JVM) and
  `:app:assembleDebug` **BUILD SUCCESSFUL in 3m 27s with 0 warnings**; APK badging checked
  (versionCode 6 / versionName 0.6.0); every new class confirmed present in the APK's dex.
- Not verified: the visual result and the touch behaviour on the device. No screenshot tooling is
  set up (Paparazzi remains unproven on this toolchain), so the first real check is Saeed installing
  `/sdcard/Download/ideasmo-debug.apk`.
- Open visual questions for that check: does a link tap land reliably on a thin line; is the node
  panel reachable often enough (single tap → action bar, long-press → panel); does the blueprint /
  charcoal board read well against the node cards; does the hex pattern appear at normal zoom.

### v0.7.0 sign-off (2026-10-15)

- Implemented: the board's coordinate system with a fixed origin (`BoardSpace`), the origin marker on
  the canvas, **Arrange anchored on the origin** (the central node's centre on (0, 0)), the view
  following the arrangement, the **Origin** toolbar button, the *Show the origin* display setting and
  Help text.
- Found and fixed on the way: the v0.6.0 double-origin bug that displaced the whole map on every
  Arrange — caught by a new regression test, not by eye. This is very likely the concrete reason
  Arrange felt like it did not work.
- Verified how: `:app:testDebugUnitTest` **14/14 green** (the structure engine, now including "the
  central node lands on the origin", "arranging twice cannot drift", "anchoring works for radial" and
  the double-offset regression) and `:app:assembleDebug` **BUILD SUCCESSFUL in 2m 12s with 0
  warnings**; APK badging v0.7.0 (versionCode 7); `BoardSpace` and `drawBoardOrigin` confirmed in the
  APK dex.
- Not verified: the same gap as v0.6.0 — the look and the touch feel on the device. The first real
  check is Saeed installing the APK.

### v0.8.0 sign-off (2026-10-15)
- Implemented: the node action square (four bars around the selected node, generated from
  `NodeActionCatalog`) with 16 node actions — Text, Image, Connect, Notes, More, Copy, Cut, Paste,
  Delete, Child, Sibling, Duplicate, Reattach, Resize, Colour, Shape — plus per-node style
  (colour/shape/size) that the layout engine now respects, an app node clipboard, a Details view that
  copies a node as machine-readable data, and ids + descriptions on every action and every style
  option.
- Not implemented on purpose (and why, in section 6.8): **Group** (needs multi-select, which does not
  exist yet), **Schema** (ambiguous — two plausible readings), the **⊖** button (ambiguous between
  collapse and delete), and the pencil shortcut (duplicates Text).
- Verified how: `:app:testDebugUnitTest` **24/24 green** (16 structure-engine tests including per-node
  sizes and origin anchoring with a resized central node, plus 8 catalogue/style tests) and
  `:app:assembleDebug` **BUILD SUCCESSFUL in 2m 48s with 0 warnings**; APK badging v0.8.0
  (versionCode 8); every new class confirmed in the APK dex.
- Not verified: the same standing gap — the look and the touch feel on the device. The square is the
  biggest new visual surface in the app, so the device check matters more than usual here: does the
  square fit at 100 % zoom, does it stay reachable at the edges of a node near the screen border, and
  does it get in the way while dragging.

### v0.10.0 sign-off (2026-10-16)
- Implemented, item by item from Saeed's report on v0.9.0 (with the two screenshots):
  1. **The action square is relative to its node** and can no longer lose a bar. It scales with the
     board (`SQUARE_MIN_SCALE` = 0.45, capped at 1), and the viewport adjustment is now a **single
     clamp** (`minDx..maxDx`) instead of two independent pushes that could undo each other — which is
     exactly how the left bar disappeared at 25 % zoom, where the old square was ~478 dp wide on a
     412 dp screen. If the square still cannot fit, it is centred on the viewport so it keeps its shape.
     Below `LABEL_MIN_SCALE` (0.7) the bars drop their labels and become icon-only (44 dp natural
     height instead of 60), which keeps them legible instead of turning into grey mush. The scaling is
     done by composing the square inside a **scaled `Density`** (`LocalDensity`), not by a draw-time
     `graphicsLayer`: the bars' own layout boxes are then exactly the pixels on screen, so every
     button keeps an exact touch target at every zoom and nothing depends on hit-testing through a
     layer transform.
  2. **Resize handles**: 22 dp at 100 % zoom instead of 30 dp, they shrink with the card
     (`HANDLE_MIN` = 12 dp floor), they are capped at a third of the card so the eight of them cannot
     collapse into one blob, their touch box is capped at half the card so they cannot steal each
     other's drags, and the dashed outline + the ring width follow the zoom too.
  3. **Zoom out to 5 %** (`MIN_SCALE`), with `zoomStepAt()`: 25 % steps above a quarter (the familiar
     behaviour) and 5 % steps below it, otherwise 5 % would be unreachable.
  4. **The docked node panel is 40 % of the screen** (`PANEL_WIDTH_FRACTION`). v0.9.0 computed
     `(viewportW * 0.82).coerceAtMost(340f)` and then treated the PIXELS as dp, so the panel came out
     ~130 dp wide and its content about 98 dp — every line broke into fragments and the panel was hard
     to tell apart from the board. Now: 40 % of the real width, `surfaceVariant` + outline + shadow so
     it reads as a panel, tighter content padding, and the rows rebuilt to fit a narrow panel
     (two-line branch rows, 30 dp swatches, scrollable chip rows).
  5. **Arrange moved into the node's own properties** (`StructureLayout.arrangeBranch`): Left / Right /
     Up / Down chips in the node panel lay out **only that node's subtree**, and the selected node
     **stays exactly where it is**. The board's Arrange button is removed. The whole-map Arrange (which
     still anchors on the origin) remains in the project settings.
- Also fixed, because it is the other half of "arrange does not arrange properly":
  `StructureLayout.spaceOf()` — the auto-layout reserves the size each card **really measures**. The
  board reports it from `onSizeChanged` (`NodeStore.noteRenderedSize`, memory only), and the engine
  takes it as an explicit `measured` map so it stays pure and JVM-testable. And in `linear()`, Left/Up
  now offset back towards the parent by the **node's own size**, not by the `NODE_W` / `NODE_H`
  constants (a resized card used to straddle the level line).
- Verified how: `:app:testDebugUnitTest` **36/36 green, 0 failures, 0 errors** — StructureLayoutTest is
  now 22 (six new: the branch arrange keeps its node in place, Left/Right/Up/Down each send the subtree
  the right way, a leaf arranges nothing, a collapsed subtree is skipped, the measured card size is
  reserved, and Left/Up use the card's own size), plus NodeCatalogTest 8 and FragmentBuilderTest 6.
  `:app:assembleDebug` **BUILD SUCCESSFUL in 2m 25s with 0 warnings**.
- Not verified: the same standing gap as every build before it — **no device verification**. The look
  and the touch feel of the scaled square, the smaller handles, the 40 % panel and the 5 % zoom are
  unverified until Saeed installs the APK. The specific open questions: is a 28 dp icon-only button
  tappable enough at 25 % zoom, and does 5 % zoom reveal the whole map usefully or does the board
  pattern get too dense.

### v0.11.0 sign-off (2026-10-16, second round on the same day)
- **THE REAL BUG OF THE WHOLE ROUND: one board space.** A node's `x`/`y` were handed to
  `Modifier.offset { IntOffset(...) }`, which is **pixels**, while the card itself was sized with
  `widthOf(node).dp`, which is **dp × density**. The two were a factor of `density` (~2.6 on this
  device) apart, so:
  - the auto-layout reserved ~2.6x less room along the sibling and level axes than the cards actually
    took — the pile of overlapping cards in Saeed's screenshot;
  - the link lines were drawn to points that did not match the cards;
  - the resize handles were positioned around a rectangle 2.6x too small, which is why eight of them
    collapsed into a blob at the top-left of the card (his screenshot) instead of sitting on the
    corners and edge midpoints;
  - the action square and the node panel were anchored to the same wrong rectangle.
  `BoardState` now carries the density, everything converts through it (`worldToScreenX`,
  `screenToWorldX`, `screenRectOf`, `resetView`, `centreOnWorld`, the node offset, and the drag delta,
  which needs `/(scale * density)`), and board units mean exactly what [BoardSpace] documents: dp at
  100 % zoom. A new `centreOnWorldBand` centres a node in the strip that stays visible above the sheet.
- **The action box replaces the four-bar square** (`NodeActionBox`, `NodeActionBars` deleted): one
  rounded rectangle above the node, six columns, all sixteen catalogue actions in one place, flipping
  below the node when there is no room above, and **never scaling with the zoom** — the v0.10.0
  proportional attempt is what he called torture. The tools (Resize/Colour/Shape) open an extra row
  inside the same box.
- **The node sheet replaces the docked right panel**: `More` opens a full-width surface over the bottom
  70 % of the screen (`SHEET_HEIGHT_FRACTION`) behind a scrim, with the node itself **pinned at the
  top** — drawn by the new shared `NodeCardBody`, extracted from `BoardNode` so the sheet header is
  literally the card you tapped. Opening it also re-centres the view on the node inside the visible
  strip.
- **Arrange respects the links** (his item 5). `linear()` is now a two-pass tidy-tree: pass 1 packs the
  sibling axis as before; pass 2 places each child exactly one `levelGap` from **its own parent's edge**,
  so every link comes out the same length whatever the card sizes (v0.10.0 stepped by the largest card
  on the board). `arrangeBranch` then slides the branch along its cross axis until it is clear of nodes
  that are not part of it (bounded, deterministic, `MAX_CLEAR_STEPS`), and the whole-map `arrange` parks
  loose nodes in a column beside the result instead of leaving them on top of it (`park`).
- Verified how: `:app:testDebugUnitTest` **40/40 green, 0 failures, 0 errors** — StructureLayoutTest is
  now 26 with four new tests (every link is the same length after Arrange; a branch is slid clear of a
  blocker and its own node does not move; arranging the same branch twice is identical; loose nodes are
  parked clear of the map). `:app:assembleDebug` **BUILD SUCCESSFUL with 0 Kotlin warnings**.
- Not verified: the device again. The specific things to look at: does the box above the node cover
  something important when the node is mid-screen; does 6 columns keep the labels readable on his
  screen; is 70 % the right height for the sheet; and does the spread-out board (see below) look right
  or should the old arrangement be re-run.

### v0.12.0 sign-off (2026-10-17)
- **What a node carries became lists.** `IdeaNode.notes`, `.pictures` and `.files` (all `List<String>`,
  no limit) replace the single `description` / `picUri`; the loader migrates an older node by turning
  its description into its first note and its picture into its first image, so nothing is lost. New
  store operations `addNote/setNote/removeNote`, `addPicture/removePicture`, `addFile/removeFile`;
  `describe()` now prints the counts and every entry with its index.
- **The card shows counts, never content** (`NodeCardBody`): small icon + number for links, notes,
  images and files. That is also why his items 3 and 4 are one change — the counts that used to need a
  list view now live on the node, so the list view could go.
- **The collapse control can no longer hide** (his screenshot): it is the FIRST element of the card's
  second row, 24 dp, so a narrow card cannot push it out, and the "+N hidden" badge is one compact
  label at the end of the row.
- **The box follows his order exactly** (`NodeActionCatalog.box`): Name · Child · Sibling · Connect ·
  Note · Image · Files · Shape · Colour · Resize · Copy · Cut · Paste · Delete · Reattach · More.
  `node.text` keeps its id but is labelled **Name**; a new `node.files` action was added; `Duplicate`
  left the box (it is not in his list) and is offered from the node sheet instead, so nothing is lost.
  Each cell can carry a **count badge** (Note, Image, Files, Connect, Child).
- **Attachments UI** (`NodeAttachments.kt`): `NodeNotesDialog`, `NodeImagesDialog` (thumbnails) and
  `NodeFilesDialog` list what is already there, delete, and add new — images through the system photo
  picker, files through `OpenDocument` and `copyFileIntoAppStorage` (new `core/util/Files.kt`).
- **The bottom bar is rebuilt to his layout**: [Fit to screen] [Undo] | [−] [zoom %] [+] | [Redo]
  [View/Edit]. `BoardState.fitTo()` picks the zoom that shows every node; **Redo is new** and is built
  from whole-board snapshots (`NodeStore.snapshot()/restoreSnapshot`) inside `UndoStack.undo/redo`, so
  none of the fifteen existing undo call sites needed a second closure. **View mode** is read-only:
  taps select nothing, nodes do not drag, no create/select controls, Undo/Redo disabled.
- **Select nodes** (next to New node): Select mode where a tap toggles a node in or out of the
  selection and a **drag on the empty board draws the selection window** (press = corner, drag, release
  = select everything it touches) — no long press any more. Panning is disabled in Select mode so the
  window can be drawn; pinch still zooms.
- **Removed on his order**: the list view (`BoardListView.kt` deleted), the graph button, and the
  Origin button, marker and setting. `BoardSpace`'s origin still exists internally because Arrange
  anchors on it.
- Verified how: `:app:testDebugUnitTest` **41/41 green** — NodeCatalogTest is 9 with a new contract
  test that pins the box's exact order, the Name/Note/Files labels and the fact that Duplicate is in
  the catalogue but not in the box; StructureLayoutTest 26; FragmentBuilderTest 6.
  `:app:assembleDebug` **BUILD SUCCESSFUL with 0 Kotlin warnings** (two deprecated icons converted to
  their AutoMirrored versions).
- Not verified: the device. The specific things to look at: does a six-column box read well with the
  new order; are the count badges legible; does the 24 dp collapse circle still feel tappable; do the
  attachment dialogs read well with several notes; and does the selection window feel right without
  the long press.

### v0.13.0 sign-off (2026-10-17, second round)
- **The Drive exists.** `core/drive/DriveStore.kt` owns one folder — `filesDir/drive` — and everything the
  app stores goes through it: `list`, `importUri`, `createFolder`, `writeText`, `rename`, `delete`,
  `nodeFolder`, `migrateLegacy`, plus the pure rules in `DrivePaths.kt` (kind by extension, safe names,
  collision-free names, human sizes and dates). Paths are always relative to the Drive root, so the whole
  store can be moved or backed up as one folder; `..` is refused; a delete moves the entry to `.trash`.
- **The file manager** (`ui/drive/DriveScreen.kt`) is deliberately light: a title with the bytes used, a
  breadcrumb you can tap, one column of rows (kind icon, name, size · date), Add a file, New folder, and a
  delete with a confirmation that says the file goes to the trash. Tapping a folder opens it; tapping an
  image opens it in a viewer; tapping a text file shows the text; anything else says honestly that there is
  no in-app viewer. It is reachable from the **Home → Drive** tile.
- **The Drive and the nodes are the same storage.** `pickImage`/`pickFile` on the board now import through
  `DriveStore.importUri(..., DriveStore.nodeFolder(name))` instead of writing to `filesDir/pics|files`, so
  every picture and file a node carries is a Drive file. The node's Image and Files dialogs gained **From
  Drive**, which opens `DrivePickerDialog` (folders navigable, images filtered for pictures) and attaches a
  file that is already stored — no copy, one file.
- **Nothing is lost on upgrade**: `DriveStore.migrateLegacy` moves `pics/` and `files/` into
  `Attachments/Imported/` on the first launch, and `NodeStore.remapAttachments` rewrites the stored paths
  of every node in one pass. Both run in `MainActivity.onCreate`, before any screen is composed.
- Verified how: `:app:testDebugUnitTest` **47/47 green** — the new `DrivePathsTest` is 6 tests (every kind
  classified from the name, names sanitised, a second file with the same name gets a counter, sizes read as
  a human writes them, paths join and step up, a row subtitle is never empty); plus StructureLayoutTest 26,
  NodeCatalogTest 9, FragmentBuilderTest 6. `:app:assembleDebug` **BUILD SUCCESSFUL with 0 Kotlin warnings**
  (one label-shadowing warning found and fixed in the migration loop; two strings needed their apostrophes
  escaped, the same aapt2 trap as v0.6.0).
- Not verified: the device. Things to look at: does the file manager feel light enough; is the breadcrumb
  clear at three levels deep; does the viewer handle a big photo; do node attachments appear under
  `Attachments/<node>`; and does the migration keep every existing picture.
- Still open: a file in the Drive cannot be OPENED in another app yet (no FileProvider — the Drive shows
  what it can show itself); no rename or move inside the Drive (create, import, delete are in); notes are
  not yet exported as Drive files even though `DriveStore.writeText` exists for it.
