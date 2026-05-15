import fetch from "../../../services/fetch";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { projectApi } from "../../../utils/getApiEndpoint";
import { IRuleBlockSummary } from "./ruleBlock.types";

/** Fetches lightweight summaries of all reusable rule blocks in a project. */
export const requestRuleBlockSummaries = async (
    projectId: string,
): Promise<FetchResponse<IRuleBlockSummary[]>> => {
    return fetch({
        url: projectApi(`/${projectId}/ruleBlocks`),
    });
};
