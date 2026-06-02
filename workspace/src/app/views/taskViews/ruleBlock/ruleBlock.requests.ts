import fetch from "../../../services/fetch";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { projectApi } from "../../../utils/getApiEndpoint";
import { IRuleBlockModel, IRuleBlockSummary } from "./ruleBlock.types";
import { EvaluatedTransformEntity } from "../transform/transform.types";

/** Fetches lightweight summaries of all reusable rule blocks in a project. */
export const requestRuleBlockSummaries = async (
    projectId: string,
): Promise<FetchResponse<IRuleBlockSummary[]>> => {
    return fetch({
        url: projectApi(`/${projectId}/ruleBlocks`),
    });
};

/** Evaluates the current rule block model against its current input examples. */
export const requestRuleBlockEvaluation = async (
    projectId: string,
    taskId: string,
    ruleBlockModel: IRuleBlockModel,
): Promise<FetchResponse<EvaluatedTransformEntity[]>> => {
    return fetch({
        url: projectApi(`/${projectId}/tasks/${taskId}/evaluateRuleBlock`),
        method: "POST",
        body: ruleBlockModel,
    });
};
