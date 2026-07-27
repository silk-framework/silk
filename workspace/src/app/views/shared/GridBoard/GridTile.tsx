import React from "react";
import { Icon } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { clamp, GridLayout, GridPlacement } from "./gridEngine";
import { DragState } from "./useGridGestures";
import { GridTileIconProvider } from "./GridTileIcon";
import type { GridBoardItem } from "./GridBoard";

interface Box {
    left: number;
    top: number;
    width: number;
    height: number;
}

/**
 * Renders a tile's content and reports to the board whether it is currently empty (the widget
 * rendered nothing — e.g. the error log with no warnings). Kept mounted regardless of visibility so
 * the widget never re-fetches when it toggles between empty and non-empty. See the empty-tile
 * contract on {@link GridBoardItem.element}.
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
        // `childElementCount` alone misses a widget that renders only a text node (no elements): the
        // count is 0 yet the tile is not empty. Fall back to `textContent` so text-only content keeps
        // the tile in the grid.
        const check = () => onEmptyChange(id, el.childElementCount === 0 && !el.textContent?.trim());
        check();
        if (typeof MutationObserver === "undefined") return;
        // Watch text mutations too (characterData/subtree), so a widget swapping empty text for real
        // text — without adding/removing element nodes — still flips the tile back into the layout.
        const mo = new MutationObserver(check);
        mo.observe(el, { childList: true, characterData: true, subtree: true });
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

export interface GridTileProps {
    item: GridBoardItem;
    /** This tile's grid slot, or `undefined` when it is hidden (empty / minimized / previewing). */
    placement?: GridPlacement;
    /** True while this tile is the one being dragged/resized. */
    active: boolean;
    drag: DragState | null;
    /** grid units → board-relative px. */
    box: (it: GridLayout) => Box;
    cellW: number;
    cols: number;
    gap: number;
    rowHeight: number;
    /** True while a restore morph is flying back onto this tile (its real content stays invisible). */
    restoring: boolean;
    previewId: string | null;
    previewStyle: React.CSSProperties | null;
    onPointerDown: (e: React.PointerEvent, id: string, mode: "move" | "resize") => void;
    onMinimize: (id: string) => void;
    onPreviewOpen: (id: string) => void;
    onPreviewClose: () => void;
    onEmptyChange: (id: string, empty: boolean) => void;
    registerRef: (id: string, el: HTMLElement | null) => void;
}

/**
 * One board tile: a positioned `<article>` with hover-revealed drag / minimize / resize chrome and
 * the widget content inside. Every item renders (empty ones as `display:none`, minimized ones either
 * hidden or floated out as a fixed preview popover) so the widget stays mounted and never re-fetches.
 */
export function GridTile({
    item,
    placement,
    active,
    drag,
    box,
    cellW,
    cols,
    gap,
    rowHeight,
    restoring,
    previewId,
    previewStyle,
    onPointerDown,
    onMinimize,
    onPreviewOpen,
    onPreviewClose,
    onEmptyChange,
    registerRef,
}: GridTileProps) {
    const [t] = useTranslation();
    const b: Box = placement ? box(placement) : { left: 0, top: 0, width: 0, height: 0 };

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
            // Cap the live width at the board's right edge; `cellW + gap` is the board's `stepX`.
            b.width = clamp(o.width + drag.dx, cellW, (cols - drag.origin.x) * (cellW + gap) - gap);
            b.height = Math.max(rowHeight, o.height + drag.dy);
        }
    }

    // Hovered-in-rail: this (minimized) tile floats out as a fixed live preview.
    const previewing = !placement && item.id === previewId && previewStyle != null;

    return (
        <article
            ref={(el) => registerRef(item.id, el)}
            data-test-id={`grid-board-tile-${item.id}`}
            // While shown as a preview popover, keep it open as the pointer moves onto it, and close
            // (deferred) when the pointer leaves.
            onMouseEnter={previewing ? () => onPreviewOpen(item.id) : undefined}
            onMouseLeave={previewing ? onPreviewClose : undefined}
            style={
                placement
                    ? {
                          transform: `translate(${b.left}px, ${b.top}px)`,
                          width: b.width,
                          height: b.height,
                          opacity: restoring ? 0 : undefined,
                          transition: active ? "none" : "transform 160ms ease, width 160ms ease, height 160ms ease",
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
            {/* Chrome (drag / minimize / resize handles) — hidden while this tile is a preview popover,
                where dragging/resizing/minimizing make no sense. */}
            {!previewing && (
                <>
                    {/* Drag handle — a hover-revealed grip pill centred on the top edge. Pointer-only
                        (it has no keyboard behavior), so it is skipped in the tab order; it still shows
                        up alongside a focused minimize button. */}
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
                            onMinimize(item.id);
                        }}
                        className={
                            "absolute left-1/2 top-0 z-20 flex h-5 w-7 translate-x-[1.375rem] items-center " +
                            "justify-center rounded-b-md border border-t-0 border-border bg-card text-muted-foreground " +
                            "opacity-0 transition-opacity hover:bg-accent hover:text-foreground " +
                            "pointer-events-none group-hover:pointer-events-auto group-hover:opacity-100 " +
                            // Keyboard users tab straight onto this button — reveal it (and, via
                            // group-focus-within, the grip pill next to it) on focus.
                            "focus-visible:opacity-100 group-focus-within:opacity-100"
                        }
                    >
                        <Icon small name="toggler-minimize" />
                    </button>
                </>
            )}

            <div className="flex min-h-0 w-full flex-1 flex-col overflow-hidden">
                <TileContent id={item.id} onEmptyChange={onEmptyChange}>
                    {/* Expose this tile's icon to its subtree so each widget's header can show the same
                        icon it gets in the minimized rail (GridTileTitleIcon). The provider renders no
                        DOM, so TileContent's empty-detection is unaffected. */}
                    <GridTileIconProvider value={item.icon}>{item.element}</GridTileIconProvider>
                </TileContent>
            </div>

            {/* Resize handle — hover-revealed, bottom-right corner. Pointer-only and not focusable, so
                hide it from assistive technology entirely. */}
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
                        <path d="M16 6 L6 16 M16 11 L11 16" stroke="currentColor" strokeWidth="1.5" fill="none" />
                    </svg>
                </div>
            )}
        </article>
    );
}
