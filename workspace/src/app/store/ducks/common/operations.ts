import { commonSlice } from "@ducks/common/commonSlice";
import { batch } from "react-redux";
import asModifier from "../../../utils/asModifier";
import {
    requestArtefactList,
    requestArtefactProperties,
    requestExportTypes,
    requestInitFrontend,
    requestSearchConfig,
} from "@ducks/common/requests";
import { AlternativeTaskUpdateFunction, IPluginOverview } from "@ducks/common/typings";
import { commonOp, commonSel } from "@ducks/common/index";
import { requestCreateProject, requestCreateTask, requestUpdateProjectTask } from "@ducks/workspace/requests";
import { routerOp } from "@ducks/router";
import { IProjectTask, TaskType } from "@ducks/shared/typings";
import { HttpError } from "../../../services/fetch/responseInterceptor";
import i18Instance, { fetchStoredLang } from "../../../../language";
import { URI_PROPERTY_PARAMETER_ID } from "../../../views/shared/modals/CreateArtefactModal/ArtefactForms/UriAttributeParameterInput";
import utils from "../../../views/shared/Metadata/MetadataUtils";
import { Keyword } from "@ducks/workspace/typings";
import { MultiSuggestFieldSelectionProps } from "@eccenca/gui-elements/src/components/MultiSelect/MultiSelect";
import { READ_ONLY_PARAMETER } from "../../../views/shared/modals/CreateArtefactModal/ArtefactForms/TaskForm";
import { fillCustomPluginStore } from "../../../views/shared/ItemDepiction/ItemDepiction";
import { TaskParameters } from "../../../views/plugins/plugin.types";
import { setDefaultProjectPageSuffix } from "../../../utils/routerUtils";

const {
    setError,
    fetchAvailableDTypes,
    updateAvailableDTypes,
    setProjectId,
    setInitialSettings,
    setTaskPluginOverviews,
    setSelectedArtefactDType,
    closeArtefactModal,
    selectArtefact,
    updateProjectTask,
    createNewTask,
    setCachedArtefactProperty,
    fetchArtefactsList,
    setArtefactsList,
    setArtefactLoading,
    setTaskId,
    setModalError,
    setModalInfo,
    setExportTypes,
    changeLanguage,
    toggleNotificationMenuDisplay,
    toggleUserMenuDisplay,
} = commonSlice.actions;

const fetchCommonSettingsAsync = () => {
    return async (dispatch) => {
        try {
            const data = await requestInitFrontend();
            data.defaultProjectPageSuffix && setDefaultProjectPageSuffix(data.defaultProjectPageSuffix);
            dispatch(setInitialSettings(data));

            const overviewItems = await requestArtefactList({});
            const taskPluginOverviews = Object.keys(overviewItems).map((key) => ({
                key,
                ...overviewItems[key],
            }));
            await fillCustomPluginStore(taskPluginOverviews);
            dispatch(setTaskPluginOverviews(taskPluginOverviews));

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
                    }),
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

/** Splits the form data into normal parameters/values and variable template parameters/values. */
const splitParameterAndVariableTemplateParameters = (formData: any, variableTemplateParameterSet: Set<string>) => {
    const parameters: Record<string, any> = Object.create(null);
    const variableTemplateParameters: Record<string, any> = {};
    Object.entries(formData).forEach(([key, value]) => {
        if (variableTemplateParameterSet.has(key)) {
            variableTemplateParameters[key] = value;
        } else {
            parameters[key] = value;
        }
    });
    return {
        parameters,
        variableTemplateParameters,
    };
};

/** Builds a request object for project/task create call. */
const buildNestedTaskParameterObject = (formData: Record<string, any>): TaskParameters => {
    const returnObject: TaskParameters = Object.create(null);
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
        returnObject[propName] = buildNestedTaskParameterObject(value as Record<string, any>);
    });
    return returnObject;
};

type ArtefactDataParameters = {
    [key: string]: string;
};

const createArtefactAsync = (
    formData,
    taskType: TaskType | "Project",
    dataParameters: ArtefactDataParameters | undefined,
    // Parameters that are flagged to have variable template value
    variableTemplateParameterSet: Set<string>,
    /** If this is set, then instead of redirecting to the newly created task, this function is called. */
    alternativeCallback?: (newTask: IProjectTask) => any,
) => {
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
                            fetchCreateTaskAsync(
                                formData,
                                selectedArtefact.key,
                                taskType as TaskType,
                                dataParameters,
                                variableTemplateParameterSet,
                                alternativeCallback,
                            ),
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
    tags?: MultiSuggestFieldSelectionProps<Keyword>;
    projectId?: string;
    taskId?: string;
}) => {
    if (!payload.tags?.createdItems.length || !payload.tags.selectedItems.length) return;
    const tags = await utils.getSelectedTagsAndCreateNew(
        payload.tags?.createdItems,
        payload.projectId,
        payload.tags?.selectedItems,
    );

    await utils.updateMetaData(
        {
            label: payload.label,
            description: payload.description,
            tags: tags ?? [],
        },
        payload.projectId,
        payload.taskId,
    );
};

