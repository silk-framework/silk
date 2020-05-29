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
    updateProjectTask,
    setCachedArtefactProperty,
    fetchArtefactsList,
    setArtefactsList,
    setArtefactLoading,
    setTaskId,
    unsetTaskId,
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
            let result = Object.keys(data).map((key) => ({
                key,
                ...data[key],
            }));

            if (filters.textQuery) {
                let labelsArray = [];
                let descriptionsArray = [];
                result.forEach((eachResult) => {
                    if (eachResult.title.toLowerCase().includes(filters.textQuery.toLowerCase())) {
                        labelsArray.push(eachResult);
                    } else {
                        descriptionsArray.push(eachResult);
                    }
                });
                result = labelsArray.concat(descriptionsArray);
            }

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

/** Builds a request object for project/task create call. */
const buildTaskObject = (formData: any): object => {
    const returnObject = {};
    const nestedParamsFlat = Object.entries(formData).filter(([k, v]) => k.includes("."));
    const directParams = Object.entries(formData).filter(([k, v]) => !k.includes("."));
    // Add direct parameters
    directParams.forEach(([paramId, param]) => {
        returnObject[paramId] = "" + param;
    });
    // Group nested parameters by first parameter ID, create nested value objects
    const nestedParamsMap = nestedParamsFlat.reduce((obj, [combinedParamId, param]) => {
        const firstDot = combinedParamId.indexOf(".");
        const paramId = combinedParamId.substring(0, firstDot);
        const nestedParamId = combinedParamId.substring(firstDot + 1);
        obj[paramId] = obj[paramId] || {};
        obj[paramId][nestedParamId] = param;
        return obj;
    }, {});
    // Add nested parameters to result object and call buildTaskObject recursively
    Object.entries(nestedParamsMap).forEach(([propName, value]) => {
        returnObject[propName] = buildTaskObject(value);
    });
    return returnObject;
};

const createArtefactAsync = (formData, taskType: string) => {
    return (dispatch, getState) => {
        const { selectedArtefact } = commonSel.artefactModalSelector(getState());

        switch (selectedArtefact.key) {
            case "project":
                dispatch(workspaceOp.fetchCreateProjectAsync(formData));
                break;
            default:
                dispatch(workspaceOp.fetchCreateTaskAsync(formData, selectedArtefact.key, taskType));
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
    updateProjectTask,
    setProjectId,
    unsetProject,
    setTaskId,
    unsetTaskId,
    setSelectedArtefactDType,
    buildTaskObject,
};
