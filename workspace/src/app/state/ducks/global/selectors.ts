import { createSelector } from "reselect";
import { isAuthenticated } from "./thunks/auth.thunk";

const globalSelector = state => state.global;
const isAuthSelector = createSelector(
    [globalSelector],
    global => global.authenticated || isAuthenticated()
);
const searchStringSelector = createSelector(
    [globalSelector],
    global => global.searchString || ''
);

export default {
    globalSelector,
    searchStringSelector,
    isAuthSelector
}
