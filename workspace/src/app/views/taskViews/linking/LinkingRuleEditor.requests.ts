import fetch from "../../../services/fetch";
import { legacyLinkingEndpoint, legacyTransformEndpoint } from "../../../utils/getApiEndpoint";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { PathWithMetaData } from "../shared/rules/rule.typings";
import { IEntityLink, IEvaluatedReferenceLinks, ILinkingRule, ILinkingTaskParameters } from "./linking.types";
import { IAutocompleteDefaultResponse, TaskPlugin } from "@ducks/shared/typings";
import { CodeAutocompleteFieldPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";

/** Fetches the cached paths from the linking paths cache.*/
export const fetchLinkingCachedPaths = (
    projectId: string,
    linkingTaskId: string,
    which: "source" | "target",
    withMetaData: boolean = true,
    langPref: string = "en",
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
    langPref: string = "en",
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
    linkingRule: ILinkingRule,
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
    linkLimit: number = 100,
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

/** Get a an evaluation of the currently saved linkage rule against the reference links.
 *
 * @param projectId
 * @param linkingTaskId
 * @param withEntitiesAndSchema If true each link contains both entities and the schemata.
 */
export const referenceLinksEvaluated = (
    projectId: string,
    linkingTaskId: string,
    withEntitiesAndSchema: boolean,
): Promise<FetchResponse<IEvaluatedReferenceLinks>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/referenceLinksEvaluated`),
        query: {
            withEntitiesAndSchema,
        },
    });
};

/** Fetch evaluated links for the given linkage rule. */
export const evaluateLinkingRule = (
    projectId: string,
    linkingTaskId: string,
    linkingRule: ILinkingRule,
    linkLimit: number = 100,
    timeoutInMs: number = 30000,
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

/** Fetches auto-completion results for the linking task input paths.
 *
 * @param projectId
 * @param linkingTaskId
 * @param inputType     Fetches paths either from the source or target input data source.
 * @param searchQuery   The multi-word search query
 * @param limit         The max number of results to return.
 */
export const autoCompleteLinkingInputPaths = (
    projectId: string,
    linkingTaskId: string,
    inputType: "source" | "target",
    searchQuery: string,
    limit: number,
): Promise<FetchResponse<IAutocompleteDefaultResponse[]>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/completions/inputPaths`),
        query: {
            term: searchQuery,
            maxResults: limit,
            target: inputType === "target",
        },
    });
};

/**
 * Fetches partial auto-completion results for the linking task input paths, i.e. any part of a path could be auto-completed
 * without replacing the complete path.
 *
 * @param projectId
 * @param linkingTaskId
 * @param inputType      Fetches paths either from the source or target input data source.
 * @param inputString    The path input string
 * @param cursorPosition The cursor position inside the input string
 * @param limit          The max number of results to return.
 */
export const partialAutoCompleteLinkingInputPaths = (
    projectId: string,
    linkingTaskId: string,
    inputType: "source" | "target",
    inputString: string,
    cursorPosition: number,
    limit?: number,
    langPref?: string,
): Promise<FetchResponse<CodeAutocompleteFieldPartialAutoCompleteResult>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/completions/partialSourcePaths`),
        method: "POST",
        query: {
            target: inputType === "target",
        },
        body: {
            inputString,
            cursorPosition,
            maxSuggestions: limit,
            langPref,
        },
    });
};

/** Adds an additional path to the reference entities cache of a linking task. */
export const addPathToReferenceEntitiesCache = (
    projectId: string,
    linkingTaskId: string,
    path: string,
    toTarget: boolean,
): Promise<FetchResponse<void>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/referenceEntitiesCache/path`),
        method: "POST",
        body: {
            path,
            toTarget,
            reloadCache: true,
        },
    });
};

const linkingRuleEditorRequests = {
    fetchLinkingCachedPaths,
};

export default linkingRuleEditorRequests;
