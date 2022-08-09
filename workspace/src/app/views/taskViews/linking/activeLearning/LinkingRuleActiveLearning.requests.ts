import { FetchResponse } from "../../../../services/fetch/responseInterceptor";
import fetch from "../../../../services/fetch";
import { legacyLinkingEndpoint } from "../../../../utils/getApiEndpoint";
import { ComparisonPair, ComparisonPairs } from "./LinkingRuleActiveLearning.typings";

/** Get the comparison pair configuration for the active learning session.
 *
 * @param projectId
 * @param linkingTaskId
 */
export const activeLearningComparisonPairs = (
    projectId: string,
    linkingTaskId: string
): Promise<FetchResponse<ComparisonPairs>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/activeLearning/comparisonPairs`),
    });
};

/** Add a comparison pair to the current active learning session. */
export const addActiveLearningComparisonPair = (
    projectId: string,
    linkingTaskId: string,
    comparisonPair: ComparisonPair
): Promise<FetchResponse<void>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/activeLearning/comparisonPairs/addSelected`),
        method: "POST",
        body: comparisonPair,
    });
};

/** Remove a specific comparison pair. */
export const removeActiveLearningComparisonPair = (
    projectId: string,
    linkingTaskId: string,
    comparisonPair: ComparisonPair
): Promise<FetchResponse<void>> => {
    return fetch({
        url: legacyLinkingEndpoint(
            `/tasks/${projectId}/${linkingTaskId}/activeLearning/comparisonPairs/removeSelected`
        ),
        method: "POST",
        body: comparisonPair,
    });
};
