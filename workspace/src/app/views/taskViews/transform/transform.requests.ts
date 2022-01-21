import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import fetch from "../../../services/fetch";
import { legacyTransformEndpoint } from "../../../utils/getApiEndpoint";
import { IComplexMappingRule, ITransformRule } from "./transform.types";

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
