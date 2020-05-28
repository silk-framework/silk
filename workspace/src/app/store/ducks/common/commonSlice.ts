import { createSlice } from "@reduxjs/toolkit";
import { initialCommonState } from "./initialState";

const rootReducers = {
    setInitialSettings(state, action) {
        state.initialSettings = action.payload;
    },
    fetchAvailableDTypes(state) {
        state.availableDataTypes = {};
    },
    updateAvailableDTypes(state, action) {
        const { fieldName, modifier } = action.payload;
        state.availableDataTypes[fieldName] = modifier;
    },
    setProjectId(state, action) {
        state.currentProjectId = action.payload;
    },
    unsetProject(state) {
        state.currentProjectId = null;
    },
    setTaskId(state, action) {
        state.currentTaskId = action.payload;
    },
    unsetTaskId(state) {
        state.currentTaskId = null;
    },
    setError(state, action) {
        state.error = action.payload;
    },
    changeLanguage(state, action) {
        state.locale = action.payload.locale;
    },
    loginSuccess(state) {
        state.authenticated = true;
    },
    logoutUser(state) {
        state.authenticated = false;
    },
};

const artefactModalReducers = {
    closeArtefactModal(state) {
        state.artefactModal.isOpen = false;
        state.artefactModal.selectedArtefact = {};
        state.artefactModal.updateExistingTask = null;
    },
    selectArtefact(state, action) {
        state.artefactModal.isOpen = true;
        state.artefactModal.selectedArtefact = action.payload || {};
        state.artefactModal.updateExistingTask = null;
    },
    fetchArtefactsList(state) {
        state.artefactModal.artefactsList = [];
        state.artefactModal.error = {};
    },
    setArtefactsList(state, action) {
        state.artefactModal.artefactsList = action.payload;
    },
    setSelectedArtefactDType(state, action) {
        state.artefactModal.selectedDType = action.payload || "all";
        state.artefactModal.isOpen = true;
    },
    setCachedArtefactProperty(state, action) {
        const { key } = state.artefactModal.selectedArtefact;
        state.artefactModal.cachedArtefactProperties[key] = action.payload;
    },
    setArtefactLoading(state, action) {
        state.artefactModal.loading = action.payload;
    },
    updateProjectTask(state, action) {
        state.artefactModal.updateExistingTask = action.payload;
        state.artefactModal.isOpen = true;
    },
    setModalError(state, action) {
        state.artefactModal.error = action.payload;
    },
};

export const commonSlice = createSlice({
    name: "common",
    initialState: initialCommonState(),
    reducers: {
        ...rootReducers,
        ...artefactModalReducers,
    },
});
