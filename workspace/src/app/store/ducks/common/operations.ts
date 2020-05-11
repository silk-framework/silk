import { authorize, getTokenFromStore, isAuthenticated, logout } from "./thunks/auth.thunk";
import { changeLocale } from "./thunks/locale.thunk";
import { commonSlice } from "@ducks/common/commonSlice";
import { batch } from "react-redux";
import asModifier from "../../../utils/asModifier";
import {
    requestArtefactList,
    requestArtefactProperties,
    requestInitFrontend,
    requestSearchConfig,
} from "@ducks/common/requests";
import { IArtefactItem } from "@ducks/common/typings";
import { commonSel } from "@ducks/common/index";
import { workspaceOp } from "@ducks/workspace";

const {
    setError,
    fetchAvailableDTypes,
    updateAvailableDTypes,
    setProjectId,
    unsetProject,
    setInitialSettings,
    setSelectedArtefactDType,
    closeArtefactModal,
    selectArtefact,
    setCachedArtefactProperty,
    fetchArtefactsList,
    setArtefactsList,
    setArtefactLoading,
} = commonSlice.actions;

const fetchCommonSettingsAsync = () => {
    return async (dispatch) => {
        try {
            const data = await requestInitFrontend();
            setInitialSettings(data);
        } catch (error) {
            dispatch(setError(error));
        }
    };
};

/**
 * Fetch types modifier
 */
const fetchAvailableDTypesAsync = (id?: string) => {
    return async (dispatch) => {
        batch(() => {
            dispatch(setError({}));
            dispatch(fetchAvailableDTypes());
        });

        try {
            const data = await requestSearchConfig(id);
            const validModifier = asModifier(data.label, "itemType", data.values);

            batch(() => {
                dispatch(
                    updateAvailableDTypes({
                        fieldName: "type",
                        modifier: validModifier,
                    })
                );
            });
        } catch (error) {
            dispatch(setError(error));
        }
    };
};

const fetchArtefactsListAsync = (filters: any = {}) => {
    return async (dispatch) => {
        batch(() => {
            dispatch(fetchArtefactsList());
            dispatch(setArtefactLoading(true));
        });

        try {
            const data = await requestArtefactList(filters);
            const result = Object.keys(data).map((key) => ({
                key,
                ...data[key],
            }));

            dispatch(setArtefactsList(result));
        } catch (e) {
            dispatch(setError(e));
        } finally {
            dispatch(setArtefactLoading(false));
        }
    };
};

const getArtefactPropertiesAsync = (artefact: IArtefactItem) => {
    return async (dispatch, getState) => {
        const { cachedArtefactProperties } = commonSel.artefactModalSelector(getState());
        dispatch(selectArtefact(artefact));

        if (!cachedArtefactProperties[artefact.key]) {
            dispatch(setArtefactLoading(true));

            const data = await requestArtefactProperties(artefact.key);
            batch(() => {
                dispatch(setArtefactLoading(false));
                dispatch(setCachedArtefactProperty(data));
            });
        }
    };
};

const createArtefactAsync = (formData) => {
    return (dispatch, getState) => {
        const { selectedArtefact } = commonSel.artefactModalSelector(getState());

        switch (selectedArtefact.key) {
            case "project":
                dispatch(workspaceOp.fetchCreateProjectAsync(formData));
                break;
            default:
                // @TODO: REMOVE LATER
                // @FIXME: currently backend accept only string values, so we need to transform it
                const requestData = {};
                Object.keys(formData).forEach((key) => {
                    const value = formData[key];
                    if (typeof value === "number" || typeof value === "boolean") {
                        requestData[key] = "" + value;
                    } else if (typeof value === "object") {
                        requestData[key] = JSON.stringify(value);
                    } else {
                        requestData[key] = value;
                    }
                });

                dispatch(workspaceOp.fetchCreateTaskAsync(requestData, selectedArtefact.key));
                console.warn("Artefact type not defined");
                break;
        }
    };
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
    setSelectedArtefactDType,
};
