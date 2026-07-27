import React from "react";
import { Icon } from "@eccenca/gui-elements";
import { ValidIconName } from "@eccenca/gui-elements/src/components/atoms/Icon/canonicalIconNames";
import { AnimatePresence, motion } from "framer-motion";
import { GridLayout, usedRows } from "./gridEngine";
import {
    buildLayout,
    clearStorage,
    loadMinimized,
    loadSavedLayout,
    persistLayout,
    persistMinimized,
} from "./gridStorage";
import { useGridGestures } from "./useGridGestures";
import { useMinimizeMorph } from "./useMinimizeMorph";
import { usePreviewPopover } from "./usePreviewPopover";
import { useRegisterGridBoardReset } from "./GridBoardResetContext";
import { GridBoardMinimizedRail } from "./GridBoardMinimizedRail";
import { GridTile } from "./GridTile";

/** A single tile: its default position/size and the content to render inside it. */
export interface GridBoardItem {
    /** Stable id — also the localStorage key for this tile's persisted position. */
    id: string;
    /** Position + size (grid units) used when there is no persisted layout. */
    defaultLayout: GridLayout;
    /**
     * Rendered inside the tile. Self-carding widgets are passed through as-is.
     *
     * Empty-tile contract: a tile counts as empty — it collapses out of the layout (but stays
     * mounted, so it never re-fetches) — when its content wrapper has NO element children AND no
     * non-whitespace text. A widget that renders `null` / nothing therefore drops out; one that
     * renders even a bare text node, or an (otherwise empty) element, stays in the grid.
     */
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

/**
 * A drag-to-move / drag-to-resize tile board. The whole grid is defined by three
 * numbers ({@link GridBoardProps.cols cols} / {@link GridBoardProps.rowHeight rowHeight} /
 * {@link GridBoardProps.gap gap}); the pure layout math lives in `gridEngine.ts`, its localStorage
 * persistence in `gridStorage.ts`, and the interaction logic in the `useGridGestures` /
 * `useMinimizeMorph` / `usePreviewPopover` hooks.
 *
 * Positions are persisted to `localStorage` per `storageKey`, and a "reset layout"
 * control restores the default arrangement. Tiles whose content renders nothing collapse
 * out of the layout automatically (but stay mounted).
 */
export function GridBoard({ items, storageKey, cols = 12, rowHeight = 44, gap = 12, className }: GridBoardProps) {
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
    React.useEffect(() => persistMinimized(storageKey, minimized), [minimized, storageKey]);

    const visibleItems = React.useMemo(
        () => items.filter((it) => !emptyIds.has(it.id) && !minimizedSet.has(it.id)),
        [items, emptyIds, minimizedSet],
    );

    const [layout, setLayout] = React.useState(() => buildLayout(visibleItems, loadSavedLayout(storageKey), cols));
    const boardRef = React.useRef<HTMLDivElement | null>(null);
    const [boardW, setBoardW] = React.useState(0);
    // Refs driving the fly-to-rail / fly-back morph and the hover preview (shared across hooks + render).
    const articleRefs = React.useRef(new Map<string, HTMLElement>());
    const railRefs = React.useRef(new Map<string, HTMLElement>());

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

    const { drag, onPointerDown, onPointerMove, onPointerUp } = useGridGestures({
        layout,
        setLayout,
        cols,
        stepX,
        stepY,
    });

    const { previewId, openPreview, scheduleClosePreview, closePreviewNow, previewGeom } = usePreviewPopover({
        items,
        railRefs,
        articleRefs,
        boardRef,
        storageKey,
        cellW,
        gap,
        rowHeight,
    });

    const { morph, setMorph, minimize, restore } = useMinimizeMorph({
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
        closePreview: closePreviewNow,
    });

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

    // The set of ids a persisted layout is allowed to keep: current items ∪ minimized tiles. Stored
    // entries outside this set are orphans (widgets that no longer exist) and get pruned on write.
    const allIdKey = items.map((it) => it.id).join("|");
    const keepIds = React.useMemo(() => {
        const s = new Set(items.map((it) => it.id));
        minimized.forEach((id) => s.add(id));
        return s;
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [allIdKey, minimized]);

    // Persist once a gesture has ended (never mid-drag). Merges onto existing storage (so hidden tiles
    // keep their remembered position) and prunes orphans — see `persistLayout`.
    React.useEffect(() => {
        if (drag) return;
        persistLayout(storageKey, layout, keepIds);
    }, [layout, drag, storageKey, keepIds]);

    const boardH = (usedRows(layout) + (drag ? 2 : 0)) * stepY - gap;

    const resetLayout = () => {
        clearStorage(storageKey);
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

    const previewStyle = previewGeom?.style ?? null;

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
                    {items.map((item) => (
                        <GridTile
                            key={item.id}
                            item={item}
                            placement={placements.get(item.id)}
                            active={drag?.id === item.id}
                            drag={drag}
                            box={box}
                            cellW={cellW}
                            cols={cols}
                            gap={gap}
                            rowHeight={rowHeight}
                            // While a restore morph is flying back, keep the tile's real content invisible
                            // until the ghost lands on it, so the card appears to grow out of the rail icon.
                            restoring={morph?.dir === "restore" && morph.id === item.id}
                            previewId={previewId}
                            previewStyle={previewStyle}
                            onPointerDown={onPointerDown}
                            onMinimize={minimize}
                            onPreviewOpen={openPreview}
                            onPreviewClose={scheduleClosePreview}
                            onEmptyChange={handleEmptyChange}
                            registerRef={(id, el) => {
                                if (el) articleRefs.current.set(id, el);
                                else articleRefs.current.delete(id);
                            }}
                        />
                    ))}
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
                {previewGeom && (
                    <div
                        aria-hidden
                        className="fixed h-3 w-3 rotate-45 border-r border-t border-border bg-card"
                        style={{ left: previewGeom.arrowLeft, top: previewGeom.arrowTop, zIndex: 59 }}
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
