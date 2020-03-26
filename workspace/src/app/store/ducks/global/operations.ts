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
    closeArtefactModal, selectArtefact
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
        const artefactType = globalSel.artefactModalSelector(getState()).selectedArtefact;
        switch (artefactType) {
            case "project":
                dispatch(workspaceOp.fetchCreateProjectAsync(formData));
                break;
            default:
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
    createArtefactAsync
};
