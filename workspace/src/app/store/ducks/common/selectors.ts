import { createSelector } from "reselect";
import { isAuthenticated } from "./thunks/auth.thunk";

const commonSelector = state => state.common;

const artefactModalSelector = createSelector(
    [commonSelector],
    common => common.artefactModal
);

const isAuthSelector = createSelector(
    [commonSelector],
    common => common.authenticated || isAuthenticated()
);

const searchStringSelector = createSelector(
    [commonSelector],
    common => common.searchQuery || ''
);

const availableDTypesSelector = createSelector(
    [commonSelector],
    common => common.availableDataTypes
);

const currentProjectIdSelector = createSelector(
    [commonSelector],
    common => common.currentProjectId
);

const initialSettingsSelector = createSelector(
    [commonSelector],
    common => common.initialSettings
);

export default {
    commonSelector,
    searchStringSelector,
    availableDTypesSelector,
    artefactModalSelector,
    isAuthSelector,
    initialSettingsSelector,
    currentProjectIdSelector
}
