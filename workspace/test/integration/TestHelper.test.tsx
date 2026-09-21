import { configureStore } from "@reduxjs/toolkit";
import { push } from "connected-react-router";
import { createMemoryHistory } from "history";

import { createStore } from "./TestHelper";

jest.mock("@reduxjs/toolkit", () => {
    const actual = jest.requireActual<typeof import("@reduxjs/toolkit")>("@reduxjs/toolkit");
    return { ...actual, configureStore: jest.fn(actual.configureStore) };
});

beforeEach(() => jest.mocked(configureStore).mockClear());

describe("integration test stores", () => {
    it("initializes reducer defaults with only one configured store", () => {
        const history = createMemoryHistory({ initialEntries: ["/first"] });
        const store = createStore(history, {});

        expect(configureStore).toHaveBeenCalledTimes(1);
        expect(Object.keys(store.getState()).sort()).toEqual(["common", "error", "router", "workspace"]);
        expect(store.getState().common.initialSettings.emptyWorkspace).toBe(true);
        expect(store.getState().router.location.pathname).toBe("/first");
    });

    it("merges nested overrides without changing defaults or the supplied state", () => {
        const history = createMemoryHistory();
        const defaults = createStore(history, {}).getState();
        const overrides = { common: { initialSettings: { dmBaseUrl: "https://example.test" } } };
        const store = createStore(history, overrides);

        expect(store.getState().common.initialSettings).toEqual({
            ...defaults.common.initialSettings,
            dmBaseUrl: "https://example.test",
        });
        expect(store.getState().workspace).toEqual(defaults.workspace);
        expect(defaults.common.initialSettings.dmBaseUrl).toBeUndefined();
        expect(overrides).toEqual({ common: { initialSettings: { dmBaseUrl: "https://example.test" } } });
    });

    it("keeps stores independent and preserves thunk and router middleware", () => {
        const firstHistory = createMemoryHistory({ initialEntries: ["/first"] });
        const secondHistory = createMemoryHistory({ initialEntries: ["/second"] });
        const first = createStore(firstHistory, {});
        const second = createStore(secondHistory, {});

        first.dispatch({ type: "common/toggleUserMenuDisplay", payload: true });
        expect(first.getState().common.userMenuDisplay).toBe(true);
        expect(second.getState().common.userMenuDisplay).toBe(false);
        expect(first.dispatch(() => "thunk result")).toBe("thunk result");
        first.dispatch(push("/next"));
        expect(firstHistory.location.pathname).toBe("/next");
        expect(secondHistory.location.pathname).toBe("/second");
    });
});
