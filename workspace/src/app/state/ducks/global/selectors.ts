import { createSelector } from "reselect";
import { isAuthenticated } from "./thunks/auth.thunk";

const globalSelector = state => state.global;
const isAuthSelector = createSelector(
    [globalSelector],
    global => global.authenticated || isAuthenticated()
);

export default {
    globalSelector,
    isAuthSelector
}
