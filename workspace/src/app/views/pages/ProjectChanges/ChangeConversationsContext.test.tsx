import React from "react";
import { ChangeConversationAccessError } from "./ChangeConversationAccessError";
import { act, render, waitFor } from "@testing-library/react";
import {
    ChangeConversationsContext,
    RequestChangeConversationsContext,
    ChangeConversationsProvider,
    identifyChangeConversations,
    RefreshChangeConversationsContext,
} from "./ChangeConversationsContext";

const mockLoadConversations = jest.fn();
const mockLoadConversation = jest.fn();
const mockSource = { loadConversations: mockLoadConversations, loadConversation: mockLoadConversation };
let mockEnabled = true;
let mockUserUri = "first";
let mockApiBasePath = "/custom/dp/api/companion/interactive";
jest.mock("@ducks/common", () => ({ commonSel: { initialSettingsSelector: jest.fn() } }));
jest.mock("react-redux", () => ({
    useSelector: () => ({
        companion: { enabled: mockEnabled, apiBasePath: mockApiBasePath },
        userUri: mockUserUri,
    }),
}));
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
        {(values) => (
            <div data-testid="metadata">
                {Array.from(values, ([id, value]) => `${id}:${value.number}:${value.title}`).join("|")}
            </div>
        )}
    </ChangeConversationsContext.Consumer>
);

const Reload = () => (
    <RefreshChangeConversationsContext.Consumer>
        {(refresh) => <button onClick={refresh}>Reload</button>}
    </RefreshChangeConversationsContext.Consumer>
);

beforeEach(() => {
    mockEnabled = true;
    mockUserUri = "first";
    mockApiBasePath = "/custom/dp/api/companion/interactive";
    mockLoadConversations.mockReset();
    mockLoadConversation.mockReset();
});

it("keeps cached metadata without requesting it again when the window gains focus", async () => {
    mockLoadConversations.mockResolvedValue([{ id: "a", label: "Known" }]);
    const view = render(
        <ChangeConversationsProvider>
            <Consumer />
        </ChangeConversationsProvider>,
    );
    await waitFor(() => expect(view.container).toHaveTextContent("a:1:Known"));
    await act(async () => {
        window.dispatchEvent(new Event("focus"));
        window.dispatchEvent(new Event("focus"));
    });
    expect(mockLoadConversations).toHaveBeenCalledTimes(1);
    expect(view.container).toHaveTextContent("a:1:Known");
});

it("loads independently of child renders and clears titles when a manual refresh loses access", async () => {
    mockLoadConversations.mockResolvedValueOnce([{ id: "a", label: "Private title" }]);
    const view = render(
        <ChangeConversationsProvider>
            <Consumer />
            <Reload />
        </ChangeConversationsProvider>,
    );
    await waitFor(() => expect(view.container).toHaveTextContent("a:1:Private title"));
    expect(mockLoadConversations).toHaveBeenCalledWith(expect.any(AbortSignal), "/custom/dp/api/companion/interactive");
    view.rerender(
        <ChangeConversationsProvider>
            <Consumer />
            <Reload />
        </ChangeConversationsProvider>,
    );
    expect(mockLoadConversations).toHaveBeenCalledTimes(1);
    mockLoadConversations.mockRejectedValueOnce(new ChangeConversationAccessError("Forbidden"));
    act(() => view.getByRole("button", { name: "Reload" }).click());
    await waitFor(() => expect(view.getByTestId("metadata").textContent).toBe(""));
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
    await waitFor(() => expect(view.container).toHaveTextContent("a:1:Renamed|b:2:New"));
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
            <Reload />
        </ChangeConversationsProvider>,
    );
    mockLoadConversations.mockResolvedValueOnce([{ id: "new", label: "Current" }]);
    act(() => view.getByRole("button", { name: "Reload" }).click());
    await waitFor(() => expect(view.container).toHaveTextContent("new:1:Current"));
    await act(async () => {
        finishFirst?.([{ id: "old", label: "Stale" }]);
    });
    expect(view.container).not.toHaveTextContent("Stale");
    expect(view.container).toHaveTextContent("new:1:Current");
});

