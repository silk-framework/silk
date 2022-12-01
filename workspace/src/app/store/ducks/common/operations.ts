import { commonSlice } from "@ducks/common/commonSlice";
import { commonOp, commonSel } from "@ducks/common/index";
import {
    requestArtefactList,
    requestArtefactProperties,
    requestExportTypes,
    requestInitFrontend,
    requestSearchConfig,
} from "@ducks/common/requests";
import { IPluginOverview } from "@ducks/common/typings";
import { routerOp } from "@ducks/router";
import { TaskType } from "@ducks/shared/typings";
import { requestCreateProject, requestCreateTask, requestUpdateProjectTask } from "@ducks/workspace/requests";
import { Keyword } from "@ducks/workspace/typings";
import { SelectedParamsType } from "@eccenca/gui-elements/src/components/MultiSelect/MultiSelect";
import { batch } from "react-redux";

import i18Instance, { fetchStoredLang } from "../../../../language";
import { HttpError } from "../../../services/fetch/responseInterceptor";
import asModifier from "../../../utils/asModifier";
import utils from "../../../views/shared/Metadata/MetadataUtils";
import { URI_PROPERTY_PARAMETER_ID } from "../../../views/shared/modals/CreateArtefactModal/ArtefactForms/UriAttributeParameterInput";

const {
    setError,
    fetchAvailableDTypes,
    updateAvailableDTypes,
    setProjectId,
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
    setModalError,
    setExportTypes,
    changeLanguage,
} = commonSlice.actions;

const fetchCommonSettingsAsync = () => {
    return async (dispatch) => {
        try {
            const data = await requestInitFrontend();
            dispatch(setInitialSettings(data));

            const selectedLng = fetchStoredLang();
            if (!selectedLng) {
                dispatch(changeLocale(data.initialLanguage));
            } else {
                // Just make sure that specific flags for DM are set
                dispatch(changeLocale(selectedLng));
            }
        } catch (error) {
            dispatch(setError(error));
        }
    };
};

const fetchExportTypesAsync = () => {
    return async (dispatch) => {
        try {
            const data = await requestExportTypes();
            dispatch(setExportTypes(data));
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

/** Resets the artefacts list to 0 elements. */
const resetArtefactsList = () => {
    return async (dispatch) => {
        dispatch(setArtefactsList([]));
    };
};

const getArtefactPropertiesAsync = (artefact: IPluginOverview) => {
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

type ArtefactDataParameters = {
    [key: string]: string;
};

const createArtefactAsync = (formData, taskType: TaskType | "Project", dataParameters?: ArtefactDataParameters) => {
    return async (dispatch, getState) => {
        const { selectedArtefact } = commonSel.artefactModalSelector(getState());

        switch (taskType) {
            case "Project":
                await dispatch(fetchCreateProjectAsync(formData));
                break;
            default:
                if (selectedArtefact) {
                    selectedArtefact &&
                        (await dispatch(
                            fetchCreateTaskAsync(formData, selectedArtefact.key, taskType as TaskType, dataParameters)
                        ));
                } else {
                    console.error("selectedArtefact not set! Cannot create item.");
                }
                break;
        }
    };
};

/** creates new tags for specific taskType and updates the metadata */
const createTagsAndAddToMetadata = async (payload: {
    label: string;
    description?: string;
    tags?: SelectedParamsType<Keyword>;
    projectId?: string;
    taskId?: string;
}) => {
    if (!payload.tags?.createdItems.length || !payload.tags.selectedItems.length) return;
    const tags = await utils.getSelectedTagsAndCreateNew(
        payload.tags?.createdItems,
        payload.projectId,
        payload.tags?.selectedItems
    );

    await utils.updateMetaData(
        {
            label: payload.label,
            description: payload.description,
            tags: tags ?? [],
        },
        payload.projectId,
        payload.taskId
    );
};

/** Extracts form attributes that should be added to the data object directly instead of the parameter object. */
const extractDataAttributes = (formData): ArtefactDataParameters => {
    let returnValue: ArtefactDataParameters = {};
    const uriAttribute = formData[URI_PROPERTY_PARAMETER_ID];
    returnValue = {};
    returnValue[URI_PROPERTY_PARAMETER_ID] = uriAttribute;
    return returnValue;
};

const fetchCreateTaskAsync = (
    formData: any,
    artefactId: string,
    taskType: TaskType,
    dataParameters?: { [key: string]: string }
) => {
    return async (dispatch, getState) => {
        const currentProjectId = commonSel.currentProjectIdSelector(getState());
        const { label, description, id, tags, ...restFormData } = formData;
        const requestData = buildTaskObject(restFormData);
        const metadata = {
            label,
            description,
        };

        const payload = {
            metadata,
            id,
            data: {
                ...dataParameters,
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
            await createTagsAndAddToMetadata({
                label,
                description,
                tags,
                projectId: currentProjectId,
                taskId: data.data.id,
            });
            batch(() => {
                dispatch(closeArtefactModal());
                dispatch(
                    routerOp.goToTaskPage({
                        id: data.data.id,
                        type: taskType,
                        projectId: currentProjectId,
                        label,
                    })
                );
            });
        } catch (e) {
            if (e.isFetchError) {
                dispatch(setModalError((e as HttpError).errorResponse));
            }
        }
    };
};

/** Updates the technical parameters of a project task. */
const fetchUpdateTaskAsync = (
    projectId: string,
    itemId: string,
    formData: any,
    dataParameters?: ArtefactDataParameters
) => {
    return async (dispatch) => {
        const requestData = buildTaskObject(formData);
        const payload = {
            data: {
                ...dataParameters,
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

const fetchCreateProjectAsync = (formData: {
    label: string;
    description?: string;
    id?: string;
    tags?: SelectedParamsType<Keyword>;
}) => {
    return async (dispatch) => {
        dispatch(setModalError({}));
        const { label, description, id, tags } = formData;
        const payload = {
            metaData: {
                label,
                description,
            },
        };
        if (id) payload["id"] = id;
        try {
            const data = await requestCreateProject(payload);
            await createTagsAndAddToMetadata({ label, description, tags, projectId: data.name });
            // Added project, workspace state may have changed
            dispatch(commonOp.fetchCommonSettingsAsync());
            dispatch(closeArtefactModal());
            dispatch(routerOp.goToPage(`projects/${data.name}`, { projectLabel: label, itemType: "project" }));
        } catch (e) {
            dispatch(setModalError(e.response.data));
        }
    };
};

const resetArtefactModal =
    (shouldClose: boolean = false) =>
    (dispatch) => {
        dispatch(selectArtefact(undefined));
        dispatch(setModalError({}));
        if (shouldClose) {
            dispatch(closeArtefactModal());
        }
    };

const changeLocale = (locale: string) => {
    return async (dispatch) => {
        await i18Instance.changeLanguage(locale);
        dispatch(changeLanguage(locale));
    };
};

const commonOps = {
    changeLocale,
    fetchAvailableDTypesAsync,
    fetchArtefactsListAsync,
    resetArtefactsList,
    createArtefactAsync,
    fetchCommonSettingsAsync,
    getArtefactPropertiesAsync,
    closeArtefactModal,
    selectArtefact,
    updateProjectTask,
    setProjectId,
    setTaskId,
    setSelectedArtefactDType,
    setModalError,
    buildTaskObject,
    fetchCreateTaskAsync,
    fetchUpdateTaskAsync,
    fetchCreateProjectAsync,
    resetArtefactModal,
    fetchExportTypesAsync,
    extractDataAttributes,
};

export default commonOps;
