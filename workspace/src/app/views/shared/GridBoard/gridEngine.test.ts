import {
    apply,
    clamp,
    GridPlacement,
    insertTile,
    resolveCollisions,
    sanitizeSavedLayout,
    usedRows,
} from "./gridEngine";
import { buildLayout } from "./gridStorage";

const noOverlaps = (layout: GridPlacement[]): boolean =>
    layout.every((a) =>
        layout.every(
            (b) => a.id === b.id || !(a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y),
        ),
    );

describe("gridEngine", () => {
    describe("clamp", () => {
        it("bounds a value to [lo, hi]", () => {
            expect(clamp(5, 0, 10)).toBe(5);
            expect(clamp(-3, 0, 10)).toBe(0);
            expect(clamp(42, 0, 10)).toBe(10);
        });
    });

    describe("resolveCollisions", () => {
        it("returns a non-overlapping layout unchanged (free placement invariant)", () => {
            const input: GridPlacement[] = [
                { id: "a", x: 0, y: 0, w: 4, h: 2 },
                { id: "b", x: 8, y: 3, w: 4, h: 2 }, // free space left AND above — must be kept
                { id: "c", x: 4, y: 9, w: 4, h: 3 },
            ];
            expect(resolveCollisions(input, "a")).toEqual(expect.arrayContaining(input));
            expect(resolveCollisions(input, "a")).toHaveLength(input.length);
        });

        it("never floats a tile up, even with empty rows above it", () => {
            const input: GridPlacement[] = [{ id: "a", x: 3, y: 7, w: 4, h: 2 }];
            expect(resolveCollisions(input, null)).toEqual(input);
        });

        it("pushes an overlapped tile straight down, just below the pinned tile", () => {
            const result = resolveCollisions(
                [
                    { id: "a", x: 0, y: 0, w: 6, h: 3 }, // pinned
                    { id: "b", x: 2, y: 1, w: 4, h: 2 }, // overlaps a
                ],
                "a",
            );
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.a).toMatchObject({ x: 0, y: 0 });
            expect(byId.b).toMatchObject({ x: 2, y: 3 }); // x unchanged, y = a.y + a.h
            expect(noOverlaps(result)).toBe(true);
        });

        it("cascades a push chain without moving unaffected tiles", () => {
            const result = resolveCollisions(
                [
                    { id: "a", x: 0, y: 0, w: 4, h: 2 }, // pinned onto b
                    { id: "b", x: 0, y: 1, w: 4, h: 2 }, // pushed to y2 → collides with c
                    { id: "c", x: 0, y: 3, w: 4, h: 2 }, // pushed to y4
                    { id: "d", x: 8, y: 0, w: 4, h: 2 }, // elsewhere — untouched
                ],
                "a",
            );
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.b).toMatchObject({ x: 0, y: 2 });
            expect(byId.c).toMatchObject({ x: 0, y: 4 });
            expect(byId.d).toMatchObject({ x: 8, y: 0 });
            expect(noOverlaps(result)).toBe(true);
        });

        it("breaks same-row ties deterministically in x order", () => {
            const input: GridPlacement[] = [
                { id: "pin", x: 0, y: 0, w: 12, h: 2 },
                { id: "right", x: 6, y: 0, w: 6, h: 2 },
                { id: "left", x: 0, y: 0, w: 6, h: 2 },
            ];
            const result = resolveCollisions(input, "pin");
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            // Both overlap the pinned full-width tile; processed left-to-right, both land at y2 side by side.
            expect(byId.left).toMatchObject({ x: 0, y: 2 });
            expect(byId.right).toMatchObject({ x: 6, y: 2 });
            expect(resolveCollisions(input, "pin")).toEqual(result);
            expect(noOverlaps(result)).toBe(true);
        });

        it("normalizes an overlapping layout without a pinned tile (reading order wins)", () => {
            const result = resolveCollisions(
                [
                    { id: "a", x: 0, y: 0, w: 4, h: 2 },
                    { id: "b", x: 0, y: 0, w: 4, h: 2 }, // stale duplicate slot
                ],
                null,
            );
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.a).toMatchObject({ x: 0, y: 0 });
            expect(byId.b).toMatchObject({ x: 0, y: 2 });
            expect(noOverlaps(result)).toBe(true);
        });
    });

    describe("apply", () => {
        it("moves a tile onto another and pushes the collided one straight down", () => {
            const start: GridPlacement[] = [
                { id: "a", x: 0, y: 0, w: 6, h: 2 },
                { id: "b", x: 6, y: 0, w: 6, h: 2 },
            ];
            const result = apply(start, "b", { x: 0, y: 0 });
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.b).toMatchObject({ x: 0, y: 0 });
            expect(byId.a).toMatchObject({ x: 0, y: 2 }); // pushed exactly below b, column kept
            expect(noOverlaps(result)).toBe(true);
        });

        it("leaves tiles outside the push chain exactly where they are", () => {
            const start: GridPlacement[] = [
                { id: "a", x: 0, y: 0, w: 4, h: 2 },
                { id: "b", x: 8, y: 5, w: 4, h: 2 }, // free space above/left — must not compact
            ];
            const result = apply(start, "a", { x: 0, y: 1 });
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.a).toMatchObject({ x: 0, y: 1 });
            expect(byId.b).toMatchObject({ x: 8, y: 5 });
        });

        it("keeps the actively edited tile exactly where requested (pinned) when growing", () => {
            const start: GridPlacement[] = [
                { id: "a", x: 0, y: 0, w: 4, h: 2 },
                { id: "b", x: 4, y: 0, w: 4, h: 2 },
                { id: "c", x: 8, y: 0, w: 4, h: 2 },
            ];
            const result = apply(start, "b", { w: 8 }); // grow b to overlap c
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.b).toMatchObject({ x: 4, y: 0, w: 8 });
            expect(byId.a).toMatchObject({ x: 0, y: 0 }); // untouched
            expect(byId.c).toMatchObject({ x: 8, y: 2 }); // pushed below b
            expect(noOverlaps(result)).toBe(true);
        });
    });

    describe("insertTile", () => {
        it("keeps the desired slot when it is free — even with space left and above", () => {
            const result = insertTile([{ id: "a", x: 0, y: 0, w: 4, h: 2 }], { id: "b", x: 6, y: 4, w: 4, h: 2 }, 12);
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.b).toMatchObject({ x: 6, y: 4 });
            expect(noOverlaps(result)).toBe(true);
        });

        it("falls back to the first free top-left slot when the desired one is occupied", () => {
            const result = insertTile(
                [
                    { id: "a", x: 0, y: 0, w: 8, h: 2 },
                    { id: "b", x: 8, y: 0, w: 4, h: 2 },
                ],
                { id: "c", x: 8, y: 0, w: 4, h: 2 },
                12,
            );
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.c).toMatchObject({ x: 0, y: 2 });
            expect(noOverlaps(result)).toBe(true);
        });

        it("terminates on non-finite dimensions (corrupt input) by coercing them", () => {
            // An `undefined` width (partial saved entry spread into a placement) used to make the
            // placement scan loop forever. It must place with a minimal sane size instead.
            const result = insertTile(
                [{ id: "b", x: 0, y: 2, w: 6, h: 2 }],
                { id: "a", x: 0, y: 0, w: undefined as unknown as number, h: NaN },
                12,
            );
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.a).toMatchObject({ w: 1, h: 1 });
            expect(byId.b).toMatchObject({ x: 0, y: 2, w: 6, h: 2 });
            expect(noOverlaps(result)).toBe(true);
        });

        it("clamps a tile wider than the grid so it can still be placed", () => {
            const result = insertTile([], { id: "a", x: 4, y: 0, w: 20, h: 2 }, 12);
            expect(result[0]).toMatchObject({ x: 0, y: 0, w: 12, h: 2 });
        });
    });

    describe("sanitizeSavedLayout", () => {
        it("accepts a well-formed entry, rounding to integers", () => {
            expect(sanitizeSavedLayout({ x: 2, y: 0.4, w: 6, h: 3 })).toEqual({ x: 2, y: 0, w: 6, h: 3 });
        });

        it("rejects a partial entry (missing dimensions)", () => {
            expect(sanitizeSavedLayout({ x: 2, y: 0 })).toBeNull();
        });

        it("rejects null and non-object values", () => {
            expect(sanitizeSavedLayout(null)).toBeNull();
            expect(sanitizeSavedLayout("wide")).toBeNull();
            expect(sanitizeSavedLayout(7)).toBeNull();
            expect(sanitizeSavedLayout(true)).toBeNull();
        });

        it("rejects non-finite dimensions", () => {
            expect(sanitizeSavedLayout({ x: 0, y: 0, w: NaN, h: 2 })).toBeNull();
            expect(sanitizeSavedLayout({ x: 0, y: Infinity, w: 4, h: 2 })).toBeNull();
            expect(sanitizeSavedLayout({ x: 0, y: 0, w: "4", h: 2 })).toBeNull();
        });

        it("clamps out-of-range values to sane bounds", () => {
            expect(sanitizeSavedLayout({ x: -3, y: 1e9, w: 0, h: 99999 })).toEqual({ x: 0, y: 500, w: 1, h: 500 });
        });
    });

    describe("buildLayout (order-dependent tie-break)", () => {
        it("pins the later item and pushes the earlier one down when two saved tiles overlap", () => {
            // Both saved entries claim the same slot. buildLayout folds in `items` order and pins each
            // saved tile as it is added, so the LATER item ("second") wins the slot and the earlier one
            // ("first") is pushed straight down.
            const boardItems = [
                { id: "first", defaultLayout: { x: 0, y: 0, w: 6, h: 3 }, element: null },
                { id: "second", defaultLayout: { x: 6, y: 0, w: 6, h: 3 }, element: null },
            ];
            const saved = {
                first: { x: 0, y: 0, w: 6, h: 3 },
                second: { x: 0, y: 0, w: 6, h: 3 },
            };
            const result = buildLayout(boardItems, saved, 12);
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.second).toMatchObject({ x: 0, y: 0 }); // later item keeps the contested slot
            expect(byId.first).toMatchObject({ x: 0, y: 3 }); // earlier item pushed below it
            expect(noOverlaps(result)).toBe(true);
        });
    });

    describe("usedRows", () => {
        it("returns the highest occupied row", () => {
            expect(
                usedRows([
                    { id: "a", x: 0, y: 0, w: 4, h: 2 },
                    { id: "b", x: 4, y: 3, w: 4, h: 4 },
                ]),
            ).toBe(7);
        });
    });
});
