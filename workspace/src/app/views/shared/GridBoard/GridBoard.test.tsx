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

    it("persists the minimized set to localStorage", () => {
        const { getByTestId } = render(<GridBoard items={items} storageKey="persist" />);
        fireEvent.click(getByTestId("grid-board-minimize-beta"));
        expect(JSON.parse(window.localStorage.getItem("diApp.gridBoard.persist.minimized") ?? "[]")).toEqual(["beta"]);
    });
});
