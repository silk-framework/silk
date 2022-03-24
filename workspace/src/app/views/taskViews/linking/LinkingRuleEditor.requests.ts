import fetch from "../../../services/fetch";
import { legacyLinkingEndpoint } from "../../../utils/getApiEndpoint";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { PathWithMetaData } from "../shared/rules/rule.typings";
import { ILinkingRule, ILinkingTaskParameters } from "./linking.types";
import { TaskPlugin } from "@ducks/shared/typings";

/** Fetches the cached paths from the linking paths cache.*/
export const fetchLinkingCachedPaths = (
    projectId: string,
    linkingTaskId: string,
    which: "source" | "target",
    withMetaData: boolean = true,
    langPref: string = "en"
): Promise<FetchResponse<string[] | PathWithMetaData[]>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/pathCacheValue/${which}`),
        query: {
            langPref,
            withMetaData,
        },
    });
};

/** Fetch the link spec. Supports returning rule operator parameter values labels. */
export const fetchLinkSpec = (
    projectId: string,
    linkingTaskId: string,
    withLabels: boolean,
    langPref: string = "en"
): Promise<FetchResponse<TaskPlugin<ILinkingTaskParameters>>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}`),
        query: {
            withLabels,
            langPref,
        },
    });
};

/** Update the linking rule of a linking task. */
export const updateLinkageRule = (
    projectId: string,
    linkingTaskId: string,
    linkingRule: ILinkingRule
): Promise<FetchResponse<void>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/rule`),
        method: "PUT",
        body: linkingRule,
    });
};

const linkingRuleEditorRequests = {
    fetchLinkingCachedPaths,
};

export default linkingRuleEditorRequests;
