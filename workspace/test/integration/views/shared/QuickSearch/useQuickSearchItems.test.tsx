import React from "react";
import { MemoryRouter } from "react-router";
import { act, renderHook } from "@testing-library/react";

import { IRecentlyViewedItem, ISearchResultsServer } from "../../../../../src/app/store/ducks/workspace/typings";
import { ISearchListResponse } from "../../../../../src/app/store/ducks/workspace/requests";
import { useQuickSearchItems } from "../../../../../src/app/views/shared/QuickSearch/useQuickSearchItems";

// The hook combines two endpoints; mocking the request module (rather than axios) lets each test
// drive the resolution timing precisely, which the debounce and out-of-order assertions rely on.
jest.mock("../../../../../src/app/store/ducks/workspace/requests", () => ({
    recentlyViewedItems: jest.fn(),
    requestSearchList: jest.fn(),
}));

// eslint-disable-next-line @typescript-eslint/no-var-requires
const requests = require("../../../../../src/app/store/ducks/workspace/requests");
const recentlyViewedItems = requests.recentlyViewedItems as jest.Mock;
const requestSearchList = requests.requestSearchList as jest.Mock;

const DEBOUNCE_MS = 250;

const recent = (taskId: string, itemType: string, label: string, path: string): IRecentlyViewedItem => ({
    projectId: "proj",
    projectLabel: "Project",
    itemType,
    taskId,
    taskLabel: label,
    itemLinks: [{ label: "open", path }],
});

const searchResult = (id: string, type: string, label: string): ISearchResultsServer => ({
    id,
    type,
    label,
    projectId: "proj",
    itemLinks: [{ label: "open", path: `/workbench/projects/proj/${type}/${id}` }],
});

const searchResponse = (results: ISearchResultsServer[]): ISearchListResponse => ({
    total: results.length,
    facets: [],
    results,
    sortByProperties: [],
});

/** A promise whose resolution is controlled by the test, for ordering assertions. */
const deferred = <T,>() => {
    let resolve!: (value: T) => void;
    let reject!: (reason?: unknown) => void;
    const promise = new Promise<T>((res, rej) => {
        resolve = res;
        reject = rej;
    });
    return { promise, resolve, reject };
};

/** Renders the hook inside a router (the hook reads the current path) with `isOpen` controllable. */
const renderQuickSearch = (initialPath = "/workbench") =>
    renderHook(({ isOpen }: { isOpen: boolean }) => useQuickSearchItems(isOpen), {
        initialProps: { isOpen: true },
        wrapper: ({ children }) => <MemoryRouter initialEntries={[initialPath]}>{children}</MemoryRouter>,
    });

const preloaded = [
    recent("w1", "workflow", "Customer ingest workflow", "/workbench/projects/proj/workflow/w1"),
    recent("d1", "dataset", "Orders dataset", "/workbench/projects/proj/dataset/d1"),
];

/** Flushes the pending preload before assertions about the query-driven behaviour. */
const flushPreload = async () => {
    await act(async () => {
        await Promise.resolve();
    });
};

