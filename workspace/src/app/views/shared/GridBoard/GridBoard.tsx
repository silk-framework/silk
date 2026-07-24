import React from "react";
import { Icon } from "@eccenca/gui-elements";
import { ValidIconName } from "@eccenca/gui-elements/src/components/atoms/Icon/canonicalIconNames";
import { useTranslation } from "react-i18next";
import { AnimatePresence, motion } from "framer-motion";
import {
    apply,
    clamp,
    GridLayout,
    GridPlacement,
    insertTile,
    MAX_GRID_UNITS,
    resolveCollisions,
    sanitizeSavedLayout,
    usedRows,
} from "./gridEngine";
import { useRegisterGridBoardReset } from "./GridBoardResetContext";
import { GridBoardMinimizedRail } from "./GridBoardMinimizedRail";
import { GridTileIconProvider } from "./GridTileIcon";

/** A single tile: its default position/size and the content to render inside it. */
export interface GridBoardItem {
    /** Stable id — also the localStorage key for this tile's persisted position. */
    id: string;
    /** Position + size (grid units) used when there is no persisted layout. */
    defaultLayout: GridLayout;
    /** Rendered inside the tile. Self-carding widgets are passed through as-is. */
    element: React.ReactNode;
    /** Accessible label for the drag handle, and the name shown when the tile is minimized. */
    title?: string;
    /** Canonical gui-elements icon name shown on the minimize button and in the minimized rail. */
    icon?: ValidIconName;
    /**
     * Explicit size (px) for this tile's hover preview popover. When omitted the popover is sized
     * from the tile's default grid footprint, so widgets that are wide/tall in the grid also get a
     * wider/taller preview. Clamped to the viewport and the space left of the rail.
     */
    previewSize?: { width?: number; height?: number };
}

export interface GridBoardProps {
    /** Tiles to lay out. The set of ids may change (conditional widgets); positions persist per id. */
    items: GridBoardItem[];
    /** Distinguishes the persisted layout of one page type from another (localStorage suffix). */
    storageKey: string;
    /** Number of columns. Default 12. */
    cols?: number;
    /** Height of one row unit, in px. Default 44. */
    rowHeight?: number;
    /** Gap between cells, in px. Default 12. */
    gap?: number;
    className?: string;
}

const STORAGE_PREFIX = "diApp.gridBoard.";

// Persisted layouts are untrusted (hand-edited / stale / corrupt localStorage): a non-object parse
// result counts as "no saved layout", and every entry is validated via `sanitizeSavedLayout` —
// invalid entries are dropped so the tile falls back to its default layout. Without this, a partial
// entry (e.g. missing `w`) reaches the engine as NaN (`insertTile` additionally coerces non-finite
// dimensions so its placement scan always terminates).
const loadSavedLayout = (storageKey: string): Record<string, GridLayout> => {
    try {
        const raw = window.localStorage.getItem(STORAGE_PREFIX + storageKey);
        const parsed: unknown = raw ? JSON.parse(raw) : null;
        if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) return {};
        const record: Record<string, GridLayout> = {};
        Object.entries(parsed).forEach(([id, entry]) => {
            const layout = sanitizeSavedLayout(entry);
            if (layout) record[id] = layout;
        });
        return record;
    } catch {
        return {};
    }
};

// Free placement: a tile with a saved slot returns to it verbatim (pinned, pushing any squatter
// down); a tile without one (new widget / reset) takes its default slot, or the first free slot if
// that is occupied. Nothing is ever compacted — holes in a saved arrangement are kept as the user
// left them.
const buildLayout = (items: GridBoardItem[], saved: Record<string, GridLayout>, cols: number): GridPlacement[] =>
    items.reduce<GridPlacement[]>(
        (layout, it) =>
            saved[it.id]
                ? resolveCollisions([...layout, { id: it.id, ...saved[it.id] }], it.id)
                : insertTile(layout, { id: it.id, ...it.defaultLayout }, cols),
        [],
    );

const minimizedStorageKey = (storageKey: string): string => STORAGE_PREFIX + storageKey + ".minimized";

// Untrusted like the layout above: a non-array (e.g. a literal "null") counts as nothing minimized,
// and non-string entries are dropped — either would otherwise crash the render.
const loadMinimized = (storageKey: string): string[] => {
    try {
        const raw = window.localStorage.getItem(minimizedStorageKey(storageKey));
        const parsed: unknown = raw ? JSON.parse(raw) : null;
        return Array.isArray(parsed) ? parsed.filter((id): id is string => typeof id === "string") : [];
    } catch {
        return [];
    }
};

