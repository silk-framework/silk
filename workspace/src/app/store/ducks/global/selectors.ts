import { createSelector } from "reselect";
import { isAuthenticated } from "./thunks/auth.thunk";

const globalSelector = state => state.global;
const isAuthSelector = createSelector(
    [globalSelector],
    global => global.authenticated || isAuthenticated()
);
const searchStringSelector = createSelector(
    [globalSelector],
    global => global.searchQuery || ''
);
const breadcrumbsSelector = createSelector(
    [globalSelector],
    global => global.breadcrumbs
);
const availableDTypesSelector = createSelector(
    [globalSelector],
    global => global.availableDataTypes
);

export default {
    globalSelector,
    searchStringSelector,
    availableDTypesSelector,
    breadcrumbsSelector,
    isAuthSelector
}
