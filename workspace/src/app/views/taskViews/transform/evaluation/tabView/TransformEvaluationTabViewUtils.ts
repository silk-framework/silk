import { FetchResponse } from "../../../../../services/fetch/responseInterceptor";
import fetch from "../../../../../services/fetch";
import { legacyTransformEndpoint } from "../../../../../utils/getApiEndpoint";
import { EvaluatedRuleEntityResult } from "./typing";

export const getEvaluatedEntities = async (
    projectId: string,
    taskId: string,
    ruleId: string,
    limit: number,
    showOnlyEntitiesWithUris: boolean
): Promise<FetchResponse<EvaluatedRuleEntityResult>> =>
    fetch({
        method: "GET",
        url: legacyTransformEndpoint(`/tasks/${projectId}/${taskId}/rule/${ruleId}/evaluated`),
        body: {
            limit,
            showOnlyEntitiesWithUris: Number(showOnlyEntitiesWithUris),
        },
    });
