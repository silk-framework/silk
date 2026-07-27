import React from "react";
import { configure, fireEvent, render } from "@testing-library/react";
import { GridBoard, GridBoardItem } from "./GridBoard";

// The codebase tags elements with `data-test-id` (hyphenated), not RTL's default `data-testid`.
configure({ testIdAttribute: "data-test-id" });

// Render framer-motion as plain, synchronous elements: strip the animation-only props and let
// AnimatePresence/LayoutGroup pass children straight through, so minimize/restore are deterministic
// in jsdom (no pending enter/exit animations holding elements in the tree).
jest.mock("framer-motion", () => {
    const react = require("react");
    const strip = (props: Record<string, unknown>) => {
        const { initial, animate, exit, transition, layout, onAnimationComplete, ...rest } = props;
        return rest;
    };
    const motion = new Proxy(
        {},
        {
            get: (_target, tag: string) =>
                // eslint-disable-next-line react/display-name
                react.forwardRef((props: Record<string, unknown>, ref: unknown) =>
                    react.createElement(tag, { ref, ...strip(props) }),
                ),
        },
    );
    return {
        __esModule: true,
        motion,
        AnimatePresence: ({ children }: { children: React.ReactNode }) =>
            react.createElement(react.Fragment, null, children),
        LayoutGroup: ({ children }: { children: React.ReactNode }) =>
            react.createElement(react.Fragment, null, children),
    };
});

const items: GridBoardItem[] = [
    {
        id: "alpha",
        title: "Alpha",
        icon: "item-info",
        defaultLayout: { x: 0, y: 0, w: 6, h: 3 },
        element: <div data-test-id="alpha-body">Alpha body</div>,
    },
    {
        id: "beta",
        title: "Beta",
        icon: "item-settings",
        defaultLayout: { x: 6, y: 0, w: 6, h: 3 },
        element: <div data-test-id="beta-body">Beta body</div>,
    },
];

describe("GridBoard minimize / restore", () => {
    beforeEach(() => window.localStorage.clear());

    it("parks a tile in the rail on minimize and brings it back on restore", () => {
        const { queryByTestId, getByTestId } = render(<GridBoard items={items} storageKey="test" />);

        // Both tiles start in the grid, none in the rail.
        expect(getByTestId("grid-board-tile-alpha")).toBeInTheDocument();
        expect(queryByTestId("grid-board-minimized-rail")).not.toBeInTheDocument();

        // Minimize alpha → a restore button for it shows up in the rail.
        fireEvent.click(getByTestId("grid-board-minimize-alpha"));
        expect(getByTestId("grid-board-minimized-rail")).toBeInTheDocument();
        expect(getByTestId("grid-board-restore-alpha")).toBeInTheDocument();
        // Its content stays mounted (no re-fetch), just hidden from the grid.
        expect(getByTestId("alpha-body")).toBeInTheDocument();

        // Restore alpha → the rail empties again.
        fireEvent.click(getByTestId("grid-board-restore-alpha"));
        expect(queryByTestId("grid-board-restore-alpha")).not.toBeInTheDocument();
        expect(queryByTestId("grid-board-minimized-rail")).not.toBeInTheDocument();
    });

    it("floats the minimized widget out as a live preview on rail hover", () => {
        const { getByTestId } = render(<GridBoard items={items} storageKey="preview" />);
        fireEvent.click(getByTestId("grid-board-minimize-alpha"));

        // Parked: the tile is hidden from the grid.
        const tile = getByTestId("grid-board-tile-alpha");
        expect(tile.style.display).toBe("none");

        // Hovering the rail icon lifts the same (still-mounted) article into a fixed preview.
        const railSpan = getByTestId("grid-board-restore-alpha").parentElement as HTMLElement;
        fireEvent.mouseEnter(railSpan);
        expect(tile.style.display).not.toBe("none");
        expect(tile.style.position).toBe("fixed");
        expect(getByTestId("alpha-body")).toBeInTheDocument(); // same element, no remount
    });

    it("survives a malformed persisted layout by falling back to default layouts", () => {
        // A partial entry (missing w/h) used to hang the render (NaN placement scan); a literal
        // "null" used to throw. Both must simply be ignored in favor of the defaults.
        window.localStorage.setItem("diApp.gridBoard.corrupt", JSON.stringify({ alpha: { x: 0 }, beta: null }));
        const { getByTestId } = render(<GridBoard items={items} storageKey="corrupt" />);
        expect(getByTestId("grid-board-tile-alpha")).toBeInTheDocument();
        expect(getByTestId("grid-board-tile-beta")).toBeInTheDocument();

        window.localStorage.setItem("diApp.gridBoard.corrupt2", JSON.stringify("null"));
        expect(() => render(<GridBoard items={items} storageKey="corrupt2" />)).not.toThrow();
    });

    it("survives a malformed persisted minimized set", () => {
        // A literal "null" (non-array) counts as nothing minimized.
        window.localStorage.setItem("diApp.gridBoard.badrail.minimized", "null");
        const first = render(<GridBoard items={items} storageKey="badrail" />);
        expect(first.getByTestId("grid-board-tile-alpha")).toBeInTheDocument();
        expect(first.queryByTestId("grid-board-minimized-rail")).not.toBeInTheDocument();
        first.unmount();

        // Non-string entries are dropped; only the valid id is parked in the rail.
        window.localStorage.setItem("diApp.gridBoard.badrail2.minimized", JSON.stringify([1, "beta"]));
        const second = render(<GridBoard items={items} storageKey="badrail2" />);
        expect(second.getByTestId("grid-board-restore-beta")).toBeInTheDocument();
        expect(second.getByTestId("grid-board-tile-alpha")).toBeInTheDocument();
    });

    it("persists the minimized set to localStorage as a versioned envelope", () => {
        const { getByTestId } = render(<GridBoard items={items} storageKey="persist" />);
        fireEvent.click(getByTestId("grid-board-minimize-beta"));
        const stored = JSON.parse(window.localStorage.getItem("diApp.gridBoard.persist.minimized") ?? "{}");
        expect(stored).toEqual({ v: 1, ids: ["beta"] });
    });
});

