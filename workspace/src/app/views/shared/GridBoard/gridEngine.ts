/**
 * Layout engine — pure functions over `{ id, x, y, w, h }` in GRID units.
 *
 * This is the whole "library": nothing here knows about React or the DOM.
 * Adapted from the drag/drop/resize dashboard-grid prototype into TypeScript.
 */

/** Position + size of a single tile, expressed in grid units (columns / rows). */
export interface GridLayout {
    x: number;
    y: number;
    w: number;
    h: number;
}

/** A placed tile: its {@link GridLayout} plus the id it belongs to. */
export interface GridPlacement extends GridLayout {
    id: string;
}

const overlaps = (a: GridPlacement, b: GridPlacement): boolean =>
    a.id !== b.id && a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;

const hits = (placed: GridPlacement[], item: GridPlacement): GridPlacement | undefined =>
    placed.find((p) => overlaps(p, item));

/**
 * Push-down collision resolution — the only pass that ever moves a tile the user didn't touch.
 * Every tile keeps its exact `x`/`y` unless it overlaps the pinned tile (or a tile that was itself
 * pushed), in which case it moves straight down just enough to clear. Never up, never sideways, so
 * free placement (empty space left/right/above) survives. Non-pinned tiles are processed in reading
 * order (`y` then `x`), which makes cascades deterministic; `y` only increases, so it terminates.
 */
export function resolveCollisions(layout: GridPlacement[], pinnedId: string | null = null): GridPlacement[] {
    const pinned = layout.find((i) => i.id === pinnedId);
    const rest = layout.filter((i) => i.id !== pinnedId).sort((a, b) => a.y - b.y || a.x - b.x);

    const placed: GridPlacement[] = pinned ? [{ ...pinned }] : [];
    for (const item of rest) {
        const it = { ...item };
        let collision: GridPlacement | undefined;
        while ((collision = hits(placed, it))) it.y = collision.y + collision.h;
        placed.push(it);
    }
    return placed;
}

/** Move or resize one card; push down only what it now overlaps. Nothing else moves. */
export const apply = (layout: GridPlacement[], id: string, patch: Partial<GridLayout>): GridPlacement[] =>
    resolveCollisions(
        layout.map((i) => (i.id === id ? { ...i, ...patch } : i)),
        id,
    );

/** The first free top-left slot (scanning rows top→bottom, columns left→right) that fits `item`. */
function place(placed: GridPlacement[], item: GridPlacement, cols: number): GridPlacement {
    for (let y = 0; ; y += 1) {
        for (let x = 0; x <= cols - item.w; x += 1) {
            const candidate = { ...item, x, y };
            if (!hits(placed, candidate)) return candidate;
        }
    }
}

/**
 * Add a tile at its desired slot if that slot is free, otherwise at the first free top-left slot
 * that fits it. Used for tiles that have no persisted position (new widgets, reset defaults).
 * Corrupt input with non-finite dimensions would make `place` scan rows forever — coerce to a
 * minimal sane size instead. A tile wider than the grid is clamped so it can still be placed.
 */
export function insertTile(layout: GridPlacement[], item: GridPlacement, cols: number): GridPlacement[] {
    const w = Math.min(Number.isFinite(item.w) ? Math.max(1, item.w) : 1, cols);
    const h = Number.isFinite(item.h) ? Math.max(1, item.h) : 1;
    const x = Number.isFinite(item.x) ? clamp(item.x, 0, cols - w) : 0;
    const y = Number.isFinite(item.y) ? Math.max(0, item.y) : 0;
    const candidate = { ...item, x, y, w, h };
    if (!hits(layout, candidate)) return [...layout, candidate];
    return [...layout, place(layout, candidate, cols)];
}

export const clamp = (v: number, lo: number, hi: number): number => Math.min(hi, Math.max(lo, v));

/** Upper bound for coordinates/sizes — anything beyond this is corrupt, not a layout. */
export const MAX_GRID_UNITS = 500;

/**
 * Validate one persisted layout entry (untrusted `localStorage` content). Returns a sane
 * {@link GridLayout} — integer coordinates, `w`/`h` at least 1, everything clamped to
 * {@link MAX_GRID_UNITS} — or `null` when the entry is not an object with finite numeric
 * `x`/`y`/`w`/`h` (callers fall back to the tile's default layout).
 */
export function sanitizeSavedLayout(entry: unknown): GridLayout | null {
    if (typeof entry !== "object" || entry === null) return null;
    const { x, y, w, h } = entry as Record<string, unknown>;
    const isFinite = (v: unknown): v is number => typeof v === "number" && Number.isFinite(v);
    if (!isFinite(x) || !isFinite(y) || !isFinite(w) || !isFinite(h)) return null;
    return {
        x: clamp(Math.round(x), 0, MAX_GRID_UNITS),
        y: clamp(Math.round(y), 0, MAX_GRID_UNITS),
        w: clamp(Math.round(w), 1, MAX_GRID_UNITS),
        h: clamp(Math.round(h), 1, MAX_GRID_UNITS),
    };
}

/** Highest occupied row (`y + h`) across the layout — the number of rows the board needs. */
export const usedRows = (layout: GridPlacement[]): number => layout.reduce((m, i) => Math.max(m, i.y + i.h), 0);
