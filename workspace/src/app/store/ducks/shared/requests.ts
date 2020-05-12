import { legacyApiEndpoint, projectApi, resourcesLegacyApi, workspaceApi } from "../../../utils/getApiEndpoint";
import fetch from "../../../services/fetch";
import { AxiosResponse } from "axios";
import qs from "qs";
import { generateNetworkError, isNetworkError } from "../../../services/errorLogger";
import {
    IDatasetConfigPreview,
    IDatasetPreview,
    IDatasetTypePayload,
    IMetadataUpdatePayload,
    IPreviewResponse,
    IProjectMetadataResponse,
    IRelatedItemsResponse,
    IRequestAutocompletePayload,
    IResourcePreview,
    ITaskMetadataResponse,
} from "@ducks/shared/typings";

const handleError = ({ response }) => {
    if (isNetworkError(response.data)) {
        return generateNetworkError(response.data);
    }
    return response.data;
};

export const requestAutocompleteResults = async (payload: IRequestAutocompletePayload) => {
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

export const requestDatasetPreview = async (
    preview: IResourcePreview | IDatasetConfigPreview | IDatasetPreview
): Promise<IPreviewResponse> => {
    try {
        const { data } = await fetch({
            url: resourcesLegacyApi("preview"),
            method: "POST",
            body: preview,
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
