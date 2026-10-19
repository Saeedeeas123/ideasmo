# Ideasmo

**A tool for idea makers.**

Capture an idea, give it structure, keep it in one place. A touch-first **board** of node cards joined by
**typed relations** — because an idea is rarely alone, and "this causes that" is not the same thing as
"this contradicts that".

Android · Kotlin · Jetpack Compose M3 · **offline** — no account, no cloud, no network permission.
Personal project of Saeed (`com.saeed.ideasmo`), built entirely on-device.

**Current version: v0.17.0** (versionCode 17)

---

## Why it exists

Most note apps hand you a list, and a list has exactly one dimension: order. Ideas do not live in one
dimension. They depend on each other, contradict each other, cause each other, and arrive in clusters
rather than queues.

The usual fix is a mind-map — which gives you lines, and a line says only "these two are connected".
That is not what the thinking was. The thinking was *"this depends on that, and it contradicts the
other thing I decided last month"*.

So Ideasmo makes the relation the first-class object. A relation is a sentence with a type, a
direction, a style and a colour role. The board is not a picture of the graph — **the board is the
graph**, and every relation is editable where you find it.

---

## Where it is now (v0.17.0)

**A complete, usable single-user tool.** The board works, the data persists, the model is stable, and
the app has been through seventeen tagged versions and a long series of real device tests.

What that means concretely:

- **The board** — pan with one finger, pinch-zoom from 5 % to 200 %, drag node cards, eight surface
  modes and six themes.
- **The relations** — a catalog of **7 families and 42 types** ("depends on", "is a", "contradicts",
  "causes" …), each with its own line style, arrow mode and colour role. Tap a link and the inspector
  opens: change the type, swap the direction, override the colour, write your own phrase, or see the
  other relations between the same pair.
- **The nodes** — nine card shapes, seventeen colours (including an RGB builder), free-form resize,
  and any number of notes, images and files attached to a node.
- **The Drive** — one private file store for the whole app, with shelves (images, texts, documents,
  audio, video, archives), recursive search, sort, and *open with…* so a PDF or a video opens in the
  app that knows how to read it.
- **The structure layer** — collapse a branch, mark central nodes, and **Arrange** a branch or the
  whole map in a direction, anchored on the board's fixed origin.
- **The logging** — a durable per-run log file plus an in-app viewer with filters, copy, share and
  session history. This is a standing rule of the project, not a feature.

Everything above is local. There is no server, no sync and no telemetry.

---

## Roadmap

> This is current intent, in priority order — not a promise. Items move when a real device test says
they should.

### Next — decided, waiting on one answer or one decision

| # | Item | What is actually blocking it |
|---|---|---|
| 1 | **Group** — put several nodes into one thing | One open question: is a group a **frame that holds** the selected nodes, or a **merge** of them into one node? The behaviour is specified; the model is not. |
| 2 | **Schema** — per-branch direction stored on the node | Deferred on Saeed's instruction. Today *Arrange a branch* is a **command**, not a stored property, so the whole-map Arrange still lays everything out in the project direction. Schema is the other half of that. |
| 3 | **Undo that survives a session** | Undo is memory-only and bounded to the last 12 steps; closing the app clears it. A persisted, deeper undo stack is the obvious next quality step. |

### Later — agreed in principle, not scheduled

- **Z — the third axis.** Currently reserved and not built: every node sits on layer 0 and Z only
  decides drawing order. No raise/lower controls yet.
- **A force-directed pass in Arrange.** Today Arrange is a **tree** layout: dense graphs with many
  cross-links are tidied along their trees and the extra links stay where they are.
- **An interactive minimap.** It travels with you today but does not accept input — deliberately left
  alone so far.
- **Visual verification tooling.** Paparazzi is unproven on this build toolchain, so the device install
  is currently the only visual check. Closing that gap would speed up every UI change.

### Explicitly not yet decided

- Whether a project can hold **more than one board**.
- Whether anything ever leaves the device.

The second one is deliberate. Ideasmo is offline-first because the ideas are not the sort of thing you
hand to a server, and because an idea tool that needs a network is a tool you cannot use in the moment
you actually have the idea.

---

## Feature map

