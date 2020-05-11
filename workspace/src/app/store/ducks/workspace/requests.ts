import { IAppliedFacetState, IFacetState, ISorterListItemState } from "@ducks/workspace/typings";
import fetch from "../../../services/fetch";
import { legacyApiEndpoint, workspaceApi } from "../../../utils/getApiEndpoint";
import { generateNetworkError, isNetworkError } from "../../../services/errorLogger";
import { VoidOrNever } from "../../../../app";

export interface ISearchListRequest {
    limit: number;
    offset: number;
    sortBy?: string;
    sortOrder?: string;
    project?: string;
    facets?: IAppliedFacetState[];
}

export interface ISearchListResponse {
    total: number;
    facets: IFacetState[];
    results: any[];
    sortByProperties: ISorterListItemState[];
}

export interface ICreateProjectPayload {
    metaData: {
        label: string;
        description?: string;
    };
}

const handleError = ({ response }) => {
    if (isNetworkError(response.data)) {
        return generateNetworkError(response.data);
    }
    return response.data;
};

export const requestSearchList = async (payload: ISearchListRequest): Promise<ISearchListResponse | never> => {
    try {
        const { data } = await fetch({
            url: workspaceApi("/searchItems"),
            method: "post",
            body: payload,
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

export const requestRemoveProject = async (itemId: string): Promise<VoidOrNever> => {
    try {
        await fetch({
            url: legacyApiEndpoint(`/projects/${itemId}`),
            method: "DELETE",
        });
    } catch (e) {
        throw handleError(e);
    }
};

export const requestRemoveTask = async (itemId: string, projectId?: string): Promise<VoidOrNever> => {
    try {
        await fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/tasks/${itemId}?removeDependentTasks=true`),
            method: "DELETE",
        });
    } catch (e) {
        throw handleError(e);
    }
};

export const requestCloneTask = async (taskId: string, projectId: string, taskNewId: string): Promise<VoidOrNever> => {
    try {
        await fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}/clone?newTask=${taskNewId}`),
            method: "POST",
        });
    } catch (e) {
        throw handleError(e);
    }
};

//missing-type
export const requestCreateTask = async (payload, projectId): Promise<any | never> => {
    try {
        const { data } = await fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/tasks`),
            method: "POST",
            body: payload,
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

//missing-type
export const requestCreateProject = async (payload: ICreateProjectPayload): Promise<any | never> => {
    try {
        const { data } = await fetch({
            url: workspaceApi(`/projects`),
            method: "POST",
            body: payload,
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

//missing-type
export const requestProjectPrefixes = async (projectId: string): Promise<any | never> => {
    try {
        const { data } = await fetch({
            url: workspaceApi(`/projects/${projectId}/prefixes`),
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

//missing-type
export const requestChangePrefixes = async (
    prefixName: string,
    prefixUri: string,
    projectId: string
): Promise<any | never> => {
    try {
        const { data } = await fetch({
            url: workspaceApi(`/projects/${projectId}/prefixes/${prefixName}`),
            method: "PUT",
            body: prefixUri,
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

//missing-type
export const requestRemoveProjectPrefix = async (prefixName: string, projectId: string): Promise<any | never> => {
    try {
        const { data } = await fetch({
            url: workspaceApi(`/projects/${projectId}/prefixes/${prefixName}`),
            method: "DELETE",
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

//missing-type
export const requestResourcesList = async (projectId: string): Promise<any | never> => {
    try {
        const { data } = await fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/resources`),
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

//missing-type
export const requestIfResourceExists = async (projectId: string, resourceName: string): Promise<any | never> => {
    try {
        const { data } = await fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/resources/${resourceName}/metadata`),
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

//missing-type
export const requestWarningList = async (projectId: string): Promise<any | never> => {
    try {
        const { data } = await fetch({
            url: workspaceApi(`/projects/${projectId}/failedTasksReport`),
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

//missing-type
export const requestWarningMarkdown = async (taskId: string, projectId: string): Promise<any | never> => {
    try {
        const { data } = await fetch({
            url: workspaceApi(`/projects/${projectId}/failedTasksReport/${taskId}`),
            headers: {
                Accept: "text/markdown",
                "Content-Type": "text/markdown",
            },
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};
