import { createAction, createSlice } from "@reduxjs/toolkit";
import { initialCommonState } from "./initialState";
import { LOCATION_CHANGE } from "connected-react-router";
import appRoutes from "../../../appRoutes";
import { matchPath } from "react-router";
import { getFullRoutePath } from "../../../utils/routerUtils";
import { getHistory } from "../../configureStore";

const commonReducers = {
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
    setTaskId(state, action) {
        state.currentTaskId = action.payload;
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
        // Calculate category counts
        const categories: Record<string, number> = {};
        categories["All"] = action.payload.length;
        action.payload.forEach((itemDescription) => {
            itemDescription.categories.forEach((category) => {
                categories[category] = (categories[category] ? categories[category] : 0) + 1;
            });
        });
        const sortedCategoryCounts = Object.entries(categories)
            .map(([category, count]) => ({ label: category, count: count }))
            .sort((left, right) => (left.label < right.label ? -1 : 1));
        state.artefactModal.artefactsList = action.payload;
        state.artefactModal.categories = sortedCategoryCounts;
    },
    setSelectedArtefactDType(state, action) {
        state.artefactModal.selectedDType = action.payload || "all";
        state.artefactModal.isOpen = true;
    },
    setCachedArtefactProperty(state, action) {
        const key = action.payload.pluginId;
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

/**
 * @override connect-react-router location change action
 * set projectId and taskId on location change
 */
const getExtraReducers = () => {
    const routerChange = createAction(LOCATION_CHANGE);
    return {
        [routerChange.toString()]: (state) => {
            const { location } = getHistory();
            const updatedState = {
                ...state,
            };

            let match;
            for (let route of appRoutes) {
                match = matchPath<{ taskId?: string; projectId?: string }>(location.pathname, {
                    path: getFullRoutePath(route.path),
                    exact: true,
                });

                if (match) {
                    updatedState.currentProjectId = match.params.projectId || null;
                    updatedState.currentTaskId = match.params.taskId || null;
                    break;
                }
            }

            if (!match) {
                updatedState.currentTaskId = null;
                updatedState.currentProjectId = null;
            }

            return updatedState;
        },
    };
};

export const commonSlice = createSlice({
    name: "common",
    initialState: initialCommonState(),
    reducers: {
        ...commonReducers,
        ...artefactModalReducers,
    },
    extraReducers: getExtraReducers(),
});