| Area | What works |
|---|---|
| Board | Pan, pinch zoom 5–200 %, drag nodes, 8 surface modes (plain, grid, dots, graph paper, ruled, isometric, hex, blueprint), travelling minimap, Fit, origin marker |
| Nodes | Per-node name, priority, size, shape and colour; any number of **notes**, **images** and **files**; the card shows only small icons with counts, never the content |
| Node action box | One rectangle above the selected node holding all sixteen actions in a 6-column grid: Name · SubNode · CoNode · Connect · Note · Image · Files · Shape · Colour · Resize · Copy · Cut · Paste · Delete · Reattach · More |
| Relations | 7 families / 42 types, searchable picker with family chips, per-relation note, colour override, direction swap, custom phrase, jump-to-either-end |
| Link inspector | Open by tapping any link on the board; shows the other relations between the same pair and deletes with Undo |
| Node panel | Links out in stored branch order (with auto-order by name / priority / age) and links in; every row opens the inspector |
| Structure | Central nodes, collapse/expand a branch, move a subtree, Arrange Right / Left / Down / Up / Radial at Compact / Normal / Wide spacing, anchored on the origin, with Undo |
| Multi-select | Rubber band on the board (long press, then drag) and a group bar; dragging moves the whole selection |
| View / Edit | A real read-only mode for showing a board without changing it |
| The Drive | One private file store: shelves with live counts, folders, import, rename, move, copy, delete to `.trash`, recursive search, sort, *open with…* through a FileProvider |
| Notes | A text menu — bold, italic, smaller/larger, font choice — with a live preview; formatting lives in the text as markers so nothing is lost on export |
| Settings | An index of five pages — **Board · Modes · Links · Nodes · App** — one page each |
| Logging | Durable per-run log file + in-app viewer with filters, copy, share and session history |

---

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

---

## Source map

```
app/src/main/java/com/saeed/ideasmo/
  MainActivity.kt              state-based navigation + theme state + log/prefs init
  core/log/                    AppLog (durable per-run log) + LogViewerScreen
  core/model/LinkCatalog.kt    the 7 families / 42 relation types — single source of truth
  core/model/NodeStore.kt      nodes + links, JSON persistence, migration, structure operations
  core/model/StructureLayout.kt  the layout engine — pure Kotlin, JVM-tested
  core/model/NodeActionCatalog.kt  the sixteen node actions, each with an id + description
  core/drive/                  DriveStore, DrivePaths (pure, JVM-tested), DriveOpen (FileProvider)
  core/prefs/                  ThemePrefs, BoardPrefs, NodePrefs
  ui/board/                    BoardScreen, BoardPatterns, LinkDrawing, LinkInspectorPanel,
                               LinkPickerPanel, NodePanel, NodeActionBox, ProjectSettingsPanel,
                               NodeFormPanel, NodeAttachments, NoteEditing, FragmentOptions
  ui/drive/ ui/help/ ui/home/ ui/project/ ui/settings/ ui/theme/ ui/components/
app/src/test/java/.../         JVM unit tests for the pure layers: the layout engine, drive paths,
                               the link catalog, the node catalog, note text, fragments
DESIGN.md                      the design contract (tokens, screens, decisions, honest limits)
```

The rule the code follows: **anything that can be pure Kotlin is pure Kotlin and tested on the JVM.**
The layout engine, the path rules, the catalogs and the text formatting have no Android dependency,
which is why they can be tested without a device.

---

## Version history

