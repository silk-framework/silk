import {
    datasetsLegacyApi,
    legacyApiEndpoint,
    projectApi,
    resourcesLegacyApi,
    workspaceApi,
} from "../../../utils/getApiEndpoint";
import fetch from "../../../services/fetch";
import qs from "qs";
import {
    IAutocompleteDefaultResponse,
    IDatasetConfigPreview,
    IDatasetPreview,
    IDatasetTypePayload,
    IItemInfo,
    IItemLink,
    IMetadataUpdatePayload,
    IPreviewResponse,
    IProjectMetadataResponse,
    IProjectTask,
    IRelatedItemsResponse,
    IRequestAutocompletePayload,
    IResourceListPayload,
    IResourceListResponse,
    IResourcePreview,
    ITaskMetadataResponse,
} from "@ducks/shared/typings";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";

/**
 * @private
 * @param error: HttpError
 */
const handleError = (error) => {
    return error.errorResponse;
};

/**
 * Default Endpoint to get autocompletion values
 * @param payload
 */
export const requestAutocompleteResults = async (
    payload: IRequestAutocompletePayload
): Promise<FetchResponse<IAutocompleteDefaultResponse> | never> => {
    try {
        const { data } = await fetch({
            url: workspaceApi(`/pluginParameterAutoCompletion`),
            method: "POST",
            body: payload,
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
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
    projectId?: string
): Promise<FetchResponse<ITaskMetadataResponse>> => {
    return fetch({
        url: legacyApiEndpoint(`/projects/${projectId}/tasks/${itemId}/metadata`),
    });
};

/**
 * Returns the task data for a specific project task.
 * @param projectId The project of the task.
 * @param itemId    The task ID
 * @param withLabel If true, then the returned JSON will contain optional labels in addition to the actual values, for presentation purposes.
 */
export const requestTaskData = async (
    projectId: string,
    itemId: string,
    withLabel: boolean = false
): Promise<IProjectTask> => {
    const queryParams: any = {};
    if (withLabel) {
        queryParams.withLabels = true;
    }

    try {
        const { data } = await fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/tasks/${itemId}`),
            body: queryParams,
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
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
): Promise<IRelatedItemsResponse> => {
    const query = qs.stringify(textQuery);
    try {
        const { data } = await fetch({
            url: workspaceApi(`/projects/${projectId}/tasks/${taskId}/relatedItems${query}`),
            body: {
                textQuery: textQuery,
            },
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
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

/** Fetches project task item information like the item type. */
export const requestTaskItemInfo = async (projectId: string, taskId: string): Promise<FetchResponse<IItemInfo>> => {
    return fetch({
        url: projectApi(`${projectId}/tasks/${taskId}/itemInfo`),
    });
};

export const requestResourcesList = async (
    projectId: string,
    filters: IResourceListPayload = {}
): Promise<FetchResponse<IResourceListResponse> | never> => {
    return fetch({
        url: legacyApiEndpoint(`/projects/${projectId}/resources`),
        body: filters,
    });
};

export const requestPreview = async (
    preview: IResourcePreview | IDatasetConfigPreview | IDatasetPreview
): Promise<FetchResponse<IPreviewResponse>> => {
    const url = (preview as IDatasetPreview).dataset ? datasetsLegacyApi("preview") : resourcesLegacyApi("preview");
    return fetch({
        url,
        method: "POST",
        body: preview,
    });
};

export const requestItemLinks = async (projectId: string, taskId: string): Promise<FetchResponse<IItemLink[]>> => {
    return fetch({
        url: workspaceApi(`/projects/${projectId}/tasks/${taskId}/links`),
    });
};
