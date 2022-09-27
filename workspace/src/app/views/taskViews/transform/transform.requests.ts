import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import fetch from "../../../services/fetch";
import { legacyTransformEndpoint } from "../../../utils/getApiEndpoint";
import { IComplexMappingRule, ITransformRule } from "./transform.types";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";

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
    term = ""
): Promise<FetchResponse<IAutocompleteDefaultResponse[]>> => {
    return fetch({
        url: legacyTransformEndpoint(
            `/tasks/${projectId}/${taskId}/rule/${ruleId}/completions/sourcePaths?maxResults=1000&term=${term}`
        ),
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
    ruleId: string,
    rule,
    limit: number = 100
): Promise<FetchResponse<any>> => {
    return fetch({
        url: legacyTransformEndpoint(`/tasks/${projectId}/${transformTaskId}/rule/root/evaluateRule`),
        method: "POST",
        body: rule,
        query: {
            limit,
        },
    });
};