/** A single tile's viewport rectangle, captured for the minimize/restore fly animation. */
interface MorphRect {
    top: number;
    left: number;
    width: number;
    height: number;
}

const toMorphRect = (r: DOMRect): MorphRect => ({ top: r.top, left: r.left, width: r.width, height: r.height });

interface Morph {
    id: string;
    icon?: ValidIconName;
    title?: string;
    from: MorphRect;
    to: MorphRect;
    /** `minimize`: grid slot → rail icon. `restore`: rail icon → grid slot. */
    dir: "minimize" | "restore";
}

interface Gesture {
    id: string;
    mode: "move" | "resize";
    origin: GridPlacement;
    px: number;
    py: number;
    /**
     * Layout snapshot at gesture start. Every pointer-move resolves collisions from this snapshot
     * (not the previous frame), so a tile pushed aside springs back the moment the drag moves away.
     */
    startLayout: GridPlacement[];
    /** Last applied target — skips redundant layout updates while the pointer stays in a cell. */
    last: GridLayout;
}

interface DragState {
    id: string;
    mode: "move" | "resize";
    origin: GridPlacement;
    dx: number;
    dy: number;
}

/**
 * Renders a tile's content and reports to the board whether it is currently empty (the widget
 * rendered nothing — e.g. the error log with no warnings). Kept mounted regardless of visibility so
 * the widget never re-fetches when it toggles between empty and non-empty.
 */
function TileContent({
    id,
    onEmptyChange,
    children,
}: {
    id: string;
    onEmptyChange: (id: string, empty: boolean) => void;
    children: React.ReactNode;
}) {
    const ref = React.useRef<HTMLDivElement | null>(null);
    React.useLayoutEffect(() => {
        const el = ref.current;
        if (!el) return;
        const check = () => onEmptyChange(id, el.childElementCount === 0);
        check();
        if (typeof MutationObserver === "undefined") return;
        const mo = new MutationObserver(check);
        mo.observe(el, { childList: true });
        return () => mo.disconnect();
    }, [id, onEmptyChange]);
    // Grow to fill the tile via flexbox (percentage heights don't resolve through this flex chain).
    // The card-fill for self-carding widgets is done with a flat CSS rule keyed on `eccapp-gridboard-tile`
    // (see theme/gridboard.css): a Tailwind `[&>.eccgui-card]` arbitrary variant does not apply here, and
    // some tile cards come from plugins whose markup we cannot edit, so a selector-based rule is required.
    // GridTileCard-wrapped items grow via their own `flex-1` root and are unaffected.
    return (
        <div ref={ref} className="eccapp-gridboard-tile flex min-h-0 w-full flex-1 flex-col">
            {children}
        </div>
    );
}

/**
 * A drag-to-move / drag-to-resize tile board. The whole grid is defined by three
 * numbers ({@link GridBoardProps.cols cols} / {@link GridBoardProps.rowHeight rowHeight} /
 * {@link GridBoardProps.gap gap}); the pure layout math lives in `gridEngine.ts`.
 *
 * Positions are persisted to `localStorage` per `storageKey`, and a "reset layout"
 * control restores the default arrangement. Tiles whose content renders nothing collapse
 * out of the layout automatically (but stay mounted).
 */