it("retains known metadata after failed refreshes and absence from the recent list", async () => {
    mockLoadConversations.mockResolvedValueOnce([{ id: "a", label: "Known" }]);
    const view = render(
        <ChangeConversationsProvider>
            <Consumer />
            <Reload />
        </ChangeConversationsProvider>,
    );
    await waitFor(() => expect(view.container).toHaveTextContent("a:1:Known"));
    mockLoadConversations.mockRejectedValue(new Error("Temporary server failure"));
    await act(async () => view.getByRole("button", { name: "Reload" }).click());
    expect(view.container).toHaveTextContent("a:1:Known");
    await act(async () => view.getByRole("button", { name: "Reload" }).click());
    expect(view.container).toHaveTextContent("a:1:Known");
    mockLoadConversations.mockResolvedValueOnce([]);
    await act(async () => view.getByRole("button", { name: "Reload" }).click());
    expect(view.container).toHaveTextContent("a:1:Known");
});

it.each(["user", "api", "disabled"])(
    "clears cached metadata when %s changes even if the next lookup fails",
    async (change) => {
        mockLoadConversations.mockResolvedValueOnce([{ id: "a", label: "Private" }]);
        const tree = (
            <ChangeConversationsProvider>
                <Consumer />
            </ChangeConversationsProvider>
        );
        const view = render(tree);
        await waitFor(() => expect(view.container).toHaveTextContent("Private"));
        mockLoadConversations.mockRejectedValue(new Error("Unavailable"));
        if (change === "user") mockUserUri = "second";
        if (change === "api") mockApiBasePath = "/other/api";
        if (change === "disabled") mockEnabled = false;
        view.rerender(
            <ChangeConversationsProvider>
                <Consumer />
            </ChangeConversationsProvider>,
        );
        await waitFor(() => expect(view.container.textContent).toBe(""));
    },
);

const RequestMetadata = ({ ids }: { ids: string[] }) => {
    const request = React.useContext(RequestChangeConversationsContext);
    React.useEffect(() => request(ids), [request, ids]);
    return null;
};
const cachedTree = (ids: string[]) => (
    <ChangeConversationsProvider>
        <RequestMetadata ids={ids} />
        <Consumer />
        <Reload />
    </ChangeConversationsProvider>
);

it("resolves missing referenced IDs once and retains them across pages and reloads", async () => {
    mockLoadConversations.mockResolvedValue([{ id: "recent", label: "Recent" }]);
    mockLoadConversation.mockResolvedValue({ id: "old", label: "Older" });
    const view = render(cachedTree(["recent", "old", "old"]));
    await waitFor(() => expect(view.container).toHaveTextContent("old:2:Older"));
    expect(mockLoadConversation).toHaveBeenCalledTimes(1);
    expect(mockLoadConversation).toHaveBeenCalledWith("old", expect.any(AbortSignal), mockApiBasePath);
    view.rerender(cachedTree(["recent"]));
    view.rerender(cachedTree(["old"]));
    await act(async () => view.getByRole("button", { name: "Reload" }).click());
    expect(view.container).toHaveTextContent("old:2:Older");
    expect(mockLoadConversation).toHaveBeenCalledTimes(1);
    mockLoadConversations.mockResolvedValueOnce([{ id: "old", label: "Now recent" }]);
    await act(async () => view.getByRole("button", { name: "Reload" }).click());
    expect(view.container).toHaveTextContent("old:2:Now recent");
    expect(view.container).toHaveTextContent("recent:1:Recent");
    expect(mockLoadConversation).toHaveBeenCalledTimes(1);
});

