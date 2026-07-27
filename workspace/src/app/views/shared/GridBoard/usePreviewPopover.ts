import React from "react";
import { clamp } from "./gridEngine";
import { loadSavedLayout } from "./gridStorage";
import type { GridBoardItem } from "./GridBoard";

const DEFAULT_PREVIEW_W = 380;
const DEFAULT_PREVIEW_H = 300;
const ARROW = 12;

/** Precomputed, viewport-fixed geometry for the hover preview popover (and its pointer arrow). */
export interface PreviewGeom {
    /** `position:fixed` box for the floated article. */
    style: React.CSSProperties;
    /** Left edge of the small arrow square that points back at the rail icon. */
    arrowLeft: number;
    /** Top edge of that arrow square. */
    arrowTop: number;
}

export interface UsePreviewPopover {
    /** Id of the tile currently floated out as a preview, or `null`. */
    previewId: string | null;
    openPreview: (id: string) => void;
    scheduleClosePreview: () => void;
    closePreviewNow: () => void;
    /** Geometry for the open preview, or `null` when nothing is previewing. */
    previewGeom: PreviewGeom | null;
}

/**
 * The interactive hover preview: a minimized tile's (still-mounted) article floats out as a fixed
 * popover to the left of its rail icon. Owns the open/close timers (a short close delay bridges the
 * gap between icon and popover), a capture-phase scroll-close, and — crucially — the popover
 * geometry, which is measured ONCE per open in a layout effect rather than re-read from the DOM on
 * every render (the previous inline version called `getBoundingClientRect` during render).
 */
export function usePreviewPopover({
    items,
    railRefs,
    articleRefs,
    boardRef,
    storageKey,
    cellW,
    gap,
    rowHeight,
}: {
    items: GridBoardItem[];
    railRefs: React.MutableRefObject<Map<string, HTMLElement>>;
    articleRefs: React.MutableRefObject<Map<string, HTMLElement>>;
    boardRef: React.MutableRefObject<HTMLDivElement | null>;
    storageKey: string;
    cellW: number;
    gap: number;
    rowHeight: number;
}): UsePreviewPopover {
    const [previewId, setPreviewId] = React.useState<string | null>(null);
    const [previewGeom, setPreviewGeom] = React.useState<PreviewGeom | null>(null);
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

    // Measure the popover geometry once per open (and if the board metrics change while it is open),
    // never during render. The popover is sized to the widget's CURRENT dashboard footprint
    // (persisted layout incl. any user resize; falls back to defaultLayout), so it "keeps the size"
    // it had on the board; an explicit `previewSize` overrides. Clamped to the viewport and to the
    // space left of the rail; its top is clamped to the board top so it never rides over the header.
    React.useLayoutEffect(() => {
        if (!previewId) {
            setPreviewGeom(null);
            return;
        }
        const previewItem = items.find((it) => it.id === previewId);
        const railEl = previewItem ? railRefs.current.get(previewItem.id) : undefined;
        if (!previewItem || !railEl) {
            setPreviewGeom(null);
            return;
        }
        const railRect = railEl.getBoundingClientRect();
        const viewportH = typeof window !== "undefined" ? window.innerHeight : DEFAULT_PREVIEW_H + 16;
        const boardTop = boardRef.current?.getBoundingClientRect().top ?? 8;
        const remembered = loadSavedLayout(storageKey)[previewItem.id] ?? previewItem.defaultLayout;
        const naturalW = remembered && cellW > 0 ? remembered.w * cellW + (remembered.w - 1) * gap : DEFAULT_PREVIEW_W;
        const naturalH = remembered ? remembered.h * rowHeight + (remembered.h - 1) * gap : DEFAULT_PREVIEW_H;
        const maxW = Math.max(280, railRect.left - 16);
        const width = clamp(Math.round(previewItem.previewSize?.width ?? naturalW), 300, Math.min(760, maxW));
        const height = clamp(
            Math.round(previewItem.previewSize?.height ?? naturalH),
            200,
            Math.max(220, viewportH - 32),
        );
        const left = Math.max(8, railRect.left - ARROW - width);
        const top = Math.min(
            Math.max(boardTop, railRect.top + railRect.height / 2 - height / 2),
            viewportH - height - 8,
        );
        // Arrow tip Y: aim at the rail icon centre, kept within the popover's rounded body.
        const arrowTop = clamp(railRect.top + railRect.height / 2 - ARROW / 2, top + 12, top + height - 24);
        setPreviewGeom({
            style: { position: "fixed", width, height, left, top, zIndex: 60 },
            arrowLeft: left + width - ARROW / 2,
            arrowTop,
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [previewId, cellW, gap, rowHeight]);

    return { previewId, openPreview, scheduleClosePreview, closePreviewNow, previewGeom };
}
