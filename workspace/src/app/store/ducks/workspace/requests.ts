import {
    IAppliedFacetState,
    IFacetState,
    IProjectExecutionStatus,
    IProjectImportDetails,
    IRecentlyViewedItem,
    ISearchResultsServer,
    ISorterListItemState,
    ITaskLink,
    Keywords,
} from "@ducks/workspace/typings";
import fetch from "../../../services/fetch";
import { legacyApiEndpoint, workspaceApi } from "../../../utils/getApiEndpoint";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";

export interface ISearchListRequest {
    limit?: number;
    offset?: number;
    sortBy?: string;
    sortOrder?: string;
    project?: string;
    facets?: IAppliedFacetState[];
    textQuery?: string;
}

export interface ISearchListResponse {
    total: number;
    facets: IFacetState[];
    results: ISearchResultsServer[];
    sortByProperties: ISorterListItemState[];
}

export interface ICreateProjectPayload {
    id?: string;
    metaData: {
        label: string;
        description?: string;
    };
}

const handleError = (e) => {
    return e.errorResponse;
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

export const requestRemoveProject = async (itemId: string): Promise<FetchResponse<void>> => {
    return await fetch({
        url: legacyApiEndpoint(`/projects/${itemId}`),
        method: "DELETE",
    });
};

export const requestRemoveTask = async (
    itemId: string,
    projectId?: string,
    removeDependentTasks: boolean = false
): Promise<FetchResponse<void>> => {
    return await fetch({
        url:
            legacyApiEndpoint(`/projects/${projectId}/tasks/${itemId}`) +
            (removeDependentTasks ? "?removeDependentTasks=true" : ""),
        method: "DELETE",
    });
};

interface IClonedItem {
    id: string;
    detailsPage: string;
}

export const requestCopyProject = async (projectId: string, payload: any) => {
    return fetch({
        url: legacyApiEndpoint(`/projects/${projectId}/copy`),
        method: "POST",
        body: payload,
    });
};

export const requestCopyTask = async (projectId: string, taskId: string, payload: any) => {
    return fetch({
        url: legacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}/copy`),
        method: "POST",
        body: payload,
    });
};

export const requestCloneTask = async (
    taskId: string,
    projectId: string,
    payload: any
): Promise<FetchResponse<IClonedItem>> => {
    return fetch({
        url: workspaceApi(`/projects/${projectId}/tasks/${taskId}/clone`),
        method: "POST",
        body: payload,
    });
};

export const requestCloneProject = async (projectId: string, payload: any): Promise<FetchResponse<IClonedItem>> => {
    return fetch({
        url: workspaceApi(`/projects/${projectId}/clone`),
        method: "POST",
        body: payload,
    });
};

//missing-type
export const requestCreateTask = async (payload, projectId): Promise<any | never> => {
    return fetch({
        url: legacyApiEndpoint(`/projects/${projectId}/tasks`),
        method: "POST",
        body: payload,
    });
};

// Update project task
export const requestUpdateProjectTask = async (projectId: string, itemId: string, payload): Promise<void> => {
    try {
        const { data } = await fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/tasks/${itemId}`),
            method: "PATCH",
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
export const requestIfResourceExists = async (projectId: string, resourceName: string): Promise<boolean> => {
    try {
        const { data } = await fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/resources/${encodeURIComponent(resourceName)}/metadata`),
        });
        return "size" in data;
    } catch (e) {
        if (e.isHttpError && e.httpStatus === 404) {
            return false;
        }
        throw e;
    }
};

/** Remove a project file resource. */
export const requestRemoveProjectResource = async (projectId: string, resourceName: string): Promise<void> => {
    try {
        await fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/resources/${encodeURIComponent(resourceName)}`),
            method: "DELETE",
        });
    } catch (e) {
        throw handleError(e);
    }
};

/** Returns all tasks that depend on a specific resource. */
export const projectFileResourceDependents = async (
    projectId: string,
    resourceName: string
): Promise<FetchResponse<ITaskLink[]>> => {
    return fetch({
        url: legacyApiEndpoint(`/projects/${projectId}/resources/${encodeURIComponent(resourceName)}/usage`),
    });
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

const projectImportEndpoint = (projectImportId: string) => workspaceApi(`/projectImport/${projectImportId}`);

/** Fetch the project import details for the previously uploaded project file. */
export const requestProjectImportDetails = async (
    projectImportId: string
): Promise<FetchResponse<IProjectImportDetails>> => {
    return fetch({
        url: projectImportEndpoint(projectImportId),
    });
};

/** Deletes the project import resource and the uploaded file in the backend. */
export const requestDeleteProjectImport = async (projectImportId: string): Promise<FetchResponse<void>> => {
    return fetch({
        url: projectImportEndpoint(projectImportId),
        method: "DELETE",
    });
};

/** Start the actual project import for the previously uploaded project file.
 *
 * @param projectImportId The project import ID.
 * @param generateNewId   If the project should be imported under a freshly generated ID. E.g. when there already exists a project with the same ID.
 */
export const requestStartProjectImport = async (
    projectImportId: string,
    generateNewId: boolean,
    overwriteExistingProject: boolean
): Promise<FetchResponse<void>> => {
    return fetch({
        url:
            projectImportEndpoint(projectImportId) +
            `?generateNewId=${generateNewId}&overwriteExisting=${overwriteExistingProject}`,
        method: "POST",
    });
};

/** When the actual project import has been started, this endpoint will inform about the progress. */
export const requestProjectImportExecutionStatus = async (
    projectImportId: string
): Promise<FetchResponse<IProjectExecutionStatus>> => {
    return fetch({
        url: projectImportEndpoint(projectImportId) + "/status",
    });
};

/** Fetch the recently viewed items of the current user. */
export const recentlyViewedItems = async (): Promise<FetchResponse<IRecentlyViewedItem[]>> => {
    return fetch({
        url: workspaceApi("/recentlyViewedItems"),
    });
};

//get all tags for a project
export const requestProjectTags = async (projectId: string): Promise<FetchResponse<{ tags: Keywords }>> =>
    fetch({
        url: workspaceApi(`/projects/${projectId}/tags`),
    });