describe("GridBoard storage envelope", () => {
    beforeEach(() => window.localStorage.clear());

    it("migrates a bare v0 layout to the versioned envelope, preserving positions", () => {
        // Original (pre-envelope) format: a bare Record<id, GridLayout>.
        window.localStorage.setItem(
            "diApp.gridBoard.legacy",
            JSON.stringify({ alpha: { x: 2, y: 1, w: 6, h: 3 }, beta: { x: 8, y: 0, w: 4, h: 2 } }),
        );
        render(<GridBoard items={items} storageKey="legacy" />);
        // The mount-time persist rewrites it as { v: 1, tiles: {…} } without losing the saved slots.
        const stored = JSON.parse(window.localStorage.getItem("diApp.gridBoard.legacy") ?? "{}");
        expect(stored.v).toBe(1);
        expect(stored.tiles.alpha).toMatchObject({ x: 2, y: 1, w: 6, h: 3 });
        expect(stored.tiles.beta).toMatchObject({ x: 8, y: 0, w: 4, h: 2 });
    });

    it("accepts a bare v0 minimized array and re-persists it as an envelope", () => {
        window.localStorage.setItem("diApp.gridBoard.legacymin.minimized", JSON.stringify(["beta"]));
        const { getByTestId } = render(<GridBoard items={items} storageKey="legacymin" />);
        // The v0 array is honored: beta starts parked in the rail.
        expect(getByTestId("grid-board-restore-beta")).toBeInTheDocument();
        const stored = JSON.parse(window.localStorage.getItem("diApp.gridBoard.legacymin.minimized") ?? "{}");
        expect(stored).toEqual({ v: 1, ids: ["beta"] });
    });

    it("prunes orphaned layout entries not backed by a current item or minimized tile", () => {
        window.localStorage.setItem(
            "diApp.gridBoard.orphan",
            JSON.stringify({
                v: 1,
                tiles: {
                    alpha: { x: 0, y: 0, w: 6, h: 3 },
                    beta: { x: 6, y: 0, w: 6, h: 3 },
                    ghost: { x: 0, y: 3, w: 6, h: 3 }, // no such item — must not survive a write
                },
            }),
        );
        render(<GridBoard items={items} storageKey="orphan" />);
        const stored = JSON.parse(window.localStorage.getItem("diApp.gridBoard.orphan") ?? "{}");
        expect(stored.tiles.alpha).toBeDefined();
        expect(stored.tiles.beta).toBeDefined();
        expect(stored.tiles.ghost).toBeUndefined();
    });
});

