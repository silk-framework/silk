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

/** Float an item up until it bumps into something, then drop it just below. */
function settle(placed: GridPlacement[], item: GridPlacement): GridPlacement {
    const it = { ...item };
    while (it.y > 0 && !hits(placed, it)) it.y -= 1;
    let collision: GridPlacement | undefined;
    while ((collision = hits(placed, it))) it.y = collision.y + collision.h;
    return it;
}

/**
 * Gravity pass. `pinnedId` (the card under the cursor) keeps its exact slot;
 * everything else settles around it, topmost first.
 */
export function compact(layout: GridPlacement[], pinnedId: string | null = null): GridPlacement[] {
    const pinned = layout.find((i) => i.id === pinnedId);
    const rest = layout.filter((i) => i.id !== pinnedId).sort((a, b) => a.y - b.y || a.x - b.x);

    const placed: GridPlacement[] = pinned ? [{ ...pinned }] : [];
    for (const item of rest) placed.push(settle(placed, item));
    return placed;
}

/** Move or resize one card, then let gravity resolve the consequences. */
export const apply = (layout: GridPlacement[], id: string, patch: Partial<GridLayout>): GridPlacement[] =>
    compact(
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
 * Reflow that also closes HORIZONTAL gaps. Unlike {@link compact} (which only floats each tile
 * straight up in its own column), this packs every tile into the first free top-left slot that fits
 * its size — so a hole left by a removed/minimized tile is reclaimed by whichever remaining tile fits
 * there, moving up *and* sideways. Tiles keep their `w`/`h`; only `x`/`y` change. Placement order is
 * the tiles' current reading order (`y` then `x`), which keeps the arrangement roughly stable.
 */
export function repack(layout: GridPlacement[], cols: number): GridPlacement[] {
    const order = [...layout].sort((a, b) => a.y - b.y || a.x - b.x);
    const placed: GridPlacement[] = [];
    for (const item of order) {
        // Corrupt input (e.g. a malformed persisted layout) with non-finite dimensions would make
        // `place` scan rows forever — coerce to a minimal sane size instead. A tile wider than the
        // grid is clamped so it can still be placed.
        const w = Math.min(Number.isFinite(item.w) ? Math.max(1, item.w) : 1, cols);
        const h = Number.isFinite(item.h) ? Math.max(1, item.h) : 1;
        placed.push(place(placed, { ...item, w, h }, cols));
    }
    return placed;
}

export const clamp = (v: number, lo: number, hi: number): number => Math.min(hi, Math.max(lo, v));

/** Upper bound for persisted coordinates/sizes — anything beyond this is corrupt, not a layout. */
const MAX_GRID_UNITS = 500;

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
