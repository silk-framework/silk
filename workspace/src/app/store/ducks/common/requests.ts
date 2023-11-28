import fetch from "../../../services/fetch";
import { coreApi, legacyApiEndpoint, projectApi, workspaceApi } from "../../../utils/getApiEndpoint";
import {
    IPluginDetails,
    IOverviewArtefactItemList,
    IExportTypes,
    IInitFrontend,
    ProjectTaskDownloadInfo,
} from "@ducks/common/typings";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";

const handleError = (error) => {
    return error.errorResponse;
};

/**
 * Get initial configs for whole application
 */
export const requestInitFrontend = async (): Promise<IInitFrontend | never> => {
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
export const requestArtefactList = async (payload: any): Promise<IOverviewArtefactItemList | never> => {
    try {
        const { data } = await fetch({
            url: coreApi("/taskPlugins?addMarkdownDocumentation=true"),
            body: payload,
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

/**
 * validate custom projectId by ensuring uniqueness and makes sanity checks
 * @param projectId
 * @returns
 */
export const requestProjectIdValidation = async (projectId: string): Promise<FetchResponse<void>> =>
    fetch({
        url: projectApi(
            `/validateIdentifier?${new URLSearchParams({
                projectIdentifier: projectId,
            })}`
        ),
    });

/**
 * validate custom taskId by ensuring uniqueness and makes sanity checks
 * @param taskId
 * @param projectId
 * @returns
 */

export const requestTaskIdValidation = (taskId: string, projectId: string): Promise<FetchResponse<void>> =>
    fetch({
        url: projectApi(
            `/${projectId}/validateIdentifier?${new URLSearchParams({
                taskIdentifier: taskId,
            })}`
        ),
    });

/**
 * Get properties(form) for specific plugin
 * @param artefactKey
 */
export const requestArtefactProperties = async (artefactKey: string): Promise<IPluginDetails> => {
    try {
        const { data } = await fetch({
            url: coreApi(`/plugins/${artefactKey}`),
            query: {
                withLabels: true,
                addMarkdownDocumentation: true,
            },
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

/**
 * Get all available import/export types
 */
export const requestExportTypes = async (): Promise<IExportTypes[]> => {
    try {
        const { data } = await fetch({
            url: legacyApiEndpoint(`/marshallingPlugins`),
        });
        return data;
    } catch (e) {
        throw handleError(e);
    }
};

/**
 * provides only information about whether a task can be downloaded or not.
 * @param projectId
 * @param taskId
 */
export const checkIfTaskSupportsDownload = async (
    projectId: string,
    taskId: string
): Promise<FetchResponse<ProjectTaskDownloadInfo>> =>
    fetch({ url: legacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}/downloadInfo`) });

/** Fetches the rule operator plugins used in the linking and transform operators. */
export const requestRuleOperatorPluginsDetails = (
    inputOperatorsOnly: boolean
): Promise<FetchResponse<{ [key: string]: IPluginDetails }>> => {
    return fetch({
        url: coreApi("/ruleOperatorPlugins"),
        query: {
            inputOperatorsOnly: inputOperatorsOnly,
            addMarkdownDocumentation: true,
        },
    });
};

/** Fetches plugin description of a specific rule operator plugin */
export const requestRuleOperatorPluginDetails = async (
    pluginId: string,
    addMarkdownDocumentation: boolean,
    withLabels: boolean
): Promise<FetchResponse<IPluginDetails>> => {
    return fetch({
        url: coreApi(`/ruleOperatorPlugins/${pluginId}`),
        query: {
            addMarkdownDocumentation: addMarkdownDocumentation,
            withLabels: withLabels,
        },
    });
};
