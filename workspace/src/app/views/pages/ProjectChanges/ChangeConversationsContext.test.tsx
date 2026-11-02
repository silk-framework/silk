import React from "react";
import { act, render, waitFor } from "@testing-library/react";
import {
    ChangeConversationsContext,
    ChangeConversationsProvider,
    identifyChangeConversations,
    RefreshChangeConversationsContext,
} from "./ChangeConversationsContext";

const mockLoadConversations = jest.fn();
const mockSource = { loadConversations: mockLoadConversations };
let mockEnabled = true;
jest.mock("@ducks/common", () => ({ commonSel: { initialSettingsSelector: jest.fn() } }));
jest.mock("react-redux", () => ({ useSelector: () => ({ companion: { enabled: mockEnabled } }) }));
jest.mock("../../plugins/PluginRegistry", () => ({
    SUPPORTED_PLUGINS: { DI_CHANGE_CONVERSATIONS: "conversations" },
    pluginRegistry: { pluginComponent: () => mockSource },
}));

it("keeps numbers across reordered lists, deletions and duplicate titles; new IDs get new numbers", () => {
    const numbers = new Map<string, number>();
    const first = identifyChangeConversations(
        [
            { id: "a", label: "Same" },
            { id: "b", label: "Same" },
        ],
        numbers,
    );
    expect(first.get("a")).toEqual({ number: 1, title: "Same" });
    expect(first.get("b")).toEqual({ number: 2, title: "Same" });
    const refreshed = identifyChangeConversations(
        [
            { id: "b", label: "Renamed" },
            { id: "c", summary: "Fallback" },
        ],
        numbers,
    );
    expect(refreshed.has("a")).toBe(false);
    expect(refreshed.get("b")).toEqual({ number: 2, title: "Renamed" });
    expect(refreshed.get("c")).toEqual({ number: 3, title: "Fallback" });
});

const Consumer = () => (
    <ChangeConversationsContext.Consumer>
        {(values) => <div>{Array.from(values, ([id, value]) => `${id}:${value.number}:${value.title}`).join("|")}</div>}
    </ChangeConversationsContext.Consumer>
);

beforeEach(() => {
    mockEnabled = true;
    mockLoadConversations.mockReset();
});

it("loads independently of child renders, refreshes on focus and clears titles when access fails", async () => {
    mockLoadConversations.mockResolvedValueOnce([{ id: "a", label: "Private title" }]);
    const view = render(
        <ChangeConversationsProvider>
            <Consumer />
        </ChangeConversationsProvider>,
    );
    await waitFor(() => expect(view.container).toHaveTextContent("a:1:Private title"));
    view.rerender(
        <ChangeConversationsProvider>
            <Consumer />
        </ChangeConversationsProvider>,
    );
    expect(mockLoadConversations).toHaveBeenCalledTimes(1);
    mockLoadConversations.mockRejectedValueOnce(new Error("Forbidden"));
    act(() => window.dispatchEvent(new Event("focus")));
    await waitFor(() => expect(view.container.textContent).toBe(""));
    view.unmount();
    expect(mockLoadConversations.mock.calls[0][0].aborted).toBe(true);
    expect(mockLoadConversations.mock.calls[1][0].aborted).toBe(true);
});

it("does not contact Explore when the companion is disabled", () => {
    mockEnabled = false;
    render(
        <ChangeConversationsProvider>
            <Consumer />
        </ChangeConversationsProvider>,
    );
    expect(mockLoadConversations).not.toHaveBeenCalled();
});

it("refreshes metadata on journal reload without reassigning existing numbers", async () => {
    mockLoadConversations.mockResolvedValueOnce([{ id: "a", label: "First" }]);
    const view = render(
        <ChangeConversationsProvider>
            <Consumer />
            <RefreshChangeConversationsContext.Consumer>
                {(refresh) => <button onClick={refresh}>Reload</button>}
            </RefreshChangeConversationsContext.Consumer>
        </ChangeConversationsProvider>,
    );
    await waitFor(() => expect(view.container).toHaveTextContent("a:1:First"));
    mockLoadConversations.mockResolvedValueOnce([
        { id: "b", label: "New" },
        { id: "a", label: "Renamed" },
    ]);
    act(() => view.getByRole("button", { name: "Reload" }).click());
    await waitFor(() => expect(view.container).toHaveTextContent("b:2:New|a:1:Renamed"));
});

it("ignores an earlier lookup that finishes after a newer one", async () => {
    let finishFirst: ((value: { id: string; label: string }[]) => void) | undefined;
    mockLoadConversations.mockReturnValueOnce(
        new Promise((resolve) => {
            finishFirst = resolve;
        }),
    );
    const view = render(
        <ChangeConversationsProvider>
            <Consumer />
        </ChangeConversationsProvider>,
    );
    mockLoadConversations.mockResolvedValueOnce([{ id: "new", label: "Current" }]);
    act(() => window.dispatchEvent(new Event("focus")));
    await waitFor(() => expect(view.container).toHaveTextContent("new:1:Current"));
    await act(async () => {
        finishFirst?.([{ id: "old", label: "Stale" }]);
    });
    expect(view.container).not.toHaveTextContent("Stale");
    expect(view.container).toHaveTextContent("new:1:Current");
});
