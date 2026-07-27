/**
 * localStorage persistence + layout assembly for {@link GridBoard} — the impure counterpart to the
 * pure `gridEngine.ts`. Everything here reads/writes `window.localStorage` (guarded, since it can be
 * unavailable or hold hand-edited / stale / corrupt content) and turns a saved arrangement into a
 * concrete {@link GridPlacement} list.
 */
import { GridLayout, GridPlacement, insertTile, resolveCollisions, sanitizeSavedLayout } from "./gridEngine";
// Type-only (erased at runtime), so this does not create an import cycle with GridBoard.tsx.
import type { GridBoardItem } from "./GridBoard";

export const STORAGE_PREFIX = "diApp.gridBoard.";

const layoutStorageKey = (storageKey: string): string => STORAGE_PREFIX + storageKey;
export const minimizedStorageKey = (storageKey: string): string => STORAGE_PREFIX + storageKey + ".minimized";

/**
 * Persisted-schema version. `v0` is the original bare format (a `Record<id, GridLayout>` for the
 * layout, a `string[]` for the minimized set) written before this envelope existed; it is accepted
 * on read and silently migrated to `v1` on the next write.
 */
const SCHEMA_VERSION = 1;

interface LayoutEnvelope {
    v: number;
    tiles: Record<string, unknown>;
}

interface MinimizedEnvelope {
    v: number;
    ids: unknown;
}

const isRecord = (v: unknown): v is Record<string, unknown> => typeof v === "object" && v !== null && !Array.isArray(v);

// Persisted layouts are untrusted (hand-edited / stale / corrupt localStorage): a non-object parse
// result counts as "no saved layout", and every entry is validated via `sanitizeSavedLayout` —
// invalid entries are dropped so the tile falls back to its default layout. Without this, a partial
// entry (e.g. missing `w`) reaches the engine as NaN (`insertTile` additionally coerces non-finite
// dimensions so its placement scan always terminates).
export const loadSavedLayout = (storageKey: string): Record<string, GridLayout> => {
    try {
        const raw = window.localStorage.getItem(layoutStorageKey(storageKey));
        const parsed: unknown = raw ? JSON.parse(raw) : null;
        if (!isRecord(parsed)) return {};
        // Accept both the versioned envelope ({v:1, tiles:{…}}) and the original bare v0 record
        // ({id: layout}). The `v`/`tiles` probe can't be fooled by a v0 tile literally named "v" or
        // "tiles" — a tile's value is a layout object, never the number `v` an envelope carries.
        const tiles = parsed.tiles;
        const source: Record<string, unknown> = typeof parsed.v === "number" && isRecord(tiles) ? tiles : parsed;
        const record: Record<string, GridLayout> = {};
        Object.entries(source).forEach(([id, entry]) => {
            const layout = sanitizeSavedLayout(entry);
            if (layout) record[id] = layout;
        });
        return record;
    } catch {
        return {};
    }
};

/**
 * Merge the current `layout` onto whatever is already stored (so hidden/minimized tiles keep their
 * remembered slot), prune any entry whose id is not in `keepIds` (a current item or a minimized
 * tile), and write it back as the versioned envelope. Pruning is the reason a write needs `keepIds`:
 * without it, ids for widgets that no longer exist accumulate in storage forever.
 */
export const persistLayout = (storageKey: string, layout: GridPlacement[], keepIds: ReadonlySet<string>): void => {
    try {
        const record = loadSavedLayout(storageKey);
        layout.forEach(({ id, x, y, w, h }) => {
            record[id] = { x, y, w, h };
        });
        const tiles: Record<string, GridLayout> = {};
        Object.entries(record).forEach(([id, entry]) => {
            if (keepIds.has(id)) tiles[id] = entry;
        });
        const envelope: LayoutEnvelope = { v: SCHEMA_VERSION, tiles };
        window.localStorage.setItem(layoutStorageKey(storageKey), JSON.stringify(envelope));
    } catch {
        /* storage unavailable — layout simply won't persist */
    }
};

// Untrusted like the layout above: a non-array (e.g. a literal "null") counts as nothing minimized,
// and non-string entries are dropped — either would otherwise crash the render.
export const loadMinimized = (storageKey: string): string[] => {
    try {
        const raw = window.localStorage.getItem(minimizedStorageKey(storageKey));
        const parsed: unknown = raw ? JSON.parse(raw) : null;
        // Accept both the versioned envelope ({v:1, ids:[…]}) and the original bare v0 array.
        const ids: unknown = Array.isArray(parsed)
            ? parsed
            : isRecord(parsed) && typeof parsed.v === "number"
              ? parsed.ids
              : null;
        return Array.isArray(ids) ? ids.filter((id): id is string => typeof id === "string") : [];
    } catch {
        return [];
    }
};

export const persistMinimized = (storageKey: string, minimized: string[]): void => {
    try {
        const envelope: MinimizedEnvelope = { v: SCHEMA_VERSION, ids: minimized };
        window.localStorage.setItem(minimizedStorageKey(storageKey), JSON.stringify(envelope));
    } catch {
        /* storage unavailable — minimized set simply won't persist */
    }
};

/** Drop this board's persisted layout + minimized set entirely (used by "reset layout"). */
export const clearStorage = (storageKey: string): void => {
    try {
        window.localStorage.removeItem(layoutStorageKey(storageKey));
        window.localStorage.removeItem(minimizedStorageKey(storageKey));
    } catch {
        /* ignore */
    }
};

/**
 * Free placement: a tile with a saved slot returns to it verbatim (pinned, pushing any squatter
 * down); a tile without one (new widget / reset) takes its default slot, or the first free slot if
 * that is occupied. Nothing is ever compacted — holes in a saved arrangement are kept as the user
 * left them.
 *
 * Order-dependent by design: tiles are folded in `items` order and each saved tile is pinned as it
 * is added, so when two saved tiles claim overlapping slots the LATER item in `items` wins the slot
 * and the earlier one is pushed down (the final pin resolves against the already-placed earlier tile).
 */
export const buildLayout = (items: GridBoardItem[], saved: Record<string, GridLayout>, cols: number): GridPlacement[] =>
    items.reduce<GridPlacement[]>(
        (layout, it) =>
            saved[it.id]
                ? resolveCollisions([...layout, { id: it.id, ...saved[it.id] }], it.id)
                : insertTile(layout, { id: it.id, ...it.defaultLayout }, cols),
        [],
    );