describe("useQuickSearchItems", () => {
    beforeEach(() => {
        jest.useFakeTimers();
        recentlyViewedItems.mockReset();
        requestSearchList.mockReset();
        recentlyViewedItems.mockResolvedValue({ data: preloaded });
        requestSearchList.mockResolvedValue(searchResponse([]));
    });

    afterEach(() => {
        act(() => {
            jest.runOnlyPendingTimers();
        });
        jest.useRealTimers();
    });

    it("preloads the recently viewed items and stops loading", async () => {
        const { result } = renderQuickSearch();
        // Before the preload resolves the hook reports it is loading.
        expect(result.current.loading).toBe(true);

        await flushPreload();

        expect(result.current.loading).toBe(false);
        expect(result.current.items.map((i) => i.label)).toEqual(["Customer ingest workflow", "Orders dataset"]);
        // No query yet, so the workspace search is never issued.
        expect(requestSearchList).not.toHaveBeenCalled();
    });

    it("debounces the workspace search by 250ms and issues a single request for a burst of keystrokes", async () => {
        const { result } = renderQuickSearch();
        await flushPreload();

        act(() => result.current.setQuery("cust"));
        act(() => result.current.setQuery("custo"));
        act(() => result.current.setQuery("customer"));

        // Still inside the debounce window: nothing has been requested.
        await act(async () => {
            jest.advanceTimersByTime(DEBOUNCE_MS - 1);
        });
        expect(requestSearchList).not.toHaveBeenCalled();

        // Crossing the threshold fires exactly one request, for the latest text only.
        await act(async () => {
            jest.advanceTimersByTime(1);
        });
        expect(requestSearchList).toHaveBeenCalledTimes(1);
        expect(requestSearchList).toHaveBeenCalledWith(expect.objectContaining({ textQuery: "customer" }));
    });

    it("shows the locally filtered preload immediately, then lets the server result replace it", async () => {
        const { result } = renderQuickSearch();
        await flushPreload();

        // As soon as text is entered — before the debounced request even fires — the preloaded
        // items are filtered locally so there is something to show without a round trip.
        act(() => result.current.setQuery("orders"));
        expect(result.current.items.map((i) => i.label)).toEqual(["Orders dataset"]);

        // The server answers with an authoritative, different set for the same query; it wins.
        requestSearchList.mockResolvedValueOnce(searchResponse([searchResult("srv1", "dataset", "Orders (server)")]));
        await act(async () => {
            jest.advanceTimersByTime(DEBOUNCE_MS);
        });
        await act(async () => {
            await Promise.resolve();
        });
        expect(result.current.items.map((i) => i.label)).toEqual(["Orders (server)"]);
    });

    it("discards a stale response that arrives after a newer query (out-of-order guard)", async () => {
        const first = deferred<ISearchListResponse>();
        const second = deferred<ISearchListResponse>();
        requestSearchList.mockImplementation(({ textQuery }: { textQuery: string }) =>
            textQuery === "aa" ? first.promise : second.promise,
        );

        const { result } = renderQuickSearch();
        await flushPreload();

        // First query fires its (still pending) request.
        act(() => result.current.setQuery("aa"));
        await act(async () => {
            jest.advanceTimersByTime(DEBOUNCE_MS);
        });

        // A newer query supersedes it and fires its own request.
        act(() => result.current.setQuery("bb"));
        await act(async () => {
            jest.advanceTimersByTime(DEBOUNCE_MS);
        });

        // The newer request resolves first and is applied.
        await act(async () => {
            second.resolve(searchResponse([searchResult("b", "workflow", "BB result")]));
            await Promise.resolve();
        });
        expect(result.current.items.map((i) => i.label)).toEqual(["BB result"]);

        // The stale first response now arrives late — it must be ignored, not overwrite "bb".
        await act(async () => {
            first.resolve(searchResponse([searchResult("a", "workflow", "AA result")]));
            await Promise.resolve();
        });
        expect(result.current.items.map((i) => i.label)).toEqual(["BB result"]);
    });

    it("keeps the locally filtered preload when the workspace search fails", async () => {
        const { result } = renderQuickSearch();
        await flushPreload();

        requestSearchList.mockRejectedValueOnce(new Error("search boom"));
        act(() => result.current.setQuery("customer"));
        await act(async () => {
            jest.advanceTimersByTime(DEBOUNCE_MS);
        });
        await act(async () => {
            await Promise.resolve();
        });

        // A failed search is not surfaced as an error and the local match stays visible.
        expect(result.current.error).toBeNull();
        expect(result.current.items.map((i) => i.label)).toEqual(["Customer ingest workflow"]);
    });

    it("resets query and search result when it is closed, so re-opening starts clean", async () => {
        const { result, rerender } = renderQuickSearch();
        await flushPreload();

        act(() => result.current.setQuery("orders"));
        await act(async () => {
            jest.advanceTimersByTime(DEBOUNCE_MS);
        });
        await act(async () => {
            await Promise.resolve();
        });
        expect(result.current.query).toBe("orders");

        // Closing clears the query; the flat preload is shown again.
        rerender({ isOpen: false });
        expect(result.current.query).toBe("");

        // Re-opening reloads the preload and shows it, with no leftover query.
        rerender({ isOpen: true });
        await flushPreload();
        expect(result.current.query).toBe("");
        expect(result.current.items.map((i) => i.label)).toEqual(["Customer ingest workflow", "Orders dataset"]);
    });

    it("swaps the most-recent item when it is the page the user is already on", async () => {
        // The first preloaded item points at the current path, which is never a useful suggestion.
        recentlyViewedItems.mockResolvedValueOnce({
            data: [
                recent("here", "workflow", "Current page", "/workbench/projects/proj/workflow/here"),
                recent("other", "dataset", "Other item", "/workbench/projects/proj/dataset/other"),
            ],
        });

        const { result } = renderQuickSearch("/workbench/projects/proj/workflow/here");
        await flushPreload();

        // The current-page item is demoted below the next one.
        expect(result.current.items.map((i) => i.label)).toEqual(["Other item", "Current page"]);
    });

    it("surfaces a preload failure as an error and stops loading", async () => {
        const errorResponse = { title: "Preload failed", detail: "nope" };
        recentlyViewedItems.mockRejectedValueOnce({ isFetchError: true, errorResponse });

        const { result } = renderQuickSearch();
        await flushPreload();

        expect(result.current.loading).toBe(false);
        expect(result.current.error).toEqual(errorResponse);
    });
});
