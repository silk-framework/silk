import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import fetch from "../../../services/fetch";
import { legacyTransformEndpoint } from "../../../utils/getApiEndpoint";
import {IComplexMappingRule, ITransformRule, PartialBy} from "./transform.types";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import {TaskContext} from "../../shared/projectTaskTabView/projectTaskTabView.typing";

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
            taskContext
        }
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
