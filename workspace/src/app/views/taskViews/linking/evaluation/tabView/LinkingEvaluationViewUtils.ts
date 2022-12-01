import fetch from "../../../../../services/fetch";
import { legacyLinkingEndpoint } from "../../../../../utils/getApiEndpoint";
import { FetchResponse } from "../../../../../services/fetch/responseInterceptor";
import { EvaluationLinkInputValue, LinkingEvaluationResult } from "./typings";
import { ILinkingRule } from "../../linking.types";

///linking/tasks/:project/:linkingTaskId/evaluate
export const getLinkingEvaluations = async (
    projectId: string,
    taskId: string,
    pagination: { current: number; total: number; limit: number }
): Promise<FetchResponse<{ links: LinkingEvaluationResult[]; linkRule: ILinkingRule }> | undefined> =>
    fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${taskId}/evaluate`),
        query: { page: pagination.current, limit: pagination.limit },
    });

/**
 *
 * get all preceding ids
 * get path
 */
export const getOperatorPath = (
    operatorInput: any,
    precedingIds: string[]
): { precedingIds: string[]; path: string } => {
    if (operatorInput.path) {
        return {
            precedingIds: [...precedingIds, operatorInput.id],
            path: operatorInput.path,
        };
    }

    return operatorInput.inputs.reduce((acc, val) => {
        acc = getOperatorPath(val, [...precedingIds, val.id]);
        return acc;
    }, {} as { precedingIds: string[]; path: string });
};

export const getLinkRuleInputPaths = (operatorInput: any) => {
    const linkRuleInputPaths = { source: {}, target: {} } as EvaluationLinkInputValue;
    //no intermittent inputs but are direct paths
    if (!operatorInput.sourceInput?.inputs) {
        const sourceInputPath = operatorInput.sourceInput.path;
        linkRuleInputPaths.source[sourceInputPath] = [operatorInput.sourceInput.id];
    } else {
        operatorInput.sourceInput.inputs.forEach((i) => {
            const { path, precedingIds } = getOperatorPath(i, [operatorInput.sourceInput.id]);
            if (linkRuleInputPaths.source[path]) {
                linkRuleInputPaths.source[path].push(...precedingIds);
            } else {
                linkRuleInputPaths.source[path] = precedingIds;
            }
        });
    }

    if (!operatorInput.targetInput?.inputs) {
        const targetInputPath = operatorInput.targetInput.path;
        linkRuleInputPaths.target[targetInputPath] = [operatorInput.targetInput.id];
    } else {
        operatorInput.targetInput.inputs.forEach((i) => {
            const { path, precedingIds } = getOperatorPath(i, [operatorInput.targetInput.id]);
            if (linkRuleInputPaths.target[path]) {
                linkRuleInputPaths.target[path].push(...precedingIds);
            } else {
                linkRuleInputPaths.target[path] = precedingIds;
            }
        });
    }
    return linkRuleInputPaths;
};