it("does not repeat failed or unavailable lookups until an explicit reload", async () => {
    mockLoadConversations.mockResolvedValue([]);
    mockLoadConversation.mockRejectedValueOnce(new Error("Temporary"));
    const view = render(cachedTree(["old"]));
    await waitFor(() => expect(mockLoadConversation).toHaveBeenCalledTimes(1));
    await act(async () => view.rerender(cachedTree(["old"])));
    expect(mockLoadConversation).toHaveBeenCalledTimes(1);
    mockLoadConversation.mockResolvedValueOnce(null);
    await act(async () => view.getByRole("button", { name: "Reload" }).click());
    expect(mockLoadConversation).toHaveBeenCalledTimes(2);
    mockLoadConversation.mockResolvedValueOnce({ id: "old", label: "Recovered" });
    await act(async () => view.getByRole("button", { name: "Reload" }).click());
    expect(view.container).toHaveTextContent("old:1:Recovered");
});

it.each(["user", "api", "disabled"])("ignores fallback responses after a %s change", async (change) => {
    let finish: ((value: { id: string; label: string }) => void) | undefined;
    mockLoadConversations.mockResolvedValue([]);
    mockLoadConversation.mockReturnValueOnce(
        new Promise((resolve) => {
            finish = resolve;
        }),
    );
    const view = render(cachedTree(["old"]));
    await waitFor(() => expect(mockLoadConversation).toHaveBeenCalledTimes(1));
    const oldSignal = mockLoadConversation.mock.calls[0][1];
    if (change === "user") mockUserUri = "second";
    if (change === "api") mockApiBasePath = "/other/api";
    if (change === "disabled") mockEnabled = false;
    mockLoadConversation.mockResolvedValueOnce(null);
    view.rerender(cachedTree(["old"]));
    await act(async () => finish?.({ id: "old", label: "Private" }));
    expect(oldSignal.aborted).toBe(true);
    expect(view.container).not.toHaveTextContent("Private");
});

it("does not overwrite fresh recent metadata with a superseded fallback", async () => {
    let finish: ((value: { id: string; label: string }) => void) | undefined;
    mockLoadConversations.mockResolvedValueOnce([]);
    mockLoadConversation.mockReturnValueOnce(
        new Promise((resolve) => {
            finish = resolve;
        }),
    );
    const view = render(cachedTree(["old"]));
    await waitFor(() => expect(mockLoadConversation).toHaveBeenCalledTimes(1));
    mockLoadConversations.mockResolvedValueOnce([{ id: "old", label: "Fresh" }]);
    await act(async () => view.getByRole("button", { name: "Reload" }).click());
    await act(async () => finish?.({ id: "old", label: "Stale" }));
    expect(view.container).toHaveTextContent("old:1:Fresh");
    expect(view.container).not.toHaveTextContent("Stale");
});

it("clears metadata and stops pending fallbacks when the session is lost", async () => {
    mockLoadConversations.mockResolvedValue([{ id: "recent", label: "Private" }]);
    let finish: ((value: { id: string; label: string }) => void) | undefined;
    mockLoadConversation.mockImplementation((id: string) =>
        id === "lost"
            ? Promise.reject(new ChangeConversationAccessError("Unauthorized"))
            : new Promise((resolve) => {
                  finish = resolve;
              }),
    );
    const view = render(cachedTree(["lost", "pending"]));
    await waitFor(() => expect(mockLoadConversation).toHaveBeenCalledTimes(2));
    await act(async () => finish?.({ id: "pending", label: "Private fallback" }));
    expect(view.getByTestId("metadata").textContent).toBe("");
    expect(mockLoadConversation.mock.calls[1][1].aborted).toBe(true);
});

it("keeps accessible metadata when a specific conversation is unavailable", async () => {
    mockLoadConversations.mockResolvedValue([{ id: "recent", label: "Accessible" }]);
    mockLoadConversation.mockResolvedValue(null);
    const view = render(cachedTree(["missing"]));
    await waitFor(() => expect(mockLoadConversation).toHaveBeenCalledTimes(1));
    expect(view.container).toHaveTextContent("recent:1:Accessible");
    expect(view.container).not.toHaveTextContent("missing:");
});
