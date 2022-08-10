import { FetchResponse } from "../../../../services/fetch/responseInterceptor";
import fetch from "../../../../services/fetch";
import { legacyLinkingEndpoint } from "../../../../utils/getApiEndpoint";
import { ActiveLearningDecisions, ComparisonPair, ComparisonPairs } from "./LinkingRuleActiveLearning.typings";
import { IEntityLink, ReferenceLinks } from "../linking.types";

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

/** Get the next link candidate. */
export const nextActiveLearningLinkCandidate = (
    projectId: string,
    linkingTaskId: string
): Promise<FetchResponse<IEntityLink>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/activeLearning/nextLinkCandidate`),
    });
};

/** Fetch reference links / decision history for active learning. */
export const fetchActiveLearningReferenceLinks = (
    projectId: string,
    linkingTaskId: string
): Promise<FetchResponse<ReferenceLinks>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/activeLearning/referenceLinks`),
    });
};

/** Adds or changes a reference link. */
export const submitActiveLearningReferenceLink = (
    projectId: string,
    linkingTaskId: string,
    sourceUri: string,
    targetUri: string,
    decision: ActiveLearningDecisions
): Promise<FetchResponse<ReferenceLinks>> => {
    return fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/activeLearning/referenceLinks/add`),
        method: "POST",
        query: {
            linkSource: sourceUri,
            linkTarget: targetUri,
            decision: decision,
        },
    });
};