export function GridBoard({ items, storageKey, cols = 12, rowHeight = 44, gap = 12, className }: GridBoardProps) {
    const [t] = useTranslation();
    const [emptyIds, setEmptyIds] = React.useState<ReadonlySet<string>>(() => new Set());
    const handleEmptyChange = React.useCallback((id: string, empty: boolean) => {
        setEmptyIds((prev) => {
            if (empty === prev.has(id)) return prev;
            const next = new Set(prev);
            if (empty) next.add(id);
            else next.delete(id);
            return next;
        });
    }, []);

    // Minimized tiles: parked in the right rail, in the order they were minimized. Excluded from the
    // grid layout (like empty tiles) but their `<article>` stays mounted, so widgets never re-fetch
    // or lose in-progress state (e.g. an open editor) while parked.
    const [minimized, setMinimized] = React.useState<string[]>(() => loadMinimized(storageKey));
    const minimizedSet = React.useMemo(() => new Set(minimized), [minimized]);
    React.useEffect(() => {
        try {
            window.localStorage.setItem(minimizedStorageKey(storageKey), JSON.stringify(minimized));
        } catch {
            /* storage unavailable — minimized set simply won't persist */
        }
    }, [minimized, storageKey]);

    const visibleItems = React.useMemo(
        () => items.filter((it) => !emptyIds.has(it.id) && !minimizedSet.has(it.id)),
        [items, emptyIds, minimizedSet],
    );

    const [layout, setLayout] = React.useState<GridPlacement[]>(() =>
        buildLayout(visibleItems, loadSavedLayout(storageKey), cols),
    );
    const [drag, setDrag] = React.useState<DragState | null>(null);
    const gesture = React.useRef<Gesture | null>(null);
    const boardRef = React.useRef<HTMLDivElement | null>(null);
    const [boardW, setBoardW] = React.useState(0);

    // Refs + transient state driving the fly-to-rail / fly-back morph.
    const articleRefs = React.useRef(new Map<string, HTMLElement>());
    const railRefs = React.useRef(new Map<string, HTMLElement>());
    const pendingMorph = React.useRef<{ id: string; rect: MorphRect; dir: "minimize" | "restore" } | null>(null);
    const [morph, setMorph] = React.useState<Morph | null>(null);
    // Rail icon currently hovered/pinned: its (still-mounted) widget floats out as an interactive
    // preview popover. A short close delay bridges the gap between the icon and the popover so the
    // pointer can travel onto the popover without it vanishing.
    const [previewId, setPreviewId] = React.useState<string | null>(null);
    const previewCloseTimer = React.useRef<ReturnType<typeof setTimeout> | null>(null);
    const cancelPreviewClose = () => {
        if (previewCloseTimer.current) {
            clearTimeout(previewCloseTimer.current);
            previewCloseTimer.current = null;
        }
    };
    const openPreview = (id: string) => {
        cancelPreviewClose();
        setPreviewId(id);
    };
    const scheduleClosePreview = () => {
        cancelPreviewClose();
        previewCloseTimer.current = setTimeout(() => setPreviewId(null), 140);
    };
    const closePreviewNow = () => {
        cancelPreviewClose();
        setPreviewId(null);
    };
    React.useEffect(() => cancelPreviewClose, []);
    // The popover is position:fixed and measured once when it opens, so any scroll outside it would
    // leave it floating over unrelated content (mouseleave never fires while the pointer rests on
    // it). Close it instead of trying to re-position — capture phase so nested scrollers count too.
    React.useEffect(() => {
        if (!previewId) return;
        const onScroll = (e: Event) => {
            // Scrolling the live widget inside the popover doesn't move the popover — keep it open.
            const el = articleRefs.current.get(previewId);
            if (el && e.target instanceof Node && el.contains(e.target)) return;
            closePreviewNow();
        };
        window.addEventListener("scroll", onScroll, true);
        return () => window.removeEventListener("scroll", onScroll, true);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [previewId]);

    const placements = React.useMemo(() => new Map(layout.map((p) => [p.id, p])), [layout]);
    // Re-derive the layout whenever the set of visible tiles changes (a conditional / empty widget
    // appeared or vanished). Bail when the layout already covers exactly the visible ids: minimize/
    // restore set their layout synchronously (with the restored tile pinned to its remembered slot),
    // and a rebuild from storage here would re-resolve that arrangement without the pin.
    const idKey = visibleItems.map((it) => it.id).join("|");
    React.useEffect(() => {
        setLayout((prev) => {
            const prevIds = new Set(prev.map((p) => p.id));
            const ids = visibleItems.map((it) => it.id);
            if (ids.length === prevIds.size && ids.every((id) => prevIds.has(id))) return prev;
            return buildLayout(visibleItems, loadSavedLayout(storageKey), cols);
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [idKey, storageKey, cols]);

    // Persist once a gesture has ended (never mid-drag). Merge onto existing storage so hidden tiles
    // keep their remembered position.
    React.useEffect(() => {
        if (drag) return;
        try {
            const record = loadSavedLayout(storageKey);
            layout.forEach(({ id, x, y, w, h }) => {
                record[id] = { x, y, w, h };
            });
            window.localStorage.setItem(STORAGE_PREFIX + storageKey, JSON.stringify(record));
        } catch {
            /* storage unavailable — layout simply won't persist */
        }
    }, [layout, drag, storageKey]);

    // The one measurement everything else derives from.
    React.useEffect(() => {
        const el = boardRef.current;
        if (!el || typeof ResizeObserver === "undefined") {
            if (el) setBoardW(el.clientWidth);
            return;
        }
        const ro = new ResizeObserver(([e]) => setBoardW(e.contentRect.width));
        ro.observe(el);
        return () => ro.disconnect();
    }, []);

    const cellW = boardW ? (boardW - gap * (cols - 1)) / cols : 0;
    const stepX = cellW + gap;
    const stepY = rowHeight + gap;

    // grid units -> pixels
    const box = (it: GridLayout) => ({
        left: it.x * stepX,
        top: it.y * stepY,
        width: it.w * cellW + (it.w - 1) * gap,
        height: it.h * rowHeight + (it.h - 1) * gap,
    });

    const boardH = (usedRows(layout) + (drag ? 2 : 0)) * stepY - gap;

    /* ---------------- pointer gestures ---------------- */

    const onPointerDown = (e: React.PointerEvent, id: string, mode: "move" | "resize") => {
        if (e.pointerType === "mouse" && e.button !== 0) return;
        e.preventDefault();
        e.currentTarget.setPointerCapture?.(e.pointerId);
        const origin = layout.find((i) => i.id === id);
        if (!origin) return;
        gesture.current = { id, mode, origin, px: e.clientX, py: e.clientY, startLayout: layout, last: origin };
        setDrag({ id, mode, origin, dx: 0, dy: 0 });
    };

    const onPointerMove = (e: React.PointerEvent) => {
        const g = gesture.current;
        if (!g) return;
        const dx = e.clientX - g.px;
        const dy = e.clientY - g.py;
        setDrag((d) => (d ? { ...d, dx, dy } : d));

        // Snap the pointer delta to whole cells and resolve from the gesture-start snapshot, so
        // tiles pushed down on an earlier frame return to their slot once the drag moves on.
        if (g.mode === "move") {
            const x = clamp(Math.round(g.origin.x + dx / stepX), 0, cols - g.origin.w);
            const y = clamp(Math.round(g.origin.y + dy / stepY), 0, MAX_GRID_UNITS - g.origin.h);
            if (x === g.last.x && y === g.last.y) return;
            g.last = { ...g.last, x, y };
            setLayout(apply(g.startLayout, g.id, { x, y }));
        } else {
            const w = clamp(Math.round(g.origin.w + dx / stepX), 1, cols - g.origin.x);
            const h = clamp(Math.round(g.origin.h + dy / stepY), 1, MAX_GRID_UNITS - g.origin.y);
            if (w === g.last.w && h === g.last.h) return;
            g.last = { ...g.last, w, h };
            setLayout(apply(g.startLayout, g.id, { w, h }));
        }
    };

    const onPointerUp = () => {
        if (!gesture.current) return;
        gesture.current = null;
        setDrag(null); // the layout stays exactly as dropped; persistence fires now that drag is null
    };

    /* ---------------- minimize / restore ---------------- */

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
        closePreviewNow();
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
        closePreviewNow();
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

    const resetLayout = () => {
        try {
            window.localStorage.removeItem(STORAGE_PREFIX + storageKey);
            window.localStorage.removeItem(minimizedStorageKey(storageKey));
        } catch {
            /* ignore */
        }
        setMinimized([]);
        setMorph(null);
        // Empty saved record → every tile takes its authored default slot verbatim.
        setLayout(
            buildLayout(
                items.filter((it) => !emptyIds.has(it.id)),
                {},
                cols,
            ),
        );
    };

    // Expose the reset action to global chrome (the Help menu) instead of an in-board button.
    useRegisterGridBoardReset(resetLayout);

    /* ---------------- render ---------------- */

    const railItems = minimized
        .map((id) => items.find((it) => it.id === id))
        .filter((it): it is GridBoardItem => Boolean(it))
        .map((it) => ({ id: it.id, title: it.title, icon: it.icon }));

    // Interactive preview: float the hovered rail tile's (still-mounted) article out as a fixed
    // popover to the left of its rail icon. Same element, so no remount/re-fetch — just repositioned,
    // and fully interactive (scroll/click the live widget). The top is clamped to the board's top so
    // the popover never rides up over the app header; a small arrow points back at the rail icon.
    const DEFAULT_PREVIEW_W = 380;
    const DEFAULT_PREVIEW_H = 300;
    const ARROW = 12;
    const previewItem = previewId ? items.find((it) => it.id === previewId) : undefined;
    const previewRect = previewItem ? railRefs.current.get(previewItem.id)?.getBoundingClientRect() : undefined;
    const viewportH = typeof window !== "undefined" ? window.innerHeight : DEFAULT_PREVIEW_H + 16;
    const boardTop = boardRef.current?.getBoundingClientRect().top ?? 8;
    // Size the popover to the widget's CURRENT dashboard footprint (persisted layout incl. any user
    // resize; falls back to defaultLayout), so it "keeps the size" it had on the board. An explicit
    // `previewSize` overrides. Clamped to the viewport and to the space left of the rail.
    const remembered = previewItem
        ? (loadSavedLayout(storageKey)[previewItem.id] ?? previewItem.defaultLayout)
        : undefined;
    const naturalW = remembered && cellW > 0 ? remembered.w * cellW + (remembered.w - 1) * gap : DEFAULT_PREVIEW_W;
    const naturalH = remembered ? remembered.h * rowHeight + (remembered.h - 1) * gap : DEFAULT_PREVIEW_H;
    const maxW = previewRect ? Math.max(280, previewRect.left - 16) : 760;
    const previewW = previewRect
        ? clamp(Math.round(previewItem?.previewSize?.width ?? naturalW), 300, Math.min(760, maxW))
        : DEFAULT_PREVIEW_W;
    const previewH = previewRect
        ? clamp(Math.round(previewItem?.previewSize?.height ?? naturalH), 200, Math.max(220, viewportH - 32))
        : DEFAULT_PREVIEW_H;
    const previewLeft = previewRect ? Math.max(8, previewRect.left - ARROW - previewW) : 0;
    const previewTop = previewRect
        ? Math.min(
              Math.max(boardTop, previewRect.top + previewRect.height / 2 - previewH / 2),
              viewportH - previewH - 8,
          )
        : 0;
    const previewStyle: React.CSSProperties | null =
        previewId && previewRect
            ? { position: "fixed", width: previewW, height: previewH, left: previewLeft, top: previewTop, zIndex: 60 }
            : null;
    // Arrow tip Y: aim at the rail icon centre, kept within the popover's rounded body.
    const arrowTop = previewRect
        ? clamp(previewRect.top + previewRect.height / 2 - ARROW / 2, previewTop + 12, previewTop + previewH - 24)
        : 0;

    return (
        <div className={className} data-test-id="grid-board">
            <div className="flex items-start gap-3">
                <div
                    ref={boardRef}
                    className={`relative min-w-0 flex-1${drag ? " select-none" : ""}`}
                    style={{ height: Math.max(boardH, 0) }}
                    onPointerMove={onPointerMove}
                    onPointerUp={onPointerUp}
                    onPointerCancel={onPointerUp}
                >
                    {/* Column guides — only while a gesture is live. */}
                    {drag && cellW > 0 && (
                        <div className="pointer-events-none absolute inset-0">
                            {Array.from({ length: cols }, (_, i) => (
                                <div
                                    key={i}
                                    className="absolute top-0 h-full rounded-sm bg-muted/50"
                                    style={{ left: i * stepX, width: cellW }}
                                />
                            ))}
                        </div>
                    )}

                    {/* Placeholder: where the dragged card will actually land. */}
                    {drag && placements.get(drag.id) && (
                        <div
                            className="pointer-events-none absolute rounded-lg border border-dashed border-primary/60 bg-primary/5"
                            style={box(placements.get(drag.id) ?? drag.origin)}
                        />
                    )}

                    {/* Every item renders in a stable slot; empty ones are display:none (kept mounted so the
                    widget keeps observing its own content and never re-fetches). */}
                    {items.map((item) => {
                        const placement = placements.get(item.id);
                        const active = drag?.id === item.id;
                        const b = placement ? box(placement) : { left: 0, top: 0, width: 0, height: 0 };

                        // The active card follows the cursor freely; the rest sit on the grid.
                        if (active && drag && placement) {
                            const o = box(drag.origin);
                            if (drag.mode === "move") {
                                b.left = o.left + drag.dx;
                                b.top = o.top + drag.dy;
                                b.width = o.width;
                                b.height = o.height;
                            } else {
                                b.left = o.left;
                                b.top = o.top;
                                b.width = clamp(o.width + drag.dx, cellW, (cols - drag.origin.x) * stepX - gap);
                                b.height = Math.max(rowHeight, o.height + drag.dy);
                            }
                        }

                        // While a restore morph is flying back, keep this tile's real content invisible
                        // until the ghost lands on it, so the card appears to grow out of the rail icon.
                        const restoring = morph?.dir === "restore" && morph.id === item.id;
                        // Hovered-in-rail: this (minimized) tile floats out as a fixed live preview.
                        const previewing = !placement && item.id === previewId && previewStyle != null;

                        return (
                            <article
                                key={item.id}
                                ref={(el) => {
                                    if (el) articleRefs.current.set(item.id, el);
                                    else articleRefs.current.delete(item.id);
                                }}
                                data-test-id={`grid-board-tile-${item.id}`}
                                // While shown as a preview popover, keep it open as the pointer moves onto
                                // it, and close (deferred) when the pointer leaves.
                                onMouseEnter={previewing ? () => openPreview(item.id) : undefined}
                                onMouseLeave={previewing ? scheduleClosePreview : undefined}
                                style={
                                    placement
                                        ? {
                                              transform: `translate(${b.left}px, ${b.top}px)`,
                                              width: b.width,
                                              height: b.height,
                                              opacity: restoring ? 0 : undefined,
                                              transition: active
                                                  ? "none"
                                                  : "transform 160ms ease, width 160ms ease, height 160ms ease",
                                          }
                                        : previewing
                                          ? (previewStyle as React.CSSProperties)
                                          : { display: "none" }
                                }
                                className={
                                    "group flex flex-col overflow-hidden rounded-lg " +
                                    (previewing ? "bg-card shadow-2xl ring-1 ring-border " : "absolute left-0 top-0 ") +
                                    (active ? "z-10 shadow-lg" : "")
                                }
                            >
                                {/* Chrome (drag / minimize / resize handles) — hidden while this tile is a
                                preview popover, where dragging/resizing/minimizing make no sense. */}
                                {!previewing && (
                                    <>
                                        {/* Drag handle — a hover-revealed grip pill centred on the top edge.
                                Pointer-only (it has no keyboard behavior), so it is skipped in the
                                tab order; it still shows up alongside a focused minimize button. */}
                                        <button
                                            type="button"
                                            tabIndex={-1}
                                            aria-label={t("GridBoard.move", "Move card")}
                                            title={item.title ?? item.id}
                                            onPointerDown={(e) => onPointerDown(e, item.id, "move")}
                                            className={
                                                "absolute left-1/2 top-0 z-20 flex h-5 w-10 -translate-x-1/2 touch-none items-center " +
                                                "justify-center rounded-b-md border border-t-0 border-border bg-card text-muted-foreground " +
                                                "opacity-0 transition-opacity hover:bg-accent group-hover:pointer-events-auto group-hover:opacity-100 " +
                                                "group-focus-within:opacity-100 " +
                                                (active
                                                    ? "pointer-events-auto cursor-grabbing opacity-100"
                                                    : "pointer-events-none cursor-grab")
                                            }
                                        >
                                            <Icon small name="item-draggable" />
                                        </button>

                                        {/* Minimize — sits just right of the grip; parks the tile in the rail.
                                stopPropagation so it never starts a drag/resize gesture. */}
                                        <button
                                            type="button"
                                            aria-label={t("GridBoard.minimize", "Minimize card")}
                                            title={t("GridBoard.minimize", "Minimize card")}
                                            data-test-id={`grid-board-minimize-${item.id}`}
                                            onPointerDown={(e) => e.stopPropagation()}
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                minimize(item.id);
                                            }}
                                            className={
                                                "absolute left-1/2 top-0 z-20 flex h-5 w-7 translate-x-[1.375rem] items-center " +
                                                "justify-center rounded-b-md border border-t-0 border-border bg-card text-muted-foreground " +
                                                "opacity-0 transition-opacity hover:bg-accent hover:text-foreground " +
                                                "pointer-events-none group-hover:pointer-events-auto group-hover:opacity-100 " +
                                                // Keyboard users tab straight onto this button — reveal it (and,
                                                // via group-focus-within, the grip pill next to it) on focus.
                                                "focus-visible:opacity-100 group-focus-within:opacity-100"
                                            }
                                        >
                                            <Icon small name="toggler-minimize" />
                                        </button>
                                    </>
                                )}

                                <div className="flex min-h-0 w-full flex-1 flex-col overflow-hidden">
                                    <TileContent id={item.id} onEmptyChange={handleEmptyChange}>
                                        {/* Expose this tile's icon to its subtree so each widget's header can show
                                        the same icon it gets in the minimized rail (GridTileTitleIcon). The
                                        provider renders no DOM, so TileContent's empty-detection is unaffected. */}
                                        <GridTileIconProvider value={item.icon}>{item.element}</GridTileIconProvider>
                                    </TileContent>
                                </div>

                                {/* Resize handle — hover-revealed, bottom-right corner. Pointer-only and
                                not focusable, so hide it from assistive technology entirely. */}
                                {!previewing && (
                                    <div
                                        aria-hidden="true"
                                        onPointerDown={(e) => onPointerDown(e, item.id, "resize")}
                                        className={
                                            "absolute bottom-0 right-0 z-20 h-4 w-4 touch-none cursor-se-resize text-muted-foreground " +
                                            "opacity-0 transition-opacity group-hover:pointer-events-auto group-hover:opacity-100 " +
                                            (active ? "pointer-events-auto opacity-100" : "pointer-events-none")
                                        }
                                    >
                                        <svg viewBox="0 0 16 16" className="h-full w-full">
                                            <path
                                                d="M16 6 L6 16 M16 11 L11 16"
                                                stroke="currentColor"
                                                strokeWidth="1.5"
                                                fill="none"
                                            />
                                        </svg>
                                    </div>
                                )}
                            </article>
                        );
                    })}
                </div>

                <GridBoardMinimizedRail
                    items={railItems}
                    onRestore={restore}
                    registerRef={(id, el) => {
                        if (el) railRefs.current.set(id, el);
                        else railRefs.current.delete(id);
                    }}
                    hiddenId={morph?.dir === "minimize" ? morph.id : null}
                    onPreview={(id) => (id ? openPreview(id) : scheduleClosePreview())}
                />

                {/* Preview arrow — a small rotated square at the popover's right edge pointing at the
                    rail icon (tooltip-style). Rendered separately so the popover's overflow:hidden
                    doesn't clip it. */}
                {previewStyle && previewRect && (
                    <div
                        aria-hidden
                        className="fixed h-3 w-3 rotate-45 border-r border-t border-border bg-card"
                        style={{ left: previewLeft + previewW - ARROW / 2, top: arrowTop, zIndex: 59 }}
                        onMouseEnter={() => previewId && openPreview(previewId)}
                        onMouseLeave={scheduleClosePreview}
                    />
                )}
            </div>

            {/* Flying ghost: a card-face that morphs between the tile's grid slot and its rail icon.
                Fixed-position over the viewport so `from`/`to` (getBoundingClientRect) map directly.
                Content stays mounted in the grid; only this lightweight face animates. */}
            <AnimatePresence>
                {morph && (
                    <motion.div
                        key={morph.id + morph.dir}
                        aria-hidden
                        data-test-id="grid-board-morph-ghost"
                        className="pointer-events-none fixed z-50 flex items-center gap-2 overflow-hidden rounded-lg border border-border bg-card px-3 text-muted-foreground shadow-lg"
                        initial={{
                            top: morph.from.top,
                            left: morph.from.left,
                            width: morph.from.width,
                            height: morph.from.height,
                            opacity: morph.dir === "minimize" ? 1 : 0.6,
                        }}
                        animate={{
                            top: morph.to.top,
                            left: morph.to.left,
                            width: morph.to.width,
                            height: morph.to.height,
                            opacity: morph.dir === "minimize" ? 0.6 : 1,
                        }}
                        transition={{ type: "spring", stiffness: 380, damping: 34 }}
                        onAnimationComplete={() => setMorph(null)}
                    >
                        <Icon name={morph.icon ?? "item-viewdetails"} />
                        <span className="truncate text-sm font-medium text-foreground">{morph.title}</span>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}

export default GridBoard;
