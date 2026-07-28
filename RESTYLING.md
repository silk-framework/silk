# Workbench restyling — what changed & why (silk scope)

Scope: the silk-side changes of the `experimental/restyling` branch — the workspace app shell,
pages, and new backend endpoints. This is the repo-scoped summary; the full cross-repo record
— including the complete decision log with verbatim quotes — lives in the DI superproject on
the same branch: `RESTYLING_CHANGES.md` and `RESTYLING_DECISIONS.md` at the `data-integration`
repo root. The design-system side is documented in `libs/gui-elements/RESTYLING.md` and its
`CHANGELOG.md`.

## App shell: sidebar + slim header

**What:** the old tall header (nav menu, logo, Create, notifications, sliding user panel)
split into a collapsible left sidebar (brand tile, project tile, project-scoped
Tasks/Activities, Datasets, user profile tile with language + appearance switcher in the
footer) and a slim sticky single-row header (sidebar toggle, breadcrumbs, quick-search box,
split Create button, Help menu, notifications bell).

**Why:** two reference designs were adopted deliberately — the shadcn *sidebar-07* block for
the shell mechanics and the "Workbench Chrome 2A" design for the look. The recorded scope
decision was **"visual restyle, real IA"**: take the design's styling but do not invent IA
elements the app doesn't have (no PINNED list, no per-type counts, no live badges). The
sidebar converged on **plain shadcn defaults** after several custom-styled iterations were
rejected; the project tile ended as a simple link to the projects page (after popup-switcher
and inline-switcher iterations); the sidebar remembers its state and defaults to closed
(screen space favors content). The chrome accent is the eccenca brand orange (#f29100,
`--brand` token) — an explicit "match the design source" decision; `--primary` stays blue.

**Header stability principle:** the header is global chrome and must never carry
page-dependent content — header icon buttons are a closed set, and page-scoped actions live in
an "Actions" tile in the page body (see the sourced best-practice rationale in the DI-root
`RESTYLING_CHANGES.md` §5). The quick search became a real command palette (recents before
typing, grouped results, hybrid ~50-item preload + debounced server search — parity with the
search page) because the old header "search" was only a button opening the recently-viewed
modal.

## GridBoard tile dashboards (detail pages)

**What:** Project, Transform, Linking, Dataset, Task, and Workflow pages dropped the fixed
two-column layout for a 12-column board of draggable, resizable, minimizable tiles with
per-page-type localStorage persistence and free tile placement.

**Why:** the fixed layout was "very inflexible", and the earlier attempts to relocate the
right-hand meta-widget column (drawer, tabs) only moved the problem. The grid engine is a
deliberately dependency-free script (no layout library); recorded decisions made the *whole
page* tiles on *all* detail pages at once. Auto-compaction was later **removed** in favor of
fully free placement with push-down collisions because the layout engine kept fighting user
intent (tiles must stay where dropped) — recorded as the new default everywhere, not a
per-user toggle. Widgets show their icon in front of their title so minimized rail entries
stay recognizable.

## /workbench list page

**What:** left filter sidebar replaced by a toolbar (compact search, filter dropdowns with
item-type icons, standalone sort button, table/grid toggle); default view is a real columnar
table with visible row actions; the Project page's "Contents" tile reuses the same list.

**Why:** the sidebar navigation and `/workbench?itemType=…` offered the same filtering twice —
one surface had to win. Table is the default with actions visible because the old card list
hid actions behind icon buttons. There is no date column because the search-result items carry
no created/modified fields. The two surfaces keep **per-surface** viewMode/pageSize keys so
toggling grid inside a project no longer silently changes /workbench.

## Mapping Creator V2 (silk-side)

The MC2 editor itself lives in workspacePlugins (DI repo), but silk carries its embedding
(mapping-creator tab on the transform page, no feature flag) and the debug endpoints backing
it: peak with pagination/search, `peakSourcePath`, and the Turtle output endpoint for selected
rules (pagination contract: one page = one input record including its connected subgraph).
The transform peek keeps validation-error rows (keep-and-flag via an optional `error` field)
instead of silently dropping records — aligned with the evaluated endpoint's existing
behavior.

## Other functional changes

- Workflow editor: sidebar cards wrap all tags at one font size (no truncation), clean drag
  ghost (Chromium setDragImage workaround); the pre-refactoring branch was the behavioral
  reference.
- Rule editor: the operator filter is now wrapping labeled FilterChips (icon-only squares
  proved unfixably fragile with up to 7 filters in a ~280px column).
- Create dialog: intent-dependent — a concrete type from the split button opens the form
  directly; the generic Create path shows the clickable type card first (develop behavior).
- Editor tab bar: segmented control with URL↔tab syncing; dark mode: appearance switcher in
  the sidebar user menu.

## Toolchain

React 19 across the workspace (required by the vendored shadcn/ui registry code in
gui-elements); Tailwind v4 CLI runs as a sidecar beside the webpack 4 build; framer-motion
(tile minimize morph) and cmdk (quick-search palette) added.

## Guiding principles (condensed)

Behavior parity against a named reference (develop branch / old commits) before improvement;
plain shadcn defaults over custom decoration; one design system everywhere (consistent icon
sizes, spacing, typography, badges); stable global chrome; root-cause fixes over patching;
the running app — not green gates — is the final judge.
