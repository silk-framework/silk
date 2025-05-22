import fetch from "../../../../../services/fetch";
import { legacyLinkingEndpoint } from "../../../../../utils/getApiEndpoint";
import { FetchResponse } from "../../../../../services/fetch/responseInterceptor";
import {
    EvaluationLinkInputValue,
    LinkEvaluationFilters,
    LinkEvaluationSortBy,
    LinkRuleEvaluationResult,
    ReferenceLinkType,
} from "./typings";
import { IAggregationOperator, IComparisonOperator, ISimilarityOperator } from "../../linking.types";
import { IPluginDetails } from "@ducks/common/typings";
import { IPathInput, ITransformOperator } from "views/taskViews/shared/rules/rule.typings";
import { TreeNodeInfo } from "@eccenca/gui-elements";

/**
 * Get evaluated links.
 * @param projectId  Project ID
 * @param taskId     Linking Task ID
 * @param pagination Pagination object
 * @param query      Text query to filter links by.
 * @param filters    Filters that are applied to the links.
 * @param sortBy     Sort criteria that are applied hierarchically.
 * @param includeReferenceLinks If all reference links should be included in the result. Some reference links may still be included
 *                              even if this is set to false.
 */
export const getEvaluatedLinks = async (
    projectId: string,
    taskId: string,
    pagination: { current: number; total: number; limit: number },
    query: string = "",
    filters: Array<keyof typeof LinkEvaluationFilters> = [],
    sortBy: LinkEvaluationSortBy[] = [],
    includeReferenceLinks = false,
    includeEvaluationLinks = true,
): Promise<FetchResponse<LinkRuleEvaluationResult>> =>
    fetch({
        method: "POST",
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${taskId}/evaluate`),
        body: {
            query,
            offset: (pagination.current - 1) * pagination.limit,
            limit: pagination.limit,
            filters,
            sortBy,
            includeReferenceLinks,
            includeEvaluationLinks,
        },
    });

//update reference link state to either positive, negative or unlabelled
export const updateReferenceLink = async (
    projectId: string,
    taskId: string,
    source: string,
    target: string,
    linkType: ReferenceLinkType,
): Promise<FetchResponse<any>> =>
    fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${taskId}/referenceLink`),
        method: linkType === "unlabeled" ? "DELETE" : "PUT",
        query: {
            linkType: linkType === "unlabeled" ? undefined : linkType,
            source,
            target,
        },
    });

export const referenceLinksChangeRequest = async (
    projectId: string,
    taskId: string,
    query: {
        positive?: boolean;
        negative?: boolean;
        unlabeled?: boolean;
        generateNegative?: boolean;
    },
    method: "DELETE" | "PUT",
    body?: any,
): Promise<FetchResponse<any>> =>
    fetch({
        url: legacyLinkingEndpoint(`/tasks/${projectId}/${taskId}/referenceLinks`),
        method,
        query,
        body,
    });

export const getOperatorPath = (operatorInput: any): Array<{ id: string; path: string }> => {
    if (operatorInput.path) {
        return [{ path: operatorInput.path, id: operatorInput.id }];
    }

    return operatorInput.inputs.reduce(
        (acc, val) => {
            acc = [...acc, ...getOperatorPath(val)];
            return acc;
        },
        [] as Array<{ id: string; path: string }>,
    );
};

export const getLinkRuleInputPaths = (operatorInput: ISimilarityOperator) =>
    ["sourceInput", "targetInput"].reduce(
        (linkRuleInputPaths, inputPathType) => {
            const label = inputPathType.replace("Input", "");
            //no intermittent inputs but are direct paths
            if (!operatorInput[inputPathType]?.inputs) {
                linkRuleInputPaths[label][operatorInput[inputPathType].path] = operatorInput[inputPathType].id;
            } else {
                operatorInput[inputPathType].inputs.forEach((i) => {
                    getOperatorPath(i).forEach(({ path, id }) => {
                        linkRuleInputPaths[label][path] = id;
                    });
                });
            }
            return linkRuleInputPaths;
        },
        { source: {}, target: {} } as EvaluationLinkInputValue<string>,
    );

export const getOperatorLabel = (
    operator: any,
    operatorPlugins: IPluginDetails[],
    emptyPathLabel: string,
): string | undefined => {
    switch (operator.type) {
        case "Aggregation":
            return operatorPlugins.find((plugin) => plugin.pluginId === (operator as IAggregationOperator).aggregator)
                ?.title;
        case "Comparison":
            return operatorPlugins.find((plugin) => plugin.pluginId === (operator as IComparisonOperator).metric)
                ?.title;
        case "transformInput":
            return operatorPlugins.find((plugin) => plugin.pluginId === (operator as ITransformOperator).function)
                ?.title;
        case "pathInput":
            return (operator as IPathInput).path || emptyPathLabel;
        default:
            return undefined;
    }
};

export const getParentNodes = (tree: TreeNodeInfo, nodeId: string, ancestors = [] as string[]): Array<string> => {
    let match = [] as string[];
    const traverse = (tree: TreeNodeInfo, nodeId: string, ancestors = [] as string[]) => {
        if (tree.id === nodeId) {
            match = ancestors;
        }

        if (!tree.childNodes?.length) return [];

        tree.childNodes.forEach((subTree) => {
            traverse(subTree, nodeId, [...ancestors, tree.id as string]);
        });
    };
    traverse(tree, nodeId);
    return match;
};
