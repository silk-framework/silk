import { authorize, getTokenFromStore, isAuthenticated, logout } from "./thunks/auth.thunk";
import { changeLocale } from "./thunks/locale.thunk";
import { commonSlice } from "@ducks/common/commonSlice";
import { batch } from "react-redux";
import { workspaceApi } from "../../../utils/getApiEndpoint";
import fetch from '../../../services/fetch';
import asModifier from "../../../utils/asModifier";
import { createArtefactAsync, fetchArtefactsListAsync, getArtefactPropertiesAsync } from "./thunks/artefactModal.thunk";

const {
    setError, fetchAvailableDTypes, updateAvailableDTypes, setProjectId,
    unsetProject, setInitialSettings,
    setSelectedArtefactDType,
    closeArtefactModal,
    selectArtefact,
} = commonSlice.actions;

const fetchCommonSettingsAsync = () => {
    return async dispatch => {
        try {
            const {data} = await fetch({
                url: workspaceApi('/initFrontend'),
            });
            setInitialSettings(data);
        } catch (e) {
            dispatch(setError(e.response.data));
        }
    };
};
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
                url: workspaceApi(url),
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

export default {
    changeLocale,
    isAuthenticated,
    getTokenFromStore,
    authorize,
    logout,
    fetchAvailableDTypesAsync,
    fetchArtefactsListAsync,
    createArtefactAsync,
    fetchCommonSettingsAsync,
    getArtefactPropertiesAsync,
    closeArtefactModal,
    selectArtefact,
    setProjectId,
    unsetProject,
    setSelectedArtefactDType
};
