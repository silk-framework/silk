import React from "react";
import { IconButton } from "@eccenca/gui-elements";
import { ValidIconName } from "@eccenca/gui-elements/src/components/atoms/Icon/canonicalIconNames";
import { AnimatePresence, motion } from "framer-motion";
import { useTranslation } from "react-i18next";

/** One parked tile in the rail: enough to render its icon and restore it. */
export interface MinimizedRailItem {
    id: string;
    /** Full widget name — shown as the icon's tooltip. */
    title?: React.ReactNode;
    /** Canonical gui-elements icon name; falls back to a generic glyph. */
    icon?: ValidIconName;
}

interface GridBoardMinimizedRailProps {
    /** Parked tiles, in the order they were minimized. */
    items: MinimizedRailItem[];
    /** Restore a tile back into the grid. */
    onRestore: (id: string) => void;
    /** Registers each icon's DOM node so the board can measure it for the fly-in/out morph. */
    registerRef: (id: string, el: HTMLElement | null) => void;
    /** Icon currently mid-morph — hidden until the flying ghost lands on it. */
    hiddenId?: string | null;
    /** Hovered icon (or null on leave) — the board shows a live preview of that widget. */
    onPreview?: (id: string | null) => void;
}

/**
 * The vertical rail of minimized {@link GridBoard} tiles, pinned to the right of the board. Each
 * parked tile shows only its icon (full name on hover) and restores on click. Icons pop in/out with
 * a spring; the travel between grid slot and rail is animated by the board's flying ghost.
 */
export function GridBoardMinimizedRail({
    items,
    onRestore,
    registerRef,
    hiddenId,
    onPreview,
}: GridBoardMinimizedRailProps) {
    const [t] = useTranslation();
    if (items.length === 0) return null;
    return (
        <div
            data-test-id="grid-board-minimized-rail"
            className="sticky top-2 flex h-fit w-12 shrink-0 flex-col items-center gap-2 self-start rounded-lg border border-border bg-card/60 p-2"
        >
            <AnimatePresence initial={false}>
                {items.map((it) => (
                    <motion.span
                        key={it.id}
                        ref={(el) => registerRef(it.id, el)}
                        layout
                        initial={{ scale: 0, opacity: 0 }}
                        animate={{ scale: 1, opacity: hiddenId === it.id ? 0 : 1 }}
                        exit={{ scale: 0, opacity: 0 }}
                        transition={{ type: "spring", stiffness: 500, damping: 30 }}
                        className="block"
                        onMouseEnter={() => onPreview?.(it.id)}
                        onMouseLeave={() => onPreview?.(null)}
                    >
                        <IconButton
                            name={it.icon ?? "item-viewdetails"}
                            // Native title only (tooltipAsTitle) — the live preview replaces the tooltip.
                            text={typeof it.title === "string" ? it.title : t("GridBoard.restore", "Restore card")}
                            tooltipAsTitle
                            data-test-id={`grid-board-restore-${it.id}`}
                            onClick={() => onRestore(it.id)}
                        />
                    </motion.span>
                ))}
            </AnimatePresence>
        </div>
    );
}

export default GridBoardMinimizedRail;
