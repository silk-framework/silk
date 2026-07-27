import React from "react";
import { ValidIconName } from "@eccenca/gui-elements/src/components/atoms/Icon/canonicalIconNames";
import { GridLayout, GridPlacement, resolveCollisions } from "./gridEngine";
import { loadSavedLayout } from "./gridStorage";
import type { GridBoardItem } from "./GridBoard";

/** A single tile's viewport rectangle, captured for the minimize/restore fly animation. */
export interface MorphRect {
    top: number;
    left: number;
    width: number;
    height: number;
}

export interface Morph {
    id: string;
    icon?: ValidIconName;
    title?: string;
    from: MorphRect;
    to: MorphRect;
    /** `minimize`: grid slot → rail icon. `restore`: rail icon → grid slot. */
    dir: "minimize" | "restore";
}

const toMorphRect = (r: DOMRect): MorphRect => ({ top: r.top, left: r.left, width: r.width, height: r.height });

export interface UseMinimizeMorph {
    /** The in-flight fly animation, or `null`. */
    morph: Morph | null;
    setMorph: React.Dispatch<React.SetStateAction<Morph | null>>;
    /** Park a tile in the rail (fly its card-face grid slot → rail icon). */
    minimize: (id: string) => void;
    /** Bring a parked tile back (fly rail icon → the slot it will occupy). */
    restore: (id: string) => void;
}

/**
 * The minimize/restore flow and its FLIP "fly to/from the rail" morph. Owns the transient morph
 * state; drives layout + minimized-set changes through the setters it is given. The pixel geometry
 * comes in as `box` (grid-units → board-relative px) so this hook stays free of the board's cell
 * math.
 */
export function useMinimizeMorph({
    items,
    layout,
    setLayout,
    minimized,
    setMinimized,
    articleRefs,
    railRefs,
    boardRef,
    box,
    storageKey,
    closePreview,
}: {
    items: GridBoardItem[];
    layout: GridPlacement[];
    setLayout: React.Dispatch<React.SetStateAction<GridPlacement[]>>;
    minimized: string[];
    setMinimized: React.Dispatch<React.SetStateAction<string[]>>;
    articleRefs: React.MutableRefObject<Map<string, HTMLElement>>;
    railRefs: React.MutableRefObject<Map<string, HTMLElement>>;
    boardRef: React.MutableRefObject<HTMLDivElement | null>;
    box: (it: GridLayout) => { left: number; top: number; width: number; height: number };
    storageKey: string;
    closePreview: () => void;
}): UseMinimizeMorph {
    const pendingMorph = React.useRef<{ id: string; rect: MorphRect; dir: "minimize" | "restore" } | null>(null);
    const [morph, setMorph] = React.useState<Morph | null>(null);

    // Grid-unit placement (board-relative) → a viewport rect, matching getBoundingClientRect space.
    const placementToViewportRect = (pl: GridLayout): MorphRect | null => {
        const board = boardRef.current?.getBoundingClientRect();
        if (!board) return null;
        const bx = box(pl);
        return { top: board.top + bx.top, left: board.left + bx.left, width: bx.width, height: bx.height };
    };

    // Park a tile in the rail: fly the card-face from its grid slot to the rail icon. The rail icon
    // only mounts after this render, so its box is measured in the layout effect below.
    const minimize = (id: string) => {
        closePreview();
        const el = articleRefs.current.get(id);
        pendingMorph.current = el ? { id, rect: toMorphRect(el.getBoundingClientRect()), dir: "minimize" } : null;
        // Drop it out of the grid immediately — its slot stays empty (free placement, no reflow).
        setLayout((l) => l.filter((p) => p.id !== id));
        setMinimized((m) => (m.includes(id) ? m : [...m, id]));
    };

    // Bring a parked tile back: fly the card-face from the rail icon to the slot it will occupy. The
    // slot is computed from grid math (not the DOM) so the morph starts before the tile is painted,
    // keeping the real content hidden (opacity 0) until the ghost lands.
    const restore = (id: string) => {
        closePreview();
        const railEl = railRefs.current.get(id);
        const item = items.find((it) => it.id === id);
        // The restored tile wins its remembered slot (pinned — a squatter gets pushed down); a tile
        // without one (never persisted) takes its default slot the same way. Computed from the live
        // layout, not a rebuild, so the tiles already on the board keep their exact positions.
        const slot = loadSavedLayout(storageKey)[id] ?? item?.defaultLayout;
        const nextLayout = slot
            ? resolveCollisions([...layout.filter((p) => p.id !== id), { id, ...slot }], id)
            : layout;
        const pl = nextLayout.find((p) => p.id === id);
        const to = pl ? placementToViewportRect(pl) : null;
        if (railEl && to) {
            setMorph({
                id,
                icon: item?.icon,
                title: item?.title,
                from: toMorphRect(railEl.getBoundingClientRect()),
                to,
                dir: "restore",
            });
        }
        setLayout(nextLayout);
        setMinimized((m) => m.filter((x) => x !== id));
    };

    // Minimize's target (the rail icon) only exists after the state update, so measure it here.
    React.useLayoutEffect(() => {
        const p = pendingMorph.current;
        if (!p || p.dir !== "minimize") return;
        pendingMorph.current = null;
        const railEl = railRefs.current.get(p.id);
        if (!railEl) return; // rail icon missing (shouldn't happen) — skip the animation, not the action
        const item = items.find((it) => it.id === p.id);
        setMorph({
            id: p.id,
            icon: item?.icon,
            title: item?.title,
            from: p.rect,
            to: toMorphRect(railEl.getBoundingClientRect()),
            dir: "minimize",
        });
    }, [minimized, items]);

    return { morph, setMorph, minimize, restore };
}
