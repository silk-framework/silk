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
import { requestCreateProject, requestCreateTask, requestUpdateProjectTask } from "@ducks/workspace/requests";
import { routerOp } from "@ducks/router";

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
    setModalError,
} = commonSlice.actions;

const itemTypeToPathMap = {
    Transform: "transform",
    Linking: "linking",
    Workflow: "workflow",
    CustomTask: "task",
    Dataset: "dataset",
};

const itemTypeToPath = (itemType: string) => {
    if (itemTypeToPathMap[itemType]) {
        return itemTypeToPathMap[itemType];
    } else {
        return "task";
    }
};

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
            let textQuery = filters.textQuery.toLowerCase();
            let result = Object.keys(data).map((key) => ({
                key,
                ...data[key],
            }));

            if (filters.textQuery) {
                result.sort((a, b) => {
                    let titleA = a.title.toLowerCase(), titleB = b.title.toLowerCase();
                    
                    if (titleA.includes(textQuery) && !titleB.includes(textQuery))
                        return -1;
                    return 0
                })
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
                dispatch(fetchCreateProjectAsync(formData));
                break;
            default:
                dispatch(fetchCreateTaskAsync(formData, selectedArtefact.key, taskType));
                break;
        }
    };
};

const fetchCreateTaskAsync = (formData: any, artefactId: string, taskType: string) => {
    return async (dispatch, getState) => {
        const currentProjectId = commonSel.currentProjectIdSelector(getState());
        const { label, description, ...restFormData } = formData;
        const requestData = buildTaskObject(restFormData);
        const metadata = {
            label,
            description,
        };

        const payload = {
            metadata,
            data: {
                taskType: taskType,
                type: artefactId,
                parameters: {
                    ...requestData,
                },
            },
        };

        dispatch(setModalError({}));

        try {
            const data = await requestCreateTask(payload, currentProjectId);
            batch(() => {
                dispatch(closeArtefactModal());
                dispatch(
                    routerOp.goToPage(`projects/${currentProjectId}/${itemTypeToPath(taskType)}/${data.id}`, {
                        taskLabel: label,
                    })
                );
            });
        } catch (e) {
            dispatch(setModalError(e));
        }
    };
};

/** Updates the technical parameters of a project task. */
const fetchUpdateTaskAsync = (projectId: string, itemId: string, formData: any) => {
    return async (dispatch) => {
        const requestData = buildTaskObject(formData);
        const payload = {
            data: {
                parameters: {
                    ...requestData,
                },
            },
        };
        dispatch(setModalError({}));
        try {
            await requestUpdateProjectTask(projectId, itemId, payload);
            dispatch(closeArtefactModal());
        } catch (e) {
            dispatch(setModalError(e));
        }
    };
};

const fetchCreateProjectAsync = (formData: { label: string; description?: string }) => {
    return async (dispatch) => {
        dispatch(setModalError({}));
        const { label, description } = formData;
        try {
            const data = await requestCreateProject({
                metaData: {
                    label,
                    description,
                },
            });
            dispatch(closeArtefactModal());
            dispatch(routerOp.goToPage(`projects/${data.name}`, { projectLabel: label }));
        } catch (e) {
            dispatch(setModalError(e.response.data));
        }
    };
};

const resetArtefactModal = (shouldClose: boolean = false) => (dispatch) => {
    dispatch(selectArtefact(null));
    dispatch(setModalError({}));
    if (shouldClose) {
        dispatch(closeArtefactModal());
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
    itemTypeToPath,
    closeArtefactModal,
    selectArtefact,
    updateProjectTask,
    setProjectId,
    unsetProject,
    setTaskId,
    unsetTaskId,
    setSelectedArtefactDType,
    setModalError,
    buildTaskObject,
    fetchCreateTaskAsync,
    fetchUpdateTaskAsync,
    fetchCreateProjectAsync,
    resetArtefactModal,
};
