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

## 6. Screen inventory — v0.1.0 ships the home screen only

| Screen | Default | Empty | Loading | Error | Long content |
|---|---|---|---|---|---|
| Home | ✅ | ✅ (recent list empty) | n/a — no async data source yet | n/a — no async data source yet | ✅ scrolls, labels ellipsised |
| Logs (long-press the title) | ✅ | ✅ ("No log lines yet.") | n/a | ✅ banner if the log file could not be opened | ✅ scrolls |

Honest note: loading/error states for Home arrive with the first module that reads data
(Project / Knowledge base). They are not faked now.

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

- Implemented: 2026-10-15
- Verified how: build + install by Saeed (device loop). Paparazzi screenshot module not yet set up.
- Rounds of self-critique: 1 (structure/token pass against the checklist above).
- Open visual issues: none known; first real judgement belongs to Saeed on the device.