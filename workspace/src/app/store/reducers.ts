import { combineSlices } from "@reduxjs/toolkit";

import workspace from "@ducks/workspace";
import common from "@ducks/common";
import routerReducers from "@ducks/router";
import { Reducer, Action } from "redux";
import { IStore } from "./typings/IStore";
import error from "@ducks/error";

/** Minimal slice contract accepted by `injectSlice` (an RTK slice satisfies it). */
interface InjectableSlice {
    reducerPath: string;
    reducer: Reducer;
}

let combinedReducer: ReturnType<typeof buildRootReducer> | undefined;
const pendingSlices: InjectableSlice[] = [];

const buildRootReducer = (history) =>
    combineSlices({
        common: common.reducer,
        workspace,
        error: error,
        router: routerReducers(history),
    }).withLazyLoadedSlices<Record<string, unknown>>();

/**
 * Registers an additional slice on the root reducer, e.g. from a workspace plugin
 * (plugins cannot be imported here — the dependency points the other way).
 * Safe to call before or after store creation: plugin entry modules load in
 * unspecified order relative to it. Lazily-injected state materializes on the
 * next dispatched action; selectors must tolerate an undefined slice until then.
 */
export const injectSlice = (slice: InjectableSlice): void => {
    if (combinedReducer) {
        combinedReducer.inject(slice);
    } else {
        pendingSlices.push(slice);
    }
};

const reducers = (history): Reducer<IStore, Action> => {
    combinedReducer = buildRootReducer(history);
    pendingSlices.splice(0).forEach((slice) => combinedReducer?.inject(slice));
    return combinedReducer as unknown as Reducer<IStore, Action>;
};

export default reducers;
