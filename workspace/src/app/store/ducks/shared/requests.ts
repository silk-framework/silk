import { legacyApiEndpoint, projectApi, workspaceApi } from "../../../utils/getApiEndpoint";
import fetch from "../../../services/fetch";
import { AxiosResponse } from "axios";
import qs from "qs";
import { generateNetworkError, isNetworkError } from "../../../services/errorLogger";
import {
    IAutocompleteDefaultResponse,
    IDatasetTypePayload,
    IMetadataUpdatePayload,
    IProjectMetadataResponse,
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
): Promise<IAutocompleteDefaultResponse> => {
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

export const requestRelatedItems = async (
    projectId: string,
    taskId: string,
    textQuery: string = ""
): Promise<IRelatedItemsResponse> => {
    const query = qs.stringify(textQuery);
    try {
        const { data } = await fetch({
            url: workspaceApi(`/projects/${projectId}/tasks/${taskId}/relatedItems${query}`),
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
