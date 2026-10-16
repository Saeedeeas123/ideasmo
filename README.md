# Ideasmo

An idea processor for Android: capture an idea, give it structure, keep it in one place.
Personal project of Saeed (`com.saeed.ideasmo`), built entirely on-device.

**Current version: v0.8.0** (versionCode 8)

## What it is

A **board** you travel through with touch — pan with one finger, pinch to zoom, drag the node cards —
with **typed relations** between the nodes. A relation is a sentence, not just a line: the catalog
holds 7 families and 42 types ("depends on", "is a", "contradicts", "causes" …), each with its own
line style, arrow mode and colour role.

## Feature map (v0.6.0)

| Area | What works |
|---|---|
| Board | Pan, pinch zoom 25–200 %, drag nodes, grid/pattern surface, travelling minimap, reset view, **origin marker + Origin button** |
| Nodes | **Node action square**: four bars around the selected node — Text · Image · Connect · Notes · More / Copy · Cut · Paste · Delete / Child · Sibling · Duplicate · Reattach / Resize · Colour · Shape. Generated from `NodeActionCatalog` (every action has an id + description) |
| Node style | Per-node size (4 steps), colour (5 theme roles), shape (4 shapes) — and the auto-layout measures each card, so a resized node gets its own space |
| Data surface | Every node and link carries an id; **More → Details** shows a node as machine-readable text (fields, coordinates from the origin, links with ids) and copies it in one tap |
| Links | Create from a node or from the link manager; **tap a link to open the inspector** |
| Link inspector | Change the relation, swap its direction, override its colour, add your own phrase, see the other relations between the same pair, delete with Undo |
| Node panel | **Links out** in stored branch order (up/down arrows, auto-order by name / priority / age) and **Links in**; every row opens the inspector |
| Structure | Central nodes (more than one), collapse/expand a branch, move a subtree left/right/up/down, **Arrange** in Right / Left / Down / Up / Radial at Compact / Normal / Wide spacing — **anchored on the board origin**, with Undo |
| Board modes | 8 surfaces — plain, grid, dots, graph paper, ruled lines, isometric, hex, blueprint — each with a live preview |
| Board themes | App theme (day/night + dynamic colour), paper, warm, mint, charcoal, blueprint |
| List view | The same nodes and the same relations, grouped into links out / links in, every row opening the inspector |
| Settings | Day/night, Help, app logs, About |
| Logging | Durable per-run log file + in-app log viewer with filters, copy, share, sessions (standing rule) |

## Build (on this device — proot Ubuntu ARM64)

Frozen toolchain: AGP 8.2.2 · Kotlin 1.9.22 · Compose BOM 2024.02.00 · compiler ext 1.5.8 ·
activity-compose 1.8.2 · compileSdk/targetSdk 35 · buildTools 35.0.1 · JDK 17 · Gradle 8.5.
`gradle.properties` must keep the aapt2 override (`android.aapt2FromMavenOverride` → the ARM-native
binary); an x86_64 aapt2 from Maven cannot execute here.

```bash
cd /root/projects/ideasmo
./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon   # ~3 min
cp -f app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/ideasmo-debug.apk
chmod 644 /sdcard/Download/ideasmo-debug.apk
```

The APK is installed manually by Saeed — that is the designed handoff (the agent has no install path).

## Source map

```
app/src/main/java/com/saeed/ideasmo/
  MainActivity.kt              state-based navigation + theme state + log/prefs init
  core/log/                    AppLog (durable per-run log) + LogViewerScreen
  core/model/LinkCatalog.kt    the 7 families / 42 relation types — single source of truth
  core/model/NodeStore.kt      nodes + links, JSON persistence, migration, structure operations
  core/model/StructureLayout.kt  the layout engine — pure Kotlin, JVM-tested
  core/prefs/                  ThemePrefs, BoardPrefs
  ui/board/                    BoardScreen, BoardListView, BoardPatterns, LinkDrawing,
                               LinkInspectorPanel, LinkPickerPanel, NodePanel,
                               ProjectSettingsPanel, NodeFormPanel
  ui/components/               Defaults (shared components), GridTile
  ui/home · ui/project · ui/settings · ui/help · ui/theme
app/src/test/java/.../StructureLayoutTest.kt   10 JVM tests for the structure engine
DESIGN.md                    the design contract (tokens, screens, decisions, honest limits)
```

## Version history

| Tag | What it added |
|---|---|
| v0.1.0 | Home screen (Compose M3 + design tokens), durable log + in-app viewer, adaptive icon |
| v0.2.0 | Project grid + Idea node editor, real zoom, day/night theme, node store |
| v0.3.0 | The board: pannable/pinch-zoom canvas with draggable nodes, visible grid, reset view |
| v0.4.0 | Node relations on the board + list view, travelling minimap, delete with Undo |
| v0.5.0 | Full typed-relation system (7 families / 42 types), link manager, real Settings + Help |
| v0.6.0 | Clickable/editable links (link inspector), node panel (links in/out + branch order), structure layer (central nodes, collapse, 5-direction Arrange), 8 board modes + 6 board themes |
| v0.7.0 | **Board origin** — a fixed (0, 0) with a marker on the canvas; Arrange now anchors the central node on the origin (deterministic, cannot drift); Origin button; Show-the-origin setting. Fixed the v0.6.0 double-origin drift bug |
| v0.8.0 | **Node action square** (four bars around the selected node) with 16 node actions from a catalog: rename, image, connect, notes, clipboard (copy/cut/paste), delete, child/sibling/duplicate, **reattach** (re-parent with cycle protection), resize/colour/shape — plus per-node style honoured by the layout, an app node clipboard, and a Details view that copies a node as machine-readable data |

## Honest limits (v0.8.0)

- **Group** and **Schema** from the reference app are not built — Group needs multi-select (which the
  board does not have yet) and Schema has two plausible meanings. Both are waiting on your answer.
- The clipboard is the **app's own** (nodes, not text) and lives in memory only.
- A node's height is still an estimate for the layout (the card grows with its content), so a very
  long note can make a card slightly taller than the space the auto-layout reserved.
- Arrange is a **tree** layout: dense graphs with many cross-links are tidied along their trees and the
  extra links stay where they are (no force-directed pass yet).
- Arrange moves the map to the **origin**; if you had deliberately parked a map somewhere else in board
  space, use Undo or drag it back (the origin is the intended home for a structured map).
- Z (the third axis) is **reserved, not built**: every node is on layer 0 and Z only decides drawing
  order. No raise/lower controls yet.
- The minimap is not interactive (deliberate — ordered left alone).
- No multi-select, no rubber-band selection, no node resize, and **no node edit after creation**.
- Collapse hides a subtree from the board and the list; a badge, the node panel's Expand or *Expand
  all* bring it back.
- No visual verification tooling (Paparazzi is unproven on this toolchain): the device install is the
  only visual check.
