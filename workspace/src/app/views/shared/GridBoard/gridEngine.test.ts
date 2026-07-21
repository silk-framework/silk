import { apply, clamp, compact, GridPlacement, repack, sanitizeSavedLayout, usedRows } from "./gridEngine";

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

    describe("compact", () => {
        it("floats tiles up to close vertical gaps while keeping their column", () => {
            const result = compact([
                { id: "a", x: 0, y: 0, w: 4, h: 2 },
                { id: "b", x: 0, y: 5, w: 4, h: 2 }, // gap below a
                { id: "c", x: 4, y: 9, w: 4, h: 3 }, // separate column
            ]);
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.a.y).toBe(0);
            expect(byId.b.y).toBe(2); // settled directly under a
            expect(byId.b.x).toBe(0); // column preserved
            expect(byId.c.y).toBe(0); // its own column, floats to the top
            expect(byId.c.x).toBe(4);
            expect(noOverlaps(result)).toBe(true);
        });

        it("is idempotent on an already-settled layout", () => {
            const once = compact([
                { id: "a", x: 0, y: 0, w: 6, h: 2 },
                { id: "b", x: 6, y: 0, w: 6, h: 2 },
                { id: "c", x: 0, y: 2, w: 12, h: 3 },
            ]);
            expect(compact(once)).toEqual(once);
        });
    });

    describe("apply", () => {
        it("moves a tile onto another and pushes the collided one below", () => {
            const start: GridPlacement[] = [
                { id: "a", x: 0, y: 0, w: 6, h: 2 },
                { id: "b", x: 6, y: 0, w: 6, h: 2 },
            ];
            // Drag b onto a's column at the top; a is pinned nowhere, so it must move out of the way.
            const result = apply(start, "b", { x: 0, y: 0 });
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.b).toMatchObject({ x: 0, y: 0 });
            expect(byId.a.y).toBeGreaterThanOrEqual(2); // pushed below the pinned b
            expect(noOverlaps(result)).toBe(true);
        });

        it("keeps the actively edited tile exactly where requested (pinned)", () => {
            const start: GridPlacement[] = [
                { id: "a", x: 0, y: 0, w: 4, h: 2 },
                { id: "b", x: 4, y: 0, w: 4, h: 2 },
                { id: "c", x: 8, y: 0, w: 4, h: 2 },
            ];
            const result = apply(start, "b", { w: 8 }); // grow b to overlap c
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.b).toMatchObject({ x: 4, y: 0, w: 8 });
            expect(noOverlaps(result)).toBe(true);
        });
    });

    describe("repack", () => {
        it("fills a right-column hole by moving a lower tile up and sideways", () => {
            // a spans the left; b sits top-right; the slot under b (right column, rows 2-3) is empty;
            // c lives lower-left. repack should pull c up into the free top-left/right space.
            const result = repack(
                [
                    { id: "a", x: 0, y: 0, w: 6, h: 4 },
                    { id: "b", x: 6, y: 0, w: 6, h: 2 },
                    { id: "c", x: 0, y: 6, w: 6, h: 2 }, // far below — a gap above it
                ],
                12,
            );
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            // c must float up to close the vertical gap (no empty rows between tiles).
            expect(Math.min(byId.a.y, byId.b.y, byId.c.y)).toBe(0);
            expect(usedRows(result)).toBeLessThan(6); // tighter than the sparse input
            expect(noOverlaps(result)).toBe(true);
        });

        it("moves a tile up and sideways into a freed pocket", () => {
            // Tall `a` on the left, short `b` top-middle. The pocket to a's right below b (x4-11, y2-3)
            // is empty, while wide `c` sits far below. repack should pull c up AND across into it.
            const result = repack(
                [
                    { id: "a", x: 0, y: 0, w: 4, h: 4 },
                    { id: "b", x: 4, y: 0, w: 4, h: 2 },
                    { id: "c", x: 0, y: 6, w: 8, h: 2 }, // far below, left column
                ],
                12,
            );
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.a).toMatchObject({ x: 0, y: 0 });
            expect(byId.b).toMatchObject({ x: 4, y: 0 });
            // c floats up from y6→y2 and shifts right from x0→x4 to fill the pocket.
            expect(byId.c).toMatchObject({ x: 4, y: 2 });
            expect(noOverlaps(result)).toBe(true);
        });

        it("respects the column count and preserves every tile's size", () => {
            const input: GridPlacement[] = [
                { id: "a", x: 0, y: 0, w: 8, h: 2 },
                { id: "b", x: 0, y: 2, w: 8, h: 2 },
                { id: "c", x: 0, y: 4, w: 5, h: 2 },
            ];
            const result = repack(input, 12);
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            input.forEach((t) => {
                expect(byId[t.id].w).toBe(t.w);
                expect(byId[t.id].h).toBe(t.h);
            });
            result.forEach((t) => expect(t.x + t.w).toBeLessThanOrEqual(12));
            expect(noOverlaps(result)).toBe(true);
        });

        it("is deterministic for the same input", () => {
            const input: GridPlacement[] = [
                { id: "a", x: 3, y: 1, w: 4, h: 2 },
                { id: "b", x: 0, y: 0, w: 3, h: 3 },
                { id: "c", x: 8, y: 5, w: 4, h: 2 },
            ];
            expect(repack(input, 12)).toEqual(repack(input, 12));
        });

        it("terminates on non-finite dimensions (corrupt persisted layout) by coercing them", () => {
            // An `undefined` width (partial saved entry spread into a placement) used to make the
            // placement scan loop forever. It must place with a minimal sane size instead.
            const result = repack(
                [
                    { id: "a", x: 0, y: 0, w: undefined as unknown as number, h: NaN },
                    { id: "b", x: 0, y: 2, w: 6, h: 2 },
                ],
                12,
            );
            const byId = Object.fromEntries(result.map((i) => [i.id, i]));
            expect(byId.a).toMatchObject({ w: 1, h: 1 });
            expect(byId.b).toMatchObject({ w: 6, h: 2 });
            expect(noOverlaps(result)).toBe(true);
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