/** Extracts form attributes that should be added to the data object directly instead of the parameter object. */
const extractDataAttributes = (formData): ArtefactDataParameters => {
    const returnValue: ArtefactDataParameters = Object.create(null);
    returnValue[URI_PROPERTY_PARAMETER_ID] = formData[URI_PROPERTY_PARAMETER_ID];
    returnValue[READ_ONLY_PARAMETER] = formData[READ_ONLY_PARAMETER];
    return returnValue;
};

const fetchCreateTaskAsync = (
    formData: any,
    artefactId: string,
    taskType: TaskType,
    dataParameters: { [key: string]: string } | undefined,
    // Parameters that are flagged to have variable template value
    variableTemplateParameterSet: Set<string>,
    /** If this is set, then instead of redirecting to the newly created task, this function is called. */
    alternativeCallback?: (newTask: IProjectTask) => any,
) => {
    return async (dispatch, getState) => {
        const currentProjectId = commonSel.currentProjectIdSelector(getState());
        const { label, description, id, tags, ...restFormData } = formData;
        const { parameters, variableTemplateParameters } = splitParameterAndVariableTemplateParameters(
            restFormData,
            variableTemplateParameterSet,
        );
        const parameterData = buildNestedTaskParameterObject(parameters);
        const variableTemplateData = buildNestedTaskParameterObject(variableTemplateParameters);
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
                    ...parameterData,
                },
                templates: {
                    ...variableTemplateData,
                },
            },
        };

        dispatch(setModalError({}));
        try {
            const data = await requestCreateTask(payload, currentProjectId);
            const newTask = data.data;
            const newTaskId = newTask.id;
            await createTagsAndAddToMetadata({
                label,
                description,
                tags,
                projectId: currentProjectId,
                taskId: newTaskId,
            });

            batch(() => {
                dispatch(closeArtefactModal());
                alternativeCallback
                    ? alternativeCallback(newTask)
                    : dispatch(
                          routerOp.goToTaskPage({
                              id: newTaskId,
                              type: taskType,
                              projectId: currentProjectId,
                              label,
                          }),
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
    dataParameters: ArtefactDataParameters | undefined,
    // Parameters that are flagged to have variable template value
    variableTemplateParameterSet: Set<string>,
    /** Function that is called instead of the task PATCH endpoint. */
    alternativeUpdateFunction?: AlternativeTaskUpdateFunction,
) => {
    return async (dispatch) => {
        const { parameters, variableTemplateParameters } = splitParameterAndVariableTemplateParameters(
            formData,
            variableTemplateParameterSet,
        );
        const parameterData = buildNestedTaskParameterObject(parameters);
        const variableTemplateData = buildNestedTaskParameterObject(variableTemplateParameters);
        const payload = {
            data: {
                ...dataParameters,
                parameters: {
                    ...parameterData,
                },
                templates: {
                    ...variableTemplateData,
                },
            },
        };
        dispatch(setModalError({}));
        try {
            if (alternativeUpdateFunction) {
                await alternativeUpdateFunction(projectId, itemId, parameterData, variableTemplateData, dataParameters);
            } else {
                await requestUpdateProjectTask(projectId, itemId, payload);
            }
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
    tags?: MultiSuggestFieldSelectionProps<Keyword>;
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
        batch(() => {
            dispatch(selectArtefact(undefined));
            dispatch(setModalError({}));
            dispatch(setModalInfo(undefined));
            if (shouldClose) {
                dispatch(closeArtefactModal());
            }
        });
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
    createNewTask,
    setProjectId,
    setTaskId,
    setSelectedArtefactDType,
    setModalError,
    setModalInfo,
    buildNestedTaskParameterObject: buildNestedTaskParameterObject,
    fetchCreateTaskAsync,
    fetchUpdateTaskAsync,
    fetchCreateProjectAsync,
    resetArtefactModal,
    fetchExportTypesAsync,
    extractDataAttributes,
    splitParameterAndVariableTemplateParameters,
    toggleNotificationMenuDisplay,
    toggleUserMenuDisplay,
};

export default commonOps;
