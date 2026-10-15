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