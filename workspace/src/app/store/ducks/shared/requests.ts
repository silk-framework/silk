import { legacyApiEndpoint, projectApi, workspaceApi } from "../../../utils/getApiEndpoint";
import fetch, { handleRequest, FetchReponse } from "../../../services/fetch";
import { AxiosResponse } from "axios";
import qs from "qs";
import { generateNetworkError, isNetworkError } from "../../../services/errorLogger";
import {
    IAutocompleteDefaultResponse,
    IDatasetTypePayload,
    IMetadataUpdatePayload,
    IProjectMetadataResponse,
    IProjectTask,
    IRelatedItemsResponse,
    IRequestAutocompletePayload,
    IResourceListPayload,
    IResourceListResponse,
    ITaskMetadataResponse,
} from "@ducks/shared/typings";

/**
 * @private
 * @param response
 */
const handleError = ({ response }) => {
    if (isNetworkError(response.data)) {
        return generateNetworkError(response.data);
    }
    return response.data;
};

/**
 * Default Endpoint to get autocompletion values
 * @param payload
 */
export const requestAutocompleteResults = async (
    payload: IRequestAutocompletePayload
): Promise<FetchReponse<IAutocompleteDefaultResponse>> => {
    return handleRequest(
        fetch({
            url: workspaceApi(`/pluginParameterAutoCompletion`),
            method: "POST",
            body: payload,
        })
    );
};

/**
 * Get Project Metadata
 * @param itemId
 */
export const requestProjectMetadata = async (itemId: string): Promise<IProjectMetadataResponse> => {
    try {
        const { data } = await fetch({
            url: projectApi(`/${itemId}/metaData`),
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

export const requestTaskMetadata = async (itemId: string, projectId?: string): Promise<ITaskMetadataResponse> => {
    try {
        const { data } = await fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/tasks/${itemId}/metadata`),
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
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
): Promise<FetchReponse<IProjectTask>> => {
    const queryParams: any = {};
    if (withLabel) {
        queryParams.withLabels = true;
    }

    return handleRequest(
        fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/tasks/${itemId}`),
            body: queryParams,
        })
    );
};

/**
 * Updates the meta data of a project.
 * @param itemId
 * @param payload
 */
export const requestUpdateProjectMetadata = async (
    itemId: string,
    payload: IMetadataUpdatePayload
): Promise<IProjectMetadataResponse> => {
    try {
        const { data }: AxiosResponse<IProjectMetadataResponse> = await fetch({
            url: workspaceApi(`/projects/${itemId}/metaData`),
            method: "PUT",
            body: payload,
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
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
): Promise<ITaskMetadataResponse> => {
    try {
        const { data }: AxiosResponse<ITaskMetadataResponse> = await fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/tasks/${itemId}/metadata`),
            method: "PUT",
            body: payload,
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
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
): Promise<string[]> => {
    try {
        const { data } = await fetch({
            url: legacyApiEndpoint(`projects/${projectId}/datasets/${datasetId}/types`),
            method: "GET",
            body: payload,
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

export const requestResourcesList = async (
    projectId: string,
    filters: IResourceListPayload = {}
): Promise<IResourceListResponse | never> => {
    try {
        const { data } = await fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/resources`),
            body: filters,
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};
