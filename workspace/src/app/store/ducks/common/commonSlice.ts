import { createAction, createSlice, PayloadAction } from "@reduxjs/toolkit";
import { initialCommonState } from "./initialState";
import { LOCATION_CHANGE } from "connected-react-router";
import appRoutes from "../../../appRoutes";
import { matchPath } from "react-router";
import { getFullRoutePath } from "../../../utils/routerUtils";
import { getHistory } from "../../configureStore";
import {
    IPluginOverview,
    IAvailableDataType,
    IPluginDetails,
    IExportTypes,
    IInitFrontend,
    IProjectTaskUpdatePayload,
} from "@ducks/common/typings";
import { setStoredLang } from "../../../../language";

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
        setInitialSettings: (state, action: PayloadAction<IInitFrontend>) => {
            state.initialSettings = action.payload;
        },

        fetchAvailableDTypes: (state) => {
            state.availableDataTypes = {};
        },

        setExportTypes: (state, action: PayloadAction<IExportTypes[]>) => {
            state.exportTypes = action.payload;
        },

        updateAvailableDTypes: (state, action: PayloadAction<{ fieldName: string; modifier: IAvailableDataType }>) => {
            const { fieldName, modifier } = action.payload;
            state.availableDataTypes[fieldName] = modifier;
        },

        setProjectId: (state, action: PayloadAction<string>) => {
            state.currentProjectId = action.payload;
        },

        setTaskId: (state, action: PayloadAction<string>) => {
            state.currentTaskId = action.payload;
        },

        setError: (state, action: PayloadAction<any>) => {
            state.error = action.payload;
        },

        changeLanguage: (state, action: PayloadAction<string>) => {
            const lang = action.payload;
            state.locale = lang;
            setStoredLang(lang);
        },

        loginSuccess: (state) => {
            state.authenticated = true;
        },

        logoutUser: (state) => {
            state.authenticated = false;
        },

        closeArtefactModal: (state) => {
            state.artefactModal.isOpen = false;
            state.artefactModal.selectedArtefact = undefined;
            state.artefactModal.updateExistingTask = undefined;
        },

        selectArtefact: (state, action: PayloadAction<IPluginOverview | undefined>) => {
            state.artefactModal.isOpen = true;
            state.artefactModal.selectedArtefact = action.payload || ({} as IPluginOverview);
            state.artefactModal.updateExistingTask = undefined;
        },

        fetchArtefactsList: (state) => {
            state.artefactModal.artefactsList = [];
            state.artefactModal.error = {};
        },

        setArtefactsList: (state, action: PayloadAction<IPluginOverview[]>) => {
            // Calculate category counts
            const categories: Record<string, number> = Object.create(null);
            categories["All"] = action.payload.length;
            action.payload.forEach((itemDescription) => {
                (itemDescription.categories ?? []).forEach((category) => {
                    categories[category] = (categories[category] ? categories[category] : 0) + 1;
                });
            });
            const sortedCategoryCounts = Object.entries(categories)
                .map(([category, count]) => ({ label: category, count: count }))
                .sort((left, right) => (left.label < right.label ? -1 : 1));
            state.artefactModal.artefactsList = action.payload;
            state.artefactModal.categories = sortedCategoryCounts;
        },

        setSelectedArtefactDType: (state, action: PayloadAction<string | undefined>) => {
            state.artefactModal.selectedDType = action.payload || "all";
            state.artefactModal.isOpen = true;
        },

        setCachedArtefactProperty: (state, action: PayloadAction<IPluginDetails>) => {
            const key = action.payload.pluginId;
            state.artefactModal.cachedArtefactProperties[key] = action.payload;
        },

        setArtefactLoading: (state, action: PayloadAction<boolean>) => {
            state.artefactModal.loading = action.payload;
        },

        updateProjectTask: (state, action: PayloadAction<IProjectTaskUpdatePayload>) => {
            state.artefactModal.updateExistingTask = action.payload;
            state.artefactModal.isOpen = true;
        },

        setModalError: (state, action: PayloadAction<any>) => {
            state.artefactModal.error = action.payload;
        },
    },
    extraReducers: getExtraReducers(),
});