// Free-placement drag behavior. These tests need real cell geometry: with no ResizeObserver the
// board falls back to `clientWidth`, which we stub so that a 12-column board (gap 12) gets an
// 88px cell — stepX = 100px, stepY = 56px (rowHeight 44) — keeping expected positions round.
describe("GridBoard free placement", () => {
    const BOARD_W = 1188; // 12 * 88 + 11 * 12
    const STEP_X = 100;
    const STEP_Y = 56;

    const savedResizeObserver = (global as { ResizeObserver?: unknown }).ResizeObserver;

    beforeEach(() => {
        window.localStorage.clear();
        delete (global as { ResizeObserver?: unknown }).ResizeObserver;
        Object.defineProperty(HTMLElement.prototype, "clientWidth", {
            configurable: true,
            get: () => BOARD_W,
        });
    });

    afterEach(() => {
        (global as { ResizeObserver?: unknown }).ResizeObserver = savedResizeObserver;
        delete (HTMLElement.prototype as unknown as Record<string, unknown>).clientWidth;
    });

    const boardOf = (tile: HTMLElement): HTMLElement => tile.parentElement as HTMLElement;
    const translate = (x: number, y: number) => `translate(${x * STEP_X}px, ${y * STEP_Y}px)`;
    // The layout is persisted as a versioned envelope { v, tiles }; unwrap `tiles` for the assertions.
    const savedLayout = (storageKey: string) =>
        JSON.parse(window.localStorage.getItem(`diApp.gridBoard.${storageKey}`) ?? '{"tiles":{}}').tiles ?? {};

    it("keeps a tile exactly where it is dropped — no compaction toward the top-left", () => {
        const { getByTestId, getByTitle } = render(<GridBoard items={items} storageKey="free" />);
        const board = boardOf(getByTestId("grid-board-tile-alpha"));

        // Drag alpha 4 columns right and 5 rows down: free space remains on its left AND above.
        fireEvent.pointerDown(getByTitle("Alpha"), { clientX: 0, clientY: 0 });
        fireEvent.pointerMove(board, { clientX: 4 * STEP_X, clientY: 5 * STEP_Y });
        fireEvent.pointerUp(board);

        expect(savedLayout("free").alpha).toEqual({ x: 4, y: 5, w: 6, h: 3 });
        expect(savedLayout("free").beta).toEqual({ x: 6, y: 0, w: 6, h: 3 });
        expect(getByTestId("grid-board-tile-alpha").style.transform).toBe(translate(4, 5));
        expect(getByTestId("grid-board-tile-beta").style.transform).toBe(translate(6, 0));
    });

    it("pushes an overlapped tile down and lets it spring back when the drag moves away", () => {
        const { getByTestId, getByTitle } = render(<GridBoard items={items} storageKey="push" />);
        const board = boardOf(getByTestId("grid-board-tile-alpha"));
        const beta = getByTestId("grid-board-tile-beta");

        // Drag alpha onto beta's slot → beta is pushed straight down below it.
        fireEvent.pointerDown(getByTitle("Alpha"), { clientX: 0, clientY: 0 });
        fireEvent.pointerMove(board, { clientX: 6 * STEP_X, clientY: 0 });
        expect(beta.style.transform).toBe(translate(6, 3));

        // Same gesture, drag away again → beta returns to its original slot (snapshot resolution).
        fireEvent.pointerMove(board, { clientX: 0, clientY: 5 * STEP_Y });
        expect(beta.style.transform).toBe(translate(6, 0));

        fireEvent.pointerUp(board);
        expect(savedLayout("push").alpha).toEqual({ x: 0, y: 5, w: 6, h: 3 });
        expect(savedLayout("push").beta).toEqual({ x: 6, y: 0, w: 6, h: 3 });
    });

    it("leaves the hole of a minimized tile instead of reflowing the rest", () => {
        window.localStorage.setItem(
            "diApp.gridBoard.hole",
            JSON.stringify({ alpha: { x: 0, y: 0, w: 6, h: 3 }, beta: { x: 0, y: 3, w: 6, h: 3 } }),
        );
        const { getByTestId } = render(<GridBoard items={items} storageKey="hole" />);

        fireEvent.click(getByTestId("grid-board-minimize-alpha"));

        // beta stays below the (now empty) alpha slot — nothing moves up.
        expect(getByTestId("grid-board-tile-beta").style.transform).toBe(translate(0, 3));
        expect(savedLayout("hole").beta).toEqual({ x: 0, y: 3, w: 6, h: 3 });
        expect(savedLayout("hole").alpha).toEqual({ x: 0, y: 0, w: 6, h: 3 }); // slot remembered
    });

    it("restores a tile to its remembered slot, pushing a squatter down", () => {
        window.localStorage.setItem(
            "diApp.gridBoard.squat",
            JSON.stringify({ alpha: { x: 0, y: 0, w: 6, h: 3 }, beta: { x: 0, y: 0, w: 6, h: 3 } }),
        );
        window.localStorage.setItem("diApp.gridBoard.squat.minimized", JSON.stringify(["alpha"]));
        const { getByTestId } = render(<GridBoard items={items} storageKey="squat" />);

        // beta occupies alpha's remembered slot while alpha is parked.
        expect(getByTestId("grid-board-tile-beta").style.transform).toBe(translate(0, 0));

        fireEvent.click(getByTestId("grid-board-restore-alpha"));
        expect(getByTestId("grid-board-tile-alpha").style.transform).toBe(translate(0, 0));
        expect(getByTestId("grid-board-tile-beta").style.transform).toBe(translate(0, 3));
    });
});
