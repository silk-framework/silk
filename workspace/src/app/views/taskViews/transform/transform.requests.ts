import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import fetch from "../../../services/fetch";
import { legacyTransformEndpoint } from "../../../utils/getApiEndpoint";
import {IComplexMappingRule, ITransformRule, PartialBy} from "./transform.types";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import { TaskContext } from "../../shared/projectTaskTabView/projectTaskTabView.typing";
import { IPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";

/** Fetches a transform rule. */
export const requestTransformRule = async (
    projectId: string,
    transformId: string,
    ruleId: string,
    convertToComplex: boolean = true
): Promise<FetchResponse<ITransformRule>> => {
    return fetch({
        url: legacyTransformEndpoint(
            `/tasks/${projectId}/${transformId}/rule/${ruleId}` + (convertToComplex ? "?convertToComplex=true" : "")
        ),
    });
};

/**
 * Fetches partial auto-completion results for the transforms task input paths, i.e. any part of a path could be auto-completed
 * without replacing the complete path.
 *
 * @param projectId
 * @param transformTaskId
 * @param rule
 * @param inputType      Fetches paths either from the source or target input data source.
 * @param inputString    The path input string
 * @param cursorPosition The cursor position inside the input string
 * @param limit          The max number of results to return.
 */
export const partialAutoCompleteTransformInputPaths = (
    projectId: string,
    transformTaskId: string,
    rule: string,
    inputString: string,
    cursorPosition: number,
    limit?: number
): Promise<FetchResponse<IPartialAutoCompleteResult>> => {
    return fetch({
        url: legacyTransformEndpoint(
            `/tasks/${projectId}/${transformTaskId}/rule/${rule}/completions/partialSourcePaths`
        ),
        method: "POST",
        body: {
            inputString,
            cursorPosition,
            maxSuggestions: limit,
        },
    });
};

/** fetch source paths for transform editor */
export const autoCompleteTransformSourcePath = (
    projectId: string,
    taskId: string,
    ruleId: string,
    term = "",
    taskContext?: TaskContext,
    limit = 100
): Promise<FetchResponse<IAutocompleteDefaultResponse[]>> => {
    return fetch({
        url: legacyTransformEndpoint(
            `/tasks/${projectId}/${taskId}/rule/${ruleId}/completions/sourcePaths?maxResults=${limit}&term=${term}`
        ),
        method: "POST",
        body: {
            taskContext,
        },
    });
};

/** Save a transform rule. */
export const putTransformRule = async (
    projectId: string,
    transformId: string,
    ruleId: string,
    complexTransformRule: IComplexMappingRule
): Promise<FetchResponse<void>> => {
    return fetch({
        url: legacyTransformEndpoint(`/tasks/${projectId}/${transformId}/rule/${ruleId}`),
        method: "PUT",
        body: complexTransformRule,
    });
};

/** get evaluation values for a given ruleId */
export const evaluateTransformRule = async (
    projectId: string,
    transformTaskId: string,
    containerRuleId: string,
    rule,
    limit: number = 100
): Promise<FetchResponse<any>> => {
    return fetch({
        url: legacyTransformEndpoint(`/tasks/${projectId}/${transformTaskId}/rule/${containerRuleId}/evaluateRule`),
        method: "POST",
        body: rule,
        query: {
            limit,
        },
    });
};

/** Appends a transform rule as a child of a container rule. */
export const appendTransformRule = async (
    projectId: string,
    transformId: string,
    containerRuleId: string,
    newRule: PartialBy<ITransformRule, "id" | "metadata">,
    afterRuleId?: string
): Promise<FetchResponse<ITransformRule>> => {
    return fetch({
        url: legacyTransformEndpoint(`/tasks/${projectId}/${transformId}/rule/${containerRuleId}/rules` + (afterRuleId ? `?afterRuleId=${afterRuleId}` : "")),
        method: "POST",
        body: newRule,
    });
};

/** Appends a transform rule as a child of a container rule. */
export const removeTransformRule = async (
    projectId: string,
    transformId: string,
    ruleId: string
): Promise<FetchResponse<ITransformRule>> => {
    return fetch({
        url: legacyTransformEndpoint(`/tasks/${projectId}/${transformId}/rule/${ruleId}`),
        method: "DELETE"
    });
};
