import React from "react";
import { createBrowserHistory } from "history";
import { act, fireEvent, RenderResult } from "@testing-library/react";
import { shadcn } from "@eccenca/gui-elements";

import mockAxios from "../../../../__mocks__/axios";
import { renderWrapper, workspacePath } from "../../../TestHelper";
import { triggerHotkeyHandler } from "../../../../../src/app/views/shared/HotKeyHandler/HotKeyHandler";
import { IRecentlyViewedItem, ISearchResultsServer } from "../../../../../src/app/store/ducks/workspace/typings";
import { ISearchListResponse } from "../../../../../src/app/store/ducks/workspace/requests";
import { QuickSearchDialog } from "../../../../../src/app/views/shared/QuickSearch/QuickSearchDialog";

// Drive the two endpoints directly so the debounce timer is the only thing standing between a
// keystroke and the (deterministic) server response.
jest.mock("../../../../../src/app/store/ducks/workspace/requests", () => ({
    recentlyViewedItems: jest.fn(),
    requestSearchList: jest.fn(),
}));

// eslint-disable-next-line @typescript-eslint/no-var-requires
const requests = require("../../../../../src/app/store/ducks/workspace/requests");
const recentlyViewedItems = requests.recentlyViewedItems as jest.Mock;
const requestSearchList = requests.requestSearchList as jest.Mock;

const HOTKEY = "mod+k";
const DEBOUNCE_MS = 250;

const recent = (taskId: string, itemType: string, label: string): IRecentlyViewedItem => ({
    projectId: "proj",
    projectLabel: "Project",
    itemType,
    taskId,
    taskLabel: label,
    itemLinks: [{ label: "open", path: workspacePath(`/projects/proj/${itemType}/${taskId}`) }],
});

const searchResult = (id: string, type: string, label: string): ISearchResultsServer => ({
    id,
    type,
    label,
    projectId: "proj",
    itemLinks: [{ label: "open", path: workspacePath(`/projects/proj/${type}/${id}`) }],
});

const searchResponse = (results: ISearchResultsServer[]): ISearchListResponse => ({
    total: results.length,
    facets: [],
    results,
    sortByProperties: [],
});

const preloaded = [recent("w1", "workflow", "Customer workflow"), recent("d1", "dataset", "Orders dataset")];

describe("QuickSearchDialog", () => {
    let wrapper: RenderResult;
    let history: ReturnType<typeof createBrowserHistory>;

    const renderDialog = () => {
        history = createBrowserHistory();
        history.push(workspacePath(""));
        return renderWrapper(
            <shadcn.TooltipProvider>
                <QuickSearchDialog />
            </shadcn.TooltipProvider>,
            history,
            { common: { initialSettings: { hotKeys: { quickSearch: HOTKEY } } } },
        );
    };

    // The repo tags elements with `data-test-id` (hyphenated), which is not RTL's default
    // `data-testid`, so the palette (portaled to the body) is reached via a CSS query on baseElement.
    const q = (selector: string): HTMLElement | null => wrapper.baseElement.querySelector(selector);
    const modal = () => q('[data-test-id="quick-search-modal"]');

    /** Opens the palette via its hotkey and flushes the preload. */
    const openViaHotkey = async () => {
        await act(async () => {
            triggerHotkeyHandler(HOTKEY);
        });
        // Flush the preload request so the items render.
        await act(async () => {
            await Promise.resolve();
        });
        expect(modal()).toBeInTheDocument();
    };

    const typeQuery = (value: string) => {
        const input = q('[data-test-id="quick-search-input"]') as HTMLInputElement;
        fireEvent.change(input, { target: { value } });
    };

    /** Runs the debounce timer and flushes the resulting request. */
    const runDebounce = async () => {
        await act(async () => {
            jest.advanceTimersByTime(DEBOUNCE_MS);
        });
        await act(async () => {
            await Promise.resolve();
        });
    };

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
        wrapper.unmount();
        mockAxios.reset();
    });

    it("opens on the quick-search hotkey and lists the recently viewed items", async () => {
        wrapper = renderDialog();
        // Closed initially.
        expect(modal()).not.toBeInTheDocument();

        await openViaHotkey();

        expect(wrapper.getByText("Recently viewed")).toBeInTheDocument();
        expect(wrapper.getByText("Customer workflow")).toBeInTheDocument();
        expect(wrapper.getByText("Orders dataset")).toBeInTheDocument();
    });

    it("groups the server results by item type in the canonical order", async () => {
        wrapper = renderDialog();
        await openViaHotkey();

        // Deliberately unordered server payload across several types.
        requestSearchList.mockResolvedValueOnce(
            searchResponse([
                searchResult("d1", "dataset", "A dataset"),
                searchResult("w1", "workflow", "A workflow"),
                searchResult("t1", "transform", "A transform"),
            ]),
        );
        typeQuery("a");
        await runDebounce();

        const headings = modal()!.querySelectorAll("[cmdk-group-heading]");
        const headingTexts = Array.from(headings).map((h) => h.textContent);
        // Canonical order: workflow before transform before dataset — regardless of payload order.
        expect(headingTexts).toEqual(["Workflow", "Transform", "Dataset"]);
        // `toHaveTextContent` rather than `getByText`: the Highlighter splits the label around the
        // matched "a", so the label text is spread across several nodes.
        expect(modal()).toHaveTextContent("A workflow");
    });

    it("navigates to the selected item when Enter is pressed", async () => {
        wrapper = renderDialog();
        await openViaHotkey();

        const input = q('[data-test-id="quick-search-input"]') as HTMLInputElement;
        // cmdk selects the first item by default; Enter activates it.
        await act(async () => {
            fireEvent.keyDown(input, { key: "Enter" });
        });

        expect(history.location.pathname).toBe(workspacePath("/projects/proj/workflow/w1"));
        // Selecting an item closes the palette.
        expect(modal()).not.toBeInTheDocument();
    });

    it("jumps off to the workspace search page for the current query", async () => {
        wrapper = renderDialog();
        await openViaHotkey();

        typeQuery("orders");
        await runDebounce();

        await act(async () => {
            fireEvent.click(q('[data-test-id="quick-search-workspace-search"]') as HTMLElement);
        });

        expect(history.location.pathname).toBe(workspacePath(""));
        expect(history.location.search).toBe("?textQuery=orders");
        expect(modal()).not.toBeInTheDocument();
    });
});
