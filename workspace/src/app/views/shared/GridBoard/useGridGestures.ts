import React from "react";
import { apply, clamp, GridLayout, GridPlacement, MAX_GRID_UNITS } from "./gridEngine";

/** Live drag/resize state, consumed by the render to float the active tile with the cursor. */
export interface DragState {
    id: string;
    mode: "move" | "resize";
    origin: GridPlacement;
    dx: number;
    dy: number;
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

export interface UseGridGestures {
    /** The in-flight gesture, or `null` when idle. */
    drag: DragState | null;
    onPointerDown: (e: React.PointerEvent, id: string, mode: "move" | "resize") => void;
    onPointerMove: (e: React.PointerEvent) => void;
    onPointerUp: () => void;
}

/**
 * Pointer-driven move/resize for the board. Snaps the pointer delta to whole cells and re-resolves
 * the layout from the gesture-start snapshot each move (pinning the dragged tile), so pushed-aside
 * tiles spring back as the drag moves on. Persistence is intentionally NOT done here — the caller
 * writes the layout once `drag` returns to `null`.
 */
export function useGridGestures({
    layout,
    setLayout,
    cols,
    stepX,
    stepY,
}: {
    layout: GridPlacement[];
    setLayout: React.Dispatch<React.SetStateAction<GridPlacement[]>>;
    cols: number;
    stepX: number;
    stepY: number;
}): UseGridGestures {
    const [drag, setDrag] = React.useState<DragState | null>(null);
    const gesture = React.useRef<Gesture | null>(null);

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

    return { drag, onPointerDown, onPointerMove, onPointerUp };
}
