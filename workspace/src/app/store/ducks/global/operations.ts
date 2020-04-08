import { authorize, getTokenFromStore, isAuthenticated, logout } from "./thunks/auth.thunk";
import { changeLocale } from "./thunks/locale.thunk";
import { globalSlice } from "@ducks/global/globalSlice";
import { batch } from "react-redux";
import { getApiEndpoint } from "../../../utils/getApiEndpoint";
import fetch from '../../../services/fetch';
import asModifier from "../../../utils/asModifier";
import { globalOp, globalSel } from "@ducks/global/index";
import { workspaceOp } from "@ducks/workspace";

const {
    addBreadcrumb, setError, fetchAvailableDTypes,
    updateAvailableDTypes, fetchArtefactsList, setArtefactsList,
    closeArtefactModal, selectArtefact, setProjectId, unsetProject
} = globalSlice.actions;

/**
 * Fetch types modifier
 */
const fetchAvailableDTypesAsync = (id?: string) => {
    return async dispatch => {
        batch(() => {
            dispatch(setError({}));
            dispatch(fetchAvailableDTypes());
        });
        try {
            const url = id ? `/searchConfig/types?projectId=${id}` : `/searchConfig/types`;

            const {data} = await fetch({
                url: getApiEndpoint(url),
            });
            const validModifier = asModifier(data.label, 'itemType', data.values);
            batch(() => {
                dispatch(updateAvailableDTypes({
                    fieldName: 'type',
                    modifier: validModifier
                }));
            });
        } catch (e) {
            dispatch(setError(e.response.data));
        }
    }
};

const fetchArtefactsListAsync = () => {
    return async dispatch => {
        dispatch(fetchArtefactsList());
        try {
            // @FIXME: Replace with correct plugins list
            const {data} = await fetch({
                url: '/dataintegration/core/plugins/org.silkframework.dataset.Dataset',
            });
            const result = Object.keys(data).map(key => ({
                key,
                ...data[key]
            }));

            dispatch(setArtefactsList(result));
        } catch (e) {
            dispatch(setError(e.response.data));
        }
    }
};

const createArtefactAsync = (formData) => {
    return (dispatch, getState) => {
        const {selectedArtefact} = globalSel.artefactModalSelector(getState());

        switch (selectedArtefact.key) {
            case "project":
                dispatch(workspaceOp.fetchCreateProjectAsync(formData));
                break;
            default:
                // @TODO: REMOVE LATER
                // @FIXME: currently backend accept only string values, so we need to transform it
                const requestData = {};
                Object.keys(formData).map(key => {
                    const value = formData[key];
                    if (typeof value === 'number' || typeof value === 'boolean') {
                        requestData[key] = ''+value;
                    } else if (typeof value === 'object') {
                        requestData[key] = JSON.stringify(value);
                    } else {
                        requestData[key] = value;
                    }
                });

                dispatch(workspaceOp.fetchCreateTaskAsync(requestData, selectedArtefact.key));
                console.warn('Artefact type not defined');
                break;
        }
    }
};

export default {
    changeLocale,
    isAuthenticated,
    getTokenFromStore,
    authorize,
    logout,
    addBreadcrumb,
    fetchAvailableDTypesAsync,
    fetchArtefactsListAsync,
    closeArtefactModal,
    selectArtefact,
    setProjectId,
    unsetProject,
    createArtefactAsync
};
