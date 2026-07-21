import React from "react";

interface GridBoardResetContextValue {
    /** True while a GridBoard is mounted and can be reset. */
    hasBoard: boolean;
    /** Reset the currently mounted board to its default layout (no-op when none is mounted). */
    reset: () => void;
    /** A GridBoard registers its reset handler here; returns an unregister function. */
    registerReset: (handler: () => void) => () => void;
}

const noop = () => {};

export const GridBoardResetContext = React.createContext<GridBoardResetContextValue>({
    hasBoard: false,
    reset: noop,
    registerReset: () => noop,
});

/**
 * Lets a page's {@link GridBoard} expose its "reset layout" action to global chrome (the Help menu),
 * so the control lives outside the board itself. Only one board is mounted per page.
 */
export function GridBoardResetProvider({ children }: { children: React.ReactNode }) {
    const handlerRef = React.useRef<(() => void) | null>(null);
    const [hasBoard, setHasBoard] = React.useState(false);

    const registerReset = React.useCallback((handler: () => void) => {
        handlerRef.current = handler;
        setHasBoard(true);
        return () => {
            if (handlerRef.current === handler) {
                handlerRef.current = null;
                setHasBoard(false);
            }
        };
    }, []);

    const reset = React.useCallback(() => handlerRef.current?.(), []);

    const value = React.useMemo(() => ({ hasBoard, reset, registerReset }), [hasBoard, reset, registerReset]);
    return <GridBoardResetContext.Provider value={value}>{children}</GridBoardResetContext.Provider>;
}

/** Registers a board's reset handler for the lifetime of the board (latest handler always used). */
export function useRegisterGridBoardReset(reset: () => void) {
    const { registerReset } = React.useContext(GridBoardResetContext);
    const resetRef = React.useRef(reset);
    resetRef.current = reset;
    React.useEffect(() => registerReset(() => resetRef.current()), [registerReset]);
}

/** Read side, e.g. for the Help menu: whether a board is mounted and how to reset it. */
export function useGridBoardReset() {
    return React.useContext(GridBoardResetContext);
}