| Tag | What it added |
|---|---|
| v0.1.0 | Home screen (Compose M3 + design tokens), durable log + in-app viewer, adaptive icon |
| v0.2.0 | Project grid + Idea node editor, real zoom, day/night theme, node store |
| v0.3.0 | The board: pannable/pinch-zoom canvas with draggable nodes, visible grid, reset view |
| v0.4.0 | Node relations on the board + list view, travelling minimap, delete with Undo |
| v0.5.0 | Full typed-relation system (7 families / 42 types), link manager, real Settings + Help |
| v0.6.0 | Clickable/editable links (link inspector), node panel (links in/out + branch order), structure layer (central nodes, collapse, 5-direction Arrange), 8 board modes + 6 board themes |
| v0.7.0 | **Board origin** — a fixed (0, 0) with a marker on the canvas; Arrange now anchors the central node on the origin (deterministic, cannot drift). Fixed the v0.6.0 double-origin drift bug |
| v0.8.0 | **Node action square** (four bars around the selected node) with 16 node actions from a catalog — including **reattach** (re-parent with cycle protection) — plus per-node style honoured by the layout and a Details view that copies a node as machine-readable data |
| v0.9.0 | **Back is a step**; the action square becomes **relative to its node**; **tools inside the square** (free-form resize, colour palette, shape picker); per-node `w`/`h` in board units; **the toast is gone** (inline NoticeBar + a real undo stack); Copy/Cut/Duplicate ask whether links travel; **multi-select + rubber band + group bar**; the collapse ⊖/⊕ control moves onto the card |
| v0.10.0 | **The square scales with the board** and can never lose a bar; **resize handles shrink with the card**; **zoom out to 5 %**; the node panel becomes 40 % of the screen; **Arrange moves into the node's own properties**; the layout reserves the size each card really measures, so branches cannot overlap |
| v0.11.0 | **One board space** — a node's stored position becomes board units (dp at 100 %), so the auto-layout, the link lines, the handles and the action box all agree with the card on screen. **The action box**: one rectangle above the node, 6-column grid, fixed size at every zoom. **The node sheet**: More opens a full-width rectangle over the bottom 70 %. **Standard link length** after an Arrange |
| v0.12.0 | **Nodes carry lists**: any number of notes, images and files; the card shows only icons with counts. **Name** replaces Text; the box order becomes the current sixteen. **The list and graph buttons are gone** (the board is the graph); the toolbar is Fit · Undo \| − % + \| Redo · View/Edit; **View mode is read-only**; **Select nodes** with a drag window |
| v0.13.0 | **The Drive** — one private file store for the whole app, and **it is the same storage the nodes use**: a file picked for a node is copied under `Attachments/<node>/`, and the pre-Drive folders are migrated in on first run so nothing is lost |
| v0.14.0 | **The Drive's basic operations** — rename, move, copy, delete, recursive **search**, **sort** — plus **open with…** through a FileProvider, so PDFs, documents, audio and video finally open. A note can be saved into the Drive as a text file. **The insets bug is fixed for good** |
| v0.15.0 | **The Drive is file-first**: shelves with live counts, and picking one lists every file of that kind wherever it sits. **Import follows the shelf.** **Priority moves up beside the Name**, the card's name is one line, and **Arrange moves into the node's own menu** with a **Free** option. **Links**: search + family chips, the relation's own note, jump-to-either-end, and a filter for a node with many relations |
| v0.16.0 | **Settings hold settings.** The **Structure** group is gone and its board-wide **commands** (Arrange the map, Collapse all, Expand all) move to the board's own menu, where a command belongs. What is left is four groups of remembered preferences — **Board · Modes · Links · App** — and a dead `show_origin` preference is removed with it |
| v0.17.0 | **Settings get their own pages** — an index of **Board · Modes · Links · Nodes · App**, one page each; the new **Nodes** page sets what a new node is born with. **Cancel now cancels** (the created-but-unnamed node is taken back, and Back does the same). **Child/Sibling are renamed SubNode/CoNode.** **Connect opens the same resizable board sheet as More.** **Notes get a text menu** (bold, italic, size, font) with a live preview. **Nine card shapes** and **seventeen colours**, including an RGB builder |

---

## Honest limits (v0.17.0)

- **Group** and **Schema** are still not built. Group is waiting on one answer (frame vs merge); Schema
  is deferred on Saeed's instruction.
- **Undo is memory-only**, bounded to the last 12 steps; closing the app clears it.
- **Z is reserved, not built**: every node is on layer 0 and Z only decides drawing order.
- **Arrange is a tree layout.** Dense graphs with many cross-links are tidied along their trees and
  the extra links stay where they are — there is no force-directed pass yet.
- **Arrange a branch is a command, not a stored property**, so the whole-map Arrange still lays the map
  out in the project direction.
- A branch that cannot find clear space after 40 steps (a very dense board) keeps its ideal position
  instead of moving further.
- **Arrange moves the map to the origin.** If you had deliberately parked a map elsewhere in board
  space, use Undo or drag it back.
- The **rubber band needs a long press** on empty board before dragging; nothing on screen says so yet
  except the Help entry.
- The **action box is a fixed size at every zoom**, on purpose: at 5 % it looks large next to a very
  small node. That is the trade for buttons that are always tappable.
- **Resize handles** are drawn on the card and can be partly covered by the bars when a node is very
  small; the corner handles stay reachable, and an invisible touch margin keeps them draggable.
- The **clipboard is the app's own** (nodes, not text) and lives in memory only.
- The **minimap is not interactive** (deliberate — ordered left alone).
- **No visual verification tooling**: Paparazzi is unproven on this toolchain, so the device install is
  the only visual check.
- Opening the v0.11.0+ build re-reads an existing board in the new unit (positions are board units, not
  raw pixels), so everything spreads out and stops overlapping. The stored numbers are not touched, and
  one *Arrange* re-tidies the map in the new space.

---

<sub>Built on a phone, with an agent, one version at a time. The full design contract — tokens,
 screens, every decision and its reason — is in [`DESIGN.md`](DESIGN.md).</sub>
