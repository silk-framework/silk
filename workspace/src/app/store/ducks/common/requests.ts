import fetch from "../../../services/fetch";
import { coreApi, workspaceApi } from "../../../utils/getApiEndpoint";
import { generateNetworkError, isNetworkError } from "../../../services/errorLogger";

const handleError = ({ response }) => {
    if (isNetworkError(response.data)) {
        return generateNetworkError(response.data);
    }
    return response.data;
};

/**
 * Get initial configs for whole application
 */
export const requestInitFrontend = async (): Promise<any | never> => {
    try {
        const { data } = await fetch({
            url: workspaceApi("/initFrontend"),
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

/**
 * get common data types or for specific project
 * @param projectId
 */
export const requestSearchConfig = async (projectId?: string): Promise<any | never> => {
    try {
        const url = projectId ? `/searchConfig/types?projectId=${projectId}` : `/searchConfig/types`;
        const { data } = await fetch({
            url: workspaceApi(url),
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

/**
 * Get plugins list
 * @param payload
 */
export const requestArtefactList = async (payload: any): Promise<any | never> => {
    try {
        const { data } = await fetch({
            url: coreApi("/taskPlugins"),
            body: payload,
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

/**
 * Get properties(form) for specific plugin
 * @param artefactKey
 */
export const requestArtefactProperties = async (artefactKey: string): Promise<any | never> => {
    try {
        const { data } = await fetch({
            url: coreApi(`/plugins/${artefactKey}`),
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};
