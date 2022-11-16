import fetch from "../../../../../services/fetch";
import { legacyLinkingEndpoint } from "../../../../../utils/getApiEndpoint";
import { FetchResponse } from "../../../../../services/fetch/responseInterceptor";
import { LinkingEvaluationResult } from "./typings";

///linking/tasks/:project/:linkingTaskId/evaluate
export const getLinkingEvaluations = async (
    projectId: string,
    taskId: string
): Promise<FetchResponse<{ links: LinkingEvaluationResult[] }> | undefined> =>
    fetch({ url: legacyLinkingEndpoint(`/tasks/${projectId}/${taskId}/evaluate`) });
