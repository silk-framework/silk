import { legacyApiEndpoint, projectApi, rootPath, workspaceApi } from "../../../utils/getApiEndpoint";
import fetch from "../../../services/fetch";
import qs from "qs";
import {
    IArbitraryPluginParameters,
    IAutocompleteDefaultResponse,
    IDatasetTypePayload,
    IItemLink,
    IMetadataUpdatePayload,
    IProjectMetadataResponse,
    IProjectResource,
    IProjectTask,
    IRelatedItemsResponse,
    IRequestAutocompletePayload,
    IResourceListPayload,
    ITaskMetadataResponse,
} from "@ducks/shared/typings";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { DatasetCharacteristics } from "../../../views/shared/typings";

/**
 * Default Endpoint to get autocompletion values
 * @param payload
 */
export const requestAutocompleteResults = async (
    payload: IRequestAutocompletePayload
): Promise<FetchResponse<IAutocompleteDefaultResponse[]>> => {
    return fetch({
        url: workspaceApi(`/pluginParameterAutoCompletion`),
        method: "POST",
        body: payload,
    });
};

/**
 * Get Project Metadata
 * @param itemId
 */
export const requestProjectMetadata = async (itemId: string): Promise<FetchResponse<IProjectMetadataResponse>> => {
    return fetch({
        url: projectApi(`/${itemId}/metaData`),
    });
};

export const requestTaskMetadata = async (
    itemId: string,
    projectId: string,
    withTaskLinks?: boolean
): Promise<FetchResponse<ITaskMetadataResponse>> => {
    return fetch({
        url: legacyApiEndpoint(`/projects/${projectId}/tasks/${itemId}/metadata`),
        query: withTaskLinks
            ? {
                  withTaskLinks: true,
              }
            : undefined,
    });
};

/**
 * Returns the task data for a specific project task.
 * @param projectId The project of the task.
 * @param itemId    The task ID
 * @param withLabel If true, then the returned JSON will contain optional labels in addition to the actual values, for presentation purposes.
 */
export const requestTaskData = async <TASK_PARAMETERS = IArbitraryPluginParameters>(
    projectId: string,
    itemId: string,
    withLabel: boolean = false
): Promise<FetchResponse<IProjectTask<TASK_PARAMETERS>>> => {
    const queryParams: any = Object.create(null);
    if (withLabel) {
        queryParams.withLabels = true;
    }

    return fetch({
        url: legacyApiEndpoint(`/projects/${projectId}/tasks/${itemId}`),
        body: queryParams,
    });
};

/**
 * Updates the meta data of a project.
 * @param itemId
 * @param payload
 */
export const requestUpdateProjectMetadata = async (
    itemId: string,
    payload: IMetadataUpdatePayload
): Promise<FetchResponse<IProjectMetadataResponse>> => {
    return fetch({
        url: workspaceApi(`/projects/${itemId}/metaData`),
        method: "PUT",
        body: payload,
    });
};

/**
 * Updates project task meta data.
 * @param itemId    The ID of the task.
 * @param payload   The meta data object.
 * @param projectId The project of the task.
 */
export const requestUpdateTaskMetadata = async (
    itemId: string,
    payload: IMetadataUpdatePayload,
    projectId?: string
): Promise<FetchResponse<ITaskMetadataResponse>> => {
    return fetch({
        url: legacyApiEndpoint(`/projects/${projectId}/tasks/${itemId}/metadata`),
        method: "PUT",
        body: payload,
    });
};

/**
 * Returns related items of a task
 * @param projectId The project of the task
 * @param taskId The ID of the project task.
 * @param textQuery A multi-word text query to filter the related items by.
 */
export const requestRelatedItems = async (
    projectId: string,
    taskId: string,
    textQuery: string = ""
): Promise<FetchResponse<IRelatedItemsResponse>> => {
    const query = qs.stringify(textQuery);
    return fetch({
        url: workspaceApi(`/projects/${projectId}/tasks/${taskId}/relatedItems${query}`),
        body: {
            textQuery: textQuery,
        },
    });
};

export const requestDatasetTypes = async (
    datasetId: string,
    projectId: string,
    payload: IDatasetTypePayload = {}
): Promise<FetchResponse<string[]>> => {
    return fetch({
        url: legacyApiEndpoint(`projects/${projectId}/datasets/${datasetId}/types`),
        method: "GET",
        body: payload,
    });
};

export const requestResourcesList = async (
    projectId: string,
    filters: IResourceListPayload = {}
): Promise<FetchResponse<IProjectResource[]>> => {
    return fetch({
        url: legacyApiEndpoint(`/projects/${projectId}/resources`),
        body: filters,
    });
};

export const requestItemLinks = async (projectId: string, taskId: string): Promise<FetchResponse<IItemLink[]>> => {
    return fetch({
        url: workspaceApi(`/projects/${projectId}/tasks/${taskId}/links`),
    });
};

/** Fetches dataset characteristics for a specific dataset. */
export const requestDatasetCharacteristics = async (
    projectId: string,
    datasetId: string
): Promise<FetchResponse<DatasetCharacteristics>> => {
    return fetch({
        url: projectApi(`/${projectId}/datasets/${datasetId}/characteristics`),
    });
};

export const performAction = (projectId: string, task: string, actionKey: string) => {
    return fetch({
        url: rootPath(`/workspace/projects/${projectId}/tasks/${task}/action/${actionKey}`),
        method: "POST",
        body: {},
    });
};
