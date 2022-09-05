import { FetchResponse } from "../../../../services/fetch/responseInterceptor";
import fetch from "../../../../services/fetch";
import { learningApi } from "../../../../utils/getApiEndpoint";
import {
    ActiveLearningBestRule,
    ActiveLearningDecisions,
    ActiveLearningLinkCandidate,
    ActiveLearningReferenceLinks,
    ActiveLearningSessionInfo,
    ComparisonPair,
    ComparisonPairs,
} from "./LinkingRuleActiveLearning.typings";
import { ReferenceLinks } from "../linking.types";

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
        url: learningApi(`/tasks/${projectId}/${linkingTaskId}/activeLearning/comparisonPairs`),
    });
};

/** Add a comparison pair to the current active learning session. */
export const addActiveLearningComparisonPair = (
    projectId: string,
    linkingTaskId: string,
    comparisonPair: ComparisonPair
): Promise<FetchResponse<void>> => {
    return fetch({
        url: learningApi(`/tasks/${projectId}/${linkingTaskId}/activeLearning/comparisonPairs/addSelected`),
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
        url: learningApi(`/tasks/${projectId}/${linkingTaskId}/activeLearning/comparisonPairs/removeSelected`),
        method: "POST",
        body: comparisonPair,
    });
};

/** Get the next link candidate. */
export const nextActiveLearningLinkCandidate = (
    projectId: string,
    linkingTaskId: string
): Promise<FetchResponse<ActiveLearningLinkCandidate>> => {
    return fetch({
        url: learningApi(`/tasks/${projectId}/${linkingTaskId}/activeLearning/nextLinkCandidate`),
    });
};

/** Get the current best learned linkage rule. */
export const bestLearnedLinkageRule = (
    projectId: string,
    linkingTaskId: string
): Promise<FetchResponse<ActiveLearningBestRule>> => {
    return fetch({
        url: learningApi(`/tasks/${projectId}/${linkingTaskId}/activeLearning/bestRule`),
        query: {
            withLabels: true,
        },
    });
};

/** Fetch reference links / decision history for active learning. */
export const fetchActiveLearningReferenceLinks = (
    projectId: string,
    linkingTaskId: string,
    includePositiveLinks: boolean = true,
    includeNegativeLinks: boolean = true,
    includeUnlabeledLinks: boolean = false
): Promise<FetchResponse<ActiveLearningReferenceLinks>> => {
    return fetch({
        url: learningApi(`/tasks/${projectId}/${linkingTaskId}/activeLearning/referenceLinks`),
        query: {
            withComparisons: true,
            includePositiveLinks,
            includeNegativeLinks,
            includeUnlabeledLinks,
        },
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
        url: learningApi(`/tasks/${projectId}/${linkingTaskId}/activeLearning/referenceLinks/add`),
        method: "POST",
        query: {
            linkSource: sourceUri,
            linkTarget: targetUri,
            decision: decision,
        },
    });
};

/** Save the current active learning state. */
export const saveActiveLearningResults = (
    projectId: string,
    linkingTaskId: string,
    saveRule: boolean,
    saveReferenceLinks: boolean
): Promise<FetchResponse<void>> => {
    return fetch({
        url: learningApi(`/tasks/${projectId}/${linkingTaskId}/activeLearning/saveResult`),
        method: "POST",
        query: {
            saveRule,
            saveReferenceLinks,
        },
    });
};

/** Reset learning state. */
export const resetActiveLearningSession = (projectId: string, linkingTaskId: string): Promise<FetchResponse<void>> => {
    return fetch({
        url: learningApi(`/tasks/${projectId}/${linkingTaskId}/activeLearning/reset`),
        method: "POST",
    });
};

/** Infos about the current active learning session. */
export const fetchActiveLearningSessionInfo = (
    projectId: string,
    linkingTaskId: string
): Promise<FetchResponse<ActiveLearningSessionInfo>> => {
    return fetch({
        url: learningApi(`/tasks/${projectId}/${linkingTaskId}/activeLearning/info`),
    });
};
