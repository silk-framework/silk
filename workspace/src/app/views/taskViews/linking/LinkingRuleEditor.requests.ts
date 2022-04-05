import fetch from "../../../services/fetch";
import { legacyLinkingEndpoint } from "../../../utils/getApiEndpoint";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { PathWithMetaData } from "../shared/rules/rule.typings";
import { IEntityLink, IEvaluatedReferenceLinks, ILinkingRule, ILinkingTaskParameters } from "./linking.types";
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

/** Get a an evaluation of the linkage rule against the reference links. */
export const evaluateLinkingRuleAgainstReferenceEntities = (
    projectId: string,
    linkingTaskId: string,
    linkingRule: ILinkingRule,
    linkLimit: number = 100
): Promise<FetchResponse<IEvaluatedReferenceLinks>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/referenceLinksEvaluated`),
        method: "POST",
        body: linkingRule,
        query: {
            linkLimit,
        },
    });
};

/** Fetch evaluated links for the given linkage rule. */
export const evaluateLinkingRule = (
    projectId: string,
    linkingTaskId: string,
    linkingRule: ILinkingRule,
    linkLimit: number = 100,
    timeoutInMs: number = 30000
): Promise<FetchResponse<IEntityLink[]>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/evaluateLinkageRule`),
        method: "POST",
        body: linkingRule,
        query: {
            linkLimit,
            timeoutInMs,
        },
    });
};

/**
 * linkLimit: Int ?= 1000, timeoutInMs: Int ?= 30000, includeReferenceLinks: Boolean ?= false
 */

const linkingRuleEditorRequests = {
    fetchLinkingCachedPaths,
};

export default linkingRuleEditorRequests;
