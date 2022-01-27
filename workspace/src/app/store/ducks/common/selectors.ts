import { createSelector } from "reselect";
import { IStore } from "../../typings/IStore";

const commonSelector = (state: IStore) => state.common;

const artefactModalSelector = createSelector([commonSelector], (common) => common.artefactModal);

const searchStringSelector = createSelector([commonSelector], (common) => common.searchQuery || "");

const availableDTypesSelector = createSelector([commonSelector], (common) => common.availableDataTypes);

const currentProjectIdSelector = createSelector([commonSelector], (common) => common.currentProjectId);

const currentTaskIdSelector = createSelector([commonSelector], (common) => common.currentTaskId);

const initialSettingsSelector = createSelector([commonSelector], (common) => common.initialSettings);

const exportTypesSelector = createSelector([commonSelector], (common) => common.exportTypes);

const commonSelectors = {
    commonSelector,
    searchStringSelector,
    availableDTypesSelector,
    artefactModalSelector,
    initialSettingsSelector,
    currentTaskIdSelector,
    currentProjectIdSelector,
    exportTypesSelector,
};

export default commonSelectors;
