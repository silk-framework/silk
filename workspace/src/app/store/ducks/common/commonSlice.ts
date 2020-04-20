import { createSlice } from "@reduxjs/toolkit";
import { initialCommonState } from "./initialState";
import { generateNetworkError, isNetworkError } from "../../../services/errorLogger";

const rootReducers = {
    setInitialSettings(state, action) {
        state.initialSettings = action.payload;
    },
    fetchAvailableDTypes(state) {
        state.availableDataTypes = {};
    },
    updateAvailableDTypes(state, action) {
        const {fieldName, modifier} = action.payload;
        state.availableDataTypes[fieldName] = modifier;
    },
    setProjectId(state, action) {
        state.currentProjectId = action.payload;
    },
    unsetProject(state) {
        state.currentProjectId = null;
    },
    setError(state, action) {
        let error = action.payload;
        if (isNetworkError(error)) {
            error = generateNetworkError(error);
        }
        state.error = error;
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
    },
    selectArtefact(state, action) {
        state.artefactModal.isOpen = true;
        state.artefactModal.selectedArtefact = action.payload || {};
    },
    fetchArtefactsList(state) {
        state.artefactModal.artefactsList = [];
    },
    setArtefactsList(state, action) {
        state.artefactModal.artefactsList = action.payload;
    },
    setSelectedArtefactDType(state, action) {
        state.artefactModal.selectedDType = action.payload;
        state.artefactModal.isOpen = true;
    },
    setCachedArtefactProperty(state, action) {
        const {key} = state.artefactModal.selectedArtefact;
        state.artefactModal.cachedArtefactProperties[key] = action.payload;
    },
    setArtefactLoading(state, action) {
        state.artefactModal.loading = action.payload;
    },
};

export const commonSlice = createSlice({
    name: 'common',
    initialState: initialCommonState(),
    reducers: {
        ...rootReducers,
        ...artefactModalReducers,
    }
});
